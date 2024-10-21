var PLUGIN_NAME = 'DbCopyPlugin';

var DbCopyPlugin = {
    copyDbFromStorage: function(options) {
        return new Promise(function (resolve, reject) {
            cordova.exec(resolve, reject, PLUGIN_NAME, 'copyDbFromStorage', [options]);
        });
    },
    copyDbToStorage: function(options) {
        return new Promise(function (resolve, reject) {
            cordova.exec(resolve, reject, PLUGIN_NAME, 'copyDbToStorage', [options]);
        });
    },
};

module.exports = DbCopyPlugin;
