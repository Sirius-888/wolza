package com.wolza.arduinoapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoSearchActivity extends AppCompatActivity {

    private static final String TAG = "AutoSearch";
    private PreviewView previewView;
    private ImageButton btnCapture, btnFlash, btnGallery;
    private Button btnRetake, btnIdentify;
    private ImageView imgCaptured;
    private LinearLayout cameraLayout, previewLayout;
    private TextView tvInstruction;
    private ImageView btnBack;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private int flashMode = ImageCapture.FLASH_MODE_OFF;

    private Bitmap capturedBitmap;
    private String mode = "search";
    private String plantName = "";
    private int plantPosition = -1;

    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final int REQUEST_CODE_GALLERY = 200;
    private static final int REQUEST_CODE_IDENTIFY = 300;
    
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA
    };

    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_search);

        // Get extras if coming from Garden verification
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "search";
        plantName = getIntent().getStringExtra("plant_name");
        plantPosition = getIntent().getIntExtra("plant_position", -1);

        initViews();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        setupClickListeners();
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        btnCapture = findViewById(R.id.btnCapture);
        btnFlash = findViewById(R.id.btnFlash);
        btnGallery = findViewById(R.id.btnGallery);
        btnRetake = findViewById(R.id.btnRetake);
        btnIdentify = findViewById(R.id.btnIdentify);
        imgCaptured = findViewById(R.id.imgCaptured);
        cameraLayout = findViewById(R.id.cameraLayout);
        previewLayout = findViewById(R.id.previewLayout);
        tvInstruction = findViewById(R.id.tvInstruction);
        btnBack = findViewById(R.id.btnBack);

        if ("verify".equals(mode)) {
            tvInstruction.setText("Verify: " + plantName);
        }
    }

    private void setupClickListeners() {
        btnCapture.setOnClickListener(v -> capturePhoto());
        btnFlash.setOnClickListener(v -> toggleFlash());
        btnGallery.setOnClickListener(v -> openGallery());
        btnRetake.setOnClickListener(v -> resetToCamera());
        btnIdentify.setOnClickListener(v -> identifyFlower());
        btnBack.setOnClickListener(v -> finish());
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        imageCapture = new ImageCapture.Builder()
                .setFlashMode(flashMode)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void toggleFlash() {
        if (imageCapture == null) return;
        if (flashMode == ImageCapture.FLASH_MODE_OFF) {
            flashMode = ImageCapture.FLASH_MODE_ON;
            btnFlash.setImageResource(R.drawable.ic_flash_on);
        } else {
            flashMode = ImageCapture.FLASH_MODE_OFF;
            btnFlash.setImageResource(R.drawable.ic_flash_off);
        }
        imageCapture.setFlashMode(flashMode);
    }

    private void capturePhoto() {
        if (imageCapture == null) return;

        File photoDir = new File(getExternalFilesDir(null), "FlowerID");
        if (!photoDir.exists()) photoDir.mkdirs();

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis());
        File photoFile = new File(photoDir, "IMG_" + timestamp + ".jpg");

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        capturedBitmap = processAndResize(photoFile.getAbsolutePath());
                        if (capturedBitmap != null) {
                            showCapturedImagePreview();
                        } else {
                            Toast.makeText(AutoSearchActivity.this, "Failed to process image", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Toast.makeText(AutoSearchActivity.this, "Capture failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Bitmap processAndResize(String path) {
        try {
            Bitmap original = BitmapFactory.decodeFile(path);
            if (original == null) return null;

            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: matrix.postRotate(90); break;
                case ExifInterface.ORIENTATION_ROTATE_180: matrix.postRotate(180); break;
                case ExifInterface.ORIENTATION_ROTATE_270: matrix.postRotate(270); break;
            }

            int maxSize = 512;
            float scale = Math.min((float)maxSize / original.getWidth(), (float)maxSize / original.getHeight());
            if (scale < 1.0) matrix.postScale(scale, scale);

            return Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
        } catch (Exception e) {
            Log.e(TAG, "Image processing error", e);
            return null;
        }
    }

    private void showCapturedImagePreview() {
        cameraLayout.setVisibility(View.GONE);
        previewLayout.setVisibility(View.VISIBLE);
        imgCaptured.setImageBitmap(capturedBitmap);
        if ("verify".equals(mode)) {
            tvInstruction.setText("Verify " + plantName + "?");
        } else {
            tvInstruction.setText("Identify this Flower?");
        }
    }

    private void resetToCamera() {
        previewLayout.setVisibility(View.GONE);
        cameraLayout.setVisibility(View.VISIBLE);
        capturedBitmap = null;
        if ("verify".equals(mode)) {
            tvInstruction.setText("Verify: " + plantName);
        } else {
            tvInstruction.setText("Take a clear photo of the flower");
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                
                int maxSize = 512;
                float scale = Math.min((float)maxSize / bitmap.getWidth(), (float)maxSize / bitmap.getHeight());
                Matrix matrix = new Matrix();
                if (scale < 1.0) matrix.postScale(scale, scale);
                capturedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                
                showCapturedImagePreview();
            } catch (IOException e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_IDENTIFY && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data);
            finish();
        }
    }

    private void identifyFlower() {
        if (capturedBitmap == null) return;
        
        Toast.makeText(this, "Preparing flower data...", Toast.LENGTH_SHORT).show();
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
        byte[] bytes = stream.toByteArray();
        String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);

        Intent intent = new Intent(this, IdentificationResultActivity.class);
        intent.putExtra("image", base64Image);
        intent.putExtra("mode", mode);
        intent.putExtra("plant_name", plantName);
        intent.putExtra("plant_position", plantPosition);
        
        if ("verify".equals(mode)) {
            startActivityForResult(intent, REQUEST_CODE_IDENTIFY);
        } else {
            startActivity(intent);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
