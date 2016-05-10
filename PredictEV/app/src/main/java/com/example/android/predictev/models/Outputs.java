package com.example.android.predictev.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by stewartmcmillan on 5/6/16.
 */
public class Outputs {

    @SerializedName("company_id")
    String companyId;
    @SerializedName("utility_name")
    String utilityName;
    @SerializedName("utility_info")
    Utility [] utilities;
    @SerializedName("commercial")
    double commercialRate;
    @SerializedName("industrial")
    double industrialRate;
    @SerializedName("residential")
    double residentialRate;


    public Utility[] getUtilities() {
        return utilities;
    }

    public String getUtilityName() {
        return utilityName;
    }

    public double getCommercialRate() {
        return commercialRate;
    }

    public double getIndustrialRate() {
        return industrialRate;
    }

    public double getResidentialRate() {
        return residentialRate;
    }

    public String getCompanyId() {
        return companyId;
    }
}
