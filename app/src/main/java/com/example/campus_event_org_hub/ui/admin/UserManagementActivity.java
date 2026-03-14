package com.example.campus_event_org_hub.ui.admin;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campus_event_org_hub.R;
import com.example.campus_event_org_hub.data.DatabaseHelper;
import com.example.campus_event_org_hub.util.ImageUtils;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private RecyclerView rv;
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        db = new DatabaseHelper(this);

        findViewById(R.id.btn_back_user_mgmt).setOnClickListener(v -> finish());

        rv = findViewById(R.id.rv_users);
        rv.setLayoutManager(new LinearLayoutManager(this));

        loadUsers();
    }

    private void loadUsers() {
        List<User> userList = new ArrayList<>();
        Cursor cursor = db.getAllUsers();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int imgIdx = cursor.getColumnIndex(DatabaseHelper.COLUMN_USER_PROFILE_IMG);
                String imgPath = imgIdx >= 0 ? cursor.getString(imgIdx) : "";
                userList.add(new User(
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_STUDENT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_ROLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USER_DEPARTMENT)),
                        imgPath != null ? imgPath : ""
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter = new UserAdapter(userList);
        rv.setAdapter(adapter);
    }

    static class User {
        String name, id, role, dept, profileImg;
        User(String n, String i, String r, String d, String img) {
            name = n; id = i; role = r; dept = d; profileImg = img;
        }
    }

    class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {
        private final List<User> list;
        UserAdapter(List<User> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            User u = list.get(pos);
            h.name.setText(u.name);
            h.details.setText(u.id + " | " + u.dept);
            h.role.setText(u.role);

            // Load avatar
            if (h.avatar != null) {
                ImageUtils.load(h.itemView.getContext(), h.avatar,
                        u.profileImg, R.drawable.ic_image_placeholder);
            }

            h.itemView.setOnClickListener(v -> {
                String newRole = u.role.equalsIgnoreCase("Student") ? "Officer" : "Student";
                new AlertDialog.Builder(UserManagementActivity.this)
                        .setTitle("Change User Role")
                        .setMessage("Do you want to change " + u.name + " to " + newRole + "?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            db.updateUserRole(u.id, newRole);
                            u.role = newRole;
                            notifyItemChanged(pos);
                            Toast.makeText(UserManagementActivity.this, "Role updated!", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView name, details, role;
            ShapeableImageView avatar;
            VH(View v) {
                super(v);
                name    = v.findViewById(R.id.user_item_name);
                details = v.findViewById(R.id.user_item_details);
                role    = v.findViewById(R.id.user_item_role);
                avatar  = v.findViewById(R.id.user_item_avatar);
            }
        }
    }
}
