package com.wolza.arduinoapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;

public class IdentificationResultActivity extends AppCompatActivity {

    private static final String TAG = "IdentificationResult";

    private ImageView imgPlant;
    private TextView tvPlantName, tvScientificName, tvConfidence, tvStatus, tvError;
    private ProgressBar progressBar;
    private LinearLayout resultLayout, errorLayout, progressLayout;
    private ScrollView scrollViewResults;
    private Button btnTryAgain, btnDone;
    private CardView cardMoreInfo;
    private ImageView btnBack;

    private String base64Image;
    private String mode = "search";
    private int plantPosition = -1;
    private String identifiedCommonName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identification_result);

        initViews();
        setupClickListeners();

        base64Image = getIntent().getStringExtra("image");
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "search";
        plantPosition = getIntent().getIntExtra("plant_position", -1);

        if (base64Image != null && !base64Image.isEmpty()) {
            displayImage();
            identifyFlower();
        } else {
            showError("No image received");
        }
    }

    private void initViews() {
        imgPlant = findViewById(R.id.imgPlant);
        tvPlantName = findViewById(R.id.tvPlantName);
        tvScientificName = findViewById(R.id.tvScientificName);
        tvConfidence = findViewById(R.id.tvConfidence);
        tvStatus = findViewById(R.id.tvStatus);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);
        progressLayout = findViewById(R.id.progressLayout);
        resultLayout = findViewById(R.id.resultLayout);
        errorLayout = findViewById(R.id.errorLayout);
        scrollViewResults = findViewById(R.id.scrollViewResults);
        btnTryAgain = findViewById(R.id.btnTryAgain);
        btnDone = findViewById(R.id.btnDone);
        cardMoreInfo = findViewById(R.id.cardMoreInfo);
        btnBack = findViewById(R.id.btnBack);

        showLoading(true);
    }

    private void setupClickListeners() {
        btnTryAgain.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> handleDone());
        btnBack.setOnClickListener(v -> finish());

        cardMoreInfo.setOnClickListener(v -> {
            if (!identifiedCommonName.isEmpty()) {
                Toast.makeText(this, "Learning more about " + identifiedCommonName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleDone() {
        if (!identifiedCommonName.isEmpty()) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("identified_plant", identifiedCommonName);
            resultIntent.putExtra("image", base64Image); // CRITICAL: Pass the photo back!
            resultIntent.putExtra("plant_position", plantPosition);
            resultIntent.putExtra("expected_plant", getIntent().getStringExtra("plant_name"));
            setResult(RESULT_OK, resultIntent);
        }
        finish();
    }

    private void displayImage() {
        try {
            byte[] decoded = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            imgPlant.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Image decode error", e);
        }
    }

    private void identifyFlower() {
        ApiService.getInstance().identifyPlant(base64Image, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> parseResponse(response));
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> showError(errorMessage));
            }
        });
    }

    private void parseResponse(JSONObject json) {
        try {
            JSONObject result = json.getJSONObject("result");
            
            if (result.has("is_plant")) {
                JSONObject isPlantObj = result.getJSONObject("is_plant");
                if (!isPlantObj.optBoolean("binary", true)) {
                    showError("This doesn't look like a plant or flower.");
                    return;
                }
            }

            JSONObject classification = result.getJSONObject("classification");
            JSONArray suggestions = classification.getJSONArray("suggestions");

            if (suggestions.length() == 0) {
                showError("No flower identified. Try a clearer photo.");
                return;
            }

            JSONObject topMatch = suggestions.getJSONObject(0);
            String scientificName = topMatch.optString("name", "Unknown");
            double probability = topMatch.optDouble("probability", 0) * 100;

            identifiedCommonName = scientificName;
            if (topMatch.has("details")) {
                JSONObject details = topMatch.getJSONObject("details");
                if (details.has("common_names")) {
                    JSONArray commonNames = details.getJSONArray("common_names");
                    if (commonNames.length() > 0) {
                        identifiedCommonName = commonNames.getString(0);
                    }
                }
            }

            tvPlantName.setText(capitalize(identifiedCommonName));
            tvScientificName.setText(scientificName);
            
            DecimalFormat df = new DecimalFormat("#.#");
            tvConfidence.setText("Match Confidence: " + df.format(probability) + "%");

            showResults();

        } catch (Exception e) {
            Log.e(TAG, "Parse error", e);
            showError("Analysis Failed: Could not process flower data.");
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void showLoading(boolean show) {
        progressLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        scrollViewResults.setVisibility(show ? View.GONE : View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        tvStatus.setText("Identifying flower...");
    }

    private void showResults() {
        progressLayout.setVisibility(View.GONE);
        resultLayout.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        scrollViewResults.setVisibility(View.VISIBLE);
    }

    private void showError(String error) {
        progressLayout.setVisibility(View.GONE);
        resultLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        scrollViewResults.setVisibility(View.VISIBLE);
        tvError.setText(error);
    }
}