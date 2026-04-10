package com.example.campus_event_org_hub.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.ui.events.EventAdapter;
import com.example.campus_event_org_hub.util.Refreshable;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.campus_event_org_hub.util.ServerTimeUtil;

public class DiscoverFragment extends Fragment implements Refreshable {

    private DatabaseHelper dbHelper;
    private String userDept = "";
    private String currentTimeFilter = "month";

    private LinearLayout tabContainer;
    private TextView tabToday, tabCampus, tabMyDept, tabExplore;
    private LinearLayout contentToday, contentCampus, contentMyDept, contentExplore;
    private NestedScrollView nestedScrollView;
    private RecyclerView rvTodayEvents, rvCampusEvents, rvMyDeptEvents;
    private TextView tvTodayEmpty, tvCampusEmpty, tvMyDeptEmpty;
    private LinearLayout deptListContainer;
    private TextView tvDeptEmpty;
    private ChipGroup chipGroupDeptFilter;

    private EventAdapter todayAdapter, campusAdapter, deptAdapter;
    private SwipeRefreshLayout swipeRefresh;

    private int currentTab = 0;
    private int scrollPositionToday = 0, scrollPositionCampus = 0, scrollPositionMyDept = 0, scrollPositionExplore = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        dbHelper = DatabaseHelper.getInstance(requireContext());

        Bundle args = getArguments();
        userDept = args != null ? args.getString("USER_DEPT", "") : "";

        initViews(view);
        setupTabSwitching();

        swipeRefresh = view.findViewById(R.id.swipe_refresh_discover);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeColors(
                    ContextCompat.getColor(requireContext(), R.color.primary_green));
            swipeRefresh.setOnRefreshListener(() -> {
                loadAllContent();
                swipeRefresh.setRefreshing(false);
            });
        }

        loadAllContent();

        return view;
    }

    private void initViews(View view) {
        tabContainer = view.findViewById(R.id.tab_container);
        tabToday = view.findViewById(R.id.tab_today);
        tabCampus = view.findViewById(R.id.tab_campus);
        tabMyDept = view.findViewById(R.id.tab_my_dept);
        tabExplore = view.findViewById(R.id.tab_explore);

        contentToday = view.findViewById(R.id.content_today);
        contentCampus = view.findViewById(R.id.content_campus);
        contentMyDept = view.findViewById(R.id.content_my_dept);
        contentExplore = view.findViewById(R.id.content_explore);

        nestedScrollView = view.findViewById(R.id.nested_scroll_view);

        rvTodayEvents = view.findViewById(R.id.rv_today_events);
        rvCampusEvents = view.findViewById(R.id.rv_campus_events);
        rvMyDeptEvents = view.findViewById(R.id.rv_my_dept_events);

        tvTodayEmpty = view.findViewById(R.id.tv_today_empty);
        tvCampusEmpty = view.findViewById(R.id.tv_campus_empty);
        tvMyDeptEmpty = view.findViewById(R.id.tv_my_dept_empty);

        deptListContainer = view.findViewById(R.id.dept_list_container);
        tvDeptEmpty = view.findViewById(R.id.tv_dept_empty);
        chipGroupDeptFilter = view.findViewById(R.id.chip_group_dept_filter);

        if (rvTodayEvents != null) {
            rvTodayEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvTodayEvents.setNestedScrollingEnabled(false);
        }
        if (rvCampusEvents != null) {
            rvCampusEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvCampusEvents.setNestedScrollingEnabled(false);
        }
        if (rvMyDeptEvents != null) {
            rvMyDeptEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvMyDeptEvents.setNestedScrollingEnabled(false);
        }

        if (tabToday != null) {
            tabToday.setSelected(true);
        }
    }

    private void setupTabSwitching() {
        if (tabToday != null) tabToday.setOnClickListener(v -> switchTab(0));
        if (tabCampus != null) tabCampus.setOnClickListener(v -> switchTab(1));
        if (tabMyDept != null) tabMyDept.setOnClickListener(v -> switchTab(2));
        if (tabExplore != null) tabExplore.setOnClickListener(v -> switchTab(3));

        if (chipGroupDeptFilter != null) {
            chipGroupDeptFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                String filter = "month";
                if (id == R.id.chip_dept_today) filter = "today";
                else if (id == R.id.chip_dept_week) filter = "week";
                else if (id == R.id.chip_dept_month) filter = "month";
                else if (id == R.id.chip_dept_year) filter = "year";
                loadDepartmentStats(filter);
            });
        }
    }

    private void switchTab(int tab) {
        if (contentToday == null || contentCampus == null || 
            contentMyDept == null || contentExplore == null) {
            return;
        }

        saveScrollPosition(currentTab);

        if (tabToday != null) tabToday.setSelected(tab == 0);
        if (tabCampus != null) tabCampus.setSelected(tab == 1);
        if (tabMyDept != null) tabMyDept.setSelected(tab == 2);
        if (tabExplore != null) tabExplore.setSelected(tab == 3);

        currentTab = tab;
        showContent(tab);
        restoreScrollPosition(tab);
    }

    private void showContent(int tab) {
        if (contentToday != null) contentToday.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        if (contentCampus != null) contentCampus.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        if (contentMyDept != null) contentMyDept.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        if (contentExplore != null) contentExplore.setVisibility(tab == 3 ? View.VISIBLE : View.GONE);
    }

    private void saveScrollPosition(int tab) {
        if (nestedScrollView == null) return;
        
        switch (tab) {
            case 0: scrollPositionToday = nestedScrollView.getScrollY(); break;
            case 1: scrollPositionCampus = nestedScrollView.getScrollY(); break;
            case 2: scrollPositionMyDept = nestedScrollView.getScrollY(); break;
            case 3: scrollPositionExplore = nestedScrollView.getScrollY(); break;
        }
    }

    private void restoreScrollPosition(int tab) {
        if (nestedScrollView == null) return;
        
        int position;
        switch (tab) {
            case 0: position = scrollPositionToday; break;
            case 1: position = scrollPositionCampus; break;
            case 2: position = scrollPositionMyDept; break;
            case 3: position = scrollPositionExplore; break;
            default: position = 0;
        }
        nestedScrollView.post(() -> nestedScrollView.scrollTo(0, position));
    }

    private void loadAllContent() {
        loadTodayEvents();
        loadCampusEvents();
        loadMyDepartmentEvents();
        loadDepartmentStats(currentTimeFilter);
    }

    private boolean isCampusTag(String tag) {
        return tag.equals("CAMPUS") || tag.equals("UCC") || tag.equals("UNIVERSITY");
    }

    /**
     * Extracts the abbreviation from a department string.
     * e.g. "College of Liberal Arts and Sciences (CLAS)" → "CLAS"
     * If no parenthesised abbreviation found, returns the trimmed input uppercased.
     */
    private String deptAbbr(String dept) {
        if (dept == null || dept.isEmpty()) return "";
        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(dept);
        if (m.find()) return m.group(1).trim().toUpperCase();
        return dept.trim().toUpperCase();
    }

    /**
     * Returns true only if the event's tags include the given department abbreviation.
     * Campus-wide tags (#CAMPUS / #UCC / #UNIVERSITY) are intentionally excluded here
     * so that campus-wide events appear only in the Campus tab, not in My Dept.
     * Tags are stored as space-separated tokens like "#CLAS #COE".
     */
    private boolean eventMatchesDept(String tags, String abbr) {
        if (tags == null || tags.isEmpty()) return false;
        if (abbr == null || abbr.isEmpty()) return false;
        // Split on whitespace, strip leading '#', compare exact token
        for (String token : tags.trim().split("\\s+")) {
            String t = token.replace("#", "").toUpperCase();
            if (t.equals(abbr)) return true;
        }
        return false;
    }

    // ── Today Events ─────────────────────────────────────────────────────────

    private void loadTodayEvents() {
        try {
            String today = ServerTimeUtil.todayString();

            List<Event> allEvents = dbHelper.getAllEvents();
            List<Event> todayEvents = new ArrayList<>();

            if (allEvents != null) {
                for (Event e : allEvents) {
                    String status = e.getStatus();
                    if (!"APPROVED".equals(status) && !"HAPPENING".equals(status)) continue;
                    String eventDate = e.getDate();
                    if (eventDate == null) continue;
                    // Normalise to yyyy-MM-dd in case a timestamp was stored
                    if (eventDate.length() > 10) eventDate = eventDate.substring(0, 10);
                    if (!eventDate.equals(today)) continue;
                    todayEvents.add(e);
                }
            }

            if (todayEvents.isEmpty()) {
                tvTodayEmpty.setVisibility(View.VISIBLE);
                rvTodayEvents.setVisibility(View.GONE);
            } else {
                tvTodayEmpty.setVisibility(View.GONE);
                rvTodayEvents.setVisibility(View.VISIBLE);
                todayAdapter = new EventAdapter(todayEvents);
                rvTodayEvents.setAdapter(todayAdapter);
            }
        } catch (Exception e) {
            tvTodayEmpty.setVisibility(View.VISIBLE);
            rvTodayEvents.setVisibility(View.GONE);
        }
    }

    // ── Campus Events ────────────────────────────────────────────────────────

    private void loadCampusEvents() {
        try {
            String today = ServerTimeUtil.todayString();

            List<Event> allEvents = dbHelper.getAllEvents();
            List<Event> campusEvents = new ArrayList<>();

            if (allEvents != null) {
                for (Event e : allEvents) {
                    String status = e.getStatus();
                    if (!"APPROVED".equals(status) && !"HAPPENING".equals(status)) continue;
                    String eventDate = e.getDate();
                    if (eventDate == null) continue;
                    if (eventDate.length() > 10) eventDate = eventDate.substring(0, 10);
                    if (eventDate.compareTo(today) < 0) continue;
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
                campusAdapter = new EventAdapter(campusEvents);
                rvCampusEvents.setAdapter(campusAdapter);
            }
        } catch (Exception e) {
            tvCampusEmpty.setVisibility(View.VISIBLE);
            rvCampusEvents.setVisibility(View.GONE);
        }
    }

    // ── My Department Events ────────────────────────────────────────────────

    private void loadMyDepartmentEvents() {
        try {
            String today = ServerTimeUtil.todayString();

            List<Event> allEvents = dbHelper.getAllEvents();
            List<Event> myDeptEvents = new ArrayList<>();

            if (allEvents != null && userDept != null && !userDept.isEmpty()) {
                String abbr = deptAbbr(userDept);
                for (Event e : allEvents) {
                    String status = e.getStatus();
                    if (!"APPROVED".equals(status) && !"HAPPENING".equals(status)) continue;
                    String eventDate = e.getDate();
                    if (eventDate == null) continue;
                    if (eventDate.length() > 10) eventDate = eventDate.substring(0, 10);
                    if (eventDate.compareTo(today) < 0) continue;
                    if (eventMatchesDept(e.getTags(), abbr)) {
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
                deptAdapter = new EventAdapter(myDeptEvents);
                rvMyDeptEvents.setAdapter(deptAdapter);
            }
        } catch (Exception e) {
            tvMyDeptEmpty.setVisibility(View.VISIBLE);
            rvMyDeptEvents.setVisibility(View.GONE);
        }
    }

    // ── Department Statistics ───────────────────────────────────────────────

    private void loadDepartmentStats(String timeFilter) {
        currentTimeFilter = timeFilter;
        deptListContainer.removeAllViews();

        try {
            List<Event> all = dbHelper.getAllEvents();
            if (all == null) all = new ArrayList<>();

            String today = ServerTimeUtil.todayString();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 6);
            String endOfWeek = sdf.format(cal.getTime());
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
                    addDepartmentRow(entry.getKey(), entry.getValue(), maxCount, i + 1);
                }
            }

        } catch (Exception e) {
            tvDeptEmpty.setVisibility(View.VISIBLE);
            tvDeptEmpty.setText("Error loading data");
        }
    }

    private void addDepartmentRow(String deptName, int count, int maxCount, int rank) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_department_stat, deptListContainer, false);

        TextView tvRank = row.findViewById(R.id.tv_dept_rank);
        TextView tvName = row.findViewById(R.id.tv_dept_name);
        TextView tvCount = row.findViewById(R.id.tv_dept_count);
        View progressBar = row.findViewById(R.id.dept_progress_bar);

        tvRank.setText(String.valueOf(rank));
        tvName.setText(deptName);
        tvCount.setText(String.valueOf(count));

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

        row.setOnClickListener(v -> switchTab(2));

        deptListContainer.addView(row);
    }

    /** Fix Bug 1 (Refreshable): called by MainActivity after a Firestore real-time update. */
    @Override
    public void refresh() {
        loadAllContent();
    }

    // Fix Bug 3: reload content when the fragment becomes visible again (e.g. after
    // admin approves an event and the user switches back to the Discover tab).
    @Override
    public void onResume() {
        super.onResume();
        loadAllContent();
    }

}
