package com.stewartmcm.evclarity.fragment;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
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
import com.stewartmcm.evclarity.EvApplication;
import com.stewartmcm.evclarity.R;
import com.stewartmcm.evclarity.SumLoggedTripsTask;
import com.stewartmcm.evclarity.TripAdapter;
import com.stewartmcm.evclarity.db.Contract;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TripListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
    @BindView(R.id.error)
    TextView errorTextView;

    @BindView(R.id.recyclerview_triplog_empty)
    TextView noTripsYetTextView;

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;

    @Inject
    SharedPreferences sharedPrefs;
    private int tripPosition;
    private TripAdapter adapter;
    private GoogleApiClient googleApiClient;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //TODO: determine if this needs ot happen in fragment lifecyle overrides or if MainActivity covers this
        buildGoogleApiClient();

        View view = inflater.inflate(R.layout.fragment_trip_list, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        ((EvApplication) getActivity().getApplication()).evComponent.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onStart() {
        super.onStart();
        new SumLoggedTripsTask(getActivity()).execute();
        googleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter = new TripAdapter(getContext());

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setReverseLayout(true);

        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(recyclerView.getContext(), mLayoutManager.getOrientation());

        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setAdapter(adapter);

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
                int newTripArraySize = adapter.removeTrip(tripPosition);
                adapter.notifyItemRemoved(tripPosition);
                adapter.notifyItemRangeChanged(tripPosition, newTripArraySize);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] TRIP_COLUMNS = {
                Contract.Trip._ID,
                Contract.Trip.COLUMN_DATE,
                Contract.Trip.COLUMN_TRIP_MILES,
                Contract.Trip.COLUMN_TRIP_SAVINGS
        };

        return new CursorLoader(getContext(),
                Contract.Trip.uri,
                TRIP_COLUMNS,
                null,
                null,
                null);

    }

    //TODO: is loadermanager needed?
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
//        adapter.swapCursor(data);
        adapter.notifyDataSetChanged();

        if (data.getCount() != 0) {
            errorTextView.setVisibility(View.GONE);
            noTripsYetTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
//        adapter.swapCursor(null);
        adapter.notifyDataSetChanged();

    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            //TODO is this necessary if its being done in MainActivity
            boolean isTracking = sharedPrefs.getBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, false);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, !isTracking);
            editor.apply();
        } else {
            Toast.makeText(getContext(), getString(R.string.no_gps_data), Toast.LENGTH_SHORT).show();
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
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
         Log.e(this.getTag(), "Connection failed: " + result.getErrorCode());
    }

    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .build();
    }

}
