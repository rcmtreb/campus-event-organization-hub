package com.example.campus_event_org_hub.ui.base;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

public abstract class BaseActivity extends AppCompatActivity {

    private boolean exitPressedOnce = false;
    private final Handler exitHandler = new Handler(Looper.getMainLooper());
    private static final int EXIT_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (exitPressedOnce) {
                    exitHandler.removeCallbacksAndMessages(null);
                    finish();
                    return;
                }

                exitPressedOnce = true;
                View rootView = findViewById(android.R.id.content);
                Snackbar.make(rootView, "Press back again to exit", Snackbar.LENGTH_LONG).show();

                exitHandler.postDelayed(() -> exitPressedOnce = false, EXIT_DELAY_MS);
            }
        });
    }
}
