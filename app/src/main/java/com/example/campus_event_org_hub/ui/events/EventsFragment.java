package com.example.campus_event_org_hub.ui.events;

import android.content.ContentResolver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> eventList;
    private SearchView searchView;
    private ChipGroup chipGroup;
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);

        dbHelper = new DatabaseHelper(getContext());
        recyclerView = view.findViewById(R.id.events_recycler_view);
        searchView = view.findViewById(R.id.search_view);
        chipGroup = view.findViewById(R.id.chip_group_filter);

        loadEvents();

        FloatingActionButton fab = view.findViewById(R.id.fab_create_event);
        
        String role = getArguments() != null ? getArguments().getString("USER_ROLE", "Student") : "Student";
        if ("Student".equalsIgnoreCase(role)) {
            fab.setVisibility(View.GONE);
        } else {
            fab.setVisibility(View.VISIBLE);
        }

        fab.setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CreateEventFragment())
                .addToBackStack(null)
                .commit());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    adapter.filterByCategory(chip.getText().toString());
                }
            }
        });

        return view;
    }

    private void loadEvents() {
        eventList = dbHelper.getAllEvents(); // Gets "APPROVED" events
        
        if (eventList.isEmpty()) {
            addDefaultEvents();
            eventList = dbHelper.getAllEvents();
        }

        adapter = new EventAdapter(eventList);
        recyclerView.setAdapter(adapter);
    }

    private void addDefaultEvents() {
        String resPrefix = ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + 
                           requireContext().getPackageName() + "/";

        dbHelper.addEvent(new Event("Tech Summit 2026", "An annual conference about the future of technology.", "2026-03-15", "#tech #summit", "College of Engineering", "Academic", resPrefix + R.drawable.banner_tech_summit, "APPROVED"));
        dbHelper.addEvent(new Event("Campus Art Fair", "Featuring artwork from students across all departments.", "2026-03-20", "#art #fair", "Fine Arts Department", "Social", resPrefix + R.drawable.banner_art_fair, "APPROVED"));
        dbHelper.addEvent(new Event("Career Week", "Connect with top employers and find your dream job.", "2026-04-10", "#career #jobs", "Career Services", "Academic", resPrefix + R.drawable.banner_career_week, "APPROVED"));
    }
}
