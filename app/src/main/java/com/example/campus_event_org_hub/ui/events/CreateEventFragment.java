package com.example.campus_event_org_hub.ui.events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Event;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

public class CreateEventFragment extends Fragment {

    private Uri selectedImageUri;
    private ImageView bannerPreview;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        bannerPreview.setImageURI(uri);
                        try {
                            getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception e) {}
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_event, container, false);
        bannerPreview = view.findViewById(R.id.create_event_banner_preview);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        view.findViewById(R.id.btn_pick_banner).setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));

        view.findViewById(R.id.btn_create_event_submit).setOnClickListener(v -> {
            TextInputEditText titleEt = view.findViewById(R.id.et_create_title);
            TextInputEditText descEt = view.findViewById(R.id.et_create_description);
            TextInputEditText dateEt = view.findViewById(R.id.et_create_date);
            TextInputEditText organizerEt = view.findViewById(R.id.et_create_organizer);
            TextInputEditText tagsEt = view.findViewById(R.id.et_create_tags);
            ChipGroup categoryChipGroup = view.findViewById(R.id.chip_group_category);

            String title = titleEt.getText().toString().trim();
            String desc = descEt.getText().toString().trim();
            String date = dateEt.getText().toString().trim();
            String organizer = organizerEt.getText().toString().trim();
            String tags = tagsEt.getText().toString().trim();

            if (title.isEmpty() || desc.isEmpty() || date.isEmpty() || organizer.isEmpty()) {
                Toast.makeText(getContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String category = "Academic";
            int checkedId = categoryChipGroup.getCheckedChipId();
            if (checkedId != View.NO_ID) {
                Chip chip = categoryChipGroup.findViewById(checkedId);
                if (chip != null) category = chip.getText().toString();
            }

            String imagePath = selectedImageUri != null ? selectedImageUri.toString() : "";
            DatabaseHelper db = new DatabaseHelper(getContext());
            // New events are PENDING by default
            Event newEvent = new Event(title, desc, date, tags, organizer, category, imagePath, "PENDING");
            if (db.addEvent(newEvent) != -1) {
                Toast.makeText(getContext(), "Event submitted for approval!", Toast.LENGTH_LONG).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }
}
