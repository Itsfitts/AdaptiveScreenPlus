package com.wb.extrotator;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * [comment]「[comment]」[comment]（v4.22 [comment]）。
 *
 * <p>[comment]Yes[comment]：WiFi / [comment] / [comment] / [comment] / [comment] / [comment] / [comment]，
 * [comment]「[comment]」（{@link CoverAutoRot}，[comment]）。
 *
 * <p><b>[comment]Yes[comment]</b>（v4.23 [comment]）：Default[comment] {@link #ORDER}，[comment]「＋」[comment]
 * [comment]、[comment]、[comment]，[comment] {@link #list}/{@link #save} [comment]。
 *
 * <p>[comment]：[comment]、[comment]Yes[comment]、[comment]Yes[comment]Yes[comment]、[comment]。
 *
 * <p><b>[comment]</b>：[comment]Yes[comment]，[comment] ——
 * [comment]Yes[comment]Settings[comment]（[comment] WRITE_SECURE_SETTINGS，[comment]）；[comment] CameraManager
 * （[comment]）；[comment]（WiFi / [comment] / [comment] / [comment] / [comment]）[comment] Shizuku [comment] shell，
 * [comment]。
 *
 * <p>⚠ [comment] {@code Context} [comment]Yes[comment] —— {@link #toggle} [comment]<b>[comment]</b>
 * （shell [comment]，[comment]），[comment]，
 * [comment]。
 *
 * <p>⚠ [comment] v4.21 [comment]：[comment]。
 * [comment]<b>[comment]</b>，[comment]Yes[comment]，[comment] ——
 * [comment]Yes"[comment]"。
 */
public final class QuickSettings {

    private static final String TAG = "QuickSettings";

    public static final int WIFI = 0;
    public static final int DATA = 1;
    public static final int BLUETOOTH = 2;
    public static final int AUTOROTATE = 3;
    public static final int FLASH = 4;
    public static final int DND = 5;
    public static final int AIRPLANE = 6;
    /**
     * [comment]：[comment]。
     *
     * <p>[comment] {@link #AUTOROTATE} Yes<b>[comment]</b>：[comment]Yes[comment]，[comment]Default[comment]（[comment]）；
     * [comment]Yes[comment]，[comment] + shell [comment]（[comment] {@link CoverAutoRot}）。
     */
    public static final int COVER_ROT = 7;

    /**
     * [comment]Yes<b>[comment]</b>：[comment]，[comment]"[comment]Yes[comment]Yes[comment]"[comment]，
     * [comment] {@link #isOn} [comment] false（[comment]），[comment] {@link #isAction}。
     */
    /** [comment]（[comment] / [comment]，[comment] {@link DualScreen#toggleFold}） */
    public static final int ACTION_FOLD = 8;
    /** [comment]（{@link CastService}） */
    public static final int ACTION_CAST = 9;
    /** [comment] —— [comment] */
    public static final int ACTION_RECENTS = 10;

    /**
     * Default[comment]（[comment]）。
     *
     * <p>⚠ [comment]Yes<b>[comment]</b>[comment]，[comment]Yes {@link #COVER_ROT}（[comment]），
     * [comment]Yes {@link #AUTOROTATE}（[comment]Default[comment] = [comment]）—— [comment]，
     * [comment]：「[comment]」。
     */
    public static final int[] ORDER = {
            WIFI, DATA, BLUETOOTH, COVER_ROT, FLASH, DND, AIRPLANE
    };

    /** [comment] —— [comment]「＋」[comment]Yes[comment] */
    public static final int[] ALL_IDS = {
            WIFI, DATA, BLUETOOTH, COVER_ROT, FLASH, DND, AIRPLANE,
            ACTION_FOLD, ACTION_CAST, ACTION_RECENTS
    };

    private static final String SP = "cover_qs";
    private static final String K_LIST = "qs_list";

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(SP, Context.MODE_PRIVATE);
    }

    /**
     * [comment]、[comment]。
     *
     * <p>⚠ [comment]「[comment]」[comment]「[comment]」：[comment]Default[comment]，[comment]Yes[comment] ——
     * [comment] {@code contains} [comment]，[comment]Yes[comment]。
     *
     * <p>[comment] id（[comment]）[comment]，[comment]。
     */
    public static List<Integer> list(Context c) {
        List<Integer> out = new ArrayList<>();
        String s = null;
        try {
            SharedPreferences p = sp(c);
            s = p.contains(K_LIST) ? p.getString(K_LIST, "") : null;
        } catch (Throwable ignored) {
        }
        if (s == null) {
            for (int id : ORDER) {
                out.add(id);
            }
            return out;
        }
        for (String one : s.split(",")) {
            try {
                int id = Integer.parseInt(one.trim());
                if (known(id) && !out.contains(id)) {
                    out.add(id);
                }
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    public static boolean has(Context c, int id) {
        return list(c).contains(id);
    }

    private static void save(Context c, List<Integer> v) {
        StringBuilder sb = new StringBuilder();
        for (int id : v) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id);
        }
        try {
            sp(c).edit().putString(K_LIST, sb.toString()).apply();
        } catch (Throwable ignored) {
        }
    }

    /** [comment]；[comment]Success（[comment]） */
    public static void add(Context c, int id) {
        List<Integer> v = list(c);
        if (!v.contains(id)) {
            v.add(id);
            save(c, v);
        }
    }

    public static void remove(Context c, int id) {
        List<Integer> v = list(c);
        if (v.remove(Integer.valueOf(id))) {
            save(c, v);
        }
    }

    /** [comment]；[comment]/[comment] */
    public static void move(Context c, int id, int dir) {
        List<Integer> v = list(c);
        int i = v.indexOf(id);
        int j = i + (dir < 0 ? -1 : 1);
        if (i < 0 || j < 0 || j >= v.size()) {
            return;
        }
        v.set(i, v.get(j));
        v.set(j, id);
        save(c, v);
    }

    /** [comment]Default[comment] */
    public static void reset(Context c) {
        save(c, new ArrayList<Integer>() {
            {
                for (int id : ORDER) {
                    add(id);
                }
            }
        });
    }

    private static boolean known(int id) {
        for (int k : ALL_IDS) {
            if (k == id) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ [comment]

    public static String name(int id) {
        switch (id) {
            case WIFI:
                return "WiFi";
            case DATA:
                return "Text";
            case BLUETOOTH:
                return "Text";
            case AUTOROTATE:
                return "Text";
            case FLASH:
                return "Text";
            case DND:
                return "Text";
            case AIRPLANE:
                return "Text";
            case COVER_ROT:
                return "Text";
            case ACTION_FOLD:
                return "Text";
            case ACTION_CAST:
                return "Text";
            case ACTION_RECENTS:
                return "Text";
            default:
                return "";
        }
    }

    public static int icon(int id) {
        switch (id) {
            case WIFI:
                return R.drawable.ic_qs_wifi;
            case DATA:
                return R.drawable.ic_qs_data;
            case BLUETOOTH:
                return R.drawable.ic_qs_bt;
            case AUTOROTATE:
                return R.drawable.ic_qs_rotate;
            case FLASH:
                return R.drawable.ic_qs_flash;
            case DND:
                return R.drawable.ic_qs_dnd;
            case AIRPLANE:
                return R.drawable.ic_qs_airplane;
            case COVER_ROT:
                return R.drawable.ic_qs_screen_rotate;
            case ACTION_FOLD:
                return R.drawable.ic_qs_fold;
            case ACTION_CAST:
                return R.drawable.ic_qs_cast;
            case ACTION_RECENTS:
                return R.drawable.ic_qs_recents;
            default:
                return R.drawable.ic_qs_wifi;
        }
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment]Yes[comment]。[comment]"[comment]"，[comment]。 */
    public static boolean isOn(Context c, int id) {
        try {
            switch (id) {
                case AUTOROTATE:
                    return Settings.System.getInt(c.getContentResolver(),
                            S_ACCEL, 0) == 1;
                case WIFI:
                    return Settings.Global.getInt(c.getContentResolver(), S_WIFI_ON, 0) != 0;
                case DATA:
                    return Settings.Global.getInt(c.getContentResolver(), S_MOBILE_DATA, 0) != 0;
                case BLUETOOTH:
                    return Settings.Global.getInt(c.getContentResolver(), S_BT_ON, 0) != 0;
                case AIRPLANE:
                    return Settings.Global.getInt(c.getContentResolver(), S_AIRPLANE, 0) != 0;
                case FLASH:
                    // [comment] —— [comment]Yes"[comment]"，
                    // [comment]，[comment]。
                    torchReady(c);
                    return torchOn;
                case DND:
                    return dndOn(c);
                case COVER_ROT:
                    return CoverAutoRot.on(c);
                default:
                    return false;
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed " + name(id), t);
            return false;
        }
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]。
     *
     * @return null = [comment]；No[comment]Yes[comment]（[comment]）
     */
    public static String toggle(Context c, int id) {
        try {
            switch (id) {
                case AUTOROTATE:
                    return toggleAccel(c);
                case WIFI:
                    return onOff(c, id, "svc wifi ");
                case DATA:
                    return onOff(c, id, "svc data ");
                case BLUETOOTH:
                    // [comment]：[comment]
                    return onOff(c, id, "svc bluetooth ", "cmd bluetooth_manager ");
                case AIRPLANE:
                    return toggleAirplane(c);
                case FLASH:
                    return toggleTorch(c);
                case DND:
                    return toggleDnd(c);
                case COVER_ROT:
                    // [comment]/[comment]（[comment]、[comment]）
                    return CoverAutoRot.toggle(c);
                case ACTION_FOLD:
                    return QuickActions.run(c, QuickActions.KEY_TOGGLE_FOLD);
                case ACTION_CAST:
                    return QuickActions.run(c, QuickActions.KEY_CAST);
                case ACTION_RECENTS:
                    return openRecents(c);
                default:
                    return "Text";
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed " + name(id), t);
            return "Text";
        }
    }

    /**
     * [comment]Yes[comment]Yes「[comment]」[comment]<b>[comment]</b>（[comment]Yes[comment]）。
     *
     * <p>[comment] —— [comment]"[comment]"[comment]，
     * [comment] {@link #isOn} [comment]Yes[comment]，[comment]。
     */
    public static boolean isAction(int id) {
        return id == ACTION_FOLD || id == ACTION_CAST || id == ACTION_RECENTS;
    }

    /**
     * [comment]：[comment]「[comment]」。
     *
     * <p>[comment]、[comment]Yes[comment]
     * （{@link CoverRecentsGesture#openRecentsNow}）—— [comment]Yes[comment]，
     * [comment]，[comment]<b>[comment] Shizuku</b>（[comment]）。
     */
    private static String openRecents(Context c) {
        int did = CoverDisplay.id(c);
        if (did <= 0) {
            return "Text";
        }
        CoverRecentsGesture.openRecentsNow(did);
        return null;
    }

    // ------------------------------------------------------------------ [comment]

    private static final String S_ACCEL = "accelerometer_rotation";
    private static final String S_WIFI_ON = "wifi_on";
    private static final String S_MOBILE_DATA = "mobile_data";
    private static final String S_BT_ON = "bluetooth_on";
    private static final String S_AIRPLANE = "airplane_mode_on";

    /**
     * [comment] —— [comment] Shizuku [comment]：[comment]Yes[comment] Settings.System [comment]，
     * [comment] WRITE_SECURE_SETTINGS（「[comment]Settings[comment]」[comment]），[comment]。
     * [comment]（[comment]）[comment] shell。
     */
    private static String toggleAccel(Context c) {
        final int v = isOn(c, AUTOROTATE) ? 0 : 1;
        try {
            Settings.System.putInt(c.getContentResolver(), S_ACCEL, v);
        } catch (Throwable t) {
            Log.w(TAG, "Text accelerometer_rotation Failed，Text shell：" + t);
        }
        // [comment]：[comment]（[comment]）[comment] putInt [comment]Yes"[comment]"[comment]，
        // [comment]"[comment]"[comment]Success。
        if (isOn(c, AUTOROTATE) == (v == 1)) {
            return null;
        }
        String err = shell("settings put system " + S_ACCEL + " " + v);
        return err != null ? err : (isOn(c, AUTOROTATE) == (v == 1) ? null : "Text");
    }

    /**
     * WiFi / [comment] / [comment] [comment]：[comment] {@code enable|disable} [comment] + [comment]。
     *
     * <p>[comment]Yes[comment] —— [comment] One UI [comment] shell [comment]
     * （[comment] {@code svc bluetooth} [comment] {@code cmd bluetooth_manager} [comment]），
     * [comment]。
     */
    private static String onOff(Context c, int id, String... prefixes) {
        final boolean cur = isOn(c, id);
        String last = "Text";
        for (String base : prefixes) {
            String err = shell(base + (cur ? "disable" : "enable"));
            if (isOn(c, id) != cur) {
                return null;
            }
            last = err != null ? err : "Text";
        }
        return last;
    }

    /**
     * [comment]。
     *
     * <p>[comment] {@code cmd connectivity airplane-mode}（Android 11 [comment]）；[comment]
     * [comment] —— [comment] {@code Settings.Global} [comment]（[comment]，
     * [comment]，[comment]Yes[comment]）。
     */
    private static String toggleAirplane(Context c) {
        final boolean cur = isOn(c, AIRPLANE);
        final boolean want = !cur;
        String err = shell("cmd connectivity airplane-mode " + (want ? "enable" : "disable"));
        if (err == null && isOn(c, AIRPLANE) == want) {
            return null;
        }
        try {
            Settings.Global.putInt(c.getContentResolver(), S_AIRPLANE, want ? 1 : 0);
        } catch (Throwable t) {
            Log.w(TAG, "Text airplane_mode_on Failed：" + t);
        }
        shell("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state " + want);
        if (isOn(c, AIRPLANE) == want) {
            return null;
        }
        return err != null ? err : "Text";
    }

    /** [comment]：[comment]，[comment]「[comment]」[comment] —— [comment] shell [comment] */
    private static boolean dndOn(Context c) {
        NotificationManager nm = (NotificationManager)
                c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return false;
        }
        return nm.getCurrentInterruptionFilter()
                != NotificationManager.INTERRUPTION_FILTER_ALL;
    }

    private static String toggleDnd(Context c) {
        NotificationManager nm = (NotificationManager)
                c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return "Text";
        }
        final boolean want = !dndOn(c);
        final int f = want ? NotificationManager.INTERRUPTION_FILTER_PRIORITY
                : NotificationManager.INTERRUPTION_FILTER_ALL;
        try {
            nm.setInterruptionFilter(f);
            return null;
        } catch (Throwable t) {
            Log.i(TAG, "Text，Text shell Text：" + t);
        }
        String err = shell("cmd notification allow_dnd " + c.getPackageName());
        if (err != null) {
            return err;
        }
        try {
            nm.setInterruptionFilter(f);
            return null;
        } catch (Throwable t) {
            return "Text";
        }
    }

    // ----- [comment]

    private static CameraManager cam;
    private static boolean torchHooked;
    private static volatile boolean torchOn;
    private static String torchId;

    /** [comment]（[comment]），[comment] */
    private static boolean torchReady(Context c) {
        if (cam == null) {
            try {
                cam = (CameraManager)
                        c.getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
            } catch (Throwable t) {
                cam = null;
            }
        }
        if (cam == null) {
            return false;
        }
        if (!torchHooked) {
            torchHooked = true;
            try {
                cam.registerTorchCallback(new CameraManager.TorchCallback() {
                    @Override
                    public void onTorchModeChanged(String cameraId, boolean enabled) {
                        torchId = cameraId;
                        torchOn = enabled;
                    }

                    @Override
                    public void onTorchModeUnavailable(String cameraId) {
                        torchOn = false;
                    }
                }, new Handler(Looper.getMainLooper()));
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed：" + t);
            }
        }
        return true;
    }

    /** [comment] id */
    private static String findTorch(Context c) {
        try {
            for (String id : cam.getCameraIdList()) {
                CameraCharacteristics ch = cam.getCameraCharacteristics(id);
                Boolean fl = ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer face = ch.get(CameraCharacteristics.LENS_FACING);
                if (Boolean.TRUE.equals(fl) && face != null
                        && face == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed：" + t);
        }
        return null;
    }

    private static String toggleTorch(Context c) {
        if (!torchReady(c)) {
            return "Text";
        }
        String id = torchId != null ? torchId : findTorch(c);
        if (id == null) {
            return "Text";
        }
        final boolean want = !torchOn;
        try {
            cam.setTorchMode(id, want);
            return null;
        } catch (Throwable t) {
            Log.i(TAG, "Text（TextYesText），Text shell Text：" + t);
        }
        // [comment]Yes[comment]，[comment]**[comment]**[comment] —— [comment]，
        // [comment]，[comment] Shizuku [comment]（`pm grant` [comment] development [comment]，shell [comment]）。
        String err = shell("pm grant " + c.getPackageName() + " android.permission.CAMERA");
        if (err != null) {
            return err;
        }
        try {
            cam.setTorchMode(id, want);
            return null;
        } catch (Throwable t) {
            Log.w(TAG, "TextYesText", t);
            return "Text";
        }
    }

    // ------------------------------------------------------------------ shell

    /**
     * [comment] shell，[comment]"[comment]"[comment]。
     *
     * <p>{@link ShellRunner#run} [comment] stdout [comment] stderr [comment]（stderr [comment] {@code !}），
     * [comment] / [comment] SELinux [comment]Yes {@code inaccessible or not found} [comment]，
     * [comment]Failed —— [comment]"[comment]Success"[comment]。
     */
    private static String shell(String cmd) {
        if (!ShellRunner.isReady()) {
            return "Text Shizuku";
        }
        String out = ShellRunner.run(cmd, 10);
        if (out == null || out.isEmpty()) {
            return null;
        }
        if (out.startsWith("ERR:")) {
            return "Text";
        }
        String low = out.toLowerCase(java.util.Locale.ROOT);
        if (low.contains("exception") || low.contains("denied")
                || low.contains("inaccessible") || low.contains("not found")
                || low.contains("unknown command") || low.contains("usage:")) {
            Log.w(TAG, cmd + " => " + out);
            return "Text";
        }
        Log.d(TAG, cmd + " => " + out);
        return null;
    }
}
