package com.wb.extrotator;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 「[comment]」（[comment]，Galaxy Z Flip [comment] displayId=1）[comment]。
 *
 * <p>[comment] {@link DisplayProfile} [comment]None[comment] —— [comment]Yes「[comment]」，[comment]Yes[comment]
 * [comment]，[comment] Beta [comment]。
 *
 * <p><b>[comment]Yes[comment]，[comment]</b>：
 * <pre>
 *   wm size &lt;W&gt;x&lt;H&gt; -d &lt;id&gt;        ✅ [comment]（base/cur [comment]）
 *   wm size -d &lt;id&gt; &lt;W&gt;x&lt;H&gt;        ❌ [comment]None[comment]（[comment]Yes Physical size）
 *   wm density &lt;dpi&gt; -d &lt;id&gt;       ✅ [comment]（[comment] Override density）
 *   wm size reset -d &lt;id&gt;          ✅ [comment]
 *   wm density reset -d &lt;id&gt;       ✅ [comment]
 *   wm fixed-to-user-rotation -d &lt;id&gt; enabled   ← [comment]，No[comment] lock [comment]
 *   cmd window user-rotation -d &lt;id&gt; lock &lt;0..3&gt;
 *   cmd window user-rotation -d &lt;id&gt; free        ← [comment]
 * </pre>
 *
 * <p>⚠ [comment]Yes {@code cmd window user-rotation}，<b>[comment]Yes</b> {@code wm user-rotation} —— [comment]
 * [comment]Default[comment]。
 *
 * <p>[comment] binder，<b>[comment]</b>。
 */
public final class CoverDisplay {

    private static final String TAG = "CoverDisplay";

    private static final Pattern P_SIZE = Pattern.compile("(\\d+)\\s*x\\s*(\\d+)");
    private static final Pattern P_LOCK = Pattern.compile("lock\\s+(\\d)");

    /** [comment]（0..3），[comment] -1 */
    public static final int ROT_FREE = -1;

    private CoverDisplay() {
    }

    /** [comment] display id；[comment]（[comment]）[comment] -1 */
    public static int id(Context ctx) {
        try {
            DisplayManager dm = (DisplayManager) ctx.getApplicationContext()
                    .getSystemService(Context.DISPLAY_SERVICE);
            Display c = DisplayUtil.coverDisplay(dm);
            return c == null ? -1 : c.getDisplayId();
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Shizuku [comment] + [comment]，[comment] */
    public static boolean available(Context ctx) {
        return ShellRunner.isReady() && id(ctx) > 0;
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]（[comment]、[comment]、[comment]），[comment]。
     * <b>[comment]</b>（[comment] shell [comment]）
     */
    public static State read(Context ctx) {
        State s = new State();
        int did = id(ctx);
        s.displayId = did;
        if (did <= 0) {
            return s;
        }

        String sizeOut = ShellRunner.run("wm size -d " + did);
        s.physSize = firstMatch(P_SIZE, section(sizeOut, "Physical"));
        String ov = section(sizeOut, "Override");
        s.overrideSize = ov == null ? null : firstMatch(P_SIZE, ov);

        String dpiOut = ShellRunner.run("wm density -d " + did);
        s.physDpi = parseInt(section(dpiOut, "Physical"));
        s.overrideDpi = parseInt(section(dpiOut, "Override"));

        s.rotation = currentRotation(ctx, did);
        return s;
    }

    /**
     * [comment]：0..3 Yes[comment] lock [comment]；
     * {@link #ROT_FREE} [comment]（[comment]）。
     */
    public static int currentRotation(Context ctx, int did) {
        try {
            String out = ShellRunner.run("cmd window user-rotation -d " + did);
            if (out != null) {
                if (out.contains("free")) {
                    return ROT_FREE;
                }
                Matcher m = P_LOCK.matcher(out);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        } catch (Throwable ignored) {
        }
        // [comment]：[comment]。⚠️ [comment]，[comment]。
        try {
            DisplayManager dm = (DisplayManager) ctx.getApplicationContext()
                    .getSystemService(Context.DISPLAY_SERVICE);
            Display d = dm == null ? null : dm.getDisplay(did);
            return d == null ? ROT_FREE : d.getRotation();
        } catch (Throwable t) {
            return ROT_FREE;
        }
    }

    // ------------------------------------------------------------------ [comment] / DPI

    /**
     * [comment] DPI。<b>[comment]</b>。
     *
     * @param dpi [comment] 0 [comment] DPI（[comment]）
     * @return null [comment]Success，No[comment]Yes[comment]Error[comment]
     */
    public static String applySize(Context ctx, int w, int h, int dpi) {
        int did = id(ctx);
        if (did <= 0) {
            return "Text";
        }
        if (!ShellRunner.isReady()) {
            return "Shizuku Not Ready";
        }
        if (w < 200 || h < 200) {
            return "Text（Text ≥ 200）";
        }
        // [comment]Yes[comment]：size [comment]、-d id [comment]
        ShellRunner.run("wm size " + w + "x" + h + " -d " + did);
        if (dpi > 0) {
            ShellRunner.run("wm density " + dpi + " -d " + did);
        }
        return null;
    }

    /**
     * [comment] / DPI [comment]。<b>[comment]</b>。
     *
     * @param withDpi YesNo[comment] DPI [comment]
     */
    public static String resetSize(Context ctx, boolean withDpi) {
        int did = id(ctx);
        if (did <= 0) {
            return "Text";
        }
        if (!ShellRunner.isReady()) {
            return "Shizuku Not Ready";
        }
        ShellRunner.run("wm size reset -d " + did);
        if (withDpi) {
            ShellRunner.run("wm density reset -d " + did);
        }
        return null;
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment] 90°（0→1→2→3→0）。
     *
     * <p>[comment] {@code fixed-to-user-rotation}：[comment]Yes"Allow[comment]"[comment]
     * [comment]，[comment]，[comment]。
     *
     * <b>[comment]</b>
     *
     * @return [comment]（0..3）；-1 [comment]Failed
     */
    public static int rotate(Context ctx) {
        int did = id(ctx);
        if (did <= 0 || !ShellRunner.isReady()) {
            return -1;
        }
        int cur = currentRotation(ctx, did);
        // [comment]（free，[comment] -1）[comment]**[comment] 90°**，[comment] 0°：
        // 0° [comment]Yes[comment]，[comment]，[comment]。
        // （[comment]Yes[comment]「[comment]」[comment]，[comment] free [comment]Yes 1。）
        int next = (cur < 0 ? 1 : (cur + 1) % 4);
        ShellRunner.run("wm fixed-to-user-rotation -d " + did + " enabled");
        ShellRunner.run("cmd window user-rotation -d " + did + " lock " + next);
        Log.i(TAG, "Text " + cur + " -> " + next);
        return next;
    }

    /** [comment]，[comment]。<b>[comment]</b> */
    public static boolean resetRotation(Context ctx) {
        int did = id(ctx);
        if (did <= 0 || !ShellRunner.isReady()) {
            return false;
        }
        ShellRunner.run("cmd window user-rotation -d " + did + " free");
        return true;
    }

    // ------------------------------------------------------------------ [comment]（[comment]）[comment]

    /** [comment]：[comment]，[comment]"[comment]"[comment]Yes 180° */
    public static final int ROT_NATIVE = 2;

    /**
     * [comment]（[comment]）。<b>[comment]</b>（[comment] 0.3 [comment]）。
     *
     * <p>[comment]<b>[comment]</b>[comment]，[comment]：[comment] = {@link DualScreen#TENT}
     * （{@code CLOSED} [comment]，[comment]）；[comment] 180°（{@link #ROT_NATIVE}）——
     * 0° / 90° / 270° [comment]。
     *
     * <p>[comment] {@code mAppBounds.top} [comment] 0 [comment] 66（= [comment]），[comment]Yes[comment]
     * <b>[comment]</b>；[comment]Yes[comment]（[comment]No[comment]）。
     *
     * <p>⚠️ [comment]Yes<b>[comment]</b>，[comment]「[comment] / [comment]」[comment]。[comment]
     * （{@code device_state state reset} + lock [comment]）—— [comment] reset [comment]
     * （[comment] top=0）。
     *
     * @return true = [comment]
     */
    public static boolean avoidCutout(Context ctx) {
        int did = id(ctx);
        if (did <= 0 || !ShellRunner.isReady()) {
            return false;
        }
        DualScreen.setState(DualScreen.TENT);
        // [comment]，[comment] lock [comment] TENT [comment]
        sleep(250);
        // [comment]：[comment]Allow[comment]，[comment]，No[comment] lock [comment]
        ShellRunner.run("wm fixed-to-user-rotation -d " + did + " enabled");
        ShellRunner.run("cmd window user-rotation -d " + did + " lock " + ROT_NATIVE);
        Log.i(TAG, "Text：TENT + lock " + ROT_NATIVE);
        return true;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment] "Physical size: 748x720" [comment] */
    private static String section(String out, String key) {
        if (out == null) {
            return null;
        }
        for (String line : out.split("\n")) {
            if (line.trim().startsWith(key)) {
                int i = line.indexOf(':');
                return i < 0 ? null : line.substring(i + 1).trim();
            }
        }
        return null;
    }

    private static String firstMatch(Pattern p, String s) {
        if (s == null) {
            return null;
        }
        Matcher m = p.matcher(s);
        return m.find() ? (m.group(1) + "x" + m.group(2)) : null;
    }

    private static int parseInt(String s) {
        if (s == null) {
            return 0;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (Throwable t) {
            return 0;
        }
    }

    /** [comment] → [comment] */
    public static String degreeText(int rot) {
        switch (rot) {
            case 0:
                return "0°";
            case 1:
                return "90°";
            case 2:
                return "180°";
            case 3:
                return "270°";
            default:
                return "Text";
        }
    }

    /** [comment] */
    public static final class State {
        public int displayId = -1;
        /** [comment]，[comment] 748x720 */
        public String physSize;
        /** [comment]；[comment] null */
        public String overrideSize;
        /** [comment] DPI */
        public int physDpi;
        /** [comment] DPI；0 = [comment] */
        public int overrideDpi;
        /** 0..3 = [comment]；{@link #ROT_FREE} = [comment] */
        public int rotation = ROT_FREE;

        public boolean hasSizeOverride() {
            return overrideSize != null;
        }

        public boolean hasDpiOverride() {
            return overrideDpi > 0;
        }

        /** [comment]（[comment] DPI [comment]） */
        public boolean overridden() {
            return hasSizeOverride() || hasDpiOverride();
        }
    }
}
