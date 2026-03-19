package com.example.campus_event_org_hub.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class VenueFragment extends Fragment {

    // Fixed campus venue list: name, capacity
    private static final String[][] VENUES = {
            {"Gymnasium",          "800"},
            {"Auditorium",         "500"},
            {"Function Hall",      "200"},
            {"Basketball Court",   "300"},
            {"Open Grounds",       "1000"},
            {"Conference Room A",  "50"},
            {"Conference Room B",  "50"},
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_venue, container, false);

        DatabaseHelper dbHelper = new DatabaseHelper(requireContext());

        // Show today's date in header
        TextView tvDate = view.findViewById(R.id.tv_venue_date);
        String today = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        tvDate.setText(today);

        String todayDb = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        // Build venue data with today's bookings
        List<VenueItem> items = new ArrayList<>();
        for (String[] v : VENUES) {
            String name     = v[0];
            String capacity = v[1];
            // Query events that mention this venue in tags (e.g. #GYM) or title
            List<Event> bookings = dbHelper.getEventsByVenueAndDate(name, todayDb);
            items.add(new VenueItem(name, capacity, bookings));
        }

        RecyclerView rv = view.findViewById(R.id.rv_venues);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new VenueAdapter(items));

        return view;
    }

    // ── Data model ────────────────────────────────────────────────────────

    public static class VenueItem {
        public final String name;
        public final String capacity;
        public final List<Event> bookings;

        public VenueItem(String name, String capacity, List<Event> bookings) {
            this.name     = name;
            this.capacity = capacity;
            this.bookings = bookings != null ? bookings : new ArrayList<>();
        }

        public boolean isAvailable() { return bookings.isEmpty(); }
    }
}
