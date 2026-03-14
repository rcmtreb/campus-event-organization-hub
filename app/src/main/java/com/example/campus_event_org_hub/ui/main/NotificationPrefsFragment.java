package com.example.campus_event_org_hub.ui.main;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;

public class NotificationPrefsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification_prefs, container, false);

        Bundle args   = getArguments();
        String sid    = args != null ? args.getString("USER_STUDENT_ID", "") : "";

        ImageButton btnBack = view.findViewById(R.id.btn_back_notif);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        AutoCompleteTextView dropdown = view.findViewById(R.id.notif_pref_dropdown);
        String[] prefs = {"All Events", "My Department Only", "Registered Events Only", "None"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, prefs);
        dropdown.setAdapter(adapter);

        // Load current pref from DB
        if (!sid.isEmpty()) {
            DatabaseHelper db = new DatabaseHelper(requireContext());
            Cursor c = db.getUserByStudentId(sid);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(DatabaseHelper.COLUMN_USER_NOTIF_PREF);
                String saved = idx >= 0 ? c.getString(idx) : "All Events";
                if (saved == null || saved.isEmpty()) saved = "All Events";
                dropdown.setText(saved, false);
                c.close();
            }
        }

        view.findViewById(R.id.notif_save_btn).setOnClickListener(v -> {
            String selected = dropdown.getText().toString().trim();
            if (selected.isEmpty()) {
                Toast.makeText(getContext(), "Please select a preference.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (sid.isEmpty()) {
                Toast.makeText(getContext(), "Cannot save: no student ID.", Toast.LENGTH_SHORT).show();
                return;
            }
            DatabaseHelper db = new DatabaseHelper(requireContext());
            db.updateNotifPref(sid, selected);
            Toast.makeText(getContext(), "Preferences saved!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}
