package com.zebra.basicintent1;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;

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

    private String userName;
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
        scanItems = new ArrayList<>();
        scannedEpcs = new HashSet<>();
        uiHandler = new Handler(Looper.getMainLooper());

        tvUserName = findViewById(R.id.tvUserName);
        tvScanCount = findViewById(R.id.tvScanCount);
        tvReaderStatus = findViewById(R.id.tvReaderStatus);
        lvScans = findViewById(R.id.lvScans);
        btnConnect = findViewById(R.id.btnConnect);
        btnStartScan = findViewById(R.id.btnStartScan);
        Button btnExport = findViewById(R.id.btnExport);
        Button btnFinish = findViewById(R.id.btnFinish);

        tvUserName.setText("Auditor: " + userName);
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
                    connectReader();
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

                new AlertDialog.Builder(RfidScanActivity.this)
                    .setTitle("Finish RFID Audit")
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

        // Initialize readers
        initReaders();
    }

    private void initReaders() {
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
        ScanItem item = new ScanItem(epc, "", userName);
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

    private void exportToCsv() {
        if (scanItems.isEmpty()) {
            Toast.makeText(this, "No tags to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.US).format(new Date());
            String safeUserName = userName.replaceAll("[^a-zA-Z0-9]", "_");
            String fileName = "RFID-AUDIT-" + timestamp + "-" + safeUserName + ".csv";

            File exportDir = new File(getExternalFilesDir(null), "exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File file = new File(exportDir, fileName);
            FileWriter writer = new FileWriter(file);

            // Write header
            writer.append("EPC,Auditor\n");

            // Write data (in reverse order so oldest first)
            for (int i = scanItems.size() - 1; i >= 0; i--) {
                ScanItem item = scanItems.get(i);
                writer.append(escapeForCsv(item.getBarcodeData()));
                writer.append(",");
                writer.append(escapeForCsv(item.getUserName()));
                writer.append("\n");
            }

            writer.flush();
            writer.close();

            // Share the file
            shareFile(file);

            Toast.makeText(this, "Exported " + scanItems.size() + " tags", Toast.LENGTH_SHORT).show();

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
