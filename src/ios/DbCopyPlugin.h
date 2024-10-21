#import <Cordova/CDV.h>

@interface SensorPlugin : CDVPlugin

// Method to get the list of sensors
- (void)getSensorList:(CDVInvokedUrlCommand *)command;

@end
