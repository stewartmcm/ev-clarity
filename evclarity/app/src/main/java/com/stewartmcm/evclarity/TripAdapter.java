package com.stewartmcm.evclarity;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.stewartmcm.evclarity.db.PredictEvDatabaseHelper;
import com.stewartmcm.evclarity.fragment.TripListFragment;
import com.stewartmcm.evclarity.model.Trip;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripAdapterViewHolder> {

    static final String TAG = "TRIP_ADAPTER";
    final private Context mContext;
    private Cursor mCursor;
    private ArrayList<Trip> mTrips;
    final private View mEmptyView;
    final private TripAdapterOnClickHandler mClickHandler;



    public static class TripAdapterViewHolder extends RecyclerView.ViewHolder {
        TextView mTotalSavingsTextView;
        TextView mTotalMileageTextView;
        TextView mMileageTextView;
        TextView mDateTimeTextView;
        TextView mSavingsTextView;

        public TripAdapterViewHolder(View itemView) {
            super(itemView);

            mTotalSavingsTextView = (TextView) itemView.findViewById(R.id.savings_text_view);
            mTotalMileageTextView = (TextView) itemView.findViewById(R.id.total_mileage_textview);

            mMileageTextView = (TextView) itemView.findViewById(R.id.list_item_mileage);
            mDateTimeTextView = (TextView) itemView.findViewById(R.id.list_item_date);
            mSavingsTextView = (TextView) itemView.findViewById(R.id.list_item_savings);
        }
    }
    public static interface TripAdapterOnClickHandler {
        void onClick(String dateTime, TripAdapterViewHolder viewHolder);
    }

    //TODO: use factory method instead
    public TripAdapter(Context context, TripAdapterOnClickHandler clickHandler, View emptyView) {
        mContext = context;
        mClickHandler = clickHandler;
        mEmptyView = emptyView;

        mTrips = new ArrayList<>();

        SQLiteDatabase db = null;
        PredictEvDatabaseHelper mHelper = PredictEvDatabaseHelper.getInstance(mContext);
        db = mHelper.getReadableDatabase();

        mCursor = db.query("TRIP", null, null, null, null, null, null);
        mCursor.moveToFirst();

        for (int i = 0; i < mCursor.getCount(); i++) {

            Trip trip = new Trip(mCursor.getString(TripListFragment.COL_DATE),
                    mCursor.getFloat(TripListFragment.COL_TRIP_MILES),
                    mCursor.getFloat(TripListFragment.COL_TRIP_SAVINGS));

            mTrips.add(trip);
            mCursor.moveToNext();
        }
        mCursor.close();
    }

    @Override
    public TripAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_trip, parent, false);

        return new TripAdapterViewHolder(item);
    }

    @Override
    public void onBindViewHolder(TripAdapterViewHolder holder, int position) {

        float miles = mTrips.get(position).getMiles();
        float savings = mTrips.get(position).getSavings();

        DecimalFormat milesFormat = new DecimalFormat("##0.0");
        DecimalFormat savingsFormat = new DecimalFormat("###.00");

        String savingsString = savingsFormat.format(savings);
        Log.i(TAG, "doInBackground: " + savingsString);

        String milesString = milesFormat.format(miles);
        Log.i(TAG, "doInBackground: " + savingsString);

        holder.mMileageTextView.setText(milesString + " miles");
        holder.mSavingsTextView.setText("$" + savingsString);
        holder.mDateTimeTextView.setText(mTrips.get(position).getTimeStamp());
    }

    public void onSaveInstanceState(Bundle outState) {
//        mICM.onSaveInstanceState(outState);
    }

    public void setCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

//    public String getTripAtPosition(int position) {
//
//        mCursor.moveToPosition(position);
//        return mCursor.getString(MainActivity.COL_DATE);
//    }

    @Override
    public int getItemCount() {
        return mTrips.size();
    }

    public int removeTrip(int tripPosition) {
        mTrips.remove(tripPosition);

        return mTrips.size();
        //todo:consider returning new array size?
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public Cursor getCursor() {
        return mCursor;
    }
}