package com.example.campus_event_org_hub.ui.events;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class EventsFragment extends Fragment {

    private static final String TAG = "CEOH_EVENTS";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private EventAdapter adapter;
    private List<Event> eventList;
    private SearchView searchView;
    private ChipGroup chipGroup;
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view;
        try {
            view = inflater.inflate(R.layout.fragment_events, container, false);
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL: failed to inflate fragment_events layout", e);
            return new android.widget.FrameLayout(requireContext());
        }

        try {
            dbHelper = new DatabaseHelper(requireContext());

            recyclerView = view.findViewById(R.id.events_recycler_view);
            swipeRefresh = view.findViewById(R.id.swipe_refresh_events);
            searchView   = view.findViewById(R.id.search_view);
            chipGroup    = view.findViewById(R.id.chip_group_filter);

            // Set layout manager programmatically to prevent XML inflation crashes
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

            // Brand the refresh spinner with the app's primary colour
            swipeRefresh.setColorSchemeResources(R.color.primary_blue, R.color.primary_dark);

            loadEvents();

            // ── Pull-to-refresh ──────────────────────────────────────────────
            swipeRefresh.setOnRefreshListener(() ->
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    reloadEvents();
                    swipeRefresh.setRefreshing(false);
                }, 600)
            );

            // ── FAB (Officers only) ──────────────────────────────────────────
            FloatingActionButton fab = view.findViewById(R.id.fab_create_event);
            String role = getArguments() != null
                    ? getArguments().getString("USER_ROLE", "Student") : "Student";
            fab.setVisibility("Student".equalsIgnoreCase(role) ? View.GONE : View.VISIBLE);
            fab.setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CreateEventFragment())
                    .addToBackStack(null)
                    .commit());

            // ── Search ───────────────────────────────────────────────────────
            if (searchView != null) {
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override public boolean onQueryTextSubmit(String q) { return false; }
                    @Override public boolean onQueryTextChange(String newText) {
                        if (adapter != null) adapter.getFilter().filter(newText);
                        return false;
                    }
                });
            }

            // ── Category chips ───────────────────────────────────────────────
            if (chipGroup != null) {
                chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                    if (!checkedIds.isEmpty() && adapter != null) {
                        Chip chip = group.findViewById(checkedIds.get(0));
                        if (chip != null) adapter.filterByCategory(chip.getText().toString());
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "CRITICAL CRASH in EventsFragment", e);
            Toast.makeText(getContext(), "Error loading events: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        return view;
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    /** Load events from DB. */
    private void loadEvents() {
        try {
            eventList = dbHelper.getAllEvents();
            if (eventList == null) eventList = new ArrayList<>();

            String sid = getArguments() != null
                    ? getArguments().getString("USER_STUDENT_ID", "") : "";
            adapter = new EventAdapter(eventList, sid);
            recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error in loadEvents", e);
            eventList = new ArrayList<>();
            adapter = new EventAdapter(eventList);
            recyclerView.setAdapter(adapter);
        }
    }

    /**
     * Refresh pass — re-queries the DB, updates the adapter in place, then
     * scrolls smoothly back to the top so the user can see the updated list.
     */
    private void reloadEvents() {
        try {
            List<Event> fresh = dbHelper.getAllEvents();
            if (fresh == null) fresh = new ArrayList<>();
            eventList.clear();
            eventList.addAll(fresh);
            adapter.notifyDataSetChanged();
            // Scroll back to the top with a smooth animation
            recyclerView.smoothScrollToPosition(0);
        } catch (Exception e) {
            Log.e(TAG, "Error in reloadEvents", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Keep list fresh when user navigates back (e.g. after admin approved/cancelled events)
        if (adapter != null) reloadEvents();
    }
}
