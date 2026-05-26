package com.wolza.arduinoapp;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlantCareDatabase {

    private static PlantCareDatabase instance;
    private Map<String, PlantCareProfile> plantProfiles;
    private Context context;
    private static final String PREFS_NAME = "PlantCarePrefs";
    private static final String KEY_PLANT_DATA = "plant_data";

    private PlantCareDatabase(Context context) {
        this.context = context;
        loadProfiles();
    }

    public static synchronized PlantCareDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new PlantCareDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private void loadProfiles() {
        // Встроенная база + загрузка сохраненных
        plantProfiles = new HashMap<>();

        // === БАЗОВЫЕ РАСТЕНИЯ ===

        // Роза
        PlantCareProfile rose = new PlantCareProfile();
        rose.setPlantName("Роза");
        rose.setScientificName("Rosa");
        rose.setCategory("цветущее");
        rose.setBaseWateringDays(3);
        rose.setMinMoisturePercent(30);
        rose.setMaxMoisturePercent(70);
        rose.setLightRequirement("прямое солнце");
        rose.setMinTemp(15);
        rose.setMaxTemp(28);
        rose.setNeedsMisting(true);
        rose.setDifficulty("средняя");
        rose.setDescription("Розы любят регулярный полив, но не переувлажнение. Лучше поливать утром или вечером.");
        plantProfiles.put("роза", rose);
        plantProfiles.put("rose", rose);

        // Кактус
        PlantCareProfile cactus = new PlantCareProfile();
        cactus.setPlantName("Кактус");
        cactus.setScientificName("Cactaceae");
        cactus.setCategory("суккулент");
        cactus.setBaseWateringDays(14);
        cactus.setMinMoisturePercent(10);
        cactus.setMaxMoisturePercent(40);
        cactus.setLightRequirement("яркий рассеянный свет");
        cactus.setMinTemp(10);
        cactus.setMaxTemp(35);
        cactus.setNeedsMisting(false);
        cactus.setDifficulty("легкая");
        cactus.setDescription("Кактусы накапливают воду, поливай редко, но обильно. Зимой полив сократить до 1 раза в месяц.");
        plantProfiles.put("кактус", cactus);
        plantProfiles.put("cactus", cactus);

        // Орхидея
        PlantCareProfile orchid = new PlantCareProfile();
        orchid.setPlantName("Орхидея");
        orchid.setScientificName("Orchidaceae");
        orchid.setCategory("тропическое");
        orchid.setBaseWateringDays(7);
        orchid.setMinMoisturePercent(40);
        orchid.setMaxMoisturePercent(80);
        orchid.setLightRequirement("яркий рассеянный свет");
        orchid.setMinTemp(18);
        orchid.setMaxTemp(28);
        orchid.setNeedsMisting(true);
        orchid.setDifficulty("сложная");
        orchid.setDescription("Орхидеи любят влажность, но не застой воды. Поливай, когда корни становятся серебристыми.");
        plantProfiles.put("орхидея", orchid);
        plantProfiles.put("orchid", orchid);

        // Фикус
        PlantCareProfile ficus = new PlantCareProfile();
        ficus.setPlantName("Фикус");
        ficus.setScientificName("Ficus");
        ficus.setCategory("декоративно-лиственное");
        ficus.setBaseWateringDays(5);
        ficus.setMinMoisturePercent(25);
        ficus.setMaxMoisturePercent(65);
        ficus.setLightRequirement("яркий рассеянный свет");
        ficus.setMinTemp(16);
        ficus.setMaxTemp(28);
        ficus.setNeedsMisting(true);
        ficus.setDifficulty("средняя");
        ficus.setDescription("Фикус не любит перестановок. Поливай, когда верхний слой почвы подсохнет.");
        plantProfiles.put("фикус", ficus);
        plantProfiles.put("ficus", ficus);

        // Суккулент (общий)
        PlantCareProfile succulent = new PlantCareProfile();
        succulent.setPlantName("Суккулент");
        succulent.setScientificName("Succulent");
        succulent.setCategory("суккулент");
        succulent.setBaseWateringDays(10);
        succulent.setMinMoisturePercent(10);
        succulent.setMaxMoisturePercent(50);
        succulent.setLightRequirement("яркий свет");
        succulent.setMinTemp(10);
        succulent.setMaxTemp(32);
        succulent.setNeedsMisting(false);
        succulent.setDifficulty("легкая");
        succulent.setDescription("Суккуленты запасают воду. Лучше недолить, чем перелить.");
        plantProfiles.put("суккулент", succulent);
        plantProfiles.put("succulent", succulent);

        // Алоэ
        PlantCareProfile aloe = new PlantCareProfile();
        aloe.setPlantName("Алоэ");
        aloe.setScientificName("Aloe vera");
        aloe.setCategory("суккулент");
        aloe.setBaseWateringDays(12);
        aloe.setMinMoisturePercent(10);
        aloe.setMaxMoisturePercent(45);
        aloe.setLightRequirement("яркий рассеянный свет");
        aloe.setMinTemp(12);
        aloe.setMaxTemp(30);
        aloe.setNeedsMisting(false);
        aloe.setDifficulty("легкая");
        aloe.setDescription("Алоэ — лекарственное растение. Зимой поливать 1 раз в месяц.");
        plantProfiles.put("алоэ", aloe);
        plantProfiles.put("aloe", aloe);

        // Лаванда
        PlantCareProfile lavender = new PlantCareProfile();
        lavender.setPlantName("Лаванда");
        lavender.setScientificName("Lavandula");
        lavender.setCategory("цветущее");
        lavender.setBaseWateringDays(6);
        lavender.setMinMoisturePercent(25);
        lavender.setMaxMoisturePercent(55);
        lavender.setLightRequirement("прямое солнце");
        lavender.setMinTemp(12);
        lavender.setMaxTemp(30);
        lavender.setNeedsMisting(false);
        lavender.setDifficulty("средняя");
        lavender.setDescription("Лаванда не любит застоя воды. Почва должна просыхать между поливами.");
        plantProfiles.put("лаванда", lavender);
        plantProfiles.put("lavender", lavender);

        // Загружаем сохраненные профили пользователя
        loadUserProfiles();
    }

    private void loadUserProfiles() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PLANT_DATA, "");
        if (!json.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, PlantCareProfile>>() {}.getType();
            Map<String, PlantCareProfile> userProfiles = gson.fromJson(json, type);
            if (userProfiles != null) {
                plantProfiles.putAll(userProfiles);
            }
        }
    }

    public void saveUserProfile(PlantCareProfile profile) {
        plantProfiles.put(profile.getPlantName().toLowerCase(), profile);
        saveAllUserProfiles();
    }

    private void saveAllUserProfiles() {
        // Сохраняем только пользовательские профили
        // Для простоты сохраняем все, но можно фильтровать
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = gson.toJson(plantProfiles);
        prefs.edit().putString(KEY_PLANT_DATA, json).apply();
    }

    public PlantCareProfile getPlantProfile(String plantName) {
        if (plantName == null) return null;
        String key = plantName.toLowerCase().trim();

        // Прямое совпадение
        if (plantProfiles.containsKey(key)) {
            return plantProfiles.get(key);
        }

        // Частичное совпадение
        for (Map.Entry<String, PlantCareProfile> entry : plantProfiles.entrySet()) {
            if (key.contains(entry.getKey()) || entry.getKey().contains(key)) {
                return entry.getValue();
            }
        }

        // Дефолтный профиль
        return getDefaultProfile();
    }

    public PlantCareProfile getDefaultProfile() {
        PlantCareProfile defaultProfile = new PlantCareProfile();
        defaultProfile.setPlantName("Неизвестное растение");
        defaultProfile.setCategory("обычное");
        defaultProfile.setBaseWateringDays(4);
        defaultProfile.setMinMoisturePercent(30);
        defaultProfile.setMaxMoisturePercent(70);
        defaultProfile.setLightRequirement("рассеянный свет");
        defaultProfile.setMinTemp(15);
        defaultProfile.setMaxTemp(28);
        defaultProfile.setDifficulty("средняя");
        defaultProfile.setDescription("Растение не определено. Рекомендуем поливать раз в 4-5 дней, когда верхний слой почвы подсохнет.");
        return defaultProfile;
    }

    public List<PlantCareProfile> getAllPlants() {
        return new ArrayList<>(plantProfiles.values());
    }

    public String getWateringAdvice(PlantCareProfile profile, int daysSinceLastWater, boolean summer) {
        int recommendedDays = profile.getBaseWateringDays();

        // Корректировка по сезону
        if (summer) {
            recommendedDays = Math.max(1, recommendedDays - 1);
        } else {
            recommendedDays = recommendedDays + 2;
        }

        int daysUntilWater = recommendedDays - daysSinceLastWater;

        if (daysUntilWater <= 0) {
            return "🚨 СРОЧНО ПОЛИТЬ! Растение требует воды прямо сейчас.";
        } else if (daysUntilWater == 1) {
            return "💧 Завтра пора поливать " + profile.getPlantName() + ".";
        } else {
            return "✅ " + profile.getPlantName() + " в порядке. Следующий полив через " + daysUntilWater + " дн.";
        }
    }

    public String getCareTips(PlantCareProfile profile) {
        StringBuilder tips = new StringBuilder();
        tips.append("🌿 ").append(profile.getPlantName()).append("\n\n");
        tips.append("💧 Полив: каждые ").append(profile.getBaseWateringDays()).append(" дн.\n");
        tips.append("☀️ Свет: ").append(profile.getLightRequirement()).append("\n");
        tips.append("🌡️ Температура: ").append(profile.getMinTemp()).append("-").append(profile.getMaxTemp()).append("°C\n");
        tips.append("📝 ").append(profile.getDescription());
        return tips.toString();
    }
}