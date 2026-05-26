package com.wolza.arduinoapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class SmartWateringAssistant {

    private static final String TAG = "SmartWatering";
    private static final String PREFS_NAME = "SmartWateringPrefs";
    private static final String KEY_REMINDERS = "smart_reminders";

    private Context context;
    private PlantCareDatabase plantDatabase;
    private static SmartWateringAssistant instance;

    private SmartWateringAssistant(Context context) {
        this.context = context;
        this.plantDatabase = PlantCareDatabase.getInstance(context);
    }

    public static synchronized SmartWateringAssistant getInstance(Context context) {
        if (instance == null) {
            instance = new SmartWateringAssistant(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Creates a SmartReminder using results from the AI API.
     */
    public SmartReminder createReminderFromAI(JSONObject apiResponse, String userEnteredName) {
        SmartReminder reminder = new SmartReminder();
        reminder.setId(UUID.randomUUID().toString());
        reminder.setCreatedAt(System.currentTimeMillis());

        try {
            JSONObject result = apiResponse.getJSONObject("result");
            
            // 1. Identify plant name
            String plantName = userEnteredName;
            if (plantName == null || plantName.isEmpty()) {
                JSONObject classification = result.getJSONObject("classification");
                JSONArray suggestions = classification.getJSONArray("suggestions");
                if (suggestions.length() > 0) {
                    plantName = suggestions.getJSONObject(0).getString("name");
                } else {
                    plantName = "Unknown Plant";
                }
            }
            reminder.setPlantName(plantName);

            // 2. Analyze condition (Health)
            PlantPhotoAnalyzer.PlantCondition condition = PlantPhotoAnalyzer.parseHealthAssessment(apiResponse);
            reminder.setPlantCondition(condition);

            // 3. Get profile from database for care tips
            PlantCareProfile profile = plantDatabase.getPlantProfile(plantName);
            reminder.setCareProfile(profile);

            // 4. Calculate interval
            int wateringInterval = calculateWateringInterval(profile, condition);
            reminder.setRecommendedWateringDays(wateringInterval);

            // 5. Set next date
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, wateringInterval);
            reminder.setNextWateringDate(calendar.getTimeInMillis());

            // 6. Generate recommendation text
            reminder.setRecommendation(PlantPhotoAnalyzer.getCareRecommendation(condition, profile));

            // 7. Save locally
            saveReminder(reminder);
            return reminder;

        } catch (Exception e) {
            Log.e(TAG, "Failed to create reminder from AI", e);
            return null;
        }
    }

    private int calculateWateringInterval(PlantCareProfile profile, PlantPhotoAnalyzer.PlantCondition condition) {
        int baseDays = (profile != null) ? profile.getBaseWateringDays() : 4;
        
        if (condition.isDry) return Math.max(1, baseDays - 2);
        if (condition.isYellow || condition.isOverwatered) return baseDays + 2;
        
        return baseDays;
    }

    public void saveReminder(SmartReminder reminder) {
        List<SmartReminder> reminders = getAllReminders();
        boolean exists = false;
        for (int i = 0; i < reminders.size(); i++) {
            if (reminders.get(i).getId().equals(reminder.getId())) {
                reminders.set(i, reminder);
                exists = true;
                break;
            }
        }
        if (!exists) {
            reminders.add(reminder);
        }
        saveRemindersList(reminders);
    }

    public List<SmartReminder> getAllReminders() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_REMINDERS, "");
        if (json.isEmpty()) return new ArrayList<>();
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<List<SmartReminder>>() {}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveRemindersList(List<SmartReminder> reminders) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = new Gson().toJson(reminders);
        prefs.edit().putString(KEY_REMINDERS, json).apply();
    }

    public void deleteReminder(String id) {
        List<SmartReminder> reminders = getAllReminders();
        reminders.removeIf(r -> r.getId().equals(id));
        saveRemindersList(reminders);
    }

    public void markAsWatered(String id) {
        List<SmartReminder> reminders = getAllReminders();
        for (SmartReminder r : reminders) {
            if (r.getId().equals(id)) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, r.getRecommendedWateringDays());
                r.setNextWateringDate(cal.getTimeInMillis());
                r.setLastWateredDate(System.currentTimeMillis());
                r.setWateredCount(r.getWateredCount() + 1);
                break;
            }
        }
        saveRemindersList(reminders);
    }
}
