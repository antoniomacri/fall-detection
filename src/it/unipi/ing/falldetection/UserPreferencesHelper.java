package it.unipi.ing.falldetection;

import it.unipi.ing.falldetection.ContactListPreference.Contact;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

public final class UserPreferencesHelper
{
    public static String getUserSex(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("user_sex", "");
    }

    public static float getUserAge(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getFloat("user_age", 0.0f);
    }

    public static float getUserHeight(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getFloat("user_height", 0.0f);
    }

    public static float getUserWeight(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getFloat("user_weight", 0.0f);
    }

    public static boolean isVibrateEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("alerts_vibrate", false);
    }

    public static String getAlarmSound(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("alerts_alarm_sound", "");
    }

    public static int getAlertTimeout(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt("alerts_interval", 0);
    }

    public static boolean isSmsAlertEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("alerts_send_sms", false);
    }

    public static boolean isPhoneCallAlertEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("alerts_make_call", false);
    }

    public static String getSmsContent(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultValue = context.getString(R.string.sms_content_default);
        return sharedPreferences.getString("sms_content", defaultValue);
    }

    public static boolean isAddLocationEnabled(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("sms_add_location", false);
    }

    public static Contact[] getSmsRecipients(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String persistedString = sharedPreferences.getString("sms_recipients", "");
        String[] values = ContactListPreference.unpack(persistedString);
        List<Contact> contacts = new ArrayList<Contact>(values.length);
        for (String value : values) {
            String title = UserPreferencesHelper.getContactNameFromNumber(context, value);
            if (!TextUtils.isEmpty(title)) {
                contacts.add(new Contact(title, value));
            }
            else {
                contacts.add(new Contact(context.getString(R.string.unknown_number), value));
            }
        }
        return contacts.toArray(new Contact[contacts.size()]);
    }

    public static Contact getPhoneCallRecipient(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String number = sharedPreferences.getString("phone_call_recipient", "");
        if (!TextUtils.isEmpty(number)) {
            String name = UserPreferencesHelper.getContactNameFromNumber(context, number);
            if (!TextUtils.isEmpty(name)) {
                return new Contact(name, number);
            }
            else {
                return new Contact(context.getString(R.string.unknown_number), number);
            }
        }
        return null;
    }

    private static String getContactNameFromNumber(Context context, String number)
    {
        ContentResolver cr = context.getContentResolver();
        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, sortOrder);
        while (cursor.moveToNext()) {
            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            int hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
            if (Integer.parseInt(cursor.getString(hasPhoneIndex)) <= 0) {
                continue;
            }
            Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null,
                    null);
            int displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            String displayName = cursor.getString(displayNameIndex);
            while (phones.moveToNext()) {
                int numberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                if (number.equals(PhoneNumberUtils.stripSeparators(phones.getString(numberIndex))))
                    return displayName;
            }
            phones.close();
        }
        return null;
    }

    private UserPreferencesHelper() {
    }
}
