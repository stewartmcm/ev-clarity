package com.stewartmcm.android.evclarity.services;

import com.stewartmcm.android.evclarity.models.UtilityArray;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;


/**
 * Created by stewartmcmillan on 5/6/16.
 */
public interface UtilityRateAPIService {

    // http://developer.nrel.gov/api/utility_rates/v3.json?
    // api_key=vIp4VQcx5zLfEr7Mi61aGd2vjIDpBpIqQRRQCoWt&
    // address=<:userZip>

    @GET("v3.json")
    Call<UtilityArray> getElectricityProviders(@Query("api_key") String apiKey, @Query("lat") String userLat, @Query("lon") String userLon);
}
