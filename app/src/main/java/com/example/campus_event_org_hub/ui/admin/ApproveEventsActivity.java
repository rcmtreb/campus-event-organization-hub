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
        if (officerSid == null || officerSid.isEmpty()) return; // silently skip — no officer found

        String type    = approved ? "APPROVED" : "REJECTED";
        String message = approved
                ? "Your event \"" + e.getTitle() + "\" has been approved and is now live!"
                : "Your event \"" + e.getTitle() + "\" was rejected by the admin.";
        db.insertNotification(officerSid, e.getId(), type, message, "", "", "", "");
    }

    /**
     * Notify students whose department matches the event's audience tags.
     * Tags are stored as "#CBA #CCS" or "#ALL". Each tag maps to the department
     * abbreviation used in the users table (e.g. "CBA" matches "College of Business and Accountancy (CBA)").
     */
    private void notifyTargetedStudents(Event e) {
        String tags = e.getTags();
        if (tags == null || tags.isEmpty()) return;

        String message = "New event for your department: \"" + e.getTitle() +
                "\" on " + e.getDate() +
                (e.getTime() != null && !e.getTime().isEmpty() ? " at " + e.getTime() : "") + ".";

        String[] parts = tags.trim().split("\\s+");
        java.util.Set<String> notifiedSids = new java.util.HashSet<>();

        for (String part : parts) {
            // Strip leading "#"
            String abbr = part.startsWith("#") ? part.substring(1) : part;
            if (abbr.isEmpty()) continue;

            List<String> studentIds = db.getStudentIdsByDeptAbbr(abbr);
            for (String sid : studentIds) {
                if (notifiedSids.contains(sid)) continue; // don't double-notify
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
