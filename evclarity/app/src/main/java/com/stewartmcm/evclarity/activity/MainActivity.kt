package com.stewartmcm.evclarity.activity

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.LocationServices
import com.stewartmcm.evclarity.Constants
import com.stewartmcm.evclarity.EvApplication
import com.stewartmcm.evclarity.R
import com.stewartmcm.evclarity.service.ActivityUpdatesIntentService
import javax.inject.Inject

class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    @Inject
    lateinit var sharedPrefs: SharedPreferences
    lateinit var googleApiClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar()
        (application as EvApplication).evComponent.inject(this)
        buildGoogleApiClient()
    }

    override fun onResume() {
        super.onResume()
        googleApiClient.connect()
        invalidateOptionsMenu()
    }

    override fun onStop() {
        super.onStop()
        googleApiClient.disconnect()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val trackingSwitch = menu.findItem(R.id.myswitch).actionView
                .findViewById<View>(R.id.switchForActionBar) as SwitchCompat

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

        trackingSwitch.isChecked = sharedPrefs.getBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false)

        trackingSwitch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {

                if (!hasLocationPermission()) {
                    val alertDialog = AlertDialog.Builder(this@MainActivity).create()
                    alertDialog.setTitle(getString(R.string.location_permission_alert_header))
                    alertDialog.setMessage(getString(R.string.location_permission_alert))
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dialog_ok)
                    ) { dialog, _ -> dialog.dismiss() }
                    alertDialog.show()
                } else if (!gpsEnabled) {
                    val alertDialog = AlertDialog.Builder(this@MainActivity,
                            R.style.MyAlertDialogStyle)
                    alertDialog.setTitle(R.string.location_not_avail_alert_header)
                    alertDialog.setMessage(R.string.location_not_avail_alert_message)
                    alertDialog.setPositiveButton(R.string.got_to_settings_button) { paramDialogInterface, paramInt ->
                        val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(myIntent)
                    }
                    alertDialog.show()
                } else if (!googleApiClient.isConnected) {
                    putSharedPrefsBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false)
                    Toast.makeText(this@MainActivity, getString(R.string.not_connected),
                            Toast.LENGTH_SHORT).show()

                } else {
                    toggleDriveTracking(true)
                }

            } else {
                if (!googleApiClient.isConnected) {
                    Toast.makeText(this@MainActivity, getString(R.string.not_connected), Toast.LENGTH_SHORT).show()
                    return@OnCheckedChangeListener
                } else if (!gpsEnabled) {
                    putSharedPrefsBoolean(Constants.KEY_SHARED_PREF_GPS_STATE, false)

                }
                toggleDriveTracking(false)
            }
            trackingSwitch.isChecked = sharedPrefs.getBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false)
        })
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

    @Synchronized
    private fun buildGoogleApiClient() {
        googleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .build()
    }

    override fun onConnected(connectionHint: Bundle?) {
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionCheck != PackageManager.PERMISSION_GRANTED && !ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
            val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(this,
                    permissions,
                    Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION)
        }
    }

    override fun onConnectionFailed(result: ConnectionResult) {
         Log.i("Google Api Client", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    override fun onConnectionSuspended(cause: Int) {
        googleApiClient.connect()
    }

    override fun onResult(status: Status) {
        if (!status.isSuccess) Toast.makeText(this, getString(R.string.no_gps_data), Toast.LENGTH_SHORT).show()
    }

    private fun setSupportActionBar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
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
        val pendingIntent = PendingIntent.getForegroundService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (isTracking) {
            ActivityRecognition.getClient(this)
                    .requestActivityUpdates(
                            Constants.ACTIVITY_UPDATES_INTERVAL_IN_MILLISECONDS,
                            pendingIntent
                    )
            Toast.makeText(this@MainActivity, getString(R.string.drive_tracking_on),
                    Toast.LENGTH_SHORT).show()
        } else {
            ActivityRecognition.getClient(this).removeActivityUpdates(
                    pendingIntent
            )
            Toast.makeText(this@MainActivity, getString(R.string.drive_tracking_off),
                    Toast.LENGTH_SHORT).show()
        }
        putSharedPrefsBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, isTracking)

    }

    private fun putSharedPrefsBoolean(key: String, isTracking: Boolean) {
        val editor = sharedPrefs.edit()
        editor.putBoolean(key, isTracking)
        editor.apply()
    }

    private fun hasLocationPermission(): Boolean {
        val permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)

        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }
}