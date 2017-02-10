package com.stewartmcm.android.evclarity.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static com.stewartmcm.android.evclarity.data.Contract.Trip.getStockFromUri;


public class TripProvider extends ContentProvider {

    private static final int TRIP = 100;
    private static final int TRIP_FOR_DATETIME = 101;

    private static final UriMatcher uriMatcher = buildUriMatcher();

    private PredictEvDatabaseHelper dbHelper;

    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(Contract.AUTHORITY, Contract.PATH_TRIP, TRIP);
        matcher.addURI(Contract.AUTHORITY, Contract.PATH_TRIP_WITH_DATETIME, TRIP_FOR_DATETIME);
        return matcher;
    }


    @Override
    public boolean onCreate() {
        dbHelper = new PredictEvDatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor returnCursor;
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        switch (uriMatcher.match(uri)) {
            case TRIP:
                returnCursor = db.query(
                        Contract.Trip.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;

            case TRIP_FOR_DATETIME:
                returnCursor = db.query(
                        Contract.Trip.TABLE_NAME,
                        projection,
                        Contract.Trip.COLUMN_DATE + " = ?",
                        new String[]{getStockFromUri(uri)},
                        null,
                        null,
                        sortOrder
                );

                break;
            default:
                throw new UnsupportedOperationException("Unknown URI:" + uri);
        }

        Context context = getContext();
        if (context != null){
            returnCursor.setNotificationUri(context.getContentResolver(), uri);
        }

        return returnCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Uri returnUri;

        switch (uriMatcher.match(uri)) {
            case TRIP:
                db.insert(
                        Contract.Trip.TABLE_NAME,
                        null,
                        values
                );
                returnUri = Contract.Trip.uri;
                break;
            default:
                throw new UnsupportedOperationException("Unknown URI:" + uri);
        }

        Context context = getContext();
        if (context != null){
            context.getContentResolver().notifyChange(uri, null);
        }

        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted;

        if (null == selection) {
            selection = "1";
        }
        switch (uriMatcher.match(uri)) {
            case TRIP:
                rowsDeleted = db.delete(
                        Contract.Trip.TABLE_NAME,
                        selection,
                        selectionArgs
                );

                break;

            case TRIP_FOR_DATETIME:
                String symbol = Contract.Trip.getStockFromUri(uri);
                rowsDeleted = db.delete(
                        Contract.Trip.TABLE_NAME,
                        '"' + symbol + '"' + " =" + Contract.Trip.COLUMN_DATE,
                        selectionArgs
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown URI:" + uri);
        }

        if (rowsDeleted != 0) {
            Context context = getContext();
            if (context != null){
                context.getContentResolver().notifyChange(uri, null);
            }
        }

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {

        final SQLiteDatabase db = dbHelper.getWritableDatabase();

        switch (uriMatcher.match(uri)) {
            case TRIP:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        db.insert(
                                Contract.Trip.TABLE_NAME,
                                null,
                                value
                        );
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                Context context = getContext();
                if (context != null) {
                    context.getContentResolver().notifyChange(uri, null);
                }

                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }


    }
}
