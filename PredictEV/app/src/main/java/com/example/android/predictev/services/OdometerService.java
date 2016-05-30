package com.example.android.predictev.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.text.DecimalFormat;

public class OdometerService extends Service {
    private static final String TAG = "OdometerService";
    private final IBinder binder = new OdometerBinder();
    private static double distanceInMeters;
    private static Location lastLocation = null;
    private LocationManager locManager;
    private LocationListener listener;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: OdometerService is running");
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

    public double getMiles() {
        double rawMiles = this.distanceInMeters / 1609.344;

        DecimalFormat decimal = new DecimalFormat("#.00");
        double miles = Double.valueOf(decimal.format(rawMiles));
        Log.i(TAG, "tripmileage: " + miles);

        return miles;
    }

    public double reset() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return this.distanceInMeters / 1609.344;
        }
//        locManager.removeUpdates(listener);
        distanceInMeters = 0.0;
        return 0.0;
    }

    public class OdometerBinder extends Binder {
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
}
