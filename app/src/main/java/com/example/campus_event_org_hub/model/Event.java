package com.example.campus_event_org_hub.model;

import java.io.Serializable;

public class Event implements Serializable {
    private int id;
    private String title;
    private String description;
    private String date;
    private String time;
    private String startTime;
    private String endTime;
    private String tags;
    private String organizer;
    private String category;
    private String imagePath;
    private String status;
    private String creatorSid; // student_id of the officer who submitted this event
    private String venue;
    private String timeInCode;
    private String timeOutCode;

    // Full constructor (with id and time)
    public Event(int id, String title, String description, String date, String time,
                 String tags, String organizer, String category, String imagePath, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.startTime = "";
        this.endTime = "";
        this.tags = tags;
        this.organizer = organizer;
        this.category = category;
        this.imagePath = imagePath;
        this.status = status;
    }

    // Legacy constructor without id (for creation flows without time)
    public Event(String title, String description, String date, String tags, String organizer,
                 String category, String imagePath, String status) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = "";
        this.startTime = "";
        this.endTime = "";
        this.tags = tags;
        this.organizer = organizer;
        this.category = category;
        this.imagePath = imagePath;
        this.status = status;
    }

    // Creation constructor with time
    public Event(String title, String description, String date, String time, String tags,
                 String organizer, String category, String imagePath, String status) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.startTime = "";
        this.endTime = "";
        this.tags = tags;
        this.organizer = organizer;
        this.category = category;
        this.imagePath = imagePath;
        this.status = status;
    }

    // Legacy constructor with id, without time
    public Event(int id, String title, String description, String date, String tags,
                 String organizer, String category, String imagePath, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = "";
        this.startTime = "";
        this.endTime = "";
        this.tags = tags;
        this.organizer = organizer;
        this.category = category;
        this.imagePath = imagePath;
        this.status = status;
    }

    public int getId()             { return id; }
    public String getTitle()       { return title; }
    public String getDescription() { return description; }
    public String getDate()        { return date; }
    public String getTime()        { return time != null ? time : ""; }
    public String getStartTime()   { return startTime != null ? startTime : ""; }
    public void   setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime()     { return endTime != null ? endTime : ""; }
    public void   setEndTime(String endTime) { this.endTime = endTime; }
    public String getTags()        { return tags; }
    public String getOrganizer()   { return organizer; }
    public String getCategory()    { return category; }
    public String getImagePath()   { return imagePath; }
    public String getStatus()      { return status; }
    public void   setStatus(String status) { this.status = status; }
    public String getCreatorSid()  { return creatorSid != null ? creatorSid : ""; }
    public void   setCreatorSid(String creatorSid) { this.creatorSid = creatorSid; }
    public String getVenue()       { return venue != null ? venue : ""; }
    public void   setVenue(String venue) { this.venue = venue; }
    public String getTimeInCode()  { return timeInCode != null ? timeInCode : ""; }
    public void   setTimeInCode(String code) { this.timeInCode = code; }
    public String getTimeOutCode() { return timeOutCode != null ? timeOutCode : ""; }
    public void   setTimeOutCode(String code) { this.timeOutCode = code; }

    /** Convenience alias — some views call getEventTime() */
    public String getEventTime()   { return getTime(); }
}
