package com.wolza.arduinoapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MoistureData {

    private int moisturePercent;
    private int rawValue;
    private long timestamp;
    private String timeString;

    public MoistureData(int moisturePercent, int rawValue) {
        this.moisturePercent = moisturePercent;
        this.rawValue = rawValue;
        this.timestamp = System.currentTimeMillis();
        this.timeString = formatTime(timestamp);
    }

    public MoistureData(int moisturePercent, int rawValue, long timestamp) {
        this.moisturePercent = moisturePercent;
        this.rawValue = rawValue;
        this.timestamp = timestamp;
        this.timeString = formatTime(timestamp);
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public int getMoisturePercent() {
        return moisturePercent;
    }

    public void setMoisturePercent(int moisturePercent) {
        this.moisturePercent = moisturePercent;
    }

    public int getRawValue() {
        return rawValue;
    }

    public void setRawValue(int rawValue) {
        this.rawValue = rawValue;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.timeString = formatTime(timestamp);
    }

    public String getTimeString() {
        return timeString;
    }

    // Get moisture status based on percentage
    public String getMoistureStatus() {
        if (moisturePercent < 30) {
            return "DRY - Water needed!";
        } else if (moisturePercent < 60) {
            return "Moderate";
        } else {
            return "Moist - Good";
        }
    }

    // Get color resource based on moisture level
    public int getStatusColor() {
        if (moisturePercent < 30) {
            return 0xFFFF4444; // Red
        } else if (moisturePercent < 60) {
            return 0xFFFFBB33; // Orange/Yellow
        } else {
            return 0xFF99CC00; // Green
        }
    }

    // Get emoji/icon based on moisture level
    public String getStatusEmoji() {
        if (moisturePercent < 30) {
            return "⚠️"; // Warning
        } else if (moisturePercent < 60) {
            return "🌱"; // Seedling
        } else {
            return "🌿"; // Plant
        }
    }

    @Override
    public String toString() {
        return "MoistureData{" +
                "moisturePercent=" + moisturePercent +
                "%, rawValue=" + rawValue +
                ", time='" + timeString + '\'' +
                '}';
    }
}