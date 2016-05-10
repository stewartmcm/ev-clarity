package com.example.android.predictev.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by stewartmcmillan on 5/6/16.
 */
public class Utility {
    //commented code below was taken from Vice project

    @SerializedName("company_id")
    String companyId;
    @SerializedName("utility_name")
    String utilityName;

    public String getCompanyId() {
        return companyId;
    }

    public String getUtilityName() {
        return utilityName;
    }
}
