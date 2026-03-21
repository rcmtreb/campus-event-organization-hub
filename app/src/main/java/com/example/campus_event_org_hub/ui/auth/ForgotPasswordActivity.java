package com.example.campus_event_org_hub.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_event_org_hub.R;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnReset;
    private TextView tvMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.et_reset_email);
        btnReset = findViewById(R.id.btn_request_reset);
        tvMessage = findViewById(R.id.tv_reset_message);

        btnReset.setOnClickListener(v -> requestPasswordReset());
    }

    private void requestPasswordReset() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Enter your email");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            return;
        }

        btnReset.setEnabled(false);
        tvMessage.setText("Sending request...");
        tvMessage.setVisibility(android.view.View.VISIBLE);

        Map<String, Object> data = new HashMap<>();
        data.put("email", email);

        FirebaseFunctions.getInstance()
                .getHttpsCallable("requestPasswordReset")
                .call(data)
                .addOnSuccessListener(result -> {
                    tvMessage.setText("If an account exists with this email, a reset link has been sent. Check your inbox and spam folder.");
                    tvMessage.setVisibility(android.view.View.VISIBLE);
                    btnReset.setVisibility(android.view.View.GONE);
                })
                .addOnFailureListener(e -> {
                    btnReset.setEnabled(true);
                    tvMessage.setText("Error sending request. Please try again.");
                    tvMessage.setVisibility(android.view.View.VISIBLE);
                });
    }
}
