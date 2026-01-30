package com.zebra.basicintent1;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Handles uploading scan data to Airtable via their REST API.
 */
public class AirtableUploader {
    
    private static final String TAG = "AirtableUploader";
    private static final String AIRTABLE_API_URL = "https://api.airtable.com/v0/";
    private static final int MAX_RECORDS_PER_REQUEST = 10; // Airtable limit
    
    public interface UploadCallback {
        void onSuccess(int recordCount);
        void onError(String error);
    }
    
    private final String apiKey;
    private final String baseId;
    private final String tableName;
    
    public AirtableUploader(String apiKey, String baseId, String tableName) {
        this.apiKey = apiKey;
        this.baseId = baseId;
        this.tableName = tableName;
    }
    
    /**
     * Upload scan items to Airtable asynchronously.
     * 
     * @param items List of ScanItem objects to upload
     * @param auditType Type of audit (e.g., "AUDIT-WEIGHT", "AUDIT-SPEED", "RFID-AUDIT")
     * @param callback Callback for success/error handling
     */
    public void uploadScans(List<ScanItem> items, String auditType, UploadCallback callback) {
        new UploadTask(items, auditType, callback).execute();
    }
    
    private class UploadTask extends AsyncTask<Void, Void, String> {
        
        private final List<ScanItem> items;
        private final String auditType;
        private final UploadCallback callback;
        private int successCount = 0;
        
        UploadTask(List<ScanItem> items, String auditType, UploadCallback callback) {
            this.items = items;
            this.auditType = auditType;
            this.callback = callback;
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                // Upload in batches of 10 (Airtable limit)
                for (int i = 0; i < items.size(); i += MAX_RECORDS_PER_REQUEST) {
                    int end = Math.min(i + MAX_RECORDS_PER_REQUEST, items.size());
                    List<ScanItem> batch = items.subList(i, end);
                    
                    String result = uploadBatch(batch);
                    if (result != null) {
                        return result; // Error occurred
                    }
                    successCount += batch.size();
                }
                return null; // Success
            } catch (Exception e) {
                Log.e(TAG, "Upload failed", e);
                return e.getMessage();
            }
        }
        
        private String uploadBatch(List<ScanItem> batch) {
            HttpURLConnection connection = null;
            try {
                String urlString = AIRTABLE_API_URL + baseId + "/" + tableName;
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                
                // Build JSON payload
                JSONObject payload = new JSONObject();
                JSONArray records = new JSONArray();
                
                for (ScanItem item : batch) {
                    JSONObject record = new JSONObject();
                    JSONObject fields = new JSONObject();
                    
                    fields.put("Timestamp", item.getTimestamp());
                    fields.put("Barcode", item.getBarcodeData());
                    fields.put("Room", item.getRoom());
                    fields.put("Auditor", item.getUserName());
                    fields.put("AuditType", auditType);
                    
                    // Add weight as number (Airtable field is Number type)
                    String weight = item.getWeight();
                    if (weight != null && !weight.trim().isEmpty()) {
                        try {
                            double weightValue = Double.parseDouble(weight.trim());
                            fields.put("Weight", weightValue);
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid weight value, skipping: " + weight);
                        }
                    }
                    
                    record.put("fields", fields);
                    records.put(record);
                }
                
                payload.put("records", records);
                
                // Send request
                String jsonPayload = payload.toString();
                Log.d(TAG, "Sending to Airtable: " + jsonPayload);
                
                OutputStream os = connection.getOutputStream();
                os.write(jsonPayload.getBytes("UTF-8"));
                os.flush();
                os.close();
                
                // Check response
                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    Log.d(TAG, "Batch uploaded successfully");
                    return null; // Success
                } else {
                    // Read error response
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    Log.e(TAG, "Airtable error: " + response.toString());
                    return "Airtable error (" + responseCode + "): " + response.toString();
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading batch", e);
                return e.getMessage();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        
        @Override
        protected void onPostExecute(String error) {
            if (error == null) {
                callback.onSuccess(successCount);
            } else {
                callback.onError(error);
            }
        }
    }
}
