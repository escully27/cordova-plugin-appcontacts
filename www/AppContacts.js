var exec = require('cordova/exec');

var AppContacts = {
    fetchContacts: function (successCallback, errorCallback) {
        // successCallback: function that receives the list of contacts on success
        // errorCallback: function that receives the error message on failure
        exec(successCallback, errorCallback, 'AppContacts', 'fetchContacts', []);
    },
    checkPermission: function (successCallback, errorCallback) {
        // successCallback: function that receives the permission status string
        // errorCallback: function that receives the error message on failure
        exec(successCallback, errorCallback, 'AppContacts', 'checkPermission', []);
    },
    openSettings: function (successCallback, errorCallback) {
        // successCallback: called when the settings page is opened
        // errorCallback: called on failure (e.g., if the feature isn't supported)
        exec(successCallback, errorCallback, 'AppContacts', 'openSettings', []);
    }
};

module.exports = AppContacts;