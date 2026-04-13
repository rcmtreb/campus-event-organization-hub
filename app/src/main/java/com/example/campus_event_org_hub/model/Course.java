package com.example.campus_event_org_hub.model;

public class Course {
    private int    courseId;
    private String courseCode;
    private String courseName;
    private String department;
    private int    durationYears;

    public Course(int courseId, String courseCode, String courseName,
                  String department, int durationYears) {
        this.courseId      = courseId;
        this.courseCode    = courseCode   != null ? courseCode   : "";
        this.courseName    = courseName   != null ? courseName   : "";
        this.department    = department   != null ? department   : "";
        this.durationYears = durationYears > 0 ? durationYears : 4;
    }

    /** Convenience constructor for new (unsaved) courses — id is 0. */
    public Course(String courseCode, String courseName, String department, int durationYears) {
        this(0, courseCode, courseName, department, durationYears);
    }

    public int    getCourseId()      { return courseId; }
    public String getCourseCode()    { return courseCode; }
    public String getCourseName()    { return courseName; }
    public String getDepartment()    { return department; }
    public int    getDurationYears() { return durationYears; }

    public void setCourseId(int courseId)           { this.courseId      = courseId; }
    public void setCourseCode(String courseCode)     { this.courseCode    = courseCode; }
    public void setCourseName(String courseName)     { this.courseName    = courseName; }
    public void setDepartment(String department)     { this.department    = department; }
    public void setDurationYears(int durationYears)  { this.durationYears = durationYears; }

    @Override
    public String toString() { return courseCode + " — " + courseName; }
}
