package com.zebra.basicintent1;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AuditScanActivity extends AppCompatActivity {

    private static final String TAG = "AuditScanActivity";
    
    // DataWedge constants
    private static final String ACTION_DATAWEDGE = "com.symbol.datawedge.api.ACTION";
    
    // Intent action for receiving scans
    private static final String SCAN_ACTION = "com.zebra.cannascanner.SCAN";
    private static final String PROFILE_NAME = "CannaScanner";
    
    // Email recipient for audit reports
    private static final String REPORT_EMAIL = "385501f8.NECraftCultivators.com@amer.teams.ms";

    private String userName;
    private String auditMode;
    private boolean isSpeedMode;
    private List<ScanItem> scanItems;
    private ScanItemAdapter adapter;
    private TextView tvUserName;
    private TextView tvScanCount;
    private TextView tvInstructions;
    private ListView lvScans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_scan);

        userName = getIntent().getStringExtra("USER_NAME");
        auditMode = getIntent().getStringExtra(MainActivity.EXTRA_AUDIT_MODE);
        if (auditMode == null) {
            auditMode = MainActivity.MODE_WEIGHT;
        }
        isSpeedMode = MainActivity.MODE_SPEED.equals(auditMode);

        // Set title based on mode
        if (isSpeedMode) {
            setTitle("Audit-Speed Session");
        } else {
            setTitle("Audit-Weight Session");
        }

        scanItems = new ArrayList<>();

        tvUserName = findViewById(R.id.tvUserName);
        tvScanCount = findViewById(R.id.tvScanCount);
        tvInstructions = findViewById(R.id.tvInstructions);
        lvScans = findViewById(R.id.lvScans);
        Button btnExport = findViewById(R.id.btnExport);
        Button btnFinish = findViewById(R.id.btnFinish);

        tvUserName.setText("Auditor: " + userName);
        updateScanCount();

        // Update instructions based on mode
        if (isSpeedMode) {
            tvInstructions.setText("Speed mode: Scan barcodes rapidly (no weight entry)");
        } else {
            tvInstructions.setText("Scan a barcode to add it to the audit list");
        }

        adapter = new ScanItemAdapter(this, scanItems);
        lvScans.setAdapter(adapter);

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportToCsv();
            }
        });

        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scanItems.isEmpty()) {
                    finish();
                    return;
                }
                
                new AlertDialog.Builder(AuditScanActivity.this)
                    .setTitle("Finish Audit")
                    .setMessage("Would you like to export the CSV before finishing?")
                    .setPositiveButton("Export & Finish", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            exportToCsv();
                            finish();
                        }
                    })
                    .setNegativeButton("Just Finish", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
            }
        });

        // Setup DataWedge profile
        setupDataWedge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerScanReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(scanReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }

    private void registerScanReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction(SCAN_ACTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scanReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(scanReceiver, filter);
        }
        
        Log.d(TAG, "Scan receiver registered for action: " + SCAN_ACTION);
    }

    private void setupDataWedge() {
        // Step 1: Create profile if it doesn't exist
        sendDataWedgeCommand("com.symbol.datawedge.api.CREATE_PROFILE", PROFILE_NAME);
        
        // Step 2: Configure the profile
        Bundle profileConfig = new Bundle();
        profileConfig.putString("PROFILE_NAME", PROFILE_NAME);
        profileConfig.putString("PROFILE_ENABLED", "true");
        profileConfig.putString("CONFIG_MODE", "UPDATE");
        
        // Associate with this app's package
        Bundle appConfig = new Bundle();
        appConfig.putString("PACKAGE_NAME", getPackageName());
        appConfig.putStringArray("ACTIVITY_LIST", new String[]{"*"});
        profileConfig.putParcelableArray("APP_LIST", new Bundle[]{appConfig});
        
        sendDataWedgeCommand("com.symbol.datawedge.api.SET_CONFIG", profileConfig);
        
        // Step 3: Configure barcode input
        Bundle barcodeConfig = new Bundle();
        barcodeConfig.putString("PROFILE_NAME", PROFILE_NAME);
        barcodeConfig.putString("PROFILE_ENABLED", "true");
        barcodeConfig.putString("CONFIG_MODE", "UPDATE");
        
        Bundle barcodePlugin = new Bundle();
        barcodePlugin.putString("PLUGIN_NAME", "BARCODE");
        barcodePlugin.putString("RESET_CONFIG", "true");
        
        Bundle barcodeProps = new Bundle();
        barcodeProps.putString("scanner_input_enabled", "true");
        barcodePlugin.putBundle("PARAM_LIST", barcodeProps);
        
        barcodeConfig.putBundle("PLUGIN_CONFIG", barcodePlugin);
        sendDataWedgeCommand("com.symbol.datawedge.api.SET_CONFIG", barcodeConfig);
        
        // Step 4: Configure intent output
        Bundle intentConfig = new Bundle();
        intentConfig.putString("PROFILE_NAME", PROFILE_NAME);
        intentConfig.putString("PROFILE_ENABLED", "true");
        intentConfig.putString("CONFIG_MODE", "UPDATE");
        
        Bundle intentPlugin = new Bundle();
        intentPlugin.putString("PLUGIN_NAME", "INTENT");
        intentPlugin.putString("RESET_CONFIG", "true");
        
        Bundle intentProps = new Bundle();
        intentProps.putString("intent_output_enabled", "true");
        intentProps.putString("intent_action", SCAN_ACTION);
        intentProps.putString("intent_category", "android.intent.category.DEFAULT");
        intentProps.putString("intent_delivery", "2"); // Broadcast
        intentPlugin.putBundle("PARAM_LIST", intentProps);
        
        intentConfig.putBundle("PLUGIN_CONFIG", intentPlugin);
        sendDataWedgeCommand("com.symbol.datawedge.api.SET_CONFIG", intentConfig);
        
        // Step 5: Disable keystroke output
        Bundle keystrokeConfig = new Bundle();
        keystrokeConfig.putString("PROFILE_NAME", PROFILE_NAME);
        keystrokeConfig.putString("PROFILE_ENABLED", "true");
        keystrokeConfig.putString("CONFIG_MODE", "UPDATE");
        
        Bundle keystrokePlugin = new Bundle();
        keystrokePlugin.putString("PLUGIN_NAME", "KEYSTROKE");
        keystrokePlugin.putString("RESET_CONFIG", "true");
        
        Bundle keystrokeProps = new Bundle();
        keystrokeProps.putString("keystroke_output_enabled", "false");
        keystrokePlugin.putBundle("PARAM_LIST", keystrokeProps);
        
        keystrokeConfig.putBundle("PLUGIN_CONFIG", keystrokePlugin);
        sendDataWedgeCommand("com.symbol.datawedge.api.SET_CONFIG", keystrokeConfig);
        
        Log.d(TAG, "DataWedge profile '" + PROFILE_NAME + "' configured");
    }

    private void sendDataWedgeCommand(String extraName, String extraValue) {
        Intent intent = new Intent();
        intent.setAction(ACTION_DATAWEDGE);
        intent.putExtra(extraName, extraValue);
        sendBroadcast(intent);
    }

    private void sendDataWedgeCommand(String extraName, Bundle extraValue) {
        Intent intent = new Intent();
        intent.setAction(ACTION_DATAWEDGE);
        intent.putExtra(extraName, extraValue);
        sendBroadcast(intent);
    }

    private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received broadcast with action: " + action);
            
            // Log all extras for debugging
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Log.d(TAG, "  Extra: " + key + " = " + extras.get(key));
                }
            }
            
            if (SCAN_ACTION.equals(action)) {
                // Try multiple possible keys for barcode data
                String barcodeData = intent.getStringExtra("com.symbol.datawedge.data_string");
                
                if (barcodeData == null) {
                    barcodeData = intent.getStringExtra(
                        getResources().getString(R.string.datawedge_intent_key_data));
                }
                
                Log.d(TAG, "Barcode data: " + barcodeData);
                
                if (barcodeData != null && !barcodeData.isEmpty()) {
                    handleScan(barcodeData);
                } else {
                    Toast.makeText(AuditScanActivity.this, 
                        "Scan received but no data found", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private void handleScan(final String barcodeData) {
        if (isSpeedMode) {
            // Speed mode: add directly without weight dialog
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addScanItem(barcodeData, "");
                }
            });
        } else {
            // Weight mode: show dialog to enter weight
            showWeightDialog(barcodeData);
        }
    }

    private void showWeightDialog(final String barcodeData) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(AuditScanActivity.this);
                builder.setTitle("Scanned: " + barcodeData);

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_weight_input, null);
                final EditText etWeight = dialogView.findViewById(R.id.etWeight);
                builder.setView(dialogView);

                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String weight = etWeight.getText().toString().trim();
                        addScanItem(barcodeData, weight);
                    }
                });

                builder.setNegativeButton("Skip Weight", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addScanItem(barcodeData, "");
                    }
                });

                builder.setCancelable(false);
                builder.show();
            }
        });
    }

    private void addScanItem(String barcodeData, String weight) {
        ScanItem item = new ScanItem(barcodeData, weight, userName);
        scanItems.add(0, item); // Add to top of list
        adapter.notifyDataSetChanged();
        updateScanCount();
        
        Log.d(TAG, "Added scan item: " + barcodeData + ", weight: " + weight);
        Toast.makeText(this, "Added: " + barcodeData, Toast.LENGTH_SHORT).show();
    }

    private void updateScanCount() {
        tvScanCount.setText("Scans: " + scanItems.size());
    }

    private void exportToCsv() {
        if (scanItems.isEmpty()) {
            Toast.makeText(this, "No scans to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.US).format(new Date());
            String safeUserName = userName.replaceAll("[^a-zA-Z0-9]", "_");
            String modePrefix = isSpeedMode ? "AUDIT-SPEED" : "AUDIT-WEIGHT";
            String fileName = modePrefix + "-" + timestamp + "-" + safeUserName + ".csv";
            
            File exportDir = new File(getExternalFilesDir(null), "exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            File file = new File(exportDir, fileName);
            FileWriter writer = new FileWriter(file);
            
            // Write header - different for speed mode (no weight column)
            if (isSpeedMode) {
                writer.append("Barcode,Auditor\n");
            } else {
                writer.append("Barcode,Weight (g),Auditor\n");
            }
            
            // Write data (in reverse order so oldest first)
            for (int i = scanItems.size() - 1; i >= 0; i--) {
                ScanItem item = scanItems.get(i);
                if (isSpeedMode) {
                    writer.append(escapeForCsv(item.getBarcodeData()));
                    writer.append(",");
                    writer.append(escapeForCsv(item.getUserName()));
                } else {
                    writer.append(item.toCsvRow());
                }
                writer.append("\n");
            }
            
            writer.flush();
            writer.close();

            // Share the file
            shareFile(file);
            
            Toast.makeText(this, "Exported " + scanItems.size() + " items", Toast.LENGTH_SHORT).show();
            
        } catch (IOException e) {
            Toast.makeText(this, "Error exporting: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, 
            getPackageName() + ".fileprovider", file);
        
        // Create email intent with pre-filled recipient
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{REPORT_EMAIL});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Audit Report: " + file.getName());
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Please find attached the audit report.\n\nAuditor: " + userName + "\nItems scanned: " + scanItems.size());
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Send Audit Report"));
        } catch (android.content.ActivityNotFoundException e) {
            // Fallback to generic share if no email app
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Export Audit CSV"));
        }
    }
}
