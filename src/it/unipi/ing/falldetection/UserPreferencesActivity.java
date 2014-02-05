package it.unipi.ing.falldetection;

import java.util.*;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.*;
import android.text.TextUtils;

public class UserPreferencesActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener
{
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);

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

    private void updateSummary(Preference pref)
    {
        if (pref instanceof PreferenceCategory) {
            PreferenceCategory pCat = (PreferenceCategory)pref;
            for (int i = 0, count = pCat.getPreferenceCount(); i < count; i++) {
                updateSummary(pCat.getPreference(i));
            }
        }
        else if (pref instanceof EditTextPreference) {
            EditTextPreference edit = (EditTextPreference)pref;
            setSummaryOrSummaryEmpty(pref, edit.getText());
        }
        else if (pref instanceof RingtonePreference) {
            RingtonePreference ringtonePref = (RingtonePreference)pref;
            String key = ringtonePref.getKey();
            String value = ringtonePref.getSharedPreferences().getString(key, "");
            if (!TextUtils.isEmpty(value)) {
                Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(value));
                value = ringtone.getTitle(this);
            }
            setSummaryOrSummaryEmpty(pref, value);

        }
        else if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference)pref;
            if (listPref.getEntry() == null) {
                pref.setSummary(getResources().getString(R.string.not_set));
            }
            else {
                pref.setSummary(listPref.getEntry());
            }
        }
        else if (pref instanceof ContactListPreference) {
            ContactListPreference clist = (ContactListPreference)pref;
            String[] values = clist.getValues();
            if (values.length == 0) {
                pref.setSummary(R.string.no_recipient_specified);
            }
            else {
                List<String> contactTitles = new ArrayList<String>();
                List<String> contactValues = new ArrayList<String>();
                ContactListPreference.Contact[] contacts = clist.getAllContacts();
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
                    pref.setSummary(contactTitles.get(0) + " (" + contactValues.get(0) + ")");
                }
                else {
                    pref.setSummary(TextUtils.join(", ", contactTitles));
                }
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
