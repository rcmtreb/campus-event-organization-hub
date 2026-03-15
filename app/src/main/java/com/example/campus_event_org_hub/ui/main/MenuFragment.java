package com.example.campus_event_org_hub.ui.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.ui.auth.LoginActivity;
import com.example.campus_event_org_hub.util.ImageUtils;
import com.google.android.material.imageview.ShapeableImageView;

public class MenuFragment extends Fragment {

    private ShapeableImageView avatarView;
    private String studentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        Bundle args = getArguments();
        String name      = args != null ? args.getString("USER_NAME",       "UCC Student") : "UCC Student";
        String role      = args != null ? args.getString("USER_ROLE",       "Student")     : "Student";
        String dept      = args != null ? args.getString("USER_DEPT",       "General")     : "General";
        studentId        = args != null ? args.getString("USER_STUDENT_ID", "")            : "";

        TextView tvUserName = view.findViewById(R.id.tv_menu_user_name);
        TextView tvUserRole = view.findViewById(R.id.tv_menu_user_role);
        LinearLayout sectionOfficerTools = view.findViewById(R.id.section_officer_tools);

        tvUserName.setText(name);
        tvUserRole.setText(role + " \u2022 " + dept);

        if ("Officer".equalsIgnoreCase(role)) {
            sectionOfficerTools.setVisibility(View.VISIBLE);
        } else {
            sectionOfficerTools.setVisibility(View.GONE);
        }

        // Load avatar from DB
        avatarView = view.findViewById(R.id.menu_avatar);
        if (avatarView != null && !studentId.isEmpty()) {
            DatabaseHelper db = new DatabaseHelper(requireContext());
            Cursor c = db.getUserByStudentId(studentId);
            if (c != null && c.moveToFirst()) {
                int imgIdx = c.getColumnIndex(DatabaseHelper.COLUMN_USER_PROFILE_IMG);
                String imgPath = imgIdx >= 0 ? c.getString(imgIdx) : null;
                if (imgPath != null && !imgPath.isEmpty()) {
                    loadAvatarInto(avatarView, imgPath);
                }
                c.close();
            }
        }

        // Officer tools
        view.findViewById(R.id.menu_officer_my_events).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new OfficerMyEventsFragment(), true);
            }
        });
        view.findViewById(R.id.menu_officer_pending_events).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new OfficerPendingEventsFragment(), true);
            }
        });
        view.findViewById(R.id.menu_officer_analytics).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new OfficerAnalyticsFragment(), true);
            }
        });

        // Account
        view.findViewById(R.id.menu_profile).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new ProfileFragment(), true);
            }
        });
        view.findViewById(R.id.menu_my_events).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new RegisteredEventsFragment(), true);
            }
        });

        // Settings
        view.findViewById(R.id.menu_notifications_settings).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new NotificationPrefsFragment(), true);
            }
        });
        view.findViewById(R.id.menu_dark_mode).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new SettingsFragment(), true);
            }
        });
        view.findViewById(R.id.menu_privacy).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new PrivacyFragment(), true);
            }
        });
        view.findViewById(R.id.menu_security).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new SecurityFragment(), true);
            }
        });

        // Help & Support dialog
        view.findViewById(R.id.menu_help).setOnClickListener(v -> showHelpDialog());

        // About CEOH dialog (includes privacy policy)
        view.findViewById(R.id.menu_about).setOnClickListener(v -> showAboutDialog());

        // Logout
        view.findViewById(R.id.menu_logout).setOnClickListener(v -> confirmLogout());

        return view;
    }

    private void loadAvatarInto(ShapeableImageView imageView, String path) {
        ImageUtils.load(requireContext(), imageView, path, R.drawable.ic_image_placeholder);
        imageView.clearColorFilter();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload avatar from DB in case it was updated while this fragment was in the back stack
        if (avatarView != null && studentId != null && !studentId.isEmpty()) {
            DatabaseHelper db = new DatabaseHelper(requireContext());
            Cursor c = db.getUserByStudentId(studentId);
            if (c != null && c.moveToFirst()) {
                int imgIdx = c.getColumnIndex(DatabaseHelper.COLUMN_USER_PROFILE_IMG);
                String imgPath = imgIdx >= 0 ? c.getString(imgIdx) : null;
                if (imgPath != null && !imgPath.isEmpty()) {
                    loadAvatarInto(avatarView, imgPath);
                }
                c.close();
            }
        }
    }

    /** Called by MainActivity when the user saves a new profile picture in ProfileFragment. */
    public void refreshAvatar(String newImagePath) {
        if (avatarView != null && newImagePath != null) {
            loadAvatarInto(avatarView, newImagePath);
        }
    }

    // ─── Dialogs ────────────────────────────────────────────────────────────────

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("About CEOH")
                .setMessage(
                        "Campus Event Organization Hub (CEOH)\n" +
                        "Version 1.0\n\n" +
                        "CEOH is the official event management platform of the University of the Cordilleras (UCC). " +
                        "It connects students, officers, and faculty by providing a centralized space to discover, " +
                        "register for, and organize campus events.\n\n" +
                        "Features:\n" +
                        "  \u2022 Browse and search approved campus events\n" +
                        "  \u2022 Officer tools for submitting and managing events\n" +
                        "  \u2022 Admin dashboard for approvals and user management\n" +
                        "  \u2022 Department-based event categorization\n\n" +
                        "Developed by the BSCS students of UCC.\n\n" +
                        "\u00a9 2026 University of the Cordilleras. All rights reserved.\n\n" +
                        "\u2015\u2015\u2015\u2015\u2015\u2015\u2015\u2015\u2015\u2015\u2015\u2015\u2015\u2015\u2015\n\n" +
                        "Privacy Policy\n\n" +
                        "CEOH is committed to protecting your personal information. " +
                        "Below is a summary of how your data is handled.\n\n" +
                        "Data We Collect:\n" +
                        "  \u2022 Full name, student ID, email address\n" +
                        "  \u2022 Department and role (Student / Officer)\n" +
                        "  \u2022 Events you register for or create\n\n" +
                        "How We Use Your Data:\n" +
                        "  \u2022 To authenticate your account\n" +
                        "  \u2022 To display your profile and event history\n" +
                        "  \u2022 To notify you of upcoming events (if enabled)\n\n" +
                        "Data Storage:\n" +
                        "All data is stored locally on your device using a secure SQLite database. " +
                        "No data is transmitted to external servers.\n\n" +
                        "For privacy concerns, contact: privacy@ucc.edu.ph\n\n" +
                        "Last updated: March 2026"
                )
                .setPositiveButton("Close", null)
                .show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Help & Support")
                .setMessage(
                        "Frequently Asked Questions\n\n" +
                        "Q: How do I register for an event?\n" +
                        "A: Open any event and tap the \"Register for Event\" button at the bottom of the event details page.\n\n" +
                        "Q: How do I create an event?\n" +
                        "A: Officer accounts can tap the \u002B button on the Events screen to submit a new event. " +
                        "Events are reviewed by an Admin before becoming visible to all students.\n\n" +
                        "Q: Why can't I see my submitted event?\n" +
                        "A: Submitted events are set to PENDING until an Admin approves them. " +
                        "Contact your college admin to expedite the review.\n\n" +
                        "Q: How do I become an Officer?\n" +
                        "A: Request a role change from the Admin via User Management, or register with an Officer role.\n\n" +
                        "Q: My data disappeared after logging out \u2014 is that normal?\n" +
                        "A: No. All events and accounts are stored in the local database and persist across sessions. " +
                        "If data is missing, the app database may have been reset. Contact support.\n\n" +
                        "Need more help?\n" +
                        "Email: ceoh-support@ucc.edu.ph\n" +
                        "Office: BSCS Department, Room 301, Main Building\n" +
                        "Hours: Mon\u2013Fri, 8:00 AM \u2013 5:00 PM"
                )
                .setPositiveButton("Got it", null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out? Your data will be saved.")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
