package com.example.campus_event_org_hub.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_event_org_hub.ui.base.BaseActivity;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.SyncManager;
import com.example.campus_event_org_hub.ui.auth.LoginActivity;
import com.example.campus_event_org_hub.util.SessionManager;

public class AdminActivity extends BaseActivity {

    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = new DatabaseHelper(this);

        updateStats();

        ImageButton btnLogout = findViewById(R.id.btn_admin_logout);
        LinearLayout cardApprove = findViewById(R.id.card_approve_events);
        LinearLayout cardUsers = findViewById(R.id.card_manage_users);
        LinearLayout cardEventControl = findViewById(R.id.card_event_control);
        LinearLayout cardReports = findViewById(R.id.card_reports);
        LinearLayout cardExportImport = findViewById(R.id.card_export_import);

        btnLogout.setOnClickListener(v -> {
            new SessionManager(this).clearSession();
            Toast.makeText(this, "Admin Logged Out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        cardApprove.setOnClickListener(v -> 
            startActivity(new Intent(this, ApproveEventsActivity.class)));

        cardUsers.setOnClickListener(v -> 
            startActivity(new Intent(this, UserManagementActivity.class)));

        cardEventControl.setOnClickListener(v ->
            startActivity(new Intent(this, AdminEventControlActivity.class)));

        cardReports.setOnClickListener(v ->
            startActivity(new Intent(this, AdminSystemStatsActivity.class)));

        cardExportImport.setOnClickListener(v ->
            startActivity(new Intent(this, ExportImportActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sync Firestore → local SQLite so admin sees latest data from all devices
        SyncManager.sync(this, this::updateStats);
    }

    private void updateStats() {
        TextView tvStudents = findViewById(R.id.tv_stat_students);
        TextView tvOfficers = findViewById(R.id.tv_stat_officers);
        TextView tvEvents = findViewById(R.id.tv_stat_events);
        TextView tvPending = findViewById(R.id.tv_stat_pending);
        TextView tvPendingBadge = findViewById(R.id.tv_pending_badge);

        int studentCount = db.getCount(DatabaseHelper.TABLE_USERS, DatabaseHelper.COLUMN_USER_ROLE + "=?", new String[]{"Student"});
        int officerCount = db.getCount(DatabaseHelper.TABLE_USERS, DatabaseHelper.COLUMN_USER_ROLE + "=?", new String[]{"Officer"});
        int eventCount = db.getCount(DatabaseHelper.TABLE_EVENTS, DatabaseHelper.COLUMN_STATUS + "=?", new String[]{"APPROVED"});
        int pendingCount = db.getCount(DatabaseHelper.TABLE_EVENTS, DatabaseHelper.COLUMN_STATUS + "=?", new String[]{"PENDING"});

        if (tvStudents != null) tvStudents.setText(String.valueOf(studentCount));
        if (tvOfficers != null) tvOfficers.setText(String.valueOf(officerCount));
        if (tvEvents != null) tvEvents.setText(String.valueOf(eventCount));
        if (tvPending != null) tvPending.setText(String.valueOf(pendingCount));
        if (tvPendingBadge != null) tvPendingBadge.setText(String.valueOf(pendingCount));
    }
}
