package com.stewartmcm.evclarity.db

import android.net.Uri
import android.provider.BaseColumns

object Contract {

    internal val PATH_TRIP = "trip"
    internal val PATH_TRIP_WITH_DATETIME = "trip/*"
    private val AUTHORITY = "com.stewartmcm.evclarity"
    private val BASE_URI = Uri.parse("content://$AUTHORITY")

    class Trip : BaseColumns {
        companion object {
            val uri = BASE_URI.buildUpon().appendPath(PATH_TRIP).build()
            val TABLE_NAME = "trip"
            val _ID = "_id"
            val COLUMN_DATE = "date"
            val COLUMN_TIME = "time"
            val COLUMN_ORIGIN_LAT = "origin_lat"
            val COLUMN_ORIGIN_LONG = "origin_long"
            val COLUMN_DEST_LAT = "dest_lat"
            val COLUMN_DEST_LONG = "dest_long"
            val COLUMN_TRIP_MILES = "trip_miles"
            val COLUMN_TRIP_SAVINGS = "trip_savings"

            fun makeUriForTrip(dateTime: String): Uri {
                return uri.buildUpon().appendPath(dateTime).build()
            }
        }
    }

}
