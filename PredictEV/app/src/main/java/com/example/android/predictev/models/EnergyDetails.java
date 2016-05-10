package com.example.android.predictev.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by stewartmcmillan on 5/6/16.
 */
public class EnergyDetails {

    //commented code below was taken from Vice project

    @SerializedName("address")
    String userZip;
//    @SerializedName("title")
//    String articleTitle;
//    @SerializedName("tags")
//    String [] articleTags;
//    @SerializedName("thumb")
//    String articleThumbURL;
//    @SerializedName("image")
//    String articleImageURL;


    public String getUserZip() {
        return userZip;
    }

    public void setUserZip(String userZip) {
        this.userZip = userZip;
    }
}
