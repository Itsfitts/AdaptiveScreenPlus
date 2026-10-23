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
 * <p>[comment]，[comment]（[comment]）。<b>[comment]</b>
 * —— [comment]，[comment]；[comment]<b>[comment]</b>，
 * [comment] {@link ExtPrefs#sidebarSlots(Context)} [comment] 1 ~ 4 [comment]（[comment] {@code 180 / [comment]} [comment]）。
 * [comment]<b>[comment]</b>，[comment]。[comment]，
 * [comment]；<b>[comment]，[comment]</b>。[comment]、
 * [comment]、[comment]。
 *
 * <p>⚠ [comment] {@link CoverWheel}（[comment]）[comment]：[comment]Yes[comment]、[comment]<b>[comment]</b>[comment]；
 * [comment]Yes[comment]、[comment]<b>[comment]</b>[comment]。
 *
 * <p><b>[comment]</b>：[comment]，[comment] —— [comment]Yes[comment]
 * <b>[comment]</b>：[comment]，[comment]，[comment]。
 *
 * <p><b>[comment] 180° [comment]</b>：[comment]。[comment]Yes[comment] {@code [-90°, +90°]} [comment]
 * [comment]，[comment]。[comment] 180° [comment]<b>[comment]</b>（[comment] 2n [comment]）——
 * [comment]Yes[comment] n [comment]，[comment]、[comment]，[comment]。
 *
 * <p><b>[comment]</b>：[comment]Yes[comment]、[comment] 0 [comment]，[comment]
 * —— [comment] {@link #SPIN_MIN_DEG} [comment]，"[comment]"[comment]（[comment]"[comment]"
 * [comment]）。
 *
 * <p><b>[comment]</b>（[comment] {@link CoverWheel} / {@link CoverGear} [comment]）：
 * <ol>
 *   <li>[comment]Yes[comment] {@code FLAG_NOT_TOUCHABLE} [comment]：[comment]，
 *       [comment]，[comment]；</li>
 *   <li>[comment]（[comment]），No[comment] 66px [comment]；</li>
 *   <li>{@code TYPE_ACCESSIBILITY_OVERLAY} [comment]None[comment]。</li>
 * </ol>
 */
public final class CoverRoulette {

    private static final String TAG = "CoverRoulette";

    // ------------------------------------------------------------------ [comment]

    /** [comment]（[comment]）：[comment]，[comment] */
    private static final float SPAN_DEG = 180f;
    /** [comment] = [comment] / [comment] × [comment]（[comment]，[comment]） */
    private static final float RAT_R = 0.40f;
    /** [comment]（dp）—— [comment]，[comment] */
    private static final int ARROW_DP = 18;
    /** [comment]（[comment]） */
    private static final float RAT_HUB = 0.15f;
    /** [comment]（[comment]） */
    private static final float RAT_LABEL = 0.62f;
    /** [comment]（[comment]）[comment] —— [comment]，[comment] */
    private static final float RAT_DEAD = 0.16f;
    /** [comment]"[comment]"[comment]（[comment]） */
    private static final float SEL_POP = 0.045f;
    /** [comment]，"[comment]"[comment]（[comment]） */
    private static final float SPIN_MIN_DEG = 18f;
    /** [comment]（dp） */
    private static final float RIM_DP = 3.2f;
    /** [comment]（dp）—— [comment]，[comment] */
    private static final float STUD_DP = 3.2f;

    // ------------------------------------------------------------------ [comment]

    /** [comment] / [comment]（ms） */
    private static final long SEL_ANIM_MS = 170L;
    /** [comment]：[comment]、[comment] */
    private static final long BUZZ_MS = 12L;
    private static final int BUZZ_AMP = 110;
    /** [comment]（ms）—— [comment] */
    private static final long BUZZ_GAP_MS = 60L;
    private static final long FADE_IN_MS = 90L;
    private static final long FADE_OUT_MS = 130L;

    // ------------------------------------------------------------------ [comment]

    /** [comment]：[comment]，[comment] */
    private static final int C_RIM = 0xFF4C9BFF;
    private static final int C_RIM_DARK = 0xFF1B5FB4;
    /** [comment]，[comment] */
    private static final int C_HALO = 0x4D4C9BFF;
    private static final int C_HUB_HOLE = 0x66000000;
    /**
     * [comment]。
     *
     * <p>[comment]：<b>[comment]</b>。[comment]，[comment]
     * （[comment]Yes 3 [comment]）；[comment]Yes 4，[comment]。
     */
    private static final int[] FILLS = {0x1FFFFFFF, 0x14A8C8FF, 0x0DFFFFFF, 0x1A7AA2FF};
    /** [comment]（[comment]）[comment] */
    private static final int C_ACCENT_FILL = 0x99518FE6;
    private static final int C_ACCENT = 0xFF5E9CF0;
    private static final int C_ACCENT_GLOW = 0x4D5E9CF0;
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
    private static int screenH;
    private static float density = 2.125f;
    /** 0 = [comment]，1 = [comment] */
    private static int side = 1;

    // ------------------------------------------------------------------ [comment]

    private static FrameLayout root;
    private static RouletteView view;
    private static float cx;
    private static float cy;
    private static float rWheel;
    private static float rHub;
    private static float rDead;
    /** [comment]（[comment]）；NaN = [comment]，[comment]、[comment] */
    private static float lastFingerDeg = Float.NaN;
    /** [comment]（[comment]），[comment] {@link #rotStart} [comment]。⚠ [comment] mod 180 [comment]（[comment]） */
    private static float rotDeg;
    /** [comment]：[comment] 0 [comment] */
    private static float rotStart;
    /** [comment]（[comment]）—— [comment] {@link #SPIN_MIN_DEG} [comment]"[comment]" */
    private static float spunDeg;
    /** [comment]Yes[comment]；-1 = [comment]，[comment] */
    private static int sel = -1;
    /** [comment]Yes[comment] —— [comment]"[comment]"[comment] */
    private static int prevSel = -1;
    /** sel [comment]（uptimeMillis），[comment] */
    private static long selAt;
    private static long lastBuzzAt;
    private static boolean shown;
    /** [comment]：[comment]，[comment] onDraw [comment] prefs */
    private static int[] slots = {ExtPrefs.RING_BACK, ExtPrefs.RING_RECENTS,
            ExtPrefs.RING_NOTIFY};
    /** [comment]（= 180 / [comment]）—— [comment] */
    private static float stepDeg = 60f;

    private CoverRoulette() {
    }

    static void env(Context ctx, WindowManager w, int wPx, int hPx, float den, int sd) {
        displayCtx = ctx;
        wm = w;
        screenW = wPx;
        screenH = hPx;
        density = den;
        side = sd;
        rWheel = Math.min(wPx, hPx) * RAT_R;
        rHub = rWheel * RAT_HUB;
        rDead = rWheel * RAT_DEAD;
        setSlotCount(slots.length);
        // [comment]（[comment]），[comment] —— [comment] CoverWheel.ensure()
        ensure();
    }

    /**
     * [comment]：<b>[comment]</b>（0 = 3 [comment]，[comment]）。
     *
     * <p>[comment] ⇒ [comment]（0°）；[comment] ⇒ [comment]（180°）。
     */
    private static float arrowDeg() {
        return side == 1 ? 180f : 0f;
    }

    /**
     * [comment]。⚠ [comment]，[comment] onDraw [comment]
     *
     * <p>[comment] {@link #SPAN_DEG} [comment]，[comment]Yes {@code 180 / [comment]}
     * —— [comment]Yes 360。[comment] 0 [comment]，[comment] {@code 90 - [comment]/2} [comment]。
     */
    private static void setSlotCount(int n) {
        if (n < 1) {
            n = 1;
        }
        if (n > ExtPrefs.SIDEBAR_ITEM_IDS.length) {
            n = ExtPrefs.SIDEBAR_ITEM_IDS.length;
        }
        stepDeg = SPAN_DEG / n;
        rotStart = normHalf(SPAN_DEG / 2f - stepDeg / 2f);
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
     * [comment]"[comment]"Yes[comment]，[comment]。
     */
    private static void ensure() {
        if (root != null || displayCtx == null || wm == null) {
            return;
        }
        try {
            FrameLayout r = new FrameLayout(displayCtx);
            RouletteView v = new RouletteView(displayCtx);
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
            Log.i(TAG, "Text（Text）：Text " + screenW + "x" + screenH
                    + "px，Text " + (int) rWheel + "px Text " + (int) rHub + "px");
        } catch (Throwable t) {
            Log.e(TAG, "TextFailed", t);
            root = null;
            view = null;
        }
    }

    /**
     * [comment]：[comment]。
     *
     * <p>⚠ [comment] / [comment] —— [comment]（[comment]），
     * [comment]；[comment]<b>[comment]</b>（[comment]，
     * [comment]），[comment]。[comment]，[comment]。
     */
    static void prepare(float x, float y) {
        if (displayCtx != null) {
            int[] got = ExtPrefs.sidebarSlots(displayCtx);
            if (got != null && got.length > 0) {
                slots = got;
            }
        }
        setSlotCount(slots.length);
        // [comment]：[comment]，[comment]
        cx = side == 1 ? screenW : 0f;
        cy = clamp(y, rWheel, screenH - rWheel);
        rotDeg = rotStart;
        spunDeg = 0f;
        lastFingerDeg = Float.NaN;
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
        if (view != null) {
            view.setVisibility(View.VISIBLE);
            view.invalidate();
        }
        root.animate().cancel();
        root.animate().alpha(1f).setDuration(FADE_IN_MS).start();
        Log.i(TAG, "Text：Text " + (int) cx + "," + (int) cy + "（Text"
                + (side == 1 ? "Text" : "Text") + "Text），Text " + (int) rWheel + "px，"
                + slots.length + " Text " + (int) SPAN_DEG + "° Text，Text "
                + (int) stepDeg + "°；Text（" + (int) arrowDeg() + "° Text）");
    }

    /**
     * [comment]：<b>[comment]，[comment]</b>。
     *
     * <p>⚠ [comment] {@link #norm180} [comment] —— [comment] ±180 [comment]，[comment]Yes
     * 350 [comment]，[comment]。
     *
     * <p>⚠ [comment]，[comment] ⇒ [comment]，[comment]；
     * [comment]（[comment] 0）[comment]，[comment]
     * {@link #rDead} [comment]，[comment]。
     */
    static void move(float x, float y) {
        if (radius(x, y) >= rDead) {
            float a = polarDeg(x, y);
            if (!Float.isNaN(lastFingerDeg)) {
                float d = norm180(a - lastFingerDeg);
                rotDeg = norm360(rotDeg + d);
                spunDeg += Math.abs(d);
            }
            lastFingerDeg = a;
        } else {
            // [comment]：[comment]Yes[comment]，[comment]，[comment]
            lastFingerDeg = Float.NaN;
        }
        int now = spunDeg >= SPIN_MIN_DEG ? slotUnderArrow() : -1;
        if (now != sel) {
            prevSel = sel;
            sel = now;
            selAt = SystemClock.uptimeMillis();
            // [comment]；"[comment]"Yes[comment]，[comment]
            if (now >= 0) {
                buzz();
            }
            Log.i(TAG, now < 0
                    ? "Text " + (int) SPIN_MIN_DEG + "°（Text " + (int) spunDeg + "°），Text"
                    : "Text " + now + " Text（Text " + (int) rotDeg + "°，Text "
                            + (int) spunDeg + "°）");
        }
        if (view != null) {
            view.invalidate();
        }
    }

    /** [comment]：[comment]（[comment] {@link ExtPrefs#RING_NONE} = [comment]，[comment]） */
    static int pick() {
        if (root == null || !shown || spunDeg < SPIN_MIN_DEG) {
            return ExtPrefs.RING_NONE;
        }
        int now = slotUnderArrow();
        return (now < 0 || now >= slots.length) ? ExtPrefs.RING_NONE : slots[now];
    }

    /** [comment]。⚠ [comment] —— [comment]，[comment]Yes[comment] */
    static void hide() {
        final FrameLayout r = root;
        final RouletteView v = view;
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
     * [comment]。
     *
     * <p>[comment] i [comment] {@code [arrowDeg - 90 + i * step, ...]} [comment]；[comment] rotDeg
     * [comment]。[comment]Yes"[comment]"= [comment] {@code 90 - rotDeg} [comment] {@code [0, 180)}，
     * [comment]。
     *
     * <p>⚠ [comment]Yes <b>180 [comment]Yes 360</b>（{@link #normHalf}）：[comment]、[comment]Yes[comment] 180°
     * [comment]，[comment] 180° [comment]Yes[comment] —— [comment]Yes"[comment]"[comment]。
     */
    private static int slotUnderArrow() {
        int n = slots.length;
        if (n <= 0) {
            return -1;
        }
        int i = (int) Math.floor(normHalf(SPAN_DEG / 2f - rotDeg) / stepDeg);
        if (i < 0) {
            i = 0;
        }
        return i >= n ? n - 1 : i;
    }

    /** [comment]（[comment]）：0 = [comment]，90 = [comment]（[comment]），[comment] */
    private static float polarDeg(float x, float y) {
        return (float) Math.toDegrees(Math.atan2(y - cy, x - cx));
    }

    private static float radius(float x, float y) {
        float dx = x - cx;
        float dy = y - cy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** [comment] 0 ~ 360 [comment] */
    private static float norm360(float d) {
        d %= 360f;
        return d < 0f ? d + 360f : d;
    }

    /** [comment] -180 ~ 180 [comment]（[comment] ±180 [comment]） */
    private static float norm180(float d) {
        d %= 360f;
        if (d > 180f) {
            d -= 360f;
        } else if (d < -180f) {
            d += 360f;
        }
        return d;
    }

    /** [comment] 0 ~ 180 [comment] —— [comment]，[comment]Yes[comment] 180° [comment] */
    private static float normHalf(float d) {
        d %= SPAN_DEG;
        return d < 0f ? d + SPAN_DEG : d;
    }

    private static float clamp(float v, float lo, float hi) {
        if (hi < lo) {
            return (lo + hi) / 2f;      // Text（Text）：Text
        }
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int dp(float v) {
        return Math.round(v * density);
    }

    // ------------------------------------------------------------------ [comment]

    private static final class RouletteView extends View {

        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glyph = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint scrim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();
        /**
         * [comment]。
         *
         * <p>⚠ [comment]Yes[comment]，[comment] (0,0) [comment] ——
         * [comment]"[comment] (0,0) [comment]"[comment]，[comment]，[comment]。
         * [comment]，[comment]。
         */
        private float scx = Float.NaN;
        private float scy = Float.NaN;
        private float scr = -1f;

        RouletteView(Context c) {
            super(c);
            edge.setStyle(Paint.Style.STROKE);
            edge.setStrokeWidth(1.2f * density);
            glyph.setStyle(Paint.Style.STROKE);
            glyph.setStrokeWidth(1.9f * density);
            glyph.setStrokeCap(Paint.Cap.ROUND);
            glyph.setStrokeJoin(Paint.Join.ROUND);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTypeface(Typeface.DEFAULT_BOLD);
        }

        @Override
        protected void onDraw(Canvas cv) {
            if (!shown || rWheel <= 0f) {
                return;
            }
            final int n = slots.length;
            if (n <= 0) {
                return;
            }
            final int pieces = n * 2;      // Text 2n Text（Text 180° Text）
            long now = SystemClock.uptimeMillis();

            // [comment] 0..1（[comment]）
            float t = 1f;
            if (sel >= 0 || prevSel >= 0) {
                long d = now - selAt;
                t = d >= SEL_ANIM_MS ? 1f : Math.max(0f, d / (float) SEL_ANIM_MS);
                if (t < 1f) {
                    postInvalidateOnAnimation();
                }
            }
            final float ease = 1f - (1f - t) * (1f - t);
            final boolean shrinking = prevSel >= 0 && prevSel != sel && t < 1f;
            final float popSel = SEL_POP * ease;
            final float popPrev = SEL_POP * (1f - ease);

            // ① [comment]，[comment]
            if (cx != scx || cy != scy || rWheel != scr) {
                scx = cx;
                scy = cy;
                scr = rWheel;
                scrim.setShader(new RadialGradient(cx, cy, Math.max(1f, rWheel * 1.15f),
                        C_SCRIM_IN, C_SCRIM_OUT, Shader.TileMode.CLAMP));
            }
            cv.drawCircle(cx, cy, rWheel * 1.15f, scrim);

            // ② [comment]。[comment] n [comment]（Canvas [comment]），[comment]。
            //    [comment]，[comment]、[comment]
            for (int k = 0; k < pieces; k++) {
                if (isOn(k) || (shrinking && isPrev(k))) {
                    continue;
                }
                slice(cv, k, FILLS[(k % n) % FILLS.length], C_EDGE, 0f);
            }
            if (shrinking) {
                for (int k = 0; k < pieces; k++) {
                    if (isPrev(k)) {
                        slice(cv, k, FILLS[(k % n) % FILLS.length], C_EDGE, popPrev);
                    }
                }
            }
            if (sel >= 0) {
                for (int k = 0; k < pieces; k++) {
                    if (isOn(k)) {
                        slice(cv, k, C_ACCENT_FILL, C_ACCENT, popSel);
                        if (n > 1) {
                            arcGlow(cv, k, C_ACCENT_GLOW, 6.5f * density, popSel);
                        }
                    }
                }
            }

            // ③ [comment] + [comment] + [comment]
            drawRim(cv, pieces);

            // ④ [comment]，[comment]（[comment]）。
            //    [comment]。
            float dir = side == 1 ? -1f : 1f;
            for (int k = 0; k < pieces; k++) {
                int act = slots[k % n];
                float am = (float) Math.toRadians(startDeg(k) + stepDeg * 0.5f);
                float cp = (float) Math.cos(am);
                if (side == 1 ? cp >= 0f : cp <= 0f) {
                    continue;      // Text，Text
                }
                float pop = isOn(k) ? popSel : (shrinking && isPrev(k) ? popPrev : 0f);
                float rm = rWheel * RAT_LABEL + pop * rWheel;
                float px = cx + cp * rm;
                float py = cy + (float) Math.sin(am) * rm;
                boolean on = isOn(k);
                float s = (on ? 12.5f : 11f) * density;

                glyph.setColor(on ? C_TEXT_ON : C_TEXT);
                glyph.setStrokeWidth((on ? 2.5f : 1.9f) * density);
                CoverGlyphs.draw(cv, glyph, dot, path, rect, act,
                        px, py - 10f * density, s, dir);

                text.setColor(on ? C_TEXT_ON : C_TEXT);
                text.setTextSize((on ? 14.5f : 12.5f) * density);
                cv.drawText(CoverGlyphs.label(act), px, py + 23f * density, text);
            }

            // ⑤ [comment]：[comment]，[comment]，[comment]
            drawArrow(cv);
        }

        /** [comment] k [comment]Yes[comment]Yes[comment] */
        private static boolean isOn(int k) {
            return sel >= 0 && k % slots.length == sel;
        }

        /** [comment] k [comment]Yes[comment]Yes"[comment]"[comment] */
        private static boolean isPrev(int k) {
            return prevSel >= 0 && k % slots.length == prevSel;
        }

        /**
         * [comment] k [comment]（0 = 3 [comment]，[comment] = [comment]）。
         *
         * <p>[comment]Yes"[comment]"（[comment]），[comment] —— [comment] 180° [comment]
         * （{@link #normHalf}），[comment] 2n [comment]None[comment]。
         */
        private static float startDeg(int k) {
            return arrowDeg() - SPAN_DEG / 2f + normHalf(rotDeg) + k * stepDeg;
        }

        /** [comment] k [comment]（[comment]）；pop = [comment]（[comment]） */
        private void slice(Canvas cv, int k, int fillColor, int edgeColor, float pop) {
            float start = startDeg(k);
            fill.setStyle(Paint.Style.FILL);
            fill.setShader(null);
            fill.setColor(fillColor);
            edge.setStyle(Paint.Style.STROKE);
            edge.setColor(edgeColor);
            edge.setStrokeWidth(1.2f * density);

            cv.save();
            if (pop != 0f) {
                float am = (float) Math.toRadians(start + stepDeg * 0.5f);
                cv.translate((float) Math.cos(am) * pop * rWheel,
                        (float) Math.sin(am) * pop * rWheel);
            }
            path.reset();
            path.moveTo(cx, cy);
            rect.set(cx - rWheel, cy - rWheel, cx + rWheel, cy + rWheel);
            path.arcTo(rect, start, stepDeg);
            path.close();
            cv.drawPath(path, fill);
            cv.drawPath(path, edge);
            cv.restore();
        }

        /** [comment] k [comment]（[comment]） */
        private void arcGlow(Canvas cv, int k, int color, float width, float pop) {
            float start = startDeg(k);
            edge.setStyle(Paint.Style.STROKE);
            edge.setColor(color);
            edge.setStrokeWidth(width);
            cv.save();
            if (pop != 0f) {
                float am = (float) Math.toRadians(start + stepDeg * 0.5f);
                cv.translate((float) Math.cos(am) * pop * rWheel,
                        (float) Math.sin(am) * pop * rWheel);
            }
            rect.set(cx - rWheel, cy - rWheel, cx + rWheel, cy + rWheel);
            cv.drawArc(rect, start, stepDeg, false, edge);
            cv.restore();
            edge.setStrokeWidth(1.2f * density);
        }

        /** [comment] + [comment] + [comment] + [comment] */
        private void drawRim(Canvas cv, int pieces) {
            edge.setStyle(Paint.Style.STROKE);
            edge.setColor(C_HALO);
            edge.setStrokeWidth(9f * density);
            cv.drawCircle(cx, cy, rWheel * 1.01f, edge);

            edge.setColor(C_RIM);
            edge.setStrokeWidth(RIM_DP * density);
            cv.drawCircle(cx, cy, rWheel, edge);
            edge.setColor(C_RIM_DARK);
            edge.setStrokeWidth(1.2f * density);
            cv.drawCircle(cx, cy, rWheel * 0.982f, edge);

            fill.setStyle(Paint.Style.FILL);
            fill.setShader(null);
            fill.setColor(C_RIM);
            for (int k = 0; k < pieces; k++) {
                double a = Math.toRadians(startDeg(k));
                cv.drawCircle(cx + (float) Math.cos(a) * rWheel * 0.945f,
                        cy + (float) Math.sin(a) * rWheel * 0.945f, STUD_DP * density, fill);
            }
            // [comment]：[comment] + [comment]，[comment]（[comment]，[comment]）
            cv.drawCircle(cx, cy, rHub, fill);
            fill.setColor(C_HUB_HOLE);
            cv.drawCircle(cx, cy, rHub * 0.45f, fill);
        }

        /**
         * [comment]：[comment]，[comment]，[comment]。
         *
         * <p>[comment]Yes[comment] {@link #arrowDeg()} [comment]
         * —— [comment]，[comment]。
         */
        private void drawArrow(Canvas cv) {
            float tip = rWheel - 4f * density;
            float back = rWheel + dp(ARROW_DP);
            float half = 13f * density;
            cv.save();
            cv.translate(cx, cy);
            cv.rotate(arrowDeg());
            path.reset();
            path.moveTo(tip, 0f);
            path.lineTo(back, -half);
            path.lineTo(back, half);
            path.close();
            fill.setStyle(Paint.Style.FILL);
            fill.setShader(null);
            fill.setColor(C_RIM);
            cv.drawPath(path, fill);
            edge.setStyle(Paint.Style.STROKE);
            edge.setColor(C_RIM_DARK);
            edge.setStrokeWidth(1.2f * density);
            cv.drawPath(path, edge);
            cv.restore();
        }
    }
}
