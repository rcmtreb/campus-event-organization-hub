package com.example.campus_event_org_hub.ui.events;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.util.ImageUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class EventDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Event event = (Event) getIntent().getSerializableExtra("event");
        String studentId = getIntent().getStringExtra("USER_STUDENT_ID");
        if (studentId == null) studentId = "";

        if (event != null) {
            ImageView eventImage    = findViewById(R.id.detail_event_image);
            TextView  title         = findViewById(R.id.detail_event_title);
            TextView  date          = findViewById(R.id.detail_event_date);
            TextView  description   = findViewById(R.id.detail_event_description);
            ChipGroup tagsChipGroup = findViewById(R.id.detail_tags_chip_group);
            TextView  organizer     = findViewById(R.id.detail_event_organizer);
            TextView  organizerContact = findViewById(R.id.detail_organizer_contact);
            ImageButton bookmarkButton = findViewById(R.id.btn_bookmark);
            ImageButton shareButton    = findViewById(R.id.btn_share);
            Button registerButton      = findViewById(R.id.btn_register);

            ImageUtils.load(this, eventImage, event.getImagePath(), R.drawable.ic_image_placeholder);

            title.setText(event.getTitle());
            date.setText(event.getDate());
            description.setText(event.getDescription());
            organizer.setText(event.getOrganizer());
            organizerContact.setText("Contact: " + event.getOrganizer().toLowerCase().replace(" ", ".") + "@university.edu");

            String tagsString = event.getTags();
            if (tagsString != null && !tagsString.isEmpty()) {
                String[] tagsArray = tagsString.split(" ");
                for (String tag : tagsArray) {
                    if (!tag.trim().isEmpty()) {
                        Chip chip = new Chip(new ContextThemeWrapper(this, R.style.Widget_App_Chip_Detail), null, 0);
                        chip.setText(tag);
                        tagsChipGroup.addView(chip);
                    }
                }
            }

            setTitle("Event Details");
            bookmarkButton.setOnClickListener(v ->
                    Toast.makeText(this, "Event saved to bookmarks!", Toast.LENGTH_SHORT).show());
            shareButton.setOnClickListener(v ->
                    Toast.makeText(this, "Sharing event: " + event.getTitle(), Toast.LENGTH_SHORT).show());

            // ── Registration state ────────────────────────────────────────────
            DatabaseHelper db = new DatabaseHelper(this);
            final String finalStudentId = studentId;
            final int eventId = event.getId();

            if (finalStudentId.isEmpty()) {
                // Not a student (e.g., admin/officer viewing) — disable button
                registerButton.setEnabled(false);
                registerButton.setText("Register for Event");
            } else if (db.isRegistered(finalStudentId, eventId)) {
                setRegisteredState(registerButton);
            } else {
                registerButton.setEnabled(true);
                registerButton.setText("Register for Event");
                registerButton.setOnClickListener(v -> {
                    boolean success = db.registerForEvent(finalStudentId, eventId);
                    if (success) {
                        setRegisteredState(registerButton);
                        Toast.makeText(this, "Successfully registered!", Toast.LENGTH_LONG).show();
                    } else {
                        // Might have been registered from another session
                        setRegisteredState(registerButton);
                        Toast.makeText(this, "You are already registered for this event.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void setRegisteredState(Button btn) {
        btn.setEnabled(false);
        btn.setText("Already Registered \u2713");
        btn.setBackgroundTintList(
                ContextCompat.getColorStateList(this, R.color.success_green));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
