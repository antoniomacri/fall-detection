package it.unipi.ing.falldetection;

import java.util.*;

import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.AttributeSet;

/**
 * A {@link Preference} that displays a list of contacts as a dialog.
 * <p>
 * This preference will store into the SharedPreferences a string composed by a list of values. To
 * unpack this list from the persisted string, use the {@link #unpack} method. This list will
 * contain one or more values from the array returned by {@link #getAllContacts}.
 */
public class ContactListPreference extends DialogPreference
{
    private static final String separator = "\u0001\u0007\u001D\u0007\u0001";

    /**
     * Represents a contact consisting of a title and a value (e.g., phone number, email address).
     */
    public static class Contact
    {
        public final String title;
        public final String value;

        public Contact(String title, String value) {
            this.title = title;
            this.value = value;
        }

        @Override
        public String toString() {
            return title + " (" + value + ")";
        }
    }

    private final Contact[] contacts;
    private List<String> values = new ArrayList<String>();
    private List<String> newValues = new ArrayList<String>();
    private boolean preferenceChanged;
    private boolean singleChoice = false;

    /**
     * Constructs a new instance of ContactListPreference.
     *
     * @param context
     *            The Context this is associated with, through which it can access the current
     *            theme, resources, {@link SharedPreferences}, etc.
     * @param attrs
     *            The attributes of the XML tag that is inflating the preference.
     *
     *            Currently, just the "singleChoice" attribute is supported, which specifies whether
     *            the user is allowed to choice only a single contact from the list or many of them.
     */
    public ContactListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ContactListPreference);
        for (int i = 0, count = a.getIndexCount(); i < count; ++i) {
            int attr = a.getIndex(i);
            switch (attr) {
            case R.styleable.ContactListPreference_singleChoice:
                singleChoice = a.getBoolean(attr, false);
                break;
            }
        }
        a.recycle();

        contacts = getContacts().toArray(new Contact[0]);
    }

    /**
     * Constructs a new instance of ContactListPreference.
     *
     * @param context
     *            The Context this is associated with, through which it can access the current
     *            theme, resources, {@link SharedPreferences}, etc.
     */
    public ContactListPreference(Context context) {
        super(context, null);

        contacts = getContacts().toArray(new Contact[0]);
    }

    /**
     * Gets the list of all contacts to be shown in the dialog.
     *
     * This list contains all contacts known to the system (for example, all contacts in the phone
     * book) plus all values retrieved from the SharedPreferences without a matching contact (for
     * example, all numbers specified in the preferences with no corresponding contact in the phone
     * book). With the latter values, a default title is used for the contacts.
     */
    public Contact[] getAllContacts() {
        List<Contact> result = Arrays.asList(contacts);
        for (String number : values) {
            boolean present = false;
            for (Contact c : result) {
                if (number.equals(c.value)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                String name = getContext().getResources().getString(R.string.unknown_number);
                result.add(new Contact(name, number));
            }
        }
        return result.toArray(new Contact[result.size()]);
    }

    /**
     * Gets the values (e.g., phone numbers or email addresses) of the contacts to be persisted.
     */
    public String[] getValues() {
        return values.toArray(new String[values.size()]);
    }

    /**
     * Sets the values of the contacts to be persisted.
     *
     * @param array
     *            The values of the contacts to be persisted. This array should contain values
     *            obtained using the {@link #getAllContacts()} method.
     */
    private void setValues(String[] array) {
        values = Arrays.asList(array);
        persistString(pack(values));
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder)
    {
        super.onPrepareDialogBuilder(builder);

        String[] items = new String[contacts.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = contacts[i].toString();
        }
        if (singleChoice) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String number = contacts[which].value;
                    if (newValues.size() != 1 || newValues.get(0) != number) {
                        newValues.clear();
                        newValues.add(number);
                        preferenceChanged = true;
                    }
                }
            };
            builder.setSingleChoiceItems(items, getSelectedItem(), listener);
        }
        else {
            boolean[] checkedItems = getSelectedItems();
            OnMultiChoiceClickListener listener = new OnMultiChoiceClickListener() {
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    if (isChecked) {
                        preferenceChanged |= newValues.add(contacts[which].value);
                    }
                    else {
                        preferenceChanged |= newValues.remove(contacts[which].value);
                    }
                }
            };
            builder.setMultiChoiceItems(items, checkedItems, listener);
        }
        newValues.clear();
        newValues.addAll(values);
    }

    private int getSelectedItem()
    {
        if (values.size() > 0) {
            final String value = values.get(0);
            for (int i = 0, count = contacts.length; i < count; i++) {
                if (value.equals(contacts[i].value))
                    return i;
            }
        }
        return -1;
    }

    private boolean[] getSelectedItems()
    {
        final int count = contacts.length;
        boolean[] result = new boolean[count];
        for (int i = 0; i < count; i++) {
            result[i] = values.contains(contacts[i].value);
        }
        return result;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        super.onDialogClosed(positiveResult);

        if (positiveResult && preferenceChanged) {
            if (callChangeListener(newValues)) {
                setValues(newValues.toArray(new String[newValues.size()]));
            }
        }
        preferenceChanged = false;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index)
    {
        final CharSequence[] defaultValues = a.getTextArray(index);
        final int count = defaultValues.length;
        final String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = defaultValues[i].toString();
        }
        return result;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
    {
        String[] vals = restoreValue ? unpack(getPersistedString(pack(values)))
                : (String[])defaultValue;
        if (singleChoice && vals.length > 1) {
            vals = new String[] { vals[0] };
        }
        setValues(vals);
    }

    /**
     * Gets all contacts from the phone book. For each contact, the title will contain the display
     * name, the value will contain the associated phone number.
     */
    protected List<Contact> getContacts()
    {
        List<Contact> contacts = new ArrayList<Contact>();
        ContentResolver cr = getContext().getContentResolver();
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
                String number = phones.getString(numberIndex);
                contacts.add(new Contact(displayName, PhoneNumberUtils.stripSeparators(number)));
            }
            phones.close();
        }
        return contacts;
    }

    public static String pack(List<String> array) {
        if (array == null || array.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.get(0));
        for (int i = 1; i < array.size(); i++) {
            sb.append(separator).append(array.get(i));
        }
        return sb.toString();
    }

    public static String[] unpack(String val) {
        if (val == null || "".equals(val)) {
            return new String[0];
        }
        else {
            return val.split(separator);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.values = getValues();
        return myState;
    }

    private static class SavedState extends BaseSavedState
    {
        String[] values;

        public SavedState(Parcel source) {
            super(source);
            values = unpack(source.readString());
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeStringArray(values);
        }
    }
}
