package com.zebra.basicintent1;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScanItem {
    private String barcodeData;
    private String weight;
    private String userName;
    private String room;
    private String timestamp;

    public ScanItem(String barcodeData, String weight, String userName, String room) {
        this.barcodeData = barcodeData;
        this.weight = weight;
        this.userName = userName;
        this.room = room;
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    public String getBarcodeData() {
        return barcodeData;
    }

    public void setBarcodeData(String barcodeData) {
        this.barcodeData = barcodeData;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String toCsvRow() {
        // Escape any commas or quotes in the data
        String escapedBarcode = escapeForCsv(barcodeData);
        String escapedWeight = escapeForCsv(weight);
        String escapedUser = escapeForCsv(userName);
        String escapedRoom = escapeForCsv(room);
        String escapedTimestamp = escapeForCsv(timestamp);
        return escapedTimestamp + "," + escapedBarcode + "," + escapedWeight + "," + escapedRoom + "," + escapedUser;
    }

    private String escapeForCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
