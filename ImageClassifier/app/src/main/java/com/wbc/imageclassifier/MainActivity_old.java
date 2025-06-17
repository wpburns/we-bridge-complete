//package com.wbc.imageclassifier;
//
//import android.Manifest;
//import android.app.Activity;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.database.Cursor;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//import androidx.annotation.NonNull;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import okhttp3.*;
//import org.json.JSONException;
//import org.json.JSONObject;
//import java.io.File;
//import java.io.IOException;
//
//public class MainActivity2 extends Activity {
//    private static final int PICK_IMAGE_REQUEST = 1;
//    private static final int PERMISSION_REQUEST_CODE = 100;
//    private static final String API_ENDPOINT = "https://correct-optimal-marmoset.ngrok-free.app/detects"; // Replace with your API endpoint
//
//    private Button selectImageBtn, uploadBtn;
//    private TextView responseText;
//    private Uri selectedImageUri;
//    private OkHttpClient client;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        client = new OkHttpClient();
//
//        selectImageBtn = findViewById(R.id.selectImageBtn);
//        uploadBtn = findViewById(R.id.uploadBtn);
//        responseText = findViewById(R.id.responseText);
//
//        selectImageBtn.setOnClickListener(v -> checkPermissionAndSelectImage());
//        uploadBtn.setOnClickListener(v -> uploadImage());
//
//        uploadBtn.setEnabled(false);
//    }
//
//    private void checkPermissionAndSelectImage() {
//        // For Android 13+ (API 33+), we need READ_MEDIA_IMAGES instead of READ_EXTERNAL_STORAGE
//        String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
//                ? Manifest.permission.READ_MEDIA_IMAGES
//                : Manifest.permission.READ_EXTERNAL_STORAGE;
//
//        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
//            // Show rationale if needed
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
//                Toast.makeText(this, "Permission needed to access images", Toast.LENGTH_LONG).show();
//            }
//            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
//        } else {
//            selectImage();
//        }
//    }
//
//    private void selectImage() {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(intent, PICK_IMAGE_REQUEST);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
//                selectImage();
//            } else {
//                Toast.makeText(this, "Permission denied. Cannot access images.", Toast.LENGTH_LONG).show();
//                // Check if user selected "Don't ask again"
//                String permission = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
//                        ? Manifest.permission.READ_MEDIA_IMAGES
//                        : Manifest.permission.READ_EXTERNAL_STORAGE;
//
//                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
//                    Toast.makeText(this, "Please enable permission in Settings", Toast.LENGTH_LONG).show();
//                }
//            }
//        }
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
//            selectedImageUri = data.getData();
//            if (selectedImageUri != null) {
//                uploadBtn.setEnabled(true);
//                Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    private void uploadImage() {
//        if (selectedImageUri == null) {
//            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        uploadBtn.setEnabled(false);
//        responseText.setText("Uploading...");
//
//        String filePath = getRealPathFromURI(selectedImageUri);
//        if (filePath == null) {
//            Toast.makeText(this, "Error getting file path", Toast.LENGTH_SHORT).show();
//            uploadBtn.setEnabled(true);
//            return;
//        }
//
//        File file = new File(filePath);
//        RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), file);
//        MultipartBody requestBody = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("image", file.getName(), fileBody)
//                .build();
//
//        Request request = new Request.Builder()
//                .url(API_ENDPOINT)
//                .post(requestBody)
//                .addHeader("ngrok-skip-browser-warning", "true")
//                .addHeader("User-Agent", "ImageUploadApp/1.0 (Android; Mobile)")
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                runOnUiThread(() -> {
//                    responseText.setText("Upload failed: " + e.getMessage());
//                    uploadBtn.setEnabled(true);
//                });
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                String responseBody = response.body().string();
//                runOnUiThread(() -> {
//                    if (response.isSuccessful()) {
//                        displayJsonResponse(responseBody);
//                    } else {
//                        responseText.setText("Upload failed: " + response.code());
//                    }
//                    uploadBtn.setEnabled(true);
//                });
//            }
//        });
//    }
//
//    private void displayJsonResponse(String jsonString) {
//        try {
//            JSONObject json = new JSONObject(jsonString);
//            StringBuilder formatted = new StringBuilder();
//            formatted.append("API Response:\n\n");
//
//            // Format JSON for display
//            formatJsonObject(json, formatted, 0);
//
//            responseText.setText(formatted.toString());
//        } catch (JSONException e) {
//            responseText.setText("Response: " + jsonString);
//        }
//    }
//
//    private void formatJsonObject(JSONObject json, StringBuilder sb, int indent) throws JSONException {
//        String indentStr = "  ".repeat(indent);
//        java.util.Iterator<String> keys = json.keys();
//
//        while (keys.hasNext()) {
//            String key = keys.next();
//            Object value = json.get(key);
//
//            sb.append(indentStr).append(key).append(": ");
//
//            if (value instanceof JSONObject) {
//                sb.append("\n");
//                formatJsonObject((JSONObject) value, sb, indent + 1);
//            } else {
//                sb.append(value.toString()).append("\n");
//            }
//        }
//    }
//
//    private String getRealPathFromURI(Uri uri) {
//        String[] projection = {MediaStore.Images.Media.DATA};
//        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
//            if (cursor != null && cursor.moveToFirst()) {
//                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//                return cursor.getString(columnIndex);
//            }
//        }
//        return null;
//    }
//}