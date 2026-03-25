package com.example.campus_event_org_hub.ui.events;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.util.ImageUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.campus_event_org_hub.util.ServerTimeUtil;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> implements Filterable {

    private List<Event> eventList;
    private List<Event> eventListFull;
    private String studentId;
    private String userRole;
    private DatabaseHelper dbHelper;
    private ActivityResultLauncher<Intent> eventDetailLauncher;

    // ── Constructors ─────────────────────────────────────────────────────────

    public EventAdapter(List<Event> eventList) {
        this.eventList     = eventList;
        this.eventListFull = new ArrayList<>(eventList);
        this.studentId     = "";
        this.userRole      = "Student";
        this.dbHelper      = null;
        this.eventDetailLauncher = null;
    }

    public EventAdapter(List<Event> eventList, String studentId) {
        this.eventList     = eventList;
        this.eventListFull = new ArrayList<>(eventList);
        this.studentId     = studentId != null ? studentId : "";
        this.userRole      = "Student";
        this.dbHelper      = null;
        this.eventDetailLauncher = null;
    }

    /** Full constructor used by EventsFragment — enables long-press hide for Students. */
    public EventAdapter(List<Event> eventList, String studentId, String userRole,
                        DatabaseHelper dbHelper) {
        this.eventList     = eventList;
        this.eventListFull = new ArrayList<>(eventList);
        this.studentId     = studentId != null ? studentId : "";
        this.userRole      = userRole  != null ? userRole  : "Student";
        this.dbHelper      = dbHelper;
        this.eventDetailLauncher = null;
    }

    /** Constructor with ActivityResultLauncher for returning data from EventDetailActivity. */
    public EventAdapter(List<Event> eventList, String studentId, String userRole,
                        DatabaseHelper dbHelper, ActivityResultLauncher<Intent> launcher) {
        this.eventList     = eventList;
        this.eventListFull = new ArrayList<>(eventList);
        this.studentId     = studentId != null ? studentId : "";
        this.userRole      = userRole  != null ? userRole  : "Student";
        this.dbHelper      = dbHelper;
        this.eventDetailLauncher = launcher;
        setHasStableIds(true);
    }

    // ── ViewHolder lifecycle ─────────────────────────────────────────────────

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_list_item, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.title.setText(event.getTitle());
        holder.date.setText(event.getDate());
        holder.description.setText(event.getDescription());
        if (holder.startTime != null) {
            holder.startTime.setText(event.getStartTime() != null ? event.getStartTime() : "");
        }
        if (holder.endTime != null) {
            holder.endTime.setText(event.getEndTime() != null ? event.getEndTime() : "");
        }
        if (holder.category != null) {
            holder.category.setText(event.getCategory() != null ? event.getCategory() : "");
        }

        // Use the category-mapped 9-patch banner as the fallback when no image is uploaded
        int fallbackBanner = ImageUtils.getDefaultBannerForCategory(event.getCategory());
        ImageUtils.load(holder.itemView.getContext(), holder.image,
                event.getImagePath(), fallbackBanner);

        // Status badge for CANCELLED / POSTPONED (below description)
        if (holder.statusBadge != null) {
            String status = event.getStatus();
            if ("CANCELLED".equals(status)) {
                holder.statusBadge.setVisibility(View.VISIBLE);
                holder.statusBadge.setText("CANCELLED");
                holder.statusBadge.setBackgroundColor(0xFFD32F2F);
            } else if ("POSTPONED".equals(status)) {
                holder.statusBadge.setVisibility(View.VISIBLE);
                holder.statusBadge.setText("POSTPONED");
                holder.statusBadge.setBackgroundColor(0xFF7B1FA2);
            } else {
                holder.statusBadge.setVisibility(View.GONE);
            }
        }

        // Timeline badge: UPCOMING / HAPPENING / ENDED / POSTPONED / CANCELLED
        if (holder.timelineBadge != null) {
            String status = event.getStatus();
            if ("CANCELLED".equals(status)) {
                holder.timelineBadge.setText("CANCELLED");
                holder.timelineBadge.setBackgroundColor(0xFFD32F2F);
                holder.timelineBadge.setVisibility(View.VISIBLE);
            } else if ("POSTPONED".equals(status)) {
                holder.timelineBadge.setText("POSTPONED");
                holder.timelineBadge.setBackgroundColor(0xFF7B1FA2);
                holder.timelineBadge.setVisibility(View.VISIBLE);
            } else if ("APPROVED".equals(status)) {
                String today = ServerTimeUtil.todayString();
                String eventDateStr = event.getDate();

                if (eventDateStr == null || eventDateStr.isEmpty()) {
                    holder.timelineBadge.setVisibility(View.GONE);
                } else if (eventDateStr.compareTo(today) == 0) {
                    String endTime = event.getEndTime();
                    boolean hasEnded = false;
                    if (endTime != null && !endTime.isEmpty()) {
                        try {
                            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            Date now = ServerTimeUtil.now();
                            String currentTimeStr = timeFormat.format(now);
                            Date currentTime = timeFormat.parse(currentTimeStr);
                            Date endTimeDate = timeFormat.parse(endTime);
                            if (currentTime != null && endTimeDate != null && endTimeDate.before(currentTime)) {
                                hasEnded = true;
                            }
                        } catch (ParseException ignored) { }
                    }
                    if (hasEnded) {
                        holder.timelineBadge.setText("ENDED");
                        holder.timelineBadge.setBackgroundColor(0xFF757575);
                    } else {
                        holder.timelineBadge.setText("HAPPENING");
                        holder.timelineBadge.setBackgroundColor(0xFF0288D1);
                    }
                    holder.timelineBadge.setVisibility(View.VISIBLE);
                } else {
                    holder.timelineBadge.setText("UPCOMING");
                    holder.timelineBadge.setBackgroundColor(0xFF388E3C);
                    holder.timelineBadge.setVisibility(View.VISIBLE);
                }
            } else {
                holder.timelineBadge.setVisibility(View.GONE);
            }
        }

        // ── Tap: open detail ────────────────────────────────────────────────
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EventDetailActivity.class);
            intent.putExtra("event", event);
            intent.putExtra("USER_STUDENT_ID", studentId);
            intent.putExtra("USER_ROLE", userRole);

            // Use launcher if available, otherwise fall back to startActivity
            if (eventDetailLauncher != null) {
                eventDetailLauncher.launch(intent);
            } else {
                v.getContext().startActivity(intent);
            }
        });

        // ── Long-press: Students can hide an event from their feed ───────────
        holder.itemView.setOnLongClickListener(v -> {
            if (!"Student".equalsIgnoreCase(userRole) || dbHelper == null) return false;
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Hide event?")
                    .setMessage("\"" + event.getTitle() + "\" will be removed from your feed.")
                    .setPositiveButton("Hide", (dialog, which) -> {
                        dbHelper.hideEvent(event.getId());
                        int idx = holder.getAdapterPosition();
                        if (idx != RecyclerView.NO_ID) {
                            eventList.remove(idx);
                            eventListFull.remove(event);
                            notifyItemRemoved(idx);
                        }
                        Toast.makeText(v.getContext(), "Event hidden", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override
    public long getItemId(int position) {
        return eventList.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    // ── Filtering ────────────────────────────────────────────────────────────

    @Override
    public Filter getFilter() {
        return eventFilter;
    }

    private final Filter eventFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Event> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(eventListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Event item : eventListFull) {
                    if (item.getTitle().toLowerCase().contains(filterPattern) ||
                        item.getDescription().toLowerCase().contains(filterPattern) ||
                        (item.getCategory() != null &&
                         item.getCategory().toLowerCase().contains(filterPattern))) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            eventList.clear();
            eventList.addAll((List<Event>) results.values);
            notifyDataSetChanged();
        }
    };

    /** Replaces the full backing list (used after a data refresh) then re-applies the active filter. */
    public void updateFullList(List<Event> freshList, String activeCategory) {
        eventListFull.clear();
        eventListFull.addAll(freshList);
        filterByCategory(activeCategory != null ? activeCategory : "All");
    }

    public void filterByCategory(String category) {
        List<Event> filteredList = new ArrayList<>();
        if (category.equalsIgnoreCase("All")) {
            filteredList.addAll(eventListFull);
        } else {
            for (Event item : eventListFull) {
                if (item.getCategory() != null &&
                    item.getCategory().equalsIgnoreCase(category)) {
                    filteredList.add(item);
                }
            }
        }
        eventList.clear();
        eventList.addAll(filteredList);
        notifyDataSetChanged();
    }

    // ── ViewHolder ───────────────────────────────────────────────────────────

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, description, category, statusBadge, timelineBadge, startTime, endTime;
        ImageView image;
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            title         = itemView.findViewById(R.id.event_title);
            date          = itemView.findViewById(R.id.event_date);
            description   = itemView.findViewById(R.id.event_description);
            category      = itemView.findViewById(R.id.event_category);
            image         = itemView.findViewById(R.id.event_image);
            statusBadge   = itemView.findViewById(R.id.tv_event_status_badge);
            timelineBadge = itemView.findViewById(R.id.tv_timeline_badge);
            startTime     = itemView.findViewById(R.id.event_start_time);
            endTime       = itemView.findViewById(R.id.event_end_time);
        }
    }
}
