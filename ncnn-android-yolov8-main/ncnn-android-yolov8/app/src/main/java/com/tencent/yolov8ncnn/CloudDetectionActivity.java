package com.tencent.yolov8ncnn;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CloudDetectionActivity extends Activity {

    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_PERMISSION = 2;

    private ImageView imageView;
    private TextView resultTextView;
    private Bitmap selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_detection);

        imageView = findViewById(R.id.imageView);
        resultTextView = findViewById(R.id.resultTextView);
        Button selectImageButton = findViewById(R.id.selectImageButton);
        Button uploadButton = findViewById(R.id.uploadButton);

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    openImagePicker();
                } else {
                    requestPermission();
                }
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImage != null) {
                    new UploadImageTask().execute();
                } else {
                    Toast.makeText(CloudDetectionActivity.this, "请先选择一张图像", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "需要授权访问外部存储器才能选择图像", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                selectedImage = BitmapFactory.decodeStream(inputStream);
                imageView.setImageBitmap(selectedImage);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "无法读取图像", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class UploadImageTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // 将Bitmap转换为Base64编码的字符串
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                String imageString = Base64.encodeToString(byteArray, Base64.DEFAULT);

                // 创建JSON对象
                JSONObject json = new JSONObject();
                json.put("image", imageString);

                // 创建HTTP连接
                URL url = new URL("https://qisingtech.xicp.fun/detect"); // 替换为您的服务器地址
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // 发送数据
                byte[] data = json.toString().getBytes();
                conn.getOutputStream().write(data);

                // 接收响应数据
                InputStream inputStream = conn.getInputStream();
                ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    responseStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();

                // 将响应数据解码为字符串
                String responseString = responseStream.toString("UTF-8");
                return responseString;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String responseString) {
            if (responseString != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(responseString);
                    String message = jsonResponse.getString("message");
                    if (message.equals("No objects detected")) {
                        // 在应用中显示 "没有检测到目标"
                        resultTextView.setText("没有检测到目标");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                resultTextView.setText("连接服务器失败");
            }
        }
    }
}


