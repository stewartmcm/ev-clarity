package com.stewartmcm.android.evclarity.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;
import com.stewartmcm.android.evclarity.R;
import com.stewartmcm.android.evclarity.TripAdapter;
import com.stewartmcm.android.evclarity.data.Contract;
import com.stewartmcm.android.evclarity.data.PredictEvDatabaseHelper;
import com.stewartmcm.android.evclarity.services.DetectedActivitiesIntentService;
import com.stewartmcm.android.evclarity.services.OdometerService;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Main2Activity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<Status> {

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.recycler_view)
    RecyclerView tripRecyclerView;

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.error)
    TextView error;


    private int mPosition = RecyclerView.NO_POSITION;
    protected static final String TAG = "Main2Activity";

    private TripAdapter mTripAdapter;
    private SwitchCompat trackingSwitch;
    private int mChoiceMode;
    private String latString;
    private String lonString;
    private boolean driving;
    private boolean isChecked;
    public GoogleApiClient mGoogleApiClient;
    private ArrayList<DetectedActivity> mDetectedActivities;
    private OdometerService odometer;
    private boolean bound = false;
    private Location mLastLocation;
    public Location mCurrentLocation;
    private double currentMPG;
    private double sumLoggedTripsDouble;
    private String currentMPGString;
    private String utilityRateString;
    private String gasPriceString;
    private TextView monthlySavingsTextView;
    private TextView totalMileageTextView;
    SQLiteDatabase db;
    private Cursor sumCursor;
    private Cursor recentTripCursor;
    private double recentTrip;
    private TextView recentTripTextView;
    private double utilityRate;
    private double gasPrice;

    private static final int TRIP_LOADER = 0;
    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] TRIP_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            Contract.Trip._ID,
            Contract.Trip.COLUMN_DATE,
            Contract.Trip.COLUMN_TIME,
            Contract.Trip.COLUMN_ORIGIN_LAT,
            Contract.Trip.COLUMN_ORIGIN_LONG,
            Contract.Trip.COLUMN_DEST_LAT,
            Contract.Trip.COLUMN_DEST_LONG,
            Contract.Trip.COLUMN_TRIP_MILES,
            Contract.Trip.COLUMN_TRIP_SAVINGS
    };

    // These indices are tied to TRIP_COLUMNS.  If TRIP_COLUMNS changes, these
    // must change.
    static final int COL_ID = 0;
    public static final int COL_DATE = 1;
    static final int COL_TIME = 2;
    static final int COL_ORIGIN_LAT = 3;
    static final int COL_ORIGIN_LONG = 4;
    static final int COL_DEST_LAT = 5;
    static final int COL_DEST_LONG = 6;
    public static final int COL_TRIP_MILES = 7;
    public static final int COL_TRIP_SAVINGS = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(0f);

        ButterKnife.bind(this);

        View emptyView = this.findViewById(R.id.recyclerview_triplog_empty);

        Log.i(TAG, "onCreate called");

        loadSavedPreferences();

        monthlySavingsTextView = (TextView) findViewById(R.id.savings_text_view);

        new SumLoggedTripsTask().execute(monthlySavingsTextView);

        // Reuse the value of mDetectedActivities from the bundle if possible. This maintains state
        // across device orientation changes. If mDetectedActivities is not stored in the bundle,
        // populate it with DetectedActivity objects whose confidence is set to 0. Doing this
        // ensures that the bar graphs for only only the most recently detected activities are
        // filled in.
        if (savedInstanceState != null && savedInstanceState.containsKey(
                Constants.DETECTED_ACTIVITIES)) {
            mDetectedActivities = (ArrayList<DetectedActivity>) savedInstanceState.getSerializable(
                    Constants.DETECTED_ACTIVITIES);
        } else {
            mDetectedActivities = new ArrayList<DetectedActivity>();

            // Set the confidence level of each monitored activity to zero.
            for (int i = 0; i < Constants.MONITORED_ACTIVITIES.length; i++) {
                mDetectedActivities.add(new DetectedActivity(Constants.MONITORED_ACTIVITIES[i], 0));
            }
        }

        mTripAdapter = new TripAdapter(this, new TripAdapter.TripAdapterOnClickHandler() {

            @Override
            public void onClick(String dateTime, TripAdapter.TripAdapterViewHolder viewHolder) {
                mPosition = viewHolder.getAdapterPosition();
                onItemSelected(Contract.Trip.makeUriForTrip(dateTime),
                        viewHolder
                );
            }
        }, emptyView, mChoiceMode);

        tripRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tripRecyclerView.setAdapter(mTripAdapter);

        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null) {
            mTripAdapter.onRestoreInstanceState(savedInstanceState);
        }

        getSupportLoaderManager().initLoader(TRIP_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
//                String dateTime = mTripAdapter.getTripAtPosition(viewHolder.getAdapterPosition());
                //TODO: implement remove trip method
//                PrefUtils.removeStock(Main2Activity.this, symbol);
//                getContentResolver().delete(Contract.Trip.makeUriForTrip(symbol), null, null);
            }
        }).attachToRecyclerView(tripRecyclerView);

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();
    }

    private void savePreferencesBoolean(String key, Boolean value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart called");

        loadSavedPreferences();
//        setTrackingSwitch();

        new SumLoggedTripsTask().execute(monthlySavingsTextView);
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume called");

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Connected to GoogleApiClient");
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (mLastLocation != null) {
                latString = String.valueOf(mLastLocation.getLatitude());
                Log.i(TAG, "latString: " + latString);
                lonString = String.valueOf(mLastLocation.getLongitude());
                Log.i(TAG, "lonString: " + lonString);
            }

        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                ActivityCompat.requestPermissions(this,
                        permissions,
                        Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION);

            }
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * ActivityRecognition API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Connected to GoogleApiClient");
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (mLastLocation != null) {
                latString = String.valueOf(mLastLocation.getLatitude());
                Log.i(TAG, "latString: " + latString);
                lonString = String.valueOf(mLastLocation.getLongitude());
                Log.i(TAG, "lonString: " + lonString);
            }

        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                ActivityCompat.requestPermissions(this,
                        permissions,
                        Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION);

            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    /**
     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes
     * available. Either method can complete successfully or with an error.
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Toggle the status of activity updates requested, and save in shared preferences.
            boolean requestingUpdates = !getUpdatesRequestedState();
            setUpdatesRequestedState(requestingUpdates);

        } else {
            Log.e(TAG, "Error adding or removing activity detection: " + status.getStatusMessage());
            Toast.makeText(
                    this,
                    "Unable to turn on Activity Recognition",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Retrieves a SharedPreference object used to store or read values in this app. If a
     * preferences file passed as the first argument to {@link #getSharedPreferences}
     * does not exist, it is created when {@link SharedPreferences.Editor} is used to commit
     * data.
     */
    private SharedPreferences getSharedPreferencesInstance() {
        return getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
    }

    /**
     * Retrieves the boolean from SharedPreferences that tracks whether we are requesting activity
     * updates.
     */
    private boolean getUpdatesRequestedState() {
        return getSharedPreferencesInstance()
                .getBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, false);
    }

    /**
     * Sets the boolean in SharedPreferences that tracks whether we are requesting activity
     * updates.
     */
    private void setUpdatesRequestedState(boolean requestingUpdates) {
        getSharedPreferencesInstance()
                .edit()
                .putBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, requestingUpdates)
                .commit();
    }

    /**
     * loads utility and gas settings that were set in EnergySettingsActivity;
     * stores the state of the tracking switch
     */
    private void loadSavedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        utilityRateString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_RATE, "0.0000");
        gasPriceString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_GAS_PRICE, "0");
        currentMPGString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG, "0.0");
        isChecked = sharedPreferences.getBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false);
        Log.i(TAG, "loadSavedPreferences: isChecked: " + isChecked);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        mTripAdapter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    protected void setTrackingSwitch(SwitchCompat trackingSwitch) {
        //TODO: make sure isChecked is set up properly
        if (isChecked) {
            trackingSwitch.setChecked(true);
        } else {
            trackingSwitch.setChecked(false);

        }
    }

    //sums all logged trips asynchronously when executed[onCreate]
    private class SumLoggedTripsTask extends AsyncTask<TextView, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Boolean doInBackground(TextView... params) {
            SQLiteOpenHelper mHelper = new PredictEvDatabaseHelper(Main2Activity.this);

            try {
                db = mHelper.getReadableDatabase();
                sumCursor = db.query("TRIP", new String[]{"SUM(TRIP_MILES) AS sum"},
                        null, null, null, null, null);

                recentTripCursor = db.query("TRIP", new String[]{PredictEvDatabaseHelper.COL_TRIP_MILES},
                        null, null, null, null, null);

                return true;

            } catch (SQLiteException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            sumCursor.moveToFirst();
            if (success) {
                // TODO: update comment describing code below
                sumLoggedTripsDouble = sumCursor.getDouble(0);
                double savings = calcSavings(sumLoggedTripsDouble);

                monthlySavingsTextView = (TextView) findViewById(R.id.savings_text_view);
                monthlySavingsTextView.setText("$" + String.format("%.2f", savings));

                totalMileageTextView = (TextView) findViewById(R.id.total_mileage_textview);
                totalMileageTextView.setText(String.format("%.0f", sumLoggedTripsDouble));

                Log.i(TAG, "onPostExecute: savingsText: " + String.format("%.2f", calcSavings(sumLoggedTripsDouble)));


            } else {

                Toast toast = Toast.makeText(Main2Activity.this, "Database unavailable", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    private class RecentTripTask extends AsyncTask<TextView, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Boolean doInBackground(TextView... params) {
            SQLiteOpenHelper mHelper = new PredictEvDatabaseHelper(Main2Activity.this);

            try {
                db = mHelper.getReadableDatabase();
                recentTripCursor = db.query("TRIP", new String[]{PredictEvDatabaseHelper.COL_TRIP_MILES},
                        null, null, null, null, null);

                return true;

            } catch (SQLiteException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            if (success) {

                recentTripCursor.moveToLast();
                recentTrip = recentTripCursor.getDouble(0);
                recentTripTextView = (TextView) findViewById(R.id.recent_trip_textview);
                recentTripTextView.setText(String.format("%.2f", recentTrip));

                Log.i(TAG, "onPostExecute: Recent Trip: " + String.format("%.2f", recentTrip));


            } else {

                Toast toast = Toast.makeText(Main2Activity.this, "Database unavailable", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    private double calcSavings(double mileageDouble) {

        double savings;
        utilityRate = Double.parseDouble(utilityRateString);

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

        Log.i(TAG, "calcSavings: gasPrice: " + gasPrice);


        if (utilityRate != 0.0) {
            Log.i(TAG, "calcSavings: utilityRateString: " + utilityRateString);

            // .3 is Nissan Leaf's kWh per mile driven (EV equivalent of mpg)
            savings = mileageDouble * ((gasPrice / currentMPG) - (.3 * utilityRate));

            return savings;
        }
        return 0.00;

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new CursorLoader(this,
                Contract.Trip.uri,
                TRIP_COLUMNS,
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mTripAdapter.swapCursor(data);

        if (data.getCount() != 0) {
            error.setVisibility(View.GONE);
        }
//        mTripAdapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mTripAdapter.swapCursor(null);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        trackingSwitch = (SwitchCompat) menu.findItem(R.id.myswitch).getActionView().findViewById(R.id.switchForActionBar);
        setTrackingSwitch(trackingSwitch);

        trackingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    if (!mGoogleApiClient.isConnected()) {
                        Toast.makeText(Main2Activity.this, getString(R.string.not_connected),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                            mGoogleApiClient,
                            Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                            getActivityDetectionPendingIntent()
                    ).setResultCallback(Main2Activity.this);
                    isChecked = true;
                    savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, isChecked);
                    Toast.makeText(Main2Activity.this, "tracking",
                            Toast.LENGTH_SHORT).show();

                } else {

                    if (!mGoogleApiClient.isConnected()) {
                        Toast.makeText(Main2Activity.this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Remove all activity updates for the PendingIntent that was used to request activity
                    // updates.
                    ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                            mGoogleApiClient,
                            getActivityDetectionPendingIntent()
                    ).setResultCallback(Main2Activity.this);
                    isChecked = false;
                    savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, isChecked);
                    Toast.makeText(Main2Activity.this, "not tracking",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(Main2Activity.this, EnergySettingsActivity.class);
            intent.putExtra(Constants.EXTRA_USER_LAT, latString);
            intent.putExtra(Constants.EXTRA_USER_LON, lonString);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause called");
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop called");
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy called");
    }

    //TODO: Create TripDetailActivity with map of individual trip
    //    @Override
    public void onItemSelected(Uri contentUri, TripAdapter.TripAdapterViewHolder vh) {

//        Intent intent = new Intent(this, TripDetailActivity.class)
//                .setData(contentUri);

//        ActivityOptionsCompat activityOptions =
//                ActivityOptionsCompat.makeSceneTransitionAnimation(this,
//                        new Pair<View, String>(vh.mIconView, getString(R.string.detail_icon_transition_name)));
//        startActivity(intent);

    }
}
