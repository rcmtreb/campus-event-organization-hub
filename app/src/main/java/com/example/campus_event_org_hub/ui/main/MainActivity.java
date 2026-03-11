package com.example.campus_event_org_hub.ui.main;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.ui.events.EventsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private String userName, userRole, userDept;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userName = getIntent().getStringExtra("USER_NAME");
        userRole = getIntent().getStringExtra("USER_ROLE");
        userDept = getIntent().getStringExtra("USER_DEPT");

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        if (savedInstanceState == null) {
            loadFragment(new EventsFragment());
        }
    }

    public void loadFragment(Fragment fragment) {
        if (fragment != null) {
            Bundle args = new Bundle();
            args.putString("USER_NAME", userName);
            args.putString("USER_ROLE", userRole);
            args.putString("USER_DEPT", userDept);
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;

                    int itemId = item.getItemId();
                    if (itemId == R.id.nav_events) {
                        selectedFragment = new EventsFragment();
                    } else if (itemId == R.id.nav_notifications) {
                        selectedFragment = new NotificationsFragment();
                    } else if (itemId == R.id.nav_menu) {
                        selectedFragment = new MenuFragment();
                    }

                    if (selectedFragment != null) {
                        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        loadFragment(selectedFragment);
                    }
                    return true;
                }
            };
}
