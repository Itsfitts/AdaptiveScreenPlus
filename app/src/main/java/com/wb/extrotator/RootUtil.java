package com.wb.extrotator;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Root（su）[comment]。
 *
 * <p>[comment] {@link ShellRunner}（[comment] Shizuku [comment] ADB [comment]）Yes[comment]：
 * [comment] root [comment]（KernelSU / Magisk）[comment] uid=0。Authorized[comment]，
 * [comment]Yes {@code ShellRunner} [comment] shell [comment]，{@code su -c} [comment] ——
 * adb shell [comment]Yes root [comment]。
 *
 * <p>⚠ [comment]（{@link #probe()}）[comment] exec [comment] {@code su -c id}：
 * [comment] root [comment]，exec [comment] ——
 * [comment]，[comment]。
 *
 * <p>⚠ [comment]、[comment]：[comment] root（KernelSU [comment]）[comment]，
 * [comment]"Authorized"[comment]。[comment]。
 */
public final class RootUtil {

    private static final String TAG = "RootUtil";

    /** [comment] */
    public static final int ST_UNKNOWN = 0;
    /** [comment] su [comment]（[comment] root） */
    public static final int ST_NONE = 1;
    /** [comment] su [comment] */
    public static final int ST_DENIED = 2;
    /** Authorized（id [comment] uid=0） */
    public static final int ST_OK = 3;

    /**
     * [comment] root [comment] su [comment]。
     *
     * <p>⭐ KernelSU / APatch [comment] su <b>[comment]</b> {@code /system/bin/su}，[comment]
     * {@code /data/adb/ksu/bin/su}（KernelSU）/ {@code /data/adb/ap/bin/su}（APatch）。
     * [comment] Z Flip5 Yes KernelSU —— [comment] Magisk [comment]「None root」
     * （2026-10-18 [comment]：[comment]Unauthorized，[comment]）。
     */
    private static final String[] SU_PATHS = {
            "/data/adb/ksu/bin/su", "/data/adb/ap/bin/su",
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
            "/debug_ramdisk/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/vendor/bin/su",
    };

    private static volatile int state = ST_UNKNOWN;

    private RootUtil() {
    }

    /** [comment]（{@link #ST_UNKNOWN} [comment]） */
    public static int state() {
        return state;
    }

    /** su [comment] */
    public static boolean hasSu() {
        return findSu() != null;
    }

    /**
     * [comment]，[comment] {@code done}（[comment] UI [comment]）。
     */
    public static void refresh(Runnable done) {
        new Thread(() -> {
            state = probe();
            Log.i(TAG, "Text => " + name(state));
            if (done != null) {
                done.run();
            }
        }, "root-probe").start();
    }

    /**
     * [comment]「Root [comment]」：[comment] {@code su -c id}，[comment] root [comment]，
     * [comment]。[comment] su [comment]，[comment] {@link #refresh} [comment]Yes[comment]。
     */
    public static void request(Runnable done) {
        new Thread(() -> {
            state = probe();
            Log.i(TAG, "Text => " + name(state));
            if (done != null) {
                done.run();
            }
        }, "root-request").start();
    }

    /** [comment] su [comment]；[comment] null */
    private static String findSu() {
        for (String p : SU_PATHS) {
            try {
                if (new File(p).exists()) {
                    return p;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /** [comment] {@code su -c id}：[comment] su [comment] uid=0 ⇒ Authorized */
    private static int probe() {
        final String su = findSu();
        if (su == null) {
            return ST_NONE;
        }
        Process p = null;
        try {
            p = new ProcessBuilder(su, "-c", "id")
                    .redirectErrorStream(true)
                    .start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            p.waitFor();
            String out = sb.toString().trim();
            Log.d(TAG, "su -c id => " + out);
            return out.contains("uid=0") ? ST_OK : ST_DENIED;
        } catch (Throwable t) {
            Log.w(TAG, "probe Failed", t);
            return ST_DENIED;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    /**
     * [comment] root [comment]，[comment] stdout/stderr；[comment] null。
     *
     * <p>[comment] {@link ShellRunner} [comment]：root Authorized[comment]，[comment]Yes shell [comment]
     * {@code su -c} [comment]（adb shell [comment]Yes root [comment]）。[comment]，[comment]。
     */
    public static String exec(String cmd) {
        return exec(cmd, 0);
    }

    /**
     * [comment] {@link #exec}：[comment] destroy [comment]、[comment]Disable，[comment]。
     *
     * <p>[comment] {@code ShellRunner.run(cmd, timeoutSec)} [comment] —— [comment]Yes
     * [comment]，[comment]Yes[comment]。
     *
     * @param timeoutSec ≤0 [comment]
     */
    public static String exec(String cmd, int timeoutSec) {
        final String su = findSu();
        if (su == null) {
            return null;
        }
        if (timeoutSec <= 0) {
            return execBlocking(su, cmd, null);
        }
        final Process[] holder = new Process[1];
        final String[] box = new String[]{""};
        Thread reader = new Thread(() -> box[0] = execBlocking(su, cmd, holder),
                "root-exec");
        reader.setDaemon(true);
        reader.start();
        try {
            reader.join(timeoutSec * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        if (reader.isAlive()) {
            Log.d(TAG, cmd + " => [TIMEOUT " + timeoutSec + "s] Text");
            Process p = holder[0];
            if (p != null) {
                try {
                    p.destroy();
                } catch (Throwable ignored) {
                }
            }
            return "TIMEOUT";
        }
        return box[0];
    }

    /**
     * [comment]（{@link #exec} [comment]）。
     *
     * @param holder [comment] null [comment]，[comment] start [comment]，[comment] destroy
     */
    private static String execBlocking(String su, String cmd, Process[] holder) {
        Process p = null;
        try {
            p = new ProcessBuilder(su, "-c", cmd)
                    .redirectErrorStream(true)
                    .start();
            if (holder != null) {
                holder[0] = p;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            p.waitFor();
            String out = sb.toString().trim();
            Log.d(TAG, "su -c " + cmd + " => " + out);
            return out;
        } catch (Throwable t) {
            Log.w(TAG, "exec Failed: " + cmd, t);
            return null;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    /**
     * [comment] root [comment]，[comment] stdout <b>[comment]</b>（screencap [comment]）。
     *
     * <p>[comment] {@link #exec} [comment] {@code ShellRunner}：[comment] exec，[comment] ——
     * [comment] BufferedReader [comment] PNG [comment]。[comment] / [comment] null。
     *
     * @param maxBytes [comment]，[comment]None[comment]
     */
    public static byte[] capture(String cmd, int maxBytes) {
        final String su = findSu();
        if (su == null) {
            return null;
        }
        Process p = null;
        try {
            p = new ProcessBuilder(su, "-c", cmd)
                    .redirectErrorStream(true)
                    .start();
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(1 << 16);
            try (java.io.InputStream is = p.getInputStream()) {
                byte[] buf = new byte[64 * 1024];
                int n;
                int total = 0;
                while ((n = is.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                    total += n;
                    if (total >= maxBytes) {
                        Log.w(TAG, "capture Text " + maxBytes + "，Text: " + cmd);
                        break;
                    }
                }
            }
            p.waitFor();
            byte[] out = bos.toByteArray();
            return out.length > 0 ? out : null;
        } catch (Throwable t) {
            Log.w(TAG, "capture Failed: " + cmd, t);
            return null;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    /** [comment]（[comment]） */
    static String name(int st) {
        switch (st) {
            case ST_NONE:
                return "None root";
            case ST_DENIED:
                return "Unauthorized";
            case ST_OK:
                return "Authorized";
            default:
                return "Text";
        }
    }
}
