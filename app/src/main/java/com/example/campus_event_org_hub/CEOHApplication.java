package com.example.campus_event_org_hub;

import android.app.Application;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class CEOHApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Force Light Mode globally at the start of the application
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Enable Firestore offline persistence so writes are queued to disk
        // and survive app kills when the device is offline.
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        } catch (Exception e) {
            Log.e("CEOHApplication", "Failed to configure Firestore settings", e);
        }
    }
}
