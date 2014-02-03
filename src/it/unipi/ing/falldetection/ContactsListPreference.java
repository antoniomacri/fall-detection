package it.unipi.ing.falldetection;

import java.util.*;

import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.AttributeSet;
import android.util.Pair;

/**
 * A {@link Preference} that displays a list of entries as a dialog.
 * <p>
 * This preference will store a set of strings into the SharedPreferences. This set will contain one
 * or more values from the {@link #setEntryValues(CharSequence[])} array.
 * 
 * @attr ref android.R.styleable#MultiSelectListPreference_entries
 * @attr ref android.R.styleable#MultiSelectListPreference_entryValues
 */
public class ContactsListPreference extends DialogPreference
{
    private static final String separator = "\u0001\u0007\u001D\u0007\u0001";

    private String[] mEntries;
    private String[] mEntryValues;
    private Set<String> mValues = new HashSet<String>();
    private Set<String> mNewValues = new HashSet<String>();
    private boolean mPreferenceChanged;
    private boolean singleChoice = false;

    public ContactsListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = getContext().obtainStyledAttributes(attrs,
                R.styleable.ContactsListPreference);
        for (int i = 0, count = a.getIndexCount(); i < count; ++i) {
            int attr = a.getIndex(i);
            switch (attr) {
            case R.styleable.ContactsListPreference_singleChoice:
                singleChoice = a.getBoolean(attr, false);
                break;
            }
        }
        a.recycle();
    }

    public ContactsListPreference(Context context) {
        this(context, null);
    }

    /**
     * The list of entries to be shown in the list in subsequent dialogs.
     * 
     * @return The list as an array.
     */
    public String[] getEntries() {
        return mEntries;
    }

    /**
     * Returns the array of values to be saved for the preference.
     * 
     * @return The array of values.
     */
    public String[] getEntryValues() {
        return mEntryValues;
    }

    /**
     * Retrieves the current value of the key.
     */
    public Set<String> getValues() {
        return mValues;
    }

    /**
     * Sets the value of the key. This should contain entries in {@link #getEntryValues()}.
     * 
     * @param values
     *            The values to set for the key.
     */
    private void setValues(Set<String> values) {
        mValues.clear();
        mValues.addAll(values);
        persistString(pack(values));
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder)
    {
        super.onPrepareDialogBuilder(builder);

        setupContacts();

        String[] titles = new String[mEntries.length];
        for (int i = 0; i < titles.length; i++) {
            titles[i] = mEntries[i] + " (" + mEntryValues[i] + ")";
        }
        if (singleChoice) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String value = mEntryValues[which];
                    if (mNewValues.size() != 1 || mNewValues.iterator().next() != value) {
                        mNewValues.clear();
                        mNewValues.add(value);
                        mPreferenceChanged = true;
                    }
                }
            };
            builder.setSingleChoiceItems(titles, getSelectedItem(), listener);
        }
        else {
            boolean[] checkedItems = getSelectedItems();
            OnMultiChoiceClickListener listener = new OnMultiChoiceClickListener() {
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    if (isChecked) {
                        mPreferenceChanged |= mNewValues.add(mEntryValues[which]);
                    }
                    else {
                        mPreferenceChanged |= mNewValues.remove(mEntryValues[which]);
                    }
                }
            };
            builder.setMultiChoiceItems(titles, checkedItems, listener);
        }
        mNewValues.clear();
        mNewValues.addAll(mValues);
    }

    private int getSelectedItem()
    {
        if (!mValues.isEmpty()) {
            final String value = mValues.iterator().next();
            for (int i = 0, count = mEntryValues.length; i < count; i++)
                if (value.equals(mEntryValues[i]))
                    return i;
        }
        return -1;
    }

    private boolean[] getSelectedItems()
    {
        final int count = mEntryValues.length;
        boolean[] result = new boolean[count];
        for (int i = 0; i < count; i++) {
            result[i] = mValues.contains(mEntryValues[i]);
        }
        return result;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        super.onDialogClosed(positiveResult);

        if (positiveResult && mPreferenceChanged) {
            final Set<String> values = mNewValues;
            if (callChangeListener(values)) {
                setValues(values);
            }
        }
        mPreferenceChanged = false;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index)
    {
        final CharSequence[] defaultValues = a.getTextArray(index);
        final int valueCount = defaultValues.length;
        final Set<String> result = new HashSet<String>();
        for (int i = 0; i < valueCount; i++) {
            result.add(defaultValues[i].toString());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
    {
        Set<String> values = restoreValue ? unpack(getPersistedString(pack(mValues)))
                : (Set<String>)defaultValue;
        if (singleChoice && values.size() > 1) {
            String first = values.iterator().next();
            values.clear();
            values.add(first);
        }
        setValues(values);
    }

    protected static String pack(Set<String> set) {
        return join(set, separator);
    }

    protected static Set<String> unpack(String val) {
        if (val == null || "".equals(val)) {
            return new HashSet<String>();
        }
        else {
            Set<String> set = new HashSet<String>();
            set.addAll(Arrays.asList(val.split(separator)));
            return set;
        }
    }

    protected static String join(Iterable<?> iterable, String separator) {
        Iterator<?> oIter;
        if (iterable == null || (!(oIter = iterable.iterator()).hasNext()))
            return "";
        StringBuilder oBuilder = new StringBuilder(String.valueOf(oIter.next()));
        while (oIter.hasNext())
            oBuilder.append(separator).append(oIter.next());
        return oBuilder.toString();
    }

    private void setupContacts()
    {
        if (mEntries == null) {
            Pair<List<String>, List<String>> list = getContacts();
            for (String value : mValues) {
                if (!list.second.contains(value)) {
                    list.first.add(getContext().getResources().getString(R.string.unknown_number));
                    list.second.add(value);
                }
            }
            mEntries = list.first.toArray(new String[list.first.size()]);
            mEntryValues = list.second.toArray(new String[list.second.size()]);
        }
    }

    protected Pair<List<String>, List<String>> getContacts()
    {
        List<String> displayNames = new ArrayList<String>();
        List<String> numbers = new ArrayList<String>();
        Pair<List<String>, List<String>> list;
        list = new Pair<List<String>, List<String>>(displayNames, numbers);

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
                displayNames.add(displayName);
                numbers.add(PhoneNumberUtils.stripSeparators(number));
            }
            phones.close();
        }
        return list;
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
        Set<String> values;

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
            dest.writeStringArray(values.toArray(new String[0]));
        }
    }
}
