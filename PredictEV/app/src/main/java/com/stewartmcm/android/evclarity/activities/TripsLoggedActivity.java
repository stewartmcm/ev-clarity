package com.stewartmcm.android.evclarity.activities;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
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

        loggedTripsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Log.i(TAG, "deleteTrip method, position : " + position);
                int tripNo = position;
                new DeleteTripTask().execute(tripNo);

                new GetLoggedTripsTask().execute(loggedTripsListView);
                return true;
            }
        });

        loggedTripsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(TripsLoggedActivity.this, getString(R.string.long_click_to_delete),
                        Toast.LENGTH_LONG).show();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        new GetLoggedTripsTask().execute(loggedTripsListView);

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

                db = mHelper.getWritableDatabase();
                cursor = db.query("TRIP", new String[]{"_id", "TRIP_MILES"},
                        null, null, null, null, null);

                if(cursor.moveToPosition(tripNo)) {
                    String rowId = cursor.getString(cursor.getColumnIndex(PredictEvDatabaseHelper.COL_ID));

                    db.delete("TRIP", PredictEvDatabaseHelper.COL_ID + "=?", new String[]{rowId});
                }
                db.close();
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