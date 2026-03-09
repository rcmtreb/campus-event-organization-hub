package com.example.campus_event_org_hub;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MenuFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        view.findViewById(R.id.menu_profile).setOnClickListener(v ->
                Toast.makeText(getContext(), "My Profile — coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.menu_my_events).setOnClickListener(v ->
                Toast.makeText(getContext(), "My Registered Events — coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.menu_notifications_settings).setOnClickListener(v ->
                Toast.makeText(getContext(), "Notification Preferences — coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.menu_dark_mode).setOnClickListener(v ->
                Toast.makeText(getContext(), "Appearance & Theme — coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.menu_privacy).setOnClickListener(v ->
                Toast.makeText(getContext(), "Privacy & Security — coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.menu_help).setOnClickListener(v ->
                Toast.makeText(getContext(), "Help & Support — coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.menu_about).setOnClickListener(v ->
                Toast.makeText(getContext(), "Campus Event Organization Hub v1.0", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }
}
