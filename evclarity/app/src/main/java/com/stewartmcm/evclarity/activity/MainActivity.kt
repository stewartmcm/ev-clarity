package com.stewartmcm.evclarity.activity

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.stewartmcm.evclarity.Constants
import com.stewartmcm.evclarity.EvApplication
import com.stewartmcm.evclarity.R
import com.stewartmcm.evclarity.service.ActivityUpdatesIntentService
import com.stewartmcm.evclarity.service.LocationUpdatesIntentService
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var sharedPrefs: SharedPreferences
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar()
        (application as EvApplication).evComponent.inject(this)
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val trackingSwitch = menu.findItem(R.id.myswitch).actionView?.findViewById<View>(R.id.switchForActionBar) as SwitchCompat

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

        trackingSwitch.isChecked = sharedPrefs.getBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false) && hasAllPermissions()

        trackingSwitch.setOnCheckedChangeListener { switch, isChecked ->
            if (isChecked) {
                if (!gpsEnabled) {
                    showPermissionDeniedDialog(getString(R.string.location_not_avail_alert_message))
                    val alertDialog = AlertDialog.Builder(this@MainActivity,
                            R.style.MyAlertDialogStyle)
                    alertDialog.setTitle(R.string.location_not_avail_alert_header)
                    alertDialog.setMessage(R.string.location_not_avail_alert_message)
                    alertDialog.setPositiveButton(R.string.go_to_settings_button) { paramDialogInterface, paramInt ->
                        val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(myIntent)
                    }
                    alertDialog.show()
                } else if (hasAllPermissions()){
                    toggleDriveTracking(true)
                } else {
                    requestAllPermissions()
                }
                switch.isChecked = true

            } else {
                if (!gpsEnabled) {
                    putSharedPrefsBoolean(Constants.KEY_SHARED_PREF_GPS_STATE, false)
                }
                toggleDriveTracking(false)
            }
            trackingSwitch.isChecked = sharedPrefs.getBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)

        if (item.itemId == R.id.action_settings && navController.currentDestination!!.id != R.id.energySettingsFragment) {
            navController.navigate(R.id.energySettingsFragment)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setSupportActionBar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(true)
        }
        supportActionBar!!.elevation = 0f
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    private fun toggleDriveTracking(isTracking: Boolean) {
        val intent = Intent(this, ActivityUpdatesIntentService::class.java)
        val pendingIntent = PendingIntent.getForegroundService(this, Constants.DRIVE_TRACKING_FOREGROUND_SERVICE_REQUEST, intent, PendingIntent.FLAG_IMMUTABLE)

        if (isTracking) {
            ActivityRecognition.getClient(this).requestActivityUpdates(Constants.ACTIVITY_UPDATES_INTERVAL_IN_MILLISECONDS, pendingIntent)
            Toast.makeText(this@MainActivity, getString(R.string.drive_tracking_on),
                    Toast.LENGTH_SHORT).show()
        } else {
            ActivityRecognition.getClient(this).removeActivityUpdates(pendingIntent)
            removeLocationUpdates()
            putSharedPrefsBoolean(Constants.KEY_SHARED_PREF_IS_DRIVING, false)
            Toast.makeText(this@MainActivity, getString(R.string.drive_tracking_off),
                    Toast.LENGTH_SHORT).show()
        }
        putSharedPrefsBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, isTracking)

    }

    private fun removeLocationUpdates() {
        val locationUpdatesIntent = Intent(this, LocationUpdatesIntentService::class.java)
        val locationUpdatesPendingIntent = PendingIntent.getForegroundService(this, 0, locationUpdatesIntent, PendingIntent.FLAG_IMMUTABLE)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.removeLocationUpdates(locationUpdatesPendingIntent)
    }

    private fun putSharedPrefsBoolean(key: String, isTracking: Boolean) {
        val editor = sharedPrefs.edit()
        editor.putBoolean(key, isTracking)
        editor.apply()
    }

    private fun hasAllPermissions(): Boolean {
        return (hasBackgroundLocationPermission() && hasFineLocationPermission() && hasActivityRecognitionPermission())
    }

    private fun hasFineLocationPermission(): Boolean {
        val fineLocationPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)

        return fineLocationPermissionCheck == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        val backgroundLocationPermissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        return backgroundLocationPermissionCheck == PackageManager.PERMISSION_GRANTED
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        val permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION)

        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAllPermissions() {
        ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ),
                Constants.TRACKING_SWITCH_PERMISSIONS_RESULT_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.TRACKING_SWITCH_PERMISSIONS_RESULT_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showPermissionDeniedDialog(getString(R.string.permission_rationale_background_location))
            }
            if (grantResults.isNotEmpty() && grantResults[1] == PackageManager.PERMISSION_DENIED) {
                showPermissionDeniedDialog(getString(R.string.permission_rationale_general))
            }
            if (grantResults.isNotEmpty() && grantResults[2] == PackageManager.PERMISSION_DENIED) {
                showPermissionDeniedDialog(getString(R.string.permission_rationale_general))
            }
        }
    }

    private fun showPermissionDeniedDialog(msg: String) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton(getString(R.string.go_to_settings_button)) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(getString(R.string.cancel_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent()
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", this.packageName, null)
        intent.setData(uri)
        this.startActivity(intent)
    }
}