package com.example.android.predictev;

import java.sql.Date;
import java.sql.Time;

    //TODO: determine if this class is necessary or redundant (to sqLite db)

/**
 * Created by stewartmcmillan on 5/4/16.
 */
public class Trip {
    private Date date;
    private Time time;
    private double originLat;
    private double originLong;
    private double destLat;
    private double destLong;

    //TODO: determine if array below needed if we already have db table for trips set up
    //public static final Trip[] trips;

    public Trip(Date date, Time time, double originLat, double originLong, double destLat, double destLong) {
        this.date = date;
        this.time = time;
        this.originLat = originLat;
        this.originLong = originLong;
        this.destLat = destLat;
        this.destLong = destLong;
    }

    public Date getDate() {
        return date;
    }

    public Time getTime() {
        return time;
    }

    public double getOriginLat() {
        return originLat;
    }

    public double getOriginLong() {
        return originLong;
    }

    public double getDestLat() {
        return destLat;
    }

    public double getDestLong() {
        return destLong;
    }

    @Override
    public String toString() {
        return "Trip{" +
                "date=" + date +
                ", time=" + time +
                '}';
    }
}
