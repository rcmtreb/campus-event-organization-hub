package com.example.campus_event_org_hub.ui.events;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.example.campus_event_org_hub.util.ImageUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateEventFragment extends Fragment {

    // Maps chip abbreviation → full department name stored in DB
    private static final String[][] DEPT_MAP = {
            {"CBA",   "College of Business Accountancy (CBA)"},
            {"CCJE",  "College of Criminal Justice Education (CCJE)"},
            {"COED",  "College of Education (COED)"},
            {"COE",   "College of Engineering (COE)"},
            {"COL",   "College of Law (COL)"},
            {"CLAS",  "College of Liberal Arts and Sciences (CLAS)"},
            {"GS",    "Graduate School (GS)"},
    };

    private Uri selectedImageUri;
    private ImageView bannerPreview;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        ImageUtils.load(requireContext(), bannerPreview,
                                uri.toString(), R.drawable.ic_image_placeholder);
                        try {
                            requireContext().getContentResolver()
                                    .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignored) {}
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);

        // --- Get logged-in user info passed from MainActivity ---
        Bundle args = getArguments();
        String userName      = args != null ? args.getString("USER_NAME",       "") : "";
        String userDept      = args != null ? args.getString("USER_DEPT",       "") : "";
        String userStudentId = args != null ? args.getString("USER_STUDENT_ID", "") : "";

        // --- Views ---
        bannerPreview = view.findViewById(R.id.create_event_banner_preview);
        TextInputEditText titleEt       = view.findViewById(R.id.et_create_title);
        TextInputEditText descEt        = view.findViewById(R.id.et_create_description);
        TextInputEditText dateEt        = view.findViewById(R.id.et_create_date);
        TextInputLayout   tilDate       = view.findViewById(R.id.til_create_date);
        TextInputEditText timeEt        = view.findViewById(R.id.et_create_time);
        TextInputLayout   tilTime       = view.findViewById(R.id.til_create_time);
        AutoCompleteTextView venueAcv   = view.findViewById(R.id.acv_create_venue);
        TextInputEditText organizerEt   = view.findViewById(R.id.et_create_organizer);
        AutoCompleteTextView deptAcv    = view.findViewById(R.id.acv_create_department);
        ChipGroup audienceChipGroup     = view.findViewById(R.id.chip_group_audience);
        ChipGroup categoryChipGroup     = view.findViewById(R.id.chip_group_category);

        // --- Banner picker ---
        view.findViewById(R.id.btn_pick_banner).setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));

        // --- Date picker ---
        View.OnClickListener openDatePicker = v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(
                    requireContext(),
                    (picker, year, month, dayOfMonth) -> {
                        String formatted = String.format(Locale.getDefault(),
                                "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        dateEt.setText(formatted);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        };
        dateEt.setOnClickListener(openDatePicker);
        tilDate.setEndIconOnClickListener(openDatePicker);

        // --- Time picker ---
        View.OnClickListener openTimePicker = v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(
                    requireContext(),
                    (tp, hourOfDay, minute) -> {
                        String formatted = String.format(Locale.getDefault(),
                                "%02d:%02d", hourOfDay, minute);
                        timeEt.setText(formatted);
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    true
            ).show();
        };
        timeEt.setOnClickListener(openTimePicker);
        tilTime.setEndIconOnClickListener(openTimePicker);

        // --- Venue dropdown ---
        String[] venueNames = {
                "Gymnasium", "Auditorium", "Function Hall",
                "Basketball Court", "Open Grounds",
                "Conference Room A", "Conference Room B"
        };
        ArrayAdapter<String> venueAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, venueNames);
        venueAcv.setAdapter(venueAdapter);

        // --- Department dropdown ---
        String[] deptFullNames = requireContext().getResources()
                .getStringArray(R.array.departments_array);
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, deptFullNames);
        deptAcv.setAdapter(deptAdapter);

        // Pre-select the user's own department in the dropdown
        if (userDept != null && !userDept.isEmpty()) {
            deptAcv.setText(userDept, false);
        }

        // Auto-fill organizer with the logged-in officer's name (read-only)
        if (userName != null && !userName.isEmpty()) {
            organizerEt.setText(userName);
        }

        // --- Target Audience chips — pre-check user's department ---
        preselectAudienceChip(audienceChipGroup, userDept);

        // "Campus" chip logic: mutually exclusive with department chips
        // When Campus is checked, uncheck all department chips
        Chip chipCampus = view.findViewById(R.id.chip_dept_campus);
        chipCampus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setAllDeptChips(audienceChipGroup, false);
            }
        });

        // If any department chip is checked while Campus is checked, uncheck Campus
        int[] deptChipIds = {R.id.chip_dept_cba, R.id.chip_dept_ccje, R.id.chip_dept_coed,
                R.id.chip_dept_coe, R.id.chip_dept_col, R.id.chip_dept_clas, R.id.chip_dept_gs};
        for (int id : deptChipIds) {
            Chip c = view.findViewById(id);
            c.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && chipCampus.isChecked()) {
                    chipCampus.setChecked(false);
                }
            });
        }

        // --- Submit ---
        view.findViewById(R.id.btn_create_event_submit).setOnClickListener(v -> {
            String title    = titleEt.getText() != null ? titleEt.getText().toString().trim() : "";
            String desc     = descEt.getText()  != null ? descEt.getText().toString().trim()  : "";
            String date     = dateEt.getText()  != null ? dateEt.getText().toString().trim()  : "";
            String time     = timeEt.getText()  != null ? timeEt.getText().toString().trim()  : "";
            String venue    = venueAcv.getText().toString().trim();
            String organizer= organizerEt.getText() != null ? organizerEt.getText().toString().trim() : "";
            String dept     = deptAcv.getText().toString().trim();

            if (title.isEmpty() || desc.isEmpty() || date.isEmpty() || time.isEmpty() || venue.isEmpty() || dept.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Collect selected audience chips → store as tags e.g. "#CLAS #COE"
            List<String> selectedAudience = new ArrayList<>();
            for (int id : deptChipIds) {
                Chip c = audienceChipGroup.findViewById(id);
                if (c != null && c.isChecked()) {
                    selectedAudience.add("#" + c.getText().toString());
                }
            }
            if (chipCampus.isChecked()) {
                selectedAudience.clear();
                selectedAudience.add("#CAMPUS");
            }
            if (selectedAudience.isEmpty()) {
                Toast.makeText(getContext(), "Please select at least one target audience department", Toast.LENGTH_SHORT).show();
                return;
            }
            String tags = android.text.TextUtils.join(" ", selectedAudience);

            // Determine category
            String category = "Academic & Professional";
            int checkedId = categoryChipGroup.getCheckedChipId();
            if (checkedId != View.NO_ID) {
                Chip chip = categoryChipGroup.findViewById(checkedId);
                if (chip != null) category = chip.getText().toString();
            }

            // organizer field = "Organizer – Venue – Dept" so existing display code still shows meaningful text
            String organizerDisplay = organizer + " \u2013 " + venue + " \u2013 " + dept;

            String imagePath = selectedImageUri != null ? selectedImageUri.toString() : "";
            DatabaseHelper db = new DatabaseHelper(requireContext());
            Event newEvent = new Event(title, desc, date, time, tags, organizerDisplay, category, imagePath, "PENDING");
            newEvent.setCreatorSid(userStudentId);
            newEvent.setVenue(venue);
            if (db.addEvent(newEvent) != -1) {
                Toast.makeText(getContext(), "Event submitted for approval!", Toast.LENGTH_LONG).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                Toast.makeText(getContext(), "Failed to submit event. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    /**
     * Pre-checks the audience chip that matches the user's department.
     * Matches by abbreviation found in the full department name string.
     */
    private void preselectAudienceChip(ChipGroup group, String userDept) {
        if (userDept == null || userDept.isEmpty()) return;
        String upper = userDept.toUpperCase(Locale.getDefault());

        int[] chipIds  = {R.id.chip_dept_cba, R.id.chip_dept_ccje, R.id.chip_dept_coed,
                          R.id.chip_dept_coe, R.id.chip_dept_col, R.id.chip_dept_clas,
                          R.id.chip_dept_gs};
        String[] abbrs = {"CBA", "CCJE", "COED", "COE", "COL", "CLAS", "GS"};

        for (int i = 0; i < abbrs.length; i++) {
            if (upper.contains(abbrs[i])) {
                Chip c = group.findViewById(chipIds[i]);
                if (c != null) c.setChecked(true);
                return; // only pre-check the user's own dept
            }
        }
    }

    /**
     * Sets all individual department chips to the given checked state.
     */
    private void setAllDeptChips(ChipGroup group, boolean checked) {
        int[] chipIds = {R.id.chip_dept_cba, R.id.chip_dept_ccje, R.id.chip_dept_coed,
                         R.id.chip_dept_coe, R.id.chip_dept_col, R.id.chip_dept_clas,
                         R.id.chip_dept_gs};
        for (int id : chipIds) {
            Chip c = group.findViewById(id);
            if (c != null) c.setChecked(checked);
        }
    }
}
