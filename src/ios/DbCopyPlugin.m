#import "SensorPlugin.h"
#import <CoreMotion/CoreMotion.h>

@interface SensorPlugin ()
@property (strong, nonatomic) CMMotionManager *motionManager;
@end

@implementation SensorPlugin

- (void)pluginInitialize {
    self.motionManager = [[CMMotionManager alloc] init];
}

- (void)getSensorList:(CDVInvokedUrlCommand *)command {
    NSMutableArray *sensorsArray = [[NSMutableArray alloc] init];

    // Accelerometer
    if (self.motionManager.isAccelerometerAvailable) {
        [sensorsArray addObject:[self sensorDictionaryWithName:@"Accelerometer" type:@"Accelerometer"]];
    }

    // Gyroscope
    if (self.motionManager.isGyroAvailable) {
        [sensorsArray addObject:[self sensorDictionaryWithName:@"Gyroscope" type:@"Gyroscope"]];
    }

    // Magnetometer
    if (self.motionManager.isMagnetometerAvailable) {
        [sensorsArray addObject:[self sensorDictionaryWithName:@"Magnetometer" type:@"Magnetometer"]];
    }

    // Device Motion
    if (self.motionManager.isDeviceMotionAvailable) {
        [sensorsArray addObject:[self sensorDictionaryWithName:@"Device Motion" type:@"DeviceMotion"]];
    }

    // Creating the plugin result
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:sensorsArray];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (NSDictionary *)sensorDictionaryWithName:(NSString *)name type:(NSString *)type {
    return @{
        @"name": name,
        @"type": type,
        // Add other sensor properties as needed
    };
}

@end
