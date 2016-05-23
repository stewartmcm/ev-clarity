package com.example.android.predictev;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class OdometerIntentService extends IntentService {

    protected static final String TAG = "OdometerIntentService";

    /**
     * Adapter backed by a list of DetectedActivity objects.
     */
    private DetectedActivitiesAdapter mAdapter;

    /**
     * A receiver for DetectedActivity objects broadcast by the
     * {@code ActivityDetectionIntentService}.
     */
//    protected ActivityDetectionBroadcastReceiver mBroadcastReceiver;

    private static double distanceInMeters;
    private static Location lastLocation = null;
    private LocationManager locManager;
    private LocationListener listener;
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_FOO = "com.example.android.predictev.action.FOO";
    public static final String ACTION_BAZ = "com.example.android.predictev.action.BAZ";

    // TODO: Rename parameters
    public static final String EXTRA_PARAM1 = "com.example.android.predictev.extra.PARAM1";
    public static final String EXTRA_PARAM2 = "com.example.android.predictev.extra.PARAM2";

    public OdometerIntentService() {
        super("OdometerIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (lastLocation == null) {
                    lastLocation = location;
                }
                distanceInMeters += location.distanceTo(lastLocation);
                lastLocation = location;
            }

            @Override
            public void onStatusChanged(String arg0, int arg1, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String arg0) {

            }

            @Override
            public void onProviderDisabled(String arg0) {

            }
        };

        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, listener);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {

//            mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();

            final String action = intent.getAction();
            if (ACTION_FOO.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionFoo(param1, param2);
            } else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            }
        }
    }

//    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            ArrayList<DetectedActivity> updatedActivities =
//                    intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);
//
//            updateDetectedActivitiesList(updatedActivities);
//
//            for (DetectedActivity activity : updatedActivities) {
//                switch (activity.getType()) {
//
//                    case DetectedActivity.IN_VEHICLE: {
//                        Log.i("MainActivity", "On foot %: " + activity.getConfidence());
//                        if (activity.getConfidence() >= 75) {
//                            recordDrive();
//                        }
//                        break;
//
//                    }
//                    case DetectedActivity.WALKING: {
//                        Log.i("MainActivity", "Still %: " + activity.getConfidence());
//
//                        if (odometer.getMiles() != 0 && activity.getConfidence() >= 75) {
//                            Log.i(TAG, "onReceive: " + odometer.getMiles());
//                            logDrive();
//                        }
//                        break;
//                    }
//
//                }
//            }
//        }
//    }

    /**
     * Processes the list of freshly detected activities. Asks the adapter to update its list of
     * DetectedActivities with new {@code DetectedActivity} objects reflecting the latest detected
     * activities.
     */
    protected void updateDetectedActivitiesList(ArrayList<DetectedActivity> detectedActivities) {
        mAdapter.updateActivities(detectedActivities);

    }

    public double getMiles() {
        return this.distanceInMeters / 1609.344;
    }

    public double reset() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return this.distanceInMeters / 1609.344;
        }
        locManager.removeUpdates(listener);
        distanceInMeters = 0.0;
        return 0.0;
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
