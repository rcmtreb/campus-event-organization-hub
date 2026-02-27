package com.example.campus_event_org_hub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> eventList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);

        recyclerView = view.findViewById(R.id.events_recycler_view);

        // Create sample data
        eventList = new ArrayList<>();
        eventList.add(new Event("Tech Summit 2026", "An annual conference about the future of technology.", "2026-03-15", "#tech #summit #students", "College of Engineering", R.drawable.ic_image_placeholder));
        eventList.add(new Event("Campus Art Fair", "Featuring artwork from students across all departments.", "2026-03-20", "#art #fair #creative", "Fine Arts Department", R.drawable.ic_image_placeholder));
        eventList.add(new Event("Career Week", "Connect with top employers and find your dream job.", "2026-04-10", "#career #jobs #networking", "Career Services", R.drawable.ic_image_placeholder));
        eventList.add(new Event("Music Festival", "A weekend of live music from local and student bands.", "2026-04-25", "#music #festival #live", "Student Government", R.drawable.ic_image_placeholder));


        adapter = new EventAdapter(eventList);
        recyclerView.setAdapter(adapter);

        return view;
    }
}

