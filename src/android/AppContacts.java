package com.trackster.appcontacts;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

public class AppContacts extends CordovaPlugin {

    private static final int CONTACTS_REQUEST_CODE = 1;
    private static final String READ_CONTACTS_PERMISSION = android.Manifest.permission.READ_CONTACTS;
    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        
        if ("fetchContacts".equals(action)) {
            // Check for permission before fetching
            if (cordova.hasPermission(READ_CONTACTS_PERMISSION)) {
                fetchContacts();
            } else {
                cordova.requestPermissions(this, CONTACTS_REQUEST_CODE, new String[]{READ_CONTACTS_PERMISSION});
            }
            return true;
        } else if ("checkPermission".equals(action)) {
            // Simply check and return the permission status
            checkPermission();
            return true;
        } else if ("openSettings".equals(action)) {
            // Open the device settings for this app
            openSettings();
            return true;
        }
        return false;
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", cordova.getActivity().getPackageName(), null);
        intent.setData(uri);
        cordova.getActivity().startActivity(intent);
        this.callbackContext.success();
    }

    private void checkPermission() {
        if (cordova.hasPermission(READ_CONTACTS_PERMISSION)) {
            this.callbackContext.success("authorized");
        } else {
            this.callbackContext.success("denied");
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (requestCode == CONTACTS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, now fetch contacts
                fetchContacts();
            } else {
                // Permission denied
                sendContactsResult("denied", new JSONArray(), "Permission denied to access contacts.");
            }
        }
    }

    private void fetchContacts() {
        JSONArray contactsArray = new JSONArray();
        ContentResolver cr = this.cordova.getActivity().getContentResolver();

        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                do {
                    JSONObject contact = new JSONObject();
                    String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                    if (name != null) {
                        // Query for structured name (first and last name)
                        Cursor nameCursor = cr.query(
                            ContactsContract.Data.CONTENT_URI,
                            null,
                            ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                            new String[]{id, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
                            null
                        );

                        if (nameCursor != null && nameCursor.moveToFirst()) {
                            String firstName = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                            String lastName = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                            contact.put("firstName", firstName);
                            contact.put("lastName", lastName);
                            nameCursor.close();
                        } else {
                            contact.put("firstName", "");
                            contact.put("lastName", "");
                        }

                        // Fetch phone numbers
                        if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                            JSONArray phoneNumbers = new JSONArray();
                            Cursor pCur = cr.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id},
                                null
                            );
                            while (pCur.moveToNext()) {
                                JSONObject phoneNumber = new JSONObject();
                                String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                String phoneType = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                                phoneNumber.put("value", phoneNo);
                                phoneNumber.put("label", ContactsContract.CommonDataKinds.Phone.getTypeLabel(this.cordova.getActivity().getResources(), Integer.parseInt(phoneType), "").toString());
                                phoneNumbers.put(phoneNumber);
                            }
                            pCur.close();
                            contact.put("phoneNumbers", phoneNumbers);
                        }

                        // Fetch email addresses
                        JSONArray emailAddresses = new JSONArray();
                        Cursor emailCur = cr.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            new String[]{id},
                            null
                        );
                        while (emailCur.moveToNext()) {
                            JSONObject emailAddress = new JSONObject();
                            String email = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                            String emailType = emailCur.getString(emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
                            emailAddress.put("value", email);
                            emailAddress.put("label", ContactsContract.CommonDataKinds.Email.getTypeLabel(this.cordova.getActivity().getResources(), Integer.parseInt(emailType), "").toString());
                            emailAddresses.put(emailAddress);
                        }
                        emailCur.close();
                        contact.put("emailAddresses", emailAddresses);

                        contactsArray.put(contact);
                    }
                } while (cursor.moveToNext());

                sendContactsResult("granted", contactsArray, null);

            } catch (Exception e) {
                sendContactsResult("error", new JSONArray(), "An error occurred while fetching contacts: " + e.getMessage());
            } finally {
                cursor.close();
            }
        } else {
            sendContactsResult("granted", contactsArray, null);
        }
    }

    private void sendContactsResult(String status, JSONArray contacts, String message) {
        try {
            JSONObject result = new JSONObject();
            result.put("status", status);
            result.put("contacts", contacts);
            if (message != null) {
                result.put("message", message);
            }

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
            callbackContext.sendPluginResult(pluginResult);
        } catch (JSONException e) {
            callbackContext.error(e.getMessage());
        }
    }
}