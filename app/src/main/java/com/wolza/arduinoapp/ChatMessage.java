package com.wolza.arduinoapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatMessage {
    private String id;
    private String userId;
    private String userName;
    private String message;
    private String imageUrl;
    private String userAvatar;
    private long timestamp;

    public ChatMessage() {}

    public ChatMessage(String userId, String userName, String message, String imageUrl, String userAvatar, long timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.message = message;
        this.imageUrl = imageUrl;
        this.userAvatar = userAvatar;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getMessage() { return message; }
    public String getImageUrl() { return imageUrl; }
    public String getUserAvatar() { return userAvatar; }
    public long getTimestamp() { return timestamp; }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}