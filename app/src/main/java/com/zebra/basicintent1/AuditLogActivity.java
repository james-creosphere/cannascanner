package com.zebra.basicintent1;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class AuditLogActivity extends AppCompatActivity {

    private AuditLogManager logManager;
    private List<AuditLog> logs;
    private AuditLogAdapter adapter;
    private TextView tvEmpty;
    private ListView lvAuditLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_log);

        setTitle("Audit Log");

        logManager = new AuditLogManager(this);
        tvEmpty = findViewById(R.id.tvEmpty);
        lvAuditLogs = findViewById(R.id.lvAuditLogs);

        loadLogs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLogs();
    }

    private void loadLogs() {
        logs = logManager.getAllLogs();

        if (logs.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvAuditLogs.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            lvAuditLogs.setVisibility(View.VISIBLE);
            adapter = new AuditLogAdapter(this, logs);
            lvAuditLogs.setAdapter(adapter);
        }
    }

    private void retryUpload(final AuditLog log) {
        AirtableConfig config = new AirtableConfig(this);

        if (!config.isConfigured()) {
            Toast.makeText(this, "Airtable not configured. Go to Settings.", Toast.LENGTH_LONG).show();
            return;
        }

        if (log.getScanItems() == null || log.getScanItems().isEmpty()) {
            Toast.makeText(this, "No scan data available for retry", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Retrying upload...", Toast.LENGTH_SHORT).show();

        AirtableUploader uploader = new AirtableUploader(
            config.getApiKey(),
            config.getBaseId(),
            config.getTableName()
        );

        uploader.uploadScans(log.getScanItems(), log.getAuditType(), new AirtableUploader.UploadCallback() {
            @Override
            public void onSuccess(final int recordCount) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logManager.updateUploadStatus(log.getId(), AuditLog.UploadStatus.UPLOADED, null);
                        Toast.makeText(AuditLogActivity.this,
                            "Successfully uploaded " + recordCount + " records",
                            Toast.LENGTH_SHORT).show();
                        loadLogs();
                    }
                });
            }

            @Override
            public void onError(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logManager.updateUploadStatus(log.getId(), AuditLog.UploadStatus.FAILED, error);
                        Toast.makeText(AuditLogActivity.this,
                            "Upload failed: " + error,
                            Toast.LENGTH_LONG).show();
                        loadLogs();
                    }
                });
            }
        });
    }

    private class AuditLogAdapter extends ArrayAdapter<AuditLog> {

        public AuditLogAdapter(Context context, List<AuditLog> logs) {
            super(context, R.layout.item_audit_log, logs);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_audit_log, parent, false);
            }

            final AuditLog log = getItem(position);

            TextView tvAuditType = convertView.findViewById(R.id.tvAuditType);
            TextView tvAuditDetails = convertView.findViewById(R.id.tvAuditDetails);
            TextView tvAuditTimestamp = convertView.findViewById(R.id.tvAuditTimestamp);
            TextView tvStatus = convertView.findViewById(R.id.tvStatus);
            Button btnRetry = convertView.findViewById(R.id.btnRetry);

            tvAuditType.setText(log.getAuditType());
            tvAuditDetails.setText(log.getAuditor() + " | " + log.getRoom() + " | " + log.getScanCount() + " scans");
            tvAuditTimestamp.setText(log.getTimestamp());

            switch (log.getUploadStatus()) {
                case UPLOADED:
                    tvStatus.setText("Uploaded");
                    tvStatus.setTextColor(0xFF4CAF50); // Green
                    btnRetry.setVisibility(View.GONE);
                    break;
                case FAILED:
                    tvStatus.setText("Failed");
                    tvStatus.setTextColor(0xFFF44336); // Red
                    btnRetry.setVisibility(View.VISIBLE);
                    btnRetry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            retryUpload(log);
                        }
                    });
                    break;
                case PENDING:
                    tvStatus.setText("Pending");
                    tvStatus.setTextColor(0xFFFF9800); // Orange
                    btnRetry.setVisibility(View.GONE);
                    break;
            }

            return convertView;
        }
    }
}
