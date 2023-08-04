package com.tencent.yolov8ncnn;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class CloudDetectionActivity extends Activity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;

    private ImageView imageView;
    private TextView resultTextView;
    private Button selectImageButton;
    private Button uploadButton;
    private String selectedImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_detection);

        imageView = findViewById(R.id.imageView);
        resultTextView = findViewById(R.id.resultTextView);
        selectImageButton = findViewById(R.id.selectImageButton);
        uploadButton = findViewById(R.id.uploadButton);

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageGallery();
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImagePath != null) {
                    uploadImage(selectedImagePath);
                } else {
                    resultTextView.setText("请先选择图片");
                }
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，申请权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            // 检查权限是否被授予
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已被授予，可以执行上传操作
                uploadImage(selectedImagePath);
            } else {
                // 权限被拒绝，提示用户
                Toast.makeText(this, "需要读取外部存储器的权限才能上传图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImageGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            selectedImagePath = getRealPathFromURI(imageUri);
            imageView.setImageURI(imageUri);
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor == null) {
            return null;
        }
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(columnIndex);
        cursor.close();
        return path;
    }

    private void uploadImage(final String imagePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String serverUrl = "http://192.168.1.104:8001/detect";
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";

                try {
                    URL url = new URL(serverUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + imagePath + "\"" + lineEnd);
                    dos.writeBytes(lineEnd);

                    InputStream fileInputStream = new FileInputStream(imagePath);
                    int bytesRead, bufferSize;
                    byte[] buffer;
                    bufferSize = 1024;
                    buffer = new byte[bufferSize];
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    while (bytesRead > 0) {
                        dos.write(buffer, 0, bufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    
                    int serverResponseCode = conn.getResponseCode();
                    // 获取服务器响应消息
                    String serverResponseMessage = conn.getResponseMessage();

                    if (serverResponseCode == 200) {
                        Log.d("Upload", "File uploaded successfully");

                        // 处理服务器返回的结果，更新UI
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resultTextView.setText("上传成功");

                                // 解析检测结果并更新UI
                                String responseStr = serverResponseMessage;
                                try {
                                    JSONObject responseJson = new JSONObject(responseStr);
                                    boolean success = responseJson.getBoolean("success");
                                    if (success) {
                                        // 获取结果图片的URL并显示
                                        String resultImagePath = responseJson.getString("result_image_path");
                                        Uri resultImageUri = Uri.parse(resultImagePath);
                                        imageView.setImageURI(resultImageUri);

                                        // 获取检测结果并显示
                                        JSONArray detectionResults = responseJson.getJSONArray("detection_results");
                                        showDetectionResults(detectionResults);
                                    } else {
                                        // 上传成功但目标检测失败
                                        resultTextView.setText("目标检测失败");
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Toast.makeText(CloudDetectionActivity.this, "解析服务器响应失败", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Log.e("Upload", "File upload failed, server response code: " + serverResponseCode);

                        // 处理上传失败的情况，更新UI
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resultTextView.setText("上传失败");
                            }
                        });
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void showDetectionResults(JSONArray detectionResults) {
        // 解析云端检测结果并绘制检测框等信息
        // 这里根据检测结果的具体格式来处理，例如绘制检测框、类别标签和置信度等信息
        // ...

        // 示例代码：直接在TextView中显示检测结果的JSON字符串
        resultTextView.setText(detectionResults.toString());
    }
}
