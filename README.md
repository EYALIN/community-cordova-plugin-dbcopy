[![NPM version](https://img.shields.io/npm/v/community-cordova-plugin-sensors)](https://www.npmjs.com/package/community-cordova-plugin-sensors)


# community-cordova-plugin-sensors


I dedicate a considerable amount of my free time to developing and maintaining many cordova plugins for the community ([See the list with all my maintained plugins][community_plugins]).
To help ensure this plugin is kept updated,
new features are added and bugfixes are implemented quickly,
please donate a couple of dollars (or a little more if you can stretch) as this will help me to afford to dedicate time to its maintenance.
Please consider donating if you're using this plugin in an app that makes you money, 
or if you're asking for new features or priority bug fixes. Thank you!

[![](https://img.shields.io/static/v1?label=Sponsor%20Me&style=for-the-badge&message=%E2%9D%A4&logo=GitHub&color=%23fe8e86)](https://github.com/sponsors/eyalin)



---

# community-cordova-plugin-sensors

`community-cordova-plugin-sensors` is a Cordova plugin that enables access to sensor data on Android and iOS devices. It provides functionality to retrieve a list of available sensors and their properties.

## Features

- Retrieve a list of sensors available on the device.
- Access sensor properties on Android and iOS.

## Installation

```bash
cordova plugin add community-cordova-plugin-sensors
```

## Supported Platforms

- Android
- iOS

## Usage

To use the plugin, call the `getSensorList` method. This method returns a promise that resolves with an array of sensor objects.

### Example

```javascript
document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady() {
    window.SensorPlugin.getSensorList()
        .then(sensors => {
            console.log('Sensors:', sensors);
        })
        .catch(error => {
            console.error('Error:', error);
        });
}
```

## API

### Methods

#### getSensorList

Returns a promise that resolves with an array of sensor objects.

- **Android Response**: Each sensor object includes the following properties:
    - `name`: String. Unique name of the sensor.
    - `fifoMaxEventCount`: Integer. Maximum number of events that can be batched.
    - `fifoReservedEventCount`: Integer. Number of events reserved for batching.
    - `highestDirectReportRateLevel`: Integer. Highest direct report rate level.
    - `id`: Integer. Unique sensor identifier.
    - `maxDelay`: Integer. Maximum delay in microseconds.
    - `maximumRange`: Float. Maximum range of the sensor.
    - `minDelay`: Integer. Minimum delay between events in microseconds.
    - `power`: Float. Power used by the sensor in mA.
    - `reportingMode`: Integer. Reporting mode of the sensor.
    - `resolution`: Float. Resolution of the sensor.
    - `stringType`: String. Type of the sensor as a string.
    - `type`: Integer. Generic type of the sensor.
    - `vendor`: String. Vendor of the sensor.
    - `version`: Integer. Version of the sensor module.
    - `isAdditionalInfoSupported`: Boolean. If additional information API is supported.
    - `isDynamicSensor`: Boolean. If the sensor is dynamic.
    - `isWakeUpSensor`: Boolean. If the sensor is a wake-up sensor.

- **iOS Response**: Each sensor object includes the following properties:
    - `name`: String. Name of the sensor (e.g., "Accelerometer", "Gyroscope").
    - `type`: String. Type of the sensor (e.g., "Accelerometer", "Gyroscope").

*Note: iOS API limitations mean only the sensor's name and type are available, unlike the comprehensive data available on Android.*

## License

This project is licensed under the MIT License.

---

This README provides a clear overview of the plugin's capabilities and the differences in data provided by the Android and iOS implementations. Feel free to modify or expand this template as needed for your project.

[w3c_spec]: https://www.w3.org/TR/battery-status/
[status_object]: #status-object
[community_plugins]: https://github.com/EYALIN?tab=repositories&q=community&type=&language=&sort=
