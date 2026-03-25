package com.example.campus_event_org_hub.ui.admin;

import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.ui.base.BaseActivity;
import com.example.campus_event_org_hub.util.ImageUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends com.example.campus_event_org_hub.ui.base.BaseActivity {

    private DatabaseHelper db;
    private RecyclerView rv;
    private UserAdapter adapter;
    private TextView tvCount;

    // masterList = all users from DB; displayList = filtered subset shown in the RecyclerView
    private final List<User> masterList  = new ArrayList<>();
    private final List<User> displayList = new ArrayList<>();

    private String currentQuery = "";
    private String currentDept  = "All"; // chip text: "All","CBA","CCJE","COED","COE","COL","CLAS","GS"

    @Override
    protected boolean useEdgeToEdge() { return true; }

    @Override
    protected boolean isExitOnBackEnabled() { return false; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        db = DatabaseHelper.getInstance(this);

        // Result count label
        tvCount = findViewById(R.id.tv_user_count);

        // RecyclerView
        rv = findViewById(R.id.rv_users);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(displayList);
        rv.setAdapter(adapter);

        // Search bar
        EditText etSearch = findViewById(R.id.et_search_users);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim().toLowerCase();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Department chip filter
        ChipGroup chipGroup = findViewById(R.id.chip_group_dept);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = group.findViewById(checkedIds.get(0));
            currentDept = chip != null ? chip.getText().toString() : "All";
            applyFilter();
        });

        loadUsers();
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadUsers() {
        masterList.clear();
        Cursor cursor = db.getAllUsers();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int imgIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_PROFILE_IMG);
                String imgPath = imgIdx >= 0 ? cursor.getString(imgIdx) : "";
                masterList.add(new User(
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_STUDENT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ROLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_DEPARTMENT)),
                        imgPath != null ? imgPath : ""
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        applyFilter();
    }

    /**
     * Filters masterList by the active search query AND selected department chip,
     * pushes results into displayList, and refreshes the adapter + count label.
     */
    private void applyFilter() {
        displayList.clear();
        for (User u : masterList) {
            boolean matchesSearch = currentQuery.isEmpty()
                    || (u.name != null && u.name.toLowerCase().contains(currentQuery))
                    || (u.id   != null && u.id.toLowerCase().contains(currentQuery));

            // dept is stored as "College of Engineering (COE)" — match the abbreviation in parens
            boolean matchesDept = "All".equals(currentDept)
                    || (u.dept != null && u.dept.contains("(" + currentDept + ")"));

            if (matchesSearch && matchesDept) displayList.add(u);
        }
        adapter.notifyDataSetChanged();
        int n = displayList.size();
        tvCount.setText(n + " user" + (n == 1 ? "" : "s"));
    }

    // ── Model ─────────────────────────────────────────────────────────────────

    static class User {
        String name, id, role, dept, profileImg;
        User(String n, String i, String r, String d, String img) {
            name = n; id = i; role = r; dept = d; profileImg = img;
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {
        private final List<User> list;
        UserAdapter(List<User> list) { this.list = list; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.user_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            User u = list.get(pos);
            h.name.setText(u.name);
            h.details.setText(u.id + "-S  |  " + (u.dept != null ? u.dept : "—"));
            h.role.setText(u.role);

            // Badge tint: green for Student, orange for Officer
            int badgeColor = "Officer".equalsIgnoreCase(u.role)
                    ? ContextCompat.getColor(h.itemView.getContext(), R.color.primary_orange)
                    : ContextCompat.getColor(h.itemView.getContext(), R.color.primary_green);
            h.role.setBackgroundTintList(ColorStateList.valueOf(badgeColor));

            // Avatar
            if (h.avatar != null) {
                ImageUtils.load(h.itemView.getContext(), h.avatar,
                        u.profileImg, R.drawable.ic_image_placeholder);
            }

            // Tap row → toggle role
            h.itemView.setOnClickListener(v -> {
                String newRole = "Student".equalsIgnoreCase(u.role) ? "Officer" : "Student";
                new AlertDialog.Builder(UserManagementActivity.this)
                        .setTitle("Change User Role")
                        .setMessage("Change " + u.name + " to " + newRole + "?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            db.updateUserRole(u.id, newRole);
                            u.role = newRole;
                            notifyItemChanged(h.getAdapterPosition());
                            Toast.makeText(UserManagementActivity.this,
                                    "Role updated!", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            // Delete button
            h.btnDelete.setOnClickListener(v ->
                new AlertDialog.Builder(UserManagementActivity.this)
                        .setTitle("Delete Account")
                        .setMessage("Permanently delete " + u.name + " (" + u.id + "-S)?\n\n"
                                + "This will also remove their registrations and notifications.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            db.deleteUserAccount(u.id);
                            // Remove from both lists so filter state stays consistent
                            masterList.remove(u);
                            int idx = list.indexOf(u);
                            if (idx >= 0) {
                                list.remove(idx);
                                notifyItemRemoved(idx);
                            }
                            int n = list.size();
                            tvCount.setText(n + " user" + (n == 1 ? "" : "s"));
                            Toast.makeText(UserManagementActivity.this,
                                    u.name + " deleted.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show()
            );
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView name, details, role;
            ShapeableImageView avatar;
            ImageButton btnDelete;
            VH(View v) {
                super(v);
                name      = v.findViewById(R.id.user_item_name);
                details   = v.findViewById(R.id.user_item_details);
                role      = v.findViewById(R.id.user_item_role);
                avatar    = v.findViewById(R.id.user_item_avatar);
                btnDelete = v.findViewById(R.id.btn_delete_user);
            }
        }
    }
}
