package com.wb.extrotator;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

/**
 * 「None[comment]」[comment] —— [comment]Yes「Shizuku [comment]」，[comment]<b>[comment] Shizuku</b>。
 *
 * <p>[comment] {@code adb_wifi_enabled=1} [comment]Yes<b>[comment]</b>：[comment] adbd [comment] TLS [comment]、[comment]
 * [comment]、<b>[comment] 0</b>，[comment]「Allow」[comment] 1。[comment]Yes[comment]：[comment] Wi-Fi
 * [comment]「[comment]Allow」[comment] ⇒ [comment]，[comment]、[comment]；[comment] ⇒ [comment]，
 * [comment] {@link AutoAuth} [comment]「[comment]Allow」[comment]「Allow」，[comment] —— [comment]，
 * [comment] 1 [comment]。
 *
 * <p>⚠ [comment] 2 [comment]（[comment] {@link AutoAuth}）；[comment]
 * {@code WRITE_SECURE_SETTINGS}（{@code pm grant} [comment]，[comment]），[comment] Shizuku [comment]
 * [comment]，[comment]。[comment]。
 */
public final class AdbWifi {

    private static final String TAG = "AdbWifi";

    /** [comment]。[comment]。 */
    public static final String KEY = "adb_wifi_enabled";

    /** Allow[comment] {@link #KEY} [comment]。development [comment]，[comment] {@code pm grant} [comment]。 */
    private static final String PERM = "android.permission.WRITE_SECURE_SETTINGS";

    /** [comment]、[comment] */
    private static final long OPEN_SETTLE_MS = 900L;

    /** [comment]（[comment] 40ms [comment]，[comment]） */
    private static final long APPLY_MS = 400L;

    /**
     * [comment]「[comment]」[comment]，[comment]<b>[comment]</b>。
     * [comment]，[comment]Yes[comment] 0、[comment]。
     */
    private static final long SETTLE_MS = 800L;

    /** [comment]「[comment]」[comment]，[comment] */
    private static final long CONFIRM_MS = 250L;

    /** [comment] {@link AutoAuth} [comment]「[comment]Allow」[comment]「Allow」[comment] */
    private static final long POLL_MS = 300L;

    /** [comment] */
    private static final int POLL_TRIES = 7;

    /** [comment]（[comment]） */
    private static final int ROUNDS = 3;

    private AdbWifi() {
    }

    /** None[comment]。[comment]Yes[comment]，[comment]，[comment]Failed[comment]"[comment]"[comment]。 */
    public static boolean isOn(Context c) {
        return readValue(c) == 1;
    }

    /** [comment] Shizuku [comment]（[comment]）。 */
    public static boolean canWriteDirect(Context c) {
        try {
            return c.getApplicationContext().checkSelfPermission(PERM)
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * [comment]。<b>[comment]</b>（[comment]），[comment]。
     * [comment]<b>[comment]</b>：[comment]Yes[comment]。
     *
     * @return null = Success；No[comment]Yes[comment]
     */
    public static String set(Context c, boolean on) {
        if (!canWriteDirect(c)) {
            // Shizuku [comment]；[comment]，[comment]
            ensureWritePermission(c);
        }
        if (!canWriteDirect(c)) {
            Log.w(TAG, "Text " + PERM + "，Text " + KEY);
            return "TextSettingsText（Text Shizuku Text）";
        }

        if (!on) {
            return apply(c, 0);
        }

        // ① [comment]：[comment] ⇒ [comment]，[comment]、[comment]。
        //   [comment]Yes"[comment]"，[comment]Yes"[comment] 1 [comment]"（[comment] confirmOn）。
        if (writeKey(c, 1) && confirmOn(c)) {
            Log.i(TAG, "NoneTextEnable（Text，Text）");
            return null;
        }

        // ② [comment] ⇒ [comment]：[comment]，[comment]None[comment]
        return unfoldRoute(c);
    }

    /**
     * [comment]：[comment] → [comment] → [comment]None[comment] → [comment]。[comment] app [comment]。
     */
    private static String unfoldRoute(Context c) {
        if (!AutoAuth.isReady()) {
            Log.w(TAG, "NoneText，Text");
            return "Text：NoneText";
        }
        Log.i(TAG, "Text Wi-Fi Text ⇒ Text");
        // [comment]，[comment]。[comment]Yes[comment]：
        // [comment]「Allow / Confirm」[comment]（[comment]）。
        AutoAuth.arm();
        if (!DualScreen.openLocal()) {
            AutoAuth.disarm();
            return "Text（cmd device_state Text）";
        }

        boolean ok = false;
        try {
            sleep(OPEN_SETTLE_MS);
            for (int r = 1; r <= ROUNDS && !ok; r++) {
                if (!writeKey(c, 1)) {
                    break;
                }
                for (int i = 0; i < POLL_TRIES && !ok; i++) {
                    sleep(POLL_MS);
                    // [comment]Yes 1 [comment]：[comment]Yes 1，
                    // [comment] 0 [comment]
                    if (readValue(c) == 1) {
                        sleep(250);
                        ok = readValue(c) == 1;
                    }
                }
                Log.i(TAG, "Text " + r + " Text：" + (ok ? "Success" : "Text"));
            }
        } finally {
            DualScreen.closeLocal();
            sleep(APPLY_MS);
            AutoAuth.disarm();
        }

        int back = readValue(c);
        if (back == 1) {
            Log.i(TAG, "NoneTextEnable（Text「TextAllow」Text）");
            return null;
        }
        return "Text（Text " + back + "）";
    }

    /**
     * [comment] {@code 1} [comment]，[comment]<b>[comment]</b>。<b>[comment]</b>。
     *
     * <p>⚠ [comment]「[comment] {@code 1} [comment]」：[comment] {@code putInt} [comment] SettingsProvider [comment]
     * [comment]Yes[comment]，[comment]；[comment]，[comment] 0 [comment] ——
     * [comment]Yes[comment]「[comment]」[comment]Success，[comment]，[comment]Success、[comment]。
     */
    private static boolean confirmOn(Context c) {
        sleep(SETTLE_MS);
        if (readValue(c) != 1) {
            return false;
        }
        sleep(CONFIRM_MS);
        return readValue(c) == 1;
    }

    /** [comment]、[comment]（[comment]/[comment]） */
    private static String apply(Context c, int v) {
        if (!writeKey(c, v)) {
            return "Text " + KEY + " Failed";
        }
        int got = waitValue(c, v, APPLY_MS + 400);
        if (got == v) {
            Log.i(TAG, "NoneText" + (v == 1 ? "TextEnable" : "TextDisable"));
            return null;
        }
        Log.w(TAG, "Text " + v + "，TextYes " + got);
        return (v == 1 ? "Text" : "TextDisable") + "（Text " + got + "）";
    }

    private static boolean writeKey(Context c, int v) {
        try {
            Settings.Global.putInt(c.getApplicationContext().getContentResolver(), KEY, v);
            Log.i(TAG, KEY + " <- " + v);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "Text " + KEY + " Failed", t);
            return false;
        }
    }

    private static int readValue(Context c) {
        try {
            return Settings.Global.getInt(c.getApplicationContext().getContentResolver(), KEY, 0);
        } catch (Throwable t) {
            Log.w(TAG, "Text " + KEY + " Failed: " + t);
            return -1;
        }
    }

    /** [comment] {@code ms} [comment]，[comment] {@code want}；[comment] */
    private static int waitValue(Context c, int want, long ms) {
        long end = System.currentTimeMillis() + ms;
        int got = readValue(c);
        while (got != want && System.currentTimeMillis() < end) {
            sleep(120);
            got = readValue(c);
        }
        return got;
    }

    /**
     * [comment] {@link #PERM}。[comment] Shizuku [comment]。
     *
     * <p>[comment]Yes[comment]，[comment]Yes[comment]，[comment] Shizuku。
     */
    private static void ensureWritePermission(Context c) {
        if (!ShellRunner.isReady()) {
            return;
        }
        String out = ShellRunner.run("pm grant " + c.getPackageName() + " " + PERM);
        Log.i(TAG, "Text pm grant => " + out);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
