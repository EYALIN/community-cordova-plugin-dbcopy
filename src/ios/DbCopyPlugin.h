#import <Cordova/CDV.h>

@interface DbCopyPlugin : CDVPlugin

- (void)copyDbFromStorage:(CDVInvokedUrlCommand*)command;
- (void)copyDbToStorage:(CDVInvokedUrlCommand*)command;

@end
