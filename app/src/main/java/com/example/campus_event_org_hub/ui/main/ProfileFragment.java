package com.example.campus_event_org_hub.ui.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import com.example.campus_event_org_hub.util.ImageUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;

public class ProfileFragment extends Fragment {

    /** Implemented by MainActivity to receive avatar-change notifications. */
    public interface OnProfileUpdatedListener {
        void onProfilePictureUpdated(String newImagePath);
    }

    private static final int PICK_IMAGE_REQUEST = 101;

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
                int gIdx   = c.getColumnIndex(DatabaseHelper.COLUMN_USER_GENDER);
                int mIdx   = c.getColumnIndex(DatabaseHelper.COLUMN_USER_MOBILE);
                int imgIdx = c.getColumnIndex(DatabaseHelper.COLUMN_USER_PROFILE_IMG);

                String savedGender = gIdx  >= 0 ? c.getString(gIdx)  : "";
                String savedMobile = mIdx  >= 0 ? c.getString(mIdx)  : "";
                String savedImg    = imgIdx >= 0 ? c.getString(imgIdx) : "";

                if (savedGender != null && !savedGender.isEmpty()) genderDropdown.setText(savedGender, false);
                if (savedMobile != null && !savedMobile.isEmpty()) mobileField.setText(savedMobile);
                if (savedImg != null && !savedImg.isEmpty()) {
                    // Accept device-independent formats directly.
                    // For legacy local paths, only use them if the file still exists on this device.
                    if (savedImg.startsWith("data:image/")
                            || savedImg.startsWith("http://")
                            || savedImg.startsWith("https://")) {
                        selectedImagePath = savedImg;
                    } else if (new File(savedImg).exists()) {
                        selectedImagePath = savedImg;
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

            // If imageDeleted is true, pass "" to explicitly clear DB column.
            // If selectedImagePath is non-null, pass it to update.
            // If neither (no change), pass null which skips the column update.
            String imgPathForDb = imageDeleted ? "" : selectedImagePath;

            DatabaseHelper db = DatabaseHelper.getInstance(requireContext());
            boolean ok = db.updateUserProfile(studentId, gender, mobile, imgPathForDb);
            Toast.makeText(getContext(), ok ? "Profile saved!" : "Save failed.", Toast.LENGTH_SHORT).show();
            if (ok) {
                imageDeleted = false; // reset flag after successful save
                if (profileUpdatedListener != null) {
                    profileUpdatedListener.onProfilePictureUpdated(imgPathForDb);
                }
            }
        });

        return view;
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
                        // which == 1 → Delete Photo (only reachable when hasPhoto == true)
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
        // Reset avatar to placeholder (null path → loadAvatar shows placeholder)
        loadAvatarFromPath(null);
        Toast.makeText(getContext(), "Photo removed. Tap Save to confirm.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            // Show a local preview immediately while encoding happens in the background
            loadAvatarFromPath(uri.toString());
            // Encode to Base64 so the image works on any device
            Toast.makeText(getContext(), "Processing photo...", Toast.LENGTH_SHORT).show();
            new FirebaseStorageHelper().uploadProfilePhoto(requireContext(), uri, studentId,
                    new FirebaseStorageHelper.UploadCallback() {
                        @Override
                        public void onSuccess(String downloadUrl) {
                            if (getContext() == null) return;
                            selectedImagePath = downloadUrl;
                            imageDeleted = false;
                            // Refresh avatar with the Base64 data-URI
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
