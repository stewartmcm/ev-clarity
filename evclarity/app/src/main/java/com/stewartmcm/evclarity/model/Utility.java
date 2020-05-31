package com.stewartmcm.evclarity.model;

import com.google.gson.annotations.SerializedName;

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
