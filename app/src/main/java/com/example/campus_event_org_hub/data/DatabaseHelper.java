package com.example.campus_event_org_hub.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.model.NotifModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ceoh.db";
    private static final int DATABASE_VERSION = 13; // v13: added is_archived + archived_at to notifications

    // Table: Events
    public static final String TABLE_EVENTS       = "events";
    public static final String COLUMN_ID          = "id";
    public static final String COLUMN_TITLE       = "title";
    public static final String COLUMN_DESC        = "description";
    public static final String COLUMN_DATE        = "date";
    public static final String COLUMN_EVENT_TIME  = "event_time";
    public static final String COLUMN_TAGS        = "tags";
    public static final String COLUMN_ORGANIZER   = "organizer";
    public static final String COLUMN_CATEGORY    = "category";
    public static final String COLUMN_IMAGE_PATH  = "image_path";
    public static final String COLUMN_STATUS      = "status";
    public static final String COLUMN_CREATOR_SID = "creator_sid";

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

    // ── CREATE statements ────────────────────────────────────────────────────

    private static final String CREATE_TABLE_EVENTS =
            "CREATE TABLE " + TABLE_EVENTS + " (" +
            COLUMN_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TITLE      + " TEXT, " +
            COLUMN_DESC       + " TEXT, " +
            COLUMN_DATE       + " TEXT, " +
            COLUMN_EVENT_TIME + " TEXT DEFAULT '', " +
            COLUMN_TAGS       + " TEXT, " +
            COLUMN_ORGANIZER  + " TEXT, " +
            COLUMN_CATEGORY   + " TEXT, " +
            COLUMN_IMAGE_PATH + " TEXT, " +
            COLUMN_STATUS     + " TEXT DEFAULT 'PENDING', " +
            COLUMN_CREATOR_SID + " TEXT DEFAULT '')";

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
            COLUMN_USER_NOTIF_PREF  + " TEXT DEFAULT 'All Events')";

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

    // ── Constructor ──────────────────────────────────────────────────────────

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_EVENTS);
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_REGISTRATIONS);
        db.execSQL(CREATE_TABLE_NOTIFICATIONS);
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
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        ensureCoreTables(db);
        ensureUsersTableColumns(db);
        ensureEventTableColumns(db);
        ensureNotificationsTable(db);
        // Remove legacy seed events that were added automatically on first launch
        removeSeedEvents(db);
        // Backfill creator_sid for events created before v11 or synced without it
        backfillCreatorSid(db);
        // Pass db directly — avoids recursive getWritableDatabase() call inside onOpen
        deleteEndedEventsOlderThan(db, 30);
        // Purge notifications archived more than 30 days ago
        deleteExpiredArchivedNotifications();
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
            if (!cols.contains(COLUMN_CREATOR_SID))
                db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_CREATOR_SID + " TEXT DEFAULT ''");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "ensureEventTableColumns failed", e);
        } finally {
            if (cursor != null) cursor.close();
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
                             String password, String role, String department) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COLUMN_USER_NAME, name);
        v.put(COLUMN_USER_STUDENT_ID, studentId);
        v.put(COLUMN_USER_EMAIL, email);
        v.put(COLUMN_USER_PASSWORD, password);
        v.put(COLUMN_USER_ROLE, role);
        v.put(COLUMN_USER_DEPARTMENT, department);
        long id = db.insert(TABLE_USERS, null, v);
        db.close();
        if (id != -1) {
            // Mirror to Firestore (password NOT synced)
            new FirestoreHelper().upsertUser(studentId, name, email, role, department,
                    "", "", "", "All Events");
        }
        return id;
    }

    public Cursor checkUser(String loginInput, String password) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            return db.rawQuery(
                    "SELECT * FROM " + TABLE_USERS + " WHERE (" +
                    COLUMN_USER_EMAIL + "=? OR " + COLUMN_USER_STUDENT_ID + "=?) AND " +
                    COLUMN_USER_PASSWORD + "=?",
                    new String[]{loginInput, loginInput, password});
        } catch (Exception e) {
            Log.e("DatabaseHelper", "checkUser failed", e);
            return null;
        }
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
        new FirestoreHelper().updateUserField(studentId, "role", newRole);
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
                FirestoreHelper fsh = new FirestoreHelper();
                if (gender != null)           fsh.updateUserField(studentId, "gender",        gender);
                if (mobile != null)           fsh.updateUserField(studentId, "mobile",        mobile);
                if (profileImagePath != null) fsh.updateUserField(studentId, "profile_image", profileImagePath);
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
            if (!oldPassword.equals(stored)) { db.close(); return false; }
            ContentValues v = new ContentValues();
            v.put(COLUMN_USER_PASSWORD, newPassword);
            db.update(TABLE_USERS, v, COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
            db.close();
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
            new FirestoreHelper().updateUserField(studentId, "notif_pref", pref);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "updateNotifPref failed", e);
        }
    }

    // ── Registration Operations ───────────────────────────────────────────────

    public boolean registerForEvent(String studentId, int eventId) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date());
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

    // ── Sync helpers (called by SyncManager to import Firestore data) ─────────

    /**
     * Upsert a user row from Firestore. Uses INSERT OR REPLACE so existing rows
     * are overwritten with the latest cloud data.
     * Password priority: local password wins if it exists; otherwise use the Firestore password
     * (handles fresh-install case where user has changed their password on another device).
     */
    public void syncUpsertUser(String studentId, String name, String email,
                                String role, String department,
                                String gender, String mobile,
                                String profileImage, String notifPref,
                                String firestorePassword) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            // Preserve local password if it exists; fall back to Firestore password for fresh installs
            String localPassword = "";
            Cursor c = db.rawQuery("SELECT " + COLUMN_USER_PASSWORD + " FROM " + TABLE_USERS +
                    " WHERE " + COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
            if (c != null && c.moveToFirst()) { localPassword = c.getString(0); c.close(); }

            // Use local password if available, otherwise use the one from Firestore
            String passwordToStore = (localPassword != null && !localPassword.isEmpty())
                    ? localPassword
                    : (firestorePassword != null ? firestorePassword : "");

            ContentValues v = new ContentValues();
            v.put(COLUMN_USER_STUDENT_ID,  studentId);
            v.put(COLUMN_USER_NAME,        name);
            v.put(COLUMN_USER_EMAIL,       email);
            v.put(COLUMN_USER_PASSWORD,    passwordToStore);
            v.put(COLUMN_USER_ROLE,        role);
            v.put(COLUMN_USER_DEPARTMENT,  department);
            v.put(COLUMN_USER_GENDER,      gender        != null ? gender        : "");
            v.put(COLUMN_USER_MOBILE,      mobile        != null ? mobile        : "");
            v.put(COLUMN_USER_PROFILE_IMG, profileImage  != null ? profileImage  : "");
            v.put(COLUMN_USER_NOTIF_PREF,  notifPref     != null ? notifPref     : "All Events");
            db.insertWithOnConflict(TABLE_USERS, null, v, SQLiteDatabase.CONFLICT_REPLACE);
            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "syncUpsertUser failed", e);
        }
    }

    /**
     * Upsert an event row from Firestore. localId is the Firestore doc id (= original SQLite id).
     */
    public void syncUpsertEvent(int localId, String title, String description,
                                 String date, String time, String tags,
                                 String organizer, String category,
                                 String imagePath, String status, String creatorSid) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put(COLUMN_ID,          localId);
            v.put(COLUMN_TITLE,       title);
            v.put(COLUMN_DESC,        description);
            v.put(COLUMN_DATE,        date);
            v.put(COLUMN_EVENT_TIME,  time       != null ? time       : "");
            v.put(COLUMN_TAGS,        tags       != null ? tags       : "");
            v.put(COLUMN_ORGANIZER,   organizer);
            v.put(COLUMN_CATEGORY,    category);
            v.put(COLUMN_IMAGE_PATH,  imagePath  != null ? imagePath  : "");
            v.put(COLUMN_STATUS,      status);
            v.put(COLUMN_CREATOR_SID, creatorSid != null ? creatorSid : "");
            db.insertWithOnConflict(TABLE_EVENTS, null, v, SQLiteDatabase.CONFLICT_REPLACE);
            db.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "syncUpsertEvent failed", e);
        }
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
     */
    public void syncUpsertNotification(int localNotifId, String recipientSid, int eventId,
                                        String type, String message, String reason,
                                        String suggestedDate, String suggestedTime,
                                        String instructions,
                                        int isRead, String createdAt) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
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
            db.insertWithOnConflict(TABLE_NOTIFICATIONS, null, v, SQLiteDatabase.CONFLICT_REPLACE);
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
        v.put(COLUMN_TAGS, event.getTags());
        v.put(COLUMN_ORGANIZER, event.getOrganizer());
        v.put(COLUMN_CATEGORY, event.getCategory());
        v.put(COLUMN_IMAGE_PATH, event.getImagePath());
        v.put(COLUMN_STATUS, event.getStatus());
        v.put(COLUMN_CREATOR_SID, event.getCreatorSid() != null ? event.getCreatorSid() : "");
        long id = db.insert(TABLE_EVENTS, null, v);
        db.close();
        if (id != -1) {
            new FirestoreHelper().upsertEvent((int) id,
                    event.getTitle(), event.getDescription(), event.getDate(),
                    event.getTime(), event.getTags(), event.getOrganizer(),
                    event.getCategory(), event.getImagePath(), event.getStatus(),
                    event.getCreatorSid());

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
        db.delete(TABLE_EVENTS, COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
        db.close();
        new FirestoreHelper().deleteEvent(eventId);
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
                               String organizer, String category) {
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
     * Returns all non-PENDING events (APPROVED, CANCELLED, POSTPONED) for the student browse list.
     */
    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT * FROM " + TABLE_EVENTS +
                    " WHERE " + COLUMN_STATUS + " != 'PENDING'" +
                    " ORDER BY " + COLUMN_DATE + " DESC", null);
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
     * Parses organizer string (e.g. "Alberto – CCS") and looks up the officer's student_id.
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
     */
    public void deleteExpiredArchivedNotifications() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            // SQLite date math: delete rows where archived_at < (now - 30 days)
            db.execSQL(
                "DELETE FROM " + TABLE_NOTIFICATIONS +
                " WHERE " + COLUMN_NOTIF_IS_ARCHIVED + "=1" +
                " AND " + COLUMN_NOTIF_ARCHIVED_AT + " != ''" +
                " AND datetime(" + COLUMN_NOTIF_ARCHIVED_AT + ") < datetime('now', '-30 days')"
            );
            db.close();
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Event eventFromCursor(Cursor c) {
        int timeIdx = c.getColumnIndex(COLUMN_EVENT_TIME);
        String time = timeIdx >= 0 ? c.getString(timeIdx) : "";
        if (time == null) time = "";
        int creatorIdx = c.getColumnIndex(COLUMN_CREATOR_SID);
        String creatorSid = creatorIdx >= 0 ? c.getString(creatorIdx) : "";
        if (creatorSid == null) creatorSid = "";
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
        e.setCreatorSid(creatorSid);
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
                        if (cols.length < 11) continue;
                        try {
                            int localId = Integer.parseInt(cols[0].trim());
                            syncUpsertEvent(localId, cols[1], cols[2], cols[3], cols[4],
                                    cols[5], cols[6], cols[7], cols[8], cols[9], cols[10]);
                            count++;
                        } catch (Exception ignored) {}
                    }
                } else if (header.startsWith("user_pk,")) {
                    // Users table
                    for (int i = 1; i < lines.length; i++) {
                        String[] cols = splitCsvLine(lines[i]);
                        if (cols.length < 9) continue;
                        try {
                            // cols: user_pk, name, student_id, email, password(skip), role, dept, gender, mobile, profile_image, notif_pref
                            syncUpsertUser(cols[2], cols[1], cols[3], cols[5], cols[6],
                                    cols[7], cols[8],
                                    cols.length > 9 ? cols[9] : "",
                                    cols.length > 10 ? cols[10] : "All Events",
                                    "" /* password not imported from CSV */);
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
     * Also deletes their registrations and notifications from local DB and Firestore.
     */
    public void deleteUserAccount(String studentId) {
        if (studentId == null || studentId.isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Delete registrations
            db.delete(TABLE_REGISTRATIONS, COLUMN_REG_STUDENT_ID + "=?", new String[]{studentId});
            // Delete notifications sent to this user
            db.delete(TABLE_NOTIFICATIONS, COLUMN_NOTIF_RECIPIENT_SID + "=?", new String[]{studentId});
            // Delete the user row
            db.delete(TABLE_USERS, COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
            // Mirror to Firestore
            FirestoreHelper fs = new FirestoreHelper();
            fs.deleteUser(studentId);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "deleteUserAccount failed", e);
        }
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
