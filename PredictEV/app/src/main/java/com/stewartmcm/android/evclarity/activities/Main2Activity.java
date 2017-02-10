package com.stewartmcm.android.evclarity.activities;

import android.database.Cursor;
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

import com.stewartmcm.android.evclarity.R;
import com.stewartmcm.android.evclarity.TripAdapter;
import com.stewartmcm.android.evclarity.data.Contract;

import butterknife.BindView;
import butterknife.ButterKnife;

public class Main2Activity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.recycler_view)
    RecyclerView tripRecyclerView;
    private int mPosition = RecyclerView.NO_POSITION;
    protected static final String TAG = "TripsLoggedActivity";
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.error)
    TextView error;
    private TripAdapter mTripAdapter;
    private int mChoiceMode;

    private static final int TRIP_LOADER = 0;
    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] TRIP_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            Contract.Trip._ID,
            Contract.Trip.COLUMN_DATE,
            Contract.Trip.COLUMN_TIME,
            Contract.Trip.COLUMN_ORIGIN_LAT,
            Contract.Trip.COLUMN_ORIGIN_LONG,
            Contract.Trip.COLUMN_DEST_LAT,
            Contract.Trip.COLUMN_DEST_LONG,
            Contract.Trip.COLUMN_TRIP_MILES,
            Contract.Trip.COLUMN_TRIP_SAVINGS
    };

    // These indices are tied to TRIP_COLUMNS.  If TRIP_COLUMNS changes, these
    // must change.
    static final int COL_ID = 0;
    public static final int COL_DATE = 1;
    static final int COL_TIME = 2;
    static final int COL_ORIGIN_LAT = 3;
    static final int COL_ORIGIN_LONG = 4;
    static final int COL_DEST_LAT = 5;
    static final int COL_DEST_LONG = 6;
    public static final int COL_TRIP_MILES = 7;
    public static final int COL_TRIP_SAVINGS = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        View emptyView = this.findViewById(R.id.recyclerview_watchlist_empty);

        mTripAdapter = new TripAdapter(this, new TripAdapter.TripAdapterOnClickHandler() {

            @Override
            public void onClick(String dateTime, TripAdapter.TripAdapterViewHolder viewHolder) {
                mPosition = viewHolder.getAdapterPosition();
                onItemSelected(Contract.Trip.makeUriForTrip(dateTime),
                        viewHolder
                );
            }
        }, emptyView, mChoiceMode);

        tripRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tripRecyclerView.setAdapter(mTripAdapter);

        getSupportLoaderManager().initLoader(TRIP_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
//                String symbol = mTripAdapter.getTripAtPosition(viewHolder.getAdapterPosition());
                //TODO: implement remove trip method
//                PrefUtils.removeStock(Main2Activity.this, symbol);
//                getContentResolver().delete(Contract.Trip.makeUriForTrip(symbol), null, null);
            }
        }).attachToRecyclerView(tripRecyclerView);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        return new CursorLoader(this,
                Contract.Trip.uri,
                TRIP_COLUMNS,
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mTripAdapter.swapCursor(data);

        if (data.getCount() != 0) {
            error.setVisibility(View.GONE);
        }
//        mTripAdapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mTripAdapter.swapCursor(null);
    }

    //TODO: Create TripDetailActivity with map of individual trip
    //    @Override
    public void onItemSelected(Uri contentUri, TripAdapter.TripAdapterViewHolder vh) {

//        Intent intent = new Intent(this, TripDetailActivity.class)
//                .setData(contentUri);

//        ActivityOptionsCompat activityOptions =
//                ActivityOptionsCompat.makeSceneTransitionAnimation(this,
//                        new Pair<View, String>(vh.mIconView, getString(R.string.detail_icon_transition_name)));
//        startActivity(intent);

    }
}
