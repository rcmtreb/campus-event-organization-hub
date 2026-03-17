package com.example.campus_event_org_hub.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.data.FirestoreHelper;
import com.google.android.material.textfield.TextInputEditText;

public class SecurityFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security, container, false);

        Bundle args   = getArguments();
        String sid    = args != null ? args.getString("USER_STUDENT_ID", "") : "";

        ImageButton btnBack = view.findViewById(R.id.btn_back_security);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        TextInputEditText etCurrentPw = view.findViewById(R.id.security_current_pw);
        TextInputEditText etNewPw     = view.findViewById(R.id.security_new_pw);
        TextInputEditText etConfirmPw = view.findViewById(R.id.security_confirm_pw);

        view.findViewById(R.id.security_save_btn).setOnClickListener(v -> {
            String currentPw = etCurrentPw.getText() != null ? etCurrentPw.getText().toString() : "";
            String newPw     = etNewPw.getText()     != null ? etNewPw.getText().toString()     : "";
            String confirmPw = etConfirmPw.getText() != null ? etConfirmPw.getText().toString() : "";

            if (currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPw.equals(confirmPw)) {
                Toast.makeText(getContext(), "New passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPw.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (sid.isEmpty()) {
                Toast.makeText(getContext(), "Cannot update: no student ID.", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseHelper db = new DatabaseHelper(requireContext());
            boolean ok = db.changePassword(sid, currentPw, newPw);
            if (ok) {
                // Sync new password to Firestore so other devices pick it up on next login
                new FirestoreHelper().updatePassword(sid, newPw);
                Toast.makeText(getContext(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
                etCurrentPw.setText("");
                etNewPw.setText("");
                etConfirmPw.setText("");
            } else {
                Toast.makeText(getContext(), "Incorrect current password.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
