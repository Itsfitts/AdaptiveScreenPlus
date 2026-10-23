package com.wb.extrotator;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * 「[comment]」。
 *
 * <p>[comment]，[comment]：<b>[comment]</b> ⇒ [comment]；<b>[comment]、
 * [comment]</b> ⇒ [comment]。
 *
 * <p>[comment]，[comment]None[comment]
 * {@link WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY} [comment]。
 * [comment]Yes[comment]（[comment]Yes[comment]）；[comment] {@code FLAG_NOT_TOUCH_MODAL}，
 * [comment]。[comment]（[comment] {@link #passThrough()}）。
 *
 * <p><b>[comment]</b>（v4.21 [comment]、v461 [comment]）：<br>
 * · <b>[comment]</b>[comment] shell —— [comment]Yes「[comment]」[comment] {@code am start} [comment]（[comment] {@link #shellRecents}）。
 *   [comment] Shizuku。<br>
 * · <b>[comment] / [comment]</b>[comment] {@link AccessibilityService#performGlobalAction}，
 *   <b>[comment]None[comment]</b>，[comment] Shizuku、[comment]、[comment]。
 * [comment] {@link ExtScreen#tap}/{@link ExtScreen#swipe}
 * （[comment]YesNone[comment] {@link A11yInject}）。

 * <p>⚠ <b>[comment]</b>：[comment]
 * {@code com.sec.android.app.launcher} [comment]，Yes<b>[comment]</b> —— [comment]
 * （[comment]、{@code startActivity + setLaunchDisplayId} [comment]）[comment]
 * 「[comment]」（v456 [comment]，process/v456/logcat_cover.txt：
 * {@code Launching from CoverLauncher is blocked, ask to open phone}）。
 * shell [comment]。⇒ [comment]（v461）：[comment]，
 * [comment]。
 *
 * <p>⚠ {@code performGlobalAction} [comment] display [comment]，[comment]，[comment]
 * <b>[comment]</b> —— [comment] true、[comment]（v455 [comment]，
 * process/v456/logcat2.txt）。[comment] / [comment]，[comment]。
 *
 * <p>[comment]（Default 16dp ≈ 34px）；[comment] 1/N。
 *
 * <p>⚠ [comment]Yes shell [comment]：
 * [comment]，[comment]
 * {@code am force-stop com.sec.android.app.launcher} <b>[comment]</b>：「[comment]」[comment]Yes
 * launcher [comment]，[comment]、[comment]（[comment]「None[comment]
 * [comment]」，[comment] {@code dumpsys activity recents} [comment]）。[comment]YesSuccess[comment]、
 * [comment]，[comment]「[comment] force-stop [comment]」[comment] —— [comment]Yes[comment]。
 * [comment]Yes[comment]（[comment] 1 [comment]），[comment]。
 *
 * <p>⚠ [comment]「[comment]」（{@code dumpsys input} [comment] display 1 [comment]
 * {@code swipe-up} [comment] {@code extra-swipe-up} [comment] SPY [comment]），[comment]、
 * [comment]，[comment]「[comment]」。[comment]，[comment]Yes[comment]
 * [comment]「[comment]」[comment]「[comment]」（Settings[comment]）。
 *
 * <p>⚠ 「[comment]」[comment]Yes {@code now - lastMoveAt >= HOLD_MS}（[comment]），
 * <b>[comment]Yes</b> {@code now - downAt >= HOLD_MS}（[comment]）—— [comment]「[comment]」
 * [comment]。
 */
public final class CoverRecentsGesture {

    private static final String TAG = "CoverRecents";

    /**
     * [comment]（dp）。⚠ [comment]「[comment]」—— [comment]Yes[comment]，[comment]。
     * [comment]：[comment] 11dp(≈23px) / [comment] 16dp(≈34px) / [comment] 24dp(≈51px)，Default[comment]。
     */
    private static final int[] BAND_DP = {11, 16, 24};
    /** [comment]"[comment]"（dp）。[comment]，[comment] */
    private static final int SWIPE_DP = 24;     // v4.6：34 -> 24，Text
    /** [comment]"[comment]"（ms）。[comment]Yes**[comment]**，[comment]Yes"[comment]" */
    private static final long HOLD_MS = 140L;    // v4.6：300 -> 140
    /**
     * [comment]"[comment]"[comment] px，[comment]"[comment]"。
     *
     * <p>⚠ [comment]Yes<b>[comment]</b>，[comment]Yes[comment]。v4.6 [comment]Yes[comment]（6px），
     * [comment]：[comment]"[comment]"[comment]；[comment]、
     * [comment]，[comment] MOVE [comment]，[comment]Yes"[comment]"。
     * [comment]Yes[comment]"[comment]"。
     */
    private static final float MOVE_TOL_PX = 8f;
    /** [comment]（dp）[comment]Yes[comment]（[comment]） */
    private static final int DOWN_TOL_DP = 8;
    /** [comment]（dp）、[comment]，[comment]Yes[comment]，[comment] */
    private static final int SIDE_TOL_DP = 12;
    /** [comment]（ms）。[comment]，[comment] */
    private static final long BUZZ_MS = 22L;
    /** [comment]（1~255）。[comment]Default[comment]，[comment]"[comment]"[comment] */
    private static final int BUZZ_AMP = 140;
    /** [comment]（ms）：[comment]，[comment] */
    private static final long COOLDOWN_MS = 650L;
    /** [comment]（[comment]）[comment] / [comment]（dp）。[comment]Yes"[comment]"[comment] */
    private static final int BAR_W_DP = 92;
    private static final int BAR_H_DP = 4;
    /** [comment]（View alpha）：[comment] / [comment] / [comment] */
    private static final float BAR_REST = 0.34f;
    private static final float BAR_TOUCH = 0.78f;
    private static final float BAR_FULL = 1f;
    /** [comment]"[comment] → [comment] → [comment]"[comment]（ms） */
    private static final long BAR_FADE_OUT_MS = 150L;
    private static final long BAR_FADE_IN_MS = 280L;
    /**
     * [comment]：[comment] 1/N。
     *
     * <p>[comment]，[comment]Yes[comment] —— [comment]。
     * [comment]，[comment]。[comment]：[comment] 1/4 / [comment] 1/3（[comment]）/ [comment] 1/2 / [comment]。
     */
    private static final int[] ZONE_DIV = {4, 3, 2, 1};
    /**
     * 「[comment] 0° [comment]」[comment] 0°。
     *
     * <p>[comment]Yes {@code Surface.ROTATION_0} —— "[comment]"。[comment]Yes[comment]
     * （{@link CoverDisplay#ROT_NATIVE} = 180°），[comment] 0° Yes[comment]。
     */
    private static final int CORNER_ROTATION = 0;
    /** [comment]（dp）[comment]「[comment]」，[comment]；[comment]，[comment] */
    private static final int TAP_SLOP_DP = 8;
    /** [comment]：[comment]、[comment]，[comment] */
    private static final long REPLAY_ARM_MS = 60L;
    /** [comment]（[comment]） */
    private static final long REPLAY_STEP_MS = 24L;
    /** [comment]、[comment] */
    private static final long REPLAY_RESTORE_MS = 90L;

    private static final Handler UI = new Handler(Looper.getMainLooper());

    private static WindowManager wm;
    private static View strip;
    /** [comment]（"[comment]"）。[comment]，[comment] */
    private static View pill;
    /** [comment]，[comment]（[comment] Activity，[comment]） */
    private static Context appCtx;
    private static int shownDisplay = -1;
    /**
     * [comment]。
     *
     * <p>⚠ [comment] {@code Resources.getSystem()} [comment] dp —— [comment]Yes**Default[comment]（[comment]）**[comment]（480/160=3.0），
     * [comment]Yes 340dpi。[comment]：[comment]。
     */
    private static float density = 2.125f;
    private static float downX;
    private static float downY;
    private static float lastX;
    private static float lastY;
    private static float maxUp;
    private static long downAt;
    private static long lastMoveAt;
    /** [comment]"[comment]"[comment]：[comment] MOVE_TOL_PX [comment] */
    private static float stillRefX;
    private static float stillRefY;
    /** [comment]（[comment]、[comment]） */
    private static boolean reached;
    /**
     * [comment]，[comment]。
     *
     * <p>⚠ [comment]<b>[comment]</b>：[comment]、[comment] MOVE，
     * [comment]Yes[comment]，[comment]（v4.6 [comment]"[comment]"[comment]Yes[comment]）。
     */
    private static boolean holdOk;
    /** [comment]"[comment]Yes[comment]"[comment]（[comment] / [comment]），[comment] */
    private static boolean cancelled;
    private static boolean fired;
    /** [comment]，[comment]（[comment] COOLDOWN_MS） */
    private static long firedAt;
    /** [comment]（[comment]） */
    private static int screenW;
    private static int screenH;
    private static int winLeft;
    private static int winTop;
    /** [comment]。[comment] / [comment] FLAG_NOT_TOUCHABLE，[comment] */
    private static WindowManager.LayoutParams bandLp;
    /** [comment]（[comment]），[comment] */
    private static final float[] pathX = new float[9];
    private static final float[] pathY = new float[9];
    private static int pathN;
    /** [comment]（[comment]「Yes[comment]Yes[comment]」） */
    private static float maxDisp;
    /** [comment] ⇒ [comment]，[comment] */
    private static boolean replaying;
    /** [comment]（[comment]） */
    private static boolean passOff;
    /** [comment]Yes[comment]Yes[comment]（[comment] {@link #wantCornerLeft}） */
    private static boolean cornerLeft;
    /** [comment]：0° ↔ [comment] */
    private static DisplayManager rotationDm;
    private static DisplayManager.DisplayListener rotationListener;

    private CoverRecentsGesture() {
    }

    private static final Runnable holdFired = () -> {
        if (fired || cancelled || !reached || holdOk) {
            return;
        }
        // [comment]：[comment]（[comment]Yes[comment]Yes[comment]），[comment]。
        // [comment]（ACTION_UP）—— [comment]，[comment]。
        holdOk = true;
        Log.i(TAG, "Text（" + HOLD_MS + "ms Text）=> Text");
        barPress(BAR_FULL, 1.9f, 90);
    };

    /** [comment]（[comment] {@link #barFadeOutThenBack}） */
    private static final Runnable barReturn = () -> barPress(BAR_REST, 1f, BAR_FADE_IN_MS);

    /** [comment]（[comment]） */
    public static boolean installed() {
        return strip != null;
    }

    /**
     * [comment]Settings[comment]：[comment] ⇒ [comment]；No[comment]。
     * <p>[comment]、[comment]、[comment]，[comment]。
     */
    public static void sync(Context ctx) {
        boolean want = ExtPrefs.coverRecentsGesture(ctx);
        int display = CoverDisplay.id(ctx);
        Log.d(TAG, "sync want=" + want + " Text=" + display + " Text=" + installed());
        if (!want || display <= 0) {
            remove();
            return;
        }
        if (strip != null && shownDisplay == display) {
            return;
        }
        remove();
        install(ctx, display);
    }

    /**
     * Settings[comment]（[comment] / [comment]）⇒ [comment]。
     *
     * <p>{@link #sync} [comment]"[comment]"[comment]，[comment]。
     */
    public static void refresh(Context ctx) {
        remove();
        sync(ctx);
    }

    private static void install(Context ctx, int display) {
        if (!(ctx instanceof AccessibilityService)) {
            // TYPE_ACCESSIBILITY_OVERLAY [comment]None[comment]
            Log.w(TAG, "TextYesNoneText，Text");
            return;
        }
        try {
            DisplayManager dm = (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
            Display d = dm == null ? null : dm.getDisplay(display);
            if (d == null) {
                Log.w(TAG, "Text display " + display);
                return;
            }
            Context displayCtx = ctx.createDisplayContext(d);
            wm = (WindowManager) displayCtx.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return;
            }
            density = displayCtx.getResources().getDisplayMetrics().density;
            appCtx = ctx.getApplicationContext();

            final int band = bandDp(ctx);
            final int bandPx = dp(band);
            final int div = zoneDiv(ctx);

            // [comment]Yes[comment]，[comment] —— [comment]"[comment]"
            FrameLayout v = new FrameLayout(displayCtx);
            View bar = makeBar(displayCtx, dp(BAR_W_DP), dp(BAR_H_DP));
            FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                    dp(BAR_W_DP), dp(BAR_H_DP));
            blp.gravity = Gravity.CENTER;
            v.addView(bar, blp);
            v.setOnTouchListener((view, e) -> onTouch(view, e));

            // [comment] 1/N：[comment]，[comment]Yes[comment]，
            // [comment] —— [comment]，[comment]。
            // ⚠ [comment]「[comment]」[comment]，[comment]Yes app [comment]：[comment]Yes 748x654（[comment] 66px [comment]），
            // [comment]Yes[comment] Gravity.BOTTOM [comment]。[comment] winTop [comment]「[comment]」[comment]，
            // [comment] CoverBlur [comment]（RULES §64.1）：[comment] 654 [comment]，[comment] 66px。
            DisplayMetrics dmet = displayCtx.getResources().getDisplayMetrics();
            screenW = dmet.widthPixels;
            screenH = dmet.heightPixels;
            try {
                if (dm != null) {
                    Display real = dm.getDisplay(display);
                    if (real != null) {
                        Point pt = new Point();
                        real.getRealSize(pt);
                        if (pt.x > 0 && pt.y > 0) {
                            screenW = pt.x;
                            screenH = pt.y;
                        }
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed，Text", t);
            }
            final int winW = Math.max(1, Math.round(screenW / (float) div));
            // [comment] 0° [comment] ⇒ [comment]，No[comment]
            cornerLeft = wantCornerLeft(ctx, display);
            winLeft = cornerLeft ? 0 : (screenW - winW) / 2;
            winTop = screenH - bandPx;

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    winW,
                    bandPx,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    // [comment]（[comment]）＋ [comment]
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            // [comment]；[comment]"0° [comment]"[comment]Yes 0° [comment]，
            // No[comment]。[comment]Yes[comment]，[comment]，[comment]。
            lp.gravity = Gravity.BOTTOM
                    | (cornerLeft ? Gravity.START : Gravity.CENTER_HORIZONTAL);

            // [comment]：[comment]，[comment]
            CoverBlur.attach(displayCtx, wm, (AccessibilityService) ctx, display,
                    screenW, screenH);

            wm.addView(v, lp);
            strip = v;
            pill = bar;
            bandLp = lp;
            shownDisplay = display;
            listenRotation(ctx, display);
            Log.i(TAG, "Text display " + display + "，Text " + winW + "x"
                    + bandPx + "px（Text " + band + "dp，Text 1/" + div + "），Text "
                    + dp(BAR_W_DP) + "x" + dp(BAR_H_DP) + "px / Text " + screenW + "x"
                    + screenH + "px" + (cornerLeft ? "，Text（Text 0°）" : ""));
        } catch (Throwable t) {
            Log.e(TAG, "TextFailed", t);
            CoverBlur.detach();
            strip = null;
            pill = null;
            wm = null;
            bandLp = null;
            replaying = false;
            shownDisplay = -1;
        }
    }

    /**
     * [comment]。
     *
     * <p>public Yes[comment]None[comment] {@link VolumeKeyService} [comment] ——
     * [comment]，[comment]。
     */
    public static void remove() {
        View v = strip;
        WindowManager w = wm;
        strip = null;
        pill = null;
        wm = null;
        bandLp = null;
        shownDisplay = -1;
        appCtx = null;
        replaying = false;
        passOff = false;
        if (rotationDm != null && rotationListener != null) {
            try {
                rotationDm.unregisterDisplayListener(rotationListener);
            } catch (Throwable ignored) {
            }
        }
        rotationDm = null;
        rotationListener = null;
        cornerLeft = false;
        UI.removeCallbacksAndMessages(null);
        CoverBlur.detach();
        if (v != null && w != null) {
            try {
                w.removeViewImmediate(v);
                Log.i(TAG, "Text");
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed", t);
            }
        }
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]，[comment]。
     *
     * <p>[comment]：[comment]，<b>[comment]</b>[comment]Yes 0°。
     * [comment] —— [comment]Yes"[comment] 0° [comment]"。
     */
    private static boolean wantCornerLeft(Context ctx, int display) {
        if (ctx == null || !ExtPrefs.coverRecentsCornerLeft(ctx)) {
            return false;
        }
        return liveRotation(ctx, display) == CORNER_ROTATION;
    }

    /**
     * [comment]（0..3 = {@code Surface.ROTATION_*}；[comment] -1）。
     *
     * <p>[comment] {@link Display#getRotation()} [comment]Yes {@link CoverDisplay#currentRotation}：
     * [comment] shell（[comment]），[comment]。
     * [comment]，[comment]，[comment]。
     */
    private static int liveRotation(Context ctx, int display) {
        try {
            DisplayManager dm = (DisplayManager) ctx.getApplicationContext()
                    .getSystemService(Context.DISPLAY_SERVICE);
            Display d = dm == null ? null : dm.getDisplay(display);
            return d == null ? -1 : d.getRotation();
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * [comment]：0° [comment]，[comment]"[comment] / [comment]"[comment]。
     *
     * <p>[comment]、[comment] —— [comment]，{@code updateViewLayout}
     * [comment]，[comment]。
     */
    private static void listenRotation(Context ctx, int display) {
        if (rotationListener != null) {
            return;
        }
        try {
            DisplayManager dm = (DisplayManager) ctx.getApplicationContext()
                    .getSystemService(Context.DISPLAY_SERVICE);
            if (dm == null) {
                return;
            }
            rotationDm = dm;
            rotationListener = new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    if (displayId != shownDisplay) {
                        return;
                    }
                    UI.post(CoverRecentsGesture::applyCorner);
                }
            };
            dm.registerDisplayListener(rotationListener, UI);
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed（0° Text）", t);
        }
    }

    /** [comment] ⇒ [comment]（[comment] {@link #listenRotation}）；[comment] */
    private static void applyCorner() {
        final WindowManager w = wm;
        final WindowManager.LayoutParams p = bandLp;
        final View s = strip;
        if (w == null || p == null || s == null) {
            return;
        }
        boolean want = wantCornerLeft(appCtx, shownDisplay);
        if (want == cornerLeft) {
            return;
        }
        cornerLeft = want;
        p.gravity = Gravity.BOTTOM
                | (cornerLeft ? Gravity.START : Gravity.CENTER_HORIZONTAL);
        winLeft = cornerLeft ? 0 : Math.max(0, (screenW - p.width) / 2);
        winTop = screenH - p.height;
        try {
            w.updateViewLayout(s, p);
            Log.i(TAG, "Text " + (cornerLeft ? "0°" : "Text") + " ⇒ Text"
                    + (cornerLeft ? "Text" : "Text"));
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
    }

    // ------------------------------------------------------------------ [comment]

    private static boolean onTouch(View view, MotionEvent e) {
        final long now = System.currentTimeMillis();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // [comment]：[comment]Yes[comment]，[comment]
                if (replaying) {
                    return true;
                }
                downX = e.getX();
                downY = e.getY();
                lastX = downX;
                lastY = downY;
                maxUp = 0;
                maxDisp = 0;
                pathN = 0;
                record(downX, downY);
                downAt = now;
                lastMoveAt = now;
                stillRefX = downX;
                stillRefY = downY;
                reached = false;
                holdOk = false;
                fired = false;
                // [comment]、[comment] —— [comment]，[comment]
                passOff = now - firedAt < COOLDOWN_MS;
                UI.removeCallbacks(holdFired);
                UI.removeCallbacks(barReturn);
                if (passOff) {
                    cancelled = true;
                    Log.d(TAG, "Text（Text " + (now - firedAt) + "ms），Text");
                    return true;
                }
                cancelled = false;
                // [comment]，No[comment] MOVE。[comment]。
                barPress(BAR_TOUCH, 1.1f, 80);
                // [comment]：[comment]
                CoverBlur.prepare();
                return true;

            case MotionEvent.ACTION_MOVE: {
                if (replaying) {
                    return true;
                }
                if (cancelled) {
                    // [comment]「[comment]Yes[comment]」[comment]，[comment] —— [comment]
                    record(e.getX(), e.getY());
                    return true;
                }
                float up = downY - e.getY();
                float side = Math.abs(e.getX() - downX);
                if (up > maxUp) {
                    maxUp = up;
                }
                float disp = (float) Math.hypot(e.getX() - downX, e.getY() - downY);
                if (disp > maxDisp) {
                    maxDisp = disp;
                }
                record(e.getX(), e.getY());
                // ① [comment]（[comment]）[comment]（[comment]）⇒ [comment]Yes[comment]，[comment]
                if (up < -dp(DOWN_TOL_DP) || (side > dp(SIDE_TOL_DP) && side > up)) {
                    cancelled = true;
                    UI.removeCallbacks(holdFired);
                    CoverBlur.hide();
                    barPress(BAR_REST, 1f, 180);
                    Log.d(TAG, "Text：Text " + (int) up + "px Text " + (int) side + "px");
                    return true;
                }
                // ② [comment]：[comment]（[comment]）
                if (maxUp < dp(SWIPE_DP)) {
                    return true;
                }
                // [comment] => [comment]，[comment]
                CoverBlur.show(maxUp);
                if (!reached) {
                    // [comment]：[comment]、[comment] + [comment]
                    reached = true;
                    holdOk = false;
                    stillRefX = e.getX();
                    stillRefY = e.getY();
                    lastX = e.getX();
                    lastY = e.getY();
                    lastMoveAt = now;
                    barPress(BAR_FULL, 1.5f, 110);
                    UI.removeCallbacks(holdFired);
                    UI.postDelayed(holdFired, HOLD_MS);
                    return true;
                }
                // ③ [comment]"[comment]"[comment] MOVE_TOL_PX ⇒ [comment]：[comment]。
                //    [comment]（[comment] MOVE_TOL_PX）[comment]，[comment] holdOk。
                if (Math.abs(e.getY() - stillRefY) > MOVE_TOL_PX
                        || Math.abs(e.getX() - stillRefX) > MOVE_TOL_PX) {
                    stillRefX = e.getX();
                    stillRefY = e.getY();
                    lastX = e.getX();
                    lastY = e.getY();
                    lastMoveAt = now;
                    UI.removeCallbacks(holdFired);
                    UI.postDelayed(holdFired, HOLD_MS);
                }
                return true;
            }

            case MotionEvent.ACTION_UP: {
                UI.removeCallbacks(holdFired);
                // [comment]：[comment]（[comment]）
                CoverBlur.hide();
                final float upNow = downY - e.getY();
                // [comment]：[comment]，[comment]
                if (!cancelled && reached && holdOk) {
                    fired = true;
                    firedAt = now;
                    Log.i(TAG, "Text（Text " + (int) maxUp + "px，Text "
                            + (now - downAt) + "ms）=> Text，Text");
                    trigger();
                    reset();
                    return true;
                }
                // [comment] ⇒ [comment]。
                // [comment]「[comment]」，[comment]，
                // [comment]（[comment]，[comment]）。
                if (!cancelled && reached && upNow >= dp(SWIPE_DP) * 0.5f) {
                    fired = true;
                    firedAt = now;
                    Log.i(TAG, "Text（Text " + (int) maxUp + "px，Text "
                            + (now - downAt) + "ms）=> Text");
                    triggerHome();
                    reset();
                    return true;
                }
                Log.d(TAG, "Text：Text=" + reached + " Text=" + holdOk + " Text=" + cancelled
                        + " Text " + (int) maxUp + "px Text " + (int) maxDisp + "px Text "
                        + (now - downAt) + "ms");
                if (maxUp >= dp(DOWN_TOL_DP)) {
                    // [comment] => [comment]（[comment]）
                    barFadeOutThenBack(200L);
                } else {
                    // [comment]Yes[comment]：[comment]，[comment]
                    barPress(BAR_REST, 1f, 200);
                }
                // [comment]Yes[comment] ⇒ [comment]。
                // [comment]：[comment]，[comment]。
                if (!passOff) {
                    passThrough();
                }
                reset();
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
                if (replaying) {
                    return true;
                }
                UI.removeCallbacks(holdFired);
                CoverBlur.hide();
                Log.d(TAG, "Text，Text");
                barPress(BAR_REST, 1f, 220);
                reset();
                return true;

            default:
                return true;
        }
    }

    /** [comment]（[comment]，[comment]Yes[comment]） */
    private static void reset() {
        reached = false;
        cancelled = false;
    }

    /**
     * [comment]：[comment]<b>[comment]</b>[comment] —— [comment]，[comment]。
     */
    private static void trigger() {
        // [comment]，[comment]
        CoverBlur.invalidate();
        openRecentsNow(shownDisplay, "Text");
        // [comment]：[comment]，[comment]
        barFadeOutThenBack(620L);
    }

    /**
     * [comment]。
     *
     * <p>[comment]Yes[comment]「[comment]」。[comment]、
     * [comment]，[comment]「[comment]」。
     * [comment]「[comment]」—— [comment]，
     * [comment]，[comment]Yes[comment]：[comment]。
     */
    private static void triggerHome() {
        CoverBlur.invalidate();
        // [comment]：[comment]，[comment]
        barFadeOutThenBack(420L);
        goHome(shownDisplay);
    }

    // ------------------------------------------------------------------ [comment]（[comment] Shizuku）

    /** None[comment] */
    private static final int A_BACK = AccessibilityService.GLOBAL_ACTION_BACK;
    private static final int A_HOME = AccessibilityService.GLOBAL_ACTION_HOME;
    private static final int A_RECENTS = AccessibilityService.GLOBAL_ACTION_RECENTS;
    /**
     * [comment]，[comment]。
     *
     * <p>450ms Yes[comment]「[comment]」[comment] {@code COVER_LAUNCH_RECENTS_DELAY_MS}（0x1c2）[comment]：
     * [comment]、[comment]，[comment]、[comment]。
     */
    private static final long FALLBACK_MS = 450L;

    /**
     * [comment]None[comment]（[comment] / [comment] / [comment]）。
     *
     * <p>[comment]Yes[comment]<b>[comment]</b>[comment]：None[comment]
     * {@link AccessibilityService#performGlobalAction}，[comment]。
     *
     * <p>⚠ [comment] display [comment]，[comment] —— [comment]
     * （v455 [comment]：[comment] true、[comment]，process/v456/logcat2.txt）。
     * [comment]，[comment]。
     *
     * <p>⚠ [comment]<b>[comment]</b>：[comment]，[comment]。
     */
    private static boolean globalAction(int action, String what) {
        AccessibilityService s = A11yInject.service();
        if (s == null) {
            Log.w(TAG, what + "：NoneText，Text");
            return false;
        }
        try {
            boolean ok = s.performGlobalAction(action);
            Log.i(TAG, what + " => performGlobalAction(" + action + ") Text " + ok);
            return ok;
        } catch (Throwable t) {
            Log.w(TAG, what + "：performGlobalAction Text", t);
            return false;
        }
    }

    /**
     * [comment]（[comment]）。
     *
     * <p>⚠ [comment]<b>[comment]</b> —— [comment]Yes[comment] Activity，Yes[comment]
     * [comment]。[comment]；[comment]，[comment]。
     */
    public static void goBack() {
        buzz();
        globalAction(A_BACK, "Text");
    }

    /**
     * [comment]（[comment]）。
     *
     * <p>[comment]，[comment] {@link #FALLBACK_MS} [comment]「[comment]」——
     * [comment]（[comment] {@link #globalAction}）。
     */
    public static void goHome(final int display) {
        buzz();
        CoverPolicy.ensureLauncher(appCtx);
        globalAction(A_HOME, "Text");
        if (display <= 0) {
            return;
        }
        UI.postDelayed(() -> {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            if (!launchOnDisplay(display, home, "Text（Text）")) {
                shellHome(display);
            }
        }, FALLBACK_MS);
    }

    /**
     * [comment]<b>None[comment]</b>[comment] Intent [comment] {@code display} [comment]。
     *
     * <p>[comment]：{@code ActivityOptions.setLaunchDisplayId} Yes[comment] API，None[comment]
     * {@code startActivity} [comment] —— [comment]。
     * 「[comment]」[comment]Yes[comment]。
     *
     * @return true = [comment]（[comment]）
     */
    private static boolean launchOnDisplay(int display, Intent intent, String what) {
        AccessibilityService s = A11yInject.service();
        if (s == null) {
            Log.w(TAG, what + "：NoneText，Text");
            return false;
        }
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // [comment] —— [comment]
            // 「[comment]」（[comment] CoverPolicy [comment]）。[comment]
            // HOME / [comment]Yes[comment] launcher，[comment]。
            String pkg = intent.getComponent() != null
                    ? intent.getComponent().getPackageName() : intent.getPackage();
            CoverPolicy.ensure(appCtx, pkg != null ? pkg : CoverPolicy.LAUNCHER_PKG);
            ActivityOptions opts = ActivityOptions.makeBasic();
            opts.setLaunchDisplayId(display);
            s.startActivity(intent, opts.toBundle());
            Log.i(TAG, what + " => Text " + display + " Text：" + intent.getComponent());
            return true;
        } catch (Throwable t) {
            Log.w(TAG, what + "：TextFailed", t);
            return false;
        }
    }

    /** [comment]（[comment] Shizuku [comment]） */
    private static void shellHome(int display) {
        new Thread(() -> {
            String out = ShellRunner.run("am start --display " + display
                    + " -a android.intent.action.MAIN -c android.intent.category.HOME"
                    + " -f 0x10800000 2>&1", 12);
            Log.i(TAG, "Text（shell Text）=> " + out);
        }, "cover-home").start();
    }

    /**
     * [comment]<b>[comment]</b>[comment]（[comment]）。
     *
     * <p>[comment]、[comment]、[comment]，[comment]。
     *
     * <p>[comment]「[comment]」[comment] {@code triggerCoverRecents}：[comment]，[comment]
     * {@link #FALLBACK_MS} [comment]。[comment]YesNone[comment]，[comment]。
     *
     * <p>⚠ [comment]Yes {@code com.sec.android.app.launcher}，[comment]
     * 「[comment]」（v456 [comment] {@code Launching from CoverLauncher is blocked,
     * ask to open phone}）；manifest [comment] {@code cover_launcher_policy_app} [comment]
     * [comment]Yes[comment]。
     *
     * <p>⚠ [comment] shell Yes<b>[comment]</b>[comment]，[comment] Shizuku [comment]。
     * [comment]：[comment] {@code am force-stop com.sec.android.app.launcher; sleep 0.2;}
     * [comment]。[comment]Yes<b>[comment]</b>[comment]，[comment]Yes「[comment]」—— [comment]YesSuccess[comment]、[comment]，
     * [comment] launcher [comment]（[comment]）。
     */
    public static void openRecentsNow(final int display) {
        openRecentsNow(display, "Text");
    }

    private static void openRecentsNow(final int display, final String why) {
        if (display <= 0) {
            return;
        }
        buzz();
        // v461：[comment] shell（「[comment]」[comment]）。[comment]Yes**[comment]**，
        // [comment]「[comment]」；shell [comment]。
        if (ShellRunner.isReady()) {
            shellRecents(display, why);
            return;
        }
        // [comment] Shizuku [comment]：[comment] display [comment]、[comment]，
        // [comment]（[comment] globalAction [comment]），[comment]None。
        Log.w(TAG, why + "：Text Shizuku，Text（Text）");
        globalAction(A_RECENTS, why);
    }

    /** [comment]（[comment] shell，[comment]Yes「[comment]」[comment]；[comment] Shizuku [comment]） */
    private static void shellRecents(int display, String why) {
        new Thread(() -> {
            String cmd = "am force-stop com.sec.android.app.launcher; sleep 0.2; "
                    + "am start --display " + display
                    + " -a android.intent.action.MAIN -c android.intent.category.DEFAULT"
                    + " -f 0x10800000"
                    + " -n com.sec.android.app.launcher/com.android.quickstep.RecentsActivity"
                    + " >/dev/null 2>&1"
                    + " || input -d " + display + " keyevent 187"
                    + " || input keyevent 187";
            String out = ShellRunner.run(cmd, 20);
            Log.i(TAG, why + "（shell）=> " + out);
        }, "cover-recents").start();
    }

    /**
     * [comment]。
     *
     * <p>[comment]Default[comment]（{@code getDefaultVibrator}）[comment]Yes[comment]：
     * [comment]。[comment]
     * [comment] {@code IllegalArgumentException}，[comment]。
     */
    private static void buzz() {
        // ⚠ [comment]，[comment]、appCtx [comment]Yes[comment] ——
        //    [comment]None[comment]，[comment]（[comment]）。
        Context c = appCtx != null ? appCtx : A11yInject.service();
        if (c == null) {
            return;
        }
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager)
                        c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                v = vm == null ? null : vm.getDefaultVibrator();
            } else {
                v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (v == null || !v.hasVibrator()) {
                Log.d(TAG, "Text，Text");
                return;
            }
            // ⚠ VibrationEffect.createOneShot [comment]，[comment]。
            //    [comment]Yes[comment]，[comment]Default[comment]（-1）。
            int amp = v.hasAmplitudeControl() ? BUZZ_AMP : VibrationEffect.DEFAULT_AMPLITUDE;
            v.vibrate(VibrationEffect.createOneShot(BUZZ_MS, amp));
            Log.d(TAG, "Text " + BUZZ_MS + "ms amp=" + amp);
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed（Text）", t);
        }
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]（[comment]"[comment]"）。
     *
     * <p>[comment]，[comment] {@link View#setAlpha} [comment] —— [comment]
     * [comment]，[comment]。
     */
    private static View makeBar(Context c, int wPx, int hPx) {
        View v = new View(c);
        GradientDrawable d = new GradientDrawable();
        d.setColor(0xFFFFFFFF);
        d.setCornerRadius(hPx / 2f);
        v.setBackground(d);
        v.setAlpha(BAR_REST);
        return v;
    }

    /**
     * [comment]"[comment]"[comment]。
     *
     * @param stretchY [comment]（1 = [comment]）。[comment]，[comment]
     * @param ms       [comment]；[comment] 0 [comment]
     */
    private static void barPress(float alpha, float stretchY, long ms) {
        View p = pill;
        if (p == null) {
            return;
        }
        p.animate().cancel();
        if (ms <= 0) {
            p.setAlpha(alpha);
            p.setScaleY(stretchY);
            return;
        }
        p.animate().alpha(alpha).scaleY(stretchY).setDuration(ms).start();
    }

    /**
     * [comment]"[comment]"：[comment]，[comment]，[comment]。
     *
     * <p>[comment]Yes[comment] —— [comment]，[comment]。
     *
     * @param restDelayMs [comment]（[comment]）
     */
    private static void barFadeOutThenBack(long restDelayMs) {
        final View p = pill;
        if (p == null) {
            return;
        }
        UI.removeCallbacks(barReturn);
        p.animate().cancel();
        p.animate().alpha(0f).scaleY(1f).setDuration(BAR_FADE_OUT_MS).start();
        UI.postDelayed(barReturn, BAR_FADE_OUT_MS + restDelayMs);
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]。[comment] —— [comment]，[comment]。
     */
    private static void record(float x, float y) {
        if (pathN > 0) {
            float dx = x - pathX[pathN - 1];
            float dy = y - pathY[pathN - 1];
            if (dx * dx + dy * dy < 36f) {   // 6px Text
                return;
            }
        }
        if (pathN >= pathX.length) {
            return;
        }
        pathX[pathN] = x;
        pathY[pathN] = y;
        pathN++;
    }

    /**
     * [comment]。
     *
     * <p>[comment]，[comment]<b>[comment]</b>[comment] —— [comment]Yes[comment]。
     * [comment]「[comment]Yes[comment]」[comment]，[comment]，[comment]「[comment]」：
     * [comment]。
     *
     * <p>⚠ [comment]（[comment]Yes[comment]），[comment]
     * {@code FLAG_NOT_TOUCHABLE}，[comment]，[comment]。
     * [comment]（{@link #replaying}）。
     *
     * <p>⭐ [comment]<b>[comment] Shizuku</b>：[comment] {@link ExtScreen#tap}/{@link ExtScreen#swipe}，
     * [comment]YesNone[comment]（{@link A11yInject}，{@code dispatchGesture} [comment] displayId）；
     * [comment]None[comment]，[comment] {@code input}。
     *
     * <p>⚠ [comment] {@code FLAG_NOT_TOUCHABLE} [comment]：[comment]
     * [comment]，[comment]。
     */
    private static void passThrough() {
        final WindowManager w = wm;
        final WindowManager.LayoutParams p = bandLp;
        final View s = strip;
        if (w == null || p == null || s == null || replaying) {
            return;
        }
        // [comment]（[comment] + [comment]）
        final int n = Math.max(1, pathN);
        final int[] xs = new int[n];
        final int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = Math.round(winLeft + pathX[i]);
            ys[i] = Math.round(winTop + pathY[i]);
        }
        final boolean drag = maxDisp >= dp(TAP_SLOP_DP) && n > 1;
        final int display = shownDisplay;
        replaying = true;
        p.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        try {
            w.updateViewLayout(s, p);
        } catch (Throwable t) {
            replaying = false;
            Log.w(TAG, "TextFailed：Text", t);
            return;
        }
        new Thread(() -> {
            try {
                // [comment] FLAG_NOT_TOUCHABLE [comment]，[comment]
                Thread.sleep(REPLAY_ARM_MS);
                if (drag) {
                    // [comment]：[comment]，[comment]
                    ExtScreen.swipe(display, xs[0], ys[0], xs[n - 1], ys[n - 1],
                            (int) Math.max(60L, (n - 1) * REPLAY_STEP_MS));
                    Log.i(TAG, "Text（Text）：Text (" + xs[0] + "," + ys[0] + ") → ("
                            + xs[n - 1] + "," + ys[n - 1] + ")，Text " + (int) maxDisp + "px");
                } else {
                    ExtScreen.tap(display, xs[0], ys[0]);
                    Log.i(TAG, "Text（Text）：Text (" + xs[0] + "," + ys[0] + ")");
                }
                Thread.sleep(REPLAY_RESTORE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                Log.w(TAG, "Text", t);
            } finally {
                UI.post(CoverRecentsGesture::restoreTouchable);
            }
        }, "cover-passthru").start();
    }

    /** [comment]，[comment]。⚠ [comment]SuccessFailed[comment] */
    private static void restoreTouchable() {
        replaying = false;
        WindowManager w = wm;
        WindowManager.LayoutParams p = bandLp;
        View s = strip;
        if (w == null || p == null || s == null) {
            return;
        }
        p.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        try {
            w.updateViewLayout(s, p);
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
    }

    /** [comment]（[comment] 1/N，v4.7） */
    private static int zoneDiv(Context ctx) {
        int idx = ExtPrefs.coverRecentsZone(ctx);
        if (idx < 0 || idx >= ZONE_DIV.length) {
            idx = 1;
        }
        return ZONE_DIV[idx];
    }

    /** [comment]（dp） */
    private static int bandDp(Context ctx) {
        int idx = ExtPrefs.coverRecentsBand(ctx);
        if (idx < 0 || idx >= BAND_DP.length) {
            idx = 1;
        }
        return BAND_DP[idx];
    }

    private static int dp(float v) {
        return Math.round(v * density);
    }
}
