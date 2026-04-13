package com.example.campus_event_org_hub.ui.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.FirebaseStorageHelper;
import com.example.campus_event_org_hub.model.Course;
import com.example.campus_event_org_hub.util.ImageUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class ProfileFragment extends Fragment {

    /** Implemented by MainActivity to receive avatar-change notifications. */
    public interface OnProfileUpdatedListener {
        void onProfilePictureUpdated(String newImagePath);
    }

    private static final int PICK_IMAGE_REQUEST = 101;
    private static final String PREFS_LEGACY = "ceoh_legacy_prompt";
    private static final String KEY_LEGACY_PROMPTED = "legacy_prompted_";

    private String studentId, email, dept, name, role;
    private String selectedImagePath = null;
    /** True when the user has explicitly deleted their photo this session (before saving). */
    private boolean imageDeleted = false;
    private ImageView avatarView;
    private OnProfileUpdatedListener profileUpdatedListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnProfileUpdatedListener) {
            profileUpdatedListener = (OnProfileUpdatedListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        profileUpdatedListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        Bundle args = getArguments();
        name      = args != null ? args.getString("USER_NAME",       "User")    : "User";
        role      = args != null ? args.getString("USER_ROLE",       "Student") : "Student";
        dept      = args != null ? args.getString("USER_DEPT",       "General") : "General";
        email     = args != null ? args.getString("USER_EMAIL",      "")        : "";
        studentId = args != null ? args.getString("USER_STUDENT_ID", "")        : "";

        avatarView = view.findViewById(R.id.profile_avatar);
        TextView tvName      = view.findViewById(R.id.profile_name);
        TextView tvRoleBadge = view.findViewById(R.id.profile_role_badge);
        TextView tvSid       = view.findViewById(R.id.profile_student_id);
        TextView tvEmail     = view.findViewById(R.id.profile_email);
        TextView tvDept      = view.findViewById(R.id.profile_dept);

        // Academic info views
        MaterialCardView cardAcademic   = view.findViewById(R.id.card_academic_info);
        TextView tvYearLevel            = view.findViewById(R.id.profile_year_level);
        TextView tvSection              = view.findViewById(R.id.profile_section);
        TextView tvCourse               = view.findViewById(R.id.profile_course);
        TextView tvStudentStatus        = view.findViewById(R.id.profile_student_status);
        TextView tvCompleteNudge        = view.findViewById(R.id.profile_complete_nudge);

        tvName.setText(name);
        tvRoleBadge.setText(role);
        tvSid.setText(studentId.isEmpty() ? "\u2014" : studentId + "-S");
        tvEmail.setText(email.isEmpty() ? "\u2014" : email);
        tvDept.setText(dept.isEmpty() ? "\u2014" : dept);

        // Gender dropdown
        AutoCompleteTextView genderDropdown = view.findViewById(R.id.profile_gender);
        String[] genderOptions = {"Male", "Female", "Prefer not to say"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, genderOptions);
        genderDropdown.setAdapter(genderAdapter);

        TextInputEditText mobileField = view.findViewById(R.id.profile_mobile);

        // Load existing profile data from DB
        if (!studentId.isEmpty()) {
            DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
            Cursor c = db.getUserByStudentId(studentId);
            if (c != null && c.moveToFirst()) {
                int gIdx      = c.getColumnIndex(DatabaseHelper.COLUMN_USER_GENDER);
                int mIdx      = c.getColumnIndex(DatabaseHelper.COLUMN_USER_MOBILE);
                int imgIdx    = c.getColumnIndex(DatabaseHelper.COLUMN_USER_PROFILE_IMG);
                int yrIdx     = c.getColumnIndex(DatabaseHelper.COLUMN_USER_YEAR_LEVEL);
                int secIdx    = c.getColumnIndex(DatabaseHelper.COLUMN_USER_SECTION);
                int courseIdx = c.getColumnIndex(DatabaseHelper.COLUMN_USER_COURSE_ID);
                int statIdx   = c.getColumnIndex(DatabaseHelper.COLUMN_USER_STUDENT_STATUS);

                String savedGender = gIdx   >= 0 ? c.getString(gIdx)   : "";
                String savedMobile = mIdx   >= 0 ? c.getString(mIdx)   : "";
                String savedImg    = imgIdx >= 0 ? c.getString(imgIdx)  : "";

                if (savedGender != null && !savedGender.isEmpty()) genderDropdown.setText(savedGender, false);
                if (savedMobile != null && !savedMobile.isEmpty()) mobileField.setText(savedMobile);
                if (savedImg != null && !savedImg.isEmpty()) {
                    if (savedImg.startsWith("data:image/")
                            || savedImg.startsWith("http://")
                            || savedImg.startsWith("https://")) {
                        selectedImagePath = savedImg;
                    } else if (new File(savedImg).exists()) {
                        selectedImagePath = savedImg;
                    }
                }

                // ── Academic info card (students only) ────────────────────────
                if ("Student".equals(role)) {
                    cardAcademic.setVisibility(View.VISIBLE);

                    int yearLevel = yrIdx >= 0 ? c.getInt(yrIdx) : 0;
                    String section    = secIdx    >= 0 ? c.getString(secIdx)    : "";
                    int courseId      = courseIdx >= 0 ? c.getInt(courseIdx)    : 0;
                    String status     = statIdx   >= 0 ? c.getString(statIdx)   : "";

                    tvYearLevel.setText(yearLevel > 0 ? formatOrdinalYear(yearLevel) : "\u2014");
                    tvSection.setText(section != null && !section.isEmpty() ? section : "\u2014");
                    tvStudentStatus.setText(status != null && !status.isEmpty() ? status : "\u2014");

                    // Resolve course name from id
                    boolean hasCourse = courseId > 0;
                    if (hasCourse) {
                        Course course = db.getCourseById(courseId);
                        tvCourse.setText(course != null ? course.getCourseCode() : "\u2014");
                    } else {
                        tvCourse.setText("\u2014");
                    }

                    // Show "Complete profile" nudge if course is not yet set
                    boolean needsLegacyPrompt = !hasCourse && !hasBeenLegacyPrompted();
                    if (!hasCourse) {
                        tvCompleteNudge.setVisibility(View.VISIBLE);
                        tvCompleteNudge.setOnClickListener(nv -> showLegacyPrompt(db));
                    }

                    // Auto-show legacy dialog once per student
                    if (needsLegacyPrompt) {
                        markLegacyPrompted();
                        view.post(() -> showLegacyPrompt(db));
                    }
                }

                c.close();
            }
        }
        // Always apply avatar (photo or placeholder) so scaleType/tint/padding are correct
        loadAvatarFromPath(selectedImagePath);

        // Tap avatar to show Upload / Delete dialog
        view.findViewById(R.id.profile_avatar_container).setOnClickListener(v ->
                showAvatarOptionsDialog());

        // Save button
        view.findViewById(R.id.profile_save_btn).setOnClickListener(v -> {
            String gender = genderDropdown.getText().toString().trim();
            String mobile = mobileField.getText() != null ? mobileField.getText().toString().trim() : "";

            if (studentId.isEmpty()) {
                Toast.makeText(getContext(), "Cannot save: no student ID found.", Toast.LENGTH_SHORT).show();
                return;
            }

            String imgPathForDb = imageDeleted ? "" : selectedImagePath;

            DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
            boolean ok = db.updateUserProfile(studentId, gender, mobile, imgPathForDb);
            Toast.makeText(getContext(), ok ? "Profile saved!" : "Save failed.", Toast.LENGTH_SHORT).show();
            if (ok) {
                imageDeleted = false;
                if (profileUpdatedListener != null) {
                    profileUpdatedListener.onProfilePictureUpdated(imgPathForDb);
                }
            }
        });

        return view;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /** Convert int year level 1-4 to "1st Year", "2nd Year", "3rd Year", "4th Year". */
    private static String formatOrdinalYear(int year) {
        switch (year) {
            case 1: return "1st Year";
            case 2: return "2nd Year";
            case 3: return "3rd Year";
            case 4: return "4th Year";
            default: return year + "th Year";
        }
    }

    // ── Legacy prompt (one-time dialog for students missing course/section) ────

    private boolean hasBeenLegacyPrompted() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_LEGACY, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LEGACY_PROMPTED + studentId, false);
    }

    private void markLegacyPrompted() {
        requireContext().getSharedPreferences(PREFS_LEGACY, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_LEGACY_PROMPTED + studentId, true).apply();
    }

    private void showLegacyPrompt(DatabaseHelper db) {
        if (getParentFragmentManager() == null) return;
        LegacyStudentPromptDialog dialog = LegacyStudentPromptDialog.newInstance(studentId);
        dialog.setOnSavedListener((courseId, yearLevel, section) -> {
            Course course = db.getCourseById(courseId);
            View v = getView();
            if (v == null) return;
            TextView tvCourse    = v.findViewById(R.id.profile_course);
            TextView tvYearLevel = v.findViewById(R.id.profile_year_level);
            TextView tvSection  = v.findViewById(R.id.profile_section);
            TextView nudge      = v.findViewById(R.id.profile_complete_nudge);
            if (course != null) tvCourse.setText(course.getCourseCode());
            tvYearLevel.setText(formatOrdinalYear(yearLevel));
            if (section != null && !section.isEmpty()) tvSection.setText(section);
            nudge.setVisibility(View.GONE);
        });
        dialog.show(getParentFragmentManager(), "LegacyPrompt");
    }

    // ── Avatar options dialog ─────────────────────────────────────────────────

    private void showAvatarOptionsDialog() {
        boolean hasPhoto = (selectedImagePath != null && !selectedImagePath.isEmpty());

        String[] options = hasPhoto
                ? new String[]{"Upload Photo", "Delete Photo"}
                : new String[]{"Upload Photo"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Profile Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickImageFromGallery();
                    } else {
                        deleteProfileImage();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void deleteProfileImage() {
        selectedImagePath = null;
        imageDeleted = true;
        loadAvatarFromPath(null);
        Toast.makeText(getContext(), "Photo removed. Tap Save to confirm.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            loadAvatarFromPath(uri.toString());
            Toast.makeText(getContext(), "Processing photo...", Toast.LENGTH_SHORT).show();
            new FirebaseStorageHelper().uploadProfilePhoto(requireContext(), uri, studentId,
                    new FirebaseStorageHelper.UploadCallback() {
                        @Override
                        public void onSuccess(String downloadUrl) {
                            if (getContext() == null) return;
                            selectedImagePath = downloadUrl;
                            imageDeleted = false;
                            loadAvatarFromPath(downloadUrl);
                            Toast.makeText(getContext(), "Photo ready.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            if (getContext() == null) return;
                            Toast.makeText(getContext(),
                                    "Photo processing failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    /** Load avatar from an https:// URL, absolute file path, or content URI string. */
    private void loadAvatarFromPath(String path) {
        int paddingPx = dpToPx(10);
        ImageUtils.loadAvatar(requireContext(), avatarView, path,
                R.color.text_on_primary, paddingPx);
    }

    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
