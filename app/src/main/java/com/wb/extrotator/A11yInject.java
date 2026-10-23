package com.wb.extrotator;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

/**
 * [comment]<b>[comment]</b>[comment] —— [comment]None[comment] {@code dispatchGesture}。
 *
 * <p><b>[comment] {@code input}</b>：v3.6 [comment] / [comment]
 * {@code ShellRunner.run("input -d N ...")}，[comment] —— [comment]
 * （sh + input [comment] JVM），[comment] 40~65ms；[comment]Yes {@code input} [comment]Yes「[comment]」[comment]，
 * [comment]<b>[comment]</b>[comment] InputDispatcher [comment]，[comment] 5~8 [comment]，[comment]
 * [comment]，[comment]；[comment] MOVE，[comment] {@code input} [comment]。
 *
 * <p>{@code dispatchGesture} [comment]：<b>[comment]</b>（[comment] binder [comment]）、
 * <b>[comment]</b>、[comment]<b>[comment]</b> —— [comment]，[comment]Yes[comment]
 * [comment]，[comment]Yes「[comment]」[comment]Yes「[comment]」。
 *
 * <p><b>[comment]</b>：[comment] {@link GestureDescription.StrokeDescription#continueStroke}
 * [comment]<b>[comment]</b> —— [comment] {@code willContinue=true} [comment]
 * （[comment]Yes「[comment]」[comment]），[comment]，[comment]
 * {@code willContinue=false} [comment]。
 *
 * <p>⚠⚠ <b>[comment]「[comment]」[comment]</b>：[comment] dispatch [comment] callback，
 * [comment] {@code onCancelled} Yes[comment]（system_server [comment]），[comment] {@code dragStart}
 * [comment] —— [comment]Yes[comment]Cancel[comment]。[comment]：[comment] 8ms [comment]Cancel，
 * [comment] 700ms [comment]、[comment]，[comment]Yes「[comment] 2~3 [comment]」。
 * [comment]（[comment] {@link #cb}）：<b>[comment]（{@code gen}）</b>——[comment] {@code dragStart} [comment]，
 * [comment]；<b>[comment]（{@code seq}）</b>——[comment]，[comment]<b>[comment]</b>
 * [comment]Cancel[comment]（[comment]Cancel，Yes[comment]）。
 *
 * <p>⚠ <b>[comment]</b>：{@code setDisplayId} [comment] API 30+（[comment] false [comment]
 * {@code input}）；「[comment]」[comment] —— [comment]（[comment]），
 * [comment] {@link #KEEP_MS} [comment]Yes[comment]（[comment]「[comment]」[comment]）。
 */
public final class A11yInject {

    private static final String TAG = "ExtA11yInject";

    /** [comment] —— [comment]，60ms [comment] */
    private static final long TAP_MS = 60L;
    /** [comment] */
    private static final long SEG_MS = 150L;
    /** [comment]（[comment]）[comment] */
    private static final long END_MS = 40L;
    /** [comment]：[comment]，[comment]"[comment]"[comment] */
    private static final long KEEP_MS = 90L;
    /**
     * [comment]（[comment]）。
     *
     * <p>[comment] MOVE [comment] Hz，[comment] dispatchGesture [comment] ——
     * [comment] onCancelled（[comment]「[comment]Cancel」）。
     *
     * <p>[comment] 24ms（[comment] 42 [comment]/[comment]）：[comment]；[comment]，
     * [comment]。
     */
    private static final long STEP_MS = 24L;
    /** [comment] */
    private static final long MIN_SEG_MS = 16L;
    private static final long SEG_MAX_MS = 200L;
    /** [comment] —— [comment] 60s，[comment] */
    private static final long MAX_CHAIN_MS = 55_000L;
    /** [comment] */
    private static final int PATH_MAX_PTS = 8;

    /** None[comment]；[comment] = [comment] */
    private static volatile AccessibilityService svc;

    /** [comment]（null = [comment]） */
    private static volatile GestureDescription.StrokeDescription chain;
    private static volatile int chainDisplay = -1;
    /** [comment]<b>[comment]</b>[comment]（[comment]） */
    private static volatile float chainX, chainY;
    /** [comment]，[comment]"[comment]" */
    private static volatile long chainAt;
    /** [comment] dispatchGesture [comment] —— [comment]"[comment]=[comment]"[comment] */
    private static volatile long lastFireAt;

    /**
     * [comment]，[comment]<b>[comment]</b>[comment]（[comment]）。
     *
     * <p>v3.18 [comment]。[comment]"[comment]"，[comment] —— [comment]Yes[comment]，
     * [comment]。[comment] Path [comment]，
     * [comment]，[comment]。
     */
    private static final float[] pathX = new float[PATH_MAX_PTS];
    private static final float[] pathY = new float[PATH_MAX_PTS];
    private static volatile int pathN;

    /**
     * [comment]（[comment]Cancel[comment]）。
     *
     * <p>⚠ [comment]<b>[comment]</b>：v3.7 [comment] onCancelled [comment]，
     * [comment]Yes[comment] continueStroke ⇒ [comment]Yes[comment]，
     * [comment]Yes"[comment] / [comment]"。[comment] dead，
     * {@code dragTo} [comment] false，[comment] {@code input} [comment]。
     */
    private static volatile boolean dead;

    /** [comment] —— [comment] {@link #dragStart} [comment]。[comment]。 */
    private static volatile int chainGen;
    /** [comment] —— [comment]"[comment]"[comment]Cancel[comment]。 */
    private static volatile int segSeq;

    private static final Handler H = new Handler(Looper.getMainLooper());

    private A11yInject() {
    }

    // ------------------------------------------------------------------ [comment]

    /** None[comment] */
    static void attach(AccessibilityService s) {
        svc = s;
        Log.i(TAG, "Text（API " + Build.VERSION.SDK_INT + "）");
    }

    /**
     * [comment] —— [comment]（{@link CoverSidebar} / {@link CoverRecentsGesture}）[comment]。
     *
     * <p>[comment]<b>[comment]</b>：[comment]，[comment] {@link #detach} [comment]。
     * [comment]、[comment] —— [comment]。
     */
    static AccessibilityService service() {
        return svc;
    }

    /** [comment] / [comment] —— [comment]，[comment] */
    static void detach(AccessibilityService s) {
        if (svc != s) {
            return;
        }
        svc = null;
        abortChain();
        Log.i(TAG, "Text（Text）");
    }

    /** [comment] */
    public static boolean ready() {
        return svc != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment] */
    public static boolean tap(int displayId, int x, int y) {
        Path p = new Path();
        p.moveTo(x, y);
        return fireOnce(displayId, new GestureDescription.StrokeDescription(p, 0L, TAP_MS, false));
    }

    /**
     * [comment] ms [comment]。
     *
     * <p>[comment]"[comment] - [comment] - [comment]"[comment]：[comment] ms [comment]，
     * [comment]Yes"[comment] ms [comment]"，[comment] / [comment] / [comment]。
     */
    public static boolean longPress(int displayId, int x, int y, long ms) {
        Path p = new Path();
        p.moveTo(x, y);
        return fireOnce(displayId, new GestureDescription.StrokeDescription(
                p, 0L, Math.max(200L, ms), false));
    }

    /** [comment] (x1,y1) [comment] (x2,y2)，[comment] ms —— [comment] / [comment] */
    public static boolean swipe(int displayId, int x1, int y1, int x2, int y2, long ms) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        return fireOnce(displayId, new GestureDescription.StrokeDescription(
                p, 0L, Math.max(60L, ms), false));
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]（[comment]）—— [comment] {@code willContinue} [comment]，
     * [comment]Yes"[comment]"[comment]。
     *
     * @return false = [comment]，[comment] {@code input} [comment]
     */
    public static boolean dragStart(int displayId, int x, int y) {
        if (!ready()) {
            return false;
        }
        abortChain();

        // Enable[comment]：[comment]，[comment]Cancel[comment]（[comment] cb [comment]）
        final int gen = ++chainGen;
        segSeq = 0;

        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.StrokeDescription st =
                new GestureDescription.StrokeDescription(p, 0L, SEG_MS, true);
        if (!fire(displayId, st, gen)) {
            return false;
        }
        chain = st;
        chainDisplay = displayId;
        chainX = x;
        chainY = y;
        chainAt = SystemClock.uptimeMillis();
        lastFireAt = chainAt;
        pathN = 0;
        dead = false;
        H.removeCallbacks(keepAlive);
        H.postDelayed(keepAlive, KEEP_MS);
        return true;
    }

    /** [comment] (x,y) —— [comment]，[comment] */
    public static boolean dragTo(int displayId, int x, int y) {
        GestureDescription.StrokeDescription prev = chain;
        if (prev == null || chainDisplay != displayId) {
            return false;
        }
        if (prev.getStartTime() + prev.getDuration() > MAX_CHAIN_MS) {
            Log.w(TAG, "Text " + MAX_CHAIN_MS + "ms，Text");
            dragEnd(displayId, x, y);
            return false;
        }
        if (dead) {
            // [comment]Cancel[comment]，[comment]Yes[comment] —— [comment] input [comment]
            return false;
        }
        // [comment]（[comment]）
        if (x == chainX && y == chainY && pathN == 0) {
            return true;
        }
        // [comment]（[comment]，[comment]）
        if (pathN > 0 && pathX[pathN - 1] == x && pathY[pathN - 1] == y) {
            // [comment]，[comment]
        } else if (pathN >= PATH_MAX_PTS) {
            // [comment]：[comment]，[comment]
            return flushSegment(displayId);
        } else {
            pathX[pathN] = x;
            pathY[pathN] = y;
            pathN++;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastFireAt >= STEP_MS) {
            return flushSegment(displayId);
        }
        return true;
    }

    /**
     * [comment]。
     *
     * <p>⚠⚠ [comment]：<b>[comment]「[comment]」，[comment]</b>。[comment]Yes[comment]
     * [comment] —— [comment] 16ms [comment] 150ms [comment]，[comment] 10 [comment]，
     * [comment]Yes「[comment]」。[comment]，[comment]：[comment]，[comment]。
     *
     * @return false = [comment]，[comment] {@code input}
     */
    private static boolean flushSegment(int displayId) {
        GestureDescription.StrokeDescription prev = chain;
        if (prev == null || chainDisplay != displayId || dead) {
            return false;
        }
        long now = SystemClock.uptimeMillis();
        long dur = now - lastFireAt;
        if (dur < MIN_SEG_MS) {
            dur = MIN_SEG_MS;
        } else if (dur > SEG_MAX_MS) {
            dur = SEG_MAX_MS;
        }

        Path p = new Path();
        p.moveTo(chainX, chainY);
        for (int i = 0; i < pathN; i++) {
            p.lineTo(pathX[i], pathY[i]);
        }
        final float nx = pathN > 0 ? pathX[pathN - 1] : chainX;
        final float ny = pathN > 0 ? pathY[pathN - 1] : chainY;

        GestureDescription.StrokeDescription st = prev.continueStroke(
                p, prev.getStartTime() + prev.getDuration(), dur, true);
        if (!fire(displayId, st, chainGen)) {
            abortChain();
            return false;
        }
        chain = st;
        chainX = nx;
        chainY = ny;
        chainAt = now;
        lastFireAt = now;
        pathN = 0;
        return true;
    }

    /** [comment] —— [comment]，[comment] */
    public static boolean dragEnd(int displayId, int x, int y) {
        GestureDescription.StrokeDescription prev = chain;
        H.removeCallbacks(keepAlive);
        final int gen = chainGen;
        chain = null;
        chainDisplay = -1;
        pathN = 0;
        if (prev == null || !ready()) {
            return false;
        }
        Path p = new Path();
        p.moveTo(chainX, chainY);
        if (x != chainX || y != chainY) {
            p.lineTo(x, y);
        }
        GestureDescription.StrokeDescription st = prev.continueStroke(
                p, prev.getStartTime() + prev.getDuration(), END_MS, false);
        return fire(displayId, st, gen);
    }

    /**
     * [comment] —— [comment]，[comment]"[comment]"[comment]。
     *
     * <p>[comment]，[comment] {@link #SEG_MS} [comment]，[comment]，
     * [comment]：[comment]。
     *
     * <p>[comment]"[comment]、[comment]"[comment]（[comment]）。
     */
    private static final Runnable keepAlive = new Runnable() {
        @Override
        public void run() {
            GestureDescription.StrokeDescription prev = chain;
            if (prev == null || !ready()) {
                return;
            }
            long now = SystemClock.uptimeMillis();
            if (pathN > 0 || now - chainAt >= KEEP_MS) {
                if (!flushSegment(chainDisplay)) {
                    // [comment]（[comment]Yes[comment]/[comment]Cancel）—— [comment]，[comment]
                    Log.w(TAG, "TextFailed，Text");
                    abortChain();
                    return;
                }
            }
            H.postDelayed(this, KEEP_MS);
        }
    };

    /** [comment]（[comment] / [comment] / [comment]）—— [comment] {@link ExtScreen#dragAbort()} */
    public static void abort() {
        abortChain();
    }

    private static void abortChain() {
        H.removeCallbacks(keepAlive);
        final GestureDescription.StrokeDescription prev = chain;
        final int disp = chainDisplay;
        final float x = chainX, y = chainY;
        chain = null;
        chainDisplay = -1;
        pathN = 0;
        /*
         * ⚠ [comment]：[comment] onCancelled，
         * [comment]，[comment]"[comment]"[comment] dragStart [comment]、[comment]
         * （v3.17 [comment] bug，[comment]）。
         */
        chainGen++;
        AccessibilityService s = svc;
        if (prev == null || s == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        // [comment]，[comment] —— [comment]"[comment]"
        Path p = new Path();
        p.moveTo(x, y);
        try {
            s.dispatchGesture(build(disp, prev.continueStroke(
                    p, prev.getStartTime() + prev.getDuration(), END_MS, false)), null, null);
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed: " + t);
        }
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment]（[comment] / [comment] / [comment]）—— [comment]，[comment] */
    private static boolean fireOnce(int displayId, GestureDescription.StrokeDescription st) {
        AccessibilityService s = svc;
        if (s == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false;
        }
        try {
            return s.dispatchGesture(build(displayId, st),
                    new AccessibilityService.GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription g) {
                            Log.d(TAG, "Text");
                        }

                        @Override
                        public void onCancelled(GestureDescription g) {
                            Log.w(TAG, "TextCancel");
                        }
                    }, null);
        } catch (Throwable t) {
            Log.w(TAG, "dispatchGesture Text: " + t);
            return false;
        }
    }

    /**
     * [comment]<b>[comment]</b>[comment]，[comment]"[comment]Yes[comment]、[comment]"。
     *
     * <p>⚠ [comment] new [comment] —— [comment]。[comment]Yes[comment] v3.17 [comment]：
     * [comment] {@code gen}/{@code seq}，[comment]"[comment]CancelYes[comment]"。
     */
    private static boolean fire(int displayId, GestureDescription.StrokeDescription st, int gen) {
        AccessibilityService s = svc;
        if (s == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false;
        }
        final int mySeq = ++segSeq;
        try {
            boolean ok = s.dispatchGesture(build(displayId, st), cb(gen, mySeq), null);
            /*
             * ⚠ [comment]Yes<b>[comment]</b> —— [comment]，[comment] onCompleted [comment]。
             *
             * [comment]：[comment] 1.46s [comment]，[comment] onCompleted [comment] 7 [comment]、
             * [comment] 500ms —— [comment]Yes[comment]、[comment]，[comment]
             * 「[comment]」[comment]「[comment]」，[comment] 410ms。
             */
            Log.d(TAG, "Text #" + gen + "." + mySeq
                    + " start=" + st.getStartTime() + " dur=" + st.getDuration()
                    + (ok ? "" : "  ← dispatchGesture Text false"));
            return ok;
        } catch (Throwable t) {
            Log.w(TAG, "dispatchGesture Text: " + t);
            return false;
        }
    }

    /**
     * [comment] —— <b>[comment]「[comment]」[comment] bug [comment]</b>。
     *
     * <p>[comment]Yes[comment] callback，{@code onCancelled} [comment] {@code chain != null} [comment] dead。
     * [comment]Cancel[comment]Yes[comment]、[comment]，[comment] {@code dragStart} [comment] ⇒
     * <b>[comment]</b>，[comment]。
     *
     * <p>[comment]：{@code gen != chainGen} ⇒ Yes[comment]（[comment]）[comment]，[comment]None[comment]；
     * {@code seq != segSeq} ⇒ Yes[comment]<b>[comment]</b>[comment]，[comment]。
     * [comment]「[comment]」[comment]Cancel，[comment]Yes[comment]。
     */
    private static AccessibilityService.GestureResultCallback cb(final int gen, final int seq) {
        return new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription g) {
                Log.d(TAG, "Text #" + gen + "." + seq);
            }

            @Override
            public void onCancelled(GestureDescription g) {
                if (gen != chainGen) {
                    Log.d(TAG, "TextCancelText（#" + gen + "." + seq + "）—— Text");
                    return;
                }
                if (seq != segSeq) {
                    Log.d(TAG, "TextCancelText（#" + gen + "." + seq + "）—— Text，Text");
                    return;
                }
                if (chain != null) {
                    dead = true;
                    chain = null;
                    chainDisplay = -1;
                    pathN = 0;
                    H.removeCallbacks(keepAlive);
                    Log.w(TAG, "TextCancel（#" + gen + "." + seq
                            + "）—— Text（Text / Text / Text）");
                } else {
                    Log.w(TAG, "TextCancel（#" + gen + "." + seq
                            + "，Text）");
                }
            }
        };
    }

    private static GestureDescription build(int displayId, GestureDescription.StrokeDescription st) {
        GestureDescription.Builder b = new GestureDescription.Builder().addStroke(st);
        b.setDisplayId(displayId);
        return b.build();
    }
}
