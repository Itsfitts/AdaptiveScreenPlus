package com.wb.extrotator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Method;

/**
 * [comment] —— [comment]<b>[comment]</b>[comment]。
 *
 * <p><b>[comment]</b>：[comment]，[comment]Yes「[comment]」，
 * [comment]Yes[comment]，[comment]；[comment]Yes[comment]、[comment]。
 *
 * <p><b>[comment]</b>：[comment]（USB / uinput）[comment]，[comment]，
 * [comment] root [comment] —— [comment]：[comment] display context [comment]
 * {@code TYPE_APPLICATION_OVERLAY} [comment]，[comment]「[comment]」[comment]。
 *
 * <p>⚠ [comment]、[comment]（[comment]）：[comment] shell [comment]（{@code app_process}）[comment] ——
 * WMS [comment] {@code openSession} [comment] {@code Unknown pid=… uid=2000}；{@code Presentation} ——
 * [comment] {@code InvalidDisplayException}，[comment] FLAG_PRESENTATION [comment]，[comment]（Default[comment]）[comment]。
 *
 * <p><b>[comment]：[comment] = [comment] = [comment]</b>：[comment] {@code lp.x/lp.y} [comment]
 * {@code input -d N tap x y} [comment]Yes[comment]（[comment]，[comment] 1920×1080 [comment]
 * lp(160,360) [comment] (162,393)），[comment]Yes[comment]，[comment]。
 *
 * <p>⚠ <b>[comment]（bounds）[comment]，[comment] Display</b>：display context [comment]
 * {@code getResources().getDisplayMetrics()} [comment]<b>[comment]</b>[comment] —— [comment]Yes
 * 1920×1080，[comment] 1496×1242，[comment]Yes[comment] x≤1495，[comment] 424px [comment]（[comment]
 * 「[comment]」）。[comment] {@code dumpsys window displays} [comment] {@code cur=WxH}
 * （WindowManager [comment]，[comment] {@code input} / {@code screencap} [comment]）。
 * ⚠ Display [comment] API [comment]：{@code getRealSize/getMetrics} [comment]，
 * {@code getMode()} [comment] {@code wm size} [comment]。
 */
public final class CursorOverlay {

    private static final String TAG = "ExtCursor";

    /** [comment]（dp） */
    private static final float SIZE_DP = 28f;

    /** [comment] —— [comment] display changed */
    private static final long CALIB_DEBOUNCE_MS = 350L;

    private static View view;
    private static WindowManager wm;
    private static WindowManager.LayoutParams lp;
    private static int sizePx;
    /** [comment]（= [comment] = [comment]） */
    private static int boundW, boundH;
    /** [comment]（[comment]）；-1 = [comment] */
    private static int cx = -1, cy = -1;
    private static DisplayManager dm;
    private static DisplayManager.DisplayListener displayListener;

    /** [comment]（[comment]） */
    private static Context appCtx;
    private static int targetDisplay = -1;
    private static long lastCalibAt;
    /** [comment]（Activity [comment] onStop [comment]，[comment]） */
    private static Runnable boundsListener;

    /** [comment] */
    static boolean hot;

    private CursorOverlay() {
    }

    public static boolean isOn() {
        return view != null;
    }

    /** [comment]；[comment] null */
    public static int[] bounds() {
        return view == null ? null : new int[]{boundW, boundH};
    }

    public static int x() {
        return cx;
    }

    public static int y() {
        return cy;
    }

    /** [comment]（[comment] / [comment]）[comment]，[comment] */
    public static void setBoundsListener(Runnable r) {
        boundsListener = r;
    }

    /**
     * [comment]。[comment] true。
     *
     * @param displayId [comment] id（[comment] {@link ExtScreen#INNER_ID}）
     * @return [comment] / [comment]；Failed[comment] false（[comment]、[comment]，[comment]）
     */
    public static boolean show(Context app, int displayId) {
        if (view != null) {
            return true;
        }
        try {
            DisplayManager dmg = (DisplayManager) app.getSystemService(Context.DISPLAY_SERVICE);
            Display d = dmg.getDisplay(displayId);
            if (d == null) {
                Log.w(TAG, "Text " + displayId + " Text，Text");
                return false;
            }
            Context dc = displayContext(app, d);
            WindowManager w = (WindowManager) dc.getSystemService(Context.WINDOW_SERVICE);
            android.util.DisplayMetrics m = dc.getResources().getDisplayMetrics();
            // [comment]Yes[comment]（[comment] 480dpi → 3.0）；[comment] 2.0，
            // [comment]，[comment]。
            float density = m.density;
            if (density < 1f || density > 6f) {
                density = 2f;
            }
            sizePx = Math.max(24, Math.round(SIZE_DP * density));

            // [comment]：display context [comment]，[comment]。
            boundW = m.widthPixels > 0 ? m.widthPixels : 1080;
            boundH = m.heightPixels > 0 ? m.heightPixels : 1920;
            if (cx < 0) {
                // [comment]：[comment]，[comment]
                cx = boundW / 2;
                cy = boundH / 2;
            }
            cx = clamp(cx, 0, Math.max(0, boundW - 1));
            cy = clamp(cy, 0, Math.max(0, boundH - 1));

            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    sizePx, sizePx,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.TOP | Gravity.LEFT;
            p.setTitle("ext-cursor");
            p.x = cx - sizePx / 2;
            p.y = cy - sizePx / 2;

            CursorView v = new CursorView(dc, sizePx);
            w.addView(v, p);

            view = v;
            wm = w;
            lp = p;
            dm = dmg;
            appCtx = app.getApplicationContext();
            targetDisplay = displayId;
            registerDisplayListener(dmg);
            Log.i(TAG, "Text " + displayId + "：Text " + boundW + "×" + boundH
                    + "，Text " + sizePx + "px，Text " + cx + "," + cy);
            calibrate();     // Text
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "TextFailed", t);
            view = null;
            wm = null;
            lp = null;
            return false;
        }
    }

    /** [comment]（[comment]，[comment]） */
    public static void move(int x, int y) {
        if (view == null) {
            return;
        }
        int nx = clamp(x, 0, Math.max(0, boundW - 1));
        int ny = clamp(y, 0, Math.max(0, boundH - 1));
        if (nx == cx && ny == cy) {
            return;
        }
        cx = nx;
        cy = ny;
        lp.x = cx - sizePx / 2;
        lp.y = cy - sizePx / 2;
        try {
            wm.updateViewLayout(view, lp);
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed: " + t);
        }
    }

    /** [comment]/[comment] → [comment]（[comment]） */
    public static void setHot(boolean h) {
        if (view == null || hot == h) {
            return;
        }
        hot = h;
        view.invalidate();
    }

    /** [comment] */
    public static void hide() {
        if (view == null) {
            return;
        }
        try {
            wm.removeViewImmediate(view);
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed: " + t);
        }
        view = null;
        wm = null;
        lp = null;
        hot = false;
        Log.i(TAG, "Text");
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]，[comment]。
     *
     * <p>[comment]：[comment] —— display context [comment] metrics [comment]
     * （1920×1080 [comment] 1496×1242），{@code dumpsys window displays} [comment] {@code cur=WxH}
     * [comment]Yes WindowManager [comment]，[comment] {@code input} / {@code screencap} [comment]。
     *
     * <p>[comment] shell [comment]，[comment] {@link ExtScreen} [comment]，[comment]。
     */
    private static void calibrate() {
        final Context c = appCtx;
        final int id = targetDisplay;
        if (c == null || id < 0 || view == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastCalibAt < CALIB_DEBOUNCE_MS) {
            return;   // Text，Text
        }
        lastCalibAt = now;
        ExtScreen.exec("dumpsys window displays", out -> {
            final int[] s = ExtScreen.parseInjectionSize(out, id);
            final View v = view;
            if (s == null) {
                Log.w(TAG, "TextFailed：Text dumpsys Text " + id + " Text cur=");
                return;
            }
            if (v == null) {
                return;
            }
            v.post(() -> applyBounds(s[0], s[1], "dumpsys"));
        });
    }

    /**
     * [comment]。**[comment]**[comment]Yes[comment] ——
     * [comment] ↔ [comment] 1080×1920 [comment] 1920×1080 [comment]，
     * [comment]，[comment]。
     */
    private static void applyBounds(int w, int h, String src) {
        if (view == null || w <= 0 || h <= 0) {
            return;
        }
        if (w == boundW && h == boundH) {
            return;
        }
        float rx = boundW > 0 ? (float) cx / boundW : 0.5f;
        float ry = boundH > 0 ? (float) cy / boundH : 0.5f;
        int ow = boundW, oh = boundH;
        boundW = w;
        boundH = h;
        cx = clamp(Math.round(rx * boundW), 0, Math.max(0, boundW - 1));
        cy = clamp(Math.round(ry * boundH), 0, Math.max(0, boundH - 1));
        lp.x = cx - sizePx / 2;
        lp.y = cy - sizePx / 2;
        try {
            wm.updateViewLayout(view, lp);
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed: " + t);
        }
        Log.i(TAG, "Text " + ow + "×" + oh + " → " + boundW + "×" + boundH
                + "（" + src + "），Text " + cx + "," + cy);
        if (boundsListener != null) {
            boundsListener.run();
        }
    }

    /** [comment]（[comment] / [comment] / [comment]）→ [comment] */
    private static void registerDisplayListener(DisplayManager dmg) {
        if (displayListener != null) {
            return;
        }
        displayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
            }

            @Override
            public void onDisplayRemoved(int displayId) {
            }

            @Override
            public void onDisplayChanged(int displayId) {
                if (view == null || displayId != targetDisplay) {
                    return;   // TextNoneText
                }
                Log.i(TAG, "Text " + displayId + " Text → Text");
                calibrate();
            }
        };
        dmg.registerDisplayListener(displayListener, new Handler(Looper.getMainLooper()));
    }

    /** {@code Context.createDisplayContext} Yes @SystemApi，[comment] stub [comment] —— [comment] */
    private static Context displayContext(Context app, Display d) throws Exception {
        Method m = Context.class.getMethod("createDisplayContext", Display.class);
        m.setAccessible(true);
        return (Context) m.invoke(app, d);
    }

    static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment] + [comment]。
     *
     * <p>[comment]：[comment]，[comment]
     * [comment]；[comment]，[comment]，[comment]"[comment]"[comment]Yes[comment]，
     * [comment]。
     */
    private static class CursorView extends View {

        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int size;

        CursorView(Context c, int size) {
            super(c);
            this.size = size;
            stroke.setStyle(Paint.Style.STROKE);
            fill.setStyle(Paint.Style.FILL);
            dot.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas cv) {
            float c = size / 2f;
            float u = size / 28f;              // Text 28px Text
            float r = c - 5f * u;              // Text，Text

            // [comment]：[comment]
            stroke.setColor(0xB0000000);
            stroke.setStrokeWidth(5f * u);
            cv.drawCircle(c, c, r, stroke);

            // [comment]，[comment]"[comment]"[comment]
            if (hot) {
                fill.setColor(0x33FFFFFF);
                cv.drawCircle(c, c, r, fill);
            }

            // [comment]
            stroke.setColor(Color.WHITE);
            stroke.setStrokeWidth(2.2f * u);
            cv.drawCircle(c, c, r, stroke);

            // [comment] = [comment]
            dot.setColor(Color.WHITE);
            cv.drawCircle(c, c, (hot ? 4.6f : 3.2f) * u, dot);
            dot.setColor(0x80000000);
            cv.drawCircle(c, c, (hot ? 2.2f : 1.5f) * u, dot);
        }
    }
}
