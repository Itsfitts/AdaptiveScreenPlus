package com.wb.extrotator;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 「[comment]」[comment]：[comment] CPU / [comment] / [comment] / [comment] / [comment]。
 * ⚠ [comment]，{@link Cb} [comment]。
 */
public final class StatusMonitor {

    private static final String TAG = "ExtStatus";

    /** [comment]。[comment]，[comment] */
    private static final long PERIOD_MS = 1000L;

    /** [comment]（1 [comment] = 1 [comment]） */
    public static final int HIST = 60;

    /** [comment]（℃）。thermal [comment] -273 [comment]，[comment] */
    private static final float TEMP_MIN = -20f, TEMP_MAX = 200f;

    /** [comment]；[comment]，[comment] */
    public static final class Snap {
        /** CPU [comment] 0~100；-1 = [comment]（[comment]） */
        public int cpuPct = -1;
        public int memPct = -1;
        public long memUsedMb = -1, memTotalMb = -1;
        /** [comment] CPU [comment]；NaN = [comment] */
        public float tempC = Float.NaN;
        /** [comment]（[comment]）[comment]；NaN = [comment] */
        public float refreshHz = Float.NaN;
        /** [comment]，[comment] */
        public int panelW = 0, panelH = 0;
        public int battPct = -1;
        public boolean charging;

        /** [comment]（policy）[comment] / [comment]（KHz）[comment]；[comment]Yes[comment] */
        public int[] curKhz = new int[0];
        public int[] maxKhz = new int[0];
        public int[] nrCores = new int[0];

        /** CPU [comment]，[comment]Yes[comment]；[comment]Yes -1 */
        public int[] hist = new int[HIST];
    }

    /** [comment]，<b>[comment]</b> */
    public interface Cb {
        void onSample(Snap s);
    }

    private final Context app;
    private Activity host;
    private final Cb cb;

    /** [comment]，[comment] */
    private final int[] hist = new int[HIST];

    private ScheduledExecutorService exec;

    // ---- [comment]Yes"[comment]"[comment]，[comment] listFiles ----
    /** [comment]：idlePaths[cpu] = {state0/time, state1/time, ...} */
    private String[][] idlePaths;
    /** CPU [comment] temp [comment] */
    private String[] tempPaths;
    /** [comment] cpufreq [comment] */
    private File[] policyDirs;
    /** [comment]（KHz），[comment] policyDirs [comment] */
    private int[] policyMaxKhz;

    // ---- CPU [comment] ----
    private long lastIdleUs = -1, lastWallUs = -1;
    private List<Integer> lastCpus;

    public StatusMonitor(Activity host, Cb cb) {
        this.host = host;
        this.cb = cb;
        this.app = host.getApplicationContext();
        java.util.Arrays.fill(hist, -1);
    }

    /** [comment]（[comment]，[comment] {@link #PERIOD_MS} [comment]） */
    public void start() {
        stop();
        exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "extrot-status");
            t.setDaemon(true);
            return t;
        });
        // ⚠ [comment] scheduleWithFixedDelay：[comment]，[comment]
        exec.scheduleWithFixedDelay(this::tick, 0L, PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    /** [comment]。[comment]，[comment]Yes[comment]，[comment]Yes[comment] */
    public void stop() {
        ScheduledExecutorService e = exec;
        exec = null;
        if (e != null) {
            e.shutdownNow();
        }
    }

    /** [comment]，[comment] Activity */
    public void release() {
        stop();
        host = null;
    }

    // ------------------------------------------------------------------ [comment]

    private void tick() {
        Snap s = new Snap();
        try {
            sampleCpuUsage(s);
            sampleFreq(s);
            sampleTemp(s);
            sampleMem(s);
            sampleDisplay(s);
            sampleBattery(s);
        } catch (Throwable t) {
            Log.w(TAG, "Text", t);
        }
        if (s.cpuPct >= 0) {
            System.arraycopy(hist, 1, hist, 0, HIST - 1);
            hist[HIST - 1] = s.cpuPct;
        }
        s.hist = hist;

        final Activity a = host;
        if (a == null || a.isFinishing() || a.isDestroyed()) {
            return;
        }
        a.runOnUiThread(() -> {
            if (host != null && !host.isFinishing() && !host.isDestroyed()) {
                cb.onSample(s);
            }
        });
    }

    /** [comment] CPU [comment]。[comment] online [comment]；[comment]，[comment] */
    private void sampleCpuUsage(Snap s) {
        List<Integer> cpus = onlineCpus();
        if (cpus.isEmpty()) {
            return;
        }
        if (idlePaths == null) {
            idlePaths = buildIdlePaths();
        }
        long idle = 0;
        for (int c : cpus) {
            String[] paths = c < idlePaths.length ? idlePaths[c] : null;
            if (paths == null) {
                continue;
            }
            for (String p : paths) {
                long v = readLong(p, -1L);
                if (v > 0) {
                    idle += v;
                }
            }
        }
        long wall = SystemClock.elapsedRealtimeNanos() / 1000L;

        // [comment]（>10 [comment]，[comment]）[comment]，[comment]
        boolean usable = lastIdleUs > 0 && lastWallUs > 0 && cpus.equals(lastCpus)
                && wall > lastWallUs && (wall - lastWallUs) < 10_000_000L;
        if (usable) {
            double dIdle = idle - lastIdleUs;
            double dWall = (double) (wall - lastWallUs) * cpus.size();
            if (dWall > 0) {
                double used = 1.0 - dIdle / dWall;
                used = Math.max(0.0, Math.min(1.0, used));
                s.cpuPct = (int) Math.round(used * 100.0);
            }
        }
        lastIdleUs = idle;
        lastWallUs = wall;
        lastCpus = cpus;
    }

    /** [comment] */
    private void sampleFreq(Snap s) {
        if (policyDirs == null) {
            policyDirs = policyDirs();
            policyMaxKhz = new int[policyDirs.length];
            for (int i = 0; i < policyDirs.length; i++) {
                policyMaxKhz[i] = (int) readLong(new File(policyDirs[i], "cpuinfo_max_freq")
                        .getAbsolutePath(), 0L);
            }
        }
        int n = policyDirs.length;
        if (n == 0) {
            return;
        }
        int[] cur = new int[n], max = new int[n], cores = new int[n];
        int keep = 0;
        for (int i = 0; i < n; i++) {
            long khz = readLong(new File(policyDirs[i], "scaling_cur_freq").getAbsolutePath(), 0L);
            if (khz <= 0) {
                continue;
            }
            cur[keep] = (int) khz;
            max[keep] = policyMaxKhz[i] > 0 ? policyMaxKhz[i] : (int) khz;
            cores[keep] = coreCountOf(policyDirs[i]);
            keep++;
        }
        if (keep == 0) {
            return;
        }
        s.curKhz = java.util.Arrays.copyOf(cur, keep);
        s.maxKhz = java.util.Arrays.copyOf(max, keep);
        s.nrCores = java.util.Arrays.copyOf(cores, keep);
    }

    /** [comment]：[comment] CPU [comment]，[comment] zone [comment]"[comment]" */
    private void sampleTemp(Snap s) {
        if (tempPaths == null) {
            tempPaths = buildTempPaths();
        }
        float best = Float.NaN;
        for (String p : tempPaths) {
            long milli = readLong(p, Long.MIN_VALUE);
            if (milli == Long.MIN_VALUE) {
                continue;
            }
            float c = milli / 1000f;   // TextYesText
            if (c < TEMP_MIN || c > TEMP_MAX) {
                continue;
            }
            if (Float.isNaN(best) || c > best) {
                best = c;
            }
        }
        s.tempC = best;
    }

    private void sampleMem(Snap s) {
        try {
            ActivityManager am = (ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            if (mi.totalMem <= 0) {
                return;
            }
            long used = mi.totalMem - mi.availMem;
            s.memPct = (int) (used * 100L / mi.totalMem);
            s.memUsedMb = used / 1048576L;
            s.memTotalMb = mi.totalMem / 1048576L;
        } catch (Throwable ignored) {
        }
    }

    private void sampleDisplay(Snap s) {
        try {
            DisplayManager dm = (DisplayManager) app.getSystemService(Context.DISPLAY_SERVICE);
            Display d = dm.getDisplay(Display.DEFAULT_DISPLAY);
            if (d == null) {
                return;
            }
            float hz = d.getRefreshRate();
            if (hz <= 0 && d.getMode() != null) {
                hz = d.getMode().getRefreshRate();
            }
            s.refreshHz = hz;
            Display.Mode m = d.getMode();
            if (m != null) {
                s.panelW = m.getPhysicalWidth();
                s.panelH = m.getPhysicalHeight();
            }
        } catch (Throwable ignored) {
        }
    }

    private void sampleBattery(Snap s) {
        try {
            Intent b = app.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (b == null) {
                return;
            }
            int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) {
                s.battPct = (int) (level * 100f / scale);
            }
            int st = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            s.charging = st == BatteryManager.BATTERY_STATUS_CHARGING
                    || st == BatteryManager.BATTERY_STATUS_FULL;
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ [comment]

    /** {@code /sys/devices/system/cpu/online} → [comment]，[comment] {@code 0-7} [comment] {@code 0-2,4} */
    private static List<Integer> onlineCpus() {
        List<Integer> out = new ArrayList<>();
        String s = readLine("/sys/devices/system/cpu/online");
        if (s == null) {
            return out;
        }
        for (String part : s.split(",")) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            try {
                int dash = part.indexOf('-');
                if (dash > 0) {
                    int a = Integer.parseInt(part.substring(0, dash).trim());
                    int b = Integer.parseInt(part.substring(dash + 1).trim());
                    for (int i = a; i <= b && i < 64; i++) {
                        out.add(i);
                    }
                } else {
                    out.add(Integer.parseInt(part));
                }
            } catch (Throwable ignored) {
            }
        }
        Collections.sort(out);
        return out;
    }

    private static String[][] buildIdlePaths() {
        int cores = Runtime.getRuntime().availableProcessors();
        String[][] all = new String[Math.max(cores, 8) + 8][];
        for (int c = 0; c < all.length; c++) {
            File dir = new File("/sys/devices/system/cpu/cpu" + c + "/cpuidle");
            File[] states = dir.listFiles();
            if (states == null) {
                continue;
            }
            List<String> ps = new ArrayList<>();
            for (File st : states) {
                if (!st.getName().startsWith("state")) {
                    continue;
                }
                File t = new File(st, "time");
                if (t.canRead()) {
                    ps.add(t.getAbsolutePath());
                }
            }
            if (!ps.isEmpty()) {
                Collections.sort(ps);
                all[c] = ps.toArray(new String[0]);
            }
        }
        return all;
    }

    /** [comment] CPU [comment]。[comment] cpu [comment]；[comment]，[comment]「—」 */
    private static String[] buildTempPaths() {
        List<String> out = new ArrayList<>();
        File dir = new File("/sys/class/thermal");
        File[] zones = dir.listFiles();
        if (zones == null) {
            return new String[0];
        }
        for (File z : zones) {
            if (!z.getName().startsWith("thermal_zone")) {
                continue;
            }
            String type = readLine(new File(z, "type").getAbsolutePath());
            if (type == null) {
                continue;
            }
            if (type.toLowerCase(Locale.ROOT).contains("cpu")) {
                out.add(new File(z, "temp").getAbsolutePath());
            }
        }
        Collections.sort(out);
        return out.toArray(new String[0]);
    }

    private static File[] policyDirs() {
        File base = new File("/sys/devices/system/cpu/cpufreq");
        File[] all = base.listFiles();
        if (all == null) {
            return new File[0];
        }
        List<File> out = new ArrayList<>();
        for (File f : all) {
            if (f.getName().startsWith("policy")
                    && new File(f, "scaling_cur_freq").canRead()) {
                out.add(f);
            }
        }
        Collections.sort(out, (a, b) -> a.getName().compareTo(b.getName()));
        return out.toArray(new File[0]);
    }

    /** [comment]：[comment] {@code related_cpus}（[comment] {@code 0-3}），[comment] 1 */
    private static int coreCountOf(File policyDir) {
        String s = readLine(new File(policyDir, "related_cpus").getAbsolutePath());
        if (s == null) {
            return 1;
        }
        int n = 0;
        for (String part : s.split(",")) {
            part = part.trim();
            int dash = part.indexOf('-');
            try {
                if (dash > 0) {
                    int a = Integer.parseInt(part.substring(0, dash).trim());
                    int b = Integer.parseInt(part.substring(dash + 1).trim());
                    n += Math.max(0, b - a + 1);
                } else if (!part.isEmpty()) {
                    n += 1;
                }
            } catch (Throwable ignored) {
            }
        }
        return n > 0 ? n : 1;
    }

    // ------------------------------------------------------------------ [comment]

    private static String readLine(String path) {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String l = r.readLine();
            return l == null ? null : l.trim();
        } catch (Throwable t) {
            return null;
        }
    }

    private static long readLong(String path, long fallback) {
        String s = readLine(path);
        if (s == null || s.isEmpty()) {
            return fallback;
        }
        int sp = s.indexOf(' ');
        if (sp > 0) {
            s = s.substring(0, sp);
        }
        try {
            return Long.parseLong(s);
        } catch (Throwable t) {
            return fallback;
        }
    }
}
