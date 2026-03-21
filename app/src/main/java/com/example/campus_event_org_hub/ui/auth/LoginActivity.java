package com.example.campus_event_org_hub.ui.auth;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "CEOH_LOGIN";

    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted ->
                    Log.d(TAG, "POST_NOTIFICATIONS granted: " + granted));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CEOHFirebaseMessagingService.ensureChannels(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        SessionManager session = new SessionManager(this);
        if (session.isLoggedIn()) {
            Log.d(TAG, "Valid session found, auto-logging in as: " + session.getRole());
            uploadPendingFcmToken(session.getStudentId());
            launchHome(session.getName(), session.getRole(), session.getDept(),
                    session.getEmail(), session.getStudentId());
            return;
        }

        setContentView(R.layout.activity_login);

        DatabaseHelper db = new DatabaseHelper(this);

        EditText etEmail        = findViewById(R.id.et_login_email);
        EditText etPassword     = findViewById(R.id.et_login_password);
        Button   btnLogin       = findViewById(R.id.btn_login);
        TextView tvGoToRegister = findViewById(R.id.tv_go_to_register);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            String loginInput = etEmail.getText().toString().trim();
            String password   = etPassword.getText().toString().trim();

            Log.d(TAG, "Login attempt with: " + loginInput);

            if (loginInput.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (loginInput.contains("@") && !Patterns.EMAIL_ADDRESS.matcher(loginInput).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            if (db.isLoginLocked(loginInput)) {
                long remainingMs = db.getLoginLockoutRemainingMs(loginInput);
                int minutes = (int) (remainingMs / 60000) + 1;
                Toast.makeText(this, "Too many failed attempts. Try again in " + minutes + " minute(s).", Toast.LENGTH_LONG).show();
                return;
            }

            if (loginInput.equalsIgnoreCase("admin@ucc.edu.ph") && password.equals("admin123")) {
                Log.d(TAG, "Admin credentials detected.");
                session.saveSession("Admin", "Admin", "Administration",
                        "admin@ucc.edu.ph", "admin");
                uploadPendingFcmToken("admin");
                Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }

            btnLogin.setEnabled(false);

            if (isOnline()) {
                Toast.makeText(this, "Syncing data...", Toast.LENGTH_SHORT).show();
                SyncManager.sync(this, () -> {
                    btnLogin.setEnabled(true);
                    attemptLocalLogin(db, session, loginInput, password);
                });
            } else {
                btnLogin.setEnabled(true);
                Toast.makeText(this, "No internet — using saved data", Toast.LENGTH_SHORT).show();
                attemptLocalLogin(db, session, loginInput, password);
            }
        });

        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void attemptLocalLogin(DatabaseHelper db, SessionManager session,
                                   String loginInput, String password) {
        boolean legacyUpgrade = db.wasPasswordUpgradedFromLegacy(loginInput, password);
        
        try {
            Cursor cursor = db.checkUser(loginInput, password);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx  = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_NAME);
                int roleIdx  = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ROLE);
                int deptIdx  = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_DEPARTMENT);
                int emailIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_EMAIL);
                int sidIdx   = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_STUDENT_ID);
                int verifiedIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_EMAIL_VERIFIED);

                String name  = nameIdx  >= 0 ? cursor.getString(nameIdx)  : "User";
                String role  = roleIdx  >= 0 ? cursor.getString(roleIdx)  : "Student";
                String dept  = deptIdx  >= 0 ? cursor.getString(deptIdx)  : "General";
                String email = emailIdx >= 0 ? cursor.getString(emailIdx) : "";
                String sid   = sidIdx   >= 0 ? cursor.getString(sidIdx)   : "";
                boolean emailVerified = (verifiedIdx >= 0) ? (cursor.getInt(verifiedIdx) == 1) : true;

                cursor.close();

                if (name  == null || name.trim().isEmpty())  name  = "User";
                if (role  == null || role.trim().isEmpty())  role  = "Student";
                if (dept  == null || dept.trim().isEmpty())  dept  = "General";
                if (email == null) email = "";
                if (sid   == null) sid   = "";

                db.resetLoginAttempts(loginInput);
                Log.d(TAG, "User found: " + name + " (" + role + ") sid=" + sid);

                if (!emailVerified) {
                    // Check Firebase Auth to see if user has verified since last login
                    final String fName = name, fRole = role, fDept = dept, fEmail = email, fSid = sid;
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(fEmail, password)
                            .addOnSuccessListener(authResult -> {
                                FirebaseUser firebaseUser = authResult.getUser();
                                if (firebaseUser != null) {
                                    firebaseUser.reload().addOnCompleteListener(reloadTask -> {
                                        FirebaseUser refreshed = FirebaseAuth.getInstance().getCurrentUser();
                                        if (refreshed != null && refreshed.isEmailVerified()) {
                                            // Verified! Update local DB and proceed
                                            db.setEmailVerified(fSid, true);
                                            FirebaseAuth.getInstance().signOut();
                                            session.saveSession(fName, fRole, fDept, fEmail, fSid);
                                            uploadPendingFcmToken(fSid);
                                            launchHome(fName, fRole, fDept, fEmail, fSid);
                                        } else {
                                            // Still not verified — keep Firebase Auth signed in
                                            // so EmailVerificationPendingActivity can resend/poll
                                            Intent intent = new Intent(LoginActivity.this, EmailVerificationPendingActivity.class);
                                            intent.putExtra("STUDENT_ID", fSid);
                                            intent.putExtra("EMAIL", fEmail);
                                            startActivity(intent);
                                        }
                                    });
                                } else {
                                    Intent intent = new Intent(LoginActivity.this, EmailVerificationPendingActivity.class);
                                    intent.putExtra("STUDENT_ID", fSid);
                                    intent.putExtra("EMAIL", fEmail);
                                    startActivity(intent);
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Firebase Auth sign-in failed (maybe no internet or account mismatch)
                                // Fall back to sending them to the pending screen
                                Log.w(TAG, "Firebase Auth sign-in for verify check failed", e);
                                Intent intent = new Intent(LoginActivity.this, EmailVerificationPendingActivity.class);
                                intent.putExtra("STUDENT_ID", fSid);
                                intent.putExtra("EMAIL", fEmail);
                                startActivity(intent);
                            });
                    return;
                }

                session.saveSession(name, role, dept, email, sid);
                uploadPendingFcmToken(sid);
                
                if (legacyUpgrade) {
                    Toast.makeText(this, "Welcome back! Your password has been upgraded for security.", Toast.LENGTH_LONG).show();
                }
                
                launchHome(name, role, dept, email, sid);

            } else {
                if (cursor != null) cursor.close();
                
                db.incrementLoginAttempts(loginInput);
                
                if (db.isLoginLocked(loginInput)) {
                    long remainingMs = db.getLoginLockoutRemainingMs(loginInput);
                    int minutes = (int) (remainingMs / 60000) + 1;
                    Toast.makeText(this, "Too many failed attempts. Account locked for " + minutes + " minute(s).", Toast.LENGTH_LONG).show();
                    return;
                }

                int attemptsLeft = 5 - getLoginAttempts(db, loginInput);
                if (!isOnline()) {
                    Toast.makeText(this,
                            "Login failed. Check your credentials or connect to the internet to sync your account.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Invalid credentials. " + attemptsLeft + " attempts remaining.", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL ERROR during login", e);
            Toast.makeText(this, "System error. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private int getLoginAttempts(DatabaseHelper db, String loginInput) {
        try {
            SQLiteDatabase sdb = db.getReadableDatabase();
            Cursor c = sdb.rawQuery("SELECT attempts FROM " + DatabaseHelper.TABLE_LOGIN_RATE_LIMIT +
                    " WHERE " + DatabaseHelper.COLUMN_LR_STUDENT_ID + "=?", new String[]{loginInput});
            if (c != null && c.moveToFirst()) {
                int attempts = c.getInt(0);
                c.close();
                return attempts;
            }
            if (c != null) c.close();
        } catch (Exception e) {
            Log.e(TAG, "getLoginAttempts failed", e);
        }
        return 0;
    }

    private void uploadPendingFcmToken(String sid) {
        if (sid == null || sid.isEmpty()) return;
        FirestoreHelper fsh = new FirestoreHelper();
        SharedPreferences prefs = getSharedPreferences("ceoh_fcm", MODE_PRIVATE);
        String cached = prefs.getString("pending_token", null);
        if (cached != null && !cached.isEmpty()) {
            fsh.saveFcmToken(sid, cached);
        } else {
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
