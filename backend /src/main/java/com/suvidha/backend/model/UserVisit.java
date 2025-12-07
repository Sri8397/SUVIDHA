package com.suvidha.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "user_visits")
public class UserVisit {
    @Id
    private String id;
    
    private String userIdentifier;
    private String ipAddress;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private long visitCount;

    // Getters
    public String getId() { return id; }
    public String getUserIdentifier() { return userIdentifier; }
    public String getIpAddress() { return ipAddress; }
    public LocalDateTime getFirstSeen() { return firstSeen; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public long getVisitCount() { return visitCount; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUserIdentifier(String userIdentifier) { this.userIdentifier = userIdentifier; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setFirstSeen(LocalDateTime firstSeen) { this.firstSeen = firstSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    public void setVisitCount(long visitCount) { this.visitCount = visitCount; }
}
