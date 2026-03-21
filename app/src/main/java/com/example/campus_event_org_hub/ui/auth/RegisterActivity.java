package com.example.campus_event_org_hub.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
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

public class RegisterActivity extends AppCompatActivity {

    private static final int MAX_NAME_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int STUDENT_ID_LENGTH = 8;

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
        LinearLayout layoutPasswordStrength = findViewById(R.id.layout_password_strength);
        ProgressBar progressPasswordStrength = findViewById(R.id.progress_password_strength);
        TextView tvPasswordStrength = findViewById(R.id.tv_password_strength);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.departments_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDept.setAdapter(adapter);

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
            String name = etName.getText().toString().trim();
            String studentId = etStudentId.getText().toString().trim().toUpperCase();
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

            if (!isValidName(name)) {
                Toast.makeText(this, "Name must be between 2 and " + MAX_NAME_LENGTH + " characters", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Registering...", Toast.LENGTH_SHORT).show();

            long id = db.registerUser(name, studentId, email, password, role, department);
            if (id != -1) {
                Toast.makeText(this, "Registration successful! Please check your email to verify your account.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(RegisterActivity.this, EmailVerificationPendingActivity.class));
                finish();
            } else {
                btnRegister.setEnabled(true);
                Toast.makeText(this, "Failed: Email or ID already exists", Toast.LENGTH_LONG).show();
            }
        });

        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void setupStudentIdField(EditText etStudentId) {
        InputFilter[] filters = new InputFilter[] {
            new InputFilter.LengthFilter(8) {
                @Override
                public CharSequence filter(CharSequence source, int start, int end,
                                           Spanned dest, int dstart, int dend) {
                    if (source.length() == 0) return null;

                    StringBuilder result = new StringBuilder();
                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);
                        if (Character.isDigit(c)) {
                            result.append(c);
                        }
                    }
                    return result.length() > 0 ? result.toString() : "";
                }
            }
        };
        etStudentId.setFilters(filters);
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
