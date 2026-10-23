package com.wb.extrotator;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 「[comment]」——[comment]（[comment]）[comment]（v4.23 [comment]）。
 *
 * <p><b>[comment]</b>：[comment]「[comment]」[comment]<b>Default[comment]</b>（[comment]）。[comment]Yes[comment]
 * （Default {@code lock 2} = [comment]），[comment]，[comment]「[comment] 90°」[comment]
 * [comment]。[comment]：[comment]Yes[comment]Yes[comment]，[comment]。
 *
 * <p><b>[comment]Yes[comment]</b>：[comment]<b>[comment]</b>（[comment] {@link CoverDisplay} [comment]）——
 * <pre>
 *   wm fixed-to-user-rotation -d 1 enabled          ← [comment]，[comment] lock [comment]
 *   cmd window user-rotation -d 1 lock &lt;0..3&gt;      ← 0/1/2/3 = 0°/90°/180°/270°
 * </pre>
 * [comment] = 0（[comment]）[comment] {@link CoverDisplay#ROT_NATIVE}（2，[comment]）；
 * [comment] = 1 [comment] 3，[comment]。
 *
 * <p><b>[comment]</b>：Android [comment] {@code rotation} [comment]Yes「[comment] 90°，
 * [comment] 90° [comment]，[comment]Yes {@code ROTATION_90}」。[comment]，[comment]<b>[comment]</b>
 * [comment]，[comment] <b>+x</b>。[comment]：
 * <pre>
 *   x &gt; 0（[comment] / [comment]） ⇒ lock 1      x &lt; 0（[comment] / [comment]） ⇒ lock 3
 *   y &gt; 0（[comment]） ⇒ lock 0                   y &lt; 0（[comment]） ⇒ lock 2
 * </pre>
 * ⚠ 「[comment]」= [comment] 180°：[comment]，[comment]，
 * [comment]Yes[comment] +y [comment] -y —— [comment]（[comment]Yes[comment]「[comment]」[comment]）。
 * [comment]，[comment] {@link #onAccel} [comment] {ROT_90, ROT_270} [comment]；
 * ⚠ [comment]/[comment]（[comment] 2026-09-26 [comment]：180/0 [comment]Yes[comment]）——
 * [comment]Yes[comment]，[comment]「[comment] 2」[comment]。
 *
 * <p><b>[comment]</b>：① [comment]（[comment] 52°，[comment] 38°）—— [comment]Yes[comment]，
 * [comment]；② [comment] {@link #SETTLE_MS} [comment]；③ [comment]<b>[comment]</b>
 * （[comment]/[comment]）[comment]"[comment]Yes[comment]"，[comment] —— No[comment]。
 *
 * <p>⚠ <b>[comment]</b>：[comment] {@link VolumeKeyService}（[comment]None[comment]）[comment]，
 * [comment] {@code onServiceConnected} [comment] {@link #sync} [comment]。[comment]<b>[comment]None[comment]</b> ——
 * [comment]「[comment]」[comment]。
 *
 * <p>⚠ [comment] {@code set / toggle} [comment] shell（Shizuku），<b>[comment]</b>。
 */
public final class CoverAutoRot {

    private static final String TAG = "CoverAutoRot";

    private static final String SP = "cover_autorot";
    /** [comment] */
    private static final String K_ON = "on";
    /** [comment]Yes[comment]，[comment] */
    private static final String K_BASE = "base";

    /** [comment] → [comment]（[comment]） */
    private static final float TILT_TO_LAND = 52f;
    /** [comment] → [comment]。[comment]，[comment]Yes[comment] */
    private static final float TILT_TO_PORT = 38f;
    /** [comment]，[comment] */
    private static final long SETTLE_MS = 450L;

    private static final Object LOCK = new Object();

    private static SensorManager sensors;
    private static SensorEventListener listener;
    private static volatile Context appCtx;
    private static ExecutorService worker;

    /** [comment]（0..3）；{@link Integer#MIN_VALUE} = [comment] */
    private static volatile int cur = Integer.MIN_VALUE;
    /** [comment] */
    private static int pend = Integer.MIN_VALUE;
    private static long pendAt;

    private CoverAutoRot() {
    }

    // ------------------------------------------------------------------ Settings

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(SP, Context.MODE_PRIVATE);
    }

    /** [comment]Yes[comment] */
    public static boolean on(Context c) {
        try {
            return sp(c).getBoolean(K_ON, false);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void flag(Context c, boolean on) {
        try {
            sp(c).edit().putBoolean(K_ON, on).apply();
        } catch (Throwable ignored) {
        }
    }

    /**
     * [comment]。<b>[comment]</b>（[comment] shell）。
     *
     * <p>[comment]，[comment] —— [comment]"[comment] 2"，
     * [comment]Yes[comment]，[comment]。
     *
     * @return null = [comment]；No[comment]Yes[comment]（[comment]）
     */
    public static String set(Context c, boolean want) {
        if (c == null) {
            return "Text";
        }
        Context app = c.getApplicationContext();
        int did = CoverDisplay.id(app);
        if (want) {
            if (did <= 0) {
                return "Text";
            }
            if (!ShellRunner.isReady()) {
                return "Text Shizuku";
            }
            // [comment]Yes[comment]None[comment]，[comment]
            if (!VolumeKeyService.isRunning()) {
                return "TextNoneText";
            }
            int base = CoverDisplay.currentRotation(app, did);
            sp(app).edit().putBoolean(K_ON, true).putInt(K_BASE, base).apply();
            // [comment]：[comment]，[comment] lock [comment]
            ShellRunner.run("wm fixed-to-user-rotation -d " + did + " enabled");
            String err = start(app);
            if (err != null) {
                flag(app, false);       // Text"Text"Text
                return err;
            }
            return null;
        }
        flag(app, false);
        stop();
        if (did > 0 && ShellRunner.isReady()) {
            int base = sp(app).getInt(K_BASE, CoverDisplay.ROT_NATIVE);
            if (base < 0) {
                CoverDisplay.resetRotation(app);    // TextYes"Text"Text
            } else {
                ShellRunner.run("cmd window user-rotation -d " + did + " lock " + base);
            }
        }
        return null;
    }

    /** [comment]。<b>[comment]</b>。@return null = [comment] */
    public static String toggle(Context c) {
        return c == null ? "Text" : set(c, !on(c));
    }

    /**
     * [comment]Settings[comment]：[comment]，[comment]。[comment]
     * （[comment]、[comment]，[comment]）。
     */
    static void sync(Context c) {
        if (c == null) {
            return;
        }
        Context app = c.getApplicationContext();
        if (!on(app)) {
            stop();
            return;
        }
        String err = start(app);
        if (err != null) {
            Log.w(TAG, "TextSettings：" + err);
        }
    }

    // ------------------------------------------------------------------ [comment]

    private static String start(Context app) {
        synchronized (LOCK) {
            if (listener != null) {
                return null;
            }
            try {
                SensorManager sm = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
                if (sm == null) {
                    return "Text";
                }
                Sensor s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (s == null) {
                    return "Text";
                }
                appCtx = app.getApplicationContext();
                SensorEventListener l = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent e) {
                        try {
                            onAccel(e.values);
                        } catch (Throwable t) {
                            Log.w(TAG, "Text", t);
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    }
                };
                // [comment]：[comment]，[comment]
                boolean ok = sm.registerListener(l, s, SensorManager.SENSOR_DELAY_UI, 0,
                        new Handler(Looper.getMainLooper()));
                if (!ok) {
                    return "Text";
                }
                sensors = sm;
                listener = l;
                pend = Integer.MIN_VALUE;
                // [comment]，[comment]（[comment]Yes shell），[comment]
                cur = CoverDisplay.ROT_NATIVE;
                worker().execute(() -> {
                    Context a = appCtx;
                    if (a != null) {
                        cur = CoverDisplay.currentRotation(a, CoverDisplay.id(a));
                        Log.i(TAG, "Text，Text " + cur);
                    }
                });
                return null;
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed", t);
                return "Text";
            }
        }
    }

    static void stop() {
        synchronized (LOCK) {
            if (sensors != null && listener != null) {
                try {
                    sensors.unregisterListener(listener);
                } catch (Throwable ignored) {
                }
            }
            sensors = null;
            listener = null;
            appCtx = null;
            cur = Integer.MIN_VALUE;
            pend = Integer.MIN_VALUE;
        }
    }

    /**
     * [comment]：[comment]，[comment]。
     *
     * <p>[comment]：[comment] y [comment]，[comment] x [comment]，[comment] z [comment]。
     */
    private static void onAccel(float[] v) {
        if (v == null || v.length < 3) {
            return;
        }
        float x = v[0];
        float y = v[1];
        float z = v[2];
        // [comment]（[comment]/[comment]）：[comment]Yes[comment]，[comment]
        if (Math.abs(z) > Math.abs(x) && Math.abs(z) > Math.abs(y)) {
            return;
        }
        int now = cur;
        boolean landNow = now == Surface.ROTATION_90 || now == Surface.ROTATION_270;
        float ang = (float) Math.toDegrees(Math.atan2(Math.abs(x), Math.abs(y)));
        boolean land = ang >= (landNow ? TILT_TO_PORT : TILT_TO_LAND);
        int want;
        if (land) {
            // [comment]（x > 0）= [comment] = 90°；[comment] = [comment] = 270°
            want = x > 0 ? Surface.ROTATION_90 : Surface.ROTATION_270;
        } else {
            // [comment]「[comment] / [comment]」—— [comment]，[comment] want [comment] now，
            // [comment]「[comment]」[comment]，[comment]Yes 0° [comment]（v471 [comment] bug）。
            want = y >= 0 ? Surface.ROTATION_0 : CoverDisplay.ROT_NATIVE;
        }
        if (want == now) {
            pend = Integer.MIN_VALUE;
            return;
        }
        long t = SystemClock.uptimeMillis();
        if (want != pend) {
            pend = want;
            pendAt = t;
            return;
        }
        if (t - pendAt < SETTLE_MS) {
            return;
        }
        pend = Integer.MIN_VALUE;
        fire(want);
    }

    /** [comment]。[comment]，[comment] —— [comment]。 */
    private static void fire(final int rot) {
        final Context app = appCtx;
        if (app == null) {
            return;
        }
        cur = rot;              // Text，Text
        worker().execute(() -> {
            int did = CoverDisplay.id(app);
            if (did <= 0 || !ShellRunner.isReady()) {
                Log.w(TAG, "Text（Text " + did + "，Shizuku " + ShellRunner.isReady() + "）");
                return;
            }
            // [comment]：[comment]Allow[comment]，[comment]（[comment] CoverDisplay.rotate [comment]）
            ShellRunner.run("wm fixed-to-user-rotation -d " + did + " enabled");
            ShellRunner.run("cmd window user-rotation -d " + did + " lock " + rot);
            // [comment]：[comment]，[comment]
            int real = CoverDisplay.currentRotation(app, did);
            if (real >= 0 && real != rot) {
                Log.w(TAG, "Text：Text " + rot + "，Text " + real);
                cur = real;
            } else {
                Log.i(TAG, "Text → " + rot);
            }
        });
    }

    private static ExecutorService worker() {
        synchronized (LOCK) {
            if (worker == null) {
                worker = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "cover-autorot");
                    t.setDaemon(true);
                    return t;
                });
            }
            return worker;
        }
    }
}
