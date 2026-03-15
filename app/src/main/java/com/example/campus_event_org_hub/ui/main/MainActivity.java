package com.example.campus_event_org_hub.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.SyncManager;
import com.example.campus_event_org_hub.ui.events.EventsFragment;

public class MainActivity extends AppCompatActivity
        implements ProfileFragment.OnProfileUpdatedListener {

    private static final String TAG = "CEOH_MAIN";
    private String userName, userRole, userDept, userEmail, userStudentId;
    private String currentProfileImagePath = null;

    // Custom nav tab views
    private LinearLayout tabEvents, tabNotifications, tabMenu;
    private ImageView iconEvents, iconNotifications, iconMenu;
    private TextView textEvents, textNotifications, textMenu;
    private TextView badgeNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);

            if (getIntent() != null) {
                userName      = getIntent().getStringExtra("USER_NAME");
                userRole      = getIntent().getStringExtra("USER_ROLE");
                userDept      = getIntent().getStringExtra("USER_DEPT");
                userEmail     = getIntent().getStringExtra("USER_EMAIL");
                userStudentId = getIntent().getStringExtra("USER_STUDENT_ID");
            }

            // Defaults to prevent crashes if extras are missing
            if (userName      == null) userName      = "User";
            if (userRole      == null) userRole      = "Student";
            if (userDept      == null) userDept      = "General";
            if (userEmail     == null) userEmail     = "";
            if (userStudentId == null) userStudentId = "";

            // Bind custom nav tab views
            tabEvents        = findViewById(R.id.tab_events);
            tabNotifications = findViewById(R.id.tab_notifications);
            tabMenu          = findViewById(R.id.tab_menu);

            iconEvents        = findViewById(R.id.tab_events_icon);
            iconNotifications = findViewById(R.id.tab_notifications_icon);
            iconMenu          = findViewById(R.id.tab_menu_icon);

            textEvents        = findViewById(R.id.tab_events_text);
            textNotifications = findViewById(R.id.tab_notifications_text);
            textMenu          = findViewById(R.id.tab_menu_text);

            badgeNotifications = findViewById(R.id.tab_notifications_badge);

            // Wire up click listeners
            tabEvents.setOnClickListener(v -> selectTab(0));
            tabNotifications.setOnClickListener(v -> selectTab(1));
            tabMenu.setOnClickListener(v -> selectTab(2));

            // Load default tab
            if (savedInstanceState == null) {
                selectTab(0);
            }

        } catch (Exception e) {
            Log.e(TAG, "CRITICAL CRASH in MainActivity onCreate", e);
            Toast.makeText(this, "App Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Background sync so data stays fresh on every return to the app.
        // No loading indicator here — data updates silently.
        SyncManager.sync(this, null);
        // Refresh unread notification badge
        updateNotificationBadge();
    }

    /** Reads unread notification count from DB and shows/hides the red badge. */
    public void updateNotificationBadge() {
        if (badgeNotifications == null || userStudentId == null || userStudentId.isEmpty()) return;
        try {
            DatabaseHelper db = new DatabaseHelper(this);
            int unread = db.getUnreadNotificationCount(userStudentId);
            if (unread > 0) {
                badgeNotifications.setText(unread > 99 ? "99+" : String.valueOf(unread));
                badgeNotifications.setVisibility(View.VISIBLE);
            } else {
                badgeNotifications.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "updateNotificationBadge failed", e);
        }
    }

    /** Called by NotificationsFragment when "Clear All" is tapped — hides nav badge immediately. */
    public void clearNotificationBadge() {
        if (badgeNotifications != null) badgeNotifications.setVisibility(View.GONE);
    }

    private void selectTab(int index) {
        try {
            // Reset all tabs to inactive appearance
            setTabActive(iconEvents,        textEvents,        false);
            setTabActive(iconNotifications, textNotifications, false);
            setTabActive(iconMenu,          textMenu,          false);

            // Activate selected tab and load its fragment
            Fragment fragment;
            switch (index) {
                case 1:
                    setTabActive(iconNotifications, textNotifications, true);
                    fragment = new NotificationsFragment();
                    break;
                case 2:
                    setTabActive(iconMenu, textMenu, true);
                    fragment = new MenuFragment();
                    break;
                case 0:
                default:
                    setTabActive(iconEvents, textEvents, true);
                    fragment = new EventsFragment();
                    break;
            }

            getSupportFragmentManager().popBackStack(null,
                    androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            loadFragment(fragment);

        } catch (Exception e) {
            Log.e(TAG, "CRASH in selectTab", e);
        }
    }

    private void setTabActive(ImageView icon, TextView label, boolean active) {
        int color = active
                ? ContextCompat.getColor(this, R.color.primary_blue)
                : ContextCompat.getColor(this, R.color.text_hint);
        icon.setColorFilter(color);
        label.setTextColor(color);
    }

    public void loadFragment(Fragment fragment) {
        loadFragment(fragment, false);
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        try {
            if (fragment != null) {
                Bundle args = new Bundle();
                args.putString("USER_NAME",       userName);
                args.putString("USER_ROLE",       userRole);
                args.putString("USER_DEPT",       userDept);
                args.putString("USER_EMAIL",      userEmail);
                args.putString("USER_STUDENT_ID", userStudentId);
                fragment.setArguments(args);

                androidx.fragment.app.FragmentTransaction tx =
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, fragment);
                if (addToBackStack) tx.addToBackStack(null);
                tx.commit();
            }
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL CRASH loading fragment", e);
            Toast.makeText(this, "Fragment Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── Profile picture callback ─────────────────────────────────────────────

    @Override
    public void onProfilePictureUpdated(String newImagePath) {
        currentProfileImagePath = newImagePath;
        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        if (current instanceof MenuFragment) {
            ((MenuFragment) current).refreshAvatar(newImagePath);
        }
    }
}
