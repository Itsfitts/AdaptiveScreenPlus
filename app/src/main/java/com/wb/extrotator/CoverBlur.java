package com.wb.extrotator;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.hardware.HardwareBuffer;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * [comment]「[comment]」。
 *
 * <p>[comment]（[comment]），[comment]；[comment]，[comment]。[comment]
 * [comment]<b>[comment]</b>，[comment]Yes[comment]。
 *
 * <p><b>[comment]Yes「[comment]」</b>：[comment] ——
 * {@link WindowManager.LayoutParams#FLAG_BLUR_BEHIND} [comment] {@code setBlurBehindRadius}。
 * [comment]，[comment]，None[comment]；[comment]Yes
 * [comment] View [comment]None[comment]，[comment]，[comment]，[comment]。[comment]
 * [comment]：[comment]None[comment]，[comment]，
 * [comment]。
 *
 * <p>⚠ <b>[comment]：[comment]「[comment]」[comment]，[comment]</b>：
 * {@code displayCtx.getResources().getDisplayMetrics()} [comment]Yes <b>app [comment]</b>（[comment] 748x654，
 * [comment] 66px [comment]），[comment] {@code takeScreenshot(display)} [comment]Yes<b>[comment] 748x720</b>。
 * [comment] 654 [comment]、[comment] FIT_XY [comment] 720 [comment]，[comment]Yes[comment] 9.2%、[comment] 66px [comment]
 * （[comment]「[comment]」[comment]）。[comment] {@link Display#getRealSize}
 * （[comment]），[comment] {@code Gravity.BOTTOM} [comment]。
 *
 * <p>⚠ <b>[comment]</b>：[comment]Yes<b>[comment]</b>，No[comment]
 * （[comment]）—— [comment]（{@link #prepare()}）[comment]，[comment]「[comment] → [comment]」[comment]
 * [comment]；[comment]Yes[comment]，[comment]<b>[comment]</b>[comment]，[comment] {@code visible} [comment]，
 * [comment]Allow[comment]，[comment]。
 *
 * <p>⚠ [comment]<b>Default[comment]Yes[comment]</b>[comment]，[comment] {@link RenderEffect} [comment]
 * [comment] —— [comment] {@code FLAG_HARDWARE_ACCELERATED}，[comment]。
 */
public final class CoverBlur {

    private static final String TAG = "CoverBlur";

    /** [comment]（dp）。[comment] 0 [comment]，[comment] */
    private static final float RAD_MIN_DP = 5f;
    /** [comment]（dp）。[comment] 340dpi，20dp [comment] 43px，[comment]Yes[comment] */
    private static final float RAD_MAX_DP = 20f;
    /** [comment]。[comment]，"[comment]"[comment] */
    private static final float FULL_AT_H = 0.40f;
    /** [comment]，[comment] */
    private static final float ALPHA_FROM_H = 0.06f;
    /** [comment] */
    private static final float ALPHA_IN_H = 0.09f;
    /** [comment]，[comment] */
    private static final float SCRIM = 0.14f;
    /** [comment]，[comment]，[comment]"[comment]" */
    private static final float SCRIM_NO_SHOT = 0.55f;
    /** [comment] */
    private static final long FADE_OUT_MS = 200L;
    /** [comment]，[comment] */
    private static final long SHOT_FRESH_MS = 1500L;
    /** [comment]Failed[comment]。[comment]"[comment]"：[comment]，[comment]Yes[comment] */
    private static final long RETRY_MS = 1500L;
    /** [comment]，[comment]None[comment] */
    private static final float RAD_STEP = 2f;
    /** [comment]"[comment]Yes[comment]"[comment] */
    private static final int BLANK_RANGE = 10;

    private static final Handler UI = new Handler(Looper.getMainLooper());

    private static WindowManager wm;
    private static FrameLayout root;
    private static ImageView image;
    private static View scrim;
    private static AccessibilityService svc;
    private static int display = -1;
    private static int displayW;
    private static int displayH;

    private static Bitmap shot;
    private static long shotAt;
    private static boolean capturing;
    /** [comment]Failed[comment]（uptimeMillis） */
    private static long retryAfter;
    /** [comment]：[comment]Allow[comment] */
    private static boolean visible;
    /** [comment] ImageView [comment] */
    private static boolean painted;
    /** [comment]（px），[comment] */
    private static float curUp;
    private static float lastRadius = -1f;
    private static float radMin = 11f;
    private static float radMax = 43f;
    private static float fullAt = 288f;
    private static float alphaFrom = 43f;
    private static float alphaIn = 65f;

    private CoverBlur() {
    }

    /** [comment]（[comment]） */
    static boolean attached() {
        return root != null;
    }

    /**
     * [comment]。
     *
     * <p>⚠ [comment]<b>[comment]</b>[comment]：[comment]Yes[comment]，[comment]
     * [comment]。
     */
    static void attach(Context displayCtx, WindowManager w, AccessibilityService s,
                       int displayId, int wPx, int hPx) {
        if (w == null || root != null || wPx <= 0 || hPx <= 0) {
            return;
        }
        try {
            float d = displayCtx.getResources().getDisplayMetrics().density;
            if (d <= 0f) {
                d = 2.125f;
            }
            // [comment]"[comment]"，[comment]Yes app [comment]（[comment]）。[comment] getRealSize [comment]。
            int wpx = wPx;
            int hpx = hPx;
            String how = "Text";
            try {
                DisplayManager dm =
                        (DisplayManager) displayCtx.getSystemService(Context.DISPLAY_SERVICE);
                Display disp = dm == null ? null : dm.getDisplay(displayId);
                if (disp != null) {
                    Point pt = new Point();
                    disp.getRealSize(pt);
                    if (pt.x > 0 && pt.y > 0) {
                        wpx = pt.x;
                        hpx = pt.y;
                        how = "Text";
                    }
                }
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed，Text", t);
            }

            radMin = RAD_MIN_DP * d;
            radMax = RAD_MAX_DP * d;
            fullAt = Math.max(1f, hpx * FULL_AT_H);
            alphaFrom = hpx * ALPHA_FROM_H;
            alphaIn = Math.max(1f, hpx * ALPHA_IN_H);

            FrameLayout r = new FrameLayout(displayCtx);
            ImageView iv = new ImageView(displayCtx);
            // [comment]Yes[comment]，[comment] 1:1；[comment] FIT_XY Yes[comment]，
            // [comment]，[comment]
            iv.setScaleType(ImageView.ScaleType.FIT_XY);
            r.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            View sc = new View(displayCtx);
            sc.setBackgroundColor(0xFF000000);
            sc.setAlpha(0f);
            r.addView(sc, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            r.setAlpha(0f);

            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    wpx,
                    hpx,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    // [comment]、[comment]；[comment] LAYOUT [comment]Yes[comment]；
                    // HARDWARE_ACCELERATED [comment] RenderEffect [comment]
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT);
            // [comment] BOTTOM [comment]：[comment]，[comment]Yes[comment]。
            // [comment]Yes[comment]，[comment] 0 [comment]。
            p.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            w.addView(r, p);

            wm = w;
            root = r;
            image = iv;
            scrim = sc;
            svc = s;
            display = displayId;
            displayW = wpx;
            displayH = hpx;
            shot = null;
            shotAt = 0L;
            capturing = false;
            retryAfter = 0L;
            visible = false;
            painted = false;
            curUp = 0f;
            lastRadius = -1f;
            Log.i(TAG, "Text（Text），Text " + wpx + "x" + hpx + "px（" + how
                    + "），Text " + (int) radMin + "~" + (int) radMax + "px，Text "
                    + (int) fullAt + "px，Text "
                    + (int) (alphaFrom + alphaIn) + "px");
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed（Text）", t);
            reset();
        }
    }

    /**
     * [comment]（[comment]）。[comment]：[comment]（No[comment]），
     * [comment]"[comment] → [comment]"[comment]。
     */
    static void prepare() {
        FrameLayout r = root;
        if (r == null) {
            return;
        }
        r.animate().cancel();
        r.setAlpha(0f);
        visible = false;
        curUp = 0f;
        long now = SystemClock.uptimeMillis();
        if (shot == null || now - shotAt > SHOT_FRESH_MS) {
            invalidate();
            capture();
        }
    }

    /**
     * [comment]，[comment]Yes[comment]（px）。
     * [comment]"[comment]"，[comment]、[comment]，[comment]。
     */
    static void show(float upPx) {
        FrameLayout r = root;
        if (r == null) {
            return;
        }
        visible = true;
        curUp = upPx;
        if (shot == null) {
            capture();
        }
        render();
    }

    /** [comment] / [comment] / [comment]：[comment] */
    static void hide() {
        FrameLayout r = root;
        if (r == null) {
            return;
        }
        visible = false;
        r.animate().cancel();
        r.animate().alpha(0f).setDuration(FADE_OUT_MS).start();
    }

    /** [comment]（[comment] / [comment]）=> [comment]，[comment] */
    static void invalidate() {
        Bitmap b = shot;
        shot = null;
        shotAt = 0L;
        painted = false;
        if (image != null) {
            image.setImageDrawable(null);
        }
        if (b != null) {
            b.recycle();
        }
    }

    /** [comment]（[comment] / [comment] / [comment]） */
    static void detach() {
        FrameLayout r = root;
        WindowManager w = wm;
        reset();
        if (r != null && w != null) {
            try {
                w.removeViewImmediate(r);
                Log.i(TAG, "Text");
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed", t);
            }
        }
    }

    private static void reset() {
        if (image != null) {
            image.setImageDrawable(null);
        }
        wm = null;
        root = null;
        image = null;
        scrim = null;
        svc = null;
        display = -1;
        displayW = 0;
        displayH = 0;
        capturing = false;
        retryAfter = 0L;
        visible = false;
        painted = false;
        lastRadius = -1f;
        curUp = 0f;
        Bitmap b = shot;
        shot = null;
        shotAt = 0L;
        if (b != null) {
            b.recycle();
        }
    }

    // ------------------------------------------------------------------ [comment]

    private static void render() {
        FrameLayout r = root;
        ImageView iv = image;
        View sc = scrim;
        if (r == null || iv == null || sc == null) {
            return;
        }
        // [comment]，[comment]Yes[comment]，[comment]
        if (!visible) {
            return;
        }
        if (shot != null && !painted) {
            iv.setImageBitmap(shot);
            painted = true;
        }
        float p = curUp / fullAt;
        if (p < 0f) {
            p = 0f;
        }
        if (p > 1f) {
            p = 1f;
        }
        float a = (curUp - alphaFrom) / alphaIn;
        if (a < 0f) {
            a = 0f;
        }
        if (a > 1f) {
            a = 1f;
        }
        float radius = radMin + (radMax - radMin) * p;
        if (Math.abs(radius - lastRadius) >= RAD_STEP) {
            lastRadius = radius;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                iv.setRenderEffect(RenderEffect.createBlurEffect(
                        radius, radius, Shader.TileMode.CLAMP));
            }
        }
        sc.setAlpha((shot == null ? SCRIM_NO_SHOT : SCRIM) * p);
        if (Math.abs(r.getAlpha() - a) > 0.01f) {
            r.animate().cancel();
            r.setAlpha(a);
        }
    }

    // ------------------------------------------------------------------ [comment]

    private static void capture() {
        if (capturing || shot != null || svc == null || display <= 0) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now < retryAfter) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            retryAfter = now + 60000L;
            Log.w(TAG, "Text Android 11，Text，Text");
            return;
        }
        capturing = true;
        try {
            svc.takeScreenshot(display, svc.getMainExecutor(),
                    new AccessibilityService.TakeScreenshotCallback() {
                        @Override
                        public void onSuccess(AccessibilityService.ScreenshotResult r) {
                            Bitmap b = null;
                            try {
                                HardwareBuffer buf = r.getHardwareBuffer();
                                Bitmap hb = Bitmap.wrapHardwareBuffer(buf, r.getColorSpace());
                                if (hb != null) {
                                    b = hb.copy(Bitmap.Config.ARGB_8888, false);
                                }
                                buf.close();
                            } catch (Throwable t) {
                                Log.w(TAG, "Text bitmap Failed", t);
                            }
                            capturing = false;
                            if (b == null) {
                                retryAfter = SystemClock.uptimeMillis() + RETRY_MS;
                                Log.w(TAG, "TextFailed，Text");
                                return;
                            }
                            if (blank(b)) {
                                b.recycle();
                                retryAfter = SystemClock.uptimeMillis() + RETRY_MS;
                                Log.w(TAG, "TextYesText（Text FLAG_SECURE Text），Text");
                                return;
                            }
                            shot = b;
                            shotAt = SystemClock.uptimeMillis();
                            if (b.getWidth() != displayW || b.getHeight() != displayH) {
                                Log.w(TAG, "⚠ Text " + b.getWidth() + "x" + b.getHeight()
                                        + " Text " + displayW + "x" + displayH
                                        + " Text，Text");
                            } else {
                                Log.i(TAG, "Text " + b.getWidth() + "x" + b.getHeight()
                                        + "，Text");
                            }
                            UI.post(CoverBlur::render);
                        }

                        @Override
                        public void onFailure(int errorCode) {
                            capturing = false;
                            retryAfter = SystemClock.uptimeMillis() + RETRY_MS;
                            Log.w(TAG, "TextFailed code=" + errorCode + "，Text");
                        }
                    });
        } catch (Throwable t) {
            capturing = false;
            retryAfter = SystemClock.uptimeMillis() + RETRY_MS;
            Log.w(TAG, "takeScreenshot Text（TextYesText canTakeScreenshot）", t);
        }
    }

    /**
     * [comment]Yes[comment]Yes[comment]。
     *
     * <p>[comment] 16x16 [comment]：[comment] FLAG_SECURE [comment]Yes[comment]，
     * [comment] 0，[comment]。[comment]（[comment]）[comment]。
     */
    private static boolean blank(Bitmap b) {
        Bitmap small = null;
        try {
            small = Bitmap.createScaledBitmap(b, 16, 16, true);
            int min = 255;
            int max = 0;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int c = small.getPixel(x, y);
                    int l = (Color.red(c) * 30 + Color.green(c) * 59 + Color.blue(c) * 11) / 100;
                    if (l < min) {
                        min = l;
                    }
                    if (l > max) {
                        max = l;
                    }
                }
            }
            return max - min < BLANK_RANGE;
        } catch (Throwable t) {
            return false;
        } finally {
            if (small != null && small != b) {
                small.recycle();
            }
        }
    }
}
