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
 * [comment]：<b>root（{@code su -c}）[comment]，Shizuku（[comment] ADB [comment]）[comment]</b>。
 *
 * <p>[comment]：root Authorized[comment]None[comment]（[comment]、[comment] Shizuku
 * [comment]），[comment] USB [comment] root [comment]（[comment]）[comment]。
 *
 * <p>Shizuku [comment]：API 13 [comment] {@code Shizuku.newProcess} [comment] private，
 * [comment] {@code IShizukuService.newProcess} [comment]Yes[comment]，
 * [comment] Shizuku [comment] binder [comment]。
 */
public final class ShellRunner {

    private static final String TAG = "ShellRunner";

    private ShellRunner() {
    }

    /** Shizuku [comment]YesNo[comment] */
    public static boolean isBinderAlive() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    /** [comment]YesNo[comment] Shizuku [comment] */
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
     * root [comment]YesNo[comment]。
     *
     * <p>[comment] {@link RootUtil} [comment]：[comment] exec、[comment]，[comment]。
     * [comment] {@code RootUtil.refresh} [comment] —— [comment] root
     * [comment]，[comment]Yes[comment]（[comment] root [comment]，[comment]）。
     */
    public static boolean rootReady() {
        return RootUtil.state() == RootUtil.ST_OK;
    }

    /**
     * [comment]（root [comment] Shizuku）。
     *
     * <p>[comment]Yes「[comment]」，[comment]Yes「Shizuku [comment]」—— root Authorized[comment]None[comment]
     * [comment]None[comment]。[comment] {@link #rootReady()}，
     * [comment] Shizuku [comment]。
     */
    public static boolean isReady() {
        return rootReady() || (isBinderAlive() && hasPermission());
    }

    /**
     * [comment] Shizuku [comment]。
     * [comment] Shizuku [comment] binder [comment] ShizukuBinderWrapper，[comment]。
     */
    private static IShizukuService service() throws Exception {
        IBinder binder = Shizuku.getBinder();
        if (binder == null) {
            throw new IllegalStateException("Shizuku binder Text");
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
     * [comment]，[comment] stdout/stderr。
     *
     * <p>[comment]：root Authorized[comment] {@code su -c}，No[comment] Shizuku。
     */
    public static String run(String cmd) {
        if (rootReady()) {
            String out = RootUtil.exec(cmd);
            if (out != null) {
                return out;
            }
            Log.w(TAG, "root TextFailed，Text Shizuku: " + cmd);
        }
        if (!isBinderAlive() || !hasPermission()) {
            return "ERR: Shizuku Not ReadyTextNone root";
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
     * [comment] {@link #run}。
     *
     * <p>[comment]：{@code input} Yes「[comment]」[comment]，[comment]<b>[comment]</b>[comment] InputDispatcher
     * [comment]（[comment] <b>8~26 [comment]</b>）。shell [comment] {@code timeout 2} [comment] ——
     * {@code timeout} [comment]，[comment] {@code input} [comment]，
     * [comment] EOF [comment] ⇒ [comment] 20 [comment]，[comment]，[comment]。
     *
     * <p>[comment]<b>[comment]</b>：[comment]，[comment]
     * {@code timeoutSec} [comment]，[comment] {@code destroy()} [comment]（[comment]Disable，[comment]）。
     * [comment]Yes[comment]（[comment] wait [comment]），[comment]Yes[comment] ——
     * [comment]，[comment]。
     *
     * @param timeoutSec ≤0 [comment]（{@code dumpsys} / {@code screencap} [comment]）
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
            Log.w(TAG, "root TextFailed，Text Shizuku: " + cmd);
        }
        if (!isBinderAlive() || !hasPermission()) {
            return "ERR: Shizuku Not ReadyTextNone root";
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
                Log.d(TAG, cmd + " => [TIMEOUT " + cost + "ms] Text，Text");
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
     * [comment]、[comment] stdout <b>[comment]</b>（[comment] stderr，[comment]）。
     *
     * <p>[comment] {@link #run}：[comment] {@code BufferedReader.readLine()}，Yes[comment]
     * [comment] —— [comment] {@code screencap} [comment] PNG，[comment]、[comment] stderr [comment]
     * Notice[comment]，[comment]Yes[comment]。[comment]。
     *
     * @param maxBytes [comment]，[comment]None[comment]
     * @return [comment]；[comment] / [comment] null。<b>[comment]</b>
     */
    public static byte[] capture(String cmd, int maxBytes) {
        if (rootReady()) {
            byte[] b = RootUtil.capture(cmd, maxBytes);
            if (b != null) {
                return b;
            }
            Log.w(TAG, "root TextFailed，Text Shizuku: " + cmd);
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
                    Log.w(TAG, "capture Text " + maxBytes + "，Text: " + cmd);
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

    /** [comment] shell [comment]（[comment]，[comment] {@link #capture}） */
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
