package com.stewartmcm.evclarity;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stewartmcm.evclarity.db.Contract;
import com.stewartmcm.evclarity.db.PredictEvDatabaseHelper;
import com.stewartmcm.evclarity.model.Trip;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripAdapterViewHolder> {

    private static final int COL_DATE = 1;
    private static final int COL_TRIP_MILES = 7;
    private static final int COL_TRIP_SAVINGS = 8;
    private static final String TAG = "TRIP_ADAPTER";

    private Cursor cursor;
    private ArrayList<Trip> trips;

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

    //TODO: use factory method instead
    public TripAdapter(Context context) {
        PredictEvDatabaseHelper helper = PredictEvDatabaseHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        cursor = db.query(Contract.Trip.TABLE_NAME, null, null, null, null, null, null);
        cursor.moveToFirst();

        trips = new ArrayList<>();
        for (int i = 0; i < cursor.getCount(); i++) {
            Trip trip = new Trip(cursor.getString(COL_DATE),
                    cursor.getFloat(COL_TRIP_MILES),
                    cursor.getFloat(COL_TRIP_SAVINGS));

            trips.add(trip);
            cursor.moveToNext();
        }
        cursor.close();
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

    public int removeTrip(int tripPosition) {
        trips.remove(tripPosition);
        return trips.size();
    }

    public void swapCursor(Cursor newCursor) {
        cursor = newCursor;
        notifyDataSetChanged();
    }

}