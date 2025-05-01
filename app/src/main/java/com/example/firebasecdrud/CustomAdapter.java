package com.example.firebasecdrud;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bumptech.glide.Glide;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
    private Context context;
    private List<DataModel> dataList;
    private String currentUserId;
    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    
    public CustomAdapter(Context context, List<DataModel> dataList) {
        this.context = context;
        this.dataList = dataList;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }
    
    public CustomAdapter(Context context, List<DataModel> dataList, String currentUserId) {
        this.context = context;
        this.dataList = dataList;
        this.currentUserId = currentUserId;
    }
    
    public void updateData(List<DataModel> newDataList) {
        this.dataList.clear();
        this.dataList.addAll(newDataList);
        notifyDataSetChanged();
    }

    public void addData(DataModel newData) {
        this.dataList.add(newData);
        notifyItemInserted(dataList.size() - 1);
    }

    public void updateItem(DataModel updatedData) {
        int position = -1;
        for (int i = 0; i < dataList.size(); i++) {
            if (dataList.get(i).getKey().equals(updatedData.getKey())) {
                position = i;
                break;
            }
        }
        if (position != -1) {
            dataList.set(position, updatedData);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_singleitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataModel dataModel = dataList.get(position);
        
        // Set data to views
        holder.buildingName.setText(dataModel.getBuildingName());
        holder.roomNumber.setText("Room " + dataModel.getRoomNumber());
        holder.roomDescription.setText(dataModel.getRoomDescription());
        holder.roomPrice.setText("₱" + dataModel.getRoomPrice());
        
        // Handle availability status
        boolean isAvailable = dataModel.isAvailable();
        holder.availability.setText(isAvailable ? "Available" : "Not Available");
        holder.availability.setBackgroundTintList(ColorStateList.valueOf(
            isAvailable ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336")
        ));

        // Handle image loading
        if (dataModel.getImageUris() != null && !dataModel.getImageUris().isEmpty()) {
            holder.roomImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                .load(dataModel.getImageUris())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.roomImage);
        } else {
            holder.roomImage.setVisibility(View.GONE);
        }

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> showRoomDetailsDialog(dataModel));
    }

    private void showRoomDetailsDialog(DataModel data) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_room_details, null);
            builder.setView(dialogView);

                // Initialize views
            TextView buildingNameTextView = dialogView.findViewById(R.id.buildingNameTextView);
            TextView roomNumberTextView = dialogView.findViewById(R.id.roomNumberTextView);
            TextView descriptionTextView = dialogView.findViewById(R.id.descriptionTextView);
            TextView priceTextView = dialogView.findViewById(R.id.priceTextView);
            TextView availabilityTextView = dialogView.findViewById(R.id.availabilityTextView);
            ImageView roomImageView = dialogView.findViewById(R.id.roomImageView);
            ImageButton closeButton = dialogView.findViewById(R.id.btnClose);
            MaterialButton editButton = dialogView.findViewById(R.id.editButton);
            MaterialButton deleteButton = dialogView.findViewById(R.id.deleteButton);
            
            // Set data
            buildingNameTextView.setText(data.getBuildingName());
            roomNumberTextView.setText("Room " + data.getRoomNumber());
            descriptionTextView.setText(data.getRoomDescription());
            priceTextView.setText("₱" + data.getRoomPrice());
            
            // Handle availability status
            boolean isAvailable = data.isAvailable();
            availabilityTextView.setText(isAvailable ? "Available" : "Occupied");
            availabilityTextView.setSelected(isAvailable);
            
            // Handle room image if available
            if (data.getImageUris() != null && !data.getImageUris().isEmpty()) {
                roomImageView.setVisibility(View.VISIBLE);
                // Load the first image
                String[] imageUris = data.getImageUris().split(",");
                if (imageUris.length > 0) {
                    // Load image using your preferred image loading library
                    // For example, using Glide:
                    // Glide.with(context)
                    //     .load(imageUris[0])
                    //     .into(roomImageView);
                }
            } else {
                roomImageView.setVisibility(View.GONE);
            }
            
            // Create dialog
            AlertDialog dialog = builder.create();
            
            // Set button click listeners
            closeButton.setOnClickListener(v -> dialog.dismiss());
            
            editButton.setOnClickListener(v -> {
                dialog.dismiss();
                showEditDialog(data);
            });
            
            deleteButton.setOnClickListener(v -> {
                dialog.dismiss();
                showDeleteConfirmationDialog(data);
            });
            
            // Show dialog
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(context, "Error showing room details: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showConfirmationDialog(Context context, Runnable onConfirm) {
        new AlertDialog.Builder(context)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("Discard", (dialogInterface, which) -> onConfirm.run())
            .setNegativeButton("Keep Editing", null)
            .show();
    }

    private boolean hasChanges(EditText buildingNameEditText, EditText roomNumberEditText, 
                             EditText descriptionEditText, EditText priceEditText, 
                             SwitchMaterial availabilitySwitch,
                             String originalBuildingName, String originalRoomNumber, 
                             String originalDescription, String originalPrice, 
                             boolean originalAvailability) {
        String currentBuildingName = buildingNameEditText.getText() != null ? 
                buildingNameEditText.getText().toString().trim() : "";
        String currentRoomNumber = roomNumberEditText.getText() != null ? 
                roomNumberEditText.getText().toString().trim() : "";
        String currentDescription = descriptionEditText.getText() != null ? 
                descriptionEditText.getText().toString().trim() : "";
        String currentPrice = priceEditText.getText() != null ? 
                priceEditText.getText().toString().trim() : "";
        boolean currentAvailability = availabilitySwitch.isChecked();
        
        return !currentBuildingName.equals(originalBuildingName) ||
               !currentRoomNumber.equals(originalRoomNumber) ||
               !currentDescription.equals(originalDescription) ||
               !currentPrice.equals(originalPrice) ||
               currentAvailability != originalAvailability;
    }

    private void showEditDialog(DataModel data) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_room, null);
        builder.setView(view);

        TextInputLayout buildingNameLayout = view.findViewById(R.id.buildingNameLayout);
        TextInputLayout roomNumberLayout = view.findViewById(R.id.roomNumberLayout);
        TextInputLayout descriptionLayout = view.findViewById(R.id.descriptionLayout);
        TextInputLayout priceLayout = view.findViewById(R.id.priceLayout);
        Spinner availabilitySpinner = view.findViewById(R.id.availabilitySpinner);
        ImageView roomImage = view.findViewById(R.id.roomImage);
        Button addImageButton = view.findViewById(R.id.addImageButton);

        // Set current values
        buildingNameLayout.getEditText().setText(data.getBuildingName());
        roomNumberLayout.getEditText().setText(data.getRoomNumber());
        descriptionLayout.getEditText().setText(data.getRoomDescription());
        priceLayout.getEditText().setText(String.valueOf(data.getRoomPrice()));
        
        // Setup availability spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.availability_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        availabilitySpinner.setAdapter(adapter);
        availabilitySpinner.setSelection(data.isAvailable() ? 0 : 1);

        // Load current image if exists
        if (data.getImageUris() != null && !data.getImageUris().isEmpty()) {
            roomImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                .load(data.getImageUris())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(roomImage);
        } else {
            roomImage.setVisibility(View.GONE);
        }

        // Handle image selection
        addImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            ((Activity) context).startActivityForResult(
                Intent.createChooser(intent, "Select Image"),
                PICK_IMAGE_REQUEST
            );
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle save button click
        view.findViewById(R.id.saveButton).setOnClickListener(v -> {
            String buildingName = buildingNameLayout.getEditText().getText().toString().trim();
            String roomNumber = roomNumberLayout.getEditText().getText().toString().trim();
            String description = descriptionLayout.getEditText().getText().toString().trim();
            String priceStr = priceLayout.getEditText().getText().toString().trim();
            boolean isAvailable = availabilitySpinner.getSelectedItemPosition() == 0;

            // Validate inputs
            if (buildingName.isEmpty() || roomNumber.isEmpty() || description.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double price;
            try {
                price = Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid price format", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress dialog
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Updating room...");
            progressDialog.show();

            // Update Firebase
            DatabaseReference roomRef = FirebaseDatabase.getInstance().getReference()
                .child("rooms")
                .child(data.getKey());

            Map<String, Object> updates = new HashMap<>();
            updates.put("buildingName", buildingName);
            updates.put("roomNumber", roomNumber);
            updates.put("roomDescription", description);
            updates.put("roomPrice", priceStr);
            updates.put("isAvailable", isAvailable);

            // Handle image upload if a new image was selected
            if (selectedImageUri != null) {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("room_images")
                    .child(data.getKey() + "_" + System.currentTimeMillis());

                storageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Get the download URL
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            updates.put("imageUris", uri.toString());
                            
                            // Update the room data with the new image URL
                            roomRef.updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Update the local data
                                    int position = dataList.indexOf(data);
                                    if (position != -1) {
                                        data.setBuildingName(buildingName);
                                        data.setRoomNumber(roomNumber);
                                        data.setRoomDescription(description);
                                        data.setRoomPrice(priceStr);
                                        data.setAvailable(isAvailable);
                                        data.setImageUris(uri.toString());
                                        notifyItemChanged(position);
                                    }
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "Room updated successfully", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(context, "Failed to update room: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                                });
                        });
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Failed to upload image: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
            } else {
                // Keep existing image if no new image was selected
                if (data.getImageUris() != null && !data.getImageUris().isEmpty()) {
                    updates.put("imageUris", data.getImageUris());
                }

                // Update without new image
                roomRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        // Update the local data
                        int position = dataList.indexOf(data);
                        if (position != -1) {
                            data.setBuildingName(buildingName);
                            data.setRoomNumber(roomNumber);
                            data.setRoomDescription(description);
                            data.setRoomPrice(priceStr);
                            data.setAvailable(isAvailable);
                            notifyItemChanged(position);
                        }
                        progressDialog.dismiss();
                        Toast.makeText(context, "Room updated successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Failed to update room: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
            }
        });

        // Handle cancel button click
        view.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
    }

    // Add this method to handle the image selection result
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            // Update the image preview
            ImageView roomImage = ((AlertDialog) ((Activity) context).getCurrentFocus().getParent().getParent()).findViewById(R.id.roomImage);
            roomImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                .load(selectedImageUri)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(roomImage);
        }
    }

    private void showDeleteConfirmationDialog(DataModel data) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Room");
        builder.setMessage("Are you sure you want to delete this room?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Delete from Firebase
            DatabaseReference roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(data.getKey());
            roomRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Room deleted successfully", Toast.LENGTH_SHORT).show();
                        dataList.remove(data);
                        notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to delete room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView buildingName, roomNumber, roomDescription, roomPrice, availability;
        ImageView roomImage;
        View itemView;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            buildingName = itemView.findViewById(R.id.buildingName);
            roomNumber = itemView.findViewById(R.id.roomNumber);
            roomDescription = itemView.findViewById(R.id.roomDescription);
            roomPrice = itemView.findViewById(R.id.roomPrice);
            availability = itemView.findViewById(R.id.availability);
            roomImage = itemView.findViewById(R.id.roomImage);
        }
    }
}