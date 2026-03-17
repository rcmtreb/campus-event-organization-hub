package com.example.campus_event_org_hub.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.example.campus_event_org_hub.util.ImageUtils;

import java.util.List;

public class ApproveEventsActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private RecyclerView rv;
    private ApproveAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approve_events);

        db = new DatabaseHelper(this);
        rv = findViewById(R.id.rv_approve_events);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadPendingEvents();
    }

    private void loadPendingEvents() {
        List<Event> pendingList = db.getEventsByStatus("PENDING");
        adapter = new ApproveAdapter(pendingList);
        rv.setAdapter(adapter);
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
            ImageUtils.load(h.img.getContext(), h.img, e.getImagePath(),
                    R.drawable.ic_image_placeholder);

            h.btnApprove.setOnClickListener(v -> {
                int adapterPos = h.getAdapterPosition();
                if (adapterPos == RecyclerView.NO_ID) return;
                // Disable both buttons immediately to prevent double-tap double-send
                h.btnApprove.setEnabled(false);
                h.btnReject.setEnabled(false);
                db.approveEvent(e.getId());
                notifyOfficerOfDecision(e, true);   // tell the officer it was approved
                notifyTargetedStudents(e);           // tell relevant students a new event is live
                list.remove(adapterPos);
                notifyItemRemoved(adapterPos);
                Toast.makeText(ApproveEventsActivity.this, "Event Approved!", Toast.LENGTH_SHORT).show();
            });

            h.btnReject.setOnClickListener(v -> {
                int adapterPos = h.getAdapterPosition();
                if (adapterPos == RecyclerView.NO_ID) return;
                // Disable both buttons immediately to prevent double-tap double-send
                h.btnApprove.setEnabled(false);
                h.btnReject.setEnabled(false);
                notifyOfficerOfDecision(e, false);  // tell the officer it was rejected
                db.deleteEvent(e.getId());
                list.remove(adapterPos);
                notifyItemRemoved(adapterPos);
                Toast.makeText(ApproveEventsActivity.this, "Event Rejected", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView title, organizer;
            ImageView img;
            Button btnApprove, btnReject;
            VH(View v) {
                super(v);
                title      = v.findViewById(R.id.approve_event_title);
                organizer  = v.findViewById(R.id.approve_event_organizer);
                img        = v.findViewById(R.id.approve_event_image);
                btnApprove = v.findViewById(R.id.btn_approve_now);
                btnReject  = v.findViewById(R.id.btn_reject_now);
            }
        }
    }
}
