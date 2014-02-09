package it.unipi.ing.falldetection;

import android.content.Context;
import android.content.SharedPreferences;

public final class StatisticsHelper
{
    private static final String PREFERENCES_STATISTICS = "Stats";
    private static final Object lock = new Object();

    public static int getFallDetectedCount(Context context) {
        synchronized (lock) {
            SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_STATISTICS, 0);
            return preferences.getInt("stats_falls_detected", 0);
        }
    }

    public static int getFallConfirmedCount(Context context) {
        synchronized (lock) {
            SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_STATISTICS, 0);
            return preferences.getInt("stats_falls_confirmed", 0);
        }
    }

    public static int stepFallDetectedCount(Context context) {
        synchronized (lock) {
            SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_STATISTICS, 0);
            int stats_falls_detected = preferences.getInt("stats_falls_detected", 0);
            stats_falls_detected++;
            preferences.edit().putInt("stats_falls_detected", stats_falls_detected).commit();
            return stats_falls_detected;
        }
    }

    public static int stepFallConfirmedCount(Context context) {
        synchronized (lock) {
            SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_STATISTICS, 0);
            int stats_falls_confirmed = preferences.getInt("stats_falls_confirmed", 0);
            stats_falls_confirmed++;
            preferences.edit().putInt("stats_falls_confirmed", stats_falls_confirmed).commit();
            return stats_falls_confirmed;
        }
    }

    private StatisticsHelper() {
    }
}
