package com.example.campus_event_org_hub.ui.auth;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.FirestoreHelper;
import com.example.campus_event_org_hub.data.SyncManager;
import com.example.campus_event_org_hub.service.CEOHFirebaseMessagingService;
import com.example.campus_event_org_hub.ui.admin.AdminActivity;
import com.example.campus_event_org_hub.ui.main.MainActivity;
import com.example.campus_event_org_hub.util.SessionManager;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "CEOH_LOGIN";

    /** Android 13+ runtime permission launcher for POST_NOTIFICATIONS. */
    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted ->
                    Log.d(TAG, "POST_NOTIFICATIONS granted: " + granted));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure notification channels exist regardless of login state
        CEOHFirebaseMessagingService.ensureChannels(this);

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        // ── Auto-login: if a valid session is already saved, skip the login screen ──
        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            Log.d(TAG, "Valid session found, auto-logging in as: " + session.getRole());
            uploadPendingFcmToken(session.getStudentId());
            launchHome(session.getName(), session.getRole(), session.getDept(),
                    session.getEmail(), session.getStudentId());
            return; // don't show the login UI at all
        }

        setContentView(R.layout.activity_login);

        DatabaseHelper db = new DatabaseHelper(this);

        EditText etEmail        = findViewById(R.id.et_login_email);
        EditText etPassword     = findViewById(R.id.et_login_password);
        Button   btnLogin       = findViewById(R.id.btn_login);
        TextView tvGoToRegister = findViewById(R.id.tv_go_to_register);

        btnLogin.setOnClickListener(v -> {
            String loginInput = etEmail.getText().toString().trim();
            String password   = etPassword.getText().toString().trim();

            Log.d(TAG, "Login attempt with: " + loginInput);

            if (loginInput.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Admin — hardcoded, no sync needed, works fully offline
            if (loginInput.equalsIgnoreCase("admin@ucc.edu.ph") && password.equals("admin123")) {
                Log.d(TAG, "Admin credentials detected.");
                session.saveSession("Admin", "Admin", "Administration",
                        "admin@ucc.edu.ph", "admin");
                // Upload FCM token under the "admin" sentinel so Cloud Function can reach this device
                uploadPendingFcmToken("admin");
                Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }

            btnLogin.setEnabled(false);

            if (isOnline()) {
                // 2a. Online: sync first so new-device users can log in
                Toast.makeText(this, "Syncing data...", Toast.LENGTH_SHORT).show();
                SyncManager.sync(this, () -> {
                    btnLogin.setEnabled(true);
                    attemptLocalLogin(db, session, loginInput, password);
                });
            } else {
                // 2b. Offline: skip sync, go straight to local SQLite check
                btnLogin.setEnabled(true);
                Toast.makeText(this, "No internet — using saved data", Toast.LENGTH_SHORT).show();
                attemptLocalLogin(db, session, loginInput, password);
            }
        });

        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    /**
     * Checks credentials against local SQLite. If found, saves session and launches home.
     * If not found while offline, shows a specific message distinguishing from wrong password.
     */
    private void attemptLocalLogin(DatabaseHelper db, SessionManager session,
                                   String loginInput, String password) {
        try {
            Cursor cursor = db.checkUser(loginInput, password);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx  = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_NAME);
                int roleIdx  = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ROLE);
                int deptIdx  = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_DEPARTMENT);
                int emailIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_EMAIL);
                int sidIdx   = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_STUDENT_ID);

                String name  = nameIdx  >= 0 ? cursor.getString(nameIdx)  : "User";
                String role  = roleIdx  >= 0 ? cursor.getString(roleIdx)  : "Student";
                String dept  = deptIdx  >= 0 ? cursor.getString(deptIdx)  : "General";
                String email = emailIdx >= 0 ? cursor.getString(emailIdx) : "";
                String sid   = sidIdx   >= 0 ? cursor.getString(sidIdx)   : "";

                if (name  == null || name.trim().isEmpty())  name  = "User";
                if (role  == null || role.trim().isEmpty())  role  = "Student";
                if (dept  == null || dept.trim().isEmpty())  dept  = "General";
                if (email == null) email = "";
                if (sid   == null) sid   = "";

                cursor.close();
                Log.d(TAG, "User found: " + name + " (" + role + ") sid=" + sid);

                // Persist session so next app open skips login
                session.saveSession(name, role, dept, email, sid);

                // Upload FCM token so this device can receive push notifications
                uploadPendingFcmToken(sid);

                launchHome(name, role, dept, email, sid);

            } else {
                if (cursor != null) cursor.close();
                if (!isOnline()) {
                    // Offline and not found locally — could be new device or wrong password
                    Toast.makeText(this,
                            "Login failed. Check your credentials or connect to the internet to sync your account.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL ERROR during login", e);
            Toast.makeText(this, "System error. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Uploads the device's FCM token to Firestore for the given student ID.
     * Uses a locally cached "pending_token" if available; falls back to fetching
     * a fresh token from FirebaseMessaging.
     */
    private void uploadPendingFcmToken(String sid) {
        if (sid == null || sid.isEmpty()) return;
        FirestoreHelper fsh = new FirestoreHelper();
        SharedPreferences prefs = getSharedPreferences("ceoh_fcm", MODE_PRIVATE);
        String cached = prefs.getString("pending_token", null);
        if (cached != null && !cached.isEmpty()) {
            fsh.saveFcmToken(sid, cached);
        } else {
            // Fetch fresh token
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(token -> {
                        if (token != null && !token.isEmpty()) {
                            fsh.saveFcmToken(sid, token);
                            prefs.edit().putString("pending_token", token).apply();
                        }
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "getToken failed", e));
        }
    }

    /** Routes to AdminActivity or MainActivity based on role. */
    private void launchHome(String name, String role, String dept, String email, String sid) {
        Intent intent;
        if ("Admin".equalsIgnoreCase(role)) {
            intent = new Intent(this, AdminActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("USER_NAME",       name);
            intent.putExtra("USER_ROLE",       role);
            intent.putExtra("USER_DEPT",       dept);
            intent.putExtra("USER_EMAIL",      email);
            intent.putExtra("USER_STUDENT_ID", sid);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Returns true if the device has an active internet connection. */
    private boolean isOnline() {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return false;
        }
    }
}
