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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

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
import com.stewartmcm.android.evclarity.activities.MainActivity;
import com.stewartmcm.android.evclarity.data.PredictEvDatabaseHelper;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class DetectedActivitiesIntentService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
    protected static final String TAG = Constants.DETECTED_ACTIVITIES_INTENT_SERVICE_TAG;

    private GoogleApiClient mGoogleApiClient;
    private Cursor cursor;
    private OdometerService odometer;
    private Intent odometerIntent;
    private boolean bound = false;
    private String currentMPGString;
    private String utilityRateString;
    private String gasPriceString;
    private double finalTripDistance;
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
//        Log.i(TAG, "onCreate: method ran");
        buildGoogleApiClient();
    }

    @Override
    public void onStart(Intent intent, int startId) {
//        Log.i(TAG, "onStart called");
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
//        Log.i(TAG, "onHandleIntent: method ran");
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            handleDetectedActivities(result.getProbableActivities());
        }

    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
//        Log.i(TAG, "handleDetectedActivities: method ran");

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        for (DetectedActivity activity : probableActivities) {
            switch (activity.getType()) {
                case DetectedActivity.IN_VEHICLE: {
//                    Log.i("ActivityRecogition", "In Vehicle: " + activity.getConfidence());
                    if (activity.getConfidence() >= 75 && permissionCheck == PackageManager.PERMISSION_GRANTED) {

                        odometerIntent = new Intent(this, OdometerService.class);
                        startService(odometerIntent);
                        bindService(odometerIntent, connection, Context.BIND_AUTO_CREATE);
                        bound = true;

                        if (odometer == null) {
//                            Log.i(TAG, "onHandleDetectedActivities: odometer null");
                            break;
                        } else {
//                            Log.i(TAG, "onHandleDetectedActivities: odometer not null");
                            recordDrive();
                        }
                        break;
                    }
                }
                break;

                case DetectedActivity.ON_BICYCLE: {
//                    Log.i("ActivityRecogition", "On Bicycle: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.ON_FOOT: {
//                    Log.i("ActivityRecogition", "On Foot: " + activity.getConfidence());
                    if (activity.getConfidence() >= 75 && permissionCheck == PackageManager.PERMISSION_GRANTED) {

                        loadSharedPreferences();

                        if (tripDistance == 0.0) {
//                            Log.i(TAG, "onHandleDetectedActivities: tripDistance: 0.0");
                            turnOffOdometer();
                            break;
                        } else if (tripDistance >= .25) {
//                            Log.i(TAG, "onHandleDetectedActivities: tripDistance " + tripDistance);
                            logDrive();
                            tripDistance = 0.0;
                            savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, tripDistance);
                        } else {
//                            Log.i(TAG, "onHandleDetectedActivities: getMiles < .20");
                            tripDistance = 0.0;
                            turnOffOdometer();
                        }
                    }
                    break;
                }
                case DetectedActivity.RUNNING: {
//                    Log.i("ActivityRecogition", "Running: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.STILL: {
//                    Log.i("ActivityRecogition", "Still: " + activity.getConfidence());

                    if (activity.getConfidence() >= 95 && permissionCheck == PackageManager.PERMISSION_GRANTED) {

                        loadSharedPreferences();

                        if (tripDistance == 0.0) {
//                            Log.i(TAG, "onHandleDetectedActivities: tripDistance: 0.0");
                            turnOffOdometer();
                            break;
                        } else if (tripDistance >= .25) {
//                            Log.i(TAG, "onHandleDetectedActivities: " + tripDistance);
                            logDrive();
                            tripDistance = 0.0;
                            savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, tripDistance);
                        } else {
//                            Log.i(TAG, "onHandleDetectedActivities: getMiles < .30");
                            tripDistance = 0.0;
                            turnOffOdometer();
                        }
                    }
                    break;
                }
                case DetectedActivity.TILTING: {
//                    Log.i("ActivityRecogition", "Tilting: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.WALKING: {
//                    Log.i("ActivityRecogition", "Walking: " + activity.getConfidence());

                    break;
                }
                case DetectedActivity.UNKNOWN: {
//                    Log.i("ActivityRecogition", "Unknown: " + activity.getConfidence());
                    break;
                }
            }
        }
    }

    protected void recordDrive() {
//        Log.i(TAG, "recordDrive: method called");

        tripDistance = 0.0;
        tripDistance = odometer.getMiles();
        savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, tripDistance);

//        Log.i(TAG, "runnable tripOdometer: " + tripDistance);

    }

    public double logDrive() {
//        Log.i(TAG, "logDrive: method ran");
        if (tripDistance != 0.0) {
            finalTripDistance = tripDistance;
//            Log.i(TAG, "finalTripOdometer: " + finalTripDistance);
            new LogTripTask().execute();

            DecimalFormat distanceFormat = new DecimalFormat("###.#");

            String distanceString = distanceFormat.format(tripDistance);
//            Log.i(TAG, "doInBackground: " + distanceString);

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent intent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setContentText("Trip Logged: " + distanceString + " miles");
            builder.setSmallIcon(R.drawable.ic_stat_car_icon);
            builder.setContentTitle(getString(R.string.app_name));
            builder.setContentIntent(intent);
            builder.setAutoCancel(true);

            NotificationManagerCompat.from(this).notify(0, builder.build());
            tripDistance = 0.0;
//            Log.i(TAG, "logDrive: tripOdometer: " + tripDistance);
            //TODO: test uncommenting the code below
            turnOffOdometer();
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
//            Log.i(TAG, "LogTripTask doInBackground: method ran");

            PredictEvDatabaseHelper mHelper = PredictEvDatabaseHelper.getInstance(DetectedActivitiesIntentService.this);
            SQLiteDatabase db = mHelper.getWritableDatabase();

            GregorianCalendar calender = new GregorianCalendar();

//            Date time = calender.getTime();

            double tripSavings = calcSavings(finalTripDistance);
//            Log.i(TAG, "doInBackground: " + tripSavings);

            DecimalFormat savingsFormat = new DecimalFormat("###.##");

            String savingsString = savingsFormat.format(tripSavings);
//            Log.i(TAG, "doInBackground: " + savingsString);

            //TODO: test if this code would work fine logging trips with 3 lines of code below
            cursor = db.query(Constants.TRIP_TABLE_NAME, new String[]{"SUM(TRIP_MILES) AS sum"},
                    null, null, null, null, null);
            cursor.moveToLast();
            try {
                mHelper.insertTrip(db, format(calender).toString(), "11:23", 37.828411, -122.289890, 37.805591,
                        -122.275583, finalTripDistance, savingsString);
                return true;

            } catch (SQLiteException e) {

                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            cursor.moveToFirst();
        }
    }

    private void turnOffOdometer() {
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

//        Log.i(TAG, "calcSavings: gasPrice: " + gasPrice);


        if (utilityRate != 0.0) {
//            Log.i(TAG, "calcSavings: utilityRateString: " + utilityRateString);

            // .3 is Nissan Leaf's kWh per mile driven (EV equivalent of mpg)
            savings = mileageDouble * ((gasPrice / currentMPG) - (.3 * utilityRate));

            return savings;
        }
        return 0.00;

    }

    //TODO: if this crashes app, make the method static
    public String format(GregorianCalendar calendar){
        SimpleDateFormat fmt = new SimpleDateFormat(getString(R.string.date_format));
        fmt.setCalendar(calendar);
        return fmt.format(calendar.getTime());
    }

    private void loadSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        utilityRateString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_RATE,
                getString(R.string.default_utility_rate));
        gasPriceString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_GAS_PRICE,
                getString(R.string.default_gas_price));
        currentMPGString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG,
                getString(R.string.default_mpg));
        tripDistance = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0f);
//        Log.i(TAG, "loadSavedPreferences: isChecked: " + isChecked);

    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
//            Log.i(TAG, "onServiceConnected called");
            OdometerService.OdometerBinder odometerBinder =
                    (OdometerService.OdometerBinder) binder;
            odometer = odometerBinder.getOdometer();
//            Log.i(TAG, "onServiceConnected: odometer initiated");

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
//            Log.i(TAG, "onServiceDisconnected called");
            bound = false;
        }
    };

    @Override
    public void onConnected(Bundle bundle) {
//        Log.i(TAG, "onConnected: method called");
//        int permissionCheck = ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION);
//
//        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
//            Log.i(TAG, "permission granted");
//            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
//                    mGoogleApiClient);
//
//            if (mLastLocation != null) {
//                Log.i(TAG, "onConnected: mlastLocation not null");
//                latString = String.valueOf(mLastLocation.getLatitude());
//                Log.i(TAG, "latString: " + latString);
//                lonString = String.valueOf(mLastLocation.getLongitude());
//                Log.i(TAG, "lonString: " + lonString);
//
//                Log.i(TAG, "onConnected: OdometerService now bound to DetectionActivitiesIntentService");
//            }
//
//        } else {
//            Log.i(TAG, "onConnected: location permission not granted");
//
//        }

    }

    private void savePreferencesDouble(String key, double value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        float valueFloat = (float) value;
        editor.putFloat(key, valueFloat);
        editor.commit();
    }

    @Override
    public void onConnectionSuspended(int i) {
//        Log.i(TAG, "onConnectionSuspended called");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
//        Log.i(TAG, "onConnectionFailed: method called");
    }

    @Override
    public void onResult(Status status) {
//        Log.i(TAG, "onResult: method called");
    }

    @Override
    public void onDestroy() {
//        Log.i(TAG, "onDestroy called");
        super.onDestroy();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }
}
