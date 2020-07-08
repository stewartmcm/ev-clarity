package com.stewartmcm.evclarity.service;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.stewartmcm.evclarity.Constants;
import com.stewartmcm.evclarity.R;

import timber.log.Timber;

public class LocationUpdatesIntentService extends IntentService {
    protected static final String TAG = Constants.ACTIVITY_UPDATES_INTENT_SERVICE_TAG;

    public LocationUpdatesIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(98, buildForegroundNotification());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                putPrefsDouble(Constants.KEY_SHARED_PREF_NEW_LATITUDE, location.getLatitude());
                putPrefsDouble(Constants.KEY_SHARED_PREF_NEW_LONGITUDE, location.getLongitude());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Timber.e(e);
            }
        });
    }

    private Notification buildForegroundNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.TRIP_SUMMARY_NOTIFICATION_CHANNEL_ID);
        builder.setContentText("Getting location.");
        builder.setSmallIcon(R.drawable.ic_stat_car_icon);
        builder.setContentTitle(getString(R.string.app_name));

        return builder.build();
    }

    private void putPrefsDouble(String key, double value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(key, (float) value);
        editor.apply();
    }

}