package com.stewartmcm.evclarity;

import android.content.Context;
import android.content.res.Resources;

import com.google.android.gms.location.DetectedActivity;

public final class Constants {

    private Constants() {
    }

    public static final String PACKAGE_NAME = "com.example.evclarity";

    public static final String BROADCAST_ACTION = PACKAGE_NAME + ".BROADCAST_ACTION";

    public static final String ACTIVITY_EXTRA = PACKAGE_NAME + ".ACTIVITY_EXTRA";

    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES";

    public static final String ACTIVITY_UPDATES_REQUESTED_KEY = PACKAGE_NAME +
            ".ACTIVITY_UPDATES_REQUESTED";

    public static final String DETECTED_ACTIVITIES = PACKAGE_NAME + ".DETECTED_ACTIVITIES";

    public static final String MAIN_ACTIVITY_TAG = "MAIN_ACTIVITY";

    public static final String ENERGY_SETTINGS_ACTIVITY_TAG = "ENERGYSETTINGS_ACTIVITY";

    public static final String ACTIVITY_UPDATES_INTENT_SERVICE_TAG = "ACTIVITY_UPDATES_SERVICE";
    public static final String LOCATION_UPDATES_INTENT_SERVICE_TAG = "LOCATION_UPDATES_SERVICE";

    public static final String ODOMETER_SERVICE_TAG = "ODOMETER_SERVICE";

    public static final String TRIP_TABLE_NAME = "trip";

    public static final long ACTIVITY_UPDATES_INTERVAL_IN_MILLISECONDS = 10000;

    public static final int PERMISSIONS_RESULT_REQUEST = 0;
    public static final int TRACKING_SWITCH_PERMISSIONS_RESULT_REQUEST = 1;

    public static final int DRIVE_TRACKING_FOREGROUND_SERVICE_REQUEST = 0;
    public static final int LOCATION_UPDATES_FOREGROUND_SERVICE_REQUEST = 1;

    public static final String EXTRA_USER_LAT = "USER_LAT";
    public static final String EXTRA_USER_LON = "USER_LON";

    public static final String KEY_SHARED_PREF_UTIL_NAME = "SHARED_PREF_UTIL_NAME";
    public static final String KEY_SHARED_PREF_UTIL_RATE = "SHARED_PREF_UTIL_RATE";
    public static final String KEY_SHARED_PREF_GAS_PRICE = "SHARED_PREF_GAS_PRICE";
    public static final String KEY_SHARED_PREF_CURRENT_MPG = "SHARED_PREF_CURRENT_MPG";
    public static final String KEY_SHARED_PREF_IS_DRIVING = "SHARED_PREF_DRIVING_BOOL";
    public static final String KEY_SHARED_PREF_DRIVE_TRACKING = "KEY_SHARED_PREF_DRIVE_TRACKING";
    public static final String KEY_SHARED_PREF_GPS_STATE = "KEY_SHARED_PREF_GPS_STATE";

    public static final String KEY_SHARED_PREF_TRIP_DISTANCE = "SHARED_PREF_TRIP_DISTANCE";
    public static final String KEY_SHARED_PREF_LAST_LATITUDE = "SHARED_PREF_LAST_LATITUDE";
    public static final String KEY_SHARED_PREF_LAST_LONGITUDE = "SHARED_PREF_LAST_LONGITUDE";
    public static final String KEY_SHARED_PREF_NEW_LATITUDE = "SHARED_PREF_NEW_LATITUDE";
    public static final String KEY_SHARED_PREF_NEW_LONGITUDE = "SHARED_PREF_NEW_LONGITUDE";

    public static final String KEY_SHARED_PREF_ASKED_FOR_PERMISSION = "SHARED_PREF_ASKED_FOR_PERMISSION";

    public static final String TRIP_SUMMARY_NOTIFICATION_CHANNEL_ID = "TRIP_SUMMARY";

    /**
     * List of DetectedActivity types that we monitor in this sample.
     */
    public static final int[] MONITORED_ACTIVITIES = {
            DetectedActivity.STILL,
            DetectedActivity.ON_FOOT,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.TILTING,
            DetectedActivity.UNKNOWN
    };

    /**
     * Returns a human readable String corresponding to a detected activity type.
     */
    public static String getActivityString(Context context, int detectedActivityType) {
        Resources resources = context.getResources();
        switch (detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.in_vehicle);
            case DetectedActivity.ON_BICYCLE:
                return resources.getString(R.string.on_bicycle);
            case DetectedActivity.ON_FOOT:
                return resources.getString(R.string.on_foot);
            case DetectedActivity.RUNNING:
                return resources.getString(R.string.running);
            case DetectedActivity.STILL:
                return resources.getString(R.string.still);
            case DetectedActivity.TILTING:
                return resources.getString(R.string.tilting);
            case DetectedActivity.UNKNOWN:
                return resources.getString(R.string.unknown);
            case DetectedActivity.WALKING:
                return resources.getString(R.string.walking);
            default:
                return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
        }
    }
}
