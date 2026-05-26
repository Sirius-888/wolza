package com.wolza.arduinoapp;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityPost implements Serializable {
    private String id;
    private String groupId;
    private String userId;
    private String userName;
    private String userAvatar;
    private String title;
    private String content;
    private String imageUrl;
    private long timestamp;
    private int commentCount;
    private List<String> upvotes; // List of user IDs who upvoted

    public CommunityPost() {
        this.upvotes = new ArrayList<>();
    }

    public CommunityPost(String groupId, String userId, String userName, String userAvatar, String title, String content, String imageUrl, long timestamp) {
        this.groupId = groupId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.commentCount = 0;
        this.upvotes = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public List<String> getUpvotes() { return upvotes; }
    public void setUpvotes(List<String> upvotes) { this.upvotes = upvotes; }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public int getUpvoteCount() {
        return upvotes != null ? upvotes.size() : 0;
    }
}
