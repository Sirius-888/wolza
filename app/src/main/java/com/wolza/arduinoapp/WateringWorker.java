package com.wolza.arduinoapp;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WateringWorker extends Worker {

    public WateringWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("MyGardenPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("garden_plants", "");
        
        if (json.isEmpty()) return Result.success();

        List<MyGardenActivity.GardenPlant> plants;
        try {
            plants = new Gson().fromJson(json, new TypeToken<List<MyGardenActivity.GardenPlant>>(){}.getType());
        } catch (Exception e) {
            return Result.failure();
        }

        NotificationHelper helper = new NotificationHelper(context);
        Calendar cal = Calendar.getInstance();
        int dayIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7; // Mon=0 ... Sun=6

        for (MyGardenActivity.GardenPlant plant : plants) {
            if (plant.getWeeklyPlan() != null && plant.getWeeklyPlan()[dayIndex]) {
                if (!plant.isWateredToday()) {
                    helper.sendWateringReminder(plant.getFlower().getName());
                }
            }
        }

        return Result.success();
    }
}
