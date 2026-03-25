package com.example.campus_event_org_hub.ui.admin;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

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
    private Timestamp snapshotUpdatedAt = null;

    private TextInputEditText etTitle, etDesc, etDate, etTime, etOrganizer, etCategory, etTags;

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

        // Time picker
        TextInputLayout tilTime = findViewById(R.id.til_edit_time);
        etTime.setOnClickListener(v -> openTimePicker());
        tilTime.setEndIconOnClickListener(v -> openTimePicker());

        // Save button
        MaterialButton btnSave = findViewById(R.id.btn_save_event_edit);
        btnSave.setOnClickListener(v -> saveChanges());

        // Read the current updated_at sentinel from Firestore in the background.
        // This is used for optimistic-lock conflict detection when saving.
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

        // Write local SQLite first (immediate, no conflict risk since SQLite is per-device).
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        boolean localOk = db.updateEvent(eventId, title, desc, date, time, tags, organizer, category, originalVenue);

        if (!localOk) {
            Toast.makeText(this, "Update failed. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Now write to Firestore with conflict check.
        FirestoreHelper fsh = new FirestoreHelper();
        fsh.updateEventFieldsWithConflictCheck(
                eventId, title, desc, date, time, tags, organizer, category,
                snapshotUpdatedAt,
                /* onSuccess */ () -> runOnUiThread(() -> {
                    Toast.makeText(this, "Event updated successfully.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }),
                /* onConflict */ () -> runOnUiThread(() -> {
                    // Another admin edited this event while the screen was open.
                    new AlertDialog.Builder(this)
                            .setTitle("Edit Conflict")
                            .setMessage("Another admin has modified this event since you opened it.\n\n" +
                                    "Your local changes have been saved. " +
                                    "Tap \"Reload\" to fetch the latest version, " +
                                    "or \"Keep Mine\" to overwrite the server with your edits.")
                            .setPositiveButton("Keep Mine", (d, w) -> {
                                // Force-write without the conflict check.
                                fsh.updateEventFields(eventId, title, desc, date, time, tags, organizer, category);
                                Toast.makeText(this, "Your changes have been saved.", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            })
                            .setNegativeButton("Reload", (d, w) -> {
                                // Discard local edits — let AdminEventControlActivity reload.
                                setResult(RESULT_CANCELED);
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                })
        );
    }
}
