package com.example.trackmytrip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener {
    FusedLocationProviderClient client;
    private Context mContext;
    private EditText searchPlaceEt;
    private Button saveLocationBtn;
    private GoogleMap mMap;
    public String variableTripName, variableTripInterval, variableTripTime;
    private GoogleApiClient mGoogleApiClient;
    MyDataBaseHelper myDB;
    ArrayList<String> tripName, tripDate, tripInterval;
    ArrayList<Double> tripLatitude;
    ArrayList<Double> tripLongitude;
    TextView distanceTotal, speedTotal, timeTotal;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Initialize fused Location
        client = LocationServices.getFusedLocationProviderClient(this);
        //textview initialization
        distanceTotal = findViewById(R.id.distanceTotal);
        speedTotal = findViewById(R.id.speedTotal);
        timeTotal = findViewById(R.id.timeTotal);
        //get data from database
        myDB = new MyDataBaseHelper(MapsActivity.this);
        tripName = new ArrayList<>();
        tripInterval = new ArrayList<>();
        tripDate = new ArrayList<>();
        tripLatitude = new ArrayList<Double>();
        tripLongitude = new ArrayList<Double>();

        //sync of the database to the respective array list
        displayData();
        mContext = MapsActivity.this;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                if (mMap != null)
                    mMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                //checkLocationPermission();
            }
        }
    }

    private void displayData() {
        Cursor cursor = myDB.readAllData();
        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No Data", Toast.LENGTH_LONG).show();
        } else {
            while (cursor.moveToNext()) {
                tripName.add(cursor.getString(0));
                tripDate.add(cursor.getString(2));
                tripInterval.add(cursor.getString(1));

            }
        }
    }

    private void displayLocations(String variable_tripname) {

        Cursor c = myDB.readAllLocations(variable_tripname);
        if (c.getCount() == 0) {
            Toast.makeText(this, "No Data in array", Toast.LENGTH_LONG).show();
        } else {
            while (c.moveToNext()) {

                tripLatitude.add(c.getDouble(2));
                tripLongitude.add(c.getDouble(3));
            }
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        //receiving the intent from recyclerview on click of a button
        double getLatitude, getLongitude;
        ArrayList<LatLng> latsAndLong = new ArrayList<>();
        double dist;
        double totalDist = 0;
        float speed;
        float time;
        Intent intent = getIntent();
        int string = intent.getIntExtra("positionOfTrip", 0);

        //select trip to view on map
        variableTripName = tripName.get(string);
        variableTripInterval = tripInterval.get(string);
        variableTripTime = tripDate.get(string);

        time = (Float.parseFloat(variableTripInterval)) / 1000;
        Log.d("time", "" + time);

        //get locations for selected trip
        displayLocations(variableTripTime);

        Log.d("triplatitude", tripLatitude.toString());
        Log.d("triplongitude", tripLongitude.toString());


        System.out.println(tripLatitude.size());
        System.out.println(tripLongitude.size());

        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        for (int i = 0; i < tripLatitude.size(); i++) {
            getLatitude = tripLatitude.get(i);
            getLongitude = tripLongitude.get(i);

            LatLng latLng = new LatLng(getLatitude, getLongitude);
            System.out.println(latLng.latitude);
            System.out.println(latLng.longitude);
            latsAndLong.add(latLng);
        }

        //first location
        mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
        mMap.setBuildingsEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latsAndLong.get(0), 19));
        MarkerOptions optionsfirst = new MarkerOptions().position(latsAndLong.get(0)).title("This is first location");
        mMap.addMarker(optionsfirst);

        for (int j = 0, k = 0; j < latsAndLong.size() - 1; j++, k++) {
            //conversion of m/s to km/hr
            dist = 0;
            speed = 0;
            dist = SphericalUtil.computeDistanceBetween(latsAndLong.get(k), latsAndLong.get(k + 1));
            speed = (float) (dist / time);

            totalDist += dist;
            float speedInKilometersPerHour = (float) (speed * 3.6);
            System.out.println(speedInKilometersPerHour);
            int markerDrawable = 0;
            int color = 0;
            //adding lines and markers according to speeds travelled
            if (speedInKilometersPerHour <= 5.0f)//less than 10kmph for walking
            {
                markerDrawable = R.drawable.icon_marker_grey;
                color = Color.GRAY;

            } else if (speedInKilometersPerHour > 5.0f && speedInKilometersPerHour <= 15.0f)  //greater than 10kmph less than 20kmph
            {
                markerDrawable = R.drawable.icon_marker_green;
                color = Color.GREEN;

            } else if (speedInKilometersPerHour > 15.0f && speedInKilometersPerHour <= 25.0f)  //greater than 20kmph less than 30kmph
            {
                markerDrawable = R.drawable.icon_marker_yellow;
                color = Color.YELLOW;
            } else if (speedInKilometersPerHour > 25.0f && speedInKilometersPerHour <= 35.0f)  //greater than 30kmph less than 40kmph
            {
                markerDrawable = R.drawable.icon_marker_magenta;
                color = Color.MAGENTA;

            } else {  //greater than 35kmph
                markerDrawable = R.drawable.icon_marker_red;
                color = Color.RED;
            }

            MarkerOptions options = new MarkerOptions()
                    .position(latsAndLong.get(j + 1))
                    .icon(bitmapDescriptorFromVector(getApplicationContext(), markerDrawable));
            mMap.addMarker(options);
            PolylineOptions line = new PolylineOptions().add((latsAndLong.get(j)), latsAndLong.get(j + 1)).width(10).color(color);
            mMap.addPolyline(line);

        }


        //total average velocity and total distance calculation
        totalDist = totalDist / 1000;//m to km
        time = time * (tripLatitude.size()) - 1;//actual interval
        time = time / 3600;//total time
        speed = (float) (totalDist / time);//kmph
        distanceTotal.setText("" + String.format("%.3f", totalDist) + " Km");
        timeTotal.setText("" + String.format("%.3f", time) + " Hours");
        speedTotal.setText("" + String.format("%.3f", speed) + " Kmph");

        //last location
        MarkerOptions optionslast = new MarkerOptions().position(latsAndLong.get(latsAndLong.size() - 1)).title("This is last location");
        mMap.addMarker(optionslast);

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //marker design
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
