package com.example.campus_event_org_hub.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RegisteredEventsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registered_events, container, false);

        Bundle args   = getArguments();
        String sid    = args != null ? args.getString("USER_STUDENT_ID", "") : "";

        ImageButton btnBack = view.findViewById(R.id.btn_back_registered);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        RecyclerView recycler   = view.findViewById(R.id.registered_events_recycler);
        View         emptyState = view.findViewById(R.id.registered_empty_state);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (sid.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
            return view;
        }

        DatabaseHelper db = new DatabaseHelper(requireContext());
        List<Event> events = db.getRegisteredEvents(sid);

        if (events.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
            recycler.setAdapter(new RegisteredEventsAdapter(events));
        }

        return view;
    }

    // ── Inner adapter ────────────────────────────────────────────────────────

    private static class RegisteredEventsAdapter
            extends RecyclerView.Adapter<RegisteredEventsAdapter.VH> {

        private final List<Event> items;

        RegisteredEventsAdapter(List<Event> items) { this.items = items; }

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
            h.countdown.setText(buildCountdown(e.getDate()));
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String buildCountdown(String dateStr) {
            if (dateStr == null || dateStr.isEmpty()) return "Date unknown";
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date eventDate = sdf.parse(dateStr);
                if (eventDate == null) return "Date unknown";
                long diffMs = eventDate.getTime() - System.currentTimeMillis();
                if (diffMs <= 0) return "Event has passed";
                long days  = TimeUnit.MILLISECONDS.toDays(diffMs);
                long hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24;
                if (days > 0) return days + "d " + hours + "h remaining";
                return hours + "h remaining";
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
