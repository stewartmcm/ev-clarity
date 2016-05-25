package com.example.android.predictev.services;

import android.Manifest;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.example.android.predictev.activities.Constants;
import com.example.android.predictev.PredictEvDatabaseHelper;
import com.example.android.predictev.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class DetectedActivitiesIntentService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
    protected static final String TAG = "DetectedActivities";

    private GoogleApiClient mGoogleApiClient;
    Cursor cursor;
    SQLiteDatabase db;
    private OdometerService odometer;
    Intent odometerIntent;
    private boolean driving;
    private boolean bound = false;
    private Location mLastLocation;
    private String latString;
    private String lonString;
    private double finalTripDistance;
    private static double distanceInMeters;
    private static Location lastLocation = null;
    private LocationManager locManager;
    private LocationListener listener;
    public double tripDistance;

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public DetectedActivitiesIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: method ran");
        buildGoogleApiClient();

        odometerIntent = new Intent(this, OdometerService.class);
        bindService(odometerIntent, connection, Context.BIND_AUTO_CREATE);
        Log.i(TAG, "onCreate: OdometerService now bound to DetectionActivitiesIntentService");
    }

    /**
     * Handles incoming intents.
     * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
     *               is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent: method ran");
        if(ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getProbableActivities());
        }

        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        Intent localIntent = new Intent(Constants.BROADCAST_ACTION);

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

        // Log each activity.
        Log.i(TAG, "activities detected");
        for (DetectedActivity da: detectedActivities) {
            Log.i(TAG, Constants.getActivityString(
                            getApplicationContext(),
                            da.getType()) + " " + da.getConfidence() + "%"
            );
        }

        // Broadcast the list of detected activities.
        localIntent.putExtra(Constants.ACTIVITY_EXTRA, detectedActivities);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        Log.i(TAG, "handleDetectedActivities: method ran");

        for( DetectedActivity activity : probableActivities ) {
            switch( activity.getType() ) {
                case DetectedActivity.IN_VEHICLE: {
                    Log.i("ActivityRecogition", "In Vehicle: " + activity.getConfidence());
                    if( activity.getConfidence() >= 75 ) {

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                        builder.setContentText( "Are you driving?" );
                        builder.setSmallIcon(R.mipmap.ic_launcher);
                        builder.setContentTitle(getString(R.string.app_name));
                        NotificationManagerCompat.from(this).notify(0, builder.build());
                        int permissionCheck = ContextCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION);

                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            Log.i(TAG, "Connected to GoogleApiClient");

                        } else {

                        }
                    }

                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    Log.i( "ActivityRecogition", "On Bicycle: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    Log.i("ActivityRecogition", "On Foot: " + activity.getConfidence());
                    if( activity.getConfidence() >= 75 ) {

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                        builder.setContentText( "Are you on foot?" );
                        builder.setSmallIcon(R.mipmap.ic_launcher);
                        builder.setContentTitle(getString(R.string.app_name));
                        NotificationManagerCompat.from(this).notify(0, builder.build());
                        int permissionCheck = ContextCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION);

                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            Log.i(TAG, "On foot and Location Permission is granted");
                            if (odometer == null) {
                                Log.i(TAG, "onReceive: odometer null");
                                break;
                            } else if (activity.getConfidence() >= 75) {
                                driving = true;
                                recordDrive();
                            }
                            break;

                        }
                    }
                    break;
                }
                case DetectedActivity.RUNNING: {
                    Log.i( "ActivityRecogition", "Running: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.STILL: {
                    Log.i("ActivityRecogition", "Still: " + activity.getConfidence());
                    if( activity.getConfidence() >= 75 ) {

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                        builder.setContentText( "Are you still?" );
                        builder.setSmallIcon(R.mipmap.ic_launcher);
                        builder.setContentTitle(getString(R.string.app_name));
                        NotificationManagerCompat.from(this).notify(0, builder.build());
                        int permissionCheck = ContextCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION);

                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            Log.i(TAG, "Still and Location Permission is granted");
                            Log.i("MainActivity", "Still %: " + activity.getConfidence());
                            if (odometer == null) {
                                Log.i(TAG, "onReceive: odometer null");
                                break;
                            } else if (odometer.getMiles() != 0 && activity.getConfidence() >= 75) {
                                Log.i(TAG, "onReceive: getMiles != 0 && still > 75");
                                driving = false;
                                Log .i(TAG, "onReceive: " + odometer.getMiles());
                                logDrive();
                            }else {
                                Log.i(TAG, "onReceive: getMiles == 0 && still > 75");
                            }
                            break;

                        } else {

                        }
                    }
                    break;
                }
                case DetectedActivity.TILTING: {
                    Log.i( "ActivityRecogition", "Tilting: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.WALKING: {
                    Log.i( "ActivityRecogition", "Walking: " + activity.getConfidence() );

                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    Log.i( "ActivityRecogition", "Unknown: " + activity.getConfidence() );
                    break;
                }
            }
        }
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

    protected void recordDrive() {
        Log.i(TAG, "recordDrive: method called");

        final android.os.Handler handler = new android.os.Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                tripDistance = 0.0;
                if (driving) {
                    tripDistance = odometer.getMiles();
                    Log.i(TAG, "runnable tripOdometer: " + tripDistance);
                }

                handler.postDelayed(this, 1000);
            }

        });

    }

    public double logDrive() {
        Log.i(TAG, "logDrive: method ran");
        if (odometer != null) {
            finalTripDistance = odometer.getMiles();
            Log.i(TAG, "finalTripOdometer: " + finalTripDistance);
            new LogTripTask().execute();
            odometer.reset();
            Log.i(TAG, "logDrive: tripOdometer: " + odometer.reset());
            unbindService(connection);
            bound = false;
        }
        return finalTripDistance;

    }

//    public double getMiles() {
//        return this.distanceInMeters / 1609.344;
//    }

//    public double reset() {
//        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return this.distanceInMeters / 1609.344;
//        }
//        locManager.removeUpdates(listener);
//        distanceInMeters = 0.0;
//        return 0.0;
//    }

    //logs new trip asynchronously when executed
    private class LogTripTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Log.i(TAG, "LogTripTask doInBackground: method ran");

            PredictEvDatabaseHelper mHelper = PredictEvDatabaseHelper.getInstance(DetectedActivitiesIntentService.this);
            db = mHelper.getWritableDatabase();
            cursor = db.query("TRIP", new String[]{"SUM(TRIP_MILES) AS sum"},
                    null, null, null, null, null);
            cursor.moveToLast();
            try {
                mHelper.insertTrip(db,"2016-05-01","11:23",37.828411,-122.289890,37.805591,-122.275583,finalTripDistance);
                return true;

            } catch (SQLiteException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            cursor.moveToFirst();
            if (success) {

                double sumLoggedTripsDouble = cursor.getDouble(0);

                Log.i(TAG, "onPostExecute: new trip added to db");

            } else {

                Log.i(TAG, "onPostExecute: database unavailable");
            }
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            OdometerService.OdometerBinder odometerBinder =
                    (OdometerService.OdometerBinder) binder;
            odometer = odometerBinder.getOdometer();
            Log.i(TAG, "onServiceConnected: odometer initiated");
            bound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;
        }
    };

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected: method called");
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "permission granted");
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            if (mLastLocation != null) {
                Log.i(TAG, "onConnected: mlastLocation not null");
                latString = String.valueOf(mLastLocation.getLatitude());
                Log.i(TAG, "latString: " + latString);
                lonString = String.valueOf(mLastLocation.getLongitude());
                Log.i(TAG, "lonString: " + lonString);
            }

        } else {
            Log.i(TAG, "onConnected: mLastLocation is null");
    
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed: method called");
    }

    @Override
    public void onResult(Status status) {
        Log.i(TAG, "onResult: method called");

    }
}
