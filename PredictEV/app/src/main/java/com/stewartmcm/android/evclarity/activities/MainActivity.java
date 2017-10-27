package com.stewartmcm.android.evclarity.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
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
import com.stewartmcm.android.evclarity.R;
import com.stewartmcm.android.evclarity.TripAdapter;
import com.stewartmcm.android.evclarity.data.Contract;
import com.stewartmcm.android.evclarity.data.PredictEvDatabaseHelper;
import com.stewartmcm.android.evclarity.services.DetectedActivitiesIntentService;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.stewartmcm.android.evclarity.R.id.error;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    @BindView(R.id.savings_text_view)
    TextView monthlySavingsTextView;

    @BindView(R.id.total_mileage_textview)
    TextView totalMileageTextView;

    @BindView(error)
    TextView errorTextView;

    @BindView(R.id.recyclerview_triplog_empty)
    TextView noTripsYetTextView;

//    @BindView(R.id.recyclerview_triplog_empty)
//    View emptyView;

    private int mPosition = RecyclerView.NO_POSITION;
    private int mChoiceMode;
    private int tripPosition;
    private boolean isChecked;
    private String latString;
    private String lonString;
    private String currentMPGString;
    private String utilityRateString;
    private String gasPriceString;
    private boolean mGpsEnabled;
    private Cursor sumTripsCursor;
    private TripAdapter mTripAdapter;
    public GoogleApiClient mGoogleApiClient;
    public Location mCurrentLocation;
    public SQLiteDatabase db;

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

        ButterKnife.bind(this);
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
    protected void onResume() {
        super.onResume();

        mTripAdapter = new TripAdapter(this, new TripAdapter.TripAdapterOnClickHandler() {

            @Override
            public void onClick(String dateTime, TripAdapter.TripAdapterViewHolder viewHolder) {
                mPosition = viewHolder.getAdapterPosition();

                //TODO: implement to launch detail activity onClick of each item
                //onItemSelected(Contract.Trip.makeUriForTrip(dateTime),
                //viewHolder
                //);
            }
        }, noTripsYetTextView);

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
                new DeleteTripTask().execute(tripPosition);
                new SumLoggedTripsTask().execute();
                int newTripArraySize = mTripAdapter.removeTrip(tripPosition);
                mTripAdapter.notifyItemRemoved(tripPosition);
                mTripAdapter.notifyItemRangeChanged(tripPosition, newTripArraySize);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(tripRecyclerView);
        invalidateOptionsMenu();
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

    //deletes trip asynchronously when executed
    private class DeleteTripTask extends AsyncTask<Integer, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Integer... trips) {
            int tripNo = trips[0];

            //TODO: Test deleting line below and declaring db in try catch statement
            SQLiteDatabase db = null;
            PredictEvDatabaseHelper mHelper = PredictEvDatabaseHelper.getInstance(MainActivity.this);

            try {
                //TODO: try replacing query columns with constants
                db = mHelper.getWritableDatabase();
                Cursor deleteTripCursor = db.query(Constants.TRIP_TABLE_NAME, new String[]{"_id", "TRIP_MILES"},
                        null, null, null, null, null);

                if (deleteTripCursor.moveToPosition(tripNo)) {
                    String rowId = deleteTripCursor.getString(deleteTripCursor.getColumnIndex(PredictEvDatabaseHelper.COL_ID));

                    db.delete(Constants.TRIP_TABLE_NAME, PredictEvDatabaseHelper.COL_ID + "=?", new String[]{rowId});
                    deleteTripCursor.close();
                }

                return true;

            } catch (SQLiteException e) {
                Toast toast = Toast.makeText(MainActivity.this, getString(R.string.database_unavailable),
                        Toast.LENGTH_SHORT);
                toast.show();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

        }
    }

    private class SumLoggedTripsTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            SQLiteOpenHelper mHelper = new PredictEvDatabaseHelper(MainActivity.this);

            try {
                db = mHelper.getReadableDatabase();
                sumTripsCursor = db.query(Constants.TRIP_TABLE_NAME, new String[]{"SUM(TRIP_MILES) AS sum"},
                        null, null, null, null, null);
                return true;
            } catch (SQLiteException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            sumTripsCursor.moveToFirst();
            if (success) {
                double sumLoggedTripsDouble = sumTripsCursor.getDouble(0);
                double savings = calcSavings(sumLoggedTripsDouble);

                monthlySavingsTextView.setText(getString(R.string.$) + String.format(getString(R.string.savings_format), savings));
                totalMileageTextView.setText(String.format(getString(R.string.mileage_format),
                        sumLoggedTripsDouble));
                sumTripsCursor.close();

            } else {
                Toast toast = Toast.makeText(MainActivity.this, getString(R.string.database_unavailable),
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        }
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
        loadSharedPreferences();
        new SumLoggedTripsTask().execute();
        mGoogleApiClient.connect();
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

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasLocationPermission()) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (mCurrentLocation != null) {
                latString = String.valueOf(mCurrentLocation.getLatitude());
                lonString = String.valueOf(mCurrentLocation.getLongitude());
            }

        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
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
        utilityRateString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_RATE,
                getString(R.string.default_electricity_rate));
        gasPriceString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_GAS_PRICE,
                getString(R.string.default_gas_price));
        currentMPGString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG,
                getString(R.string.default_mpg));
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

    private double calcSavings(double mileageDouble) {
        double savings;
        double currentMPG;
        double gasPrice;
        double utilityRate = Double.parseDouble(utilityRateString);


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

        if (utilityRate != 0.0) {
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
            errorTextView.setVisibility(View.GONE);
            noTripsYetTextView.setVisibility(View.GONE);
        }
        //TODO: try uncommenting this when testing on swipe
//        mTripAdapter.setCursor(data);
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
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //TODO: Create TripDetailActivity with map of individual trip
    //    @Override
//    public void onItemSelected(Uri contentUri, TripAdapter.TripAdapterViewHolder vh) {

//        Intent intent = new Intent(this, TripDetailActivity.class)
//                .setData(contentUri);

//        ActivityOptionsCompat activityOptions =
//                ActivityOptionsCompat.makeSceneTransitionAnimation(this,
//                        new Pair<View, String>(vh.mIconView, getString(R.string.detail_icon_transition_name)));
//        startActivity(intent);
//    }
}