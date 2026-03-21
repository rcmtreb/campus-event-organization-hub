package com.example.campus_event_org_hub.util;

public class PasswordStrengthUtil {

    public enum Strength {
        WEAK(0, "Weak"),
        FAIR(1, "Fair"),
        GOOD(2, "Good"),
        STRONG(3, "Strong");

        public final int level;
        public final String label;

        Strength(int level, String label) {
            this.level = level;
            this.label = label;
        }
    }

    public static Strength calculateStrength(String password) {
        if (password == null || password.isEmpty()) {
            return Strength.WEAK;
        }

        int score = 0;

        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.length() >= 16) score++;

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        if (hasLower) score++;
        if (hasUpper) score++;
        if (hasDigit) score++;
        if (hasSpecial) score += 2;

        if (score <= 2) return Strength.WEAK;
        if (score <= 4) return Strength.FAIR;
        if (score <= 6) return Strength.GOOD;
        return Strength.STRONG;
    }

    public static int getColorResId(Strength strength) {
        switch (strength) {
            case WEAK: return android.R.color.holo_red_light;
            case FAIR: return android.R.color.holo_orange_light;
            case GOOD: return android.R.color.holo_blue_light;
            case STRONG: return android.R.color.holo_green_light;
            default: return android.R.color.darker_gray;
        }
    }

    public static float getProgress(Strength strength) {
        switch (strength) {
            case WEAK: return 0.25f;
            case FAIR: return 0.50f;
            case GOOD: return 0.75f;
            case STRONG: return 1.0f;
            default: return 0f;
        }
    }
}
