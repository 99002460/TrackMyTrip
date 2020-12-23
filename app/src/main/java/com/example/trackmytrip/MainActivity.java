package com.example.trackmytrip;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    //Splash Screen

    //Initialize Map Variables
    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient client;
    Button startButton, stopButton, showTrips;
    Timer t = new Timer();
    Calendar c = Calendar.getInstance();
    GoogleMap mMap;
    MyDataBaseHelper myDB;
    LocationBroadcastReceiver locationBroadcastReceiver;

    SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
    TimerTask tt;
    protected LatLng latLng;
    private double latitude;
    private double longitude;
    private double velocity;
    LocationManager locationManager;
    ImageButton imageButton;
    public int spinnerValue = 1000;
    //pop window
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private EditText tripName;
    private String stripName;
    private String var_tripName;
    private String var_Timer;
    private Button done;
    LatLng latlng;
    public ArrayList<String> locations;
    ArrayList<Double> tripLatitude;
    ArrayList<Double> tripLongitude;
    String[] timer = {"Choose time interval", "5 seconds", "10 seconds", "30 seconds", "1 minutes", "5 minutes"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myDB = new MyDataBaseHelper(MainActivity.this);
        tripLatitude = new ArrayList<>();
        tripLongitude = new ArrayList<>();
        locationBroadcastReceiver = new LocationBroadcastReceiver();

        //Assign Map variables
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync((OnMapReadyCallback) this);

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            //requestlocation
            {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                // request permission for location
                //getCurrentLocation();
            }
        }

        //Initialize fused Location
        client = LocationServices.getFusedLocationProviderClient(this);

        //initialize buttons
        startButton = findViewById(R.id.start);
        stopButton = findViewById(R.id.stop);
        showTrips = findViewById(R.id.tripsDB);
        imageButton = findViewById(R.id.imageButton);

        getFirstLocation();
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMap.clear();
                getCurrentLocation();
                addMarkerLive(latlng);

            }
        });

        showTrips.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TripRecyclerView.class);
                startActivity(intent);

            }
        });

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                getCurrentLocation();
                addMarkerLive(latlng);
                dialogBuilder = new AlertDialog.Builder(MainActivity.this, R.style.CustomAlertDialog);
                final View tripPopupView = getLayoutInflater().inflate(R.layout.popup, null);
                dialogBuilder.setTitle("                           Trip Details");
                tripName = (EditText) tripPopupView.findViewById(R.id.TripName);
                stripName = tripName.getText().toString();
                done = (Button) tripPopupView.findViewById(R.id.doneButton);

                //Getting the instance of Spinner and applying OnItemSelectedListener on it
                Spinner spin = (Spinner) tripPopupView.findViewById(R.id.spinner);
                //spin.setOnItemSelectedListener(this);
                //Creating the ArrayAdapter instance having the list
                ArrayAdapter aa = new ArrayAdapter(MainActivity.this, android.R.layout.simple_spinner_item, timer);
                aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                //Setting the ArrayAdapter data on the Spinner
                spin.setAdapter(aa);
                done.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onClick(View v) {


                        //define done button of popup
                        if (!(spin.getSelectedItem().toString().equalsIgnoreCase("Choose time interval") && tripName != null)) {
                            startTrip();
                        } else {
                            spin.setSelection(2);
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                            LocalDateTime now = LocalDateTime.now();
                            Toast.makeText(getApplicationContext(), "Details not entered, Default Name Applied", Toast.LENGTH_LONG).show();
                            tripName = (EditText) tripPopupView.findViewById(R.id.TripName);
                            tripName.setText("Trip on " + dtf.format(now));
                            startTrip();

                        }
                    }

                    @RequiresApi(api = Build.VERSION_CODES.O)
                    private void startTrip() {
                        startService();
                        stopButton.setEnabled(true);
                        startButton.setEnabled(false);
                        imageButton.setEnabled(false);

                        var_tripName = tripName.getText().toString();

                        var_Timer = spin.getSelectedItem().toString();

                        dialog.dismiss();

                        switch (var_Timer) {
                            case "5 seconds":
                                spinnerValue = 5000;
                                break;

                            case "30 seconds":
                                spinnerValue = 30000;
                                break;

                            case "1 minutes":
                                spinnerValue = 60000;
                                break;

                            case "5 minutes":
                                spinnerValue = 300000;
                                break;

                            default:
                                spinnerValue = 10000;
                                break;
                        }
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                        LocalDateTime now = LocalDateTime.now();
                        String f1 = dtf.format(now);
                        MyDataBaseHelper myDataBaseHelper = new MyDataBaseHelper(MainActivity.this);
                        //setting the time for updates
                        LocationService.TIMER_REAL = spinnerValue;
                        myDataBaseHelper.addTrip(var_tripName, f1, spinnerValue);
                        locations = new ArrayList<>();
                        DateTimeFormatter dtfnew = DateTimeFormatter.ofPattern("HH:mm:ss");
                        String time = dtfnew.format(now);
                        myDataBaseHelper.addLocation(time, latitude, longitude, f1, velocity);
                        //creation of timer for updating every interval

                        tt = new TimerTask() {
                            @Override
                            public void run() {
                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
                                LocalDateTime now = LocalDateTime.now();
                                String time = dtf.format(now);
                                displayLocations(f1);

                                //Live Plotting
                                Double getLatitude = tripLatitude.get(tripLatitude.size() - 1);
                                Double getLongitude = tripLongitude.get(tripLongitude.size() - 1);
                                LatLng previousLocation = new LatLng(getLatitude, getLongitude);
                                LatLng newLocation = new LatLng(latitude, longitude);
                                //add new location to database
                                myDataBaseHelper.addLocation(time, latitude, longitude, f1, velocity);

                                //polyline on Ui thread as timer is running on main thread

                                runOnUiThread(() -> {
                                    PolylineOptions line = new PolylineOptions().add(previousLocation, newLocation).width(10).color(Color.RED);
                                    mMap.addPolyline(line);
                                    addMarkerLive(newLocation);

                                });
                                getCurrentLocation();

                            }
                        };

                        t.scheduleAtFixedRate(tt, spinnerValue, spinnerValue);

                        Toast.makeText(getApplicationContext(), "Trip has Started!", Toast.LENGTH_LONG).show();
                    }

                });

                dialogBuilder.setView(tripPopupView);
                dialog = dialogBuilder.create();
                dialog.show();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                imageButton.setEnabled(true);
                unregisterReceiver(locationBroadcastReceiver);
                if (tt == null) {
                    Toast.makeText(getApplicationContext(), "Trip should start to end!!", Toast.LENGTH_SHORT).show();
                } else {
                    mMap.clear();
                    tt.cancel();
                    Toast.makeText(getApplicationContext(), "Trip Ended!!", Toast.LENGTH_LONG).show();
                    getCurrentLocation();
                    addMarkerLive(latlng);
                }
            }
        });

    }


    public void startService() {
        //register broadcast receiver
        IntentFilter intentFilter = new IntentFilter("ACT_LOC");
        registerReceiver(locationBroadcastReceiver, intentFilter);
        Intent intent = new Intent(MainActivity.this, LocationService.class);
        startService(intent);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    public class LocationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("ACT_LOC")) {
                double lat = intent.getDoubleExtra("latitude", 0f);
                double lon = intent.getDoubleExtra("longitude", 0f);
                if (mMap != null) {
                }
                //Toast.makeText(MainActivity.this,"Latitude is: "+ lat+" Longitude is :"+lon,Toast.LENGTH_SHORT).show();
            }
        }
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

    private void getCurrentLocation() {
        //Initialize Task Location

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {

                            latlng = new LatLng(location.getLatitude(), location.getLongitude());
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            velocity = location.getSpeed();

                        }
                    });
                }
            }
        });
    }

    private void getFirstLocation() {
        //Initialize Task Location

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {

                            latlng = new LatLng(location.getLatitude(), location.getLongitude());
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            velocity = location.getSpeed();

                            // Creating a marker
                            MarkerOptions options = new MarkerOptions()
                                    .position(latlng)
                                    .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.icon_marker));

                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 19));

                            googleMap.addMarker(options);

                        }
                    });
                }
            }
        });
    }

    private void addMarkerLive(LatLng latlng) {
        // Creating a marker
        MarkerOptions options = new MarkerOptions()
                .position(latlng)
                .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.icon_marker));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 19));

        mMap.addMarker(options);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 44) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }

    //request location from database for live plotting
    private void displayLocations(String variable_tripname) {

        Cursor c = myDB.readAllLocations(variable_tripname);
        if (c.getCount() == 0) {
            Toast.makeText(this, "No Data in array", Toast.LENGTH_LONG).show();
        } else {
            while (c.moveToNext()) {
                tripLongitude.add(c.getDouble(3));
                tripLatitude.add(c.getDouble(2));

            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }
}