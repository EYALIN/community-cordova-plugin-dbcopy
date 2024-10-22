I dedicate a considerable amount of my free time to developing and maintaining many Cordova plugins for the community ([See the list with all my maintained plugins][community_plugins]).
To help ensure this plugin is kept updated, new features are added, and bugfixes are implemented quickly, please donate a couple of dollars (or a little more if you can stretch) as this will help me to afford to dedicate time to its maintenance.
Please consider donating if you're using this plugin in an app that makes you money, or if you're asking for new features or priority bug fixes. Thank you!

[![](https://img.shields.io/static/v1?label=Sponsor%20Me&style=for-the-badge&message=%E2%9D%A4&logo=GitHub&color=%23fe8e86)](https://github.com/sponsors/eyalin)

[community_plugins]: https://github.com/EYALIN?tab=repositories&q=community&type=&language=&sort=

# Db Copy Cordova Plugin

## Description
The Db Copy plugin allows you to copy a database file from a base64-encoded source to your application's storage and vice versa. It supports both Android and iOS platforms and provides functionality to facilitate the transfer of SQLite databases for Cordova applications.

## Features
- Copy database from a base64-encoded source to the app's storage.
- Copy database from the app's storage to external storage.
- Supports Android and iOS platforms.
- Configurable options for overwriting existing files and deleting old databases.

## Installation
To install the plugin, use the following Cordova command:
```sh
cordova plugin add community-cordova-plugin-dbcopy
```

## Usage
### JavaScript Interface
The plugin provides two main functions that can be called from your JavaScript code.

#### `copyDbFromStorage(options, successCallback, errorCallback)`
Copies a database from a base64-encoded source to the application's storage.

- **Options**
  - `dbName` (string): The name of the database to copy.
  - `base64Source` (string): The base64-encoded database source.
  - `location` (string, optional): The location to store the database (`default`, `documents`, or `external`). Default is `default`.
  - `deleteOldDb` (boolean, optional): Whether to delete the existing database with the same name. Default is `false`.

- **Example**
```javascript
DbCopy.copyDbFromStorage({
  dbName: 'myDatabase.db',
  base64Source: '<base64_encoded_string>',
  location: 'documents',
  deleteOldDb: true
}, function() {
  console.log('Database copied successfully.');
}, function(error) {
  console.error('Error copying database: ', error);
});
```

#### `copyDbToStorage(options, successCallback, errorCallback)`
Copies a database from the application's storage to an external storage path.

- **Options**
  - `fileName` (string): The name of the database to copy.
  - `fullPath` (string): The full path where the database will be copied.
  - `overwrite` (boolean, optional): Whether to overwrite the existing file. Default is `false`.

- **Example**
```javascript
DbCopy.copyDbToStorage({
  fileName: 'myDatabase.db',
  fullPath: 'file:///storage/emulated/0/',
  overwrite: true
}, function() {
  console.log('Database copied to storage successfully.');
}, function(error) {
  console.error('Error copying database to storage: ', error);
});
```

## Supported Platforms
- Android
- iOS

## API Reference
### Methods
- **`copyDbFromStorage(options, successCallback, errorCallback)`**
- **`copyDbToStorage(options, successCallback, errorCallback)`**

## License
This plugin is open-sourced under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for more information.

## Contributing
Contributions are welcome! Please create an issue or submit a pull request to contribute.

## Issues
If you find any issues or have suggestions for new features, please report them on the [GitHub Issues page](https://github.com/EYALIN/community-cordova-plugin-dbcopy/issues).

## Donations
If you find this plugin useful, consider making a donation to support ongoing development and maintenance. Your support is greatly appreciated!

