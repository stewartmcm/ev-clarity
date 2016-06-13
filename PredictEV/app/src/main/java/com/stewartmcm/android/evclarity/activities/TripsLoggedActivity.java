package com.stewartmcm.android.evclarity.activities;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.predictev.R;
import com.stewartmcm.android.evclarity.PredictEvDatabaseHelper;
import com.stewartmcm.android.evclarity.TripsLoggedCursorAdapter;
import com.stewartmcm.android.evclarity.services.OdometerService;

public class TripsLoggedActivity extends AppCompatActivity {
    protected static final String TAG = "TripsLoggedActivity";
    SQLiteDatabase db;
    private ListView loggedTripsListView;
    private Cursor cursor;
    private OdometerService odometer;
    private boolean bound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trips_logged);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loggedTripsListView = (ListView) findViewById(R.id.trips_logged_list_view);

        //TODO: create a cursor for delete task to work with

        String[] mProjection = {
                PredictEvDatabaseHelper.COL_ID,
                PredictEvDatabaseHelper.COL_TRIP_MILES,
                PredictEvDatabaseHelper.COL_DATE,
        };

        loggedTripsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Log.i(TAG, "deleteTrip method, position : " + position);
                int tripNo = position;
                new DeleteTripTask().execute(tripNo);
                Snackbar.make(view, "Trip deleted.", Snackbar.LENGTH_LONG).setAction("Action", null).show();

                new GetLoggedTripsTask().execute(loggedTripsListView);
                return true;
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        new GetLoggedTripsTask().execute(loggedTripsListView);

    }

    @Override
    protected void onRestart() {
        super.onRestart();
//        try{
//            PredictEvDatabaseHelper mHelper = new PredictEvDatabaseHelper(this);
//            db = mHelper.getReadableDatabase();
//            Cursor newCursor = db.query("TRIP",
//                    new String[]{"_id", "TRIP_MILES"},
//                    null, null, null, null, null);
//
//            loggedTripsListView = (ListView) findViewById(R.id.trips_logged_list_view);
//
//            TripsLoggedCursorAdapter customAdapter = new TripsLoggedCursorAdapter(
//                    TripsLoggedActivity.this,
//                    cursor,
//                    0);
//            customAdapter.changeCursor(newCursor);
//            cursor = newCursor;
//
//        } catch(SQLiteException e) {
//            Toast toast = Toast.makeText(this, "Database unavailable.", Toast.LENGTH_SHORT);
//            toast.show();
//        }
    }

    //deletes trip asynchronously when executed
    private class DeleteTripTask extends AsyncTask<Integer, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Boolean doInBackground(Integer... trips) {

            int tripNo = trips[0];

            SQLiteDatabase db = null;
            PredictEvDatabaseHelper mHelper = PredictEvDatabaseHelper.getInstance(TripsLoggedActivity.this);
            //TODO: move cursor to first

            try {

                Log.i(TAG, "doInBackground: position: " + tripNo);
                //TODO: if deleteTrip returns > 0 then show snackbar below

                if (mHelper.deleteTrip(Integer.toString(tripNo)) > 0) {
                    Log.d(TAG, "doInBackground: # of trips deleted > 0");
                } else {
                    Log.d(TAG, "doInBackground: NO TRIPS DELETED");
                }

//                db = mHelper.getWritableDatabase();
//                db.delete("TRIP","_id = ?", new String[] {Integer.toString(tripNo)});
//                db.close();
                //TODO: try swap cursor??
                return true;

            } catch (SQLiteException e) {
                Toast toast = Toast.makeText(TripsLoggedActivity.this, "Database unavailable.", Toast.LENGTH_SHORT);
                toast.show();
                return false;

            } finally {
                if (db != null)
                    db.close();
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

//

        }
    }

    //inner class retrieves all logged trips asynchronously when executed[onCreate]
    private class GetLoggedTripsTask extends AsyncTask<ListView, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Boolean doInBackground(ListView... params) {
            SQLiteOpenHelper mHelper = new PredictEvDatabaseHelper(TripsLoggedActivity.this);

            //TODO: move cursor to first

            try {
                db = mHelper.getReadableDatabase();
                cursor = db.query("TRIP", new String[]{"_id", "TRIP_MILES"},
                        null, null, null, null, null);
                return true;

            } catch (SQLiteException e) {
                Toast toast = Toast.makeText(TripsLoggedActivity.this, "Database unavailable.", Toast.LENGTH_SHORT);
                toast.show();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            if (success) {
                // fill listview with data from cursor

                TripsLoggedCursorAdapter customAdapter = new TripsLoggedCursorAdapter(
                        TripsLoggedActivity.this,
                        cursor,
                        0);
                loggedTripsListView.setAdapter(customAdapter);

//                CursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(TripsLoggedActivity.this,
//                        android.R.layout.simple_list_item_1, cursor,
//                        new String[]{"TRIP_MILES"},
//                        new int[]{android.R.id.text1}, 0);
//                loggedTripsListView.setAdapter(simpleCursorAdapter);

            } else {

                Toast toast = Toast.makeText(TripsLoggedActivity.this, "Database unavailable", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cursor.close();
        db.close();
    }
}