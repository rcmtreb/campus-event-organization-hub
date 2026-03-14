package com.example.campus_event_org_hub.ui.auth;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.SyncManager;
import com.example.campus_event_org_hub.ui.admin.AdminActivity;
import com.example.campus_event_org_hub.ui.main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "CEOH_LOGIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        DatabaseHelper db = new DatabaseHelper(this);

        EditText etEmail    = findViewById(R.id.et_login_email);
        EditText etPassword = findViewById(R.id.et_login_password);
        Button   btnLogin   = findViewById(R.id.btn_login);
        TextView tvGoToRegister = findViewById(R.id.tv_go_to_register);

        btnLogin.setOnClickListener(v -> {
            String loginInput = etEmail.getText().toString().trim();
            String password   = etPassword.getText().toString().trim();

            Log.d(TAG, "Login attempt with: " + loginInput);

            if (loginInput.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Check Admin — no sync needed, credentials are hardcoded
            if (loginInput.equalsIgnoreCase("admin@ucc.edu.ph") && password.equals("admin123")) {
                Log.d(TAG, "Admin credentials detected.");
                Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }

            // 2. Sync Firestore → SQLite first so new-device users can log in,
            //    then check credentials against the local (now up-to-date) DB.
            btnLogin.setEnabled(false);
            Toast.makeText(this, "Syncing data...", Toast.LENGTH_SHORT).show();

            SyncManager.sync(this, () -> {
                // Back on main thread after sync
                btnLogin.setEnabled(true);
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

                        Log.d(TAG, "User found: " + name + " (" + role + ") sid=" + sid);
                        cursor.close();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("USER_NAME",       name);
                        intent.putExtra("USER_ROLE",       role);
                        intent.putExtra("USER_DEPT",       dept);
                        intent.putExtra("USER_EMAIL",      email);
                        intent.putExtra("USER_STUDENT_ID", sid);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        Log.d(TAG, "Starting MainActivity...");
                        startActivity(intent);
                        finish();
                    } else {
                        Log.d(TAG, "No user found in DB for " + loginInput);
                        if (cursor != null) cursor.close();
                        Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "CRITICAL ERROR during login", e);
                    Toast.makeText(this, "System Error: Check logs", Toast.LENGTH_LONG).show();
                }
            });
        });

        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }
}
