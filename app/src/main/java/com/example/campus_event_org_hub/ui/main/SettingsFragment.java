package com.example.campus_event_org_hub.ui.main;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "app_theme";
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        RadioGroup rgTheme = view.findViewById(R.id.rg_theme);

        // Set initial selection based on saved preference
        int savedTheme = getSavedTheme();
        switch (savedTheme) {
            case THEME_LIGHT:
                rgTheme.check(R.id.rb_theme_light);
                break;
            case THEME_DARK:
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
                theme = THEME_LIGHT;
            } else if (checkedId == R.id.rb_theme_dark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                theme = THEME_DARK;
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                theme = THEME_SYSTEM;
            }
            saveTheme(theme);
            // Recreate activity to apply theme while staying on Settings
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });

        return view;
    }

    private void saveTheme(int theme) {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME, theme).apply();
    }

    private int getSavedTheme() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, THEME_SYSTEM);
    }
}
