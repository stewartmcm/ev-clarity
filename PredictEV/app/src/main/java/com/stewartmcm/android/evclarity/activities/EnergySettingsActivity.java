package com.stewartmcm.android.evclarity.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.stewartmcm.android.evclarity.R;
import com.stewartmcm.android.evclarity.models.Utility;
import com.stewartmcm.android.evclarity.models.UtilityArray;
import com.stewartmcm.android.evclarity.services.UtilityRateAPIService;

import java.util.ArrayList;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EnergySettingsActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    protected static final String TAG = Constants.ENERGY_SETTINGS_ACTIVITY_TAG;
    private String utilityRateString;
    private String gasPriceString;
    private String currentMPGString;
    private String utilityName;
    private String latString;
    private String lonString;
    private TextView currentUtilityTextView;
    private TextView utilityRateTextView;
    private EditText gasPriceEditText;
    private EditText mpgEditText;
    private Location mLastLocation;
    public Location mCurrentLocation;
    public GoogleApiClient mGoogleApiClient;
    private double utilityRate;
    private ArrayList<Utility> utilities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            utilityName = savedInstanceState.getString(Constants.KEY_SHARED_PREF_UTIL_NAME);
//            Log.i(TAG, "onCreate: utilityName: " + utilityName);
            utilityRateString = savedInstanceState.getString(Constants.KEY_SHARED_PREF_UTIL_RATE);
//            Log.i(TAG, "onCreate: utilityRate: " + utilityRateString);

            initLayoutElements();

            currentUtilityTextView.setText(utilityName);
            utilityRateTextView.setText(R.string.$ + utilityRateString + R.string.kWh);

        } else {

            initLayoutElements();
            loadSavedPreferences();
        }

        // TODO: add logic to display list of utilities if user's lat/lon returns multiple utility providers
        utilities = new ArrayList<>();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                findUtilities();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    private void loadSavedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        utilityName = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_NAME,
                getString(R.string.set_electricity_provider_cue));
        utilityRateString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_RATE,
                getString(R.string.default_electricity_rate));
        gasPriceString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_GAS_PRICE,
                getString(R.string.default_gas_price));
        currentMPGString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG,
                getString(R.string.default_mpg));

        currentUtilityTextView.setText(utilityName);
        utilityRateTextView.setText(utilityRateString);
        gasPriceEditText.setText(gasPriceString);
        mpgEditText.setText(currentMPGString);
    }

    private void initLayoutElements() {

        currentUtilityTextView = (TextView) findViewById(R.id.current_utility_text_view);
        utilityRateTextView = (TextView) findViewById(R.id.utility_rate_text_view);
        gasPriceEditText = (EditText) findViewById(R.id.gas_price_edit_text);
        mpgEditText = (EditText) findViewById(R.id.mpg_edit_text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        return true;
    }

    public void findUtilities() {

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            if (mLastLocation != null) {
                latString = String.valueOf(mLastLocation.getLatitude());
                lonString = String.valueOf(mLastLocation.getLongitude());
            }

        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                AlertDialog alertDialog = new AlertDialog.Builder(EnergySettingsActivity.this).create();
                alertDialog.setTitle("Permission Needed");
                alertDialog.setMessage("EV Clarity uses your device's location to calculate your potential savings. " +
                        "You can grant EV Clarity permission to track your location in your device's settings.");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            } else {

                // No explanation needed, we can request the permission.
                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                ActivityCompat.requestPermissions(this,
                        permissions,
                        Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            }
        }

        Retrofit retrofit = new Retrofit.Builder().baseUrl(getString(R.string.nrel_api_path))
                .addConverterFactory(GsonConverterFactory.create()).build();
        UtilityRateAPIService mService = retrofit.create(UtilityRateAPIService.class);

        String API_KEY = "vIp4VQcx5zLfEr7Mi61aGd2vjIDpBpIqQRRQCoWt";

        Call<UtilityArray> call = null;

        call = mService.getElectricityProviders(API_KEY, latString, lonString);

        if (call != null) {
            if (latString != null && lonString != null){
                call.enqueue(new Callback<UtilityArray>() {
                    //                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onResponse(Call<UtilityArray> call, Response<UtilityArray> response) {
                        Utility[] utilityArray = response.body().getOutputs().getUtilities();
                        ArrayList<Utility> localUtilities = new ArrayList<>(Arrays.asList(utilityArray));
                        utilities.addAll(localUtilities);

                        utilityName = utilities.get(0).getUtilityName();
                        currentUtilityTextView.setText(utilityName);

                        utilityRate = response.body().getOutputs().getResidentialRate();
                        utilityRateString = String.valueOf(utilityRate);
                        utilityRateTextView.setText(utilityRateString);
                        Toast.makeText(getBaseContext(), getString(R.string.electricity_provider_set),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Call<UtilityArray> call, Throwable t) {

                    }

                });
            }

        } else {
            Toast.makeText(this, getString(R.string.no_network_connection),
                    Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(Constants.KEY_SHARED_PREF_UTIL_NAME, utilityName);
        savedInstanceState.putString(Constants.KEY_SHARED_PREF_UTIL_RATE, utilityRateString);
    }

    private void savePreferencesString(String key, String value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        Log.i(TAG, "onPause called");
        savePreferencesString(Constants.KEY_SHARED_PREF_UTIL_NAME, utilityName);
//        Log.i(TAG, "onDestroy: utitlityName: " + utilityName);
        savePreferencesString(Constants.KEY_SHARED_PREF_UTIL_RATE, utilityRateString);

        gasPriceString = gasPriceEditText.getText().toString();
        currentMPGString = mpgEditText.getText().toString();
        savePreferencesString(Constants.KEY_SHARED_PREF_GAS_PRICE, gasPriceString);
        savePreferencesString(Constants.KEY_SHARED_PREF_CURRENT_MPG, currentMPGString);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
//        Log.i(TAG, "onStop called");
    }

    @Override
    protected void onDestroy() {
//        Log.i(TAG, "onDestroy called");
        super.onDestroy();
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * ActivityRecognition API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
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
//                Log.i(TAG, "latString: " + latString);
                lonString = String.valueOf(mLastLocation.getLongitude());
//                Log.i(TAG, "lonString: " + lonString);
            }

        } else {
            //TODO:Test removing this check, just requesting the permission.
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
        mGoogleApiClient.connect();
        // Log.i(TAG, "Connection suspended");
    }

}
