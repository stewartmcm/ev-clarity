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
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;

public class TripLogActivity extends AppCompatActivity {

    private ArrayList<String> tripsLogged;
    private ArrayAdapter<String> mAdapter;
    SQLiteDatabase db;
    private ListView loggedTripsListView;
    private Cursor cursor;
    private OdometerService odometer;
    private boolean bound = false;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_log);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loggedTripsListView = (ListView) findViewById(R.id.trips_logged_list_view);

        new GetLoggedTripsTask().execute(loggedTripsListView);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client.connect();
//        Intent intent = new Intent(this, OdometerService.class);
//        bindService(intent, connection, Context.BIND_AUTO_CREATE);
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "TripLog Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app deep link URI is correct.
//                Uri.parse("android-app://com.example.android.predictev/http/host/path")
//        );
//        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "TripLog Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app deep link URI is correct.
//                Uri.parse("android-app://com.example.android.predictev/http/host/path")
//        );
//        AppIndex.AppIndexApi.end(client, viewAction);
//        if (bound) {
//            unbindService(connection);
//            bound = false;
//        }
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client.disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cursor.close();
        db.close();
    }

//    private ServiceConnection connection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder binder) {
//            OdometerService.OdometerBinder odometerBinder =
//                    (OdometerService.OdometerBinder) binder;
//            odometer = odometerBinder.getOdometer();
//            bound = true;
//
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName) {
//            bound = false;
//        }
//    };
}
