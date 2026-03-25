package com.example.campus_event_org_hub.util;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides a tamper-resistant current time by computing the offset between
 * the device clock and a Firestore server timestamp.
 *
 * Usage:
 *   ServerTimeUtil.sync();                      // call once at app start / MainActivity.onResume
 *   long serverMs = ServerTimeUtil.nowMillis(); // use everywhere instead of System.currentTimeMillis()
 *   Date serverDate = ServerTimeUtil.now();
 *   String today = ServerTimeUtil.todayString(); // "yyyy-MM-dd" in Locale.US
 *
 * If Firestore is unreachable (offline) the last-known offset is used, which
 * is still better than raw device time after the user manually changes the clock.
 */
public class ServerTimeUtil {

    private static final String TAG = "ServerTimeUtil";
    private static final String COLLECTION = "_server_time";
    private static final String DOC_ID     = "ping";

    /**
     * Offset in milliseconds: serverMs = System.currentTimeMillis() + offsetMs.
     * Volatile so reads across threads are safe without synchronization.
     */
    private static volatile long offsetMs = 0L;

    /** Whether a sync has been attempted at least once this session. */
    private static volatile boolean synced = false;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fire-and-forget: writes a server timestamp to Firestore and reads it back
     * to compute the clock offset.  Safe to call from any thread.
     * The result is stored in {@link #offsetMs} for all subsequent calls to
     * {@link #nowMillis()}.
     */
    public static void sync() {
        long localBefore = System.currentTimeMillis();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection(COLLECTION).document(DOC_ID);

        Map<String, Object> payload = new HashMap<>();
        payload.put("ts", com.google.firebase.firestore.FieldValue.serverTimestamp());

        ref.set(payload, SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                    ref.get().addOnSuccessListener(snap -> {
                        if (snap == null || !snap.exists()) return;
                        Timestamp serverTs = snap.getTimestamp("ts");
                        if (serverTs == null) return;

                        long localAfter  = System.currentTimeMillis();
                        // Midpoint of the round-trip as a proxy for "when the server wrote the ts"
                        long localMid    = (localBefore + localAfter) / 2;
                        long serverMs    = serverTs.toDate().getTime();
                        offsetMs = serverMs - localMid;
                        synced   = true;
                        Log.d(TAG, "Clock offset: " + offsetMs + " ms (server ahead of device by this amount)");
                    }).addOnFailureListener(e ->
                        Log.w(TAG, "Could not read server timestamp — using last offset", e))
                ).addOnFailureListener(e ->
                    Log.w(TAG, "Could not write server-time ping — using last offset", e));
    }

    /** Returns the current time in milliseconds, adjusted by the server offset. */
    public static long nowMillis() {
        return System.currentTimeMillis() + offsetMs;
    }

    /** Returns the current server-corrected {@link Date}. */
    public static Date now() {
        return new Date(nowMillis());
    }

    /**
     * Returns today's date as "yyyy-MM-dd" using Locale.US (UTC-safe),
     * based on server-corrected time.
     */
    public static String todayString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(now());
    }

    /** True once at least one Firestore sync has completed this session. */
    public static boolean isSynced() {
        return synced;
    }
}
