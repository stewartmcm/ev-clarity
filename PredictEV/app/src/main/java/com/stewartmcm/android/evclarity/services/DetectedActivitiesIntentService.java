package com.stewartmcm.android.evclarity.services;

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;
import com.stewartmcm.android.evclarity.LogTripTask;
import com.stewartmcm.android.evclarity.R;
import com.stewartmcm.android.evclarity.activities.Constants;
import com.stewartmcm.android.evclarity.activities.MainActivity;

import java.text.DecimalFormat;
import java.util.List;

public class DetectedActivitiesIntentService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
    protected static final String TAG = Constants.DETECTED_ACTIVITIES_INTENT_SERVICE_TAG;

    private GoogleApiClient mGoogleApiClient;
    private OdometerService odometer;
    private Intent odometerIntent;
    private boolean bound = false;
    private boolean driving = false;
    private double finalTripDistance;
    public double tripDistance;

    public DetectedActivitiesIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        mGoogleApiClient.connect();
    }

    protected synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(ActivityRecognition.API)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent: method ran");
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getProbableActivities());
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        Log.i(TAG, "handleDetectedActivities: method ran");

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        for (DetectedActivity activity : probableActivities) {
            switch (activity.getType()) {
                case DetectedActivity.IN_VEHICLE: {
                    Log.i(TAG, "In Vehicle: " + activity.getConfidence());
                    if (activity.getConfidence() >= 75 && permissionCheck == PackageManager.PERMISSION_GRANTED) {

                        driving = true;
                        savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVING_BOOL, driving);
                        odometerIntent = new Intent(this, OdometerService.class);
                        //TODO: shouldn't start and bind service
                        startService(odometerIntent);
                        bindService(odometerIntent, connection, Context.BIND_AUTO_CREATE);
                        bound = true;

                        recordDrive();
                        break;
                    }
                }
                break;

                case DetectedActivity.ON_BICYCLE: {
                    Log.i(TAG, "On Bicycle: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    Log.i(TAG, "On Foot: " + activity.getConfidence());
                    if (activity.getConfidence() >= 75 && permissionCheck == PackageManager.PERMISSION_GRANTED) {

                        loadSharedPreferences();
                        driving = false;
                        savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVING_BOOL, driving);

                        if (tripDistance == 0.0) {
                            Log.i(TAG, "onHandleDetectedActivities: tripDistance: 0.0");
                            turnOffOdometer();
                            break;
                        } else if (tripDistance >= .25) {
                            Log.i(TAG, "onHandleDetectedActivities: tripDistance " + tripDistance);
                            logDrive();
                            tripDistance = 0.0;
                            savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, tripDistance);
                        } else {
                            Log.i(TAG, "onHandleDetectedActivities: getMiles < .20");
                            tripDistance = 0.0;
                            turnOffOdometer();
                        }
                    }
                    break;
                }
                case DetectedActivity.RUNNING: {
                    Log.i(TAG, "Running: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.STILL: {
                    Log.i(TAG, "Still: " + activity.getConfidence());

                    if (activity.getConfidence() >= 95 && permissionCheck == PackageManager.PERMISSION_GRANTED) {

                        loadSharedPreferences();
                        driving = false;
                        savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVING_BOOL, driving);

                        if (tripDistance == 0.0) {
                            Log.i(TAG, "onHandleDetectedActivities: tripDistance: 0.0");
                            turnOffOdometer();
                            break;
                        } else if (tripDistance >= .25) {
                            Log.i(TAG, "onHandleDetectedActivities: " + tripDistance);
                            logDrive();
                            tripDistance = 0.0;
                        } else {
                            Log.i(TAG, "onHandleDetectedActivities: getMiles < .25");
                            tripDistance = 0.0;
                            turnOffOdometer();
                        }
                    }
                    break;
                }
            }
        }
    }

    private void recordDrive() {
        Log.i(TAG, "recordDrive: method called");
        loadSharedPreferences();

        if (odometer != null) {
            tripDistance = odometer.getMiles();
        }

        savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, tripDistance);
        Log.i(TAG, "tripDistance" + tripDistance);
    }

    private double logDrive() {
        Log.i(TAG, "logDrive: method ran");
        if (tripDistance != 0.0) {
            finalTripDistance = tripDistance;
            Log.i(TAG, "finalTripOdometer: " + finalTripDistance);
            new LogTripTask(this, finalTripDistance).execute();
            buildNotification(tripDistance);
            tripDistance = 0.0;
            Log.i(TAG, "tripOdometer reset to: " + tripDistance);
            turnOffOdometer();
        }
        return finalTripDistance;

    }

    private void buildNotification(double distance) {
        DecimalFormat distanceFormat = new DecimalFormat(getString(R.string.distance_decimal_format));
        String distanceString = distanceFormat.format(distance);
        Log.i(TAG, "doInBackground: " + distanceString);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentText(getString(R.string.notification_copy_1) + distanceString +
                getString(R.string.notification_copy_2));
        builder.setSmallIcon(R.drawable.ic_stat_car_icon);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentIntent(intent);
        builder.setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(0, builder.build());
    }

    private void turnOffOdometer() {
        savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0);
        odometerIntent = new Intent(this, OdometerService.class);
        startService(odometerIntent);
        bindService(odometerIntent, connection, Context.BIND_AUTO_CREATE);

        if (odometer != null) {
            odometer.reset();
        }

        stopService(odometerIntent);
        unbindService(connection);
        bound = false;
    }

    private void loadSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        tripDistance = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0f);
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            Log.i(TAG, "onServiceConnected called");
            OdometerService.OdometerBinder odometerBinder =
                    (OdometerService.OdometerBinder) binder;
            odometer = odometerBinder.getOdometer();
            Log.i(TAG, "onServiceConnected: odometer initiated");

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected called");
            bound = false;
        }
    };

    @Override
    public void onConnected(Bundle bundle) {

    }

    private void savePreferencesDouble(String key, double value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        float valueFloat = (float) value;
        editor.putFloat(key, valueFloat);
        editor.apply();
    }

    private void savePreferencesBoolean(String key, boolean value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean valueBoolean = (boolean) value;
        editor.putBoolean(key, valueBoolean);
        editor.apply();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended called");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed: method called");
    }

    @Override
    public void onResult(Status status) {
        Log.i(TAG, "onResult: method called");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy called");
        super.onDestroy();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }
}