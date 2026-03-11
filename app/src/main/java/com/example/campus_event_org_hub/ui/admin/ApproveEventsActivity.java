package com.example.campus_event_org_hub.ui.admin;

import android.net.Uri;
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

    class ApproveAdapter extends RecyclerView.Adapter<ApproveAdapter.VH> {
        private final List<Event> list;
        ApproveAdapter(List<Event> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.approve_event_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Event e = list.get(pos);
            h.title.setText(e.getTitle());
            h.organizer.setText(e.getOrganizer());
            if (e.getImagePath() != null && !e.getImagePath().isEmpty()) {
                h.img.setImageURI(Uri.parse(e.getImagePath()));
            }
            h.btn.setOnClickListener(v -> {
                db.approveEvent(e.getId());
                Toast.makeText(ApproveEventsActivity.this, "Event Approved!", Toast.LENGTH_SHORT).show();
                list.remove(pos);
                notifyItemRemoved(pos);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView title, organizer;
            ImageView img;
            Button btn;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.approve_event_title);
                organizer = v.findViewById(R.id.approve_event_organizer);
                img = v.findViewById(R.id.approve_event_image);
                btn = v.findViewById(R.id.btn_approve_now);
            }
        }
    }
}
