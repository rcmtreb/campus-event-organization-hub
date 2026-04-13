package com.example.campus_event_org_hub.model;

/**
 * Represents a user row from the local SQLite users table.
 * Used by UserManagementActivity and related admin screens.
 */
public class User {
    public String name;
    public String id;             // student_id
    public String role;
    public String dept;
    public String profileImg;
    public int    yearLevel;
    public int    courseId;
    public String studentStatus;
    public String section;
    public String campus;
    public int    enrollmentYear;
    public String lastLogin;

    public User(String name, String id, String role, String dept, String profileImg) {
        this.name        = name        != null ? name        : "";
        this.id          = id          != null ? id          : "";
        this.role        = role        != null ? role        : "";
        this.dept        = dept        != null ? dept        : "";
        this.profileImg  = profileImg  != null ? profileImg  : "";
        this.yearLevel     = 1;
        this.courseId      = 0;
        this.studentStatus = "ACTIVE";
        this.section       = "";
        this.campus        = "S";
        this.enrollmentYear = 0;
        this.lastLogin     = "";
    }
}
