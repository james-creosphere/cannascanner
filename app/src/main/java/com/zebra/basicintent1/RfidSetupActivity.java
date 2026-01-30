package com.zebra.basicintent1;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RfidSetupActivity extends AppCompatActivity {

    private EditText etUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rfid_setup);

        setTitle("RFID Tag Audit Setup");

        etUserName = findViewById(R.id.etUserName);
        Button btnStartAudit = findViewById(R.id.btnStartAudit);

        btnStartAudit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = etUserName.getText().toString().trim();
                if (userName.isEmpty()) {
                    Toast.makeText(RfidSetupActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(RfidSetupActivity.this, RfidScanActivity.class);
                intent.putExtra("USER_NAME", userName);
                startActivity(intent);
                finish();
            }
        });
    }
}
