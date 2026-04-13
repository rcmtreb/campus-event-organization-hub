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
    private Chip chipTotal;
    private Chip chipTimedIn;
    private Chip chipTimedOut;
    private TextView tvEmpty;
    private RecyclerView rv;

    private String filterDept   = "All Departments";
    private String filterStatus = "All Statuses";

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
            TextView  tvName, tvDept, tvEmail;
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
                tvName.setText(r.getName().isEmpty() ? r.getStudentId() : r.getName());
                tvDept.setText(r.getDepartment().isEmpty() ? "Unknown dept." : r.getDepartment());
                tvEmail.setText(r.getEmail().isEmpty() ? r.getStudentId() : r.getEmail());

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
                    bindPhoto(r.getTimeInPhoto(),  ivTimeInPhoto,  tvNoTimeInPhoto);
                    bindPhoto(r.getTimeOutPhoto(), ivTimeOutPhoto, tvNoTimeOutPhoto);
                }

                // Tap on either photo → open full-screen dialog
                ivTimeInPhoto.setOnClickListener(v ->
                        showFullScreenPhoto(r.getTimeInPhoto(), r.getName() + " – Time In"));
                ivTimeOutPhoto.setOnClickListener(v ->
                        showFullScreenPhoto(r.getTimeOutPhoto(), r.getName() + " – Time Out"));
            }

            private void bindPhoto(String base64, ImageView iv, TextView noPhotoTv) {
                if (base64 != null && !base64.isEmpty()) {
                    loadBase64Image(base64, iv, 0);
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
     * Decode a Base64 string (with or without "data:image/...;base64," prefix)
     * and display it into the given ImageView. Falls back to fallbackResId on failure.
     */
    private void loadBase64Image(String base64, ImageView iv, int fallbackResId) {
        if (base64 == null || base64.isEmpty()) {
            if (fallbackResId != 0) iv.setImageResource(fallbackResId);
            return;
        }
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
                if (bmp != null) {
                    iv.setImageBitmap(bmp);
                } else if (fallbackResId != 0) {
                    iv.setImageResource(fallbackResId);
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
