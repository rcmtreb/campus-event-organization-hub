package com.example.campus_event_org_hub.data;

import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
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
 *   users/          — doc id = student_id
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

    private final FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public FirebaseFirestore getDb() { return db; }

    // ── Users ─────────────────────────────────────────────────────────────────

    public void upsertUser(String studentId, String name, String email,
                           String role, String department,
                           String gender, String mobile, String profileImage,
                           String notifPref) {
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
        db.collection(COL_USERS).document(studentId)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "upsertUser failed: " + studentId, e));
    }

    public void upsertUserWithVerification(String studentId, String name, String email,
                                           String role, String department,
                                           String gender, String mobile, String profileImage,
                                           String notifPref, boolean emailVerified,
                                           String verificationToken, String hashedPassword) {
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
        db.collection(COL_USERS).document(studentId)
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "upsertUserWithVerification failed: " + studentId, e));
    }

    public void updateUserField(String studentId, String field, Object value) {
        db.collection(COL_USERS).document(studentId)
                .update(field, value)
                .addOnFailureListener(e -> Log.e(TAG, "updateUserField failed", e));
    }

    /**
     * Store the device's FCM registration token in the user's Firestore document.
     * The Cloud Function reads this token to address push notifications to the correct device.
     */
    public void saveFcmToken(String studentId, String token) {
        if (studentId == null || studentId.isEmpty() || token == null || token.isEmpty()) return;
        db.collection(COL_USERS).document(studentId)
                .update("fcm_token", token)
                .addOnFailureListener(e ->
                        // Document may not exist yet — use set(merge) as fallback
                        db.collection(COL_USERS).document(studentId)
                                .set(java.util.Collections.singletonMap("fcm_token", token),
                                        SetOptions.merge())
                                .addOnFailureListener(e2 ->
                                        Log.e(TAG, "saveFcmToken failed: " + studentId, e2)));
    }

    /**
     * Sync a changed password to Firestore so other devices can pull it on next login sync.
     * The password is stored in the users document under the "password" field.
     */
    public void updatePassword(String studentId, String newPassword) {
        db.collection(COL_USERS).document(studentId)
                .update("password", newPassword)
                .addOnFailureListener(e -> Log.e(TAG, "updatePassword failed: " + studentId, e));
    }

    public void deleteUser(String studentId) {
        db.collection(COL_USERS).document(studentId)
                .delete()
                .addOnFailureListener(e -> Log.e(TAG, "deleteUser failed: " + studentId, e));
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
        Map<String, Object> data = new HashMap<>();
        data.put("local_id",    localId);
        data.put("title",       title);
        data.put("description", description);
        data.put("date",        date);
        data.put("event_time",  time    != null ? time    : "");
        data.put("start_time",  startTime != null ? startTime : "");
        data.put("end_time",    endTime != null ? endTime : "");
        data.put("tags",        tags    != null ? tags    : "");
        data.put("organizer",   organizer);
        data.put("category",    category);
        data.put("image_path",  imagePath != null ? imagePath : "");
        data.put("status",      status);
        data.put("creator_sid", creatorSid != null ? creatorSid : "");
        db.collection(COL_EVENTS).document(String.valueOf(localId))
                .set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "upsertEvent failed: " + localId, e));
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
                                  String date, String time, String tags,
                                  String organizer, String category) {
        Map<String, Object> data = new HashMap<>();
        data.put("title",       title);
        data.put("description", description);
        data.put("date",        date);
        data.put("event_time",  time     != null ? time     : "");
        data.put("tags",        tags     != null ? tags     : "");
        data.put("organizer",   organizer);
        data.put("category",    category);
        db.collection(COL_EVENTS).document(String.valueOf(localId))
                .update(data)
                .addOnFailureListener(e -> Log.e(TAG, "updateEventFields failed", e));
    }

    public void deleteEvent(int localId) {
        db.collection(COL_EVENTS).document(String.valueOf(localId))
                .delete()
                .addOnFailureListener(e -> Log.e(TAG, "deleteEvent failed", e));
    }

    // ── Registrations ─────────────────────────────────────────────────────────

    public void upsertRegistration(String studentId, int eventId, String timestamp) {
        String docId = studentId + "_" + eventId;
        Map<String, Object> data = new HashMap<>();
        data.put("student_id",  studentId);
        data.put("event_id",    eventId);
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
        data.put("event_id",        eventId);
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
