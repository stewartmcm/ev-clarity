package com.stewartmcm.evclarity.model;

import com.google.gson.annotations.SerializedName;

public class Utility {

    @SerializedName("company_id")
    private String companyId;

    @SerializedName("utility_name")
    private String utilityName;

    public String getCompanyId() {
        return companyId;
    }

    public String getUtilityName() {
        return utilityName;
    }
}
