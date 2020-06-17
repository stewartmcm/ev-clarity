package com.stewartmcm.evclarity.fragment

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.LocationServices
import com.stewartmcm.evclarity.Constants
import com.stewartmcm.evclarity.EvApplication
import com.stewartmcm.evclarity.R
import com.stewartmcm.evclarity.model.Utility
import com.stewartmcm.evclarity.model.UtilityArray
import com.stewartmcm.evclarity.service.UtilityRateAPIService
import kotlinx.android.synthetic.main.fragment_energy_settings.*
import kotlinx.android.synthetic.main.fragment_energy_settings.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import javax.inject.Inject

class EnergySettingsFragment : Fragment(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    @Inject
    lateinit var sharedPrefs: SharedPreferences

    //TODO: inject googleapiclient
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var utilities: ArrayList<Utility>
    private var utilityRateString: String? = null
    private var gasPriceString: String? = null
    private var currentMPGString: String? = null
    private var utilityName: String? = null
    private var latString: String? = null
    private var lonString: String? = null
    private var utilityRate: Double = 0.toDouble()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        getSavedEnergySettings()
        val view = inflater.inflate(R.layout.fragment_energy_settings, container, false)
        view.current_utility_text_view.text = utilityName
        view.utility_rate_text_view.text = utilityRateString
        view.gas_price_edit_text.setText(gasPriceString)
        view.mpg_edit_text.setText(currentMPGString)
        view.fab.setOnClickListener { findUtilities() }
        return view
    }

    override fun onAttach(context: Context) {
        (requireActivity().application as EvApplication).evComponent.inject(this)
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        googleApiClient = GoogleApiClient.Builder(requireContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .build()
        googleApiClient.connect()


    }

    override fun onPause() {
        super.onPause()
        googleApiClient!!.disconnect()
    }

    override fun onDestroyView() {
        saveEnergySettings()
        super.onDestroyView()
    }

    override fun onConnected(bundle: Bundle?) {
        Log.d(this.tag, "Google Api Client CONNECTED")

    }

    override fun onConnectionSuspended(i: Int) {
        Log.d(this.tag, "Google Api Client SUSPENDED")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(this.tag, "Google Api Client CONNECTION FAILED")
    }

    private fun findUtilities() {

        checkLocationPermission()

        val retrofit = Retrofit.Builder().baseUrl(getString(R.string.nrel_api_path))
                .addConverterFactory(GsonConverterFactory.create()).build()
        val service = retrofit.create(UtilityRateAPIService::class.java)

        val API_KEY = requireContext().getString(R.string.nrel_api_key)

        val call: Call<UtilityArray>? = service.getElectricityProviders(API_KEY, latString, lonString)

        if (call != null) {
            if (latString != null && lonString != null) {
                call.enqueue(object : Callback<UtilityArray> {
                    override fun onResponse(call: Call<UtilityArray>, response: Response<UtilityArray>) {
                        val utilityArray = response.body()!!.outputs.utilities
                        val localUtilities = ArrayList(Arrays.asList(*utilityArray))

                        utilityName = localUtilities[0].utilityName
                        utilityRate = response.body()!!.outputs.residentialRate

                        current_utility_text_view.text = utilityName
                        utilityRateString = utilityRate.toString()
                        utility_rate_text_view.text = utilityRateString
                        Toast.makeText(context, getString(R.string.electricity_provider_set),
                                Toast.LENGTH_LONG).show()
                    }

                    override fun onFailure(call: Call<UtilityArray>, t: Throwable) {
                        Toast.makeText(context, getString(R.string.no_network_connection),
                                Toast.LENGTH_SHORT).show()
                    }
                })
            }

        } else {
            Toast.makeText(context, getString(R.string.no_network_connection),
                    Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSavedEnergySettings() {
        utilityName = sharedPrefs.getString(Constants.KEY_SHARED_PREF_UTIL_NAME, getString(R.string.set_electricity_provider_cue))
        utilityRateString = sharedPrefs.getString(Constants.KEY_SHARED_PREF_UTIL_RATE, getString(R.string.default_electricity_rate))
        gasPriceString = sharedPrefs.getString(Constants.KEY_SHARED_PREF_GAS_PRICE, getString(R.string.default_gas_price))
        currentMPGString = sharedPrefs.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG, getString(R.string.default_mpg))
    }

    private fun saveEnergySettings() {
        utilityRateString = utilityRate.toString()
        gasPriceString = gas_price_edit_text.text.toString()
        currentMPGString = mpg_edit_text.text.toString()

        val editor = sharedPrefs.edit()
        editor.putString(Constants.KEY_SHARED_PREF_UTIL_NAME, utilityName)
        editor.putString(Constants.KEY_SHARED_PREF_UTIL_RATE, utilityRateString)
        editor.putString(Constants.KEY_SHARED_PREF_GAS_PRICE, gasPriceString)
        editor.putString(Constants.KEY_SHARED_PREF_CURRENT_MPG, currentMPGString)
        editor.apply()
    }

    private fun checkLocationPermission() {
        val permissionCheck = ContextCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
            latString = lastLocation.latitude.toString()
            lonString = lastLocation.longitude.toString()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

            val alertDialog = AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.location_permission_alert_header))
                    .setMessage(getString(R.string.location_permission_alert))
                    .create()

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dialog_ok)) {
                dialog, which -> dialog.dismiss()
            }
            alertDialog.show()
        } else {
            val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(requireActivity(),
                    permissions,
                    Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION)
        }
    }

}
