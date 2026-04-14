package com.example.campus_event_org_hub.data;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Pulls Firestore collections into the local SQLite database.
 *
 * Strategy: Firestore is source of truth for shared data (events, notifications,
 * user profiles). On every sync, we fetch all documents and UPSERT them locally
 * via DatabaseHelper.syncUpsert*() methods.
 *
 * Attendance is synced separately via syncAttendancesOnly() — called explicitly
 * by officers when needed (not during general sync to avoid DB lock contention).
 *
 * Passwords are NEVER synced from Firestore — they remain local-only.
 *
 * Usage:
 *   SyncManager.sync(context, () -> { // runs on main thread when done });
 *   SyncManager.syncAttendancesOnly(context, null); // officer-only, explicit call
 */
public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final long TIMEOUT_SECONDS = 10;

    /**
     * Pull Firestore → SQLite on a background thread (events, users, notifications).
     * Attendance is NOT included here — use syncAttendancesOnly() separately.
     * @param onComplete Runnable executed on the calling thread's Looper when sync finishes
     *                   (may be null). Always called even if sync partially fails.
     */
    public static void sync(Context context, Runnable onComplete) {
        EXECUTOR.execute(() -> {
            DatabaseHelper db   = DatabaseHelper.getInstance(context);
            FirestoreHelper fsh = new FirestoreHelper();

            syncUsers(db, fsh);
            syncEvents(db, fsh);
            syncNotifications(db, fsh);

            Log.d(TAG, "Sync complete");
            if (onComplete != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    /**
     * Pull attendance records from Firestore → local SQLite.
     * Call this explicitly from officer/admin screens when attendance data is needed
     * (e.g., when opening the attendee list). Runs on background thread.
     */
    public static void syncAttendancesOnly(Context context, Runnable onComplete) {
        EXECUTOR.execute(() -> {
            DatabaseHelper db   = DatabaseHelper.getInstance(context);
            FirestoreHelper fsh = new FirestoreHelper();
            syncAttendances(db, fsh);
            Log.d(TAG, "Attendance sync complete");
            if (onComplete != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(onComplete);
            }
        });
    }

    // ── per-collection sync ───────────────────────────────────────────────────

    private static void syncUsers(DatabaseHelper db, FirestoreHelper fsh) {
        try {
            QuerySnapshot snap = Tasks.await(
                    fsh.getDb().collection(FirestoreHelper.COL_USERS).get(),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<DocumentSnapshot> docs = snap.getDocuments();
            for (DocumentSnapshot d : docs) {
                String firebaseUid = d.getId(); // Doc ID = Firebase Auth UID
                String sid         = str(d, "student_id");
                String name        = str(d, "name");
                String email       = str(d, "email");
                String role        = str(d, "role");
                String dept        = str(d, "department");
                String gender      = str(d, "gender");
                String mobile      = str(d, "mobile");
                String profileImg  = str(d, "profile_image");
                String notifPref   = str(d, "notif_pref");
                String password    = str(d, "password");
                boolean emailVerified = intVal(d, "email_verified") == 1;
                if (sid == null || sid.isEmpty()) continue;
                db.syncUpsertUser(sid, name, email, role, dept,
                        gender, mobile, profileImg, notifPref, password, emailVerified, firebaseUid);
            }
            Log.d(TAG, "Synced " + docs.size() + " users");
        } catch (Exception e) {
            Log.e(TAG, "syncUsers failed", e);
        }
    }

    private static void syncEvents(DatabaseHelper db, FirestoreHelper fsh) {
        try {
            QuerySnapshot snap = Tasks.await(
                    fsh.getDb().collection(FirestoreHelper.COL_EVENTS).get(),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<DocumentSnapshot> docs = snap.getDocuments();
            for (DocumentSnapshot d : docs) {
                int    localId    = intVal(d, "local_id");
                String title      = str(d, "title");
                String desc       = str(d, "description");
                String date       = str(d, "date");
                String time       = str(d, "event_time");
                String startTime  = str(d, "start_time");
                String endTime    = str(d, "end_time");
                String tags       = str(d, "tags");
                String organizer  = str(d, "organizer");
                String category   = str(d, "category");
                String imagePath  = str(d, "image_path");
                String status     = str(d, "status");
                String creatorSid = str(d, "creator_sid");
                String timeInCode  = str(d, "time_in_code");
                String timeOutCode = str(d, "time_out_code");
                if (localId <= 0 || title == null) continue;
                db.syncUpsertEvent(localId, title, desc, date, time, tags,
                        organizer, category, imagePath, status, creatorSid, startTime, endTime,
                        timeInCode, timeOutCode);
            }
            Log.d(TAG, "Synced " + docs.size() + " events");
        } catch (Exception e) {
            Log.e(TAG, "syncEvents failed", e);
        }
    }

    private static void syncNotifications(DatabaseHelper db, FirestoreHelper fsh) {
        try {
            QuerySnapshot snap = Tasks.await(
                    fsh.getDb().collection(FirestoreHelper.COL_NOTIFICATIONS).get(),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<DocumentSnapshot> docs = snap.getDocuments();
            for (DocumentSnapshot d : docs) {
                int    notifId       = intVal(d, "local_notif_id");
                String recipientSid  = str(d, "recipient_sid");
                int    eventId       = intVal(d, "event_id");
                String type          = str(d, "type");
                String message       = str(d, "message");
                String reason        = str(d, "reason");
                String suggestedDate = str(d, "suggested_date");
                String suggestedTime = str(d, "suggested_time");
                String instructions  = str(d, "instructions");
                int    isRead        = intVal(d, "is_read");
                String createdAt     = str(d, "created_at");
                if (notifId <= 0 || recipientSid == null) continue;
                db.syncUpsertNotification(notifId, recipientSid, eventId,
                        type, message, reason, suggestedDate, suggestedTime,
                        instructions, isRead, createdAt);
            }
            Log.d(TAG, "Synced " + docs.size() + " notifications");
        } catch (Exception e) {
            Log.e(TAG, "syncNotifications failed", e);
        }
    }

    private static void syncAttendances(DatabaseHelper db, FirestoreHelper fsh) {
        try {
            QuerySnapshot snap = Tasks.await(
                    fsh.getDb().collection(FirestoreHelper.COL_ATTENDANCE).get(),
                    TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<DocumentSnapshot> docs = snap.getDocuments();
            for (DocumentSnapshot d : docs) {
                int    eventId          = intVal(d, "event_id");
                String studentId        = str(d, "student_id");
                String timeInAt         = str(d, "time_in_at");
                String timeOutAt        = str(d, "time_out_at");
                String timeInPhoto      = str(d, "time_in_photo");
                String timeOutPhoto     = str(d, "time_out_photo");
                String timeInWindowOpen  = str(d, "time_in_window_open");
                String timeInWindowClose = str(d, "time_in_window_close");
                String timeOutWindowOpen  = str(d, "time_out_window_open");
                String timeOutWindowClose = str(d, "time_out_window_close");
                long   updatedAt        = longVal(d, "updated_at");
                if (eventId <= 0 || studentId == null || studentId.isEmpty()) continue;
                db.syncUpsertAttendance(eventId, studentId, timeInAt, timeOutAt,
                        timeInPhoto, timeOutPhoto, timeInWindowOpen, timeInWindowClose,
                        timeOutWindowOpen, timeOutWindowClose, updatedAt);
            }
            Log.d(TAG, "Synced " + docs.size() + " attendance records");
        } catch (Exception e) {
            Log.e(TAG, "syncAttendances failed", e);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String str(DocumentSnapshot d, String field) {
        Object v = d.get(field);
        return v != null ? v.toString() : "";
    }

    private static int intVal(DocumentSnapshot d, String field) {
        try {
            Object v = d.get(field);
            if (v == null) return 0;
            if (v instanceof Long)   return ((Long) v).intValue();
            if (v instanceof Double) return ((Double) v).intValue();
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private static long longVal(DocumentSnapshot d, String field) {
        try {
            Object v = d.get(field);
            if (v == null) return 0L;
            if (v instanceof Long)   return ((Long) v);
            if (v instanceof Integer) return ((Integer) v).longValue();
            if (v instanceof Double) return ((Double) v).longValue();
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return 0L;
        }
    }
}
