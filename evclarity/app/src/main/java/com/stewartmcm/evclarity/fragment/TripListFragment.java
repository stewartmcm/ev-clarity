package com.stewartmcm.evclarity.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;
import com.stewartmcm.evclarity.Constants;
import com.stewartmcm.evclarity.DeleteTripTask;
import com.stewartmcm.evclarity.R;
import com.stewartmcm.evclarity.SumLoggedTripsTask;
import com.stewartmcm.evclarity.TripAdapter;
import com.stewartmcm.evclarity.db.Contract;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.content.Context.MODE_PRIVATE;

public class TripListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
    @BindView(R.id.error)
    TextView errorTextView;

    @BindView(R.id.recyclerview_triplog_empty)
    TextView noTripsYetTextView;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    private int tripPosition;
    private TripAdapter mTripAdapter;
    private GoogleApiClient mGoogleApiClient;

    private static final int TRIP_LOADER = 0;
    private static final String[] TRIP_COLUMNS = {
            Contract.Trip._ID,
            Contract.Trip.COLUMN_DATE,
            Contract.Trip.COLUMN_TRIP_MILES,
            Contract.Trip.COLUMN_TRIP_SAVINGS
    };

    public static final int COL_DATE = 1;
    public static final int COL_TRIP_MILES = 7;
    public static final int COL_TRIP_SAVINGS = 8;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        buildGoogleApiClient();
        View view = inflater.inflate(R.layout.fragment_trip_list, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        new SumLoggedTripsTask(getActivity()).execute();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTripAdapter = new TripAdapter(getContext(), null, noTripsYetTextView);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setReverseLayout(true);

        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(recyclerView.getContext(), mLayoutManager.getOrientation());

        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(mTripAdapter);

        LoaderManager.getInstance(this).initLoader(TRIP_LOADER, null, this);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                tripPosition = viewHolder.getAdapterPosition();
                new DeleteTripTask(getContext()).execute(tripPosition);
                new SumLoggedTripsTask(getActivity()).execute();
                int newTripArraySize = mTripAdapter.removeTrip(tripPosition);
                mTripAdapter.notifyItemRemoved(tripPosition);
                mTripAdapter.notifyItemRangeChanged(tripPosition, newTripArraySize);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(),
                Contract.Trip.uri,
                TRIP_COLUMNS,
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mTripAdapter.swapCursor(data);

        if (data.getCount() != 0) {
            errorTextView.setVisibility(View.GONE);
            noTripsYetTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mTripAdapter.swapCursor(null);
    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            boolean requestingUpdates = !getUpdatesRequestedState();
            setUpdatesRequestedState(requestingUpdates);

        } else {
            Toast.makeText(
                    getContext(),
                    getString(R.string.no_gps_data),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        int permissionCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED
                && !ActivityCompat.shouldShowRequestPermissionRationale (getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(getActivity(),
                    permissions,
                    Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
         Log.e(this.getTag(), "Connection failed: " + result.getErrorCode());
    }

    private void setUpdatesRequestedState(boolean requestingUpdates) {
        getContext().getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, requestingUpdates)
                .apply();
    }

    private boolean getUpdatesRequestedState() {
        return getContext().getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE)
                .getBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, false);
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .build();
    }

}
