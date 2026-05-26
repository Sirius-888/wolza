package com.wolza.arduinoapp;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class PlantPhotoAnalyzer {

    private static final String TAG = "PlantPhotoAnalyzer";

    // Состояния растения на основе анализа
    public static class PlantCondition {
        public boolean isHealthy;
        public boolean isDry;           // сухие листья
        public boolean isYellow;        // желтые листья
        public boolean isOverwatered;   // перелив
        public boolean needsMisting;    // нуждается в опрыскивании
        public int healthPercent;       // 0-100
        public String recommendation;
        public String details;

        public PlantCondition() {
            isHealthy = true;
            isDry = false;
            isYellow = false;
            isOverwatered = false;
            needsMisting = false;
            healthPercent = 80;
            recommendation = "Растение в хорошем состоянии";
            details = "";
        }
    }

    /**
     * Анализ состояния растения по фото (без API, базовый анализ цвета)
     * Для более точного анализа используй Plant.id API
     */
    public static PlantCondition analyzePlantPhoto(Bitmap bitmap) {
        PlantCondition condition = new PlantCondition();

        if (bitmap == null) {
            condition.recommendation = "Не удалось проанализировать фото";
            condition.healthPercent = 50;
            return condition;
        }

        try {
            // Уменьшаем изображение для анализа
            Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true);

            int[] pixels = new int[smallBitmap.getWidth() * smallBitmap.getHeight()];
            smallBitmap.getPixels(pixels, 0, smallBitmap.getWidth(), 0, 0,
                    smallBitmap.getWidth(), smallBitmap.getHeight());

            int greenCount = 0;
            int yellowCount = 0;
            int brownCount = 0;
            int totalPixels = pixels.length;

            for (int pixel : pixels) {
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                // Определяем здоровый зеленый цвет
                if (g > r && g > b && g > 100) {
                    greenCount++;
                }

                // Желтые оттенки (проблема)
                if (r > 150 && g > 150 && b < 100) {
                    yellowCount++;
                }

                // Коричневые/сухие участки
                if (r > 120 && g < 100 && b < 80) {
                    brownCount++;
                }
            }

            double greenPercent = (double) greenCount / totalPixels * 100;
            double yellowPercent = (double) yellowCount / totalPixels * 100;
            double brownPercent = (double) brownCount / totalPixels * 100;

            Log.d(TAG, "Анализ: зеленый=" + greenPercent + "%, желтый=" + yellowPercent + "%, коричневый=" + brownPercent + "%");

            // Оценка здоровья
            if (greenPercent > 40) {
                condition.isHealthy = true;
                condition.healthPercent = 80 + (int)(greenPercent / 2);
                condition.healthPercent = Math.min(condition.healthPercent, 100);
                condition.recommendation = "Растение выглядит здоровым! 🌿";
                condition.details = "Хороший зеленый цвет, нормальный рост.";
            } else if (yellowPercent > 15) {
                condition.isHealthy = false;
                condition.isYellow = true;
                condition.healthPercent = 50;
                condition.recommendation = "⚠️ Обнаружены желтые листья";
                condition.details = "Возможные причины: перелив, нехватка света или питательных веществ.";
            } else if (brownPercent > 10) {
                condition.isHealthy = false;
                condition.isDry = true;
                condition.healthPercent = 40;
                condition.recommendation = "🚨 Листья выглядят сухими!";
                condition.details = "Растению может не хватать влаги или воздух слишком сухой.";
            } else {
                condition.healthPercent = 60;
                condition.recommendation = "🌱 Растение нуждается во внимании";
                condition.details = "Проверь полив и освещение.";
            }

            // Проверка на потребность в опрыскивании
            if (brownPercent > 5 && greenPercent < 30) {
                condition.needsMisting = true;
                condition.details += " Рекомендуется опрыскивание.";
            }

            smallBitmap.recycle();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка анализа: " + e.getMessage());
            condition.recommendation = "Ошибка анализа";
            condition.healthPercent = 50;
        }

        return condition;
    }

    /**
     * Анализ через Plant.id API (более точный)
     * Этот метод вызывается после получения данных от API
     */
    public static PlantCondition parseHealthAssessment(JSONObject apiResponse) {
        PlantCondition condition = new PlantCondition();

        try {
            JSONObject result = apiResponse.getJSONObject("result");
            JSONObject disease = result.getJSONObject("disease");
            JSONArray suggestions = disease.getJSONArray("suggestions");

            if (suggestions.length() > 0) {
                JSONObject top = suggestions.getJSONObject(0);
                String diseaseName = top.getString("name");
                double probability = top.getDouble("probability") * 100;

                condition.isHealthy = false;
                condition.healthPercent = (int)(100 - probability);
                condition.recommendation = "🩺 Обнаружено: " + diseaseName;

                if (top.has("details")) {
                    JSONObject details = top.getJSONObject("details");
                    if (details.has("description")) {
                        condition.details = details.getString("description");
                    }
                    if (details.has("treatment")) {
                        JSONObject treatment = details.getJSONObject("treatment");
                        if (treatment.has("chemical")) {
                            condition.details += "\n\nЛечение: " + treatment.getString("chemical");
                        }
                    }
                }
            } else {
                condition.isHealthy = true;
                condition.healthPercent = 90;
                condition.recommendation = "🌿 Растение здорово!";
                condition.details = "Болезней не обнаружено.";
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка парсинга health assessment: " + e.getMessage());
            condition.recommendation = "Не удалось определить состояние";
        }

        return condition;
    }

    /**
     * Получить текстовую рекомендацию по уходу на основе состояния
     */
    public static String getCareRecommendation(PlantCondition condition, PlantCareProfile profile) {
        StringBuilder sb = new StringBuilder();

        if (condition.isHealthy) {
            sb.append("🌿 Растение в хорошем состоянии!\n\n");
        } else {
            sb.append("⚠️ Требуется внимание!\n\n");
        }

        if (condition.isDry) {
            sb.append("💧 Растение выглядит сухим. Полей его и увеличь влажность воздуха.\n\n");
        }

        if (condition.isYellow) {
            sb.append("🍂 Желтые листья: возможно, перелив или нехватка света. Дай почве просохнуть.\n\n");
        }

        if (condition.isOverwatered) {
            sb.append("💦 Признаки перелива. Уменьши полив и проверь дренаж.\n\n");
        }

        if (condition.needsMisting) {
            sb.append("💨 Рекомендуется опрыскивание листьев.\n\n");
        }

        if (profile != null) {
            sb.append("📋 Совет для ").append(profile.getPlantName()).append(":\n");
            sb.append("• Полив: каждые ").append(profile.getBaseWateringDays()).append(" дней\n");
            sb.append("• Свет: ").append(profile.getLightRequirement()).append("\n");
            sb.append("• Температура: ").append(profile.getMinTemp()).append("-")
                    .append(profile.getMaxTemp()).append("°C\n");
        }

        return sb.toString();
    }
}