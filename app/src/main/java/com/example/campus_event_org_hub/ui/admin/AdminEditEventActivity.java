package com.example.campus_event_org_hub.ui.admin;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.FirestoreHelper;
import com.example.campus_event_org_hub.ui.base.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;

// venue field is preserved from the original event; admin edit form does not expose it

/**
 * Allows the admin to edit the details of any event.
 * Uses an optimistic-lock sentinel (Firestore updated_at timestamp) to detect
 * concurrent edits by another admin and shows a conflict dialog instead of
 * silently overwriting.
 * Returns RESULT_OK so AdminEventControlActivity reloads its list.
 */
public class AdminEditEventActivity extends com.example.campus_event_org_hub.ui.base.BaseActivity {

    private static final String TAG = "AdminEditEvent";

    private int       eventId;
    private String    originalVenue = "";

    /** Firestore updated_at at the time the admin opened this screen — used for conflict check. */
    private volatile Timestamp snapshotUpdatedAt = null;

    private TextInputEditText etTitle, etDesc, etDate, etStartTime, etEndTime,
                              etOrganizer, etCategory, etTags;

    @Override
    protected boolean useEdgeToEdge() { return true; }

    @Override
    protected boolean isExitOnBackEnabled() { return false; }

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
        etStartTime = findViewById(R.id.et_edit_start_time);
        etEndTime   = findViewById(R.id.et_edit_end_time);
        etOrganizer = findViewById(R.id.et_edit_organizer);
        etCategory  = findViewById(R.id.et_edit_category);
        etTags      = findViewById(R.id.et_edit_tags);

        // Pre-fill from intent extras
        etTitle.setText(getIntent().getStringExtra("EVENT_TITLE"));
        etDesc.setText(getIntent().getStringExtra("EVENT_DESC"));
        etDate.setText(getIntent().getStringExtra("EVENT_DATE"));
        etStartTime.setText(getIntent().getStringExtra("EVENT_START_TIME"));
        etEndTime.setText(getIntent().getStringExtra("EVENT_END_TIME"));
        etOrganizer.setText(getIntent().getStringExtra("EVENT_ORGANIZER"));
        etCategory.setText(getIntent().getStringExtra("EVENT_CATEGORY"));
        etTags.setText(getIntent().getStringExtra("EVENT_TAGS"));

        // Start time picker
        TextInputLayout tilStartTime = findViewById(R.id.til_edit_start_time);
        etStartTime.setOnClickListener(v -> openTimePicker(etStartTime));
        tilStartTime.setEndIconOnClickListener(v -> openTimePicker(etStartTime));

        // End time picker
        TextInputLayout tilEndTime = findViewById(R.id.til_edit_end_time);
        etEndTime.setOnClickListener(v -> openTimePicker(etEndTime));
        tilEndTime.setEndIconOnClickListener(v -> openTimePicker(etEndTime));

        // Save button
        MaterialButton btnSave = findViewById(R.id.btn_save_event_edit);
        btnSave.setOnClickListener(v -> saveChanges());

        // Read the current updated_at sentinel from Firestore in the background.
        // volatile ensures the main-thread save sees the written value.
        if (eventId != -1) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    FirestoreHelper fsh = new FirestoreHelper();
                    snapshotUpdatedAt = fsh.getEventUpdatedAt(eventId);
                } catch (Exception e) {
                    Log.w(TAG, "Could not read updated_at; conflict detection disabled", e);
                }
            });
        }
    }

    private void openTimePicker(TextInputEditText target) {
        // Pre-seed the picker with whatever is already in the field, or now.
        int initHour, initMinute;
        String current = target.getText() != null ? target.getText().toString().trim() : "";
        if (current.matches("\\d{2}:\\d{2}")) {
            String[] parts = current.split(":");
            initHour   = Integer.parseInt(parts[0]);
            initMinute = Integer.parseInt(parts[1]);
        } else {
            Calendar cal = Calendar.getInstance();
            initHour   = cal.get(Calendar.HOUR_OF_DAY);
            initMinute = cal.get(Calendar.MINUTE);
        }
        new TimePickerDialog(this,
                (view, hour, minute) -> {
                    String formatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    target.setText(formatted);
                },
                initHour, initMinute, true
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
        String startTime = etStartTime.getText() != null ? etStartTime.getText().toString().trim() : "";
        String endTime   = etEndTime.getText()   != null ? etEndTime.getText().toString().trim()   : "";
        String organizer = etOrganizer.getText() != null ? etOrganizer.getText().toString().trim() : "";
        String category  = etCategory.getText()  != null ? etCategory.getText().toString().trim()  : "";
        String tags      = etTags.getText()      != null ? etTags.getText().toString().trim()      : "";

        if (title.isEmpty() || desc.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Title, description, and date are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(this, "Start time and end time are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build the legacy event_time display string from start+end.
        String eventTime = startTime + " - " + endTime;

        // Write local SQLite first (immediate, no conflict risk since SQLite is per-device).
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        boolean localOk = db.updateEvent(eventId, title, desc, date,
                eventTime, startTime, endTime,
                tags, organizer, category, originalVenue);

        if (!localOk) {
            Toast.makeText(this, "Update failed. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Local write succeeded — show feedback and close immediately.
        // Firestore sync continues in the background.
        Toast.makeText(this, "Event updated successfully.", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();

        // Sync to Firestore with conflict check (runs after screen closes).
        FirestoreHelper fsh = new FirestoreHelper();
        fsh.updateEventFieldsWithConflictCheck(
                eventId, title, desc, date, eventTime, startTime, endTime,
                tags, organizer, category,
                snapshotUpdatedAt,
                /* onSuccess */ null,
                /* onConflict */ () -> runOnUiThread(() ->
                    Toast.makeText(getApplicationContext(),
                            "Note: A conflict was detected with another admin's edit. " +
                            "Re-open the event to verify.", Toast.LENGTH_LONG).show()
                )
        );
    }
}
