package com.stewartmcm.evclarity.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.stewartmcm.evclarity.model.Trip
import java.util.*

class PredictEvDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    val allTrips: ArrayList<Trip>
        get() {
            val db = this.readableDatabase
            val cursor = db.query(Contract.Trip.TABLE_NAME, TRIP_COLUMNS, null, null, null, null, null)
            cursor.moveToFirst()

            val trips = ArrayList<Trip>()
            for (i in 0 until cursor.count) {
                val trip = Trip(cursor.getString(COL_DATE_INDEX),
                        cursor.getFloat(COL_TRIP_MILES_INDEX),
                        cursor.getFloat(COL_TRIP_SAVINGS_INDEX))

                trips.add(trip)
                cursor.moveToNext()
            }

            cursor.close()
            return trips
        }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE TRIP (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "DATE NUMERIC, "
                + "TIME NUMERIC, "
                + "ORIGIN_LAT REAL, "
                + "ORIGIN_LONG REAL, "
                + "DEST_LAT REAL, "
                + "DEST_LONG REAL, "
                + "TRIP_MILES REAL, "
                + "TRIP_SAVINGS REAL);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun insertTrip(db: SQLiteDatabase, date: String, time: String, originLat: Double,
                   originLong: Double, destLat: Double, destLong: Double, tripMiles: Double, tripSavings: String) {

        val tripValues = ContentValues()
        tripValues.put(COL_DATE, date)
        tripValues.put(COL_TIME, time)
        tripValues.put(COL_ORIGIN_LAT, originLat)
        tripValues.put(COL_ORIGIN_LONG, originLong)
        tripValues.put(COL_DEST_LAT, destLat)
        tripValues.put(COL_DEST_LONG, destLong)
        tripValues.put(COL_TRIP_MILES, tripMiles)
        tripValues.put(COL_TRIP_SAVINGS, tripSavings)
        db.insert(TRIP_TABLE_NAME, null, tripValues)
    }

    fun deleteTrip(id: String): Int {
        val db = this.writableDatabase
        Log.i("PredictEVDBHelper", "deleteTrip: $id")
        return db.delete(TRIP_TABLE_NAME, "_id = ?", arrayOf(id))
    }

    companion object {

        val TRIP_TABLE_NAME = Contract.Trip.TABLE_NAME

        //TODO: Revisit this db setup
        val COL_ID = Contract.Trip._ID
        val COL_DATE = Contract.Trip.COLUMN_DATE
        val COL_TIME = Contract.Trip.COLUMN_TIME
        val COL_ORIGIN_LAT = Contract.Trip.COLUMN_ORIGIN_LAT
        val COL_ORIGIN_LONG = Contract.Trip.COLUMN_ORIGIN_LONG
        val COL_DEST_LAT = Contract.Trip.COLUMN_DEST_LAT
        val COL_DEST_LONG = Contract.Trip.COLUMN_DEST_LONG
        val COL_TRIP_MILES = Contract.Trip.COLUMN_TRIP_MILES
        val COL_TRIP_SAVINGS = Contract.Trip.COLUMN_TRIP_SAVINGS

        private val COL_DATE_INDEX = 1
        private val COL_TRIP_MILES_INDEX = 7
        private val COL_TRIP_SAVINGS_INDEX = 8

        private val TRIP_COLUMNS = arrayOf(COL_ID, COL_DATE, COL_TIME, COL_ORIGIN_LAT, COL_ORIGIN_LONG, COL_DEST_LAT, COL_DEST_LONG, COL_TRIP_MILES, COL_TRIP_SAVINGS)

        private val DB_NAME = "predictev"
        private val DB_VERSION = 2
    }
}
