package com.zebra.basicintent1;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.RFIDResults;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RegionInfo;
import com.zebra.rfid.api3.RegulatoryConfig;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.SupportedRegions;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RfidScanActivity extends AppCompatActivity implements RfidEventsListener {

    private static final String TAG = "RfidScanActivity";
    private static final String REPORT_EMAIL = "385501f8.NECraftCultivators.com@amer.teams.ms";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private String userName;
    private String room;
    private List<ScanItem> scanItems;
    private Set<String> scannedEpcs;
    private ScanItemAdapter adapter;
    private TextView tvUserName;
    private TextView tvScanCount;
    private TextView tvReaderStatus;
    private ListView lvScans;
    private Button btnConnect;
    private Button btnStartScan;

    // RFID Reader
    private Readers readers;
    private RFIDReader reader;
    private boolean isReaderConnected = false;
    private boolean isInventoryRunning = false;
    private Handler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rfid_scan);

        setTitle("RFID Tag Audit");

        userName = getIntent().getStringExtra("USER_NAME");
        room = getIntent().getStringExtra("ROOM");
        scanItems = new ArrayList<>();
        scannedEpcs = new HashSet<>();
        uiHandler = new Handler(Looper.getMainLooper());

        tvUserName = findViewById(R.id.tvUserName);
        tvScanCount = findViewById(R.id.tvScanCount);
        tvReaderStatus = findViewById(R.id.tvReaderStatus);
        lvScans = findViewById(R.id.lvScans);
        btnConnect = findViewById(R.id.btnConnect);
        btnStartScan = findViewById(R.id.btnStartScan);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnSubmit = findViewById(R.id.btnSubmit);

        tvUserName.setText("Auditor: " + userName + " | Room: " + room);
        updateScanCount();
        updateReaderStatus("Disconnected");

        adapter = new ScanItemAdapter(this, scanItems);
        lvScans.setAdapter(adapter);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isReaderConnected) {
                    disconnectReader();
                } else {
                    if (checkPermissions()) {
                        connectReader();
                    } else {
                        requestPermissions();
                    }
                }
            }
        });

        btnStartScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInventoryRunning) {
                    stopInventory();
                } else {
                    startInventory();
                }
            }
        });
        btnStartScan.setEnabled(false);

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scanItems.isEmpty()) {
                    Toast.makeText(RfidScanActivity.this, "Nothing to reset", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                new AlertDialog.Builder(RfidScanActivity.this)
                    .setTitle("Reset Audit")
                    .setMessage("Are you sure you want to clear all " + scanItems.size() + " scans?")
                    .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            scanItems.clear();
                            scannedEpcs.clear();
                            adapter.notifyDataSetChanged();
                            updateScanCount();
                            Toast.makeText(RfidScanActivity.this, "Audit reset", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (scanItems.isEmpty()) {
                    Toast.makeText(RfidScanActivity.this, "No scans to submit", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Export CSV first
                final String csvFileName = exportToCsvAndGetFilename();
                
                // Then upload to Airtable
                uploadToAirtableWithCallback(csvFileName);
            }
        });

        // Check permissions on startup
        if (!checkPermissions()) {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires BLUETOOTH_CONNECT and BLUETOOTH_SCAN
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Older Android versions need location permission for Bluetooth scanning
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }
        
        // Location permissions (needed for Bluetooth on older Android and sometimes for RFID)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(this, "Permissions granted. You can now connect.", Toast.LENGTH_SHORT).show();
                // Initialize readers after permissions are granted
                initReaders();
            } else {
                Toast.makeText(this, "Bluetooth/Location permissions are required for RFID", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initReaders() {
        if (!checkPermissions()) {
            Log.w(TAG, "Cannot initialize readers - permissions not granted");
            return;
        }
        
        try {
            readers = new Readers(this, ENUM_TRANSPORT.ALL);
            Log.d(TAG, "Readers initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing readers: " + e.getMessage());
            Toast.makeText(this, "Error initializing RFID: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void connectReader() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPreExecute() {
                updateReaderStatus("Connecting...");
                btnConnect.setEnabled(false);
            }

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    if (readers == null) {
                        readers = new Readers(RfidScanActivity.this, ENUM_TRANSPORT.ALL);
                    }

                    ArrayList<ReaderDevice> availableReaders = readers.GetAvailableRFIDReaderList();
                    
                    if (availableReaders == null || availableReaders.isEmpty()) {
                        return "No RFID readers found. Please connect RFD40.";
                    }

                    // Connect to the first available reader
                    ReaderDevice readerDevice = availableReaders.get(0);
                    reader = readerDevice.getRFIDReader();
                    
                    if (!reader.isConnected()) {
                        reader.connect();
                    }

                    if (reader.isConnected()) {
                        // Configure region first
                        configureRegion();
                        // Then configure reader settings
                        configureReader();
                        return null; // Success
                    } else {
                        return "Failed to connect to reader";
                    }
                } catch (InvalidUsageException e) {
                    Log.e(TAG, "InvalidUsageException: " + e.getMessage());
                    return "Connection error: " + e.getInfo();
                } catch (OperationFailureException e) {
                    Log.e(TAG, "OperationFailureException: " + e.getMessage());
                    // Check if region not configured
                    if (e.getResults() == RFIDResults.RFID_READER_REGION_NOT_CONFIGURED) {
                        try {
                            configureRegion();
                            configureReader();
                            return null; // Success after configuring region
                        } catch (Exception e2) {
                            return "Region config failed: " + e2.getMessage();
                        }
                    }
                    return "Connection failed: " + e.getResults().toString();
                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e.getMessage());
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String error) {
                btnConnect.setEnabled(true);
                if (error == null) {
                    isReaderConnected = true;
                    updateReaderStatus("Connected: " + reader.getHostName());
                    btnConnect.setText("Disconnect");
                    btnStartScan.setEnabled(true);
                    Toast.makeText(RfidScanActivity.this, "Reader connected", Toast.LENGTH_SHORT).show();
                } else {
                    updateReaderStatus("Disconnected");
                    Toast.makeText(RfidScanActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private void configureRegion() throws InvalidUsageException, OperationFailureException {
        if (reader == null || !reader.isConnected()) return;

        try {
            // Get supported regions
            SupportedRegions supportedRegions = reader.ReaderCapabilities.SupportedRegions;
            int numRegions = supportedRegions.length();
            
            Log.d(TAG, "Supported regions: " + numRegions);
            
            if (numRegions > 0) {
                // Try to find North America (NA) region, or use the first available
                RegionInfo selectedRegion = null;
                
                for (int i = 0; i < numRegions; i++) {
                    RegionInfo region = supportedRegions.getRegionInfo(i);
                    Log.d(TAG, "Region " + i + ": " + region.getRegionCode() + " - " + region.getName());
                    
                    // Prefer NA (North America) or USA region
                    String code = region.getRegionCode();
                    if (code != null && (code.equals("NA") || code.equals("USA") || code.contains("FCC"))) {
                        selectedRegion = region;
                        break;
                    }
                }
                
                // If no NA region found, use the first one
                if (selectedRegion == null) {
                    selectedRegion = supportedRegions.getRegionInfo(0);
                }
                
                // Configure the region
                RegulatoryConfig regulatoryConfig = reader.Config.getRegulatoryConfig();
                regulatoryConfig.setRegion(selectedRegion.getRegionCode());
                
                if (selectedRegion.isHoppingConfigurable()) {
                    regulatoryConfig.setIsHoppingOn(true);
                }
                
                // Set enabled channels
                String[] channels = selectedRegion.getSupportedChannels();
                if (channels != null && channels.length > 0) {
                    regulatoryConfig.setEnabledChannels(channels);
                }
                
                reader.Config.setRegulatoryConfig(regulatoryConfig);
                
                Log.d(TAG, "Region configured: " + selectedRegion.getRegionCode() + " - " + selectedRegion.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error configuring region: " + e.getMessage());
            throw e;
        }
    }

    private void configureReader() throws InvalidUsageException, OperationFailureException {
        if (reader == null || !reader.isConnected()) return;

        // Set up trigger mode
        TriggerInfo triggerInfo = new TriggerInfo();
        triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
        triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);

        // Set up event listener
        reader.Events.addEventsListener(this);
        reader.Events.setInventoryStartEvent(true);
        reader.Events.setInventoryStopEvent(true);
        reader.Events.setTagReadEvent(true);
        reader.Events.setAttachTagDataWithReadEvent(true);
        
        // Configure for handheld trigger
        reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);

        Log.d(TAG, "Reader configured");
    }

    private void disconnectReader() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    if (reader != null) {
                        if (isInventoryRunning) {
                            reader.Actions.Inventory.stop();
                        }
                        reader.Events.removeEventsListener(RfidScanActivity.this);
                        reader.disconnect();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error disconnecting: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                isReaderConnected = false;
                isInventoryRunning = false;
                updateReaderStatus("Disconnected");
                btnConnect.setText("Connect");
                btnStartScan.setText("Start Scanning");
                btnStartScan.setEnabled(false);
                Toast.makeText(RfidScanActivity.this, "Reader disconnected", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void startInventory() {
        if (reader == null || !reader.isConnected()) {
            Toast.makeText(this, "Reader not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            reader.Actions.Inventory.perform();
            isInventoryRunning = true;
            btnStartScan.setText("Stop Scanning");
            updateReaderStatus("Scanning...");
            Log.d(TAG, "Inventory started");
        } catch (InvalidUsageException e) {
            Log.e(TAG, "InvalidUsageException: " + e.getMessage());
            Toast.makeText(this, "Error starting scan: " + e.getInfo(), Toast.LENGTH_SHORT).show();
        } catch (OperationFailureException e) {
            Log.e(TAG, "OperationFailureException: " + e.getMessage());
            Toast.makeText(this, "Scan failed: " + e.getResults().toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopInventory() {
        if (reader == null || !reader.isConnected()) return;

        try {
            reader.Actions.Inventory.stop();
            isInventoryRunning = false;
            btnStartScan.setText("Start Scanning");
            updateReaderStatus("Connected: " + reader.getHostName());
            Log.d(TAG, "Inventory stopped");
        } catch (InvalidUsageException e) {
            Log.e(TAG, "InvalidUsageException: " + e.getMessage());
        } catch (OperationFailureException e) {
            Log.e(TAG, "OperationFailureException: " + e.getMessage());
        }
    }

    // RfidEventsListener implementation
    @Override
    public void eventReadNotify(RfidReadEvents e) {
        TagData[] tags = reader.Actions.getReadTags(100);
        if (tags != null) {
            for (TagData tag : tags) {
                final String epc = tag.getTagID();
                if (epc != null && !scannedEpcs.contains(epc)) {
                    scannedEpcs.add(epc);
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            addScanItem(epc);
                        }
                    });
                }
            }
        }
    }

    @Override
    public void eventStatusNotify(RfidStatusEvents e) {
        final STATUS_EVENT_TYPE eventType = e.StatusEventData.getStatusEventType();
        Log.d(TAG, "Status event: " + eventType);
        
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (eventType == STATUS_EVENT_TYPE.INVENTORY_START_EVENT) {
                    updateReaderStatus("Scanning...");
                } else if (eventType == STATUS_EVENT_TYPE.INVENTORY_STOP_EVENT) {
                    isInventoryRunning = false;
                    btnStartScan.setText("Start Scanning");
                    if (isReaderConnected && reader != null) {
                        updateReaderStatus("Connected: " + reader.getHostName());
                    }
                } else if (eventType == STATUS_EVENT_TYPE.DISCONNECTION_EVENT) {
                    isReaderConnected = false;
                    isInventoryRunning = false;
                    updateReaderStatus("Disconnected");
                    btnConnect.setText("Connect");
                    btnStartScan.setEnabled(false);
                    Toast.makeText(RfidScanActivity.this, "Reader disconnected", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addScanItem(String epc) {
        ScanItem item = new ScanItem(epc, "", userName, room);
        scanItems.add(0, item);
        adapter.notifyDataSetChanged();
        updateScanCount();
        Log.d(TAG, "Added RFID tag: " + epc);
    }

    private void updateScanCount() {
        tvScanCount.setText("Tags: " + scanItems.size());
    }

    private void updateReaderStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvReaderStatus.setText("Reader: " + status);
            }
        });
    }

    private String exportToCsvAndGetFilename() {
        if (scanItems.isEmpty()) {
            return null;
        }

        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.US).format(new Date());
            String safeUserName = userName.replaceAll("[^a-zA-Z0-9]", "_");
            String fileName = "RFID-AUDIT-" + timestamp + "-" + safeUserName + ".csv";

            // Save to Downloads folder for easy access
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File file = new File(exportDir, fileName);
            FileWriter writer = new FileWriter(file);

            // Write header
            writer.append("Timestamp,EPC,Room,Auditor\n");

            // Write data (in reverse order so oldest first)
            for (int i = scanItems.size() - 1; i >= 0; i--) {
                ScanItem item = scanItems.get(i);
                writer.append(escapeForCsv(item.getTimestamp()));
                writer.append(",");
                writer.append(escapeForCsv(item.getBarcodeData()));
                writer.append(",");
                writer.append(escapeForCsv(item.getRoom()));
                writer.append(",");
                writer.append(escapeForCsv(item.getUserName()));
                writer.append("\n");
            }

            writer.flush();
            writer.close();

            // Show success with file location
            Toast.makeText(this, "Saved to Downloads: " + fileName, Toast.LENGTH_LONG).show();

            return fileName;

        } catch (IOException e) {
            Toast.makeText(this, "Error exporting: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null;
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

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{REPORT_EMAIL});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "RFID Audit Report: " + file.getName());
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Please find attached the RFID audit report.\n\nAuditor: " + userName + "\nTags scanned: " + scanItems.size());
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send RFID Audit Report"));
        } catch (android.content.ActivityNotFoundException ex) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Export RFID Audit CSV"));
        }
    }

    private void uploadToAirtableWithCallback(final String csvFileName) {
        final AirtableConfig config = new AirtableConfig(this);
        final AuditLogManager logManager = new AuditLogManager(this);
        final String auditType = "RFID-AUDIT";
        final String auditId = logManager.generateId();
        final String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
        
        // Create audit log entry
        final AuditLog auditLog = new AuditLog(
            auditId,
            auditType,
            userName,
            room,
            timestamp,
            csvFileName,
            scanItems.size(),
            AuditLog.UploadStatus.PENDING,
            new ArrayList<>(scanItems)
        );
        
        if (!config.shouldUpload()) {
            Log.d(TAG, "Airtable upload disabled or not configured");
            // Save as uploaded since Airtable is not configured
            auditLog.setUploadStatus(AuditLog.UploadStatus.UPLOADED);
            auditLog.setScanItems(new ArrayList<ScanItem>()); // Clear items to save space
            logManager.saveAuditLog(auditLog);
            Toast.makeText(this, "Submitted successfully", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (scanItems.isEmpty()) {
            finish();
            return;
        }

        // Save log as pending before upload attempt
        logManager.saveAuditLog(auditLog);

        AirtableUploader uploader = new AirtableUploader(
            config.getApiKey(),
            config.getBaseId(),
            config.getTableName()
        );

        uploader.uploadScans(scanItems, auditType, new AirtableUploader.UploadCallback() {
            @Override
            public void onSuccess(final int recordCount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logManager.updateUploadStatus(auditId, AuditLog.UploadStatus.UPLOADED, null);
                        Toast.makeText(RfidScanActivity.this,
                            "Uploaded " + recordCount + " records to Airtable",
                            Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logManager.updateUploadStatus(auditId, AuditLog.UploadStatus.FAILED, error);
                        String fileName = csvFileName != null ? csvFileName : "the CSV file";
                        new AlertDialog.Builder(RfidScanActivity.this)
                            .setTitle("Airtable Upload Failed")
                            .setMessage("Airtable upload failed, please upload CSV manually:\n\n" + fileName + "\n\nYou can retry from the Audit Log screen.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setCancelable(false)
                            .show();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (reader != null) {
                if (isInventoryRunning) {
                    reader.Actions.Inventory.stop();
                }
                reader.Events.removeEventsListener(this);
                reader.disconnect();
                reader = null;
            }
            if (readers != null) {
                readers.Dispose();
                readers = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
        }
    }
}
