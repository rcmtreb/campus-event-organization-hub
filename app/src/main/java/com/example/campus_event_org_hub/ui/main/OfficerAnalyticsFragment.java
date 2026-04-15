package com.example.campus_event_org_hub.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.google.android.material.button.MaterialButton;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OfficerAnalyticsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_officer_analytics, container, false);

        Bundle args = getArguments();
        String officerSid = args != null ? args.getString("USER_STUDENT_ID", "") : "";

        TextView tvTotalEvents        = view.findViewById(R.id.tv_total_events);
        TextView tvTotalRegistrations = view.findViewById(R.id.tv_total_registrations);
        LinearLayout breakdownContainer = view.findViewById(R.id.container_event_breakdown);
        View tvNoEvents              = view.findViewById(R.id.tv_no_events);

        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());

        List<Event> officerEvents = db.getEventsByCreatorSid(officerSid);
        int totalRegs = db.getTotalRegistrationsForCreatorSid(officerSid);

        tvTotalEvents.setText(String.valueOf(officerEvents.size()));
        tvTotalRegistrations.setText(String.valueOf(totalRegs));

        if (officerEvents.isEmpty()) {
            tvNoEvents.setVisibility(View.VISIBLE);
            return view;
        }

        tvNoEvents.setVisibility(View.GONE);

        // Reverse so oldest is added first (goes to bottom), latest added last (goes to top)
        Collections.reverse(officerEvents);

        for (Event e : officerEvents) {
            addEventSectionProgrammatically(breakdownContainer, e, db);
        }

        return view;
    }

    /**
     * Wraps each event's analytics in its own MaterialCardView using the new layout.
     */
    private void addEventSectionProgrammatically(LinearLayout parent, Event e,
                                                  DatabaseHelper db) {
        View cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_event_breakdown, parent, false);

        TextView tvTitle = cardView.findViewById(R.id.tv_breakdown_title);
        TextView tvDateStatus = cardView.findViewById(R.id.tv_breakdown_date_status);
        TextView tvTotal = cardView.findViewById(R.id.tv_breakdown_total);
        TextView tvTimeIn = cardView.findViewById(R.id.tv_breakdown_time_in);
        TextView tvTimeOut = cardView.findViewById(R.id.tv_breakdown_time_out);
        LinearLayout containerDepts = cardView.findViewById(R.id.container_breakdown_depts);
        LinearLayout containerCourses = cardView.findViewById(R.id.container_breakdown_courses);
        MaterialButton btnViewAttendees = cardView.findViewById(R.id.btn_view_attendees);

        tvTitle.setText(e.getTitle());

        String dateTime = e.getTime().isEmpty() ? e.getDate() : e.getDate() + "  " + e.getTime();
        tvDateStatus.setText(dateTime + "  |  " + e.getStatus());

        // Department stats
        Map<String, Integer> deptStats = db.getEventRegistrationStats(e.getId());
        int eventTotal = 0;
        for (int cnt : deptStats.values()) eventTotal += cnt;

        // Course stats
        Map<String, Integer> courseStats = db.getEventRegistrationStatsByCourse(e.getId());

        int timeInCount  = db.getAttendanceCount(e.getId());
        int timeOutCount = db.getTimeOutCount(e.getId());

        tvTotal.setText("Registered: " + eventTotal);
        tvTimeIn.setText("Timed In: " + timeInCount);
        tvTimeOut.setText("Timed Out: " + timeOutCount);

        if (!deptStats.isEmpty()) {
            for (Map.Entry<String, Integer> entry : deptStats.entrySet()) {
                TextView tvDept = new TextView(requireContext());
                tvDept.setText("  " + entry.getKey() + ": " + entry.getValue());
                tvDept.setTextColor(getResources().getColor(R.color.text_secondary, null));
                tvDept.setTextSize(12);
                containerDepts.addView(tvDept);
            }
        }

        if (!courseStats.isEmpty()) {
            for (Map.Entry<String, Integer> entry : courseStats.entrySet()) {
                TextView tvCourse = new TextView(requireContext());
                tvCourse.setText("  " + entry.getKey() + ": " + entry.getValue());
                tvCourse.setTextColor(getResources().getColor(R.color.text_secondary, null));
                tvCourse.setTextSize(12);
                containerCourses.addView(tvCourse);
            }
        }

        // "View Attendees" button → OfficerAttendeeListActivity
        btnViewAttendees.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), OfficerAttendeeListActivity.class);
            intent.putExtra(OfficerAttendeeListActivity.EXTRA_EVENT_ID, e.getId());
            intent.putExtra(OfficerAttendeeListActivity.EXTRA_EVENT_TITLE, e.getTitle());
            startActivity(intent);
        });

        parent.addView(cardView);
    }
}
