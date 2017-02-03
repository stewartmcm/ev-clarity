package com.stewartmcm.android.evclarity.activities;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.TextView;

import com.example.android.predictev.R;
import com.stewartmcm.android.evclarity.TripCursorAdapter;
import com.stewartmcm.android.evclarity.data.Contract;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Main2Activity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final int TRIP_LOADER = 0;
    @BindView(R.id.recycler_view)
    RecyclerView tripRecyclerView;
    private int mPosition = RecyclerView.NO_POSITION;
    protected static final String TAG = "TripsLoggedActivity";
    SQLiteDatabase db;
    @BindView(R.id.error)
    TextView error;
    private Cursor cursor;
    private TripCursorAdapter mTripAdapter;
    private int mChoiceMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        ButterKnife.bind(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        View emptyView = this.findViewById(R.id.recyclerview_watchlist_empty);

        mTripAdapter = new TripCursorAdapter(this, new TripCursorAdapter.TripAdapterOnClickHandler() {

            @Override
            public void onClick(String dateTime, TripCursorAdapter.TripAdapterViewHolder viewHolder) {
                mPosition = viewHolder.getAdapterPosition();
                onItemSelected(Contract.Trip.makeUriForTrip(dateTime),
                        viewHolder
                );
            }
        }, emptyView, mChoiceMode);

        tripRecyclerView.setAdapter(mTripAdapter);
        tripRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        getSupportLoaderManager().initLoader(TRIP_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                String symbol = mTripAdapter.getTripAtPosition(viewHolder.getAdapterPosition());
                //TODO: implement remove stock method
//                PrefUtils.removeStock(Main2Activity.this, symbol);
                getContentResolver().delete(Contract.Trip.makeUriForTrip(symbol), null, null);
            }
        }).attachToRecyclerView(tripRecyclerView);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                Contract.Trip.uri,
                Contract.Trip.TRIP_COLUMNS.toArray(new String[]{}),
                null, null, Contract.Trip.COLUMN_MILEAGE);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data.getCount() != 0) {
            error.setVisibility(View.GONE);
        }
        mTripAdapter.setCursor(data);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mTripAdapter.setCursor(null);
    }

    //TODO: Create TripDetailActivity with map of individual trip
    //    @Override
    public void onItemSelected(Uri contentUri, TripCursorAdapter.TripAdapterViewHolder vh) {

//        Intent intent = new Intent(this, TripDetailActivity.class)
//                .setData(contentUri);

//        ActivityOptionsCompat activityOptions =
//                ActivityOptionsCompat.makeSceneTransitionAnimation(this,
//                        new Pair<View, String>(vh.mIconView, getString(R.string.detail_icon_transition_name)));
//        startActivity(intent);

    }
}
