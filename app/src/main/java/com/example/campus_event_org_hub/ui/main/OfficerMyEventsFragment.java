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
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OfficerMyEventsFragment extends Fragment {

    private static final int TAB_UPCOMING  = 0;
    private static final int TAB_HAPPENING = 1;
    private static final int TAB_ENDED     = 2;

    private RecyclerView rv;
    private TextView tvEmpty;
    private String officerName;
    private List<Event> allOfficerEvents = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_officer_my_events, container, false);

        Bundle args = getArguments();
        officerName = args != null ? args.getString("USER_NAME", "") : "";

        rv      = view.findViewById(R.id.rv_my_events);
        tvEmpty = view.findViewById(R.id.tv_my_events_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        ImageButton btnBack = view.findViewById(R.id.btn_back_my_events);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
            });
        }

        // Set up tabs
        TabLayout tabs = view.findViewById(R.id.tabs_my_events);
        tabs.addTab(tabs.newTab().setText("Upcoming"));
        tabs.addTab(tabs.newTab().setText("Happening"));
        tabs.addTab(tabs.newTab().setText("Ended"));

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showTab(tab.getPosition());
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
        showTab(TAB_UPCOMING);
    }

    private void loadEvents() {
        DatabaseHelper db = new DatabaseHelper(requireContext());
        allOfficerEvents = db.getEventsByOfficer(officerName);
    }

    private void showTab(int tab) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        List<Event> filtered = new ArrayList<>();

        for (Event e : allOfficerEvents) {
            String date = e.getDate();
            int cmp = date.compareTo(today);
            if (tab == TAB_UPCOMING  && cmp > 0) filtered.add(e);
            if (tab == TAB_HAPPENING && cmp == 0) filtered.add(e);
            if (tab == TAB_ENDED     && cmp < 0) filtered.add(e);
        }

        if (filtered.isEmpty()) {
            rv.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            rv.setAdapter(new EventsAdapter(filtered));
        }
    }

    // ── Minimal adapter reusing event_list_item.xml ──────────────────────────

    static class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.VH> {

        private final List<Event> list;

        EventsAdapter(List<Event> list) { this.list = list; }

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

            // Status badge in category chip for clarity
            String statusLabel = e.getCategory() + " \u2022 " + e.getStatus();
            h.category.setText(statusLabel);
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, desc, date, category;
            VH(View v) {
                super(v);
                title    = v.findViewById(R.id.event_title);
                desc     = v.findViewById(R.id.event_description);
                date     = v.findViewById(R.id.event_date);
                category = v.findViewById(R.id.event_category);
            }
        }
    }
}
