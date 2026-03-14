package com.example.campus_event_org_hub.ui.auth;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.ui.admin.AdminActivity;
import com.example.campus_event_org_hub.ui.main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "CEOH_LOGIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        DatabaseHelper db = new DatabaseHelper(this);

        EditText etEmail = findViewById(R.id.et_login_email);
        EditText etPassword = findViewById(R.id.et_login_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvGoToRegister = findViewById(R.id.tv_go_to_register);

        btnLogin.setOnClickListener(v -> {
            String loginInput = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            Log.d(TAG, "Login attempt with: " + loginInput);

            if (loginInput.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Check Admin
            if (loginInput.equalsIgnoreCase("admin@ucc.edu.ph") && password.equals("admin123")) {
                Log.d(TAG, "Admin credentials detected.");
                Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }

            // 2. Check Database
            try {
                Cursor cursor = db.checkUser(loginInput, password);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex  = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_NAME);
                    int roleIndex  = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_ROLE);
                    int deptIndex  = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_DEPARTMENT);
                    int emailIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_EMAIL);
                    int sidIndex   = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_STUDENT_ID);

                    String name  = nameIndex  >= 0 ? cursor.getString(nameIndex)  : "User";
                    String role  = roleIndex  >= 0 ? cursor.getString(roleIndex)  : "Student";
                    String dept  = deptIndex  >= 0 ? cursor.getString(deptIndex)  : "General";
                    String email = emailIndex >= 0 ? cursor.getString(emailIndex) : "";
                    String sid   = sidIndex   >= 0 ? cursor.getString(sidIndex)   : "";

                    if (name  == null || name.trim().isEmpty())  name  = "User";
                    if (role  == null || role.trim().isEmpty())  role  = "Student";
                    if (dept  == null || dept.trim().isEmpty())  dept  = "General";
                    if (email == null) email = "";
                    if (sid   == null) sid   = "";

                    Log.d(TAG, "User found: " + name + " (" + role + ") sid=" + sid);
                    cursor.close();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("USER_NAME", name);
                    intent.putExtra("USER_ROLE", role);
                    intent.putExtra("USER_DEPT", dept);
                    intent.putExtra("USER_EMAIL", email);
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

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}
