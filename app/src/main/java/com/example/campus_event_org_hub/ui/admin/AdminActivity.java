package com.example.campus_event_org_hub.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.ui.auth.LoginActivity;
import com.google.android.material.card.MaterialCardView;

public class AdminActivity extends AppCompatActivity {

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = new DatabaseHelper(this);

        updateStats();

        ImageButton btnLogout = findViewById(R.id.btn_admin_logout);
        MaterialCardView cardApprove = findViewById(R.id.card_approve_events);
        MaterialCardView cardUsers = findViewById(R.id.card_manage_users);

        btnLogout.setOnClickListener(v -> {
            Toast.makeText(this, "Admin Logged Out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        cardApprove.setOnClickListener(v -> {
            // Intent to real approval activity
            startActivity(new Intent(this, ApproveEventsActivity.class));
        });

        cardUsers.setOnClickListener(v -> 
            Toast.makeText(this, "Opening User Management...", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats(); // Refresh stats when returning to dashboard
    }

    private void updateStats() {
        TextView tvTotalStudents = findViewById(R.id.tv_stat_students);
        TextView tvActiveEvents = findViewById(R.id.tv_stat_events);
        TextView tvPendingBadge = findViewById(R.id.tv_pending_badge);

        int studentCount = db.getCount(DatabaseHelper.TABLE_USERS, DatabaseHelper.COLUMN_USER_ROLE + "=?", new String[]{"Student"});
        int eventCount = db.getCount(DatabaseHelper.TABLE_EVENTS, DatabaseHelper.COLUMN_STATUS + "=?", new String[]{"APPROVED"});
        int pendingCount = db.getCount(DatabaseHelper.TABLE_EVENTS, DatabaseHelper.COLUMN_STATUS + "=?", new String[]{"PENDING"});

        // I need to add these IDs to activity_admin.xml first
        if (tvTotalStudents != null) tvTotalStudents.setText(String.valueOf(studentCount));
        if (tvActiveEvents != null) tvActiveEvents.setText(String.valueOf(eventCount));
        if (tvPendingBadge != null) tvPendingBadge.setText(String.valueOf(pendingCount));
    }
}
