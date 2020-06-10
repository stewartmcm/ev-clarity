package com.stewartmcm.evclarity.fragment;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;
import com.stewartmcm.evclarity.Constants;
import com.stewartmcm.evclarity.EvApplication;
import com.stewartmcm.evclarity.R;
import com.stewartmcm.evclarity.model.Utility;
import com.stewartmcm.evclarity.model.UtilityArray;
import com.stewartmcm.evclarity.service.UtilityRateAPIService;

import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EnergySettingsFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    @BindView(R.id.current_utility_text_view)
    TextView currentUtilityTextView;

    @BindView(R.id.utility_rate_text_view)
    TextView utilityRateTextView;

    @BindView(R.id.gas_price_edit_text)
    EditText gasPriceEditText;

    @BindView(R.id.mpg_edit_text)
    EditText mpgEditText;

    @Inject
    SharedPreferences sharedPrefs;
    private String utilityRateString;
    private String gasPriceString;
    private String currentMPGString;
    private String utilityName;
    private String latString;
    private String lonString;
    private double utilityRate;
    private ArrayList<Utility> utilities;
    private GoogleApiClient googleApiClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_energy_settings, container, false);
        ButterKnife.bind(this, view);
        loadSharedPreferences();
        currentUtilityTextView.setText(utilityName);
        utilityRateTextView.setText(utilityRateString);
        gasPriceEditText.setText(gasPriceString);
        mpgEditText.setText(currentMPGString);

        // TODO: add logic to display list of utilities if user's lat/lon returns multiple utility providers
        utilities = new ArrayList<>();

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        ((EvApplication) getActivity().getApplication()).evComponent.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        googleApiClient.disconnect();
    }

    @Override
    public void onDestroyView() {
        utilityRateString = String.valueOf(utilityRate);
        gasPriceString = gasPriceEditText.getText().toString();
        currentMPGString = mpgEditText.getText().toString();

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(Constants.KEY_SHARED_PREF_UTIL_NAME, utilityName);
        editor.putString(Constants.KEY_SHARED_PREF_UTIL_RATE, utilityRateString);
        editor.putString(Constants.KEY_SHARED_PREF_GAS_PRICE, gasPriceString);
        editor.putString(Constants.KEY_SHARED_PREF_CURRENT_MPG, currentMPGString);
        editor.apply();
        super.onDestroyView();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(this.getTag(), "Google Api Client CONNECTED");

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(this.getTag(), "Google Api Client SUSPENDED");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(this.getTag(), "Google Api Client CONNECTION FAILED");
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
                        //TODO: figure out why utility isn't being displayed
                        Utility[] utilityArray = response.body().getOutputs().getUtilities();
                        ArrayList<Utility> localUtilities = new ArrayList<>(Arrays.asList(utilityArray));
                        utilities.addAll(localUtilities);

                        utilityName = utilities.get(0).getUtilityName();
                        utilityRate = response.body().getOutputs().getResidentialRate();

                        currentUtilityTextView.setText(utilityName);
                        utilityRateString = String.valueOf(utilityRate);
                        utilityRateTextView.setText(utilityRateString);
                        Toast.makeText(getContext(), getString(R.string.electricity_provider_set),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Call<UtilityArray> call, Throwable t) {
                        Toast.makeText(getContext(), getString(R.string.no_network_connection),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } else {
            Toast.makeText(getContext(), getString(R.string.no_network_connection),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSharedPreferences() {
        utilityName = sharedPrefs.getString(Constants.KEY_SHARED_PREF_UTIL_NAME,
                getString(R.string.set_electricity_provider_cue));
        utilityRateString = sharedPrefs.getString(Constants.KEY_SHARED_PREF_UTIL_RATE,
                getString(R.string.default_electricity_rate));
        gasPriceString = sharedPrefs.getString(Constants.KEY_SHARED_PREF_GAS_PRICE,
                getString(R.string.default_gas_price));
        currentMPGString = sharedPrefs.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG,
                getString(R.string.default_mpg));
    }

    private void checkLocationPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    googleApiClient);

            if (lastLocation != null) {
                latString = String.valueOf(lastLocation.getLatitude());
                lonString = String.valueOf(lastLocation.getLongitude());
            }

        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
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
                ActivityCompat.requestPermissions(getActivity(),
                        permissions,
                        Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            }
        }
    }

}
