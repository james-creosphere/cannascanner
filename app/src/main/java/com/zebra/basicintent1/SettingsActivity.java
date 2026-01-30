package com.zebra.basicintent1;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchEnabled;
    private EditText etApiKey;
    private EditText etBaseId;
    private EditText etTableName;
    private AirtableConfig config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setTitle("Settings");

        config = new AirtableConfig(this);

        switchEnabled = findViewById(R.id.switchEnabled);
        etApiKey = findViewById(R.id.etApiKey);
        etBaseId = findViewById(R.id.etBaseId);
        etTableName = findViewById(R.id.etTableName);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnTest = findViewById(R.id.btnTest);

        // Load existing settings
        loadSettings();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnection();
            }
        });
    }

    private void loadSettings() {
        switchEnabled.setChecked(config.isEnabled());
        etApiKey.setText(config.getApiKey());
        etBaseId.setText(config.getBaseId());
        etTableName.setText(config.getTableName());
    }

    private void saveSettings() {
        String apiKey = etApiKey.getText().toString().trim();
        String baseId = etBaseId.getText().toString().trim();
        String tableName = etTableName.getText().toString().trim();
        boolean enabled = switchEnabled.isChecked();

        if (enabled) {
            if (apiKey.isEmpty()) {
                etApiKey.setError("API Key is required");
                etApiKey.requestFocus();
                return;
            }
            if (baseId.isEmpty()) {
                etBaseId.setError("Base ID is required");
                etBaseId.requestFocus();
                return;
            }
            if (tableName.isEmpty()) {
                etTableName.setError("Table Name is required");
                etTableName.requestFocus();
                return;
            }
        }

        config.saveSettings(apiKey, baseId, tableName, enabled);
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void testConnection() {
        String apiKey = etApiKey.getText().toString().trim();
        String baseId = etBaseId.getText().toString().trim();
        String tableName = etTableName.getText().toString().trim();

        if (apiKey.isEmpty() || baseId.isEmpty() || tableName.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields first", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Testing connection...", Toast.LENGTH_SHORT).show();

        // Create a test record
        List<ScanItem> testItems = new ArrayList<>();
        testItems.add(new ScanItem("TEST-BARCODE-12345", "", "Test User", "Test Room"));

        AirtableUploader uploader = new AirtableUploader(apiKey, baseId, tableName);
        uploader.uploadScans(testItems, "CONNECTION-TEST", new AirtableUploader.UploadCallback() {
            @Override
            public void onSuccess(int recordCount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SettingsActivity.this, 
                            "Connection successful! Test record created.", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SettingsActivity.this, 
                            "Connection failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
