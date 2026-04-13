package com.example.campus_event_org_hub.ui.main;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.AttendeeRecord;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Officer screen: full per-attendee list for a single event.
 * Launched from OfficerAnalyticsFragment with:
 *   EXTRA_EVENT_ID   (int)
 *   EXTRA_EVENT_TITLE (String)
 */
public class OfficerAttendeeListActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID    = "OFFICER_ATT_EVENT_ID";
    public static final String EXTRA_EVENT_TITLE = "OFFICER_ATT_EVENT_TITLE";

    private AttendeeAdapter adapter;
    private List<AttendeeRecord> allRecords   = new ArrayList<>();
    private List<AttendeeRecord> shown        = new ArrayList<>();

    private TextInputEditText etSearch;
    private Spinner spinnerDept;
    private Spinner spinnerStatus;
    private Spinner spinnerCourse;
    private Spinner spinnerYearLevel;
    private Spinner spinnerSection;
    private Chip chipTotal;
    private Chip chipTimedIn;
    private Chip chipTimedOut;
    private TextView tvEmpty;
    private RecyclerView rv;

    private String filterDept     = "All Departments";
    private String filterStatus   = "All Statuses";
    private String filterCourse   = "All Courses";
    private int    filterYear    = 0; // 0 = All Years
    private String filterSection = "All Sections";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_officer_attendee_list);

        int eventId     = getIntent().getIntExtra(EXTRA_EVENT_ID, -1);
        String title    = getIntent().getStringExtra(EXTRA_EVENT_TITLE);

        // Toolbar
        TextView tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        tvToolbarTitle.setText(title != null ? title : "Attendees");

        // Views
        etSearch      = findViewById(R.id.et_search);
        spinnerDept   = findViewById(R.id.spinner_department);
        spinnerStatus = findViewById(R.id.spinner_status);
        spinnerCourse = findViewById(R.id.spinner_course);
        spinnerYearLevel = findViewById(R.id.spinner_year_level);
        spinnerSection = findViewById(R.id.spinner_section);
        chipTotal     = findViewById(R.id.chip_total);
        chipTimedIn   = findViewById(R.id.chip_timed_in);
        chipTimedOut  = findViewById(R.id.chip_timed_out);
        tvEmpty       = findViewById(R.id.tv_empty);
        rv            = findViewById(R.id.rv_attendees);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendeeAdapter();
        rv.setAdapter(adapter);

        // Load data off main thread
        if (eventId > 0) {
            loadAttendees(eventId);
        }

        // Search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ── Data loading ────────────────────────────────────────────────────────────

    private void loadAttendees(int eventId) {
        new AsyncTask<Integer, Void, List<AttendeeRecord>>() {
            @Override
            protected List<AttendeeRecord> doInBackground(Integer... params) {
                return DatabaseHelper.getInstance(getApplicationContext())
                        .getAttendeeDetailsForEvent(params[0]);
            }

            @Override
            protected void onPostExecute(List<AttendeeRecord> records) {
                allRecords.clear();
                allRecords.addAll(records);
                setupSpinners();
                applyFilters();
            }
        }.execute(eventId);
    }

    // ── Spinner setup ────────────────────────────────────────────────────────────

    private void setupSpinners() {
        // Collect unique departments
        Set<String> depts = new LinkedHashSet<>();
        depts.add("All Departments");
        for (AttendeeRecord r : allRecords) {
            if (r.getDepartment() != null && !r.getDepartment().isEmpty()) {
                depts.add(r.getDepartment());
            }
        }
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new ArrayList<>(depts));
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDept.setAdapter(deptAdapter);
        spinnerDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterDept = (String) p.getItemAtPosition(pos);
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Status spinner
        String[] statuses = {"All Statuses", "Timed Out", "Timed In", "Absent"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterStatus = (String) p.getItemAtPosition(pos);
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Course spinner — "All Courses" + unique course codes from data
        Set<String> courseCodes = new LinkedHashSet<>();
        courseCodes.add("All Courses");
        for (AttendeeRecord r : allRecords) {
            if (r.getCourseCode() != null && !r.getCourseCode().isEmpty()) {
                courseCodes.add(r.getCourseCode());
            }
        }
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new ArrayList<>(courseCodes));
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourse.setAdapter(courseAdapter);
        spinnerCourse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterCourse = (String) p.getItemAtPosition(pos);
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Year Level spinner — ordinal labels
        String[] years = {"All Years", "1st Year", "2nd Year", "3rd Year", "4th Year"};
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYearLevel.setAdapter(yearAdapter);
        spinnerYearLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String selected = (String) p.getItemAtPosition(pos);
                filterYear = "All Years".equals(selected) ? 0 : parseOrdinalYear(selected);
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Section spinner — "All Sections" + A, B, C
        String[] sections = {"All Sections", "A", "B", "C"};
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, sections);
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSection.setAdapter(sectionAdapter);
        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                filterSection = (String) p.getItemAtPosition(pos);
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /** Convert "1st Year" / "2nd Year" / "3rd Year" / "4th Year" → int 1-4, else 0. */
    private static int parseOrdinalYear(String label) {
        if (label == null) return 0;
        if (label.startsWith("1st")) return 1;
        if (label.startsWith("2nd")) return 2;
        if (label.startsWith("3rd")) return 3;
        if (label.startsWith("4th")) return 4;
        return 0;
    }

    /** Convert int year level 1-4 to "1st Year", "2nd Year", etc. */
    private static String ordinalYear(int year) {
        switch (year) {
            case 1: return "1st Year";
            case 2: return "2nd Year";
            case 3: return "3rd Year";
            case 4: return "4th Year";
            default: return year + "th Year";
        }
    }

    // ── Filtering ────────────────────────────────────────────────────────────────

    private void applyFilters() {
        String query = etSearch.getText() != null
                ? etSearch.getText().toString().trim().toLowerCase() : "";

        shown.clear();
        int timedInCnt = 0, timedOutCnt = 0;

        for (AttendeeRecord r : allRecords) {
            // Name filter
            if (!query.isEmpty() && !r.getName().toLowerCase().contains(query)) continue;
            // Dept filter
            if (!"All Departments".equals(filterDept) && !filterDept.equals(r.getDepartment())) continue;
            // Course filter
            if (!"All Courses".equals(filterCourse) && !filterCourse.equals(r.getCourseCode())) continue;
            // Year Level filter
            if (filterYear != 0 && r.getYearLevel() != filterYear) continue;
            // Section filter
            if (!"All Sections".equals(filterSection) && !filterSection.equals(r.getSection())) continue;
            // Status filter
            if (!"All Statuses".equals(filterStatus)) {
                String want = filterStatus; // "Timed Out" / "Timed In" / "Absent"
                AttendeeRecord.Status s = r.getStatus();
                if ("Timed Out".equals(want)  && s != AttendeeRecord.Status.TIMED_OUT)  continue;
                if ("Timed In".equals(want)   && s != AttendeeRecord.Status.TIMED_IN)   continue;
                if ("Absent".equals(want)     && s != AttendeeRecord.Status.ABSENT)     continue;
            }
            shown.add(r);
        }

        // Count for summary chips (over full unfiltered list)
        for (AttendeeRecord r : allRecords) {
            if (r.getStatus() == AttendeeRecord.Status.TIMED_IN ||
                r.getStatus() == AttendeeRecord.Status.TIMED_OUT) timedInCnt++;
            if (r.getStatus() == AttendeeRecord.Status.TIMED_OUT) timedOutCnt++;
        }

        chipTotal.setText("Registered: " + allRecords.size());
        chipTimedIn.setText("In: " + timedInCnt);
        chipTimedOut.setText("Out: " + timedOutCnt);

        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setVisibility(shown.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ── RecyclerView adapter ─────────────────────────────────────────────────────

    private class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_officer_attendee, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            h.bind(shown.get(position));
        }

        @Override
        public int getItemCount() { return shown.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivProfile;
            TextView  tvName, tvDept, tvEmail, tvAcademicInfo;
            Chip      chipStatus;
            TextView  tvTimeIn, tvTimeOut;
            View      rowPhotos;
            ImageView ivTimeInPhoto, ivTimeOutPhoto;
            TextView  tvNoTimeInPhoto, tvNoTimeOutPhoto;

            VH(View v) {
                super(v);
                ivProfile       = v.findViewById(R.id.iv_profile);
                tvName          = v.findViewById(R.id.tv_name);
                tvDept          = v.findViewById(R.id.tv_department);
                tvEmail         = v.findViewById(R.id.tv_email);
                tvAcademicInfo  = v.findViewById(R.id.tv_academic_info);
                chipStatus      = v.findViewById(R.id.chip_status);
                tvTimeIn        = v.findViewById(R.id.tv_time_in);
                tvTimeOut       = v.findViewById(R.id.tv_time_out);
                rowPhotos       = v.findViewById(R.id.row_photos);
                ivTimeInPhoto   = v.findViewById(R.id.iv_time_in_photo);
                ivTimeOutPhoto  = v.findViewById(R.id.iv_time_out_photo);
                tvNoTimeInPhoto = v.findViewById(R.id.tv_no_time_in_photo);
                tvNoTimeOutPhoto= v.findViewById(R.id.tv_no_time_out_photo);
            }

            void bind(AttendeeRecord r) {
                tvName.setText(r.getName().isEmpty() ? r.getStudentId() + "-S" : r.getName());
                tvDept.setText(r.getDepartment().isEmpty() ? "Unknown dept." : r.getDepartment());
                tvEmail.setText(r.getEmail().isEmpty() ? r.getStudentId() + "-S" : r.getEmail());

                // Academic info: course code + ordinal year + section letter
                String courseCode = r.getCourseCode();
                int yearLevel = r.getYearLevel();
                String section = r.getSection();
                if (courseCode != null && !courseCode.isEmpty()) {
                    String yr = yearLevel > 0 ? ordinalYear(yearLevel) : "";
                    String sec = (section != null && !section.isEmpty()) ? " | Section " + section : "";
                    tvAcademicInfo.setText(courseCode + (yr.isEmpty() ? "" : " | " + yr) + sec);
                    tvAcademicInfo.setVisibility(View.VISIBLE);
                } else {
                    tvAcademicInfo.setVisibility(View.GONE);
                }

                // Profile photo — use tag guard to prevent recycled-view flicker
                loadBase64Image(r.getProfilePhoto(), ivProfile, R.drawable.ic_person_placeholder);

                // Profile photo
                loadBase64Image(r.getProfilePhoto(), ivProfile, R.drawable.ic_person_placeholder);

                // Status chip
                switch (r.getStatus()) {
                    case TIMED_OUT:
                        chipStatus.setText("Timed Out");
                        chipStatus.setChipBackgroundColorResource(R.color.text_secondary);
                        chipStatus.setTextColor(getResources().getColor(R.color.white, null));
                        break;
                    case TIMED_IN:
                        chipStatus.setText("Timed In");
                        chipStatus.setChipBackgroundColorResource(R.color.primary_green);
                        chipStatus.setTextColor(getResources().getColor(R.color.white, null));
                        break;
                    case ABSENT:
                    default:
                        chipStatus.setText("Absent");
                        chipStatus.setChipBackgroundColorResource(R.color.error_red);
                        chipStatus.setTextColor(getResources().getColor(R.color.white, null));
                        break;
                }

                // Timestamps
                tvTimeIn.setText(r.getTimeIn().isEmpty() ? "—" : formatTimestamp(r.getTimeIn()));
                tvTimeOut.setText(r.getTimeOut().isEmpty() ? "—" : formatTimestamp(r.getTimeOut()));

                // Show photos section if the attendee has any attendance record
                boolean hasAttendance = !r.getTimeIn().isEmpty();
                rowPhotos.setVisibility(hasAttendance ? View.VISIBLE : View.GONE);

                if (hasAttendance) {
                    // Tag each ImageView with the current bind call ID so stale
                    // async decodes don't overwrite recycled views
                    long bindTag = System.currentTimeMillis();
                    ivTimeInPhoto.setTag(bindTag);
                    ivTimeOutPhoto.setTag(bindTag);
                    bindPhotoWithTag(r.getTimeInPhoto(),  ivTimeInPhoto,  tvNoTimeInPhoto,  bindTag);
                    bindPhotoWithTag(r.getTimeOutPhoto(), ivTimeOutPhoto, tvNoTimeOutPhoto, bindTag);
                } else {
                    // Clear any previous photo
                    ivTimeInPhoto.setImageDrawable(null);
                    ivTimeOutPhoto.setImageDrawable(null);
                }

                // Tap on either photo → open full-screen dialog
                ivTimeInPhoto.setOnClickListener(v ->
                        showFullScreenPhoto(r.getTimeInPhoto(), r.getName() + " – Time In"));
                ivTimeOutPhoto.setOnClickListener(v ->
                        showFullScreenPhoto(r.getTimeOutPhoto(), r.getName() + " – Time Out"));
            }

            /** Binds a photo with a tag guard to prevent recycled-view flicker. */
            private void bindPhotoWithTag(String base64, ImageView iv, TextView noPhotoTv, long tag) {
                if (base64 != null && !base64.isEmpty()) {
                    loadBase64ImageTagged(base64, iv, tag);
                    iv.setVisibility(View.VISIBLE);
                    noPhotoTv.setVisibility(View.GONE);
                } else {
                    iv.setVisibility(View.GONE);
                    noPhotoTv.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Decode a Base64 string and display it in the ImageView.
     * Uses setTag/getTag to guard against RecyclerView view recycling.
     * Falls back to fallbackResId on failure.
     */
    private void loadBase64Image(String base64, ImageView iv, int fallbackResId) {
        if (base64 == null || base64.isEmpty()) {
            if (fallbackResId != 0) iv.setImageResource(fallbackResId);
            return;
        }
        final long tag = System.currentTimeMillis();
        iv.setTag(tag);
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                try {
                    String data = params[0];
                    if (data.contains(",")) data = data.substring(data.indexOf(',') + 1);
                    byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bmp) {
                if (iv == null) return;
                // Only apply if this ImageView hasn't been recycled for another item
                if ((long) iv.getTag() != tag) return;
                if (bmp != null) {
                    iv.setImageBitmap(bmp);
                } else if (fallbackResId != 0) {
                    iv.setImageResource(fallbackResId);
                }
            }
        }.execute(base64);
    }

    /**
     * Same as loadBase64Image but accepts an explicit tag to guard against recycling.
     */
    private void loadBase64ImageTagged(String base64, ImageView iv, long expectedTag) {
        if (base64 == null || base64.isEmpty()) return;
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                try {
                    String data = params[0];
                    if (data.contains(",")) data = data.substring(data.indexOf(',') + 1);
                    byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bmp) {
                if (iv == null) return;
                if ((long) iv.getTag() != expectedTag) return;
                if (bmp != null) {
                    iv.setImageBitmap(bmp);
                }
            }
        }.execute(base64);
    }

    /** Show the photo full-screen in an AlertDialog. */
    private void showFullScreenPhoto(String base64, String title) {
        if (base64 == null || base64.isEmpty()) return;
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                try {
                    String data = params[0];
                    if (data.contains(",")) data = data.substring(data.indexOf(',') + 1);
                    byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                } catch (Exception e) { return null; }
            }

            @Override
            protected void onPostExecute(Bitmap bmp) {
                if (bmp == null) return;
                ImageView iv = new ImageView(OfficerAttendeeListActivity.this);
                iv.setImageBitmap(bmp);
                iv.setAdjustViewBounds(true);
                iv.setPadding(8, 8, 8, 8);
                new AlertDialog.Builder(OfficerAttendeeListActivity.this)
                        .setTitle(title)
                        .setView(iv)
                        .setPositiveButton("Close", null)
                        .show();
            }
        }.execute(base64);
    }

    /**
     * Shorten "yyyy-MM-dd HH:mm:ss" → "MMM dd, yyyy  HH:mm" for display.
     * Falls back to the raw string on any parse error.
     */
    private String formatTimestamp(String raw) {
        if (raw == null || raw.isEmpty()) return "—";
        try {
            java.text.SimpleDateFormat sdfIn  =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
            java.text.SimpleDateFormat sdfOut =
                    new java.text.SimpleDateFormat("MMM dd, yyyy  HH:mm", java.util.Locale.US);
            return sdfOut.format(sdfIn.parse(raw));
        } catch (Exception e) {
            return raw;
        }
    }
}
