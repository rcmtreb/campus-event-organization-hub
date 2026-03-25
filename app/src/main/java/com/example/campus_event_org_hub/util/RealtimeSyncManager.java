package com.example.campus_event_org_hub.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.FirestoreHelper;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Attaches Firestore snapshot listeners on the four shared collections
 * (events, notifications, registrations, users) and writes incremental
 * changes into the local SQLite database as they arrive.
 *
 * Usage:
 *   RealtimeSyncManager rsm = new RealtimeSyncManager(context, onChangeCallback);
 *   rsm.start();   // in onResume
 *   rsm.stop();    // in onPause / onDestroy
 *
 * The optional {@code onChangeCallback} is posted to the main thread whenever
 * any document changes — callers can use it to refresh UI.
 */
public class RealtimeSyncManager {

    private static final String TAG = "RealtimeSyncManager";

    private final Context               appContext;
    private final Runnable              onChangeCallback;
    private final Handler               mainHandler = new Handler(Looper.getMainLooper());
    private final FirebaseFirestore     db          = FirebaseFirestore.getInstance();
    private final List<ListenerRegistration> listeners = new ArrayList<>();

    public RealtimeSyncManager(Context context, Runnable onChangeCallback) {
        this.appContext       = context.getApplicationContext();
        this.onChangeCallback = onChangeCallback;
    }

    /** Attach all snapshot listeners. Safe to call multiple times (idempotent via stop first). */
    public void start() {
        stop(); // Remove any existing listeners before attaching new ones.
        listenToEvents();
        listenToNotifications();
        listenToRegistrations();
        listenToUsers();
        Log.d(TAG, "Realtime listeners started");
    }

    /** Detach all snapshot listeners and release resources. */
    public void stop() {
        for (ListenerRegistration reg : listeners) {
            reg.remove();
        }
        listeners.clear();
        Log.d(TAG, "Realtime listeners stopped");
    }

    // ── Events ────────────────────────────────────────────────────────────────

    private void listenToEvents() {
        ListenerRegistration reg = db.collection(FirestoreHelper.COL_EVENTS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Events listener error", error);
                        return;
                    }
                    if (snapshots == null || snapshots.isEmpty()) return;

                    DatabaseHelper dbHelper = DatabaseHelper.getInstance(appContext);
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        com.google.firebase.firestore.DocumentSnapshot d = dc.getDocument();
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
                        if (localId <= 0 || title == null || title.isEmpty()) continue;

                        if (dc.getType() == DocumentChange.Type.REMOVED) {
                            // Event deleted remotely — handled by admin; skip local delete
                            // to avoid accidental data loss on poor connectivity.
                            continue;
                        }
                        dbHelper.syncUpsertEvent(localId, title, desc, date, time, tags,
                                organizer, category, imagePath, status, creatorSid,
                                startTime, endTime, timeInCode, timeOutCode);
                    }
                    notifyChange();
                });
        listeners.add(reg);
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private void listenToNotifications() {
        ListenerRegistration reg = db.collection(FirestoreHelper.COL_NOTIFICATIONS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Notifications listener error", error);
                        return;
                    }
                    if (snapshots == null || snapshots.isEmpty()) return;

                    DatabaseHelper dbHelper = DatabaseHelper.getInstance(appContext);
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.REMOVED) continue;
                        com.google.firebase.firestore.DocumentSnapshot d = dc.getDocument();
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
                        if (notifId <= 0 || recipientSid == null || recipientSid.isEmpty()) continue;
                        dbHelper.syncUpsertNotification(notifId, recipientSid, eventId,
                                type, message, reason, suggestedDate, suggestedTime,
                                instructions, isRead, createdAt);
                    }
                    notifyChange();
                });
        listeners.add(reg);
    }

    // ── Registrations ─────────────────────────────────────────────────────────

    private void listenToRegistrations() {
        ListenerRegistration reg = db.collection(FirestoreHelper.COL_REGISTRATIONS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Registrations listener error", error);
                        return;
                    }
                    if (snapshots == null || snapshots.isEmpty()) return;

                    DatabaseHelper dbHelper = DatabaseHelper.getInstance(appContext);
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.REMOVED) continue;
                        com.google.firebase.firestore.DocumentSnapshot d = dc.getDocument();
                        String sid       = str(d, "student_id");
                        int    eventId   = intVal(d, "event_id");
                        String timestamp = str(d, "timestamp");
                        if (sid == null || sid.isEmpty() || eventId <= 0) continue;
                        dbHelper.syncUpsertRegistration(sid, eventId, timestamp);
                    }
                    notifyChange();
                });
        listeners.add(reg);
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    private void listenToUsers() {
        ListenerRegistration reg = db.collection(FirestoreHelper.COL_USERS)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Users listener error", error);
                        return;
                    }
                    if (snapshots == null || snapshots.isEmpty()) return;

                    DatabaseHelper dbHelper = DatabaseHelper.getInstance(appContext);
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.REMOVED) continue;
                        com.google.firebase.firestore.DocumentSnapshot d = dc.getDocument();
                        String sid        = str(d, "student_id");
                        String name       = str(d, "name");
                        String email      = str(d, "email");
                        String role       = str(d, "role");
                        String dept       = str(d, "department");
                        String gender     = str(d, "gender");
                        String mobile     = str(d, "mobile");
                        String profileImg = str(d, "profile_image");
                        String notifPref  = str(d, "notif_pref");
                        // Password is never synced from Firestore to local DB.
                        boolean emailVerified = intVal(d, "email_verified") == 1;
                        if (sid == null || sid.isEmpty()) continue;
                        dbHelper.syncUpsertUser(sid, name, email, role, dept,
                                gender, mobile, profileImg, notifPref, null, emailVerified);
                    }
                    notifyChange();
                });
        listeners.add(reg);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void notifyChange() {
        if (onChangeCallback != null) {
            mainHandler.post(onChangeCallback);
        }
    }

    private static String str(com.google.firebase.firestore.DocumentSnapshot d, String field) {
        Object v = d.get(field);
        return v != null ? v.toString() : "";
    }

    private static int intVal(com.google.firebase.firestore.DocumentSnapshot d, String field) {
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
}
