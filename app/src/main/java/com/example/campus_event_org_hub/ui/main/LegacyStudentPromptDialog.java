package com.example.campus_event_org_hub.ui.main;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Course;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class LegacyStudentPromptDialog extends DialogFragment {

    public interface OnSavedListener {
        void onSaved(int courseId, int yearLevel, String section);
    }

    private static final String ARG_STUDENT_ID = "student_id";

    private String studentId;
    private List<Course> courseList = new ArrayList<>();
    private int selectedCourseId = -1;
    private OnSavedListener onSavedListener;

    public static LegacyStudentPromptDialog newInstance(String studentId) {
        LegacyStudentPromptDialog dialog = new LegacyStudentPromptDialog();
        Bundle args = new Bundle();
        args.putString(ARG_STUDENT_ID, studentId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnSavedListener(OnSavedListener listener) {
        this.onSavedListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        studentId = getArguments() != null ? getArguments().getString(ARG_STUDENT_ID, "") : "";

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_legacy_prompt, null, false);

        Spinner spinnerCourse  = view.findViewById(R.id.dialog_spinner_course);
        Spinner spinnerYear    = view.findViewById(R.id.dialog_spinner_year);
        Spinner spinnerSection = view.findViewById(R.id.dialog_spinner_section);

        DatabaseHelper db = DatabaseHelper.getInstance(requireContext());

        populateYearSpinner(spinnerYear);
        populateSections(spinnerSection);

        // Load courses in background
        new Thread(() -> {
            List<Course> courses = db.getAllCourses();
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                courseList.clear();
                courseList.addAll(courses);
                List<String> names = new ArrayList<>();
                names.add("Select Course");
                for (Course c : courseList) names.add(c.getCourseCode() + " \u2013 " + c.getCourseName());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        R.layout.spinner_item, names) {
                    @Override
                    public boolean isEnabled(int position) { return position != 0; }
                    @Override
                    public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                        View v = super.getDropDownView(position, convertView, parent);
                        android.widget.TextView tv = (android.widget.TextView) v;
                        tv.setTextColor(position == 0
                                ? getResources().getColor(R.color.text_hint, null)
                                : getResources().getColor(R.color.text_primary, null));
                        return v;
                    }
                };
                adapter.setDropDownViewResource(R.layout.spinner_item);
                spinnerCourse.setAdapter(adapter);
                spinnerCourse.setSelection(0);
            });
        }).start();

        // Reset year + section when course changes
        spinnerCourse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                if (position == 0) {
                    selectedCourseId = -1;
                    spinnerYear.setSelection(0);
                    spinnerSection.setSelection(0);
                } else {
                    Course chosen = courseList.get(position - 1);
                    selectedCourseId = chosen.getCourseId();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { selectedCourseId = -1; }
        });

        view.findViewById(R.id.dialog_btn_skip).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.dialog_btn_save).setOnClickListener(v -> {
            if (spinnerCourse.getSelectedItemPosition() == 0) {
                Toast.makeText(requireContext(), "Please select a course.", Toast.LENGTH_SHORT).show();
                return;
            }
            int yearLevel = spinnerYear.getSelectedItemPosition(); // placeholder at 0 → 1st Year at 1
            if (spinnerSection.getSelectedItemPosition() == 0) {
                Toast.makeText(requireContext(), "Please select your section", Toast.LENGTH_SHORT).show();
                return;
            }
            String section = spinnerSection.getSelectedItem() != null
                    ? spinnerSection.getSelectedItem().toString() : "";

            new Thread(() -> {
                db.updateStudentAcademicInfo(studentId, selectedCourseId, yearLevel, section);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    if (onSavedListener != null) onSavedListener.onSaved(selectedCourseId, yearLevel, section);
                    dismiss();
                });
            }).start();
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }

    private void populateYearSpinner(Spinner spinner) {
        String[] years = {"Select Year Level", "1st Year", "2nd Year", "3rd Year", "4th Year"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(),
                R.layout.spinner_item, years) {
            @Override
            public boolean isEnabled(int position) { return position != 0; }
            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                android.widget.TextView tv = (android.widget.TextView) v;
                tv.setTextColor(position == 0
                        ? getResources().getColor(R.color.text_hint, null)
                        : getResources().getColor(R.color.text_primary, null));
                return v;
            }
        };
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
    }

    private void populateSections(Spinner spinner) {
        String[] sections = {"Select Section", "A", "B", "C"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(),
                R.layout.spinner_item, sections) {
            @Override
            public boolean isEnabled(int position) { return position != 0; }
            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                android.widget.TextView tv = (android.widget.TextView) v;
                tv.setTextColor(position == 0
                        ? getResources().getColor(R.color.text_hint, null)
                        : getResources().getColor(R.color.text_primary, null));
                return v;
            }
        };
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);
    }
}
