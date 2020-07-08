package com.stewartmcm.evclarity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.TextView;
import android.widget.Toast;

import com.stewartmcm.evclarity.db.PredictEvDatabaseHelper;

public class SumLoggedTripsTask extends AsyncTask<Void, Void, Boolean> {

    private Cursor mCursor;
    private SQLiteDatabase db;
    private String mCurrentMPGString;
    private String mGasPriceString;
    private String mUtilityRateString;
    private Activity mActivity;

    public SumLoggedTripsTask (Activity activity) {
        mActivity = activity;
    }

    @Override
    protected void onPreExecute() { super.onPreExecute(); }

    @Override
    protected Boolean doInBackground(Void... params) {
        SQLiteOpenHelper helper = new PredictEvDatabaseHelper(mActivity);
        db = helper.getReadableDatabase();
        try {
            mCursor = db.query(Constants.TRIP_TABLE_NAME, new String[]{"SUM(TRIP_MILES) AS sum"},
                    null, null, null, null, null);
            return true;
        } catch (SQLiteException e) {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);

        mCursor.moveToFirst();
        if (success) {
            double sumLoggedTripsDouble = mCursor.getDouble(0);
            double savings = calcSavings(sumLoggedTripsDouble);

            TextView monthlySavingsTextView = (TextView) mActivity.findViewById(R.id.savings_textview);
            TextView totalMileageTextView = (TextView) mActivity.findViewById(R.id.total_mileage_textview);

            monthlySavingsTextView.setText(mActivity.getString(R.string.dollar_sign) + String.format(mActivity.getString(R.string.savings_format), savings));
            totalMileageTextView.setText(String.format(mActivity.getString(R.string.mileage_format),
                    sumLoggedTripsDouble));

        } else {
            Toast toast = Toast.makeText(mActivity, mActivity.getString(R.string.database_unavailable),
                    Toast.LENGTH_SHORT);
            toast.show();
        }
        mCursor.close();
        db.close();
    }

    private double calcSavings(double mileageDouble) {
        double savings;
        double currentMPG;
        double gasPrice;

        loadSharedPreferences();

        if (mGasPriceString.isEmpty()) {
            gasPrice = 0.0;
        } else {
            gasPrice = Double.parseDouble(mGasPriceString);
        }

        if (mCurrentMPGString.isEmpty()) {
            currentMPG = 0.0;
        } else {
            currentMPG = Double.parseDouble(mCurrentMPGString);
        }

        if (Double.parseDouble(mUtilityRateString) != 0.0) {
            // .3 is Nissan Leaf's kWh per mile driven (EV equivalent of mpg)
            savings = mileageDouble * ((gasPrice / currentMPG) - (.3 * Double.parseDouble(mUtilityRateString)));
            return savings;
        }

        return 0.00;
    }

    private void loadSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mUtilityRateString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_UTIL_RATE,
                mActivity.getString(R.string.default_electricity_rate));
        mGasPriceString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_GAS_PRICE,
                mActivity.getString(R.string.default_gas_price));
        mCurrentMPGString = sharedPreferences.getString(Constants.KEY_SHARED_PREF_CURRENT_MPG,
                mActivity.getString(R.string.default_mpg));
    }
}
