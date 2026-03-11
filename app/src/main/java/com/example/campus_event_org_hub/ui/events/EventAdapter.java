package com.example.campus_event_org_hub.ui.events;

import android.content.Intent;
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> implements Filterable {

    private List<Event> eventList;
    private List<Event> eventListFull;

    public EventAdapter(List<Event> eventList) {
        this.eventList = eventList;
        this.eventListFull = new ArrayList<>(eventList);
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

        if (event.getImagePath() != null && !event.getImagePath().isEmpty()) {
            try {
                holder.image.setImageURI(Uri.parse(event.getImagePath()));
            } catch (Exception e) {
                holder.image.setImageResource(R.drawable.ic_image_placeholder);
            }
        } else {
            holder.image.setImageResource(R.drawable.ic_image_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), EventDetailActivity.class);
            intent.putExtra("event", event);
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
        TextView title, date, description;
        ImageView image;
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.event_title);
            date = itemView.findViewById(R.id.event_date);
            description = itemView.findViewById(R.id.event_description);
            image = itemView.findViewById(R.id.event_image);
        }
    }
}
