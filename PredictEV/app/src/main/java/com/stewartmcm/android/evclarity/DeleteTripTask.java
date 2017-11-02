package com.stewartmcm.android.evclarity;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.widget.Toast;

import com.stewartmcm.android.evclarity.activities.Constants;
import com.stewartmcm.android.evclarity.data.PredictEvDatabaseHelper;

public class DeleteTripTask extends AsyncTask<Integer, Void, Boolean> {
    private Context mContext;

    public DeleteTripTask(Context context) {
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Integer... trips) {
        int tripNo = trips[0];

        //TODO: Test deleting line below and declaring db in try catch statement
        SQLiteDatabase db = null;
        PredictEvDatabaseHelper mHelper = PredictEvDatabaseHelper.getInstance(mContext);

        try {
            //TODO: try replacing query columns with constants
            db = mHelper.getWritableDatabase();
            Cursor deleteTripCursor = db.query(Constants.TRIP_TABLE_NAME, new String[]{"_id", "TRIP_MILES"},
                    null, null, null, null, null);

            if (deleteTripCursor.moveToPosition(tripNo)) {
                String rowId = deleteTripCursor.getString(deleteTripCursor.getColumnIndex(PredictEvDatabaseHelper.COL_ID));

                db.delete(Constants.TRIP_TABLE_NAME, PredictEvDatabaseHelper.COL_ID + "=?", new String[]{rowId});
                deleteTripCursor.close();
            }

            return true;

        } catch (SQLiteException e) {
            Toast toast = Toast.makeText(mContext, mContext.getString(R.string.database_unavailable),
                    Toast.LENGTH_SHORT);
            toast.show();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);

    }
}
