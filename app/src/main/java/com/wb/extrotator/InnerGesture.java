package com.wb.extrotator;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
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
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

/**
 * 「[comment]」：[comment]，[comment]。
 *
 * <p>[comment]<b>[comment]</b>[comment]（[comment]）：[comment] ⇒ [comment]；[comment] ⇒ [comment]；
 * [comment] ⇒ [comment]。
 *
 * <p><b>[comment]</b>：[comment]，[comment]None[comment]
 * {@link WindowManager.LayoutParams#TYPE_ACCESSIBILITY_OVERLAY} [comment]。[comment]Yes[comment]
 * [comment] —— [comment]，[comment]<b>[comment]</b>
 * （[comment] {@link Band#passThrough()}），No[comment]。
 *
 * <p><b>[comment]None[comment]</b>（{@link AccessibilityService#performGlobalAction}），
 * [comment] shell、[comment] Shizuku —— [comment]Default[comment]（[comment]），[comment] {@code input keyevent} [comment]。
 * [comment]No[comment]Yes[comment]：[comment]Yes[comment]<b>[comment]</b>[comment] DPAD [comment]、[comment]。
 *
 * <p>[comment]Yes[comment]，[comment] {@link GestureAnim}（[comment]，[comment] {@link ExtPrefs#gestureAnimStyle}）：
 * [comment] / [comment] / [comment]。
 *
 * <p>⚠ [comment]Yes[comment]「[comment]」[comment]。[comment]，[comment]
 * [comment]，[comment]。
 *
 * <p>⚠ 「[comment]」[comment]Yes {@code now - lastMoveAt >= HOLD_MS}（[comment]），<b>[comment]Yes</b>
 * {@code now - downAt >= HOLD_MS} —— [comment]「[comment]」[comment]。
 * [comment]（[comment] {@link CoverRecentsGesture}）。
 */
public final class InnerGesture {

    private static final String TAG = "InnerGesture";

    /** [comment]（dp）。[comment]：[comment]，[comment] */
    private static final int EDGE_W_DP = 20;
    /** [comment]（dp） */
    private static final int BOTTOM_H_DP = 20;
    /**
     * [comment]：<b>[comment]</b>，[comment]。
     *
     * <p>⚠ [comment]Yes<b>[comment]</b>：[comment]，[comment]「[comment]」
     * [comment]（[comment] 3 [comment]）。[comment]（[comment]）[comment]（[comment]）
     * [comment]，[comment] —— [comment]，[comment]。
     *
     * <p>[comment] 1/{@link #HOT_BOT_DEN}（[comment] 3/4 [comment]）；
     * [comment]、[comment] 1/{@link #HOT_SIDE_BOT_DEN}（[comment] 5/6）。
     * ⭐ [comment]，[comment]（[comment] {@code install()} [comment] {@code landscape} [comment]）。
     */
    private static final int HOT_BOT_DEN = 8;
    /** [comment]<b>[comment]</b>：[comment] 1/6，[comment] / [comment] */
    private static final int HOT_SIDE_BOT_DEN = 6;
    /**
     * ⭐ [comment]：[comment] px = [comment] ÷ [comment]。Default 9 ⇒ Z Flip5 [comment] 1080/9 = 120px。
     * <p>[comment] MyGesture（{@code sensitivity_size} Default 3 [comment] {@code n0 = [comment]/(3*2+3)}），
     * [comment]，[comment] dp [comment]（[comment] 34dp / [comment] 24dp [comment]）。
     * <p>⭐ [comment]Yes[comment]：[comment]Yes"[comment]、[comment]"，
     * [comment] —— [comment]"[comment]"[comment]"[comment]"。
     */
    private static final int TRIGGER_DEN = 9;
    /**
     * [comment]：[comment] × [comment] > [comment] ⇒ [comment]/[comment]，[comment]（MyGesture [comment] 1.08）。
     * <p>⚠ [comment] {@link #COMMIT_DP} [comment] —— [comment]。
     */
    private static final float AXIS_RATIO = 1.08f;
    /** [comment]（ms）⇒ [comment] */
    private static final long HOLD_MS = 140L;
    /** [comment]"[comment]"[comment] px，[comment]"[comment]" */
    private static final float MOVE_TOL_PX = 8f;
    /** [comment]（dp）[comment]Yes[comment]（[comment]） */
    private static final int DOWN_TOL_DP = 8;
    /**
     * [comment]（dp）：[comment]Yes[comment] —— [comment]/[comment]，
     * [comment]Yes"[comment]"，[comment]（[comment]）。
     * [comment] {@link #AXIS_RATIO} [comment]/[comment]。
     */
    private static final int COMMIT_DP = 12;
    /** [comment]「[comment]」[comment]Yes「[comment]」，[comment]Yes[comment] */
    private static final int TAP_SLOP_DP = 8;
    /**
     * [comment]「[comment]」[comment] {@link #DECIDE_SLOP_DP}，[comment]Yes[comment]，
     * [comment]、[comment]。
     *
     * <p>⚠ [comment]Yes[comment]：[comment]、[comment]。[comment]「[comment] 6 [comment]」
     * [comment]「[comment]」，[comment] —— [comment]，[comment] UP。
     *
     * <p>⭐ 70ms → 300ms，[comment]"[comment] 3dp"[comment]"[comment] slop"：
     * 3dp [comment]（[comment]，[comment]），70ms [comment]Yes[comment]
     * （[comment]-[comment]-[comment]，70ms [comment]，[comment]）。
     * 300ms [comment] MyGesture [comment] —— [comment]，[comment]Yes[comment]。
     */
    private static final long DECIDE_MS = 300L;
    /**
     * {@link #DECIDE_MS} [comment]，[comment]（dp）[comment]"[comment]" ⇒ [comment]。
     * <p>⚠ [comment]，[comment] —— [comment]，[comment]；
     * [comment]"[comment]"（[comment] CD），[comment] {@link #trigPx} [comment]。
     */
    private static final int DECIDE_SLOP_DP = 8;
    /** [comment] CANCEL/UP [comment]（ms），[comment] */
    private static final long LIVE_GUARD_MS = 3000L;
    /**
     * [comment]"[comment]"[comment]。
     *
     * <p>⚠ [comment]Yes<b>[comment]</b>[comment]，[comment]（{@link #trigPx}）[comment]：[comment]，
     * [comment]，[comment] 70% [comment]。
     * [comment]Yes 40%（{@code CoverBlur.FULL_AT_H}）—— [comment]"[comment]"，
     * [comment]，[comment]Yes[comment]。
     * ⚠ [comment] dp [comment]（130dp = [comment] 20%）[comment]，[comment]。
     */
    private static final float HOME_FULL_AT = 0.70f;
    /** [comment]（dp）[comment] —— [comment] */
    private static final int ANIM_ARM_DP = 6;
    /** [comment]（[comment]） */
    private static final long BUZZ_MS = 22L;
    private static final int BUZZ_AMP = 140;
    /** [comment]：[comment]、[comment] */
    private static final long COOLDOWN_MS = 650L;
    /** [comment]：[comment]、[comment] */
    private static final long REPLAY_ARM_MS = 60L;
    private static final long REPLAY_STEP_MS = 24L;
    private static final long REPLAY_RESTORE_MS = 90L;

    // ------------------------------------------------------------------ [comment]（[comment]）

    /** [comment]：[comment] ÷ [comment]，[comment] [{@link #BAR_W_MIN}, {@link #BAR_W_MAX}] —— [comment]，[comment] */
    private static final int BAR_W_DEN = 3;
    private static final int BAR_W_MIN = 300;
    private static final int BAR_W_MAX = 900;
    /** [comment]（dp）—— [comment]（4dp）[comment]，[comment]"[comment]"[comment] */
    private static final int BAR_H_DP = 5;
    /** [comment]（px）：[comment]；[comment] */
    private static final int SUMMON_HALF_PX = 150;
    /** [comment]：[comment]（px）[comment] ⇒ [comment] */
    private static final int SUMMON_PX = 80;
    /** [comment]：[comment] */
    private static final long SUMMON_TTL_MS = 3000L;
    /** [comment]"[comment]"[comment]：[comment] ⇒ [comment]Yes[comment]，[comment] */
    private static final long SUMMON_STALL_MS = 400L;
    /** [comment]（[comment]，[comment] {@link CoverRecentsGesture}） */
    private static final float BAR_REST = 0.34f;
    private static final float BAR_TOUCH = 0.78f;
    private static final float BAR_FULL = 1f;
    private static final long BAR_FADE_OUT_MS = 150L;
    private static final long BAR_FADE_IN_MS = 280L;
    private static final long BAR_REST_DELAY_MS = 180L;

    private static final Handler UI = new Handler(Looper.getMainLooper());

    private static WindowManager wm;
    /** [comment]，[comment]"[comment]"[comment] */
    private static AccessibilityService svc;
    private static float density = 3f;
    private static int screenW;
    private static int screenH;
    /** [comment]（px）：install [comment] {@link #TRIGGER_DEN} [comment] —— [comment]÷9 */
    private static int trigPx;
    /** [comment]（px）：install [comment] {@link #BAR_W_DEN} [comment]（[comment] 1/3，[comment] [min, max]） */
    private static int barW;
    /** [comment]Yes[comment]Yes「[comment]」[comment] ⇒ [comment]（[comment] → [comment]） */
    private static boolean gameMode;
    /** [comment]（3 [comment]，[comment] {@link #summonTtl}） */
    private static boolean summoned;
    /**
     * [comment]（[comment]）。
     *
     * <p>⭐ [comment]：[comment] {@link GestureAnim} [comment]，
     * [comment]（[comment] {@link #install}）—— [comment]。
     * [comment]Yes"[comment]"，[comment]。
     */
    private static View barTop;
    /**
     * [comment]<b>[comment]</b>：[comment] WindowManager [comment]Yes[comment]，[comment]Yes {@link #barTop} ——
     * [comment]（[comment] 1.9 [comment]），[comment] {@link #BAR_H_DP} [comment]。
     * remove [comment]Yes[comment]。
     */
    private static View barBox;
    /** [comment]（[comment]，[comment] {@link #initForeground}） */
    private static String fgPkg;

    /** [comment]：[comment]（[comment]） */
    private static final Runnable summonTtl = () -> {
        if (!summoned) {
            return;
        }
        summoned = false;
        applyBarMode();
        Log.i(TAG, "Text：Text " + SUMMON_TTL_MS + "ms NoneText，Text");
    };

    /** [comment]（[comment] {@link #barFadeOutThenBack()}） */
    private static final Runnable barReturn = () -> barPress(BAR_REST, 1f, BAR_FADE_IN_MS);

    /** [comment] + [comment] + [comment]。[comment]，[comment]（[comment]） */
    private static Band[] bands;

    private InnerGesture() {
    }

    /** [comment]（[comment]） */
    public static boolean installed() {
        return wm != null && bands != null;
    }

    /**
     * [comment]Settings[comment]：[comment] ⇒ [comment]；No[comment]。
     *
     * <p>[comment]、[comment]，[comment]。
     */
    public static void sync(Context ctx) {
        boolean want = ExtPrefs.innerGesture(ctx);
        Log.d(TAG, "sync want=" + want + " Text=" + installed());
        if (!want) {
            remove();
            return;
        }
        if (installed()) {
            return;
        }
        install(ctx);
    }

    /** [comment] ⇒ [comment]（[comment]） */
    public static void refresh(Context ctx) {
        remove();
        sync(ctx);
    }

    // ------------------------------------------------------------------ [comment] / [comment]（[comment]）

    /**
     * [comment]（None[comment]，[comment] {@link VolumeKeyService#onAccessibilityEvent}）。
     *
     * <p>[comment]；[comment]「[comment]」[comment]：[comment] ⇒ [comment]
     * （[comment]），[comment] ⇒ [comment]。[comment]，[comment]。
     */
    public static void onForegroundApp(Context ctx, String pkg) {
        if (pkg == null || pkg.equals(fgPkg)) {
            return;
        }
        fgPkg = pkg;
        if (!installed()) {
            return;
        }
        final boolean want = ExtPrefs.gameGestureOn(ctx, pkg);
        if (want == gameMode) {
            return;
        }
        gameMode = want;
        summoned = false;
        UI.removeCallbacks(summonTtl);
        applyBarMode();
        Log.i(TAG, "Text：" + pkg + " ⇒ " + (want ? "Text（Text）" : "Text"));
    }

    /**
     * [comment]。
     *
     * <p>[comment]：[comment]（[comment]Settings[comment]None[comment]），[comment]
     * [comment]，[comment]，[comment]。
     */
    public static void initForeground(AccessibilityService s) {
        if (s == null) {
            return;
        }
        try {
            AccessibilityNodeInfo root = s.getRootInActiveWindow();
            if (root == null) {
                return;
            }
            CharSequence p = root.getPackageName();
            if (p != null) {
                onForegroundApp(s, p.toString());
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed（Text，Text）", t);
        }
    }

    /** 「[comment]」[comment]（Beta [comment]）：[comment] */
    public static void reevalMode(Context ctx) {
        if (!installed()) {
            return;
        }
        final boolean want = fgPkg != null && ExtPrefs.gameGestureOn(ctx, fgPkg);
        if (want == gameMode) {
            return;
        }
        gameMode = want;
        summoned = false;
        UI.removeCallbacks(summonTtl);
        applyBarMode();
        Log.i(TAG, "Text ⇒ " + (want ? "Text（Text）" : "Text"));
    }

    /** [comment] */
    private static Band bottomBand() {
        Band[] bs = bands;
        return bs == null ? null : bs[0];
    }

    /**
     * [comment]：[comment] barW、[comment]。
     *
     * <p>⭐ [comment]（[comment]）：[comment]（[comment]）[comment] {@link GestureAnim}
     * [comment]，[comment]Yes[comment]（[comment] {@link #install}）—— [comment]
     * [comment]，[comment]。[comment]Yes"[comment]"，
     * [comment]：[comment]<b>[comment]</b>，[comment]。
     *
     * <p>[comment] 3 [comment]（[comment] 1.9 [comment]），[comment] pivot [comment] —— setScaleY
     * [comment]，[comment]。
     */
    private static void installBarTop(Context displayCtx, WindowManager w) {
        try {
            final int hPx = Math.max(1, dp(BAR_H_DP));
            // [comment]：[comment] 3 [comment]（[comment] 1.9 [comment]）。clipChildren [comment] ——
            //  pivot [comment]、[comment]，[comment]。
            FrameLayout box = new FrameLayout(displayCtx);
            box.setClipChildren(false);
            View bar = new View(displayCtx);
            GradientDrawable d = new GradientDrawable();
            d.setColor(0xFFFFFFFF);
            d.setCornerRadius(hPx / 2f);
            bar.setBackground(d);
            bar.setAlpha(0f);
            // ⚠⚠ [comment] hPx [comment]：WindowManager.addView [comment] params
            //  **[comment]**[comment] View，[comment] addView(bar, p) [comment] 15dp（[comment]）
            //  —— [comment]"[comment]"[comment]Yes[comment]。[comment] params [comment]，bar [comment]
            //  FrameLayout.LayoutParams [comment]，[comment]、[comment]。
            FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(barW, hPx);
            blp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            box.addView(bar, blp);
            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    barW, hPx * 3,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    // [comment]、[comment]（[comment]）；[comment] LAYOUT [comment]，[comment]
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            // ⚠ [comment] gravity：[comment] y Yes"[comment]"[comment]（[comment]）
            p.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            p.y = dp(3);
            w.addView(box, p);
            // pivot [comment]：[comment]，[comment]（[comment]，[comment] barPress [comment]）
            bar.setPivotY(hPx);
            barTop = bar;
            barBox = box;
            Log.i(TAG, "Text（Text，Text）：" + barW + "x" + hPx + "px");
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed（Text）", t);
            barTop = null;
            barBox = null;
        }
    }

    /**
     * [comment]：[comment]（[comment]）；[comment]、[comment]。
     *
     * <p>[comment]、[comment]、[comment]。
     */
    private static void applyBarMode() {
        final View p = barTop;
        if (p == null) {
            return;
        }
        p.animate().cancel();
        p.setScaleY(1f);
        p.setAlpha(gameMode ? (summoned ? BAR_FULL : 0f) : BAR_REST);
    }

    /**
     * [comment]"[comment]"[comment]（[comment]，[comment]
     * {@link CoverRecentsGesture#barPress}）。
     *
     * @param stretchY [comment]（1 = [comment]）。[comment]，[comment]
     * @param ms       [comment]；[comment] 0 [comment]
     */
    private static void barPress(float alpha, float stretchY, long ms) {
        final View p = barTop;
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
     * <p>[comment]Yes[comment]（[comment]）—— [comment]，
     * [comment]。[comment]（[comment]，[comment] 3 [comment]）。
     */
    private static void barFadeOutThenBack() {
        final View p = barTop;
        if (p == null || gameMode) {
            return;
        }
        UI.removeCallbacks(barReturn);
        p.animate().cancel();
        p.animate().alpha(0f).scaleY(1f).setDuration(BAR_FADE_OUT_MS).start();
        UI.postDelayed(barReturn, BAR_FADE_OUT_MS + BAR_REST_DELAY_MS);
    }

    /** [comment]（[comment] x/y、[comment]Yes[comment]） */
    private static DisplayManager.DisplayListener displayListener;

    /** [comment]。[comment]Yes[comment] */
    private static final Runnable REBUILD = () -> {
        AccessibilityService s = svc;
        if (s != null) {
            Log.i(TAG, "Text，Text");
            refresh(s);
        }
    };

    /**
     * [comment]。
     *
     * <p>⚠ [comment]Yes[comment]（[comment] {@code screenW - edgeW}）。
     * [comment]，[comment]：[comment] —— [comment]、
     * [comment]，[comment]。[comment]。
     */
    private static void watchDisplay(final AccessibilityService s) {
        if (displayListener != null) {
            return;
        }
        DisplayManager dm = (DisplayManager) s.getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) {
            return;
        }
        DisplayManager.DisplayListener l = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
            }

            @Override
            public void onDisplayRemoved(int displayId) {
            }

            @Override
            public void onDisplayChanged(int displayId) {
                if (displayId != Display.DEFAULT_DISPLAY) {
                    return;
                }
                // [comment]、[comment] —— [comment]
                if (!sizeChanged(s)) {
                    return;
                }
                // ⚠ [comment]，[comment]
                UI.removeCallbacks(REBUILD);
                UI.postDelayed(REBUILD, 350L);
            }
        };
        try {
            dm.registerDisplayListener(l, UI);
            displayListener = l;
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed（Text）", t);
        }
    }

    /** [comment]？ */
    private static boolean sizeChanged(Context ctx) {
        try {
            DisplayManager dm =
                    (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
            Display d = dm == null ? null : dm.getDisplay(Display.DEFAULT_DISPLAY);
            if (d == null) {
                return false;
            }
            Point pt = new Point();
            d.getRealSize(pt);
            return pt.x > 0 && pt.y > 0 && (pt.x != screenW || pt.y != screenH);
        } catch (Throwable t) {
            return false;
        }
    }

    /** [comment]。[comment]，No[comment] listener [comment] */
    private static void unwatchDisplay(Context ctx) {
        DisplayManager.DisplayListener l = displayListener;
        displayListener = null;
        if (l == null || ctx == null) {
            return;
        }
        try {
            DisplayManager dm =
                    (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
            if (dm != null) {
                dm.unregisterDisplayListener(l);
            }
        } catch (Throwable t) {
            Log.w(TAG, "CancelTextFailed", t);
        }
    }

    private static void install(Context ctx) {
        if (!(ctx instanceof AccessibilityService)) {
            // TYPE_ACCESSIBILITY_OVERLAY [comment]None[comment]
            Log.w(TAG, "TextYesNoneText，Text");
            return;
        }
        try {
            final AccessibilityService s = (AccessibilityService) ctx;
            DisplayManager dm = (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
            Display d = dm == null ? null : dm.getDisplay(Display.DEFAULT_DISPLAY);
            if (d == null) {
                Log.w(TAG, "TextDefaultText");
                return;
            }
            Context displayCtx = ctx.createDisplayContext(d);
            WindowManager w = (WindowManager) displayCtx.getSystemService(Context.WINDOW_SERVICE);
            if (w == null) {
                return;
            }
            density = displayCtx.getResources().getDisplayMetrics().density;

            // ⚠ [comment]「[comment]」[comment]，[comment]Yes app [comment] —— [comment] px
            //（RULES §64.1 [comment]，[comment] 66px）。[comment] FLAG_LAYOUT_IN_SCREEN [comment]，
            // [comment]，No[comment]。
            DisplayMetrics met = displayCtx.getResources().getDisplayMetrics();
            screenW = met.widthPixels;
            screenH = met.heightPixels;
            try {
                Point pt = new Point();
                d.getRealSize(pt);
                if (pt.x > 0 && pt.y > 0) {
                    screenW = pt.x;
                    screenH = pt.y;
                }
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed，Text", t);
            }

            // ⭐ [comment]（px）= [comment] ÷ TRIGGER_DEN —— [comment] MyGesture
            //（sensitivity_size Default 3 [comment] n0 = [comment]/(3*2+3) = [comment]/9）。
            // [comment]Yes[comment]：[comment]Yes"[comment]、[comment]"，
            // [comment]；[comment]，[comment] dp [comment]。
            trigPx = Math.max(1, Math.min(screenW, screenH) / TRIGGER_DEN);

            // [comment]（px）：[comment] ÷ BAR_W_DEN，[comment] [BAR_W_MIN, BAR_W_MAX]
            // —— [comment]，[comment]（[comment]）
            barW = Math.max(BAR_W_MIN, Math.min(BAR_W_MAX, screenW / BAR_W_DEN));

            final int edgeW = Math.max(1, dp(EDGE_W_DP));
            final int botH = Math.max(1, dp(BOTTOM_H_DP));
            // [comment]：[comment]，[comment]。
            // [comment]：[comment] —— [comment]Yes[comment]（[comment] 3 [comment]）。
            //   [comment]Yes「[comment]」、[comment]Yes「[comment]」，[comment]，[comment]。
            //   ⭐ [comment]「[comment] 2/3」：[comment]，[comment]。
            final boolean landscape = screenW > screenH;
            final int botInset = landscape ? Math.max(1, screenW / HOT_BOT_DEN) : 0;
            final int sideBottomInset = landscape ? Math.max(1, screenH / HOT_SIDE_BOT_DEN) : 0;
            final int edgeH = Math.max(1, screenH - sideBottomInset);
            final int botW = Math.max(1, screenW - botInset * 2);

            bands = new Band[]{
                    new Band(true, false),      // Text：Text / Text
                    new Band(false, true),      // Text：Text
                    new Band(false, false),     // Text：Text
            };
            final int[] xs = {botInset, 0, Math.max(0, screenW - edgeW)};
            final int[] ys = {Math.max(0, screenH - botH), 0, 0};
            final int[] ws = {botW, edgeW, edgeW};
            final int[] hs = {botH, edgeH, edgeH};

            wm = w;
            svc = s;
            for (int i = 0; i < bands.length; i++) {
                bands[i].attach(displayCtx, w, xs[i], ys[i], ws[i], hs[i]);
            }
            // ⚠ [comment]<b>[comment]</b>[comment]：[comment]，[comment]（[comment]）
            GestureAnim.install(displayCtx, w, s, screenW, screenH, density);
            // ⭐ [comment]<b>[comment]</b>：[comment]，
            // [comment]（[comment]）。[comment]，
            // [comment] barPress() [comment] + [comment]（[comment]）。
            installBarTop(displayCtx, w);
            // [comment]Yes「[comment]」[comment] ⇒ [comment]（[comment]，[comment]）
            gameMode = fgPkg != null && ExtPrefs.gameGestureOn(ctx, fgPkg);
            summoned = false;
            applyBarMode();
            Log.i(TAG, "Text：Text " + screenW + "x" + screenH + "px，Text " + edgeW
                    + "px Text、Text " + botH + "px Text；Text " + trigPx
                    + "px（Text/" + TRIGGER_DEN + "）；Text "
                    + (landscape ? ("Text（Text " + botInset
                    + "px、Text " + sideBottomInset + "px）") : "Text")
                    + "，density " + density);
            watchDisplay(s);
        } catch (Throwable t) {
            Log.e(TAG, "TextFailed", t);
            remove();
        }
    }

    /**
     * [comment]。
     *
     * <p>public Yes[comment]None[comment] —— [comment]，[comment]
     * [comment]。
     */
    public static void remove() {
        WindowManager w = wm;
        Band[] bs = bands;
        AccessibilityService s = svc;
        wm = null;
        bands = null;
        svc = null;
        unwatchDisplay(s);
        gameMode = false;
        summoned = false;
        UI.removeCallbacksAndMessages(null);
        // ⚠ [comment] return <b>[comment]</b>：bands Yes[comment]（[comment]）
        GestureAnim.remove();
        // [comment]，[comment]（[comment]）。
        // ⚠ [comment]Yes<b>[comment]</b>：[comment] WindowManager [comment]Yes[comment]，bar [comment]Yes[comment] view，
        //   [comment] bar [comment] removeViewImmediate [comment]"not attached"、[comment]。
        View bb = barBox;
        barTop = null;
        barBox = null;
        if (bb != null && w != null) {
            try {
                w.removeViewImmediate(bb);
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed", t);
            }
        }
        if (bs == null) {
            return;
        }
        for (Band b : bs) {
            b.detach(w);
        }
        Log.i(TAG, "Text");
    }

    /** [comment]None[comment]。[comment] shell [comment]，[comment] Shizuku */
    private static void go(int action, String what) {
        AccessibilityService s = svc;
        if (s == null) {
            Log.w(TAG, what + "：NoneText");
            return;
        }
        try {
            boolean ok = s.performGlobalAction(action);
            Log.i(TAG, what + " => performGlobalAction(" + action + ") Text " + ok);
        } catch (Throwable t) {
            Log.w(TAG, what + "：Text", t);
        }
    }

    /**
     * [comment]。[comment]Default[comment]，[comment]。
     * [comment]（{@link CoverRecentsGesture}）。
     */
    private static void buzz() {
        Context c = svc;
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
                return;
            }
            int amp = v.hasAmplitudeControl() ? BUZZ_AMP : VibrationEffect.DEFAULT_AMPLITUDE;
            v.vibrate(VibrationEffect.createOneShot(BUZZ_MS, amp));
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed（Text）", t);
        }
    }

    private static int dp(float v) {
        return Math.round(v * density);
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]。[comment] / [comment]，[comment]。
     *
     * <p>[comment]：[comment]（[comment]），
     * [comment]，[comment]。
     */
    private static final class Band {

        /** [comment]（[comment]） */
        final boolean bottom;
        /** [comment]（[comment]"[comment]"）；[comment]None[comment] */
        final boolean left;

        View view;
        WindowManager.LayoutParams lp;
        /** [comment]（[comment]） */
        int winX;
        int winY;

        float downX;
        float downY;
        float stillRefX;
        float stillRefY;
        /** [comment]：[comment] = [comment] px；[comment] = [comment] px */
        float peak;
        /** [comment]（[comment]、[comment]），[comment]"Yes[comment]Yes[comment]" */
        float cross;
        float maxDisp;
        long downAt;
        long lastMoveAt;
        long firedAt;

        boolean reached;
        /** [comment]（[comment] ANIM_ARM_DP [comment]） */
        boolean armed;
        /**
         * [comment]。
         *
         * <p>⚠ [comment]<b>[comment]</b>：[comment]、[comment]，
         * [comment]，[comment]。
         */
        boolean holdOk;
        /** [comment]"[comment]Yes[comment]"[comment]，[comment] */
        boolean cancelled;
        boolean fired;
        /** [comment]（[comment]） */
        boolean passOff;
        /** [comment]·[comment]：[comment] ⇒ [comment]（[comment]Yes[comment]） */
        boolean summonConsumed;
        /** [comment]：[comment]"[comment]"[comment] */
        float summonRefY;
        long summonMoveAt;
        /** [comment] ⇒ [comment]，[comment] */
        boolean replaying;
        /**
         * [comment]、[comment]。
         *
         * <p>⚠ [comment] {@link #passThrough()} Yes[comment]：[comment]Yes[comment]<b>[comment]</b>[comment]，
         * [comment]「[comment]」[comment]「[comment]」；[comment]Yes[comment]「[comment]Yes[comment]」[comment]<b>[comment]</b>[comment]，
         * [comment] —— [comment]、[comment]，[comment]Yes[comment]。
         */
        boolean live;

        final float[] px = new float[9];
        final float[] py = new float[9];
        int n;

        Band(boolean bottom, boolean left) {
            this.bottom = bottom;
            this.left = left;
        }

        /**
         * [comment]（[comment] MOVE [comment]）[comment]：[comment]。
         *
         * <p>⚠ [comment] MOVE [comment]：[comment]<b>[comment] MOVE</b>，
         * [comment] —— [comment]。
         */
        final Runnable decideFired = () -> {
            if (fired || cancelled || live || replaying) {
                return;
            }
            // ⚠ [comment]Yes「[comment]」[comment]Yes[comment] —— [comment]，
            //  [comment]；[comment]"[comment]"（[comment] CD），
            //  [comment] trigPx [comment]，[comment]。
            final float lx = n > 0 ? px[n - 1] : downX;
            final float ly = n > 0 ? py[n - 1] : downY;
            final float disp = (float) Math.hypot(lx - downX, ly - downY);
            if (disp >= dp(DECIDE_SLOP_DP)) {
                return;
            }
            cancelled = true;
            GestureAnim.cancel();
            Log.d(TAG, side() + "：Text（" + DECIDE_MS + "ms，Text " + (int) disp
                    + "px）⇒ Text");
            beginLive();
        };

        /** [comment]：[comment] + [comment]，[comment] */
        final Runnable holdFired = () -> {
            if (fired || cancelled || !reached || holdOk) {
                return;
            }
            holdOk = true;
            Log.i(TAG, "Text：Text（" + HOLD_MS + "ms Text）=> Text");
            // [comment] ⇒ [comment]"[comment]"[comment]
            GestureAnim.progress(1f);
            buzz();
        };

        void attach(Context displayCtx, WindowManager w, int x, int y, int width, int height) {
            FrameLayout v = new FrameLayout(displayCtx);
            v.setOnTouchListener((vv, e) -> onTouch(e));

            // ⚠⚠ [comment] FLAG_SPLIT_TOUCH，No[comment]。
            //  [comment]：「[comment]Settings[comment]，[comment]，
            //  [comment]」。[comment] ⇒ [comment] ⇒
            //  [comment]，[comment]Yes[comment]"[comment]"（MyGesture [comment]）。
            //  [comment]，[comment]，[comment]。
            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    width,
                    height,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    // [comment]（[comment]）＋ [comment]
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            // ⚠⚠ [comment] gravity：[comment]"[comment]"[comment]，{@code x / y} [comment]
            // **[comment]**，[comment]Yes[comment] —— [comment]
            //（[comment]，[comment]），[comment]Yes[comment]"[comment]"。
            //  [comment]：[comment] (0,0,60x1860) [comment] frame=[510,94]，510 = [comment]。
            //  [comment] FLAG_LAYOUT_NO_LIMITS [comment] inset（[comment] 94 [comment]Yes[comment] inset）。
            p.gravity = Gravity.TOP | Gravity.START;
            // [comment] gravity，x / y [comment]Yes[comment]（[comment] / [comment] / [comment]）
            p.x = x;
            p.y = y;

            try {
                w.addView(v, p);
            } catch (Throwable t) {
                Log.e(TAG, "TextFailed（" + side() + "）", t);
                return;
            }
            view = v;
            lp = p;
            winX = x;
            winY = y;
            // [comment]（[comment] Shizuku [comment] @hide [comment]），[comment]
            new Thread(() -> TouchInject.ready(), "inner-passthru-warm").start();
        }

        void detach(WindowManager w) {
            View v = view;
            view = null;
            lp = null;
            replaying = false;
            passOff = false;
            if (v != null && w != null) {
                try {
                    w.removeViewImmediate(v);
                } catch (Throwable t) {
                    Log.w(TAG, "TextFailed（" + side() + "）", t);
                }
            }
        }

        String side() {
            return bottom ? "Text" : (left ? "Text" : "Text");
        }

        boolean onTouch(MotionEvent e) {
            final long now = System.currentTimeMillis();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (replaying || live) {
                        return true;
                    }
                    // ⭐ [comment]：[comment]，[comment] DOWN [comment]
                    //   · [comment] ⇒ [comment]（[comment] barW，[comment] 1/3）
                    //   · [comment] ⇒ [comment] ±SUMMON_HALF_PX [comment]（[comment]）
                    // return false = [comment]、[comment]，[comment]
                    if (bottom && gameMode) {
                        final float sx = winX + e.getX();
                        final float half = summoned ? barW / 2f : SUMMON_HALF_PX;
                        if (Math.abs(sx - screenW / 2f) > half) {
                            return false;
                        }
                    }
                    downX = e.getX();
                    downY = e.getY();
                    stillRefX = downX;
                    stillRefY = downY;
                    peak = 0;
                    cross = 0;
                    maxDisp = 0;
                    live = false;
                    n = 0;
                    record(downX, downY);
                    downAt = now;
                    lastMoveAt = now;
                    reached = false;
                    armed = false;
                    holdOk = false;
                    fired = false;
                    summonConsumed = false;
                    summonRefY = downY;
                    summonMoveAt = now;
                    UI.removeCallbacks(holdFired);
                    UI.removeCallbacks(decideFired);
                    // [comment]，[comment]
                    GestureAnim.cancel();
                    // [comment]、[comment] —— [comment]，[comment]
                    passOff = now - firedAt < COOLDOWN_MS;
                    if (passOff) {
                        cancelled = true;
                        Log.d(TAG, side() + "：Text（Text " + (now - firedAt) + "ms），Text、Text");
                        beginLive();
                        return true;
                    }
                    cancelled = false;
                    // [comment]（[comment] MOVE，[comment] MOVE [comment]）
                    UI.postDelayed(decideFired, DECIDE_MS);
                    if (bottom) {
                        if (gameMode) {
                            // [comment]：[comment] 3 [comment]。
                            // [comment] —— [comment]，[comment]
                            if (summoned) {
                                UI.removeCallbacks(summonTtl);
                                UI.postDelayed(summonTtl, SUMMON_TTL_MS);
                                barPress(BAR_TOUCH, 1.1f, 80);
                            }
                        } else {
                            barPress(BAR_TOUCH, 1.1f, 80);
                        }
                    }
                    // [comment]，No[comment] MOVE
                    return true;

                case MotionEvent.ACTION_MOVE: {
                    if (replaying) {
                        return true;
                    }
                    // [comment]：[comment]，[comment]
                    if (live) {
                        return true;
                    }
                    if (cancelled) {
                        // [comment]"[comment]Yes[comment]"[comment]，[comment] —— [comment]
                        record(e.getX(), e.getY());
                        return true;
                    }
                    // [comment]：[comment]Yes[comment]
                    GestureAnim.at(winX + e.getX(), winY + e.getY());
                    final float dx = e.getX() - downX;
                    final float dy = e.getY() - downY;
                    final float disp = (float) Math.hypot(dx, dy);
                    if (disp > maxDisp) {
                        maxDisp = disp;
                    }
                    record(e.getX(), e.getY());

                    // [comment] DECIDE_SLOP_DP ⇒ [comment]Yes[comment]，[comment]。
                    // ⚠ [comment]：[comment]，[comment]「[comment]」
                    // [comment]（[comment]Yes[comment]）。
                    // ⚠ [comment]、[comment] 300ms —— [comment]Yes"70ms [comment] 3dp"：
                    //   3dp [comment]（[comment]），70ms [comment]Yes[comment]（[comment]）。
                    //   disp [comment]（= [comment]），[comment]
                    if (now - downAt >= DECIDE_MS && disp < dp(DECIDE_SLOP_DP)) {
                        cancelled = true;
                        GestureAnim.cancel();
                        Log.d(TAG, side() + "：Text（Text " + (int) disp + "px，"
                                + (now - downAt) + "ms）⇒ Text");
                        beginLive();
                        return true;
                    }

                    if (bottom) {
                        final float up = -dy;
                        // [comment]·[comment]：[comment]"[comment]"[comment]（[comment]）
                        if (gameMode && !summoned && e.getY() < summonRefY - MOVE_TOL_PX) {
                            summonRefY = e.getY();
                            summonMoveAt = now;
                        }
                        // ⭐ [comment]·[comment]：[comment] SUMMON_PX ⇒ [comment]。
                        //   [comment]（cancelled）：[comment]，[comment] ——
                        //   [comment]（[comment]。[comment]：
                        //   「[comment]」）。
                        if (gameMode && !summoned && !cancelled && up >= SUMMON_PX
                                && Math.abs(dx) * AXIS_RATIO <= Math.abs(dy)) {
                            summoned = true;
                            cancelled = true;
                            summonConsumed = true;
                            UI.removeCallbacks(decideFired);
                            UI.removeCallbacks(holdFired);
                            GestureAnim.cancel();
                            barPress(BAR_FULL, 1.9f, 90);
                            UI.removeCallbacks(summonTtl);
                            UI.postDelayed(summonTtl, SUMMON_TTL_MS);
                            buzz();
                            Log.i(TAG, "Text：Text " + (int) up + "px ⇒ Text，"
                                    + SUMMON_TTL_MS + "ms Text");
                            return true;
                        }
                        if (up > peak) {
                            peak = up;
                        }
                        if (Math.abs(dx) > cross) {
                            cross = Math.abs(dx);
                        }
                        // ① [comment]Yes[comment] ⇒ [comment]。[comment]（COMMIT_DP）：
                        //   [comment]Yes[comment]（[comment]/[comment]）——
                        //   [comment]，[comment]，[comment]。
                        //   · [comment]（[comment] DOWN_TOL_DP）⇒ [comment]
                        //   · [comment] AXIS_RATIO [comment]、[comment] ⇒ [comment]Yes[comment]
                        if (dy > dp(DOWN_TOL_DP)
                                || (Math.abs(dx) > dp(COMMIT_DP)
                                        && Math.abs(dx) * AXIS_RATIO > Math.abs(dy))) {
                            cancelled = true;
                            UI.removeCallbacks(holdFired);
                            GestureAnim.cancel();
                            Log.d(TAG, "Text：Text（Text " + (int) up + "px Text " + (int) cross + "px）⇒ Text");
                            beginLive();
                            return true;
                        }
                        // ⭐ [comment]·[comment]：[comment] ⇒ [comment]Yes[comment]，
                        //   Yes[comment]（[comment]）—— [comment]，[comment]。
                        //   ⚠ [comment]"[comment]"[comment]、[comment]：[comment]（[comment]、[comment]、[comment]）
                        //   [comment]（MyGesture [comment]：[comment]Yes[comment]）。
                        if (gameMode && !summoned && !cancelled && up > 0
                                && now - summonMoveAt >= SUMMON_STALL_MS) {
                            cancelled = true;
                            GestureAnim.cancel();
                            Log.d(TAG, "Text：Text " + (int) up + "px Text "
                                    + (now - summonMoveAt) + "ms，Text ⇒ Text");
                            beginLive();
                            return true;
                        }

                        // ② [comment]：[comment]（[comment]）
                        //   ⚠ [comment]，[comment]Yes[comment] —— [comment]Yes"[comment]"，
                        //   [comment]，[comment]。
                        if (up < trigPx) {
                            return true;
                        }
                        if (!reached) {
                            // [comment]：[comment]，[comment]
                            reached = true;
                            holdOk = false;
                            stillRefX = e.getX();
                            stillRefY = e.getY();
                            lastMoveAt = now;
                            UI.removeCallbacks(holdFired);
                            UI.postDelayed(holdFired, HOLD_MS);
                            // [comment]（[comment]）
                            armed = true;
                            GestureAnim.begin(GestureAnim.KIND_HOME);
                            GestureAnim.progress(up / (screenH * HOME_FULL_AT));
                            return true;
                        }
                        // ③ [comment]"[comment]"[comment] ⇒ [comment]，[comment]
                        if (Math.abs(e.getY() - stillRefY) > MOVE_TOL_PX
                                || Math.abs(e.getX() - stillRefX) > MOVE_TOL_PX) {
                            stillRefX = e.getX();
                            stillRefY = e.getY();
                            lastMoveAt = now;
                            UI.removeCallbacks(holdFired);
                            UI.postDelayed(holdFired, HOLD_MS);
                        }
                        GestureAnim.progress(up / (screenH * HOME_FULL_AT));
                        return true;
                    }

                    // [comment]：[comment]（[comment] = [comment]，[comment] = [comment]）
                    final float inward = left ? dx : -dx;
                    if (inward > peak) {
                        peak = inward;
                    }
                    if (Math.abs(dy) > cross) {
                        cross = Math.abs(dy);
                    }
                    // [comment]Yes[comment] ⇒ [comment]Yes[comment]，[comment]。
                    //   ⚠ [comment] COMMIT_DP [comment] —— [comment]Yes[comment]；
                    //   [comment] "cross > peak" [comment]，[comment]。
                    if (Math.abs(dy) > dp(COMMIT_DP) && Math.abs(dy) * AXIS_RATIO > Math.abs(dx)) {
                        cancelled = true;
                        GestureAnim.cancel();
                        Log.d(TAG, side() + "：Text（Text " + (int) cross + "px Text " + (int) peak + "px）⇒ Text");
                        beginLive();
                        return true;
                    }
                    // [comment]：[comment]
                    if (inward >= dp(ANIM_ARM_DP)) {
                        if (!armed) {
                            armed = true;
                            GestureAnim.begin(left ? GestureAnim.KIND_BACK_L : GestureAnim.KIND_BACK_R);
                        }
                        GestureAnim.progress(inward / trigPx);
                    }
                    return true;
                }

                case MotionEvent.ACTION_UP: {
                    UI.removeCallbacks(holdFired);
                    UI.removeCallbacks(decideFired);
                    if (live) {
                        unhookLive();
                        reset();
                        return true;
                    }
                    if (bottom) {
                        // ⭐ [comment]「[comment]」，[comment]Yes[comment] —— [comment]
                        //   （[comment]，[comment]，[comment]Yes[comment]）
                        final float upNow = -(e.getY() - downY);
                        // holdOk [comment] reached（holdFired [comment] reached [comment]），[comment]
                        if (!cancelled && holdOk && upNow >= trigPx * 0.5f) {
                            fired = true;
                            firedAt = now;
                            Log.i(TAG, "Text（Text " + (int) upNow + "px，Text "
                                    + (now - downAt) + "ms）=> Text");
                            GestureAnim.end(true);
                            buzz();
                            barPress(BAR_REST, 1f, 200);
                            go(AccessibilityService.GLOBAL_ACTION_RECENTS, "Text");
                            reset();
                            return true;
                        }
                        if (!cancelled && upNow >= trigPx
                                && Math.abs(e.getX() - downX) <= upNow * AXIS_RATIO) {
                            fired = true;
                            firedAt = now;
                            Log.i(TAG, "Text（Text " + (int) upNow + "px，Text "
                                    + (now - downAt) + "ms）=> Text");
                            GestureAnim.end(true);
                            buzz();
                            barPress(BAR_REST, 1f, 200);
                            go(AccessibilityService.GLOBAL_ACTION_HOME, "Text");
                            reset();
                            return true;
                        }
                        Log.d(TAG, "Text：Text=" + holdOk
                                + " Text=" + cancelled + " Text " + (int) peak + "px Text "
                                + (int) upNow + "px Text " + (now - downAt) + "ms");
                    } else if (!cancelled) {
                        // ⭐ [comment]「[comment]」，[comment]Yes[comment] —— [comment]
                        final float inwardNow = left ? e.getX() - downX : downX - e.getX();
                        if (inwardNow >= trigPx && Math.abs(e.getY() - downY) <= inwardNow * AXIS_RATIO) {
                            fired = true;
                            firedAt = now;
                            Log.i(TAG, side() + "Text " + (int) inwardNow + "px（Text " + trigPx
                                    + "px，Text " + (now - downAt) + "ms）=> Text");
                            GestureAnim.end(true);
                            buzz();
                            go(AccessibilityService.GLOBAL_ACTION_BACK, "Text");
                            reset();
                            return true;
                        }
                        Log.d(TAG, side() + "Text：Text " + (int) inwardNow + "px（Text "
                                + trigPx + "px）Text " + (int) peak + "px Text=" + cancelled + " Text "
                                + (now - downAt) + "ms");
                    } else {
                        Log.d(TAG, side() + "Text：Text=" + cancelled + " Text "
                                + (now - downAt) + "ms");
                    }
                    // [comment]Yes[comment] ⇒ [comment]，[comment]，[comment]
                    GestureAnim.end(false);
                    if (bottom) {
                        if (gameMode) {
                            // [comment]：[comment]，3 [comment]
                            barPress(BAR_REST, 1f, 200);
                        } else {
                            barFadeOutThenBack();
                        }
                    }
                    // ⚠ [comment] —— [comment]Yes[comment]，[comment]
                    if (!passOff && !(bottom && summonConsumed)) {
                        passThrough();
                    }
                    reset();
                    return true;
                }

                case MotionEvent.ACTION_CANCEL:
                    if (live) {
                        unhookLive();
                        reset();
                        return true;
                    }
                    if (replaying) {
                        return true;
                    }
                    UI.removeCallbacks(holdFired);
                    UI.removeCallbacks(decideFired);
                    GestureAnim.cancel();
                    if (bottom) {
                        barPress(BAR_REST, 1f, 180);
                    }
                    Log.d(TAG, side() + "：Text，Text");
                    reset();
                    return true;

                default:
                    return true;
            }
        }

        /** [comment]（[comment]，[comment]Yes[comment]） */
        void reset() {
            reached = false;
            cancelled = false;
            live = false;
        }

        /** [comment]。[comment] —— [comment] */
        void record(float x, float y) {
            if (n > 0) {
                float dx = x - px[n - 1];
                float dy = y - py[n - 1];
                if (dx * dx + dy * dy < 36f) {      // 6px Text
                    return;
                }
            }
            if (n >= px.length) {
                return;
            }
            px[n] = x;
            py[n] = y;
            n++;
        }

        /**
         * [comment]：[comment]，[comment]。
         *
         * <p>[comment]Yes[comment]：[comment]，[comment] —— [comment] CANCEL、
         * [comment] DOWN，[comment]Yes[comment]，[comment]。
         * [comment]（[comment]、[comment]，[comment] tap [comment]）。
         */
        void beginLive() {
            if (live) {
                return;
            }
            live = true;
            WindowManager w = wm;
            WindowManager.LayoutParams p = lp;
            View v = view;
            if (w == null || p == null || v == null) {
                return;
            }
            p.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            try {
                w.updateViewLayout(v, p);
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed（" + side() + "）", t);
                return;
            }
            Log.i(TAG, side() + "Text：Text");
            if (bottom) {
                barPress(BAR_REST, 1f, 180);
            }
            UI.removeCallbacks(liveGuard);
            UI.postDelayed(liveGuard, LIVE_GUARD_MS);
        }

        /** [comment]：[comment] */
        void unhookLive() {
            live = false;
            UI.removeCallbacks(liveGuard);
            restoreTouchable();
        }

        /** [comment]：[comment] CANCEL/UP（[comment]），[comment] */
        final Runnable liveGuard = () -> {
            if (live) {
                Log.d(TAG, side() + "Text");
                unhookLive();
            }
        };

        /**
         * [comment]。
         *
         * <p>[comment]，[comment]<b>[comment]</b>[comment] —— [comment]Yes[comment]。
         * [comment]、[comment]。
         *
         * <p>⚠ [comment]（[comment]Yes[comment]），[comment]
         * {@code FLAG_NOT_TOUCHABLE}，[comment]，[comment]。
         *
         * <p>[comment]（[comment] Shizuku）[comment]<b>[comment]</b>，[comment] ——
         * [comment]。
         */
        void passThrough() {
            final WindowManager w = wm;
            final WindowManager.LayoutParams p = lp;
            final View v = view;
            if (w == null || p == null || v == null || replaying) {
                return;
            }
            final int cnt = Math.max(1, n);
            final int[] xs = new int[cnt];
            final int[] ys = new int[cnt];
            for (int i = 0; i < cnt; i++) {
                xs[i] = Math.round(winX + px[i]);
                ys[i] = Math.round(winY + py[i]);
            }
            final boolean drag = maxDisp >= dp(TAP_SLOP_DP) && cnt > 1;
            replaying = true;
            p.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            try {
                w.updateViewLayout(v, p);
            } catch (Throwable t) {
                replaying = false;
                Log.w(TAG, "TextFailed：Text", t);
                return;
            }
            new Thread(() -> {
                try {
                    if (!TouchInject.ready()) {
                        Log.w(TAG, "TextFailed：Text（" + TouchInject.why() + "），Text");
                        return;
                    }
                    Thread.sleep(REPLAY_ARM_MS);
                    long dt = SystemClock.uptimeMillis();
                    boolean ok = TouchInject.send(Display.DEFAULT_DISPLAY,
                            MotionEvent.ACTION_DOWN, xs[0], ys[0], dt);
                    for (int i = 1; i < cnt; i++) {
                        Thread.sleep(REPLAY_STEP_MS);
                        TouchInject.send(Display.DEFAULT_DISPLAY, MotionEvent.ACTION_MOVE,
                                xs[i], ys[i], dt);
                    }
                    Thread.sleep(REPLAY_STEP_MS);
                    TouchInject.send(Display.DEFAULT_DISPLAY, MotionEvent.ACTION_UP,
                            xs[cnt - 1], ys[cnt - 1], dt);
                    if (ok) {
                        Log.i(TAG, side() + "Text" + (drag ? "（Text " + cnt + " Text）" : "（Text）")
                                + "：Text (" + xs[0] + "," + ys[0] + ") Text " + (int) maxDisp + "px");
                    } else {
                        Log.w(TAG, side() + "TextFailed：Text（" + TouchInject.why() + "）");
                    }
                    Thread.sleep(REPLAY_RESTORE_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    Log.w(TAG, "Text", t);
                } finally {
                    UI.post(this::restoreTouchable);
                }
            }, "inner-passthru").start();
        }

        /** [comment]，[comment]。⚠ [comment]SuccessFailed[comment] */
        void restoreTouchable() {
            replaying = false;
            WindowManager w = wm;
            WindowManager.LayoutParams p = lp;
            View v = view;
            if (w == null || p == null || v == null) {
                return;
            }
            p.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            try {
                w.updateViewLayout(v, p);
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed（" + side() + "）", t);
            }
        }
    }
}
