package com.example.campus_event_org_hub.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.ui.events.EventAdapter;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DiscoverFragment extends Fragment {

    private DatabaseHelper dbHelper;
    private String userDept = "";
    private String currentTimeFilter = "month";
    private LinearLayout deptListContainer;
    private TextView tvDeptEmpty;
    private RecyclerView rvMyDeptEvents;
    private TextView tvMyDeptEmpty;
    private RecyclerView rvCampusEvents;
    private TextView tvCampusEmpty;
    private EventAdapter deptEventsAdapter;
    private EventAdapter campusEventsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        dbHelper = new DatabaseHelper(requireContext());

        Bundle args = getArguments();
        userDept = args != null ? args.getString("USER_DEPT", "") : "";

        deptListContainer = view.findViewById(R.id.dept_list_container);
        tvDeptEmpty = view.findViewById(R.id.tv_dept_empty);
        rvMyDeptEvents = view.findViewById(R.id.rv_my_dept_events);
        tvMyDeptEmpty = view.findViewById(R.id.tv_my_dept_empty);
        rvCampusEvents = view.findViewById(R.id.rv_campus_events);
        tvCampusEmpty = view.findViewById(R.id.tv_campus_empty);

        loadCampusEvents(view);
        loadDepartmentStats(view, currentTimeFilter);
        setupDeptFilter(view);
        loadMyDepartmentEvents(view);

        return view;
    }

    // ── Campus Events ─────────────────────────────────────────────────────────

    private void loadCampusEvents(View view) {
        rvCampusEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCampusEvents.setNestedScrollingEnabled(false);

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());

            List<Event> allEvents = dbHelper.getAllEvents();
            List<Event> campusEvents = new ArrayList<>();

            if (allEvents != null) {
                for (Event e : allEvents) {
                    if (!"APPROVED".equals(e.getStatus())) continue;
                    String eventDate = e.getDate();
                    if (eventDate == null || eventDate.compareTo(today) < 0) continue;
                    String tags = e.getTags();
                    if (tags != null) {
                        String upperTags = tags.toUpperCase();
                        if (upperTags.contains("CAMPUS") || upperTags.contains("UCC") || upperTags.contains("UNIVERSITY")) {
                            campusEvents.add(e);
                        }
                    }
                }
            }

            if (campusEvents.isEmpty()) {
                tvCampusEmpty.setVisibility(View.VISIBLE);
                rvCampusEvents.setVisibility(View.GONE);
            } else {
                tvCampusEmpty.setVisibility(View.GONE);
                rvCampusEvents.setVisibility(View.VISIBLE);
                campusEventsAdapter = new EventAdapter(campusEvents);
                rvCampusEvents.setAdapter(campusEventsAdapter);
            }

        } catch (Exception e) {
            tvCampusEmpty.setVisibility(View.VISIBLE);
            rvCampusEvents.setVisibility(View.GONE);
        }
    }

    // ── Department Statistics ─────────────────────────────────────────────

    private boolean isCampusTag(String tag) {
        return tag.equals("CAMPUS") || tag.equals("UCC") || tag.equals("UNIVERSITY");
    }

    private void loadDepartmentStats(View view, String timeFilter) {
        currentTimeFilter = timeFilter;
        deptListContainer.removeAllViews();

        try {
            List<Event> all = dbHelper.getAllEvents();
            if (all == null) all = new ArrayList<>();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());

            Calendar cal = Calendar.getInstance();
            String endOfWeek = sdf.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 6);
            cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 1);
            String endOfMonth = sdf.format(cal.getTime());
            cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, 1);
            String endOfYear = sdf.format(cal.getTime());

            List<Event> filteredEvents = new ArrayList<>();
            for (Event e : all) {
                if (!"APPROVED".equals(e.getStatus())) continue;
                if (e.getDate() == null) continue;

                String eventDate = e.getDate();
                switch (timeFilter) {
                    case "today":
                        if (eventDate.equals(today)) filteredEvents.add(e);
                        break;
                    case "week":
                        if (eventDate.compareTo(today) >= 0 && eventDate.compareTo(endOfWeek) <= 0)
                            filteredEvents.add(e);
                        break;
                    case "month":
                        if (eventDate.compareTo(today) >= 0 && eventDate.compareTo(endOfMonth) <= 0)
                            filteredEvents.add(e);
                        break;
                    case "year":
                    default:
                        if (eventDate.compareTo(today) >= 0 && eventDate.compareTo(endOfYear) <= 0)
                            filteredEvents.add(e);
                        break;
                }
            }

            Map<String, Integer> deptCount = new HashMap<>();
            for (Event e : filteredEvents) {
                String tags = e.getTags();
                if (tags != null && !tags.isEmpty()) {
                    String[] tagArray = tags.split("\\s+");
                    for (String tag : tagArray) {
                        tag = tag.replace("#", "").trim().toUpperCase();
                        if (!tag.isEmpty() && !isCampusTag(tag)) {
                            deptCount.put(tag, deptCount.getOrDefault(tag, 0) + 1);
                        }
                    }
                }
            }

            if (deptCount.isEmpty()) {
                tvDeptEmpty.setVisibility(View.VISIBLE);
                tvDeptEmpty.setText("No events found for this period");
            } else {
                tvDeptEmpty.setVisibility(View.GONE);

                List<Map.Entry<String, Integer>> sortedDepts = new ArrayList<>(deptCount.entrySet());
                sortedDepts.sort((a, b) -> b.getValue().compareTo(a.getValue()));

                int maxDisplay = Math.min(sortedDepts.size(), 8);
                int maxCount = sortedDepts.get(0).getValue();

                for (int i = 0; i < maxDisplay; i++) {
                    Map.Entry<String, Integer> entry = sortedDepts.get(i);
                    addDepartmentRow(deptListContainer, entry.getKey(), entry.getValue(), maxCount, i + 1);
                }
            }

        } catch (Exception e) {
            tvDeptEmpty.setVisibility(View.VISIBLE);
            tvDeptEmpty.setText("Error loading data");
        }
    }

    private void addDepartmentRow(LinearLayout container, String deptName, int count, int maxCount, int rank) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_department_stat, container, false);

        TextView tvRank = row.findViewById(R.id.tv_dept_rank);
        TextView tvName = row.findViewById(R.id.tv_dept_name);
        TextView tvCount = row.findViewById(R.id.tv_dept_count);
        View progressBar = row.findViewById(R.id.dept_progress_bar);

        tvRank.setText(String.valueOf(rank));
        tvName.setText(deptName);
        tvCount.setText(count + " event" + (count != 1 ? "s" : ""));

        int rankColor;
        switch (rank) {
            case 1: rankColor = 0xFFFFD700; break;
            case 2: rankColor = 0xFFC0C0C0; break;
            case 3: rankColor = 0xFFCD7F32; break;
            default: rankColor = 0xFF9E9E9E; break;
        }
        tvRank.getBackground().setTint(rankColor);

        float progress = maxCount > 0 ? (float) count / maxCount : 0;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) progressBar.getLayoutParams();
        params.width = 0;
        params.weight = progress;
        progressBar.setLayoutParams(params);
        progressBar.setBackgroundColor(0xFF5A9E3A);

        container.addView(row);
    }

    // ── Department Time Filter ────────────────────────────────────────────

    private void setupDeptFilter(View view) {
        ChipGroup cg = view.findViewById(R.id.chip_group_dept_filter);
        cg.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            String filter = "month";
            if (id == R.id.chip_dept_today) filter = "today";
            else if (id == R.id.chip_dept_week) filter = "week";
            else if (id == R.id.chip_dept_month) filter = "month";
            else if (id == R.id.chip_dept_year) filter = "year";
            loadDepartmentStats(view, filter);
        });
    }

    // ── My Department Events ──────────────────────────────────────────────

    private void loadMyDepartmentEvents(View view) {
        rvMyDeptEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyDeptEvents.setNestedScrollingEnabled(false);

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());

            List<Event> allEvents = dbHelper.getAllEvents();
            List<Event> myDeptEvents = new ArrayList<>();

            if (allEvents != null && userDept != null && !userDept.isEmpty()) {
                for (Event e : allEvents) {
                    if (!"APPROVED".equals(e.getStatus())) continue;
                    String eventDate = e.getDate();
                    if (eventDate == null || eventDate.compareTo(today) < 0) continue;
                    String tags = e.getTags();
                    if (tags != null && tags.toUpperCase().contains(userDept.toUpperCase())) {
                        myDeptEvents.add(e);
                    }
                }
            }

            if (myDeptEvents.isEmpty()) {
                tvMyDeptEmpty.setVisibility(View.VISIBLE);
                rvMyDeptEvents.setVisibility(View.GONE);
            } else {
                tvMyDeptEmpty.setVisibility(View.GONE);
                rvMyDeptEvents.setVisibility(View.VISIBLE);
                deptEventsAdapter = new EventAdapter(myDeptEvents);
                rvMyDeptEvents.setAdapter(deptEventsAdapter);
            }

        } catch (Exception e) {
            tvMyDeptEmpty.setVisibility(View.VISIBLE);
            rvMyDeptEvents.setVisibility(View.GONE);
        }
    }
}
