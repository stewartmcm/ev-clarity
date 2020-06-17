package com.stewartmcm.evclarity.fragment

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.LocationServices
import com.stewartmcm.evclarity.*
import com.stewartmcm.evclarity.db.Contract
import kotlinx.android.synthetic.main.fragment_trip_list.*
import javax.inject.Inject

class TripListFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    @Inject
    lateinit var sharedPrefs: SharedPreferences
    private var tripPosition: Int = 0
    private var adapter: TripAdapter? = null
    private var googleApiClient: GoogleApiClient? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        //TODO: determine if this needs ot happen in fragment lifecyle overrides or if MainActivity covers this
        buildGoogleApiClient()
        return inflater.inflate(R.layout.fragment_trip_list, container, false)
    }

    override fun onAttach(context: Context) {
        (requireActivity().application as EvApplication).evComponent.inject(this)
        super.onAttach(context)
    }

    override fun onStart() {
        super.onStart()
        SumLoggedTripsTask(activity).execute()
        googleApiClient!!.connect()
    }

    override fun onResume() {
        super.onResume()
        adapter = TripAdapter(requireContext())

        val mLayoutManager = LinearLayoutManager(context)
        mLayoutManager.reverseLayout = true

        val dividerItemDecoration = DividerItemDecoration(recycler_view.context, mLayoutManager.orientation)

        recycler_view.layoutManager = mLayoutManager
        recycler_view.addItemDecoration(dividerItemDecoration)
        recycler_view.adapter = adapter

        val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                tripPosition = viewHolder.adapterPosition
                DeleteTripTask(context).execute(tripPosition)
                SumLoggedTripsTask(activity).execute()
                val newTripArraySize = adapter!!.removeTrip(tripPosition)
                adapter!!.notifyItemRemoved(tripPosition)
                adapter!!.notifyItemRangeChanged(tripPosition, newTripArraySize)
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(recycler_view)
    }

    override fun onStop() {
        super.onStop()
        googleApiClient!!.disconnect()
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val TRIP_COLUMNS = arrayOf(Contract.Trip._ID, Contract.Trip.COLUMN_DATE, Contract.Trip.COLUMN_TRIP_MILES, Contract.Trip.COLUMN_TRIP_SAVINGS)

        return CursorLoader(requireContext(),
                Contract.Trip.uri,
                TRIP_COLUMNS,
                null, null, null)

    }

    //TODO: is loadermanager needed?
    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        adapter!!.notifyDataSetChanged()

        if (data.count != 0) {
            error_text_view.visibility = View.GONE
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter!!.notifyDataSetChanged()

    }

    override fun onResult(status: Status) {
        if (status.isSuccess) {
            //TODO is this necessary if its being done in MainActivity
            val isTracking = sharedPrefs!!.getBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, false)
            val editor = sharedPrefs!!.edit()
            editor.putBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, !isTracking)
            editor.apply()
        } else {
            Toast.makeText(context, getString(R.string.no_gps_data), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnected(connectionHint: Bundle?) {
        val permissionCheck = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionCheck != PackageManager.PERMISSION_GRANTED && !ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
            val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(requireActivity(),
                    permissions,
                    Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION)
        }
    }

    override fun onConnectionSuspended(cause: Int) {
        googleApiClient!!.connect()
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        Log.e(this.tag, "Connection failed: " + result.errorCode)
    }

    @Synchronized
    private fun buildGoogleApiClient() {
        googleApiClient = GoogleApiClient.Builder(requireContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .build()
    }

}
