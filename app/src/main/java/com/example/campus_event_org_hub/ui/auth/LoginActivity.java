package com.example.campus_event_org_hub.ui.auth;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
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
            try {
                uploadPendingFcmToken(session.getFirebaseUid());
                launchHome(session.getName(), session.getRole(), session.getDept(),
                        session.getEmail(), session.getStudentId());
            } catch (Throwable t) {
                Log.e(TAG, "Auto-login crashed", t);
                session.clearSession();
                setContentView(R.layout.activity_login);
                Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        setContentView(R.layout.activity_login);

        DatabaseHelper db = DatabaseHelper.getInstance(this);

        EditText etEmail        = findViewById(R.id.et_login_email);
        EditText etPassword     = findViewById(R.id.et_login_password);
        Button   btnLogin       = findViewById(R.id.btn_login);
        TextView tvGoToRegister = findViewById(R.id.tv_go_to_register);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        findViewById(R.id.iv_social_facebook).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://web.facebook.com/univofcaloocanofficial"))));
        findViewById(R.id.iv_social_instagram).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.instagram.com/"))));
        findViewById(R.id.iv_social_tiktok).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.tiktok.com/@univofcalooocan"))));

        btnLogin.setOnClickListener(v -> {
            try {
                String loginInput = etEmail.getText().toString().trim();
                String password   = etPassword.getText().toString().trim();

                Log.d(TAG, "Login attempt with: " + loginInput);

                if (loginInput.isEmpty() || password.isEmpty()) {
                    showLoginToast("Please fill in your Student ID/Email and password.", true);
                    return;
                }

                if (loginInput.contains("@") && !Patterns.EMAIL_ADDRESS.matcher(loginInput).matches()) {
                    showLoginToast("Please enter a valid email address.", true);
                    return;
                }

                if (db.isLoginLocked(loginInput) && !isOnline()) {
                    long remainingMs = db.getLoginLockoutRemainingMs(loginInput);
                    int minutes = (int) (remainingMs / 60000) + 1;
                    showLoginToast("Account locked due to too many attempts. Try again in " + minutes + " minute(s).", true);
                    return;
                }

                btnLogin.setEnabled(false);

                if (isOnline()) {
                    String authEmail = loginInput.contains("@")
                            ? loginInput
                            : db.getEmailForLoginInput(loginInput);
                    if (authEmail == null || !authEmail.contains("@")) {
                        Log.w(TAG, "No email mapping for input=" + loginInput + "; using local login path");
                        proceedWithNormalLogin(db, session, loginInput, password, btnLogin);
                        return;
                    }
                    final String finalAuthEmail = authEmail;
                    showLoginToast("Signing in...", false);
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(finalAuthEmail, password)
                            .addOnSuccessListener(authResult -> {
                                try {
                                    FirebaseUser firebaseUser = authResult.getUser();
                                    if (firebaseUser != null) {
                                        firebaseUser.getIdToken(false)
                                                .addOnSuccessListener(tokenResult -> {
                                                    try {
                                                        Object adminClaim = tokenResult.getClaims().get("admin");
                                                        if (Boolean.TRUE.equals(adminClaim)) {
                                                            Log.d(TAG, "Admin custom claim detected — launching AdminActivity");
                                                            String adminUid = firebaseUser.getUid();
                                                            session.saveSession("Admin", "Admin", "Administration", finalAuthEmail, "admin", adminUid);
                                                            uploadPendingFcmToken(adminUid);
                                                            Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(intent);
                                                            finish();
                                                        } else {
                                                            Log.d(TAG, "Firebase Auth succeeded — fetching user from Firestore");
                                                            proceedWithFirestoreLogin(db, session, loginInput, password,
                                                                    firebaseUser.getUid(), finalAuthEmail, firebaseUser, btnLogin);
                                                        }
                                                    } catch (Throwable t) {
                                                        Log.e(TAG, "getIdToken callback crashed", t);
                                                        btnLogin.setEnabled(true);
                                                        attemptLocalLogin(db, session, loginInput, password);
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.w(TAG, "getIdToken failed — proceeding with Firestore login", e);
                                                    proceedWithFirestoreLogin(db, session, loginInput, password,
                                                            firebaseUser.getUid(), finalAuthEmail, firebaseUser, btnLogin);
                                                });
                                    } else {
                                        proceedWithFirestoreLogin(db, session, loginInput, password,
                                                null, finalAuthEmail, null, btnLogin);
                                    }
                                } catch (Throwable t) {
                                    Log.e(TAG, "Firebase Auth success callback crashed", t);
                                    btnLogin.setEnabled(true);
                                    attemptLocalLogin(db, session, loginInput, password);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Firebase Auth FAILED: " + e.getMessage() + " — falling back to local DB", e);
                                proceedWithNormalLogin(db, session, loginInput, password, btnLogin);
                            });
                } else {
                    proceedWithNormalLogin(db, session, loginInput, password, btnLogin);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Login button crashed", t);
                btnLogin.setEnabled(true);
                Toast.makeText(this, "Login error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void proceedWithFirestoreLogin(DatabaseHelper db, SessionManager session,
                                            String loginInput, String password, String firebaseUid,
                                            String firebaseEmail, FirebaseUser firebaseUser, Button btnLogin) {
        final String inputUid = firebaseUid;
        final String inputEmail = firebaseEmail;
        Log.d(TAG, "proceedWithFirestoreLogin: uid=" + inputUid + ", input=" + loginInput);
        new Thread(() -> {
            try {
                FirestoreHelper fsh = new FirestoreHelper();
                DocumentSnapshot doc = (inputUid != null && !inputUid.isEmpty())
                        ? fsh.getUserByUid(inputUid) : null;

                if (doc != null && doc.exists()) {
                    String sid          = str(doc, "student_id");
                    String name         = str(doc, "name");
                    String email        = str(doc, "email");
                    String role         = str(doc, "role");
                    String dept         = str(doc, "department");
                    String gender       = str(doc, "gender");
                    String mobile       = str(doc, "mobile");
                    String profileImg   = str(doc, "profile_image");
                    String notifPref    = str(doc, "notif_pref");
                    String hashedPwd    = str(doc, "password");
                    boolean emailVerified = intVal(doc, "email_verified") == 1;
                    String resolvedUid = inputUid;

                    Log.d(TAG, "Firestore user found: " + name + " (" + role + "), sid=" + sid
                            + ", docVerified=" + emailVerified);

                    if (sid == null || sid.isEmpty()) {
                        sid = loginInput.contains("@") ? db.getStudentIdForEmail(loginInput) : loginInput;
                    }
                    if (name == null || name.isEmpty()) name = "User";
                    if (role == null || role.isEmpty())  role = "Student";
                    if (dept == null || dept.isEmpty())  dept = "General";

                    final String finalSid = sid;
                    final String finalUid = resolvedUid;
                    final String finalName = name;
                    final String finalRole = role;
                    final String finalDept = dept;
                    final String finalEmail = email;
                    final boolean finalEv = emailVerified;

                    if (!emailVerified) {
                        runOnUiThread(() -> {
                            Intent intent = new Intent(LoginActivity.this, EmailVerificationPendingActivity.class);
                            intent.putExtra("STUDENT_ID", finalSid);
                            intent.putExtra("EMAIL", finalEmail);
                            startActivity(intent);
                        });
                        return;
                    }

                    clearLoginLockouts(db, loginInput, finalSid, finalEmail);
                    db.syncUpsertUser(finalSid, finalName, finalEmail, finalRole, finalDept,
                            gender, mobile, profileImg, notifPref, hashedPwd, finalEv, finalUid);
                    db.setFirebaseUid(finalSid, finalUid != null ? finalUid : "");
                    if (finalEv) {
                        db.setEmailVerified(finalSid, true);
                    }
                    if ("Student".equals(finalRole)) {
                        db.updateLastLogin(finalSid);
                    }

                    runOnUiThread(() -> {
                        session.saveSession(finalName, finalRole, finalDept, finalEmail, finalSid, finalUid);
                        uploadPendingFcmToken(finalUid);
                        launchHome(finalName, finalRole, finalDept, finalEmail, finalSid);
                    });
                } else {
                    Log.w(TAG, "Firestore user NOT found for uid=" + inputUid + " — creating minimal local user");
                    createMinimalLocalUserAndLogin(db, session, loginInput, password,
                            inputUid, firebaseEmail, firebaseUser, btnLogin);
                }
            } catch (Throwable t) {
                Log.e(TAG, "proceedWithFirestoreLogin crashed — using Firebase Auth session directly", t);
                createMinimalLocalUserAndLogin(db, session, loginInput, password,
                        firebaseUser != null ? firebaseUser.getUid() : null,
                        firebaseEmail != null ? firebaseEmail
                                : (firebaseUser != null ? firebaseUser.getEmail() : null),
                        firebaseUser, btnLogin);
            }
        }).start();
    }

    private void createMinimalLocalUserAndLogin(DatabaseHelper db, SessionManager session,
                                                 String loginInput, String password, String firebaseUid,
                                                 String firebaseEmail, FirebaseUser firebaseUser, Button btnLogin) {
        String email = firebaseEmail;
        if (email == null || email.isEmpty()) {
            email = (firebaseUser != null) ? firebaseUser.getEmail() : null;
        }
        if (email == null || email.isEmpty()) {
            email = loginInput.contains("@") ? loginInput : null;
        }
        if (email == null || !email.contains("@")) {
            runOnUiThread(() -> {
                btnLogin.setEnabled(true);
                showLoginToast("Account not found. Please register first.", true);
            });
            return;
        }
        final String finalEmail = email;
        String displayName = "User";
        if (firebaseUser != null && firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()) {
            displayName = firebaseUser.getDisplayName();
        }
        String sid = loginInput.contains("@") ? db.getStudentIdForEmail(loginInput) : loginInput;
        if (sid == null || sid.isEmpty()) sid = email.split("@")[0];
        final String finalSid = sid;
        final String finalName = displayName;
        final String finalUid = firebaseUid != null ? firebaseUid : "";
        final String finalFirebaseUid = firebaseUid;
        try {
            clearLoginLockouts(db, loginInput, finalSid, finalEmail);
            String hashedPwd = "";
            try {
                hashedPwd = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
            } catch (Exception ignored) {}

            SQLiteDatabase pwDb = db.getWritableDatabase();
            ContentValues pwV = new ContentValues();
            pwV.put(DatabaseHelper.COLUMN_USER_PASSWORD, hashedPwd);
            pwV.put(DatabaseHelper.COLUMN_USER_EMAIL_VERIFIED, 1);
            int rows = pwDb.update(DatabaseHelper.TABLE_USERS, pwV,
                    DatabaseHelper.COLUMN_USER_STUDENT_ID + "=?",
                    new String[]{finalSid});
            pwDb.close();
            if (rows == 0) {
                db.syncUpsertUser(finalSid, finalName, finalEmail, "Student", "General",
                        "", "", "", "", hashedPwd, true, finalFirebaseUid);
            }
            db.setFirebaseUid(finalSid, finalFirebaseUid != null ? finalFirebaseUid : "");
            db.updateLastLogin(finalSid);
            runOnUiThread(() -> {
                session.saveSession(finalName, "Student", "General", finalEmail, finalSid, finalFirebaseUid);
                uploadPendingFcmToken(finalFirebaseUid);
                launchHome(finalName, "Student", "General", finalEmail, finalSid);
            });
        } catch (Exception e) {
            Log.e(TAG, "createMinimalLocalUserAndLogin failed", e);
            runOnUiThread(() -> {
                btnLogin.setEnabled(true);
                showLoginToast("Login failed. Please try again.", true);
            });
        }
    }

    private void proceedWithNormalLogin(DatabaseHelper db, SessionManager session,
                                         String loginInput, String password, Button btnLogin) {
        if (isOnline()) {
            showLoginToast("Syncing your account...", false);
            SyncManager.sync(this, () -> {
                try {
                    btnLogin.setEnabled(true);
                    attemptLocalLogin(db, session, loginInput, password);
                } catch (Throwable t) {
                    Log.e(TAG, "SyncManager callback crashed", t);
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Login error. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            btnLogin.setEnabled(true);
            showLoginToast("No internet connection — logging in with saved data.", false);
            attemptLocalLogin(db, session, loginInput, password);
        }
    }

    private void healEmptyPassword(DatabaseHelper db, SessionManager session,
                                   String loginInput, String password) {
        String email = db.getEmailForLoginInput(loginInput);
        if (email == null || email.isEmpty()) email = loginInput;

        final String firebaseEmail = email;
        FirebaseAuth.getInstance().signInWithEmailAndPassword(firebaseEmail, password)
                .addOnSuccessListener(authResult -> {
                    try {
                        Log.d(TAG, "Firebase Auth confirmed correct password — restoring hash");
                        FirebaseUser healedUser = authResult.getUser();
                        if (healedUser != null) {
                            db.setFirebaseUid(loginInput, healedUser.getUid());
                        }
                        new Thread(() -> {
                            db.restorePassword(loginInput, password);
                            runOnUiThread(() -> {
                                attemptLocalLogin(db, session, loginInput, password);
                            });
                        }).start();
                    } catch (Throwable t) {
                        Log.e(TAG, "healEmptyPassword success callback crashed", t);
                        db.incrementLoginAttempts(loginInput);
                        Toast.makeText(this, "Login error. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Heal failed — Firebase Auth rejected credentials", e);
                    db.incrementLoginAttempts(loginInput);
                    int attemptsLeft = 5 - getLoginAttempts(db, loginInput);
                    Toast.makeText(this,
                            "Invalid credentials. " + attemptsLeft + " attempts remaining.",
                            Toast.LENGTH_SHORT).show();
                });
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
                    final String fName = name, fRole = role, fDept = dept, fEmail = email, fSid = sid;
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(fEmail, password)
                            .addOnSuccessListener(authResult -> {
                                try {
                                    FirebaseUser firebaseUser = authResult.getUser();
                                    if (firebaseUser != null) {
                                        String uid = firebaseUser.getUid();
                                        db.setFirebaseUid(fSid, uid);
                                        firebaseUser.reload().addOnCompleteListener(reloadTask -> {
                                            try {
                                                FirebaseUser refreshed = FirebaseAuth.getInstance().getCurrentUser();
                                                if (refreshed != null && refreshed.isEmailVerified()) {
                                                    db.setEmailVerified(fSid, true);
                                                    session.saveSession(fName, fRole, fDept, fEmail, fSid, uid);
                                                    uploadPendingFcmToken(uid);
                                                    launchHome(fName, fRole, fDept, fEmail, fSid);
                                                } else {
                                                    Intent intent = new Intent(LoginActivity.this, EmailVerificationPendingActivity.class);
                                                    intent.putExtra("STUDENT_ID", fSid);
                                                    intent.putExtra("EMAIL", fEmail);
                                                    startActivity(intent);
                                                }
                                            } catch (Throwable t) {
                                                Log.e(TAG, "reload callback crashed", t);
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
                                } catch (Throwable t) {
                                    Log.e(TAG, "Firebase Auth verify callback crashed", t);
                                    Intent intent = new Intent(LoginActivity.this, EmailVerificationPendingActivity.class);
                                    intent.putExtra("STUDENT_ID", fSid);
                                    intent.putExtra("EMAIL", fEmail);
                                    startActivity(intent);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Firebase Auth sign-in for verify check failed", e);
                                Intent intent = new Intent(LoginActivity.this, EmailVerificationPendingActivity.class);
                                intent.putExtra("STUDENT_ID", fSid);
                                intent.putExtra("EMAIL", fEmail);
                                startActivity(intent);
                            });
                    return;
                }

                String firebaseUid = db.getFirebaseUid(sid);

                session.saveSession(name, role, dept, email, sid, firebaseUid);
                uploadPendingFcmToken(firebaseUid);

                if ("Student".equals(role)) {
                    final String finalSid = sid;
                    new Thread(() -> db.updateLastLogin(finalSid)).start();
                }

                if (legacyUpgrade) {
                    showLoginToast("Welcome back! Your password has been upgraded for security.", false);
                }

                final String fName2 = name, fRole2 = role, fDept2 = dept, fEmail2 = email, fSid2 = sid;
                if (FirebaseAuth.getInstance().getCurrentUser() == null
                        && fEmail2 != null && !fEmail2.isEmpty() && isOnline()) {
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(fEmail2, password)
                            .addOnCompleteListener(task -> {
                                try {
                                    FirebaseUser silentUser = FirebaseAuth.getInstance().getCurrentUser();
                                    if (silentUser != null) {
                                        String uid = silentUser.getUid();
                                        db.setFirebaseUid(fSid2, uid);
                                        session.saveSession(fName2, fRole2, fDept2, fEmail2, fSid2, uid);
                                        uploadPendingFcmToken(uid);
                                    }
                                    launchHome(fName2, fRole2, fDept2, fEmail2, fSid2);
                                } catch (Throwable t) {
                                    Log.e(TAG, "Silent Firebase sign-in callback crashed", t);
                                    launchHome(fName2, fRole2, fDept2, fEmail2, fSid2);
                                }
                            });
                } else {
                    launchHome(name, role, dept, email, sid);
                }

            } else {
                if (cursor != null) cursor.close();

                if (db.userExistsWithEmptyPassword(loginInput)) {
                    Log.w(TAG, "User exists with empty password — attempting Firebase Auth self-heal");
                    healEmptyPassword(db, session, loginInput, password);
                    return;
                }

                db.incrementLoginAttempts(loginInput);

                if (db.isLoginLocked(loginInput)) {
                    long remainingMs = db.getLoginLockoutRemainingMs(loginInput);
                    int minutes = (int) (remainingMs / 60000) + 1;
                    showLoginToast("Account locked after too many attempts. Try again in " + minutes + " minute(s).", true);
                    return;
                }

                int attemptsLeft = 5 - getLoginAttempts(db, loginInput);
                if (!isOnline()) {
                    showLoginToast("Login failed. Your account may not be synced. Connect to the internet and try again.", true);
                } else if (!userExistsForLoginInput(db, loginInput)) {
                    showLoginToast("Account not found on this device. Try again once synced, or register.", true);
                } else {
                    showLoginToast("Wrong password. Please check and try again. " + attemptsLeft + " attempt(s) left.", true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL ERROR during login", e);
            Toast.makeText(this, "System error. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean userExistsForLoginInput(DatabaseHelper db, String loginInput) {
        try {
            SQLiteDatabase sdb = db.getReadableDatabase();
            Cursor c = sdb.rawQuery(
                    "SELECT 1 FROM " + DatabaseHelper.TABLE_USERS +
                    " WHERE " + DatabaseHelper.COLUMN_USER_EMAIL + "=? OR " + DatabaseHelper.COLUMN_USER_STUDENT_ID + "=? LIMIT 1",
                    new String[]{loginInput, loginInput});
            boolean exists = (c != null && c.moveToFirst());
            if (c != null) c.close();
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "userExistsForLoginInput failed", e);
            return false;
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

    private void uploadPendingFcmToken(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isEmpty()) return;
        try {
            FirestoreHelper fsh = new FirestoreHelper();
            SharedPreferences prefs = getSharedPreferences("ceoh_fcm", MODE_PRIVATE);
            String cached = prefs.getString("pending_token", null);
            if (cached != null && !cached.isEmpty()) {
                fsh.saveFcmToken(firebaseUid, cached);
            } else {
                FirebaseMessaging.getInstance().getToken()
                        .addOnSuccessListener(token -> {
                            if (token != null && !token.isEmpty()) {
                                fsh.saveFcmToken(firebaseUid, token);
                                prefs.edit().putString("pending_token", token).apply();
                            }
                        })
                        .addOnFailureListener(e -> Log.w(TAG, "getToken failed", e));
            }
        } catch (Throwable t) {
            Log.w(TAG, "uploadPendingFcmToken failed (non-critical)", t);
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

    private void showLoginToast(String message, boolean isError) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        View logo = findViewById(R.id.iv_logo);
        if (logo != null) {
            int[] location = new int[2];
            logo.getLocationOnScreen(location);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, location[1] - (int)(16 * getResources().getDisplayMetrics().density));
        } else {
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, (int)(400 * getResources().getDisplayMetrics().density));
        }
        toast.show();
    }

    private void clearLoginLockouts(DatabaseHelper db, String loginInput, String studentId, String email) {
        try {
            if (loginInput != null && !loginInput.isEmpty()) {
                db.resetLoginAttempts(loginInput);
            }
            if (studentId != null && !studentId.isEmpty()) {
                db.resetLoginAttempts(studentId);
            }
            if (email != null && !email.isEmpty()) {
                db.resetLoginAttempts(email);
            }
        } catch (Exception e) {
            Log.w(TAG, "clearLoginLockouts failed", e);
        }
    }

    private String str(DocumentSnapshot d, String field) {
        Object v = d.get(field);
        return v != null ? v.toString() : "";
    }

    private int intVal(DocumentSnapshot d, String field) {
        try {
            Object v = d.get(field);
            if (v == null) return 0;
            if (v instanceof Long)   return ((Long) v).intValue();
            if (v instanceof Double) return ((Double) v).intValue();
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
