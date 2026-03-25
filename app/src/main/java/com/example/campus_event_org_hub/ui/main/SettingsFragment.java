package com.example.campus_event_org_hub.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.CEOHApplication;
import com.example.campus_event_org_hub.R;

public class SettingsFragment extends Fragment {

    // Use constants from CEOHApplication — single source of truth.
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME  = "app_theme";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        RadioGroup rgTheme = view.findViewById(R.id.rg_theme);

        // Set initial selection based on saved preference
        int savedTheme = getSavedTheme();
        switch (savedTheme) {
            case CEOHApplication.THEME_LIGHT:
                rgTheme.check(R.id.rb_theme_light);
                break;
            case CEOHApplication.THEME_DARK:
                rgTheme.check(R.id.rb_theme_dark);
                break;
            default:
                rgTheme.check(R.id.rb_theme_system);
                break;
        }

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int theme;
            if (checkedId == R.id.rb_theme_light) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                theme = CEOHApplication.THEME_LIGHT;
            } else if (checkedId == R.id.rb_theme_dark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                theme = CEOHApplication.THEME_DARK;
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                theme = CEOHApplication.THEME_SYSTEM;
            }
            saveTheme(theme);
            // AppCompatDelegate.setDefaultNightMode() already triggers activity recreation
            // automatically — do NOT call getActivity().recreate() here, as that would
            // cause a double-recreate and wipe the fragment back stack.
        });

        return view;
    }

    private void saveTheme(int theme) {
        requireContext()
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit().putInt(KEY_THEME, theme).apply();
    }

    private int getSavedTheme() {
        return requireContext()
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getInt(KEY_THEME, CEOHApplication.THEME_SYSTEM);
    }
}
