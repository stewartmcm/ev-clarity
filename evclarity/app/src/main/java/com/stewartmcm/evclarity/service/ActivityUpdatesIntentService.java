package com.stewartmcm.evclarity.service;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.stewartmcm.evclarity.Constants;
import com.stewartmcm.evclarity.LogTripTask;
import com.stewartmcm.evclarity.R;
import com.stewartmcm.evclarity.activity.MainActivity;

import java.text.DecimalFormat;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class ActivityUpdatesIntentService extends IntentService {
    protected static final String TAG = Constants.ACTIVITY_UPDATES_INTENT_SERVICE_TAG;
    private FusedLocationProviderClient fusedLocationProviderClient;

    public ActivityUpdatesIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(99, buildForegroundNotification());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getProbableActivities());
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        //TODO: needed?
        boolean hasLocationPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean driving = sharedPreferences.getBoolean(Constants.KEY_SHARED_PREF_IS_DRIVING, false);
        double tripDistance = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0f);

        for (DetectedActivity activity : probableActivities) {
            switch (activity.getType()) {
                case DetectedActivity.IN_VEHICLE:
                    if (activity.getConfidence() >= 90 && hasLocationPermission) {
                        if (!driving) {
                            requestLocationUpdates();
                            driving = true;
                        }
                        savePreferencesBoolean(Constants.KEY_SHARED_PREF_IS_DRIVING, true);
                        updateTripDistance();
                    }
                    break;

                case DetectedActivity.ON_FOOT:
                    //TODO: confirm that this location permission check isn't needed here
                    // hasLocationPermission
                    if (activity.getConfidence() >= 90) {
                        if (driving) {
                            driving = false;
                            removeLocationUpdates();
                        }
                        savePreferencesBoolean(Constants.KEY_SHARED_PREF_IS_DRIVING, false);

                        if (tripDistance >= .25) {
                            logDrive(tripDistance);
                            savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0);
                            savePreferencesDouble(Constants.KEY_SHARED_PREF_LAST_LATITUDE, 0.0);
                            savePreferencesDouble(Constants.KEY_SHARED_PREF_LAST_LONGITUDE, 0.0);
                        } else {
                            savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0);
                        }
                    }
                    break;
            }
        }
    }

    private void requestLocationUpdates() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        Intent locationUpdatesIntent = new Intent(this, LocationUpdatesIntentService.class);
        PendingIntent locationUpdatesPendingIntent = PendingIntent.getForegroundService(this, 0, locationUpdatesIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(2000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationUpdatesPendingIntent);
    }

    private void removeLocationUpdates() {
        Intent locationUpdatesIntent = new Intent(this, LocationUpdatesIntentService.class);
        PendingIntent locationUpdatesPendingIntent = PendingIntent.getForegroundService(this, 0, locationUpdatesIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationProviderClient.removeLocationUpdates(locationUpdatesPendingIntent);
    }

    private Location buildLocation(float lat, float lon) {
        Location location = new Location("");
        location.setLatitude(lat);
        location.setLongitude(lon);
        return location;
    }

    private void updateTripDistance() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        double tripDistance = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0f);

        //TODO: null check here
        float lastLat = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_LAST_LATITUDE, 0.0f);
        float lastLon = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_LAST_LONGITUDE, 0.0f);
        float newLat = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_NEW_LATITUDE, 0.0f);
        float newLon = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_NEW_LONGITUDE, 0.0f);

        Location newLocation = buildLocation(newLat, newLon);

        Location lastLocation;
        if (lastLat != 0.0f) lastLocation = buildLocation(lastLat, lastLon);
        else lastLocation = newLocation;

        tripDistance += lastLocation.distanceTo(newLocation);

        savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, tripDistance);
        savePreferencesDouble(Constants.KEY_SHARED_PREF_LAST_LATITUDE, newLat);
        savePreferencesDouble(Constants.KEY_SHARED_PREF_LAST_LONGITUDE, newLon);

    }

    private double getMiles(double meters) {
        double miles = meters / 1609.344;
        return miles;
    }

    private void logDrive(double tripDistance) {
        double miles = getMiles(tripDistance);
        new LogTripTask(this, miles).execute();
        buildNotification(miles);
        savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0);
    }

    private void buildNotification(double distance) {
        DecimalFormat distanceFormat = new DecimalFormat(getString(R.string.distance_decimal_format));
        String distanceString = distanceFormat.format(distance);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.TRIP_SUMMARY_NOTIFICATION_CHANNEL_ID);
        builder.setContentText(getString(R.string.notification_copy_1) + distanceString +
                getString(R.string.notification_copy_2));
        builder.setSmallIcon(R.drawable.ic_stat_car_icon);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentIntent(intent);
        builder.setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(0, builder.build());
    }

    private Notification buildForegroundNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.TRIP_SUMMARY_NOTIFICATION_CHANNEL_ID);
        builder.setContentText("Tracking Activity");
        builder.setSmallIcon(R.drawable.ic_stat_car_icon);
        builder.setContentTitle(getString(R.string.app_name));

        return builder.build();
    }

    private void savePreferencesDouble(String key, double value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(key, (float) value);
        editor.apply();
    }

    private void savePreferencesBoolean(String key, boolean value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

}