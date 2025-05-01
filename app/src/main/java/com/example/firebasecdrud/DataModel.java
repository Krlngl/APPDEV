package com.example.firebasecdrud;

public class DataModel {
    private String key;
    private String buildingName;
    private String roomNumber;
    private String roomDescription;
    private String roomPrice;
    private boolean isAvailable;
    private String imageUris;

    public DataModel() {
        // Default constructor required for Firebase
    }

    public DataModel(String buildingName, String roomNumber, String roomDescription, 
                    String roomPrice, boolean isAvailable) {
        this.buildingName = buildingName;
        this.roomNumber = roomNumber;
        this.roomDescription = roomDescription;
        this.roomPrice = roomPrice;
        this.isAvailable = isAvailable;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomDescription() {
        return roomDescription;
    }

    public void setRoomDescription(String roomDescription) {
        this.roomDescription = roomDescription;
    }

    public String getRoomPrice() {
        return roomPrice;
    }

    public void setRoomPrice(String roomPrice) {
        this.roomPrice = roomPrice;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public String getImageUris() {
        return imageUris;
    }

    public void setImageUris(String imageUris) {
        this.imageUris = imageUris;
    }
}
