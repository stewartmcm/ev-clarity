package com.stewartmcm.android.evclarity.data;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.stewartmcm.android.evclarity.R;

public class TripAdapterViewHolder extends RecyclerView.ViewHolder {
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
