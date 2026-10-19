package com.wb.extrotator;

import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import moe.shizuku.server.IRemoteProcess;
import moe.shizuku.server.IShizukuService;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;

/**
 * 特权命令执行中枢：<b>root（{@code su -c}）优先，Shizuku（借 ADB 身份）兜底</b>。
 *
 * <p>为什么双通道：root 已授权时不再依赖无线调试配对（没有调试弹窗、不怕 Shizuku
 * 被系统杀），拔了 USB 只用 root 的场合（比如接着便携屏时）照样能跑命令。
 *
 * <p>Shizuku 那条路：API 13 把 {@code Shizuku.newProcess} 改成了 private，
 * 但底层 {@code IShizukuService.newProcess} 依然是公开接口，
 * 所以这里直接用 Shizuku 服务 binder 调它。
 */
public final class ShellRunner {

    private static final String TAG = "ShellRunner";

    private ShellRunner() {
    }

    /** Shizuku 服务是否在运行 */
    public static boolean isBinderAlive() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    /** 本应用是否已获得 Shizuku 授权 */
    public static boolean hasPermission() {
        try {
            if (Shizuku.isPreV11()) {
                return true;
            }
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * root 通道是否就绪。
     *
     * <p>纯读 {@link RootUtil} 的内存状态：不 exec、不弹窗，任何线程都能调。
     * 状态每次回前台由 {@code RootUtil.refresh} 刷新 —— 后台里刚授权的 root
     * 要等下一次回前台才认，这是故意的（探测会弹 root 窗，不能随手跑）。
     */
    public static boolean rootReady() {
        return RootUtil.state() == RootUtil.ST_OK;
    }

    /**
     * 任一特权通道就绪（root 或 Shizuku）。
     *
     * <p>语义是「能跑特权命令吗」，不是「Shizuku 通了吗」—— root 已授权时无线调试
     * 在不在都无所谓。界面文案要显示通道名时单独判 {@link #rootReady()}，
     * 别把这个当 Shizuku 状态读。
     */
    public static boolean isReady() {
        return rootReady() || (isBinderAlive() && hasPermission());
    }

    /**
     * 拿到 Shizuku 服务代理。
     * 新版 Shizuku 的 binder 可能需要包一层 ShizukuBinderWrapper，这里两种都试。
     */
    private static IShizukuService service() throws Exception {
        IBinder binder = Shizuku.getBinder();
        if (binder == null) {
            throw new IllegalStateException("Shizuku binder 为空");
        }
        IShizukuService direct = IShizukuService.Stub.asInterface(binder);
        try {
            direct.getVersion();
            return direct;
        } catch (Throwable first) {
            IShizukuService wrapped =
                    IShizukuService.Stub.asInterface(new ShizukuBinderWrapper(binder));
            wrapped.getVersion();
            return wrapped;
        }
    }

    /**
     * 执行一条命令，返回合并后的 stdout/stderr。
     *
     * <p>双通道：root 已授权走 {@code su -c}，否则回落 Shizuku。
     */
    public static String run(String cmd) {
        if (rootReady()) {
            String out = RootUtil.exec(cmd);
            if (out != null) {
                return out;
            }
            Log.w(TAG, "root 通道执行失败，回落 Shizuku: " + cmd);
        }
        if (!isBinderAlive() || !hasPermission()) {
            return "ERR: Shizuku 未就绪且无 root";
        }
        IRemoteProcess rp = null;
        try {
            IShizukuService svc = service();
            rp = svc.newProcess(new String[]{"sh", "-c", cmd}, null, null);

            StringBuilder sb = new StringBuilder();
            readInto(rp.getInputStream(), sb, "");
            readInto(rp.getErrorStream(), sb, "!");
            int code = rp.waitFor();

            String out = sb.toString().trim();
            Log.d(TAG, cmd + " => [" + code + "] " + out);
            return out;
        } catch (Throwable t) {
            Log.e(TAG, "run failed: " + cmd, t);
            return "ERR: " + t;
        } finally {
            if (rp != null) {
                try {
                    rp.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * 带读取上限的 {@link #run}。
     *
     * <p>为什么必须：{@code input} 是「等结果」模式，目标屏<b>没有焦点窗口</b>时 InputDispatcher
     * 会一直等（实测一条命令卡 <b>8~26 秒</b>）。shell 端 {@code timeout 2} 兜不住 ——
     * {@code timeout} 只杀得掉直接子进程，被孤立的 {@code input} 继续持有那条管道，
     * 这端要读到 EOF 才回来 ⇒ 仍 20 秒一条，队列全堵死，后续点击还会被丢掉。
     *
     * <p>所以上限必须加在<b>我们自己这一端</b>：读取丢给旁路线程，主线程最多等
     * {@code timeoutSec} 秒，到点 {@code destroy()} 掉进程（管道随之关闭，旁路线程自然退出）。
     * 命令该送还是送出去了（注入发生在 wait 之前），超时丢掉的只是结果回显 ——
     * 事件进管线的顺序仍按提交顺序，连发不会乱序。
     *
     * @param timeoutSec ≤0 表示不限时（{@code dumpsys} / {@code screencap} 那种大输出走原路）
     */
    public static String run(String cmd, int timeoutSec) {
        if (timeoutSec <= 0) {
            return run(cmd);
        }
        if (rootReady()) {
            String out = RootUtil.exec(cmd, timeoutSec);
            if (out != null) {
                return out;
            }
            Log.w(TAG, "root 通道执行失败，回落 Shizuku: " + cmd);
        }
        if (!isBinderAlive() || !hasPermission()) {
            return "ERR: Shizuku 未就绪且无 root";
        }
        IRemoteProcess rp = null;
        final String[] box = new String[]{""};
        try {
            final IRemoteProcess proc = service().newProcess(
                    new String[]{"sh", "-c", cmd}, null, null);
            rp = proc;
            Thread reader = new Thread(() -> {
                StringBuilder sb = new StringBuilder();
                try {
                    readInto(proc.getInputStream(), sb, "");
                    readInto(proc.getErrorStream(), sb, "!");
                } catch (Throwable ignored) {
                }
                box[0] = sb.toString().trim();
            }, "shell-bounded");
            reader.setDaemon(true);
            long t0 = System.currentTimeMillis();
            reader.start();
            reader.join(timeoutSec * 1000L);
            long cost = System.currentTimeMillis() - t0;
            if (reader.isAlive()) {
                Log.d(TAG, cmd + " => [TIMEOUT " + cost + "ms] 放弃等待，队列放行");
                try {
                    proc.destroy();
                } catch (Throwable ignored) {
                }
                reader.join(300);
                return box[0].isEmpty() ? "TIMEOUT" : box[0] + " TIMEOUT";
            }
            Log.d(TAG, cmd + " => [" + cost + "ms] " + box[0]);
            return box[0];
        } catch (Throwable t) {
            Log.e(TAG, "run(bounded) failed: " + cmd, t);
            return "ERR: " + t;
        } finally {
            if (rp != null) {
                try {
                    rp.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * 只跑命令、把 stdout <b>原样读成字节</b>（不去碰 stderr，也不按行拆）。
     *
     * <p>为什么不能复用 {@link #run}：那个走 {@code BufferedReader.readLine()}，是给文本输出
     * 准备的 —— 拿它读 {@code screencap} 的 PNG，会在第一个换行字节处断掉、还会拼上 stderr 的
     * 提示行，出来的是坏图。抓画面的路只有这一条。
     *
     * @param maxBytes 上限，防命令异常时无限读把内存吃光
     * @return 读到的字节；没授权 / 起不了进程返回 null。<b>阻塞</b>
     */
    public static byte[] capture(String cmd, int maxBytes) {
        if (rootReady()) {
            byte[] b = RootUtil.capture(cmd, maxBytes);
            if (b != null) {
                return b;
            }
            Log.w(TAG, "root 通道抓取失败，回落 Shizuku: " + cmd);
        }
        if (!isBinderAlive() || !hasPermission()) {
            return null;
        }
        IRemoteProcess rp = startProcess(cmd);
        if (rp == null) {
            return null;
        }
        try (InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(rp.getInputStream())) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 18);
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
            return bos.toByteArray();
        } catch (Throwable t) {
            Log.e(TAG, "capture failed: " + cmd, t);
            return null;
        } finally {
            try {
                rp.destroy();
            } catch (Throwable ignored) {
            }
        }
    }

    /** 起一个 shell 进程（拿得到进程就能边跑边读，见 {@link #capture}） */
    private static IRemoteProcess startProcess(String cmd) {
        try {
            IShizukuService svc = service();
            return svc.newProcess(new String[]{"sh", "-c", cmd}, null, null);
        } catch (Throwable t) {
            Log.e(TAG, "startProcess failed: " + cmd, t);
            return null;
        }
    }

    private static void readInto(ParcelFileDescriptor pfd, StringBuilder sb, String prefix) {
        if (pfd == null) {
            return;
        }
        try (InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
             BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(prefix).append(line).append('\n');
            }
        } catch (Throwable ignored) {
        }
    }
}
