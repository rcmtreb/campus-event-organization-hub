package com.example.campus_event_org_hub.ui.main;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import androidx.core.content.ContextCompat;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.FirestoreHelper;
import com.example.campus_event_org_hub.data.SyncManager;
import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.model.NotifModel;
import com.example.campus_event_org_hub.ui.events.EventDetailActivity;
import com.example.campus_event_org_hub.util.ServerTimeUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

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

    private MaterialButton btnArchiveSelected;

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

        db = DatabaseHelper.getInstance(requireContext());
        allNotifs     = db.getNotificationsForUser(studentId);
        visibleNotifs = new ArrayList<>(allNotifs);

        rv = view.findViewById(R.id.notifications_recycler);
        adapter = new NotifAdapter(visibleNotifs, db, this);
        rv.setAdapter(adapter);

        btnArchiveSelected = view.findViewById(R.id.btn_archive_selected);

        // Header badge and Clear All are rendered inside the RecyclerView header row.
        updateUnreadBadge();

        btnArchiveSelected.setOnClickListener(v -> confirmArchiveSelected());

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
        adapter.collapseAll();
        btnArchiveSelected.setVisibility(View.VISIBLE);
        updateArchiveButtonLabel();
        // Refresh header row so Clear All hides while in selection mode
        adapter.notifyItemChanged(0);
    }

    void exitSelectionMode() {
        selectionMode = false;
        selectedIds.clear();
        adapter.collapseAll();
        btnArchiveSelected.setVisibility(View.GONE);
        updateUnreadBadge();
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
        db.markAllNotificationsRead(studentId);
        allNotifs.clear();
        allNotifs.addAll(db.getNotificationsForUser(studentId));
        visibleNotifs.clear();
        visibleNotifs.addAll(allNotifs);
        adapter.collapseAll();
        rv.smoothScrollToPosition(0);
        updateUnreadBadge();
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    }

    // ── Clear All (in-memory only, no DB change) ──────────────────────────────

    void clearAllNotifications() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear Notifications")
                .setMessage("Are you sure that you will clear all your notifications?")
                .setPositiveButton("Yes, Clear All", (dialog, which) -> {
                    visibleNotifs.clear();
                    adapter.notifyDataSetChanged();
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
        // Refresh the header row (position 0) which renders the unread badge and Clear All button
        if (adapter != null) {
            adapter.notifyItemChanged(0);
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

            // Notify admin that the officer proposed a new date for review
            Bundle args = getArguments();
            String officerName = args != null ? args.getString("USER_NAME", "An officer") : "An officer";
            String adminMsg = "\uD83D\uDD04 An officer has proposed a new schedule for a postponed event.\n"
                    + "Proposed by: " + officerName + "\n"
                    + "New date: " + chosenDate + " at " + timeDisplay + "\n"
                    + "Open Pending Approvals to review and approve.";
            db.insertNotification("admin", item.getEventId(), "RESUBMIT", adminMsg,
                    "", chosenDate, chosenTime, "");

            Toast.makeText(requireContext(),
                    "Rescheduled to " + chosenDate + " at " + timeDisplay
                            + ". Awaiting admin approval.",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), "Failed to update event.", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    static class NotifAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_ITEM   = 1;

        private final List<NotifModel> items;
        private final DatabaseHelper db;
        private final NotificationsFragment fragment;
        private final Set<Integer> expandedIds = new HashSet<>();

        NotifAdapter(List<NotifModel> items, DatabaseHelper db, NotificationsFragment fragment) {
            this.items    = items;
            this.db       = db;
            this.fragment = fragment;
        }

        void collapseAll() {
            expandedIds.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_HEADER) {
                View v = inflater.inflate(R.layout.notification_list_header, parent, false);
                return new HeaderVH(v);
            }
            View v = inflater.inflate(R.layout.notification_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            if (pos == 0) {
                bindHeader((HeaderVH) holder);
            } else {
                bindItem((VH) holder, pos - 1);
            }
        }

        // ── Header binding ────────────────────────────────────────────────────

        private void bindHeader(HeaderVH h) {
            boolean inSelection = fragment.isSelectionMode();

            // Unread count badge
            int unread = 0;
            for (NotifModel n : items) { if (!n.isRead()) unread++; }
            if (unread > 0) {
                h.tvUnreadCount.setText(unread + " unread");
                h.tvUnreadCount.setVisibility(View.VISIBLE);
            } else {
                h.tvUnreadCount.setVisibility(View.GONE);
            }

            // Clear All button — visible only when not in selection mode and list is non-empty
            if (!inSelection && !items.isEmpty()) {
                h.btnClearAll.setVisibility(View.VISIBLE);
                h.btnClearAll.setOnClickListener(v -> fragment.clearAllNotifications());
            } else {
                h.btnClearAll.setVisibility(View.GONE);
            }
        }

        // ── Item binding ──────────────────────────────────────────────────────

        private void bindItem(@NonNull VH h, int itemPos) {
            NotifModel item = items.get(itemPos);
            // The adapter position in the RecyclerView is itemPos + 1 (header at 0)
            int adapterPos = itemPos + 1;

            boolean inSelection = fragment.isSelectionMode();
            boolean checked = fragment.isSelected(item.getNotifId());
            boolean isExpanded = expandedIds.contains(item.getNotifId());

            // ── Summary Section ──

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

            // Message preview (1 line)
            String fullMessage = item.getMessage();
            h.messagePreview.setText(fullMessage);

            // Time - show relative time
            h.time.setText(getRelativeTime(item.getCreatedAt()));

            // Unread dot
            h.unreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

            // Expand indicator rotation
            h.expandIndicator.setRotation(isExpanded ? 180f : 0f);

            // ── Details Section (expanded) ──
            h.detailsContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            // Full message
            h.message.setText(fullMessage);

            // Reason
            String reason = item.getReason();
            if (reason != null && !reason.isEmpty()) {
                h.reason.setText(reason);
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
                    StringBuilder sb = new StringBuilder();
                    if (hasDate) sb.append(formatDate(sugDate));
                    if (hasDate && hasTime) sb.append(" at ");
                    if (hasTime) sb.append(formatTime(sugTime));
                    h.suggestedDate.setText(sb.toString());
                    h.suggestedDate.setVisibility(View.VISIBLE);
                } else {
                    h.suggestedDate.setVisibility(View.GONE);
                }
                h.btnRespond.setVisibility(inSelection ? View.GONE : View.VISIBLE);
                if (!inSelection) {
                    h.btnRespond.setOnClickListener(v -> {
                        if (!item.isRead()) {
                            db.markNotificationRead(item.getNotifId());
                            item.setRead(true);
                            h.unreadDot.setVisibility(View.GONE);
                            fragment.updateUnreadBadge();
                        }
                        fragment.openReschedulePicker(item, adapterPos);
                    });
                }
            } else {
                h.suggestedDate.setVisibility(View.GONE);
                h.btnRespond.setVisibility(View.GONE);
            }

            // View Event button — for NEW_EVENT and APPROVED types (only if event hasn't ended)
            boolean isEnded = false;
            Event ev = db.getEventById(item.getEventId());
            if (ev != null) {
                String status = ev.getStatus();
                if ("ENDED".equals(status) || "CANCELLED".equals(status)) {
                    isEnded = true;
                } else {
                    String today = ServerTimeUtil.todayString();
                    if (ev.getDate() != null && ev.getDate().compareTo(today) < 0) {
                        isEnded = true;
                    }
                }
            }
            if (("NEW_EVENT".equals(item.getType()) || "APPROVED".equals(item.getType())) && !inSelection && !isEnded) {
                h.btnViewEvent.setVisibility(View.VISIBLE);
                h.btnViewEvent.setOnClickListener(v -> {
                    // Mark read before navigating
                    if (!item.isRead()) {
                        db.markNotificationRead(item.getNotifId());
                        item.setRead(true);
                        h.unreadDot.setVisibility(View.GONE);
                        fragment.updateUnreadBadge();
                    }
                    // Fetch this event from Firestore and update local SQLite, then open it
                    android.content.Context viewContext = h.itemView.getContext();
                    new Thread(() -> {
                        new FirestoreHelper().fetchEventById(item.getEventId(), viewContext);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Event event = db.getEventById(item.getEventId());
                            if (event == null) {
                                Toast.makeText(viewContext,
                                        "Event not found.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Bundle args = fragment.getArguments();
                            String studentId = args != null ? args.getString("USER_STUDENT_ID", "") : "";
                            String userRole  = args != null ? args.getString("USER_ROLE", "") : "";
                            Intent intent = new Intent(viewContext, EventDetailActivity.class);
                            intent.putExtra("event", event);
                            intent.putExtra("USER_STUDENT_ID", studentId);
                            intent.putExtra("USER_ROLE", userRole);
                            viewContext.startActivity(intent);
                        });
                    }).start();
                });
            } else {
                h.btnViewEvent.setVisibility(View.GONE);
            }

            // Mark as read/unread button
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

            // ── Selection Mode (checkbox) ──
            h.checkbox.setVisibility(inSelection ? View.VISIBLE : View.GONE);
            h.checkbox.setChecked(checked);
            h.card.setCardBackgroundColor(
                    checked
                    ? ContextCompat.getColor(h.itemView.getContext(), R.color.notif_selection_tint)
                    : ContextCompat.getColor(h.itemView.getContext(), R.color.surface)
            );

            // ── Click Handlers ──

            // Long-press: enter selection mode
            h.itemView.setOnLongClickListener(v -> {
                if (!fragment.isSelectionMode()) {
                    fragment.enterSelectionMode(item);
                }
                return true;
            });

            // Tap on summary: expand/collapse or selection
            h.summaryContainer.setOnClickListener(v -> {
                if (inSelection) {
                    fragment.toggleSelection(item.getNotifId());
                } else {
                    if (!item.isRead()) {
                        db.markNotificationRead(item.getNotifId());
                        item.setRead(true);
                        h.unreadDot.setVisibility(View.GONE);
                        fragment.updateUnreadBadge();
                    }
                    if (expandedIds.contains(item.getNotifId())) {
                        expandedIds.remove(item.getNotifId());
                    } else {
                        expandedIds.add(item.getNotifId());
                    }
                    notifyItemChanged(adapterPos);
                }
            });
        }

        private String getRelativeTime(String createdAt) {
            if (createdAt == null || createdAt.isEmpty()) return "";
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                java.util.Date date = sdf.parse(createdAt);
                if (date == null) return createdAt;
                // Use server-corrected time to prevent device clock manipulation
                long diff = ServerTimeUtil.nowMillis() - date.getTime();
                long minutes = diff / (60 * 1000);
                long hours = minutes / 60;
                long days = hours / 24;
                if (minutes < 1) return "Just now";
                if (minutes < 60) return minutes + "m ago";
                if (hours < 24) return hours + "h ago";
                if (days < 7) return days + "d ago";
                return createdAt.substring(0, Math.min(10, createdAt.length()));
            } catch (Exception e) {
                return createdAt;
            }
        }

        private String formatDate(String date) {
            if (date == null || date.isEmpty()) return "";
            try {
                String[] parts = date.split("-");
                if (parts.length == 3) {
                    int month = Integer.parseInt(parts[1]);
                    int day = Integer.parseInt(parts[2]);
                    String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
                    return months[month - 1] + " " + day;
                }
            } catch (Exception ignored) {}
            return date;
        }

        private String formatTime(String time) {
            if (time == null || time.isEmpty()) return "";
            try {
                String[] tp = time.split(":");
                int h = Integer.parseInt(tp[0]);
                int m = Integer.parseInt(tp[1]);
                String ampm = h >= 12 ? "PM" : "AM";
                int h12 = h % 12; if (h12 == 0) h12 = 12;
                return String.format(Locale.getDefault(), "%d:%02d %s", h12, m, ampm);
            } catch (Exception ignored) {}
            return time;
        }

        // items.size() + 1 for the header row at position 0
        @Override public int getItemCount() { return items.size() + 1; }

        // ── Header ViewHolder ─────────────────────────────────────────────────

        static class HeaderVH extends RecyclerView.ViewHolder {
            TextView tvUnreadCount;
            com.google.android.material.button.MaterialButton btnClearAll;

            HeaderVH(@NonNull View v) {
                super(v);
                tvUnreadCount = v.findViewById(R.id.tv_notif_unread_count);
                btnClearAll   = v.findViewById(R.id.btn_clear_all_notifications);
            }
        }

        // ── Item ViewHolder ───────────────────────────────────────────────────

        static class VH extends RecyclerView.ViewHolder {
            MaterialCardView card;
            LinearLayout summaryContainer;
            LinearLayout detailsContainer;
            CheckBox checkbox;
            TextView title, messagePreview, message, reason, suggestedDate, time;
            ImageView expandIndicator;
            View unreadDot;
            MaterialButton btnRespond;
            MaterialButton btnMarkReadToggle;
            MaterialButton btnViewEvent;

            VH(@NonNull View v) {
                super(v);
                card              = v.findViewById(R.id.notif_card);
                summaryContainer  = v.findViewById(R.id.summary_container);
                detailsContainer  = v.findViewById(R.id.details_container);
                checkbox          = v.findViewById(R.id.notif_checkbox);
                title             = v.findViewById(R.id.notif_title);
                messagePreview    = v.findViewById(R.id.notif_message_preview);
                message           = v.findViewById(R.id.notif_message);
                reason            = v.findViewById(R.id.notif_reason);
                suggestedDate     = v.findViewById(R.id.notif_suggested_date);
                time              = v.findViewById(R.id.notif_time);
                expandIndicator   = v.findViewById(R.id.expand_indicator);
                unreadDot         = v.findViewById(R.id.notif_unread_dot);
                btnRespond        = v.findViewById(R.id.btn_respond_postpone);
                btnMarkReadToggle = v.findViewById(R.id.btn_mark_read_toggle);
                btnViewEvent      = v.findViewById(R.id.btn_view_event);
            }
        }
    }
}
