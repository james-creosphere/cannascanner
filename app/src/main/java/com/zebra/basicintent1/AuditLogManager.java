package com.zebra.basicintent1;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages storage and retrieval of audit logs.
 */
public class AuditLogManager {
    
    private static final String TAG = "AuditLogManager";
    private static final String PREFS_NAME = "AuditLogs";
    private static final String KEY_LOGS = "logs";
    private static final int MAX_LOGS = 50; // Keep last 50 audits
    
    private final SharedPreferences prefs;
    
    public AuditLogManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Generate a unique ID for a new audit.
     */
    public String generateId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Save a new audit log.
     */
    public void saveAuditLog(AuditLog log) {
        List<AuditLog> logs = getAllLogs();
        
        // Check if log already exists (update case)
        boolean found = false;
        for (int i = 0; i < logs.size(); i++) {
            if (logs.get(i).getId().equals(log.getId())) {
                logs.set(i, log);
                found = true;
                break;
            }
        }
        
        if (!found) {
            // Add new log at the beginning
            logs.add(0, log);
        }
        
        // Trim to max size
        while (logs.size() > MAX_LOGS) {
            logs.remove(logs.size() - 1);
        }
        
        saveLogs(logs);
    }
    
    /**
     * Update the upload status of an audit log.
     */
    public void updateUploadStatus(String logId, AuditLog.UploadStatus status, String errorMessage) {
        List<AuditLog> logs = getAllLogs();
        
        for (AuditLog log : logs) {
            if (log.getId().equals(logId)) {
                log.setUploadStatus(status);
                log.setErrorMessage(errorMessage);
                
                // Clear scan items if upload succeeded (save space)
                if (status == AuditLog.UploadStatus.UPLOADED) {
                    log.setScanItems(new ArrayList<ScanItem>());
                }
                break;
            }
        }
        
        saveLogs(logs);
    }
    
    /**
     * Get all audit logs.
     */
    public List<AuditLog> getAllLogs() {
        List<AuditLog> logs = new ArrayList<>();
        String json = prefs.getString(KEY_LOGS, "[]");
        
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                AuditLog log = AuditLog.fromJson(array.getJSONObject(i));
                logs.add(log);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading audit logs", e);
        }
        
        return logs;
    }
    
    /**
     * Get logs that failed to upload.
     */
    public List<AuditLog> getFailedLogs() {
        List<AuditLog> allLogs = getAllLogs();
        List<AuditLog> failedLogs = new ArrayList<>();
        
        for (AuditLog log : allLogs) {
            if (log.getUploadStatus() == AuditLog.UploadStatus.FAILED) {
                failedLogs.add(log);
            }
        }
        
        return failedLogs;
    }
    
    /**
     * Get a specific audit log by ID.
     */
    public AuditLog getLogById(String id) {
        List<AuditLog> logs = getAllLogs();
        
        for (AuditLog log : logs) {
            if (log.getId().equals(id)) {
                return log;
            }
        }
        
        return null;
    }
    
    /**
     * Delete an audit log.
     */
    public void deleteLog(String id) {
        List<AuditLog> logs = getAllLogs();
        
        for (int i = 0; i < logs.size(); i++) {
            if (logs.get(i).getId().equals(id)) {
                logs.remove(i);
                break;
            }
        }
        
        saveLogs(logs);
    }
    
    /**
     * Clear all logs.
     */
    public void clearAllLogs() {
        prefs.edit().remove(KEY_LOGS).apply();
    }
    
    private void saveLogs(List<AuditLog> logs) {
        try {
            JSONArray array = new JSONArray();
            for (AuditLog log : logs) {
                array.put(log.toJson());
            }
            prefs.edit().putString(KEY_LOGS, array.toString()).apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving audit logs", e);
        }
    }
}
