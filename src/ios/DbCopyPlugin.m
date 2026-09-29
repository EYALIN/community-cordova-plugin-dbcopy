#import "DbCopyPlugin.h"

@implementation DbCopyPlugin

static NSString* const kSqliteHeader = @"SQLite format 3\0";

- (BOOL)dataHasSqliteHeader:(NSData*)data {
    const char header[16] = "SQLite format 3\0"; // 16 bytes incl. trailing NUL
    if (!data || [data length] < 16) {
        return NO;
    }
    return memcmp([data bytes], header, 16) == 0;
}

- (void)deleteSidecarsForPath:(NSString*)destPath {
    NSFileManager* fileManager = [NSFileManager defaultManager];
    NSArray* suffixes = @[@"-journal", @"-wal", @"-shm"];
    for (NSString* suffix in suffixes) {
        NSString* sidecar = [destPath stringByAppendingString:suffix];
        if ([fileManager fileExistsAtPath:sidecar]) {
            [fileManager removeItemAtPath:sidecar error:nil];
        }
    }
}

- (void)copyDbFromStorage:(CDVInvokedUrlCommand*)command {
    CDVPluginResult* pluginResult = nil;
    NSFileManager* fileManager = [NSFileManager defaultManager];
    // Temp file lives in the SAME directory as the destination DB (not NSTemporaryDirectory())
    // so the final replace is a same-volume move, and so cleanup is centralized in one place.
    NSString* tempPath = nil;
    NSString* bakPath = nil;
    BOOL renamedOldToBak = NO;

    @try {
        NSDictionary* options = [command.arguments objectAtIndex:0];
        NSString* dbName = options[@"dbName"];
        NSString* base64Source = options[@"base64Source"];
        NSString* location = options[@"location"] ?: @"default";
        BOOL deleteOldDb = [options[@"deleteOldDb"] boolValue];

        // Validate base64 source
        if (!base64Source || [base64Source length] == 0) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                messageAsDictionary:@{@"success": @NO, @"message": @"base64Source is empty or nil"}];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            return;
        }

        // Determine the destination path for the database
        NSString* destPath = [self getDatabasePath:location dbName:dbName];
        NSString* destDir = [destPath stringByDeletingLastPathComponent];

        // Ensure the destination directory exists
        if (![fileManager fileExistsAtPath:destDir]) {
            NSError* createDirError;
            if (![fileManager createDirectoryAtPath:destDir withIntermediateDirectories:YES attributes:nil error:&createDirError]) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                    messageAsDictionary:@{@"success": @NO, @"message": [NSString stringWithFormat:@"Failed to create database directory: %@", createDirError.localizedDescription]}];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                return;
            }
        }

        // Decode Base64 string to a temp file in destDir, validating BEFORE touching the live DB.
        NSData* decodedData = [[NSData alloc] initWithBase64EncodedString:base64Source options:NSDataBase64DecodingIgnoreUnknownCharacters];
        if (!decodedData || [decodedData length] == 0) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                messageAsDictionary:@{@"success": @NO, @"message": @"Failed to decode base64 data - invalid format"}];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            return;
        }

        if (![self dataHasSqliteHeader:decodedData]) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                messageAsDictionary:@{@"success": @NO, @"message": @"Source data is not a valid SQLite database (bad header)."}];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            return;
        }

        tempPath = [destDir stringByAppendingPathComponent:
            [NSString stringWithFormat:@"%@.tmp-%f", dbName, [[NSDate date] timeIntervalSince1970]]];
        NSError* writeError;
        if (![decodedData writeToFile:tempPath options:NSDataWritingAtomic error:&writeError]) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                messageAsDictionary:@{@"success": @NO, @"message": [NSString stringWithFormat:@"Failed to write temp file: %@", writeError.localizedDescription]}];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            [fileManager removeItemAtPath:tempPath error:nil];
            return;
        }

        // Only now, with a validated payload safely on disk, do we touch the live DB.
        BOOL destExists = [fileManager fileExistsAtPath:destPath];
        if (deleteOldDb || destExists) {
            if (destExists) {
                bakPath = [destPath stringByAppendingPathExtension:@"bak"];
                if ([fileManager fileExistsAtPath:bakPath]) {
                    [fileManager removeItemAtPath:bakPath error:nil];
                }
                NSError* bakError;
                if (![fileManager moveItemAtPath:destPath toPath:bakPath error:&bakError]) {
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                        messageAsDictionary:@{@"success": @NO, @"message": [NSString stringWithFormat:@"Failed to back up existing database: %@", bakError.localizedDescription]}];
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                    [fileManager removeItemAtPath:tempPath error:nil];
                    return;
                }
                renamedOldToBak = YES;
            }
        }

        NSError* moveError;
        if (![fileManager moveItemAtPath:tempPath toPath:destPath error:&moveError]) {
            // restore original DB, nothing was actually replaced
            if (renamedOldToBak) {
                [fileManager moveItemAtPath:bakPath toPath:destPath error:nil];
            }
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                messageAsDictionary:@{@"success": @NO, @"message": [NSString stringWithFormat:@"Failed to install the new database: %@", moveError.localizedDescription]}];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            [fileManager removeItemAtPath:tempPath error:nil];
            return;
        }

        // Success: sidecars from the OLD database are stale relative to the new file.
        [self deleteSidecarsForPath:destPath];
        if (renamedOldToBak) {
            [fileManager removeItemAtPath:bakPath error:nil];
        }

        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
            messageAsDictionary:@{@"success": @YES, @"message": @"Database copied successfully."}];
    } @catch (NSException* exception) {
        // best-effort restore of the original DB on unexpected failure
        if (renamedOldToBak && bakPath) {
            [fileManager moveItemAtPath:bakPath toPath:[bakPath stringByDeletingPathExtension] error:nil];
        }
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
            messageAsDictionary:@{@"success": @NO, @"message": exception.reason ?: @"Unknown error"}];
    } @finally {
        // ALWAYS clean up the temp file — it must never be left behind, success or failure.
        if (tempPath && [fileManager fileExistsAtPath:tempPath]) {
            [fileManager removeItemAtPath:tempPath error:nil];
        }
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)copyDbToStorage:(CDVInvokedUrlCommand*)command {
    CDVPluginResult* pluginResult = nil;
    @try {
        NSDictionary* options = [command.arguments objectAtIndex:0];
        NSString* fileName = options[@"fileName"];
        NSString* fullPath = options[@"fullPath"];
        BOOL overwrite = [options[@"overwrite"] boolValue];

        // Convert file:// URL to path if needed (Cordova's dataDirectory returns a URL)
        if ([fullPath hasPrefix:@"file://"]) {
            NSURL* url = [NSURL URLWithString:fullPath];
            fullPath = [url path];
        }

        // Get the app's database path for the specified file
        NSString* sourcePath = [self getDatabasePath:@"default" dbName:fileName];
        NSString* destPath = [fullPath stringByAppendingPathComponent:fileName];

        NSFileManager* fileManager = [NSFileManager defaultManager];

        // Create the destination directory if it doesn't exist
        // Use fullPath directly as the directory (not stringByDeletingLastPathComponent)
        // because fullPath already contains the backup folder path
        if (![fileManager fileExistsAtPath:fullPath]) {
            NSError* createDirError;
            if (![fileManager createDirectoryAtPath:fullPath withIntermediateDirectories:YES attributes:nil error:&createDirError]) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                    messageAsDictionary:@{@"success": @NO, @"message": [NSString stringWithFormat:@"Failed to create destination directory: %@", createDirError.localizedDescription]}];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                return;
            }
        }

        // Check if the destination file exists and handle overwrite option
        if ([fileManager fileExistsAtPath:destPath] && !overwrite) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                messageAsDictionary:@{@"success": @NO, @"message": @"File already exists and overwrite is set to false."}];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            return;
        }

        // Copy the database from sourcePath to the provided fullPath
        NSError* copyError;
        if (![fileManager copyItemAtPath:sourcePath toPath:destPath error:&copyError]) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                messageAsDictionary:@{@"success": @NO, @"message": [NSString stringWithFormat:@"Failed to copy database to storage: %@", copyError.localizedDescription]}];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                messageAsDictionary:@{@"success": @YES, @"message": @"Database copied to storage successfully."}];
        }
    } @catch (NSException* exception) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
            messageAsDictionary:@{@"success": @NO, @"message": exception.reason ?: @"Unknown error"}];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

// Helper method to get the database path based on location
- (NSString*)getDatabasePath:(NSString*)location dbName:(NSString*)dbName {
    NSString* basePath;
    if ([location isEqualToString:@"documents"]) {
        basePath = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
    } else if ([location isEqualToString:@"external"]) {
        basePath = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) firstObject];
    } else {
        // Default location for cordova-sqlite-storage is Library/LocalDatabase
        NSString* libraryPath = [NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES) firstObject];
        basePath = [libraryPath stringByAppendingPathComponent:@"LocalDatabase"];
    }
    return [basePath stringByAppendingPathComponent:dbName];
}

@end
