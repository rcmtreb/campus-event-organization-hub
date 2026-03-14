package com.example.campus_event_org_hub.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.util.ImageUtils;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ProfileFragment extends Fragment {

    /** Implemented by MainActivity to receive avatar-change notifications. */
    public interface OnProfileUpdatedListener {
        void onProfilePictureUpdated(String newImagePath);
    }

    private static final int PICK_IMAGE_REQUEST = 101;

    private String studentId, email, dept, name, role;
    private String selectedImagePath = null;
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
        TextView tvName     = view.findViewById(R.id.profile_name);
        TextView tvRoleBadge = view.findViewById(R.id.profile_role_badge);
        TextView tvSid      = view.findViewById(R.id.profile_student_id);
        TextView tvEmail    = view.findViewById(R.id.profile_email);
        TextView tvDept     = view.findViewById(R.id.profile_dept);

        tvName.setText(name);
        tvRoleBadge.setText(role);
        tvSid.setText(studentId.isEmpty() ? "\u2014" : studentId);
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
            DatabaseHelper db = new DatabaseHelper(requireContext());
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
                    selectedImagePath = savedImg;
                    loadAvatarFromPath(savedImg);
                }
                c.close();
            }
        }

        // Tap avatar to pick image
        view.findViewById(R.id.profile_avatar_container).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // Save button
        view.findViewById(R.id.profile_save_btn).setOnClickListener(v -> {
            String gender = genderDropdown.getText().toString().trim();
            String mobile = mobileField.getText() != null ? mobileField.getText().toString().trim() : "";

            if (studentId.isEmpty()) {
                Toast.makeText(getContext(), "Cannot save: no student ID found.", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseHelper db = new DatabaseHelper(requireContext());
            boolean ok = db.updateUserProfile(studentId, gender, mobile, selectedImagePath);
            Toast.makeText(getContext(), ok ? "Profile saved!" : "Save failed.", Toast.LENGTH_SHORT).show();
            if (ok && profileUpdatedListener != null) {
                profileUpdatedListener.onProfilePictureUpdated(selectedImagePath);
            }
        });

        // Back button
        ImageButton btnBack = view.findViewById(R.id.btn_back_profile);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
            });
        }

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            String internalPath = copyImageToInternal(uri);
            if (internalPath != null) {
                selectedImagePath = internalPath;
                loadAvatarFromPath(internalPath);
            } else {
                Toast.makeText(getContext(), "Could not load image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Copy picked image to internal storage and return the file path, or null on failure. */
    private String copyImageToInternal(Uri uri) {
        try {
            File dir = new File(requireContext().getFilesDir(), "profile_pics");
            if (!dir.exists()) dir.mkdirs();
            // Sanitize studentId for use as filename
            String safeId = studentId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            File dest = new File(dir, safeId + ".jpg");
            InputStream in = requireContext().getContentResolver().openInputStream(uri);
            if (in == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(in);
            in.close();
            if (bmp == null) return null;
            OutputStream out = new FileOutputStream(dest);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            return dest.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    /** Load avatar from an internal file path (absolute) or content URI string. */
    private void loadAvatarFromPath(String path) {
        ImageUtils.load(requireContext(), avatarView, path, R.drawable.ic_image_placeholder);
        avatarView.setPadding(0, 0, 0, 0);
    }
}
