package com.stewartmcm.android.evclarity.data;


import android.net.Uri;
import android.provider.BaseColumns;

import com.google.common.collect.ImmutableList;

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

        public static final String TABLE_NAME = "trips";

        //TODO: make sure this contract maps perfectly to the dbhelper

        public static final String _ID = "_id";
        public static final String COLUMN_MILEAGE = "mileage";
        public static final String COLUMN_DATE_TIME = "date_time";
        public static final String COLUMN_START_LOCATION = "absolute_change";
        public static final String COLUMN_END_LOCATION = "percentage_change";
        public static final String COLUMN_SAVINGS = "history";
        public static final int POSITION_ID = 0;
        public static final int POSITION_SYMBOL = 1;
        public static final int POSITION_PRICE = 2;
        public static final int POSITION_ABSOLUTE_CHANGE = 3;
        public static final int POSITION_PERCENTAGE_CHANGE = 4;
        public static final int POSITION_HISTORY = 5;
        public static final ImmutableList<String> TRIP_COLUMNS = ImmutableList.of(
                _ID,
                COLUMN_MILEAGE,
                COLUMN_DATE_TIME,
                COLUMN_START_LOCATION,
                COLUMN_END_LOCATION,
                COLUMN_SAVINGS
        );

        public static Uri makeUriForTrip(String symbol) {
            return uri.buildUpon().appendPath(symbol).build();
        }

        static String getStockFromUri(Uri queryUri) {
            return queryUri.getLastPathSegment();
        }


    }

}
