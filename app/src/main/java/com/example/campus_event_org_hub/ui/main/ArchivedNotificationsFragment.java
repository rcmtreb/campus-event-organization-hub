package com.example.campus_event_org_hub.ui.main;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.NotifModel;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class ArchivedNotificationsFragment extends Fragment {

    private DatabaseHelper db;
    private String studentId;
    private List<NotifModel> archivedList;
    private ArchiveAdapter adapter;
    private View emptyState;
    private RecyclerView recyclerView;
    private TextView tvArchiveCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_archived_notifications, container, false);

        Bundle args = getArguments();
        studentId = args != null ? args.getString("USER_STUDENT_ID", "") : "";

        db = DatabaseHelper.getInstance(requireContext());
        archivedList = db.getArchivedNotificationsForUser(studentId);

        recyclerView  = view.findViewById(R.id.archive_recycler);
        emptyState    = view.findViewById(R.id.archive_empty_state);
        tvArchiveCount = view.findViewById(R.id.tv_archive_count);

        adapter = new ArchiveAdapter(archivedList, this);
        recyclerView.setAdapter(adapter);

        updateVisibility();
        return view;
    }

    void updateVisibility() {
        boolean empty = archivedList.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (tvArchiveCount != null) {
            int n = archivedList.size();
            if (n > 0) {
                tvArchiveCount.setText(n + " item" + (n == 1 ? "" : "s"));
                tvArchiveCount.setVisibility(View.VISIBLE);
            } else {
                tvArchiveCount.setVisibility(View.GONE);
            }
        }
    }

    void onUnarchive(NotifModel item, int pos) {
        db.unarchiveNotification(item.getNotifId());
        archivedList.remove(pos);
        adapter.notifyItemRemoved(pos);
        adapter.notifyItemRangeChanged(pos, archivedList.size());
        updateVisibility();
        Toast.makeText(requireContext(), "Moved back to notifications.", Toast.LENGTH_SHORT).show();
    }

    void onDelete(NotifModel item, int pos) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Notification")
                .setMessage("Permanently delete this notification? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    db.deleteNotification(item.getNotifId());
                    archivedList.remove(pos);
                    adapter.notifyItemRemoved(pos);
                    adapter.notifyItemRangeChanged(pos, archivedList.size());
                    updateVisibility();
                    Toast.makeText(requireContext(), "Notification deleted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    static class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.VH> {

        private final List<NotifModel> items;
        private final ArchivedNotificationsFragment fragment;

        ArchiveAdapter(List<NotifModel> items, ArchivedNotificationsFragment fragment) {
            this.items    = items;
            this.fragment = fragment;
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

            // Title
            String typeLabel;
            switch (item.getType() != null ? item.getType() : "") {
                case "POSTPONED": typeLabel = "Event Postponed"; break;
                case "CANCELLED": typeLabel = "Event Cancelled"; break;
                case "APPROVED":  typeLabel = "Event Approved";  break;
                case "REJECTED":  typeLabel = "Event Rejected";  break;
                case "NEW_EVENT": typeLabel = "New Event";       break;
                default: typeLabel = item.getType() != null ? item.getType() : "Notification";
            }
            h.title.setText(typeLabel);
            h.message.setText(item.getMessage());

            // Archived-at timestamp as the time row
            String archivedAt = item.getArchivedAt();
            h.time.setText(archivedAt != null && !archivedAt.isEmpty()
                    ? "Archived: " + archivedAt
                    : item.getCreatedAt());

            // No unread dot in archive
            h.unreadDot.setVisibility(View.GONE);
            // No checkbox in archive
            h.checkbox.setVisibility(View.GONE);
            // No respond button in archive
            h.btnRespond.setVisibility(View.GONE);

            // Reason
            String reason = item.getReason();
            if (reason != null && !reason.isEmpty()) {
                h.reason.setText("Reason: " + reason);
                h.reason.setVisibility(View.VISIBLE);
            } else {
                h.reason.setVisibility(View.GONE);
            }

            // Suggested date/time (POSTPONED)
            if ("POSTPONED".equals(item.getType())) {
                String sugDate = item.getSuggestedDate();
                String sugTime = item.getSuggestedTime();
                boolean hasDate = sugDate != null && !sugDate.isEmpty();
                boolean hasTime = sugTime != null && !sugTime.isEmpty();
                if (hasDate || hasTime) {
                    StringBuilder sb = new StringBuilder("Suggested schedule: ");
                    if (hasDate) sb.append(sugDate);
                    if (hasDate && hasTime) sb.append(" at ");
                    if (hasTime) {
                        String td = sugTime;
                        try {
                            String[] tp = sugTime.split(":");
                            int hh = Integer.parseInt(tp[0]);
                            int mm = Integer.parseInt(tp[1]);
                            String ampm = hh >= 12 ? "PM" : "AM";
                            int h12 = hh % 12; if (h12 == 0) h12 = 12;
                            td = String.format(Locale.getDefault(), "%d:%02d %s", h12, mm, ampm);
                        } catch (Exception ignored) {}
                        sb.append(td);
                    }
                    h.suggestedDate.setText(sb.toString());
                    h.suggestedDate.setVisibility(View.VISIBLE);
                } else {
                    h.suggestedDate.setVisibility(View.GONE);
                }
            } else {
                h.suggestedDate.setVisibility(View.GONE);
            }

            // Card: slightly tinted to distinguish from regular notifications
            h.card.setCardBackgroundColor(0xFFF3E5F5); // soft lavender

            // Tap → options dialog: Unarchive or Delete
            h.itemView.setOnClickListener(v -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Archived Notification")
                        .setItems(new String[]{"Move back to Notifications", "Delete permanently"},
                                (dialog, which) -> {
                                    int currentPos = h.getAdapterPosition();
                                    if (currentPos == RecyclerView.NO_ID) return;
                                    if (which == 0) {
                                        fragment.onUnarchive(item, currentPos);
                                    } else {
                                        fragment.onDelete(item, currentPos);
                                    }
                                })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            com.google.android.material.card.MaterialCardView card;
            android.widget.CheckBox checkbox;
            TextView title, message, reason, suggestedDate, time;
            View unreadDot;
            MaterialButton btnRespond;

            VH(@NonNull View v) {
                super(v);
                card          = v.findViewById(R.id.notif_card);
                checkbox      = v.findViewById(R.id.notif_checkbox);
                title         = v.findViewById(R.id.notif_title);
                message       = v.findViewById(R.id.notif_message);
                reason        = v.findViewById(R.id.notif_reason);
                suggestedDate = v.findViewById(R.id.notif_suggested_date);
                time          = v.findViewById(R.id.notif_time);
                unreadDot     = v.findViewById(R.id.notif_unread_dot);
                btnRespond    = v.findViewById(R.id.btn_respond_postpone);
            }
        }
    }
}
