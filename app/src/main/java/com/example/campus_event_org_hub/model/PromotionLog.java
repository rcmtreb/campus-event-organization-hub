package com.example.campus_event_org_hub.model;

public class PromotionLog {
    private final int    logId;
    private final String studentId;
    private final int    oldYearLevel;
    private final int    newYearLevel;
    private final String promotedAt;
    private final String promotionType;  // "AUTO" or "MANUAL"

    public PromotionLog(int logId, String studentId, int oldYearLevel, int newYearLevel,
                        String promotedAt, String promotionType) {
        this.logId         = logId;
        this.studentId     = studentId     != null ? studentId     : "";
        this.oldYearLevel  = oldYearLevel;
        this.newYearLevel  = newYearLevel;
        this.promotedAt    = promotedAt    != null ? promotedAt    : "";
        this.promotionType = promotionType != null ? promotionType : "AUTO";
    }

    public int    getLogId()         { return logId; }
    public String getStudentId()     { return studentId; }
    public int    getOldYearLevel()  { return oldYearLevel; }
    public int    getNewYearLevel()  { return newYearLevel; }
    public String getPromotedAt()    { return promotedAt; }
    public String getPromotionType() { return promotionType; }
}
