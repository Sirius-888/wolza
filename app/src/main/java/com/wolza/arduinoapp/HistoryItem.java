package com.wolza.arduinoapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryItem {
    private String type;
    private String name;
    private String description;
    private String imageUrl;
    private String timestamp;
    private long timestampLong;

    public HistoryItem() {}

    public HistoryItem(String type, String name, String description, String imageUrl, long timestampLong) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.timestampLong = timestampLong;
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
        this.timestamp = sdf.format(new Date(timestampLong));
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getTimestamp() { return timestamp; }
    public long getTimestampLong() { return timestampLong; }

    public void setType(String type) { this.type = type; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setTimestampLong(long timestampLong) { this.timestampLong = timestampLong; }
}