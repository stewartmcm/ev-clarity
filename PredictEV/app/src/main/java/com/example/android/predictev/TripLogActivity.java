package com.example.android.predictev;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.example.android.predictev.services.OdometerService;

import java.util.ArrayList;

public class TripLogActivity extends AppCompatActivity {

    private ArrayList<String> tripsLogged;
    private ArrayAdapter<String> mAdapter;
    SQLiteDatabase db;
    private ListView loggedTripsListView;
    private Cursor cursor;
    private OdometerService odometer;
    private boolean bound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_log);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loggedTripsListView = (ListView) findViewById(R.id.trips_logged_list_view);

        new GetLoggedTripsTask().execute(loggedTripsListView);
    }

    //inner class retrieves all logged trips asynchronously when executed[onCreate]
    private class GetLoggedTripsTask extends AsyncTask<ListView, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Boolean doInBackground(ListView... params) {
            SQLiteOpenHelper mHelper = new PredictEvDatabaseHelper(TripLogActivity.this);

            try {
                db = mHelper.getReadableDatabase();
                cursor = db.query("TRIP", new String[]{"_id", "TRIP_MILES"},
                        null, null, null, null, null);
                return true;

            } catch (SQLiteException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            if (success) {
                // fill listview with data from cursor
                CursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(TripLogActivity.this,
                        android.R.layout.simple_list_item_1, cursor,
                        new String[]{"TRIP_MILES"},
                        new int[]{android.R.id.text1}, 0);
                loggedTripsListView.setAdapter(simpleCursorAdapter);

            } else {

                Toast toast = Toast.makeText(TripLogActivity.this, "Database unavailable", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cursor.close();
        db.close();
    }

}
