package com.wb.extrotator;

import android.accessibilityservice.AccessibilityService;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * [comment]<b>[comment]</b>：[comment]，[comment]。
 *
 * <p>[comment]、<b>[comment]</b>[comment]None[comment]，[comment]。[comment]Yes[comment]，
 * [comment] {@link InnerGesture} [comment]：[comment]"[comment]"，[comment]"[comment]"。
 *
 * <p><b>[comment]</b>[comment]：
 * <ul>
 *   <li><b>[comment]</b> —— [comment]、[comment]，[comment]；
 *       [comment] {@link CoverBlur}（[comment]）[comment]。[comment]"[comment]"Yes[comment]：
 *       [comment]，[comment]，
 *       [comment]，[comment]。[comment] 1:1 [comment]。</li>
 *   <li><b>[comment]</b> —— [comment]<b>[comment]</b>，[comment]Yes[comment]（[comment]）。
 *       [comment] MyGesture [comment] {@code bottom_light.png}：[comment]「[comment]」[comment]
 *       [comment]，[comment]（[comment] × [comment] 5% [comment]）。</li>
 * </ul>
 * <p>[comment]"[comment]"[comment] {@code InnerGesture} [comment]（[comment] = [comment] 70%）。
 * [comment] 40% [comment] —— [comment]、[comment]，Yes[comment]。
 *
 * <p><b>[comment]</b>（{@link ExtPrefs#gestureAnimStyle}）：
 * <ul>
 *   <li><b>[comment]</b>（{@link #STYLE_NATIVE}）—— [comment]<b>[comment]</b>、
 *       [comment]<b>[comment]</b>[comment]。</li>
 *   <li><b>[comment]</b>（{@link #STYLE_GLOW}）—— [comment]<b>[comment]</b>：
 *       [comment]，[comment] {@link #GLOW_DEPTH_DP} dp [comment]，[comment]；
 *       [comment]，[comment] RGB [comment]。
 *       [comment]Yes MyGesture [comment]<b>Default[comment]「[comment]」</b>——[comment]Yes
 *       {@code left_light.png} [comment]<b>[comment]</b>，[comment] {@link #TONES}
 *       [comment] {@code setTint}，[comment]。
 *       ⚠ [comment]<b>[comment]Yes[comment]</b>（[comment]Yes[comment]「[comment]」[comment]、pro [comment]，[comment]），
 *       <b>[comment]Yes[comment]</b>（[comment]Yes[comment]）。
 *       [comment] {@code left_light / right_light}，[comment] {@code bottom_light}
 *       ——<b>[comment]</b>，[comment]Yes[comment]。</li>
 * </ul>
 *
 * <p>⚠ <b>[comment] gravity</b>：[comment]"[comment]"[comment]，{@code x / y} [comment]
 * <b>[comment]</b> —— [comment]，[comment]
 * [comment]"[comment]Yes[comment]"。v4.21b [comment]（RULES §114.2）。
 *
 * <p>⚠ <b>[comment] = [comment]</b>：[comment]，[comment]
 * {@link InnerGesture#install} [comment]<b>[comment]</b>[comment]，[comment]（[comment]）。
 */
public final class GestureAnim {

    private static final String TAG = "GestureAnim";

    /** [comment]：[comment] / [comment] / [comment] / [comment] */
    public static final int KIND_NONE = 0;
    public static final int KIND_BACK_L = 1;
    public static final int KIND_BACK_R = 2;
    public static final int KIND_HOME = 3;

    /** [comment]：0 = [comment]（Default），1 = [comment] */
    public static final int STYLE_NATIVE = 0;
    public static final int STYLE_GLOW = 1;

    /** [comment]：[comment]、[comment]、[comment]（dp） */
    private static final float NATIVE_R_DP = 17f;
    private static final float NATIVE_INSET_DP = 8f;
    private static final float NATIVE_PUSH_DP = 16f;
    /** [comment]：[comment]、[comment]（dp；[comment]） */
    private static final float NATIVE_ARM_DP = 5f;
    private static final float NATIVE_WING_DP = 4.5f;

    /**
     * [comment]：[comment]（[comment]）—— MyGesture [comment]Yes[comment]。
     * [comment]、[comment]（[comment]）。
     */
    private static final float GLOW_SPAN = 0.88f;
    /**
     * [comment]：[comment]（dp；[comment]）—— 32 → 40 Yes「[comment]」[comment]。
     */
    private static final float GLOW_DEPTH_DP = 40f;
    /** [comment]：[comment]（[comment]，[comment]） */
    private static final int GLOW_BLOBS = 9;
    /** [comment]：[comment] = [comment]（[comment]，[comment]） */
    private static final float GLOW_BLOB_R = 0.115f;
    /** [comment]：[comment] —— 1 = [comment]，[comment] RGB [comment] */
    private static final int GLOW_HUE_STEP = 1;
    /**
     * [comment]：[comment]。
     *
     * <p>[comment] {@code left_light.png} [comment] 180/255（= 0.71）[comment] 0.34/[comment]。
     * [comment]：[comment] 0.46，[comment] 0.58 —— [comment]
     * [comment]。[comment]Yes<b>[comment]</b>[comment]「[comment]」，[comment]Yes[comment]。
     * [comment]：[comment] alpha [comment]。
     */
    private static final float GLOW_GAIN = 0.58f;

    /**
     * [comment] 16 [comment]。[comment] MyGesture [comment]（{@code me.hisn.utils.k} [comment]）：
     * [comment] → [comment] → [comment] → [comment] → [comment] → [comment] → [comment] → [comment] → [comment]。[comment]<b>[comment]</b>，
     * [comment]"[comment] RGB [comment]"[comment]。
     */
    private static final int[] TONES = {
            0xFFE51C23, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7,
            0xFF3F51B5, 0xFF5677FC, 0xFF03A9F4, 0xFF00BCD4,
            0xFF009688, 0xFF259B24, 0xFF8BC34A, 0xFFCDDC39,
            0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722,
    };

    /** [comment]：[comment]（dp）、[comment] */
    private static final float BLUR_MIN_DP = 7f;
    private static final float BLUR_MAX_DP = 26f;
    /** [comment]，[comment]（[comment]） */
    private static final float ALPHA_AT = 0.35f;
    /** [comment] / [comment]（[comment]）[comment] */
    private static final float SCRIM = 0.20f;
    private static final float SCRIM_NO_SHOT = 0.55f;
    /** [comment]，[comment]None[comment] */
    private static final float RAD_STEP = 2f;
    /** [comment]Failed[comment]（[comment]） */
    private static final long RETRY_MS = 1500L;

    private static WindowManager wm;
    private static AccessibilityService svc;
    private static AnimView root;
    private static float density = 3f;
    private static int sw;
    private static int sh;
    /** [comment]（[comment]）。[comment] */
    private static float fingerX;
    private static float fingerY;

    private GestureAnim() {
    }

    /** [comment] */
    static boolean installed() {
        return root != null;
    }

    /** [comment]。[comment] {@link InnerGesture#install} [comment]（[comment]） */
    static void install(Context displayCtx, WindowManager w, AccessibilityService s,
                        int wPx, int hPx, float dens) {
        if (root != null) {
            return;
        }
        svc = s;
        wm = w;
        sw = wPx;
        sh = hPx;
        density = dens;
        fingerX = wPx / 2f;
        fingerY = hPx / 2f;
        try {
            AnimView v = new AnimView(displayCtx);
            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    sw, sh,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    // [comment]、[comment]（[comment]）；[comment] LAYOUT [comment]Yes[comment]
                    //（[comment] NO_LIMITS [comment] inset [comment]）
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT);
            // ⚠ [comment] gravity：[comment] x / y Yes"[comment]"[comment]
            p.gravity = Gravity.TOP | Gravity.START;
            p.x = 0;
            p.y = 0;
            w.addView(v, p);
            root = v;
            Log.i(TAG, "Text：" + sw + "x" + sh + "px");
        } catch (Throwable t) {
            Log.e(TAG, "TextFailed", t);
            wm = null;
            svc = null;
        }
    }

    /** [comment]。None[comment] —— [comment]Yes[comment] */
    static void remove() {
        AnimView v = root;
        WindowManager w = wm;
        root = null;
        wm = null;
        svc = null;
        if (v == null) {
            return;
        }
        v.abort();
        if (w != null) {
            try {
                w.removeViewImmediate(v);
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed", t);
            }
        }
        Log.i(TAG, "Text");
    }

    /** [comment]（[comment]）[comment]。[comment]、[comment]，[comment] */
    static void begin(int kind) {
        AnimView v = root;
        if (v == null || kind == KIND_NONE) {
            return;
        }
        v.start(kind, styleNow());
    }

    /** [comment]（[comment]）。[comment]，[comment] MOVE [comment] */
    static void at(float x, float y) {
        fingerX = x;
        fingerY = y;
    }

    /** [comment]：0 = [comment]，1 = [comment] */
    static void progress(float p) {
        AnimView v = root;
        if (v != null) {
            v.setProgress(p);
        }
    }

    /**
     * [comment]。
     *
     * @param fired [comment] —— [comment]，[comment]
     */
    static void end(boolean fired) {
        AnimView v = root;
        if (v != null) {
            v.finish(fired);
        }
    }

    /** [comment]Yes[comment]（[comment]）：[comment]，[comment] */
    static void cancel() {
        AnimView v = root;
        if (v != null) {
            v.abort();
        }
    }

    private static int styleNow() {
        Context c = svc;
        if (c == null) {
            return STYLE_NATIVE;
        }
        return ExtPrefs.gestureAnimStyle(c) == STYLE_GLOW ? STYLE_GLOW : STYLE_NATIVE;
    }

    private static float dpPx(float v) {
        return v * density;
    }

    /** [comment] {@code v} [comment] [lo, hi]；[comment]（[comment]）[comment] */
    private static float clamp(float v, float lo, float hi) {
        if (hi <= lo) {
            return (lo + hi) / 2f;
        }
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]：[comment]Yes[comment]（[comment]，[comment]），
     * [comment]，[comment]Yes[comment]。[comment]，[comment]Yes[comment]。
     */
    private static final class AnimView extends FrameLayout {

        private final ImageView blurIv;
        private final View scrim;
        private final IndView ind;

        private int kind = KIND_NONE;
        private int style = STYLE_NATIVE;
        /** 0..1：[comment] */
        private float p;
        /** [comment]Allow[comment]（[comment]Yes[comment]，[comment]） */
        private boolean visible;
        /** [comment]，No[comment] */
        private boolean hideForShot;
        private boolean asked;
        private long retryAfter;
        /** [comment]（[comment]，[comment]） */
        private int toneIdx;
        private float lastRadius = -1f;
        /** [comment]（[comment]，1:1 [comment]） */
        private Bitmap shot;
        private ValueAnimator anim;

        AnimView(Context c) {
            super(c);
            setLayerType(LAYER_TYPE_HARDWARE, null);
            blurIv = new ImageView(c);
            blurIv.setScaleType(ImageView.ScaleType.FIT_XY);
            blurIv.setAlpha(0f);
            addView(blurIv, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            scrim = new View(c);
            scrim.setBackgroundColor(0xFF000000);
            scrim.setAlpha(0f);
            addView(scrim, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            ind = new IndView(c, this);
            addView(ind, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
        }

        void start(int k, int st) {
            abortAnim();
            kind = k;
            style = st;
            p = 0f;
            setAlpha(1f);
            visible = true;
            hideForShot = false;
            asked = false;
            lastRadius = -1f;
            toneIdx = (int) (Math.random() * TONES.length);
            recycleShot();
            blurIv.setImageDrawable(null);
            if (k == KIND_HOME && st != STYLE_GLOW) {
                // [comment] —— [comment]。
                // ★ [comment]Yes[comment]，[comment]，[comment]、[comment]。
                grab();
            }
            apply();
        }

        void setProgress(float v) {
            if (!visible) {
                return;
            }
            float n = v < 0f ? 0f : (v > 1f ? 1f : v);
            if (n == p) {
                return;
            }
            p = n;
            apply();
        }

        void finish(boolean fired) {
            if (!visible) {
                return;
            }
            visible = false;
            hideForShot = false;
            if (fired) {
                p = 1f;
            }
            fadeOut(fired ? 200L : 140L);
        }

        void abort() {
            abortAnim();
            visible = false;
            hideForShot = false;
            kind = KIND_NONE;
            p = 0f;
            setAlpha(1f);
            recycleShot();
            blurIv.setImageDrawable(null);
            blurIv.setAlpha(0f);
            scrim.setAlpha(0f);
            ind.invalidate();
        }

        private void abortAnim() {
            ValueAnimator a = anim;
            anim = null;
            if (a != null) {
                a.cancel();
            }
        }

        private void fadeOut(long ms) {
            abortAnim();
            ValueAnimator a = ValueAnimator.ofFloat(getAlpha(), 0f);
            anim = a;
            a.setDuration(ms);
            a.addUpdateListener(x -> setAlpha((float) x.getAnimatedValue()));
            a.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator x) {
                    if (anim != x) {
                        return;
                    }
                    anim = null;
                    kind = KIND_NONE;
                    p = 0f;
                    setAlpha(1f);
                    recycleShot();
                    blurIv.setImageDrawable(null);
                    blurIv.setAlpha(0f);
                    scrim.setAlpha(0f);
                    ind.invalidate();
                }
            });
            a.start();
        }

        private void recycleShot() {
            Bitmap b = shot;
            shot = null;
            if (b != null && !b.isRecycled()) {
                b.recycle();
            }
        }

        /** [comment]"[comment] + [comment]"（[comment]；[comment]） */
        private boolean blurHome() {
            return kind == KIND_HOME && style != STYLE_GLOW;
        }

        /** [comment] */
        private void apply() {
            final boolean show = visible && !hideForShot;
            if (!show || !blurHome()) {
                // [comment]、[comment]，[comment]；[comment]
                blurIv.setAlpha(0f);
                scrim.setAlpha(0f);
            }
            if (show && blurHome()) {
                Bitmap b = shot;
                final boolean hasShot = b != null && !b.isRecycled();
                blurIv.setAlpha(hasShot ? Math.min(1f, p / ALPHA_AT) : 0f);
                scrim.setAlpha((hasShot ? SCRIM : SCRIM_NO_SHOT) * p);
                if (hasShot) {
                    final float rad = dpPx(BLUR_MIN_DP)
                            + (dpPx(BLUR_MAX_DP) - dpPx(BLUR_MIN_DP)) * p;
                    if (Math.abs(rad - lastRadius) >= RAD_STEP) {
                        lastRadius = rad;
                        setBlur(rad);
                    }
                }
            }
            ind.invalidate();
        }

        /** [comment]。API 31 [comment] RenderEffect，[comment] */
        private void setBlur(float radius) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return;
            }
            try {
                blurIv.setRenderEffect(RenderEffect.createBlurEffect(
                        radius, radius, Shader.TileMode.CLAMP));
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed（Text）", t);
            }
        }

        // -------------------------------------------------------------- [comment]

        /**
         * [comment]。[comment] {@link CoverBlur} [comment]，[comment]Yes[comment]。
         *
         * <p>[comment] {@code hideForShot} [comment]、[comment] —— [comment]Yes[comment]，
         * [comment]（[comment]）。
         */
        private void grab() {
            AccessibilityService s = svc;
            if (s == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now < retryAfter || asked) {
                return;
            }
            asked = true;
            hideForShot = true;
            apply();
            try {
                s.takeScreenshot(Display.DEFAULT_DISPLAY, s.getMainExecutor(),
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
                                hideForShot = false;
                                if (b == null || blank(b)) {
                                    if (b != null) {
                                        b.recycle();
                                    }
                                    retryAfter = System.currentTimeMillis() + RETRY_MS;
                                    Log.w(TAG, "Text（Text），Text");
                                    apply();
                                    return;
                                }
                                if (!visible || kind != KIND_HOME) {
                                    // [comment] —— [comment]Yes[comment]，[comment]
                                    b.recycle();
                                    apply();
                                    return;
                                }
                                recycleShot();
                                shot = b;
                                blurIv.setImageBitmap(b);
                                Log.i(TAG, "Text：" + b.getWidth() + "x" + b.getHeight()
                                        + "（Text " + sw + "x" + sh + "）");
                                apply();
                            }

                            @Override
                            public void onFailure(int errorCode) {
                                hideForShot = false;
                                retryAfter = System.currentTimeMillis() + RETRY_MS;
                                Log.w(TAG, "TextFailed code=" + errorCode + "，Text");
                                apply();
                            }
                        });
            } catch (Throwable t) {
                hideForShot = false;
                retryAfter = System.currentTimeMillis() + RETRY_MS;
                Log.w(TAG, "takeScreenshot Text（TextYesText canTakeScreenshot）", t);
                apply();
            }
        }

        /**
         * [comment]Yes[comment]Yes[comment]。
         *
         * <p>[comment] 16x16 [comment]：[comment] {@code FLAG_SECURE} [comment]Yes[comment]，
         * [comment] 0。[comment]（[comment]）[comment]。[comment] CoverBlur [comment]。
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
                return max - min < 10;
            } catch (Throwable t) {
                return false;
            } finally {
                if (small != null && small != b) {
                    small.recycle();
                }
            }
        }
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment]（[comment] ImageView [comment]）。[comment]Yes[comment] */
    private static final class IndView extends View {

        private final AnimView owner;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        IndView(Context c, AnimView o) {
            super(c);
            owner = o;
        }

        @Override
        protected void onDraw(Canvas c) {
            AnimView a = owner;
            if (!a.visible || a.hideForShot || a.p <= 0.01f) {
                return;
            }
            if (a.kind == KIND_NONE) {
                return;
            }
            if (a.style == STYLE_GLOW) {
                // [comment]（[comment]）
                drawGlow(c, a);
            } else if (a.kind != KIND_HOME) {
                // [comment]，[comment]
                drawNative(c, a);
            }
        }

        /** [comment]：[comment] + [comment] */
        private void drawNative(Canvas c, AnimView a) {
            final boolean left = a.kind == KIND_BACK_L;
            final float r = dpPx(NATIVE_R_DP);
            final float inset = dpPx(NATIVE_INSET_DP) + a.p * dpPx(NATIVE_PUSH_DP);
            final float cx = left ? inset + r : sw - inset - r;
            final float cy = sh / 2f;

            // [comment]，[comment]
            paint.reset();
            paint.setAntiAlias(true);
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb((int) (255f * (0.34f + 0.46f * a.p)), 0, 0, 0));
            c.drawCircle(cx, cy, r, paint);

            // [comment]<b>[comment]</b>：[comment]、[comment]（[comment]Yes[comment]）
            final float dir = left ? 1f : -1f;
            final float arm = dpPx(NATIVE_ARM_DP) + dpPx(3f) * a.p;
            final float wing = dpPx(NATIVE_WING_DP) + dpPx(2f) * a.p;
            final float tipX = cx + dir * arm;
            final float backX = cx - dir * arm;

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.6f, dpPx(2.2f)));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(Color.argb((int) (255f * (0.60f + 0.40f * a.p)), 255, 255, 255));
            c.drawLine(backX, cy, tipX, cy, paint);
            c.drawLine(tipX, cy, tipX - dir * wing, cy - wing, paint);
            c.drawLine(tipX, cy, tipX - dir * wing, cy + wing, paint);
        }

        /**
         * [comment]：[comment]<b>[comment]</b>。
         *
         * <p>[comment] {@link #halo} [comment]：[comment]、[comment]，
         * [comment]Yes[comment]Yes"[comment]、[comment] GLOW_DEPTH_DP [comment]"。
         * [comment]（[comment]），[comment] sin [comment]
         * —— [comment]Yes"[comment]、[comment]"[comment]，
         * [comment] MyGesture [comment] {@code left_light.png} [comment]。
         *
         * <p>[comment]Yes<b>[comment]</b>[comment]：[comment] i [comment] {@code [comment] + i} [comment]，
         * [comment] —— [comment]Yes"[comment] RGB [comment]"[comment]。
         *
         * <p><b>[comment]</b>，[comment]Yes[comment]：[comment]Yes[comment]（[comment] = [comment]、
         * [comment] = [comment]），[comment]Yes[comment]（[comment] = [comment]、[comment] = [comment]）。{@link #halo} [comment]Yes
         * "X [comment] / Y [comment]"[comment]，[comment]、[comment] {@code y = sh}
         * [comment] —— [comment]Yes[comment]、[comment] {@link #GLOW_DEPTH_DP} [comment]。
         */
        private void drawGlow(Canvas c, AnimView a) {
            final boolean bottom = a.kind == KIND_HOME;
            // [comment]（[comment] = [comment]，[comment] = [comment]）
            final float edgeLen = bottom ? sw : sh;
            final float depth = dpPx(GLOW_DEPTH_DP) * (0.55f + 0.45f * a.p);
            final float span = edgeLen * GLOW_SPAN;
            final float r = edgeLen * GLOW_BLOB_R * (0.5f + 0.5f * a.p);
            // [comment]，[comment]（[comment]）
            final float half = span * 0.5f;
            final float mid = clamp(bottom ? fingerX : fingerY, half, edgeLen - half);
            final float from = mid - half;
            // [comment]：[comment] x=0、[comment] x=sw、[comment] y=sh
            final float edge = bottom ? sh : (a.kind == KIND_BACK_L ? 0f : sw);
            for (int i = 0; i < GLOW_BLOBS; i++) {
                final float t = (i + 0.5f) / GLOW_BLOBS;
                // [comment]：[comment] 0、[comment]（[comment]）
                final float w = (float) Math.sin(Math.PI * t);
                final int hue = TONES[(a.toneIdx + i * GLOW_HUE_STEP) % TONES.length];
                final float u = from + span * t;
                final float alpha = a.p * w * GLOW_GAIN;
                if (bottom) {
                    halo(c, u, edge, r, depth, hue, alpha);
                } else {
                    halo(c, edge, u, r, depth, hue, alpha);
                }
            }
        }

        /** [comment]：[comment] {@code (x, y)}，[comment] {@code reach / halfLen} [comment] */
        private void halo(Canvas c, float x, float y, float halfLen, float reach,
                          int tone, float alpha) {
            if (halfLen < 2f || reach < 1f || alpha <= 0.01f) {
                return;
            }
            final int r = Color.red(tone);
            final int g = Color.green(tone);
            final int b = Color.blue(tone);
            final int core = Color.argb((int) (255f * 0.80f * alpha), r, g, b);
            final int mid = Color.argb((int) (255f * 0.28f * alpha), r, g, b);
            final int out = Color.argb(0, r, g, b);

            paint.reset();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(x, y, halfLen,
                    new int[]{core, mid, out}, new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP));
            final int save = c.save();
            c.scale(1f, reach / halfLen, x, y);
            c.drawCircle(x, y, halfLen, paint);
            c.restoreToCount(save);
            paint.setShader(null);
        }
    }
}
