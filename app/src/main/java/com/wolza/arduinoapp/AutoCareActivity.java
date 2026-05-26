package com.wolza.arduinoapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AutoCareActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Button btnSelectDate;
    private TextView tvSelectedDate, tvSummary, tvDetailedPlan, tvTotalWater;
    private LinearLayout layoutResults;

    private Calendar returnDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_care);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSummary = findViewById(R.id.tvSummary);
        tvDetailedPlan = findViewById(R.id.tvDetailedPlan);
        tvTotalWater = findViewById(R.id.tvTotalWater);
        layoutResults = findViewById(R.id.layoutResults);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSelectDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    returnDate.set(Calendar.YEAR, year);
                    returnDate.set(Calendar.MONTH, month);
                    returnDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    
                    tvSelectedDate.setText("Returning on: " + dateFormat.format(returnDate.getTime()));
                    calculateWaterRequirements();
                },
                returnDate.get(Calendar.YEAR),
                returnDate.get(Calendar.MONTH),
                returnDate.get(Calendar.DAY_OF_MONTH));

        // Return date must be in the future
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        datePickerDialog.show();
    }

    private void calculateWaterRequirements() {
        List<MyGardenActivity.GardenPlant> gardenPlants = loadGardenPlants();
        if (gardenPlants.isEmpty()) {
            Toast.makeText(this, "Your garden is empty! Add plants first.", Toast.LENGTH_LONG).show();
            return;
        }

        long diffInMillis = returnDate.getTimeInMillis() - System.currentTimeMillis();
        long daysAway = TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1;

        if (daysAway <= 0) {
            layoutResults.setVisibility(View.GONE);
            return;
        }

        layoutResults.setVisibility(View.VISIBLE);
        tvSummary.setText("You will be away for " + daysAway + " days.");

        StringBuilder planBuilder = new StringBuilder();
        double totalWaterLiters = 0;

        for (MyGardenActivity.GardenPlant plant : gardenPlants) {
            FlowerListActivity.Flower flower = plant.getFlower();
            PlantCareProfile profile = PlantCareDatabase.getInstance(this).getPlantProfile(flower.getName());
            
            // Basic estimation logic:
            // 1. Determine how many times the plant needs water based on baseWateringDays
            // 2. Estimate water volume based on category/difficulty
            
            int wateringInterval = (profile != null) ? profile.getBaseWateringDays() : 4;
            int timesToWater = (int) (daysAway / wateringInterval);
            if (timesToWater == 0 && daysAway > 0) timesToWater = 1; // At least once if away for some time
            
            double waterPerSession = 0.2; // 200ml default
            if (profile != null) {
                if ("суккулент".equals(profile.getCategory())) waterPerSession = 0.1;
                else if ("тропическое".equals(profile.getCategory())) waterPerSession = 0.4;
                else if ("сложная".equals(profile.getDifficulty())) waterPerSession = 0.3;
            }
            
            double plantTotalWater = timesToWater * waterPerSession;
            totalWaterLiters += plantTotalWater;

            planBuilder.append("🌿 ").append(flower.getName())
                    .append("\n   • Schedule: Every ").append(wateringInterval).append(" days")
                    .append("\n   • Estimated sessions: ").append(timesToWater)
                    .append("\n   • Total water: ").append(String.format(Locale.getDefault(), "%.1f L", plantTotalWater))
                    .append("\n\n");
        }

        tvDetailedPlan.setText(planBuilder.toString());
        tvTotalWater.setText(String.format(Locale.getDefault(), "Total Water Needed: %.2f Liters", totalWaterLiters));
    }

    private List<MyGardenActivity.GardenPlant> loadGardenPlants() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyGardenPrefs", Context.MODE_PRIVATE);
        String json = sharedPreferences.getString("garden_plants", "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        } else {
            Gson gson = new Gson();
            Type type = new TypeToken<List<MyGardenActivity.GardenPlant>>() {}.getType();
            return gson.fromJson(json, type);
        }
    }
}
