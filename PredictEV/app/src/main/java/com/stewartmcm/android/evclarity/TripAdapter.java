package com.stewartmcm.android.evclarity;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.stewartmcm.android.evclarity.activities.Main2Activity;
import com.stewartmcm.android.evclarity.data.Contract;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by stewartmcmillan on 5/26/16.
 */
public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripAdapterViewHolder> {
    final private Context mContext;
    LayoutInflater cursorInflater;
    private Cursor mCursor;
    final private View mEmptyView;
    final private TripAdapterOnClickHandler mClickHandler;
    final private ItemChoiceManager mICM;

    public class TripAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.mileage_text_view)
        TextView mileage;

        TextView dateTime;
        TextView savings;

        public TripAdapterViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            int symbolColumn = mCursor.getColumnIndex(Contract.Trip.COLUMN_DATE);
            mClickHandler.onClick(mCursor.getString(symbolColumn), this);
            mICM.onClick(this);

        }
    }
    public static interface TripAdapterOnClickHandler {
        void onClick(String symbol, TripAdapterViewHolder viewHolder);
    }

    public TripAdapter(Context context, TripAdapterOnClickHandler clickHandler, View emptyView, int choiceMode) {
        mContext = context;
        mClickHandler = clickHandler;
        mEmptyView = emptyView;
        mICM = new ItemChoiceManager(this);
        mICM.setChoiceMode(choiceMode);
    }

    @Override
    public TripAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.mileage_item, parent, false);

        return new TripAdapterViewHolder(item);
    }

    @Override
    public void onBindViewHolder(TripAdapterViewHolder holder, int position) {

        mCursor.moveToPosition(position);

        //TODO: build contract and content provider for trip data
        holder.mileage.setText(mCursor.getInt(Main2Activity.COL_TRIP_MILES));
//        holder.dateTime.setText(dollarFormat.format(mCursor.getFloat(Contract.Quote.POSITION_PRICE)));
//        holder.savings.setText(dollarFormat.format(mCursor.getFloat(Contract.Quote.POSITION_PRICE)));

        mICM.onBindViewHolder(holder, position);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mICM.onRestoreInstanceState(savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) {
        mICM.onSaveInstanceState(outState);
    }

    public int getSelectedItemPosition() {
        return mICM.getSelectedItemPosition();
    }

    public void setCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

//    public String getTripAtPosition(int position) {
//
//        mCursor.moveToPosition(position);
//        return mCursor.getString(Main2Activity.COL_DATE);
//    }

    @Override
    public int getItemCount() {
        int count = 0;
        if (mCursor != null) {
            count = mCursor.getCount();
        }
        return count;
    }

    public void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
        notifyDataSetChanged();
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void selectView(RecyclerView.ViewHolder viewHolder) {
        if ( viewHolder instanceof TripAdapterViewHolder ) {
            TripAdapterViewHolder vfh = (TripAdapterViewHolder)viewHolder;
            vfh.onClick(vfh.itemView);
        }
    }

//    =========== Old ListView code below; new RecyclerView code above

//    @Override
//    public void bindView(View view, Context context, Cursor cursor) {
//        TextView tripMileageTextView = (TextView) view.findViewById(R.id.mileage_text_view);
////        TextView dateTextView = (TextView) view.findViewById(R.id.date_text_view);
//        // might not need textview below
//        TextView staticMilesTextView;
//
//        String mileage = cursor.getString( cursor.getColumnIndex( PredictEvDatabaseHelper.COL_TRIP_MILES ) );
//        tripMileageTextView.setText(mileage + " miles");
//
////        String date = cursor.getString( cursor.getColumnIndex(PredictEvDatabaseHelper.COL_DATE));
////        dateTextView.setText(date);
//    }
//
//    @Override
//    public View newView(Context context, Cursor cursor, ViewGroup parent) {
//
//        return cursorInflater.inflate(R.layout.mileage_item, parent, false);
//    }
}