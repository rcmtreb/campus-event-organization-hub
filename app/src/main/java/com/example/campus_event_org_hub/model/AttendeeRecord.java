package com.example.campus_event_org_hub.model;

/**
 * Represents a single attendee row for the officer's per-event attendee list.
 * Built by JOINing registrations + users + attendance tables.
 */
public class AttendeeRecord {

    /** Attendance status values */
    public enum Status {
        TIMED_OUT,   // has both time_in and time_out
        TIMED_IN,    // has time_in but no time_out
        ABSENT,      // registered but no attendance row
    }

    // ── User fields ────────────────────────────────────────────────────────────
    private final String studentId;
    private final String name;
    private final String email;
    private final String department;
    private final String profilePhoto;   // Base64 data URI or ""

    // ── Attendance fields ──────────────────────────────────────────────────────
    private final Status status;
    private final String timeIn;         // "yyyy-MM-dd HH:mm:ss" or ""
    private final String timeOut;        // "yyyy-MM-dd HH:mm:ss" or ""
    private final String timeInPhoto;    // Base64 data URI or ""
    private final String timeOutPhoto;   // Base64 data URI or ""

    // ── Registration field ─────────────────────────────────────────────────────
    private final String registeredAt;   // "yyyy-MM-dd HH:mm:ss"

    public AttendeeRecord(String studentId, String name, String email, String department,
                          String profilePhoto, Status status,
                          String timeIn, String timeOut,
                          String timeInPhoto, String timeOutPhoto,
                          String registeredAt) {
        this.studentId    = studentId   != null ? studentId   : "";
        this.name         = name        != null ? name        : "";
        this.email        = email       != null ? email       : "";
        this.department   = department  != null ? department  : "";
        this.profilePhoto = profilePhoto != null ? profilePhoto : "";
        this.status       = status      != null ? status      : Status.ABSENT;
        this.timeIn       = timeIn      != null ? timeIn      : "";
        this.timeOut      = timeOut     != null ? timeOut     : "";
        this.timeInPhoto  = timeInPhoto  != null ? timeInPhoto  : "";
        this.timeOutPhoto = timeOutPhoto != null ? timeOutPhoto : "";
        this.registeredAt = registeredAt != null ? registeredAt : "";
    }

    public String getStudentId()    { return studentId; }
    public String getName()         { return name; }
    public String getEmail()        { return email; }
    public String getDepartment()   { return department; }
    public String getProfilePhoto() { return profilePhoto; }
    public Status getStatus()       { return status; }
    public String getTimeIn()       { return timeIn; }
    public String getTimeOut()      { return timeOut; }
    public String getTimeInPhoto()  { return timeInPhoto; }
    public String getTimeOutPhoto() { return timeOutPhoto; }
    public String getRegisteredAt() { return registeredAt; }
}
