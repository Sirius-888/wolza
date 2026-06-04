package com.wolza.arduinoapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AddItemActivity extends AppCompatActivity {

    private ImageView imgItem;
    private EditText etName, etPrice, etDescription;
    private Button btnSelectImage, btnAutoIdentify, btnSubmit;
    private ProgressBar progressBar;
    private ImageView btnBack;

    private String base64Image = "";
    private String itemCategory = "Plants"; // Default

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Get category from Intent
        if (getIntent().hasExtra("category")) {
            itemCategory = getIntent().getStringExtra("category");
        }

        initViews();
        setupToolbar();
        setupClickListeners();

        // Hide AI identify for non-plant items
        if (!itemCategory.equalsIgnoreCase("Plants")) {
            btnAutoIdentify.setVisibility(View.GONE);
        }
    }

    private void initViews() {
        imgItem = findViewById(R.id.imgItem);
        etName = findViewById(R.id.etName);
        etPrice = findViewById(R.id.etPrice);
        etDescription = findViewById(R.id.etDescription);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnAutoIdentify = findViewById(R.id.btnAutoIdentify);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String title = "Add " + itemCategory;
            if (itemCategory.equals("Wolza")) title = "Add Wolza Product";
            getSupportActionBar().setTitle(title);
        }
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> openImagePicker());
        btnAutoIdentify.setOnClickListener(v -> autoIdentifyPlant());
        btnSubmit.setOnClickListener(v -> submitItem());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            Bitmap resizedBitmap = ImageUtils.handleImageUri(this, uri, 600);
            if (resizedBitmap != null) {
                imgItem.setImageBitmap(resizedBitmap);
                base64Image = ImageUtils.bitmapToBase64(resizedBitmap, 80);
            } else {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void autoIdentifyPlant() {
        if (base64Image.isEmpty()) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        btnAutoIdentify.setText("Identifying...");

        ApiService.getInstance().identifyPlant(base64Image, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    showLoading(false);
                    btnAutoIdentify.setText("AI Identify");
                    try {
                        JSONObject result = response.getJSONObject("result");
                        JSONObject classification = result.getJSONObject("classification");
                        JSONArray suggestions = classification.getJSONArray("suggestions");

                        if (suggestions.length() > 0) {
                            JSONObject top = suggestions.getJSONObject(0);
                            String name = top.getString("name");
                            if (top.has("details")) {
                                JSONObject details = top.getJSONObject("details");
                                if (details.has("common_names")) {
                                    JSONArray commonNames = details.getJSONArray("common_names");
                                    if (commonNames.length() > 0) name = commonNames.getString(0);
                                }
                            }
                            etName.setText(capitalize(name));
                        }
                    } catch (Exception e) {
                        Toast.makeText(AddItemActivity.this, "Identification error", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    showLoading(false);
                    btnAutoIdentify.setText("AI Identify");
                    Toast.makeText(AddItemActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void submitItem() {
        String name = etName.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(price) || base64Image.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("price", price);
        item.put("description", description);
        item.put("imageUrl", base64Image);
        item.put("category", itemCategory);
        item.put("sellerId", currentUser.getUid());
        item.put("sellerName", itemCategory.equals("Wolza") ? "Official Wolza Shop" : (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User"));
        item.put("timestamp", System.currentTimeMillis());

        db.collection("market_items").add(item)
                .addOnSuccessListener(documentReference -> {
                    showLoading(false);
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!show);
        btnSubmit.setText(show ? "Listing..." : "List Item");
    }
}
