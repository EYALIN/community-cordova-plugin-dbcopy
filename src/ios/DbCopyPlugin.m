#import "DbCopyPlugin.h"

@implementation DbCopyPlugin

- (void)copyDbFromStorage:(CDVInvokedUrlCommand*)command {
    CDVPluginResult* pluginResult = nil;
    @try {
        NSDictionary* options = [command.arguments objectAtIndex:0];
        NSString* dbName = options[@"dbName"];
        NSString* base64Source = options[@"base64Source"];
        NSString* location = options[@"location"] ?: @"default";
        BOOL deleteOldDb = [options[@"deleteOldDb"] boolValue];

        // Determine the destination path for the database
        NSString* destPath = [self getDatabasePath:location dbName:dbName];

        // Check if the old DB exists and if it should be deleted
        NSFileManager* fileManager = [NSFileManager defaultManager];
        if (deleteOldDb && [fileManager fileExistsAtPath:destPath]) {
            NSError* error;
            if (![fileManager removeItemAtPath:destPath error:&error]) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[NSString stringWithFormat:@"Failed to delete old database: %@", error.localizedDescription]];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                return;
            }
        }

        // Decode Base64 string and write it to a temporary file
        NSData* decodedData = [[NSData alloc] initWithBase64EncodedString:base64Source options:0];
        NSString* tempPath = [NSTemporaryDirectory() stringByAppendingPathComponent:dbName];
        [decodedData writeToFile:tempPath atomically:YES];

        // Copy the temporary file to the destination
        NSError* copyError;
        if (![fileManager copyItemAtPath:tempPath toPath:destPath error:&copyError]) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[NSString stringWithFormat:@"Failed to copy database: %@", copyError.localizedDescription]];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Database copied successfully."];
        }
    } @catch (NSException* exception) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:exception.reason];
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

        // Get the app's database path for the specified file
        NSString* sourcePath = [self getDatabasePath:@"default" dbName:fileName];
        NSString* destPath = [fullPath stringByAppendingPathComponent:fileName];

        NSFileManager* fileManager = [NSFileManager defaultManager];

        // Create the destination directory if it doesn't exist
        NSString* destDir = [fullPath stringByDeletingLastPathComponent];
        if (![fileManager fileExistsAtPath:destDir]) {
            NSError* createDirError;
            if (![fileManager createDirectoryAtPath:destDir withIntermediateDirectories:YES attributes:nil error:&createDirError]) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[NSString stringWithFormat:@"Failed to create destination directory: %@", createDirError.localizedDescription]];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                return;
            }
        }

        // Check if the destination file exists and handle overwrite option
        if ([fileManager fileExistsAtPath:destPath] && !overwrite) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"File already exists and overwrite is set to false."];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            return;
        }

        // Copy the database from sourcePath to the provided fullPath
        NSError* copyError;
        if (![fileManager copyItemAtPath:sourcePath toPath:destPath error:&copyError]) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[NSString stringWithFormat:@"Failed to copy database to storage: %@", copyError.localizedDescription]];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Database copied to storage successfully."];
        }
    } @catch (NSException* exception) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:exception.reason];
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
        basePath = [[self.commandDelegate pathForResource:dbName] stringByDeletingLastPathComponent];
    }
    return [basePath stringByAppendingPathComponent:dbName];
}

@end
