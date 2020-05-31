package com.stewartmcm.android.evclarity.model;

/**
 * Created by stewartmcmillan on 2/24/17.
 */

public class Trip {

    private float miles;

    private String timeStamp;

    private float savings;

    //TODO: determine if array below needed if we already have db table for trips set up
    //public static final Trip[] trips;

    public Trip(String dateTime, float miles, float savings) {
        this.timeStamp = dateTime;
        this.miles = miles;
        this.savings = savings;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String dateTime) {
        this.timeStamp = dateTime;
    }

    public float getMiles() {
        return miles;
    }

    public void setMiles(float miles) {
        this.miles = miles;
    }

    public float getSavings() {
        return savings;
    }

    public void setSavings(float savings) {
        this.savings = savings;
    }

    @Override
    public String toString() {
        return "Trip{" +
                "timeStamp=" + timeStamp +
                '}';
    }
}
