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

    private String userName;
    private List<ScanItem> scanItems;
    private ScanItemAdapter adapter;
    private TextView tvUserName;
    private TextView tvScanCount;
    private ListView lvScans;
    private String pendingBarcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_scan);

        setTitle("Audit Session");

        userName = getIntent().getStringExtra("USER_NAME");
        scanItems = new ArrayList<>();

        tvUserName = findViewById(R.id.tvUserName);
        tvScanCount = findViewById(R.id.tvScanCount);
        lvScans = findViewById(R.id.lvScans);
        Button btnExport = findViewById(R.id.btnExport);
        Button btnFinish = findViewById(R.id.btnFinish);

        tvUserName.setText("Auditor: " + userName);
        updateScanCount();

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

        // Register for DataWedge intents
        IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction(getResources().getString(R.string.activity_intent_filter_action));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scanReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(scanReceiver, filter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(scanReceiver);
    }

    private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(getResources().getString(R.string.activity_intent_filter_action))) {
                String barcodeData = intent.getStringExtra(
                    getResources().getString(R.string.datawedge_intent_key_data));
                
                if (barcodeData != null && !barcodeData.isEmpty()) {
                    showWeightDialog(barcodeData);
                }
            }
        }
    };

    private void showWeightDialog(final String barcodeData) {
        pendingBarcode = barcodeData;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

    private void addScanItem(String barcodeData, String weight) {
        ScanItem item = new ScanItem(barcodeData, weight, userName);
        scanItems.add(0, item); // Add to top of list
        adapter.notifyDataSetChanged();
        updateScanCount();
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
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = "audit_" + timestamp + ".csv";
            
            File exportDir = new File(getExternalFilesDir(null), "exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            File file = new File(exportDir, fileName);
            FileWriter writer = new FileWriter(file);
            
            // Write header
            writer.append("Barcode,Weight (g),Auditor\n");
            
            // Write data (in reverse order so oldest first)
            for (int i = scanItems.size() - 1; i >= 0; i--) {
                writer.append(scanItems.get(i).toCsvRow());
                writer.append("\n");
            }
            
            writer.flush();
            writer.close();

            // Share the file
            shareFile(file);
            
        } catch (IOException e) {
            Toast.makeText(this, "Error exporting: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, 
            getPackageName() + ".fileprovider", file);
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(shareIntent, "Export Audit CSV"));
    }
}
