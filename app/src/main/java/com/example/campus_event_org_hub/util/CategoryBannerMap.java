package com.example.campus_event_org_hub.util;

import com.example.campus_event_org_hub.R;

/**
 * Maps event category strings to their corresponding banner drawable resource IDs.
 * Falls back to {@link R.drawable#ic_image_placeholder} for unknown categories.
 */
public final class CategoryBannerMap {

    private CategoryBannerMap() {}

    /**
     * Returns a drawable resource ID for the given category name.
     *
     * @param category the category string stored on the event (e.g. "Academic", "Tech")
     * @return a drawable res-id suitable for use with ImageView / Glide / Picasso
     */
    public static int getBanner(String category) {
        if (category == null) return R.drawable.ic_image_placeholder;
        switch (category.trim()) {
            // ── Core Academic & Professional ──────────────────────────────────
            case "Academic":
            case "Seminar":
                return R.drawable.banner_android_workshop;   // generic academic look

            case "Workshop":
                return R.drawable.banner_android_workshop;

            case "Career":
                return R.drawable.banner_career_week;

            // ── Student Life & Social ─────────────────────────────────────────
            case "Social":
            case "Cultural":
                return R.drawable.banner_music_festival;

            // ── Athletics & Wellness ──────────────────────────────────────────
            case "Sports":
            case "Wellness":
                return R.drawable.banner_basketball_finals;

            // ── Arts & Culture ────────────────────────────────────────────────
            case "Arts":
            case "Exhibit":
                return R.drawable.banner_art_fair;

            // ── Administrative & Ceremonial ───────────────────────────────────
            case "Ceremony":
                return R.drawable.banner_music_festival;

            // ── Competitions & Special Interest ──────────────────────────────
            case "Competition":
                return R.drawable.banner_basketball_finals;

            case "Tech":
                return R.drawable.banner_tech_summit;

            default:
                return R.drawable.ic_image_placeholder;
        }
    }
}
