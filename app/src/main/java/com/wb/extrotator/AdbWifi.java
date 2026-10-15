package com.wb.extrotator;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

/**
 * 「无线调试」OnOff内核 —— 存在的意义就是「Shizuku 起不来时把它打On」，所以<b>不能依赖 Shizuku</b>。
 *
 * <p>写 {@code adb_wifi_enabled=1} 只是<b>发起请求</b>: 系统会起 adbd 的 TLS 端口、把确认框投到
 * Internal Screen、<b>并立刻把键复位成 0</b>，只有点了框上的「Allow」才真正写回 1。于是分两步: 当前 Wi-Fi
 * 已在系统「始终Allow」名单里 ⇒ 直接写就成，不弹框、不闪屏；不在 ⇒ 展On内屏让确认框成为可见窗口，
 * 由 {@link AutoAuth} 勾「始终Allow」并点「Allow」，再折回 —— 点过一次之后这个网络永久进名单，
 * 以后都走第 1 条。
 *
 * <p>⚠ 代点只在第 2 条那一小段里武装 (见 {@link AutoAuth}) ；写这个键要
 * {@code WRITE_SECURE_SETTINGS} ({@code pm grant} 授得动，清单里必须先声明) ，只在 Shizuku 恰好
 * 活着时顺手补一次，补不上就报错。读值不需要任何权限。
 */
public final class AdbWifi {

    private static final String TAG = "AdbWifi";

    /** 系统那个键。OnOff的真实状态就看它。 */
    public static final String KEY = "adb_wifi_enabled";

    /** Allow自己写 {@link #KEY} 的权限。development 级，可用 {@code pm grant} 授。 */
    private static final String PERM = "android.permission.WRITE_SECURE_SETTINGS";

    /** 展On之后等内屏真亮起来、确认框画出来 */
    private static final long OPEN_SETTLE_MS = 900L;

    /** 写完键之后等系统落地 (实测 40ms 上下，留宽点)  */
    private static final long APPLY_MS = 400L;

    /**
     * 写完「On」之后，等这么久再判定它<b>稳住了没有</b>。
     * 网络没登记时，系统就是在这段时间里把键复位成 0、把确认框拉起来的。
     */
    private static final long SETTLE_MS = 800L;

    /** 判定「稳住」时的第二次读，跟第一次隔这么久 */
    private static final long CONFIRM_MS = 250L;

    /** 等 {@link AutoAuth} 把「始终Allow」和「Allow」点掉的轮询间隔 */
    private static final long POLL_MS = 300L;

    /** 每个轮次里轮询几次 */
    private static final int POLL_TRIES = 7;

    /** 展On状态下最多重写几轮 (弹窗偶尔会晚一拍画出来)  */
    private static final int ROUNDS = 3;

    private AdbWifi() {
    }

    /** 无线调试现在On着没有。读的是系统里的实际值，不需要权限，读Failed按"没On"处理。 */
    public static boolean isOn(Context c) {
        return readValue(c) == 1;
    }

    /** 现在能不能不靠 Shizuku 直接把键写下去 (给日志和界面用) 。 */
    public static boolean canWriteDirect(Context c) {
        try {
            return c.getApplicationContext().checkSelfPermission(PERM)
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 把OnOff写到指定状态。<b>阻塞</b> (走展On那条路要好几秒) ，必须在Recents线程调用。
     * 写完<b>必须回读</b>: 这个键恰好是会被系统悄悄改回去的那一种。
     *
     * @return null = Success；否则是给用户看的一句话
     */
    public static String set(Context c, boolean on) {
        if (!canWriteDirect(c)) {
            // Shizuku 恰好活着就顺手补一次授权；它不在就补不上，下面直接报错
            ensureWritePermission(c);
        }
        if (!canWriteDirect(c)) {
            Log.w(TAG, "No " + PERM + ", cannot write " + KEY);
            return "Missing WRITE_SECURE_SETTINGS permission (grant via PC or Shizuku)";
        }

        if (!on) {
            return apply(c, 0);
        }

        // ① 便宜的一条: 当前网络已登记 ⇒ 直接写就成，不用展On、不闪屏。
        //   判据是"写下去之后稳住了"，不是"Read 1 就完" (见 confirmOn) 。
        if (writeKey(c, 1) && confirmOn(c)) {
            Log.i(TAG, "无线调试已Enabled (网络已登记，直接写键) ");
            return null;
        }

        // ② 没成 ⇒ 这个网络没登记过: 展OnInternal Screen，让确认框露出来给无障碍点
        return unfoldRoute(c);
    }

    /**
     * 完整那条路: 展On → 写键 → 等无障碍点掉确认框 → 折回。全程只用 app 身份。
     */
    private static String unfoldRoute(Context c) {
        if (!AutoAuth.isReady()) {
            Log.w(TAG, "无障碍服务没在跑，点不了确认框");
            return "点不了确认框: 无障碍服务没在跑";
        }
        Log.i(TAG, "这个 Wi-Fi 没登记过 ⇒ 展On内屏让确认框露出来");
        // 代点只在这一小段里武装，折回后立刻卸下。常On是不行的:
        // 系统里所有带「Allow / OK」的弹窗都会被一起点掉 (用户实测到了) 。
        AutoAuth.arm();
        if (!DualScreen.openLocal()) {
            AutoAuth.disarm();
            return "没能展OnInternal Screen (cmd device_state 不可用) ";
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
                    // 连读两次都是 1 才算成: 刚写下去那一下也是 1，
                    // 系统把它复位回 0 只要几十毫秒
                    if (readValue(c) == 1) {
                        sleep(250);
                        ok = readValue(c) == 1;
                    }
                }
                Log.i(TAG, "第 " + r + " 轮: " + (ok ? "Success" : "还没成"));
            }
        } finally {
            DualScreen.closeLocal();
            sleep(APPLY_MS);
            AutoAuth.disarm();
        }

        int back = readValue(c);
        if (back == 1) {
            Log.i(TAG, "无线调试已Enabled (这个网络已记进「始终Allow」名单) ");
            return null;
        }
        return "没能打On (回读 " + back + "）";
    }

    /**
     * 写完 {@code 1} 之后，它<b>稳住了没有</b>。<b>阻塞</b>。
     *
     * <p>⚠ 不能用「读到一次 {@code 1} 就算成」: 刚 {@code putInt} 完 SettingsProvider 的缓存立刻
     * 就是新值，读一次必定命中；而网络没登记时，系统会在几百毫秒内把键复位成 0 并拉起确认框 ——
     * 只读一次就是把「马上要被打回去」当成Success，压根不走展On那条路，真机表现为报Success、其实没On。
     */
    private static boolean confirmOn(Context c) {
        sleep(SETTLE_MS);
        if (readValue(c) != 1) {
            return false;
        }
        sleep(CONFIRM_MS);
        return readValue(c) == 1;
    }

    /** 只写键、不回读那条路 (On/Off都用得上)  */
    private static String apply(Context c, int v) {
        if (!writeKey(c, v)) {
            return "写 " + KEY + " Failed";
        }
        int got = waitValue(c, v, APPLY_MS + 400);
        if (got == v) {
            Log.i(TAG, "无线调试" + (v == 1 ? "已Enabled" : "已Disabled"));
            return null;
        }
        Log.w(TAG, "想写 " + v + "，回读是 " + got);
        return (v == 1 ? "没能打On" : "没能Disabled") + " (回读 " + got + "）";
    }

    private static boolean writeKey(Context c, int v) {
        try {
            Settings.Global.putInt(c.getApplicationContext().getContentResolver(), KEY, v);
            Log.i(TAG, KEY + " <- " + v);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "写 " + KEY + " Failed", t);
            return false;
        }
    }

    private static int readValue(Context c) {
        try {
            return Settings.Global.getInt(c.getApplicationContext().getContentResolver(), KEY, 0);
        } catch (Throwable t) {
            Log.w(TAG, "读 " + KEY + " Failed: " + t);
            return -1;
        }
    }

    /** 在 {@code ms} 之内轮询，直到读回 {@code want}；Back最后一次读到的值 */
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
     * 确保自己拿到 {@link #PERM}。只有 Shizuku 恰好活着时才做得到。
     *
     * <p>这是本类唯一借一次外力的地方，而且只是补一次授权，OnOff本身全程不依赖 Shizuku。
     */
    private static void ensureWritePermission(Context c) {
        if (!ShellRunner.isReady()) {
            return;
        }
        String out = ShellRunner.run("pm grant " + c.getPackageName() + " " + PERM);
        Log.i(TAG, "顺手补一次权限 pm grant => " + out);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
