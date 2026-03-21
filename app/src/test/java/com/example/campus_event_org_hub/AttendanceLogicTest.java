package com.example.campus_event_org_hub;

import static org.junit.Assert.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import com.example.campus_event_org_hub.data.DatabaseHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class AttendanceLogicTest {

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private Context context;

    private static final String TEST_EVENT_ID = "1";
    private static final String TEST_STUDENT_ID = "STU001";
    private static final String TEST_STUDENT_ID_2 = "STU002";
    private static final String TEST_CODE_IN = "ABC123";
    private static final String TEST_CODE_OUT = "XYZ789";
    private static final String TEST_CODE_WRONG = "WRONG1";

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        setupTestData();
    }

    @After
    public void tearDown() throws Exception {
        if (db != null && db.isOpen()) {
            db.execSQL("DELETE FROM " + DatabaseHelper.TABLE_ATTENDANCE_CODES);
            db.execSQL("DELETE FROM " + DatabaseHelper.TABLE_ATTENDANCE);
            db.execSQL("DELETE FROM " + DatabaseHelper.TABLE_NOTIFICATIONS);
            db.close();
        }
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
    }

    private void setupTestData() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        String startTime = String.format(Locale.getDefault(), "%02d:00", cal.get(Calendar.HOUR_OF_DAY));
        cal.add(Calendar.HOUR_OF_DAY, 2);
        String endTime = String.format(Locale.getDefault(), "%02d:00", cal.get(Calendar.HOUR_OF_DAY));

        ContentValues event = new ContentValues();
        event.put(DatabaseHelper.COLUMN_ID, 1);
        event.put(DatabaseHelper.COLUMN_TITLE, "Test Event");
        event.put(DatabaseHelper.COLUMN_DATE, today);
        event.put(DatabaseHelper.COLUMN_START_TIME, startTime);
        event.put(DatabaseHelper.COLUMN_END_TIME, endTime);
        event.put(DatabaseHelper.COLUMN_STATUS, "APPROVED");
        event.put(DatabaseHelper.COLUMN_TIME_IN_CODE, TEST_CODE_IN);
        event.put(DatabaseHelper.COLUMN_TIME_OUT_CODE, TEST_CODE_OUT);
        db.insert(DatabaseHelper.TABLE_EVENTS, null, event);

        ContentValues student = new ContentValues();
        student.put(DatabaseHelper.COLUMN_USER_STUDENT_ID, TEST_STUDENT_ID);
        student.put(DatabaseHelper.COLUMN_USER_NAME, "Test Student");
        student.put(DatabaseHelper.COLUMN_USER_ROLE, "STUDENT");
        db.insert(DatabaseHelper.TABLE_USERS, null, student);

        ContentValues student2 = new ContentValues();
        student2.put(DatabaseHelper.COLUMN_USER_STUDENT_ID, TEST_STUDENT_ID_2);
        student2.put(DatabaseHelper.COLUMN_USER_NAME, "Test Student 2");
        student2.put(DatabaseHelper.COLUMN_USER_ROLE, "STUDENT");
        db.insert(DatabaseHelper.TABLE_USERS, null, student2);

        ContentValues registration = new ContentValues();
        registration.put(DatabaseHelper.COLUMN_REG_STUDENT_ID, TEST_STUDENT_ID);
        registration.put(DatabaseHelper.COLUMN_REG_EVENT_ID, 1);
        db.insert(DatabaseHelper.TABLE_REGISTRATIONS, null, registration);
    }

    private void insertPerStudentCode(int eventId, String studentId, String code, String type, String status) {
        ContentValues codeValues = new ContentValues();
        codeValues.put(DatabaseHelper.COLUMN_ATT_CODE_EVENT_ID, eventId);
        codeValues.put(DatabaseHelper.COLUMN_ATT_CODE_STUDENT_ID, studentId);
        codeValues.put(DatabaseHelper.COLUMN_ATT_CODE, code);
        codeValues.put(DatabaseHelper.COLUMN_ATT_CODE_TYPE, type);
        codeValues.put(DatabaseHelper.COLUMN_ATT_CODE_STATUS, status);
        codeValues.put(DatabaseHelper.COLUMN_ATT_CODE_CREATED_AT, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        db.insert(DatabaseHelper.TABLE_ATTENDANCE_CODES, null, codeValues);
    }

    private String getCodeStatus(String code) {
        Cursor c = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_ATT_CODE_STATUS + 
            " FROM " + DatabaseHelper.TABLE_ATTENDANCE_CODES + 
            " WHERE " + DatabaseHelper.COLUMN_ATT_CODE + "=?", new String[]{code});
        if (c != null && c.moveToFirst()) {
            String status = c.getString(0);
            c.close();
            return status;
        }
        if (c != null) c.close();
        return null;
    }

    private boolean hasTimeIn(int eventId, String studentId) {
        Cursor c = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_ATT_TIME_IN_AT + 
            " FROM " + DatabaseHelper.TABLE_ATTENDANCE + 
            " WHERE " + DatabaseHelper.COLUMN_ATT_EVENT_ID + "=? AND " + DatabaseHelper.COLUMN_ATT_STUDENT_ID + "=?",
            new String[]{String.valueOf(eventId), studentId});
        if (c != null && c.moveToFirst()) {
            String timeIn = c.getString(0);
            c.close();
            return timeIn != null && !timeIn.isEmpty();
        }
        if (c != null) c.close();
        return false;
    }

    private boolean hasTimeOut(int eventId, String studentId) {
        Cursor c = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_ATT_TIME_OUT_AT + 
            " FROM " + DatabaseHelper.TABLE_ATTENDANCE + 
            " WHERE " + DatabaseHelper.COLUMN_ATT_EVENT_ID + "=? AND " + DatabaseHelper.COLUMN_ATT_STUDENT_ID + "=?",
            new String[]{String.valueOf(eventId), studentId});
        if (c != null && c.moveToFirst()) {
            String timeOut = c.getString(0);
            c.close();
            return timeOut != null && !timeOut.isEmpty();
        }
        if (c != null) c.close();
        return false;
    }

    // =========================================================================
    // TEST: One-Time Redemption - Time-In
    // =========================================================================
    @Test
    public void testOneTimeRedemption_TimeIn_CodeCanOnlyBeUsedOnce() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "CODE001", "IN", "UNUSED");
        
        int firstResult = dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "CODE001");
        assertEquals("First submission should succeed", 0, firstResult);
        
        int secondResult = dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "CODE001");
        assertEquals("Second submission of same code should be rejected", 2, secondResult);
        
        assertEquals("Code status should be USED", "USED", getCodeStatus("CODE001"));
    }

    @Test
    public void testOneTimeRedemption_TimeOut_CodeCanOnlyBeUsedOnce() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "CODE001", "IN", "UNUSED");
        insertPerStudentCode(1, TEST_STUDENT_ID, "CODE002", "OUT", "UNUSED");
        
        dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "CODE001");
        
        int firstResult = dbHelper.submitTimeOut(1, TEST_STUDENT_ID, "CODE002");
        assertEquals("First time-out submission should succeed", 0, firstResult);
        
        int secondResult = dbHelper.submitTimeOut(1, TEST_STUDENT_ID, "CODE002");
        assertEquals("Second submission of same time-out code should be rejected", 3, secondResult);
        
        assertEquals("Code status should be USED", "USED", getCodeStatus("CODE002"));
    }

    // =========================================================================
    // TEST: Code Consumption Validation
    // =========================================================================
    @Test
    public void testConsumeAttendanceCode_Success() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "CONSUM01", "IN", "UNUSED");
        
        boolean result = dbHelper.consumeAttendanceCode(1, TEST_STUDENT_ID, "IN", "CONSUM01");
        assertTrue("consumeAttendanceCode should return true on success", result);
        assertEquals("Code status should be USED", "USED", getCodeStatus("CONSUM01"));
    }

    @Test
    public void testConsumeAttendanceCode_AlreadyUsed_Fails() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "CONSUM02", "IN", "UNUSED");
        
        dbHelper.consumeAttendanceCode(1, TEST_STUDENT_ID, "IN", "CONSUM02");
        
        boolean secondResult = dbHelper.consumeAttendanceCode(1, TEST_STUDENT_ID, "IN", "CONSUM02");
        assertFalse("Second consume should fail", secondResult);
    }

    @Test
    public void testConsumeAttendanceCode_WrongCode_Fails() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "CONSUM03", "IN", "UNUSED");
        
        boolean result = dbHelper.consumeAttendanceCode(1, TEST_STUDENT_ID, "IN", "WRONGCD");
        assertFalse("Consume with wrong code should fail", result);
        assertEquals("Code status should still be UNUSED", "UNUSED", getCodeStatus("CONSUM03"));
    }

    // =========================================================================
    // TEST: Wrong Code Rejection
    // =========================================================================
    @Test
    public void testSubmitTimeIn_WrongCode_Rejected() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "CORRECT1", "IN", "UNUSED");
        
        int result = dbHelper.submitTimeIn(1, TEST_STUDENT_ID, TEST_CODE_WRONG);
        assertEquals("Wrong code should be rejected", 1, result);
        assertFalse("Student should NOT have time-in recorded", hasTimeIn(1, TEST_STUDENT_ID));
    }

    @Test
    public void testSubmitTimeOut_WrongCode_Rejected() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "CORRECT2", "OUT", "UNUSED");
        insertPerStudentCode(1, TEST_STUDENT_ID, "INCODE01", "IN", "UNUSED");
        
        dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "INCODE01");
        
        int result = dbHelper.submitTimeOut(1, TEST_STUDENT_ID, TEST_CODE_WRONG);
        assertEquals("Wrong code should be rejected", 1, result);
        assertFalse("Student should NOT have time-out recorded", hasTimeOut(1, TEST_STUDENT_ID));
    }

    // =========================================================================
    // TEST: Already Timed In/Out Rejection
    // =========================================================================
    @Test
    public void testSubmitTimeIn_AlreadyTimedIn_Rejected() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "FIRSTCO", "IN", "UNUSED");
        insertPerStudentCode(1, TEST_STUDENT_ID, "SECOND1", "IN", "UNUSED");
        
        dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "FIRSTCO");
        
        int result = dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "SECOND1");
        assertEquals("Already timed in should be rejected", 2, result);
    }

    @Test
    public void testSubmitTimeOut_NotTimedIn_Rejected() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "OUTCODE1", "OUT", "UNUSED");
        
        int result = dbHelper.submitTimeOut(1, TEST_STUDENT_ID, "OUTCODE1");
        assertEquals("Not timed in should be rejected for time-out", 2, result);
    }

    @Test
    public void testSubmitTimeOut_AlreadyTimedOut_Rejected() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "INCODE2", "IN", "UNUSED");
        insertPerStudentCode(1, TEST_STUDENT_ID, "OUTCODE2", "OUT", "UNUSED");
        
        dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "INCODE2");
        dbHelper.submitTimeOut(1, TEST_STUDENT_ID, "OUTCODE2");
        
        int result = dbHelper.submitTimeOut(1, TEST_STUDENT_ID, "OUTCODE2");
        assertEquals("Already timed out should be rejected", 3, result);
    }

    // =========================================================================
    // TEST: Event Not Approved Rejection
    // =========================================================================
    @Test
    public void testRequestAttendanceCode_EventNotApproved_Rejected() {
        ContentValues event = new ContentValues();
        event.put(DatabaseHelper.COLUMN_ID, 2);
        event.put(DatabaseHelper.COLUMN_TITLE, "Pending Event");
        event.put(DatabaseHelper.COLUMN_STATUS, "PENDING");
        db.insert(DatabaseHelper.TABLE_EVENTS, null, event);
        
        long result = dbHelper.requestAttendanceCode(2, TEST_STUDENT_ID, "IN");
        assertEquals("Code request for non-approved event should fail", -1, result);
    }

    // =========================================================================
    // TEST: Code Expiration - Old Codes Expired on New Request
    // =========================================================================
    @Test
    public void testRequestAttendanceCode_OldCodesExpired() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "OLDTIME1", "IN", "UNUSED");
        
        String oldStatus = getCodeStatus("OLDTIME1");
        assertEquals("Initial status should be UNUSED", "UNUSED", oldStatus);
        
        dbHelper.requestAttendanceCode(1, TEST_STUDENT_ID, "IN");
        
        String newStatus = getCodeStatus("OLDTIME1");
        assertEquals("Old unused code should be EXPIRED after new request", "EXPIRED", newStatus);
    }

    @Test
    public void testRequestAttendanceCode_UsedCodesNotAffected() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "USEDCODE", "IN", "USED");
        
        dbHelper.requestAttendanceCode(1, TEST_STUDENT_ID, "IN");
        
        assertEquals("Used codes should remain USED", "USED", getCodeStatus("USEDCODE"));
    }

    // =========================================================================
    // TEST: Per-Student Code Priority Over Event-Level Code
    // =========================================================================
    @Test
    public void testSubmitTimeIn_PerStudentCodePreferred() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "PERSONAL1", "IN", "UNUSED");
        
        int result = dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "PERSONAL1");
        assertEquals("Per-student code should work", 0, result);
        
        assertEquals("Per-student code should be consumed", "USED", getCodeStatus("PERSONAL1"));
    }

    @Test
    public void testSubmitTimeIn_EventCodeFallback() {
        int result = dbHelper.submitTimeIn(1, TEST_STUDENT_ID, TEST_CODE_IN);
        assertEquals("Event-level code should work as fallback", 0, result);
        assertTrue("Student should have time-in", hasTimeIn(1, TEST_STUDENT_ID));
    }

    // =========================================================================
    // TEST: Invalid Input Handling
    // =========================================================================
    @Test
    public void testSubmitTimeIn_InvalidEventId_Rejected() {
        int result = dbHelper.submitTimeIn(0, TEST_STUDENT_ID, "ANYCODE");
        assertEquals("Invalid event ID should be rejected", 1, result);
    }

    @Test
    public void testSubmitTimeIn_NullStudentId_Rejected() {
        int result = dbHelper.submitTimeIn(1, null, "ANYCODE");
        assertEquals("Null student ID should be rejected", 1, result);
    }

    @Test
    public void testSubmitTimeIn_EmptyCode_Rejected() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "VALIDCOD", "IN", "UNUSED");
        int result = dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "");
        assertEquals("Empty code should be rejected", 1, result);
    }

    @Test
    public void testRequestAttendanceCode_InvalidType_Rejected() {
        long result = dbHelper.requestAttendanceCode(1, TEST_STUDENT_ID, "INVALID");
        assertEquals("Invalid type should be rejected", -1, result);
    }

    @Test
    public void testConsumeAttendanceCode_InvalidInput_Fails() {
        assertFalse("Invalid event ID should fail", 
            dbHelper.consumeAttendanceCode(0, TEST_STUDENT_ID, "IN", "CODE"));
        assertFalse("Null student ID should fail", 
            dbHelper.consumeAttendanceCode(1, null, "IN", "CODE"));
        assertFalse("Empty code should fail", 
            dbHelper.consumeAttendanceCode(1, TEST_STUDENT_ID, "IN", ""));
        assertFalse("Invalid type should fail", 
            dbHelper.consumeAttendanceCode(1, TEST_STUDENT_ID, "INVALID", "CODE"));
    }

    // =========================================================================
    // TEST: Multiple Students - Isolation
    // =========================================================================
    @Test
    public void testMultipleStudents_CodesAreIsolated() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "STU1CODE", "IN", "UNUSED");
        insertPerStudentCode(1, TEST_STUDENT_ID_2, "STU2CODE", "IN", "UNUSED");
        
        int result1 = dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "STU1CODE");
        assertEquals("Student 1 time-in should succeed", 0, result1);
        
        int result2 = dbHelper.submitTimeIn(1, TEST_STUDENT_ID_2, "STU2CODE");
        assertEquals("Student 2 time-in should succeed", 0, result2);
        
        assertTrue("Student 1 should have time-in", hasTimeIn(1, TEST_STUDENT_ID));
        assertTrue("Student 2 should have time-in", hasTimeIn(1, TEST_STUDENT_ID_2));
        
        assertEquals("Student 1's code should be USED", "USED", getCodeStatus("STU1CODE"));
        assertEquals("Student 2's code should be USED", "USED", getCodeStatus("STU2CODE"));
    }

    // =========================================================================
    // TEST: Race Condition Simulation (Same Code Concurrent Submission)
    // =========================================================================
    @Test
    public void testRaceCondition_SameCodeSubmittedTwice() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "RACETEST", "IN", "UNUSED");
        
        int firstResult = dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "RACETEST");
        int secondResult = dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "RACETEST");
        
        assertTrue("First submission should succeed", firstResult == 0 || firstResult == 2);
        assertTrue("Second submission should fail (already used)", secondResult == 2 || secondResult == 1);
        
        assertEquals("Code should only be used once", "USED", getCodeStatus("RACETEST"));
    }

    // =========================================================================
    // TEST: Code Generation Uniqueness
    // =========================================================================
    @Test
    public void testGenerateUniqueAttendanceCode_GeneratesUniqueCodes() throws Exception {
        Method method = DatabaseHelper.class.getDeclaredMethod("generateUniqueAttendanceCode");
        method.setAccessible(true);
        
        String code1 = (String) method.invoke(dbHelper);
        String code2 = (String) method.invoke(dbHelper);
        
        assertNotNull("Generated code should not be null", code1);
        assertNotNull("Generated code should not be null", code2);
        assertNotEquals("Codes should be unique", code1, code2);
        assertEquals("Code length should be 6", 6, code1.length());
        assertEquals("Code length should be 6", 6, code2.length());
    }

    // =========================================================================
    // TEST: Active Code Retrieval
    // =========================================================================
    @Test
    public void testGetActiveAttendanceCode_ReturnsUnusedCode() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "ACTIVEC1", "IN", "UNUSED");
        insertPerStudentCode(1, TEST_STUDENT_ID, "EXPIRED1", "IN", "EXPIRED");
        insertPerStudentCode(1, TEST_STUDENT_ID, "USEDCODE1", "IN", "USED");
        
        String activeCode = dbHelper.getActiveAttendanceCode(1, TEST_STUDENT_ID, "IN");
        assertEquals("Should return the UNUSED code", "ACTIVEC1", activeCode);
    }

    @Test
    public void testGetActiveAttendanceCode_NoUnusedCode_ReturnsEmpty() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "ONLYUSED", "IN", "USED");
        
        String activeCode = dbHelper.getActiveAttendanceCode(1, TEST_STUDENT_ID, "IN");
        assertEquals("Should return empty when no unused codes", "", activeCode);
    }

    // =========================================================================
    // TEST: Full Attendance Flow
    // =========================================================================
    @Test
    public void testFullAttendanceFlow_InThenOut() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "FULLFLOW1", "IN", "UNUSED");
        insertPerStudentCode(1, TEST_STUDENT_ID, "FULLFLOW2", "OUT", "UNUSED");
        
        int timeInResult = dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "FULLFLOW1");
        assertEquals("Time-in should succeed", 0, timeInResult);
        assertTrue("Student should have time-in", hasTimeIn(1, TEST_STUDENT_ID));
        assertEquals("Time-in code should be USED", "USED", getCodeStatus("FULLFLOW1"));
        
        int timeOutResult = dbHelper.submitTimeOut(1, TEST_STUDENT_ID, "FULLFLOW2");
        assertEquals("Time-out should succeed", 0, timeOutResult);
        assertTrue("Student should have time-out", hasTimeOut(1, TEST_STUDENT_ID));
        assertEquals("Time-out code should be USED", "USED", getCodeStatus("FULLFLOW2"));
    }

    // =========================================================================
    // TEST: Attendance Record Retrieval
    // =========================================================================
    @Test
    public void testGetAttendanceRecord_ReturnsCorrectTimestamps() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "RECORD01", "IN", "UNUSED");
        insertPerStudentCode(1, TEST_STUDENT_ID, "RECORD02", "OUT", "UNUSED");
        
        dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "RECORD01");
        dbHelper.submitTimeOut(1, TEST_STUDENT_ID, "RECORD02");
        
        String[] record = dbHelper.getAttendanceRecord(1, TEST_STUDENT_ID);
        assertNotNull("Record should not be null", record);
        assertEquals("Record should have 2 elements", 2, record.length);
        assertFalse("Time-in should be recorded", record[0].isEmpty());
        assertFalse("Time-out should be recorded", record[1].isEmpty());
    }

    @Test
    public void testGetAttendanceRecord_NoRecord_ReturnsNull() {
        String[] record = dbHelper.getAttendanceRecord(1, "NONEXISTENT");
        assertNull("No record should return null", record);
    }

    // =========================================================================
    // TEST: Rate Limiting
    // =========================================================================
    @Test
    public void testRateLimit_NoLimitInitially() {
        assertFalse("Should not be rate limited initially", 
            dbHelper.isRateLimited(1, TEST_STUDENT_ID, "IN"));
    }

    @Test
    public void testRateLimit_BlocksAfterMaxFailedAttempts() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "RATELIM1", "IN", "UNUSED");
        
        for (int i = 0; i < 5; i++) {
            dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "WRONGCODE" + i);
        }
        
        assertTrue("Should be rate limited after 5 failed attempts", 
            dbHelper.isRateLimited(1, TEST_STUDENT_ID, "IN"));
    }

    @Test
    public void testRateLimit_RateLimitedSubmissionBlocked() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "RATELIM2", "IN", "UNUSED");
        
        for (int i = 0; i < 5; i++) {
            dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "WRONGCODE" + i);
        }
        
        int result = dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "RATELIM2");
        assertEquals("Rate limited submission should return -4", -4, result);
    }

    @Test
    public void testRateLimit_SuccessfulSubmissionResetsCounter() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "RESETCNT1", "IN", "UNUSED");
        insertPerStudentCode(1, TEST_STUDENT_ID, "RESETCNT2", "IN", "UNUSED");
        
        dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "RESETCNT1");
        assertFalse("Should not be rate limited after success", 
            dbHelper.isRateLimited(1, TEST_STUDENT_ID, "IN"));
    }

    @Test
    public void testRateLimit_TimeOutIndependentOfTimeIn() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "INCODE3", "IN", "UNUSED");
        
        for (int i = 0; i < 5; i++) {
            dbHelper.submitTimeOut(1, TEST_STUDENT_ID, "WRONGCODE" + i);
        }
        
        assertTrue("Should be rate limited for time-out", 
            dbHelper.isRateLimited(1, TEST_STUDENT_ID, "OUT"));
        assertFalse("Should NOT be rate limited for time-in", 
            dbHelper.isRateLimited(1, TEST_STUDENT_ID, "IN"));
    }

    @Test
    public void testResetFailedAttempts_ClearsRateLimit() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "RESETALL", "IN", "UNUSED");
        
        for (int i = 0; i < 5; i++) {
            dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "WRONGCODE" + i);
        }
        
        assertTrue("Should be rate limited", 
            dbHelper.isRateLimited(1, TEST_STUDENT_ID, "IN"));
        
        dbHelper.resetFailedAttempts(1, TEST_STUDENT_ID, "IN");
        
        assertFalse("Should not be rate limited after reset", 
            dbHelper.isRateLimited(1, TEST_STUDENT_ID, "IN"));
    }

    // =========================================================================
    // TEST: Registration Validation
    // =========================================================================
    @Test
    public void testRequestAttendanceCode_UnregisteredStudent_Rejected() {
        long result = dbHelper.requestAttendanceCode(1, "UNREGISTERED_STUDENT", "IN");
        assertEquals("Unregistered student should be rejected", -6, result);
    }

    @Test
    public void testRequestAttendanceCode_RegisteredStudent_Succeeds() {
        long result = dbHelper.requestAttendanceCode(1, TEST_STUDENT_ID, "IN");
        assertTrue("Registered student should get code", result > 0);
    }

    // =========================================================================
    // TEST: Audit Logging
    // =========================================================================
    @Test
    public void testLogAttendanceAttempt_CreatesAuditRecord() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "AUDIT001", "IN", "UNUSED");
        
        dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "AUDIT001", "TestDevice", "127.0.0.1");
        
        SQLiteDatabase auditDb = dbHelper.getReadableDatabase();
        Cursor auditCursor = auditDb.rawQuery(
            "SELECT * FROM " + DatabaseHelper.TABLE_ATTENDANCE_AUDIT + 
            " WHERE " + DatabaseHelper.COLUMN_AUDIT_STUDENT_ID + "=?",
            new String[]{TEST_STUDENT_ID});
        
        assertTrue("Audit record should be created", auditCursor != null && auditCursor.moveToFirst());
        if (auditCursor != null) auditCursor.close();
    }

    @Test
    public void testAuditLog_FailedAttempt_Recorded() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "AUDIT002", "IN", "UNUSED");
        
        dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "WRONGCODE", "TestDevice", "127.0.0.1");
        
        SQLiteDatabase auditDb = dbHelper.getReadableDatabase();
        Cursor auditCursor = auditDb.rawQuery(
            "SELECT " + DatabaseHelper.COLUMN_AUDIT_RESULT_CODE + 
            " FROM " + DatabaseHelper.TABLE_ATTENDANCE_AUDIT + 
            " WHERE " + DatabaseHelper.COLUMN_AUDIT_STUDENT_ID + "=? AND " +
            DatabaseHelper.COLUMN_AUDIT_ACTION + "='SUBMIT'",
            new String[]{TEST_STUDENT_ID});
        
        assertTrue("Failed attempt should be in audit log", auditCursor != null && auditCursor.moveToFirst());
        int resultCode = auditCursor.getInt(0);
        assertEquals("Failed attempt should have result code 1", 1, resultCode);
        if (auditCursor != null) auditCursor.close();
    }

    // =========================================================================
    // TEST: Error Handling - Edge Cases
    // =========================================================================
    @Test
    public void testSubmitTimeIn_InvalidEventId_ReturnsNegative() {
        int result = dbHelper.submitTimeIn(0, TEST_STUDENT_ID, "ANYCODE", "", "");
        assertTrue("Invalid event ID should return negative", result < 0);
    }

    @Test
    public void testSubmitTimeOut_InvalidEventId_ReturnsNegative() {
        int result = dbHelper.submitTimeOut(0, TEST_STUDENT_ID, "ANYCODE", "", "");
        assertTrue("Invalid event ID should return negative", result < 0);
    }

    @Test
    public void testConsumeAttendanceCode_InvalidInput_ReturnsFalse() {
        assertFalse("Invalid event ID should return false", 
            dbHelper.consumeAttendanceCode(0, TEST_STUDENT_ID, "IN", "CODE"));
        assertFalse("Null student ID should return false", 
            dbHelper.consumeAttendanceCode(1, null, "IN", "CODE"));
        assertFalse("Empty code should return false", 
            dbHelper.consumeAttendanceCode(1, TEST_STUDENT_ID, "IN", ""));
        assertFalse("Invalid type should return false", 
            dbHelper.consumeAttendanceCode(1, TEST_STUDENT_ID, "INVALID", "CODE"));
    }

    @Test
    public void testRequestAttendanceCode_InvalidType_ReturnsNegative() {
        long result = dbHelper.requestAttendanceCode(1, TEST_STUDENT_ID, "INVALID");
        assertTrue("Invalid type should return -1", result < 0);
    }

    @Test
    public void testRequestAttendanceCode_InvalidEventId_ReturnsNegative() {
        long result = dbHelper.requestAttendanceCode(0, TEST_STUDENT_ID, "IN");
        assertTrue("Invalid event ID should return -1", result < 0);
    }

    @Test
    public void testRequestAttendanceCode_NullStudentId_ReturnsNegative() {
        long result = dbHelper.requestAttendanceCode(1, null, "IN");
        assertTrue("Null student ID should return -1", result < 0);
    }

    // =========================================================================
    // TEST: Transaction Atomicity - Code Consumption
    // =========================================================================
    @Test
    public void testConsumeAttendanceCode_AtomicTransaction() {
        insertPerStudentCode(1, TEST_STUDENT_ID, "ATOMIC01", "IN", "UNUSED");
        
        boolean firstConsume = dbHelper.consumeAttendanceCode(1, TEST_STUDENT_ID, "IN", "ATOMIC01");
        boolean secondConsume = dbHelper.consumeAttendanceCode(1, TEST_STUDENT_ID, "IN", "ATOMIC01");
        
        assertTrue("First consume should succeed", firstConsume);
        assertFalse("Second consume should fail", secondConsume);
        assertEquals("Code should be marked USED", "USED", getCodeStatus("ATOMIC01"));
    }

    // =========================================================================
    // TEST: Multiple Events - Isolation
    // =========================================================================
    @Test
    public void testMultipleEvents_RateLimitsAreIsolated() {
        ContentValues event2 = new ContentValues();
        event2.put(DatabaseHelper.COLUMN_ID, 2);
        event2.put(DatabaseHelper.COLUMN_TITLE, "Event 2");
        event2.put(DatabaseHelper.COLUMN_STATUS, "APPROVED");
        db.insert(DatabaseHelper.TABLE_EVENTS, null, event2);
        
        ContentValues reg2 = new ContentValues();
        reg2.put(DatabaseHelper.COLUMN_REG_STUDENT_ID, TEST_STUDENT_ID);
        reg2.put(DatabaseHelper.COLUMN_REG_EVENT_ID, 2);
        db.insert(DatabaseHelper.TABLE_REGISTRATIONS, null, reg2);
        
        for (int i = 0; i < 5; i++) {
            dbHelper.submitTimeIn(1, TEST_STUDENT_ID, "WRONGCODE" + i);
        }
        
        assertTrue("Should be rate limited for event 1", 
            dbHelper.isRateLimited(1, TEST_STUDENT_ID, "IN"));
        assertFalse("Should NOT be rate limited for event 2", 
            dbHelper.isRateLimited(2, TEST_STUDENT_ID, "IN"));
    }
}
