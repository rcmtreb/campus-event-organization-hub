package com.example.campus_event_org_hub.ui.admin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminEventControlActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private List<Event> events;
    private EventControlAdapter adapter;

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Reload the list after an edit
                    events.clear();
                    events.addAll(db.getAllEventsForAdmin());
                    adapter.notifyDataSetChanged();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_control);

        db = new DatabaseHelper(this);

        ImageButton btnBack = findViewById(R.id.btn_back_event_control);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_event_control);
        rv.setLayoutManager(new LinearLayoutManager(this));

        events = db.getAllEventsForAdmin();
        adapter = new EventControlAdapter();
        rv.setAdapter(adapter);
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    class EventControlAdapter extends RecyclerView.Adapter<EventControlAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.admin_event_control_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Event e = events.get(position);
            h.tvTitle.setText(e.getTitle());
            h.tvDate.setText(e.getDate() + (e.getTime().isEmpty() ? "" : "  " + e.getTime()));
            h.tvStatus.setText(e.getStatus());

            // Status badge colour
            switch (e.getStatus()) {
                case "APPROVED":
                    h.tvStatus.setBackgroundResource(android.R.color.holo_green_dark); break;
                case "PENDING":
                    h.tvStatus.setBackgroundResource(android.R.color.holo_orange_light); break;
                case "CANCELLED":
                    h.tvStatus.setBackgroundResource(android.R.color.holo_red_light); break;
                case "POSTPONED":
                    h.tvStatus.setBackgroundResource(android.R.color.holo_purple); break;
                default:
                    h.tvStatus.setBackgroundResource(R.color.primary_blue);
            }

            // Timeline badge: UPCOMING / ENDED / POSTPONED (based on date + status)
            String status = e.getStatus();
            if ("POSTPONED".equals(status)) {
                h.tvTimelineBadge.setText("POSTPONED");
                h.tvTimelineBadge.setBackgroundResource(android.R.color.holo_purple);
                h.tvTimelineBadge.setVisibility(View.VISIBLE);
            } else if ("APPROVED".equals(status)) {
                boolean isEnded = false;
                try {
                    Date eventDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .parse(e.getDate());
                    // Strip time from today so we compare date only
                    Calendar todayCal = Calendar.getInstance();
                    todayCal.set(Calendar.HOUR_OF_DAY, 0);
                    todayCal.set(Calendar.MINUTE, 0);
                    todayCal.set(Calendar.SECOND, 0);
                    todayCal.set(Calendar.MILLISECOND, 0);
                    if (eventDate != null && eventDate.before(todayCal.getTime())) {
                        isEnded = true;
                    }
                } catch (ParseException ignored) { }
                if (isEnded) {
                    h.tvTimelineBadge.setText("ENDED");
                    h.tvTimelineBadge.setBackgroundResource(android.R.color.darker_gray);
                } else {
                    h.tvTimelineBadge.setText("UPCOMING");
                    h.tvTimelineBadge.setBackgroundResource(android.R.color.holo_green_dark);
                }
                h.tvTimelineBadge.setVisibility(View.VISIBLE);
            } else {
                // PENDING, CANCELLED, or unknown — hide the timeline badge
                h.tvTimelineBadge.setVisibility(View.GONE);
            }

            h.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(AdminEventControlActivity.this,
                        AdminEditEventActivity.class);
                intent.putExtra("EVENT_ID",          e.getId());
                intent.putExtra("EVENT_TITLE",       e.getTitle());
                intent.putExtra("EVENT_DESC",        e.getDescription());
                intent.putExtra("EVENT_DATE",        e.getDate());
                intent.putExtra("EVENT_TIME",        e.getTime());
                intent.putExtra("EVENT_TAGS",        e.getTags());
                intent.putExtra("EVENT_ORGANIZER",   e.getOrganizer());
                intent.putExtra("EVENT_CATEGORY",    e.getCategory());
                editLauncher.launch(intent);
            });
            h.btnDelete.setOnClickListener(v -> confirmDelete(e, position));
            h.btnCancel.setOnClickListener(v -> showCancelDialog(e, position));
            h.btnPostpone.setOnClickListener(v -> showPostponeDialog(e, position));
        }

        @Override public int getItemCount() { return events.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate, tvStatus, tvTimelineBadge;
            MaterialButton btnEdit, btnDelete, btnCancel, btnPostpone;
            VH(@NonNull View v) {
                super(v);
                tvTitle         = v.findViewById(R.id.tv_control_title);
                tvDate          = v.findViewById(R.id.tv_control_date);
                tvStatus        = v.findViewById(R.id.tv_control_status);
                tvTimelineBadge = v.findViewById(R.id.tv_timeline_badge);
                btnEdit         = v.findViewById(R.id.btn_edit_event);
                btnDelete       = v.findViewById(R.id.btn_delete_event);
                btnCancel       = v.findViewById(R.id.btn_cancel_event);
                btnPostpone     = v.findViewById(R.id.btn_postpone_event);
            }
        }
    }

    // ── Dialog helpers ───────────────────────────────────────────────────────

    private void confirmDelete(Event e, int pos) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Permanently delete \"" + e.getTitle() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    db.deleteEvent(e.getId());
                    events.remove(pos);
                    adapter.notifyItemRemoved(pos);
                    Toast.makeText(this, "Event deleted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCancelDialog(Event e, int pos) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reason, null);
        EditText etReason = dialogView.findViewById(R.id.et_reason);
        EditText etInstructions = dialogView.findViewById(R.id.et_instructions);

        new AlertDialog.Builder(this)
                .setTitle("Cancel Event")
                .setView(dialogView)
                .setPositiveButton("Confirm Cancel", (d, w) -> {
                    String reason = etReason.getText().toString().trim();
                    String instructions = etInstructions.getText().toString().trim();
                    db.cancelEvent(e.getId());
                    e.setStatus("CANCELLED");
                    notifyOfficer(e, "CANCELLED", reason, "", "", instructions);
                    adapter.notifyItemChanged(pos);
                    Toast.makeText(this, "Event cancelled.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Back", null)
                .show();
    }

    private void showPostponeDialog(Event e, int pos) {
        Calendar cal = Calendar.getInstance();
        // Pre-fill the picker with the event's current date using direct field parsing
        // to avoid timezone shifts that occur when using Date/setTime()
        String[] parts = e.getDate().split("-");
        if (parts.length == 3) {
            try {
                cal.set(Calendar.YEAR,         Integer.parseInt(parts[0]));
                cal.set(Calendar.MONTH,        Integer.parseInt(parts[1]) - 1); // 0-based
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            } catch (NumberFormatException ignored) { }
        }

        DatePickerDialog dpd = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    String suggestedDate = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", year, month + 1, day);
                    // After picking date, prompt for a suggested time
                    Calendar timeCal = Calendar.getInstance();
                    TimePickerDialog tpd = new TimePickerDialog(this,
                            (tv, hourOfDay, minute) -> {
                                String suggestedTime = String.format(Locale.getDefault(),
                                        "%02d:%02d", hourOfDay, minute);
                                showPostponeReasonDialog(e, pos, suggestedDate, suggestedTime);
                            },
                            timeCal.get(Calendar.HOUR_OF_DAY),
                            timeCal.get(Calendar.MINUTE),
                            false /* 12-hour clock */);
                    tpd.setTitle("Select Suggested New Time");
                    tpd.show();
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dpd.setTitle("Select Suggested New Date");
        dpd.show();
    }

    private void showPostponeReasonDialog(Event e, int pos, String suggestedDate, String suggestedTime) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reason, null);
        EditText etReason = dialogView.findViewById(R.id.et_reason);
        EditText etInstructions = dialogView.findViewById(R.id.et_instructions);

        // Format time to 12-hour display for the message
        String timeDisplay = suggestedTime;
        try {
            String[] tp = suggestedTime.split(":");
            int h = Integer.parseInt(tp[0]);
            int m = Integer.parseInt(tp[1]);
            String ampm = h >= 12 ? "PM" : "AM";
            int h12 = h % 12; if (h12 == 0) h12 = 12;
            timeDisplay = String.format(Locale.getDefault(), "%d:%02d %s", h12, m, ampm);
        } catch (Exception ignored) { }

        new AlertDialog.Builder(this)
                .setTitle("Postpone Event")
                .setMessage("Suggested new date: " + suggestedDate + "\nSuggested new time: " + timeDisplay)
                .setView(dialogView)
                .setPositiveButton("Confirm Postpone", (d, w) -> {
                    String reason = etReason.getText().toString().trim();
                    String instructions = etInstructions.getText().toString().trim();
                    db.postponeEvent(e.getId());
                    e.setStatus("POSTPONED");
                    notifyOfficer(e, "POSTPONED", reason, suggestedDate, suggestedTime, instructions);
                    adapter.notifyItemChanged(pos);
                    Toast.makeText(this, "Event postponed.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Back", null)
                .show();
    }

    /**
     * Inserts a notification for the officer who created this event.
     * Uses the stored creator_sid directly — no fuzzy name lookup needed.
     * Falls back to the old organizer-name lookup for events created before v11.
     */
    private void notifyOfficer(Event e, String type, String reason,
                                String suggestedDate, String suggestedTime,
                                String instructions) {
        String officerSid = e.getCreatorSid();
        if (officerSid == null || officerSid.isEmpty()) {
            // Fallback for pre-v11 events that don't have a stored creator_sid
            officerSid = db.getOfficerStudentIdFromOrganizer(e.getOrganizer());
        }
        if (officerSid == null || officerSid.isEmpty()) {
            Toast.makeText(this,
                    "Could not find the event creator — notification not sent.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String message;
        if ("POSTPONED".equals(type)) {
            String timeDisplay = (suggestedTime != null && !suggestedTime.isEmpty())
                    ? " at " + suggestedTime : "";
            message = "\u23F3 Your event \u201c" + e.getTitle() + "\u201d has been postponed by the admin.\n"
                    + "Suggested new schedule: " + suggestedDate + timeDisplay + ".\n"
                    + (!reason.isEmpty() ? "Reason: " + reason + "\n" : "")
                    + (!instructions.isEmpty() ? "Instructions: " + instructions + "\n" : "")
                    + "Open your Notifications to confirm or propose a different date.";
        } else {
            message = "\u274C Your event \u201c" + e.getTitle() + "\u201d has been cancelled by the admin.\n"
                    + (!reason.isEmpty() ? "Reason: " + reason + "\n" : "")
                    + (!instructions.isEmpty() ? "Instructions: " + instructions : "");
        }

        long id = db.insertNotification(officerSid, e.getId(), type, message,
                reason, suggestedDate, suggestedTime, instructions);
        if (id == -1) {
            Toast.makeText(this, "Warning: notification could not be saved.", Toast.LENGTH_SHORT).show();
        }
    }
}
