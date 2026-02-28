package com.example.campus_event_org_hub;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

        if (event != null) {
            ImageView eventImage = findViewById(R.id.detail_event_image);
            TextView title = findViewById(R.id.detail_event_title);
            TextView date = findViewById(R.id.detail_event_date);
            TextView description = findViewById(R.id.detail_event_description);
            ChipGroup tagsChipGroup = findViewById(R.id.detail_tags_chip_group);
            TextView organizer = findViewById(R.id.detail_event_organizer);
            TextView organizerContact = findViewById(R.id.detail_organizer_contact);
            ImageButton bookmarkButton = findViewById(R.id.btn_bookmark);
            ImageButton shareButton = findViewById(R.id.btn_share);
            Button registerButton = findViewById(R.id.btn_register);

            eventImage.setImageResource(event.getImageResId());
            title.setText(event.getTitle());
            date.setText(event.getDate());
            description.setText(event.getDescription());
            organizer.setText(event.getOrganizer());
            
            organizerContact.setText("Contact: " + event.getOrganizer().toLowerCase().replace(" ", ".") + "@university.edu");

            // Populate Chips with custom style
            String tagsString = event.getTags();
            if (tagsString != null && !tagsString.isEmpty()) {
                String[] tagsArray = tagsString.split(" ");
                for (String tag : tagsArray) {
                    if (!tag.trim().isEmpty()) {
                        // Use ContextThemeWrapper to apply the style
                        Chip chip = new Chip(new ContextThemeWrapper(this, R.style.Widget_App_Chip_Detail), null, 0);
                        chip.setText(tag);
                        tagsChipGroup.addView(chip);
                    }
                }
            }

            setTitle("Event Details");

            bookmarkButton.setOnClickListener(v -> {
                Toast.makeText(this, "Event saved to bookmarks!", Toast.LENGTH_SHORT).show();
            });

            shareButton.setOnClickListener(v -> {
                Toast.makeText(this, "Sharing event: " + event.getTitle(), Toast.LENGTH_SHORT).show();
            });

            registerButton.setOnClickListener(v -> {
                Toast.makeText(this, "Successfully registered for " + event.getTitle() + "!", Toast.LENGTH_LONG).show();
                registerButton.setEnabled(false);
                registerButton.setText("Registered");
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
