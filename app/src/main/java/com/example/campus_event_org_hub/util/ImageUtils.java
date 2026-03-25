package com.example.campus_event_org_hub.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.ImageView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

import com.example.campus_event_org_hub.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    /** Background thread pool for off-main-thread network/disk image loading. */
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    /** Tag key used to track the currently-loaded URL so we skip redundant loads. */
    private static final int TAG_URL = 0x7f123456;

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
     * Loads a profile/avatar image into a ShapeableImageView.
     *
     * When {@code path} is empty or the file cannot be decoded:
     *   – Sets the placeholder icon (ic_person)
     *   – Restores the tint to {@code placeholderTintColor}
     *   – Restores the inner padding to {@code placeholderPaddingPx}
     *
     * When the image loads successfully:
     *   – Clears the tint (setImageTintList(null)) and color filter
     *   – Removes padding so the photo fills the full circle
     *
     * @param ctx                  Context
     * @param imageView            Target ShapeableImageView (or any ImageView)
     * @param path                 Absolute file path or content URI string, may be null/empty
     * @param placeholderTintColor Color resource ID to apply as tint when showing placeholder
     * @param placeholderPaddingPx Padding in pixels to restore when showing placeholder
     */
     public static void loadAvatar(Context ctx, ImageView imageView, String path,
                                  @ColorRes int placeholderTintColor, int placeholderPaddingPx) {
        String oldTag = (String) imageView.getTag(TAG_URL);

        if (path == null || path.isEmpty()) {
            imageView.setTag(TAG_URL, null);
            showAvatarPlaceholder(imageView);
            return;
        }

        if (path.equals(oldTag)) {
            return;
        }

        imageView.setTag(TAG_URL, path);

        // Base64 data-URIs can be decoded directly (but off the main thread — they can be large)
        if (path.startsWith("data:image/")) {
            showAvatarPlaceholder(imageView);
            EXECUTOR.execute(() -> {
                Bitmap bmp = decodeBitmapFromBase64(path);
                MAIN_HANDLER.post(() -> {
                    if (!path.equals(imageView.getTag(TAG_URL))) return;
                    if (bmp != null) {
                        imageView.clearColorFilter();
                        imageView.setImageTintList(null);
                        imageView.setPadding(0, 0, 0, 0);
                        imageView.setBackground(null);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        imageView.setImageBitmap(bmp);
                    } else {
                        showAvatarPlaceholder(imageView);
                    }
                });
            });
            return;
        }
        // Network URLs must be loaded off the main thread
        if (path.startsWith("http://") || path.startsWith("https://")) {
            showAvatarPlaceholder(imageView);
            EXECUTOR.execute(() -> {
                Bitmap bmp = decodeBitmapFromUrl(path);
                MAIN_HANDLER.post(() -> {
                    if (!path.equals(imageView.getTag(TAG_URL))) return;
                    if (bmp != null) {
                        imageView.clearColorFilter();
                        imageView.setImageTintList(null);
                        imageView.setPadding(0, 0, 0, 0);
                        imageView.setBackground(null);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        imageView.setImageBitmap(bmp);
                    } else {
                        showAvatarPlaceholder(imageView);
                    }
                });
            });
            return;
        }
        Bitmap bmp = decodeBitmap(ctx, path, imageView);
        if (bmp != null) {
            imageView.clearColorFilter();
            imageView.setImageTintList(null);
            imageView.setPadding(0, 0, 0, 0);
            imageView.setBackground(null);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageBitmap(bmp);
        } else {
            showAvatarPlaceholder(imageView);
        }
    }

    /**
     * Resets an avatar ImageView to the placeholder state using a self-contained
     * layer-list drawable (green circle + white person icon).  No padding,
     * background, or tint manipulation needed.
     */
    private static void showAvatarPlaceholder(ImageView imageView) {
        imageView.setImageTintList(null);
        imageView.clearColorFilter();
        imageView.setPadding(0, 0, 0, 0);
        imageView.setBackground(null);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setImageResource(R.drawable.avatar_placeholder);
    }

    /**
     * Load an image into {@code imageView} from {@code path}, falling back to
     * {@code placeholderRes} on any error or when {@code path} is empty.
     *
     * Images are decoded at a resolution that ensures they are at least 110% of
     * the target view dimensions, preventing blurry up-scaling while maintaining
     * memory efficiency.  Uses ViewTreeObserver for accurate sizing when the view
     * has not been measured yet.
     *
     * @param ctx            Context (Activity or Fragment context both work)
     * @param imageView      Target ImageView
     * @param path           Absolute file path (starts with "/") OR content URI string
     * @param placeholderRes Drawable resource ID to show as fallback
     */
    public static void load(Context ctx, ImageView imageView, String path, int placeholderRes) {
        // Capture the tag BEFORE any changes — this is the "already loaded" state.
        String oldTag = (String) imageView.getTag(TAG_URL);

        if (path == null || path.isEmpty()) {
            imageView.setTag(TAG_URL, null);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setImageResource(placeholderRes);
            return;
        }

        // If the same URL is already tagged in this ImageView, skip entirely.
        // Don't check drawable — it may still be decoding on a previous bind.
        // This prevents the placeholder flash on scroll/filter rebinds.
        if (path.equals(oldTag)) {
            return;
        }

        // Update tag first — this guards against stale async results below.
        imageView.setTag(TAG_URL, path);

        // Base64 data-URIs — decode off the main thread
        if (path.startsWith("data:image/")) {
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setImageResource(placeholderRes);
            EXECUTOR.execute(() -> {
                Bitmap bmp = decodeBitmapFromBase64(path);
                MAIN_HANDLER.post(() -> {
                    // Skip if the tag changed (view was rebound to a different URL).
                    if (!path.equals(imageView.getTag(TAG_URL))) return;
                    if (bmp != null) {
                        imageView.clearColorFilter();
                        imageView.setImageTintList(null);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        imageView.setImageBitmap(bmp);
                    } else {
                        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        imageView.setImageResource(placeholderRes);
                    }
                });
            });
            return;
        }
        // Network URLs must be loaded off the main thread
        if (path.startsWith("http://") || path.startsWith("https://")) {
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setImageResource(placeholderRes);
            EXECUTOR.execute(() -> {
                Bitmap bmp = decodeBitmapFromUrl(path);
                MAIN_HANDLER.post(() -> {
                    if (!path.equals(imageView.getTag(TAG_URL))) return;
                    if (bmp != null) {
                        imageView.clearColorFilter();
                        imageView.setImageTintList(null);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        imageView.setImageBitmap(bmp);
                    } else {
                        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        imageView.setImageResource(placeholderRes);
                    }
                });
            });
            return;
        }
        Bitmap bmp = decodeBitmap(ctx, path, imageView);
        if (bmp != null) {
            imageView.clearColorFilter();
            imageView.setImageTintList(null);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageBitmap(bmp);
        } else {
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setImageResource(placeholderRes);
        }
    }

    /** Decode a bitmap from an absolute path or content URI string. Returns null on failure. */
    private static Bitmap decodeBitmap(Context ctx, String path, ImageView imageView) {
        try {
            if (path.startsWith("/")) {
                // ── Step 1: read bounds only ──────────────────────────────
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, opts);

                // ── Step 2: compute sample size ───────────────────────────
                opts.inSampleSize       = calculateInSampleSize(opts, imageView);
                opts.inJustDecodeBounds = false;
                opts.inPreferredConfig  = Bitmap.Config.ARGB_8888;

                return BitmapFactory.decodeFile(path, opts);
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
                    Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
                    is.close();
                    return bmp;
                }
            }
        } catch (Exception e) {
            // SecurityException, FileNotFoundException, IOException, etc. — return null
        }
        return null;
    }

    /**
     * Calculates the largest inSampleSize value that still results in a bitmap
     * at least as large as the target view (or 1080px if the view has not been
     * laid out yet).  A 10% buffer is added to target dimensions to ensure the
     * decoded bitmap is never smaller than the display size, preventing blurry
     * up-scaling.  Uses ViewTreeObserver to get actual view dimensions when
     * the view hasn't been measured yet.
     */
    private static int calculateInSampleSize(BitmapFactory.Options opts, ImageView iv) {
        int rawW = opts.outWidth;
        int rawH = opts.outHeight;
        if (rawW <= 0 || rawH <= 0) return 1;

        final int[] targetSize = getViewTargetSize(iv);
        int targetW = targetSize[0];
        int targetH = targetSize[1];

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

    /**
     * Gets the target size for image loading. If the view has been measured,
     * returns actual dimensions with 10% buffer. Otherwise waits for layout
     * via ViewTreeObserver or falls back to 1080px minimum.
     */
    private static int[] getViewTargetSize(ImageView iv) {
        int measuredW = iv.getMeasuredWidth();
        int measuredH = iv.getMeasuredHeight();

        if (measuredW > 0 && measuredH > 0) {
            return new int[] {
                (int) (measuredW * 1.1f),
                (int) (measuredH * 1.1f)
            };
        }

        int layoutW = iv.getWidth();
        int layoutH = iv.getHeight();

        if (layoutW > 0 && layoutH > 0) {
            return new int[] {
                (int) (layoutW * 1.1f),
                (int) (layoutH * 1.1f)
            };
        }

        return new int[] { 1080, 1080 };
    }

    /**
     * Decodes a bitmap from a data:image/...;base64,... URI.
     * Must be called from a background thread for large images.
     * Returns null on any error.
     */
    private static Bitmap decodeBitmapFromBase64(String dataUri) {
        try {
            int commaIndex = dataUri.indexOf(',');
            if (commaIndex < 0) return null;
            String base64Data = dataUri.substring(commaIndex + 1);
            byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Downloads and decodes a bitmap from an http/https URL.
     * Must be called from a background thread.
     * Returns null on any error.
     */
    private static Bitmap decodeBitmapFromUrl(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            conn.setDoInput(true);
            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            InputStream is = conn.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            return bmp;
        } catch (Exception e) {
            // Network error, timeout, etc.
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
