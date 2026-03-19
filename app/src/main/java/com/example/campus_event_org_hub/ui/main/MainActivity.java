package com.example.campus_event_org_hub.ui.main;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.SyncManager;
import com.example.campus_event_org_hub.ui.events.EventsFragment;
import com.example.campus_event_org_hub.ui.main.MenuFragment;

public class MainActivity extends AppCompatActivity
        implements ProfileFragment.OnProfileUpdatedListener {

    private static final String TAG = "CEOH_MAIN";
    private String userName, userRole, userDept, userEmail, userStudentId;
    private String currentProfileImagePath = null;

    // ── Top bar ──────────────────────────────────────────────────────────────
    private TextView tvPageTitle;
    private ImageView ivNotifBell;
    private TextView tvNotifBadge;

    // ── Bottom nav tab views ─────────────────────────────────────────────────
    private LinearLayout tabDiscover, tabEvents, tabVenue, tabProfile;
    private ImageView   iconDiscover, iconEvents, iconVenue, iconProfile;
    private TextView    textDiscover, textEvents, textVenue, textProfile;

    private int currentTab = 0; // 0=Discover, 1=Events, 2=Venue, 3=Profile

    // ── Tab titles shown in the top bar ─────────────────────────────────────
    private static final String[] TAB_TITLES = { "Discover", "Events", "Venue", "Profile" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure content doesn't draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        try {
            setContentView(R.layout.activity_main);

            // Read user info from login intent
            if (getIntent() != null) {
                userName      = getIntent().getStringExtra("USER_NAME");
                userRole      = getIntent().getStringExtra("USER_ROLE");
                userDept      = getIntent().getStringExtra("USER_DEPT");
                userEmail     = getIntent().getStringExtra("USER_EMAIL");
                userStudentId = getIntent().getStringExtra("USER_STUDENT_ID");
            }
            if (userName      == null) userName      = "User";
            if (userRole      == null) userRole      = "Student";
            if (userDept      == null) userDept      = "General";
            if (userEmail     == null) userEmail     = "";
            if (userStudentId == null) userStudentId = "";

            // ── Bind top bar ─────────────────────────────────────────────────
            tvPageTitle  = findViewById(R.id.tv_page_title);
            ivNotifBell  = findViewById(R.id.iv_notif_bell);
            tvNotifBadge = findViewById(R.id.tv_notif_badge);

            ivNotifBell.setOnClickListener(v -> {
                // Load NotificationsFragment on bell tap (back-stackable)
                NotificationsFragment nf = new NotificationsFragment();
                loadFragment(nf, true, "Notifications");
            });

            // ── Bind bottom nav ──────────────────────────────────────────────
            tabDiscover = findViewById(R.id.tab_discover);
            tabEvents   = findViewById(R.id.tab_events);
            tabVenue    = findViewById(R.id.tab_venue);
            tabProfile  = findViewById(R.id.tab_profile);

            iconDiscover = findViewById(R.id.tab_discover_icon);
            iconEvents   = findViewById(R.id.tab_events_icon);
            iconVenue    = findViewById(R.id.tab_venue_icon);
            iconProfile  = findViewById(R.id.tab_profile_icon);

            textDiscover = findViewById(R.id.tab_discover_text);
            textEvents   = findViewById(R.id.tab_events_text);
            textVenue    = findViewById(R.id.tab_venue_text);
            textProfile  = findViewById(R.id.tab_profile_text);

            tabDiscover.setOnClickListener(v -> selectTab(0));
            tabEvents  .setOnClickListener(v -> selectTab(1));
            tabVenue   .setOnClickListener(v -> selectTab(2));
            tabProfile .setOnClickListener(v -> selectTab(3));

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
        SyncManager.sync(this, null);
        updateNotificationBadge();
    }

    // ── Notification badge ───────────────────────────────────────────────────

    /** Reads unread count from DB and shows/hides the top-bar badge. */
    public void updateNotificationBadge() {
        if (tvNotifBadge == null || userStudentId == null || userStudentId.isEmpty()) return;
        try {
            DatabaseHelper db = new DatabaseHelper(this);
            int unread = db.getUnreadNotificationCount(userStudentId);
            if (unread > 0) {
                tvNotifBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                tvNotifBadge.setVisibility(View.VISIBLE);
            } else {
                tvNotifBadge.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "updateNotificationBadge failed", e);
        }
    }

    /** Called by NotificationsFragment when "Clear All" is tapped. */
    public void clearNotificationBadge() {
        if (tvNotifBadge != null) tvNotifBadge.setVisibility(View.GONE);
    }

    // ── Tab selection ────────────────────────────────────────────────────────

    public void selectTab(int index) {
        selectTabWithArgs(index, null);
    }

    /**
     * Select a bottom-nav tab and optionally pass extra args to the fragment.
     * Called externally by DiscoverFragment quick-filter chips.
     */
    public void selectTabWithArgs(int index, Bundle extraArgs) {
        try {
            currentTab = index;

            // Reset all tabs
            setTabActive(iconDiscover, textDiscover, false);
            setTabActive(iconEvents,   textEvents,   false);
            setTabActive(iconVenue,    textVenue,    false);
            setTabActive(iconProfile,  textProfile,  false);

            Fragment fragment;
            switch (index) {
                case 1:
                    setTabActive(iconEvents, textEvents, true);
                    fragment = new EventsFragment();
                    break;
                case 2:
                    setTabActive(iconVenue, textVenue, true);
                    fragment = new VenueFragment();
                    break;
                case 3:
                    setTabActive(iconProfile, textProfile, true);
                    fragment = new MenuFragment();
                    break;
                case 0:
                default:
                    setTabActive(iconDiscover, textDiscover, true);
                    fragment = new DiscoverFragment();
                    break;
            }

            // Update top-bar title
            if (tvPageTitle != null && index >= 0 && index < TAB_TITLES.length) {
                tvPageTitle.setText(TAB_TITLES[index]);
            }

            // Merge extra args if provided
            if (extraArgs != null) {
                Bundle existing = fragment.getArguments();
                if (existing == null) existing = new Bundle();
                existing.putAll(extraArgs);
                fragment.setArguments(existing);
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
                ? ContextCompat.getColor(this, R.color.primary_green)
                : ContextCompat.getColor(this, R.color.text_hint);
        icon.setColorFilter(color);
        label.setTextColor(color);
    }

    // ── Toolbar title ──────────────────────────────────────────────────────

    public void setToolbarTitle(String title) {
        if (tvPageTitle != null) {
            tvPageTitle.setText(title);
        }
    }

    public void showNotificationBell(boolean show) {
        if (ivNotifBell != null) {
            ivNotifBell.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    // ── Fragment loading ─────────────────────────────────────────────────────

    public void loadFragment(Fragment fragment) {
        loadFragment(fragment, false, null);
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        loadFragment(fragment, addToBackStack, null);
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack, String title) {
        try {
            if (fragment == null) return;

            // Set custom title if provided
            if (title != null && !title.isEmpty()) {
                setToolbarTitle(title);
            }

            // Inject user info into every fragment
            Bundle args = fragment.getArguments();
            if (args == null) args = new Bundle();
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

        } catch (Exception e) {
            Log.e(TAG, "CRITICAL CRASH loading fragment", e);
            Toast.makeText(this, "Fragment Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── Profile picture callback ─────────────────────────────────────────────

    @Override
    public void onProfilePictureUpdated(String newImagePath) {
        currentProfileImagePath = newImagePath;
    }
}
