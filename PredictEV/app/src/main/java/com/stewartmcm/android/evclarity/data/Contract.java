package com.stewartmcm.android.evclarity.data;


import android.net.Uri;
import android.provider.BaseColumns;

public final class Contract {

    static final String AUTHORITY = "com.stewartmcm.android.evclarity";
    static final String PATH_TRIP = "trip";
    static final String PATH_TRIP_WITH_DATETIME = "trip/*";
    private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    private Contract() {
    }

    @SuppressWarnings("unused")
    public static final class Trip implements BaseColumns {


        public static final Uri uri = BASE_URI.buildUpon().appendPath(PATH_TRIP).build();

        public static final String TABLE_NAME = "trip";

        public static final String _ID = "_id";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_ORIGIN_LAT = "origin_lat";
        public static final String COLUMN_ORIGIN_LONG = "origin_long";
        public static final String COLUMN_DEST_LAT = "dest_lat";
        public static final String COLUMN_DEST_LONG = "dest_long";
        public static final String COLUMN_TRIP_MILES = "trip_miles";
        public static final String COLUMN_TRIP_SAVINGS = "trip_savings";

        public static Uri makeUriForTrip(String dateTime) {
            return uri.buildUpon().appendPath(dateTime).build();
        }

        static String getStockFromUri(Uri queryUri) {
            return queryUri.getLastPathSegment();
        }

    }

}
