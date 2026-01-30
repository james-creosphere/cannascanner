package com.zebra.basicintent1;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages Airtable configuration settings using SharedPreferences.
 */
public class AirtableConfig {
    
    private static final String PREFS_NAME = "AirtablePrefs";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_BASE_ID = "base_id";
    private static final String KEY_TABLE_NAME = "table_name";
    private static final String KEY_ENABLED = "enabled";
    
    private final SharedPreferences prefs;
    
    public AirtableConfig(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }
    
    public void setApiKey(String apiKey) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
    }
    
    public String getBaseId() {
        return prefs.getString(KEY_BASE_ID, "");
    }
    
    public void setBaseId(String baseId) {
        prefs.edit().putString(KEY_BASE_ID, baseId).apply();
    }
    
    public String getTableName() {
        return prefs.getString(KEY_TABLE_NAME, "");
    }
    
    public void setTableName(String tableName) {
        prefs.edit().putString(KEY_TABLE_NAME, tableName).apply();
    }
    
    public boolean isEnabled() {
        return prefs.getBoolean(KEY_ENABLED, false);
    }
    
    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
    
    /**
     * Check if all required configuration is present.
     */
    public boolean isConfigured() {
        return !getApiKey().isEmpty() 
            && !getBaseId().isEmpty() 
            && !getTableName().isEmpty();
    }
    
    /**
     * Check if Airtable upload should be attempted.
     */
    public boolean shouldUpload() {
        return isEnabled() && isConfigured();
    }
    
    /**
     * Save all settings at once.
     */
    public void saveSettings(String apiKey, String baseId, String tableName, boolean enabled) {
        prefs.edit()
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_BASE_ID, baseId)
            .putString(KEY_TABLE_NAME, tableName)
            .putBoolean(KEY_ENABLED, enabled)
            .apply();
    }
}
