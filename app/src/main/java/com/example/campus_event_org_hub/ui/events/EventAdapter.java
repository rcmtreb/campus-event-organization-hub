package com.example.campus_event_org_hub.ui.events;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.util.ImageUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> implements Filterable {

    private List<Event> eventList;
    private List<Event> eventListFull;
    private String studentId;

    public EventAdapter(List<Event> eventList) {
        this.eventList = eventList;
        this.eventListFull = new ArrayList<>(eventList);
        this.studentId = "";
    }

    public EventAdapter(List<Event> eventList, String studentId) {
        this.eventList = eventList;
        this.eventListFull = new ArrayList<>(eventList);
        this.studentId = studentId != null ? studentId : "";
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_list_item, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.title.setText(event.getTitle());
        holder.date.setText(event.getDate());
        holder.description.setText(event.getDescription());
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
                holder.statusBadge.setBackgroundColor(0xFFD32F2F); // error red
            } else if ("POSTPONED".equals(status)) {
                holder.statusBadge.setVisibility(View.VISIBLE);
                holder.statusBadge.setText("POSTPONED");
                holder.statusBadge.setBackgroundColor(0xFF7B1FA2); // purple
            } else {
                holder.statusBadge.setVisibility(View.GONE);
            }
        }

        // Timeline badge (top row, beside date): UPCOMING / HAPPENING / ENDED / POSTPONED / CANCELLED
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
                // Compute UPCOMING / HAPPENING / ENDED from date
                Calendar todayCal = Calendar.getInstance();
                todayCal.set(Calendar.HOUR_OF_DAY, 0);
                todayCal.set(Calendar.MINUTE, 0);
                todayCal.set(Calendar.SECOND, 0);
                todayCal.set(Calendar.MILLISECOND, 0);
                try {
                    Date eventDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .parse(event.getDate());
                    if (eventDate == null) {
                        holder.timelineBadge.setVisibility(View.GONE);
                    } else if (eventDate.before(todayCal.getTime())) {
                        holder.timelineBadge.setText("ENDED");
                        holder.timelineBadge.setBackgroundColor(0xFF757575); // gray
                        holder.timelineBadge.setVisibility(View.VISIBLE);
                    } else if (eventDate.equals(todayCal.getTime())) {
                        holder.timelineBadge.setText("HAPPENING");
                        holder.timelineBadge.setBackgroundColor(0xFF0288D1); // light blue
                        holder.timelineBadge.setVisibility(View.VISIBLE);
                    } else {
                        holder.timelineBadge.setText("UPCOMING");
                        holder.timelineBadge.setBackgroundColor(0xFF388E3C); // green
                        holder.timelineBadge.setVisibility(View.VISIBLE);
                    }
                } catch (ParseException ex) {
                    holder.timelineBadge.setVisibility(View.GONE);
                }
            } else {
                holder.timelineBadge.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EventDetailActivity.class);
            intent.putExtra("event", event);
            intent.putExtra("USER_STUDENT_ID", studentId);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    @Override
    public Filter getFilter() {
        return eventFilter;
    }

    private Filter eventFilter = new Filter() {
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
                        item.getCategory().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            eventList.clear();
            eventList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    public void filterByCategory(String category) {
        List<Event> filteredList = new ArrayList<>();
        if (category.equalsIgnoreCase("All")) {
            filteredList.addAll(eventListFull);
        } else {
            for (Event item : eventListFull) {
                if (item.getCategory().equalsIgnoreCase(category)) {
                    filteredList.add(item);
                }
            }
        }
        eventList.clear();
        eventList.addAll(filteredList);
        notifyDataSetChanged();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, description, category, statusBadge, timelineBadge;
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
        }
    }
}
