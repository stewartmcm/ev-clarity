package com.stewartmcm.android.evclarity.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
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
import com.google.android.gms.location.LocationServices;
import com.stewartmcm.android.evclarity.DeleteTripTask;
import com.stewartmcm.android.evclarity.R;
import com.stewartmcm.android.evclarity.SumLoggedTripsTask;
import com.stewartmcm.android.evclarity.TripAdapter;
import com.stewartmcm.android.evclarity.data.Contract;
import com.stewartmcm.android.evclarity.services.DetectedActivitiesIntentService;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.stewartmcm.android.evclarity.R.id.error;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    @BindView(error)
    TextView errorTextView;

    @BindView(R.id.recyclerview_triplog_empty)
    TextView noTripsYetTextView;

    private int tripPosition;
    private boolean isChecked;
    private boolean mGpsEnabled;
    private TripAdapter mTripAdapter;
    private GoogleApiClient mGoogleApiClient;

    private static final int TRIP_LOADER = 0;
    private static final String[] TRIP_COLUMNS = {
            // A lot of the columns below won't be used actively in the app's current state, however
            // a lot of these columns will be necessary planned features
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
    public static final int COL_DATE = 1;
    public static final int COL_TRIP_MILES = 7;
    public static final int COL_TRIP_SAVINGS = 8;
    //    static final int COL_ID = 0;
    //    static final int COL_TIME = 2;
    //    static final int COL_ORIGIN_LAT = 3;
    //    static final int COL_ORIGIN_LONG = 4;
    //    static final int COL_DEST_LAT = 5;
    //    static final int COL_DEST_LONG = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar();
        loadSharedPreferences();
        buildGoogleApiClient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadSharedPreferences();
        new SumLoggedTripsTask(this).execute();
        ButterKnife.bind(this);
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mTripAdapter = new TripAdapter(this, null, noTripsYetTextView);

        RecyclerView tripRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);

        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(tripRecyclerView.getContext(), mLayoutManager.getOrientation());

        tripRecyclerView.setLayoutManager(mLayoutManager);
        tripRecyclerView.addItemDecoration(dividerItemDecoration);
        tripRecyclerView.setAdapter(mTripAdapter);

        getSupportLoaderManager().initLoader(TRIP_LOADER, null, this);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                tripPosition = viewHolder.getAdapterPosition();
                new DeleteTripTask(getApplicationContext()).execute(tripPosition);
                new SumLoggedTripsTask(MainActivity.this).execute();
                int newTripArraySize = mTripAdapter.removeTrip(tripPosition);
                mTripAdapter.notifyItemRemoved(tripPosition);
                mTripAdapter.notifyItemRangeChanged(tripPosition, newTripArraySize);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(tripRecyclerView);
        invalidateOptionsMenu();
    }

    private void setSupportActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        getSupportActionBar().setElevation(0f);
    }

    @Override
    public void invalidateOptionsMenu() {
        super.supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        savePreferencesBoolean(Constants.KEY_SHARED_PREF_GPS_STATE, mGpsEnabled);
        loadSharedPreferences();
        return super.onPrepareOptionsMenu(menu);
    }

    private void savePreferencesBoolean(String key, Boolean value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED && !ActivityCompat.shouldShowRequestPermissionRationale (this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this,
                    permissions,
                    Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    public void onResult(Status status) {
        if (status.isSuccess()) {
            boolean requestingUpdates = !getUpdatesRequestedState();
            setUpdatesRequestedState(requestingUpdates);

        } else {
            Toast.makeText(
                    this,
                    getString(R.string.no_gps_data),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private SharedPreferences getSharedPreferencesInstance() {
        return getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
    }

    private boolean getUpdatesRequestedState() {
        return getSharedPreferencesInstance()
                .getBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, false);
    }

    private void setUpdatesRequestedState(boolean requestingUpdates) {
        getSharedPreferencesInstance()
                .edit()
                .putBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, requestingUpdates)
                .apply();
    }

    private void loadSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        isChecked = sharedPreferences.getBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
//        mTripAdapter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    protected void setTrackingSwitch(SwitchCompat trackingSwitch) {
        loadSharedPreferences();

        if (isChecked) {
            trackingSwitch.setChecked(true);
        } else {
            trackingSwitch.setChecked(false);
        }
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
            errorTextView.setVisibility(View.GONE);
            noTripsYetTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mTripAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        final SwitchCompat trackingSwitch = (SwitchCompat) menu.findItem(R.id.myswitch).getActionView()
                .findViewById(R.id.switchForActionBar);

        setTrackingSwitch(trackingSwitch);

        trackingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    mGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

                    if (!hasLocationPermission()) {
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog.setTitle(getString(R.string.location_permission_alert_header));
                        alertDialog.setMessage(getString(R.string.location_permission_alert));
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dialog_ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    } else if (!mGpsEnabled) {

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this,
                                R.style.MyAlertDialogStyle);
                        alertDialog.setTitle(R.string.location_not_avail_alert_header);
                        alertDialog.setMessage(R.string.location_not_avail_alert_message);
                        alertDialog.setPositiveButton(R.string.got_to_settings_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(myIntent);
                                //get gps
                            }
                        });
                        alertDialog.show();
                    } else if (!mGoogleApiClient.isConnected()) {
                        savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false);
                        Toast.makeText(MainActivity.this, getString(R.string.not_connected),
                                Toast.LENGTH_SHORT).show();

                    } else {
                        setDriveTracking(true);
                    }

                } else {

                    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    mGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

                    if (!mGoogleApiClient.isConnected()) {
                        Toast.makeText(MainActivity.this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
                        return;
                    } else if (!mGpsEnabled) {
                        savePreferencesBoolean(Constants.KEY_SHARED_PREF_GPS_STATE, false);

                    }
                    // Remove all activity updates for the PendingIntent that was used to request activity
                    // updates.
                    setDriveTracking(false);
                    ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                            mGoogleApiClient,
                            getActivityDetectionPendingIntent()
                    ).setResultCallback(MainActivity.this);
                    savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, false);
                }
                setTrackingSwitch(trackingSwitch);
            }
        });

        return true;
    }

    private void setDriveTracking(boolean isTracking) {
        if (isTracking) {
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                    mGoogleApiClient,
                    Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                    getActivityDetectionPendingIntent()
            ).setResultCallback(MainActivity.this);
            Toast.makeText(MainActivity.this, getString(R.string.drive_tracking_on),
                    Toast.LENGTH_SHORT).show();
        } else {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                    mGoogleApiClient,
                    getActivityDetectionPendingIntent()
            ).setResultCallback(MainActivity.this);
            Toast.makeText(MainActivity.this, getString(R.string.drive_tracking_off),
                    Toast.LENGTH_SHORT).show();
        }
        savePreferencesBoolean(Constants.KEY_SHARED_PREF_DRIVE_TRACKING, isTracking);

    }

    private boolean hasLocationPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, EnergySettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }
}