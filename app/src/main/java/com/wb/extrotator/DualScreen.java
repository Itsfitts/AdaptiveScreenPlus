package com.wb.extrotator;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;

/**
 * [comment]（DeviceStateManager）[comment] [comment]。
 *
 * <p>[comment]：[comment] —— [comment]（MainActivity）[comment]
 * （WidgetActionActivity）。[comment] shell [comment]（[comment]Yes「[comment]
 * [comment] Display [comment]」[comment]，[comment] 1 [comment]），[comment]。
 *
 * <p>[comment] Z Flip5 [comment]：
 * <pre>
 *   0 = CLOSED（[comment] / [comment]）      1 = TENT（[comment]）
 *   2 = HALF_OPENED（Flex [comment]）   3 = OPENED（[comment] / [comment]）
 *   4 = CONCURRENT_INNER_DEFAULT（[comment]）
 * </pre>
 * [comment]Yes {@code cmd device_state state <id>}，[comment] shell（Shizuku）[comment]，<b>[comment] root</b>；
 * {@code state reset} [comment]。[comment]Yes<b>[comment]</b>[comment]（[comment]）。
 *
 * <p>[comment]「[comment]」[comment] sleep / [comment] binder，<b>[comment]</b>。
 */
public final class DualScreen {

    private static final String TAG = "DualScreen";

    public static final int CLOSED = 0;
    public static final int TENT = 1;
    public static final int HALF_OPENED = 2;
    public static final int OPENED = 3;
    public static final int CONCURRENT = 4;

    private static final String[] NAMES = {
            "Text CLOSED", "Text TENT", "Text Flex HALF_OPENED",
            "Text OPENED", "Text CONCURRENT_INNER_DEFAULT"};
    private static final String[] SHORT = {
            "Text", "Text", "Text", "Text", "Text"};

    /** [comment]，[comment] Display [comment] */
    private static final long CONCURRENT_SETTLE_MS = 1800L;
    /** [comment] */
    private static final long COVER_RETRY_MS = 900L;
    /** [comment]，[comment]、[comment] */
    private static final long HOME_SETTLE_MS = 900L;
    /** [comment]，[comment] */
    private static final long VERIFY_MS = 350L;
    /**
     * [comment]，[comment]"[comment]"[comment]。
     * [comment] state 0 [comment] 1 [comment]，[comment]Yes[comment]。
     */
    private static final long WAKE_SETTLE_MS = 1100L;
    /** [comment] */
    private static final long WAKE_RETRY_MS = 500L;
    /** [comment] */
    private static final int WAKE_TRIES = 3;

    private DualScreen() {
    }

    public static String stateName(int id) {
        return id >= 0 && id < NAMES.length ? NAMES[id] : ("Text(" + id + ")");
    }

    public static String shortName(int id) {
        return id >= 0 && id < SHORT.length ? SHORT[id] : ("Text(" + id + ")");
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment] id [comment]"[comment]"。
     *
     * <p>[comment]Yes[comment]"[comment] / [comment]"[comment]：[comment]、Flex [comment]。
     * [comment] {@code cur == CLOSED} [comment]，[comment]Yes"[comment]"[comment] ——
     * [comment]Yes"[comment]、[comment]"[comment]。
     */
    private static boolean isCollapsed(int id) {
        return id == CLOSED || id == TENT || id == HALF_OPENED;
    }

    /**
     * [comment]<b>[comment]</b>[comment]（Committed [comment]）。
     *
     * <p>[comment] {@code cmd device_state print-state}：[comment]Yes[comment]，[comment]Yes[comment]。
     * 「[comment]」[comment]Yes[comment]，[comment]、[comment]
     * （None[comment]）[comment]。
     *
     * @return [comment] id；[comment] / [comment]Failed[comment] -1。<b>[comment]</b>
     */
    public static int readDeviceState() {
        try {
            return Integer.parseInt(ShellRunner.run("cmd device_state print-state").trim());
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * [comment]<b>[comment]</b>[comment]（[comment] Base [comment]）。
     *
     * <p>[comment] {@link #readDeviceState} [comment]：{@code cmd device_state state}（[comment]）
     * [comment] ——
     * <pre>
     *   Committed state: …identifier=1, name='TENT'    ← [comment]
     *   Base state:      …identifier=0, name='CLOSED'  ← [comment]
     *   Override state:  …identifier=1, name='TENT'
     * </pre>
     * [comment]，Yes[comment]「[comment]、[comment]Yes[comment]」[comment]
     * [comment]。
     *
     * @return [comment] id；[comment]Failed[comment] -1。<b>[comment]</b>
     */
    public static int readPhysicalState() {
        try {
            String out = ShellRunner.run("cmd device_state state");
            for (String line : out.split("\n")) {
                if (line.contains("Base state")) {
                    int id = identifierOf(line);
                    if (id >= 0) {
                        return id;
                    }
                }
            }
        } catch (Throwable ignored) {
            // [comment]，[comment] -1
        }
        return -1;
    }

    /** [comment] {@code DeviceState{identifier=3, name='OPENED', …}} [comment] identifier */
    private static int identifierOf(String line) {
        int i = line.indexOf("identifier=");
        if (i < 0) {
            return -1;
        }
        int j = i + "identifier=".length();
        int k = j;
        while (k < line.length() && Character.isDigit(line.charAt(k))) {
            k++;
        }
        try {
            return k > j ? Integer.parseInt(line.substring(j, k)) : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * [comment]，[comment] {@link Display#getState()}。
     *
     * <p>[comment] {@code getDisplays()} Yes[comment] DisplayManager [comment]。
     *
     * @return {@link Display#STATE_ON} / {@link Display#STATE_OFF} [comment]；[comment] -1
     */
    public static int readDisplayState(Context ctx, int displayId) {
        try {
            DisplayManager dm = (DisplayManager) ctx.getApplicationContext()
                    .getSystemService(Context.DISPLAY_SERVICE);
            if (dm == null) {
                return -1;
            }
            dm.getDisplays();
            Display d = dm.getDisplay(displayId);
            return d == null ? -1 : d.getState();
        } catch (Throwable t) {
            Log.w(TAG, "Text display " + displayId + " TextFailed: " + t);
            return -1;
        }
    }

    /**
     * [comment]Yes[comment]Yes「[comment]（[comment]）[comment]」—— [comment]Yes[comment]Yes[comment]。
     *
     * <p>[comment]，[comment]：
     * <ol>
     *   <li><b>[comment]</b> → [comment]（[comment]）；</li>
     *   <li>[comment]、<b>[comment]</b> → [comment]；</li>
     *   <li>[comment]（[comment] / [comment]）→ [comment]。</li>
     * </ol>
     *
     * <p>⚠ [comment] 3 [comment]：[comment] {@code getState()} [comment]Yes OFF，[comment] 1 [comment]「[comment]」
     * [comment]「[comment]」。
     *
     * <p>[comment]：[comment]（[comment]）[comment]，[comment]
     * [comment] —— [comment]Yes[comment]。
     */
    public static boolean isCoverMode(Context ctx) {
        int inner = readDisplayState(ctx, Display.DEFAULT_DISPLAY);
        if (inner == Display.STATE_ON) {
            return false;
        }
        try {
            DisplayManager dm = (DisplayManager) ctx.getApplicationContext()
                    .getSystemService(Context.DISPLAY_SERVICE);
            Display cover = dm == null ? null : DisplayUtil.coverDisplay(dm);
            if (cover != null && readDisplayState(ctx, cover.getDisplayId()) == Display.STATE_ON) {
                return true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed: " + t);
        }
        return isCollapsed(readDeviceState());
    }

    /**
     * [comment]。<b>[comment]</b>
     *
     * <p>[comment] Shizuku [comment] {@link #cmdLocal}（[comment] fork [comment]
     * {@code cmd device_state}）—— [comment]，[comment] {@code app_accessible}
     * [comment]（[comment] / [comment] / [comment]）。[comment] Shizuku，
     * [comment] {@link #appCanRequest} [comment]。
     */
    public static void setState(int state) {
        if (ShellRunner.isReady()) {
            ShellRunner.run("cmd device_state state " + state);
        } else {
            cmdLocal("state", String.valueOf(state));
        }
    }

    /**
     * [comment]<b>[comment]</b>[comment]（[comment]Yes[comment] Shizuku [comment]）。
     *
     * <p>AOSP [comment] {@code app_accessible} [comment]：[comment] / [comment] / [comment]。
     * [comment] —— [comment] Shizuku。
     */
    public static boolean appCanRequest(int state) {
        return state == OPENED || state == HALF_OPENED || state == CONCURRENT;
    }

    /** [comment]，[comment]。<b>[comment]</b> */
    public static void resetState() {
        ShellRunner.run("cmd device_state state reset");
    }

    // ------------------------------------------------- [comment] Shizuku [comment]

    /**
     * [comment]<b>[comment]</b>[comment] {@code cmd device_state …}。
     *
     * <p>[comment] Shizuku：AOSP [comment] {@code DeviceStateManagerService.requestState} [comment]Yes
     * 「[comment] {@code CONTROL_DEVICE_STATE} [comment]，<b>[comment]</b>[comment]Yes[comment]
     * {@code app_accessible} [comment]」—— {@link #OPENED} / {@link #HALF_OPENED} /
     * {@link #CONCURRENT} [comment]，{@link #CLOSED} / {@link #TENT} [comment]。[comment]
     * （app [comment]，[comment]）{@code state 1 → 3} Success、[comment] {@code -> ON}。
     *
     * <p>[comment]Yes「None[comment]」[comment]：[comment]<b>[comment]</b>[comment]，
     * None[comment]；[comment]，[comment]。
     *
     * @return [comment]（{@code print-state} [comment]）；[comment] null。<b>[comment]</b>
     */
    public static String cmdLocal(String... args) {
        Process p = null;
        try {
            String[] cmd = new String[args.length + 2];
            cmd[0] = "/system/bin/cmd";
            cmd[1] = "device_state";
            System.arraycopy(args, 0, cmd, 2, args.length);
            p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream(), "UTF-8"));
            try {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            } finally {
                r.close();
            }
            String out = sb.toString();
            int rc = p.waitFor();
            Log.i(TAG, "cmd device_state " + java.util.Arrays.toString(args)
                    + " => rc=" + rc + " out=" + out.trim());
            return out;
        } catch (Throwable t) {
            Log.w(TAG, "cmd device_state " + java.util.Arrays.toString(args) + " Failed: " + t);
            return null;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    /** [comment] Shizuku：[comment]（{@code state 3}）。<b>[comment]</b> */
    public static boolean openLocal() {
        return cmdLocal("state", String.valueOf(OPENED)) != null;
    }

    /** [comment] Shizuku：[comment]（{@code state 0}）。<b>[comment]</b> */
    public static boolean closeLocal() {
        return cmdLocal("state", String.valueOf(CLOSED)) != null;
    }

    /** [comment] Shizuku：[comment]，Failed[comment] -1。<b>[comment]</b> */
    public static int readDeviceStateLocal() {
        String out = cmdLocal("print-state");
        try {
            return Integer.parseInt(out.trim());
        } catch (Throwable t) {
            return -1;
        }
    }

    /** [comment]（[comment]）[comment] display id，[comment] -1 */
    public static int coverDisplayId(DisplayManager dm) {
        Display c = DisplayUtil.coverDisplay(dm);
        return c == null ? -1 : c.getDisplayId();
    }

    /**
     * [comment] ↔ [comment]。
     *
     * <p>[comment]Yes {@link #isCoverMode}（[comment]），<b>[comment]Yes</b>[comment]：[comment]
     * [comment]，[comment]。
     *
     * <p>[comment]：{@code cmd device_state state N} [comment]「[comment]」，
     * [comment] {@link #VERIFY_MS} [comment]，[comment]。
     *
     * <p><b>[comment] Shizuku [comment]</b>：[comment]（{@code OPENED} [comment] {@code app_accessible}）
     * [comment]；[comment] {@code CLOSED}，[comment]，[comment] ——
     * [comment] -1，[comment]（[comment] {@link #appCanRequest}）。
     *
     * @return [comment] id（[comment]「[comment]」[comment]）；[comment] -1。
     *         <b>[comment]</b>（[comment] 0.5 s）
     */
    public static int toggleFold(Context ctx) {
        boolean wasCover = isCoverMode(ctx);
        int target = wasCover ? OPENED : CLOSED;
        if (!ShellRunner.isReady() && !appCanRequest(target)) {
            Log.w(TAG, "toggleFold：Text Shizuku，Text " + stateName(target) + "，Text");
            return -1;
        }
        setState(target);

        sleep(VERIFY_MS);
        boolean nowCover = isCoverMode(ctx);
        if (nowCover == wasCover) {
            Log.w(TAG, "toggleFold Text：wasCover=" + wasCover
                    + " target=" + target + " → Text");
            setState(target);
            sleep(VERIFY_MS);
            nowCover = isCoverMode(ctx);
        }
        int result = nowCover ? CLOSED : OPENED;
        Log.i(TAG, "toggleFold wasCover=" + wasCover + " target=" + target
                + "（Text " + stateName(result) + "）");

        // ⚠ [comment]"[comment]" —— [comment]（[comment] 1 [comment]
        // [comment]，[comment] AOD）。[comment]Yes"[comment]"，
        // [comment]None[comment]（[comment]Yes"[comment]"）。[comment]。
        keepScreenOn(ctx, isCollapsed(target));
        return result;
    }

    /**
     * [comment]：[comment] → [comment] → [comment]。
     *
     * <p>[comment]：{@code cmd device_state state 4} [comment]Yes[comment]，[comment]
     * [comment] Display [comment]；[comment] {@code am start} [comment]，
     * [comment]Yes「[comment]」。
     *
     * <p><b>[comment]</b>（[comment] 2 [comment]），[comment]。
     *
     * @return [comment]YesNoSuccess[comment]
     */
    public static boolean oneKeyDual(Context ctx, DisplayManager dm) {
        if (!ShellRunner.isReady()) {
            return false;
        }
        setState(CONCURRENT);
        sleep(CONCURRENT_SETTLE_MS);

        int cover = coverDisplayId(dm);
        if (cover < 0) {
            sleep(COVER_RETRY_MS);
            cover = coverDisplayId(dm);
        }
        if (cover < 0) {
            Log.w(TAG, "Text，Text id");
            return false;
        }
        boolean ok = SecondaryLauncher.launchOwn(ctx, cover, SecondaryHomeActivity.class);
        Log.i(TAG, "oneKeyDual cover=" + cover + " ok=" + ok);
        return ok;
    }

    /**
     * [comment]：[comment] → [comment] → [comment]「[comment]」[comment]，[comment]、[comment]。
     *
     * <p>[comment] {@link #oneKeyDual} [comment]（[comment]：{@code cmd device_state state 4} [comment]Yes[comment]，
     * [comment] Display [comment]，[comment]Yes"[comment]"），
     * [comment]、[comment]（{@code fullscreen} / {@code auto_cast}，
     * [comment] {@link CastActivity#EXTRA_FULLSCREEN}）。
     *
     * <p>[comment]：[comment]，[comment] ——
     * {@link CastActivity} [comment] Shizuku [comment] {@code PROJECT_MEDIA} [comment] allow，
     * [comment]、[comment]；[comment] Shizuku [comment]Notice，[comment]。
     *
     * <p><b>[comment]</b>（[comment] 2 [comment]），[comment]。
     *
     * @return [comment]YesNoSuccess[comment]
     */
    public static boolean oneKeyCast(Context ctx, DisplayManager dm) {
        if (!ShellRunner.isReady()) {
            return false;
        }
        setState(CONCURRENT);
        sleep(CONCURRENT_SETTLE_MS);

        int cover = coverDisplayId(dm);
        if (cover < 0) {
            sleep(COVER_RETRY_MS);
            cover = coverDisplayId(dm);
        }
        if (cover < 0) {
            Log.w(TAG, "Text，Text id");
            return false;
        }
        /*
         * v4.16：[comment]，[comment]。[comment]，[comment]
         * [comment]，[comment]（[comment]）。
         */
        SecondaryLauncher.launchOwn(ctx, cover, SecondaryHomeActivity.class);
        sleep(HOME_SETTLE_MS);

        Bundle args = new Bundle();
        args.putBoolean(CastActivity.EXTRA_FULLSCREEN, true);
        args.putBoolean(CastActivity.EXTRA_AUTO_CAST, true);
        boolean ok = SecondaryLauncher.launchOwn(ctx, cover, CastActivity.class, args);
        Log.i(TAG, "oneKeyCast cover=" + cover + " ok=" + ok);
        return ok;
    }

    /**
     * [comment]，<b>[comment] / [comment]</b>。
     *
     * <p>[comment] {@link #oneKeyDual} [comment]Yes[comment] {@code setState(CONCURRENT)} ——
     * [comment]Yes「[comment]」，[comment]Yes[comment]：[comment]、
     * [comment]。
     *
     * <p>[comment]：[comment]Yes[comment]，[comment]（[comment] displayId=1
     * [comment]），[comment]。[comment]。
     *
     * <p><b>[comment]</b>（[comment] 0.5 [comment]），[comment]。
     *
     * @return [comment]YesNo[comment]
     */
    public static boolean openSecondaryHome(Context ctx, DisplayManager dm) {
        if (!ShellRunner.isReady()) {
            return false;
        }
        int cover = coverDisplayId(dm);
        if (cover < 0) {
            sleep(COVER_RETRY_MS);
            cover = coverDisplayId(dm);
        }
        if (cover < 0) {
            Log.w(TAG, "openSecondaryHome：Text id");
            return false;
        }
        boolean ok = SecondaryLauncher.launchOwn(ctx, cover, SecondaryHomeActivity.class);
        Log.i(TAG, "openSecondaryHome cover=" + cover + " ok=" + ok);
        return ok;
    }

    /**
     * [comment]「[comment]」Yes[comment] —— [comment]。
     *
     * <p>[comment]：[comment] {@link #CLOSED} [comment]「[comment]」[comment]，[comment] {@code goToSleep} ——
     * [comment] 1 [comment]、[comment] AOD、[comment] {@code Dozing}
     * （[comment]「[comment]」[comment]，[comment]Yes[comment]）。[comment]。
     *
     * <p>[comment] {@link PowerManager#isInteractive()}（[comment]），[comment]<b>[comment]Yes</b>[comment]
     * state：[comment]Yes[comment]，[comment]Yes[comment]，[comment]「[comment]」[comment]。
     *
     * @param waitForSleep true = [comment]Yes[comment]，[comment]（[comment]，
     *                     [comment]）；[comment]，[comment]
     *                     <b>[comment]</b>（[comment] 1~2.5 [comment]），[comment]
     */
    public static void keepScreenOn(Context ctx, boolean waitForSleep) {
        PowerManager pm = (PowerManager) ctx.getApplicationContext()
                .getSystemService(Context.POWER_SERVICE);
        if (waitForSleep) {
            sleep(WAKE_SETTLE_MS);
        }
        for (int i = 0; i < WAKE_TRIES; i++) {
            if (pm == null || pm.isInteractive()) {
                return;
            }
            wakeUp();
            sleep(WAKE_RETRY_MS);
        }
        Log.w(TAG, "keepScreenOn Text " + WAKE_TRIES + " Text");
    }

    /** [comment]（[comment] shell，[comment] Shizuku）。<b>[comment]</b> */
    private static void wakeUp() {
        try {
            ShellRunner.run("input keyevent KEYCODE_WAKEUP");
            Log.i(TAG, "keepScreenOn Text");
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed: " + t);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
