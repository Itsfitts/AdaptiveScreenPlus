package com.wb.extrotator;

import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;

import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.SystemServiceHelper;

/**
 * [comment]<b>[comment]</b>[comment] —— [comment] Binder [comment]，[comment]。
 *
 * <p>[comment] {@code input} [comment] JVM，[comment] 47ms（[comment] 21 [comment]/[comment]；
 * 4 [comment] 44 [comment]/[comment]），[comment] 120Hz / [comment] 60Hz，[comment]；
 * [comment] {@code IInputManager.injectInputEvent} [comment]/[comment]、[comment] 1ms [comment]。
 *
 * <p>[comment] {@code INJECT_EVENTS} [comment]（shell / [comment]），[comment]
 * {@link ShizukuBinderWrapper} [comment] Shizuku [comment] shell [comment] —— [comment] {@code adb shell input}
 * [comment]。
 *
 * <p>⚠ {@code IInputManager} [comment] {@code MotionEvent.setDisplayId} [comment]Yes @hide、[comment]，
 * [comment] {@link #ready()} [comment] false，[comment]<b>[comment]</b>[comment]
 * {@link ExtScreen#motionMove} [comment] —— [comment]。
 */
public final class TouchInject {

    private static final String TAG = "ExtTouch";

    /** IInputManager.INJECT_INPUT_EVENT_MODE_ASYNC：[comment]，[comment] */
    private static final int MODE_ASYNC = 0;

    /** [comment]（[comment]） */
    private static final int FAIL_LIMIT = 4;

    /** [comment]Failed[comment]（Shizuku [comment]Yes[comment]） */
    private static final long REPROBE_MS = 5000L;

    private static boolean probed;
    private static boolean ok;
    private static String why = "Text";
    /** [comment] —— [comment] */
    private static boolean blockedOnce;

    private static Object im;
    private static Method mInject;
    private static Method mSetDisplay;
    private static int fails;

    /** [comment] —— [comment]Failed[comment]（[comment] Shizuku [comment]） */
    private static long probedAt;

    private TouchInject() {
    }

    /** [comment] */
    public static synchronized boolean ready() {
        if (!ok && !blockedOnce
                && (!probed || SystemClock.uptimeMillis() - probedAt > REPROBE_MS)) {
            probe();
        }
        return ok;
    }

    /** [comment]，[comment] —— [comment]，[comment]"[comment]"[comment] */
    public static synchronized String why() {
        ready();
        return why;
    }

    /**
     * [comment]。⚠ <b>[comment]</b> —— [comment] Shizuku [comment] binder [comment] @hide [comment]，
     * [comment]：[comment]，[comment]，[comment]Yes"[comment]、[comment]、
     * [comment]"，[comment]（[comment]）。
     * [comment]，1.5 [comment] —— [comment] input [comment]，
     * <b>[comment]Allow"[comment]"</b>。
     */
    private static void probe() {
        probed = true;
        probedAt = SystemClock.uptimeMillis();

        if (blockedOnce) {
            ok = false;
            why = "Text，Text";
            Log.w(TAG, why);
            return;
        }

        // [comment]：[comment] Shizuku [comment]，
        // [comment]（[comment]，[comment] 1.5 [comment]）。
        if (!Shizuku.pingBinder()) {
            ok = false;
            why = "Shizuku Text";
            Log.i(TAG, "Text: " + why);
            return;
        }

        final String[] box = new String[1];
        Thread t = new Thread(() -> box[0] = tryProbe(), "extrot-touchinject-probe");
        t.setDaemon(true);
        t.start();
        try {
            t.join(1500L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (t.isAlive()) {
            blockedOnce = true;
            ok = false;
            why = "Text（1.5 Text）—— Text input Text";
        } else if (box[0] == null) {
            ok = true;
            why = "Shizuku Text";
        } else {
            ok = false;
            why = box[0];
        }
        Log.i(TAG, "Text: " + why);
    }

    /** [comment]。Success[comment] null，Failed[comment]。[comment]，[comment] {@link #probe()} [comment] */
    private static String tryProbe() {
        try {
            Log.i(TAG, "Text·1/3 Text input Text");
            IBinder raw = SystemServiceHelper.getSystemService("input");
            if (raw == null) {
                return "Text input Text";
            }
            Log.i(TAG, "Text·2/3 IInputManager$Stub.asInterface");
            IBinder wrapped = new ShizukuBinderWrapper(raw);
            Class<?> stub = Class.forName("android.hardware.input.IInputManager$Stub");
            im = stub.getMethod("asInterface", IBinder.class).invoke(null, wrapped);

            Log.i(TAG, "Text·3/3 Text injectInputEvent Text");
            mInject = Class.forName("android.hardware.input.IInputManager")
                    .getMethod("injectInputEvent", InputEvent.class, int.class);

            try {
                mSetDisplay = MotionEvent.class.getMethod("setDisplayId", int.class);
            } catch (Throwable ignored) {
                // [comment] —— [comment]Yes display 0，Default[comment]
                mSetDisplay = null;
            }
            return null;
        } catch (Throwable t) {
            return "Text: " + t;
        }
    }

    /**
     * [comment]。
     *
     * @param downTime MOVE / UP [comment] DOWN [comment]；≤0 [comment]
     * @return false = [comment]，[comment] {@code input}
     */
    public static synchronized boolean send(int displayId, int action, float x, float y,
                                            long downTime) {
        if (!ready()) {
            return false;
        }
        long now = SystemClock.uptimeMillis();
        MotionEvent ev = null;
        try {
            ev = MotionEvent.obtain(downTime <= 0L ? now : downTime, now, action, x, y, 0);
            ev.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            if (mSetDisplay != null) {
                mSetDisplay.invoke(ev, displayId);
            }
            Object r = mInject.invoke(im, ev, MODE_ASYNC);
            if (r instanceof Boolean && ((Boolean) r)) {
                fails = 0;
                return true;
            }
            if (++fails >= FAIL_LIMIT) {
                ok = false;
                why = "injectInputEvent Text（Text？）—— Text input Text";
                Log.w(TAG, why);
            }
        } catch (Throwable t) {
            ok = false;
            why = "Text: " + t;
            Log.w(TAG, why, t);
        } finally {
            if (ev != null) {
                ev.recycle();
            }
        }
        return false;
    }
}
