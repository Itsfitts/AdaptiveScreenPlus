package com.wb.extrotator;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * 「[comment]」—— [comment]（display 0）[comment] TextureView [comment]，[comment]
 * [comment]：「[comment] / [comment]」[comment]「[comment] / [comment]」。
 *
 * <p>[comment] {@link CastService} [comment] + [comment]，[comment]；[comment]，
 * [comment]「[comment]」。
 *
 * <p>[comment]Yes[comment]：{@link #fitMirror()} [comment] TextureView [comment]<b>[comment]</b>[comment]
 * （[comment] + [comment]），[comment]Yes[comment] —— [comment]、[comment]。[comment]「[comment]」[comment]「[comment]」
 * [comment]，{@link #srcX}/{@link #srcY} [comment]，[comment]。
 *
 * <p>[comment] {@code willContinue} [comment]（[comment] = [comment]，[comment] = [comment]），
 * [comment] {@link TouchInjector}：[comment] Shizuku [comment]，[comment] {@code input} [comment]。
 */
public class CastActivity extends Activity implements CastService.Listener {

    private static final String TAG = "ExtCastUI";

    /** [comment] requestCode */
    private static final int REQ_CAPTURE = 4701;

    /**
     * [comment]。
     *
     * <p>[comment]「[comment]」[comment]（[comment] {@code DualScreen.oneKeyCast}）：[comment]Yes"[comment]
     * [comment]"，[comment]「[comment]」[comment]。[comment]。
     */
    public static final String EXTRA_FULLSCREEN = "fullscreen";
    /** [comment]（[comment]，[comment]「[comment]」[comment]） */
    public static final String EXTRA_AUTO_CAST = "auto_cast";

    /**
     * [comment]。
     *
     * <p>[comment]Yes"[comment]"[comment]Yes"[comment]"[comment] —— [comment] 1080 [comment]，
     * [comment]，6 [comment]，[comment]。
     */
    private static final float ZOOM_MAX = 6f;

    /**
     * [comment]，[comment]「[comment]」。
     *
     * <p>[comment]Yes[comment]，[comment]、[comment]、[comment]Yes[comment]。[comment]「[comment] DOWN」，
     * [comment] {@code abort()} [comment] —— [comment]Yes「[comment] + [comment]」，
     * <b>[comment]Yes[comment]</b>：[comment]，[comment]。
     *
     * <p>[comment]：[comment] ⇒ <b>[comment]</b>；[comment] touch slop ⇒
     * [comment]，[comment]（[comment]）；[comment] ⇒ [comment]，[comment]
     * [comment]，[comment]。
     *
     * <p>[comment]（[comment] 500ms，[comment]）。[comment] 200ms：
     * [comment]（[comment] 60ms [comment]），[comment]；[comment]Yes[comment]。
     */
    private static final long PRESS_GRACE_MS = 200L;

    /**
     * [comment]，[comment]Allow[comment]。
     *
     * <p>⚠ [comment]Yes「[comment]」，Yes[comment]：[comment]Yes[comment]<b>[comment]</b>
     * [comment]「[comment]」[comment] —— [comment] {@code wm size} [comment] 0.378s [comment] vd，[comment] 67.5%；
     * 0.56s [comment]Yes[comment]；3.0s [comment] 100% [comment]。[comment] {@code wm size} [comment] WindowManager、
     * [comment] SurfaceFlinger：{@code dumpsys window displays} [comment] {@code cur=} [comment]，[comment]。
     *
     * <p>1.2s = [comment]（[comment] 0.6s）[comment]。
     */
    private static final long RESIZE_SETTLE_MS = 1200;

    private TextView tvState, tvIdle, tvPath, btnToggle;
    private TextureView mirrorView;
    private FrameLayout mirrorBox;
    /** 「[comment]」「Allow[comment]」[comment] */
    private CheckBox cbResize, cbZoom;

    /* ------------- [comment] -------------
     * [comment] = [comment]（[comment] / [comment] / [comment] / [comment]）[comment]，[comment]，
     * [comment]，[comment]。[comment] {@link #setFullscreen}。
     */
    /** [comment]Yes[comment]Yes[comment] */
    private boolean fullscreen;
    /** [comment]（[comment] mirrorBox [comment]，[comment]） */
    private View rootCast, rowTop, rowOptions, colOps;
    /** [comment] —— [comment]，[comment] */
    private View rowMain;
    /** [comment] / [comment]，[comment] */
    private int padL, padT, padR, padB;
    private int marMainTop;

    private final Handler ui = new Handler(Looper.getMainLooper());

    /**
     * [comment] —— [comment]Yes[comment]，[comment]Yes[comment]。
     * [comment]（[comment] {@code dumpsys window displays}，[comment]，[comment]）。
     *
     * <p>⚠ [comment]<b>[comment]</b>「[comment]」：[comment] {@code wm size} [comment]，
     * [comment] 1080×2640、[comment] 1080×1920，[comment]。
     */
    private volatile int[] srcSize;

    /**
     * [comment] / [comment]。
     *
     * <p><b>[comment]</b>：srcSize [comment]"[comment]"[comment]（[comment]、
     * [comment]），[comment]<b>[comment]</b>[comment]（[comment]）[comment] —— [comment]Yes
     * "[comment]"[comment]：[comment]、[comment] 1920×1080 [comment]，[comment]Yes 1080×1920，
     * [comment]。
     */
    private DisplayManager dmCast;

    /** [comment]：[comment] onDisplayChanged [comment]，[comment] */
    private static final long REFIT_DEBOUNCE_MS = 900L;

    /**
     * [comment] wm size（「[comment]」）[comment]，[comment]"[comment]"：
     * [comment]Yes[comment]，[comment]，[comment]。
     */
    private static final long REFIT_QUIET_MS = 3000L;

    private final Runnable refitTask = this::refitInner;

    private final DisplayManager.DisplayListener innerWatcher =
            new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    /*
                     * ⚠ [comment]，[comment] displayId [comment]（[comment]、[comment]）；
                     * [comment] refitInner [comment] —— "[comment]"。
                     */
                    if (displayId != ExtScreen.INNER_ID) {
                        return;
                    }
                    ui.removeCallbacks(refitTask);
                    ui.postDelayed(refitTask, REFIT_DEBOUNCE_MS);
                }
            };

    private Surface surface;

    /** [comment]YesNo[comment] touch slop（[comment]/[comment]） */
    private float downX, downY;
    /** [comment] —— [comment]"[comment]"[comment]（[comment]） */
    private float lastX, lastY;
    private long downAt;
    private boolean moved;
    /**
     * "[comment]"[comment]。
     * [comment] = [comment]（[comment]）；[comment] = [comment]。
     */
    private boolean downSent;

    /* ---------------- [comment]：[comment] TouchInjector ----------------
     *
     * v3.19 [comment]"[comment] + [comment]"[comment]，[comment]Yes[comment]，
     * [comment]Yes[comment]。v3.20 [comment] {@link TouchInjector} [comment]。
     *
     * [comment]：[comment]"[comment]"[comment]"[comment]"，[comment]。
     */
    private TouchInjector inj;
    /** [comment]（[comment]） */
    private volatile boolean directPath;

    /* ------------- [comment] -------------
     * [comment] View [comment] {@code setScaleX/setScaleY/setTranslationX/Y}，[comment]
     * {@code TextureView.setTransform([comment])}：[comment]「[comment]」[comment]「[comment]」
     * Yes[comment]（ViewGroup.transformPointToViewLocal），[comment]「[comment]」[comment]「[comment]」[comment]；
     * [comment]，[comment]，[comment]、[comment]。
     * [comment] {@link #srcX} [comment]「[comment] ÷ [comment]」—— [comment]。
     */

    /** [comment]（1 = [comment]） */
    private float zoomS = 1f;
    /** [comment]，[comment]Yes[comment] */
    private float zoomTx, zoomTy;
    /** [comment]"[comment]"[comment]（[comment]，[comment]） */
    private long zoomLogAt;
    /** [comment] */
    private boolean pinch;
    /**
     * [comment]<b>[comment]</b>[comment]「[comment]」，[comment]。
     *
     * <p>[comment] {@link #pinch} [comment]：[comment]，{@code ACTION_POINTER_UP}
     * [comment] {@code pinch} [comment]，[comment]Yes[comment] {@code ACTION_UP} [comment]Yes[comment]，
     * [comment]「[comment]」[comment]、[comment]，<b>[comment]</b>。
     * [comment]。
     */
    private boolean pinchEnd;
    /** [comment] */
    private float pinchDist, pinchX, pinchY;

    /* ---------------- [comment] ---------------- */

    /**
     * [comment]，[comment]Yes[comment]Yes[comment]。
     *
     * <p>[comment]：[comment]（[comment] / [comment]）[comment]，
     * [comment]"[comment]Settings"[comment] —— [comment]。
     */
    private static boolean resizeApplied;
    /** [comment]（{@code null} = [comment]，[comment] reset） */
    private static String prevForced;
    /** [comment]，[comment]"[comment]Yes[comment]Yes[comment]" */
    private static String resizedTo;

    /**
     * [comment]Yes[comment]（{@link SystemClock#uptimeMillis}，0 = [comment]）。
     *
     * <p>[comment]Yes[comment]"[comment]" —— [comment] {@link #settleResize}。
     * [comment]：[comment]（[comment] / [comment]）[comment]，No[comment]。
     */
    private static long resizedAt;

    /**
     * "[comment]"（v3.20 [comment]）。
     *
     * <p>[comment]：{@link #onStop} [comment]"[comment]"。
     * [comment]「[comment]」[comment]，[comment] onStop ——
     * [comment]Yes[comment]，[comment]，[comment]。
     */
    private boolean castPending;

    /** [comment]Update[comment] —— [comment]"[comment]"[comment]Yes"[comment]"，[comment] */
    private int frameN;
    private long frameAt;

    /** [comment]"[comment]Yes[comment]Yes[comment]"[comment]，[comment]（[comment]） */
    private int slop;

    /* ------------- [comment]（v4.32） -------------
     * [comment]，[comment]：[comment]，
     * [comment]「[comment]」。[comment] {@link #dispatchTouchEvent}。
     */

    /** [comment]「[comment]」[comment]（[comment] GONE） */
    private View barExit;
    /** [comment]Yes[comment]（[comment]，[comment]"Yes[comment]Yes[comment]"） */
    private float pullDownY;
    /** [comment]"[comment]"[comment] —— [comment] */
    private boolean pulling;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cast);

        slop = Math.max(1, ViewConfiguration.get(this).getScaledTouchSlop());

        tvState = findViewById(R.id.tvCastState);
        tvIdle = findViewById(R.id.tvCastIdle);
        tvPath = findViewById(R.id.tvCastPath);
        btnToggle = findViewById(R.id.btnCastToggle);
        mirrorView = findViewById(R.id.mirrorView);
        mirrorBox = findViewById(R.id.mirrorBox);
        rootCast = findViewById(R.id.rootCast);
        rowTop = findViewById(R.id.rowCastTop);
        rowOptions = findViewById(R.id.rowCastOptions);
        colOps = findViewById(R.id.colCastOps);
        rowMain = findViewById(R.id.rowCastMain);
        if (rowMain != null && rowMain.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            marMainTop = ((ViewGroup.MarginLayoutParams) rowMain.getLayoutParams()).topMargin;
        }
        if (rootCast != null) {
            padL = rootCast.getPaddingLeft();
            padT = rootCast.getPaddingTop();
            padR = rootCast.getPaddingRight();
            padB = rootCast.getPaddingBottom();
        }

        ExtUi.info(this, R.id.iCastTop, R.string.ext_cast_title, R.string.cast_info);

        findViewById(R.id.btnCastClose).setOnClickListener(v -> finish());
        // [comment]：[comment]（[comment]，[comment]）
        findViewById(R.id.btnCastFull).setOnClickListener(v -> setFullscreen(true));
        btnToggle.setOnClickListener(v -> {
            if (CastService.ready()) {
                CastService s = CastService.get();
                if (s != null) {
                    s.stopCast(getString(R.string.cast_stopped));
                }
            } else {
                requestCast();
            }
        });

        // [comment]"[comment]"[comment] —— [comment]Yes[comment]
        findViewById(R.id.btnCastBack).setOnClickListener(
                v -> ExtScreen.key(ExtScreen.INNER_ID, "KEYCODE_BACK"));
        findViewById(R.id.btnCastHome).setOnClickListener(
                v -> ExtScreen.key(ExtScreen.INNER_ID, "KEYCODE_HOME"));
        /*
         * 「[comment]」= [comment]：[comment]，[comment]「[comment]Disable」[comment]
         * （[comment]Yes[comment]）。
         * [comment] `input -d 0 keyevent KEYCODE_APP_SWITCH` [comment] RecentsActivity。
         */
        findViewById(R.id.btnCastRecent).setOnClickListener(
                v -> ExtScreen.key(ExtScreen.INNER_ID, "KEYCODE_APP_SWITCH"));

        // [comment]：[comment]，[comment]
        barExit = findViewById(R.id.btnCastExitFull);
        if (barExit != null) {
            barExit.setOnClickListener(v -> {
                hideExitBar();
                setFullscreen(false);
            });
        }

        mirrorView.setOnTouchListener(this::onMirrorTouch);
        mirrorView.setSurfaceTextureListener(surfaceListener);
        mirrorBox.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> fitMirror());

        bindOptions();

        probeSrcSizeAsync();
        watchInnerDisplay();
        startInjector();
        updateUi(CastService.ready(), null);

        // [comment]：[comment] / [comment]（[comment]，[comment] handleEntry）
        handleEntry(getIntent(), true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        /*
         * ⚠ [comment]Yes singleInstance：[comment]、[comment]，`am start` [comment]Yes[comment]Yes
         * onCreate。[comment]，[comment] getIntent() [comment]Yes[comment]。
         */
        setIntent(intent);
        handleEntry(intent, false);
    }

    /**
     * [comment]（[comment]「[comment]」[comment]）。[comment]，[comment]。
     *
     * @param fresh true = [comment]。[comment]，[comment]、[comment]。
     */
    private void handleEntry(Intent it, boolean fresh) {
        if (it == null) {
            return;
        }
        if (it.getBooleanExtra(EXTRA_FULLSCREEN, false)) {
            setFullscreen(true);
        }
        if (!it.getBooleanExtra(EXTRA_AUTO_CAST, false) || CastService.ready()) {
            return;
        }
        if (fresh) {
            // [comment]：[comment]，[comment]Yes[comment]（[comment] resizeForOption）
            mirrorBox.post(this::requestCast);
        } else {
            requestCast();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        /*
         * [comment]，[comment] RotationService [comment]Settings —— [comment]
         * （[comment] ExtControlMode）。v3.20 [comment]：[comment]「[comment]」[comment]，
         * [comment]Yes[comment]，[comment]。
         */
        ExtControlMode.on();
        // [comment]（[comment]Yes[comment]）
        bindCast();
        /*
         * [comment]，[comment]「[comment]」[comment]，[comment] ——
         * onStop [comment]（[comment]），[comment]。
         */
        if (cbResize != null && cbResize.isChecked() && !CastService.ready() && mirrorBox != null) {
            mirrorBox.post(this::resizeForOption);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // ⚠ [comment]Yes[comment]，[comment] —— [comment]"[comment]"，
        // [comment]（[comment] abortChain [comment]）
        cancelPress();
        downSent = false;
        injAbort();
        ExtControlMode.off();
        CastService s = CastService.get();
        if (s != null) {
            s.setListener(null);
        }
        /*
         * ⚠ [comment]。[comment]Yes「[comment]」，[comment]
         * [comment]，[comment]；[comment] onStart [comment]。
         *
         * ⚠ [comment]「[comment]」[comment]：[comment]（[comment]）、[comment]
         * （castPending，[comment]Yes[comment]）。
         */
        if (!castPending && !CastService.ready()) {
            restoreCastResize();
        }
        /*
         * [comment]，[comment] —— [comment]Settings[comment]。
         * [comment] setFullscreen(false)，[comment]「[comment] / [comment]」[comment]，
         * [comment]。
         */
        applyCoverOverlays(true);
    }

    @Override
    protected void onDestroy() {
        stopInjector();
        CastService s = CastService.get();
        if (s != null) {
            s.setListener(null);
            s.detach();
            /*
             * [comment]（[comment]Yes[comment]）[comment] —— [comment]，
             * [comment]"[comment]"，[comment]。
             *
             * ⚠ [comment] isFinishing() [comment]：[comment] / [comment]，
             * [comment]、[comment]，[comment]。
             */
            if (isFinishing()) {
                s.stopCast(getString(R.string.cast_stopped));
            }
        }
        unwatchInnerDisplay();
        resetZoom();
        releaseSurface();
        /*
         * [comment]。
         * ⚠ [comment]：[comment]Yes[comment] CastService.stopCast，
         * [comment]（onCastState）[comment] onStop [comment]，[comment]。
         * restoreCastResize [comment]Yes[comment]（[comment]），[comment]。
         */
        if (isFinishing()) {
            restoreCastResize();
        }
        super.onDestroy();
    }

    // ------------------------------------------------------------------ [comment]

    private void requestCast() {
        MediaProjectionManager mpm =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (mpm == null) {
            ExtUi.toast(this, getString(R.string.cast_fail, "Text"));
            return;
        }
        if (!ShellRunner.isReady()) {
            // [comment] Shizuku [comment]，[comment]（[comment] grantProjection）——
            // [comment]「[comment]…」，[comment]。
            ExtUi.toast(this, getString(R.string.cast_no_shizuku_dialog));
        }
        /*
         * ⚠ [comment]Yes[comment]：[comment]，[comment]、[comment]。
         * [comment]Yes[comment]"[comment]"[comment]，[comment]；
         * [comment]（[comment]）[comment]，[comment]。
         */
        // [comment]，onStop [comment]（[comment] castPending）
        castPending = true;
        tvState.setText(getString(R.string.cast_preparing));
        applyCastResize(() -> {
            // [comment]"[comment]"，[comment]（[comment] grantProjection）
            grantProjection(() -> {
                try {
                    startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE);
                } catch (Throwable t) {
                    Log.w(TAG, "TextFailed: " + t);
                    castPending = false;
                    ExtUi.toast(this, getString(R.string.cast_fail, String.valueOf(t)));
                }
            });
        });
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]。
     *
     * <p>[comment] prefs —— [comment]Yes"[comment]"，[comment]Yes"[comment]Settings"，
     * [comment]（[comment]）。
     */
    private void bindOptions() {
        cbResize = findViewById(R.id.cbCastResize);
        cbZoom = findViewById(R.id.cbCastZoom);
        if (cbResize == null || cbZoom == null) {
            return;
        }
        cbResize.setChecked(ExtPrefs.castResize(this));
        cbZoom.setChecked(ExtPrefs.castZoom(this));
        cbResize.setOnCheckedChangeListener((v, on) -> {
            ExtPrefs.setCastResize(this, on);
            /*
             * [comment]（[comment]）——
             * [comment]，[comment]。
             */
            if (CastService.ready()) {
                ExtUi.toast(this, getString(R.string.cast_opt_resize_late));
                return;
            }
            /*
             * ⚠ [comment]，**[comment]**[comment]「[comment]」（v3.20 [comment]，[comment] resizeForOption）：
             * [comment]"[comment]"[comment]"[comment]"[comment]，[comment]Yes[comment]
             * [comment] —— [comment]，[comment]。
             */
            if (on) {
                resizeForOption();
            } else {
                // Cancel[comment]，[comment]
                restoreCastResize();
                srcSize = null;
                probeSrcSizeAsync();
            }
        });
        cbZoom.setOnCheckedChangeListener((v, on) -> {
            ExtPrefs.setCastZoom(this, on);
            if (!on) {
                // [comment]，[comment]（[comment]）
                resetZoom();
            }
        });
        /*
         * [comment]，[comment] —— [comment]「[comment]」[comment]，
         * [comment]Yes[comment]（[comment] resizeForOption）。
         * [comment] post [comment]Yes[comment]，[comment]（[comment]）。
         */
        if (cbResize.isChecked()) {
            mirrorBox.post(this::resizeForOption);
        }
    }

    /** 「Allow[comment]」[comment]（[comment]） */
    private boolean zoomOn() {
        return cbZoom != null && cbZoom.isChecked();
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * 「[comment]」[comment] —— <b>[comment]</b>，[comment]Yes[comment]「[comment]」[comment]。
     *
     * <p>[comment]Yes[comment]<b>[comment]</b>[comment]，[comment]
     * [comment] TextureView [comment]。[comment]「[comment]」[comment]，[comment]
     * 67.5% / 46.8%，[comment]；[comment]「[comment]、[comment]、[comment]」[comment]，[comment]，
     * [comment] 100% [comment]、[comment] 0。
     */
    private void resizeForOption() {
        if (cbResize == null || !cbResize.isChecked()) {
            return;
        }
        // [comment]（onCreate [comment]）[comment]
        if (mirrorBox != null && mirrorBox.getWidth() <= 0) {
            mirrorBox.post(this::resizeForOption);
            return;
        }
        applyCastResize(() -> {
            /*
             * [comment]，[comment] —— [comment]；
             * probeSrcSizeAsync [comment] fitMirror()，[comment]。
             */
            srcSize = null;
            probeSrcSizeAsync();
        });
    }

    /**
     * [comment]，[comment]。
     *
     * <p>[comment] = [comment] + [comment]，[comment]Yes[comment]，[comment]Yes {@link #rootCast}
     * [comment]；[comment] {@link #mirrorBox}，[comment]Yes[comment]。
     *
     * <p>⚠ [comment] {@link #mirrorBox} [comment]"[comment]"[comment]（v4.32 [comment]Yes[comment]）：
     * [comment]、[comment]，[comment]，[comment]。
     *
     * @return null = [comment]，[comment]
     */
    private int[] fullscreenBoxSize() {
        /*
         * ⚠ [comment] rootCast（v4.33 [comment]）：[comment]、[comment]，
         * [comment]Yes rootCast [comment]；[comment]，[comment]Yes"[comment]Yes[comment]"[comment]。
         * [comment]，[comment]。
         */
        if (rootCast != null && rootCast.getWidth() > 0 && rootCast.getHeight() > 0) {
            return new int[]{rootCast.getWidth(), rootCast.getHeight()};
        }
        return null;
    }

    /**
     * [comment]。
     *
     * <p>[comment]Yes[comment]（[comment] 1080×1920），[comment]，[comment]、
     * [comment]，[comment]；[comment]，[comment]。
     *
     * <p>⚠ [comment]Yes[comment]Settings（{@code wm size}），[comment]：[comment]，
     * [comment] reset [comment] —— [comment]（{@link ScreenSizeUtil#forced}），
     * [comment]。
     *
     * @param then [comment]（Success、Failed、[comment]，[comment]）[comment]
     */
    private void applyCastResize(Runnable then) {
        if (cbResize == null || !cbResize.isChecked()) {
            then.run();
            return;
        }
        /*
         * ⚠ [comment]「[comment]」[comment]，[comment]Yes[comment]（v4.32）。[comment]、[comment]，
         * [comment]、[comment]；[comment]，[comment] ——
         * [comment]Yes[comment]，[comment]。
         */
        int[] box = fullscreenBoxSize();
        final int bw = box == null ? 0 : box[0];
        final int bh = box == null ? 0 : box[1];
        new Thread(() -> {
            try {
                int base = 1080;
                int[] cur = srcSize;
                if (cur == null) {
                    cur = probeSrcSize();      // Text，Text
                }
                if (cur != null) {
                    // [comment]"[comment]"：[comment]，[comment]
                    base = Math.min(cur[0], cur[1]);
                }
                int[] fit = ScreenSizeUtil.fitToBox(bw, bh, base);
                if (fit == null) {
                    Log.w(TAG, "Text：Text（" + bw + "×" + bh + "）");
                } else {
                    if (!resizeApplied) {
                        prevForced = ScreenSizeUtil.forced();
                        resizeApplied = true;
                    }
                    boolean rotated = screenRotated90();
                    /*
                     * ⚠ [comment]（user_rotation = 1 / 3）。`wm size` [comment]Yes「[comment]」[comment]
                     * [comment]，[comment] 90° [comment]，[comment]Yes fit，[comment]
                     * [comment]。[comment]（[comment]，[comment]）。
                     */
                    int w = rotated ? fit[1] : fit[0];
                    int h = rotated ? fit[0] : fit[1];
                    /*
                     * ⚠ resizedTo [comment]Yes[comment]（[comment]Yes fit）：[comment]Yes「[comment]Settings[comment]
                     * [comment]Yes[comment]Yes[comment]」，[comment] ScreenSizeUtil.forced()（[comment]Yes[comment]）。
                     * [comment] fit [comment]，[comment]「[comment]」[comment]Deny[comment]。
                     */
                    final String want = w + "x" + h;
                    if (want.equals(ScreenSizeUtil.forced())) {
                        /*
                         * [comment]Yes[comment]（[comment]、[comment]）——**[comment]**：[comment]，
                         * [comment]，[comment]Yes[comment]（[comment] resizeForOption）。
                         * resizedAt [comment]，[comment]。
                         */
                        resizedTo = want;
                        Log.i(TAG, "Text：TextYes " + want + "，Text");
                    } else {
                        ScreenSizeUtil.write(w, h, 0);
                        resizedTo = want;
                        resizedAt = SystemClock.uptimeMillis();
                        Log.i(TAG, "Text → Text " + w + "×" + h + "（Text90°=" + rotated
                                + "）· Text " + fit[0] + "x" + fit[1] + " · Text " + bw + "×" + bh
                                + " · TextYes " + (prevForced == null ? "NoneText" : prevForced));
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed: " + t);
            }
            ui.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                /*
                 * [comment]，[comment] —— [comment] ensureSrcSize
                 * [comment]（[comment]）。[comment]，[comment]：
                 * [comment] srcSize [comment]Yes[comment]。
                 */
                srcSize = null;
                then.run();
            });
        }, "extrot-cast-resize").start();
    }

    /**
     * [comment]「[comment]」[comment]（[comment] {@link #RESIZE_SETTLE_MS}）。
     *
     * <p>[comment]Yes「[comment]」[comment]Yes「[comment] 1.2 [comment]」：[comment]Settings[comment]、[comment]、
     * [comment]，[comment]。
     *
     * <p>⚠ [comment]：[comment] 1.2 [comment]Yes ANR [comment]。
     *
     * @param then [comment]
     */
    private void settleResize(Runnable then) {
        final long left = RESIZE_SETTLE_MS - (SystemClock.uptimeMillis() - resizedAt);
        if (resizedAt == 0L || left <= 0L) {
            then.run();
            return;
        }
        Log.i(TAG, "Text：Text " + left + "ms（Text）");
        new Thread(() -> {
            SystemClock.sleep(left);
            ui.post(then);
        }, "extrot-cast-settle").start();
    }

    /**
     * [comment]（[comment]，[comment]）。
     *
     * <p>⚠ [comment]"[comment]Yes[comment]"[comment]：{@link #resizeApplied} Yes[comment]，
     * [comment]；[comment]，[comment]Yes[comment]。
     */
    private void restoreCastResize() {
        if (!resizeApplied) {
            return;
        }
        resizeApplied = false;
        final String prev = prevForced;
        prevForced = null;
        final String mine = resizedTo;
        resizedTo = null;
        new Thread(() -> {
            try {
                String now = ScreenSizeUtil.forced();
                if (mine != null && now != null && !mine.equals(now)) {
                    /*
                     * [comment]Yes[comment] —— [comment]（[comment]
                     * 「[comment]」[comment]）。[comment]：[comment]Yes"[comment]"，
                     * [comment]Yes"[comment]"。
                     */
                    Log.i(TAG, "Text（TextYes " + now + "），Text");
                    return;
                }
                String r = ScreenSizeUtil.restore(prev);
                Log.i(TAG, "Text → " + (r == null ? "" : r.trim())
                        + "（Text " + (prev == null ? "NoneText" : prev) + "）");
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed: " + t);
            }
        }, "extrot-cast-resize-back").start();
    }

    /**
     * [comment]Yes[comment]Yes[comment]。
     *
     * <p>[comment]Yes {@code settings get system user_rotation}：1 / 3 [comment]Yes[comment] 90°。
     * [comment] RotationService [comment]Yes[comment]（「[comment]」），
     * [comment] {@link ExtControlMode#on()}，[comment]。
     */
    private boolean screenRotated90() {
        try {
            String s = ShellRunner.run("settings get system user_rotation");
            if (s == null) {
                return false;
            }
            String t = s.trim();
            return "1".equals(t) || "3".equals(t);
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * [comment] Shizuku（shell [comment]）[comment]<b>[comment]</b>[comment] {@code PROJECT_MEDIA} [comment] allow。
     *
     * <p>[comment]Yes[comment]，Yes[comment]<b>[comment]</b>：[comment]
     * （[comment] {@code Requested w=1080 h=1374}），[comment] 748×654，「[comment] / Cancel」[comment]
     * [comment]，[comment]。appops [comment]Yes allow [comment] Activity [comment] RESULT_OK、[comment]。
     *
     * <p>[comment] {@code PROJECT_MEDIA} [comment]（[comment]、[comment]），[comment]；
     * [comment] {@link CastService} [comment]Default。
     *
     * <p>[comment]：[comment]，[comment]Yes[comment]。
     */
    private void grantProjection(Runnable then) {
        new Thread(() -> {
            String out = null;
            try {
                if (ShellRunner.isReady()) {
                    out = ShellRunner.run("appops set " + getPackageName()
                            + " PROJECT_MEDIA allow");
                }
            } catch (Throwable t) {
                Log.w(TAG, "PROJECT_MEDIA TextFailed: " + t);
            }
            final String res = out;
            ui.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (res != null) {
                    Log.i(TAG, "PROJECT_MEDIA Text → " + res.trim());
                }
                then.run();
            });
        }, "extrot-cast-grant").start();
    }

    /**
     * [comment] {@link CastService} [comment]，[comment]。
     *
     * <p>⚠ [comment] {@code onStart()} [comment]：[comment] {@code onActivityResult} [comment]，
     * [comment] {@code onStart()} [comment]，[comment] Service [comment] Activity [comment] ——
     * [comment]Yes[comment]，[comment]「[comment]…」、[comment]Yes「[comment]」。
     * [comment]，[comment] {@link #awaitCast()}。
     */
    private void bindCast() {
        CastService s = CastService.get();
        if (s != null) {
            s.setListener(this);
        }
        updateUi(CastService.ready(), CastService.ready() ? null : CastService.lastWhy());
        attachIfPossible();
    }

    /**
     * [comment]Yes[comment]（{@code startForegroundService} → [comment] {@code onCreate}），
     * [comment]，[comment]。
     *
     * <p>[comment]：2 [comment] Handler [comment] —— [comment]，
     * [comment]Yes[comment]，[comment]Yes[comment]「[comment]…」。
     */
    private void awaitCast() {
        final int[] tries = {0};
        Runnable probe = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (CastService.get() != null) {
                    bindCast();
                    return;
                }
                if (++tries[0] < 10) {
                    ui.postDelayed(this, 200L);
                }
            }
        };
        ui.postDelayed(probe, 120L);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        // [comment]YesDeny[comment]"[comment]"，onStop [comment]
        castPending = false;
        if (req != REQ_CAPTURE) {
            super.onActivityResult(req, res, data);
            return;
        }
        if (res != RESULT_OK || data == null) {
            ExtUi.toast(this, getString(R.string.cast_denied));
            updateUi(false, getString(R.string.cast_denied));
            // [comment]：[comment]，[comment]
            if (fullscreen) {
                setFullscreen(false);
            }
            return;
        }
        tvState.setText(getString(R.string.cast_preparing));
        // ⚠ [comment]：[comment]Yes[comment]，[comment]、[comment]。
        // [comment] dumpsys（[comment] Shizuku），[comment]。
        ensureSrcSize(() -> {
            /*
             * ⚠ [comment]：[comment] srcSize [comment]（[comment] probeSrcSizeAsync），
             * [comment] srcSize[0] [comment]。
             */
            final int[] wh = srcSize;
            if (wh == null) {
                updateUi(false, getString(R.string.cast_no_shizuku));
                return;
            }
            // [comment]，[comment] SurfaceFlinger，[comment]（[comment] RESIZE_SETTLE_MS）
            settleResize(() -> {
                /*
                 * [comment] 320 [comment]，[comment] 480：[comment]（[comment] 1199×999、[comment] vd [comment]Yes
                 * 1296×1080、dpi=320）[comment]**[comment]**，[comment]。
                 * [comment]「[comment] vd [comment] : vd [comment]」[comment] —— [comment] RESIZE_SETTLE_MS。
                 */
                CastService.start(this, res, data, wh[0], wh[1], 320);
                // [comment]、[comment]（[comment]Yes[comment]）
                awaitCast();
            });
        });
    }

    // ------------------------------------------------------------------ [comment] / [comment]

    /** [comment]"[comment] / [comment]"[comment]（[comment]，[comment] onDestroy [comment]） */
    private void watchInnerDisplay() {
        if (dmCast != null) {
            return;
        }
        dmCast = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (dmCast != null) {
            dmCast.registerDisplayListener(innerWatcher, ui);
        }
    }

    private void unwatchInnerDisplay() {
        if (dmCast != null) {
            try {
                dmCast.unregisterDisplayListener(innerWatcher);
            } catch (Throwable ignored) {
            }
            dmCast = null;
        }
        ui.removeCallbacks(refitTask);
    }

    /**
     * [comment]：[comment]，[comment]，[comment]。
     *
     * <p><b>[comment]</b>：[comment] —— [comment]Yes[comment]（[comment] 1920×1080），
     * [comment] 1080×1920 [comment]Yes[comment]，vd [comment]Yes[comment]，[comment]。
     *
     * <p>[comment]：[comment]（[comment] {@link #probeSrcSizeAsync} [comment]）、[comment]
     * [comment]（castPending，[comment]Yes[comment]）、[comment] wm size [comment] 3 [comment]。
     */
    private void refitInner() {
        if (isFinishing() || isDestroyed() || castPending || !CastService.ready()) {
            return;
        }
        if (resizedAt != 0L && SystemClock.uptimeMillis() - resizedAt < REFIT_QUIET_MS) {
            return;
        }
        new Thread(() -> {
            final int[] wh = probeSrcSize();
            ui.post(() -> {
                if (wh == null || isFinishing() || isDestroyed()) {
                    return;
                }
                final int[] old = srcSize;
                if (old != null && old[0] == wh[0] && old[1] == wh[1]) {
                    // [comment]（[comment] / [comment] onDisplayChanged）—— [comment]
                    return;
                }
                CastService s = CastService.get();
                boolean ok = s != null && s.refit(wh[0], wh[1]);
                Log.i(TAG, "Text " + (old == null ? "?" : old[0] + "×" + old[1])
                        + " → " + wh[0] + "×" + wh[1] + " · Text=" + ok);
                srcSize = wh;
                /*
                 * ⚠ [comment]Yes[comment]：[comment]，[comment]。[comment]，[comment]Yes
                 * "[comment]、vd [comment]Yes[comment]"，[comment]。
                 * [comment]：TextureView Yes[comment]。
                 */
                syncSurfaceSize();
                fitMirror();
            });
        }, "extrot-cast-refit").start();
    }

    /** [comment] */
    private void probeSrcSizeAsync() {
        if (srcSize != null) {
            return;
        }
        new Thread(() -> {
            final int[] wh = probeSrcSize();
            ui.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                /*
                 * ⚠ [comment]「[comment]」[comment]：[comment]Failed（Shizuku [comment] / [comment]）[comment] ——
                 * [comment] fitMirror [comment]（[comment]），[comment]（srcX/srcY [comment]Yes 0）。
                 */
                if (wh != null) {
                    srcSize = wh;
                    Log.i(TAG, "Text " + wh[0] + "×" + wh[1]);
                }
                fitMirror();
                updateUi(CastService.ready(), null);
            });
        }, "extrot-cast-size").start();
    }

    /** [comment]；[comment] */
    private void ensureSrcSize(Runnable then) {
        if (srcSize != null) {
            then.run();
            return;
        }
        new Thread(() -> {
            final int[] wh = probeSrcSize();
            ui.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (wh == null) {
                    // [comment]：[comment] srcSize [comment]
                    ExtUi.toast(this, getString(R.string.cast_no_shizuku));
                    updateUi(false, getString(R.string.cast_no_shizuku));
                    return;
                }
                srcSize = wh;
                then.run();
            });
        }, "extrot-cast-size").start();
    }

    /**
     * [comment] = [comment]。
     *
     * <p>[comment]Yes {@link ExtScreen#injectionSize}（[comment] {@code dumpsys window displays} [comment]
     * {@code cur=WxH}），[comment] {@code wm size} [comment]，[comment] {@code input -d 0} [comment]Yes[comment]。
     * ⚠ [comment] {@code getRealSize()} / {@code getMode()}：[comment]、
     * [comment]，[comment]。
     *
     * @return null = [comment]（Shizuku [comment]）
     */
    private int[] probeSrcSize() {
        try {
            if (ShellRunner.isReady()) {
                return ExtScreen.injectionSize(this, ExtScreen.INNER_ID);
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed: " + t);
        }
        return null;
    }

    /**
     * [comment]（[comment] + [comment]）。
     *
     * <p>TextureView Default[comment]<b>[comment]</b>[comment] —— [comment]Yes[comment]，
     * [comment]，[comment]。[comment]、[comment]。
     *
     * <p>[comment]"[comment]"[comment]"[comment]"[comment]，
     * [comment]Yes {@link #srcX} [comment]。
     */
    private void fitMirror() {
        final int[] wh = srcSize;
        if (wh == null || wh[0] <= 0 || mirrorView == null || mirrorBox == null) {
            return;
        }
        int bw = mirrorBox.getWidth(), bh = mirrorBox.getHeight();
        if (bw <= 0 || bh <= 0) {
            return;
        }
        /*
         * ⚠ [comment]：[comment]
         * （v4.32 [comment]：[comment] 1496，[comment] 1296），[comment]，[comment]
         * [comment]，[comment]Yes"[comment]"，[comment]。[comment] **[comment]**（decorView）[comment] ——
         * [comment]，[comment]。
         *
         * ⚠ [comment] getResources().getDisplayMetrics() [comment]（v4.33 [comment]）：[comment]
         * [comment]（[comment]Yes 1242 [comment] 1308），[comment]。
         */
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int capW = 0, capH = 0;
        if (getWindow() != null && getWindow().getDecorView() != null) {
            capW = getWindow().getDecorView().getWidth();
            capH = getWindow().getDecorView().getHeight();
        }
        if (capW > 0 && bw > capW) {
            Log.w(TAG, "Text（" + bw + " > " + capW + "），Text");
            bw = capW;
        }
        if (capH > 0 && bh > capH) {
            Log.w(TAG, "Text（" + bh + " > " + capH + "），Text");
            bh = capH;
        }
        float scale = Math.min((float) bw / wh[0], (float) bh / wh[1]);
        int w = Math.max(1, Math.round(wh[0] * scale));
        int h = Math.max(1, Math.round(wh[1] * scale));
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mirrorView.getLayoutParams();
        if (lp.width == w && lp.height == h) {
            // [comment]，[comment]（[comment]Yes）—— [comment]
            syncSurfaceSize();
            return;
        }
        /*
         * ⚠ [comment]Yes"[comment]"[comment] —— [comment]，[comment]，
         * [comment]。[comment]、[comment]、[comment]，[comment]Yes[comment]。
         */
        Log.i(TAG, "Text：Text " + bw + "×" + bh + " · Text " + wh[0] + "×" + wh[1]
                + " · Text " + w + "×" + h + "（scale=" + scale + "）");
        /*
         * ⚠ [comment]：v4.32 [comment]，[comment] ——
         * [comment]Yes"[comment]"[comment]"[comment]"[comment]。[comment]、[comment]、
         * [comment]，[comment]Yes[comment]。
         */
        int[] loc = new int[2];
        if (mirrorBox != null) {
            mirrorBox.getLocationOnScreen(loc);
        }
        Log.i(TAG, "Text：Text " + getWindow().getDecorView().getWidth()
                + "×" + getWindow().getDecorView().getHeight()
                + " · Text " + loc[0] + "," + loc[1]
                + " · Text " + dm.widthPixels + "×" + dm.heightPixels + " d=" + dm.density
                + " · Text " + zoomS + " · Text " + fullscreen);
        lp.width = w;
        lp.height = h;
        lp.gravity = Gravity.CENTER;
        mirrorView.setLayoutParams(lp);
        final int fw = w, fh = h;
        mirrorView.post(() -> {
            if (mirrorView != null) {
                Log.i(TAG, "Text：Text " + fw + "×" + fh + " · Text "
                        + mirrorView.getWidth() + "×" + mirrorView.getHeight()
                        + " Text " + mirrorView.getLeft() + "," + mirrorView.getTop());
                syncSurfaceSize();
            }
        });
        /*
         * [comment]，[comment]Yes[comment]（pivot [comment]、[comment]Yes[comment]）——
         * [comment]"[comment]、[comment]"。[comment]
         * （[comment]Yes[comment]）。
         */
        if (zoomS != 1f || zoomTx != 0f || zoomTy != 0f) {
            resetZoom();
        }
    }

    /**
     * [comment]「Default[comment]」[comment]**[comment]**（v4.35 [comment]Yes[comment]）。
     *
     * <p>⚠ [comment]Yes {@code srcSize}（[comment]），**[comment]Yes[comment]**。
     * [comment]、[comment] 1:1 [comment]：[comment]，
     * [comment]Yes[comment] —— [comment]，[comment]Yes"[comment]"。
     * [comment]，[comment]，[comment]Yes"[comment]"[comment]。
     *
     * <p>⚠ [comment]：TextureView [comment]
     * <b>[comment]</b>[comment]，[comment] {@code onSurfaceTextureSizeChanged} [comment]；
     * [comment]、[comment]，[comment] {@link #attachIfPossible()} [comment]。
     */
    private void syncSurfaceSize() {
        if (mirrorView == null) {
            return;
        }
        SurfaceTexture tex = mirrorView.getSurfaceTexture();
        if (tex == null) {
            return;
        }
        int[] wh = srcSize;
        int w = (wh != null && wh[0] > 0) ? wh[0] : mirrorView.getWidth();
        int h = (wh != null && wh[1] > 0) ? wh[1] : mirrorView.getHeight();
        if (w > 0 && h > 0) {
            tex.setDefaultBufferSize(w, h);
            Log.i(TAG, "Text " + w + "×" + h
                    + " · Text " + mirrorView.getWidth() + "×" + mirrorView.getHeight());
        }
    }

    /** [comment]Authorized → [comment] */
    private void attachIfPossible() {
        CastService s = CastService.get();
        if (s == null || surface == null || !surface.isValid()) {
            return;
        }
        // [comment]：[comment]，[comment]
        syncSurfaceSize();
        fitMirror();
        s.attach(surface);
    }

    private void releaseSurface() {
        if (surface != null) {
            try {
                surface.release();
            } catch (Throwable ignored) {
            }
            surface = null;
        }
    }

    private final TextureView.SurfaceTextureListener surfaceListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
                    releaseSurface();
                    surface = new Surface(st);
                    Log.i(TAG, "Text " + w + "×" + h);
                    attachIfPossible();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {
                    /*
                     * ⚠ [comment]，TextureView [comment]**[comment]**[comment] ——
                     * [comment]Yes[comment]（[comment] syncSurfaceSize [comment]），[comment]。
                     * [comment]Yes[comment]：[comment]，[comment]Yes[comment]，[comment]。
                     */
                    syncSurfaceSize();
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
                    CastService s = CastService.get();
                    if (s != null) {
                        // [comment] Surface，[comment]
                        s.detach();
                    }
                    releaseSurface();
                    return true;
                }

                /**
                 * [comment] —— [comment]，[comment]「[comment]」[comment]Yes[comment]
                 * （[comment] = [comment]；[comment] 60fps [comment] = [comment]）。
                 */
                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture st) {
                    frameN++;
                    long now = SystemClock.uptimeMillis();
                    if (frameAt == 0L) {
                        frameAt = now;
                    } else if (now - frameAt >= 1000L) {
                        Log.i(TAG, "Text " + frameN + " fps");
                        frameN = 0;
                        frameAt = now;
                    }
                }
            };

    // ------------------------------------------------------------------ [comment] → [comment]

    /**
     * [comment]，[comment]。
     *
     * <p>⚠ [comment] false：[comment]Yes[comment]，[comment]"[comment]"，
     * [comment]Yes"[comment]"，[comment]。
     */
    private boolean onMirrorTouch(View v, MotionEvent e) {
        if (pulling) {
            // [comment]"[comment]"（[comment]）：[comment]，[comment]
            return true;
        }
        if (!CastService.ready()) {
            return false;
        }
        float tx = e.getX(), ty = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                /*
                 * [comment]（v473）：[comment]，[comment]"[comment] / [comment] / [comment]"[comment]
                 * [comment] —— [comment]"[comment]、[comment]"，[comment]
                 * Yes[comment]Yes[comment]；[comment]，[comment]
                 * "[comment]([comment]) x,y"，[comment]Yes[comment]。
                 */
                Log.i(TAG, "Text v=" + (int) tx + "," + (int) ty + " · Text "
                        + (mirrorView == null ? 0 : mirrorView.getWidth()) + "×"
                        + (mirrorView == null ? 0 : mirrorView.getHeight()) + " · Text "
                        + (srcSize == null ? "?" : srcSize[0] + "×" + srcSize[1]));
                downX = tx;
                downY = ty;
                lastX = tx;
                lastY = ty;
                downAt = SystemClock.uptimeMillis();
                moved = false;
                pinch = false;
                pinchEnd = false;
                downSent = false;
                /*
                 * ⚠ [comment]**[comment]**[comment]，[comment] PRESS_GRACE_MS —— [comment]，
                 * [comment]Yes[comment]，[comment]（[comment] PRESS_GRACE_MS）。
                 */
                armPress();
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                /*
                 * [comment]：[comment]「[comment]」，[comment]。
                 * [comment] ⇒ [comment]，[comment]；[comment] ⇒ [comment]，
                 * [comment]Yes[comment]（[comment]，[comment] PRESS_GRACE_MS）。
                 */
                if (e.getPointerCount() >= 2) {
                    pinchEnd = true;
                    cancelPress();
                    if (downSent) {
                        injAbort();
                        downSent = false;
                    }
                    if (zoomOn()) {
                        startPinch(e);
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (pinch) {
                    movePinch(e);
                    return true;
                }
                // [comment]（[comment]「Allow[comment]」[comment]）：[comment]、[comment]
                if (pinchEnd) {
                    return true;
                }
                lastX = tx;
                lastY = ty;
                /*
                 * [comment] touch slop ⇒ [comment]Yes[comment]、[comment]Yes[comment]，[comment]。
                 * ⚠ [comment]：[comment]"[comment]"，
                 * [comment] press [comment] move [comment]"[comment]"，
                 * [comment]。
                 */
                if (!downSent && !moved
                        && Math.hypot(tx - downX, ty - downY) > slop) {
                    moved = true;
                    flushPress();
                    return true;
                }
                if (downSent) {
                    injMove(srcX(tx, ty), srcY(tx, ty));
                }
                return true;

            case MotionEvent.ACTION_POINTER_UP:
                // [comment]：[comment]（[comment]）
                if (pinch && e.getPointerCount() <= 2) {
                    pinch = false;
                    pinchDist = 0f;
                }
                return true;

            case MotionEvent.ACTION_UP:
                cancelPress();
                if (pinch || pinchEnd) {
                    pinch = false;
                    pinchEnd = false;
                    pinchDist = 0f;
                    Log.i(TAG, "Text → " + zoomText());
                    return true;      // TextYes"Text"，TextYesText
                }
                if (downSent) {
                    injRelease(srcX(tx, ty), srcY(tx, ty));
                    Log.d(TAG, (moved ? "Text " : "Text ") + srcX(tx, ty) + "," + srcY(tx, ty));
                } else {
                    /*
                     * [comment]、[comment] ⇒ [comment]Yes[comment]。
                     * [comment]，[comment]（DOWN + [comment] + UP）。
                     */
                    injTap(srcX(tx, ty), srcY(tx, ty));
                    Log.d(TAG, "Text(Text) " + srcX(tx, ty) + "," + srcY(tx, ty));
                }
                flashPath();
                return true;

            case MotionEvent.ACTION_CANCEL:
                // [comment]（[comment] / [comment]）：[comment]，
                // [comment]"[comment]"，[comment]
                cancelPress();
                pinch = false;
                pinchEnd = false;
                if (downSent) {
                    injAbort();
                    downSent = false;
                }
                return true;

            default:
                return false;
        }
    }

    // ------------------------------------------------------ [comment]

    /** [comment]：[comment]"[comment]"[comment]（[comment]） */
    private final Runnable pressTask = new Runnable() {
        @Override
        public void run() {
            if (downSent || pinchEnd || isFinishing() || isDestroyed()
                    || !CastService.ready()) {
                return;
            }
            downSent = true;
            injPress(srcX(lastX, lastY), srcY(lastX, lastY));
            Log.d(TAG, "Text(Text) " + srcX(lastX, lastY) + "," + srcY(lastX, lastY));
        }
    };

    /** [comment]，[comment]（[comment] {@link #PRESS_GRACE_MS}） */
    private void armPress() {
        ui.removeCallbacks(pressTask);
        ui.postDelayed(pressTask, PRESS_GRACE_MS);
    }

    private void cancelPress() {
        ui.removeCallbacks(pressTask);
    }

    /** [comment]：[comment]，[comment] */
    private void flushPress() {
        cancelPress();
        if (downSent) {
            return;
        }
        downSent = true;
        injPress(srcX(downX, downY), srcY(downX, downY));
        Log.d(TAG, "Text(Text) " + srcX(downX, downY) + "," + srcY(downX, downY));
    }

    // ------------------------------------------------------------------ [comment]（[comment]）

    /**
     * [comment]：[comment]。
     *
     * <p>[comment] —— [comment]（[comment]），
     * [comment]"[comment]"，[comment]。
     */
    private void startPinch(MotionEvent e) {
        pinch = true;
        pinchDist = spread(e);
        pinchX = (e.getX(0) + e.getX(1)) / 2f;
        pinchY = (e.getY(0) + e.getY(1)) / 2f;
    }

    /**
     * [comment]：[comment]"[comment]"[comment]，[comment]"[comment]"[comment]。
     *
     * <p>[comment]<b>[comment]</b>，[comment]Yes[comment] —— [comment]Yes[comment]，[comment]。
     * [comment]、[comment]"[comment]/[comment]"[comment] {@link #clampZoom} [comment]。
     */
    private void movePinch(MotionEvent e) {
        float d = spread(e);
        if (pinchDist <= 0f || d <= 0f) {
            startPinch(e);
            return;
        }
        float f = d / pinchDist;
        float cx = (e.getX(0) + e.getX(1)) / 2f;
        float cy = (e.getY(0) + e.getY(1)) / 2f;

        int vw = mirrorView == null ? 0 : mirrorView.getWidth();
        int vh = mirrorView == null ? 0 : mirrorView.getHeight();
        if (vw <= 0 || vh <= 0) {
            return;
        }
        final float s0 = zoomS;
        float s = Math.min(ZOOM_MAX, Math.max(1f, s0 * f));
        /*
         * [comment]（[comment]Yes[comment]）：
         *   ① [comment]：[comment]Yes[comment]"[comment]"[comment]，[comment]"[comment]"[comment]。
         *      [comment]Yes pinchX，[comment] (s0 − s) × ([comment] − [comment])。
         *   ② [comment]：[comment] s0 × ([comment])
         *      —— [comment] 1 [comment] s0 [comment]。
         */
        float nx = zoomTx + (s0 - s) * (pinchX - vw / 2f) + s0 * (cx - pinchX);
        float ny = zoomTy + (s0 - s) * (pinchY - vh / 2f) + s0 * (cy - pinchY);
        applyZoom(s, nx, ny);

        long now = SystemClock.uptimeMillis();
        if (now - zoomLogAt >= 200L) {
            zoomLogAt = now;
            // [comment]**[comment]** —— [comment]Yes[comment]
            Log.i(TAG, "Text " + zoomText() + " · Text → Text "
                    + srcX(cx, cy) + "," + srcY(cx, cy));
        }

        pinchDist = d;
        pinchX = cx;
        pinchY = cy;
    }

    /** [comment]（[comment] 0） */
    private static float spread(MotionEvent e) {
        if (e.getPointerCount() < 2) {
            return 0f;
        }
        return (float) Math.hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1));
    }

    /**
     * [comment]，[comment] View [comment]/[comment]。
     *
     * <p>[comment]：[comment] 1×~{@link #ZOOM_MAX}×，[comment]<b>[comment]</b>（[comment]
     * [comment]，[comment]，[comment] {@link #clampAxis}）。
     *
     * <p>⚠ setScale/setTranslation [comment]<b>[comment]</b>[comment]：[comment]
     * [comment]，[comment] {@code e.getX()} [comment]Yes[comment]。
     */
    private void applyZoom(float s, float tx, float ty) {
        int vw = mirrorView == null ? 0 : mirrorView.getWidth();
        int vh = mirrorView == null ? 0 : mirrorView.getHeight();
        int bw = mirrorBox == null ? 0 : mirrorBox.getWidth();
        int bh = mirrorBox == null ? 0 : mirrorBox.getHeight();
        if (vw <= 0 || vh <= 0 || bw <= 0 || bh <= 0) {
            // [comment]（[comment]）：[comment]"[comment]"，[comment] fitMirror [comment]
            zoomS = 1f;
            zoomTx = 0f;
            zoomTy = 0f;
            return;
        }
        zoomS = Math.min(ZOOM_MAX, Math.max(1f, s));
        zoomTx = clampAxis(tx, zoomS, vw, bw, mirrorView.getLeft());
        zoomTy = clampAxis(ty, zoomS, vh, bh, mirrorView.getTop());
        mirrorView.setPivotX(vw / 2f);
        mirrorView.setPivotY(vh / 2f);
        mirrorView.setScaleX(zoomS);
        mirrorView.setScaleY(zoomS);
        mirrorView.setTranslationX(zoomTx);
        mirrorView.setTranslationY(zoomTy);
    }

    /**
     * [comment]：[comment]Allow[comment] [[comment] − [comment]/2, [comment]/2] [comment]（[comment]）；
     * [comment]（[comment]「[comment]」[comment]Yes[comment]）[comment]，[comment]。
     *
     * @param t   [comment]（[comment]）
     * @param s   [comment]
     * @param vs  [comment]（[comment]）
     * @param bs  [comment]
     * @param off [comment]
     */
    private static float clampAxis(float t, float s, int vs, int bs, int off) {
        float span = s * vs;
        float c = off + vs / 2f + t;
        if (span >= bs) {
            if (c < bs - span / 2f) {
                c = bs - span / 2f;
            }
            if (c > span / 2f) {
                c = span / 2f;
            }
        } else {
            c = bs / 2f;
        }
        return c - off - vs / 2f;
    }

    /** [comment]（[comment] / [comment] / [comment]） */
    private void resetZoom() {
        pinch = false;
        pinchEnd = false;
        pinchDist = 0f;
        applyZoom(1f, 0f, 0f);
    }

    /** [comment]（[comment]） */
    private String zoomText() {
        return String.format(java.util.Locale.US, "%.2f× Text %.0f,%.0f",
                zoomS, zoomTx, zoomTy);
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment] —— [comment]（[comment] + [comment] + [comment]/[comment]）[comment]
     * {@link TouchInjector} [comment]，[comment]。
     *
     * <p>[comment] 0：[comment]Yes"[comment] DOWN"，[comment]
     * [comment]Yes[comment]，[comment]（[comment]，[comment] TouchInjector）。
     */
    private void startInjector() {
        if (inj != null) {
            return;
        }
        inj = new TouchInjector("ExtCastInject", ExtScreen.INNER_ID, 0L);
        inj.setStatus((direct, detail) -> {
            directPath = direct;
            ui.post(this::flashPath);
        });
        inj.start();
    }

    private void stopInjector() {
        if (inj != null) {
            inj.stop();
            inj = null;
        }
    }

    /** [comment]（UI [comment]，[comment]） */
    private void injPress(int x, int y) {
        if (inj != null) {
            inj.press(x, y);
        }
    }

    /** [comment]（UI [comment]，[comment] —— [comment]，[comment]） */
    private void injMove(int x, int y) {
        if (inj != null) {
            inj.move(x, y);
        }
    }

    /** [comment]（DOWN + [comment] + UP）—— [comment] */
    private void injTap(int x, int y) {
        if (inj != null) {
            inj.tap(x, y);
        }
    }

    /** [comment]（[comment]，[comment]） */
    private void injRelease(int x, int y) {
        if (inj != null) {
            inj.release(x, y);
        }
    }

    /** [comment]（[comment] / [comment]）—— [comment]，[comment] */
    private void injAbort() {
        if (inj != null) {
            inj.abort();
        }
    }

    /**
     * [comment] → [comment]：[comment] ÷ [comment] × [comment]，[comment]Yes[comment]
     * （[comment]Yes[comment]，[comment] {@link #fitMirror}）。
     *
     * <p>⚠ [comment]：{@code e.getX()} [comment]Yes[comment]，[comment]
     * [comment]（pivot / scale / translation）[comment]
     * （ViewGroup.transformPointToViewLocal），[comment]。
     */
    private int srcX(float vx, float vy) {
        final int[] wh = srcSize;
        int w = mirrorView == null ? 0 : mirrorView.getWidth();
        if (wh == null || w <= 0) {
            return 0;
        }
        float u = Math.min(1f, Math.max(0f, vx / w));
        return Math.round(u * wh[0]);
    }

    private int srcY(float vx, float vy) {
        final int[] wh = srcSize;
        int h = mirrorView == null ? 0 : mirrorView.getHeight();
        if (wh == null || h <= 0) {
            return 0;
        }
        float u = Math.min(1f, Math.max(0f, vy / h));
        return Math.round(u * wh[1]);
    }

    // ------------------------------------------------------------------ [comment]

    /* ------------- [comment]（v4.32） -------------
     * [comment]，[comment]：[comment]<b>[comment]</b>，
     * [comment]「[comment]」。[comment] {@link #dispatchTouchEvent}。
     */

    /** [comment]"[comment]"[comment]、[comment]：[comment]Yes 24dp */
    private float pullDp(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    /**
     * [comment] → [comment]「[comment]」[comment]。
     *
     * <p>⚠ [comment]，[comment]（[comment]、[comment] true）：[comment]
     * [comment]，[comment]Yes[comment]"[comment]"[comment]
     * （{@link #cancelPress} / {@link #injAbort}），[comment]"[comment]、[comment]"。
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (fullscreen && mirrorView != null) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    pullDownY = e.getY();
                    pulling = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!pulling && pullDownY <= pullDp(24)
                            && e.getY() - pullDownY > pullDp(24)) {
                        pulling = true;
                        cancelPress();
                        injAbort();
                        showExitBar();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    pulling = false;
                    break;
                default:
                    break;
            }
        }
        return super.dispatchTouchEvent(e);
    }

    /** [comment]「[comment]」[comment]（[comment]，[comment]） */
    private void showExitBar() {
        if (barExit == null || barExit.getVisibility() == View.VISIBLE) {
            return;
        }
        barExit.setVisibility(View.VISIBLE);
        barExit.setAlpha(0f);
        barExit.animate().alpha(1f).setDuration(140).start();
    }

    /** [comment]「[comment]」：[comment]、[comment]，[comment]Yes[comment] */
    private void hideExitBar() {
        if (barExit != null) {
            barExit.setVisibility(View.GONE);
        }
    }

    /**
     * [comment]。
     *
     * <p>[comment]（[comment] / [comment] / [comment] / [comment]）[comment]，[comment]；
     * [comment]，[comment]（[comment]）。
     * [comment]"[comment]"，[comment]，[comment]。
     *
     * <p>⚠ [comment]Yes<b>[comment]</b>[comment]（[comment] {@link #fitMirror}）：[comment]，
     * <b>[comment]</b>。[comment]「[comment]」—— [comment]，
     * [comment]，[comment]。
     */
    private void setFullscreen(boolean on) {
        fullscreen = on;
        final int v = on ? View.GONE : View.VISIBLE;
        if (rowTop != null) {
            rowTop.setVisibility(v);
        }
        if (rowOptions != null) {
            rowOptions.setVisibility(v);
        }
        if (colOps != null) {
            colOps.setVisibility(v);
        }
        if (tvState != null) {
            tvState.setVisibility(v);
        }
        if (rootCast != null) {
            rootCast.setPadding(on ? 0 : padL, on ? 0 : padT, on ? 0 : padR, on ? 0 : padB);
        }
        /*
         * [comment] 5dp [comment] —— [comment]Yes"[comment]，
         * [comment]Yes[comment]"，[comment]（v4.33）。
         */
        if (rowMain != null && rowMain.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp =
                    (ViewGroup.MarginLayoutParams) rowMain.getLayoutParams();
            mlp.topMargin = on ? 0 : marMainTop;
            rowMain.setLayoutParams(mlp);
        }
        // [comment]，[comment]Yes[comment]：[comment]，[comment]
        hideExitBar();
        applyFullscreenBars(on);
        applyCoverOverlays(!on);
        if (mirrorBox != null) {
            // [comment]，[comment]（[comment]Yes[comment] + [comment]）
            mirrorBox.post(this::fitMirror);
        }
        /*
         * ⚠ [comment]"[comment]"[comment]：[comment]Yes[comment]，
         * [comment]，[comment]（[comment]）。
         */
        if (cbResize != null && cbResize.isChecked() && !CastService.ready() && mirrorBox != null) {
            mirrorBox.post(this::resizeForOption);
        }
    }

    /**
     * [comment]：[comment]，[comment]。
     *
     * <p>⚠ [comment]"[comment]"（v4.32 [comment]）：[comment]"[comment]"
     * [comment]「[comment]」，[comment]（[comment]）；[comment]
     * [comment]，[comment]。
     */
    private void applyFullscreenBars(boolean on) {
        try {
            Window w = getWindow();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController c = w.getInsetsController();
                if (c == null) {
                    return;
                }
                /*
                 * ⚠ [comment]「[comment]」（v4.32 [comment]）：[comment]Yes"[comment]"，
                 * [comment]（[comment]），[comment]；
                 * [comment]，[comment]。
                 */
                if (on) {
                    c.hide(WindowInsets.Type.systemBars());
                } else {
                    c.show(WindowInsets.Type.systemBars());
                }
            } else if (on) {
                w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed: " + t);
        }
    }

    /**
     * [comment]（[comment]、[comment]）[comment]：[comment]，[comment]Settings[comment]。
     *
     * <p>[comment]：[comment]，[comment]<b>[comment]</b>，
     * [comment]（[comment]）。
     *
     * <p>[comment] {@code sync} —— [comment]，[comment]，
     * [comment]「[comment]Yes[comment]」。
     */
    private void applyCoverOverlays(boolean show) {
        try {
            if (!show) {
                CoverSidebar.remove();
                CoverRecentsGesture.remove();
                return;
            }
            AccessibilityService s = A11yInject.service();
            if (s == null) {
                // None[comment]，[comment]
                return;
            }
            CoverSidebar.sync(s);
            CoverRecentsGesture.sync(s);
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed: " + t);
        }
    }

    // ------------------------------------------------------------------ [comment]

    @Override
    public void onCastState(boolean projecting, String why) {
        ui.post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            updateUi(projecting, why);
            if (projecting) {
                castPending = false;    // Text，Text onStop
                attachIfPossible();
            } else {
                // [comment]Yes[comment]「[comment]」[comment]、[comment]Yes[comment]，[comment] ——
                // [comment]
                restoreCastResize();
                /*
                 * [comment]：[comment]Yes[comment]，[comment]，[comment]，
                 * [comment]，[comment]"[comment]"[comment]。
                 */
                if (fullscreen) {
                    setFullscreen(false);
                }
            }
        });
    }

    private void updateUi(boolean projecting, String why) {
        btnToggle.setText(projecting ? R.string.cast_stop : R.string.cast_start);
        tvIdle.setVisibility(projecting ? View.GONE : View.VISIBLE);
        if (why != null) {
            tvState.setText(why);
        } else if (projecting) {
            final int[] wh = srcSize;
            tvState.setText(getString(R.string.cast_state_fmt,
                    wh == null ? "?" : wh[0] + "×" + wh[1], pathText()));
        } else {
            tvState.setText(getString(R.string.cast_state_idle));
        }
        flashPath();
        fitMirror();
    }

    /**
     * [comment]（[comment]：[comment]"[comment]"）。
     *
     * <p>v3.19 [comment]：Shizuku [comment]（[comment]）[comment] {@code input} [comment]（[comment]）。
     * [comment]Yes"[comment] / [comment]"—— [comment]Yes"[comment]"[comment]，[comment]。
     */
    private String pathText() {
        /*
         * ⚠ [comment] directPath（[comment]Yes"[comment]Success[comment]"[comment]）——
         * [comment]、[comment]Yes[comment]，[comment]「[comment]」，
         * [comment]。[comment]"[comment]"[comment]，[comment]
         * （[comment] TouchpadActivity.pathText）。
         */
        return getString(directPath || TouchInject.ready()
                ? R.string.cast_path_direct : R.string.cast_path_input);
    }

    private void flashPath() {
        if (tvPath != null) {
            tvPath.setText(pathText());
        }
    }
}
