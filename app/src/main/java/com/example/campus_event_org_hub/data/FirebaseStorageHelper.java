package com.example.campus_event_org_hub.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Encodes images as Base64 data-URIs and returns them via the callback.
 *
 * All images are converted to PNG (lossless) before encoding, regardless of
 * the source format (JPEG, HEIC, WebP, etc.).  They are stored directly in
 * Firestore / SQLite as:
 *   data:image/png;base64,<base64-encoded-bytes>
 * which is completely device-independent — no Firebase Storage needed.
 *
 * Profile photos are capped at 200×200 px; event banners at 400×400 px,
 * keeping each string comfortably under Firestore's 1 MB document limit.
 */
public class FirebaseStorageHelper {

    private static final String TAG = "FirebaseStorageHelper";
    // PNG is lossless; quality is ignored by Android for PNG but kept for clarity.
    private static final int PNG_QUALITY = 100;

    /** Max dimension for profile photos (keeps the Base64 string small). */
    private static final int MAX_DIM_PROFILE = 200;
    /** Max dimension for event banners. */
    private static final int MAX_DIM_BANNER  = 1080;

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface UploadCallback {
        /** Called on the main thread with the data:image/jpeg;base64,... string. */
        void onSuccess(String downloadUrl);
        /** Called on the main thread with a human-readable error message. */
        void onFailure(String errorMessage);
    }

    // ── Profile photo ──────────────────────────────────────────────────────────

    /**
     * Encode a profile photo from a content:// or file:// URI.
     * Runs on a background thread; callback is delivered on the main thread.
     */
    public void uploadProfilePhoto(Context ctx, Uri uri, String studentId,
                                   UploadCallback callback) {
        EXECUTOR.execute(() -> {
            byte[] bytes = uriToJpegBytes(ctx, uri, MAX_DIM_PROFILE);
            if (bytes == null) {
                MAIN_HANDLER.post(() -> callback.onFailure("Could not read image from gallery."));
                return;
            }
            String dataUri = toDataUri(bytes);
            MAIN_HANDLER.post(() -> callback.onSuccess(dataUri));
        });
    }

    // ── Event banner ───────────────────────────────────────────────────────────

    /**
     * Encode an event banner from a content:// or file:// URI.
     * Runs on a background thread; callback is delivered on the main thread.
     */
    public void uploadEventBanner(Context ctx, Uri uri, String eventKey,
                                  UploadCallback callback) {
        EXECUTOR.execute(() -> {
            byte[] bytes = uriToJpegBytes(ctx, uri, MAX_DIM_BANNER);
            if (bytes == null) {
                MAIN_HANDLER.post(() -> callback.onFailure("Could not read image from gallery."));
                return;
            }
            String dataUri = toDataUri(bytes);
            MAIN_HANDLER.post(() -> callback.onSuccess(dataUri));
        });
    }

    // ── Internal helpers ───────────────────────────────────────────────────────

    /**
     * Read a content:// or file:// URI, scale the bitmap so its longest side
     * is at most {@code maxDim}, then re-encode as PNG bytes.
     * All source formats (JPEG, HEIC, WebP, etc.) are converted to PNG.
     */
    private byte[] uriToJpegBytes(Context ctx, Uri uri, int maxDim) {
        try {
            InputStream is = ctx.getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            if (bmp == null) return null;
            bmp = scaleBitmapIfNeeded(bmp, maxDim);
            return bitmapToPngBytes(bmp);
        } catch (Exception e) {
            Log.e(TAG, "uriToJpegBytes failed", e);
            return null;
        }
    }

    /** Scale bitmap so its longest side is at most maxDim. */
    private Bitmap scaleBitmapIfNeeded(Bitmap bmp, int maxDim) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        if (w <= maxDim && h <= maxDim) return bmp;
        float scale = (float) maxDim / Math.max(w, h);
        return Bitmap.createScaledBitmap(bmp,
                Math.max(1, Math.round(w * scale)),
                Math.max(1, Math.round(h * scale)), true);
    }

    private byte[] bitmapToPngBytes(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, baos);
        return baos.toByteArray();
    }

    /** Wrap raw PNG bytes in a data-URI prefix. */
    private String toDataUri(byte[] pngBytes) {
        return "data:image/png;base64," +
                Base64.encodeToString(pngBytes, Base64.NO_WRAP);
    }
}
