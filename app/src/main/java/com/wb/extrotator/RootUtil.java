package com.wb.extrotator;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Root（su）授权检测与请求。
 *
 * <p>跟 {@link ShellRunner}（走 Shizuku 借 ADB 权限）是两条平行的特权通道：
 * 这条直接向 root 管理器（KernelSU / Magisk）要 uid=0。已授权的设备上，
 * 凡是 {@code ShellRunner} 用 shell 身份跑的命令，{@code su -c} 都能跑 ——
 * adb shell 权限是 root 的子集。
 *
 * <p>⚠ 探测（{@link #probe()}）会真的 exec 一次 {@code su -c id}：
 * 没授权时 root 管理器会弹窗等用户点，exec 会一直阻塞到点掉为止 ——
 * 所以探测必须放后台线程跑，且不能设短超时。
 *
 * <p>⚠ 状态只存内存、不落盘：临时 root（KernelSU 临时模式）重启就失效，
 * 落盘会在重启后显示"已授权"的假阳性。每次回前台重新探。
 */
public final class RootUtil {

    private static final String TAG = "RootUtil";

    /** 没检测过 */
    public static final int ST_UNKNOWN = 0;
    /** 没有 su 二进制（设备没 root） */
    public static final int ST_NONE = 1;
    /** 有 su 但本应用没拿到授权 */
    public static final int ST_DENIED = 2;
    /** 已授权（id 返回 uid=0） */
    public static final int ST_OK = 3;

    /**
     * 各 root 管理器放 su 的位置。
     *
     * <p>⭐ KernelSU / APatch 的 su <b>不在</b> {@code /system/bin/su}，而在
     * {@code /data/adb/ksu/bin/su}（KernelSU）/ {@code /data/adb/ap/bin/su}（APatch）。
     * 这台 Z Flip5 是 KernelSU —— 只写 Magisk 那套路径会永远探成「无 root」
     * （2026-10-18 踩过：授权按钮按了却报未授权，根因就在这）。
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

    /** 当前状态（{@link #ST_UNKNOWN} 等） */
    public static int state() {
        return state;
    }

    /** su 二进制在不在 */
    public static boolean hasSu() {
        return findSu() != null;
    }

    /**
     * 后台探测一次状态，探完回调用线程执行 {@code done}（不会自己切 UI 线程）。
     */
    public static void refresh(Runnable done) {
        new Thread(() -> {
            state = probe();
            Log.i(TAG, "探测 => " + name(state));
            if (done != null) {
                done.run();
            }
        }, "root-probe").start();
    }

    /**
     * 点「Root 授权」：跑一次 {@code su -c id}，没授权就借它触发 root 管理器弹窗，
     * 跑完刷新状态。授权后 su 不再弹窗，以后 {@link #refresh} 就是纯探测。
     */
    public static void request(Runnable done) {
        new Thread(() -> {
            state = probe();
            Log.i(TAG, "授权请求 => " + name(state));
            if (done != null) {
                done.run();
            }
        }, "root-request").start();
    }

    /** 找到的 su 绝对路径；没有返回 null */
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

    /** 跑 {@code su -c id}：有 su 且输出带 uid=0 ⇒ 已授权 */
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
            Log.w(TAG, "probe 失败", t);
            return ST_DENIED;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    /**
     * 以 root 身份跑一条命令，返回合并后的 stdout/stderr；跑不了返回 null。
     *
     * <p>给 {@link ShellRunner} 双通道用：root 已授权时，凡是 shell 身份能跑的命令
     * {@code su -c} 都能跑（adb shell 权限是 root 的子集）。不限时，调用方保证在后台线程。
     */
    public static String exec(String cmd) {
        return exec(cmd, 0);
    }

    /**
     * 带超时的 {@link #exec}：到点 destroy 进程、管道随之关闭，旁路读线程自然退出。
     *
     * <p>超时语义与 {@code ShellRunner.run(cmd, timeoutSec)} 对齐 —— 命令该送还是
     * 送出去了，丢掉的只是结果回显。
     *
     * @param timeoutSec ≤0 表示不限时
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
            Log.d(TAG, cmd + " => [TIMEOUT " + timeoutSec + "s] 放弃等待");
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
     * 同步跑完拿输出（{@link #exec} 的实体）。
     *
     * @param holder 非 null 时，进程 start 之后立刻放进来，供超时方 destroy
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
            Log.w(TAG, "exec 失败: " + cmd, t);
            return null;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    /**
     * 以 root 身份跑一条命令，把 stdout <b>原样读成字节</b>（screencap 走这条）。
     *
     * <p>与 {@link #exec} 的分工同 {@code ShellRunner}：文本走 exec，二进制走这里 ——
     * 拿 BufferedReader 读 PNG 会在第一个换行字节处断掉。读不到 / 空输出返回 null。
     *
     * @param maxBytes 读取上限，防异常时无限读
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
                        Log.w(TAG, "capture 超过上限 " + maxBytes + "，截断: " + cmd);
                        break;
                    }
                }
            }
            p.waitFor();
            byte[] out = bos.toByteArray();
            return out.length > 0 ? out : null;
        } catch (Throwable t) {
            Log.w(TAG, "capture 失败: " + cmd, t);
            return null;
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    /** 状态名（日志用） */
    static String name(int st) {
        switch (st) {
            case ST_NONE:
                return "无 root";
            case ST_DENIED:
                return "未授权";
            case ST_OK:
                return "已授权";
            default:
                return "未检测";
        }
    }
}
