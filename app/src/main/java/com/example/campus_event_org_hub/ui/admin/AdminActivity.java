package com.example.campus_event_org_hub.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.campus_event_org_hub.ui.base.BaseActivity;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.SyncManager;
import com.example.campus_event_org_hub.ui.auth.LoginActivity;
import com.example.campus_event_org_hub.util.SessionManager;

public class AdminActivity extends BaseActivity {

    private DatabaseHelper db;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected boolean useEdgeToEdge() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = DatabaseHelper.getInstance(this);

        swipeRefresh = findViewById(R.id.swipe_refresh_admin);
        swipeRefresh.setColorSchemeResources(R.color.primary_blue);
        swipeRefresh.setOnRefreshListener(() ->
            SyncManager.sync(this, () -> {
                updateStats();
                swipeRefresh.setRefreshing(false);
            })
        );

        updateStats();

        LinearLayout cardLogout = findViewById(R.id.card_logout);
        LinearLayout cardApprove = findViewById(R.id.card_approve_events);
        LinearLayout cardUsers = findViewById(R.id.card_manage_users);
        LinearLayout cardEventControl = findViewById(R.id.card_event_control);
        LinearLayout cardReports = findViewById(R.id.card_reports);
        LinearLayout cardExportImport = findViewById(R.id.card_export_import);
        LinearLayout cardDeleteAll = findViewById(R.id.card_delete_all);

        cardLogout.setOnClickListener(v -> {
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

        cardDeleteAll.setOnClickListener(v -> showDeleteAllStep1());
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
        if (tvPendingBadge != null) {
            if (pendingCount == 0) {
                tvPendingBadge.setVisibility(View.GONE);
            } else {
                tvPendingBadge.setVisibility(View.VISIBLE);
                tvPendingBadge.setText(String.valueOf(pendingCount));
            }
        }
    }

    // ── Delete All — Step 1: Warning dialog ──────────────────────────────────

    private void showDeleteAllStep1() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Data")
                .setMessage(
                        "This will permanently delete:\n\n" +
                        "  \u2022 All student and officer accounts\n" +
                        "  \u2022 All events\n" +
                        "  \u2022 All registrations\n" +
                        "  \u2022 All notifications\n\n" +
                        "Both the local database and Firestore will be wiped.\n" +
                        "Admin accounts are preserved.\n\n" +
                        "This action CANNOT be undone.")
                .setPositiveButton("Continue", (d, w) -> showDeleteAllStep2())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Delete All — Step 2: Type-to-confirm dialog ───────────────────────────

    private void showDeleteAllStep2() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete_all, null);
        EditText etConfirm = view.findViewById(R.id.et_confirm_text);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Type to confirm")
                .setView(view)
                .setPositiveButton("Delete Everything", null) // set below to prevent auto-dismiss
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnConfirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnConfirm.setTextColor(getResources().getColor(R.color.error_red, getTheme()));
            btnConfirm.setEnabled(false);

            etConfirm.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    btnConfirm.setEnabled("DELETE ALL".equals(s.toString()));
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                executeDeleteAll();
            });
        });

        dialog.show();
    }

    // ── Delete All — Execute ──────────────────────────────────────────────────

    private void executeDeleteAll() {
        Toast.makeText(this, "Deleting all data\u2026", Toast.LENGTH_SHORT).show();

        db.deleteAllStudentData(
            () -> {
                // Success — back on main thread
                Toast.makeText(this, "All data deleted successfully.", Toast.LENGTH_LONG).show();
                updateStats();
            },
            () -> {
                // Failure — SQLite was cleared but Firestore sync failed
                Toast.makeText(this,
                        "Local data cleared, but Firestore sync failed. Check your connection.",
                        Toast.LENGTH_LONG).show();
                updateStats();
            }
        );
    }
}
