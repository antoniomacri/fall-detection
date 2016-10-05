package it.unipi.ing.falldetection;

import android.os.Bundle;

public class UserPreferencesActivity extends AutoUpdatingPreferencesActivity
{
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.user_preferences);

        initialize();
    }
}
