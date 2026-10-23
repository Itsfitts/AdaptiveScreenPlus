package com.wb.extrotator;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * [comment]（[comment]）[comment] —— [comment]。
 *
 * <p><b>[comment] id [comment]Yes[comment]</b>（[comment]）：[comment]，[comment] ——
 * [comment] id（displayId，0/1/2…）[comment]<b>[comment]</b>（{@code input -d 2 tap 100 200}），
 * [comment] id（64 [comment]）[comment]<b>[comment]</b>（{@code screencap -p -d 4630947181303254916}）。
 * ⚠ [comment]：{@code screencap -d 1} [comment]（「Display Id '1' is not valid」），[comment]。
 * {@code dumpsys display} [comment]（{@code displayId N} [comment]
 * {@code uniqueId local:<[comment]id>}），{@link #physicalIdOf} [comment]Yes[comment]。
 *
 * <p><b>[comment]</b>：[comment]（Z Flip5）[comment]Yes[comment]<b>[comment]</b>[comment]，
 * [comment]（{@link Display#DEFAULT_DISPLAY}）[comment]，[comment] ——
 * [comment]，[comment]「[comment]」[comment]，[comment]（[comment]）。
 * [comment] id 0 = [comment]、{@link #COVER_ID} = 1 = [comment]。
 *
 * <p><b>[comment]Yes[comment]</b>：[comment] Binder [comment] shell [comment]，[comment]。
 * [comment]Yes {@link #exec(String, Consumer)}。
 */
public final class ExtScreen {

    private static final String TAG = "ExtScreen";

    /** [comment]（[comment]）[comment] displayId —— [comment] */
    public static final int INNER_ID = Display.DEFAULT_DISPLAY;

    /** [comment]（[comment]）[comment] displayId —— [comment] */
    public static final int COVER_ID = 1;

    /**
     * [comment]。
     * 1080p [comment] PNG [comment] KB，[comment] 24MB Yes[comment]"[comment]None[comment]"，
     * [comment]Yes[comment]。
     */
    private static final int FRAME_MAX_BYTES = 24 * 1024 * 1024;

    /**
     * [comment]。
     *
     * <p>[comment]：[comment] shell [comment]Yes[comment]、[comment]，
     * [comment]"[comment]"[comment] —— [comment]。
     * ⚠ [comment]**[comment]**[comment]（[comment] screencap [comment]，
     * [comment]），[comment] {@link TouchpadActivity} [comment]。
     */
    private static final ExecutorService IO =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "extrot-ext-io");
                t.setDaemon(true);
                return t;
            });

    private ExtScreen() {
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment] */
    public static final class Dev {
        /** [comment] id —— `input -d` [comment] */
        public final int id;
        /** [comment] id —— `screencap -d` [comment]；-1 = [comment]（[comment]） */
        public final long physicalId;
        public final String name;
        /** [comment]（[comment]Yes[comment]） */
        public final int w;
        public final int h;
        /** Yes[comment]Yes[comment] */
        public final boolean builtin;
        /**
         * [comment]Yes[comment]Yes[comment]（{@code STATE_ON}）。
         *
         * <p>v3.7 [comment]，[comment]：[comment]/[comment]<b>[comment]</b>
         * （[comment]，InputDispatcher [comment]），[comment]"[comment]"，
         * [comment]"[comment]Yes[comment]"。[comment]Yes[comment] DisplayManager [comment]，[comment] shell。
         */
        public final boolean on;

        Dev(int id, long physicalId, String name, int w, int h, boolean builtin, boolean on) {
            this.id = id;
            this.physicalId = physicalId;
            this.name = name;
            this.w = w;
            this.h = h;
            this.builtin = builtin;
            this.on = on;
        }

        /** [comment] —— [comment] */
        public boolean awake() {
            return on;
        }

        public boolean isExternal() {
            return !builtin;
        }

        /**
         * [comment] —— [comment]。
         * ⚠ [comment] {@link #name}：[comment]「[comment]」，[comment]。
         */
        public int labelRes() {
            if (!builtin) {
                return R.string.dev_external;
            }
            return id == COVER_ID ? R.string.dev_cover : R.string.dev_inner;
        }

        public String sizeText() {
            return w + "×" + h;
        }
    }

    /**
     * [comment]（{@link #find} [comment]；[comment]）。
     *
     * <p>⚠ <b>[comment] {@code dm.getDisplays()} [comment]</b>：[comment]（Z Flip5）[comment]，
     * [comment]（id 1）[comment]；[comment]。
     * {@link DisplayUtil#allDisplays(DisplayManager)} [comment] id [comment] {@code getDisplay()} [comment]，
     * [comment]Yes[comment]。
     */
    public static List<Dev> all(Context ctx) {
        List<Dev> out = new ArrayList<>();
        DisplayManager dm = dm(ctx);
        if (dm == null) {
            return out;
        }
        Map<Integer, Long> phys = physicalIdOf(ctx);
        List<Display> ds;
        try {
            ds = DisplayUtil.allDisplays(dm);
        } catch (Throwable t) {
            Log.w(TAG, "allDisplays Failed: " + t);
            return out;
        }
        if (ds == null || ds.isEmpty()) {
            return out;
        }
        for (Display d : ds) {
            if (d == null || !d.isValid()) {
                continue;
            }
            int id = d.getDisplayId();
            int[] sz = sizeOf(d);
            int w = sz[0], h = sz[1];
            Long pid = phys.get(id);
            String name;
            try {
                name = String.valueOf(d.getName());
            } catch (Throwable t) {
                name = "?";
            }
            Log.i(TAG, "Text " + id + " [" + name + "] realMetrics=" + w + "x" + h
                    + " realSize=" + rect(d, false)
                    + " mode=" + modeText(d)
                    + " metrics=" + rect(d, true));
            boolean lit;
            try {
                lit = d.getState() == Display.STATE_ON;
            } catch (Throwable t) {
                lit = true;   // Text，Text"Text"Text
            }
            out.add(new Dev(id, pid == null ? -1L : pid, name, w, h, isBuiltin(id), lit));
        }
        return out;
    }

    /**
     * [comment]。
     *
     * <p>⚠ [comment]：<b>[comment] {@code getMode()}</b>，[comment] {@code getRealMetrics()}。
     * [comment]，{@code getRealSize()} / {@code getRealMetrics()} [comment]
     * 1496×1440，[comment]Yes 1080×2640 —— [comment] API [comment]Yes
     * {@code DisplayInfo.getAppMetrics(…, mDisplayAdjustments)}，[comment]<b>[comment]</b>[comment]，
     * [comment]「[comment]」[comment]。{@code getMode()} [comment]Yes[comment]，[comment]，[comment]。
     *
     * <p>[comment]Yes[comment] {@code wm size} [comment]，[comment] —— [comment]
     * [comment]，[comment] {@link CursorOverlay#bounds()}（[comment]）：
     * [comment] {@code lp.x/lp.y}、{@code input -d N} [comment]、[comment]，[comment]。
     */
    private static int[] sizeOf(Display d) {
        try {
            Display.Mode m = d.getMode();
            if (m != null && m.getPhysicalWidth() > 0 && m.getPhysicalHeight() > 0) {
                return new int[]{m.getPhysicalWidth(), m.getPhysicalHeight()};
            }
        } catch (Throwable ignored) {
        }
        try {
            DisplayMetrics dm = new DisplayMetrics();
            d.getRealMetrics(dm);
            return new int[]{dm.widthPixels, dm.heightPixels};
        } catch (Throwable ignored) {
        }
        return new int[]{0, 0};
    }

    /** [comment]：`getRealSize()`（app=false）[comment] `getMetrics()`（app=true）[comment] */
    private static String rect(Display d, boolean app) {
        try {
            android.graphics.Point p = new android.graphics.Point();
            if (app) {
                DisplayMetrics m = new DisplayMetrics();
                d.getMetrics(m);
                return m.widthPixels + "x" + m.heightPixels;
            }
            d.getRealSize(p);
            return p.x + "x" + p.y;
        } catch (Throwable t) {
            return "?";
        }
    }

    /** [comment]：`getMode()` [comment] —— [comment] wm [comment] / [comment] */
    private static String modeText(Display d) {
        try {
            Display.Mode m = d.getMode();
            return m == null ? "?" : m.getPhysicalWidth() + "x" + m.getPhysicalHeight();
        } catch (Throwable t) {
            return "?";
        }
    }

    /** [comment]（[comment] 0 / [comment] 1）；[comment] */
    private static boolean isBuiltin(int id) {
        return id == Display.DEFAULT_DISPLAY || id == COVER_ID;
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]<b>[comment]</b>[comment] —— [comment]Yes {@code input -d N tap x y} [comment]。
     *
     * <p>[comment]Yes WindowManager：{@code dumpsys window displays} [comment]
     * {@code cur=WxH}（[comment] {@code wm size} [comment]），[comment] {@code screencap} [comment]
     * [comment]。
     *
     * <p>⚠ <b>[comment] Display [comment] API</b>，[comment]Yes[comment]：
     * {@code getRealSize()} / {@code getRealMetrics()} / {@code getMetrics()} [comment]<b>[comment]
     * [comment]</b>[comment]（[comment] 1920×1080 [comment] 1496×1242，[comment] 1495 [comment] ⇒
     * [comment]「[comment]」）；{@code getMode()} [comment]（1080×2640），
     * [comment] {@code wm size} [comment]。
     *
     * @return {w, h}；[comment] null。<b>[comment]</b>，[comment]
     */
    public static int[] injectionSize(Context ctx, int displayId) {
        if (!ShellRunner.isReady()) {
            return null;
        }
        return parseInjectionSize(ShellRunner.run("dumpsys window displays"), displayId);
    }

    /**
     * [comment] {@code dumpsys window displays} [comment] {@code cur=WxH}。
     *
     * <p>[comment]，[comment]（[comment] {@link CursorOverlay#show}）。[comment]Yes
     * 「[comment]」，[comment] {@code mDisplayId=N} [comment]，[comment] {@code cur=}。
     */
    public static int[] parseInjectionSize(String dump, int displayId) {
        if (dump == null || dump.startsWith("ERR")) {
            return null;
        }
        String key = "mDisplayId=" + displayId;
        int i = dump.indexOf(key);
        if (i < 0) {
            return null;
        }
        int j = dump.indexOf("mDisplayId=", i + key.length());
        String seg = j > i ? dump.substring(i, j) : dump.substring(i);
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("cur=(\\d+)x(\\d+)").matcher(seg);
        if (!m.find()) {
            return null;
        }
        try {
            int w = Integer.parseInt(m.group(1));
            int h = Integer.parseInt(m.group(2));
            return (w > 0 && h > 0) ? new int[]{w, h} : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * [comment] —— <b>[comment]Yes[comment]</b>（{@link Display#DEFAULT_DISPLAY} = 0）。
     *
     * <p>[comment]：[comment] —— [comment]Yes[comment]，[comment]
     * [comment]，[comment]。[comment]，「[comment]」[comment]
     * （[comment]，[comment]）。
     *
     * @return [comment]；[comment] null（[comment]Yes Shizuku [comment]，[comment]Notice）
     */
    public static Dev find(Context ctx) {
        List<Dev> list = all(ctx);
        for (Dev d : list) {
            if (d.id == Display.DEFAULT_DISPLAY) {
                return d;
            }
        }
        Log.w(TAG, "Text，Text " + list.size() + " Text");
        return null;
    }

    private static DisplayManager dm(Context ctx) {
        try {
            return (DisplayManager) ctx.getApplicationContext()
                    .getSystemService(Context.DISPLAY_SERVICE);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * [comment] id → [comment] id。
     *
     * <p>[comment] `mBaseDisplayInfo=DisplayInfo{…}` [comment]：[comment] `displayId N`
     * [comment] `uniqueId "local:<[comment]id>"`。[comment] dumpsys [comment] KB，[comment]，
     * [comment] —— [comment]，[comment]。
     */
    public static Map<Integer, Long> physicalIdOf(Context ctx) {
        Map<Integer, Long> map = new HashMap<>();
        if (!ShellRunner.isReady()) {
            return map;
        }
        String dump = ShellRunner.run("dumpsys display");
        if (dump == null || dump.startsWith("ERR")) {
            return map;
        }
        for (String line : dump.split("\n")) {
            if (!line.contains("mBaseDisplayInfo=")) {
                continue;
            }
            int id = intAfter(line, "displayId ");
            long pid = longAfter(line, "uniqueId \"local:");
            if (id >= 0 && pid > 0) {
                map.put(id, pid);
            }
        }
        Log.i(TAG, "Text→Text: " + map);
        return map;
    }

    private static int intAfter(String s, String key) {
        int i = s.indexOf(key);
        if (i < 0) {
            return -1;
        }
        int j = i + key.length();
        int k = j;
        while (k < s.length() && Character.isDigit(s.charAt(k))) {
            k++;
        }
        try {
            return k > j ? Integer.parseInt(s.substring(j, k)) : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static long longAfter(String s, String key) {
        int i = s.indexOf(key);
        if (i < 0) {
            return -1;
        }
        int j = i + key.length();
        int k = j;
        while (k < s.length() && Character.isDigit(s.charAt(k))) {
            k++;
        }
        try {
            return k > j ? Long.parseLong(s.substring(j, k)) : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment] shell [comment]（[comment]，[comment]）。
     *
     * <p>[comment]：[comment] shell [comment] + `input` [comment] JVM [comment]，[comment]。
     * [comment]Yes[comment]，[comment] {@link #IO}。
     */
    public static void exec(String cmd) {
        exec(cmd, null);
    }

    public static void exec(String cmd, Consumer<String> done) {
        exec(cmd, 0, done);
    }

    /**
     * [comment] {@link #exec}（0 = [comment]）。
     *
     * <p>[comment] {@link ShellRunner#run(String, int)} [comment] ——
     * [comment]、[comment]。[comment]，[comment] shell [comment] {@code timeout} [comment]，
     * [comment]、[comment]，[comment]。
     */
    public static void exec(String cmd, int timeoutSec, Consumer<String> done) {
        IO.execute(() -> {
            String out = ShellRunner.run(cmd, timeoutSec);
            if (done != null) {
                done.accept(out == null ? "" : out);
            }
        });
    }

    /**
     * [comment] —— <b>[comment]</b>。
     *
     * <p>[comment]：{@code input} [comment]Yes「[comment]」[comment]，[comment]<b>[comment]</b>[comment]
     * InputDispatcher [comment]，[comment] 5~8 [comment]。[comment] {@link #IO} [comment] ——
     * [comment]，[comment]，[comment]Yes「[comment]」「[comment]」。[comment]：
     * [comment]Yes[comment]（[comment]），[comment]<b>[comment]</b>。
     *
     * <p>⚠ [comment]。{@code dumpsys} [comment] 1~3 [comment]（[comment] KB [comment]）、[comment]，[comment]。
     *
     * @param timeoutSec [comment]；[comment] 2 [comment]（[comment] 40~65ms）
     */
    public static void execBounded(String cmd, int timeoutSec) {
        execBounded(cmd, timeoutSec, null);
    }

    /** [comment] + [comment]（[comment]） */
    public static void execBounded(String cmd, int timeoutSec, Consumer<String> done) {
        // [comment] shell [comment] timeout [comment] —— [comment]None[comment]（[comment] ShellRunner.run(cmd,n)）：
        // [comment] input [comment]，[comment] 20 [comment]。
        // [comment]，[comment]（"sleep 0.7; input ... UP"）[comment]。
        exec(cmd, timeoutSec, done);
    }

    /**
     * [comment]（[comment]）。
     *
     * <p>[comment]None[comment]（{@link A11yInject}）—— [comment]、[comment]、[comment]；
     * [comment]None[comment] {@code input}。
     */
    public static void tap(int displayId, int x, int y) {
        if (A11yInject.tap(displayId, x, y)) {
            return;
        }
        execBounded("input -d " + displayId + " tap " + x + " " + y, 1);
    }

    /**
     * [comment] / [comment]。
     *
     * <p>⚠ [comment]**[comment]**[comment]：[comment]Yes[comment]，[comment]。
     * [comment] {@link #dragStart} [comment]（[comment]）。
     */
    public static void swipe(int displayId, int x1, int y1, int x2, int y2, int ms) {
        if (A11yInject.swipe(displayId, x1, y1, x2, y2, ms)) {
            return;
        }
        // [comment]：swipe [comment] ms [comment]，
        // [comment] 1 [comment]（[comment]，[comment]）。
        execBounded("input -d " + displayId + " swipe " + x1 + " " + y1
                + " " + x2 + " " + y2 + " " + ms, ms / 1000 + 1);
    }

    /**
     * [comment]。
     *
     * <p>`input swipe` [comment]（[comment] MOVE，[comment] MOVE [comment] touch slop）；
     * [comment]Yes[comment] `input motionevent`：DOWN → [comment] → UP。
     * v3.7 [comment]：[comment] ms [comment]Yes"[comment] ms [comment]"。
     */
    public static void hold(int displayId, int x, int y, int ms) {
        if (A11yInject.longPress(displayId, x, y, ms)) {
            return;
        }
        // [comment] 0.4s：[comment]，[comment]。
        float sec = Math.max(0.4f, ms / 1000f);
        execBounded("input -d " + displayId + " motionevent DOWN " + x + " " + y
                + "; sleep " + sec + "; "
                + "input -d " + displayId + " motionevent UP " + x + " " + y, (int) Math.ceil(sec) + 2);
    }

    // ------------------------------------------------------------------ [comment]（v3.7 [comment]）

    /**
     * [comment] —— [comment]？
     *
     * <p>true = [comment] {@code willContinue} [comment]，[comment] {@link #dragTo}、
     * [comment] {@link #dragEnd}，<b>[comment] DOWN/MOVE/UP</b>，[comment]
     * （[comment]「[comment]」）。
     *
     * <p>false = [comment]，[comment] {@link #motionDown}/{@link #motionMove}/
     * {@link #motionUpAfter} [comment]。
     */
    public static boolean dragStart(int displayId, int x, int y) {
        return A11yInject.dragStart(displayId, x, y);
    }

    /** [comment] (x,y) */
    public static boolean dragTo(int displayId, int x, int y) {
        return A11yInject.dragTo(displayId, x, y);
    }

    /** [comment] */
    public static boolean dragEnd(int displayId, int x, int y) {
        return A11yInject.dragEnd(displayId, x, y);
    }

    /**
     * [comment]（[comment] / [comment]）。
     *
     * <p>[comment] —— [comment]，[comment]"[comment]"，
     * [comment]。
     */
    public static void dragAbort() {
        A11yInject.abort();
    }

    /**
     * [comment]（DOWN / MOVE / UP）—— [comment]。
     *
     * <p>⚠ <b>[comment] {@link #swipe} [comment]</b>：{@code input swipe} Yes「[comment]」，
     * [comment] touch slop，[comment]Cancel ⇒ [comment]。
     *
     * <p>⚠⚠ <b>[comment] shell [comment]</b>，[comment]。[comment]Yes
     * 「[comment]」[comment] —— [comment] DOWN [comment] MOVE [comment]，
     * [comment]（[comment]，[comment] {@code Dropping ... no touch gesture} [comment]）。
     * [comment]，MOVE [comment]，[comment]「[comment]」。
     */
    public static void motion(int displayId, String action, int x, int y) {
        execBounded("input -d " + displayId + " motionevent " + action + " " + x + " " + y, 1);
    }

    public static void motionDown(int displayId, int x, int y) {
        motion(displayId, "DOWN", x, y);
    }

    public static void motionMove(int displayId, int x, int y) {
        motion(displayId, "MOVE", x, y);
    }

    public static void motionUp(int displayId, int x, int y) {
        motion(displayId, "UP", x, y);
    }

    /**
     * [comment] —— [comment]。
     *
     * <p>⚠ [comment] shell [comment]（{@code sleep N; input ... UP}），[comment] {@code View.postDelayed}：
     * [comment]，[comment]（[comment]「Disable」、[comment] onStop）[comment]，
     * UP [comment] —— [comment]「[comment]」，[comment]。
     * [comment] shell [comment] Activity [comment]。
     */
    public static void motionUpAfter(int displayId, int x, int y, long delayMs) {
        if (delayMs <= 0) {
            motionUp(displayId, x, y);
            return;
        }
        int sec = (int) Math.ceil(delayMs / 1000f) + 2;
        execBounded("sleep " + (delayMs / 1000f)
                + "; input -d " + displayId + " motionevent UP " + x + " " + y, sec);
    }

    /** [comment]（[comment] KEYCODE_ [comment]，[comment]） */
    public static void key(int displayId, String keyCodeName) {
        execBounded("input -d " + displayId + " keyevent " + keyCodeName, 1);
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * `input text` [comment]。
     *
     * <p>⚠ [comment]**[comment]**（ASCII）—— [comment]，[comment]Yes `input` [comment]
     * [comment]（[comment] KeyCharacterMap [comment] KeyEvent）。[comment]
     * {@link #paste(int, String)}。
     *
     * <p>[comment]（[comment] shell [comment]），[comment]"[comment] text + [comment]"。
     */
    public static void typeText(int displayId, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        // [comment]Yes[comment] `text` [comment]，[comment] shell [comment] —— [comment]，
        // [comment]，[comment]。
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                sb.append("input -d ").append(displayId)
                        .append(" text '").append(lines[i].replace("'", "'\\''")).append("'; ");
            }
            if (i < lines.length - 1) {
                sb.append("input -d ").append(displayId).append(" keyevent KEYCODE_ENTER; ");
            }
        }
        String cmd = sb.toString().trim();
        if (!cmd.isEmpty()) {
            exec(cmd);
        }
    }

    /**
     * [comment] + [comment] —— [comment]、emoji、[comment]。
     *
     * <p>[comment]Yes**[comment]**[comment]：[comment]，[comment]Yes[comment]。
     * ⚠ Android 10 [comment]"[comment]"[comment]，[comment]Yes[comment]
     * <b>[comment]</b>；No[comment]。
     */
    public static void paste(int displayId, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        exec("input -d " + displayId + " keyevent KEYCODE_PASTE");
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]（PNG [comment]）。
     *
     * @param physicalId {@link Dev#physicalId}，[comment]Yes[comment] id
     * @return PNG [comment]；Failed[comment] null。**[comment]**
     */
    public static byte[] frame(long physicalId) {
        if (physicalId <= 0) {
            return null;
        }
        byte[] b = ShellRunner.capture("screencap -p -d " + physicalId, FRAME_MAX_BYTES);
        if (b == null || b.length < 1024) {
            return null;
        }
        return b;
    }
}
