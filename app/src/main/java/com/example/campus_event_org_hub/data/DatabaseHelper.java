package com.example.campus_event_org_hub.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.model.NotifModel;
import com.example.campus_event_org_hub.service.FcmSender;
import com.example.campus_event_org_hub.util.ServerTimeUtil;

import org.mindrot.jbcrypt.BCrypt;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ceoh.db";
    private static final int DATABASE_VERSION = 15; // v15: added email_verified, verification_token, login_attempts, locked_until columns

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static volatile DatabaseHelper instance;

    /**
     * Returns the application-wide singleton instance.
     * Always pass {@code context.getApplicationContext()} (or any Context — it
     * is converted to the application context internally to avoid leaks).
     */
    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return instance;
    }
    // ──────────────────────────────────────────────────────────────────────────

    private final Context mContext;

    // Table: Events
    public static final String TABLE_EVENTS       = "events";
    public static final String COLUMN_ID          = "id";
    public static final String COLUMN_TITLE       = "title";
    public static final String COLUMN_DESC        = "description";
    public static final String COLUMN_DATE        = "date";
    public static final String COLUMN_EVENT_TIME  = "event_time";
    public static final String COLUMN_START_TIME  = "start_time";
    public static final String COLUMN_END_TIME    = "end_time";
    public static final String COLUMN_TAGS        = "tags";
    public static final String COLUMN_ORGANIZER   = "organizer";
    public static final String COLUMN_CATEGORY    = "category";
    public static final String COLUMN_IMAGE_PATH  = "image_path";
    public static final String COLUMN_STATUS      = "status";
    public static final String COLUMN_CREATOR_SID   = "creator_sid";
    public static final String COLUMN_IS_HIDDEN     = "is_hidden";
    public static final String COLUMN_VENUE         = "venue";
    public static final String COLUMN_TIME_IN_CODE  = "time_in_code";
    public static final String COLUMN_TIME_OUT_CODE = "time_out_code";

    // Table: Attendance
    public static final String TABLE_ATTENDANCE          = "attendance";
    public static final String COLUMN_ATT_ID             = "att_id";
    public static final String COLUMN_ATT_EVENT_ID       = "att_event_id";
    public static final String COLUMN_ATT_STUDENT_ID     = "att_student_id";
    public static final String COLUMN_ATT_TIME_IN_AT     = "time_in_at";
    public static final String COLUMN_ATT_TIME_OUT_AT    = "time_out_at";
    public static final String COLUMN_ATT_TIME_IN_PHOTO  = "time_in_photo";
    public static final String COLUMN_ATT_TIME_OUT_PHOTO = "time_out_photo";
    // Window columns — record the open/close times of the attendance window at submission time
    public static final String COLUMN_ATT_TIME_IN_WINDOW_OPEN  = "time_in_window_open";
    public static final String COLUMN_ATT_TIME_IN_WINDOW_CLOSE = "time_in_window_close";
    public static final String COLUMN_ATT_TIME_OUT_WINDOW_OPEN  = "time_out_window_open";
    public static final String COLUMN_ATT_TIME_OUT_WINDOW_CLOSE = "time_out_window_close";

    // Table: Attendance Codes (per student)
    public static final String TABLE_ATTENDANCE_CODES        = "attendance_codes";
    public static final String COLUMN_ATT_CODE_ID           = "att_code_id";
    public static final String COLUMN_ATT_CODE_EVENT_ID     = "att_code_event_id";
    public static final String COLUMN_ATT_CODE_STUDENT_ID   = "att_code_student_id";
    public static final String COLUMN_ATT_CODE             = "att_code";
    public static final String COLUMN_ATT_CODE_TYPE        = "att_code_type";
    public static final String COLUMN_ATT_CODE_STATUS      = "att_code_status";
    public static final String COLUMN_ATT_CODE_CREATED_AT   = "att_code_created_at";
    public static final String COLUMN_ATT_CODE_USED_AT      = "att_code_used_at";

    // Table: Users
    public static final String TABLE_USERS             = "users";
    public static final String COLUMN_USER_ID_PK       = "user_pk";
    public static final String COLUMN_USER_NAME        = "name";
    public static final String COLUMN_USER_STUDENT_ID  = "student_id";
    public static final String COLUMN_USER_EMAIL       = "email";
    public static final String COLUMN_USER_PASSWORD    = "password";
    public static final String COLUMN_USER_ROLE        = "role";
    public static final String COLUMN_USER_DEPARTMENT  = "department";
    public static final String COLUMN_USER_GENDER      = "gender";
    public static final String COLUMN_USER_MOBILE      = "mobile";
    public static final String COLUMN_USER_PROFILE_IMG = "profile_image";
    public static final String COLUMN_USER_NOTIF_PREF  = "notif_pref";
    public static final String COLUMN_USER_EMAIL_VERIFIED = "email_verified";
    public static final String COLUMN_USER_VERIFICATION_TOKEN = "verification_token";
    public static final String COLUMN_USER_LOGIN_ATTEMPTS = "login_attempts";
    public static final String COLUMN_USER_LOCKED_UNTIL = "locked_until";
    public static final String COLUMN_USER_FIREBASE_UID = "firebase_uid";

    // Table: Login Rate Limiting
    public static final String TABLE_LOGIN_RATE_LIMIT = "login_rate_limit";
    public static final String COLUMN_LR_ID = "lr_id";
    public static final String COLUMN_LR_STUDENT_ID = "student_id";
    public static final String COLUMN_LR_ATTEMPTS = "attempts";
    public static final String COLUMN_LR_LOCKED_UNTIL = "locked_until";

    // Table: Registrations
    public static final String TABLE_REGISTRATIONS   = "registrations";
    public static final String COLUMN_REG_ID         = "reg_id";
    public static final String COLUMN_REG_STUDENT_ID = "reg_student_id";
    public static final String COLUMN_REG_EVENT_ID   = "reg_event_id";
    public static final String COLUMN_REG_TIMESTAMP  = "reg_timestamp";

    // Table: Notifications
    public static final String TABLE_NOTIFICATIONS         = "notifications";
    public static final String COLUMN_NOTIF_ID             = "notif_id";
    public static final String COLUMN_NOTIF_RECIPIENT_SID  = "recipient_sid";
    public static final String COLUMN_NOTIF_EVENT_ID       = "event_id";
    public static final String COLUMN_NOTIF_TYPE           = "type";
    public static final String COLUMN_NOTIF_MESSAGE        = "message";
    public static final String COLUMN_NOTIF_REASON         = "reason";
    public static final String COLUMN_NOTIF_SUGGESTED_DATE = "suggested_date";
    public static final String COLUMN_NOTIF_SUGGESTED_TIME = "suggested_time";
    public static final String COLUMN_NOTIF_INSTRUCTIONS   = "instructions";
    public static final String COLUMN_NOTIF_IS_READ        = "is_read";
    public static final String COLUMN_NOTIF_CREATED_AT     = "created_at";
    public static final String COLUMN_NOTIF_IS_ARCHIVED    = "is_archived";
    public static final String COLUMN_NOTIF_ARCHIVED_AT    = "archived_at";

    // Table: Attendance Rate Limiting
    public static final String TABLE_ATTENDANCE_RATE_LIMIT = "attendance_rate_limit";
    public static final String COLUMN_RL_STUDENT_ID        = "student_id";
    public static final String COLUMN_RL_EVENT_ID         = "event_id";
    public static final String COLUMN_RL_TYPE            = "type";
    public static final String COLUMN_RL_ATTEMPTS         = "attempts";
    public static final String COLUMN_RL_LOCKED_UNTIL     = "locked_until";

    // Table: Attendance Audit Log
    public static final String TABLE_ATTENDANCE_AUDIT     = "attendance_audit";
    public static final String COLUMN_AUDIT_ID            = "audit_id";
    public static final String COLUMN_AUDIT_EVENT_ID      = "event_id";
    public static final String COLUMN_AUDIT_STUDENT_ID   = "student_id";
    public static final String COLUMN_AUDIT_TYPE         = "type";
    public static final String COLUMN_AUDIT_ACTION       = "action";
    public static final String COLUMN_AUDIT_RESULT_CODE   = "result_code";
    public static final String COLUMN_AUDIT_TIMESTAMP    = "timestamp";
    public static final String COLUMN_AUDIT_DEVICE_INFO  = "device_info";
    public static final String COLUMN_AUDIT_IP_ADDRESS   = "ip_address";

    // ── CREATE statements ────────────────────────────────────────────────────

    private static final String CREATE_TABLE_EVENTS =
            "CREATE TABLE " + TABLE_EVENTS + " (" +
            COLUMN_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TITLE       + " TEXT, " +
            COLUMN_DESC        + " TEXT, " +
            COLUMN_DATE        + " TEXT, " +
            COLUMN_EVENT_TIME  + " TEXT DEFAULT '', " +
            COLUMN_START_TIME  + " TEXT DEFAULT '', " +
            COLUMN_END_TIME    + " TEXT DEFAULT '', " +
            COLUMN_TAGS        + " TEXT, " +
            COLUMN_ORGANIZER   + " TEXT, " +
            COLUMN_CATEGORY    + " TEXT, " +
            COLUMN_IMAGE_PATH  + " TEXT, " +
            COLUMN_STATUS      + " TEXT DEFAULT 'PENDING', " +
            COLUMN_CREATOR_SID + " TEXT DEFAULT '', " +
            COLUMN_IS_HIDDEN   + " INTEGER DEFAULT 0, " +
            COLUMN_VENUE       + " TEXT DEFAULT '', " +
            COLUMN_TIME_IN_CODE  + " TEXT DEFAULT '', " +
            COLUMN_TIME_OUT_CODE + " TEXT DEFAULT '')";

    private static final String CREATE_TABLE_ATTENDANCE =
            "CREATE TABLE " + TABLE_ATTENDANCE + " (" +
            COLUMN_ATT_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_ATT_EVENT_ID   + " INTEGER, " +
            COLUMN_ATT_STUDENT_ID + " TEXT, " +
            COLUMN_ATT_TIME_IN_AT  + " TEXT, " +
            COLUMN_ATT_TIME_OUT_AT + " TEXT DEFAULT '', " +
            COLUMN_ATT_TIME_IN_PHOTO  + " TEXT DEFAULT '', " +
            COLUMN_ATT_TIME_OUT_PHOTO + " TEXT DEFAULT '', " +
            COLUMN_ATT_TIME_IN_WINDOW_OPEN   + " TEXT DEFAULT '', " +
            COLUMN_ATT_TIME_IN_WINDOW_CLOSE  + " TEXT DEFAULT '', " +
            COLUMN_ATT_TIME_OUT_WINDOW_OPEN  + " TEXT DEFAULT '', " +
            COLUMN_ATT_TIME_OUT_WINDOW_CLOSE + " TEXT DEFAULT '', " +
            "UNIQUE(" + COLUMN_ATT_EVENT_ID + ", " + COLUMN_ATT_STUDENT_ID + "))";

    private static final String CREATE_TABLE_ATTENDANCE_CODES =
            "CREATE TABLE " + TABLE_ATTENDANCE_CODES + " (" +
            COLUMN_ATT_CODE_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_ATT_CODE_EVENT_ID   + " INTEGER, " +
            COLUMN_ATT_CODE_STUDENT_ID + " TEXT, " +
            COLUMN_ATT_CODE           + " TEXT, " +
            COLUMN_ATT_CODE_TYPE      + " TEXT, " +
            COLUMN_ATT_CODE_STATUS    + " TEXT, " +
            COLUMN_ATT_CODE_CREATED_AT+ " TEXT, " +
            COLUMN_ATT_CODE_USED_AT   + " TEXT DEFAULT '', " +
            "UNIQUE(" + COLUMN_ATT_CODE + "))";

    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
            COLUMN_USER_ID_PK       + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USER_NAME        + " TEXT, " +
            COLUMN_USER_STUDENT_ID  + " TEXT UNIQUE, " +
            COLUMN_USER_EMAIL       + " TEXT UNIQUE, " +
            COLUMN_USER_PASSWORD    + " TEXT, " +
            COLUMN_USER_ROLE        + " TEXT, " +
            COLUMN_USER_DEPARTMENT  + " TEXT, " +
            COLUMN_USER_GENDER      + " TEXT DEFAULT '', " +
            COLUMN_USER_MOBILE      + " TEXT DEFAULT '', " +
            COLUMN_USER_PROFILE_IMG + " TEXT DEFAULT '', " +
            COLUMN_USER_NOTIF_PREF  + " TEXT DEFAULT 'All Events', " +
            COLUMN_USER_EMAIL_VERIFIED + " INTEGER DEFAULT 1, " +
            COLUMN_USER_VERIFICATION_TOKEN + " TEXT DEFAULT '', " +
            COLUMN_USER_LOGIN_ATTEMPTS + " INTEGER DEFAULT 0, " +
            COLUMN_USER_LOCKED_UNTIL + " TEXT DEFAULT '', " +
            COLUMN_USER_FIREBASE_UID + " TEXT DEFAULT '')";

    private static final String CREATE_TABLE_REGISTRATIONS =
            "CREATE TABLE " + TABLE_REGISTRATIONS + " (" +
            COLUMN_REG_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_REG_STUDENT_ID + " TEXT, " +
            COLUMN_REG_EVENT_ID   + " INTEGER, " +
            COLUMN_REG_TIMESTAMP  + " TEXT, " +
            "UNIQUE(" + COLUMN_REG_STUDENT_ID + ", " + COLUMN_REG_EVENT_ID + "))";

    private static final String CREATE_TABLE_NOTIFICATIONS =
            "CREATE TABLE " + TABLE_NOTIFICATIONS + " (" +
            COLUMN_NOTIF_ID             + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_NOTIF_RECIPIENT_SID  + " TEXT, " +
            COLUMN_NOTIF_EVENT_ID       + " INTEGER, " +
            COLUMN_NOTIF_TYPE           + " TEXT, " +
            COLUMN_NOTIF_MESSAGE        + " TEXT, " +
            COLUMN_NOTIF_REASON         + " TEXT, " +
            COLUMN_NOTIF_SUGGESTED_DATE + " TEXT DEFAULT '', " +
            COLUMN_NOTIF_SUGGESTED_TIME + " TEXT DEFAULT '', " +
            COLUMN_NOTIF_INSTRUCTIONS   + " TEXT DEFAULT '', " +
            COLUMN_NOTIF_IS_READ        + " INTEGER DEFAULT 0, " +
            COLUMN_NOTIF_CREATED_AT     + " TEXT, " +
            COLUMN_NOTIF_IS_ARCHIVED    + " INTEGER DEFAULT 0, " +
            COLUMN_NOTIF_ARCHIVED_AT    + " TEXT DEFAULT '')";

    private static final String CREATE_TABLE_ATTENDANCE_RATE_LIMIT =
            "CREATE TABLE " + TABLE_ATTENDANCE_RATE_LIMIT + " (" +
            COLUMN_RL_STUDENT_ID   + " TEXT, " +
            COLUMN_RL_EVENT_ID     + " INTEGER, " +
            COLUMN_RL_TYPE         + " TEXT, " +
            COLUMN_RL_ATTEMPTS     + " INTEGER DEFAULT 0, " +
            COLUMN_RL_LOCKED_UNTIL + " TEXT, " +
            "PRIMARY KEY (" + COLUMN_RL_STUDENT_ID + ", " + COLUMN_RL_EVENT_ID + ", " + COLUMN_RL_TYPE + "))";

    private static final String CREATE_TABLE_ATTENDANCE_AUDIT =
            "CREATE TABLE " + TABLE_ATTENDANCE_AUDIT + " (" +
            COLUMN_AUDIT_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_AUDIT_EVENT_ID    + " INTEGER, " +
            COLUMN_AUDIT_STUDENT_ID  + " TEXT, " +
            COLUMN_AUDIT_TYPE       + " TEXT, " +
            COLUMN_AUDIT_ACTION     + " TEXT, " +
            COLUMN_AUDIT_RESULT_CODE + " INTEGER, " +
            COLUMN_AUDIT_TIMESTAMP  + " TEXT, " +
            COLUMN_AUDIT_DEVICE_INFO + " TEXT, " +
            COLUMN_AUDIT_IP_ADDRESS + " TEXT)";

    // ── Constructor ──────────────────────────────────────────────────────────

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context.getApplicationContext();
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_EVENTS);
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_REGISTRATIONS);
        db.execSQL(CREATE_TABLE_NOTIFICATIONS);
        db.execSQL(CREATE_TABLE_ATTENDANCE);
        db.execSQL(CREATE_TABLE_ATTENDANCE_CODES);
        db.execSQL(CREATE_TABLE_ATTENDANCE_RATE_LIMIT);
        db.execSQL(CREATE_TABLE_ATTENDANCE_AUDIT);
        ensureLoginRateLimitTable(db);
        ensureUsersTableColumns(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DatabaseHelper", "Upgrading DB from " + oldVersion + " to " + newVersion);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_REGISTRATIONS + " (" +
                COLUMN_REG_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_REG_STUDENT_ID + " TEXT, " +
                COLUMN_REG_EVENT_ID   + " INTEGER, " +
                COLUMN_REG_TIMESTAMP  + " TEXT, " +
                "UNIQUE(" + COLUMN_REG_STUDENT_ID + ", " + COLUMN_REG_EVENT_ID + "))");
        ensureUsersTableColumns(db);
        ensureEventTableColumns(db);
        ensureNotificationsTable(db);
        ensureAttendanceTable(db);
        ensureAttendanceCodesTable(db);
        ensureAttendanceRateLimitTable(db);
        ensureAttendanceAuditTable(db);
        ensureLoginRateLimitTable(db);
        migrateAttendancePhotoColumns(db);
        migrateAttendanceWindowColumns(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        ensureCoreTables(db);
        ensureUsersTableColumns(db);
        ensureEventTableColumns(db);
        ensureNotificationsTable(db);
        ensureAttendanceTable(db);
        ensureAttendanceCodesTable(db);
        ensureAttendanceRateLimitTable(db);
        ensureAttendanceAuditTable(db);
        ensureLoginRateLimitTable(db);
        // Remove legacy seed events that were added automatically on first launch
        removeSeedEvents(db);
        // Backfill creator_sid for events created before v11 or synced without it
        backfillCreatorSid(db);
        // Pass db directly — avoids recursive getWritableDatabase() call inside onOpen
        deleteEndedEventsOlderThan(db, 30);
        // Purge notifications archived more than 30 days ago
        deleteExpiredArchivedNotifications(db);
    }

    // ── Defensive repair ─────────────────────────────────────────────────────

    /** One-time cleanup: delete the three hard-coded seed events if they still exist. */
    private void removeSeedEvents(SQLiteDatabase db) {
        try {
            String[] seedTitles = {"Tech Summit 2026", "Campus Art Fair", "Career Week"};
            for (String title : seedTitles) {
                db.delete(TABLE_EVENTS, COLUMN_TITLE + "=?", new String[]{title});
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "removeSeedEvents failed", e);
        }
    }

    /**
     * Backfill creator_sid for events that have an empty creator_sid.
     * Parses the organizer column (format: "Name – Dept") and looks up the matching
     * officer in the users table by name LIKE match. Updates the event row if found.
     * This repairs events created before DB v11 or synced from Firestore without creator_sid.
     */
    private void backfillCreatorSid(SQLiteDatabase db) {
        try {
            // Find all events with empty creator_sid
            Cursor events = db.rawQuery(
                    "SELECT " + COLUMN_ID + ", " + COLUMN_ORGANIZER +
                    " FROM " + TABLE_EVENTS +
                    " WHERE " + COLUMN_CREATOR_SID + " IS NULL OR " + COLUMN_CREATOR_SID + " = ''",
                    null);
            if (events == null) return;
            while (events.moveToNext()) {
                int eventId   = events.getInt(0);
                String org    = events.getString(1);
                if (org == null || org.isEmpty()) continue;
                // Extract name part before " – " or " - "
                String namePart = org.split("[\u2013\\-]")[0].trim();
                if (namePart.isEmpty()) continue;
                // Find officer by name LIKE match (case-insensitive via SQLite default)
                Cursor user = db.rawQuery(
                        "SELECT " + COLUMN_USER_STUDENT_ID +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COLUMN_USER_NAME + " LIKE ? LIMIT 1",
                        new String[]{"%" + namePart + "%"});
                if (user != null && user.moveToFirst()) {
                    String sid = user.getString(0);
                    if (sid != null && !sid.isEmpty()) {
                        ContentValues cv = new ContentValues();
                        cv.put(COLUMN_CREATOR_SID, sid);
                        db.update(TABLE_EVENTS, cv,
                                COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
                        Log.d("DatabaseHelper", "backfillCreatorSid: event " + eventId +
                                " -> sid=" + sid + " (organizer=" + org + ")");
                    }
                    user.close();
                }
            }
            events.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "backfillCreatorSid failed", e);
        }
    }

    private void ensureCoreTables(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EVENTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT, " + COLUMN_DESC + " TEXT, " +
                    COLUMN_DATE + " TEXT, " +
                    COLUMN_EVENT_TIME + " TEXT DEFAULT '', " +
                    COLUMN_TAGS + " TEXT, " +
                    COLUMN_ORGANIZER + " TEXT, " + COLUMN_CATEGORY + " TEXT, " +
                    COLUMN_IMAGE_PATH + " TEXT, " +
                    COLUMN_STATUS + " TEXT DEFAULT 'PENDING')");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                    COLUMN_USER_ID_PK + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_NAME + " TEXT, " + COLUMN_USER_STUDENT_ID + " TEXT UNIQUE, " +
                    COLUMN_USER_EMAIL + " TEXT UNIQUE, " + COLUMN_USER_PASSWORD + " TEXT, " +
                    COLUMN_USER_ROLE + " TEXT, " + COLUMN_USER_DEPARTMENT + " TEXT)");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_REGISTRATIONS + " (" +
                    COLUMN_REG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_REG_STUDENT_ID + " TEXT, " + COLUMN_REG_EVENT_ID + " INTEGER, " +
                    COLUMN_REG_TIMESTAMP + " TEXT, " +
                    "UNIQUE(" + COLUMN_REG_STUDENT_ID + ", " + COLUMN_REG_EVENT_ID + "))");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "ensureCoreTables failed", e);
        }
    }

    private void ensureUsersTableColumns(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            Set<String> cols = new HashSet<>();
            cursor = db.rawQuery("PRAGMA table_info(" + TABLE_USERS + ")", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int idx = cursor.getColumnIndex("name");
                    if (idx >= 0) { String c = cursor.getString(idx); if (c != null) cols.add(c); }
                } while (cursor.moveToNext());
            }
            if (!cols.contains(COLUMN_USER_ROLE))
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_ROLE + " TEXT DEFAULT 'Student'");
            if (!cols.contains(COLUMN_USER_DEPARTMENT))
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_DEPARTMENT + " TEXT DEFAULT 'General'");
            if (!cols.contains(COLUMN_USER_GENDER))
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_GENDER + " TEXT DEFAULT ''");
            if (!cols.contains(COLUMN_USER_MOBILE))
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_MOBILE + " TEXT DEFAULT ''");
            if (!cols.contains(COLUMN_USER_PROFILE_IMG))
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_PROFILE_IMG + " TEXT DEFAULT ''");
            if (!cols.contains(COLUMN_USER_NOTIF_PREF))
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_NOTIF_PREF + " TEXT DEFAULT 'All Events'");
            if (!cols.contains(COLUMN_USER_EMAIL_VERIFIED))
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_EMAIL_VERIFIED + " INTEGER DEFAULT 1");
            if (!cols.contains(COLUMN_USER_VERIFICATION_TOKEN))
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_VERIFICATION_TOKEN + " TEXT DEFAULT ''");
            if (!cols.contains(COLUMN_USER_LOGIN_ATTEMPTS))
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_LOGIN_ATTEMPTS + " INTEGER DEFAULT 0");
            if (!cols.contains(COLUMN_USER_LOCKED_UNTIL))
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_LOCKED_UNTIL + " TEXT DEFAULT ''");
            if (!cols.contains(COLUMN_USER_FIREBASE_UID))
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_USER_FIREBASE_UID + " TEXT DEFAULT ''");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "ensureUsersTableColumns failed", e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void ensureEventTableColumns(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            Set<String> cols = new HashSet<>();
            cursor = db.rawQuery("PRAGMA table_info(" + TABLE_EVENTS + ")", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int idx = cursor.getColumnIndex("name");
                    if (idx >= 0) { String c = cursor.getString(idx); if (c != null) cols.add(c); }
                } while (cursor.moveToNext());
            }
            if (!cols.contains(COLUMN_EVENT_TIME))
                db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_EVENT_TIME + " TEXT DEFAULT ''");
            if (!cols.contains(COLUMN_START_TIME))
                db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_START_TIME + " TEXT DEFAULT ''");
            if (!cols.contains(COLUMN_END_TIME))
                db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_END_TIME + " TEXT DEFAULT ''");
            if (!cols.contains(COLUMN_CREATOR_SID))
                db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_CREATOR_SID + " TEXT DEFAULT ''");
            if (!cols.contains(COLUMN_IS_HIDDEN))
                db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_IS_HIDDEN + " INTEGER DEFAULT 0");
            if (!cols.contains(COLUMN_VENUE))
                db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_VENUE + " TEXT DEFAULT ''");
            if (!cols.contains(COLUMN_TIME_IN_CODE))
                db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_TIME_IN_CODE + " TEXT DEFAULT ''");
            if (!cols.contains(COLUMN_TIME_OUT_CODE))
                db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_TIME_OUT_CODE + " TEXT DEFAULT ''");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "ensureEventTableColumns failed", e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void ensureAttendanceTable(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ATTENDANCE + " (" +
                    COLUMN_ATT_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ATT_EVENT_ID    + " INTEGER, " +
                    COLUMN_ATT_STUDENT_ID  + " TEXT, " +
                    COLUMN_ATT_TIME_IN_AT  + " TEXT, " +
                    COLUMN_ATT_TIME_OUT_AT + " TEXT DEFAULT '', " +
                    COLUMN_ATT_TIME_IN_PHOTO  + " TEXT DEFAULT '', " +
                    COLUMN_ATT_TIME_OUT_PHOTO + " TEXT DEFAULT '', " +
                    COLUMN_ATT_TIME_IN_WINDOW_OPEN   + " TEXT DEFAULT '', " +
                    COLUMN_ATT_TIME_IN_WINDOW_CLOSE  + " TEXT DEFAULT '', " +
                    COLUMN_ATT_TIME_OUT_WINDOW_OPEN  + " TEXT DEFAULT '', " +
                    COLUMN_ATT_TIME_OUT_WINDOW_CLOSE + " TEXT DEFAULT '', " +
                    "UNIQUE(" + COLUMN_ATT_EVENT_ID + ", " + COLUMN_ATT_STUDENT_ID + "))");
            // Add window columns to existing tables (safe no-op if already present)
            migrateAttendanceWindowColumns(db);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "ensureAttendanceTable failed", e);
        }
    }

    private void migrateAttendancePhotoColumns(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE " + TABLE_ATTENDANCE
                    + " ADD COLUMN " + COLUMN_ATT_TIME_IN_PHOTO + " TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_ATTENDANCE
                    + " ADD COLUMN " + COLUMN_ATT_TIME_OUT_PHOTO + " TEXT DEFAULT ''");
        } catch (Exception e) {
            // Columns may already exist (CREATE TABLE IF NOT EXISTS handles it)
            Log.d("DatabaseHelper", "migrateAttendancePhotoColumns: " + e.getMessage());
        }
    }

    /** v16: add time_in/out window open/close columns to record the allowed window at submission. */
    private void migrateAttendanceWindowColumns(SQLiteDatabase db) {
        String[] cols = {
                COLUMN_ATT_TIME_IN_WINDOW_OPEN,
                COLUMN_ATT_TIME_IN_WINDOW_CLOSE,
                COLUMN_ATT_TIME_OUT_WINDOW_OPEN,
                COLUMN_ATT_TIME_OUT_WINDOW_CLOSE
        };
        for (String col : cols) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_ATTENDANCE + " ADD COLUMN " + col + " TEXT DEFAULT ''");
            } catch (Exception e) {
                // Already exists — safe to ignore
                Log.d("DatabaseHelper", "migrateAttendanceWindowColumns " + col + ": " + e.getMessage());
            }
        }
    }

    private void ensureAttendanceCodesTable(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ATTENDANCE_CODES + " (" +
                    COLUMN_ATT_CODE_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ATT_CODE_EVENT_ID   + " INTEGER, " +
                    COLUMN_ATT_CODE_STUDENT_ID + " TEXT, " +
                    COLUMN_ATT_CODE           + " TEXT, " +
                    COLUMN_ATT_CODE_TYPE      + " TEXT, " +
                    COLUMN_ATT_CODE_STATUS    + " TEXT, " +
                    COLUMN_ATT_CODE_CREATED_AT+ " TEXT, " +
                    COLUMN_ATT_CODE_USED_AT   + " TEXT DEFAULT '', " +
                    "UNIQUE(" + COLUMN_ATT_CODE + "))");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "ensureAttendanceCodesTable failed", e);
        }
    }

    private void ensureAttendanceRateLimitTable(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ATTENDANCE_RATE_LIMIT + " (" +
                    COLUMN_RL_STUDENT_ID   + " TEXT, " +
                    COLUMN_RL_EVENT_ID     + " INTEGER, " +
                    COLUMN_RL_TYPE         + " TEXT, " +
                    COLUMN_RL_ATTEMPTS     + " INTEGER DEFAULT 0, " +
                    COLUMN_RL_LOCKED_UNTIL + " TEXT, " +
                    "PRIMARY KEY (" + COLUMN_RL_STUDENT_ID + ", " + COLUMN_RL_EVENT_ID + ", " + COLUMN_RL_TYPE + "))");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "ensureAttendanceRateLimitTable failed", e);
        }
    }

    private void ensureAttendanceAuditTable(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ATTENDANCE_AUDIT + " (" +
                    COLUMN_AUDIT_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_AUDIT_EVENT_ID    + " INTEGER, " +
                    COLUMN_AUDIT_STUDENT_ID  + " TEXT, " +
                    COLUMN_AUDIT_TYPE       + " TEXT, " +
                    COLUMN_AUDIT_ACTION     + " TEXT, " +
                    COLUMN_AUDIT_RESULT_CODE + " INTEGER, " +
                    COLUMN_AUDIT_TIMESTAMP  + " TEXT, " +
                    COLUMN_AUDIT_DEVICE_INFO + " TEXT, " +
                    COLUMN_AUDIT_IP_ADDRESS + " TEXT)");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "ensureAttendanceAuditTable failed", e);
        }
    }

    private void ensureLoginRateLimitTable(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LOGIN_RATE_LIMIT + " (" +
                    COLUMN_LR_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_LR_STUDENT_ID  + " TEXT, " +
                    COLUMN_LR_ATTEMPTS    + " INTEGER DEFAULT 0, " +
                    COLUMN_LR_LOCKED_UNTIL + " TEXT DEFAULT '', " +
                    "UNIQUE(" + COLUMN_LR_STUDENT_ID + "))");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "ensureLoginRateLimitTable failed", e);
        }
    }

    private void ensureNotificationsTable(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + " (" +
                    COLUMN_NOTIF_ID             + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NOTIF_RECIPIENT_SID  + " TEXT, " +
                    COLUMN_NOTIF_EVENT_ID       + " INTEGER, " +
                    COLUMN_NOTIF_TYPE           + " TEXT, " +
                    COLUMN_NOTIF_MESSAGE        + " TEXT, " +
                    COLUMN_NOTIF_REASON         + " TEXT, " +
                    COLUMN_NOTIF_SUGGESTED_DATE + " TEXT DEFAULT '', " +
                    COLUMN_NOTIF_SUGGESTED_TIME + " TEXT DEFAULT '', " +
                    COLUMN_NOTIF_INSTRUCTIONS   + " TEXT DEFAULT '', " +
                    COLUMN_NOTIF_IS_READ        + " INTEGER DEFAULT 0, " +
                    COLUMN_NOTIF_CREATED_AT     + " TEXT, " +
                    COLUMN_NOTIF_IS_ARCHIVED    + " INTEGER DEFAULT 0, " +
                    COLUMN_NOTIF_ARCHIVED_AT    + " TEXT DEFAULT '')" );
            // Add columns that may be missing from older schema versions (silent fail if present)
            try { db.execSQL("ALTER TABLE " + TABLE_NOTIFICATIONS + " ADD COLUMN " + COLUMN_NOTIF_SUGGESTED_TIME + " TEXT DEFAULT ''"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_NOTIFICATIONS + " ADD COLUMN " + COLUMN_NOTIF_IS_ARCHIVED   + " INTEGER DEFAULT 0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_NOTIFICATIONS + " ADD COLUMN " + COLUMN_NOTIF_ARCHIVED_AT   + " TEXT DEFAULT ''");   } catch (Exception ignored) {}
        } catch (Exception e) {
            Log.e("DatabaseHelper", "ensureNotificationsTable failed", e);
        }
    }

    // ── User Operations ───────────────────────────────────────────────────────

    public long registerUser(String name, String studentId, String email,
                             String password, String role, String department,
                             String firebaseUid) {
        SQLiteDatabase db = this.getWritableDatabase();
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String verificationToken = java.util.UUID.randomUUID().toString();
        ContentValues v = new ContentValues();
        v.put(COLUMN_USER_NAME, name);
        v.put(COLUMN_USER_STUDENT_ID, studentId);
        v.put(COLUMN_USER_EMAIL, email);
        v.put(COLUMN_USER_PASSWORD, hashedPassword);
        v.put(COLUMN_USER_ROLE, role);
        v.put(COLUMN_USER_DEPARTMENT, department);
        v.put(COLUMN_USER_EMAIL_VERIFIED, 0);
        v.put(COLUMN_USER_VERIFICATION_TOKEN, verificationToken);
        v.put(COLUMN_USER_FIREBASE_UID, firebaseUid != null ? firebaseUid : "");
        long id = db.insert(TABLE_USERS, null, v);
        db.close();
        if (id != -1 && firebaseUid != null && !firebaseUid.isEmpty()) {
            // Mirror to Firestore — include the BCrypt hash so sync can restore it on other devices
            new FirestoreHelper().upsertUserWithVerification(firebaseUid, studentId, name, email, role, department,
                    "", "", "", "All Events", false, verificationToken, hashedPassword);
        }
        return id;
    }

    /** Returns true if this email already has a row in the local SQLite users table. */
    public boolean isEmailInLocalDb(String email) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT 1 FROM " + TABLE_USERS + " WHERE " + COLUMN_USER_EMAIL + "=? LIMIT 1",
                    new String[]{email});
            boolean exists = (c != null && c.moveToFirst());
            if (c != null) c.close();
            return exists;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "isEmailInLocalDb failed", e);
            return false;
        }
    }

    /** Returns the email address for a given loginInput (email or student_id). */
    public String getEmailForLoginInput(String loginInput) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT " + COLUMN_USER_EMAIL + " FROM " + TABLE_USERS +
                    " WHERE (" + COLUMN_USER_EMAIL + "=? OR " + COLUMN_USER_STUDENT_ID + "=?) LIMIT 1",
                    new String[]{loginInput, loginInput});
            if (c != null && c.moveToFirst()) {
                String email = c.getString(0);
                c.close();
                return email;
            }
            if (c != null) c.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getEmailForLoginInput failed", e);
        }
        return null;
    }

    /** Returns true if a user row exists for this loginInput but their stored password is empty or null. */
    public boolean userExistsWithEmptyPassword(String loginInput) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT " + COLUMN_USER_PASSWORD + " FROM " + TABLE_USERS +
                    " WHERE (" + COLUMN_USER_EMAIL + "=? OR " + COLUMN_USER_STUDENT_ID + "=?) LIMIT 1",
                    new String[]{loginInput, loginInput});
            if (c != null && c.moveToFirst()) {
                String pw = c.getString(0);
                c.close();
                return pw == null || pw.isEmpty();
            }
            if (c != null) c.close();
            return false;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "userExistsWithEmptyPassword failed", e);
            return false;
        }
    }

    /** Restores a BCrypt hash for an existing user whose password was wiped by a sync bug. */
    public void restorePassword(String loginInput, String plainPassword) {
        try {
            String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_USER_PASSWORD, hash);
            db.update(TABLE_USERS, v,
                    COLUMN_USER_EMAIL + "=? OR " + COLUMN_USER_STUDENT_ID + "=?",
                    new String[]{loginInput, loginInput});
            db.close();
            Log.d("DatabaseHelper", "Password restored for: " + loginInput);
            // Also push the hash back to Firestore so future syncs carry it
            Cursor c = this.getReadableDatabase().rawQuery(
                    "SELECT " + COLUMN_USER_STUDENT_ID + ", " + COLUMN_USER_FIREBASE_UID +
                    " FROM " + TABLE_USERS +
                    " WHERE (" + COLUMN_USER_EMAIL + "=? OR " + COLUMN_USER_STUDENT_ID + "=?) LIMIT 1",
                    new String[]{loginInput, loginInput});
            if (c != null && c.moveToFirst()) {
                String sid = c.getString(0);
                String uid = c.getString(1);
                c.close();
                if (uid != null && !uid.isEmpty()) {
                    new FirestoreHelper().updateUserField(uid, "password", hash);
                } else {
                    Log.w("DatabaseHelper", "restorePassword: no firebase_uid for sid=" + sid + ", Firestore sync skipped");
                }
            } else if (c != null) c.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "restorePassword failed", e);
        }
    }

    public Cursor checkUser(String loginInput, String password) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_USERS + " WHERE (" +
                    COLUMN_USER_EMAIL + "=? OR " + COLUMN_USER_STUDENT_ID + "=?)",
                    new String[]{loginInput, loginInput});

            if (cursor != null && cursor.moveToFirst()) {
                String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_PASSWORD));
                String studentId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_STUDENT_ID));
                boolean isLegacyPassword = !storedPassword.startsWith("$2");

                boolean passwordMatches;
                if (isLegacyPassword) {
                    passwordMatches = password.equals(storedPassword);
                } else {
                    passwordMatches = BCrypt.checkpw(password, storedPassword);
                }

                if (passwordMatches) {
                    if (isLegacyPassword) {
                        upgradePasswordToBCrypt(studentId, password);
                    }
                    return cursor;
                }
                cursor.close();
                return null;
            }
            if (cursor != null) cursor.close();
            return null;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "checkUser failed", e);
            return null;
        }
    }

    public boolean wasPasswordUpgradedFromLegacy(String loginInput, String password) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT " + COLUMN_USER_PASSWORD + " FROM " + TABLE_USERS + " WHERE (" +
                    COLUMN_USER_EMAIL + "=? OR " + COLUMN_USER_STUDENT_ID + "=?)",
                    new String[]{loginInput, loginInput});

            if (cursor != null && cursor.moveToFirst()) {
                String storedPassword = cursor.getString(0);
                cursor.close();
                return !storedPassword.startsWith("$2") && password.equals(storedPassword);
            }
            if (cursor != null) cursor.close();
            return false;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "wasPasswordUpgradedFromLegacy failed", e);
            return false;
        }
    }

    private void upgradePasswordToBCrypt(String studentId, String plainPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        ContentValues v = new ContentValues();
        v.put(COLUMN_USER_PASSWORD, hashedPassword);
        db.update(TABLE_USERS, v, COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
        db.close();
        Log.d("DatabaseHelper", "Password upgraded to BCrypt for: " + studentId);
    }

    public Cursor getUserByStudentId(String studentId) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " +
                    COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getUserByStudentId failed", e);
            return null;
        }
    }

    public int getCount(String table, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + table;
        if (whereClause != null) query += " WHERE " + whereClause;
        Cursor cursor = db.rawQuery(query, whereArgs);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) { count = cursor.getInt(0); cursor.close(); }
        return count;
    }

    public Cursor getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS +
                " ORDER BY " + COLUMN_USER_NAME + " ASC", null);
    }

    public void updateUserRole(String studentId, String newRole) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_USER_ROLE, newRole);
        db.update(TABLE_USERS, v, COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
        db.close();
        String uid = getFirebaseUid(studentId);
        if (!uid.isEmpty()) {
            new FirestoreHelper().updateUserField(uid, "role", newRole);
        }
    }

    public boolean updateUserProfile(String studentId, String gender, String mobile, String profileImagePath) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            if (gender != null)           v.put(COLUMN_USER_GENDER, gender);
            if (mobile != null)           v.put(COLUMN_USER_MOBILE, mobile);
            if (profileImagePath != null) v.put(COLUMN_USER_PROFILE_IMG, profileImagePath);
            int rows = db.update(TABLE_USERS, v, COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
            db.close();
            if (rows > 0) {
                String uid = getFirebaseUid(studentId);
                if (!uid.isEmpty()) {
                    FirestoreHelper fsh = new FirestoreHelper();
                    if (gender != null)           fsh.updateUserField(uid, "gender",        gender);
                    if (mobile != null)           fsh.updateUserField(uid, "mobile",        mobile);
                    if (profileImagePath != null) {
                        // Use sentinel "DELETED" for explicit image removal so other devices
                        // can distinguish "never had a photo" (Firestore field missing → "")
                        // from "user explicitly removed the photo" → "DELETED".
                        String firestoreImageValue = profileImagePath.isEmpty() ? "DELETED" : profileImagePath;
                        fsh.updateUserField(uid, "profile_image", firestoreImageValue);
                    }
                }
            }
            return rows > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "updateUserProfile failed", e);
            return false;
        }
    }

    public boolean changePassword(String studentId, String oldPassword, String newPassword) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            Cursor c = db.rawQuery("SELECT " + COLUMN_USER_PASSWORD + " FROM " + TABLE_USERS +
                    " WHERE " + COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
            if (c == null || !c.moveToFirst()) { if (c != null) c.close(); db.close(); return false; }
            String stored = c.getString(0);
            c.close();
            if (!BCrypt.checkpw(oldPassword, stored)) { db.close(); return false; }
            String hashedNew = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            ContentValues v = new ContentValues();
            v.put(COLUMN_USER_PASSWORD, hashedNew);
            db.update(TABLE_USERS, v, COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
            db.close();
            String uid = getFirebaseUid(studentId);
            if (!uid.isEmpty()) {
                new FirestoreHelper().updatePassword(uid, hashedNew);
            }
            return true;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "changePassword failed", e);
            return false;
        }
    }

    public void updateNotifPref(String studentId, String pref) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_USER_NOTIF_PREF, pref);
            db.update(TABLE_USERS, v, COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
            db.close();
            String uid = getFirebaseUid(studentId);
            if (!uid.isEmpty()) {
                new FirestoreHelper().updateUserField(uid, "notif_pref", pref);
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "updateNotifPref failed", e);
        }
    }

    // ── Firebase UID helpers ─────────────────────────────────────────────────

    /** Returns the stored Firebase Auth UID for a student_id, or "" if not found. */
    public String getFirebaseUid(String studentId) {
        if (studentId == null || studentId.isEmpty()) return "";
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT " + COLUMN_USER_FIREBASE_UID + " FROM " + TABLE_USERS +
                    " WHERE " + COLUMN_USER_STUDENT_ID + "=? LIMIT 1",
                    new String[]{studentId});
            if (c != null && c.moveToFirst()) {
                String uid = c.getString(0);
                c.close();
                return uid != null ? uid : "";
            }
            if (c != null) c.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getFirebaseUid failed", e);
        }
        return "";
    }

    /** Stores the Firebase Auth UID for a student_id. */
    public void setFirebaseUid(String studentId, String firebaseUid) {
        if (studentId == null || studentId.isEmpty()) return;
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_USER_FIREBASE_UID, firebaseUid != null ? firebaseUid : "");
            db.update(TABLE_USERS, v, COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "setFirebaseUid failed", e);
        }
    }

    // ── Email Verification Operations ─────────────────────────────────────────

    public boolean isEmailVerified(String studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COLUMN_USER_EMAIL_VERIFIED + " FROM " + TABLE_USERS +
                " WHERE " + COLUMN_USER_STUDENT_ID + "=?",
                new String[]{studentId});
        if (c != null && c.moveToFirst()) {
            int verified = c.getInt(0);
            c.close();
            return verified == 1;
        }
        if (c != null) c.close();
        return false;
    }

    public void setEmailVerified(String studentId, boolean verified) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_USER_EMAIL_VERIFIED, verified ? 1 : 0);
        v.put(COLUMN_USER_VERIFICATION_TOKEN, "");
        db.update(TABLE_USERS, v, COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
        db.close();
        String uid = getFirebaseUid(studentId);
        if (!uid.isEmpty()) {
            new FirestoreHelper().updateUserField(uid, "email_verified", verified ? 1 : 0);
        }
    }

    public String getVerificationToken(String studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COLUMN_USER_VERIFICATION_TOKEN + " FROM " + TABLE_USERS +
                " WHERE " + COLUMN_USER_STUDENT_ID + "=?",
                new String[]{studentId});
        if (c != null && c.moveToFirst()) {
            String token = c.getString(0);
            c.close();
            return token;
        }
        if (c != null) c.close();
        return null;
    }

    // ── Login Rate Limiting ───────────────────────────────────────────────────

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCKOUT_DURATION_MS = 15 * 60 * 1000;

    public boolean isLoginLocked(String loginInput) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COLUMN_LR_LOCKED_UNTIL + " FROM " + TABLE_LOGIN_RATE_LIMIT +
                " WHERE " + COLUMN_LR_STUDENT_ID + "=?",
                new String[]{loginInput});
        if (c != null && c.moveToFirst()) {
            String lockedUntilStr = c.getString(0);
            c.close();
            if (lockedUntilStr != null && !lockedUntilStr.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date lockedUntil = sdf.parse(lockedUntilStr);
                    if (lockedUntil != null && lockedUntil.getTime() > System.currentTimeMillis()) {
                        return true;
                    }
                } catch (Exception e) {
                    Log.e("DatabaseHelper", "isLoginLocked parse error", e);
                }
            }
        }
        if (c != null) c.close();
        return false;
    }

    public long getLoginLockoutRemainingMs(String loginInput) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COLUMN_LR_LOCKED_UNTIL + " FROM " + TABLE_LOGIN_RATE_LIMIT +
                " WHERE " + COLUMN_LR_STUDENT_ID + "=?",
                new String[]{loginInput});
        if (c != null && c.moveToFirst()) {
            String lockedUntilStr = c.getString(0);
            c.close();
            if (lockedUntilStr != null && !lockedUntilStr.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date lockedUntil = sdf.parse(lockedUntilStr);
                    if (lockedUntil != null) {
                        long remaining = lockedUntil.getTime() - System.currentTimeMillis();
                        return remaining > 0 ? remaining : 0;
                    }
                } catch (Exception e) {
                    Log.e("DatabaseHelper", "getLoginLockoutRemainingMs parse error", e);
                }
            }
        }
        if (c != null) c.close();
        return 0;
    }

    public void incrementLoginAttempts(String loginInput) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COLUMN_LR_ATTEMPTS + " FROM " + TABLE_LOGIN_RATE_LIMIT +
                " WHERE " + COLUMN_LR_STUDENT_ID + "=?",
                new String[]{loginInput});
        int attempts = 0;
        if (c != null && c.moveToFirst()) {
            attempts = c.getInt(0);
            c.close();
        }
        if (c != null) c.close();
        
        attempts++;
        String lockedUntil = "";
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            long lockTime = System.currentTimeMillis() + LOGIN_LOCKOUT_DURATION_MS;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            lockedUntil = sdf.format(new Date(lockTime));
        }
        
        ContentValues v = new ContentValues();
        v.put(COLUMN_LR_STUDENT_ID, loginInput);
        v.put(COLUMN_LR_ATTEMPTS, attempts);
        v.put(COLUMN_LR_LOCKED_UNTIL, lockedUntil);
        db.insertWithOnConflict(TABLE_LOGIN_RATE_LIMIT, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public void resetLoginAttempts(String loginInput) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LOGIN_RATE_LIMIT, COLUMN_LR_STUDENT_ID + "=?", new String[]{loginInput});
        db.close();
    }

    // ── Registration Operations ───────────────────────────────────────────────

    public boolean registerForEvent(String studentId, int eventId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(ServerTimeUtil.now());
            ContentValues v = new ContentValues();
            v.put(COLUMN_REG_STUDENT_ID, studentId);
            v.put(COLUMN_REG_EVENT_ID, eventId);
            v.put(COLUMN_REG_TIMESTAMP, timestamp);
            long id = db.insertOrThrow(TABLE_REGISTRATIONS, null, v);
            db.close();
            if (id != -1) {
                new FirestoreHelper().upsertRegistration(studentId, eventId, timestamp);
            }
            return id != -1;
        } catch (android.database.sqlite.SQLiteConstraintException e) {
            return false;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "registerForEvent failed", e);
            return false;
        }
    }

    public boolean isRegistered(String studentId, int eventId) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT 1 FROM " + TABLE_REGISTRATIONS + " WHERE " +
                    COLUMN_REG_STUDENT_ID + "=? AND " + COLUMN_REG_EVENT_ID + "=?",
                    new String[]{studentId, String.valueOf(eventId)});
            boolean result = (c != null && c.moveToFirst());
            if (c != null) c.close();
            return result;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "isRegistered failed", e);
            return false;
        }
    }

    public List<Event> getRegisteredEvents(String studentId) {
        List<Event> events = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT e.* FROM " + TABLE_EVENTS + " e " +
                    "INNER JOIN " + TABLE_REGISTRATIONS + " r ON e." + COLUMN_ID +
                    " = r." + COLUMN_REG_EVENT_ID +
                    " WHERE r." + COLUMN_REG_STUDENT_ID + "=? " +
                    "ORDER BY e." + COLUMN_DATE + " ASC",
                    new String[]{studentId});
            if (c != null && c.moveToFirst()) {
                do { events.add(eventFromCursor(c)); } while (c.moveToNext());
                c.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getRegisteredEvents failed", e);
        }
        return events;
    }

    // ── Attendance Operations ─────────────────────────────────────────────────

    /**
     * Set (or replace) the Time-In code for an event.
     * Persists to local SQLite AND pushes both codes to Firestore so other devices
     * can receive the code via the next SyncManager run.
     * Returns true if the local update succeeded.
     */
    public boolean setTimeInCode(int eventId, String code) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_TIME_IN_CODE, code != null ? code : "");
            int rows = db.update(TABLE_EVENTS, v, COLUMN_ID + "=?",
                    new String[]{String.valueOf(eventId)});
            db.close();
            if (rows > 0) {
                // Also push to Firestore so students on other devices receive it.
                String currentTimeOut = getTimeOutCodeForEvent(eventId);
                new FirestoreHelper().updateEventAttendanceCodes(eventId, code, currentTimeOut);
            }
            return rows > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "setTimeInCode failed", e);
            return false;
        }
    }

    /**
     * Set (or replace) the Time-Out code for an event.
     * Persists to local SQLite AND pushes both codes to Firestore so other devices
     * can receive the code via the next SyncManager run.
     * Returns true if the local update succeeded.
     */
    public boolean setTimeOutCode(int eventId, String code) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_TIME_OUT_CODE, code != null ? code : "");
            int rows = db.update(TABLE_EVENTS, v, COLUMN_ID + "=?",
                    new String[]{String.valueOf(eventId)});
            db.close();
            if (rows > 0) {
                // Also push to Firestore so students on other devices receive it.
                String currentTimeIn = getTimeInCodeForEvent(eventId);
                new FirestoreHelper().updateEventAttendanceCodes(eventId, currentTimeIn, code);
            }
            return rows > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "setTimeOutCode failed", e);
            return false;
        }
    }

    /** Returns the stored time_in_code for an event, or "" if not set. */
    private String getTimeInCodeForEvent(int eventId) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT " + COLUMN_TIME_IN_CODE + " FROM " + TABLE_EVENTS +
                    " WHERE " + COLUMN_ID + "=?",
                    new String[]{String.valueOf(eventId)});
            String code = "";
            if (c != null) {
                if (c.moveToFirst()) code = c.getString(0);
                c.close();
            }
            db.close();
            return code != null ? code : "";
        } catch (Exception e) {
            return "";
        }
    }

    /** Returns the stored time_out_code for an event, or "" if not set. */
    private String getTimeOutCodeForEvent(int eventId) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT " + COLUMN_TIME_OUT_CODE + " FROM " + TABLE_EVENTS +
                    " WHERE " + COLUMN_ID + "=?",
                    new String[]{String.valueOf(eventId)});
            String code = "";
            if (c != null) {
                if (c.moveToFirst()) code = c.getString(0);
                c.close();
            }
            db.close();
            return code != null ? code : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Generates a random 6-character alphanumeric code.
     */
    public static String generateAttendanceCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    public String generateUniqueAttendanceCode() {
        int attempts = 0;
        String code;
        do {
            if (attempts++ > 20) return null;
            code = generateAttendanceCode();
        } while (doesAttendanceCodeExist(code));
        return code;
    }

    private boolean doesAttendanceCodeExist(String code) {
        if (code == null || code.isEmpty()) return false;
        Cursor c = null;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            c = db.rawQuery("SELECT 1 FROM " + TABLE_ATTENDANCE_CODES + " WHERE " + COLUMN_ATT_CODE + "=? LIMIT 1", new String[]{code});
            return c != null && c.moveToFirst();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "doesAttendanceCodeExist failed", e);
            return false;
        } finally {
            if (c != null) c.close();
        }
    }

    public long requestAttendanceCode(int eventId, String studentId, String type) {
        if (eventId <= 0 || studentId == null || studentId.isEmpty()) return -1;
        type = (type != null ? type.trim().toUpperCase(Locale.getDefault()) : "");
        if (!"IN".equals(type) && !"OUT".equals(type)) return -1;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor statusCursor = db.rawQuery("SELECT " + COLUMN_STATUS + " FROM " + TABLE_EVENTS + " WHERE " + COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
        if (statusCursor == null || !statusCursor.moveToFirst()) {
            if (statusCursor != null) statusCursor.close();
            return -1;
        }
        String status = statusCursor.getString(0);
        statusCursor.close();
        if (!"APPROVED".equals(status)) {
            Log.w("DatabaseHelper", "Code request rejected: event not approved, eventId=" + eventId);
            return -1; // event not approved
        }

        Cursor regCursor = db.rawQuery(
                "SELECT 1 FROM " + TABLE_REGISTRATIONS + 
                " WHERE " + COLUMN_REG_STUDENT_ID + "=? AND " + COLUMN_REG_EVENT_ID + "=? LIMIT 1",
                new String[]{studentId, String.valueOf(eventId)});
        boolean isRegistered = regCursor != null && regCursor.moveToFirst();
        if (regCursor != null) regCursor.close();
        if (!isRegistered) {
            Log.w("DatabaseHelper", "Code request rejected: student not registered, studentId=" + studentId + ", eventId=" + eventId);
            return -6; // student not registered for event
        }

        int validationResult = checkEventTimeWindow(eventId, type);
        if (validationResult == 1) return -2; // date mismatch
        if (validationResult == 2) return -3; // too early
        if (validationResult == 3) return -4; // too late
        if ("OUT".equals(type) && validationResult == 0) {
            Cursor timeCursor = db.rawQuery("SELECT " + COLUMN_END_TIME + ", " + COLUMN_EVENT_TIME + " FROM " + TABLE_EVENTS + " WHERE " + COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
            if (timeCursor != null && timeCursor.moveToFirst()) {
                String endTimeStr = timeCursor.getString(0);
                String eventTime = timeCursor.getString(1);
                timeCursor.close();
                int endMinutes = 24 * 60 - 1;
                if (endTimeStr != null && !endTimeStr.isEmpty()) {
                    endMinutes = parseTimeToMinutes(endTimeStr, 24 * 60 - 1);
                } else if (eventTime != null && !eventTime.isEmpty()) {
                    String[] parts = eventTime.split("-");
                    if (parts.length == 2) {
                        endMinutes = parseTimeToMinutes(parts[1].trim(), 24 * 60 - 1);
                    }
                }
                Calendar now = Calendar.getInstance();
                now.setTime(ServerTimeUtil.now());
                int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
                if (nowMinutes > endMinutes) {
                    validationResult = 4;
                }
            }
        }
        if (validationResult == 4) return -5; // late but allowed

        String newCode = generateUniqueAttendanceCode();
        if (newCode == null || newCode.isEmpty()) return -1;

        db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues expire = new ContentValues();
            expire.put(COLUMN_ATT_CODE_STATUS, "EXPIRED");
            db.update(TABLE_ATTENDANCE_CODES, expire,
                    COLUMN_ATT_CODE_EVENT_ID + "=? AND " + COLUMN_ATT_CODE_STUDENT_ID + "=? AND " +
                    COLUMN_ATT_CODE_TYPE + "=? AND " + COLUMN_ATT_CODE_STATUS + "='UNUSED'",
                    new String[]{String.valueOf(eventId), studentId, type});

            ContentValues v = new ContentValues();
            v.put(COLUMN_ATT_CODE_EVENT_ID, eventId);
            v.put(COLUMN_ATT_CODE_STUDENT_ID, studentId);
            v.put(COLUMN_ATT_CODE, newCode);
            v.put(COLUMN_ATT_CODE_TYPE, type);
            v.put(COLUMN_ATT_CODE_STATUS, "UNUSED");
            v.put(COLUMN_ATT_CODE_CREATED_AT, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            long id = db.insert(TABLE_ATTENDANCE_CODES, null, v);
            if (id == -1) {
                return -1;
            }

            String subject = "Time-" + type + " code for event";
            String message = "Your Time-" + type + " code for this event is: " + newCode;
            insertNotification(studentId, eventId, "TIME_" + type + "_CODE", message,
                    "", "", "", "Use this code now to complete attendance.");

            db.setTransactionSuccessful();
            return id;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "requestAttendanceCode failed", e);
            return -1;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public String getActiveAttendanceCode(int eventId, String studentId, String type) {
        if (eventId <= 0 || studentId == null || studentId.isEmpty()) return "";
        type = (type != null ? type.trim().toUpperCase(Locale.getDefault()) : "");
        if (!"IN".equals(type) && !"OUT".equals(type)) return "";

        Cursor c = null;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            c = db.rawQuery("SELECT " + COLUMN_ATT_CODE + " FROM " + TABLE_ATTENDANCE_CODES +
                    " WHERE " + COLUMN_ATT_CODE_EVENT_ID + "=? AND " + COLUMN_ATT_CODE_STUDENT_ID + "=? AND " +
                    COLUMN_ATT_CODE_TYPE + "=? AND " + COLUMN_ATT_CODE_STATUS + "='UNUSED' " +
                    "ORDER BY " + COLUMN_ATT_CODE_CREATED_AT + " DESC LIMIT 1",
                    new String[]{String.valueOf(eventId), studentId, type});
            if (c != null && c.moveToFirst()) {
                String code = c.getString(0);
                c.close();
                return code != null ? code : "";
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getActiveAttendanceCode failed", e);
        } finally {
            if (c != null) c.close();
        }
        return "";
    }

    public boolean consumeAttendanceCode(int eventId, String studentId, String type, String code) {
        if (eventId <= 0 || studentId == null || studentId.isEmpty() || code == null || code.isEmpty()) return false;
        type = (type != null ? type.trim().toUpperCase(Locale.getDefault()) : "");
        if (!"IN".equals(type) && !"OUT".equals(type)) return false;

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_ATT_CODE_STATUS, "USED");
            v.put(COLUMN_ATT_CODE_USED_AT, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            db.beginTransactionNonExclusive();
            try {
                int rows = db.update(TABLE_ATTENDANCE_CODES, v,
                        COLUMN_ATT_CODE_EVENT_ID + "=? AND " + COLUMN_ATT_CODE_STUDENT_ID + "=? AND " +
                        COLUMN_ATT_CODE_TYPE + "=? AND " + COLUMN_ATT_CODE + "=? AND " + COLUMN_ATT_CODE_STATUS + "='UNUSED'",
                        new String[]{String.valueOf(eventId), studentId, type, code});
                if (rows > 0) {
                    db.setTransactionSuccessful();
                    return true;
                }
                return false;
            } catch (Exception e) {
                Log.e("DatabaseHelper", "consumeAttendanceCode failed", e);
                return false;
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "consumeAttendanceCode failed", e);
            return false;
        } finally {
            if (db != null) db.close();
        }
    }

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    public boolean isRateLimited(int eventId, String studentId, String type) {
        if (eventId <= 0 || studentId == null || studentId.isEmpty() || type == null) return false;
        type = type.trim().toUpperCase(Locale.getDefault());
        if (!"IN".equals(type) && !"OUT".equals(type)) return false;

        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = this.getReadableDatabase();
            c = db.rawQuery(
                    "SELECT " + COLUMN_RL_ATTEMPTS + ", " + COLUMN_RL_LOCKED_UNTIL +
                    " FROM " + TABLE_ATTENDANCE_RATE_LIMIT +
                    " WHERE " + COLUMN_RL_EVENT_ID + "=? AND " + COLUMN_RL_STUDENT_ID + "=? AND " + COLUMN_RL_TYPE + "=?",
                    new String[]{String.valueOf(eventId), studentId, type});
            if (c != null && c.moveToFirst()) {
                int attempts = c.getInt(0);
                String lockedUntilStr = c.getString(1);
                c.close();
                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    if (lockedUntilStr != null && !lockedUntilStr.isEmpty()) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            Date lockedUntil = sdf.parse(lockedUntilStr);
                            if (lockedUntil != null && lockedUntil.getTime() > ServerTimeUtil.nowMillis()) {
                                Log.w("DatabaseHelper", "Rate limited: student=" + studentId + ", event=" + eventId + ", type=" + type);
                                return true;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            if (c != null) c.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "isRateLimited failed", e);
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return false;
    }

    public void incrementFailedAttempt(int eventId, String studentId, String type) {
        if (eventId <= 0 || studentId == null || studentId.isEmpty() || type == null) return;
        type = type.trim().toUpperCase(Locale.getDefault());
        if (!"IN".equals(type) && !"OUT".equals(type)) return;

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            db.beginTransactionNonExclusive();
            try {
                Cursor c = db.rawQuery(
                        "SELECT " + COLUMN_RL_ATTEMPTS +
                        " FROM " + TABLE_ATTENDANCE_RATE_LIMIT +
                        " WHERE " + COLUMN_RL_EVENT_ID + "=? AND " + COLUMN_RL_STUDENT_ID + "=? AND " + COLUMN_RL_TYPE + "=?",
                        new String[]{String.valueOf(eventId), studentId, type});
                int currentAttempts = 0;
                if (c != null && c.moveToFirst()) {
                    currentAttempts = c.getInt(0);
                    c.close();
                }
                int newAttempts = currentAttempts + 1;
                ContentValues v = new ContentValues();
                v.put(COLUMN_RL_ATTEMPTS, newAttempts);
                if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                    long lockedUntilMs = System.currentTimeMillis() + LOCKOUT_DURATION_MS;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    v.put(COLUMN_RL_LOCKED_UNTIL, sdf.format(new Date(lockedUntilMs)));
                    Log.w("DatabaseHelper", "Locked out: student=" + studentId + ", event=" + eventId + ", type=" + type + ", attempts=" + newAttempts);
                }
                db.insertWithOnConflict(TABLE_ATTENDANCE_RATE_LIMIT, null, v, SQLiteDatabase.CONFLICT_REPLACE);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "incrementFailedAttempt failed", e);
        } finally {
            if (db != null) db.close();
        }
    }

    public void resetFailedAttempts(int eventId, String studentId, String type) {
        if (eventId <= 0 || studentId == null || studentId.isEmpty() || type == null) return;
        type = type.trim().toUpperCase(Locale.getDefault());
        if (!"IN".equals(type) && !"OUT".equals(type)) return;

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            db.delete(TABLE_ATTENDANCE_RATE_LIMIT,
                    COLUMN_RL_EVENT_ID + "=? AND " + COLUMN_RL_STUDENT_ID + "=? AND " + COLUMN_RL_TYPE + "=?",
                    new String[]{String.valueOf(eventId), studentId, type});
        } catch (Exception e) {
            Log.e("DatabaseHelper", "resetFailedAttempts failed", e);
        } finally {
            if (db != null) db.close();
        }
    }

    public void logAttendanceAttempt(int eventId, String studentId, String type, String action, int resultCode) {
        logAttendanceAttempt(eventId, studentId, type, action, resultCode, "", "");
    }

    public void logAttendanceAttempt(int eventId, String studentId, String type, String action, int resultCode, String deviceInfo, String ipAddress) {
        if (eventId <= 0 || studentId == null || studentId.isEmpty() || type == null || action == null) return;
        type = type.trim().toUpperCase(Locale.getDefault());
        action = action.trim().toUpperCase(Locale.getDefault());

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_AUDIT_EVENT_ID, eventId);
            v.put(COLUMN_AUDIT_STUDENT_ID, studentId);
            v.put(COLUMN_AUDIT_TYPE, type);
            v.put(COLUMN_AUDIT_ACTION, action);
            v.put(COLUMN_AUDIT_RESULT_CODE, resultCode);
            v.put(COLUMN_AUDIT_TIMESTAMP, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            v.put(COLUMN_AUDIT_DEVICE_INFO, deviceInfo != null ? deviceInfo : "");
            v.put(COLUMN_AUDIT_IP_ADDRESS, ipAddress != null ? ipAddress : "");
            db.insert(TABLE_ATTENDANCE_AUDIT, null, v);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "logAttendanceAttempt failed", e);
        } finally {
            if (db != null) db.close();
        }
    }

    public int checkEventTimeWindow(int eventId, String type) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT " + COLUMN_DATE + ", " + COLUMN_START_TIME + ", " + COLUMN_END_TIME + ", " + COLUMN_EVENT_TIME + " FROM " + TABLE_EVENTS + " WHERE " + COLUMN_ID + "=?",
                    new String[]{String.valueOf(eventId)});
            if (c == null || !c.moveToFirst()) {
                if (c != null) c.close();
                return -1;
            }
            String eventDate = c.getString(0);
            String startTimeStr = c.getString(1);
            String endTimeStr = c.getString(2);
            String eventTime = c.getString(3);
            c.close();
            db.close();

            // Current local time
            String today = ServerTimeUtil.todayString();
            if (!today.equals(eventDate)) {
                return 1; // date mismatch - but allow on event date
            }

            long nowMs = ServerTimeUtil.nowMillis();
            int nowMinutes = (int) ((nowMs / 60000) % (24 * 60));
            int startMinutes = 0;
            int endMinutes = 24 * 60 - 1;

            if (startTimeStr != null && !startTimeStr.isEmpty() && endTimeStr != null && !endTimeStr.isEmpty()) {
                startMinutes = parseTimeToMinutes(startTimeStr, 0);
                endMinutes = parseTimeToMinutes(endTimeStr, 24 * 60 - 1);
            } else {
                // Fallback to parsing eventTime for backward compatibility
                if (eventTime != null && !eventTime.isEmpty()) {
                    String[] parts = eventTime.split("-");
                    if (parts.length == 2) {
                        String s = parts[0].trim();
                        String e = parts[1].trim();
                        startMinutes = parseTimeToMinutes(s, 0);
                        endMinutes = parseTimeToMinutes(e, 24 * 60 - 1);
                    }
                }
            }

            if ("IN".equals(type)) {
                // Allow Time-In from 10 minutes before start up to 1 hour after start.
                // e.g. 7:00 AM event → window is 6:50 AM – 7:59 AM (start+60min exclusive).
                int allowedStart = startMinutes - 10;
                int allowedEnd   = startMinutes + 60; // 1 hour after start, exclusive
                if (nowMinutes < allowedStart) {
                    return 2; // too early
                }
                if (nowMinutes >= allowedEnd) {
                    return 3; // too late — Time-In closed 1 hour after event start
                }
            } else if ("OUT".equals(type)) {
                // Allow Time Out up to 30 minutes after end
                int allowedEnd = endMinutes + 30;
                if (nowMinutes < startMinutes) {
                    return 2; // too early
                }
                if (nowMinutes > allowedEnd) {
                    return 3; // too late
                }
                // If after end but within 30 min, it's allowed but we'll note it
            }

            return 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "checkEventTimeWindow failed", e);
            return -1;
        }
    }

    private int parseTimeToMinutes(String time, int fallback) {
        if (time == null || time.isEmpty()) return fallback;
        String[] p = time.split(":");
        if (p.length < 2) return fallback;
        try {
            int h = Integer.parseInt(p[0].trim());
            int m = Integer.parseInt(p[1].trim());
            if (h < 0 || h > 23 || m < 0 || m > 59) return fallback;
            return h * 60 + m;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Convert minutes-since-midnight to "HH:mm", clamping to [00:00, 23:59]. */
    private String minutesToHHmm(int totalMinutes) {
        // Clamp to a valid 24-hour range
        totalMinutes = Math.max(0, Math.min(totalMinutes, 23 * 60 + 59));
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        return String.format(Locale.US, "%02d:%02d", h, m);
    }

    /**
     * Record a Time-In for a student: validates the submitted code against the per-student
     * attendance code (preferred) or legacy event code, inserts/updates the attendance row,
     * and marks the used code as consumed.
     *
      * Returns:
      *   0  = success (time-in recorded)
      *   1  = wrong code
      *   2  = already timed in
      *   3  = invalid time window (device clock or event not active)
      *   5  = not registered for the event
      *   -1 = error
      */
    public int submitTimeIn(int eventId, String studentId, String submittedCode) {
        return submitTimeIn(eventId, studentId, submittedCode, "", "");
    }

    public int submitTimeIn(int eventId, String studentId, String submittedCode, String deviceInfo, String ipAddress) {
        if (eventId <= 0 || studentId == null || studentId.isEmpty() || submittedCode == null || submittedCode.isEmpty()) {
            logAttendanceAttempt(eventId, studentId, "IN", "SUBMIT", -1, deviceInfo, ipAddress);
            return -1;
        }

        if (isRateLimited(eventId, studentId, "IN")) {
            logAttendanceAttempt(eventId, studentId, "IN", "SUBMIT", -4, deviceInfo, ipAddress);
            Log.w("DatabaseHelper", "Time-in blocked due to rate limit: student=" + studentId + ", event=" + eventId);
            return -4;
        }

        try {
            int timeCheck = checkEventTimeWindow(eventId, "IN");
            if (timeCheck != 0) {
                logAttendanceAttempt(eventId, studentId, "IN", "SUBMIT", 3, deviceInfo, ipAddress);
                return 3;
            }

            // Must be registered for the event before timing in
            if (!isRegistered(studentId, eventId)) {
                logAttendanceAttempt(eventId, studentId, "IN", "SUBMIT", 5, deviceInfo, ipAddress);
                return 5; // not registered
            }

            SQLiteDatabase db = this.getWritableDatabase();
            Cursor ac = db.rawQuery(
                    "SELECT " + COLUMN_ATT_TIME_IN_AT + " FROM " + TABLE_ATTENDANCE +
                    " WHERE " + COLUMN_ATT_EVENT_ID + "=? AND " + COLUMN_ATT_STUDENT_ID + "=?",
                    new String[]{String.valueOf(eventId), studentId});
            boolean alreadyIn = ac != null && ac.moveToFirst() && ac.getString(0) != null && !ac.getString(0).isEmpty();
            if (ac != null) ac.close();
            if (alreadyIn) {
                logAttendanceAttempt(eventId, studentId, "IN", "SUBMIT", 2, deviceInfo, ipAddress);
                db.close();
                return 2;
            }

            String activeCode = getActiveAttendanceCode(eventId, studentId, "IN");
            boolean codeValid = activeCode != null && activeCode.equals(submittedCode);

            if (!codeValid) {
                Cursor ec = db.rawQuery(
                        "SELECT " + COLUMN_TIME_IN_CODE + " FROM " + TABLE_EVENTS +
                                " WHERE " + COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
                String eventCode = "";
                if (ec != null && ec.moveToFirst()) {
                    eventCode = ec.getString(0);
                    ec.close();
                }
                if (eventCode != null && !eventCode.isEmpty() && eventCode.equals(submittedCode)) {
                    codeValid = true;
                }
            }

            if (!codeValid) {
                logAttendanceAttempt(eventId, studentId, "IN", "SUBMIT", 1, deviceInfo, ipAddress);
                incrementFailedAttempt(eventId, studentId, "IN");
                db.close();
                return 1;
            }

            // Use server-corrected time to prevent phone clock manipulation.
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(new java.util.Date(ServerTimeUtil.nowMillis()));

            // Compute and record the time-in window that was active at submission time.
            // Window: (start - 10min) to (start + 60min), e.g. 7:00AM → 6:50AM–7:59AM.
            String winOpen  = "";
            String winClose = "";
            try {
                Cursor wc = db.rawQuery("SELECT " + COLUMN_START_TIME + " FROM " + TABLE_EVENTS +
                        " WHERE " + COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
                if (wc != null && wc.moveToFirst()) {
                    String st = wc.getString(0);
                    wc.close();
                    int sm = parseTimeToMinutes(st, -1);
                    if (sm >= 0) {
                        winOpen  = minutesToHHmm(sm - 10);
                        winClose = minutesToHHmm(sm + 60);
                    }
                } else { if (wc != null) wc.close(); }
            } catch (Exception ignored) {}

            ContentValues av = new ContentValues();
            av.put(COLUMN_ATT_EVENT_ID,   eventId);
            av.put(COLUMN_ATT_STUDENT_ID, studentId);
            av.put(COLUMN_ATT_TIME_IN_AT, timestamp);
            av.put(COLUMN_ATT_TIME_OUT_AT, "");
            av.put(COLUMN_ATT_TIME_IN_WINDOW_OPEN,  winOpen);
            av.put(COLUMN_ATT_TIME_IN_WINDOW_CLOSE, winClose);
            db.insertWithOnConflict(TABLE_ATTENDANCE, null, av, SQLiteDatabase.CONFLICT_IGNORE);

            if (activeCode != null && activeCode.equals(submittedCode)) {
                consumeAttendanceCode(eventId, studentId, "IN", submittedCode);
            } else {
                String newCode = generateAttendanceCode();
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_TIME_IN_CODE, newCode);
                db.update(TABLE_EVENTS, cv, COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
            }

            resetFailedAttempts(eventId, studentId, "IN");
            logAttendanceAttempt(eventId, studentId, "IN", "SUBMIT", 0, deviceInfo, ipAddress);
            db.close();
            return 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "submitTimeIn failed", e);
            logAttendanceAttempt(eventId, studentId, "IN", "SUBMIT", -1, deviceInfo, ipAddress);
            return -1;
        }
    }

    public int submitTimeIn(int eventId, String studentId, String submittedCode, String deviceInfo, String ipAddress, String photoBase64) {
        int result = submitTimeIn(eventId, studentId, submittedCode, deviceInfo, ipAddress);
        if (result == 0 && photoBase64 != null && !photoBase64.isEmpty()) {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_ATT_TIME_IN_PHOTO, photoBase64);
                db.update(TABLE_ATTENDANCE, cv,
                        COLUMN_ATT_EVENT_ID + "=? AND " + COLUMN_ATT_STUDENT_ID + "=?",
                        new String[]{String.valueOf(eventId), studentId});
                db.close();
            } catch (Exception e) {
                Log.e("DatabaseHelper", "submitTimeIn photo save failed", e);
            }
        }
        return result;
    }

    /**
     * Record a Time-Out for a student: validates the submitted code against the per-student
     * attendance code (preferred) or legacy event code, updates the attendance row,
     * and marks the used code as consumed.
     *
     * Returns:
     *   0  = success (time-out recorded)
     *   1  = wrong code
     *   2  = not yet timed in
     *   3  = already timed out
     *   4  = time window violation (too early or window closed)
     *   -1 = error
     *   -4 = rate limited
     */
    public int submitTimeOut(int eventId, String studentId, String submittedCode) {
        return submitTimeOut(eventId, studentId, submittedCode, "", "");
    }

    public int submitTimeOut(int eventId, String studentId, String submittedCode, String deviceInfo, String ipAddress) {
        if (eventId <= 0 || studentId == null || studentId.isEmpty() || submittedCode == null || submittedCode.isEmpty()) {
            logAttendanceAttempt(eventId, studentId, "OUT", "SUBMIT", -1, deviceInfo, ipAddress);
            return -1;
        }

        if (isRateLimited(eventId, studentId, "OUT")) {
            logAttendanceAttempt(eventId, studentId, "OUT", "SUBMIT", -4, deviceInfo, ipAddress);
            Log.w("DatabaseHelper", "Time-out blocked due to rate limit: student=" + studentId + ", event=" + eventId);
            return -4;
        }

        try {
            int timeCheck = checkEventTimeWindow(eventId, "OUT");
            if (timeCheck != 0) {
                logAttendanceAttempt(eventId, studentId, "OUT", "SUBMIT", 4, deviceInfo, ipAddress);
                return 4; // time window violation (distinct from 3 = already timed out)
            }

            SQLiteDatabase db = this.getWritableDatabase();

            Cursor ac = db.rawQuery(
                    "SELECT " + COLUMN_ATT_TIME_IN_AT + ", " + COLUMN_ATT_TIME_OUT_AT +
                    " FROM " + TABLE_ATTENDANCE +
                    " WHERE " + COLUMN_ATT_EVENT_ID + "=? AND " + COLUMN_ATT_STUDENT_ID + "=?",
                    new String[]{String.valueOf(eventId), studentId});
            boolean hasTimeIn = false;
            boolean alreadyOut = false;
            if (ac != null && ac.moveToFirst()) {
                String ti = ac.getString(0);
                String to = ac.getString(1);
                hasTimeIn  = ti != null && !ti.isEmpty();
                alreadyOut = to != null && !to.isEmpty();
                ac.close();
            }
            if (!hasTimeIn)  {
                logAttendanceAttempt(eventId, studentId, "OUT", "SUBMIT", 2, deviceInfo, ipAddress);
                db.close(); return 2;
            }
            if (alreadyOut) {
                logAttendanceAttempt(eventId, studentId, "OUT", "SUBMIT", 3, deviceInfo, ipAddress);
                db.close(); return 3;
            }

            String activeCode = getActiveAttendanceCode(eventId, studentId, "OUT");
            boolean codeValid = activeCode != null && activeCode.equals(submittedCode);

            if (!codeValid) {
                Cursor ec = db.rawQuery(
                        "SELECT " + COLUMN_TIME_OUT_CODE + " FROM " + TABLE_EVENTS +
                                " WHERE " + COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
                String eventCode = "";
                if (ec != null && ec.moveToFirst()) {
                    eventCode = ec.getString(0);
                    ec.close();
                }
                if (eventCode != null && !eventCode.isEmpty() && eventCode.equals(submittedCode)) {
                    codeValid = true;
                }
            }

            if (!codeValid) {
                logAttendanceAttempt(eventId, studentId, "OUT", "SUBMIT", 1, deviceInfo, ipAddress);
                incrementFailedAttempt(eventId, studentId, "OUT");
                db.close();
                return 1;
            }

            // Use server-corrected time to prevent phone clock manipulation.
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(new java.util.Date(ServerTimeUtil.nowMillis()));

            // Compute and record the time-out window that was active at submission time.
            // Window: start_time to (end_time + 30min).
            String winOpen  = "";
            String winClose = "";
            try {
                Cursor wc = db.rawQuery(
                        "SELECT " + COLUMN_START_TIME + ", " + COLUMN_END_TIME +
                        " FROM " + TABLE_EVENTS + " WHERE " + COLUMN_ID + "=?",
                        new String[]{String.valueOf(eventId)});
                if (wc != null && wc.moveToFirst()) {
                    String st = wc.getString(0);
                    String et = wc.getString(1);
                    wc.close();
                    int sm = parseTimeToMinutes(st, -1);
                    int em = parseTimeToMinutes(et, -1);
                    if (sm >= 0) winOpen  = minutesToHHmm(sm);
                    if (em >= 0) winClose = minutesToHHmm(em + 30);
                } else { if (wc != null) wc.close(); }
            } catch (Exception ignored) {}

            ContentValues av = new ContentValues();
            av.put(COLUMN_ATT_TIME_OUT_AT, timestamp);
            av.put(COLUMN_ATT_TIME_OUT_WINDOW_OPEN,  winOpen);
            av.put(COLUMN_ATT_TIME_OUT_WINDOW_CLOSE, winClose);
            db.update(TABLE_ATTENDANCE, av,
                    COLUMN_ATT_EVENT_ID + "=? AND " + COLUMN_ATT_STUDENT_ID + "=?",
                    new String[]{String.valueOf(eventId), studentId});

            if (activeCode != null && activeCode.equals(submittedCode)) {
                consumeAttendanceCode(eventId, studentId, "OUT", submittedCode);
            } else {
                String newCode = generateAttendanceCode();
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_TIME_OUT_CODE, newCode);
                db.update(TABLE_EVENTS, cv, COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
            }

            resetFailedAttempts(eventId, studentId, "OUT");
            logAttendanceAttempt(eventId, studentId, "OUT", "SUBMIT", 0, deviceInfo, ipAddress);
            db.close();
            return 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "submitTimeOut failed", e);
            logAttendanceAttempt(eventId, studentId, "OUT", "SUBMIT", -1, deviceInfo, ipAddress);
            return -1;
        }
    }

    public int submitTimeOut(int eventId, String studentId, String submittedCode, String deviceInfo, String ipAddress, String photoBase64) {
        int result = submitTimeOut(eventId, studentId, submittedCode, deviceInfo, ipAddress);
        if (result == 0 && photoBase64 != null && !photoBase64.isEmpty()) {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(COLUMN_ATT_TIME_OUT_PHOTO, photoBase64);
                db.update(TABLE_ATTENDANCE, cv,
                        COLUMN_ATT_EVENT_ID + "=? AND " + COLUMN_ATT_STUDENT_ID + "=?",
                        new String[]{String.valueOf(eventId), studentId});
                db.close();
            } catch (Exception e) {
                Log.e("DatabaseHelper", "submitTimeOut photo save failed", e);
            }
        }
        return result;
    }

    /**
     * Returns the attendance record for a student at an event, or null if none.
     * Returns a String[4]: [0] = time_in_at, [1] = time_out_at, [2] = time_in_photo, [3] = time_out_photo
     * (may be empty strings if not set).
     */
    public String[] getAttendanceRecord(int eventId, String studentId) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT " + COLUMN_ATT_TIME_IN_AT + ", " + COLUMN_ATT_TIME_OUT_AT + ", " +
                    COLUMN_ATT_TIME_IN_PHOTO + ", " + COLUMN_ATT_TIME_OUT_PHOTO +
                    " FROM " + TABLE_ATTENDANCE +
                    " WHERE " + COLUMN_ATT_EVENT_ID + "=? AND " + COLUMN_ATT_STUDENT_ID + "=?",
                    new String[]{String.valueOf(eventId), studentId});
            if (c != null && c.moveToFirst()) {
                String ti = c.getString(0); if (ti == null) ti = "";
                String to = c.getString(1); if (to == null) to = "";
                String tiPhoto = c.getString(2); if (tiPhoto == null) tiPhoto = "";
                String toPhoto = c.getString(3); if (toPhoto == null) toPhoto = "";
                c.close();
                return new String[]{ti, to, tiPhoto, toPhoto};
            }
            if (c != null) c.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getAttendanceRecord failed", e);
        }
        return null;
    }

    /**
     * Returns the count of students who have timed in for an event.
     */
    public int getAttendanceCount(int eventId) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_ATTENDANCE +
                    " WHERE " + COLUMN_ATT_EVENT_ID + "=? AND " +
                    COLUMN_ATT_TIME_IN_AT + " != ''",
                    new String[]{String.valueOf(eventId)});
            int cnt = 0;
            if (c != null && c.moveToFirst()) { cnt = c.getInt(0); c.close(); }
            return cnt;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getAttendanceCount failed", e);
            return 0;
        }
    }

    // ── Sync helpers (called by SyncManager to import Firestore data) ─────────

    /**
     * Upsert a user row from Firestore.
     *
     * Strategy:
     *   - If the user already exists locally → UPDATE only non-sensitive profile fields
     *     (name, email, role, department, gender, mobile, profileImage, notifPref).
     *     The local password and email_verified flag are NEVER overwritten by sync.
     *   - If the user does not exist locally (fresh install / new device) → INSERT the
     *     full row using the Firestore password so the user can log in immediately.
     *
     * This prevents sync from ever wiping a locally-stored BCrypt password, which was
     * the root cause of "invalid credentials" after a Firestore sync.
     */
    public void syncUpsertUser(String studentId, String name, String email,
                                String role, String department,
                                String gender, String mobile,
                                String profileImage, String notifPref,
                                String firestorePassword, boolean emailVerified,
                                String firebaseUid) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            // Check whether this user already exists locally
            Cursor c = db.rawQuery("SELECT " + COLUMN_USER_PASSWORD + ", " + COLUMN_USER_EMAIL_VERIFIED +
                    " FROM " + TABLE_USERS + " WHERE " + COLUMN_USER_STUDENT_ID + "=?",
                    new String[]{studentId});
            boolean userExists = (c != null && c.moveToFirst());
            String localPassword = userExists ? c.getString(0) : null;
            boolean localEmailVerified = userExists ? (c.getInt(1) == 1) : emailVerified;
            if (c != null) c.close();

            if (userExists) {
                // UPDATE: never touch password or email_verified.
                // Also never overwrite a locally-stored profile_image with an empty Firestore value —
                // profile images are absolute device file paths that only exist on this device.
                ContentValues v = new ContentValues();
                v.put(COLUMN_USER_NAME,       name);
                v.put(COLUMN_USER_EMAIL,      email);
                v.put(COLUMN_USER_ROLE,       role);
                v.put(COLUMN_USER_DEPARTMENT, department);
                v.put(COLUMN_USER_GENDER,     gender    != null ? gender    : "");
                v.put(COLUMN_USER_MOBILE,     mobile    != null ? mobile    : "");
                v.put(COLUMN_USER_NOTIF_PREF, notifPref != null ? notifPref : "All Events");
                // Store/update the Firebase UID if we received one
                if (firebaseUid != null && !firebaseUid.isEmpty()) {
                    v.put(COLUMN_USER_FIREBASE_UID, firebaseUid);
                }
                // Only update profile_image from Firestore when Firestore has a real value.
                // For data:image/ Base64 URIs and https:// URLs: always accept — device-independent.
                // "DELETED" is a sentinel meaning the user explicitly removed their photo.
                // For legacy local absolute paths: only use them if the file still exists on this device.
                if ("DELETED".equals(profileImage)) {
                    // Explicit delete — clear the image on this device too
                    v.put(COLUMN_USER_PROFILE_IMG, "");
                } else if (profileImage != null && !profileImage.isEmpty()) {
                    if (profileImage.startsWith("data:image/")
                            || profileImage.startsWith("http://")
                            || profileImage.startsWith("https://")) {
                        // Base64 data-URI or network URL — valid on any device
                        v.put(COLUMN_USER_PROFILE_IMG, profileImage);
                    } else if (profileImage.startsWith("/") && !new java.io.File(profileImage).exists()) {
                        // Stale local path — file was deleted or path is from another device; skip
                    } else {
                        v.put(COLUMN_USER_PROFILE_IMG, profileImage);
                    }
                }
                db.update(TABLE_USERS, v, COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
            } else {
                // INSERT: new user on this device — use Firestore password as seed
                String passwordToStore = (firestorePassword != null && !firestorePassword.isEmpty())
                        ? firestorePassword : "";
                ContentValues v = new ContentValues();
                v.put(COLUMN_USER_STUDENT_ID,     studentId);
                v.put(COLUMN_USER_NAME,           name);
                v.put(COLUMN_USER_EMAIL,          email);
                v.put(COLUMN_USER_PASSWORD,       passwordToStore);
                v.put(COLUMN_USER_ROLE,           role);
                v.put(COLUMN_USER_DEPARTMENT,     department);
                v.put(COLUMN_USER_GENDER,         gender       != null ? gender       : "");
                v.put(COLUMN_USER_MOBILE,         mobile       != null ? mobile       : "");
                v.put(COLUMN_USER_PROFILE_IMG,    profileImage != null ? profileImage : "");
                v.put(COLUMN_USER_NOTIF_PREF,     notifPref    != null ? notifPref    : "All Events");
                v.put(COLUMN_USER_EMAIL_VERIFIED, localEmailVerified ? 1 : 0);
                v.put(COLUMN_USER_FIREBASE_UID,   firebaseUid  != null ? firebaseUid  : "");
                db.insertWithOnConflict(TABLE_USERS, null, v, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "syncUpsertUser failed", e);
        }
    }

    /**
     * Upsert an event row from Firestore. localId is the Firestore doc id (= original SQLite id).
     * For new rows: full INSERT (venue/is_hidden get defaults; time_in_code/time_out_code
     *   are seeded from Firestore so codes are available immediately on first install / new device).
     * For existing rows: UPDATE all Firestore-owned columns including the attendance codes.
     *   venue and is_hidden remain local-only and are never touched.
     */
    public void syncUpsertEvent(int localId, String title, String description,
                                 String date, String time, String tags,
                                 String organizer, String category,
                                 String imagePath, String status, String creatorSid,
                                 String startTime, String endTime,
                                 String timeInCode, String timeOutCode) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            // Check whether this event already exists locally.
            boolean exists = false;
            Cursor chk = db.rawQuery(
                    "SELECT 1 FROM " + TABLE_EVENTS + " WHERE " + COLUMN_ID + "=?",
                    new String[]{String.valueOf(localId)});
            if (chk != null) {
                exists = chk.moveToFirst();
                chk.close();
            }

            if (!exists) {
                // New event: insert with defaults for local-only columns.
                ContentValues v = new ContentValues();
                v.put(COLUMN_ID,           localId);
                v.put(COLUMN_TITLE,        title);
                v.put(COLUMN_DESC,         description);
                v.put(COLUMN_DATE,         date);
                v.put(COLUMN_EVENT_TIME,   time       != null ? time       : "");
                v.put(COLUMN_START_TIME,   startTime  != null ? startTime  : "");
                v.put(COLUMN_END_TIME,     endTime    != null ? endTime    : "");
                v.put(COLUMN_TAGS,         tags       != null ? tags       : "");
                v.put(COLUMN_ORGANIZER,    organizer);
                v.put(COLUMN_CATEGORY,     category);
                v.put(COLUMN_IMAGE_PATH,   imagePath  != null ? imagePath  : "");
                v.put(COLUMN_STATUS,       status);
                v.put(COLUMN_CREATOR_SID,  creatorSid != null ? creatorSid : "");
                v.put(COLUMN_TIME_IN_CODE,  timeInCode  != null ? timeInCode  : "");
                v.put(COLUMN_TIME_OUT_CODE, timeOutCode != null ? timeOutCode : "");
                // venue, is_hidden use column DEFAULT values
                db.insertWithOnConflict(TABLE_EVENTS, null, v, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                // Existing event: update Firestore-owned columns including attendance codes.
                // Do NOT touch: venue, is_hidden.
                ContentValues v = new ContentValues();
                v.put(COLUMN_TITLE,        title);
                v.put(COLUMN_DESC,         description);
                v.put(COLUMN_DATE,         date);
                v.put(COLUMN_EVENT_TIME,   time       != null ? time       : "");
                v.put(COLUMN_START_TIME,   startTime  != null ? startTime  : "");
                v.put(COLUMN_END_TIME,     endTime    != null ? endTime    : "");
                v.put(COLUMN_TAGS,         tags       != null ? tags       : "");
                v.put(COLUMN_ORGANIZER,    organizer);
                v.put(COLUMN_CATEGORY,     category);
                v.put(COLUMN_IMAGE_PATH,   imagePath  != null ? imagePath  : "");
                v.put(COLUMN_STATUS,       status);
                v.put(COLUMN_CREATOR_SID,  creatorSid != null ? creatorSid : "");
                v.put(COLUMN_TIME_IN_CODE,  timeInCode  != null ? timeInCode  : "");
                v.put(COLUMN_TIME_OUT_CODE, timeOutCode != null ? timeOutCode : "");
                db.update(TABLE_EVENTS, v, COLUMN_ID + "=?",
                        new String[]{String.valueOf(localId)});
            }

            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "syncUpsertEvent failed", e);
        }
    }

    // Keep old overload for any existing callers (no codes)
    public void syncUpsertEvent(int localId, String title, String description,
                                 String date, String time, String tags,
                                 String organizer, String category,
                                 String imagePath, String status, String creatorSid,
                                 String startTime, String endTime) {
        syncUpsertEvent(localId, title, description, date, time, tags, organizer, category,
                imagePath, status, creatorSid, startTime, endTime, null, null);
    }

    /**
     * Upsert a registration row from Firestore.
     */
    public void syncUpsertRegistration(String studentId, int eventId, String timestamp) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_REG_STUDENT_ID, studentId);
            v.put(COLUMN_REG_EVENT_ID,   eventId);
            v.put(COLUMN_REG_TIMESTAMP,  timestamp != null ? timestamp : "");
            db.insertWithOnConflict(TABLE_REGISTRATIONS, null, v, SQLiteDatabase.CONFLICT_REPLACE);
            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "syncUpsertRegistration failed", e);
        }
    }

    /**
     * Upsert a notification row from Firestore.
     * For new rows: full INSERT (is_read/is_archived/archived_at get defaults of 0/"").
     * For existing rows: UPDATE only Firestore-owned columns;
     *   is_read, is_archived, archived_at are local-only and are never touched.
     * NOTE: the {@code isRead} parameter is kept for API compatibility but is only used
     * when inserting a brand-new row — existing rows keep their local read state.
     */
    public void syncUpsertNotification(int localNotifId, String recipientSid, int eventId,
                                        String type, String message, String reason,
                                        String suggestedDate, String suggestedTime,
                                        String instructions,
                                        int isRead, String createdAt) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            // Check whether this notification already exists locally.
            boolean exists = false;
            Cursor chk = db.rawQuery(
                    "SELECT 1 FROM " + TABLE_NOTIFICATIONS + " WHERE " + COLUMN_NOTIF_ID + "=?",
                    new String[]{String.valueOf(localNotifId)});
            if (chk != null) {
                exists = chk.moveToFirst();
                chk.close();
            }

            if (!exists) {
                // New notification: insert; is_read comes from Firestore, is_archived defaults to 0.
                ContentValues v = new ContentValues();
                v.put(COLUMN_NOTIF_ID,             localNotifId);
                v.put(COLUMN_NOTIF_RECIPIENT_SID,  recipientSid);
                v.put(COLUMN_NOTIF_EVENT_ID,       eventId);
                v.put(COLUMN_NOTIF_TYPE,           type);
                v.put(COLUMN_NOTIF_MESSAGE,        message);
                v.put(COLUMN_NOTIF_REASON,         reason        != null ? reason        : "");
                v.put(COLUMN_NOTIF_SUGGESTED_DATE, suggestedDate != null ? suggestedDate : "");
                v.put(COLUMN_NOTIF_SUGGESTED_TIME, suggestedTime != null ? suggestedTime : "");
                v.put(COLUMN_NOTIF_INSTRUCTIONS,   instructions  != null ? instructions  : "");
                v.put(COLUMN_NOTIF_IS_READ,        isRead);
                v.put(COLUMN_NOTIF_CREATED_AT,     createdAt     != null ? createdAt     : "");
                // is_archived and archived_at use column DEFAULT values (0 / "")
                db.insertWithOnConflict(TABLE_NOTIFICATIONS, null, v, SQLiteDatabase.CONFLICT_IGNORE);
            } else {
                // Existing notification: update Firestore-owned columns only.
                // Do NOT touch: is_read, is_archived, archived_at.
                ContentValues v = new ContentValues();
                v.put(COLUMN_NOTIF_TYPE,           type);
                v.put(COLUMN_NOTIF_MESSAGE,        message);
                v.put(COLUMN_NOTIF_REASON,         reason        != null ? reason        : "");
                v.put(COLUMN_NOTIF_SUGGESTED_DATE, suggestedDate != null ? suggestedDate : "");
                v.put(COLUMN_NOTIF_SUGGESTED_TIME, suggestedTime != null ? suggestedTime : "");
                v.put(COLUMN_NOTIF_INSTRUCTIONS,   instructions  != null ? instructions  : "");
                v.put(COLUMN_NOTIF_CREATED_AT,     createdAt     != null ? createdAt     : "");
                db.update(TABLE_NOTIFICATIONS, v, COLUMN_NOTIF_ID + "=?",
                        new String[]{String.valueOf(localNotifId)});
            }

            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "syncUpsertNotification failed", e);
        }
    }

    // ── Event Operations ──────────────────────────────────────────────────────

    public long addEvent(Event event) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_TITLE, event.getTitle());
        v.put(COLUMN_DESC, event.getDescription());
        v.put(COLUMN_DATE, event.getDate());
        v.put(COLUMN_EVENT_TIME, event.getTime());
        v.put(COLUMN_START_TIME, event.getStartTime());
        v.put(COLUMN_END_TIME, event.getEndTime());
        v.put(COLUMN_TAGS, event.getTags());
        v.put(COLUMN_ORGANIZER, event.getOrganizer());
        v.put(COLUMN_CATEGORY, event.getCategory());
        v.put(COLUMN_IMAGE_PATH, event.getImagePath());
        v.put(COLUMN_STATUS, event.getStatus());
        v.put(COLUMN_CREATOR_SID, event.getCreatorSid() != null ? event.getCreatorSid() : "");
        v.put(COLUMN_VENUE, event.getVenue() != null ? event.getVenue() : "");
        long id = db.insert(TABLE_EVENTS, null, v);
        db.close();
        if (id != -1) {
            new FirestoreHelper().upsertEvent((int) id,
                    event.getTitle(), event.getDescription(), event.getDate(),
                    event.getTime(), event.getTags(), event.getOrganizer(),
                    event.getCategory(), event.getImagePath(), event.getStatus(),
                    event.getCreatorSid(), event.getStartTime(), event.getEndTime());

            // Notify admin that a new event is awaiting approval
            if ("PENDING".equals(event.getStatus())) {
                String timeStr = (event.getTime() != null && !event.getTime().isEmpty())
                        ? " at " + event.getTime() : "";
                String adminMsg = "\uD83D\uDD14 New event pending approval: \u201c" + event.getTitle() + "\u201d\n"
                        + "Submitted by: " + event.getOrganizer() + "\n"
                        + "Date: " + event.getDate() + timeStr + "\n"
                        + "Open the Pending Approvals screen to review.";
                // "admin" is the fixed sentinel recipient_sid for the hardcoded admin account
                insertNotification("admin", (int) id, "NEW_PENDING", adminMsg, "", "", "", "");
            }
        }
        return id;
    }

    public void approveEvent(int eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_STATUS, "APPROVED");
        db.update(TABLE_EVENTS, v, COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
        db.close();
        new FirestoreHelper().updateEventStatus(eventId, "APPROVED");
    }

    public void deleteEvent(int eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Cascade-delete registrations and notifications tied to this event
            db.delete(TABLE_REGISTRATIONS, COLUMN_REG_EVENT_ID + "=?", new String[]{String.valueOf(eventId)});
            db.delete(TABLE_NOTIFICATIONS, COLUMN_NOTIF_EVENT_ID + "=?", new String[]{String.valueOf(eventId)});
            db.delete(TABLE_EVENTS, COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "deleteEvent SQLite failed", e);
        } finally {
            db.endTransaction();
        }
        // Mirror all three deletions to Firestore
        FirestoreHelper fs = new FirestoreHelper();
        fs.deleteEventRegistrations(eventId);
        fs.deleteEventNotifications(eventId);
        fs.deleteEvent(eventId);
    }

    public void cancelEvent(int eventId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_STATUS, "CANCELLED");
            db.update(TABLE_EVENTS, v, COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
            db.close();
            new FirestoreHelper().updateEventStatus(eventId, "CANCELLED");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "cancelEvent failed", e);
        }
    }

    public void postponeEvent(int eventId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_STATUS, "POSTPONED");
            db.update(TABLE_EVENTS, v, COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
            db.close();
            new FirestoreHelper().updateEventStatus(eventId, "POSTPONED");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "postponeEvent failed", e);
        }
    }

    public void startEvent(int eventId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_STATUS, "HAPPENING");
            db.update(TABLE_EVENTS, v, COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
            db.close();
            new FirestoreHelper().updateEventStatus(eventId, "HAPPENING");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "startEvent failed", e);
        }
    }

    public void endEvent(int eventId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_STATUS, "ENDED");
            db.update(TABLE_EVENTS, v, COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
            db.close();
            new FirestoreHelper().updateEventStatus(eventId, "ENDED");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "endEvent failed", e);
        }
    }

    /**
     * Officer confirms the admin's suggested date.
     * Sets the event's date to the given date and restores status to APPROVED.
     */
    public boolean proposeNewDate(int eventId, String newDate) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_DATE,   newDate);
            v.put(COLUMN_STATUS, "APPROVED");
            int rows = db.update(TABLE_EVENTS, v, COLUMN_ID + "=?",
                    new String[]{String.valueOf(eventId)});
            db.close();
            if (rows > 0) {
                new FirestoreHelper().updateEventDateAndStatus(eventId, newDate, "APPROVED");
            }
            return rows > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "proposeNewDate failed", e);
            return false;
        }
    }

    /**
     * Officer confirms the admin's suggested date and time.
     * Sets the event's date+time and sets status to PENDING for admin re-approval.
     */
    public boolean proposeNewDateTimePending(int eventId, String newDate, String newTime) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_DATE,       newDate);
            v.put(COLUMN_EVENT_TIME, newTime != null ? newTime : "");
            v.put(COLUMN_STATUS,     "PENDING");
            int rows = db.update(TABLE_EVENTS, v, COLUMN_ID + "=?",
                    new String[]{String.valueOf(eventId)});
            db.close();
            if (rows > 0) {
                new FirestoreHelper().updateEventDateTimeAndStatus(eventId, newDate, newTime, "PENDING");
            }
            return rows > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "proposeNewDateTimePending failed", e);
            return false;
        }
    }

    /**
     * Returns the student_ids of all Admin-role users.
     * Used to notify every admin when an officer proposes a different reschedule date.
     */
    public List<String> getAdminStudentIds() {
        List<String> ids = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT " + COLUMN_USER_STUDENT_ID + " FROM " + TABLE_USERS +
                    " WHERE " + COLUMN_USER_ROLE + " = 'Admin'", null);
            if (c != null && c.moveToFirst()) {
                do { ids.add(c.getString(0)); } while (c.moveToNext());
                c.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getAdminStudentIds failed", e);
        }
        return ids;
    }

    /**
     * Updates editable event fields (title, description, date, time, tags, organizer, category).
     * Does NOT change status or creator_sid.
     */
    public boolean updateEvent(int eventId, String title, String description,
                               String date, String time, String tags,
                               String organizer, String category, String venue) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_TITLE,      title);
            v.put(COLUMN_DESC,       description);
            v.put(COLUMN_DATE,       date);
            v.put(COLUMN_EVENT_TIME, time);
            v.put(COLUMN_TAGS,       tags);
            v.put(COLUMN_ORGANIZER,  organizer);
            v.put(COLUMN_CATEGORY,   category);
            v.put(COLUMN_VENUE,      venue != null ? venue : "");
            int rows = db.update(TABLE_EVENTS, v, COLUMN_ID + "=?",
                    new String[]{String.valueOf(eventId)});
            db.close();
            if (rows > 0) {
                new FirestoreHelper().updateEventFields(eventId, title, description,
                        date, time, tags, organizer, category);
            }
            return rows > 0;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "updateEvent failed", e);
            return false;
        }
    }

    /**
     * Returns a single Event by its local DB id, or null if not found.
     */
    public Event getEventById(int id) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT * FROM " + TABLE_EVENTS + " WHERE " + COLUMN_ID + "=?",
                    new String[]{String.valueOf(id)});
            if (c != null && c.moveToFirst()) {
                Event e = eventFromCursor(c);
                c.close();
                return e;
            }
            if (c != null) c.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getEventById failed", e);
        }
        return null;
    }

    public List<Event> getEventsByStatus(String status) {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + TABLE_EVENTS + " WHERE " + COLUMN_STATUS +
                "=? ORDER BY " + COLUMN_ID + " DESC", new String[]{status});
        if (c != null && c.moveToFirst()) {
            do { events.add(eventFromCursor(c)); } while (c.moveToNext());
            c.close();
        }
        return events;
    }

    /**
     * Returns all non-PENDING, non-hidden events for the student browse list.
     * Only returns events with date >= today (upcoming events only).
     * Uses server-corrected time to prevent device clock manipulation.
     */
    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            String today = ServerTimeUtil.todayString();
            Cursor c = db.rawQuery(
                    "SELECT * FROM " + TABLE_EVENTS +
                    " WHERE " + COLUMN_STATUS + " != 'PENDING'" +
                    " AND (IFNULL(" + COLUMN_IS_HIDDEN + ",0) = 0)" +
                    " AND " + COLUMN_DATE + " >= ?" +
                    " ORDER BY " + COLUMN_DATE + " ASC", new String[]{today});
            if (c != null && c.moveToFirst()) {
                do { events.add(eventFromCursor(c)); } while (c.moveToNext());
                c.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getAllEvents failed", e);
        }
        return events;
    }

    /**
     * Returns ALL events regardless of status. Used by admin Event Control.
     */
    public List<Event> getAllEventsForAdmin() {
        List<Event> events = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT * FROM " + TABLE_EVENTS + " ORDER BY " + COLUMN_DATE + " DESC", null);
            if (c != null && c.moveToFirst()) {
                do { events.add(eventFromCursor(c)); } while (c.moveToNext());
                c.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getAllEventsForAdmin failed", e);
        }
        return events;
    }

    // ── Officer Queries ───────────────────────────────────────────────────────

    public List<Event> getEventsByOfficer(String officerName) {
        List<Event> events = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT * FROM " + TABLE_EVENTS +
                    " WHERE " + COLUMN_ORGANIZER + " LIKE ?" +
                    " ORDER BY " + COLUMN_DATE + " ASC",
                    new String[]{"%" + officerName + "%"});
            if (c != null && c.moveToFirst()) {
                do { events.add(eventFromCursor(c)); } while (c.moveToNext());
                c.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getEventsByOfficer failed", e);
        }
        return events;
    }

    public List<Event> getEventsByCreatorSid(String creatorSid) {
        List<Event> events = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT * FROM " + TABLE_EVENTS +
                    " WHERE " + COLUMN_CREATOR_SID + " = ?" +
                    " ORDER BY " + COLUMN_DATE + " ASC",
                    new String[]{creatorSid});
            if (c != null && c.moveToFirst()) {
                do { events.add(eventFromCursor(c)); } while (c.moveToNext());
                c.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getEventsByCreatorSid failed", e);
        }
        return events;
    }

    public Map<String, Integer> getEventRegistrationStats(int eventId) {
        Map<String, Integer> stats = new HashMap<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT u." + COLUMN_USER_DEPARTMENT + ", COUNT(*) as cnt" +
                    " FROM " + TABLE_REGISTRATIONS + " r" +
                    " INNER JOIN " + TABLE_USERS + " u ON r." + COLUMN_REG_STUDENT_ID +
                    " = u." + COLUMN_USER_STUDENT_ID +
                    " WHERE r." + COLUMN_REG_EVENT_ID + "=?" +
                    " GROUP BY u." + COLUMN_USER_DEPARTMENT,
                    new String[]{String.valueOf(eventId)});
            if (c != null && c.moveToFirst()) {
                do {
                    String dept = c.getString(0);
                    int cnt = c.getInt(1);
                    if (dept == null || dept.isEmpty()) dept = "Unknown";
                    stats.put(dept, cnt);
                } while (c.moveToNext());
                c.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getEventRegistrationStats failed", e);
        }
        return stats;
    }

    public int getTotalRegistrationsForOfficer(String officerName) {
        int total = 0;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_REGISTRATIONS + " r" +
                    " INNER JOIN " + TABLE_EVENTS + " e ON r." + COLUMN_REG_EVENT_ID +
                    " = e." + COLUMN_ID +
                    " WHERE e." + COLUMN_ORGANIZER + " LIKE ?",
                    new String[]{"%" + officerName + "%"});
            if (c != null && c.moveToFirst()) { total = c.getInt(0); c.close(); }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getTotalRegistrationsForOfficer failed", e);
        }
        return total;
    }

    /**
     * Safe overload — uses the already-open db. Call this from onOpen().
     */
    public void deleteEndedEventsOlderThan(SQLiteDatabase db, int days) {
        try {
            String cutoff = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date(System.currentTimeMillis() - (long) days * 24 * 60 * 60 * 1000));
            // Collect IDs before deleting so we can mirror to Firestore
            List<Integer> ids = new ArrayList<>();
            Cursor c = db.query(TABLE_EVENTS, new String[]{COLUMN_ID},
                    COLUMN_DATE + " < ?", new String[]{cutoff}, null, null, null);
            if (c != null) {
                while (c.moveToNext()) ids.add(c.getInt(0));
                c.close();
            }
            db.delete(TABLE_EVENTS, COLUMN_DATE + " < ?", new String[]{cutoff});
            // Mirror deletions to Firestore (fire-and-forget)
            FirestoreHelper fs = new FirestoreHelper();
            for (int id : ids) fs.deleteEvent(id);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "deleteEndedEventsOlderThan(db) failed", e);
        }
    }

    /**
     * Convenience overload — opens its own writable DB. Do NOT call from onOpen().
     */
    public void deleteEndedEventsOlderThan(int days) {
        try {
            String cutoff = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(new Date(System.currentTimeMillis() - (long) days * 24 * 60 * 60 * 1000));
            SQLiteDatabase db = this.getWritableDatabase();
            // Collect IDs before deleting so we can mirror to Firestore
            List<Integer> ids = new ArrayList<>();
            Cursor c = db.query(TABLE_EVENTS, new String[]{COLUMN_ID},
                    COLUMN_DATE + " < ?", new String[]{cutoff}, null, null, null);
            if (c != null) {
                while (c.moveToNext()) ids.add(c.getInt(0));
                c.close();
            }
            db.delete(TABLE_EVENTS, COLUMN_DATE + " < ?", new String[]{cutoff});
            // Mirror deletions to Firestore (fire-and-forget)
            FirestoreHelper fs = new FirestoreHelper();
            for (int id : ids) fs.deleteEvent(id);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "deleteEndedEventsOlderThan failed", e);
        }
    }

    // ── Notification Operations ───────────────────────────────────────────────

    /**
     * Parses organizer string (e.g. "Alberto – CLAS") and looks up the officer's student_id.
     */
    public String getOfficerStudentIdFromOrganizer(String organizer) {
        if (organizer == null || organizer.isEmpty()) return null;
        try {
            String officerName = organizer.split("[\u2013\\-]")[0].trim();
            if (officerName.isEmpty()) return null;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT " + COLUMN_USER_STUDENT_ID + " FROM " + TABLE_USERS +
                    " WHERE " + COLUMN_USER_NAME + " LIKE ? AND " +
                    COLUMN_USER_ROLE + " = 'Officer' LIMIT 1",
                    new String[]{"%" + officerName + "%"});
            String sid = null;
            if (c != null && c.moveToFirst()) { sid = c.getString(0); c.close(); }
            return sid;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getOfficerStudentIdFromOrganizer failed", e);
            return null;
        }
    }

    public long insertNotification(String recipientSid, int eventId, String type,
                                   String message, String reason,
                                   String suggestedDate, String suggestedTime,
                                   String instructions) {
        try {
            String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date());
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_NOTIF_RECIPIENT_SID, recipientSid);
            v.put(COLUMN_NOTIF_EVENT_ID, eventId);
            v.put(COLUMN_NOTIF_TYPE, type);
            v.put(COLUMN_NOTIF_MESSAGE, message);
            v.put(COLUMN_NOTIF_REASON, reason);
            v.put(COLUMN_NOTIF_SUGGESTED_DATE, suggestedDate != null ? suggestedDate : "");
            v.put(COLUMN_NOTIF_SUGGESTED_TIME, suggestedTime != null ? suggestedTime : "");
            v.put(COLUMN_NOTIF_INSTRUCTIONS, instructions != null ? instructions : "");
            v.put(COLUMN_NOTIF_IS_READ, 0);
            v.put(COLUMN_NOTIF_CREATED_AT, createdAt);
            long id = db.insert(TABLE_NOTIFICATIONS, null, v);
            db.close();
            if (id != -1) {
                new FirestoreHelper().upsertNotification((int) id, recipientSid, eventId,
                        type, message, reason, suggestedDate, suggestedTime, instructions,
                        false, createdAt);
                // Send FCM push notification to the recipient's device
                FcmSender.send(mContext, recipientSid,
                        type, FcmSender.pickTitle(type), message);
            }
            return id;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "insertNotification failed", e);
            return -1;
        }
    }

    public List<NotifModel> getNotificationsForUser(String studentId) {
        List<NotifModel> list = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT * FROM " + TABLE_NOTIFICATIONS +
                    " WHERE " + COLUMN_NOTIF_RECIPIENT_SID + "=?" +
                    " AND (" + COLUMN_NOTIF_IS_ARCHIVED + "=0 OR " + COLUMN_NOTIF_IS_ARCHIVED + " IS NULL)" +
                    " ORDER BY " + COLUMN_NOTIF_ID + " DESC",
                    new String[]{studentId});
            if (c != null && c.moveToFirst()) {
                do { list.add(notifFromCursor(c)); } while (c.moveToNext());
                c.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getNotificationsForUser failed", e);
        }
        return list;
    }

    public void markNotificationRead(int notifId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_NOTIF_IS_READ, 1);
            db.update(TABLE_NOTIFICATIONS, v, COLUMN_NOTIF_ID + "=?",
                    new String[]{String.valueOf(notifId)});
            db.close();
            new FirestoreHelper().markNotificationRead(notifId);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "markNotificationRead failed", e);
        }
    }

    public void markAllNotificationsRead(String studentId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_NOTIF_IS_READ, 1);
            db.update(TABLE_NOTIFICATIONS, v,
                    COLUMN_NOTIF_RECIPIENT_SID + "=? AND (" + COLUMN_NOTIF_IS_ARCHIVED + "=0 OR " + COLUMN_NOTIF_IS_ARCHIVED + " IS NULL)",
                    new String[]{studentId});
            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "markAllNotificationsRead failed", e);
        }
    }

    public void markNotificationUnread(int notifId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_NOTIF_IS_READ, 0);
            db.update(TABLE_NOTIFICATIONS, v, COLUMN_NOTIF_ID + "=?",
                    new String[]{String.valueOf(notifId)});
            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "markNotificationUnread failed", e);
        }
    }

    /**
     * Marks the given notification IDs as archived (sets is_archived=1, archived_at=now).
     * Archived notifications are hidden from the normal list and auto-deleted after 30 days.
     */
    public void archiveNotifications(List<Integer> notifIds) {
        if (notifIds == null || notifIds.isEmpty()) return;
        try {
            String archivedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date());
            SQLiteDatabase db = this.getWritableDatabase();
            for (int id : notifIds) {
                ContentValues v = new ContentValues();
                v.put(COLUMN_NOTIF_IS_ARCHIVED, 1);
                v.put(COLUMN_NOTIF_ARCHIVED_AT, archivedAt);
                db.update(TABLE_NOTIFICATIONS, v, COLUMN_NOTIF_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "archiveNotifications failed", e);
        }
    }

    /**
     * Permanently deletes notifications that were archived more than 30 days ago.
     * Called on every app open so the DB stays clean automatically.
     * Accepts db directly to avoid recursive getWritableDatabase() call inside onOpen.
     */
    public void deleteExpiredArchivedNotifications(SQLiteDatabase db) {
        try {
            // SQLite date math: delete rows where archived_at < (now - 30 days)
            db.execSQL(
                "DELETE FROM " + TABLE_NOTIFICATIONS +
                " WHERE " + COLUMN_NOTIF_IS_ARCHIVED + "=1" +
                " AND " + COLUMN_NOTIF_ARCHIVED_AT + " != ''" +
                " AND datetime(" + COLUMN_NOTIF_ARCHIVED_AT + ") < datetime('now', '-30 days')"
            );
        } catch (Exception e) {
            Log.e("DatabaseHelper", "deleteExpiredArchivedNotifications failed", e);
        }
    }

    /** Overload for callers outside onOpen that don't have a db reference. */
    public void deleteExpiredArchivedNotifications() {
        try {
            deleteExpiredArchivedNotifications(this.getWritableDatabase());
        } catch (Exception e) {
            Log.e("DatabaseHelper", "deleteExpiredArchivedNotifications failed", e);
        }
    }

    /**
     * Returns all archived notifications for the given user, newest first.
     */
    public List<NotifModel> getArchivedNotificationsForUser(String studentId) {
        List<NotifModel> list = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT * FROM " + TABLE_NOTIFICATIONS +
                    " WHERE " + COLUMN_NOTIF_RECIPIENT_SID + "=?" +
                    " AND " + COLUMN_NOTIF_IS_ARCHIVED + "=1" +
                    " ORDER BY " + COLUMN_NOTIF_ARCHIVED_AT + " DESC",
                    new String[]{studentId});
            if (c != null && c.moveToFirst()) {
                do { list.add(notifFromCursor(c)); } while (c.moveToNext());
                c.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getArchivedNotificationsForUser failed", e);
        }
        return list;
    }

    /**
     * Returns the count of archived notifications for the given user.
     */
    public int getArchivedNotificationCount(String studentId) {
        int count = 0;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_NOTIFICATIONS +
                    " WHERE " + COLUMN_NOTIF_RECIPIENT_SID + "=?" +
                    " AND " + COLUMN_NOTIF_IS_ARCHIVED + "=1",
                    new String[]{studentId});
            if (c != null && c.moveToFirst()) { count = c.getInt(0); c.close(); }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getArchivedNotificationCount failed", e);
        }
        return count;
    }

    /**
     * Unarchives a single notification (sets is_archived=0, clears archived_at).
     */
    public void unarchiveNotification(int notifId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_NOTIF_IS_ARCHIVED, 0);
            v.put(COLUMN_NOTIF_ARCHIVED_AT, "");
            db.update(TABLE_NOTIFICATIONS, v, COLUMN_NOTIF_ID + "=?",
                    new String[]{String.valueOf(notifId)});
            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "unarchiveNotification failed", e);
        }
    }

    /**
     * Permanently deletes a single notification from the DB.
     */
    public void deleteNotification(int notifId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete(TABLE_NOTIFICATIONS, COLUMN_NOTIF_ID + "=?",
                    new String[]{String.valueOf(notifId)});
            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "deleteNotification failed", e);
        }
    }
    public int getUnreadNotificationCount(String studentId) {
        int count = 0;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_NOTIFICATIONS +
                    " WHERE " + COLUMN_NOTIF_RECIPIENT_SID + "=? AND " +
                    COLUMN_NOTIF_IS_READ + "=0",
                    new String[]{studentId});
            if (c != null && c.moveToFirst()) { count = c.getInt(0); c.close(); }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getUnreadNotificationCount failed", e);
        }
        return count;
    }

    // ── System Stats ──────────────────────────────────────────────────────────

    /**
     * Returns system-wide statistics map with keys:
     * totalStudents, totalOfficers, totalApproved, totalPending, totalCancelled,
     * totalPostponed, totalRegistrations, topEvents (List<String[]>), deptBreakdown (Map<String,Integer>)
     */
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();

            stats.put("totalStudents",      queryCount(db, TABLE_USERS,  COLUMN_USER_ROLE + "=?", new String[]{"Student"}));
            stats.put("totalOfficers",      queryCount(db, TABLE_USERS,  COLUMN_USER_ROLE + "=?", new String[]{"Officer"}));
            stats.put("totalApproved",      queryCount(db, TABLE_EVENTS, COLUMN_STATUS + "=?",    new String[]{"APPROVED"}));
            stats.put("totalPending",       queryCount(db, TABLE_EVENTS, COLUMN_STATUS + "=?",    new String[]{"PENDING"}));
            stats.put("totalCancelled",     queryCount(db, TABLE_EVENTS, COLUMN_STATUS + "=?",    new String[]{"CANCELLED"}));
            stats.put("totalPostponed",     queryCount(db, TABLE_EVENTS, COLUMN_STATUS + "=?",    new String[]{"POSTPONED"}));
            stats.put("totalRegistrations", queryCount(db, TABLE_REGISTRATIONS, null, null));

            // Top 3 events by registration count
            List<String[]> topEvents = new ArrayList<>();
            Cursor c = db.rawQuery(
                    "SELECT e." + COLUMN_TITLE + ", COUNT(r." + COLUMN_REG_ID + ") as cnt" +
                    " FROM " + TABLE_EVENTS + " e" +
                    " LEFT JOIN " + TABLE_REGISTRATIONS + " r ON e." + COLUMN_ID + " = r." + COLUMN_REG_EVENT_ID +
                    " GROUP BY e." + COLUMN_ID +
                    " ORDER BY cnt DESC LIMIT 3", null);
            if (c != null && c.moveToFirst()) {
                do { topEvents.add(new String[]{c.getString(0), String.valueOf(c.getInt(1))}); }
                while (c.moveToNext());
                c.close();
            }
            stats.put("topEvents", topEvents);

            // Per-department registration breakdown
            Map<String, Integer> deptBreakdown = new HashMap<>();
            c = db.rawQuery(
                    "SELECT u." + COLUMN_USER_DEPARTMENT + ", COUNT(*) as cnt" +
                    " FROM " + TABLE_REGISTRATIONS + " r" +
                    " INNER JOIN " + TABLE_USERS + " u ON r." + COLUMN_REG_STUDENT_ID + " = u." + COLUMN_USER_STUDENT_ID +
                    " GROUP BY u." + COLUMN_USER_DEPARTMENT, null);
            if (c != null && c.moveToFirst()) {
                do {
                    String dept = c.getString(0);
                    if (dept == null || dept.isEmpty()) dept = "Unknown";
                    deptBreakdown.put(dept, c.getInt(1));
                } while (c.moveToNext());
                c.close();
            }
            stats.put("deptBreakdown", deptBreakdown);

        } catch (Exception e) {
            Log.e("DatabaseHelper", "getSystemStats failed", e);
        }
        return stats;
    }

    private int queryCount(SQLiteDatabase db, String table, String where, String[] args) {
        String q = "SELECT COUNT(*) FROM " + table;
        if (where != null) q += " WHERE " + where;
        Cursor c = db.rawQuery(q, args);
        int count = 0;
        if (c != null && c.moveToFirst()) { count = c.getInt(0); c.close(); }
        return count;
    }

    /**
     * Returns [student_id, notif_pref] pairs for all Student-role users whose department
     * contains the given abbreviation. If abbr is "ALL", returns every Student.
     * Used by ApproveEventsActivity to enforce per-user notification preferences.
     */
    public List<String[]> getStudentIdsByDeptAbbrWithPref(String abbr) {
        List<String[]> result = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c;
            if ("ALL".equalsIgnoreCase(abbr)) {
                c = db.rawQuery(
                        "SELECT " + COLUMN_USER_STUDENT_ID + ", " + COLUMN_USER_NOTIF_PREF +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COLUMN_USER_ROLE + " = 'Student'", null);
            } else {
                c = db.rawQuery(
                        "SELECT " + COLUMN_USER_STUDENT_ID + ", " + COLUMN_USER_NOTIF_PREF +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COLUMN_USER_ROLE + " = 'Student'" +
                        " AND UPPER(" + COLUMN_USER_DEPARTMENT + ") LIKE ?",
                        new String[]{"%" + abbr.toUpperCase(Locale.getDefault()) + "%"});
            }
            if (c != null && c.moveToFirst()) {
                do {
                    String sid  = c.getString(0);
                    String pref = c.getString(1);
                    result.add(new String[]{sid, pref != null ? pref : "All Events"});
                } while (c.moveToNext());
                c.close();
            }
        } catch (Exception ex) {
            Log.e("DatabaseHelper", "getStudentIdsByDeptAbbrWithPref failed", ex);
        }
        return result;
    }

    /**
     * Returns the student_ids of all Student-role users whose department contains
     * the given abbreviation (e.g. "CBA").  Matches are case-insensitive via UPPER().
     * If abbr is "ALL", returns every Student's student_id.
     */
    public List<String> getStudentIdsByDeptAbbr(String abbr) {
        List<String> ids = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c;
            if ("ALL".equalsIgnoreCase(abbr)) {
                c = db.rawQuery(
                        "SELECT " + COLUMN_USER_STUDENT_ID + " FROM " + TABLE_USERS +
                        " WHERE " + COLUMN_USER_ROLE + " = 'Student'", null);
            } else {
                c = db.rawQuery(
                        "SELECT " + COLUMN_USER_STUDENT_ID + " FROM " + TABLE_USERS +
                        " WHERE " + COLUMN_USER_ROLE + " = 'Student'" +
                        " AND UPPER(" + COLUMN_USER_DEPARTMENT + ") LIKE ?",
                        new String[]{"%" + abbr.toUpperCase(Locale.getDefault()) + "%"});
            }
            if (c != null && c.moveToFirst()) {
                do { ids.add(c.getString(0)); } while (c.moveToNext());
                c.close();
            }
        } catch (Exception ex) {
            Log.e("DatabaseHelper", "getStudentIdsByDeptAbbr failed", ex);
        }
        return ids;
    }

    // ── New query methods (Steps 3–7) ─────────────────────────────────────────

    /**
     * Returns all non-PENDING, non-hidden events matching the given department abbreviation
     * (matched against the tags column, e.g. "#CLAS #CBA"). Pass null or "" to get all.
     */
    public List<Event> getAllEvents(String deptAbbr) {
        List<Event> events = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            String today = ServerTimeUtil.todayString();
            String hiddenCol = " AND (IFNULL(" + COLUMN_IS_HIDDEN + ",0) = 0)";
            String dateCol = " AND " + COLUMN_DATE + " >= '" + today + "'";
            if (deptAbbr == null || deptAbbr.isEmpty()) {
                Cursor c = db.rawQuery(
                        "SELECT * FROM " + TABLE_EVENTS +
                        " WHERE " + COLUMN_STATUS + " != 'PENDING'" + hiddenCol + dateCol +
                        " ORDER BY " + COLUMN_DATE + " ASC", null);
                if (c != null && c.moveToFirst()) {
                    do { events.add(eventFromCursor(c)); } while (c.moveToNext());
                    c.close();
                }
            } else {
                Cursor c = db.rawQuery(
                        "SELECT * FROM " + TABLE_EVENTS +
                        " WHERE " + COLUMN_STATUS + " != 'PENDING'" + hiddenCol + dateCol +
                        " AND UPPER(IFNULL(" + COLUMN_TAGS + ",'')) LIKE ?" +
                        " ORDER BY " + COLUMN_DATE + " ASC",
                        new String[]{"%" + deptAbbr.toUpperCase(Locale.getDefault()) + "%"});
                if (c != null && c.moveToFirst()) {
                    do { events.add(eventFromCursor(c)); } while (c.moveToNext());
                    c.close();
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getAllEvents(dept) failed", e);
        }
        return events;
    }

    /**
     * Returns a ranked list of top event creators (officers) by number of APPROVED events.
     * Each entry is String[]{name, dept, approvedCount}.
     * Limited to top 10.
     */
    public List<String[]> getTopEventCreators() {
        List<String[]> result = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            // Join events to users via creator_sid, count approved events per creator
            Cursor c = db.rawQuery(
                    "SELECT u." + COLUMN_USER_NAME + ", u." + COLUMN_USER_DEPARTMENT +
                    ", COUNT(e." + COLUMN_ID + ") as cnt" +
                    " FROM " + TABLE_EVENTS + " e" +
                    " INNER JOIN " + TABLE_USERS + " u ON e." + COLUMN_CREATOR_SID +
                    " = u." + COLUMN_USER_STUDENT_ID +
                    " WHERE e." + COLUMN_STATUS + " = 'APPROVED'" +
                    " GROUP BY u." + COLUMN_USER_STUDENT_ID +
                    " ORDER BY cnt DESC LIMIT 10", null);
            if (c != null && c.moveToFirst()) {
                do {
                    result.add(new String[]{
                            c.getString(0),               // name
                            c.getString(1),               // dept
                            String.valueOf(c.getInt(2))   // count
                    });
                } while (c.moveToNext());
                c.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getTopEventCreators failed", e);
        }
        return result;
    }

    /**
     * Returns APPROVED events whose organizer field starts with (or contains) the given venue name
     * and whose date equals the given date string (yyyy-MM-dd).
     * Used by VenueFragment to show booking slots.
     */
    public List<Event> getEventsByVenueAndDate(String venueName, String date) {
        List<Event> events = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT * FROM " + TABLE_EVENTS +
                    " WHERE " + COLUMN_STATUS + " = 'APPROVED'" +
                    " AND " + COLUMN_DATE + " = ?" +
                    " AND UPPER(IFNULL(" + COLUMN_ORGANIZER + ",'')) LIKE ?" +
                    " ORDER BY " + COLUMN_EVENT_TIME + " ASC",
                    new String[]{date, "%" + venueName.toUpperCase(Locale.getDefault()) + "%"});
            if (c != null && c.moveToFirst()) {
                do { events.add(eventFromCursor(c)); } while (c.moveToNext());
                c.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "getEventsByVenueAndDate failed", e);
        }
        return events;
    }

    /**
     * Hides an event locally (sets is_hidden=1). Hidden events are excluded from
     * the student browse list but remain in the DB.
     */
    public void hideEvent(int eventId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_IS_HIDDEN, 1);
            db.update(TABLE_EVENTS, v, COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "hideEvent failed", e);
        }
    }

    /**
     * Unhides an event (sets is_hidden=0).
     */
    public void unhideEvent(int eventId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_IS_HIDDEN, 0);
            db.update(TABLE_EVENTS, v, COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "unhideEvent failed", e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Event eventFromCursor(Cursor c) {
        int timeIdx = c.getColumnIndex(COLUMN_EVENT_TIME);
        String time = timeIdx >= 0 ? c.getString(timeIdx) : "";
        if (time == null) time = "";
        int startTimeIdx = c.getColumnIndex(COLUMN_START_TIME);
        String startTime = startTimeIdx >= 0 ? c.getString(startTimeIdx) : "";
        if (startTime == null) startTime = "";
        int endTimeIdx = c.getColumnIndex(COLUMN_END_TIME);
        String endTime = endTimeIdx >= 0 ? c.getString(endTimeIdx) : "";
        if (endTime == null) endTime = "";
        int creatorIdx = c.getColumnIndex(COLUMN_CREATOR_SID);
        String creatorSid = creatorIdx >= 0 ? c.getString(creatorIdx) : "";
        if (creatorSid == null) creatorSid = "";
        int venueIdx = c.getColumnIndex(COLUMN_VENUE);
        String venue = venueIdx >= 0 ? c.getString(venueIdx) : "";
        if (venue == null) venue = "";
        int tiCodeIdx = c.getColumnIndex(COLUMN_TIME_IN_CODE);
        String tiCode = tiCodeIdx >= 0 ? c.getString(tiCodeIdx) : "";
        if (tiCode == null) tiCode = "";
        int toCodeIdx = c.getColumnIndex(COLUMN_TIME_OUT_CODE);
        String toCode = toCodeIdx >= 0 ? c.getString(toCodeIdx) : "";
        if (toCode == null) toCode = "";
        Event e = new Event(
                c.getInt(c.getColumnIndexOrThrow(COLUMN_ID)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_DESC)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_DATE)),
                time,
                c.getString(c.getColumnIndexOrThrow(COLUMN_TAGS)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_ORGANIZER)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_STATUS))
        );
        e.setStartTime(startTime);
        e.setEndTime(endTime);
        e.setCreatorSid(creatorSid);
        e.setVenue(venue);
        e.setTimeInCode(tiCode);
        e.setTimeOutCode(toCode);
        return e;
    }

    private NotifModel notifFromCursor(Cursor c) {
        int sugTimeIdx = c.getColumnIndex(COLUMN_NOTIF_SUGGESTED_TIME);
        String suggestedTime = sugTimeIdx >= 0 ? c.getString(sugTimeIdx) : "";
        if (suggestedTime == null) suggestedTime = "";

        int isArchivedIdx = c.getColumnIndex(COLUMN_NOTIF_IS_ARCHIVED);
        boolean isArchived = isArchivedIdx >= 0 && c.getInt(isArchivedIdx) == 1;

        int archivedAtIdx = c.getColumnIndex(COLUMN_NOTIF_ARCHIVED_AT);
        String archivedAt = archivedAtIdx >= 0 ? c.getString(archivedAtIdx) : "";
        if (archivedAt == null) archivedAt = "";

        NotifModel m = new NotifModel(
                c.getInt(c.getColumnIndexOrThrow(COLUMN_NOTIF_ID)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_RECIPIENT_SID)),
                c.getInt(c.getColumnIndexOrThrow(COLUMN_NOTIF_EVENT_ID)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_TYPE)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_MESSAGE)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_REASON)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_SUGGESTED_DATE)),
                suggestedTime,
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_INSTRUCTIONS)),
                c.getInt(c.getColumnIndexOrThrow(COLUMN_NOTIF_IS_READ)) == 1,
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_CREATED_AT))
        );
        m.setArchived(isArchived);
        m.setArchivedAt(archivedAt);
        return m;
    }

    // ── Export / Import ───────────────────────────────────────────────────────

    /**
     * Export entire database to a CSV zip-style text file.
     * Each table is written as a CSV block separated by blank lines.
     * Returns the CSV string, or null on failure.
     */
    public String exportDatabaseCsv() {
        StringBuilder sb = new StringBuilder();
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            exportTableCsv(db, TABLE_EVENTS, sb);
            sb.append("\n\n");
            exportTableCsv(db, TABLE_USERS, sb);
            sb.append("\n\n");
            exportTableCsv(db, TABLE_REGISTRATIONS, sb);
            sb.append("\n\n");
            exportTableCsv(db, TABLE_NOTIFICATIONS, sb);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "exportDatabaseCsv failed", e);
            return null;
        }
        return sb.toString();
    }

    /**
     * Export registered students per event as CSV.
     * Columns: Event Title, Event Date, Student Name, Student ID, Department, Email, Registration Timestamp
     */
    public String exportRegisteredStudentsCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("Event Title,Event Date,Student Name,Student ID,Department,Email,Registration Timestamp\n");
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            String sql =
                "SELECT e." + COLUMN_TITLE + ", e." + COLUMN_DATE +
                ", u." + COLUMN_USER_NAME + ", u." + COLUMN_USER_STUDENT_ID +
                ", u." + COLUMN_USER_DEPARTMENT + ", u." + COLUMN_USER_EMAIL +
                ", r." + COLUMN_REG_TIMESTAMP +
                " FROM " + TABLE_REGISTRATIONS + " r" +
                " JOIN " + TABLE_EVENTS + " e ON e." + COLUMN_ID + " = r." + COLUMN_REG_EVENT_ID +
                " JOIN " + TABLE_USERS + " u ON u." + COLUMN_USER_STUDENT_ID + " = r." + COLUMN_REG_STUDENT_ID +
                " ORDER BY e." + COLUMN_TITLE + ", u." + COLUMN_USER_NAME;
            Cursor c = db.rawQuery(sql, null);
            if (c != null) {
                while (c.moveToNext()) {
                    sb.append(csvEscape(c.getString(0))).append(",");
                    sb.append(csvEscape(c.getString(1))).append(",");
                    sb.append(csvEscape(c.getString(2))).append(",");
                    sb.append(csvEscape(c.getString(3))).append(",");
                    sb.append(csvEscape(c.getString(4))).append(",");
                    sb.append(csvEscape(c.getString(5))).append(",");
                    sb.append(csvEscape(c.getString(6))).append("\n");
                }
                c.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "exportRegisteredStudentsCsv failed", e);
            return null;
        }
        return sb.toString();
    }

    /**
     * Import database from a full CSV string (produced by exportDatabaseCsv).
     * Parses each table block and upserts rows. Only events and users are imported
     * (registrations and notifications are skipped to avoid conflicts).
     * Returns number of rows imported, or -1 on failure.
     */
    public int importDatabaseCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return -1;
        int count = 0;
        try {
            // Split by double newline into table blocks
            String[] blocks = csv.split("\n\n+");
            for (String block : blocks) {
                String[] lines = block.split("\n");
                if (lines.length < 2) continue;
                String header = lines[0].trim();
                // Detect table by header columns
                if (header.startsWith("id,title,")) {
                    // Events table
                    for (int i = 1; i < lines.length; i++) {
                        String[] cols = splitCsvLine(lines[i]);
                        if (cols.length < 13) continue;
                        try {
                            int localId = Integer.parseInt(cols[0].trim());
                            syncUpsertEvent(localId, cols[1], cols[2], cols[3], cols[4],
                                    cols[7], cols[8], cols[9], cols[10], cols[11], cols[12], cols[5], cols[6]);
                            count++;
                        } catch (Exception ignored) {}
                    }
                } else if (header.startsWith("user_pk,")) {
                    // Users table
                    for (int i = 1; i < lines.length; i++) {
                        String[] cols = splitCsvLine(lines[i]);
                        if (cols.length < 9) continue;
                        try {
                            syncUpsertUser(cols[2], cols[1], cols[3], cols[5], cols[6],
                                    cols[7], cols[8],
                                    cols.length > 9 ? cols[9] : "",
                                    cols.length > 10 ? cols[10] : "All Events",
                                    "", true, "");
                            count++;
                        } catch (Exception ignored) {}
                    }
                }
                // registrations and notifications are re-synced via SyncManager, skip here
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "importDatabaseCsv failed", e);
            return -1;
        }
        return count;
    }

    // ── Delete User ───────────────────────────────────────────────────────────

    /**
     * Delete a user account by student_id.
     * Wraps all SQLite deletes in a transaction, cascade-deletes from Firestore,
     * and calls the deleteUserAccount Cloud Function to also remove the Firebase
     * Auth credential (which cannot be done from the client SDK for other users).
     */
    public void deleteUserAccount(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;
        // Look up firebase_uid BEFORE deleting the SQLite row
        String firebaseUid = getFirebaseUid(studentId);
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Delete registrations
            db.delete(TABLE_REGISTRATIONS, COLUMN_REG_STUDENT_ID + "=?", new String[]{studentId});
            // Delete notifications sent to this user
            db.delete(TABLE_NOTIFICATIONS, COLUMN_NOTIF_RECIPIENT_SID + "=?", new String[]{studentId});
            // Delete the user row
            db.delete(TABLE_USERS, COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "deleteUserAccount SQLite failed", e);
        } finally {
            db.endTransaction();
        }
        // Directly delete from Firestore (Cloud Function is not deployed on Spark plan).
        // Note: Firebase Auth credential for the deleted user cannot be removed from the
        // client SDK; it will remain as an orphaned Auth entry but is harmless since the
        // Firestore user doc is gone and the app uses Firestore as source of truth.
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        if (!firebaseUid.isEmpty()) {
            firestoreHelper.deleteUser(firebaseUid);
        }
        firestoreHelper.deleteUserRegistrations(studentId);
        firestoreHelper.deleteUserNotifications(studentId);
        Log.i("DatabaseHelper", "deleteUserAccount: Firestore deletion initiated for " + studentId + " (uid=" + firebaseUid + ")");
    }

    /**
     * Wipe ALL student/officer data from both local SQLite and Firestore.
     * Admin accounts are preserved. Runs Firestore cleanup on a background thread
     * and posts the result callback back to the main thread.
     *
     * @param onSuccess called on the main thread when both SQLite and Firestore are clear
     * @param onFailure called on the main thread if the Firestore phase fails
     */
    public void deleteAllStudentData(Runnable onSuccess, Runnable onFailure) {
        // Phase 1 — SQLite (fast, do it synchronously before spawning the thread)
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_REGISTRATIONS, null, null);
            db.delete(TABLE_NOTIFICATIONS, null, null);
            db.delete(TABLE_EVENTS, null, null);
            db.delete(TABLE_USERS, COLUMN_USER_ROLE + " != ?", new String[]{"Admin"});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "deleteAllStudentData SQLite failed", e);
        } finally {
            db.endTransaction();
        }

        // Phase 2 — Firestore (requires network + Tasks.await, run off main thread)
        new Thread(() -> {
            try {
                new FirestoreHelper().deleteAllData();
                if (onSuccess != null)
                    new Handler(Looper.getMainLooper()).post(onSuccess);
            } catch (Exception e) {
                Log.e("DatabaseHelper", "deleteAllStudentData Firestore failed", e);
                if (onFailure != null)
                    new Handler(Looper.getMainLooper()).post(onFailure);
            }
        }).start();
    }

    // ── CSV helpers ───────────────────────────────────────────────────────────

    private void exportTableCsv(SQLiteDatabase db, String table, StringBuilder sb) {
        Cursor c = db.rawQuery("SELECT * FROM " + table, null);
        if (c == null) return;
        // Header row
        String[] cols = c.getColumnNames();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(cols[i]);
        }
        sb.append("\n");
        // Data rows — skip password column for users
        while (c.moveToNext()) {
            for (int i = 0; i < cols.length; i++) {
                if (i > 0) sb.append(",");
                // Redact password from export
                if (table.equals(TABLE_USERS) && cols[i].equals(COLUMN_USER_PASSWORD)) {
                    sb.append("");
                } else {
                    sb.append(csvEscape(c.getString(i)));
                }
            }
            sb.append("\n");
        }
        c.close();
    }

    private String csvEscape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private String[] splitCsvLine(String line) {
        // Simple CSV split (handles quoted fields)
        List<String> result = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder field = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"'); i++;
                } else { inQuote = !inQuote; }
            } else if (ch == ',' && !inQuote) {
                result.add(field.toString()); field.setLength(0);
            } else { field.append(ch); }
        }
        result.add(field.toString());
        return result.toArray(new String[0]);
    }
}
