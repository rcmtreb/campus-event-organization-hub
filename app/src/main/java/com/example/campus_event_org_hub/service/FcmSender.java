package com.example.campus_event_org_hub.service;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends FCM HTTP v1 push notifications directly from the app using a Firebase
 * service account key (assets/service_account.json).
 *
 * Flow:
 *  1. Read service account JSON from assets
 *  2. Build a signed JWT (RS256) → exchange for OAuth2 access token
 *  3. Cache the token (valid 1 hour)
 *  4. Look up recipient FCM token from Firestore users/{sid}.fcm_token
 *  5. POST to FCM HTTP v1 messages:send endpoint
 *
 * All network work runs on a background thread — never called on main thread.
 */
public class FcmSender {

    private static final String TAG = "FcmSender";
    private static final String ASSET_FILE = "service_account.json";
    private static final String TOKEN_URL  = "https://oauth2.googleapis.com/token";
    private static final String FCM_SCOPE  = "https://www.googleapis.com/auth/firebase.messaging";

    // Cached OAuth2 access token + expiry (epoch seconds)
    private static String sCachedToken;
    private static long   sCachedTokenExpiry; // epoch seconds when token expires

    private static final ExecutorService sExecutor = Executors.newCachedThreadPool();

    // ── Public entry point ────────────────────────────────────────────────────

    /**
     * Looks up the FCM token for {@code recipientSid} from Firestore, then sends a push.
     * Runs entirely on a background thread — safe to call from any context.
     *
     * @param context       Application/Activity context (for asset loading)
     * @param recipientSid  Student ID (or "admin" sentinel)
     * @param type          Notification type string (NEW_EVENT, APPROVED, etc.)
     * @param title         Notification title
     * @param body          Notification body text
     */
    public static void send(Context context, String recipientSid,
                            String type, String title, String body) {
        if (recipientSid == null || recipientSid.isEmpty()) return;

        sExecutor.execute(() -> {
            // 1. Look up FCM token from Firestore — users are keyed by Firebase Auth UID,
            //    so we query by the student_id field to find the recipient's document.
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("student_id", recipientSid)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnap -> {
                        if (querySnap.isEmpty()) {
                            Log.w(TAG, "No user doc for sid=" + recipientSid);
                            return;
                        }
                        DocumentSnapshot snap = querySnap.getDocuments().get(0);
                        String fcmToken = snap.getString("fcm_token");
                        if (fcmToken == null || fcmToken.isEmpty()) {
                            Log.w(TAG, "No fcm_token for sid=" + recipientSid);
                            return;
                        }
                        // 2. Send push on background thread (Firestore callback is on main thread)
                        sExecutor.execute(() -> sendPush(context, fcmToken, type, title, body));
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Firestore lookup failed for sid=" + recipientSid, e));
        });
    }

    // ── Internal: send push given an FCM device token ─────────────────────────

    private static void sendPush(Context context, String fcmToken,
                                  String type, String title, String body) {
        try {
            String projectId   = getProjectId(context);
            String accessToken = getAccessToken(context);
            if (accessToken == null || projectId == null) {
                Log.e(TAG, "Cannot send push — missing accessToken or projectId");
                return;
            }

            String channelId = pickChannel(type);

            // Build FCM HTTP v1 request body
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body",  body);

            JSONObject androidNotification = new JSONObject();
            androidNotification.put("channel_id", channelId);
            androidNotification.put("icon",        "ic_notifications");

            JSONObject androidConfig = new JSONObject();
            androidConfig.put("collapse_key", type);
            androidConfig.put("priority",     "high");
            androidConfig.put("notification", androidNotification);

            JSONObject data = new JSONObject();
            data.put("type", type);

            JSONObject message = new JSONObject();
            message.put("token",        fcmToken);
            message.put("notification", notification);
            message.put("android",      androidConfig);
            message.put("data",         data);

            JSONObject payload = new JSONObject();
            payload.put("message", message);

            // POST to FCM HTTP v1
            String endpoint = "https://fcm.googleapis.com/v1/projects/"
                    + projectId + "/messages:send";
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type",  "application/json; UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);

            byte[] payloadBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(payloadBytes);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Log.d(TAG, "Push sent to sid (token truncated) type=" + type);
            } else {
                InputStream errStream = conn.getErrorStream();
                String errBody = errStream != null
                        ? new String(errStream.readAllBytes(), StandardCharsets.UTF_8) : "";
                Log.e(TAG, "FCM send failed: HTTP " + responseCode + " — " + errBody);
                // If token is invalid/expired, clear cached OAuth token so next call retries
                if (responseCode == 401) sCachedToken = null;
            }
            conn.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "sendPush error", e);
        }
    }

    // ── OAuth2 access token (JWT → token exchange) ────────────────────────────

    /**
     * Returns a valid OAuth2 access token, using the cached one if still fresh.
     * Obtains a new one (via signed JWT) otherwise.
     */
    private static synchronized String getAccessToken(Context context) {
        long nowSeconds = System.currentTimeMillis() / 1000L;
        // Use cached token if it has more than 5 minutes remaining
        if (sCachedToken != null && sCachedTokenExpiry - nowSeconds > 300) {
            return sCachedToken;
        }
        try {
            JSONObject sa        = readServiceAccount(context);
            String clientEmail   = sa.getString("client_email");
            String privateKeyPem = sa.getString("private_key");

            // Build JWT
            String jwt = buildJwt(clientEmail, privateKeyPem, nowSeconds);

            // Exchange JWT for access token
            String postBody = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer"
                    + "&assertion=" + jwt;
            URL url = new URL(TOKEN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.getOutputStream().write(postBody.getBytes(StandardCharsets.UTF_8));

            int code = conn.getResponseCode();
            if (code != 200) {
                String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                Log.e(TAG, "Token exchange failed: HTTP " + code + " — " + err);
                conn.disconnect();
                return null;
            }
            String responseBody = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            conn.disconnect();

            JSONObject tokenJson = new JSONObject(responseBody);
            sCachedToken        = tokenJson.getString("access_token");
            int expiresIn       = tokenJson.optInt("expires_in", 3600);
            sCachedTokenExpiry  = nowSeconds + expiresIn;
            return sCachedToken;

        } catch (Exception e) {
            Log.e(TAG, "getAccessToken error", e);
            return null;
        }
    }

    // ── JWT builder (RS256) ───────────────────────────────────────────────────

    private static String buildJwt(String clientEmail, String privateKeyPem, long nowSeconds)
            throws Exception {
        // Header
        String header = base64url(("{\"alg\":\"RS256\",\"typ\":\"JWT\"}").getBytes(StandardCharsets.UTF_8));

        // Claims
        JSONObject claims = new JSONObject();
        claims.put("iss",   clientEmail);
        claims.put("scope", FCM_SCOPE);
        claims.put("aud",   TOKEN_URL);
        claims.put("iat",   nowSeconds);
        claims.put("exp",   nowSeconds + 3600);
        String claimsPart = base64url(claims.toString().getBytes(StandardCharsets.UTF_8));

        String signingInput = header + "." + claimsPart;

        // Sign with RS256
        PrivateKey privateKey = loadPrivateKey(privateKeyPem);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        String signature = base64url(sig.sign());

        return signingInput + "." + signature;
    }

    private static PrivateKey loadPrivateKey(String pem) throws Exception {
        // Strip PEM headers and whitespace
        String stripped = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.decode(stripped, Base64.DEFAULT);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    // ── Asset helpers ─────────────────────────────────────────────────────────

    private static JSONObject readServiceAccount(Context context) throws Exception {
        try (InputStream is = context.getAssets().open(ASSET_FILE)) {
            byte[] bytes = is.readAllBytes();
            return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    private static String getProjectId(Context context) {
        try {
            return readServiceAccount(context).getString("project_id");
        } catch (Exception e) {
            Log.e(TAG, "getProjectId failed", e);
            return null;
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private static String base64url(byte[] data) {
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    /** Map notification type → Android notification channel ID. */
    private static String pickChannel(String type) {
        if (type == null) return CEOHFirebaseMessagingService.CHANNEL_EVENTS;
        switch (type) {
            case "APPROVED":
            case "REJECTED":
            case "POSTPONED":
            case "CANCELLED":
                return CEOHFirebaseMessagingService.CHANNEL_OFFICER;
            case "NEW_PENDING":
            case "RESUBMIT":
                return CEOHFirebaseMessagingService.CHANNEL_ADMIN;
            default:
                return CEOHFirebaseMessagingService.CHANNEL_EVENTS;
        }
    }

    /** Map notification type → human-readable title. */
    public static String pickTitle(String type) {
        if (type == null) return "Campus Event Hub";
        switch (type) {
            case "NEW_EVENT":   return "New Event Available";
            case "APPROVED":    return "Event Approved";
            case "REJECTED":    return "Event Rejected";
            case "POSTPONED":   return "Event Postponed";
            case "CANCELLED":   return "Event Cancelled";
            case "NEW_PENDING": return "New Event Pending Approval";
            case "RESUBMIT":    return "Officer Re-proposed Event Date";
            default:            return "Campus Event Hub";
        }
    }
}
