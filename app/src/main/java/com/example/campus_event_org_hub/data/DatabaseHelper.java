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
    private static final int DATABASE_VERSION = 11; // v11: added creator_sid to events

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
    public static final String COLUMN_NOTIF_INSTRUCTIONS   = "instructions";
    public static final String COLUMN_NOTIF_IS_READ        = "is_read";
    public static final String COLUMN_NOTIF_CREATED_AT     = "created_at";

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
            COLUMN_NOTIF_INSTRUCTIONS   + " TEXT DEFAULT '', " +
            COLUMN_NOTIF_IS_READ        + " INTEGER DEFAULT 0, " +
            COLUMN_NOTIF_CREATED_AT     + " TEXT)";

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
        // Pass db directly — avoids recursive getWritableDatabase() call inside onOpen
        deleteEndedEventsOlderThan(db, 30);
    }

    // ── Defensive repair ─────────────────────────────────────────────────────

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
                    COLUMN_NOTIF_INSTRUCTIONS   + " TEXT DEFAULT '', " +
                    COLUMN_NOTIF_IS_READ        + " INTEGER DEFAULT 0, " +
                    COLUMN_NOTIF_CREATED_AT     + " TEXT)");
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
     * NOTE: password is NOT synced from Firestore — it stays local only.
     */
    public void syncUpsertUser(String studentId, String name, String email,
                                String role, String department,
                                String gender, String mobile,
                                String profileImage, String notifPref) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            // Use INSERT OR REPLACE — preserve the local password by reading it first
            String localPassword = "";
            Cursor c = db.rawQuery("SELECT " + COLUMN_USER_PASSWORD + " FROM " + TABLE_USERS +
                    " WHERE " + COLUMN_USER_STUDENT_ID + "=?", new String[]{studentId});
            if (c != null && c.moveToFirst()) { localPassword = c.getString(0); c.close(); }

            ContentValues v = new ContentValues();
            v.put(COLUMN_USER_STUDENT_ID,  studentId);
            v.put(COLUMN_USER_NAME,        name);
            v.put(COLUMN_USER_EMAIL,       email);
            v.put(COLUMN_USER_PASSWORD,    localPassword); // keep local password
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
                                        String suggestedDate, String instructions,
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
                                   String suggestedDate, String instructions) {
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
            v.put(COLUMN_NOTIF_INSTRUCTIONS, instructions != null ? instructions : "");
            v.put(COLUMN_NOTIF_IS_READ, 0);
            v.put(COLUMN_NOTIF_CREATED_AT, createdAt);
            long id = db.insert(TABLE_NOTIFICATIONS, null, v);
            db.close();
            if (id != -1) {
                new FirestoreHelper().upsertNotification((int) id, recipientSid, eventId,
                        type, message, reason, suggestedDate, instructions, false, createdAt);
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
        return new NotifModel(
                c.getInt(c.getColumnIndexOrThrow(COLUMN_NOTIF_ID)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_RECIPIENT_SID)),
                c.getInt(c.getColumnIndexOrThrow(COLUMN_NOTIF_EVENT_ID)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_TYPE)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_MESSAGE)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_REASON)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_SUGGESTED_DATE)),
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_INSTRUCTIONS)),
                c.getInt(c.getColumnIndexOrThrow(COLUMN_NOTIF_IS_READ)) == 1,
                c.getString(c.getColumnIndexOrThrow(COLUMN_NOTIF_CREATED_AT))
        );
    }
}
