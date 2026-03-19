package com.example.campus_event_org_hub.ui.events;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private EditText searchView;
    private ChipGroup chipGroup;
    private DatabaseHelper dbHelper;

    /** Optional dept abbreviation filter passed from DiscoverFragment quick-filter chips. */
    private String filterDept = "";

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

            // Read optional dept filter from args
            if (getArguments() != null) {
                String d = getArguments().getString("FILTER_DEPT", "");
                filterDept = (d != null) ? d : "";
            }

            recyclerView = view.findViewById(R.id.events_recycler_view);
            swipeRefresh = view.findViewById(R.id.swipe_refresh_events);
            searchView   = view.findViewById(R.id.search_view);
            chipGroup    = view.findViewById(R.id.chip_group_filter);

            // Set layout manager programmatically to prevent XML inflation crashes
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

            // Brand the refresh spinner with the app's primary colour
            swipeRefresh.setColorSchemeResources(R.color.primary_green, R.color.primary_dark);

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
            fab.setOnClickListener(v -> {
                if (getActivity() instanceof com.example.campus_event_org_hub.ui.main.MainActivity) {
                    ((com.example.campus_event_org_hub.ui.main.MainActivity) getActivity())
                            .loadFragment(new CreateEventFragment(), true);
                } else {
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new CreateEventFragment())
                            .addToBackStack(null)
                            .commit();
                }
            });

            // ── Search ───────────────────────────────────────────────────────
            if (searchView != null) {
                searchView.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(Editable s) {
                        if (adapter != null) adapter.getFilter().filter(s.toString());
                    }
                });
                searchView.setOnEditorActionListener((v, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        if (adapter != null) adapter.getFilter().filter(searchView.getText().toString());
                        return true;
                    }
                    return false;
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

    /** Load events from DB, optionally filtered by department abbreviation. */
    private void loadEvents() {
        try {
            eventList = dbHelper.getAllEvents(filterDept);
            if (eventList == null) eventList = new ArrayList<>();

            String sid = getArguments() != null
                    ? getArguments().getString("USER_STUDENT_ID", "") : "";
            String role = getArguments() != null
                    ? getArguments().getString("USER_ROLE", "Student") : "Student";
            adapter = new EventAdapter(eventList, sid, role, dbHelper);
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
            List<Event> fresh = dbHelper.getAllEvents(filterDept);
            if (fresh == null) fresh = new ArrayList<>();
            eventList.clear();
            eventList.addAll(fresh);
            adapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(0);
        } catch (Exception e) {
            Log.e(TAG, "Error in reloadEvents", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) reloadEvents();
    }
}
