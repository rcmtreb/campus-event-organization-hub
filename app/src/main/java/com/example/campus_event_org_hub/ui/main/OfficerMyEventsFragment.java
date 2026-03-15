package com.example.campus_event_org_hub.ui.main;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OfficerMyEventsFragment extends Fragment {

    private static final int TAB_UPCOMING  = 0;
    private static final int TAB_HAPPENING = 1;
    private static final int TAB_ENDED     = 2;
    private static final int TAB_POSTPONED = 3;

    private RecyclerView rv;
    private TextView tvEmpty;
    private String officerName;
    private String officerSid;
    private List<Event> allOfficerEvents = new ArrayList<>();
    private int currentTab = TAB_UPCOMING;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_officer_my_events, container, false);

        Bundle args = getArguments();
        officerName = args != null ? args.getString("USER_NAME",       "") : "";
        officerSid  = args != null ? args.getString("USER_STUDENT_ID", "") : "";

        rv      = view.findViewById(R.id.rv_my_events);
        tvEmpty = view.findViewById(R.id.tv_my_events_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        ImageButton btnBack = view.findViewById(R.id.btn_back_my_events);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
            });
        }

        // Set up tabs — 4 tabs
        TabLayout tabs = view.findViewById(R.id.tabs_my_events);
        tabs.addTab(tabs.newTab().setText("Upcoming"));
        tabs.addTab(tabs.newTab().setText("Happening"));
        tabs.addTab(tabs.newTab().setText("Ended"));
        tabs.addTab(tabs.newTab().setText("Postponed"));

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                showTab(currentTab);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Load data
        loadEvents();
        showTab(TAB_UPCOMING);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEvents();
        showTab(currentTab);
    }

    private void loadEvents() {
        DatabaseHelper db = new DatabaseHelper(requireContext());

        // Start with events the officer created
        List<Event> created = db.getEventsByCreatorSid(officerSid);

        // Also include events the officer registered for (so registered+upcoming events show up)
        List<Event> registered = db.getRegisteredEvents(officerSid);

        // Merge, deduplicating by event ID so created events aren't listed twice
        java.util.Map<Integer, Event> merged = new java.util.LinkedHashMap<>();
        for (Event e : created)    merged.put(e.getId(), e);
        for (Event e : registered) merged.putIfAbsent(e.getId(), e);

        allOfficerEvents = new ArrayList<>(merged.values());
    }

    private void showTab(int tab) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        List<Event> filtered = new ArrayList<>();

        for (Event e : allOfficerEvents) {
            String status = e.getStatus();
            String date   = e.getDate();

            // PENDING events are excluded entirely — visible only in "Pending Events" section
            if ("PENDING".equals(status)) continue;

            // POSTPONED events go only to the Postponed tab
            if ("POSTPONED".equals(status)) {
                if (tab == TAB_POSTPONED) filtered.add(e);
                continue;
            }

            // CANCELLED events are shown in Ended tab
            if ("CANCELLED".equals(status)) {
                if (tab == TAB_ENDED) filtered.add(e);
                continue;
            }

            // For APPROVED — categorise by date
            int cmp = date.compareTo(today);
            if (tab == TAB_UPCOMING  && cmp > 0)  filtered.add(e);
            if (tab == TAB_HAPPENING && cmp == 0)  filtered.add(e);
            if (tab == TAB_ENDED     && cmp < 0)   filtered.add(e);
        }

        if (filtered.isEmpty()) {
            rv.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            rv.setAdapter(new EventsAdapter(filtered, tab == TAB_POSTPONED, this));
        }
    }

    // ── Dialog: officer confirms admin date or proposes their own ────────────

    void showPostponedActionDialog(Event e) {
        DatabaseHelper db = new DatabaseHelper(requireContext());
        String adminDate = e.getDate(); // current date stored (admin's suggested date)

        String[] options = {
                "Confirm admin's date: " + adminDate,
                "Propose a different date"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Reschedule: " + e.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Confirm admin's suggested date as-is
                        boolean ok = db.proposeNewDate(e.getId(), adminDate);
                        if (ok) {
                            e.setStatus("APPROVED");
                            Toast.makeText(requireContext(),
                                    "Date confirmed. Event is now APPROVED.", Toast.LENGTH_SHORT).show();
                            loadEvents();
                            showTab(TAB_POSTPONED);
                        } else {
                            Toast.makeText(requireContext(), "Failed to update.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Let officer pick a new date
                        pickNewDate(e, db);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickNewDate(Event e, DatabaseHelper db) {
        Calendar cal = Calendar.getInstance();
        // Pre-fill the picker with the event's postponed date using direct field parsing
        // to avoid timezone shifts that occur when using Date/setTime()
        String[] parts = e.getDate().split("-");
        if (parts.length == 3) {
            try {
                cal.set(Calendar.YEAR,         Integer.parseInt(parts[0]));
                cal.set(Calendar.MONTH,        Integer.parseInt(parts[1]) - 1); // 0-based
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            } catch (NumberFormatException ignored) { }
        }

        new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    String newDate = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", year, month + 1, day);
                    // Officer is proposing a DIFFERENT date — must go back to admin for approval
                    boolean ok = db.proposeNewDatePending(e.getId(), newDate);
                    if (ok) {
                        e.setStatus("PENDING");
                        // Notify all admins that this officer proposed a new date
                        notifyAdminsOfProposedDate(e, newDate, db);
                        Toast.makeText(requireContext(),
                                "The admin will receive your proposal date.",
                                Toast.LENGTH_LONG).show();
                        loadEvents();
                        showTab(TAB_POSTPONED);
                    } else {
                        Toast.makeText(requireContext(), "Failed to update.", Toast.LENGTH_SHORT).show();
                    }
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    /**
     * Sends a notification to every Admin so they can review and re-approve the
     * officer's proposed reschedule date.
     */
    private void notifyAdminsOfProposedDate(Event e, String proposedDate, DatabaseHelper db) {
        String message = "Officer proposed a new date (" + proposedDate + ") for the postponed event \""
                + e.getTitle() + "\". Please review and approve or reject.";
        List<String> adminIds = db.getAdminStudentIds();
        for (String adminSid : adminIds) {
            db.insertNotification(adminSid, e.getId(), "PENDING",
                    message, "Officer-proposed reschedule date", proposedDate, "");
        }
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    static class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.VH> {

        private final List<Event> list;
        private final boolean isPostponedTab;
        private final OfficerMyEventsFragment fragment;

        EventsAdapter(List<Event> list, boolean isPostponedTab,
                      OfficerMyEventsFragment fragment) {
            this.list            = list;
            this.isPostponedTab  = isPostponedTab;
            this.fragment        = fragment;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.event_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Event e = list.get(pos);
            h.title.setText(e.getTitle());
            h.desc.setText(e.getDescription());
            h.date.setText(e.getTime().isEmpty()
                    ? e.getDate()
                    : e.getDate() + "  " + e.getTime());
            h.category.setText(e.getCategory());

            // ── Timeline badge ───────────────────────────────────────────────
            String status = e.getStatus();
            if ("CANCELLED".equals(status)) {
                h.timelineBadge.setText("CANCELLED");
                h.timelineBadge.setBackgroundColor(0xFFD32F2F);
                h.timelineBadge.setVisibility(View.VISIBLE);
            } else if ("POSTPONED".equals(status)) {
                h.timelineBadge.setText("POSTPONED");
                h.timelineBadge.setBackgroundColor(0xFF7B1FA2);
                h.timelineBadge.setVisibility(View.VISIBLE);
            } else if ("PENDING".equals(status)) {
                h.timelineBadge.setText("PENDING");
                h.timelineBadge.setBackgroundColor(0xFFF57C00);
                h.timelineBadge.setVisibility(View.VISIBLE);
            } else {
                // APPROVED / PENDING — compute from date
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(new Date());
                int cmp = e.getDate().compareTo(today);
                if (cmp > 0) {
                    h.timelineBadge.setText("UPCOMING");
                    h.timelineBadge.setBackgroundColor(0xFF388E3C);
                    h.timelineBadge.setVisibility(View.VISIBLE);
                } else if (cmp == 0) {
                    h.timelineBadge.setText("HAPPENING");
                    h.timelineBadge.setBackgroundColor(0xFF0288D1);
                    h.timelineBadge.setVisibility(View.VISIBLE);
                } else {
                    h.timelineBadge.setText("ENDED");
                    h.timelineBadge.setBackgroundColor(0xFF757575);
                    h.timelineBadge.setVisibility(View.VISIBLE);
                }
            }

            // ── Tap: postponed tab → reschedule dialog; otherwise no-op ─────
            if (isPostponedTab) {
                h.itemView.setOnClickListener(v ->
                        fragment.showPostponedActionDialog(e));
            } else {
                h.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, desc, date, category, timelineBadge;
            VH(View v) {
                super(v);
                title         = v.findViewById(R.id.event_title);
                desc          = v.findViewById(R.id.event_description);
                date          = v.findViewById(R.id.event_date);
                category      = v.findViewById(R.id.event_category);
                timelineBadge = v.findViewById(R.id.tv_timeline_badge);
            }
        }
    }
}
