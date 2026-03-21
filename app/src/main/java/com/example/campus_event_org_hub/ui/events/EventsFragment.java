package com.example.campus_event_org_hub.ui.events;

import android.app.Activity;
import android.content.Intent;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventsFragment extends Fragment {

    private static final String TAG = "CEOH_EVENTS";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private EventAdapter adapter;
    private List<Event> eventList;
    private EditText searchView;
    private ChipGroup chipGroup;
    private DatabaseHelper dbHelper;
    private ActivityResultLauncher<Intent> eventDetailLauncher;

    private String filterDept = "";
    private String currentCategory = "All";
    private int scrollPosition = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        eventDetailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    boolean changed = result.getData().getBooleanExtra("registration_changed", false);
                    if (changed) {
                        reloadEvents();
                    }
                }
            });
    }

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
            dbHelper = DatabaseHelper.getInstance(requireContext());

            if (getArguments() != null) {
                String d = getArguments().getString("FILTER_DEPT", "");
                filterDept = (d != null) ? d : "";
            }

            recyclerView = view.findViewById(R.id.events_recycler_view);
            swipeRefresh = view.findViewById(R.id.swipe_refresh_events);
            searchView   = view.findViewById(R.id.search_view);
            chipGroup    = view.findViewById(R.id.chip_group_filter);

            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            swipeRefresh.setColorSchemeResources(R.color.primary_green, R.color.primary_dark);

            loadEvents();
            setupListeners(view);

        } catch (Exception e) {
            Log.e(TAG, "CRITICAL CRASH in EventsFragment", e);
            Toast.makeText(getContext(), "Error loading events: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        return view;
    }

    private void setupListeners(View view) {
        swipeRefresh.setOnRefreshListener(() ->
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                reloadEvents();
                swipeRefresh.setRefreshing(false);
            }, 600)
        );

        FloatingActionButton fab = view.findViewById(R.id.fab_create_event);
        String role = getArguments() != null
                ? getArguments().getString("USER_ROLE", "Student") : "Student";
        fab.setVisibility("Student".equalsIgnoreCase(role) ? View.GONE : View.VISIBLE);
        fab.setOnClickListener(v -> {
            saveScrollPosition();
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

        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty() && adapter != null) {
                    Chip chip = group.findViewById(checkedIds.get(0));
                    if (chip != null) {
                        currentCategory = chip.getText().toString();
                        adapter.filterByCategory(currentCategory);
                    }
                }
            });
        }
    }

    private void saveScrollPosition() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            scrollPosition = layoutManager.findFirstVisibleItemPosition();
        }
    }

    private void restoreScrollPosition() {
        recyclerView.post(() -> {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (layoutManager != null && scrollPosition > 0) {
                layoutManager.scrollToPositionWithOffset(scrollPosition, 0);
            }
        });
    }

    private void loadEvents() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());

            List<Event> allEvents = dbHelper.getAllEvents(filterDept);
            if (allEvents == null) allEvents = new ArrayList<>();

            eventList = new ArrayList<>();
            for (Event e : allEvents) {
                String eventDate = e.getDate();
                if (eventDate != null && eventDate.compareTo(today) >= 0) {
                    eventList.add(e);
                }
            }

            String sid = getArguments() != null
                    ? getArguments().getString("USER_STUDENT_ID", "") : "";
            String role = getArguments() != null
                    ? getArguments().getString("USER_ROLE", "Student") : "Student";
            adapter = new EventAdapter(eventList, sid, role, dbHelper, eventDetailLauncher);
            recyclerView.setAdapter(adapter);

            restoreScrollPosition();
        } catch (Exception e) {
            Log.e(TAG, "Error in loadEvents", e);
            eventList = new ArrayList<>();
            adapter = new EventAdapter(eventList);
            recyclerView.setAdapter(adapter);
        }
    }

    private void reloadEvents() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());

            List<Event> fresh = dbHelper.getAllEvents(filterDept);
            if (fresh == null) fresh = new ArrayList<>();

            List<Event> upcoming = new ArrayList<>();
            for (Event e : fresh) {
                String eventDate = e.getDate();
                if (eventDate != null && eventDate.compareTo(today) >= 0) {
                    upcoming.add(e);
                }
            }
            adapter.updateFullList(upcoming, currentCategory);
            recyclerView.smoothScrollToPosition(0);
        } catch (Exception e) {
            Log.e(TAG, "Error in reloadEvents", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        restoreScrollPosition();
    }
}
