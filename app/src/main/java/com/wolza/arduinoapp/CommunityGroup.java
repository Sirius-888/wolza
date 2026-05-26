package com.wolza.arduinoapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CommunityGroup implements Serializable {
    private String id;
    private String name;
    private String code;
    private String createdBy;
    private long timestamp;
    private List<String> members; // User IDs of members

    public CommunityGroup() {
        this.members = new ArrayList<>();
    }

    public CommunityGroup(String name, String code, String createdBy, long timestamp) {
        this.name = name;
        this.code = code;
        this.createdBy = createdBy;
        this.timestamp = timestamp;
        this.members = new ArrayList<>();
        this.members.add(createdBy); // Creator is the first member
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }
}
