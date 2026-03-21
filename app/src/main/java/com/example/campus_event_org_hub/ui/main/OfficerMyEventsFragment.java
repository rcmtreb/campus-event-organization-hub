package com.example.campus_event_org_hub.ui.main;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.campus_event_org_hub.util.ImageUtils;

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
    private View tvEmpty;
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
        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());

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

            // For APPROVED/HAPPENING/ENDED — categorise by status first;
            // fallback to date for legacy APPROVED events.
            if ("HAPPENING".equals(status)) {
                if (tab == TAB_HAPPENING) filtered.add(e);
                continue;
            }
            if ("ENDED".equals(status) || "CANCELLED".equals(status)) {
                if (tab == TAB_ENDED) filtered.add(e);
                continue;
            }
            if ("APPROVED".equals(status)) {
                int cmp = date.compareTo(today);
                if (tab == TAB_UPCOMING  && cmp > 0)  filtered.add(e);
                if (tab == TAB_HAPPENING && cmp == 0)  filtered.add(e);
                if (tab == TAB_ENDED     && cmp < 0)   filtered.add(e);
            }
        }

        if (filtered.isEmpty()) {
            rv.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            boolean canManageAttendance = (tab == TAB_UPCOMING || tab == TAB_HAPPENING);
            rv.setAdapter(new EventsAdapter(filtered, tab == TAB_POSTPONED, canManageAttendance, this));
        }
    }

    // ── Dialog: attendance code management ──────────────────────────────────

    void showAttendanceCodeDialog(Event event) {
        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
        List<Event> fresh = db.getEventsByCreatorSid(officerSid);
        Event ev = event;
        for (Event fe : fresh) { if (fe.getId() == event.getId()) { ev = fe; break; } }
        final Event finalEv = ev;
        final DatabaseHelper finalDb = db;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_attendance_codes, null);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_event_title);
        TextView tvTimeInCode = dialogView.findViewById(R.id.tv_time_in_code);
        TextView tvTimeOutCode = dialogView.findViewById(R.id.tv_time_out_code);
        TextView tvAttendanceCount = dialogView.findViewById(R.id.tv_attendance_count);
        Button btnGenerateTimeIn = dialogView.findViewById(R.id.btn_generate_time_in);
        Button btnGenerateTimeOut = dialogView.findViewById(R.id.btn_generate_time_out);
        Button btnStartEvent = dialogView.findViewById(R.id.btn_start_event);
        Button btnEndEvent = dialogView.findViewById(R.id.btn_end_event);

        tvTitle.setText(finalEv.getTitle());
        updateCodeDisplay(tvTimeInCode, finalEv.getTimeInCode());
        updateCodeDisplay(tvTimeOutCode, finalEv.getTimeOutCode());
        int attendCount = finalDb.getAttendanceCount(finalEv.getId());
        tvAttendanceCount.setText(attendCount + " student" + (attendCount != 1 ? "s" : "") + " attended");

        btnGenerateTimeIn.setOnClickListener(v -> {
            String newCode = DatabaseHelper.generateAttendanceCode();
            boolean ok = finalDb.setTimeInCode(finalEv.getId(), newCode);
            if (ok) {
                finalEv.setTimeInCode(newCode);
                updateCodeDisplay(tvTimeInCode, newCode);
                Toast.makeText(requireContext(), "New Time-In code: " + newCode, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "Failed to set code.", Toast.LENGTH_SHORT).show();
            }
        });

        btnGenerateTimeOut.setOnClickListener(v -> {
            String newCode = DatabaseHelper.generateAttendanceCode();
            boolean ok = finalDb.setTimeOutCode(finalEv.getId(), newCode);
            if (ok) {
                finalEv.setTimeOutCode(newCode);
                updateCodeDisplay(tvTimeOutCode, newCode);
                Toast.makeText(requireContext(), "New Time-Out code: " + newCode, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "Failed to set code.", Toast.LENGTH_SHORT).show();
            }
        });

        btnStartEvent.setOnClickListener(v -> {
            finalDb.startEvent(finalEv.getId());
            finalEv.setStatus("HAPPENING");
            Toast.makeText(requireContext(), "Event started: status set to HAPPENING.", Toast.LENGTH_SHORT).show();
            showTab(currentTab);
        });

        btnEndEvent.setOnClickListener(v -> {
            finalDb.endEvent(finalEv.getId());
            finalEv.setStatus("ENDED");
            Toast.makeText(requireContext(), "Event ended: status set to ENDED.", Toast.LENGTH_SHORT).show();
            showTab(currentTab);
        });

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .show();
    }

    private void updateCodeDisplay(TextView tv, String code) {
        if (code == null || code.isEmpty()) {
            tv.setText("---");
            tv.setTextColor(getResources().getColor(R.color.text_hint, null));
        } else {
            tv.setText(code);
            tv.setTextColor(getResources().getColor(R.color.text_primary, null));
        }
    }

    // ── Dialog: officer confirms admin date or proposes their own ────────────

    void showPostponedActionDialog(Event e) {
        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
        final String adminDate = e.getDate();
        final Event finalEvent = e;
        final DatabaseHelper finalDb = db;

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_postponed_action, null);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_event_title);
        TextView tvDate = dialogView.findViewById(R.id.tv_admin_proposed_date);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_date);
        Button btnPropose = dialogView.findViewById(R.id.btn_propose_date);

        tvTitle.setText(e.getTitle());
        tvDate.setText(formatDateDisplay(adminDate));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .create();

        btnConfirm.setOnClickListener(v -> {
            boolean ok = finalDb.proposeNewDate(finalEvent.getId(), adminDate);
            if (ok) {
                finalEvent.setStatus("APPROVED");
                Toast.makeText(requireContext(),
                        "Date confirmed. Event is now APPROVED.", Toast.LENGTH_SHORT).show();
                loadEvents();
                showTab(TAB_POSTPONED);
            } else {
                Toast.makeText(requireContext(), "Failed to update.", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        btnPropose.setOnClickListener(v -> {
            dialog.dismiss();
            pickNewDate(finalEvent, finalDb);
        });

        dialog.show();
    }

    private String formatDateDisplay(String date) {
        try {
            String[] parts = date.split("-");
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                String[] months = {"January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"};
                return months[month - 1] + " " + day + ", " + year;
            }
        } catch (Exception ignored) { }
        return date;
    }

    private void pickNewDate(Event e, DatabaseHelper db) {
        Calendar cal = Calendar.getInstance();
        String[] parts = e.getDate().split("-");
        if (parts.length == 3) {
            try {
                cal.set(Calendar.YEAR,         Integer.parseInt(parts[0]));
                cal.set(Calendar.MONTH,        Integer.parseInt(parts[1]) - 1);
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            } catch (NumberFormatException ignored) { }
        }

        new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    String newDate = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", year, month + 1, day);
                    boolean ok = db.proposeNewDateTimePending(e.getId(), newDate, "");
                    if (ok) {
                        e.setStatus("PENDING");
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

    private void notifyAdminsOfProposedDate(Event e, String proposedDate, DatabaseHelper db) {
        String message = "Officer proposed a new date (" + proposedDate + ") for the postponed event \""
                + e.getTitle() + "\". Please review and approve or reject.";
        List<String> adminIds = db.getAdminStudentIds();
        for (String adminSid : adminIds) {
            db.insertNotification(adminSid, e.getId(), "PENDING",
                    message, "Officer-proposed reschedule date", proposedDate, "", "");
        }
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    static class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.VH> {

        private final List<Event> list;
        private final boolean isPostponedTab;
        private final boolean canManageAttendance;
        private final OfficerMyEventsFragment fragment;

        EventsAdapter(List<Event> list, boolean isPostponedTab, boolean canManageAttendance,
                      OfficerMyEventsFragment fragment) {
            this.list                = list;
            this.isPostponedTab      = isPostponedTab;
            this.canManageAttendance = canManageAttendance;
            this.fragment            = fragment;
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

            // Load event banner image
            int fallback = ImageUtils.getDefaultBannerForCategory(e.getCategory());
            ImageUtils.load(h.itemView.getContext(), h.image, e.getImagePath(), fallback);

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

            // ── Tap handler ──────────────────────────────────────────────────
            if (isPostponedTab) {
                h.itemView.setOnClickListener(v -> fragment.showPostponedActionDialog(e));
            } else if (canManageAttendance) {
                h.itemView.setOnClickListener(v -> fragment.showAttendanceCodeDialog(e));
            } else {
                h.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, desc, date, category, timelineBadge;
            ImageView image;
            VH(View v) {
                super(v);
                title         = v.findViewById(R.id.event_title);
                desc          = v.findViewById(R.id.event_description);
                date          = v.findViewById(R.id.event_date);
                category      = v.findViewById(R.id.event_category);
                timelineBadge = v.findViewById(R.id.tv_timeline_badge);
                image         = v.findViewById(R.id.event_image);
            }
        }
    }
}
