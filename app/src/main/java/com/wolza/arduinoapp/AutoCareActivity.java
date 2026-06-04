package com.wolza.arduinoapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AutoCareActivity extends AppCompatActivity {

    private static final String TAG = "AutoCareActivity";

    private ImageView btnBack;
    private Button btnDepartureDate, btnReturnDate, btnSavePlan;
    private TextView tvDateRange, tvPlantList, tvSummary, tvDetailedPlan, tvTotalWater;
    private LinearLayout layoutResults;

    private Calendar departureDate = Calendar.getInstance();
    private Calendar returnDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private boolean departureSelected = false;
    private boolean returnSelected = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        try {
            super.attachBaseContext(LocaleHelper.onAttach(newBase));
        } catch (Exception e) {
            super.attachBaseContext(newBase);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_auto_care);
            initViews();
            setupClickListeners();
            displayPlants();
            loadSavedPlan();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error loading screen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnDepartureDate = findViewById(R.id.btnDepartureDate);
        btnReturnDate = findViewById(R.id.btnReturnDate);
        btnSavePlan = findViewById(R.id.btnSavePlan);
        tvDateRange = findViewById(R.id.tvDateRange);
        tvPlantList = findViewById(R.id.tvPlantList);
        tvSummary = findViewById(R.id.tvSummary);
        tvDetailedPlan = findViewById(R.id.tvDetailedPlan);
        tvTotalWater = findViewById(R.id.tvTotalWater);
        layoutResults = findViewById(R.id.layoutResults);
    }

    private void setupClickListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnDepartureDate != null) btnDepartureDate.setOnClickListener(v -> showDatePicker(true));
        if (btnReturnDate != null) btnReturnDate.setOnClickListener(v -> showDatePicker(false));
        if (btnSavePlan != null) btnSavePlan.setOnClickListener(v -> saveVacationPlan());
    }

    private void loadSavedPlan() {
        SharedPreferences prefs = getSharedPreferences("VacationPrefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean("is_active", false)) {
            departureDate.setTimeInMillis(prefs.getLong("departure_date", System.currentTimeMillis()));
            returnDate.setTimeInMillis(prefs.getLong("return_date", System.currentTimeMillis()));
            departureSelected = true;
            returnSelected = true;
            
            updateDateDisplay();
            
            if (layoutResults != null) layoutResults.setVisibility(View.VISIBLE);
            if (tvSummary != null) tvSummary.setText(prefs.getString("summary", ""));
            if (tvDetailedPlan != null) tvDetailedPlan.setText(prefs.getString("detailed_plan", ""));
            if (tvTotalWater != null) tvTotalWater.setText(prefs.getString("total_water", ""));
            
            Toast.makeText(this, "Loaded saved vacation plan", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePicker(boolean isDeparture) {
        Calendar current = isDeparture ? departureDate : returnDate;
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    if (isDeparture) {
                        departureDate.set(year, month, dayOfMonth, 0, 0, 0);
                        departureSelected = true;
                    } else {
                        returnDate.set(year, month, dayOfMonth, 23, 59, 59);
                        returnSelected = true;
                    }
                    updateDateDisplay();
                },
                current.get(Calendar.YEAR),
                current.get(Calendar.MONTH),
                current.get(Calendar.DAY_OF_MONTH));

        if (isDeparture) {
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        } else if (departureSelected) {
            datePickerDialog.getDatePicker().setMinDate(departureDate.getTimeInMillis() + TimeUnit.DAYS.toMillis(1));
        }
        
        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        String depStr = departureSelected ? dateFormat.format(departureDate.getTime()) : "...";
        String retStr = returnSelected ? dateFormat.format(returnDate.getTime()) : "...";
        if (tvDateRange != null) tvDateRange.setText("Dates: " + depStr + " — " + retStr);

        if (departureSelected && returnSelected) {
            if (returnDate.before(departureDate)) {
                Toast.makeText(this, "Return date must be after departure!", Toast.LENGTH_SHORT).show();
                if (layoutResults != null) layoutResults.setVisibility(View.GONE);
                return;
            }
            calculateWaterRequirements();
        }
    }

    private void displayPlants() {
        List<MyGardenActivity.GardenPlant> gardenPlants = loadGardenPlants();
        if (gardenPlants == null || gardenPlants.isEmpty()) {
            if (tvPlantList != null) tvPlantList.setText("Your garden is empty! Add plants first.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (MyGardenActivity.GardenPlant plant : gardenPlants) {
            if (plant != null && plant.getFlower() != null) {
                sb.append("• ").append(plant.getFlower().getName()).append("\n");
            }
        }
        if (tvPlantList != null) tvPlantList.setText(sb.toString());
    }

    private int getFrequencyFromJson(String plantName) {
        try {
            InputStream is = getAssets().open("watering_info.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            JSONObject obj = new JSONObject(json);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                JSONObject flowerObj = obj.getJSONObject(keys.next());
                if (flowerObj.getString("name").equalsIgnoreCase(plantName)) {
                    return flowerObj.getInt("weekly_frequency");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading frequency from JSON for " + plantName, e);
        }
        return 3; // Default fallback
    }

    private void calculateWaterRequirements() {
        try {
            List<MyGardenActivity.GardenPlant> gardenPlants = loadGardenPlants();
            if (gardenPlants == null || gardenPlants.isEmpty()) return;

            long diffInMillis = returnDate.getTimeInMillis() - departureDate.getTimeInMillis();
            long daysAway = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            if (daysAway <= 0) daysAway = 1;

            if (layoutResults != null) layoutResults.setVisibility(View.VISIBLE);
            if (tvSummary != null) tvSummary.setText("You will be away for " + daysAway + " days.");

            StringBuilder planBuilder = new StringBuilder();
            int totalWaterMl = 0;

            for (MyGardenActivity.GardenPlant plant : gardenPlants) {
                if (plant == null || plant.getFlower() == null) continue;
                
                String name = plant.getFlower().getName();
                if (name == null) name = "Unknown Plant";
                
                // Get frequency directly from watering_info.json as requested
                int weeklyFrequency = getFrequencyFromJson(name);

                // Calculate sessions: (Days Away / 7) * Sessions per week
                int timesToWater = (int) Math.ceil((daysAway * weeklyFrequency) / 7.0);
                if (timesToWater == 0) timesToWater = 1;
                
                PlantCareProfile profile = PlantCareDatabase.getInstance(this).getPlantProfile(name);
                int mlPerSession = getWaterAmount(profile, name);
                int plantTotalWater = timesToWater * mlPerSession;
                totalWaterMl += plantTotalWater;

                planBuilder.append("🌿 ").append(name)
                        .append("\n   • Water amount: ").append(mlPerSession).append(" ml")
                        .append("\n   • Weekly Frequency (from JSON): ").append(weeklyFrequency).append(" times/week")
                        .append("\n   • Total sessions: ").append(timesToWater)
                        .append("\n   • Total water: ").append(plantTotalWater).append(" ml")
                        .append("\n\n");
            }

            if (tvDetailedPlan != null) tvDetailedPlan.setText(planBuilder.toString());
            if (tvTotalWater != null) tvTotalWater.setText("Total Water Needed: " + totalWaterMl + " ml");
        } catch (Exception e) {
            Log.e(TAG, "Error calculating water", e);
        }
    }

    private int getWaterAmount(PlantCareProfile profile, String name) {
        String category = "";
        if (profile != null && profile.getCategory() != null) {
            category = profile.getCategory().toLowerCase();
        }
        
        String plantName = "";
        if (name != null) {
            plantName = name.toLowerCase();
        }

        if (category.contains("суккулент") || plantName.contains("cactus") || plantName.contains("aloe")) {
            return 35; 
        } else if (plantName.contains("mint") || plantName.contains("basil") || plantName.contains("лаванда") || plantName.contains("lavender")) {
            return 100; 
        } else if (category.contains("тропическое") || plantName.contains("orchid") || plantName.contains("rose") || plantName.contains("роза")) {
            return 175; 
        } else if (plantName.contains("ficus") || plantName.contains("фикус") || plantName.contains("large") || plantName.contains("дерево")) {
            return 500; 
        } else {
            return 55; 
        }
    }

    private void saveVacationPlan() {
        if (!departureSelected || !returnSelected) {
            Toast.makeText(this, "Please select dates first", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("VacationPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putLong("departure_date", departureDate.getTimeInMillis());
        editor.putLong("return_date", returnDate.getTimeInMillis());
        editor.putString("detailed_plan", tvDetailedPlan.getText().toString());
        editor.putString("summary", tvSummary.getText().toString());
        editor.putString("total_water", tvTotalWater.getText().toString());
        editor.putBoolean("is_active", true);
        
        editor.apply();

        Toast.makeText(this, "Vacation plan saved successfully!", Toast.LENGTH_SHORT).show();
    }

    private List<MyGardenActivity.GardenPlant> loadGardenPlants() {
        try {
            SharedPreferences sharedPreferences = getSharedPreferences("MyGardenPrefs", Context.MODE_PRIVATE);
            String json = sharedPreferences.getString("garden_plants", "");
            if (json == null || json.isEmpty()) {
                return new ArrayList<>();
            } else {
                Gson gson = new Gson();
                Type type = new TypeToken<List<MyGardenActivity.GardenPlant>>() {}.getType();
                List<MyGardenActivity.GardenPlant> list = gson.fromJson(json, type);
                return (list != null) ? list : new ArrayList<>();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading plants", e);
            return new ArrayList<>();
        }
    }
}
