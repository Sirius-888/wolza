package com.wolza.arduinoapp;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiService {

    private static final String TAG = "ApiService";
    private static final String BASE_URL = "https://api.plant.id/v3";
    private static final String API_KEY = "m47Uv3pT7feZfTmLgO9sGl9eB8q81JshxDNtRoH10O3frCcvVB";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private static ApiService instance;

    private ApiService() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    /**
     * Identify plant AND check health using Plant.id v3 API
     */
    public void identifyPlant(String base64Image, ApiCallback callback) {
        try {
            String cleanBase64 = base64Image.replaceAll("\\s+", "");
            if (cleanBase64.contains(",")) {
                cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
            }

            JSONObject jsonRequest = new JSONObject();
            JSONArray imagesArray = new JSONArray();
            imagesArray.put(cleanBase64);

            jsonRequest.put("images", imagesArray);
            jsonRequest.put("health", "all"); // CRITICAL: Enables health/hints data
            jsonRequest.put("similar_images", true);

            HttpUrl url = HttpUrl.parse(BASE_URL + "/identification").newBuilder()
                    .addQueryParameter("details", "common_names,scientific_name,description,taxonomy,url,treatment")
                    .addQueryParameter("language", "en")
                    .build();

            makePostRequest(url, jsonRequest, callback);

        } catch (JSONException e) {
            callback.onError("Request creation failed");
        }
    }

    private void makePostRequest(HttpUrl url, JSONObject jsonRequest, ApiCallback callback) {
        RequestBody body = RequestBody.create(jsonRequest.toString(), JSON);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Api-Key", API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error. Please check connection.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                try {
                    if (response.isSuccessful()) {
                        callback.onSuccess(new JSONObject(responseBody));
                    } else {
                        callback.onError("API Error: " + response.code());
                    }
                } catch (JSONException e) {
                    callback.onError("Failed to read server response");
                }
            }
        });
    }

    public static String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
}
