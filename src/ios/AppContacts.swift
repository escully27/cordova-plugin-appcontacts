import Foundation
import Contacts
import ContactsUI

@objc(AppContacts)
class AppContacts: CDVPlugin {

    @objc(checkPermission:)
    func checkPermission(command: CDVInvokedUrlCommand) {
        let status = CNContactStore.authorizationStatus(for: .contacts)
        var statusString = "denied"
        
        switch status {
        case .authorized:
            statusString = "granted"
        case .denied:
            statusString = "denied"
        case .notDetermined:
            statusString = ""
        case .restricted:
            statusString = "denied"
        @unknown default:
            statusString = ""
        }
        
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: statusString)
        self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(openSettings:)
    func openSettings(command: CDVInvokedUrlCommand) {
        guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Could not create settings URL.")
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
            return
        }

        if UIApplication.shared.canOpenURL(settingsUrl) {
            UIApplication.shared.open(settingsUrl, options: [:]) { (success) in
                if success {
                    let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                    self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
                } else {
                    let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Failed to open settings.")
                    self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
                }
            }
        } else {
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Cannot open settings URL.")
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        }
    }
    
    
    // This is the entry point for the plugin call
    @objc(fetchContacts:)
    func fetchContacts(command: CDVInvokedUrlCommand) {
        let store = CNContactStore()
        
        // Request authorization from the user
        store.requestAccess(for: .contacts) { (granted, error) in
            guard granted else {
                // Handle denied permission
                let payload: [String: Any] = [
                    "status": "denied",
                    "contacts": [],
                    "message": error?.localizedDescription ?? "Permission denied to access contacts."
                ]
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: payload)
                self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
                return
            }
            
            // Permission granted, now fetch contacts
            DispatchQueue.global(qos: .userInitiated).async {
                do {
                    // Define the keys you want to fetch
                    let keysToFetch = [
                        CNContactGivenNameKey as CNKeyDescriptor,
                        CNContactFamilyNameKey as CNKeyDescriptor,
                        CNContactPhoneNumbersKey as CNKeyDescriptor,
                        CNContactEmailAddressesKey as CNKeyDescriptor
                    ]
                    
                    let allContainers = try store.containers(matching: nil)
                    var allContacts: [[String: Any]] = []
                    
                    for container in allContainers {
                        let predicate = CNContact.predicateForContactsInContainer(withIdentifier: container.identifier)
                        let contacts = try store.unifiedContacts(matching: predicate, keysToFetch: keysToFetch)
                        
                        for contact in contacts {
                            var contactDict: [String: Any] = [:]
                            
                            // Name
                            contactDict["firstName"] = contact.givenName
                            contactDict["lastName"] = contact.familyName
                            
                            // Phone Numbers
                            var phoneNumbers: [[String: String]] = []
                            for phoneNumber in contact.phoneNumbers {
                                phoneNumbers.append([
                                    "value": phoneNumber.value.stringValue,
                                    "label": CNLabeledValue<NSString>.localizedString(forLabel: phoneNumber.label ?? "")
                                ])
                            }
                            contactDict["phoneNumbers"] = phoneNumbers
                            
                            // Email Addresses
                            var emailAddresses: [[String: String]] = []
                            for email in contact.emailAddresses {
                                emailAddresses.append([
                                    "value": email.value as String,
                                    "label": CNLabeledValue<NSString>.localizedString(forLabel: email.label ?? "")
                                ])
                            }
                            contactDict["emailAddresses"] = emailAddresses
                            
                            allContacts.append(contactDict)
                        }
                    }
                    
                    // Return the contacts as a JSON array
                    let payload: [String: Any] = [
                        "status": "granted",
                        "contacts": allContacts
                    ]
                    let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: payload)
                    self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
                    
                } catch {
                    let payload: [String: Any] = [
                        "status": "error",
                        "contacts": [],
                        "message": error.localizedDescription
                    ]
                    let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: payload)
                    self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
                }
            }
        }
    }
}