package com.example.campus_event_org_hub.model;

public class AcademicSettings {
    private String academicYearEnd;        // "MM-dd", e.g. "04-30"
    private String lastPromotionDate;      // ISO date, e.g. "2026-04-30", or ""
    private int    inactivityThresholdYears;

    public AcademicSettings(String academicYearEnd, String lastPromotionDate,
                             int inactivityThresholdYears) {
        this.academicYearEnd           = academicYearEnd           != null ? academicYearEnd           : "04-30";
        this.lastPromotionDate         = lastPromotionDate         != null ? lastPromotionDate         : "";
        this.inactivityThresholdYears  = inactivityThresholdYears  > 0     ? inactivityThresholdYears  : 2;
    }

    public String getAcademicYearEnd()           { return academicYearEnd; }
    public String getLastPromotionDate()          { return lastPromotionDate; }
    public int    getInactivityThresholdYears()   { return inactivityThresholdYears; }

    public void setAcademicYearEnd(String academicYearEnd)               { this.academicYearEnd           = academicYearEnd; }
    public void setLastPromotionDate(String lastPromotionDate)           { this.lastPromotionDate         = lastPromotionDate; }
    public void setInactivityThresholdYears(int inactivityThresholdYears){ this.inactivityThresholdYears  = inactivityThresholdYears; }
}
