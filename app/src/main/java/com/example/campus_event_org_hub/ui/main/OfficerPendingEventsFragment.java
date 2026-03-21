package com.example.campus_event_org_hub.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;

import java.util.List;

public class OfficerPendingEventsFragment extends Fragment {

    private RecyclerView rv;
    private View tvEmpty;
    private String officerSid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_officer_pending_events, container, false);

        Bundle args = getArguments();
        officerSid = args != null ? args.getString("USER_STUDENT_ID", "") : "";

        rv      = view.findViewById(R.id.rv_pending_events);
        tvEmpty = view.findViewById(R.id.tv_pending_events_empty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadPendingEvents();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPendingEvents();
    }

    private void loadPendingEvents() {
        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
        List<Event> pending = db.getEventsByCreatorSid(officerSid);

        // Keep only PENDING status
        pending.removeIf(e -> !"PENDING".equals(e.getStatus()));

        if (pending.isEmpty()) {
            rv.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            rv.setAdapter(new OfficerMyEventsFragment.EventsAdapter(pending, false, false, null));
        }
    }
}
