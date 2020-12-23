package com.example.trackmytrip;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TripRecyclerView extends AppCompatActivity {
    RecyclerView recyclerView;
    MyDataBaseHelper myDB;
    ArrayList<String> tripname, tripdate;
    CustomAdapter customAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_recycler_view);
        recyclerView = findViewById(R.id.tripsView);
        Intent intent = getIntent();

        myDB = new MyDataBaseHelper(TripRecyclerView.this);
        tripname = new ArrayList<>();
        tripdate = new ArrayList<>();

        displayData();

        customAdapter = new CustomAdapter(TripRecyclerView.this, tripname, tripdate, new ClickListerner() {
            @Override
            public void onDeleteClick(int position) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TripRecyclerView.this);
                builder.setMessage("Are you sure you want to delete?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                myDB.deleteRow(tripdate.get(position));
                                tripname.remove(position);
                                tripdate.remove(position);
                                customAdapter.notifyItemRemoved(position);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }

            @Override
            public void onPositionClicked(int position) {
                // callback performed on click

                Intent intent = new Intent(TripRecyclerView.this, MapsActivity.class);
                intent.putExtra("positionOfTrip", position);
                startActivity(intent);

            }
        });

        recyclerView.setAdapter(customAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(TripRecyclerView.this));
    }


    void displayData() {
        Cursor cursor = myDB.readAllData();
        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No Data", Toast.LENGTH_LONG).show();
        } else {
            while (cursor.moveToNext()) {
                tripdate.add(cursor.getString(2));
                tripname.add(cursor.getString(0));
            }
        }
    }
}