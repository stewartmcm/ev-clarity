package com.example.android.predictev.activities;

import android.content.Context;
import android.content.res.Resources;

import com.example.android.predictev.R;
import com.google.android.gms.location.DetectedActivity;

/**
 * Created by stewartmcmillan on 5/8/16.
 */
public final class Constants {

    private Constants() {
    }

    public static final String PACKAGE_NAME = "com.example.android.predictev";

    public static final String BROADCAST_ACTION = PACKAGE_NAME + ".BROADCAST_ACTION";

    public static final String ACTIVITY_EXTRA = PACKAGE_NAME + ".ACTIVITY_EXTRA";

    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES";

    public static final String ACTIVITY_UPDATES_REQUESTED_KEY = PACKAGE_NAME +
            ".ACTIVITY_UPDATES_REQUESTED";

    public static final String DETECTED_ACTIVITIES = PACKAGE_NAME + ".DETECTED_ACTIVITIES";

    /**
     * The desired time between activity detections. Larger values result in fewer activity
     * detections while improving battery life. A value of 0 results in activity detections at the
     * fastest possible rate. Getting frequent updates negatively impact battery life and a real
     * app may prefer to request less frequent updates.
     */
    public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 5000;

    /**
     * An arbitrary request code that is used to grant the app access to the user's location data.
     */
    public static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1928;

    public static final String EXTRA_USER_LAT = "USER_LAT";
    public static final String EXTRA_USER_LON = "USER_LON";

    public static final String KEY_SHARED_PREF_UTIL_NAME = "SHARED_PREF_UTIL_NAME";
    public static final String KEY_SHARED_PREF_UTIL_RATE = "SHARED_PREF_UTIL_RATE";
    public static final String KEY_SHARED_PREF_GAS_PRICE = "SHARED_PREF_GAS_PRICE";
    public static final String KEY_SHARED_PREF_CURRENT_MPG = "SHARED_PREF_CURRENT_MPG";
    public static final String KEY_SHARED_PREF_DRIVE_TRACKING = "KEY_SHARED_PREF_DRIVE_TRACKING";

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
        switch(detectedActivityType) {
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
