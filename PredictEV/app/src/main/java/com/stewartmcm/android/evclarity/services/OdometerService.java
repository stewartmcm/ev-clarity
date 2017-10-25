package com.stewartmcm.android.evclarity.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.stewartmcm.android.evclarity.activities.Constants;

public class OdometerService extends Service {
    private static final String TAG = Constants.ODOMETER_SERVICE_TAG;
    private final IBinder binder = new OdometerBinder();
    private static double distanceInMeters;
    private static Location lastLocation = null;
    private LocationManager locManager;
    private LocationListener listener;
    private boolean mDriving;

    class OdometerBinder extends Binder {
        OdometerService getOdometer() {
            return OdometerService.this;
        }
    }

    public OdometerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: OdometerService is running");

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                loadSharedPreferences();

                if (lastLocation == null) {
                    lastLocation = location;
                }

                if (mDriving ==false) {
                    distanceInMeters = 0.0;
                }

                distanceInMeters += location.distanceTo(lastLocation);
                lastLocation = location;
                savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, distanceInMeters / 1609.344);
                Log.i(TAG, "onCreate: OdoServ TripMileage: " + distanceInMeters / 1609.344);
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, listener);

    }

    @Override
    public void onDestroy() {
        locManager.removeUpdates(listener);
        Log.i(TAG, "onDestroy: location updates removed");

        super.onDestroy();
    }

    public double getMiles() {
        double rawMiles = this.distanceInMeters / 1609.344;
        Log.i(TAG, "tripmileage: " + rawMiles);

        return rawMiles;
    }

    public double reset() {
        //TODO: do we need to remove updates here? onDestroy() removes updates too.
        locManager.removeUpdates(listener);
        distanceInMeters = 0.0;
        return 0.0;
    }

    private void loadSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mDriving = sharedPreferences.getBoolean(Constants.KEY_SHARED_PREF_DRIVING_BOOL,
                false);

    }

    private void savePreferencesDouble(String key, double value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        float valueFloat = (float) value;
        editor.putFloat(key, valueFloat);
        editor.commit();
    }
}