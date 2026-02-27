package com.example.campus_event_org_hub;

import java.io.Serializable;

public class Event implements Serializable {
    private String title;
    private String description;
    private String date;
    private String tags;
    private String organizer;
    private int imageResId;

    public Event(String title, String description, String date, String tags, String organizer, int imageResId) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.tags = tags;
        this.organizer = organizer;
        this.imageResId = imageResId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public String getTags() {
        return tags;
    }

    public String getOrganizer() {
        return organizer;
    }

    public int getImageResId() {
        return imageResId;
    }
}
