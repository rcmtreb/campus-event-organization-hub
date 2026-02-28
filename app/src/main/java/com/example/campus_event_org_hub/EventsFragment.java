package com.example.campus_event_org_hub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> eventList;
    private SearchView searchView;
    private ChipGroup chipGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);

        recyclerView = view.findViewById(R.id.events_recycler_view);
        searchView = view.findViewById(R.id.search_view);
        chipGroup = view.findViewById(R.id.chip_group_filter);

        // Create sample data with categories
        eventList = new ArrayList<>();
        eventList.add(new Event("Tech Summit 2026", "An annual conference about the future of technology.", "2026-03-15", "#tech #summit #students", "College of Engineering", "Academic", R.drawable.ic_image_placeholder));
        eventList.add(new Event("Campus Art Fair", "Featuring artwork from students across all departments.", "2026-03-20", "#art #fair #creative", "Fine Arts Department", "Social", R.drawable.ic_image_placeholder));
        eventList.add(new Event("Career Week", "Connect with top employers and find your dream job.", "2026-04-10", "#career #jobs #networking", "Career Services", "Academic", R.drawable.ic_image_placeholder));
        eventList.add(new Event("Music Festival", "A weekend of live music from local and student bands.", "2026-04-25", "#music #festival #live", "Student Government", "Social", R.drawable.ic_image_placeholder));
        eventList.add(new Event("Android Workshop", "Learn to build apps with modern tools.", "2026-05-05", "#android #coding #workshop", "Google Developer Group", "Workshop", R.drawable.ic_image_placeholder));
        eventList.add(new Event("Basketball Finals", "Come and cheer for your favorite college team.", "2026-05-12", "#sports #basketball #finals", "Sports Council", "Sports", R.drawable.ic_image_placeholder));

        adapter = new EventAdapter(eventList);
        recyclerView.setAdapter(adapter);

        // Setup Search
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });

        // Setup Chip Filtering
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
}
