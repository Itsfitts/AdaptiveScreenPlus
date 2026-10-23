package com.wb.extrotator;

import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

/**
 * [comment] —— <b>[comment] + [comment]「[comment]」[comment]</b>。
 *
 * <p>[comment]（[comment]Yes[comment]）：
 * <ol>
 *   <li><b>[comment] UI [comment]</b>：{@code input} [comment]Yes[comment]，[comment] 47ms，[comment]
 *       [comment] UI [comment] —— [comment]、[comment]；</li>
 *   <li><b>[comment]</b>：None[comment]（{@link A11yInject} [comment] willContinue [comment]）[comment]
 *       [comment]Yes[comment]，[comment] —— [comment]。</li>
 * </ol>
 * [comment]：[comment]（UI [comment]）[comment]，[comment]；[comment]，
 * <b>[comment]</b>。[comment]Yes[comment]<b>[comment]</b>[comment]，[comment]Yes[comment] —— [comment]Yes[comment]
 * [comment]。
 *
 * <p><b>[comment]</b>：
 * <pre>
 *   ① Shizuku [comment]（{@link TouchInject}）—— [comment] Binder [comment]，[comment]，[comment]/[comment]
 *   ② input [comment]（{@link ExtScreen#motionDown} [comment]）—— [comment] 47ms，[comment]
 * </pre>
 * [comment]（[comment] Shizuku / [comment]Failed）[comment] ②，[comment]Yes[comment]。
 *
 * <p><b>[comment]</b>：[comment]Yes[comment] —— [comment] → [comment]
 * [comment]；[comment]「[comment]」（[comment] vs [comment]）[comment]「[comment]」
 * （[comment]），[comment]。[comment]
 * [comment]。
 *
 * <p>⚠ <b>[comment]（tap / hold / swipe）[comment]</b>：[comment]Yes[comment]，[comment]，
 * [comment]「[comment]」，[comment]<b>[comment]</b>[comment]（[comment]），
 * [comment]。
 */
public final class TouchInjector {

    /** [comment]"[comment]、[comment]" */
    public interface Status {
        /**
         * @param direct true = Shizuku [comment]；false = [comment] input [comment]
         * @param detail [comment]：[comment]、[comment]
         */
        void onPath(boolean direct, String detail);
    }

    private static final int WANT_NONE = 0;
    private static final int WANT_PRESS = 1;
    private static final int WANT_RELEASE = 2;
    /** [comment]：[comment]，[comment]（[comment]"[comment]"Yes[comment]，[comment] abort） */
    private static final int WANT_ABORT = 3;

    private static final int CMD_NONE = 0;
    private static final int CMD_TAP = 1;
    private static final int CMD_HOLD = 2;
    private static final int CMD_SWIPE = 3;

    /**
     * [comment] input [comment]，[comment]。
     *
     * <p>{@code input} [comment] 47ms：[comment]Yes 21 [comment]/[comment]，[comment]；
     * [comment] 3 [comment] ≈ 40 [comment]/[comment]，[comment]。[comment] —— [comment]，
     * [comment]。
     */
    private static final int TWEEN_MAX = 3;

    /** [comment]，[comment]（[comment]） */
    private static final long TAP_MS = 60L;

    /** [comment] swipe [comment]（[comment]Yes[comment]，[comment]） */
    private static final int SWIPE_STEPS = 12;

    private final String tag;
    /** [comment] id（[comment]） */
    private final int displayId;
    /**
     * [comment]。
     *
     * <p>[comment]：[comment]Yes"[comment]（500ms）[comment] DOWN [comment]"，
     * [comment] DOWN [comment] —— [comment]
     * [comment]，No[comment] 0.6 [comment]。
     * [comment]Yes"[comment] DOWN"，[comment]，[comment] 0。
     */
    private final long holdPadMs;

    private volatile Status status;

    /* ---------------- [comment]（UI [comment]、[comment]） ---------------- */
    private final Object lock = new Object();
    /** [comment]：[comment] +1 */
    private int seq;
    /** [comment] */
    private int x, y;
    /** [comment]：1 = [comment]，2 = [comment]，0 = [comment]Yes[comment] */
    private int want;
    /** [comment] */
    private int cmd = CMD_NONE;
    private int cx0, cy0, cx1, cy1, cms, cmin;
    /** [comment]（[comment]"[comment]"[comment]） */
    private boolean pressed;
    private boolean die;

    private Thread thread;
    /** [comment] —— MOVE / UP [comment] DOWN [comment]，[comment]Yes[comment] */
    private long downAt;
    /** [comment] */
    private volatile boolean direct;
    /** [comment] / [comment]（[comment]"[comment]"[comment]） */
    private int sent, dropped;
    private long tickAt;

    public TouchInjector(String tag, int displayId, long holdPadMs) {
        this.tag = tag;
        this.displayId = displayId;
        this.holdPadMs = Math.max(0L, holdPadMs);
    }

    public void setStatus(Status s) {
        this.status = s;
    }

    /** [comment]（true = [comment]） */
    public boolean direct() {
        return direct;
    }

    /** [comment]，[comment] —— [comment] */
    public String why() {
        return TouchInject.why();
    }

    // ------------------------------------------------------------------ [comment]

    public void start() {
        if (thread != null) {
            return;
        }
        thread = new Thread(this::loop, tag);
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        synchronized (lock) {
            die = true;
            lock.notifyAll();
        }
        Thread t = thread;
        thread = null;
        if (t != null) {
            try {
                t.join(600L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------------------------------------------------------------------ [comment]：[comment] / [comment] / [comment]

    /** [comment]（UI [comment]，[comment]） */
    public void press(int px, int py) {
        synchronized (lock) {
            x = px;
            y = py;
            seq++;
            want = WANT_PRESS;
            lock.notifyAll();
        }
    }

    /** [comment]（UI [comment]，[comment] —— [comment]，[comment]） */
    public void move(int px, int py) {
        synchronized (lock) {
            x = px;
            y = py;
            seq++;
            lock.notifyAll();
        }
    }

    /** [comment]（[comment]，[comment]） */
    public void release(int px, int py) {
        synchronized (lock) {
            x = px;
            y = py;
            seq++;
            want = WANT_RELEASE;
            lock.notifyAll();
        }
    }

    /**
     * [comment]（[comment] / [comment]）—— [comment]，[comment]。
     *
     * <p>⚠ [comment] {@link #release} [comment]：release [comment]"[comment]"[comment]
     * （[comment]），[comment]Yes[comment]
     * [comment] —— [comment]、[comment]，[comment]，[comment]。
     */
    public void abort() {
        synchronized (lock) {
            want = WANT_ABORT;
            lock.notifyAll();
        }
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]（[comment] DOWN + [comment] + UP）。
     *
     * <p>⚠ [comment]：DOWN [comment] UP [comment]，[comment]Yes[comment]
     * （[comment]，[comment]）。
     */
    public void tap(int px, int py) {
        oneShot(CMD_TAP, px, py, px, py, (int) TAP_MS, 0);
    }

    /** [comment] ms [comment] */
    public void hold(int px, int py, int ms) {
        oneShot(CMD_HOLD, px, py, px, py, Math.max(200, ms), 0);
    }

    /** [comment] (x0,y0) [comment] (x1,y1)，[comment] ms —— [comment] / [comment] */
    public void swipe(int x0, int y0, int x1, int y1, int ms) {
        oneShot(CMD_SWIPE, x0, y0, x1, y1, Math.max(60, ms), 0);
    }

    private void oneShot(int what, int a0, int b0, int a1, int b1, int ms, int min) {
        synchronized (lock) {
            // [comment]：[comment]
            cmd = what;
            cx0 = a0;
            cy0 = b0;
            cx1 = a1;
            cy1 = b1;
            cms = ms;
            cmin = min;
            lock.notifyAll();
        }
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]：[comment]Yes"[comment] → [comment] → [comment]"。
     *
     * <p>[comment] {@link #x} / {@link #y} [comment]。[comment]
     * [comment]？[comment]<b>[comment]</b>。
     */
    private void loop() {
        int lastDoneSeq = 0;
        int lx = 0, ly = 0;
        while (true) {
            int s, px, py, w, c, a0, b0, a1, b1, ms, min;
            boolean busy;
            synchronized (lock) {
                /*
                 * [comment]：[comment]Update[comment]（seq [comment]）、[comment]（want）、[comment]。
                 * ⚠ [comment] "|| pressed"：[comment]
                 * （[comment]），[comment]，
                 * [comment] —— [comment]，[comment]。
                 */
                while (!die && seq == lastDoneSeq && want == WANT_NONE
                        && (cmd == CMD_NONE || pressed)) {
                    try {
                        lock.wait(500L);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (die) {
                    break;
                }
                s = seq;
                px = x;
                py = y;
                w = want;
                want = WANT_NONE;
                c = cmd;
                a0 = cx0;
                b0 = cy0;
                a1 = cx1;
                b1 = cy1;
                ms = cms;
                min = cmin;
                busy = pressed;
                /*
                 * [comment]"[comment]"[comment]：
                 * [comment]、[comment]，[comment] ——
                 * [comment]。
                 */
                if (c != CMD_NONE && w != WANT_PRESS && !busy) {
                    cmd = CMD_NONE;
                }
            }

            if (w == WANT_PRESS) {
                down(px, py);
                synchronized (lock) {
                    pressed = true;
                }
                lx = px;
                ly = py;
                lastDoneSeq = s;
                continue;
            }

            if (!busy) {
                if (w == WANT_RELEASE) {
                    /*
                     * [comment] —— [comment]，UI [comment]
                     * "[comment]"[comment]"[comment]"，[comment]。
                     * [comment]Yes"[comment]"，[comment]。
                     */
                    quickTap(px, py);
                }
                if (c != CMD_NONE) {
                    runCmd(c, a0, b0, a1, b1, ms, min);
                }
                lastDoneSeq = s;
                continue;
            }

            if (s != lastDoneSeq) {
                if (s - lastDoneSeq > 1) {
                    // [comment]"[comment]"[comment] N [comment] —— N [comment]。
                    // [comment]Yes"[comment]"[comment]：[comment]Yes[comment]，[comment]Yes[comment]。
                    dropped += s - lastDoneSeq - 1;
                }
                move(lx, ly, px, py);
                lx = px;
                ly = py;
                lastDoneSeq = s;
            }

            if (w == WANT_RELEASE || w == WANT_ABORT) {
                if (w == WANT_RELEASE) {
                    long held = SystemClock.uptimeMillis() - downAt;
                    if (held < holdPadMs) {
                        // [comment]"[comment]"[comment]（[comment] holdPadMs [comment]）
                        SystemClock.sleep(holdPadMs - held);
                    }
                    Log.d(tag, "Text · Text "
                            + (SystemClock.uptimeMillis() - downAt) + "ms");
                }
                up(px, py);
                synchronized (lock) {
                    pressed = false;
                }
            }
        }
        // [comment] —— [comment]"[comment]"，[comment]
        if (pressed) {
            synchronized (lock) {
                pressed = false;
            }
            up(x, y);
        }
    }

    /** [comment]：DOWN [comment] UP [comment] */
    private void quickTap(int px, int py) {
        long t0 = SystemClock.uptimeMillis();
        if (TouchInject.send(displayId, MotionEvent.ACTION_DOWN, px, py, t0)) {
            direct = true;
            SystemClock.sleep(TAP_MS);
            TouchInject.send(displayId, MotionEvent.ACTION_UP, px, py, t0);
            return;
        }
        direct = false;
        ExtScreen.tap(displayId, px, py);
    }

    /** [comment]（[comment] —— [comment]Update） */
    private void runCmd(int what, int a0, int b0, int a1, int b1, int ms, int min) {
        if (what == CMD_TAP) {
            long t0 = SystemClock.uptimeMillis();
            if (TouchInject.send(displayId, MotionEvent.ACTION_DOWN, a0, b0, t0)) {
                direct = true;
                SystemClock.sleep(Math.max(TAP_MS, ms));
                TouchInject.send(displayId, MotionEvent.ACTION_UP, a1, b1, t0);
                return;
            }
            direct = false;
            ExtScreen.tap(displayId, a0, b0);
            return;
        }
        if (what == CMD_HOLD) {
            long t0 = SystemClock.uptimeMillis();
            if (TouchInject.send(displayId, MotionEvent.ACTION_DOWN, a0, b0, t0)) {
                direct = true;
                SystemClock.sleep(ms);
                TouchInject.send(displayId, MotionEvent.ACTION_UP, a1, b1, t0);
                return;
            }
            direct = false;
            ExtScreen.hold(displayId, a0, b0, ms);
            return;
        }
        // CMD_SWIPE：[comment] —— [comment]，
        // [comment] input [comment]（[comment]，[comment]Yes"[comment]"[comment]Yes"[comment]"）。
        long t0 = SystemClock.uptimeMillis();
        if (!TouchInject.send(displayId, MotionEvent.ACTION_DOWN, a0, b0, t0)) {
            direct = false;
            ExtScreen.swipe(displayId, a0, b0, a1, b1, ms);
            return;
        }
        direct = true;
        int steps = Math.max(2, Math.min(SWIPE_STEPS, ms / 12));
        long stepMs = Math.max(1L, ms / steps);
        for (int i = 1; i <= steps; i++) {
            SystemClock.sleep(stepMs);
            float f = (float) i / steps;
            TouchInject.send(displayId, MotionEvent.ACTION_MOVE,
                    Math.round(a0 + (a1 - a0) * f), Math.round(b0 + (b1 - b0) * f), t0);
        }
        TouchInject.send(displayId, MotionEvent.ACTION_UP, a1, b1, t0);
    }

    // ------------------------------------------------------------------ [comment]

    private void down(int px, int py) {
        downAt = SystemClock.uptimeMillis();
        rateTick();
        if (TouchInject.send(displayId, MotionEvent.ACTION_DOWN, px, py, downAt)) {
            direct = true;
            return;
        }
        direct = false;
        ExtScreen.motionDown(displayId, px, py);
    }

    /**
     * [comment]。
     *
     * <p>[comment]：[comment]，[comment]（[comment]，
     * [comment]）；{@code input} [comment] 47ms [comment]，[comment]
     * 1~2 [comment]<b>[comment]</b>[comment] —— [comment]（[comment] {@link #TWEEN_MAX}）。
     */
    private void move(int x0, int y0, int x1, int y1) {
        rateTick();
        if (TouchInject.send(displayId, MotionEvent.ACTION_MOVE, x1, y1, downAt)) {
            direct = true;
            return;
        }
        direct = false;
        int k = tweenSteps(x0, y0, x1, y1);
        if (k <= 1) {
            ExtScreen.motionMove(displayId, x1, y1);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= k; i++) {
            if (i > 1) {
                sb.append(" & ");
            }
            sb.append("input -d ").append(displayId).append(" motionevent MOVE ")
                    .append(x0 + Math.round((x1 - x0) * (float) i / k)).append(' ')
                    .append(y0 + Math.round((y1 - y0) * (float) i / k));
        }
        ShellRunner.run(sb.append(" & wait").toString(), 3);
    }

    private void up(int px, int py) {
        rateTick();
        if (TouchInject.send(displayId, MotionEvent.ACTION_UP, px, py, downAt)) {
            return;
        }
        ExtScreen.motionUp(displayId, px, py);
    }

    /** [comment]（[comment]，[comment] 47ms） */
    private static int tweenSteps(int x0, int y0, int x1, int y1) {
        double d = Math.hypot(x1 - x0, y1 - y0);
        if (d < 16.0) {
            return 1;
        }
        return d < 56.0 ? 2 : TWEEN_MAX;
    }

    /**
     * [comment] —— [comment]Yes[comment]"[comment]"[comment]。
     *
     * <p>[comment] {@code input} [comment] 47ms，[comment] 21 [comment]/[comment]；[comment]Yes[comment]/[comment]。
     * [comment]Yes"[comment]"——[comment]Yes[comment]，[comment]Yes[comment]。
     */
    private void rateTick() {
        sent++;
        long now = SystemClock.uptimeMillis();
        if (tickAt == 0L) {
            tickAt = now;
            return;
        }
        if (now - tickAt < 1000L) {
            return;
        }
        final boolean d = direct;
        final int n = sent, drop = dropped;
        sent = 0;
        dropped = 0;
        tickAt = now;
        if (n > 1) {
            Log.i(tag, "Text " + n + " Text/Text · " + (d ? "Text" : "Text")
                    + " · Text " + drop + " Text · " + TouchInject.why());
        }
        Status st = status;
        if (st != null) {
            st.onPath(d, n + "/Text · Text " + drop);
        }
    }
}
