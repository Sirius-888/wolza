package com.wolza.arduinoapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class DoctorActivity extends AppCompatActivity {

    private static final String TAG = "DoctorActivity";
    private ImageView imgPlant, btnBack;
    private TextView tvStatus, tvDiseaseName, tvProbability, tvDescription, tvTreatment, tvError, tvProgressStatus;
    private LinearLayout progressLayout, resultLayout, errorLayout;
    private ScrollView scrollViewResults;
    private Button btnDone, btnTryAgain;

    private String imageSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        initViews();

        imageSource = getIntent().getStringExtra("image");

        if (imageSource != null && !imageSource.isEmpty()) {
            displayImage();
            checkPlantHealth();
        } else {
            showError("No image received. Please take a photo of your plant's leaves.");
        }
    }

    private void initViews() {
        imgPlant = findViewById(R.id.imgPlant);
        btnBack = findViewById(R.id.btnBack);
        tvStatus = findViewById(R.id.tvStatus);
        tvDiseaseName = findViewById(R.id.tvDiseaseName);
        tvProbability = findViewById(R.id.tvProbability);
        tvDescription = findViewById(R.id.tvDescription);
        tvTreatment = findViewById(R.id.tvTreatment);
        tvError = findViewById(R.id.tvError);
        tvProgressStatus = findViewById(R.id.tvProgressStatus);

        progressLayout = findViewById(R.id.progressLayout);
        resultLayout = findViewById(R.id.resultLayout);
        errorLayout = findViewById(R.id.errorLayout);
        scrollViewResults = findViewById(R.id.scrollViewResults);

        btnDone = findViewById(R.id.btnDone);
        btnTryAgain = findViewById(R.id.btnTryAgain);

        btnBack.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> finish());
        btnTryAgain.setOnClickListener(v -> {
            if (imageSource != null) {
                checkPlantHealth();
            } else {
                finish();
            }
        });
    }

    private void displayImage() {
        if (imageSource.startsWith("http")) {
            Glide.with(this).load(imageSource).into(imgPlant);
        } else {
            try {
                String pureBase64 = imageSource;
                if (pureBase64.contains(",")) {
                    pureBase64 = pureBase64.substring(pureBase64.indexOf(",") + 1);
                }
                byte[] decoded = Base64.decode(pureBase64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                imgPlant.setImageBitmap(bmp);
            } catch (Exception e) {
                imgPlant.setImageResource(R.drawable.ic_flower);
            }
        }
    }

    private void checkPlantHealth() {
        showLoading(true);
        ApiService.getInstance().checkHealth(imageSource, new ApiService.ApiCallback() {
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

    private void parseResponse(JSONObject root) {
        try {
            Log.d(TAG, "Parsing Response: " + root.toString());
            
            if (!root.has("result")) {
                showError("The doctor couldn't find any results for this image.");
                return;
            }

            JSONObject result = root.getJSONObject("result");
            
            // 1. Is it a plant?
            if (result.has("is_plant")) {
                JSONObject isPlantObj = result.getJSONObject("is_plant");
                if (!isPlantObj.optBoolean("binary", true) && isPlantObj.optDouble("probability", 1.0) > 0.8) {
                    showError("This doesn't look like a plant. Please take a clearer photo of the leaves.");
                    return;
                }
            }

            // 2. Health Assessment
            JSONObject healthyObj = result.optJSONObject("is_healthy");
            // Defaults to false/0 if missing to avoid "always healthy" bug
            boolean isHealthyBinary = healthyObj != null && healthyObj.optBoolean("binary", false);
            double healthProb = healthyObj != null ? healthyObj.optDouble("probability", 0.0) : 0.0;

            JSONObject diseaseObj = result.optJSONObject("disease");
            JSONArray suggestions = (diseaseObj != null) ? diseaseObj.optJSONArray("suggestions") : null;

            // Decision Logic:
            // If the AI is not confident it's healthy, or it has strong disease suggestions, show the disease.
            if (!isHealthyBinary || (suggestions != null && suggestions.length() > 0 && suggestions.getJSONObject(0).optDouble("probability", 0) > 0.15)) {
                if (suggestions != null && suggestions.length() > 0) {
                    showDisease(suggestions.getJSONObject(0), healthProb);
                } else {
                    showHealthy(healthProb); // Fallback
                }
            } else {
                showHealthy(healthProb);
            }

        } catch (Exception e) {
            Log.e(TAG, "Parse error", e);
            showError("Analysis failed. Please try again with a better photo.");
        }
    }

    private void showDisease(JSONObject disease, double healthScore) throws Exception {
        String name = disease.optString("name", "Unknown Issue");
        double prob = disease.optDouble("probability", 0) * 100;
        
        tvStatus.setText("Plant Condition: Sick / Issue Detected ⚠️");
        tvStatus.setTextColor(0xFFD32F2F); // Red
        
        tvDiseaseName.setText(name);
        tvProbability.setText(String.format("Health Score: %.1f%% | Diagnosis Confidence: %.1f%%", healthScore * 100, prob));
        
        if (disease.has("details")) {
            JSONObject details = disease.getJSONObject("details");
            tvDescription.setText(details.optString("description", "No detailed description available for this issue."));
            
            JSONObject treatment = details.optJSONObject("treatment");
            if (treatment != null) {
                StringBuilder sb = new StringBuilder();
                Iterator<String> keys = treatment.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONArray steps = treatment.getJSONArray(key);
                    String category = key.substring(0, 1).toUpperCase() + key.substring(1);
                    sb.append(category).append(":\n");
                    for (int i = 0; i < steps.length(); i++) {
                        sb.append("• ").append(steps.getString(i)).append("\n");
                    }
                    sb.append("\n");
                }
                tvTreatment.setText(sb.toString().trim());
            } else {
                tvTreatment.setText("General Care: Prune affected leaves, check for pests, and ensure correct watering.");
            }
        } else {
            tvDescription.setText("No further details available.");
            tvTreatment.setText("Contact a plant specialist if the symptoms persist.");
        }
        showResults();
    }

    private void showHealthy(double healthScore) {
        tvStatus.setText("Plant Condition: Healthy ✨");
        tvStatus.setTextColor(0xFF2E7D32); // Green
        
        tvDiseaseName.setText("No Diseases Found");
        tvProbability.setText(String.format("Health Score: %.1f%%", healthScore * 100));
        tvDescription.setText("Your plant looks strong! We didn't detect any significant signs of diseases or pests in this photo.");
        tvTreatment.setText("Maintain current care: \n• Ensure proper sunlight\n• Stick to watering schedule\n• Clean dust off leaves regularly");
        showResults();
    }

    private void showLoading(boolean show) {
        progressLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        scrollViewResults.setVisibility(show ? View.GONE : View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        if (show) tvProgressStatus.setText("The Doctor is examining your plant...");
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
        scrollViewResults.setVisibility(View.GONE);
        tvError.setText(error);
    }
}
