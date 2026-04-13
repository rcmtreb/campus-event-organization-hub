package com.example.campus_event_org_hub.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.AcademicSettings;
import com.example.campus_event_org_hub.model.PromotionLog;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AdminAcademicSettingsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private TextInputEditText etYearEnd, etInactivity;
    private TextView tvPromotionInfo, tvLastPromotion, tvNoPromotions;
    private RecyclerView rvLog;
    private PromotionLogAdapter logAdapter;
    private List<PromotionLog> logs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_academic_settings);

        db = DatabaseHelper.getInstance(this);

        etYearEnd        = findViewById(R.id.et_year_end_date);
        etInactivity     = findViewById(R.id.et_inactivity_threshold);
        tvPromotionInfo  = findViewById(R.id.tv_promotion_info);
        tvLastPromotion  = findViewById(R.id.tv_last_promotion);
        tvNoPromotions   = findViewById(R.id.tv_no_promotions);
        rvLog            = findViewById(R.id.rv_promotion_log);

        rvLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new PromotionLogAdapter(logs);
        rvLog.setAdapter(logAdapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save_settings).setOnClickListener(v -> saveSettings());
        findViewById(R.id.btn_promote_now).setOnClickListener(v -> confirmPromoteNow());

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            AcademicSettings settings = db.getAcademicSettings();
            int pending = db.getStudentsPendingPromotion();
            List<PromotionLog> logList = db.getPromotionLog(20);

            runOnUiThread(() -> {
                if (settings != null) {
                    etYearEnd.setText(settings.getAcademicYearEnd() != null ? settings.getAcademicYearEnd() : "");
                    etInactivity.setText(String.valueOf(settings.getInactivityThresholdYears()));
                    String lastPromo = settings.getLastPromotionDate();
                    tvLastPromotion.setText("Last promotion: " +
                            (lastPromo != null && !lastPromo.isEmpty() ? lastPromo : "Never"));
                }

                tvPromotionInfo.setText(pending + " student(s) eligible for promotion.");

                logs.clear();
                logs.addAll(logList);
                logAdapter.notifyDataSetChanged();

                boolean noLogs = logs.isEmpty();
                rvLog.setVisibility(noLogs ? View.GONE : View.VISIBLE);
                tvNoPromotions.setVisibility(noLogs ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    private void saveSettings() {
        String yearEnd   = etYearEnd.getText() != null ? etYearEnd.getText().toString().trim() : "";
        String inactStr  = etInactivity.getText() != null ? etInactivity.getText().toString().trim() : "";

        if (yearEnd.isEmpty()) {
            Toast.makeText(this, "Please enter the academic year end date (MM-dd).", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!yearEnd.matches("\\d{2}-\\d{2}")) {
            Toast.makeText(this, "Date must be in MM-dd format (e.g. 05-31).", Toast.LENGTH_SHORT).show();
            return;
        }
        int inactivity = 2;
        try { inactivity = Integer.parseInt(inactStr); } catch (NumberFormatException ignored) {}

        final int finalInact = inactivity;
        new Thread(() -> {
            AcademicSettings settings = db.getAcademicSettings();
            String lastPromo = settings != null ? settings.getLastPromotionDate() : null;
            AcademicSettings updated = new AcademicSettings(yearEnd, lastPromo, finalInact);
            db.saveAcademicSettings(updated);
            runOnUiThread(() -> {
                Toast.makeText(this, "Settings saved.", Toast.LENGTH_SHORT).show();
                loadData();
            });
        }).start();
    }

    private void confirmPromoteNow() {
        new AlertDialog.Builder(this)
                .setTitle("Promote Students")
                .setMessage("This will advance all eligible active students by one year level.\n\nProceed?")
                .setPositiveButton("Promote", (d, w) -> {
                    new Thread(() -> {
                        db.promoteStudents("MANUAL");
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Promotion complete!", Toast.LENGTH_SHORT).show();
                            loadData();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── PromotionLog RecyclerView adapter ──────────────────────────────────────

    private static class PromotionLogAdapter extends RecyclerView.Adapter<PromotionLogAdapter.VH> {

        private final List<PromotionLog> data;

        PromotionLogAdapter(List<PromotionLog> data) { this.data = data; }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_promotion_log, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            PromotionLog log = data.get(pos);
            h.tvStudentId.setText(log.getStudentId() + "-S");
            h.tvYearChange.setText(ordinal(log.getOldYearLevel()) + " \u2192 " + ordinal(log.getNewYearLevel()));
            h.tvType.setText(log.getPromotionType());
            String date = log.getPromotedAt();
            h.tvDate.setText(date != null && date.length() >= 10 ? date.substring(0, 10) : "");
        }

        @Override
        public int getItemCount() { return data.size(); }

        private static String ordinal(int year) {
            switch (year) {
                case 1: return "1st Year";
                case 2: return "2nd Year";
                case 3: return "3rd Year";
                case 4: return "4th Year";
                default: return year + "th Year";
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvStudentId, tvYearChange, tvType, tvDate;
            VH(View v) {
                super(v);
                tvStudentId  = v.findViewById(R.id.tv_log_student_id);
                tvYearChange = v.findViewById(R.id.tv_log_year_change);
                tvType       = v.findViewById(R.id.tv_log_type);
                tvDate       = v.findViewById(R.id.tv_log_date);
            }
        }
    }
}
