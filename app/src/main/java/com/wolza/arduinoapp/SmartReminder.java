package com.wolza.arduinoapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmartReminder {
    private String id;
    private String plantName;
    private String imageUrl;
    private long createdAt;
    private long nextWateringDate;
    private long lastWateredDate;
    private int recommendedWateringDays;
    private int wateredCount;
    private String recommendation;
    private PlantCareProfile careProfile;
    private PlantPhotoAnalyzer.PlantCondition plantCondition;
    private boolean isActive;

    public SmartReminder() {
        this.wateredCount = 0;
        this.recommendedWateringDays = 3;
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    public String getId() { return id; }
    public String getPlantName() { return plantName; }
    public String getImageUrl() { return imageUrl; }
    public long getCreatedAt() { return createdAt; }
    public long getNextWateringDate() { return nextWateringDate; }
    public long getLastWateredDate() { return lastWateredDate; }
    public int getRecommendedWateringDays() { return recommendedWateringDays; }
    public int getWateredCount() { return wateredCount; }
    public String getRecommendation() { return recommendation; }
    public PlantCareProfile getCareProfile() { return careProfile; }
    public PlantPhotoAnalyzer.PlantCondition getPlantCondition() { return plantCondition; }
    public boolean isActive() { return isActive; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setPlantName(String plantName) { this.plantName = plantName; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setNextWateringDate(long nextWateringDate) { this.nextWateringDate = nextWateringDate; }
    public void setLastWateredDate(long lastWateredDate) { this.lastWateredDate = lastWateredDate; }
    public void setRecommendedWateringDays(int recommendedWateringDays) { this.recommendedWateringDays = recommendedWateringDays; }
    public void setWateredCount(int wateredCount) { this.wateredCount = wateredCount; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public void setCareProfile(PlantCareProfile careProfile) { this.careProfile = careProfile; }
    public void setPlantCondition(PlantPhotoAnalyzer.PlantCondition plantCondition) { this.plantCondition = plantCondition; }
    public void setActive(boolean active) { isActive = active; }

    public String getFormattedNextWateringDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(nextWateringDate));
    }

    public String getStatusText() {
        if (nextWateringDate < System.currentTimeMillis()) return "Требует полива! 🚨";
        if (plantCondition == null) return "Отслеживается 🌱";
        if (plantCondition.isHealthy) return "Здорово 🌿";
        if (plantCondition.isDry) return "Сухой грунт 💧";
        return "Требует внимания ⚠️";
    }

    public int getStatusColor() {
        if (nextWateringDate < System.currentTimeMillis()) return 0xFFF44336;
        if (plantCondition == null) return 0xFFFF9800;
        if (plantCondition.isHealthy) return 0xFF4CAF50;
        return 0xFFFF9800;
    }
}