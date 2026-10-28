var exec = require('cordova/exec');

function execWithPromise(action, args, successCallback, errorCallback) {
    var promise = new Promise(function(resolve, reject) {
        exec(function(result) {
            if (typeof successCallback === 'function') {
                successCallback(result);
            }
            resolve(result);
        }, function(error) {
            if (typeof errorCallback === 'function') {
                errorCallback(error);
            }
            reject(error);
        }, 'AppContacts', action, args || []);
    });

    if (typeof errorCallback === 'function') {
        promise.catch(function() {});
    }

    return promise;
}

var AppContacts = {
    fetchContacts: function (successCallback, errorCallback) {
        // successCallback: function that receives the list of contacts on success
        // errorCallback: function that receives the error message on failure
        return execWithPromise('fetchContacts', [], successCallback, errorCallback);
    },
    checkPermission: function (successCallback, errorCallback) {
        // successCallback: function that receives the permission status string
        // errorCallback: function that receives the error message on failure
        return execWithPromise('checkPermission', [], successCallback, errorCallback);
    },
    openSettings: function (successCallback, errorCallback) {
        // successCallback: called when the settings page is opened
        // errorCallback: called on failure (e.g., if the feature isn't supported)
        return execWithPromise('openSettings', [], successCallback, errorCallback);
    }
};

module.exports = AppContacts;