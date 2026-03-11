package com.example.campus_event_org_hub.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.ui.auth.LoginActivity;

public class MenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        Bundle args = getArguments();
        String name = args != null ? args.getString("USER_NAME", "UCC Student") : "UCC Student";
        String role = args != null ? args.getString("USER_ROLE", "Student") : "Student";
        String dept = args != null ? args.getString("USER_DEPT", "General") : "General";

        TextView tvUserName = view.findViewById(R.id.tv_menu_user_name);
        TextView tvUserRole = view.findViewById(R.id.tv_menu_user_role);
        LinearLayout sectionOfficerTools = view.findViewById(R.id.section_officer_tools);

        tvUserName.setText(name);
        tvUserRole.setText(role + " | " + dept);

        if ("Officer".equalsIgnoreCase(role)) {
            sectionOfficerTools.setVisibility(View.VISIBLE);
        } else {
            sectionOfficerTools.setVisibility(View.GONE);
        }

        // Appearance & Theme Click
        view.findViewById(R.id.menu_dark_mode).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new SettingsFragment());
            }
        });

        view.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}
