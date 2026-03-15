package com.example.campus_event_org_hub.ui.main;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.NotifModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NotificationsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rv;
    private NotifAdapter adapter;

    /** Full list loaded from DB (non-archived only). */
    private List<NotifModel> allNotifs;
    /** In-memory visible list — cleared by "Clear All" without touching DB. */
    private List<NotifModel> visibleNotifs;

    private DatabaseHelper db;
    private String studentId;

    private TextView tvUnreadCount;
    private MaterialButton btnClearAll;
    private MaterialButton btnArchiveSelected;
    private View rowArchive;
    private TextView tvArchiveCount;

    /** Whether we are currently in multi-select (archive) mode. */
    private boolean selectionMode = false;
    /** Set of notifIds currently checked by the user. */
    private final Set<Integer> selectedIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        Bundle args = getArguments();
        studentId = args != null ? args.getString("USER_STUDENT_ID", "") : "";

        db = new DatabaseHelper(requireContext());
        allNotifs     = db.getNotificationsForUser(studentId);
        visibleNotifs = new ArrayList<>(allNotifs);

        rv = view.findViewById(R.id.notifications_recycler);
        adapter = new NotifAdapter(visibleNotifs, db, this);
        rv.setAdapter(adapter);

        tvUnreadCount    = view.findViewById(R.id.tv_notif_unread_count);
        btnClearAll      = view.findViewById(R.id.btn_clear_all_notifications);
        btnArchiveSelected = view.findViewById(R.id.btn_archive_selected);
        rowArchive       = view.findViewById(R.id.row_archive);
        tvArchiveCount   = view.findViewById(R.id.tv_archive_count);

        updateUnreadBadge();
        refreshArchiveCount();

        btnClearAll.setOnClickListener(v -> clearAllNotifications());
        btnArchiveSelected.setOnClickListener(v -> confirmArchiveSelected());

        rowArchive.setOnClickListener(v -> openArchiveScreen());

        swipeRefresh = view.findViewById(R.id.swipe_refresh_notifications);
        swipeRefresh.setColorSchemeResources(R.color.primary_blue, R.color.primary_dark);
        swipeRefresh.setOnRefreshListener(() ->
            new Handler(Looper.getMainLooper()).postDelayed(this::reloadNotifications, 600)
        );

        return view;
    }

    // ── Selection mode ────────────────────────────────────────────────────────

    void enterSelectionMode(NotifModel firstItem) {
        selectionMode = true;
        selectedIds.clear();
        selectedIds.add(firstItem.getNotifId());
        adapter.notifyDataSetChanged();
        btnArchiveSelected.setVisibility(View.VISIBLE);
        btnClearAll.setVisibility(View.GONE);
        updateArchiveButtonLabel();
    }

    void exitSelectionMode() {
        selectionMode = false;
        selectedIds.clear();
        adapter.notifyDataSetChanged();
        btnArchiveSelected.setVisibility(View.GONE);
        updateUnreadBadge(); // restores Clear All visibility
    }

    void toggleSelection(int notifId) {
        if (selectedIds.contains(notifId)) {
            selectedIds.remove(notifId);
        } else {
            selectedIds.add(notifId);
        }
        if (selectedIds.isEmpty()) {
            exitSelectionMode();
        } else {
            updateArchiveButtonLabel();
            adapter.notifyDataSetChanged();
        }
    }

    boolean isSelectionMode() { return selectionMode; }
    boolean isSelected(int notifId) { return selectedIds.contains(notifId); }

    private void updateArchiveButtonLabel() {
        if (btnArchiveSelected != null) {
            int n = selectedIds.size();
            btnArchiveSelected.setText("Archive Selected (" + n + ")");
        }
    }

    // ── Archive confirmation ───────────────────────────────────────────────────

    private void confirmArchiveSelected() {
        if (selectedIds.isEmpty()) return;
        int n = selectedIds.size();
        new AlertDialog.Builder(requireContext())
                .setTitle("Archive Notifications")
                .setMessage("Archive " + n + " selected notification" + (n == 1 ? "" : "s") + "?\n\nArchived notifications will be automatically removed after 30 days.")
                .setPositiveButton("Yes, Archive", (dialog, which) -> doArchiveSelected())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doArchiveSelected() {
        List<Integer> ids = new ArrayList<>(selectedIds);
        db.archiveNotifications(ids);
        // Remove from visible list
        visibleNotifs.removeIf(n -> ids.contains(n.getNotifId()));
        allNotifs.removeIf(n -> ids.contains(n.getNotifId()));
        exitSelectionMode();
        adapter.notifyDataSetChanged();
        updateUnreadBadge();
        Toast.makeText(requireContext(),
                ids.size() + " notification" + (ids.size() == 1 ? "" : "s") + " archived.",
                Toast.LENGTH_SHORT).show();
    }

    // ── Reload (swipe-to-refresh) ─────────────────────────────────────────────

    private void reloadNotifications() {
        selectionMode = false;
        selectedIds.clear();
        allNotifs.clear();
        allNotifs.addAll(db.getNotificationsForUser(studentId));
        visibleNotifs.clear();
        visibleNotifs.addAll(allNotifs);
        adapter.notifyDataSetChanged();
        rv.smoothScrollToPosition(0);
        updateUnreadBadge();
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    }

    // ── Clear All (in-memory only, no DB change) ──────────────────────────────

    private void clearAllNotifications() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear Notifications")
                .setMessage("Are you sure that you will clear all your notifications?")
                .setPositiveButton("Yes, Clear All", (dialog, which) -> {
                    visibleNotifs.clear();
                    adapter.notifyDataSetChanged();
                    if (tvUnreadCount    != null) tvUnreadCount.setVisibility(View.GONE);
                    if (btnClearAll      != null) btnClearAll.setVisibility(View.GONE);
                    if (btnArchiveSelected != null) btnArchiveSelected.setVisibility(View.GONE);
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).clearNotificationBadge();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Badge helpers ─────────────────────────────────────────────────────────

    void updateUnreadBadge() {
        int unread = 0;
        for (NotifModel n : visibleNotifs) {
            if (!n.isRead()) unread++;
        }

        if (tvUnreadCount != null) {
            if (unread > 0) {
                tvUnreadCount.setText(unread + " unread");
                tvUnreadCount.setVisibility(View.VISIBLE);
            } else {
                tvUnreadCount.setVisibility(View.GONE);
            }
        }

        if (btnClearAll != null && !selectionMode) {
            btnClearAll.setVisibility(visibleNotifs.isEmpty() ? View.GONE : View.VISIBLE);
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateNotificationBadge();
        }
    }

    // ── Reschedule picker (officer responds to POSTPONED) ─────────────────────

    void openReschedulePicker(NotifModel item, int adapterPos) {
        Calendar cal = Calendar.getInstance();
        String sugDate = item.getSuggestedDate();
        if (sugDate != null && !sugDate.isEmpty()) {
            String[] parts = sugDate.split("-");
            if (parts.length == 3) {
                try {
                    cal.set(Calendar.YEAR,         Integer.parseInt(parts[0]));
                    cal.set(Calendar.MONTH,        Integer.parseInt(parts[1]) - 1);
                    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
                } catch (NumberFormatException ignored) { }
            }
        }
        String sugTime = item.getSuggestedTime();
        if (sugTime != null && !sugTime.isEmpty()) {
            String[] tp = sugTime.split(":");
            try {
                cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tp[0]));
                cal.set(Calendar.MINUTE,      Integer.parseInt(tp[1]));
            } catch (NumberFormatException ignored) { }
        }

        new DatePickerDialog(requireContext(),
                (dpView, year, month, day) -> {
                    String chosenDate = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", year, month + 1, day);
                    new TimePickerDialog(requireContext(),
                            (tpView, hour, minute) -> {
                                String chosenTime = String.format(Locale.getDefault(),
                                        "%02d:%02d", hour, minute);
                                confirmReschedule(item, adapterPos, chosenDate, chosenTime);
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false).show();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void confirmReschedule(NotifModel item, int adapterPos,
                                   String chosenDate, String chosenTime) {
        boolean ok = db.proposeNewDateTimePending(item.getEventId(), chosenDate, chosenTime);
        if (ok) {
            if (!item.isRead()) {
                db.markNotificationRead(item.getNotifId());
                item.setRead(true);
            }
            adapter.notifyItemChanged(adapterPos);
            updateUnreadBadge();
            String timeDisplay = chosenTime;
            try {
                String[] tp = chosenTime.split(":");
                int h = Integer.parseInt(tp[0]);
                int m = Integer.parseInt(tp[1]);
                String ampm = h >= 12 ? "PM" : "AM";
                int h12 = h % 12; if (h12 == 0) h12 = 12;
                timeDisplay = String.format(Locale.getDefault(), "%d:%02d %s", h12, m, ampm);
            } catch (Exception ignored) { }
            Toast.makeText(requireContext(),
                    "Rescheduled to " + chosenDate + " at " + timeDisplay
                            + ". Awaiting admin approval.",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), "Failed to update event.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshArchiveCount();
    }

    private void refreshArchiveCount() {
        if (db == null || tvArchiveCount == null) return;
        int count = db.getArchivedNotificationCount(studentId);
        if (count > 0) {
            tvArchiveCount.setText(String.valueOf(count));
            tvArchiveCount.setVisibility(View.VISIBLE);
        } else {
            tvArchiveCount.setVisibility(View.GONE);
        }
    }

    private void openArchiveScreen() {
        if (!(getActivity() instanceof MainActivity)) return;
        ArchivedNotificationsFragment frag = new ArchivedNotificationsFragment();
        Bundle args = new Bundle();
        args.putString("USER_STUDENT_ID", studentId);
        frag.setArguments(args);
        ((MainActivity) getActivity()).loadFragment(frag, true);
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    static class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        private final List<NotifModel> items;
        private final DatabaseHelper db;
        private final NotificationsFragment fragment;

        NotifAdapter(List<NotifModel> items, DatabaseHelper db, NotificationsFragment fragment) {
            this.items    = items;
            this.db       = db;
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
            boolean inSelection = fragment.isSelectionMode();
            boolean checked = fragment.isSelected(item.getNotifId());

            // ── Checkbox visibility & state ──
            h.checkbox.setVisibility(inSelection ? View.VISIBLE : View.GONE);
            h.checkbox.setChecked(checked);

            // ── Card highlight when selected ──
            h.card.setCardBackgroundColor(
                    checked
                    ? 0xFFE3F2FD   // light blue tint
                    : 0xFFFFFFFF   // plain white
            );

            // Title label
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

            // Unread red dot
            h.unreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

            // Reason
            String reason = item.getReason();
            if (reason != null && !reason.isEmpty()) {
                h.reason.setText("Reason: " + reason);
                h.reason.setVisibility(View.VISIBLE);
            } else {
                h.reason.setVisibility(View.GONE);
            }

            // Suggested date+time (POSTPONED only)
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
                        String timeDisplay = sugTime;
                        try {
                            String[] tp = sugTime.split(":");
                            int hh = Integer.parseInt(tp[0]);
                            int mm = Integer.parseInt(tp[1]);
                            String ampm = hh >= 12 ? "PM" : "AM";
                            int h12 = hh % 12; if (h12 == 0) h12 = 12;
                            timeDisplay = String.format(Locale.getDefault(),
                                    "%d:%02d %s", h12, mm, ampm);
                        } catch (Exception ignored) { }
                        sb.append(timeDisplay);
                    }
                    h.suggestedDate.setText(sb.toString());
                    h.suggestedDate.setVisibility(View.VISIBLE);
                } else {
                    h.suggestedDate.setVisibility(View.GONE);
                }

                // Hide Respond button while in selection mode
                h.btnRespond.setVisibility(inSelection ? View.GONE : View.VISIBLE);
                if (!inSelection) {
                    h.btnRespond.setOnClickListener(v -> {
                        if (!item.isRead()) {
                            db.markNotificationRead(item.getNotifId());
                            item.setRead(true);
                            h.unreadDot.setVisibility(View.GONE);
                            fragment.updateUnreadBadge();
                        }
                        fragment.openReschedulePicker(item, h.getAdapterPosition());
                    });
                }
            } else {
                h.suggestedDate.setVisibility(View.GONE);
                h.btnRespond.setVisibility(View.GONE);
            }

            // ── Mark as read / unread toggle ──
            if (inSelection) {
                h.btnMarkReadToggle.setVisibility(View.GONE);
            } else {
                h.btnMarkReadToggle.setVisibility(View.VISIBLE);
                h.btnMarkReadToggle.setText(item.isRead() ? "Mark as unread" : "Mark as read");
                h.btnMarkReadToggle.setOnClickListener(v -> {
                    if (item.isRead()) {
                        db.markNotificationUnread(item.getNotifId());
                        item.setRead(false);
                        h.unreadDot.setVisibility(View.VISIBLE);
                    } else {
                        db.markNotificationRead(item.getNotifId());
                        item.setRead(true);
                        h.unreadDot.setVisibility(View.GONE);
                    }
                    h.btnMarkReadToggle.setText(item.isRead() ? "Mark as unread" : "Mark as read");
                    fragment.updateUnreadBadge();
                });
            }

            // ── Long-press: enter selection mode ──
            h.itemView.setOnLongClickListener(v -> {
                if (!fragment.isSelectionMode()) {
                    fragment.enterSelectionMode(item);
                }
                return true;
            });

            // ── Tap: toggle checkbox if in selection mode, else mark read ──
            h.itemView.setOnClickListener(v -> {
                if (fragment.isSelectionMode()) {
                    fragment.toggleSelection(item.getNotifId());
                } else {
                    if (!item.isRead()) {
                        db.markNotificationRead(item.getNotifId());
                        item.setRead(true);
                        h.unreadDot.setVisibility(View.GONE);
                        notifyItemChanged(pos);
                        fragment.updateUnreadBadge();
                    }
                }
            });
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            com.google.android.material.card.MaterialCardView card;
            CheckBox checkbox;
            TextView title, message, reason, suggestedDate, time;
            View unreadDot;
            MaterialButton btnRespond;
            MaterialButton btnMarkReadToggle;

            VH(@NonNull View v) {
                super(v);
                card              = v.findViewById(R.id.notif_card);
                checkbox          = v.findViewById(R.id.notif_checkbox);
                title             = v.findViewById(R.id.notif_title);
                message           = v.findViewById(R.id.notif_message);
                reason            = v.findViewById(R.id.notif_reason);
                suggestedDate     = v.findViewById(R.id.notif_suggested_date);
                time              = v.findViewById(R.id.notif_time);
                unreadDot         = v.findViewById(R.id.notif_unread_dot);
                btnRespond        = v.findViewById(R.id.btn_respond_postpone);
                btnMarkReadToggle = v.findViewById(R.id.btn_mark_read_toggle);
            }
        }
    }
}
