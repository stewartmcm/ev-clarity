package com.stewartmcm.evclarity.service

import android.Manifest
import android.app.IntentService
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.stewartmcm.evclarity.Constants
import com.stewartmcm.evclarity.LogTripTask
import com.stewartmcm.evclarity.R
import com.stewartmcm.evclarity.activity.MainActivity
import java.text.DecimalFormat

class ActivityUpdatesIntentService : IntentService(TAG) {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        startForeground(99, buildForegroundNotification())
    }

    override fun onHandleIntent(intent: Intent?) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            handleDetectedActivities(result.probableActivities)
        }
    }

    private fun handleDetectedActivities(probableActivities: List<DetectedActivity>) {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
        val hasBackgroundLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PERMISSION_GRANTED
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        var driving = sharedPreferences.getBoolean(Constants.KEY_SHARED_PREF_IS_DRIVING, false)
        val tripDistance = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0f).toDouble()

        for (activity in probableActivities) {
            when (activity.type) {
                DetectedActivity.STILL -> if (activity.confidence >= 90 && hasFineLocationPermission && hasBackgroundLocationPermission) {
                    if (!driving) {
                        requestLocationUpdates()
                        driving = true
                    }
                    savePreferencesBoolean(Constants.KEY_SHARED_PREF_IS_DRIVING, true)
                    updateTripDistance()
                }

                DetectedActivity.ON_FOOT ->
                    if (activity.confidence >= 90) {
                        if (driving) {
                            driving = false
                            removeLocationUpdates()
                        }
                        savePreferencesBoolean(Constants.KEY_SHARED_PREF_IS_DRIVING, false)

                        if (tripDistance >= .25) {
                            logDrive(tripDistance)
                            savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0)
                            savePreferencesDouble(Constants.KEY_SHARED_PREF_LAST_LATITUDE, 0.0)
                            savePreferencesDouble(Constants.KEY_SHARED_PREF_LAST_LONGITUDE, 0.0)
                        } else {
                            savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0)
                        }
                    }
            }
        }
    }

    private fun requestLocationUpdates() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val locationUpdatesIntent = Intent(this, LocationUpdatesIntentService::class.java)
        val locationUpdatesPendingIntent = PendingIntent.getForegroundService(this, 0, locationUpdatesIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val locationRequest = LocationRequest()
        locationRequest.interval = 2000
        locationRequest.fastestInterval = 2000
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest, locationUpdatesPendingIntent)
    }

    private fun removeLocationUpdates() {
        val locationUpdatesIntent = Intent(this, LocationUpdatesIntentService::class.java)
        val locationUpdatesPendingIntent = PendingIntent.getForegroundService(this, 0, locationUpdatesIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.removeLocationUpdates(locationUpdatesPendingIntent)
    }

    private fun buildLocation(lat: Double, lon: Double): Location {
        val location = Location("")
        location.latitude = lat
        location.longitude = lon
        return location
    }

    private fun updateTripDistance() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        var tripDistance = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0f).toDouble()

        //TODO: null check here
        val lastLat = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_LAST_LATITUDE, 0.0f).toDouble()
        val lastLon = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_LAST_LONGITUDE, 0.0f).toDouble()
        val newLat = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_NEW_LATITUDE, 0.0f).toDouble()
        val newLon = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_NEW_LONGITUDE, 0.0f).toDouble()

        val newLocation = buildLocation(newLat, newLon)

        val lastLocation: Location
        if (lastLat != 0.0)
            lastLocation = buildLocation(lastLat, lastLon)
        else
            lastLocation = newLocation

        tripDistance += lastLocation.distanceTo(newLocation)

        savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, tripDistance)
        savePreferencesDouble(Constants.KEY_SHARED_PREF_LAST_LATITUDE, newLat)
        savePreferencesDouble(Constants.KEY_SHARED_PREF_LAST_LONGITUDE, newLon)

    }

    private fun getMiles(meters: Double): Double {
        return meters / 1609.344
    }

    private fun logDrive(tripDistance: Double) {
        val miles = getMiles(tripDistance)
        LogTripTask(this, miles).execute()
        buildNotification(miles)
        savePreferencesDouble(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0)
    }

    private fun buildNotification(distance: Double) {
        val distanceFormat = DecimalFormat(getString(R.string.distance_decimal_format))
        val distanceString = distanceFormat.format(distance)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val intent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0)

        val builder = NotificationCompat.Builder(this, Constants.TRIP_SUMMARY_NOTIFICATION_CHANNEL_ID)
        builder.setContentText(getString(R.string.notification_copy_1) + distanceString +
                getString(R.string.notification_copy_2))
        builder.setSmallIcon(R.drawable.ic_stat_car_icon)
        builder.setContentTitle(getString(R.string.app_name))
        builder.setContentIntent(intent)
        builder.setAutoCancel(true)

        NotificationManagerCompat.from(this).notify(0, builder.build())
    }

    private fun buildForegroundNotification(): Notification {
        val builder = NotificationCompat.Builder(this, Constants.TRIP_SUMMARY_NOTIFICATION_CHANNEL_ID)
        builder.setContentText("Tracking Activity")
        builder.setSmallIcon(R.drawable.ic_stat_car_icon)
        builder.setContentTitle(getString(R.string.app_name))

        return builder.build()
    }

    private fun savePreferencesDouble(key: String, value: Double) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()
        editor.putFloat(key, value.toFloat())
        editor.apply()
    }

    private fun savePreferencesBoolean(key: String, value: Boolean) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    companion object {
        private const val TAG = Constants.ACTIVITY_UPDATES_INTENT_SERVICE_TAG
    }

}