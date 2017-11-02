package com.stewartmcm.android.evclarity;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.stewartmcm.android.evclarity.activities.Constants;
import com.stewartmcm.android.evclarity.data.PredictEvDatabaseHelper;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import static android.content.ContentValues.TAG;

public class LogTripTask extends AsyncTask<Void, Void, Boolean> {

    private Context mContext;
    private Cursor cursor;
    private String currentMPGString;
    private String utilityRateString;
    private String gasPriceString;
    private double mFinalTripDistance;
    public double tripDistance;

    public LogTripTask(Context context, double finalTripDistance) {
        mContext = context;
        mFinalTripDistance = finalTripDistance;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Log.i(TAG, "LogTripTask doInBackground: method ran");
        PredictEvDatabaseHelper mHelper = PredictEvDatabaseHelper.getInstance(mContext);
        SQLiteDatabase db = mHelper.getWritableDatabase();
        GregorianCalendar calender = new GregorianCalendar();

        double tripSavings = calcSavings(mFinalTripDistance);
        Log.i(TAG, "doInBackground: " + tripSavings);

        DecimalFormat savingsFormat = new DecimalFormat("###.##");
        String savingsString = savingsFormat.format(tripSavings);
        Log.i(TAG, "doInBackground: " + savingsString);

        cursor = db.query(Constants.TRIP_TABLE_NAME, new String[]{"SUM(TRIP_MILES) AS sum"},
                null, null, null, null, null);
        cursor.moveToLast();
        try {
            mHelper.insertTrip(db, format(calender), "11:23", 37.828411, -122.289890, 37.805591,
                    -122.275583, mFinalTripDistance, savingsString);
            return true;

        } catch (SQLiteException e) {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        cursor.moveToFirst();
    }

    private double calcSavings(double mileageDouble) {
        loadSharedPreferences();

        double savings;
        double utilityRate = Double.parseDouble(utilityRateString);
        double gasPrice;
        double currentMPG;

        if (gasPriceString.isEmpty()) {
            gasPrice = 0.0;
        } else {
            gasPrice = Double.parseDouble(gasPriceString);
        }
//        Log.i(TAG, "calcSavings: gasPrice: " + gasPrice);


        if (currentMPGString.isEmpty()) {
            currentMPG = 0.0;
        } else {
            currentMPG = Double.parseDouble(currentMPGString);
        }

        if (utilityRate != 0.0) {
            savings = mileageDouble * ((gasPrice / currentMPG) - (.3 * utilityRate));
//            .3 is Nissan Leaf's kWh per mile driven (EV equivalent of mpg)

            return savings;
        }
        return 0.00;

    }

    private void loadSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        utilityRateString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_RATE,
                mContext.getString(R.string.default_utility_rate));
        gasPriceString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_GAS_PRICE,
                mContext.getString(R.string.default_gas_price));
        currentMPGString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG,
                mContext.getString(R.string.default_mpg));
        tripDistance = sharedPreferences.getFloat(Constants.KEY_SHARED_PREF_TRIP_DISTANCE, 0.0f);

    }

    public String format(GregorianCalendar calendar){
        SimpleDateFormat fmt = new SimpleDateFormat(mContext.getString(R.string.date_format));
        fmt.setCalendar(calendar);
        return fmt.format(calendar.getTime());
    }
}
