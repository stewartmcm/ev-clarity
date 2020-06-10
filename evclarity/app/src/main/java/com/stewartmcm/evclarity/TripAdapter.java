package com.stewartmcm.evclarity;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stewartmcm.evclarity.db.PredictEvDatabaseHelper;
import com.stewartmcm.evclarity.model.Trip;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripAdapterViewHolder> {

    private static final String TAG = "TRIP_ADAPTER";
    private ArrayList<Trip> trips;

    public TripAdapter(Context context) {
        PredictEvDatabaseHelper helper = PredictEvDatabaseHelper.getInstance(context);
        trips = helper.getAllTrips();
    }

    @NonNull
    @Override
    public TripAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_trip, parent, false);
        return new TripAdapterViewHolder(item);
    }

    @Override
    public void onBindViewHolder(TripAdapterViewHolder holder, int position) {
        float miles = trips.get(position).getMiles();
        float savings = trips.get(position).getSavings();

        DecimalFormat milesFormat = new DecimalFormat("##0.0");
        DecimalFormat savingsFormat = new DecimalFormat("###.00");

        String savingsString = savingsFormat.format(savings);
        Log.i(TAG, "savings: " + savingsString);

        String milesString = milesFormat.format(miles);
        Log.i(TAG, "mileage: " + milesString);

        holder.mileageTextView.setText(milesString + " miles");
        holder.savingsTextView.setText("$" + savingsString);
        holder.dateTimeTextView.setText(trips.get(position).getTimeStamp());
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    static class TripAdapterViewHolder extends RecyclerView.ViewHolder {
        TextView mileageTextView;
        TextView dateTimeTextView;
        TextView savingsTextView;
        TripAdapterViewHolder(View itemView) {
            super(itemView);
            mileageTextView = (TextView) itemView.findViewById(R.id.list_item_mileage);
            dateTimeTextView = (TextView) itemView.findViewById(R.id.list_item_date);
            savingsTextView = (TextView) itemView.findViewById(R.id.list_item_savings);
        }

    }

    public int removeTrip(int tripPosition) {
        trips.remove(tripPosition);
        return trips.size();
    }

}