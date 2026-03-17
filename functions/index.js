/**
 * Cloud Function: sendPushOnNotification
 *
 * Triggers on every new document in the `notifications` Firestore collection.
 * Reads the recipient's FCM token from `users/{recipient_sid}` and sends a
 * push notification via FCM HTTP v1.
 *
 * Notification document fields (written by DatabaseHelper.insertNotification):
 *   recipient_sid   — student_id string, or "admin" sentinel for the admin account
 *   event_id        — int
 *   type            — one of: NEW_EVENT, APPROVED, REJECTED, POSTPONED,
 *                              CANCELLED, NEW_PENDING, RESUBMIT
 *   message         — human-readable body text (already formatted with emojis/details)
 *   reason          — optional rejection/postpone reason
 *   suggested_date  — optional new date string
 *   suggested_time  — optional new time string
 *   instructions    — optional instructions string
 *   is_read         — 0/1
 *   created_at      — "yyyy-MM-dd HH:mm:ss"
 *   local_notif_id  — int (SQLite row id)
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// ── Channel → Android notification channel mapping ───────────────────────────
// Must match CEOHFirebaseMessagingService constants in the Android app.
const CHANNEL_EVENTS  = "ceoh_events";
const CHANNEL_OFFICER = "ceoh_officer";
const CHANNEL_ADMIN   = "ceoh_admin";

/**
 * Map a notification type string to the correct Android notification channel.
 * @param {string} type
 * @return {string}
 */
function pickChannel(type) {
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
      return CHANNEL_EVENTS; // NEW_EVENT and anything unknown
  }
}

/**
 * Map a notification type string to a short notification title.
 * @param {string} type
 * @return {string}
 */
function pickTitle(type) {
  switch (type) {
    case "NEW_EVENT":    return "New Event Available";
    case "APPROVED":     return "Event Approved";
    case "REJECTED":     return "Event Rejected";
    case "POSTPONED":    return "Event Postponed";
    case "CANCELLED":    return "Event Cancelled";
    case "NEW_PENDING":  return "New Event Pending Approval";
    case "RESUBMIT":     return "Officer Re-proposed Event Date";
    default:             return "Campus Event Hub";
  }
}

// ── Main trigger ─────────────────────────────────────────────────────────────

exports.sendPushOnNotification = functions.firestore
    .document("notifications/{notifId}")
    .onCreate(async (snap) => {
      const data = snap.data();
      if (!data) {
        console.warn("sendPushOnNotification: empty document");
        return null;
      }

      const recipientSid = data.recipient_sid;
      const type         = data.type  || "NEW_EVENT";
      const message      = data.message || "";

      if (!recipientSid) {
        console.warn("sendPushOnNotification: missing recipient_sid");
        return null;
      }

      // ── Look up FCM token ───────────────────────────────────────────────────
      let token;
      try {
        const userSnap = await db.collection("users").doc(recipientSid).get();
        if (!userSnap.exists) {
          console.warn(`sendPushOnNotification: no user doc for sid=${recipientSid}`);
          return null;
        }
        token = userSnap.get("fcm_token");
      } catch (err) {
        console.error(`sendPushOnNotification: error fetching user doc for sid=${recipientSid}`, err);
        return null;
      }

      if (!token) {
        console.warn(`sendPushOnNotification: no fcm_token for sid=${recipientSid} — skipping push`);
        return null;
      }

      // ── Build and send FCM message ──────────────────────────────────────────
      const channel = pickChannel(type);
      const title   = pickTitle(type);

      const fcmMessage = {
        token: token,
        notification: {
          title: title,
          body:  message,
        },
        android: {
          // Collapse key groups same-type pushes so the tray doesn't flood.
          collapseKey: type,
          notification: {
            channelId:  channel,
            priority:   "high",
            // Use the app's notification icon (must be in the APK drawable resources).
            icon:        "ic_notifications",
            clickAction: "FLUTTER_NOTIFICATION_CLICK", // no-op for native Android; safe to include
          },
        },
        // Pass the type so onMessageReceived() can pick the right channel in foreground.
        data: {
          type:     type,
          notif_id: String(data.local_notif_id || ""),
          event_id: String(data.event_id       || ""),
        },
      };

      try {
        const response = await messaging.send(fcmMessage);
        console.log(`sendPushOnNotification: sent to sid=${recipientSid}, type=${type}, msgId=${response}`);
      } catch (err) {
        // Log but don't throw — a failed push should not block other work.
        console.error(`sendPushOnNotification: failed to send to sid=${recipientSid}`, err);
      }

      return null;
    });
