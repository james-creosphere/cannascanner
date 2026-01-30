package com.zebra.basicintent1;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a completed audit session with its upload status.
 */
public class AuditLog {
    
    public enum UploadStatus {
        PENDING,
        UPLOADED,
        FAILED
    }
    
    private String id;
    private String auditType;
    private String auditor;
    private String room;
    private String timestamp;
    private String csvFileName;
    private int scanCount;
    private UploadStatus uploadStatus;
    private String errorMessage;
    private List<ScanItem> scanItems;
    
    public AuditLog() {
        this.scanItems = new ArrayList<>();
    }
    
    public AuditLog(String id, String auditType, String auditor, String room, 
                   String timestamp, String csvFileName, int scanCount,
                   UploadStatus uploadStatus, List<ScanItem> scanItems) {
        this.id = id;
        this.auditType = auditType;
        this.auditor = auditor;
        this.room = room;
        this.timestamp = timestamp;
        this.csvFileName = csvFileName;
        this.scanCount = scanCount;
        this.uploadStatus = uploadStatus;
        this.scanItems = scanItems != null ? scanItems : new ArrayList<ScanItem>();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getAuditType() { return auditType; }
    public void setAuditType(String auditType) { this.auditType = auditType; }
    
    public String getAuditor() { return auditor; }
    public void setAuditor(String auditor) { this.auditor = auditor; }
    
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getCsvFileName() { return csvFileName; }
    public void setCsvFileName(String csvFileName) { this.csvFileName = csvFileName; }
    
    public int getScanCount() { return scanCount; }
    public void setScanCount(int scanCount) { this.scanCount = scanCount; }
    
    public UploadStatus getUploadStatus() { return uploadStatus; }
    public void setUploadStatus(UploadStatus uploadStatus) { this.uploadStatus = uploadStatus; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public List<ScanItem> getScanItems() { return scanItems; }
    public void setScanItems(List<ScanItem> scanItems) { this.scanItems = scanItems; }
    
    /**
     * Convert to JSON for storage.
     */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("auditType", auditType);
        json.put("auditor", auditor);
        json.put("room", room);
        json.put("timestamp", timestamp);
        json.put("csvFileName", csvFileName);
        json.put("scanCount", scanCount);
        json.put("uploadStatus", uploadStatus.name());
        json.put("errorMessage", errorMessage != null ? errorMessage : "");
        
        // Store scan items for retry
        JSONArray itemsArray = new JSONArray();
        for (ScanItem item : scanItems) {
            JSONObject itemJson = new JSONObject();
            itemJson.put("barcode", item.getBarcodeData());
            itemJson.put("weight", item.getWeight() != null ? item.getWeight() : "");
            itemJson.put("userName", item.getUserName());
            itemJson.put("room", item.getRoom());
            itemJson.put("timestamp", item.getTimestamp());
            itemsArray.put(itemJson);
        }
        json.put("scanItems", itemsArray);
        
        return json;
    }
    
    /**
     * Create from JSON.
     */
    public static AuditLog fromJson(JSONObject json) throws JSONException {
        AuditLog log = new AuditLog();
        log.setId(json.getString("id"));
        log.setAuditType(json.getString("auditType"));
        log.setAuditor(json.getString("auditor"));
        log.setRoom(json.getString("room"));
        log.setTimestamp(json.getString("timestamp"));
        log.setCsvFileName(json.optString("csvFileName", ""));
        log.setScanCount(json.getInt("scanCount"));
        log.setUploadStatus(UploadStatus.valueOf(json.getString("uploadStatus")));
        log.setErrorMessage(json.optString("errorMessage", ""));
        
        // Load scan items
        List<ScanItem> items = new ArrayList<>();
        JSONArray itemsArray = json.optJSONArray("scanItems");
        if (itemsArray != null) {
            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemJson = itemsArray.getJSONObject(i);
                ScanItem item = new ScanItem(
                    itemJson.getString("barcode"),
                    itemJson.optString("weight", ""),
                    itemJson.getString("userName"),
                    itemJson.getString("room")
                );
                // Restore original timestamp
                item.setTimestamp(itemJson.getString("timestamp"));
                items.add(item);
            }
        }
        log.setScanItems(items);
        
        return log;
    }
}
