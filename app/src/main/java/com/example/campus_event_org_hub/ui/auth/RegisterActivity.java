package com.example.campus_event_org_hub.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        DatabaseHelper db = new DatabaseHelper(this);

        EditText etName = findViewById(R.id.et_reg_name);
        EditText etStudentId = findViewById(R.id.et_reg_id);
        EditText etEmail = findViewById(R.id.et_reg_email);
        EditText etPassword = findViewById(R.id.et_reg_password);
        EditText etConfirmPassword = findViewById(R.id.et_reg_confirm_password);
        RadioGroup rgRole = findViewById(R.id.rg_reg_role);
        Spinner spinnerDept = findViewById(R.id.spinner_reg_department);
        Button btnRegister = findViewById(R.id.btn_register_submit);
        TextView tvGoToLogin = findViewById(R.id.tv_go_to_login);

        // Fix for Spinner Visibility: Use custom spinner_item with explicit black text
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.departments_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDept.setAdapter(adapter);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String studentId = etStudentId.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            String department = spinnerDept.getSelectedItem().toString();
            
            int selectedId = rgRole.getCheckedRadioButtonId();
            RadioButton rb = findViewById(selectedId);
            String role = (rb != null) ? rb.getText().toString() : "Student";

            if (name.isEmpty() || studentId.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            long id = db.registerUser(name, studentId, email, password, role, department);
            if (id != -1) {
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Failed: Email or ID already exists", Toast.LENGTH_LONG).show();
            }
        });

        tvGoToLogin.setOnClickListener(v -> finish());
    }
}
