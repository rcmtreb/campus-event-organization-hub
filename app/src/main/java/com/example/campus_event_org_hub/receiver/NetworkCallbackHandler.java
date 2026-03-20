package com.example.campus_event_org_hub.receiver;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

/**
 * NetworkCallbackHandler — uses NetworkCallback API (Android 7+/API 24+) to detect
 * network connectivity changes. This replaces the deprecated CONNECTIVITY_CHANGE
 * broadcast which no longer works reliably on modern Android versions.
 */
public class NetworkCallbackHandler {

    private static final String TAG = "CEOH_NETWORK";

    public interface ConnectivityListener {
        void onConnectivityChanged(boolean isConnected);
    }

    private final ConnectivityManager connectivityManager;
    private final ConnectivityListener listener;
    private final NetworkCallback networkCallback;
    private boolean isInitialCallback = true;

    public NetworkCallbackHandler(Context context, ConnectivityListener listener) {
        this.connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listener = listener;
        this.networkCallback = new NetworkCallback();
    }

    public void register() {
        if (connectivityManager == null) return;
        
        isInitialCallback = true;
        
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        Log.d(TAG, "NetworkCallback registered");
    }

    public void unregister() {
        if (connectivityManager == null) return;
        
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            Log.d(TAG, "NetworkCallback unregistered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregister NetworkCallback", e);
        }
    }

    public boolean isConnected() {
        if (connectivityManager == null) return false;
        
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) return false;
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            Log.d(TAG, "Network available");
            if (isInitialCallback) {
                isInitialCallback = false;
                Log.d(TAG, "Skipping initial callback");
                return;
            }
            if (listener != null) {
                listener.onConnectivityChanged(true);
            }
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            Log.d(TAG, "Network lost");
            isInitialCallback = false;
            if (listener != null) {
                listener.onConnectivityChanged(false);
            }
        }
    }
}
