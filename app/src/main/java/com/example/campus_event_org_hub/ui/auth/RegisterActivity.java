package com.example.campus_event_org_hub.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.util.PasswordStrengthUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "CEOH_REGISTER";
    private static final int MAX_NAME_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int STUDENT_ID_LENGTH = 8;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        DatabaseHelper db = DatabaseHelper.getInstance(this);

        EditText etFirstName = findViewById(R.id.et_reg_first_name);
        EditText etLastName  = findViewById(R.id.et_reg_last_name);
        EditText etStudentId = findViewById(R.id.et_reg_id);
        EditText etEmail = findViewById(R.id.et_reg_email);
        EditText etPassword = findViewById(R.id.et_reg_password);
        EditText etConfirmPassword = findViewById(R.id.et_reg_confirm_password);
        RadioGroup rgRole = findViewById(R.id.rg_reg_role);
        Spinner spinnerDept = findViewById(R.id.spinner_reg_department);
        Button btnRegister = findViewById(R.id.btn_register_submit);
        TextView tvGoToLogin = findViewById(R.id.tv_go_to_login);
        LinearLayout layoutPasswordStrength = findViewById(R.id.layout_password_strength);
        ProgressBar progressPasswordStrength = findViewById(R.id.progress_password_strength);
        TextView tvPasswordStrength = findViewById(R.id.tv_password_strength);

        // ── Spinner with "Select Department" placeholder ──────────────────────
        String[] depts = getResources().getStringArray(R.array.departments_array);
        List<String> deptList = new ArrayList<>();
        deptList.add("Select Department");
        for (String d : depts) deptList.add(d);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, deptList) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0; // disable the placeholder so it can't be re-selected
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    tv.setTextColor(ContextCompat.getColor(RegisterActivity.this, R.color.text_hint));
                } else {
                    tv.setTextColor(ContextCompat.getColor(RegisterActivity.this, R.color.text_primary));
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDept.setAdapter(adapter);
        spinnerDept.setSelection(0); // show placeholder by default

        setupStudentIdField(etStudentId);

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                if (password.isEmpty()) {
                    layoutPasswordStrength.setVisibility(View.GONE);
                } else {
                    layoutPasswordStrength.setVisibility(View.VISIBLE);
                    PasswordStrengthUtil.Strength strength = PasswordStrengthUtil.calculateStrength(password);
                    int progress = (int) (PasswordStrengthUtil.getProgress(strength) * 100);
                    progressPasswordStrength.setProgress(progress);
                    tvPasswordStrength.setText(strength.label);

                    int colorRes;
                    switch (strength) {
                        case WEAK:
                            colorRes = R.color.password_weak;
                            break;
                        case FAIR:
                            colorRes = R.color.password_fair;
                            break;
                        case GOOD:
                            colorRes = R.color.password_good;
                            break;
                        case STRONG:
                            colorRes = R.color.password_strong;
                            break;
                        default:
                            colorRes = R.color.text_hint;
                    }
                    int color = ContextCompat.getColor(RegisterActivity.this, colorRes);
                    progressPasswordStrength.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
                    tvPasswordStrength.setTextColor(color);
                }
            }
        });

        btnRegister.setOnClickListener(v -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName  = etLastName.getText().toString().trim();

            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(this, "Please enter both first name and last name", Toast.LENGTH_SHORT).show();
                return;
            }

            String name = firstName + " " + lastName;
            String studentId = etStudentId.getText().toString().trim().toUpperCase();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            int selectedId = rgRole.getCheckedRadioButtonId();
            RadioButton rb = findViewById(selectedId);
            String role = (rb != null) ? rb.getText().toString() : "Student";

            // ── Validate department selection ─────────────────────────────────
            if (spinnerDept.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select a department", Toast.LENGTH_SHORT).show();
                return;
            }
            String department = spinnerDept.getSelectedItem().toString();

            if (studentId.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidName(firstName) || !isValidName(lastName)) {
                Toast.makeText(this, "First and last name must be between 2 and " + MAX_NAME_LENGTH + " characters each", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidStudentId(studentId)) {
                Toast.makeText(this, "Student ID must be 8 digits (e.g., 20230166)", Toast.LENGTH_LONG).show();
                return;
            }

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPassword(password)) {
                Toast.makeText(this, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters with letters, capitals, and numbers", Toast.LENGTH_LONG).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRegister.setEnabled(false);
            btnRegister.setText("Registering...");

            // ── Step 1: Create Firebase Auth account (for email verification only) ──
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> proceedAfterFirebaseAuth(authResult.getUser(), db, name, studentId, email, password, role, department, btnRegister))
                    .addOnFailureListener(e -> {
                        String msg = e.getMessage();
                        if (msg != null && msg.contains("email address is already in use")) {
                            // Firebase Auth has this email — check if SQLite also has it
                            if (db.isEmailInLocalDb(email)) {
                                // Legitimate duplicate: user already fully registered
                                btnRegister.setEnabled(true);
                                btnRegister.setText("Sign Up");
                                Toast.makeText(this, "This email is already registered. Try logging in.", Toast.LENGTH_LONG).show();
                            } else {
                                // Orphan Firebase Auth account (SQLite was wiped / mis-matched state)
                                // → sign in to get a handle on it, delete it, then re-register cleanly
                                Log.w(TAG, "Orphan Firebase Auth account detected for " + email + " — attempting self-heal");
                                healOrphanAndRegister(db, name, studentId, email, password, role, department, btnRegister);
                            }
                        } else {
                            btnRegister.setEnabled(true);
                            btnRegister.setText("Sign Up");
                            Toast.makeText(this, "Registration failed: " + msg, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Firebase Auth createUser failed", e);
                        }
                    });
        });

        tvGoToLogin.setOnClickListener(v -> finish());
    }

    /**
     * Called after Firebase Auth account is ready (newly created OR after orphan was healed).
     * Sends verification email and saves the profile to SQLite + Firestore.
     */
    private void proceedAfterFirebaseAuth(FirebaseUser firebaseUser, DatabaseHelper db,
                                          String name, String studentId, String email,
                                          String password, String role, String department,
                                          Button btnRegister) {
        // Extract Firebase Auth UID — this becomes the Firestore document ID for users/
        String firebaseUid = (firebaseUser != null) ? firebaseUser.getUid() : "";

        if (firebaseUser != null) {
            firebaseUser.sendEmailVerification()
                    .addOnSuccessListener(unused -> Log.d(TAG, "Verification email sent to " + email))
                    .addOnFailureListener(e -> Log.w(TAG, "Failed to send verification email", e));
        }

        // BCrypt hashing is slow (~1s) — must NOT run on the main thread
        final String uid = firebaseUid;
        new Thread(() -> {
            long id = db.registerUser(name, studentId, email, password, role, department, uid);
            runOnUiThread(() -> {
                if (id != -1) {
                    Toast.makeText(this, "Registration successful! Check your email to verify.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RegisterActivity.this, EmailVerificationPendingActivity.class);
                    intent.putExtra("STUDENT_ID", studentId);
                    intent.putExtra("EMAIL", email);
                    startActivity(intent);
                    finish();
                } else {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Sign Up");
                    Toast.makeText(this, "Failed: Email or Student ID already exists.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    /**
     * Handles the "orphan Firebase Auth account" case: SQLite is empty but Firebase Auth
     * already has this email (e.g. admin deleted the account but Firebase Auth record remains).
     *
     * Strategy:
     * 1. Try signing in with the supplied password → delete orphan → re-create.
     * 2. If sign-in fails (different password), send a password-reset email so the
     *    user can reclaim the Auth slot later, and proceed with local registration
     *    (SQLite + Firestore) so the app is usable immediately via local login.
     */
    private void healOrphanAndRegister(DatabaseHelper db, String name, String studentId,
                                       String email, String password, String role,
                                       String department, Button btnRegister) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser orphan = authResult.getUser();
                    if (orphan == null) {
                        // Shouldn't happen, but fall through to local-only registration
                        proceedLocalOnly(db, name, studentId, email, password, role, department, btnRegister);
                        return;
                    }
                    Log.d(TAG, "Signed in to orphan account, deleting it...");
                    orphan.delete()
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "Orphan Firebase Auth account deleted. Re-registering...");
                                // Now re-create the account cleanly
                                mAuth.createUserWithEmailAndPassword(email, password)
                                        .addOnSuccessListener(newAuth -> proceedAfterFirebaseAuth(
                                                newAuth.getUser(), db, name, studentId,
                                                email, password, role, department, btnRegister))
                                        .addOnFailureListener(e -> {
                                            // Auth re-create failed but we can still register locally
                                            Log.w(TAG, "Re-create after orphan delete failed", e);
                                            proceedLocalOnly(db, name, studentId, email, password, role, department, btnRegister);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Could not delete orphan account", e);
                                proceedLocalOnly(db, name, studentId, email, password, role, department, btnRegister);
                            });
                })
                .addOnFailureListener(e -> {
                    // Sign-in failed → orphan was created with a different password.
                    // Send a password-reset email so the user can reclaim the Auth slot later,
                    // then proceed with local (SQLite + Firestore) registration.
                    Log.w(TAG, "Could not sign in to orphan account — registering locally", e);
                    mAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused ->
                                    Log.d(TAG, "Password-reset email sent to " + email))
                            .addOnFailureListener(err ->
                                    Log.w(TAG, "Failed to send password-reset email", err));
                    proceedLocalOnly(db, name, studentId, email, password, role, department, btnRegister);
                });
    }

    /**
     * Registers the user in SQLite + Firestore only (no Firebase Auth account created).
     * Used when we can't delete the orphan Firebase Auth account because the old password
     * is unknown. The user can still log in via local credentials.
     */
    private void proceedLocalOnly(DatabaseHelper db, String name, String studentId,
                                  String email, String password, String role,
                                  String department, Button btnRegister) {
        new Thread(() -> {
            // No Firebase Auth user available — UID will be filled on next login
            long id = db.registerUser(name, studentId, email, password, role, department, "");
            runOnUiThread(() -> {
                if (id != -1) {
                    Toast.makeText(this,
                            "Registration successful! A password-reset email was sent — "
                            + "please check your inbox to fully activate your account.",
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RegisterActivity.this, EmailVerificationPendingActivity.class);
                    intent.putExtra("STUDENT_ID", studentId);
                    intent.putExtra("EMAIL", email);
                    startActivity(intent);
                    finish();
                } else {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Sign Up");
                    Toast.makeText(this, "Failed: Email or Student ID already exists.", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void setupStudentIdField(EditText etStudentId) {
        // Filter 1: digits only
        InputFilter digitFilter = (source, start, end, dest, dstart, dend) -> {
            StringBuilder result = new StringBuilder();
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (Character.isDigit(c)) {
                    result.append(c);
                }
            }
            // If nothing was filtered out, return null (accept as-is).
            if (result.length() == end - start) return null;
            return result.toString();
        };
        // Filter 2: max 8 characters (applied after digitFilter)
        InputFilter lengthFilter = new InputFilter.LengthFilter(STUDENT_ID_LENGTH);
        etStudentId.setFilters(new InputFilter[]{digitFilter, lengthFilter});
    }

    private boolean isValidName(String name) {
        int len = name.length();
        return len >= 2 && len <= MAX_NAME_LENGTH;
    }

    private boolean isValidStudentId(String studentId) {
        if (studentId == null || studentId.length() != STUDENT_ID_LENGTH) {
            return false;
        }
        return studentId.matches("^\\d{8}$");
    }

    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasCapital = false;
        boolean hasNumber = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
                if (Character.isUpperCase(c)) {
                    hasCapital = true;
                }
            } else if (Character.isDigit(c)) {
                hasNumber = true;
            }
        }
        return hasLetter && hasCapital && hasNumber;
    }
}
