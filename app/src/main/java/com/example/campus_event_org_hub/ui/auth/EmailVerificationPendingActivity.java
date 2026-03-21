package com.example.campus_event_org_hub.ui.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.ui.main.MainActivity;
import com.example.campus_event_org_hub.util.SessionManager;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

public class EmailVerificationPendingActivity extends AppCompatActivity {

    private TextView tvEmail;
    private TextView tvStatus;
    private Button btnResend;
    private Button btnOpenEmail;
    private String studentId;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification_pending);

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

        DatabaseHelper db = new DatabaseHelper(this);
        if (studentId != null && db.isEmailVerified(studentId)) {
            tvStatus.setText("Email verified!");
            btnResend.setVisibility(View.GONE);
            btnOpenEmail.setVisibility(View.GONE);
            new Handler().postDelayed(() -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }, 1500);
            return;
        }

        btnResend.setOnClickListener(v -> resendVerificationEmail());

        btnOpenEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendVerificationEmail() {
        if (studentId == null || email == null) {
            Toast.makeText(this, "Unable to resend. Please try registering again.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnResend.setEnabled(false);
        tvStatus.setText("Sending...");

        Map<String, Object> data = new HashMap<>();
        data.put("studentId", studentId);
        data.put("email", email);

        FirebaseFunctions.getInstance()
                .getHttpsCallable("resendVerificationEmail")
                .call(data)
                .addOnSuccessListener(result -> {
                    tvStatus.setText("Verification email sent! Check your inbox.");
                    btnResend.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Failed to send email. Please try again.");
                    btnResend.setEnabled(true);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseHelper db = new DatabaseHelper(this);
        if (studentId != null && db.isEmailVerified(studentId)) {
            Toast.makeText(this, "Email verified!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}
