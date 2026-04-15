package com.example.campus_event_org_hub.data;

import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around Firestore.
 *
 * Collection layout (mirrors the SQLite tables):
 *   users/          — doc id = Firebase Auth UID (student_id stored as a field)
 *   events/         — doc id = String.valueOf(local SQLite id)
 *   registrations/  — doc id = student_id + "_" + event_id
 *   notifications/  — doc id = String.valueOf(local notif_id)
 *
 * All writes are fire-and-forget (addOnFailureListener logs the error).
 * Reads are done via SyncManager which calls Tasks.await() off the main thread.
 */
public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";

    public static final String COL_USERS         = "users";
    public static final String COL_EVENTS        = "events";
    public static final String COL_REGISTRATIONS = "registrations";
    public static final String COL_NOTIFICATIONS = "notifications";
    public static final String COL_ATTENDANCE    = "attendance";

    private final FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public FirebaseFirestore getDb() { return db; }

    // ── Users ─────────────────────────────────────────────────────────────────

    public void upsertUser(String firebaseUid, String studentId, String name, String email,
                           String role, String department,
                           String gender, String mobile, String profileImage,
                           String notifPref) {
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Log.w(TAG, "upsertUser skipped: no firebaseUid for studentId=" + studentId);
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("student_id",    studentId);
        data.put("name",          name);
        data.put("email",         email);
        data.put("role",          role);
        data.put("department",    department);
        data.put("gender",        gender   != null ? gender      : "");
        data.put("mobile",        mobile   != null ? mobile      : "");
        data.put("profile_image", profileImage != null ? profileImage : "");
        data.put("notif_pref",    notifPref != null ? notifPref   : "All Events");
        db.collection(COL_USERS).document(firebaseUid)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "upsertUser failed: uid=" + firebaseUid + " sid=" + studentId, e));
    }

    public DocumentSnapshot getUserByUid(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Log.w(TAG, "getUserByUid skipped: no firebaseUid");
            return null;
        }
        try {
            return Tasks.await(db.collection(COL_USERS).document(firebaseUid).get(), 15, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "getUserByUid FAILED for uid=" + firebaseUid + ": " + e.getMessage(), e);
            return null;
        }
    }

    public void upsertUserWithVerification(String firebaseUid, String studentId, String name, String email,
                                            String role, String department,
                                            String gender, String mobile, String profileImage,
                                            String notifPref, boolean emailVerified,
                                            String verificationToken, String hashedPassword) {
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Log.w(TAG, "upsertUserWithVerification skipped: no firebaseUid for studentId=" + studentId);
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("student_id",              studentId);
        data.put("name",                    name);
        data.put("email",                   email);
        data.put("role",                    role);
        data.put("department",              department);
        data.put("gender",        gender   != null ? gender      : "");
        data.put("mobile",        mobile   != null ? mobile      : "");
        data.put("profile_image", profileImage != null ? profileImage : "");
        data.put("notif_pref",    notifPref != null ? notifPref   : "All Events");
        data.put("email_verified", emailVerified ? 1 : 0);
        data.put("verification_token", verificationToken != null ? verificationToken : "");
        if (hashedPassword != null && !hashedPassword.isEmpty()) {
            data.put("password", hashedPassword);
        }
        db.collection(COL_USERS).document(firebaseUid)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "upsertUserWithVerification failed: uid=" + firebaseUid + " sid=" + studentId, e));
    }

    public void updateUserField(String firebaseUid, String field, Object value) {
        // Use set(merge) instead of update() so this succeeds even if the field
        // never existed on the document (e.g. profile_image on older user docs).
        java.util.Map<String, Object> patch = new java.util.HashMap<>();
        patch.put(field, value);

        // Guard: if the user is not signed into Firebase Auth the write will be
        // rejected by security rules (request.auth == null).  Log a clear warning
        // so this is easy to spot in Logcat.
        com.google.firebase.auth.FirebaseUser currentUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "updateUserField: NO Firebase Auth session — write will likely fail"
                    + " (field=" + field + ", firebaseUid=" + firebaseUid + ")");
        }

        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Log.w(TAG, "updateUserField skipped: no firebaseUid (field=" + field + ")");
            return;
        }

        String preview = (value instanceof String && ((String) value).length() > 60)
                ? ((String) value).substring(0, 60) + "..." : String.valueOf(value);
        db.collection(COL_USERS).document(firebaseUid)
                .set(patch, SetOptions.merge())
                .addOnSuccessListener(v -> Log.d(TAG,
                        "updateUserField OK: " + field + "=" + preview + " for uid=" + firebaseUid))
                .addOnFailureListener(e -> Log.e(TAG,
                        "updateUserField FAILED: " + field + " for uid=" + firebaseUid, e));
    }

    /**
     * Store the device's FCM registration token in the user's Firestore document.
     * The Cloud Function reads this token to address push notifications to the correct device.
     */
    public void saveFcmToken(String firebaseUid, String token) {
        if (firebaseUid == null || firebaseUid.isEmpty() || token == null || token.isEmpty()) return;
        db.collection(COL_USERS).document(firebaseUid)
                .update("fcm_token", token)
                .addOnFailureListener(e ->
                        // Document may not exist yet — use set(merge) as fallback
                        db.collection(COL_USERS).document(firebaseUid)
                                .set(java.util.Collections.singletonMap("fcm_token", token),
                                        SetOptions.merge())
                                .addOnFailureListener(e2 ->
                                        Log.e(TAG, "saveFcmToken failed: uid=" + firebaseUid, e2)));
    }

    /**
     * Sync a changed password to Firestore so other devices can pull it on next login sync.
     * The password is stored in the users document under the "password" field.
     */
    public void updatePassword(String firebaseUid, String newPassword) {
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Log.w(TAG, "updatePassword skipped: no firebaseUid");
            return;
        }
        db.collection(COL_USERS).document(firebaseUid)
                .update("password", newPassword)
                .addOnFailureListener(e -> Log.e(TAG, "updatePassword failed: uid=" + firebaseUid, e));
    }

    public void deleteUser(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Log.w(TAG, "deleteUser skipped: no firebaseUid");
            return;
        }
        db.collection(COL_USERS).document(firebaseUid)
                .delete()
                .addOnFailureListener(e -> Log.e(TAG, "deleteUser failed: uid=" + firebaseUid, e));
    }

    /**
     * Delete all Firestore registration documents belonging to a student.
     * Fire-and-forget — called alongside the local SQLite delete.
     */
    public void deleteUserRegistrations(String studentId) {
        db.collection(COL_REGISTRATIONS)
                .whereEqualTo("student_id", studentId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        doc.getReference().delete()
                                .addOnFailureListener(e -> Log.e(TAG, "deleteUserRegistration doc failed", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "deleteUserRegistrations query failed: " + studentId, e));
    }

    /**
     * Delete all Firestore notification documents addressed to a student.
     * Fire-and-forget — called alongside the local SQLite delete.
     */
    public void deleteUserNotifications(String studentId) {
        db.collection(COL_NOTIFICATIONS)
                .whereEqualTo("recipient_sid", studentId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        doc.getReference().delete()
                                .addOnFailureListener(e -> Log.e(TAG, "deleteUserNotification doc failed", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "deleteUserNotifications query failed: " + studentId, e));
    }

    /**
     * Wipe all data from every Firestore collection.
     * Admin user documents (role == "Admin") are preserved.
     * MUST be called from a background thread — uses Tasks.await() internally.
     */
    public void deleteAllData() throws Exception {
        deleteCollection(COL_REGISTRATIONS, null, null);
        deleteCollection(COL_NOTIFICATIONS, null, null);
        deleteCollection(COL_EVENTS, null, null);
        deleteCollection(COL_USERS, "role", "Admin"); // keep admins
    }

    /**
     * Delete all documents in a Firestore collection, optionally skipping documents
     * where {@code skipField} equals {@code skipValue}.
     * Uses WriteBatch (max 500 per commit) and Tasks.await() — background thread only.
     */
    private void deleteCollection(String collectionName, String skipField, String skipValue)
            throws Exception {
        QuerySnapshot snapshot = Tasks.await(
                db.collection(collectionName).get(), 30, TimeUnit.SECONDS);
        WriteBatch batch = db.batch();
        int count = 0;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            if (skipField != null) {
                String val = doc.getString(skipField);
                if (skipValue.equals(val)) continue;
            }
            batch.delete(doc.getReference());
            count++;
            if (count == 500) {
                Tasks.await(batch.commit(), 30, TimeUnit.SECONDS);
                batch = db.batch();
                count = 0;
            }
        }
        if (count > 0) {
            Tasks.await(batch.commit(), 30, TimeUnit.SECONDS);
        }
    }

    // ── Events ────────────────────────────────────────────────────────────────

    /**
     * Upsert an event. docId = String.valueOf(localSqliteId).
     */
    public void upsertEvent(int localId, String title, String description,
                            String date, String time, String tags,
                            String organizer, String category,
                            String imagePath, String status, String creatorSid,
                            String startTime, String endTime) {
        upsertEvent(localId, title, description, date, time, tags, organizer, category,
                imagePath, status, creatorSid, startTime, endTime, null, null);
    }

    public void upsertEvent(int localId, String title, String description,
                            String date, String time, String tags,
                            String organizer, String category,
                            String imagePath, String status, String creatorSid,
                            String startTime, String endTime,
                            String timeInCode, String timeOutCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("local_id",      localId);
        data.put("title",         title);
        data.put("description",   description);
        data.put("date",          date);
        data.put("event_time",    time      != null ? time      : "");
        data.put("start_time",    startTime != null ? startTime : "");
        data.put("end_time",      endTime   != null ? endTime   : "");
        data.put("tags",          tags      != null ? tags      : "");
        data.put("organizer",     organizer);
        data.put("category",      category);
        data.put("image_path",    imagePath  != null ? imagePath  : "");
        data.put("status",        status);
        data.put("creator_sid",   creatorSid != null ? creatorSid : "");
        data.put("time_in_code",  timeInCode  != null ? timeInCode  : "");
        data.put("time_out_code", timeOutCode != null ? timeOutCode : "");
        db.collection(COL_EVENTS).document(String.valueOf(localId))
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "upsertEvent failed: " + localId, e));
    }

    /** Push updated attendance codes to Firestore without touching other fields. */
    public void updateEventAttendanceCodes(int localId, String timeInCode, String timeOutCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("time_in_code",  timeInCode  != null ? timeInCode  : "");
        data.put("time_out_code", timeOutCode != null ? timeOutCode : "");
        db.collection(COL_EVENTS).document(String.valueOf(localId))
                .update(data)
                .addOnFailureListener(e -> Log.e(TAG, "updateEventAttendanceCodes failed: " + localId, e));
    }

    public void updateEventStatus(int localId, String status) {
        db.collection(COL_EVENTS).document(String.valueOf(localId))
                .update("status", status)
                .addOnFailureListener(e -> Log.e(TAG, "updateEventStatus failed", e));
    }

    public void updateEventDateAndStatus(int localId, String date, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("date",   date);
        data.put("status", status);
        db.collection(COL_EVENTS).document(String.valueOf(localId))
                .update(data)
                .addOnFailureListener(e -> Log.e(TAG, "updateEventDateAndStatus failed", e));
    }

    public void updateEventDateTimeAndStatus(int localId, String date, String time, String status) {
        Map<String, Object> data = new HashMap<>();
        data.put("date",       date);
        data.put("event_time", time  != null ? time  : "");
        data.put("status",     status);
        db.collection(COL_EVENTS).document(String.valueOf(localId))
                .update(data)
                .addOnFailureListener(e -> Log.e(TAG, "updateEventDateTimeAndStatus failed", e));
    }

    public void updateEventFields(int localId, String title, String description,
                                  String date, String time, String startTime, String endTime,
                                  String tags, String organizer, String category) {
        Map<String, Object> data = new HashMap<>();
        data.put("title",       title);
        data.put("description", description);
        data.put("date",        date);
        data.put("event_time",  time      != null ? time      : "");
        data.put("start_time",  startTime != null ? startTime : "");
        data.put("end_time",    endTime   != null ? endTime   : "");
        data.put("tags",        tags      != null ? tags      : "");
        data.put("organizer",   organizer);
        data.put("category",    category);
        data.put("updated_at",  FieldValue.serverTimestamp()); // optimistic-lock sentinel
        db.collection(COL_EVENTS).document(String.valueOf(localId))
                .update(data)
                .addOnFailureListener(e -> Log.e(TAG, "updateEventFields failed", e));
    }

    /**
     * Fetches a single event from Firestore and upserts it into local SQLite.
     * Call this on a background thread. Returns the event ID on success, -1 on failure.
     */
    public int fetchEventById(int localId, android.content.Context context) {
        try {
            DocumentSnapshot snap = Tasks.await(
                    db.collection(COL_EVENTS).document(String.valueOf(localId)).get(),
                    10, java.util.concurrent.TimeUnit.SECONDS);
            if (!snap.exists()) return -1;

            DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
            String title      = snap.getString("title");
            String desc       = snap.getString("description");
            String date       = snap.getString("date");
            String time       = snap.getString("event_time");
            String startTime  = snap.getString("start_time");
            String endTime    = snap.getString("end_time");
            String tags       = snap.getString("tags");
            String organizer  = snap.getString("organizer");
            String category   = snap.getString("category");
            String imagePath  = snap.getString("image_path");
            String status     = snap.getString("status");
            String creatorSid = snap.getString("creator_sid");
            String timeInCode  = snap.getString("time_in_code");
            String timeOutCode = snap.getString("time_out_code");

            dbHelper.syncUpsertEvent(localId, title, desc, date, time, tags,
                    organizer, category, imagePath, status, creatorSid,
                    startTime, endTime, timeInCode, timeOutCode);
            return localId;
        } catch (Exception e) {
            Log.e(TAG, "fetchEventById failed", e);
            return -1;
        }
    }

    /**
     * Read the current {@code updated_at} timestamp for an event document.
     * Returns null if the document does not exist or has no timestamp.
     * MUST be called from a background thread.
     */
    public com.google.firebase.Timestamp getEventUpdatedAt(int localId) {
        try {
            DocumentSnapshot snap = Tasks.await(
                    db.collection(COL_EVENTS).document(String.valueOf(localId)).get(),
                    10, java.util.concurrent.TimeUnit.SECONDS);
            if (snap.exists()) {
                return snap.getTimestamp("updated_at");
            }
        } catch (Exception e) {
            Log.e(TAG, "getEventUpdatedAt failed", e);
        }
        return null;
    }

    /**
     * Conflict-safe event update using a Firestore transaction.
     * Reads the document inside the transaction; if {@code expectedUpdatedAt} doesn't match
     * the server value, aborts and calls {@code onConflict}. Otherwise writes the fields
     * and calls {@code onSuccess}.
     *
     * Pass {@code expectedUpdatedAt = null} to skip the conflict check (first-time write).
     */
    public void updateEventFieldsWithConflictCheck(
            int localId,
            String title, String description, String date, String time,
            String startTime, String endTime,
            String tags, String organizer, String category,
            com.google.firebase.Timestamp expectedUpdatedAt,
            Runnable onSuccess, Runnable onConflict) {

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference ref =
                    db.collection(COL_EVENTS).document(String.valueOf(localId));
            DocumentSnapshot snap = transaction.get(ref);

            if (expectedUpdatedAt != null) {
                com.google.firebase.Timestamp current = snap.getTimestamp("updated_at");
                // If server timestamp differs from what admin read, another edit happened.
                if (current != null && !current.equals(expectedUpdatedAt)) {
                    throw new com.google.firebase.firestore.FirebaseFirestoreException(
                            "CONFLICT",
                            com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("title",       title);
            data.put("description", description);
            data.put("date",        date);
            data.put("event_time",  time      != null ? time      : "");
            data.put("start_time",  startTime != null ? startTime : "");
            data.put("end_time",    endTime   != null ? endTime   : "");
            data.put("tags",        tags      != null ? tags      : "");
            data.put("organizer",   organizer);
            data.put("category",    category);
            data.put("updated_at",  FieldValue.serverTimestamp());
            transaction.update(ref, data);
            return null;
        }).addOnSuccessListener(v -> {
            if (onSuccess != null) onSuccess.run();
        }).addOnFailureListener(e -> {
            if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                com.google.firebase.firestore.FirebaseFirestoreException ffe =
                        (com.google.firebase.firestore.FirebaseFirestoreException) e;
                if (ffe.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED) {
                    if (onConflict != null) onConflict.run();
                    return;
                }
            }
            Log.e(TAG, "updateEventFieldsWithConflictCheck failed", e);
        });
    }

    public void deleteEvent(int localId) {
        db.collection(COL_EVENTS).document(String.valueOf(localId))
                .delete()
                .addOnFailureListener(e -> Log.e(TAG, "deleteEvent failed", e));
    }

    /** Delete all Firestore registrations whose event_id matches the given local event ID. */
    public void deleteEventRegistrations(int eventId) {
        db.collection(COL_REGISTRATIONS)
                .whereEqualTo("event_id", (long) eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        doc.getReference().delete()
                                .addOnFailureListener(e -> Log.e(TAG, "deleteEventRegistration doc failed", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "deleteEventRegistrations query failed: " + eventId, e));
    }

    /** Delete all Firestore notifications whose event_id matches the given local event ID. */
    public void deleteEventNotifications(int eventId) {
        db.collection(COL_NOTIFICATIONS)
                .whereEqualTo("event_id", (long) eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        doc.getReference().delete()
                                .addOnFailureListener(e -> Log.e(TAG, "deleteEventNotification doc failed", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "deleteEventNotifications query failed: " + eventId, e));
    }

    // ── Registrations ─────────────────────────────────────────────────────────

    public void upsertRegistration(String studentId, int eventId, String timestamp) {
        String docId = studentId + "_" + eventId;
        Map<String, Object> data = new HashMap<>();
        data.put("student_id",  studentId);
        data.put("event_id",    (long) eventId);
        data.put("timestamp",   timestamp);
        db.collection(COL_REGISTRATIONS).document(docId)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "upsertRegistration failed", e));
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    public void upsertNotification(int localNotifId, String recipientSid, int eventId,
                                   String type, String message, String reason,
                                   String suggestedDate, String suggestedTime,
                                   String instructions,
                                   boolean isRead, String createdAt) {
        Map<String, Object> data = new HashMap<>();
        data.put("local_notif_id",  localNotifId);
        data.put("recipient_sid",   recipientSid);
        data.put("event_id",        (long) eventId);
        data.put("type",            type);
        data.put("message",         message);
        data.put("reason",          reason        != null ? reason        : "");
        data.put("suggested_date",  suggestedDate != null ? suggestedDate : "");
        data.put("suggested_time",  suggestedTime != null ? suggestedTime : "");
        data.put("instructions",    instructions  != null ? instructions  : "");
        data.put("is_read",         isRead ? 1 : 0);
        data.put("created_at",      createdAt);
        db.collection(COL_NOTIFICATIONS).document(String.valueOf(localNotifId))
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "upsertNotification failed: " + localNotifId, e));
    }

    public void markNotificationRead(int localNotifId) {
        db.collection(COL_NOTIFICATIONS).document(String.valueOf(localNotifId))
                .update("is_read", 1)
                .addOnFailureListener(e -> Log.e(TAG, "markNotificationRead failed", e));
    }
}
