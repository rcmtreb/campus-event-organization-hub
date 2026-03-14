package com.example.campus_event_org_hub.ui.main;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.NotifModel;

import java.util.List;

public class NotificationsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rv;
    private NotifAdapter adapter;
    private List<NotifModel> notifList;
    private DatabaseHelper db;
    private String studentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        Bundle args = getArguments();
        studentId = args != null ? args.getString("USER_STUDENT_ID", "") : "";

        db = new DatabaseHelper(requireContext());
        notifList = db.getNotificationsForUser(studentId);

        rv = view.findViewById(R.id.notifications_recycler);
        adapter = new NotifAdapter(notifList, db);
        rv.setAdapter(adapter);

        swipeRefresh = view.findViewById(R.id.swipe_refresh_notifications);
        swipeRefresh.setColorSchemeResources(R.color.primary_blue, R.color.primary_dark);
        swipeRefresh.setOnRefreshListener(() ->
            new Handler(Looper.getMainLooper()).postDelayed(this::reloadNotifications, 600)
        );

        return view;
    }

    private void reloadNotifications() {
        notifList.clear();
        notifList.addAll(db.getNotificationsForUser(studentId));
        adapter.notifyDataSetChanged();
        rv.smoothScrollToPosition(0);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    static class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        private final List<NotifModel> items;
        private final DatabaseHelper db;

        NotifAdapter(List<NotifModel> items, DatabaseHelper db) {
            this.items = items;
            this.db = db;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.notification_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            NotifModel item = items.get(pos);

            // Title = type label
            String typeLabel;
            switch (item.getType() != null ? item.getType() : "") {
                case "POSTPONED":  typeLabel = "Event Postponed";  break;
                case "CANCELLED":  typeLabel = "Event Cancelled";  break;
                case "APPROVED":   typeLabel = "Event Approved";   break;
                case "REJECTED":   typeLabel = "Event Rejected";   break;
                case "NEW_EVENT":  typeLabel = "New Event";        break;
                default:           typeLabel = item.getType() != null ? item.getType() : "Notification";
            }
            h.title.setText(typeLabel);
            h.message.setText(item.getMessage());
            h.time.setText(item.getCreatedAt());
            h.unreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

            // Reason
            String reason = item.getReason();
            if (reason != null && !reason.isEmpty()) {
                h.reason.setText("Reason: " + reason);
                h.reason.setVisibility(View.VISIBLE);
            } else {
                h.reason.setVisibility(View.GONE);
            }

            // Suggested date (only for postponed)
            String suggestedDate = item.getSuggestedDate();
            if ("POSTPONED".equals(item.getType()) && suggestedDate != null && !suggestedDate.isEmpty()) {
                h.suggestedDate.setText("Suggested new date: " + suggestedDate);
                h.suggestedDate.setVisibility(View.VISIBLE);
            } else {
                h.suggestedDate.setVisibility(View.GONE);
            }

            // Mark read on tap
            h.itemView.setOnClickListener(v -> {
                if (!item.isRead()) {
                    db.markNotificationRead(item.getNotifId());
                    item.setRead(true);
                    h.unreadDot.setVisibility(View.GONE);
                }
            });
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, message, reason, suggestedDate, time;
            View unreadDot;
            VH(@NonNull View v) {
                super(v);
                title         = v.findViewById(R.id.notif_title);
                message       = v.findViewById(R.id.notif_message);
                reason        = v.findViewById(R.id.notif_reason);
                suggestedDate = v.findViewById(R.id.notif_suggested_date);
                time          = v.findViewById(R.id.notif_time);
                unreadDot     = v.findViewById(R.id.notif_unread_dot);
            }
        }
    }
}
