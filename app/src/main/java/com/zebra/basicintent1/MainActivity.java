package com.zebra.basicintent1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_AUDIT_MODE = "AUDIT_MODE";
    public static final String MODE_WEIGHT = "weight";
    public static final String MODE_SPEED = "speed";

    private TextView tvAirtableStatus;
    private AirtableConfig airtableConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        airtableConfig = new AirtableConfig(this);

        Button btnAuditWeight = findViewById(R.id.btnAuditWeight);
        Button btnAuditSpeed = findViewById(R.id.btnAuditSpeed);
        Button btnRfidTagAudit = findViewById(R.id.btnRfidTagAudit);
        Button btnSettings = findViewById(R.id.btnSettings);
        tvAirtableStatus = findViewById(R.id.tvAirtableStatus);

        btnAuditWeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AuditSetupActivity.class);
                intent.putExtra(EXTRA_AUDIT_MODE, MODE_WEIGHT);
                startActivity(intent);
            }
        });

        btnAuditSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AuditSetupActivity.class);
                intent.putExtra(EXTRA_AUDIT_MODE, MODE_SPEED);
                startActivity(intent);
            }
        });

        btnRfidTagAudit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RfidSetupActivity.class);
                startActivity(intent);
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAirtableStatus();
    }

    private void updateAirtableStatus() {
        if (airtableConfig.shouldUpload()) {
            tvAirtableStatus.setText("Airtable: Enabled");
            tvAirtableStatus.setTextColor(0xFF4CAF50); // Green
        } else if (airtableConfig.isConfigured()) {
            tvAirtableStatus.setText("Airtable: Configured (disabled)");
            tvAirtableStatus.setTextColor(0xFFFF9800); // Orange
        } else {
            tvAirtableStatus.setText("Airtable: Not configured");
            tvAirtableStatus.setTextColor(0xFF666666); // Gray
        }
    }
}
