package com.example.campus_event_org_hub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

public class CreateEventFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.btn_pick_banner).setOnClickListener(v ->
                Toast.makeText(getContext(), "Gallery picker coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.btn_create_event_submit).setOnClickListener(v -> {
            TextInputEditText titleEt       = view.findViewById(R.id.et_create_title);
            TextInputEditText descEt        = view.findViewById(R.id.et_create_description);
            TextInputEditText dateEt        = view.findViewById(R.id.et_create_date);
            TextInputEditText organizerEt   = view.findViewById(R.id.et_create_organizer);
            TextInputEditText tagsEt        = view.findViewById(R.id.et_create_tags);
            ChipGroup categoryChipGroup     = view.findViewById(R.id.chip_group_category);

            String title     = titleEt.getText() != null ? titleEt.getText().toString().trim() : "";
            String desc      = descEt.getText() != null ? descEt.getText().toString().trim() : "";
            String date      = dateEt.getText() != null ? dateEt.getText().toString().trim() : "";
            String organizer = organizerEt.getText() != null ? organizerEt.getText().toString().trim() : "";
            String tags      = tagsEt.getText() != null ? tagsEt.getText().toString().trim() : "";

            if (title.isEmpty() || desc.isEmpty() || date.isEmpty() || organizer.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Resolve selected category
            String category = "Academic";
            int checkedId = categoryChipGroup.getCheckedChipId();
            if (checkedId != View.NO_ID) {
                Chip chip = categoryChipGroup.findViewById(checkedId);
                if (chip != null) category = chip.getText().toString();
            }

            Toast.makeText(getContext(),
                    "Event \"" + title + "\" published!", Toast.LENGTH_LONG).show();

            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }
}
