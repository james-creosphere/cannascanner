package com.zebra.basicintent1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_AUDIT_MODE = "AUDIT_MODE";
    public static final String MODE_WEIGHT = "weight";
    public static final String MODE_SPEED = "speed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAuditWeight = findViewById(R.id.btnAuditWeight);
        Button btnAuditSpeed = findViewById(R.id.btnAuditSpeed);
        Button btnRfidTagAudit = findViewById(R.id.btnRfidTagAudit);

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
    }
}
