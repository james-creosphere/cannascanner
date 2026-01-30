package com.zebra.basicintent1;

public class ScanItem {
    private String barcodeData;
    private String weight;
    private String userName;

    public ScanItem(String barcodeData, String weight, String userName) {
        this.barcodeData = barcodeData;
        this.weight = weight;
        this.userName = userName;
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

    public String toCsvRow() {
        // Escape any commas or quotes in the data
        String escapedBarcode = escapeForCsv(barcodeData);
        String escapedWeight = escapeForCsv(weight);
        String escapedUser = escapeForCsv(userName);
        return escapedBarcode + "," + escapedWeight + "," + escapedUser;
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
