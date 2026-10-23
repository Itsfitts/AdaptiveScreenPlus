package com.wb.extrotator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * [comment]「[comment]」（[comment]）。
 *
 * <p>[comment]，[comment]<b>[comment]</b>[comment]，[comment]Yes[comment]
 * [comment]；[comment]，[comment]、[comment]、[comment]；<b>[comment]</b>，[comment]
 * [comment]。[comment] {@link ExtPrefs#sidebarRings(Context)}，[comment]Yes「[comment] / [comment] / [comment]」，
 * [comment] [comment]=[comment] / [comment]=[comment] / [comment]=[comment] —— [comment]，[comment]。
 *
 * <p><b>[comment]</b>：[comment] {@link #SEL_GROW}，[comment]
 * {@link #SEL_ANIM_MS} [comment]，[comment]；[comment]
 * {@link #C_ACCENT_FILL}（[comment]），[comment] {@link #C_FILL_DIM}（[comment] 4%），
 * [comment]；[comment]（[comment]）[comment] {@link #BUZZ_MS}ms，
 * [comment]、[comment]。[comment] {@link #RAT_HYST} [comment]，[comment]
 * （[comment]，[comment] / [comment]「[comment]」[comment] + [comment]）。
 *
 * <p>⚠ [comment] —— [comment]，[comment]，
 * [comment]「[comment]」。[comment]（[comment] + [comment] + [comment]）[comment]Yes[comment]。
 *
 * <p><b>[comment]「[comment]」[comment]，[comment]Yes[comment]</b>：[comment]，[comment]
 * [comment]；[comment] —— [comment]Yes[comment]，[comment]、[comment]，
 * [comment]。
 *
 * <p><b>[comment]</b>：
 * <ol>
 *   <li>[comment]Yes<b>[comment] {@code FLAG_NOT_TOUCHABLE}</b> [comment]：[comment]，
 *       [comment]，[comment]；</li>
 *   <li>[comment]<b>[comment]</b>（[comment]），No[comment] 66px
 *       [comment]；</li>
 *   <li>[comment]None[comment]（{@code TYPE_ACCESSIBILITY_OVERLAY}）。</li>
 * </ol>
 */
public final class CoverWheel {

    private static final String TAG = "CoverWheel";

    /**
     * [comment]（[comment]，[comment]）。
     *
     * <p>[comment] = [comment] - 1（1 ~ 4 [comment]）。<b>3 [comment]Yes v4.9 [comment]，[comment]</b>
     * —— [comment]Yes[comment]；[comment]"[comment]"[comment]。
     */
    private static final float[][] RAT_BY_N = {
            {0.450f},                                // 1 Text
            {0.240f, 0.450f},                        // 2 Text
            {0.150f, 0.320f, 0.450f},                // 3 Text ← Text
            {0.110f, 0.225f, 0.340f, 0.450f},        // 4 Text
    };
    /** [comment]，[comment] */
    private static final float RAT_DEAD = 0.040f;
    /**
     * [comment]<b>[comment]</b>（[comment]，v4.10）。
     *
     * <p>[comment]：[comment]"[comment]"[comment]，[comment]
     * [comment] —— [comment]Yes[comment]，[comment]"[comment]"。[comment]
     * [comment] 1 [comment] 1↔0 [comment]。[comment]，[comment]。
     */
    private static final float RAT_HYST = 0.030f;

    /**
     * [comment]。
     *
     * <p>v4.11 [comment]"[comment]"[comment]（0xFF6FB0FF → 0xFF5E9CF0，
     * [comment]）。[comment]：[comment]，[comment]。
     */
    private static final int C_ACCENT = 0xFF5E9CF0;
    /** [comment]。[comment] —— "[comment]"[comment]Yes[comment] */
    private static final int C_ACCENT_FILL = 0x99518FE6;
    private static final int C_ACCENT_GLOW = 0x4D5E9CF0;
    /** [comment]（[comment]，[comment]） */
    private static final int C_FILL = 0x14FFFFFF;
    private static final int C_FILL_DIM = 0x0AFFFFFF;
    private static final int C_EDGE = 0x26FFFFFF;
    private static final int C_TEXT = 0x8CFFFFFF;
    private static final int C_TEXT_ON = 0xFFFFFFFF;
    /** [comment]，[comment] */
    private static final int C_SCRIM_IN = 0x73000000;
    private static final int C_SCRIM_OUT = 0x00000000;

    /** [comment]（1.0 = [comment]）。[comment]"[comment]" */
    private static final float SEL_GROW = 0.10f;
    /** [comment] / [comment]（ms） */
    private static final long SEL_ANIM_MS = 170L;
    /** [comment]：[comment]、[comment]（[comment]Yes"[comment]"） */
    private static final long BUZZ_MS = 12L;
    private static final int BUZZ_AMP = 110;
    /** [comment]"[comment]"[comment]（ms）—— [comment]Yes[comment]，[comment] */
    private static final long BUZZ_GAP_MS = 60L;

    private static final long FADE_IN_MS = 90L;
    private static final long FADE_OUT_MS = 130L;

    private static final Handler UI = new Handler(Looper.getMainLooper());

    // ------------------------------------------------------------------ [comment]（install [comment] CoverSidebar [comment]）

    private static Context displayCtx;
    private static WindowManager wm;
    private static int screenW;
    private static float density = 2.125f;
    /** 0 = [comment]，1 = [comment]（[comment]"[comment]"Yes[comment]） */
    private static int side = 1;

    // ------------------------------------------------------------------ [comment]

    private static FrameLayout root;
    private static WheelView view;
    private static float cx;
    private static float cy;
    private static float lastX;
    private static float lastY;
    /** [comment]，[comment]（[comment] = [comment]）；[comment] = bounds[bounds.length - 1] */
    private static float[] bounds = {0.150f, 0.320f, 0.450f};
    private static float rOut;
    private static float rDead;
    /** [comment]（px），[comment] {@link #RAT_HYST} */
    private static float hyst;
    /** [comment]"[comment]"[comment]（uptimeMillis），[comment] */
    private static long lastBuzzAt;
    /** 0 = [comment]，[comment]（[comment]、[comment]）；-1 = [comment]（[comment]） */
    private static int selRing = -1;
    /** [comment]Yes[comment] —— [comment]"[comment]"[comment] */
    private static int prevRing = -1;
    /** selRing [comment]（uptimeMillis），[comment] */
    private static long selAt;
    private static boolean shown;
    /** [comment]：[comment]，[comment] onDraw [comment] prefs */
    private static int[] slots = {ExtPrefs.RING_BACK, ExtPrefs.RING_RECENTS,
            ExtPrefs.RING_NOTIFY};

    private CoverWheel() {
    }

    static void env(Context ctx, WindowManager w, int wPx, float den, int sd) {
        displayCtx = ctx;
        wm = w;
        screenW = wPx;
        density = den;
        side = sd;
        setRingCount(slots.length);
        rDead = screenW * RAT_DEAD;
        hyst = screenW * RAT_HYST;
        // [comment]（[comment]），[comment] —— [comment] ensure()
        ensure();
    }

    /** [comment]。⚠ [comment]，[comment] onDraw [comment] */
    private static void setRingCount(int n) {
        if (n < 1) {
            n = 1;
        }
        if (n > RAT_BY_N.length) {
            n = RAT_BY_N.length;
        }
        float[] row = RAT_BY_N[n - 1];
        bounds = new float[row.length];
        for (int i = 0; i < row.length; i++) {
            bounds[i] = screenW * row[i];
        }
        rOut = bounds[bounds.length - 1];
    }

    /** [comment] */
    private static String radiiText() {
        StringBuilder b = new StringBuilder();
        for (float r : bounds) {
            if (b.length() > 0) {
                b.append('/');
            }
            b.append((int) r);
        }
        return b.toString();
    }

    static boolean showing() {
        return root != null && shown;
    }

    /**
     * [comment]，[comment]。
     *
     * <p>⚠ [comment] install [comment]，<b>[comment] addView</b>：[comment]、[comment]
     * [comment]，[comment]（[comment] ACTION_CANCEL [comment]）
     * [comment]。[comment]Yes {@code FLAG_NOT_TOUCHABLE}，[comment]，[comment]「[comment]
     * [comment]」Yes[comment]，[comment]。
     */
    private static void ensure() {
        if (root != null || displayCtx == null || wm == null) {
            return;
        }
        try {
            FrameLayout r = new FrameLayout(displayCtx);
            WheelView v = new WheelView(displayCtx);
            v.setVisibility(View.INVISIBLE);
            r.addView(v, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
            // [comment]**[comment]**：[comment]
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;
            r.setAlpha(0f);
            wm.addView(r, lp);
            root = r;
            view = v;
            Log.i(TAG, "Text（Text）：Text " + screenW + "px，Text "
                    + radiiText() + "px");
        } catch (Throwable t) {
            Log.e(TAG, "TextFailed", t);
            root = null;
            view = null;
        }
    }

    /** [comment]：[comment]，[comment]（[comment]） */
    static void prepare(float x, float y) {
        if (displayCtx != null) {
            int[] got = ExtPrefs.sidebarSlots(displayCtx);
            if (got != null && got.length > 0) {
                slots = got;
            }
        }
        setRingCount(slots.length);
        cx = x;
        cy = y;
        lastX = x;
        lastY = y;
        selRing = -1;
        prevRing = -1;
        selAt = 0L;
        shown = false;
    }

    /** [comment]：[comment]（[comment]Yes install [comment]） */
    static void show(float x, float y) {
        ensure();
        if (root == null || slots.length == 0) {
            return;      // 「Text」Text：Text
        }
        shown = true;
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        move(x, y);
        root.animate().cancel();
        root.animate().alpha(1f).setDuration(FADE_IN_MS).start();
        Log.i(TAG, "Text：Text " + (int) cx + "," + (int) cy
                + "，Text " + radiiText() + "px，" + slots.length + " Text");
    }

    /** [comment]：Update[comment]（[comment]） */
    static void move(float x, float y) {
        lastX = x;
        lastY = y;
        int now = ringAt(x, y);
        if (now != selRing) {
            prevRing = selRing;
            selRing = now;
            selAt = SystemClock.uptimeMillis();
            // [comment]（[comment]）[comment]；[comment]Yes"Cancel"，[comment]
            if (now >= 0) {
                buzz();
            }
            Log.i(TAG, "Text " + now + " Text（Text " + (int) radius(x, y) + "px）");
        }
        if (view != null) {
            view.invalidate();
        }
    }

    /** [comment]：[comment]（[comment] {@link ExtPrefs#RING_NONE} [comment]） */
    static int pick() {
        if (root == null || !shown) {
            return ExtPrefs.RING_NONE;
        }
        int ring = ringAt(lastX, lastY);
        if (ring < 0) {
            return ExtPrefs.RING_NONE;
        }
        return ring >= slots.length ? ExtPrefs.RING_NONE : slots[ring];
    }

    /** [comment]。⚠ [comment] —— [comment]，[comment]Yes[comment] */
    static void hide() {
        final FrameLayout r = root;
        final WheelView v = view;
        shown = false;
        selRing = -1;
        prevRing = -1;
        if (r == null) {
            return;
        }
        UI.post(() -> {
            r.animate().cancel();
            r.animate().alpha(0f).setDuration(FADE_OUT_MS)
                    .withEndAction(() -> {
                        if (v != null && !shown) {
                            v.setVisibility(View.INVISIBLE);
                        }
                    })
                    .start();
        });
        Log.i(TAG, "Text");
    }

    /** [comment]（[comment]，[comment]） */
    static void release() {
        FrameLayout r = root;
        root = null;
        view = null;
        shown = false;
        selRing = -1;
        prevRing = -1;
        if (r != null && wm != null) {
            try {
                wm.removeViewImmediate(r);
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed", t);
            }
        }
        displayCtx = null;
        wm = null;
    }

    /**
     * [comment]。
     *
     * <p>[comment]Default[comment]，[comment]；[comment]Yes[comment]，
     * [comment]Default[comment]。[comment]Yes"[comment]"[comment]Yes"[comment]"，[comment] 12ms。
     */
    private static void buzz() {
        Context c = displayCtx;
        if (c == null) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastBuzzAt < BUZZ_GAP_MS) {
            return;      // Text，Text
        }
        lastBuzzAt = now;
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

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]：<b>0 = [comment]</b>，[comment]；-1 = [comment]（[comment]）。
     *
     * <p>⚠ [comment]，[comment]<b>[comment]</b>[comment]，[comment]
     * {@link #RAT_HYST} [comment] —— [comment]。
     * [comment]"[comment]"，[comment]。
     */
    private static int ringAt(float x, float y) {
        int n = bounds.length;
        if (n <= 0) {
            return -1;
        }
        float r = radius(x, y);
        // [comment]：[comment]，[comment]
        if (r < (selRing >= 0 ? rDead - hyst * 0.5f : rDead)) {
            return -1;
        }
        // [comment]，[comment]
        int raw = 0;
        for (int k = 0; k <= n - 2; k++) {
            if (r < bounds[k]) {
                raw = n - 1 - k;
                break;
            }
        }
        if (selRing < 0 || raw == selRing) {
            return raw;
        }
        // [comment] hyst [comment]（[comment]）
        if (raw < selRing) {
            return r > bounds[n - 1 - selRing] + hyst ? raw : selRing;
        }
        float lo = selRing == n - 1 ? rDead : bounds[n - 2 - selRing];
        return r < lo - hyst ? raw : selRing;
    }

    private static float radius(float x, float y) {
        float dx = x - cx;
        float dy = y - cy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]。
     *
     * <p>[comment]"[comment]"（[comment] + [comment]，[comment] Path，
     * Default[comment] WINDING [comment]）。[comment]、[comment]
     * {@link #SEL_GROW} [comment]，"[comment]"[comment]，[comment]。
     */
    private static final class WheelView extends View {

        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint scrim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();

        WheelView(Context c) {
            super(c);
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(1.2f * density);
            glyph.setStyle(Paint.Style.STROKE);
            glyph.setStrokeWidth(1.9f * density);
            glyph.setStrokeCap(Paint.Cap.ROUND);
            glyph.setStrokeJoin(Paint.Join.ROUND);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            scrim.setShader(new RadialGradient(cx, cy, rOut <= 0f ? 1f : rOut,
                    C_SCRIM_IN, C_SCRIM_OUT, Shader.TileMode.CLAMP));
        }

        @Override
        protected void onDraw(Canvas cv) {
            if (rOut <= 0f) {
                return;
            }
            // [comment] 0..1（[comment]）。[comment]。
            float t = 1f;
            if (selRing >= 0 || prevRing >= 0) {
                long dt = SystemClock.uptimeMillis() - selAt;
                t = dt >= SEL_ANIM_MS ? 1f : Math.max(0f, dt / (float) SEL_ANIM_MS);
                if (t < 1f) {
                    postInvalidateOnAnimation();
                }
            }
            float ease = 1f - (1f - t) * (1f - t);
            boolean shrinking = prevRing >= 0 && prevRing != selRing && t < 1f;

            // ① [comment]，[comment]
            cv.drawCircle(cx, cy, rOut, scrim);

            // ② [comment]。[comment]，[comment]、[comment]
            int idle = selRing >= 0 ? C_FILL_DIM : C_FILL;
            for (int i = bounds.length - 1; i >= 0; i--) {
                if (i == selRing) {
                    continue;
                }
                if (shrinking && i == prevRing) {
                    continue;
                }
                annulus(cv, i, idle, C_EDGE, 1f);
            }
            // [comment]：[comment]"[comment]"[comment]（[comment]）
            if (shrinking) {
                annulus(cv, prevRing, C_FILL_DIM, C_EDGE, 1f + SEL_GROW * (1f - ease));
            }
            if (selRing >= 0) {
                float grow = 1f + SEL_GROW * ease;
                annulus(cv, selRing, C_ACCENT_FILL, C_ACCENT, grow);
                // [comment]，[comment]
                edge.setColor(C_ACCENT_GLOW);
                edge.setStrokeWidth(6.5f * density);
                cv.drawCircle(cx, cy, outerOf(selRing) * grow, edge);
                edge.setStrokeWidth(1.2f * density);
            }

            // ③ [comment]，[comment]（[comment]），[comment]
            for (int i = 0; i < bounds.length; i++) {
                int act = i < slots.length ? slots[i] : ExtPrefs.RING_NONE;
                float grow = i == selRing ? 1f + SEL_GROW * ease
                        : (shrinking && i == prevRing ? 1f + SEL_GROW * (1f - ease) : 1f);
                drawGroup(cv, i, act, i == selRing, grow);
            }
        }

        /** [comment] i [comment]（0 = [comment]）[comment]；grow Yes[comment] */
        private void annulus(Canvas cv, int i, int fillColor, int edgeColor, float grow) {
            float inner = innerOf(i) * grow;
            float outer = outerOf(i) * grow;
            path.reset();
            path.addCircle(cx, cy, outer, Path.Direction.CW);
            if (inner > 0.5f) {
                path.addCircle(cx, cy, inner, Path.Direction.CCW);
            }
            fill.setStyle(Paint.Style.FILL);
            fill.setShader(null);
            fill.setColor(fillColor);
            cv.drawPath(path, fill);
            edge.setColor(edgeColor);
            cv.drawCircle(cx, cy, outer, edge);
            if (inner > 0.5f) {
                cv.drawCircle(cx, cy, inner, edge);
            }
        }

        /** [comment] i [comment]（0 = [comment]）[comment]；[comment]Yes[comment]，[comment]Yes 0 */
        private float innerOf(int i) {
            int k = bounds.length - 2 - i;
            return k < 0 ? 0f : bounds[k];
        }

        /** [comment] i [comment]（0 = [comment]）[comment] */
        private float outerOf(int i) {
            return bounds[bounds.length - 1 - i];
        }

        /** [comment] + [comment]，[comment]；[comment] */
        private void drawGroup(Canvas cv, int i, int act, boolean on, float grow) {
            float inner = innerOf(i) * grow;
            float outer = outerOf(i) * grow;
            float mid = inner <= 0f ? outer * 0.5f : (inner + outer) / 2f;
            float dir = side == 1 ? -1f : 1f;
            float gx = cx + dir * mid;
            float gy = cy;
            float s = (on ? 12.5f : 11f) * density;

            glyph.setColor(on ? C_TEXT_ON : C_TEXT);
            glyph.setStrokeWidth((on ? 2.5f : 1.9f) * density);
            drawGlyph(cv, act, gx, gy - 10f * density, s);

            text.setColor(on ? C_TEXT_ON : C_TEXT);
            text.setTextSize((on ? 14.5f : 12.5f) * density);
            cv.drawText(labelOf(act), gx, gy + 23f * density, text);
        }

        /** [comment]（[comment]，[comment]） */
        private void drawGlyph(Canvas cv, int act, float x, float y, float s) {
            float dir = side == 1 ? -1f : 1f;
            switch (act) {
                case ExtPrefs.RING_BACK:
                    // [comment]"[comment]"[comment]，[comment]
                    path.reset();
                    path.moveTo(x - dir * s * 0.5f, y - s * 0.55f);
                    path.lineTo(x + dir * s * 0.42f, y);
                    path.lineTo(x - dir * s * 0.5f, y + s * 0.55f);
                    cv.drawPath(path, glyph);
                    break;
                case ExtPrefs.RING_RECENTS: {
                    float w = s * 0.6f;
                    float h = s * 1.0f;
                    for (int k = 0; k < 3; k++) {
                        float ox = x + (k - 1) * s * 0.56f;
                        rect.set(ox - w / 2f, y - h / 2f, ox + w / 2f, y + h / 2f);
                        cv.drawRoundRect(rect, s * 0.22f, s * 0.22f, glyph);
                    }
                    break;
                }
                case ExtPrefs.RING_NOTIFY: {
                    rect.set(x - s * 0.5f, y - s * 0.62f, x + s * 0.5f, y + s * 0.2f);
                    path.reset();
                    path.addArc(rect, 180f, 180f);
                    path.moveTo(x - s * 0.5f, rect.centerY());
                    path.lineTo(x - s * 0.5f, y + s * 0.38f);
                    path.lineTo(x + s * 0.5f, y + s * 0.38f);
                    path.lineTo(x + s * 0.5f, rect.centerY());
                    cv.drawPath(path, glyph);
                    Paint p = dot;
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(glyph.getColor());
                    cv.drawCircle(x, y + s * 0.66f, s * 0.14f, p);
                    break;
                }
                case ExtPrefs.RING_HOME:
                    path.reset();
                    path.moveTo(x - s * 0.55f, y + s * 0.05f);
                    path.lineTo(x, y - s * 0.6f);
                    path.lineTo(x + s * 0.55f, y + s * 0.05f);
                    cv.drawPath(path, glyph);
                    rect.set(x - s * 0.34f, y + s * 0.05f, x + s * 0.34f, y + s * 0.6f);
                    cv.drawRoundRect(rect, s * 0.14f, s * 0.14f, glyph);
                    break;
                default:
                    // [comment]：[comment]
                    cv.drawCircle(x, y, s * 0.5f, glyph);
                    break;
            }
        }

        private String labelOf(int act) {
            switch (act) {
                case ExtPrefs.RING_BACK:
                    return "Text";
                case ExtPrefs.RING_RECENTS:
                    return "Text";
                case ExtPrefs.RING_NOTIFY:
                    return "Text";
                case ExtPrefs.RING_HOME:
                    return "Text";
                default:
                    return "Text";
            }
        }
    }
}
