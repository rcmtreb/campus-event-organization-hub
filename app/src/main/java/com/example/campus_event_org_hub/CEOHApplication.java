package com.example.campus_event_org_hub;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class CEOHApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Force Light Mode globally at the start of the application
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
