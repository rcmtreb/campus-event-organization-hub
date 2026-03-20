package com.example.campus_event_org_hub.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * NetworkChangeReceiver — a BroadcastReceiver that listens for
 * {@link ConnectivityManager#CONNECTIVITY_ACTION} system events.
 *
 * <p>When the device goes offline or comes back online, this receiver
 * fires {@link #ACTION_CONNECTIVITY_CHANGED} as a local broadcast so
 * that {@code MainActivity} can show or hide the offline banner.
 *
 * <p>Registration:
 * <ul>
 *   <li>Statically declared in {@code AndroidManifest.xml} so the system can
 *       deliver the broadcast even when the app is in the background.</li>
 *   <li>Also registered/unregistered dynamically in {@code MainActivity}
 *       {@code onResume}/{@code onPause} for foreground updates.</li>
 * </ul>
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "CEOH_NETWORK";

    /** Local broadcast action emitted after connectivity is evaluated. */
    public static final String ACTION_CONNECTIVITY_CHANGED =
            "com.example.campus_event_org_hub.CONNECTIVITY_CHANGED";

    /** Extra key: {@code true} = connected, {@code false} = offline. */
    public static final String EXTRA_IS_CONNECTED = "is_connected";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            return;
        }

        boolean connected = isConnected(context);
        Log.d(TAG, "Network state changed — connected=" + connected);

        // Relay to MainActivity (and any other registered listeners) via LocalBroadcastManager
        Intent local = new Intent(ACTION_CONNECTIVITY_CHANGED);
        local.putExtra(EXTRA_IS_CONNECTED, connected);
        androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(context)
                .sendBroadcast(local);
    }

    /**
     * Returns {@code true} when an active, connected network is available.
     *
     * @param context any valid context
     */
    public static boolean isConnected(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "isConnected check failed", e);
            return true; // Assume connected if we can't check
        }
    }
}
