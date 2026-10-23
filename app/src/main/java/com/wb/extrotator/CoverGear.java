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
 * <p>[comment]，<b>[comment]</b>（[comment]Yes[comment]）[comment]，
 * [comment]；[comment]（<b>[comment]「[comment]」[comment]，1 ~ 4</b>），
 * [comment]Yes[comment]（[comment] {@link ExtPrefs#sidebarSlots(Context)}，[comment]）。[comment]，[comment]
 * [comment]、[comment]、[comment]，<b>[comment]</b> —— [comment]；<b>[comment]</b>，
 * [comment]。
 *
 * <p><b>[comment]Yes[comment]</b>：[comment]，[comment]，
 * [comment]<b>[comment]</b> —— [comment]、[comment]，[comment]。
 *
 * <p><b>[comment]</b>[comment] {@link #SPEED_BY_N}：[comment]Yes[comment]，[comment]Yes[comment]，[comment]。
 *
 * <p><b>[comment]</b>（[comment] {@link CoverWheel} [comment]）：
 * <ol>
 *   <li>[comment]Yes[comment] {@code FLAG_NOT_TOUCHABLE} [comment]：[comment]，
 *       [comment]，[comment]；</li>
 *   <li>[comment]（[comment]），No[comment] 66px [comment]；</li>
 *   <li>{@code TYPE_ACCESSIBILITY_OVERLAY} [comment]None[comment]。</li>
 * </ol>
 */
public final class CoverGear {

    private static final String TAG = "CoverGear";

    // ------------------------------------------------------------------ [comment]

    /** [comment]（[comment]）：[comment]「[comment]」[comment] 0°，[comment] */
    private static final float FAN_HALF = 72f;
    /**
     * [comment]：[comment] / [comment]（[comment]）。
     *
     * <p>⚠ [comment]"[comment]"，[comment]<b>[comment] 1/2</b> ——
     * [comment] ∝ [comment]²，[comment]Yes √1.5（0.40 → 0.4899、0.36 → 0.4409）。
     * [comment]"[comment] ×1.5"[comment]，[comment] 2.25 [comment]。
     */
    private static final float RAT_R_W = 0.4899f;
    private static final float RAT_R_H = 0.4409f;
    /** [comment]（[comment]）[comment]"[comment]" */
    private static final float RAT_DEAD = 0.19f;
    /** [comment]（[comment]） */
    private static final float RAT_GEAR = 0.28f;
    /** [comment] = [comment] × [comment]，[comment] */
    private static final float SECTOR_INNER = 1.30f;
    /** [comment]"[comment]"[comment]，[comment]（[comment] + [comment]） */
    private static final float HYST_DEG = 7f;
    /** [comment] */
    private static final int TEETH = 10;

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]（[comment] / [comment]）：[comment] = [comment]（0 = [comment]），[comment]。
     *
     * <p>[comment]：「<b>[comment]</b>」。
     * [comment] 3.6 [comment]，[comment] —— [comment]，[comment]。
     */
    private static final float[][] SPEED_BY_N = {
            {300f},                        // 1 Text：Text，TextNoneText，Text
            {100f, 800f},                  // 2 Text
            {100f, 300f, 800f},            // 3 Text ← Text，Text
            {100f, 300f, 550f, 800f},      // 4 Text
    };
    /** [comment] */
    private static final float SEL_GROW = 0.07f;
    /** [comment] / [comment]（ms） */
    private static final long SEL_ANIM_MS = 170L;
    /** [comment]：[comment]、[comment] */
    private static final long BUZZ_MS = 12L;
    private static final int BUZZ_AMP = 110;
    /** [comment]（ms）—— [comment] */
    private static final long BUZZ_GAP_MS = 60L;
    private static final long FADE_IN_MS = 90L;
    private static final long FADE_OUT_MS = 130L;
    /** [comment]（[comment]） */
    private static final float MAX_STEP_S = 0.2f;

    // ------------------------------------------------------------------ [comment]

    /** [comment]：[comment]，[comment]（[comment]"[comment]"） */
    private static final int C_GEAR = 0xFF4C9BFF;
    private static final int C_GEAR_DARK = 0xFF1B5FB4;
    /** [comment]，[comment] */
    private static final int C_GEAR_HALO = 0x4D4C9BFF;
    private static final int C_GEAR_HUB = 0x66000000;
    /** [comment]（[comment]）[comment] */
    private static final int C_ACCENT_FILL = 0x99518FE6;
    private static final int C_ACCENT = 0xFF5E9CF0;
    private static final int C_ACCENT_GLOW = 0x4D5E9CF0;
    private static final int C_FILL = 0x14FFFFFF;
    private static final int C_FILL_DIM = 0x0AFFFFFF;
    private static final int C_EDGE = 0x26FFFFFF;
    private static final int C_TEXT = 0x8CFFFFFF;
    private static final int C_TEXT_ON = 0xFFFFFFFF;
    private static final int C_SCRIM_IN = 0x73000000;
    private static final int C_SCRIM_OUT = 0x00000000;

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
    private static GearView view;
    private static float cx;
    private static float cy;
    private static float lastX;
    private static float lastY;
    private static float rFan;
    private static float rInner;
    private static float rGear;
    private static float rDead;
    /** 0 [comment] / 1 [comment] / 2 [comment]；-1 = [comment]（[comment]） */
    private static int sel = -1;
    /** [comment]Yes[comment] —— [comment]"[comment]"[comment] */
    private static int prevSel = -1;
    /** sel [comment]（uptimeMillis），[comment] */
    private static long selAt;
    private static long lastBuzzAt;
    /** [comment]（[comment]）[comment]，[comment] */
    private static float rotDeg;
    private static long lastTick;
    private static boolean shown;
    /** [comment]：[comment]，[comment] onDraw [comment] prefs */
    private static int[] slots = {ExtPrefs.RING_BACK, ExtPrefs.RING_RECENTS,
            ExtPrefs.RING_NOTIFY};
    /** [comment]（= [comment] / [comment]）—— [comment] */
    private static float sectorDeg = FAN_HALF * 2f / 3f;
    /** [comment]（[comment] -FAN_HALF [comment]，[comment] [comment]-1 [comment]）—— [comment] */
    private static float[] bounds = {-24f, 24f};
    /** [comment] —— [comment] */
    private static float[] speed = {100f, 300f, 800f};

    private CoverGear() {
    }

    static void env(Context ctx, WindowManager w, int wPx, int hPx, float den, int sd) {
        displayCtx = ctx;
        wm = w;
        screenW = wPx;
        density = den;
        side = sd;
        rFan = Math.min(wPx * RAT_R_W, hPx * RAT_R_H);
        setSlotCount(slots.length);
        rGear = rFan * RAT_GEAR;
        rInner = rGear * SECTOR_INNER;
        rDead = rFan * RAT_DEAD;
        // [comment]（[comment]），[comment] —— [comment] CoverWheel.ensure()
        ensure();
    }

    /** [comment]、[comment]。⚠ [comment]，[comment] onDraw [comment] */
    private static void setSlotCount(int n) {
        if (n < 1) {
            n = 1;
        }
        if (n > SPEED_BY_N.length) {
            n = SPEED_BY_N.length;
        }
        sectorDeg = FAN_HALF * 2f / n;
        bounds = new float[n - 1];
        for (int i = 0; i < n - 1; i++) {
            bounds[i] = -FAN_HALF + sectorDeg * (i + 1);
        }
        speed = SPEED_BY_N[n - 1];
    }

    /** [comment] */
    private static String speedText() {
        StringBuilder b = new StringBuilder();
        for (float v : speed) {
            if (b.length() > 0) {
                b.append('/');
            }
            b.append((int) v);
        }
        return b.toString();
    }

    static boolean showing() {
        return root != null && shown;
    }

    /**
     * [comment]，[comment]。
     *
     * <p>⚠ [comment] install [comment]，[comment] {@code addView}：[comment]、
     * [comment]，[comment]（[comment] ACTION_CANCEL [comment]
     * [comment]）[comment]。[comment]Yes {@code FLAG_NOT_TOUCHABLE}，[comment]，
     * [comment]「[comment]」Yes[comment]，[comment]。
     */
    private static void ensure() {
        if (root != null || displayCtx == null || wm == null) {
            return;
        }
        try {
            FrameLayout r = new FrameLayout(displayCtx);
            GearView v = new GearView(displayCtx);
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
            Log.i(TAG, "Text（Text）：Text " + screenW + "px，Text " + (int) rFan
                    + "px Text " + (int) rGear + "px Text " + TEETH + " Text");
        } catch (Throwable t) {
            Log.e(TAG, "TextFailed", t);
            root = null;
            view = null;
        }
    }

    /** [comment]：[comment]（[comment]） */
    static void prepare(float x, float y) {
        if (displayCtx != null) {
            int[] got = ExtPrefs.sidebarSlots(displayCtx);
            if (got != null && got.length > 0) {
                slots = got;
            }
        }
        setSlotCount(slots.length);
        cx = x;
        cy = y;
        lastX = x;
        lastY = y;
        sel = -1;
        prevSel = -1;
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
        rotDeg = 0f;
        lastTick = SystemClock.uptimeMillis();
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
        move(x, y);
        root.animate().cancel();
        root.animate().alpha(1f).setDuration(FADE_IN_MS).start();
        Log.i(TAG, "Text：Text " + (int) cx + "," + (int) cy
                + "，Text " + (int) rFan + "px，" + slots.length + " Text "
                + speedText() + " Text");
    }

    /** [comment]：Update[comment]（[comment]） */
    static void move(float x, float y) {
        lastX = x;
        lastY = y;
        int now = sectorAt(x, y);
        if (now != sel) {
            prevSel = sel;
            sel = now;
            selAt = SystemClock.uptimeMillis();
            // [comment]（[comment]）[comment]；[comment]Yes"Cancel"，[comment]
            if (now >= 0) {
                buzz();
            }
            Log.i(TAG, "Text " + now + " Text（Text " + (int) radius(x, y)
                    + "px，Text " + (int) angleOf(x, y) + "°）=> Text "
                    + (int) (now >= 0 && now < speed.length ? speed[now] : speed[0])
                    + " Text");
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
        int now = sectorAt(lastX, lastY);
        if (now < 0) {
            return ExtPrefs.RING_NONE;
        }
        return now >= slots.length ? ExtPrefs.RING_NONE : slots[now];
    }

    /** [comment]。⚠ [comment] —— [comment]，[comment]Yes[comment] */
    static void hide() {
        final FrameLayout r = root;
        final GearView v = view;
        shown = false;
        sel = -1;
        prevSel = -1;
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
        sel = -1;
        prevSel = -1;
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

    /** [comment]（[comment] 12ms：[comment]Yes"[comment]"[comment]Yes"[comment]"） */
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
     * [comment]：0 = [comment]，[comment]；-1 = [comment]（[comment]）。
     *
     * <p>⚠ [comment]，[comment]<b>[comment]</b>[comment]，[comment]
     * {@link #HYST_DEG} [comment] —— [comment]。
     */
    private static int sectorAt(float x, float y) {
        int n = slots.length;
        if (rFan <= 0f || n <= 0) {
            return -1;
        }
        float r = radius(x, y);
        if (r < (sel >= 0 ? rDead * 0.8f : rDead)) {
            return -1;
        }
        float a = angleOf(x, y);
        int raw = n - 1;
        for (int k = 0; k < n - 1; k++) {
            if (a < bounds[k]) {
                raw = k;
                break;
            }
        }
        if (sel < 0 || raw == sel) {
            return raw;
        }
        // [comment] HYST_DEG [comment]（[comment]）
        if (raw > sel) {
            return a > bounds[sel] + HYST_DEG ? raw : sel;
        }
        return a < bounds[sel - 1] - HYST_DEG ? raw : sel;
    }

    private static float radius(float x, float y) {
        float dx = x - cx;
        float dy = y - cy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * [comment]"[comment]"[comment]（[comment]）：0 = [comment]，[comment] = [comment]，[comment] = [comment]。
     */
    private static float angleOf(float x, float y) {
        float dx = side == 1 ? cx - x : x - cx;      // Text
        float dy = y - cy;                            // Text
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    /** [comment]"[comment] = +x"[comment]：+ [comment]Yes[comment]，[comment] */
    private static float canvasDeg(float a) {
        return side == 1 ? 180f - a : a;
    }

    // ------------------------------------------------------------------ [comment]

    private static final class GearView extends View {

        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint scrim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();

        GearView(Context c) {
            super(c);
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(1.2f * density);
            glyph.setStyle(Paint.Style.STROKE);
            glyph.setStrokeWidth(1.9f * density);
            glyph.setStrokeCap(Paint.Cap.ROUND);
            glyph.setStrokeJoin(Paint.Join.ROUND);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            scrim.setShader(new RadialGradient(cx, cy, rFan <= 0f ? 1f : rFan * 1.1f,
                    C_SCRIM_IN, C_SCRIM_OUT, Shader.TileMode.CLAMP));
        }

        @Override
        protected void onDraw(Canvas cv) {
            if (!shown || rFan <= 0f) {
                return;
            }

            // ① [comment]。[comment]（[comment]）
            long now = SystemClock.uptimeMillis();
            float dt = (now - lastTick) / 1000f;
            lastTick = now;
            if (dt < 0f) {
                dt = 0f;
            }
            if (dt > MAX_STEP_S) {
                dt = MAX_STEP_S;
            }
            rotDeg = (rotDeg + dt * speedNow()) % 360f;
            postInvalidateOnAnimation();

            // [comment] 0..1（[comment]）
            float t = 1f;
            if (sel >= 0 || prevSel >= 0) {
                long d = now - selAt;
                t = d >= SEL_ANIM_MS ? 1f : Math.max(0f, d / (float) SEL_ANIM_MS);
            }
            float ease = 1f - (1f - t) * (1f - t);
            boolean shrinking = prevSel >= 0 && prevSel != sel && t < 1f;

            // ② [comment]，[comment]
            cv.drawCircle(cx, cy, rFan * 1.1f, scrim);

            // ③ [comment]。[comment]，[comment]、[comment]
            int idle = sel >= 0 ? C_FILL_DIM : C_FILL;
            for (int i = 0; i < slots.length; i++) {
                if (i == sel || (shrinking && i == prevSel)) {
                    continue;
                }
                sector(cv, i, idle, C_EDGE, 1f);
            }
            if (shrinking) {
                sector(cv, prevSel, C_FILL_DIM, C_EDGE, 1f + SEL_GROW * (1f - ease));
            }
            if (sel >= 0) {
                float grow = 1f + SEL_GROW * ease;
                sector(cv, sel, C_ACCENT_FILL, C_ACCENT, grow);
                // [comment]
                arcStroke(cv, sel, C_ACCENT_GLOW, 6.5f * density, grow);
            }

            // ④ [comment]（[comment]）
            drawGear(cv);

            // ⑤ [comment]，[comment]
            float dir = side == 1 ? -1f : 1f;
            for (int i = 0; i < slots.length; i++) {
                int act = slots[i];
                float grow = i == sel ? 1f + SEL_GROW * ease
                        : (shrinking && i == prevSel ? 1f + SEL_GROW * (1f - ease) : 1f);
                float am = (float) Math.toRadians(-FAN_HALF + sectorDeg * (i + 0.5f));
                float rm = (rInner + rFan) * 0.5f * grow;
                float gx = cx + dir * (float) Math.cos(am) * rm;
                float gy = cy + (float) Math.sin(am) * rm;
                boolean on = i == sel;
                float s = (on ? 12.5f : 11f) * density;

                glyph.setColor(on ? C_TEXT_ON : C_TEXT);
                glyph.setStrokeWidth((on ? 2.5f : 1.9f) * density);
                CoverGlyphs.draw(cv, glyph, dot, path, rect, act,
                        gx, gy - 10f * density, s, dir);

                text.setColor(on ? C_TEXT_ON : C_TEXT);
                text.setTextSize((on ? 14.5f : 12.5f) * density);
                cv.drawText(CoverGlyphs.label(act), gx, gy + 23f * density, text);
            }
        }

        /** [comment]（[comment] / [comment]） */
        private static float speedNow() {
            return (sel >= 0 && sel < speed.length) ? speed[sel] : speed[0];
        }

        /** [comment] i [comment]（i：0 [comment] / 1 [comment] / 2 [comment]）；grow Yes[comment] */
        private void sector(Canvas cv, int i, int fillColor, int edgeColor, float grow) {
            float a0 = -FAN_HALF + sectorDeg * i;
            float in = rInner * grow;
            float out = rFan * grow;
            float start = canvasDeg(a0);
            float sweep = side == 1 ? -sectorDeg : sectorDeg;

            path.reset();
            rect.set(cx - out, cy - out, cx + out, cy + out);
            path.addArc(rect, start, sweep);
            rect.set(cx - in, cy - in, cx + in, cy + in);
            path.arcTo(rect, start + sweep, -sweep);
            path.close();

            fill.setStyle(Paint.Style.FILL);
            fill.setShader(null);
            fill.setColor(fillColor);
            cv.drawPath(path, fill);
            edge.setColor(edgeColor);
            cv.drawPath(path, edge);
        }

        /** [comment] i [comment]（[comment]） */
        private void arcStroke(Canvas cv, int i, int color, float width, float grow) {
            float a0 = -FAN_HALF + sectorDeg * i;
            float in = rInner * grow;
            float out = rFan * grow;
            float start = canvasDeg(a0);
            float sweep = side == 1 ? -sectorDeg : sectorDeg;
            edge.setColor(color);
            edge.setStrokeWidth(width);
            rect.set(cx - out, cy - out, cx + out, cy + out);
            cv.drawArc(rect, start, sweep, false, edge);
            rect.set(cx - in, cy - in, cx + in, cy + in);
            cv.drawArc(rect, start, sweep, false, edge);
            edge.setStrokeWidth(1.2f * density);
        }

        /**
         * [comment]：[comment] + [comment] + [comment]，[comment] {@link #rotDeg}。
         *
         * <p>[comment]（[comment]，[comment]）。
         */
        private void drawGear(Canvas cv) {
            float r = rGear;
            float tw = r * 0.30f;
            float th = r * 0.42f;

            // [comment]：[comment]
            fill.setStyle(Paint.Style.FILL);
            fill.setShader(null);
            fill.setColor(C_GEAR_HALO);
            cv.drawCircle(cx, cy, r * 1.45f, fill);

            cv.save();
            cv.rotate(rotDeg, cx, cy);

            fill.setColor(C_GEAR);
            edge.setColor(C_GEAR_DARK);
            edge.setStrokeWidth(1.2f * density);
            for (int k = 0; k < TEETH; k++) {
                cv.save();
                cv.rotate(k * (360f / TEETH), cx, cy);
                rect.set(cx - tw / 2f, cy - r - th * 0.30f, cx + tw / 2f, cy - r + th * 0.70f);
                cv.drawRoundRect(rect, tw * 0.30f, tw * 0.30f, fill);
                cv.drawRoundRect(rect, tw * 0.30f, tw * 0.30f, edge);
                cv.restore();
            }
            // [comment]
            fill.setColor(C_GEAR);
            cv.drawCircle(cx, cy, r, fill);
            cv.drawCircle(cx, cy, r, edge);

            // [comment]：[comment]，[comment]Yes[comment]
            fill.setColor(C_GEAR_HUB);
            cv.drawCircle(cx, cy, r * 0.38f, fill);
            edge.setColor(C_GEAR);
            edge.setStrokeWidth(1.6f * density);
            cv.drawCircle(cx, cy, r * 0.38f, edge);
            edge.setStrokeWidth(1.2f * density);

            cv.restore();
        }
    }
}
