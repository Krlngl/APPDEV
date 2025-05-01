package com.example.firebasecdrud;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.firebasecdrud.ImageSliderAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.app.ProgressDialog;
import com.google.firebase.storage.StorageMetadata;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.EditorInfo;
import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity {

    private MaterialButton profileButton;
    private TextView total_records;
    private FloatingActionButton add;
    private RecyclerView rv;
    private ArrayList<DataModel> dataList = new ArrayList<>();
    private ArrayList<DataModel> filteredList = new ArrayList<>();
    private CustomAdapter adapter;
    
    // Image selection variables
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageSliderAdapter imageSliderAdapter;
    private List<Uri> selectedImages = new ArrayList<>();
    private TextInputEditText searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Not logged in, redirect to login activity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Check user role and redirect accordingly
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
            .child("users").child(currentUser.getUid());
        
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserModel user = snapshot.getValue(UserModel.class);
                if (user != null) {
                    // Continue with admin interface
                    setupAdminInterface();
                } else {
                    // If user doesn't exist in database, create as default user
                    UserModel newUser = new UserModel(
                        currentUser.getUid(),
                        currentUser.getEmail(),
                        currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User"
                    );
                    userRef.setValue(newUser);
                    setupAdminInterface();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error checking user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAdminInterface() {
        // Initialize views
        profileButton = findViewById(R.id.profileButton);
        total_records = findViewById(R.id.total_records);
        add = findViewById(R.id.btnadd);
        rv = findViewById(R.id.rv);
        searchBar = findViewById(R.id.searchBar);

        // Setup RecyclerView
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CustomAdapter(this, dataList);
        rv.setAdapter(adapter);

        // Setup profile button
        profileButton.setOnClickListener(v -> showProfileDialog());

        // Setup search functionality
        if (searchBar != null) {
            // Set search hint
            searchBar.setHint(getString(R.string.search_hint));
            
            // Setup editor action listener for keyboard search
            searchBar.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchBar.getText() != null ? searchBar.getText().toString().trim() : "";
                    filterRooms(query);
                    // Hide keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                    return true;
                }
                return false;
            });

            // Setup text change listener for real-time search
            searchBar.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim();
                    filterRooms(query);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        } else {
            Toast.makeText(this, "Search bar not initialized", Toast.LENGTH_SHORT).show();
        }

        // Load data from Firebase
        loadData();

        // Add button click listener
        add.setOnClickListener(v -> showAddDialog());
    }
    
    // Method to open image picker
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    // Handle the result from image picker
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    selectedImages.add(imageUri);
                    imageSliderAdapter.addImage(imageUri);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error selecting image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void filterRooms(String query) {
        if (dataList == null) {
            Toast.makeText(this, "Data not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        filteredList.clear();
        int availableCount = 0;
        int unavailableCount = 0;
        
        if (query.isEmpty()) {
            filteredList.addAll(dataList);
            for (DataModel room : dataList) {
                if (room.isAvailable()) {
                    availableCount++;
                } else {
                    unavailableCount++;
                }
            }
        } else {
            String lowerQuery = query.toLowerCase();
            for (DataModel room : dataList) {
                // Check building name, room number, description, and price
                String buildingName = room.getBuildingName() != null ? room.getBuildingName().toLowerCase() : "";
                String roomNumber = room.getRoomNumber() != null ? room.getRoomNumber().toLowerCase() : "";
                String roomDesc = room.getRoomDescription() != null ? room.getRoomDescription().toLowerCase() : "";
                String roomPrice = room.getRoomPrice() != null ? room.getRoomPrice().toLowerCase() : "";
                
                if (buildingName.contains(lowerQuery) ||
                    roomNumber.contains(lowerQuery) ||
                    roomDesc.contains(lowerQuery) ||
                    roomPrice.contains(lowerQuery)) {
                    filteredList.add(room);
                    if (room.isAvailable()) {
                        availableCount++;
                    } else {
                        unavailableCount++;
                    }
                }
            }
        }

        if (adapter != null) {
            adapter.updateData(filteredList);
            total_records.setText("Available: " + availableCount + " | Unavailable: " + unavailableCount);
        }
    }

    private void loadData() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("rooms");
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<DataModel> newDataList = new ArrayList<>();
                int availableCount = 0;
                int unavailableCount = 0;
                
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    DataModel dataModel = dataSnapshot.getValue(DataModel.class);
                    if (dataModel != null) {
                        newDataList.add(dataModel);
                        if (dataModel.isAvailable()) {
                            availableCount++;
                        } else {
                            unavailableCount++;
                        }
                    }
                }
                dataList = newDataList;
                filteredList = new ArrayList<>(dataList);
                adapter.updateData(filteredList);
                total_records.setText("Available: " + availableCount + " | Unavailable: " + unavailableCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_add_room, null);
        builder.setView(v);

        TextInputEditText etBuildingName = v.findViewById(R.id.buildingNameInput);
        TextInputEditText etRoomNumber = v.findViewById(R.id.roomNumberInput);
        TextInputEditText etRoomDescription = v.findViewById(R.id.descriptionInput);
        TextInputEditText etRoomPrice = v.findViewById(R.id.priceInput);
        Spinner availabilitySpinner = v.findViewById(R.id.availabilitySpinner);
        MaterialButton btnAdd = v.findViewById(R.id.btnAdd);
        MaterialButton btnCancel = v.findViewById(R.id.btnCancel);
        MaterialButton btnUploadImage = v.findViewById(R.id.btnUploadImage);
        ViewPager2 imageSlider = v.findViewById(R.id.imageSlider);

        // Initialize image slider
        imageSliderAdapter = new ImageSliderAdapter();
        imageSlider.setAdapter(imageSliderAdapter);
        
        // Reset selected images when dialog opens
        selectedImages.clear();
        imageSliderAdapter.clearImages();

        // Setup availability spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.availability_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        availabilitySpinner.setAdapter(adapter);
        availabilitySpinner.setSelection(0); // Default to Available

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // Set up image upload button
        btnUploadImage.setOnClickListener(view -> {
            if (selectedImages.size() < 4) {
                openImagePicker();
            } else {
                Toast.makeText(MainActivity.this, "Maximum 4 images allowed", Toast.LENGTH_SHORT).show();
            }
        });

        btnAdd.setOnClickListener(view -> {
            try {
                String buildingName = etBuildingName.getText() != null ? etBuildingName.getText().toString().trim() : "";
                String roomNumber = etRoomNumber.getText() != null ? etRoomNumber.getText().toString().trim() : "";
                String roomDescription = etRoomDescription.getText() != null ? etRoomDescription.getText().toString().trim() : "";
                String roomPrice = etRoomPrice.getText() != null ? etRoomPrice.getText().toString().trim() : "";
                boolean isAvailable = availabilitySpinner.getSelectedItemPosition() == 0;

                if (buildingName.isEmpty() || roomNumber.isEmpty() || roomDescription.isEmpty() || roomPrice.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create room data object
                DataModel roomData = new DataModel(
                    buildingName,
                    roomNumber,
                    roomDescription,
                    roomPrice,
                    isAvailable
                );

                // Show progress dialog
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("Uploading images...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                // If there are images to upload
                if (!selectedImages.isEmpty()) {
                    // Create a list to store download URLs
                    List<String> downloadUrls = new ArrayList<>();
                    final int[] uploadCount = {0};
                    final int[] errorCount = {0};

                    // Upload each image
                    for (Uri imageUri : selectedImages) {
                        try {
                            // Create a unique filename with proper path
                            String filename = "room_images/" + System.currentTimeMillis() + "_" + uploadCount[0] + ".jpg";
                            StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(filename);

                            // Upload the image with metadata
                            StorageMetadata metadata = new StorageMetadata.Builder()
                                .setContentType("image/jpeg")
                                .build();

                            // Upload the image
                            imageRef.putFile(imageUri, metadata)
                                .addOnProgressListener(taskSnapshot -> {
                                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                                    progressDialog.setMessage("Uploading image " + (uploadCount[0] + 1) + " of " + selectedImages.size() + 
                                        " (" + (int)progress + "%)");
                                })
                                .addOnSuccessListener(taskSnapshot -> {
                                    // Get the download URL
                                    imageRef.getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            downloadUrls.add(uri.toString());
                                            uploadCount[0]++;

                                            // If all images are uploaded, save the room data
                                            if (uploadCount[0] + errorCount[0] == selectedImages.size()) {
                                                if (errorCount[0] > 0) {
                                                    Toast.makeText(MainActivity.this, 
                                                        "Failed to upload " + errorCount[0] + " images", 
                                                        Toast.LENGTH_SHORT).show();
                                                }
                                                
                                                if (!downloadUrls.isEmpty()) {
                                                    // Join all download URLs with commas
                                                    String imageUrls = TextUtils.join(",", downloadUrls);
                                                    roomData.setImageUris(imageUrls);
                                                }

                                                // Save room data to database
                                                saveRoomData(roomData, alertDialog, progressDialog);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            errorCount[0]++;
                                            Toast.makeText(MainActivity.this, 
                                                "Failed to get image URL: " + e.getMessage(), 
                                                Toast.LENGTH_SHORT).show();
                                            
                                            if (uploadCount[0] + errorCount[0] == selectedImages.size()) {
                                                if (!downloadUrls.isEmpty()) {
                                                    String imageUrls = TextUtils.join(",", downloadUrls);
                                                    roomData.setImageUris(imageUrls);
                                                }
                                                saveRoomData(roomData, alertDialog, progressDialog);
                                            }
                                        });
                                })
                                .addOnFailureListener(e -> {
                                    errorCount[0]++;
                                    Toast.makeText(MainActivity.this, 
                                        "Failed to upload image: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                                    
                                    if (uploadCount[0] + errorCount[0] == selectedImages.size()) {
                                        if (!downloadUrls.isEmpty()) {
                                            String imageUrls = TextUtils.join(",", downloadUrls);
                                            roomData.setImageUris(imageUrls);
                                        }
                                        saveRoomData(roomData, alertDialog, progressDialog);
                                    }
                                });
                        } catch (Exception e) {
                            errorCount[0]++;
                            Toast.makeText(MainActivity.this, 
                                "Error processing image: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                            
                            if (uploadCount[0] + errorCount[0] == selectedImages.size()) {
                                if (!downloadUrls.isEmpty()) {
                                    String imageUrls = TextUtils.join(",", downloadUrls);
                                    roomData.setImageUris(imageUrls);
                                }
                                saveRoomData(roomData, alertDialog, progressDialog);
                            }
                        }
                    }
                } else {
                    // No images to upload, save room data directly
                    saveRoomData(roomData, alertDialog, progressDialog);
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(view -> alertDialog.dismiss());
    }

    private void saveRoomData(DataModel roomData, AlertDialog alertDialog, ProgressDialog progressDialog) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("rooms");
        String key = db.push().getKey();
        if (key != null) {
            roomData.setKey(key);
            db.child(key).setValue(roomData)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Room added successfully", Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Failed to add room: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void showProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_profile, null);
        builder.setView(view);

        TextView emailText = view.findViewById(R.id.emailText);
        Button logoutButton = view.findViewById(R.id.logoutButton);

        // Set user email
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            emailText.setText(currentUser.getEmail());
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        // Setup logout button
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }
}
