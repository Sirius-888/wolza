package com.wolza.arduinoapp;

public class PlantCareProfile {
    private String plantName;
    private String scientificName;
    private String category;          // "суккулент", "цветущее", "тропическое", "декоративно-лиственное"
    private int baseWateringDays;     // базовый интервал полива в днях
    private int minMoisturePercent;   // минимальная влажность почвы (%)
    private int maxMoisturePercent;   // максимальная влажность почвы (%)
    private String lightRequirement;  // "прямое солнце", "рассеянный свет", "тень"
    private int minTemp;              // минимальная температура
    private int maxTemp;              // максимальная температура
    private boolean needsMisting;     // нуждается ли в опрыскивании
    private String difficulty;        // "легкая", "средняя", "сложная"
    private String description;       // описание и советы
    private String imageUrl;          // URL изображения (опционально)

    public PlantCareProfile() {}

    // Getters
    public String getPlantName() { return plantName; }
    public String getScientificName() { return scientificName; }
    public String getCategory() { return category; }
    public int getBaseWateringDays() { return baseWateringDays; }
    public int getMinMoisturePercent() { return minMoisturePercent; }
    public int getMaxMoisturePercent() { return maxMoisturePercent; }
    public String getLightRequirement() { return lightRequirement; }
    public int getMinTemp() { return minTemp; }
    public int getMaxTemp() { return maxTemp; }
    public boolean isNeedsMisting() { return needsMisting; }
    public String getDifficulty() { return difficulty; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }

    // Setters
    public void setPlantName(String plantName) { this.plantName = plantName; }
    public void setScientificName(String scientificName) { this.scientificName = scientificName; }
    public void setCategory(String category) { this.category = category; }
    public void setBaseWateringDays(int baseWateringDays) { this.baseWateringDays = baseWateringDays; }
    public void setMinMoisturePercent(int minMoisturePercent) { this.minMoisturePercent = minMoisturePercent; }
    public void setMaxMoisturePercent(int maxMoisturePercent) { this.maxMoisturePercent = maxMoisturePercent; }
    public void setLightRequirement(String lightRequirement) { this.lightRequirement = lightRequirement; }
    public void setMinTemp(int minTemp) { this.minTemp = minTemp; }
    public void setMaxTemp(int maxTemp) { this.maxTemp = maxTemp; }
    public void setNeedsMisting(boolean needsMisting) { this.needsMisting = needsMisting; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // Получить текстовую рекомендацию по поливу
    public String getWateringRecommendation(int daysSinceLastWater, boolean isSummer) {
        int interval = baseWateringDays;
        if (isSummer) {
            interval = Math.max(1, interval - 1);
        } else {
            interval = interval + 2;
        }

        int daysLeft = interval - daysSinceLastWater;

        if (daysLeft <= 0) {
            return "🚨 ТРЕБУЕТСЯ ПОЛИВ! Почва пересохла.";
        } else if (daysLeft == 1) {
            return "💧 Завтра пора поливать " + plantName + ".";
        } else {
            return "✅ " + plantName + " в порядке. Полив через " + daysLeft + " дн.";
        }
    }

    @Override
    public String toString() {
        return plantName + " (" + category + ") — полив каждые " + baseWateringDays + " дн.";
    }
}