package com.zebra.basicintent1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AuditSetupActivity extends AppCompatActivity {

    private EditText etUserName;
    private Spinner spinnerRoom;
    private String auditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_setup);

        auditMode = getIntent().getStringExtra(MainActivity.EXTRA_AUDIT_MODE);
        if (auditMode == null) {
            auditMode = MainActivity.MODE_WEIGHT;
        }

        // Set title based on mode
        if (MainActivity.MODE_SPEED.equals(auditMode)) {
            setTitle("Audit-Speed Setup");
        } else {
            setTitle("Audit-Weight Setup");
        }

        TextView tvInstructions = findViewById(R.id.tvInstructions);
        if (MainActivity.MODE_SPEED.equals(auditMode)) {
            tvInstructions.setText("Enter your details to begin speed audit (no weight entry)");
        } else {
            tvInstructions.setText("Enter your details to begin the audit session");
        }

        etUserName = findViewById(R.id.etUserName);
        spinnerRoom = findViewById(R.id.spinnerRoom);
        Button btnStartAudit = findViewById(R.id.btnStartAudit);

        btnStartAudit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = etUserName.getText().toString().trim();
                int roomPosition = spinnerRoom.getSelectedItemPosition();
                String room = spinnerRoom.getSelectedItem().toString();
                
                if (userName.isEmpty()) {
                    Toast.makeText(AuditSetupActivity.this, "Please enter your full name", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Position 0 is "Select Room..." placeholder
                if (roomPosition == 0) {
                    Toast.makeText(AuditSetupActivity.this, "Please select a room", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(AuditSetupActivity.this, AuditScanActivity.class);
                intent.putExtra("USER_NAME", userName);
                intent.putExtra("ROOM", room);
                intent.putExtra(MainActivity.EXTRA_AUDIT_MODE, auditMode);
                startActivity(intent);
                finish();
            }
        });
    }
}
