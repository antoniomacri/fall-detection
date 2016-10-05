package it.unipi.ing.falldetection;

import android.os.Bundle;

public class UserInformationActivity extends AutoUpdatingPreferencesActivity
{
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.user_information);

        initialize();
    }
}
