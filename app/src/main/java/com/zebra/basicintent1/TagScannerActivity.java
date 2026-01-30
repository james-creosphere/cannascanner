package com.zebra.basicintent1;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class TagScannerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_scanner);

        setTitle("Tag Scanner");
    }
}
