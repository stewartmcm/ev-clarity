package com.stewartmcm.evclarity

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stewartmcm.evclarity.db.PredictEvDatabaseHelper
import com.stewartmcm.evclarity.model.Trip
import java.text.DecimalFormat
import java.util.*

class TripAdapter(context: Context) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {
    private val trips: ArrayList<Trip>

    init {
        val helper = PredictEvDatabaseHelper(context)
        trips = helper.allTrips
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val item = LayoutInflater.from(parent.context).inflate(R.layout.list_item_trip, parent, false)
        return TripViewHolder(item)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val miles = trips[position].miles
        val savings = trips[position].savings

        val milesFormat = DecimalFormat("##0.0")
        val savingsFormat = DecimalFormat("###.00")

        val savingsString = savingsFormat.format(savings.toDouble())
        Log.i(TAG, "savings: $savingsString")

        val milesString = milesFormat.format(miles.toDouble())
        Log.i(TAG, "mileage: $milesString")

        holder.mileageTextView.text = "$milesString miles"
        holder.savingsTextView.text = "$$savingsString"
        holder.dateTimeTextView.text = trips[position].timeStamp
    }

    override fun getItemCount(): Int {
        return trips.size
    }

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var mileageTextView: TextView
        var dateTimeTextView: TextView
        var savingsTextView: TextView

        init {
            mileageTextView = itemView.findViewById<View>(R.id.list_item_mileage) as TextView
            dateTimeTextView = itemView.findViewById<View>(R.id.list_item_date) as TextView
            savingsTextView = itemView.findViewById<View>(R.id.list_item_savings) as TextView
        }

    }

    fun removeTrip(tripPosition: Int): Int {
        trips.removeAt(tripPosition)
        return trips.size
    }

    companion object {
        private val TAG = "TRIP_ADAPTER"
    }

}