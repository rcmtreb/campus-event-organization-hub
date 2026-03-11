package com.example.campus_event_org_hub.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.campus_event_org_hub.model.Event;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ceoh.db";
    private static final int DATABASE_VERSION = 5; // Version 5: Added student_id column

    // Table: Events
    public static final String TABLE_EVENTS = "events";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESC = "description";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TAGS = "tags";
    public static final String COLUMN_ORGANIZER = "organizer";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_IMAGE_PATH = "image_path";
    public static final String COLUMN_STATUS = "status";

    // Table: Users
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID_PK = "user_pk";
    public static final String COLUMN_USER_NAME = "name";
    public static final String COLUMN_USER_STUDENT_ID = "student_id"; // Actual Student ID
    public static final String COLUMN_USER_EMAIL = "email";
    public static final String COLUMN_USER_PASSWORD = "password";
    public static final String COLUMN_USER_ROLE = "role";
    public static final String COLUMN_USER_DEPARTMENT = "department";

    private static final String CREATE_TABLE_EVENTS = "CREATE TABLE " + TABLE_EVENTS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_TITLE + " TEXT, " +
            COLUMN_DESC + " TEXT, " +
            COLUMN_DATE + " TEXT, " +
            COLUMN_TAGS + " TEXT, " +
            COLUMN_ORGANIZER + " TEXT, " +
            COLUMN_CATEGORY + " TEXT, " +
            COLUMN_IMAGE_PATH + " TEXT, " +
            COLUMN_STATUS + " TEXT DEFAULT 'PENDING')";

    private static final String CREATE_TABLE_USERS = "CREATE TABLE " + TABLE_USERS + " (" +
            COLUMN_USER_ID_PK + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USER_NAME + " TEXT, " +
            COLUMN_USER_STUDENT_ID + " TEXT UNIQUE, " +
            COLUMN_USER_EMAIL + " TEXT UNIQUE, " +
            COLUMN_USER_PASSWORD + " TEXT, " +
            COLUMN_USER_ROLE + " TEXT, " +
            COLUMN_USER_DEPARTMENT + " TEXT)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_EVENTS);
        db.execSQL(CREATE_TABLE_USERS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // --- User Operations ---

    public long registerUser(String name, String studentId, String email, String password, String role, String department) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_NAME, name);
        values.put(COLUMN_USER_STUDENT_ID, studentId);
        values.put(COLUMN_USER_EMAIL, email);
        values.put(COLUMN_USER_PASSWORD, password);
        values.put(COLUMN_USER_ROLE, role);
        values.put(COLUMN_USER_DEPARTMENT, department);
        long id = db.insert(TABLE_USERS, null, values);
        db.close();
        return id;
    }

    public Cursor checkUser(String loginInput, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Allow login with either Email OR Student ID
        return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE (" + 
                COLUMN_USER_EMAIL + "=? OR " + COLUMN_USER_STUDENT_ID + "=?) AND " + 
                COLUMN_USER_PASSWORD + "=?", 
                new String[]{loginInput, loginInput, password});
    }

    public int getCount(String table, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + table;
        if (whereClause != null) query += " WHERE " + whereClause;
        Cursor cursor = db.rawQuery(query, whereArgs);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    // --- Event Operations ---

    public long addEvent(Event event) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, event.getTitle());
        values.put(COLUMN_DESC, event.getDescription());
        values.put(COLUMN_DATE, event.getDate());
        values.put(COLUMN_TAGS, event.getTags());
        values.put(COLUMN_ORGANIZER, event.getOrganizer());
        values.put(COLUMN_CATEGORY, event.getCategory());
        values.put(COLUMN_IMAGE_PATH, event.getImagePath());
        values.put(COLUMN_STATUS, event.getStatus());
        long id = db.insert(TABLE_EVENTS, null, values);
        db.close();
        return id;
    }

    public void approveEvent(int eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, "APPROVED");
        db.update(TABLE_EVENTS, values, COLUMN_ID + "=?", new String[]{String.valueOf(eventId)});
        db.close();
    }

    public List<Event> getEventsByStatus(String status) {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS + " WHERE " + COLUMN_STATUS + "=? ORDER BY " + COLUMN_ID + " DESC", new String[]{status});

        if (cursor.moveToFirst()) {
            do {
                Event event = new Event(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESC)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAGS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ORGANIZER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS))
                );
                events.add(event);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return events;
    }

    public List<Event> getAllEvents() {
        return getEventsByStatus("APPROVED");
    }
}
