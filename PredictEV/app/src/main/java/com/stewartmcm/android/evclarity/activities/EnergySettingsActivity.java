package com.stewartmcm.android.evclarity.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EnergySettingsActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    @BindView(R.id.current_utility_text_view)
    TextView currentUtilityTextView;

    @BindView(R.id.utility_rate_text_view)
    TextView utilityRateTextView;

    @BindView(R.id.gas_price_edit_text)
    EditText gasPriceEditText;

    @BindView(R.id.mpg_edit_text)
    EditText mpgEditText;

    private String utilityRateString;
    private String gasPriceString;
    private String currentMPGString;
    private String utilityName;
    private String latString;
    private String lonString;
    private Location mLastLocation;
    private double utilityRate;
    private ArrayList<Utility> utilities;
    public Location mCurrentLocation;
    public GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            utilityName = savedInstanceState.getString(Constants.KEY_SHARED_PREF_UTIL_NAME);
            utilityRateString = savedInstanceState.getString(Constants.KEY_SHARED_PREF_UTIL_RATE);

            currentUtilityTextView.setText(utilityName);
            utilityRateTextView.setText(R.string.dollar_sign + utilityRateString + R.string.kWh);

        } else {
            loadSharedPreferences();
            currentUtilityTextView.setText(utilityName);
            utilityRateTextView.setText(utilityRateString);
            gasPriceEditText.setText(gasPriceString);
            mpgEditText.setText(currentMPGString);
        }

        // TODO: add logic to display list of utilities if user's lat/lon returns multiple utility providers
        utilities = new ArrayList<>();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    private void loadSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        utilityName = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_NAME,
                getString(R.string.set_electricity_provider_cue));
        utilityRateString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_RATE,
                getString(R.string.default_electricity_rate));
        gasPriceString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_GAS_PRICE,
                getString(R.string.default_gas_price));
        currentMPGString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG,
                getString(R.string.default_mpg));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        return true;
    }

    @OnClick(R.id.fab)
    public void findUtilities() {

        checkLocationPermission();

        Retrofit retrofit = new Retrofit.Builder().baseUrl(getString(R.string.nrel_api_path))
                .addConverterFactory(GsonConverterFactory.create()).build();
        UtilityRateAPIService mService = retrofit.create(UtilityRateAPIService.class);

        String API_KEY = "vIp4VQcx5zLfEr7Mi61aGd2vjIDpBpIqQRRQCoWt";

        Call<UtilityArray> call = null;

        call = mService.getElectricityProviders(API_KEY, latString, lonString);

        if (call != null) {
            if (latString != null && lonString != null){
                call.enqueue(new Callback<UtilityArray>() {
                    @Override
                    public void onResponse(Call<UtilityArray> call, Response<UtilityArray> response) {
                        Utility[] utilityArray = response.body().getOutputs().getUtilities();
                        ArrayList<Utility> localUtilities = new ArrayList<>(Arrays.asList(utilityArray));
                        utilities.addAll(localUtilities);

                        utilityName = utilities.get(0).getUtilityName();
                        utilityRate = response.body().getOutputs().getResidentialRate();

                        currentUtilityTextView.setText(utilityName);
                        utilityRateString = String.valueOf(utilityRate);
                        utilityRateTextView.setText(utilityRateString);
                        Toast.makeText(getBaseContext(), getString(R.string.electricity_provider_set),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Call<UtilityArray> call, Throwable t) {
                        Toast.makeText(getBaseContext(), getString(R.string.no_network_connection),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } else {
            Toast.makeText(this, getString(R.string.no_network_connection),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLocationPermission() {
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
                alertDialog.setTitle(getString(R.string.location_permission_alert_header));
                alertDialog.setMessage(getString(R.string.location_permission_alert));
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dialog_ok),
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
        editor.apply();
    }

    @Override
    public void onConnected(Bundle connectionHint) {

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
            //TODO:Test removing this check, just requesting the permission.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                ActivityCompat.requestPermissions(this,
                        permissions,
                        Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
//        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePreferencesString(Constants.KEY_SHARED_PREF_UTIL_NAME, utilityName);
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
}
