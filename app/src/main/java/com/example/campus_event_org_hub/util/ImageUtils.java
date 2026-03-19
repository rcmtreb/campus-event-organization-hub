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
     * Returns the appropriate banner drawable for a given event category.
     *
     * Category-specific PNG banners in res/drawable/ are used as default images
     * when an event has no uploaded banner.
     * Each category maps to its visually matching banner.
     *
     * @param category  The event's category string (case-insensitive)
     * @return          A @DrawableRes int pointing to the matching banner
     */
    @DrawableRes
    public static int getDefaultBannerForCategory(String category) {
        if (category == null || category.isEmpty()) return R.drawable.coreacademic_professional;
        switch (category.trim().toLowerCase()) {
            // Core Academic & Professional
            case "core academic":
            case "academic & professional":
            case "academic":
            case "professional":
            case "career":
            case "seminar":
            case "workshop":
            case "tech":
                return R.drawable.coreacademic_professional;

            // Student Life & Social
            case "student life & social":
            case "student life":
            case "social":
            case "wellness":
                return R.drawable.studentlife_social;

            // Athletics & Wellness
            case "athletics & wellness":
            case "athletics":
            case "sports":
            case "competition":
                return R.drawable.athletics_wellness;

            // Arts & Culture
            case "arts & culture":
            case "arts":
            case "culture":
            case "cultural":
            case "exhibit":
                return R.drawable.arts_culture;

            // Administrative & Ceremonial
            case "administrative & ceremonial":
            case "administrative":
            case "ceremonial":
            case "ceremony":
                return R.drawable.admin_ceremonial;

            // Competitions & Special Interest
            case "competitions & special interest":
            case "competitions":
            case "special interest":
                return R.drawable.comp_interests;

            default:
                return R.drawable.coreacademic_professional;
        }
    }

    /**
     * Load an image into {@code imageView} from {@code path}, falling back to
     * {@code placeholderRes} on any error or when {@code path} is empty.
     *
     * Images are decoded at a sample size that keeps them at most 2× the target
     * view dimensions to prevent blurry up-scaling while avoiding OOM errors.
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
                // ── Step 1: read bounds only ──────────────────────────────
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, opts);

                // ── Step 2: compute sample size ───────────────────────────
                opts.inSampleSize    = calculateInSampleSize(opts, imageView);
                opts.inJustDecodeBounds = false;
                opts.inPreferredConfig  = Bitmap.Config.ARGB_8888;

                bmp = BitmapFactory.decodeFile(path, opts);
            } else {
                // Content URI — must use ContentResolver; NEVER call setImageURI()
                Uri uri = Uri.parse(path);

                // ── Step 1: read bounds only ──────────────────────────────
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                InputStream is = ctx.getContentResolver().openInputStream(uri);
                if (is != null) { BitmapFactory.decodeStream(is, null, opts); is.close(); }

                // ── Step 2: decode at reduced sample size ─────────────────
                opts.inSampleSize       = calculateInSampleSize(opts, imageView);
                opts.inJustDecodeBounds = false;
                opts.inPreferredConfig  = Bitmap.Config.ARGB_8888;

                is = ctx.getContentResolver().openInputStream(uri);
                if (is != null) {
                    bmp = BitmapFactory.decodeStream(is, null, opts);
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

    /**
     * Calculates the largest inSampleSize value that still results in a bitmap
     * at least as large as the target view (or 512px if the view has not been
     * laid out yet).  Halving ensures the decoded bitmap is sharper than
     * up-scaling a heavily subsampled one.
     */
    private static int calculateInSampleSize(BitmapFactory.Options opts, ImageView iv) {
        int rawW = opts.outWidth;
        int rawH = opts.outHeight;
        if (rawW <= 0 || rawH <= 0) return 1;

        int targetW = iv.getWidth()  > 0 ? iv.getWidth()  : 512;
        int targetH = iv.getHeight() > 0 ? iv.getHeight() : 512;

        int sample = 1;
        if (rawH > targetH || rawW > targetW) {
            int halfH = rawH / 2;
            int halfW = rawW / 2;
            while ((halfH / sample) >= targetH && (halfW / sample) >= targetW) {
                sample *= 2;
            }
        }
        return sample;
    }
}
