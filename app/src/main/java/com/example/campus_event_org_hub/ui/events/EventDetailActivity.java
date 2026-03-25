package com.example.campus_event_org_hub.ui.events;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.util.ImageUtils;
import com.example.campus_event_org_hub.util.ServerTimeUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

public class EventDetailActivity extends AppCompatActivity {

    private boolean registrationChanged = false;

    /** Polls isEventActive() every minute to show the attendance card when the event starts. */
    private final Handler activeCheckHandler = new Handler(Looper.getMainLooper());
    private Runnable activeCheckRunnable;
    private static final long ACTIVE_CHECK_INTERVAL_MS = 60_000L; // 1 minute

    // Held for the periodic active-check; set when student is registered.
    private MaterialCardView attendanceCardRef;
    private DatabaseHelper   dbRef;
    private Event            eventRef;
    private String           studentIdRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Event event = (Event) getIntent().getSerializableExtra("event");
        String studentId = getIntent().getStringExtra("USER_STUDENT_ID");
        String userRole  = getIntent().getStringExtra("USER_ROLE");
        if (studentId == null) studentId = "";
        if (userRole  == null) userRole  = "";

        if (event != null) {
            ImageView  eventImage      = findViewById(R.id.detail_event_image);
            TextView   title           = findViewById(R.id.detail_event_title);
            TextView   date            = findViewById(R.id.detail_event_date);
            TextView   timeTv          = findViewById(R.id.detail_event_time);
            TextView   venueTv         = findViewById(R.id.detail_event_venue);
            TextView   description     = findViewById(R.id.detail_event_description);
            ChipGroup  tagsChipGroup   = findViewById(R.id.detail_tags_chip_group);
            TextView   organizer       = findViewById(R.id.detail_event_organizer);
            TextView   organizerContact = findViewById(R.id.detail_organizer_contact);
            ImageButton bookmarkButton = findViewById(R.id.btn_bookmark);
            ImageButton shareButton    = findViewById(R.id.btn_share);
            Button      registerButton = findViewById(R.id.btn_register);
            MaterialCardView postponedBanner  = findViewById(R.id.card_postponed_banner);
            MaterialCardView attendanceCard   = findViewById(R.id.card_attendance);

            // Banner image
            int fallbackBanner = ImageUtils.getDefaultBannerForCategory(event.getCategory());
            ImageUtils.load(this, eventImage, event.getImagePath(), fallbackBanner);

            title.setText(event.getTitle());
            date.setText(event.getDate());

            // Time (shown only if non-empty)
            String timeStr = event.getEventTime();
            if (timeStr != null && !timeStr.isEmpty()) {
                timeTv.setText(timeStr);
                timeTv.setVisibility(View.VISIBLE);
            }

            // Venue (shown only if non-empty)
            String venueStr = event.getVenue();
            if (venueStr != null && !venueStr.isEmpty()) {
                venueTv.setText(venueStr);
                venueTv.setVisibility(View.VISIBLE);
            }

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
            DatabaseHelper db = DatabaseHelper.getInstance(this);
            final String finalStudentId = studentId;
            final String finalUserRole  = userRole;
            final int eventId = event.getId();

            boolean isOfficerOrAdmin = "Officer".equals(finalUserRole) || "Admin".equals(finalUserRole);

            if ("POSTPONED".equals(event.getStatus())) {
                postponedBanner.setVisibility(View.VISIBLE);
                registerButton.setEnabled(false);
                registerButton.setText("Registration Unavailable");
                registerButton.setBackgroundTintList(
                        ContextCompat.getColorStateList(this, android.R.color.darker_gray));
            } else if (finalStudentId.isEmpty() || isOfficerOrAdmin) {
                registerButton.setEnabled(false);
                registerButton.setText("Register for Event");
            } else if (db.isRegistered(finalStudentId, eventId)) {
                setRegisteredState(registerButton);
                // Student is registered — show attendance card and start polling
                scheduleActiveCheck(attendanceCard, db, event, finalStudentId);
                bindAttendanceCard(attendanceCard, db, event, finalStudentId);
            } else {
                registerButton.setEnabled(true);
                registerButton.setText("Register for Event");
                registerButton.setOnClickListener(v -> {
                    boolean success = db.registerForEvent(finalStudentId, eventId);
                    if (success) {
                        registrationChanged = true;
                        setRegisteredState(registerButton);
                        Toast.makeText(this, "Successfully registered!", Toast.LENGTH_LONG).show();
                        // Now show attendance card and start polling
                        scheduleActiveCheck(attendanceCard, db, event, finalStudentId);
                        bindAttendanceCard(attendanceCard, db, event, finalStudentId);
                    } else {
                        setRegisteredState(registerButton);
                        Toast.makeText(this, "You are already registered for this event.", Toast.LENGTH_SHORT).show();
                        scheduleActiveCheck(attendanceCard, db, event, finalStudentId);
                        bindAttendanceCard(attendanceCard, db, event, finalStudentId);
                    }
                });
            }
        }
    }

    /**
     * Stores card/db/event/studentId refs and schedules a repeating check that shows
     * the attendance card as soon as the event becomes active while the screen is open.
     * Called immediately after the student is confirmed registered.
     */
    private void scheduleActiveCheck(MaterialCardView card, DatabaseHelper db,
                                     Event event, String studentId) {
        attendanceCardRef = card;
        dbRef             = db;
        eventRef          = event;
        studentIdRef      = studentId;

        if (activeCheckRunnable != null) {
            activeCheckHandler.removeCallbacks(activeCheckRunnable);
        }
        activeCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (attendanceCardRef != null && eventRef != null &&
                        dbRef != null && studentIdRef != null) {
                    // Re-check whether event is now active; if so, refresh the card.
                    bindAttendanceCard(attendanceCardRef, dbRef, eventRef, studentIdRef);
                }
                activeCheckHandler.postDelayed(this, ACTIVE_CHECK_INTERVAL_MS);
            }
        };
        activeCheckHandler.postDelayed(activeCheckRunnable, ACTIVE_CHECK_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop polling while screen is not visible.
        if (activeCheckRunnable != null) {
            activeCheckHandler.removeCallbacks(activeCheckRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart polling when screen returns to foreground (covers app-switch back).
        if (activeCheckRunnable != null) {
            activeCheckHandler.postDelayed(activeCheckRunnable, ACTIVE_CHECK_INTERVAL_MS);
        }
    }

    /**
     * Checks if the event is currently active (within start/end time on event date).
     *
     * Uses UTC-normalized date comparison to avoid phone timezone/locale manipulation.
     * Time-of-day comparison uses minutes-since-midnight to avoid the epoch-date bug
     * where SimpleDateFormat("HH:mm").parse() returns a Date at Jan 1 1970 which
     * would always compare as "in the past" against the real current Date.
     */
    private boolean isEventActive(Event event) {
        String startTime = event.getStartTime();
        String endTime   = event.getEndTime();

        if (startTime == null || startTime.isEmpty() ||
            endTime   == null || endTime.isEmpty()) {
            return false;
        }

        // Also show attendance for explicitly HAPPENING events even without a time window
        if ("HAPPENING".equals(event.getStatus())) {
            // If time window is set, still validate it; otherwise allow access
            if (startTime.isEmpty() || endTime.isEmpty()) return true;
        }

        try {
            // Use server-corrected date to prevent device clock manipulation.
            String eventDate = event.getDate();
            String today     = ServerTimeUtil.todayString();

            if (!eventDate.equals(today)) {
                return false;
            }

            // Compare time-of-day using minutes-since-midnight to avoid epoch-date bug
            int nowMinutes   = minutesSinceMidnight(ServerTimeUtil.now());
            int startMinutes = parseTimeToMinutes(startTime);
            int endMinutes   = parseTimeToMinutes(endTime);

            if (startMinutes < 0 || endMinutes < 0) return false;

            return nowMinutes >= startMinutes && nowMinutes < endMinutes;
        } catch (Exception e) {
            return false;
        }
    }

    /** Returns current time as minutes since midnight (0–1439). */
    private int minutesSinceMidnight(Date now) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    /** Parses "HH:mm" string to minutes since midnight. Returns -1 on failure. */
    private int parseTimeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Sets up and shows the attendance card for a registered student.
     * Only shows attendance during active event time.
     */
    private void bindAttendanceCard(MaterialCardView card, DatabaseHelper db,
                                    Event event, String studentId) {
        if (!isEventActive(event)) {
            card.setVisibility(View.GONE);
            return;
        }

        card.setVisibility(View.VISIBLE);

        TextView tvStatus    = card.findViewById(R.id.tv_attendance_status);
        View     layoutTimeIn  = card.findViewById(R.id.layout_time_in);
        View     layoutTimeOut = card.findViewById(R.id.layout_time_out);
        TextInputEditText etTimeIn  = card.findViewById(R.id.et_time_in_code);
        TextInputEditText etTimeOut = card.findViewById(R.id.et_time_out_code);
        Button   btnTimeIn  = card.findViewById(R.id.btn_time_in);
        Button   btnTimeOut = card.findViewById(R.id.btn_time_out);

        refreshAttendanceState(db, event.getId(), studentId,
                tvStatus, layoutTimeIn, layoutTimeOut, etTimeIn, etTimeOut, btnTimeIn, btnTimeOut);

        btnTimeIn.setOnClickListener(v -> {
            String code = etTimeIn.getText() != null ? etTimeIn.getText().toString().trim() : "";
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter the Time-In code shown by the officer.", Toast.LENGTH_SHORT).show();
                return;
            }
            int result = db.submitTimeIn(event.getId(), studentId, code);
            switch (result) {
                case 0:
                    Toast.makeText(this, "Time-In recorded!", Toast.LENGTH_SHORT).show();
                    etTimeIn.setText("");
                    break;
                case 1:
                    Toast.makeText(this, "Incorrect code. Please try again.", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(this, "You have already timed in.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                    break;
            }
            refreshAttendanceState(db, event.getId(), studentId,
                    tvStatus, layoutTimeIn, layoutTimeOut, etTimeIn, etTimeOut, btnTimeIn, btnTimeOut);
        });

        btnTimeOut.setOnClickListener(v -> {
            String code = etTimeOut.getText() != null ? etTimeOut.getText().toString().trim() : "";
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter the Time-Out code shown by the officer.", Toast.LENGTH_SHORT).show();
                return;
            }
            int result = db.submitTimeOut(event.getId(), studentId, code);
            switch (result) {
                case 0:
                    Toast.makeText(this, "Time-Out recorded!", Toast.LENGTH_SHORT).show();
                    etTimeOut.setText("");
                    break;
                case 1:
                    Toast.makeText(this, "Incorrect code. Please try again.", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(this, "You must Time-In first.", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(this, "You have already timed out.", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                    break;
            }
            refreshAttendanceState(db, event.getId(), studentId,
                    tvStatus, layoutTimeIn, layoutTimeOut, etTimeIn, etTimeOut, btnTimeIn, btnTimeOut);
        });
    }

    private void refreshAttendanceState(DatabaseHelper db, int eventId, String studentId,
                                        TextView tvStatus,
                                        View layoutTimeIn, View layoutTimeOut,
                                        TextInputEditText etTimeIn, TextInputEditText etTimeOut,
                                        Button btnTimeIn, Button btnTimeOut) {
        String[] rec = db.getAttendanceRecord(eventId, studentId);
        boolean hasTimeIn  = rec != null && rec[0] != null && !rec[0].isEmpty();
        boolean hasTimeOut = rec != null && rec[1] != null && !rec[1].isEmpty();

        if (hasTimeOut) {
            tvStatus.setText("Attendance complete.\nTime In: " + rec[0] + "\nTime Out: " + rec[1]);
            layoutTimeIn.setVisibility(View.GONE);
            layoutTimeOut.setVisibility(View.GONE);
        } else if (hasTimeIn) {
            String status = "Timed in at " + rec[0] + ". Submit Time-Out code when you leave.";
            tvStatus.setText(status);
            layoutTimeIn.setVisibility(View.GONE);
            layoutTimeOut.setVisibility(View.VISIBLE);
        } else {
            tvStatus.setText("Waiting for officer's attendance code.");
            layoutTimeIn.setVisibility(View.VISIBLE);
            layoutTimeOut.setVisibility(View.GONE);
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
        finishWithResult();
        return true;
    }

    @Override
    public void onBackPressed() {
        finishWithResult();
    }

    private void finishWithResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("registration_changed", registrationChanged);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
