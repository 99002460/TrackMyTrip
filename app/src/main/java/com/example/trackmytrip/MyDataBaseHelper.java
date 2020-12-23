package com.example.trackmytrip;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class MyDataBaseHelper extends SQLiteOpenHelper {
    private final Context context;

    public static final String DATABASE_NAME = "Trips.db";
    public static final int DATABASE_VERSION = 1;
    public static final String DATAPOINTS_TABLE = "datapoints_table";
    public static final String MASTER_TABLE = "master_table";
    public static final String TRIPNAME = "name";
    public static final String DATE = "uniqueid_date";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String VELOCITY = "velocity";
    public static final String TRIPINTERVAL = "interval";
    public static final String TIME = "time";


    public MyDataBaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE "
                + DATAPOINTS_TABLE + "("
                + DATE + " TEXT, "
                + TIME + " TEXT, "
                + LATITUDE + " double, "
                + LONGITUDE + " double, "
                + VELOCITY + " double );";

        String query_2 = "CREATE TABLE "
                + MASTER_TABLE + "("
                + TRIPNAME + " TEXT, "
                + TRIPINTERVAL + " TEXT, "
                + DATE + " TEXT );";

        db.execSQL(query);
        db.execSQL(query_2);

    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    void addLocation(String time, double latitude, double longitude, String date, double velocity) {

        SQLiteDatabase db;
        db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(TIME, time);
        cv.put(LATITUDE, String.valueOf(latitude));
        cv.put(LONGITUDE, String.valueOf(longitude));
        cv.put(DATE, date);
        cv.put(VELOCITY, String.valueOf(velocity));

        long result = db.insert(DATAPOINTS_TABLE, null, cv);
        if (result == -1) {
            Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
        }

    }

    void addTrip(String tripName, String date, int interval) {
        SQLiteDatabase db;
        db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(TRIPNAME, tripName);
        cv.put(DATE, date);
        cv.put(TRIPINTERVAL, interval);

        long result = db.insert(MASTER_TABLE, null, cv);
        if (result == -1) {
            Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
        }
    }

    Cursor readAllData() {

        String query = "SELECT * FROM " + MASTER_TABLE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(query, null);
        }
        return cursor;
    }

    Cursor readAllLocations(String tripTime) {

        String query = "SELECT * FROM  datapoints_table WHERE uniqueid_date='" + tripTime + "'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(query, null);
        }
        return cursor;
    }

    public void deleteRow(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(MASTER_TABLE, DATE + "='" + name + "' ;", null);
        db.delete(DATAPOINTS_TABLE, DATE + "='" + name + "' ;", null);
    }
}
