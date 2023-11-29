package com.tencent.yolov8ncnn;// NewMainActivity.java

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class NewMainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_main);

        ImageButton btnCloudDetection = findViewById(R.id.btnCloudDetection);
        ImageButton btnLocalDetection = findViewById(R.id.btnLocalDetection);

        btnCloudDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 启动 CloudDetectionActivity
                Intent intent = new Intent(NewMainActivity.this, CloudDetectionActivity.class);
                startActivity(intent);
            }
        });

        btnLocalDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 启动 LocalDetectionActivity
                Intent intent = new Intent(NewMainActivity.this, LocalDetectionActivity.class);
                startActivity(intent);
            }
        });
    }
}
