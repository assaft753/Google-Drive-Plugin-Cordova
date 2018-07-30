function GoogleDrive() { }

GoogleDrive.prototype.signIn = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "signIn");
}

GoogleDrive.prototype.signOut = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "signOut");
}

GoogleDrive.prototype.pickFolder = function (successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "pickFolder");
}

GoogleDrive.prototype.uploadFileWithPicker = function (folderId, title, description, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "uploadFileWithPicker", [folderId, title, description]);
}

GoogleDrive.prototype.uploadFile = function (drive, file_details, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, "GoogleDrive", "uploadFile", [drive, file_details]);
}

GoogleDrive.install = function () {
    if (!window.plugins) {
        window.plugins = {};
    }

    window.plugins.gdrive = new GoogleDrive();
    return window.plugins.gdrive;
};

cordova.addConstructor(GoogleDrive.install);