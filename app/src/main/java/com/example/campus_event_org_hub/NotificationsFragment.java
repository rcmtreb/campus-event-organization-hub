package com.example.campus_event_org_hub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        List<NotifItem> items = new ArrayList<>();
        items.add(new NotifItem("Registration Confirmed",
                "You have successfully registered for Tech Summit 2026.",
                "Just now", true));
        items.add(new NotifItem("Event Reminder",
                "Campus Art Fair starts tomorrow at 9:00 AM. Don't miss it!",
                "2 hours ago", true));
        items.add(new NotifItem("New Event Added",
                "Android Workshop has been added to the events list.",
                "Yesterday", false));
        items.add(new NotifItem("Seats Filling Up",
                "Only 10 seats left for Career Week. Register now!",
                "2 days ago", false));
        items.add(new NotifItem("Event Updated",
                "Music Festival venue has been changed to the Main Auditorium.",
                "3 days ago", false));
        items.add(new NotifItem("Bookmark Reminder",
                "Basketball Finals you bookmarked is in 3 days.",
                "4 days ago", false));

        RecyclerView rv = view.findViewById(R.id.notifications_recycler);
        rv.setAdapter(new NotifAdapter(items));
        return view;
    }

    // --- simple data class ---
    static class NotifItem {
        final String title, message, time;
        final boolean unread;
        NotifItem(String title, String message, String time, boolean unread) {
            this.title = title; this.message = message;
            this.time = time;   this.unread = unread;
        }
    }

    // --- inline adapter ---
    static class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        private final List<NotifItem> items;
        NotifAdapter(List<NotifItem> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.notification_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            NotifItem item = items.get(pos);
            h.title.setText(item.title);
            h.message.setText(item.message);
            h.time.setText(item.time);
            h.unreadDot.setVisibility(item.unread ? View.VISIBLE : View.GONE);
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, message, time;
            View unreadDot;
            VH(@NonNull View v) {
                super(v);
                title     = v.findViewById(R.id.notif_title);
                message   = v.findViewById(R.id.notif_message);
                time      = v.findViewById(R.id.notif_time);
                unreadDot = v.findViewById(R.id.notif_unread_dot);
            }
        }
    }
}

