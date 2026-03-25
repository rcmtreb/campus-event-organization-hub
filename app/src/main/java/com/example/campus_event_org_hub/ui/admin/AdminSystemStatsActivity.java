package com.example.campus_event_org_hub.ui.admin;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.ui.base.BaseActivity;

import java.util.List;
import java.util.Map;

public class AdminSystemStatsActivity extends BaseActivity {

    @Override
    protected boolean isExitOnBackEnabled() { return false; }

    @Override
    protected boolean useEdgeToEdge() { return true; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_system_stats);

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        Map<String, Object> stats = db.getSystemStats();

        // Users
        setText(R.id.tv_stats_students, stats.get("totalStudents"));
        setText(R.id.tv_stats_officers, stats.get("totalOfficers"));

        // Events by status
        setText(R.id.tv_stats_approved,  stats.get("totalApproved"));
        setText(R.id.tv_stats_pending,   stats.get("totalPending"));
        setText(R.id.tv_stats_cancelled, stats.get("totalCancelled"));
        setText(R.id.tv_stats_postponed, stats.get("totalPostponed"));

        // Registrations
        setText(R.id.tv_stats_total_regs, stats.get("totalRegistrations"));

        // Top events
        LinearLayout containerTopEvents = findViewById(R.id.container_top_events);
        @SuppressWarnings("unchecked")
        List<String[]> topEvents = (List<String[]>) stats.get("topEvents");
        if (topEvents != null && !topEvents.isEmpty()) {
            int rank = 1;
            for (String[] row : topEvents) {
                // row[0] = title, row[1] = count
                TextView tv = new TextView(this);
                tv.setText(rank + ". " + row[0] + "  —  " + row[1] + " registrations");
                tv.setTextColor(0xFF212121);
                tv.setTextSize(13);
                tv.setPadding(0, 0, 0, 8);
                containerTopEvents.addView(tv);
                rank++;
            }
        } else {
            TextView tv = new TextView(this);
            tv.setText("No registrations yet.");
            tv.setTextColor(0xFF9E9E9E);
            tv.setTextSize(13);
            containerTopEvents.addView(tv);
        }

        // Dept breakdown
        LinearLayout containerDept = findViewById(R.id.container_dept_breakdown);
        @SuppressWarnings("unchecked")
        Map<String, Integer> deptMap = (Map<String, Integer>) stats.get("deptBreakdown");
        if (deptMap != null && !deptMap.isEmpty()) {
            for (Map.Entry<String, Integer> entry : deptMap.entrySet()) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowLp.setMargins(0, 0, 0, 6);
                row.setLayoutParams(rowLp);

                TextView tvDept = new TextView(this);
                tvDept.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tvDept.setText(entry.getKey());
                tvDept.setTextColor(0xFF212121);
                tvDept.setTextSize(13);

                TextView tvCount = new TextView(this);
                tvCount.setText(String.valueOf(entry.getValue()));
                tvCount.setTextColor(getResources().getColor(R.color.primary_blue, null));
                tvCount.setTextSize(13);
                tvCount.setGravity(Gravity.END);

                row.addView(tvDept);
                row.addView(tvCount);
                containerDept.addView(row);
            }
        } else {
            TextView tv = new TextView(this);
            tv.setText("No department data yet.");
            tv.setTextColor(0xFF9E9E9E);
            tv.setTextSize(13);
            containerDept.addView(tv);
        }
    }

    private void setText(int viewId, Object value) {
        TextView tv = findViewById(viewId);
        if (tv != null && value != null) tv.setText(String.valueOf(value));
    }
}
