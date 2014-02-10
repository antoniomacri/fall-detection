package it.unipi.ing.falldetection;

import java.util.*;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.*;
import android.text.TextUtils;

public class AutoUpdatingPreferencesActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener
{
    @SuppressWarnings("deprecation")
    protected void initialize()
    {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        SharedPreferences sharedPreferences = preferenceScreen.getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        for (int i = 0, count = preferenceScreen.getPreferenceCount(); i < count; i++) {
            updateSummary(preferenceScreen.getPreference(i));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummary(findPreference(key));
    }

    protected void updateSummary(Preference pref)
    {
        if (pref instanceof PreferenceCategory) {
            PreferenceCategory pCat = (PreferenceCategory)pref;
            for (int i = 0, count = pCat.getPreferenceCount(); i < count; i++) {
                updateSummary(pCat.getPreference(i));
            }
        }
        else if (pref instanceof EditTextPreference) {
            updateSummary((EditTextPreference)pref);
        }
        else if (pref instanceof RingtonePreference) {
            updateSummary((RingtonePreference)pref);
        }
        else if (pref instanceof ListPreference) {
            updateSummary((ListPreference)pref);
        }
        else if (pref instanceof ContactListPreference) {
            updateSummary((ContactListPreference)pref);
        }
    }

    protected void updateSummary(EditTextPreference preference)
    {
        setSummaryOrSummaryEmpty(preference, preference.getText());
    }

    protected void updateSummary(RingtonePreference preference)
    {
        String key = preference.getKey();
        String value = preference.getSharedPreferences().getString(key, "");
        if (!TextUtils.isEmpty(value)) {
            Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(value));
            value = ringtone.getTitle(this);
        }
        setSummaryOrSummaryEmpty(preference, value);
    }

    protected void updateSummary(ListPreference preference)
    {
        if (preference.getEntry() == null) {
            preference.setSummary(getResources().getString(R.string.not_set));
        }
        else {
            preference.setSummary(preference.getEntry());
        }
    }

    protected void updateSummary(ContactListPreference preference)
    {
        String[] values = preference.getValues();
        if (values.length == 0) {
            preference.setSummary(R.string.no_recipient_specified);
        }
        else {
            List<String> contactTitles = new ArrayList<String>();
            List<String> contactValues = new ArrayList<String>();
            ContactListPreference.Contact[] contacts = preference.getAllContacts();
            for (String value : values) {
                int pos = 0;
                while (pos < contacts.length && !contacts[pos].value.equals(value)) {
                    pos++;
                    continue;
                }
                if (pos < contacts.length) {
                    contactTitles.add(contacts[pos].title);
                    contactValues.add(contacts[pos].value);
                }
                else {
                    contactTitles.add(getResources().getString(R.string.unknown_number));
                    contactValues.add(value);
                }
            }
            if (values.length == 1) {
                preference.setSummary(contactTitles.get(0) + " (" + contactValues.get(0) + ")");
            }
            else {
                preference.setSummary(TextUtils.join(", ", contactTitles));
            }
        }
    }

    private void setSummaryOrSummaryEmpty(Preference pref, String value)
    {
        if (value == null || value.matches("^\\s*$")) {
            String name = "@string/" + pref.getKey() + "_summary_empty";
            int id = getResources().getIdentifier(name, null, this.getPackageName());
            if (id != 0) {
                pref.setSummary(getResources().getString(id));
            }
            else {
                pref.setSummary(getResources().getString(R.string.not_set));
            }
        }
        else {
            String name = "@string/" + pref.getKey() + "_summary";
            int id = getResources().getIdentifier(name, null, this.getPackageName());
            if (id != 0) {
                pref.setSummary(getResources().getString(id, value));
            }
        }
    }
}
