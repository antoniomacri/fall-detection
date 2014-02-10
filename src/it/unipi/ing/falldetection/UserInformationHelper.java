package it.unipi.ing.falldetection;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public final class UserInformationHelper
{
    public static String getUserSex(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("user_sex", "");
    }

    /**
     * Gets the user's age.
     *
     * @return The age of the user expressed in years or 0.0f if no age was stored.
     */
    public static float getUserAge(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = sharedPreferences.getString("user_age", "").trim();
        if (!TextUtils.isEmpty(value)) {
            return Float.parseFloat(value);
        }
        return 0.0f;
    }

    /**
     * Gets the user's height.
     *
     * @return The height of the user expressed in centimeters or 0.0f if no height was stored.
     */
    public static float getUserHeight(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = sharedPreferences.getString("user_height", "").trim();
        if (!TextUtils.isEmpty(value)) {
            return Float.parseFloat(value);
        }
        return 0.0f;
    }

    /**
     * Gets the user's weight.
     *
     * @return The weight of the user expressed in kilograms or 0.0f if no weight was stored.
     */
    public static float getUserWeight(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = sharedPreferences.getString("user_weight", "").trim();
        if (!TextUtils.isEmpty(value)) {
            return Float.parseFloat(value);
        }
        return 0.0f;
    }

    private UserInformationHelper() {
    }
}
