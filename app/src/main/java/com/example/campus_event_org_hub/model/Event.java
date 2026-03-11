package com.example.campus_event_org_hub.model;

import java.io.Serializable;

public class Event implements Serializable {
    private int id; // Added ID
    private String title;
    private String description;
    private String date;
    private String tags;
    private String organizer;
    private String category;
    private String imagePath;
    private String status;

    public Event(int id, String title, String description, String date, String tags, String organizer, String category, String imagePath, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.tags = tags;
        this.organizer = organizer;
        this.category = category;
        this.imagePath = imagePath;
        this.status = status;
    }

    // Overloaded constructor for creation (without ID)
    public Event(String title, String description, String date, String tags, String organizer, String category, String imagePath, String status) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.tags = tags;
        this.organizer = organizer;
        this.category = category;
        this.imagePath = imagePath;
        this.status = status;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public String getTags() { return tags; }
    public String getOrganizer() { return organizer; }
    public String getCategory() { return category; }
    public String getImagePath() { return imagePath; }
    public String getStatus() { return status; }
}
