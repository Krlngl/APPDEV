package com.example.firebasecdrud;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ClientActivity extends AppCompatActivity {
    private RecyclerView rv;
    private ArrayList<DataModel> dataList = new ArrayList<>();
    private CustomAdapter adapter;
    private TextView total_records;
    private TextView welcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        // Initialize views
        rv = findViewById(R.id.rv);
        total_records = findViewById(R.id.total_records);
        welcomeText = findViewById(R.id.welcomeText);

        // Setup RecyclerView
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CustomAdapter(this, dataList);
        rv.setAdapter(adapter);

        // Set welcome message
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            welcomeText.setText("Welcome, " + currentUser.getEmail());
        }

        // Load all available rooms
        loadAvailableRooms();
    }

    private void loadAvailableRooms() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("rooms");
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dataList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    DataModel room = dataSnapshot.getValue(DataModel.class);
                    if (room != null && room.isAvailable()) {
                        room.setKey(dataSnapshot.getKey());
                        dataList.add(room);
                    }
                }
                total_records.setText("Available Rooms: " + dataList.size());
                adapter.updateData(dataList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ClientActivity.this, "Failed to load rooms", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 