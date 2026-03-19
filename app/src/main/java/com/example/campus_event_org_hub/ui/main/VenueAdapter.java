package com.example.campus_event_org_hub.ui.main;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.model.Event;

import java.util.List;

public class VenueAdapter extends RecyclerView.Adapter<VenueAdapter.VH> {

    private final List<VenueFragment.VenueItem> items;

    public VenueAdapter(List<VenueFragment.VenueItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.venue_list_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        VenueFragment.VenueItem item = items.get(position);

        holder.tvName.setText(item.name);
        holder.tvCapacity.setText("Capacity: " + item.capacity);

        // Availability dot + label
        boolean available = item.isAvailable();
        int dotColor   = available ? Color.parseColor("#388E3C") : Color.parseColor("#D32F2F");
        int labelColor = available ? Color.parseColor("#388E3C") : Color.parseColor("#D32F2F");
        holder.dotView.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(dotColor));
        holder.tvAvailLabel.setText(available ? "Available" : "Booked");
        holder.tvAvailLabel.setTextColor(labelColor);

        // Bookings list
        holder.llSlots.removeAllViews();
        if (item.bookings.isEmpty()) {
            holder.tvNoBookings.setVisibility(View.VISIBLE);
        } else {
            holder.tvNoBookings.setVisibility(View.GONE);
            for (Event e : item.bookings) {
                TextView tv = new TextView(holder.itemView.getContext());
                String time = (e.getTime() != null && !e.getTime().isEmpty())
                        ? e.getTime() : "—";
                tv.setText("• " + time + "  –  " + e.getTitle());
                tv.setTextSize(13f);
                tv.setTextColor(holder.itemView.getContext()
                        .getResources().getColor(R.color.text_primary, null));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = (int) (4 * holder.itemView.getContext()
                        .getResources().getDisplayMetrics().density);
                tv.setLayoutParams(lp);
                holder.llSlots.addView(tv);
            }
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvCapacity, tvAvailLabel, tvNoBookings;
        View dotView;
        LinearLayout llSlots;

        VH(View v) {
            super(v);
            tvName       = v.findViewById(R.id.tv_venue_name);
            tvCapacity   = v.findViewById(R.id.tv_venue_capacity);
            tvAvailLabel = v.findViewById(R.id.tv_availability_label);
            tvNoBookings = v.findViewById(R.id.tv_no_bookings);
            dotView      = v.findViewById(R.id.view_availability_dot);
            llSlots      = v.findViewById(R.id.ll_booking_slots);
        }
    }
}
