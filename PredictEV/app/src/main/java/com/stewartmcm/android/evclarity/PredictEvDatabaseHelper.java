package com.stewartmcm.android.evclarity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by stewartmcmillan on 5/2/16.
 */
public class PredictEvDatabaseHelper extends SQLiteOpenHelper {

    public static final String TRIP_TABLE_NAME = "TRIP";

    public static final String COL_ID = "_id";
    public static final String COL_DATE = "DATE";
    public static final String COL_TIME = "TIME";
    public static final String COL_ORIGIN_LAT = "ORIGIN_LAT";
    public static final String COL_ORIGIN_LONG = "ORIGIN_LONG";
    public static final String COL_DEST_LAT = "DEST_LAT";
    public static final String COL_DEST_LONG = "DEST_LONG";
    public static final String COL_TRIP_MILES = "TRIP_MILES";

    private static final String[] TRIP_COLUMNS = {COL_ID, COL_DATE, COL_TIME, COL_ORIGIN_LAT, COL_ORIGIN_LONG,
            COL_DEST_LAT, COL_DEST_LONG, COL_TRIP_MILES};

    private static final String DB_NAME = "predictev";
    private static final int DB_VERSION = 1;

    public PredictEvDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE TRIP (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "DATE NUMERIC, "
                + "TIME NUMERIC, "
                + "ORIGIN_LAT REAL, "
                + "ORIGIN_LONG REAL, "
                + "DEST_LAT REAL, "
                + "DEST_LONG REAL, "
                + "TRIP_MILES REAL);");

        insertTrip(db, "2016-05-01", "11:23", 37.805591, -122.275583, 37.828411, -122.289890, 3.34);
        insertTrip(db,"2016-05-01","11:23",37.828411,-122.289890,37.805591,-122.275583,3.26);
        insertTrip(db,"2016-05-02","8:17",37.805591,-122.275583,37.790841,-122.401280,12.76);
        insertTrip(db,"2016-05-02", "18:17", 37.790841, -122.401280, 37.805591, -122.275583, 13.56);
        insertTrip(db,"2016-05-02", "18:17", 37.790841, -122.401280, 37.805591, -122.275583, 9.86);
        insertTrip(db,"2016-05-02", "18:17", 37.790841, -122.401280, 37.805591, -122.275583, 45.36);
        insertTrip(db,"2016-05-02", "18:17", 37.790841, -122.401280, 37.805591, -122.275583, 128.45);
        insertTrip(db,"2016-05-02", "18:17", 37.790841, -122.401280, 37.805591, -122.275583, 1.78);
        insertTrip(db,"2016-05-02", "18:17", 37.790841, -122.401280, 37.805591, -122.275583, 37.89);


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    // makes sure there is only one instance of the database
    // if there isn't one, make it, otherwise return the one instance
    public static PredictEvDatabaseHelper instance;

    public static PredictEvDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PredictEvDatabaseHelper(context);
        }

        return instance;
    }

    public void insertTrip(SQLiteDatabase db, String date, String time, double originLat,
                                   double originLong, double destLat, double destLong, double tripMiles) {

        ContentValues tripValues = new ContentValues();
        tripValues.put(COL_DATE, date);
        tripValues.put(COL_TIME, time);
        tripValues.put(COL_ORIGIN_LAT, originLat);
        tripValues.put(COL_ORIGIN_LONG, originLong);
        tripValues.put(COL_DEST_LAT, destLat);
        tripValues.put(COL_DEST_LONG, destLong);
        tripValues.put(COL_TRIP_MILES, tripMiles);
        db.insert(TRIP_TABLE_NAME, null, tripValues);
    }

    public Cursor getTripList() {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TRIP_TABLE_NAME, // a. table
                TRIP_COLUMNS, // b. column names
                null, // c. selections
                null, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        return cursor;
    }
}
