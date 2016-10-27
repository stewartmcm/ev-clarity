package com.stewartmcm.android.evclarity;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.predictev.R;

/**
 * Created by stewartmcmillan on 5/26/16.
 */
public class TripsLoggedCursorAdapter extends CursorAdapter {
    LayoutInflater cursorInflater;

    public TripsLoggedCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        cursorInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView tripMileageTextView = (TextView) view.findViewById(R.id.mileage_text_view);
//        TextView dateTextView = (TextView) view.findViewById(R.id.date_text_view);
        // might not need textview below
        TextView staticMilesTextView;

        String mileage = cursor.getString( cursor.getColumnIndex( PredictEvDatabaseHelper.COL_TRIP_MILES ) );
        tripMileageTextView.setText(mileage + " miles");

//        String date = cursor.getString( cursor.getColumnIndex(PredictEvDatabaseHelper.COL_DATE));
//        dateTextView.setText(date);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        return cursorInflater.inflate(R.layout.mileage_item, parent, false);
    }
}