package com.wolza.arduinoapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.*;
import java.util.Iterator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.*;

public class DoctorActivity extends AppCompatActivity {

    private ImageView imgPlant, btnBack;
    private TextView tvStatus, tvDiseaseName, tvProbability, tvDescription, tvTreatment, tvError;
    private LinearLayout progressLayout, resultLayout, errorLayout;
    private ScrollView scrollViewResults;
    private Button btnDone, btnTryAgain;

    private static final String API_KEY = "m47Uv3pT7feZfTmLgO9sGl9eB8q81JshxDNtRoH10O3frCcvVB";

    private static final String URL =
            "https://api.plant.id/v3/health_assessment?details=local_name,description,url,treatment,classification,common_names,cause";

    private String imageSource; // Can be Base64 or URL

    private OkHttpClient client = new OkHttpClient();

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
            showError("No image provided for analysis. Please scan your plant first.");
            btnTryAgain.setText("Go Back");
            btnTryAgain.setOnClickListener(v -> finish());
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
                showLoading(true);
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
                byte[] decoded = Base64.decode(imageSource, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                imgPlant.setImageBitmap(bmp);
            } catch (Exception e) {
                Log.e("PlantDoctor", "Image decode error");
                Glide.with(this).load(R.drawable.ic_flower).into(imgPlant);
            }
        }
    }

    private void checkPlantHealth() {
        showLoading(true);
        try {
            JSONObject json = new JSONObject();
            JSONArray images = new JSONArray();
            
            // If it's not a URL, ensure it's clean Base64
            String processedImage = imageSource;
            if (!processedImage.startsWith("http") && processedImage.contains(",")) {
                processedImage = processedImage.substring(processedImage.indexOf(",") + 1);
            }
            images.put(processedImage);
            json.put("images", images);
            json.put("latitude", 40.1772); // Armenia approx
            json.put("longitude", 44.5035);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(URL)
                    .addHeader("Api-Key", API_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> showError("Network error. Please check your connection."));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String res = response.body().string();
                    runOnUiThread(() -> parseResponse(res));
                }
            });

        } catch (Exception e) {
            showError("Request preparation failed");
        }
    }

    private void parseResponse(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject result = root.getJSONObject("result");

            if (!result.has("is_healthy")) {
                showError("Could not determine health status.");
                return;
            }

            boolean isHealthy = result.getJSONObject("is_healthy").getBoolean("binary");
            
            if (isHealthy) {
                showHealthy();
            } else {
                JSONObject diseaseObj = result.getJSONObject("disease");
                JSONArray suggestions = diseaseObj.getJSONArray("suggestions");

                if (suggestions.length() == 0) {
                    showHealthy();
                    return;
                }

                JSONObject disease = suggestions.getJSONObject(0);
                String name = disease.getString("name");
                double probability = disease.getDouble("probability") * 100;

                tvDiseaseName.setText(name);
                tvProbability.setText(String.format("%.1f%%", probability));
                tvStatus.setText("Possible Issue Detected");
                tvStatus.setTextColor(0xFFD32F2F);

                if (disease.has("details")) {
                    JSONObject details = disease.getJSONObject("details");
                    if (details.has("description")) tvDescription.setText(details.getString("description"));
                    if (details.has("treatment")) {
                        JSONObject treatment = details.getJSONObject("treatment");
                        StringBuilder sb = new StringBuilder();
                        Iterator<String> keys = treatment.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            JSONArray steps = treatment.getJSONArray(key);
                            sb.append(key.toUpperCase()).append(":\n");
                            for (int i = 0; i < steps.length(); i++) {
                                sb.append("- ").append(steps.getString(i)).append("\n");
                            }
                            sb.append("\n");
                        }
                        tvTreatment.setText(sb.toString());
                    }
                }
                showResults();
            }
        } catch (Exception e) {
            Log.e("PlantDoctor", "Parse error: " + e.getMessage());
            showError("Failed to analyze plant health.");
        }
    }

    private void showHealthy() {
        tvStatus.setText("Your plant is healthy! ✨");
        tvStatus.setTextColor(0xFF2E7D32);
        tvDiseaseName.setText("No issues found");
        tvProbability.setText("100%");
        tvDescription.setText("We didn't detect any diseases. Keep up the good work with your watering plan!");
        tvTreatment.setText("Just continue providing light and following the watering schedule.");
        showResults();
    }

    private void showLoading(boolean show) {
        progressLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        scrollViewResults.setVisibility(show ? View.GONE : View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
    }

    private void showResults() {
        progressLayout.setVisibility(View.GONE);
        scrollViewResults.setVisibility(View.VISIBLE);
        resultLayout.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
    }

    private void showError(String error) {
        progressLayout.setVisibility(View.GONE);
        scrollViewResults.setVisibility(View.GONE);
        resultLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);
        tvError.setText(error);
    }
}

