package com.example.campus_event_org_hub.model;

public class NotifModel {
    private int notifId;
    private String recipientSid;
    private int eventId;
    private String type;          // "POSTPONED" or "CANCELLED"
    private String message;
    private String reason;
    private String suggestedDate;
    private String instructions;
    private boolean isRead;
    private String createdAt;

    public NotifModel(int notifId, String recipientSid, int eventId,
                      String type, String message, String reason,
                      String suggestedDate, String instructions,
                      boolean isRead, String createdAt) {
        this.notifId = notifId;
        this.recipientSid = recipientSid;
        this.eventId = eventId;
        this.type = type;
        this.message = message;
        this.reason = reason;
        this.suggestedDate = suggestedDate != null ? suggestedDate : "";
        this.instructions = instructions != null ? instructions : "";
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public int getNotifId() { return notifId; }
    public String getRecipientSid() { return recipientSid; }
    public int getEventId() { return eventId; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getReason() { return reason; }
    public String getSuggestedDate() { return suggestedDate; }
    public String getInstructions() { return instructions; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public String getCreatedAt() { return createdAt; }
}
