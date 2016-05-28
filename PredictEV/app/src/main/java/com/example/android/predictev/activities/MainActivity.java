package com.example.android.predictev.activities;import android.Manifest;import android.app.PendingIntent;import android.content.Intent;import android.content.SharedPreferences;import android.content.pm.PackageManager;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import android.database.sqlite.SQLiteException;import android.database.sqlite.SQLiteOpenHelper;import android.location.Location;import android.os.AsyncTask;import android.os.Bundle;import android.preference.PreferenceManager;import android.support.design.widget.NavigationView;import android.support.v4.app.ActivityCompat;import android.support.v4.content.ContextCompat;import android.support.v4.view.GravityCompat;import android.support.v4.widget.DrawerLayout;import android.support.v7.app.ActionBarDrawerToggle;import android.support.v7.app.AppCompatActivity;import android.support.v7.widget.Toolbar;import android.util.Log;import android.view.Menu;import android.view.MenuItem;import android.widget.CompoundButton;import android.widget.ListView;import android.widget.Switch;import android.widget.TextView;import android.widget.Toast;import com.example.android.predictev.PredictEvDatabaseHelper;import com.example.android.predictev.R;import com.example.android.predictev.services.DetectedActivitiesIntentService;import com.example.android.predictev.services.OdometerService;import com.google.android.gms.common.ConnectionResult;import com.google.android.gms.common.api.GoogleApiClient;import com.google.android.gms.common.api.ResultCallback;import com.google.android.gms.common.api.Status;import com.google.android.gms.location.ActivityRecognition;import com.google.android.gms.location.DetectedActivity;import com.google.android.gms.location.LocationServices;import java.util.ArrayList;public class MainActivity extends AppCompatActivity        implements NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.ConnectionCallbacks,        GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {    protected static final String TAG = "MainActivity";    public Switch trackingSwitch;    private boolean driving;    private boolean isChecked;    public GoogleApiClient mGoogleApiClient;    private ArrayList<DetectedActivity> mDetectedActivities;    private OdometerService odometer;    private boolean bound = false;    private Location mLastLocation;    public Location mCurrentLocation;    private String latString;    private String lonString;    private long timeStamp;    SQLiteDatabase db;    private Cursor cursor;    private ListView loggedTripsListView;    private String utilityName;    private double utilityRate;    private double gasPrice;    public double tripDistance;    private double monthlySavings;    private double currentMPG;    private String currentMPGString;    private String utilityRateString;    private String gasPriceString;    private TextView monthlySavingsTextView;    private TextView switchStatusText;    private double finalTripDistance;    public PredictEvDatabaseHelper mHelper;    @Override    protected void onCreate(Bundle savedInstanceState) {        super.onCreate(savedInstanceState);        setContentView(R.layout.activity_main);        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);        setSupportActionBar(toolbar);        Log.i(TAG, "onCreate called");        switchStatusText = (TextView) findViewById(R.id.on_off_text_view);        trackingSwitch = (Switch) findViewById(R.id.tracking_switch);        loadSavedPreferences();        setTrackingSwitch();        new SumLoggedTripsTask().execute(monthlySavingsTextView);        trackingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {            @Override            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {                if (isChecked) {                    if (!mGoogleApiClient.isConnected()) {                        Toast.makeText(MainActivity.this, getString(R.string.not_connected),                                Toast.LENGTH_SHORT).show();                        return;                    }                    ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(                            mGoogleApiClient,                            Constants.DETECTION_INTERVAL_IN_MILLISECONDS,                            getActivityDetectionPendingIntent()                    ).setResultCallback(MainActivity.this);                    isChecked = true;                    savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, isChecked);                    switchStatusText.setText("Drive Tracking ON");                } else {                    if (!mGoogleApiClient.isConnected()) {                        Toast.makeText(MainActivity.this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();                        return;                    }                    // Remove all activity updates for the PendingIntent that was used to request activity                    // updates.                    ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(                            mGoogleApiClient,                            getActivityDetectionPendingIntent()                    ).setResultCallback(MainActivity.this);                    isChecked = false;                    savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, isChecked);                    switchStatusText.setText("Drive Tracking OFF");                }            }            });        // Reuse the value of mDetectedActivities from the bundle if possible. This maintains state        // across device orientation changes. If mDetectedActivities is not stored in the bundle,        // populate it with DetectedActivity objects whose confidence is set to 0. Doing this        // ensures that the bar graphs for only only the most recently detected activities are        // filled in.        if (savedInstanceState != null && savedInstanceState.containsKey(                Constants.DETECTED_ACTIVITIES)) {            mDetectedActivities = (ArrayList<DetectedActivity>) savedInstanceState.getSerializable(                    Constants.DETECTED_ACTIVITIES);        } else {            mDetectedActivities = new ArrayList<DetectedActivity>();            // Set the confidence level of each monitored activity to zero.            for (int i = 0; i < Constants.MONITORED_ACTIVITIES.length; i++) {                mDetectedActivities.add(new DetectedActivity(Constants.MONITORED_ACTIVITIES[i], 0));            }        }        // Kick off the request to build GoogleApiClient.        buildGoogleApiClient();        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);        drawer.setDrawerListener(toggle);        toggle.syncState();        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);        navigationView.setNavigationItemSelectedListener(this);    }    private void savePreferencesBoolean(String key, Boolean value) {        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);        SharedPreferences.Editor editor = sharedPreferences.edit();        editor.putBoolean(key, value);        editor.commit();    }    @Override    protected void onStart() {        super.onStart();        Log.i(TAG, "onStart called");        loadSavedPreferences();        setTrackingSwitch();        new SumLoggedTripsTask().execute(monthlySavingsTextView);        mGoogleApiClient.connect();    }    /**     * loads utility and gas settings that were set in EnergySettingsActivity;     * stores the state of the tracking switch     */    private void loadSavedPreferences() {        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);        utilityName = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_NAME, "default name");        utilityRateString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_RATE, "0.0000");        gasPriceString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_GAS_PRICE, "0");        currentMPGString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG,"0.0");        isChecked = sharedPreferences.getBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false);        Log.i(TAG, "loadSavedPreferences: isChecked: " + isChecked);    }    @Override    protected void onResume() {        super.onResume();        Log.i(TAG, "onResume called");        int permissionCheck = ContextCompat.checkSelfPermission(this,                Manifest.permission.ACCESS_FINE_LOCATION);        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {            Log.i(TAG, "Connected to GoogleApiClient");            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(                    mGoogleApiClient);            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);            if (mLastLocation != null) {                latString = String.valueOf(mLastLocation.getLatitude());                Log.i(TAG, "latString: " + latString);                lonString = String.valueOf(mLastLocation.getLongitude());                Log.i(TAG, "lonString: " + lonString);            }        } else {            if (ActivityCompat.shouldShowRequestPermissionRationale(this,                    Manifest.permission.ACCESS_FINE_LOCATION)) {                // Show an explanation to the user *asynchronously* -- don't block                // this thread waiting for the user's response! After the user                // sees the explanation, try again to request the permission.            } else {                // No explanation needed, we can request the permission.                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};                ActivityCompat.requestPermissions(this,                        permissions,                        Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION);            }        }    }    /**     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the     * ActivityRecognition API.     */    protected synchronized void buildGoogleApiClient() {        mGoogleApiClient = new GoogleApiClient.Builder(this)                .addConnectionCallbacks(this)                .addOnConnectionFailedListener(this)                .addApi(ActivityRecognition.API)                .addApi(LocationServices.API)                .build();    }    protected void setTrackingSwitch() {        if (isChecked) {            trackingSwitch.setChecked(true);            switchStatusText.setText("Drive Tracking ON");        } else {            trackingSwitch.setChecked(false);            switchStatusText.setText("Drive Tracking OFF");        }    }    //sums all logged trips asynchronously when executed[onCreate]    private class SumLoggedTripsTask extends AsyncTask<TextView, Void, Boolean> {        @Override        protected void onPreExecute() {            super.onPreExecute();        }        @Override        protected Boolean doInBackground(TextView... params) {            SQLiteOpenHelper mHelper = new PredictEvDatabaseHelper(MainActivity.this);            try {                db = mHelper.getReadableDatabase();                cursor = db.query("TRIP", new String[] {"SUM(TRIP_MILES) AS sum"},                        null, null, null, null, null);                return true;            } catch (SQLiteException e) {                return false;            }        }        @Override        protected void onPostExecute(Boolean success) {            super.onPostExecute(success);            cursor.moveToFirst();            if (success) {                // TODO: update comment describing code below                double sumLoggedTripsDouble = cursor.getDouble(0);                double savings = calcSavings(sumLoggedTripsDouble);                Log.i(TAG, "onPostExecute: sumLoggedTrips: " + sumLoggedTripsDouble);                Log.i(TAG, "onPostExecute: savings: " + savings);                monthlySavingsTextView = (TextView) findViewById(R.id.savings_text_view);                monthlySavingsTextView.setText("$" + String.format("%.2f", savings));                Log.i(TAG, "onPostExecute: savingsText: " + String.format("%.2f", calcSavings(sumLoggedTripsDouble)) );            } else {                Toast toast = Toast.makeText(MainActivity.this, "Database unavailable", Toast.LENGTH_SHORT);                toast.show();            }        }    }    private double calcSavings(double mileageDouble) {        double savings;        utilityRate = Double.parseDouble(utilityRateString);        if (gasPriceString.isEmpty()) {            gasPrice = 0.0;        } else {            gasPrice = Double.parseDouble(gasPriceString);        }        if (currentMPGString.isEmpty()) {            currentMPG = 0.0;        } else {            currentMPG = Double.parseDouble(currentMPGString);        }        Log.i(TAG, "calcSavings: gasPrice: " + gasPrice);        if ( utilityRate != 0.0) {            Log.i(TAG, "calcSavings: utilityRateString: " + utilityRateString);            // .3 is Nissan Leaf's kWh per mile driven (EV equivalent of mpg)            savings = mileageDouble * ((gasPrice / currentMPG) - (.3 * utilityRate));            return savings;        }        return 0.00;    }    /**     * Runs when a GoogleApiClient object successfully connects.     */    @Override    public void onConnected(Bundle connectionHint) {        int permissionCheck = ContextCompat.checkSelfPermission(this,                Manifest.permission.ACCESS_FINE_LOCATION);        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {            Log.i(TAG, "Connected to GoogleApiClient");            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(                    mGoogleApiClient);            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);            if (mLastLocation != null) {                latString = String.valueOf(mLastLocation.getLatitude());                Log.i(TAG, "latString: " + latString);                lonString = String.valueOf(mLastLocation.getLongitude());                Log.i(TAG, "lonString: " + lonString);            }        } else {            if (ActivityCompat.shouldShowRequestPermissionRationale(this,                    Manifest.permission.ACCESS_FINE_LOCATION)) {                // Show an explanation to the user *asynchronously* -- don't block                // this thread waiting for the user's response! After the user                // sees the explanation, try again to request the permission.            } else {                // No explanation needed, we can request the permission.                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};                ActivityCompat.requestPermissions(this,                        permissions,                        Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION);            }        }    }    @Override    public void onConnectionFailed(ConnectionResult result) {        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in        // onConnectionFailed.        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());    }    @Override    public void onConnectionSuspended(int cause) {        // The connection to Google Play services was lost for some reason. We call connect() to        // attempt to re-establish the connection.        Log.i(TAG, "Connection suspended");        mGoogleApiClient.connect();    }    /**     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes     * available. Either method can complete successfully or with an error.     *     * @param status The Status returned through a PendingIntent when requestActivityUpdates()     *               or removeActivityUpdates() are called.     */    public void onResult(Status status) {        if (status.isSuccess()) {            // Toggle the status of activity updates requested, and save in shared preferences.            boolean requestingUpdates = !getUpdatesRequestedState();            setUpdatesRequestedState(requestingUpdates);        } else {            Log.e(TAG, "Error adding or removing activity detection: " + status.getStatusMessage());            Toast.makeText(                    this,                    "Unable to turn on Activity Recognition",                    Toast.LENGTH_SHORT            ).show();        }    }    /**     * Gets a PendingIntent to be sent for each activity detection.     */    private PendingIntent getActivityDetectionPendingIntent() {        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling        // requestActivityUpdates() and removeActivityUpdates().        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);    }    // TODO: fix shared preferences repetition    /**     * Retrieves a SharedPreference object used to store or read values in this app. If a     * preferences file passed as the first argument to {@link #getSharedPreferences}     * does not exist, it is created when {@link SharedPreferences.Editor} is used to commit     * data.     */    private SharedPreferences getSharedPreferencesInstance() {        return getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);    }    /**     * Retrieves the boolean from SharedPreferences that tracks whether we are requesting activity     * updates.     */    private boolean getUpdatesRequestedState() {        return getSharedPreferencesInstance()                .getBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, false);    }    /**     * Sets the boolean in SharedPreferences that tracks whether we are requesting activity     * updates.     */    private void setUpdatesRequestedState(boolean requestingUpdates) {        getSharedPreferencesInstance()                .edit()                .putBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, requestingUpdates)                .commit();    }    /**     * Stores the list of detected activities in the Bundle.     */    public void onSaveInstanceState(Bundle savedInstanceState) {        savedInstanceState.putSerializable(Constants.DETECTED_ACTIVITIES, mDetectedActivities);        super.onSaveInstanceState(savedInstanceState);    }    @Override    public void onBackPressed() {        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);        if (drawer.isDrawerOpen(GravityCompat.START)) {            drawer.closeDrawer(GravityCompat.START);        } else {            super.onBackPressed();        }    }    @Override    public boolean onCreateOptionsMenu(Menu menu) {        // Inflate the menu; this adds items to the action bar if it is present.        getMenuInflater().inflate(R.menu.main, menu);        return true;    }    @Override    public boolean onOptionsItemSelected(MenuItem item) {        // Handle action bar item clicks here. The action bar will        // automatically handle clicks on the Home/Up button, so long        // as you specify a parent activity in AndroidManifest.xml.        int id = item.getItemId();        //noinspection SimplifiableIfStatement        if (id == R.id.action_settings) {            Intent intent = new Intent(MainActivity.this, EnergySettingsActivity.class);            intent.putExtra(Constants.EXTRA_USER_LAT, latString);            intent.putExtra(Constants.EXTRA_USER_LON, lonString);            startActivity(intent);            return true;        }        return super.onOptionsItemSelected(item);    }    @SuppressWarnings("StatementWithEmptyBody")    @Override    public boolean onNavigationItemSelected(MenuItem item) {        // Handle navigation view item clicks here.        int id = item.getItemId();        if (id == R.id.nav_projected_savings) {            // Handle the camera action        } else if (id == R.id.nav_trips_logged) {            Intent intent = new Intent(MainActivity.this, TripsLoggedActivity.class);            startActivity(intent);        } else if (id == R.id.nav_vehicle_settings) {            Intent intent = new Intent(MainActivity.this, EnergySettingsActivity.class);            intent.putExtra(Constants.EXTRA_USER_LAT, latString);            intent.putExtra(Constants.EXTRA_USER_LON, lonString);            startActivity(intent);        }        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);        drawer.closeDrawer(GravityCompat.START);        return true;    }    @Override    protected void onPause() {        Log.i(TAG, "onPause called");        super.onPause();    }    @Override    protected void onStop() {        super.onStop();        Log.i(TAG, "onStop called");        mGoogleApiClient.disconnect();    }    @Override    protected void onDestroy() {        super.onDestroy();        Log.i(TAG, "onDestroy called");    }}