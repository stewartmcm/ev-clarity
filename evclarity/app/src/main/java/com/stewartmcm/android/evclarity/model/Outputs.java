package com.stewartmcm.android.evclarity.model;

import com.google.gson.annotations.SerializedName;

public class Outputs {

    @SerializedName("company_id")
    private String companyId;

    @SerializedName("utility_name")
    private String utilityName;

    @SerializedName("utility_info")
    private Utility [] utilities;

    @SerializedName("commercial")
    private double commercialRate;

    @SerializedName("industrial")
    private double industrialRate;

    @SerializedName("residential")
    private double residentialRate;


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
