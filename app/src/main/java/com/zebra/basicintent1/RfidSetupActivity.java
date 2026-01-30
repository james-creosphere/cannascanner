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
    private EditText etRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rfid_setup);

        setTitle("RFID Tag Audit Setup");

        etUserName = findViewById(R.id.etUserName);
        etRoom = findViewById(R.id.etRoom);
        Button btnStartAudit = findViewById(R.id.btnStartAudit);

        btnStartAudit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = etUserName.getText().toString().trim();
                String room = etRoom.getText().toString().trim();
                
                if (userName.isEmpty()) {
                    Toast.makeText(RfidSetupActivity.this, "Please enter your full name", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (room.isEmpty()) {
                    Toast.makeText(RfidSetupActivity.this, "Please enter the room", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(RfidSetupActivity.this, RfidScanActivity.class);
                intent.putExtra("USER_NAME", userName);
                intent.putExtra("ROOM", room);
                startActivity(intent);
                finish();
            }
        });
    }
}
