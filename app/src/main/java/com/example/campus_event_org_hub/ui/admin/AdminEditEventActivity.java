package com.example.campus_event_org_hub.ui.admin;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

// venue field is preserved from the original event; admin edit form does not expose it

import java.util.Calendar;
import java.util.Locale;

/**
 * Allows the admin to edit the details of any event.
 * Returns RESULT_OK so AdminEventControlActivity reloads its list.
 */
public class AdminEditEventActivity extends AppCompatActivity {

    private int eventId;
    private String originalVenue = "";
    private TextInputEditText etTitle, etDesc, etDate, etTime, etOrganizer, etCategory, etTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_event);

        eventId = getIntent().getIntExtra("EVENT_ID", -1);
        originalVenue = getIntent().getStringExtra("EVENT_VENUE") != null
                ? getIntent().getStringExtra("EVENT_VENUE") : "";

        etTitle     = findViewById(R.id.et_edit_title);
        etDesc      = findViewById(R.id.et_edit_desc);
        etDate      = findViewById(R.id.et_edit_date);
        etTime      = findViewById(R.id.et_edit_time);
        etOrganizer = findViewById(R.id.et_edit_organizer);
        etCategory  = findViewById(R.id.et_edit_category);
        etTags      = findViewById(R.id.et_edit_tags);

        // Pre-fill from intent extras
        etTitle.setText(getIntent().getStringExtra("EVENT_TITLE"));
        etDesc.setText(getIntent().getStringExtra("EVENT_DESC"));
        etDate.setText(getIntent().getStringExtra("EVENT_DATE"));
        etTime.setText(getIntent().getStringExtra("EVENT_TIME"));
        etOrganizer.setText(getIntent().getStringExtra("EVENT_ORGANIZER"));
        etCategory.setText(getIntent().getStringExtra("EVENT_CATEGORY"));
        etTags.setText(getIntent().getStringExtra("EVENT_TAGS"));

        // Back button
        ImageButton btnBack = findViewById(R.id.btn_back_edit_event);
        btnBack.setOnClickListener(v -> finish());

        // Date field is locked — no picker attached

        // Time picker
        TextInputLayout tilTime = findViewById(R.id.til_edit_time);
        etTime.setOnClickListener(v -> openTimePicker());
        tilTime.setEndIconOnClickListener(v -> openTimePicker());

        // Save button
        MaterialButton btnSave = findViewById(R.id.btn_save_event_edit);
        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void openTimePicker() {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(this,
                (view, hour, minute) -> {
                    String formatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    etTime.setText(formatted);
                },
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
        ).show();
    }

    private void saveChanges() {
        if (eventId == -1) {
            Toast.makeText(this, "Invalid event ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title     = etTitle.getText()     != null ? etTitle.getText().toString().trim()     : "";
        String desc      = etDesc.getText()      != null ? etDesc.getText().toString().trim()      : "";
        String date      = etDate.getText()      != null ? etDate.getText().toString().trim()      : "";
        String time      = etTime.getText()      != null ? etTime.getText().toString().trim()      : "";
        String organizer = etOrganizer.getText() != null ? etOrganizer.getText().toString().trim() : "";
        String category  = etCategory.getText()  != null ? etCategory.getText().toString().trim()  : "";
        String tags      = etTags.getText()      != null ? etTags.getText().toString().trim()      : "";

        if (title.isEmpty() || desc.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Title, description, and date are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        boolean ok = db.updateEvent(eventId, title, desc, date, time, tags, organizer, category, originalVenue);
        if (ok) {
            Toast.makeText(this, "Event updated successfully.", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Update failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
