package com.example.campus_event_org_hub.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.google.android.material.card.MaterialCardView;

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
        String officerName = args != null ? args.getString("USER_NAME", "") : "";

        ImageButton btnBack = view.findViewById(R.id.btn_back_analytics);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
            });
        }

        TextView tvTotalEvents        = view.findViewById(R.id.tv_total_events);
        TextView tvTotalRegistrations = view.findViewById(R.id.tv_total_registrations);
        LinearLayout breakdownContainer = view.findViewById(R.id.container_event_breakdown);
        TextView tvNoEvents           = view.findViewById(R.id.tv_no_events);

        DatabaseHelper db = new DatabaseHelper(requireContext());

        List<Event> officerEvents = db.getEventsByOfficer(officerName);
        int totalRegs = db.getTotalRegistrationsForOfficer(officerName);

        tvTotalEvents.setText(String.valueOf(officerEvents.size()));
        tvTotalRegistrations.setText(String.valueOf(totalRegs));

        if (officerEvents.isEmpty()) {
            tvNoEvents.setVisibility(View.VISIBLE);
            return view;
        }

        tvNoEvents.setVisibility(View.GONE);

        for (Event e : officerEvents) {
            addEventSectionProgrammatically(breakdownContainer, e, db);
        }

        return view;
    }

    /**
     * Wraps each event's analytics in its own MaterialCardView.
     */
    private void addEventSectionProgrammatically(LinearLayout parent, Event e,
                                                  DatabaseHelper db) {
        // Outer card
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardLp);
        card.setRadius(24f);
        card.setCardElevation(4f);
        card.setStrokeColor(0xFFE0E0E0);
        card.setStrokeWidth(1);

        // Inner linear layout
        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (14 * getResources().getDisplayMetrics().density);
        inner.setPadding(pad, pad, pad, pad);

        // Title row
        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(e.getTitle());
        tvTitle.setTextColor(getResources().getColor(R.color.black, null));
        tvTitle.setTextSize(15);
        tvTitle.setPadding(0, 0, 0, 4);
        android.graphics.Typeface bold = android.graphics.Typeface.DEFAULT_BOLD;
        tvTitle.setTypeface(bold);
        inner.addView(tvTitle);

        // Date + status
        TextView tvDate = new TextView(requireContext());
        String dateTime = e.getTime().isEmpty() ? e.getDate() : e.getDate() + "  " + e.getTime();
        tvDate.setText(dateTime + "  |  " + e.getStatus());
        tvDate.setTextColor(0xFF666666);
        tvDate.setTextSize(12);
        tvDate.setPadding(0, 0, 0, 8);
        inner.addView(tvDate);

        // Registrations stats
        Map<String, Integer> stats = db.getEventRegistrationStats(e.getId());
        int eventTotal = 0;
        for (int cnt : stats.values()) eventTotal += cnt;

        TextView tvTotal = new TextView(requireContext());
        tvTotal.setText("Registrations: " + eventTotal);
        tvTotal.setTextColor(getResources().getColor(R.color.primary_blue, null));
        tvTotal.setTextSize(13);
        tvTotal.setPadding(0, 0, 0, 4);
        inner.addView(tvTotal);

        if (!stats.isEmpty()) {
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                TextView tvDept = new TextView(requireContext());
                tvDept.setText("  " + entry.getKey() + ": " + entry.getValue());
                tvDept.setTextColor(0xFF444444);
                tvDept.setTextSize(12);
                inner.addView(tvDept);
            }
        }

        card.addView(inner);
        parent.addView(card);
    }
}
