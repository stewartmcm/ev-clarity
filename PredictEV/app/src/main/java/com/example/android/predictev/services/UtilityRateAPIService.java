package com.example.android.predictev.services;

import com.example.android.predictev.models.UtilityArray;
import com.example.android.predictev.models.UtilityRateAPIOutput;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
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

    //http://vice.com/api/article/<:id>
    @GET("&address={address}")
    Call<UtilityRateAPIOutput> getOutput(@Path("address") String userZip);
}
