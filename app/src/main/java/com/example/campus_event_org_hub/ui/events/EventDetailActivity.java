package com.example.campus_event_org_hub.ui.events;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.SyncManager;
import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.util.ImageUtils;
import com.example.campus_event_org_hub.util.ServerTimeUtil;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

public class EventDetailActivity extends AppCompatActivity {

    private boolean registrationChanged = false;

    private final Handler activeCheckHandler = new Handler(Looper.getMainLooper());
    private Runnable activeCheckRunnable;
    private static final long ACTIVE_CHECK_INTERVAL_MS = 60_000L;

    private MaterialCardView attendanceCardRef;
    private DatabaseHelper   dbRef;
    private Event            eventRef;
    private String           studentIdRef;

    private String pendingTimeInPhoto  = null;
    private String pendingTimeOutPhoto = null;
    private Uri    pendingCameraUri   = null;
    private boolean isCapturingTimeIn  = true;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && pendingCameraUri != null) {
                    encodeCameraPhoto(pendingCameraUri);
                } else {
                    pendingCameraUri = null;
                }
            });

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    if (isCapturingTimeIn) {
                        openCameraInternal();
                    } else {
                        openCameraInternal();
                    }
                } else {
                    Toast.makeText(this, "Camera permission is required to capture attendance photo.",
                            Toast.LENGTH_LONG).show();
                }
            });

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

        final String finalStudentId = studentId;
        final String finalUserRole = userRole;
        final int eventId = event.getId();

        if (event != null) {
            this.eventRef = event;
            this.studentIdRef = studentId;
            DatabaseHelper db = DatabaseHelper.getInstance(this);

            ImageView  eventImage      = findViewById(R.id.detail_event_image);
            TextView   title          = findViewById(R.id.detail_event_title);
            TextView   date           = findViewById(R.id.detail_event_date);
            TextView   timeTv         = findViewById(R.id.detail_event_time);
            TextView   venueTv        = findViewById(R.id.detail_event_venue);
            TextView   description    = findViewById(R.id.detail_event_description);
            ChipGroup  tagsChipGroup  = findViewById(R.id.detail_tags_chip_group);
            TextView   organizer      = findViewById(R.id.detail_event_organizer);
            TextView   organizerContact = findViewById(R.id.detail_organizer_contact);
            ImageButton bookmarkButton = findViewById(R.id.btn_bookmark);
            ImageButton shareButton   = findViewById(R.id.btn_share);
            Button      registerButton = findViewById(R.id.btn_register);
            MaterialCardView postponedBanner = findViewById(R.id.card_postponed_banner);
            MaterialCardView attendanceCard = findViewById(R.id.card_attendance);
            this.attendanceCardRef = attendanceCard;

            int fallbackBanner = ImageUtils.getDefaultBannerForCategory(event.getCategory());
            ImageUtils.load(this, eventImage, event.getImagePath(), fallbackBanner);

            title.setText(event.getTitle());
            date.setText(event.getDate());

            String timeStr = event.getEventTime();
            if (timeStr != null && !timeStr.isEmpty()) {
                timeTv.setText(timeStr);
                timeTv.setVisibility(View.VISIBLE);
            }

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
                scheduleActiveCheck(attendanceCard, db, event, finalStudentId);
                bindAttendanceCard(attendanceCard, db, event, finalStudentId);
            } else if (!isRegistrationOpen(event)) {
                registerButton.setEnabled(false);
                registerButton.setText("Registration Closed");
                registerButton.setBackgroundTintList(
                        ContextCompat.getColorStateList(this, android.R.color.darker_gray));
            } else {
                registerButton.setEnabled(true);
                registerButton.setText("Register for Event");
                registerButton.setOnClickListener(v -> {
                    if (!isRegistrationOpen(event)) {
                        registerButton.setEnabled(false);
                        registerButton.setText("Registration Closed");
                        registerButton.setBackgroundTintList(
                                ContextCompat.getColorStateList(this, android.R.color.darker_gray));
                        Toast.makeText(this,
                                "Registration is now closed. The 1-hour window after start time has passed.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    registerButton.setEnabled(false);
                    registerButton.setText("Registering...");
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Void... params) {
                            return db.registerForEvent(finalStudentId, eventId);
                        }
                        @Override
                        protected void onPostExecute(Boolean success) {
                            if (success) {
                                registrationChanged = true;
                                setRegisteredState(registerButton);
                                scheduleActiveCheck(attendanceCard, db, event, finalStudentId);
                                bindAttendanceCard(attendanceCard, db, event, finalStudentId);
                                Toast.makeText(EventDetailActivity.this,
                                        "Registered successfully!", Toast.LENGTH_SHORT).show();
                            } else {
                                registerButton.setEnabled(true);
                                registerButton.setText("Register for Event");
                                Toast.makeText(EventDetailActivity.this,
                                        "Registration failed. You may already be registered.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute();
                });
            }
        }
    }

    private void scheduleActiveCheck(MaterialCardView card, DatabaseHelper db,
                                     Event event, String studentId) {
        activeCheckRunnable = () -> bindAttendanceCard(card, db, event, studentId);
        activeCheckHandler.postDelayed(activeCheckRunnable, ACTIVE_CHECK_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (activeCheckRunnable != null) {
            activeCheckHandler.removeCallbacks(activeCheckRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (activeCheckRunnable != null) {
            activeCheckHandler.postDelayed(activeCheckRunnable, ACTIVE_CHECK_INTERVAL_MS);
        }
        SyncManager.sync(this, () -> {
            DatabaseHelper db = DatabaseHelper.getInstance(this);
            Event fresh = db.getEventById(eventRef != null ? eventRef.getId() : 0);
            if (fresh != null && attendanceCardRef != null && studentIdRef != null) {
                bindAttendanceCard(attendanceCardRef, db, fresh, studentIdRef);
            }
        });
    }

    /**
     * Registration is open as long as now < start + 60 minutes (same cutoff as Time-In).
     * If the event has no start time set, or the event date hasn't arrived yet, registration
     * is considered open (no cutoff to apply).
     */
    private boolean isRegistrationOpen(Event event) {
        try {
            String eventDate = event.getDate();
            String today     = ServerTimeUtil.todayString();
            // Future date — registration always open
            if (eventDate == null || eventDate.compareTo(today) > 0) return true;
            // Past date — registration closed
            if (eventDate.compareTo(today) < 0) return false;

            // Today — check time
            String startTime = event.getStartTime();
            if (startTime == null || startTime.isEmpty()) return true; // no time set, leave open

            int startMinutes = parseTimeToMinutes(startTime);
            if (startMinutes < 0) return true;

            Calendar nowCal = Calendar.getInstance();
            nowCal.setTime(ServerTimeUtil.now());
            int nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE);

            // Closes at start + 60 minutes (same as Time-In cutoff)
            return nowMinutes < startMinutes + 60;
        } catch (Exception e) {
            return true; // fail open
        }
    }

    private int minutesSinceMidnight(Date now) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    private int parseTimeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean isAttendanceWindowOpen(Event event) {
        try {
            String startTime = event.getStartTime();
            if (startTime == null || startTime.isEmpty()) return true;
            int startMinutes = parseTimeToMinutes(startTime);
            if (startMinutes < 0) return true;
            int nowMinutes = minutesSinceMidnight(ServerTimeUtil.now());
            return nowMinutes < startMinutes + 60;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isTimeInWindowOpen(Event event) {
        try {
            String startTime = event.getStartTime();
            if (startTime == null || startTime.isEmpty()) return true;
            int startMinutes = parseTimeToMinutes(startTime);
            if (startMinutes < 0) return true;
            int nowMinutes = minutesSinceMidnight(ServerTimeUtil.now());
            return nowMinutes < startMinutes + 60;
        } catch (Exception e) {
            return true;
        }
    }

    /** Returns true if the event's date has already passed. */
    private boolean isEventPast(Event event) {
        try {
            String dateStr = event.getDate();
            if (dateStr == null || dateStr.isEmpty()) return false;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date eventDate = sdf.parse(dateStr);
            Date today = sdf.parse(sdf.format(ServerTimeUtil.now()));
            return eventDate != null && eventDate.before(today);
        } catch (Exception e) {
            return false;
        }
    }

    private void bindAttendanceCard(MaterialCardView card, DatabaseHelper db,
                                    Event event, String studentId) {
        // Always show the card to registered students so they can see their attendance status.
        card.setVisibility(View.VISIBLE);

        TextView tvStatus          = card.findViewById(R.id.tv_attendance_status);
        View     layoutTimeIn      = card.findViewById(R.id.layout_time_in);
        View     layoutTimeOut     = card.findViewById(R.id.layout_time_out);
        TextInputEditText etTimeIn  = card.findViewById(R.id.et_time_in_code);
        TextInputEditText etTimeOut = card.findViewById(R.id.et_time_out_code);
        Button   btnTimeIn         = card.findViewById(R.id.btn_time_in);
        Button   btnTimeOut        = card.findViewById(R.id.btn_time_out);
        LinearLayout layoutPhoto   = card.findViewById(R.id.layout_attendance_photo);
        TextView tvPhotoRequired   = card.findViewById(R.id.tv_photo_required);
        ImageView ivPhotoPreview   = card.findViewById(R.id.iv_attendance_photo_preview);
        LinearLayout layoutPlaceholder = card.findViewById(R.id.layout_photo_placeholder);
        Button btnCapturePhoto      = card.findViewById(R.id.btn_capture_attendance_photo);

        // Re-fetch the event from DB so startTime/endTime are always current (not stale from Intent).
        Event freshEvent = db.getEventById(event.getId());
        if (freshEvent == null) freshEvent = event;
        final Event ev = freshEvent;

        // Check attendance record first — this drives the display regardless of window state.
        String[] rec = db.getAttendanceRecord(ev.getId(), studentId);
        boolean hasTimeIn  = rec != null && rec[0] != null && !rec[0].isEmpty();
        boolean hasTimeOut = rec != null && rec[1] != null && !rec[1].isEmpty();

        boolean windowOpen = isAttendanceWindowOpen(ev);

        if (hasTimeOut) {
            // Attendance fully complete — show summary, hide everything else.
            tvStatus.setText("Attendance complete.\nTime In: " + rec[0] + "\nTime Out: " + rec[1]);
            layoutTimeIn.setVisibility(View.GONE);
            layoutTimeOut.setVisibility(View.GONE);
            layoutPhoto.setVisibility(View.GONE);
            return;
        }

        if (hasTimeIn) {
            // Already timed in — show time-out form if window is still open, else show status only.
            boolean isPast = isEventPast(ev);
            String suffix;
            if (windowOpen) {
                suffix = ". Submit Time-Out code when you leave.";
            } else if (isPast) {
                suffix = ". Event has ended.";
            } else {
                suffix = ". Attendance window is currently closed.";
            }
            tvStatus.setText("Timed in at " + rec[0] + suffix);
            layoutTimeIn.setVisibility(View.GONE);
            layoutPhoto.setVisibility(windowOpen ? View.VISIBLE : View.GONE);
            layoutTimeOut.setVisibility(windowOpen ? View.VISIBLE : View.GONE);
            if (windowOpen) {
                tvPhotoRequired.setText("Capture your Time-Out photo before submitting.");
                // Prefer the pending time-out photo (just captured) over the stored time-in photo,
                // so that a 60s rebind doesn't erase the user's newly captured time-out photo.
                if (pendingTimeOutPhoto != null && !pendingTimeOutPhoto.isEmpty()) {
                    ivPhotoPreview.setVisibility(View.VISIBLE);
                    layoutPlaceholder.setVisibility(View.GONE);
                    loadPhotoPreview(ivPhotoPreview, pendingTimeOutPhoto);
                } else {
                    // No time-out photo yet — show the time-in photo as reference, or placeholder.
                    String savedTimeInPhoto = rec.length > 2 ? rec[2] : null;
                    if (savedTimeInPhoto != null && !savedTimeInPhoto.isEmpty()) {
                        ivPhotoPreview.setVisibility(View.VISIBLE);
                        layoutPlaceholder.setVisibility(View.GONE);
                        loadPhotoPreview(ivPhotoPreview, savedTimeInPhoto);
                    } else {
                        ivPhotoPreview.setVisibility(View.GONE);
                        layoutPlaceholder.setVisibility(View.VISIBLE);
                    }
                }
                btnCapturePhoto.setOnClickListener(v -> {
                    isCapturingTimeIn = false;
                    requestCameraPermission();
                });
                btnTimeOut.setOnClickListener(v -> {
                    if (pendingTimeOutPhoto == null || pendingTimeOutPhoto.isEmpty()) {
                        Toast.makeText(this, "Please capture your Time-Out photo first.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String code = etTimeOut.getText() != null ? etTimeOut.getText().toString().trim() : "";
                    if (code.isEmpty()) {
                        Toast.makeText(this, "Enter the Time-Out code shown by the officer.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Disable immediately to prevent double-submission
                    btnTimeOut.setEnabled(false);
                    int result = db.submitTimeOutWithFallback(ev.getId(), studentId, code, "", "", pendingTimeOutPhoto);
                    switch (result) {
                        case 0:
                            Toast.makeText(this, "Time-Out recorded!", Toast.LENGTH_SHORT).show();
                            etTimeOut.setText("");
                            pendingTimeOutPhoto = null;
                            break;
                        case 1:
                            Toast.makeText(this, "Incorrect code. Please try again.", Toast.LENGTH_SHORT).show();
                            btnTimeOut.setEnabled(true);
                            break;
                        case 2:
                            Toast.makeText(this, "It's too early to Time-Out — the event hasn't started yet.", Toast.LENGTH_LONG).show();
                            btnTimeOut.setEnabled(true);
                            break;
                        case 3:
                            Toast.makeText(this, "You have already timed out.", Toast.LENGTH_SHORT).show();
                            break;
                        case 4:
                            Toast.makeText(this, "Time-Out window has closed.", Toast.LENGTH_LONG).show();
                            break;
                        case 6:
                            Toast.makeText(this, "No Time-In record found. Try again in a moment.", Toast.LENGTH_LONG).show();
                            btnTimeOut.setEnabled(true);
                            break;
                        case 61:
                            Toast.makeText(this, "No Time-In record found. Please Time-In first.", Toast.LENGTH_LONG).show();
                            btnTimeOut.setEnabled(true);
                            break;
                        case -4:
                            Toast.makeText(this, "Too many incorrect attempts. Please wait 15 minutes.", Toast.LENGTH_LONG).show();
                            btnTimeOut.setEnabled(true);
                            break;
                        default:
                            Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                            btnTimeOut.setEnabled(true);
                            break;
                    }
                    bindAttendanceCard(card, db, event, studentId);
                });
            }
            return;
        }

        // Not yet timed in.
        if (!windowOpen) {
            // Distinguish past events from future ones.
            boolean isPast = isEventPast(ev);
            if (isPast) {
                tvStatus.setText("Event has ended.\nYou did not time in for this event.");
            } else {
                tvStatus.setText("Attendance window not yet open.");
            }
            layoutTimeIn.setVisibility(View.GONE);
            layoutTimeOut.setVisibility(View.GONE);
            layoutPhoto.setVisibility(View.GONE);
            return;
        }

        if (!isTimeInWindowOpen(ev)) {
            // Overall window is open (time-out still possible) but time-in cutoff has passed.
            tvStatus.setText("Time-In window has closed. You were not able to time in.");
            layoutTimeIn.setVisibility(View.GONE);
            layoutTimeOut.setVisibility(View.GONE);
            layoutPhoto.setVisibility(View.GONE);
            return;
        }

        // Window is open and student has not timed in yet — show Time-In form.
        tvStatus.setText("Waiting for officer's attendance code.");
        layoutTimeIn.setVisibility(View.VISIBLE);
        layoutTimeOut.setVisibility(View.GONE);
        layoutPhoto.setVisibility(View.VISIBLE);
        tvPhotoRequired.setText("Take a photo to verify your attendance.");

        if (pendingTimeInPhoto != null && !pendingTimeInPhoto.isEmpty()) {
            ivPhotoPreview.setVisibility(View.VISIBLE);
            layoutPlaceholder.setVisibility(View.GONE);
            loadPhotoPreview(ivPhotoPreview, pendingTimeInPhoto);
        } else {
            ivPhotoPreview.setVisibility(View.GONE);
            layoutPlaceholder.setVisibility(View.VISIBLE);
        }

        btnCapturePhoto.setOnClickListener(v -> {
            isCapturingTimeIn = true;
            requestCameraPermission();
        });

        btnTimeIn.setOnClickListener(v -> {
            if (pendingTimeInPhoto == null || pendingTimeInPhoto.isEmpty()) {
                Toast.makeText(this, "Please capture your photo first.", Toast.LENGTH_SHORT).show();
                return;
            }
            String code = etTimeIn.getText() != null ? etTimeIn.getText().toString().trim() : "";
            if (code.isEmpty()) {
                Toast.makeText(this, "Enter the Time-In code shown by the officer.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Disable immediately to prevent double-submission
            btnTimeIn.setEnabled(false);
            int result = db.submitTimeIn(ev.getId(), studentId, code, "", "", pendingTimeInPhoto);
            switch (result) {
                case 0:
                    Toast.makeText(this, "Time-In recorded!", Toast.LENGTH_SHORT).show();
                    etTimeIn.setText("");
                    pendingTimeInPhoto = null;
                    break;
                case 1:
                    Toast.makeText(this, "Incorrect code. Please try again.", Toast.LENGTH_SHORT).show();
                    btnTimeIn.setEnabled(true);
                    break;
                case 2:
                    Toast.makeText(this, "You have already timed in.", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(this, "Time-In window has closed.", Toast.LENGTH_LONG).show();
                    break;
                case 5:
                    Toast.makeText(this, "You must register for this event before timing in.", Toast.LENGTH_LONG).show();
                    btnTimeIn.setEnabled(true);
                    break;
                case -4:
                    Toast.makeText(this, "Too many incorrect attempts. Please wait 15 minutes.", Toast.LENGTH_LONG).show();
                    btnTimeIn.setEnabled(true);
                    break;
                default:
                    Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
                    btnTimeIn.setEnabled(true);
                    break;
            }
            bindAttendanceCard(card, db, event, studentId);
        });
    }

    private void loadPhotoPreview(ImageView iv, String base64Data) {
        new Thread(() -> {
            try {
                int commaIndex = base64Data.indexOf(',');
                if (commaIndex < 0) return;
                byte[] bytes = Base64.decode(base64Data.substring(commaIndex + 1), Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) {
                    final Bitmap finalBmp = bmp;
                    Handler main = new Handler(Looper.getMainLooper());
                    main.post(() -> {
                        iv.setImageBitmap(finalBmp);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    });
                }
            } catch (Exception e) {
                // ignore
            }
        }).start();
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCameraInternal();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCameraInternal() {
        try {
            File photoFile = createImageFile();
            pendingCameraUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                cameraLauncher.launch(takePictureIntent);
            } else {
                Toast.makeText(this, "No camera app available.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Could not create photo file.", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "ATTENDANCE_" + timeStamp;
        File storageDir = getCacheDir();
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    private void encodeCameraPhoto(Uri uri) {
        new Thread(() -> {
            try {
                Bitmap bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                if (bmp == null) return;

                int maxDim = 800;
                float scale = Math.min((float) maxDim / bmp.getWidth(), (float) maxDim / bmp.getHeight());
                if (scale < 1f) {
                    bmp = Bitmap.createScaledBitmap(bmp,
                            Math.round(bmp.getWidth() * scale),
                            Math.round(bmp.getHeight() * scale), true);
                }
                final Bitmap finalBmp = bmp;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
                String base64 = "data:image/png;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                Handler main = new Handler(Looper.getMainLooper());
                main.post(() -> {
                    if (isCapturingTimeIn) {
                        pendingTimeInPhoto = base64;
                    } else {
                        pendingTimeOutPhoto = base64;
                    }

                    ImageView ivPreview = findViewById(R.id.iv_attendance_photo_preview);
                    LinearLayout placeholder = findViewById(R.id.layout_photo_placeholder);
                    if (ivPreview != null) {
                        ivPreview.setVisibility(View.VISIBLE);
                        ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivPreview.setImageBitmap(finalBmp);
                    }
                    if (placeholder != null) placeholder.setVisibility(View.GONE);

                    Toast.makeText(this, "Photo captured. You can now submit your code.",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Handler main = new Handler(Looper.getMainLooper());
                main.post(() ->
                        Toast.makeText(this, "Failed to process photo.", Toast.LENGTH_SHORT).show());
            }
        }).start();
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
