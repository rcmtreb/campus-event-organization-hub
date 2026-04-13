package com.example.campus_event_org_hub.ui.admin;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.model.Course;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AdminCourseManagementActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private CourseAdapter adapter;
    private List<Course> courses = new ArrayList<>();
    private RecyclerView rvCourses;
    private TextView tvNoCourses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_course_management);

        db = DatabaseHelper.getInstance(this);

        rvCourses  = findViewById(R.id.rv_courses);
        tvNoCourses = findViewById(R.id.tv_no_courses);

        rvCourses.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAdapter(courses,
                course -> showCourseDialog(course),
                course -> confirmDeleteCourse(course));
        rvCourses.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add_course).setOnClickListener(v -> showCourseDialog(null));

        loadCourses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCourses();
    }

    private void loadCourses() {
        new Thread(() -> {
            List<Course> loaded = db.getAllCourses();
            runOnUiThread(() -> {
                courses.clear();
                courses.addAll(loaded);
                adapter.notifyDataSetChanged();
                boolean empty = courses.isEmpty();
                rvCourses.setVisibility(empty ? View.GONE : View.VISIBLE);
                tvNoCourses.setVisibility(empty ? View.VISIBLE : View.GONE);
            });
        }).start();
    }

    /** Show add-or-edit dialog. Pass null to add a new course. */
    private void showCourseDialog(Course existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_course_edit, null, false);

        TextView tvTitle          = view.findViewById(R.id.dialog_course_title);
        TextInputEditText etCode  = view.findViewById(R.id.et_course_code);
        TextInputEditText etName  = view.findViewById(R.id.et_course_name);
        Spinner spinnerDept       = view.findViewById(R.id.spinner_course_dept);
        TextInputEditText etYears = view.findViewById(R.id.et_course_years);

        // Populate department spinner
        String[] depts = getResources().getStringArray(R.array.departments_array);
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, depts);
        deptAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerDept.setAdapter(deptAdapter);

        boolean isEdit = (existing != null);
        tvTitle.setText(isEdit ? "Edit Course" : "Add Course");

        if (isEdit) {
            etCode.setText(existing.getCourseCode());
            etName.setText(existing.getCourseName());
            etYears.setText(String.valueOf(existing.getDurationYears()));
            // Pre-select matching department
            String savedDept = existing.getDepartment() != null ? existing.getDepartment() : "";
            for (int i = 0; i < depts.length; i++) {
                if (depts[i].equals(savedDept)) {
                    spinnerDept.setSelection(i);
                    break;
                }
            }
        }

        Dialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(view)
                .create();

        view.findViewById(R.id.dialog_btn_cancel).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.dialog_btn_save_course).setOnClickListener(v -> {
            String code     = etCode.getText() != null ? etCode.getText().toString().trim() : "";
            String name     = etName.getText() != null ? etName.getText().toString().trim() : "";
            String dept     = spinnerDept.getSelectedItem() != null
                                ? spinnerDept.getSelectedItem().toString() : "";
            String yearsStr = etYears.getText() != null ? etYears.getText().toString().trim() : "";

            if (code.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Course code and name are required.", Toast.LENGTH_SHORT).show();
                return;
            }
            int years = 4;
            try { years = Integer.parseInt(yearsStr); } catch (NumberFormatException ignored) {}

            final int finalYears = years;
            new Thread(() -> {
                boolean ok;
                if (isEdit) {
                    Course updated = new Course(existing.getCourseId(), code, name, dept, finalYears);
                    ok = db.updateCourse(updated) > 0;
                } else {
                    Course newCourse = new Course(0, code, name, dept, finalYears);
                    ok = db.insertCourse(newCourse) != -1;
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, ok ? "Saved!" : "Save failed.", Toast.LENGTH_SHORT).show();
                    if (ok) { dialog.dismiss(); loadCourses(); }
                });
            }).start();
        });

        dialog.show();
    }

    private void confirmDeleteCourse(Course course) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Delete \"" + course.getCourseCode() + " – " + course.getCourseName() + "\"?\n\nStudents enrolled in this course will have their course cleared.")
                .setPositiveButton("Delete", (d, w) -> {
                    new Thread(() -> {
                        boolean ok = db.deleteCourse(course.getCourseId()) > 0;
                        runOnUiThread(() -> {
                            Toast.makeText(this, ok ? "Deleted." : "Delete failed.", Toast.LENGTH_SHORT).show();
                            if (ok) loadCourses();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Inner RecyclerView Adapter ──────────────────────────────────────────────

    private static class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.VH> {

        interface OnAction { void act(Course c); }

        private final List<Course> data;
        private final OnAction onEdit;
        private final OnAction onDelete;

        CourseAdapter(List<Course> data, OnAction onEdit, OnAction onDelete) {
            this.data = data;
            this.onEdit = onEdit;
            this.onDelete = onDelete;
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_course, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            Course c = data.get(pos);
            h.tvCode.setText(c.getCourseCode());
            h.tvName.setText(c.getCourseName());
            h.tvDept.setText(c.getDepartment());
            h.tvYears.setText(c.getDurationYears() + "-year program");
            h.btnEdit.setOnClickListener(v -> onEdit.act(c));
            h.btnDelete.setOnClickListener(v -> onDelete.act(c));
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvCode, tvName, tvDept, tvYears;
            View btnEdit, btnDelete;
            VH(View v) {
                super(v);
                tvCode   = v.findViewById(R.id.tv_course_code);
                tvName   = v.findViewById(R.id.tv_course_name);
                tvDept   = v.findViewById(R.id.tv_course_dept);
                tvYears  = v.findViewById(R.id.tv_course_years);
                btnEdit  = v.findViewById(R.id.btn_edit_course);
                btnDelete = v.findViewById(R.id.btn_delete_course);
            }
        }
    }
}
