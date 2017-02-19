package com.stewartmcm.android.evclarity.services;

import android.Manifest;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
import com.stewartmcm.android.evclarity.R;
import com.stewartmcm.android.evclarity.activities.Constants;
import com.stewartmcm.android.evclarity.data.PredictEvDatabaseHelper;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
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
    private String currentMPGString;
    private String utilityRateString;
    private String gasPriceString;
    private boolean isChecked;
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

    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "onStart called");
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


    //TODO: try starting the service after device detects it's in a vehicle, not every time activity recognition has a result

    /**
     * Handles incoming intents.
     *
     * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
     *               is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent: method ran");
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            odometerIntent = new Intent(this, OdometerService.class);
            startService(odometerIntent);
            bindService(odometerIntent, connection, Context.BIND_AUTO_CREATE);
            bound = true;

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
                    Log.i("ActivityRecogition", "In Vehicle: " + activity.getConfidence());
                    if (activity.getConfidence() >= 75 && permissionCheck == PackageManager.PERMISSION_GRANTED) {

                        odometerIntent = new Intent(this, OdometerService.class);
                        startService(odometerIntent);
                        bindService(odometerIntent, connection, Context.BIND_AUTO_CREATE);
                        bound = true;

                        if (odometer == null) {
                            Log.i(TAG, "onHandleDetectedActivities: odometer null");
                            break;
                        } else {
                            Log.i(TAG, "onHandleDetectedActivities: odometer not null");
                            driving = true;
                            recordDrive();
                        }
                        break;
                    }
                }
                break;

                case DetectedActivity.ON_BICYCLE: {
                    Log.i("ActivityRecogition", "On Bicycle: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    Log.i("ActivityRecogition", "On Foot: " + activity.getConfidence());
                    if (activity.getConfidence() >= 75 && permissionCheck == PackageManager.PERMISSION_GRANTED) {

                        odometerIntent = new Intent(this, OdometerService.class);
                        startService(odometerIntent);
                        bindService(odometerIntent, connection, Context.BIND_AUTO_CREATE);
                        bound = true;

                        if (odometer == null) {
                            Log.i(TAG, "onHandleDetectedActivities: odometer null");
                            break;
                        } else if (odometer.getMiles() >= .2) {
                            Log.i(TAG, "onHandleDetectedActivities: " + odometer.getMiles());
                            logDrive();
                            driving = false;
                        } else {
                            Log.i(TAG, "onHandleDetectedActivities: getMiles < .20");
                            odometer.reset();
                        }
                    }
                    break;
                }
                case DetectedActivity.RUNNING: {
                    Log.i("ActivityRecogition", "Running: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.STILL: {
                    Log.i("ActivityRecogition", "Still: " + activity.getConfidence());
                    if (activity.getConfidence() >= 95 && permissionCheck == PackageManager.PERMISSION_GRANTED) {



                        if (odometer == null) {
                            Log.i(TAG, "onHandleDetectedActivities: odometer null");
                            break;
                        } else if (odometer.getMiles() >= .5) {

                            odometerIntent = new Intent(this, OdometerService.class);
                            startService(odometerIntent);
                            bindService(odometerIntent, connection, Context.BIND_AUTO_CREATE);
                            bound = true;

                            Log.i(TAG, "onHandleDetectedActivities: " + odometer.getMiles());
                            logDrive();
                            driving = false;
                        } else {
                            Log.i(TAG, "onHandleDetectedActivities: getMiles < .50");
                            odometer.reset();
                        }

                    }
                    break;
                }
                case DetectedActivity.TILTING: {
                    Log.i("ActivityRecogition", "Tilting: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.WALKING: {
                    Log.i("ActivityRecogition", "Walking: " + activity.getConfidence());

                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    Log.i("ActivityRecogition", "Unknown: " + activity.getConfidence());
                    break;
                }
            }
        }
    }

    protected void recordDrive() {
        Log.i(TAG, "recordDrive: method called");

        tripDistance = 0.0;
        tripDistance = odometer.getMiles();

        Log.i(TAG, "runnable tripOdometer: " + tripDistance);

    }

    public double logDrive() {
        Log.i(TAG, "logDrive: method ran");
        if (odometer != null) {
            finalTripDistance = odometer.getMiles();
            Log.i(TAG, "finalTripOdometer: " + finalTripDistance);
            new LogTripTask().execute();
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setContentText("Trip Logged: " + odometer.getMiles() + " miles");
            builder.setSmallIcon(R.drawable.ic_stat_car_icon);
            builder.setContentTitle(getString(R.string.app_name));
            NotificationManagerCompat.from(this).notify(0, builder.build());
            odometer.reset();
            Log.i(TAG, "logDrive: tripOdometer: " + odometer.reset());
            //TODO: test uncommenting the code below
//            unbindService(connection);
//            bound = false;
            stopService(odometerIntent);
        }
        return finalTripDistance;

    }

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

            GregorianCalendar calender = new GregorianCalendar();
            Date time = calender.getTime();

            double tripSavings = calcSavings(finalTripDistance);

            //TODO: test if this code would work fine logging trips with 3 lines of code below
            cursor = db.query("TRIP", new String[]{"SUM(TRIP_MILES) AS sum"},
                    null, null, null, null, null);
            cursor.moveToLast();
            try {
                mHelper.insertTrip(db, time.toString(), "11:23", 37.828411, -122.289890, 37.805591,
                        -122.275583, finalTripDistance, Double.toString(tripSavings));
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

    private double calcSavings(double mileageDouble) {

        loadSharedPreferences();

        double savings;
        double utilityRate = Double.parseDouble(utilityRateString);
        double gasPrice;
        double currentMPG;

        if (gasPriceString.isEmpty()) {
            gasPrice = 0.0;
        } else {
            gasPrice = Double.parseDouble(gasPriceString);
        }

        if (currentMPGString.isEmpty()) {
            currentMPG = 0.0;
        } else {
            currentMPG = Double.parseDouble(currentMPGString);
        }

        Log.i(TAG, "calcSavings: gasPrice: " + gasPrice);


        if (utilityRate != 0.0) {
            Log.i(TAG, "calcSavings: utilityRateString: " + utilityRateString);

            // .3 is Nissan Leaf's kWh per mile driven (EV equivalent of mpg)
            savings = mileageDouble * ((gasPrice / currentMPG) - (.3 * utilityRate));

            return savings;
        }
        return 0.00;

    }

    private void loadSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        utilityRateString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_RATE, "0.0000");
        gasPriceString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_GAS_PRICE, "0");
        currentMPGString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG, "0.0");
        isChecked = sharedPreferences.getBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false);
        Log.i(TAG, "loadSavedPreferences: isChecked: " + isChecked);

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

                Log.i(TAG, "onConnected: OdometerService now bound to DetectionActivitiesIntentService");
            }

        } else {
            Log.i(TAG, "onConnected: location permission not granted");

        }

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
