package com.example.campus_event_org_hub.ui.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.util.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerificationPendingActivity extends AppCompatActivity {

    private static final String TAG = "CEOH_EMAIL_VERIFY";

    private TextView tvEmail;
    private TextView tvStatus;
    private Button btnResend;
    private Button btnOpenEmail;
    private String studentId;
    private String email;

    private FirebaseAuth mAuth;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private static final long POLL_INTERVAL_MS = 5000; // check every 5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification_pending);

        mAuth = FirebaseAuth.getInstance();

        tvEmail = findViewById(R.id.tv_verification_email);
        tvStatus = findViewById(R.id.tv_verification_status);
        btnResend = findViewById(R.id.btn_resend_verification);
        btnOpenEmail = findViewById(R.id.btn_open_email_app);

        studentId = getIntent().getStringExtra("STUDENT_ID");
        email = getIntent().getStringExtra("EMAIL");

        if (studentId == null || email == null) {
            SessionManager session = new SessionManager(this);
            studentId = session.getStudentId();
            email = session.getEmail();
        }

        tvEmail.setText(email != null ? email : "");

        // Check if already verified in local DB
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        if (studentId != null && db.isEmailVerified(studentId)) {
            onVerificationConfirmed();
            return;
        }

        btnResend.setOnClickListener(v -> resendVerificationEmail());

        btnOpenEmail.setOnClickListener(v -> openEmailApp());

        // Always send a fresh verification email on arrival so the user has a valid link.
        // This also invalidates any stale links from previous attempts.
        resendVerificationEmail();

        // Start polling for verification
        startVerificationPolling();
    }

    /** Tries to open an email app; falls back to Gmail web if no native app responds. */
    private void openEmailApp() {
        // 1. Try Gmail app directly
        Intent gmail = getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
        if (gmail != null) {
            startActivity(gmail);
            return;
        }
        // 2. Try generic email ACTION_MAIN (works on some OEM launchers)
        Intent emailMain = new Intent(Intent.ACTION_MAIN);
        emailMain.addCategory(Intent.CATEGORY_APP_EMAIL);
        if (emailMain.resolveActivity(getPackageManager()) != null) {
            startActivity(emailMain);
            return;
        }
        // 3. Try mailto: scheme — prompts a chooser on most devices
        Intent mailto = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        if (mailto.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(mailto, "Open email app"));
            return;
        }
        // 4. Last resort: open Gmail in browser
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com")));
        } catch (Exception e) {
            Toast.makeText(this, "Please open your email app manually", Toast.LENGTH_LONG).show();
        }
    }

    private void resendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tvStatus.setText("Session expired. Please register again.");
            return;
        }

        btnResend.setEnabled(false);
        tvStatus.setText("Sending...");

        user.sendEmailVerification()
                .addOnSuccessListener(unused -> {
                    tvStatus.setText("Verification email sent! Check your inbox.");
                    btnResend.setEnabled(true);
                    Log.d(TAG, "Verification email resent to " + email);
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Failed to send email. Please try again.");
                    btnResend.setEnabled(true);
                    Log.e(TAG, "Resend verification failed", e);
                });
    }

    /**
     * Polls Firebase Auth every few seconds to check if the user clicked the
     * verification link. When verified, updates SQLite + Firestore and proceeds.
     */
    private void startVerificationPolling() {
        pollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    // user signed out or session gone — stop polling
                    return;
                }
                user.reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser refreshed = mAuth.getCurrentUser();
                        if (refreshed != null && refreshed.isEmailVerified()) {
                            onVerificationConfirmed();
                            return;
                        }
                    }
                    // Not verified yet — keep polling
                    pollHandler.postDelayed(this, POLL_INTERVAL_MS);
                });
            }
        }, POLL_INTERVAL_MS);
    }

    private void onVerificationConfirmed() {
        // Update local DB + Firestore
        if (studentId != null) {
            DatabaseHelper db = DatabaseHelper.getInstance(this);
            db.setEmailVerified(studentId, true);
        }

        // Sign out of Firebase Auth — we only used it for verification
        mAuth.signOut();

        tvStatus.setText("Email verified!");
        btnResend.setVisibility(View.GONE);
        btnOpenEmail.setVisibility(View.GONE);

        Toast.makeText(this, "Email verified! Please log in.", Toast.LENGTH_LONG).show();

        // Go back to login
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 1500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Also do a single immediate check when user returns from email app
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnSuccessListener(unused -> {
                FirebaseUser refreshed = mAuth.getCurrentUser();
                if (refreshed != null && refreshed.isEmailVerified()) {
                    onVerificationConfirmed();
                }
            });
        } else {
            // Fallback: check local DB
            if (studentId != null) {
                DatabaseHelper db = DatabaseHelper.getInstance(this);
                if (db.isEmailVerified(studentId)) {
                    onVerificationConfirmed();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pollHandler.removeCallbacksAndMessages(null);
    }
}
