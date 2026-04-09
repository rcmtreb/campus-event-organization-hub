package com.example.campus_event_org_hub.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.FirestoreHelper;
import com.example.campus_event_org_hub.ui.auth.LoginActivity;
import com.example.campus_event_org_hub.ui.main.MainActivity;
import com.example.campus_event_org_hub.util.SessionManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Handles incoming FCM messages and token refresh events.
 *
 * Notification channels:
 *   CHANNEL_EVENTS   — new/updated event alerts (students)
 *   CHANNEL_OFFICER  — event decision alerts (officers)
 *   CHANNEL_ADMIN    — pending approval alerts (admin)
 */
public class CEOHFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "CEOHFCMService";

    public static final String CHANNEL_EVENTS  = "ceoh_events";
    public static final String CHANNEL_OFFICER = "ceoh_officer";
    public static final String CHANNEL_ADMIN   = "ceoh_admin";

    // ── Token refresh ────────────────────────────────────────────────────────

    /**
     * Called whenever FCM generates a new registration token (first install, token rotation).
     * Save the token to Firestore so the Cloud Function can address this device.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        // Persist the token so LoginActivity can upload it; also upload immediately if
        // a session already exists (e.g. token rotated while the user is logged in).
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            String uid = session.getFirebaseUid();
            if (uid != null && !uid.isEmpty()) {
                new FirestoreHelper().saveFcmToken(uid, token);
            }
        }
        // Always save locally so the next login can upload it
        getSharedPreferences("ceoh_fcm", MODE_PRIVATE)
                .edit()
                .putString("pending_token", token)
                .apply();
    }

    // ── Message received ─────────────────────────────────────────────────────

    /**
     * Called when a message arrives while the app is in the FOREGROUND.
     * Background/killed-state messages are handled automatically by the system using
     * the notification payload set by the Cloud Function.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        RemoteMessage.Notification notif = message.getNotification();
        if (notif == null) return; // data-only message — ignore for now

        String title   = notif.getTitle()   != null ? notif.getTitle()   : "CEOH";
        String body    = notif.getBody()    != null ? notif.getBody()    : "";
        String channel = pickChannel(message.getData().get("type"));

        showNotification(title, body, channel);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Map notification type string to the correct channel. */
    private String pickChannel(String type) {
        if (type == null) return CHANNEL_EVENTS;
        switch (type) {
            case "APPROVED":
            case "REJECTED":
            case "POSTPONED":
            case "CANCELLED":
                return CHANNEL_OFFICER;
            case "NEW_PENDING":
            case "RESUBMIT":
                return CHANNEL_ADMIN;
            default:
                return CHANNEL_EVENTS; // NEW_EVENT and anything else
        }
    }

    private void showNotification(String title, String body, String channelId) {
        ensureChannels();

        // Fix Bug 5: route NEW_EVENT taps straight to the Events tab in MainActivity.
        // All other notification types (APPROVED/REJECTED officer decisions, admin alerts)
        // still go through LoginActivity which handles auto-login redirect.
        final Intent intent;
        if (CHANNEL_EVENTS.equals(channelId)) {
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("OPEN_TAB", 1); // 1 = Events tab
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            // Use a unique id derived from time so multiple pushes stack
            nm.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    /** Create notification channels (safe to call multiple times — no-op if already created). */
    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_EVENTS,
                "Event Announcements",
                NotificationManager.IMPORTANCE_HIGH));

        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_OFFICER,
                "My Events",
                NotificationManager.IMPORTANCE_HIGH));

        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_ADMIN,
                "Pending Approvals",
                NotificationManager.IMPORTANCE_HIGH));
    }

    private void ensureChannels() {
        ensureChannels(this);
    }
}
