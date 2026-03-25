package com.example.campus_event_org_hub.ui.base;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.example.campus_event_org_hub.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.snackbar.Snackbar;

public abstract class BaseActivity extends AppCompatActivity {

    private boolean exitPressedOnce = false;
    private final Handler exitHandler = new Handler(Looper.getMainLooper());
    private static final int EXIT_DELAY_MS = 2000;

    /**
     * Override and return false in sub-screens that should simply finish() on back press
     * rather than showing the "tap again to exit" snackbar.
     */
    protected boolean isExitOnBackEnabled() {
        return true;
    }

    /**
     * Override and return true in activities that use edge-to-edge display
     * (status bar drawn behind custom header with a gradient view).
     * The activity's layout must have a View with id @+id/status_bar_background
     * positioned at the top (constrained to parent top) before calling super.onCreate().
     */
    protected boolean useEdgeToEdge() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (useEdgeToEdge()) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        }

        super.onCreate(savedInstanceState);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();

                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                    return;
                }

                // Sub-screens (isExitOnBackEnabled() == false) just finish normally.
                if (!isExitOnBackEnabled()) {
                    finish();
                    return;
                }

                if (exitPressedOnce) {
                    exitHandler.removeCallbacksAndMessages(null);
                    finish();
                    return;
                }

                exitPressedOnce = true;
                View rootView = findViewById(android.R.id.content);

                Snackbar snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_LONG);
                View snackbarView = snackbar.getView();

                int marginHorizontal = (int) (32 * getResources().getDisplayMetrics().density);
                int marginBottom = (int) (100 * getResources().getDisplayMetrics().density);

                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                params.leftMargin = marginHorizontal;
                params.rightMargin = marginHorizontal;
                params.bottomMargin = marginBottom;
                snackbarView.setLayoutParams(params);

                ShapeAppearanceModel shapeModel = new ShapeAppearanceModel()
                        .toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, (int) (24 * getResources().getDisplayMetrics().density))
                        .build();
                snackbarView.setBackground(new MaterialShapeDrawable(shapeModel));
                // Resolve surface color from active theme so the snackbar is theme-aware
                // (dark in dark mode, light in light mode) rather than always dark_surface.
                int surfaceColor = MaterialColors.getColor(BaseActivity.this,
                        com.google.android.material.R.attr.colorSurfaceVariant,
                        ContextCompat.getColor(BaseActivity.this, R.color.dark_surface));
                snackbarView.setBackgroundColor(surfaceColor);
                snackbarView.setPadding(24, 16, 24, 16);

                TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                textView.setText("Tap back again to exit");
                // Use onSurface color so text is readable regardless of theme.
                int onSurface = MaterialColors.getColor(BaseActivity.this,
                        com.google.android.material.R.attr.colorOnSurfaceVariant, Color.WHITE);
                textView.setTextColor(onSurface);
                textView.setTextSize(14);
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_exit_to_app, 0, 0, 0);
                textView.setCompoundDrawablePadding(16);
                textView.setGravity(Gravity.CENTER_VERTICAL);

                snackbar.setAction("EXIT", v -> {
                    exitHandler.removeCallbacksAndMessages(null);
                    finish();
                });
                snackbar.setActionTextColor(ContextCompat.getColor(BaseActivity.this, R.color.primary_green));

                snackbar.show();

                exitHandler.postDelayed(() -> exitPressedOnce = false, EXIT_DELAY_MS);
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (useEdgeToEdge()) {
            View statusBarBg = findViewById(R.id.status_bar_background);
            if (statusBarBg != null) {
                ViewCompat.setOnApplyWindowInsetsListener(statusBarBg, (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(0, systemBars.top, 0, 0);
                    return insets;
                });
            }
        }
    }
}
