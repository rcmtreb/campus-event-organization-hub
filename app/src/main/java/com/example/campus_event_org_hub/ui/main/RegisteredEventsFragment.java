package com.example.campus_event_org_hub.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.ui.events.EventDetailActivity;
import com.example.campus_event_org_hub.util.ServerTimeUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RegisteredEventsFragment extends Fragment {

    private RecyclerView recycler;
    private View         emptyState;
    private String       sid;
    private String       userRole;

    private ActivityResultLauncher<Intent> detailLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register launcher in onCreate so it survives configuration changes
        detailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Refresh list in case registration changed inside EventDetailActivity
                    loadEvents();
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registered_events, container, false);

        Bundle args = getArguments();
        sid      = args != null ? args.getString("USER_STUDENT_ID", "") : "";
        userRole = args != null ? args.getString("USER_ROLE", "Student") : "Student";

        recycler   = view.findViewById(R.id.registered_events_recycler);
        emptyState = view.findViewById(R.id.registered_empty_state);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadEvents();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEvents();
    }

    private void loadEvents() {
        if (sid == null || sid.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
            return;
        }

        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
        List<Event> events = db.getRegisteredEvents(sid);

        if (events.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
            recycler.setAdapter(new RegisteredEventsAdapter(events, sid, userRole, detailLauncher));
        }
    }

    // ── Inner adapter ────────────────────────────────────────────────────────

    private static class RegisteredEventsAdapter
            extends RecyclerView.Adapter<RegisteredEventsAdapter.VH> {

        private final List<Event> items;
        private final String      studentId;
        private final String      userRole;
        private final ActivityResultLauncher<Intent> launcher;

        RegisteredEventsAdapter(List<Event> items, String studentId, String userRole,
                                ActivityResultLauncher<Intent> launcher) {
            this.items     = items;
            this.studentId = studentId;
            this.userRole  = userRole;
            this.launcher  = launcher;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.registered_event_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Event e = items.get(position);
            h.title.setText(e.getTitle());
            h.date.setText(e.getDate());
            h.category.setText(e.getCategory() != null ? e.getCategory() : "");
            h.countdown.setText(buildCountdown(e));

            // Tap to open EventDetailActivity
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), EventDetailActivity.class);
                intent.putExtra("event", e);
                intent.putExtra("USER_STUDENT_ID", studentId);
                intent.putExtra("USER_ROLE", userRole);
                if (launcher != null) {
                    launcher.launch(intent);
                } else {
                    v.getContext().startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String buildCountdown(Event event) {
            String dateStr    = event.getDate();
            String startTime  = event.getStartTime();  // "HH:mm" 24-hour format
            String endTime    = event.getEndTime();

            if (dateStr == null || dateStr.isEmpty()) return "Date unknown";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date eventDate = sdf.parse(dateStr);
                if (eventDate == null) return "Date unknown";

                long nowMs = ServerTimeUtil.nowMillis();
                java.util.Calendar nowCal = java.util.Calendar.getInstance();
                nowCal.setTimeInMillis(nowMs);

                // Build a Calendar for the event date at start time
                java.util.Calendar startCal = java.util.Calendar.getInstance();
                startCal.setTime(eventDate);
                if (startTime != null && startTime.contains(":")) {
                    String[] parts = startTime.split(":");
                    startCal.set(java.util.Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0].trim()));
                    startCal.set(java.util.Calendar.MINUTE,       Integer.parseInt(parts[1].trim()));
                } else {
                    startCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    startCal.set(java.util.Calendar.MINUTE, 0);
                }
                startCal.set(java.util.Calendar.SECOND, 0);
                startCal.set(java.util.Calendar.MILLISECOND, 0);

                // Build a Calendar for the event date at end time
                java.util.Calendar endCal = java.util.Calendar.getInstance();
                endCal.setTime(eventDate);
                if (endTime != null && endTime.contains(":")) {
                    String[] parts = endTime.split(":");
                    endCal.set(java.util.Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0].trim()));
                    endCal.set(java.util.Calendar.MINUTE,       Integer.parseInt(parts[1].trim()));
                } else {
                    endCal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                    endCal.set(java.util.Calendar.MINUTE, 59);
                }
                endCal.set(java.util.Calendar.SECOND, 59);
                endCal.set(java.util.Calendar.MILLISECOND, 999);

                if (nowMs > endCal.getTimeInMillis()) {
                    return "Event has passed";
                } else if (nowMs >= startCal.getTimeInMillis()) {
                    return "Ongoing";
                } else {
                    long diffMs = startCal.getTimeInMillis() - nowMs;
                    long days  = TimeUnit.MILLISECONDS.toDays(diffMs);
                    long hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24;
                    if (days > 0) return "in " + days + "d " + hours + "h";
                    if (hours > 0) return "in " + hours + "h";
                    long mins = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60;
                    return "in " + mins + "m";
                }
            } catch (ParseException ex) {
                return "Date unknown";
            }
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, date, category, countdown;
            VH(@NonNull View item) {
                super(item);
                title     = item.findViewById(R.id.reg_event_title);
                date      = item.findViewById(R.id.reg_event_date);
                category  = item.findViewById(R.id.reg_event_category);
                countdown = item.findViewById(R.id.reg_event_countdown);
            }
        }
    }
}
