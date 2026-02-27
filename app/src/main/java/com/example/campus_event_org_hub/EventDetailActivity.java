package com.example.campus_event_org_hub;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
            TextView title = findViewById(R.id.detail_event_title);
            TextView date = findViewById(R.id.detail_event_date);
            TextView description = findViewById(R.id.detail_event_description);
            TextView tags = findViewById(R.id.detail_event_tags);
            TextView organizer = findViewById(R.id.detail_event_organizer);
            ImageButton bookmarkButton = findViewById(R.id.btn_bookmark);
            ImageButton shareButton = findViewById(R.id.btn_share);

            title.setText(event.getTitle());
            date.setText(event.getDate());
            description.setText(event.getDescription());
            tags.setText(event.getTags());
            organizer.setText(event.getOrganizer());
            
            setTitle(event.getTitle());

            bookmarkButton.setOnClickListener(v -> {
                // TODO: Implement bookmark functionality
                Toast.makeText(this, "Bookmark clicked!", Toast.LENGTH_SHORT).show();
            });

            shareButton.setOnClickListener(v -> {
                // TODO: Implement share functionality
                Toast.makeText(this, "Share clicked!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
