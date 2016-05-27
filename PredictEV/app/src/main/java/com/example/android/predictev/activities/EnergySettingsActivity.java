package com.example.android.predictev.activities;

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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.predictev.R;
import com.example.android.predictev.models.Utility;
import com.example.android.predictev.models.UtilityArray;
import com.example.android.predictev.services.UtilityRateAPIService;

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
    private double gasPrice;
    private ListView utilityOptionsListView;
    private UtilityRateAPIService mService;
    private Retrofit retrofit;
    private String userZip;
    private String utilityName;
    private double utilityRate;
    private String utilityRateString;
    private String gasPriceString;
    private String currentMPGString;
    private ArrayList<Utility> utilities;
    private ArrayAdapter<String> mAdapter;
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
            utilityRateTextView.setText("$" + utilityRateString + " / kWh");

        } else {

            initLayoutElements();
            loadSavedPreferences();
        }

        // TODO: add logic to display list of utilities if user's lat/lon returns multiple utility providers
        utilities = new ArrayList<>();
//        utilityOptionsListView = (ListView) findViewById(R.id.utility_options_list_view);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
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
        gasPriceString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_GAS_PRICE,"0.00");
        currentMPGString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG,"0.0");

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
        retrofit = new Retrofit.Builder().baseUrl("http://developer.nrel.gov/api/utility_rates/")
                .addConverterFactory(GsonConverterFactory.create()).build();
        mService = retrofit.create(UtilityRateAPIService.class);

        Call<UtilityArray> call = null;

        call = mService.getElectricityProviders("vIp4VQcx5zLfEr7Mi61aGd2vjIDpBpIqQRRQCoWt", latString, lonString);

        if (call != null) {
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

        Log.i(TAG, "calcSavings: gasPrice: " + gasPrice);


        if ( utilityRate != 0.0) {
            Log.i(TAG, "calcSavings: utilityRateString: " + utilityRateString);

            savings = mileageDouble * ((gasPrice / 29) - (.3 * utilityRate));

            return savings;
        }
        return 0.00;

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
