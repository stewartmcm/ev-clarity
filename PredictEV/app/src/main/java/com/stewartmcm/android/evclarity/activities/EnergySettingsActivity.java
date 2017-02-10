package com.stewartmcm.android.evclarity.activities;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

public class EnergySettingsActivity extends AppCompatActivity {
    protected static final String TAG = "EnergySettingsActivity";
    private TextView currentUtilityTextView;
    private TextView utilityRateTextView;
    private EditText gasPriceEditText;
    private EditText mpgEditText;
    private String utilityName;
    private double utilityRate;
    private String utilityRateString;
    private String gasPriceString;
    private String currentMPGString;
    private ArrayList<Utility> utilities;
    private String latString;
    private String lonString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        latString = (String) getIntent().getExtras().get(Constants.EXTRA_USER_LAT);
        Log.i(TAG, "onCreate: latString : " + latString);
        lonString = (String) getIntent().getExtras().get(Constants.EXTRA_USER_LON);
        Log.i(TAG, "onCreate: latString : " + lonString);

        if (savedInstanceState != null) {
            utilityName = savedInstanceState.getString(Constants.KEY_SHARED_PREF_UTIL_NAME);
            Log.i(TAG, "onCreate: utilityName: " + utilityName);
            utilityRateString = savedInstanceState.getString(Constants.KEY_SHARED_PREF_UTIL_RATE);
            Log.i(TAG, "onCreate: utilityRate: " + utilityRateString);

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

    private void loadSavedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        utilityName = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_NAME, "Please set your location.");
        utilityRateString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_RATE, "0.0000");
        gasPriceString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_GAS_PRICE, "0.00");
        currentMPGString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG, "0.0");

        currentUtilityTextView.setText(utilityName);
        utilityRateTextView.setText(utilityRateString);

        gasPriceEditText.setText(gasPriceString);
        Log.i(TAG, "loadSavedPref gasPriceString: " + gasPriceString);

        mpgEditText.setText(currentMPGString);
        Log.i(TAG, "loadSavedPref mpgString: " + currentMPGString);

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
        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://developer.nrel.gov/api/utility_rates/")
                .addConverterFactory(GsonConverterFactory.create()).build();
        UtilityRateAPIService mService = retrofit.create(UtilityRateAPIService.class);

        Call<UtilityArray> call = null;

        call = mService.getElectricityProviders("vIp4VQcx5zLfEr7Mi61aGd2vjIDpBpIqQRRQCoWt", latString, lonString);

        if (call != null) {
            if (latString != null){
                call.enqueue(new Callback<UtilityArray>() {
                    @TargetApi(Build.VERSION_CODES.M)
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

                    }

                    @Override
                    public void onFailure(Call<UtilityArray> call, Throwable t) {
                    }
                });
            } else {
                Toast.makeText(this, getString(R.string.no_gps_data),
                        Toast.LENGTH_SHORT).show();
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
        Log.i(TAG, "onPause called");
        savePreferencesString(Constants.KEY_SHARED_PREF_UTIL_NAME, utilityName);
        Log.i(TAG, "onDestroy: utitlityName: " + utilityName);
        savePreferencesString(Constants.KEY_SHARED_PREF_UTIL_RATE, utilityRateString);

        gasPriceString = gasPriceEditText.getText().toString();
        currentMPGString = mpgEditText.getText().toString();
        savePreferencesString(Constants.KEY_SHARED_PREF_GAS_PRICE, gasPriceString);
        savePreferencesString(Constants.KEY_SHARED_PREF_CURRENT_MPG, currentMPGString);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop called");
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy called");
        super.onDestroy();
    }
}
