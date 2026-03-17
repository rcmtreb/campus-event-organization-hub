package com.example.campus_event_org_hub.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists the logged-in user's session across app restarts using SharedPreferences.
 *
 * When a user logs in, call saveSession().
 * On every app start (LoginActivity.onCreate), call isLoggedIn() — if true, skip the
 * login screen and go directly to MainActivity / AdminActivity.
 * On logout, call clearSession().
 */
public class SessionManager {

    private static final String PREF_NAME  = "ceoh_session";
    private static final String KEY_LOGGED_IN   = "is_logged_in";
    private static final String KEY_NAME        = "user_name";
    private static final String KEY_ROLE        = "user_role";
    private static final String KEY_DEPT        = "user_dept";
    private static final String KEY_EMAIL       = "user_email";
    private static final String KEY_STUDENT_ID  = "user_student_id";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** Save session after a successful login. */
    public void saveSession(String name, String role, String dept,
                            String email, String studentId) {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN,  true)
                .putString(KEY_NAME,        name      != null ? name      : "")
                .putString(KEY_ROLE,        role      != null ? role      : "Student")
                .putString(KEY_DEPT,        dept      != null ? dept      : "")
                .putString(KEY_EMAIL,       email     != null ? email     : "")
                .putString(KEY_STUDENT_ID,  studentId != null ? studentId : "")
                .apply();
    }

    /** Clear session on logout. */
    public void clearSession() {
        prefs.edit().clear().apply();
    }

    /** True if a valid session is saved. */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getName()      { return prefs.getString(KEY_NAME,       "User");    }
    public String getRole()      { return prefs.getString(KEY_ROLE,       "Student"); }
    public String getDept()      { return prefs.getString(KEY_DEPT,       "");        }
    public String getEmail()     { return prefs.getString(KEY_EMAIL,      "");        }
    public String getStudentId() { return prefs.getString(KEY_STUDENT_ID, "");        }
}
