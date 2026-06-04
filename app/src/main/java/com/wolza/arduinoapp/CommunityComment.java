package com.wolza.arduinoapp;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CommunityComment implements Serializable {
    private String id;
    private String postId;
    private String userId;
    private String userName;
    private String userAvatar;
    private String content;
    private long timestamp;
    private String replyToCommentId;   // null = top-level comment
    private String replyToUserName;    // name of the person being replied to

    public CommunityComment() {}

    public CommunityComment(String postId, String userId, String userName, String userAvatar,
                            String content, long timestamp) {
        this(postId, userId, userName, userAvatar, content, timestamp, null, null);
    }

    public CommunityComment(String postId, String userId, String userName, String userAvatar,
                            String content, long timestamp,
                            String replyToCommentId, String replyToUserName) {
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.content = content;
        this.timestamp = timestamp;
        this.replyToCommentId = replyToCommentId;
        this.replyToUserName = replyToUserName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getReplyToCommentId() { return replyToCommentId; }
    public void setReplyToCommentId(String id) { this.replyToCommentId = id; }
    public String getReplyToUserName() { return replyToUserName; }
    public void setReplyToUserName(String name) { this.replyToUserName = name; }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
