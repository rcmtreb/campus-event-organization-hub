package com.example.campus_event_org_hub.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.ui.events.EventDetailActivity;
import com.example.campus_event_org_hub.util.ImageUtils;

import java.util.List;

public class ApproveEventsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private RecyclerView rv;
    private TextView tvEmpty;
    private ApproveAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approve_events);

        db = DatabaseHelper.getInstance(this);
        rv = findViewById(R.id.rv_approve_events);
        tvEmpty = findViewById(R.id.tv_approve_empty);
        rv.setLayoutManager(new LinearLayoutManager(this));

        ImageButton btnBack = findViewById(R.id.btn_back_approve);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        loadPendingEvents();
    }

    private void loadPendingEvents() {
        List<Event> pendingList = db.getEventsByStatus("PENDING");
        adapter = new ApproveAdapter(pendingList);
        rv.setAdapter(adapter);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (tvEmpty == null) return;
        boolean empty = adapter == null || adapter.getItemCount() == 0;
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ── Notification helpers ─────────────────────────────────────────────────

    /**
     * Notify the officer who created the event.
     * Uses creator_sid directly; falls back to organizer name lookup for pre-v11 events.
     */
    private void notifyOfficerOfDecision(Event e, boolean approved) {
        String officerSid = e.getCreatorSid();
        if (officerSid == null || officerSid.isEmpty()) {
            officerSid = db.getOfficerStudentIdFromOrganizer(e.getOrganizer());
        }
        if (officerSid == null || officerSid.isEmpty()) return;

        String type;
        String message;
        if (approved) {
            type = "APPROVED";
            message = "Your event \u201c" + e.getTitle() + "\u201d has been approved by the admin "
                    + "and is now live for students to see."
                    + (e.getDate() != null && !e.getDate().isEmpty()
                        ? " It is scheduled for " + e.getDate()
                          + (e.getTime() != null && !e.getTime().isEmpty()
                              ? " at " + e.getTime() : "")
                          + "."
                        : "");
        } else {
            type = "REJECTED";
            message = "Your event \u201c" + e.getTitle() + "\u201d was not approved by the admin. "
                    + "You may review the details and re-submit if needed.";
        }
        db.insertNotification(officerSid, e.getId(), type, message, "", "", "", "");
    }

    /**
     * Notify students whose department matches the event\u2019s audience tags,
     * respecting each student\u2019s notification preference.
     *
     * Preferences:
     *   \u2022 All Events             \u2014 always notify
     *   \u2022 My Department Only     \u2014 notify only if event tags match their department
     *   \u2022 Registered Events Only \u2014 notify only if already registered
     *   \u2022 None                   \u2014 never notify
     */
    private void notifyTargetedStudents(Event e) {
        String tags = e.getTags();
        if (tags == null || tags.isEmpty()) return;

        String timeStr = (e.getTime() != null && !e.getTime().isEmpty())
                ? " at " + e.getTime() : "";
        String message = "\uD83D\uDCE2 New event: \u201c" + e.getTitle() + "\u201d\n"
                + "Date: " + e.getDate() + timeStr + "\n"
                + "Organized by: " + e.getOrganizer() + "\n"
                + "Open the Events tab to view details and register.";

        String[] parts = tags.trim().split("\\s+");
        java.util.Set<String> notifiedSids = new java.util.HashSet<>();

        for (String part : parts) {
            String abbr = part.startsWith("#") ? part.substring(1) : part;
            if (abbr.isEmpty()) continue;

            // getStudentIdsByDeptAbbrWithPref returns student_id + notif_pref pairs
            java.util.List<String[]> students = db.getStudentIdsByDeptAbbrWithPref(abbr);
            for (String[] row : students) {
                String sid  = row[0];
                String pref = row[1] != null ? row[1] : "All Events";
                if (notifiedSids.contains(sid)) continue;

                // Enforce notification preference
                switch (pref) {
                    case "None":
                        continue; // skip entirely
                    case "Registered Events Only":
                        if (!db.isRegistered(sid, e.getId())) continue;
                        break;
                    case "My Department Only":
                        // Already filtered by dept abbr above — always qualifies here
                        break;
                    default:
                        break; // "All Events" — always include
                }

                notifiedSids.add(sid);
                db.insertNotification(sid, e.getId(), "NEW_EVENT", message, "", "", "", "");
            }
        }
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    class ApproveAdapter extends RecyclerView.Adapter<ApproveAdapter.VH> {
        private final List<Event> list;
        ApproveAdapter(List<Event> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.approve_event_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Event e = list.get(pos);
            h.title.setText(e.getTitle());
            h.organizer.setText(e.getOrganizer());

            // Show date and category
            if (h.date != null) {
                String dateText = e.getDate() != null ? e.getDate() : "";
                if (e.getTime() != null && !e.getTime().isEmpty()) dateText += "  " + e.getTime();
                h.date.setText(dateText);
            }
            if (h.category != null) {
                h.category.setText(e.getCategory() != null ? e.getCategory() : "");
                h.category.setVisibility(e.getCategory() != null && !e.getCategory().isEmpty()
                        ? View.VISIBLE : View.GONE);
            }
            if (h.description != null) {
                String desc = e.getDescription() != null ? e.getDescription() : "";
                h.description.setText(desc.length() > 120 ? desc.substring(0, 120) + "..." : desc);
                h.description.setVisibility(desc.isEmpty() ? View.GONE : View.VISIBLE);
            }

            ImageUtils.load(h.img.getContext(), h.img, e.getImagePath(),
                    R.drawable.ic_image_placeholder);

            // Tap card to view full event details
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ApproveEventsActivity.this, EventDetailActivity.class);
                intent.putExtra("event", e);
                intent.putExtra("USER_STUDENT_ID", "admin");
                intent.putExtra("USER_ROLE", "Admin");
                startActivity(intent);
            });

            h.btnApprove.setOnClickListener(v -> {
                int adapterPos = h.getAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION) return;
                h.btnApprove.setEnabled(false);
                h.btnReject.setEnabled(false);
                db.approveEvent(e.getId());
                notifyOfficerOfDecision(e, true);
                notifyTargetedStudents(e);
                list.remove(adapterPos);
                notifyItemRemoved(adapterPos);
                updateEmptyState();
                Toast.makeText(ApproveEventsActivity.this, "Event Approved!", Toast.LENGTH_SHORT).show();
            });

            h.btnReject.setOnClickListener(v -> {
                int adapterPos = h.getAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION) return;
                h.btnApprove.setEnabled(false);
                h.btnReject.setEnabled(false);
                notifyOfficerOfDecision(e, false);
                db.deleteEvent(e.getId());
                list.remove(adapterPos);
                notifyItemRemoved(adapterPos);
                updateEmptyState();
                Toast.makeText(ApproveEventsActivity.this, "Event Rejected", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView title, organizer, date, category, description;
            ImageView img;
            Button btnApprove, btnReject;
            VH(View v) {
                super(v);
                title       = v.findViewById(R.id.approve_event_title);
                organizer   = v.findViewById(R.id.approve_event_organizer);
                date        = v.findViewById(R.id.approve_event_date);
                category    = v.findViewById(R.id.approve_event_category);
                description = v.findViewById(R.id.approve_event_description);
                img         = v.findViewById(R.id.approve_event_image);
                btnApprove  = v.findViewById(R.id.btn_approve_now);
                btnReject   = v.findViewById(R.id.btn_reject_now);
            }
        }
    }
}
