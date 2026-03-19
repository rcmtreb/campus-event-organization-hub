package com.example.campus_event_org_hub.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import com.example.campus_event_org_hub.R;

import java.io.InputStream;

/**
 * Safe image loading utility.
 *
 * Never call ImageView.setImageURI() directly with a content URI — on Android 10+ the URI
 * permission from a previous picker session may have expired.  The SecurityException is thrown
 * inside ImageView.onMeasure() → resolveUri(), which is OUTSIDE any try/catch at the call site,
 * so it escapes and crashes the app.
 *
 * This helper decodes the bitmap through ContentResolver.openInputStream() inside a
 * try/catch so that any SecurityException or IOException is caught and a placeholder is
 * shown instead of crashing.
 */
public final class ImageUtils {

    private ImageUtils() {}

    /**
     * Returns the appropriate 9-patch banner drawable for a given event category.
     *
     * The six .9.png banners in res/drawable/ are stretchable NinePatch assets.
     * They are used as the default/fallback image when an event has no uploaded banner.
     * Each category maps to a visually relevant banner; unrecognised categories fall
     * back to banner_tech_summit.
     *
     * @param category  The event's category string (case-insensitive)
     * @return          A @DrawableRes int pointing to the matching .9.png banner
     */
    @DrawableRes
    public static int getDefaultBannerForCategory(String category) {
        if (category == null || category.isEmpty()) return R.drawable.banner_tech_summit;
        switch (category.trim().toLowerCase()) {
            case "workshop":            return R.drawable.banner_android_workshop;
            case "sports":              return R.drawable.banner_basketball_finals;
            case "social":              return R.drawable.banner_music_festival;
            case "arts": case "cultural": return R.drawable.banner_art_fair;
            case "academic": case "career": return R.drawable.banner_career_week;
            default:                    return R.drawable.banner_tech_summit;
        }
    }

    /**
     * Load an image into {@code imageView} from {@code path}, falling back to
     * {@code placeholderRes} on any error or when {@code path} is empty.
     *
     * @param ctx            Context (Activity or Fragment context both work)
     * @param imageView      Target ImageView
     * @param path           Absolute file path (starts with "/") OR content URI string
     * @param placeholderRes Drawable resource ID to show as fallback
     */
    public static void load(Context ctx, ImageView imageView, String path, int placeholderRes) {
        if (path == null || path.isEmpty()) {
            imageView.setImageResource(placeholderRes);
            return;
        }
        try {
            Bitmap bmp = null;
            if (path.startsWith("/")) {
                // Internal/external file path — decode directly
                bmp = BitmapFactory.decodeFile(path);
            } else {
                // Content URI — must use ContentResolver; NEVER call setImageURI()
                Uri uri = Uri.parse(path);
                InputStream is = ctx.getContentResolver().openInputStream(uri);
                if (is != null) {
                    bmp = BitmapFactory.decodeStream(is);
                    is.close();
                }
            }
            if (bmp != null) {
                // Clear any XML tint so the real photo renders with correct colours
                imageView.clearColorFilter();
                imageView.setImageBitmap(bmp);
            } else {
                imageView.setImageResource(placeholderRes);
            }
        } catch (Exception e) {
            // SecurityException, FileNotFoundException, IOException, etc.
            imageView.setImageResource(placeholderRes);
        }
    }
}
