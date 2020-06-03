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
import android.preference.PreferenceManager
import android.provider.Settings
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
import com.stewartmcm.evclarity.service.DetectedActivitiesIntentService
import javax.inject.Inject

class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    @Inject
    lateinit var sharedPrefs: SharedPreferences
    private var isTracking: Boolean = false
    private var gpsEnabled: Boolean = false
    private var googleApiClient: GoogleApiClient? = null

    private val activityDetectionPendingIntent: PendingIntent
        get() {
            val intent = Intent(this, DetectedActivitiesIntentService::class.java)

            return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    private var updatesRequestedState: Boolean
        get() = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, false)
        set(requestingUpdates) = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, requestingUpdates)
                .apply()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar()
        (application as EvApplication).evComponent.inject(this)
        getDriveTrackingStatus()
        buildGoogleApiClient()

    }

    override fun onResume() {
        super.onResume()
        getDriveTrackingStatus()
        googleApiClient!!.connect()
        invalidateOptionsMenu()
    }

    override fun onStop() {
        super.onStop()
        googleApiClient!!.disconnect()
    }

    private fun setSupportActionBar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(true)
        }
        supportActionBar!!.elevation = 0f
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val trackingSwitch = menu.findItem(R.id.myswitch).actionView
                .findViewById<View>(R.id.switchForActionBar) as SwitchCompat

        setTrackingSwitch(trackingSwitch)

        trackingSwitch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {

                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

                if (!hasLocationPermission()) {
                    val alertDialog = AlertDialog.Builder(this@MainActivity).create()
                    alertDialog.setTitle(getString(R.string.location_permission_alert_header))
                    alertDialog.setMessage(getString(R.string.location_permission_alert))
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dialog_ok)
                    ) { dialog, which -> dialog.dismiss() }
                    alertDialog.show()
                } else if (!gpsEnabled) {

                    val alertDialog = AlertDialog.Builder(this@MainActivity,
                            R.style.MyAlertDialogStyle)
                    alertDialog.setTitle(R.string.location_not_avail_alert_header)
                    alertDialog.setMessage(R.string.location_not_avail_alert_message)
                    alertDialog.setPositiveButton(R.string.got_to_settings_button) { paramDialogInterface, paramInt ->
                        val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(myIntent)
                        //get gps
                    }
                    alertDialog.show()
                } else if (!googleApiClient!!.isConnected) {
                    savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false)
                    Toast.makeText(this@MainActivity, getString(R.string.not_connected),
                            Toast.LENGTH_SHORT).show()

                } else {
                    toggleDriveTracking(true)
                }

            } else {

                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

                if (!googleApiClient!!.isConnected) {
                    Toast.makeText(this@MainActivity, getString(R.string.not_connected), Toast.LENGTH_SHORT).show()
                    return@OnCheckedChangeListener
                } else if (!gpsEnabled) {
                    savePreferencesBoolean(Constants.KEY_SHARED_PREF_GPS_STATE, false)

                }
                // Remove all activity updates for the PendingIntent that was used to request activity
                // updates.
                toggleDriveTracking(false)
                ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                        googleApiClient,
                        activityDetectionPendingIntent
                ).setResultCallback(this@MainActivity)
                savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false)
            }
            setTrackingSwitch(trackingSwitch)
        })
        return true
    }

    override fun invalidateOptionsMenu() {
        super.supportInvalidateOptionsMenu()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        savePreferencesBoolean(Constants.KEY_SHARED_PREF_GPS_STATE, gpsEnabled)
        getDriveTrackingStatus()
        return super.onPrepareOptionsMenu(menu)
    }

    @Synchronized
    protected fun buildGoogleApiClient() {
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
        // Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    override fun onConnectionSuspended(cause: Int) {
        googleApiClient!!.connect()
    }

    override fun onResult(status: Status) {
        if (status.isSuccess) {
            val requestingUpdates = !updatesRequestedState
            updatesRequestedState = requestingUpdates

        } else {
            Toast.makeText(
                    this,
                    getString(R.string.no_gps_data),
                    Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getDriveTrackingStatus() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        isTracking = sharedPreferences.getBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false)
    }

    private fun savePreferencesBoolean(key: String, value: Boolean?) {

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value!!)
        editor.apply()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    private fun setTrackingSwitch(trackingSwitch: SwitchCompat) {
        getDriveTrackingStatus()

        if (isTracking) {
            trackingSwitch.isChecked = true
        } else {
            trackingSwitch.isChecked = false
        }
    }

    private fun toggleDriveTracking(isTracking: Boolean) {
        if (isTracking) {
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                    googleApiClient,
                    Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                    activityDetectionPendingIntent
            ).setResultCallback(this@MainActivity)
            Toast.makeText(this@MainActivity, getString(R.string.drive_tracking_on),
                    Toast.LENGTH_SHORT).show()
        } else {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                    googleApiClient,
                    activityDetectionPendingIntent
            ).setResultCallback(this@MainActivity)
            Toast.makeText(this@MainActivity, getString(R.string.drive_tracking_off),
                    Toast.LENGTH_SHORT).show()
        }
        savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, isTracking)

    }

    private fun hasLocationPermission(): Boolean {
        val permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)

        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)

        if (id == R.id.action_settings && navController.currentDestination!!.id != R.id.energySettingsFragment) {
            navController.navigate(R.id.energySettingsFragment)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}