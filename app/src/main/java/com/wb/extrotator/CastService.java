package com.wb.extrotator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

/**
 * 「[comment]」[comment]：<b>[comment] + [comment]</b>。
 *
 * <p><b>[comment]Yes[comment]</b>：[comment]Yes[comment]「[comment]」，[comment]Yes[comment] —— targetSdk 34
 * （Android 14）[comment]，[comment]<b>[comment] mediaProjection [comment]</b>[comment]Allow[comment]
 * {@link MediaProjectionManager#getMediaProjection}，No[comment] SecurityException。[comment]Yes[comment]：
 * <pre>
 *   [comment]「[comment]」→ [comment] → [comment] (resultCode, data)
 *     → startForegroundService（[comment]，[comment] mediaProjection）
 *       → startForeground()
 *         → getMediaProjection()   ← [comment]
 *           → [comment] TextureView [comment] Surface [comment] → createVirtualDisplay()
 * </pre>
 *
 * <p><b>[comment]Yes[comment]</b>：[comment]（display 0，[comment] DEFAULT_DISPLAY）[comment]，[comment]
 * [comment] Surface —— [comment] TextureView [comment]，[comment]「[comment]」。[comment]，[comment]
 * TextureView [comment]，[comment] {@link ExtScreen#dragStart} [comment]，
 * [comment]「[comment]」。
 *
 * <p>⚠ <b>[comment]</b>：
 * <ul>
 *   <li><b>[comment]</b>（Android 14 [comment]）：[comment]（[comment]、[comment]
 *       [comment]）[comment]<b>[comment]</b>[comment] createVirtualDisplay，[comment] {@link VirtualDisplay#setSurface}
 *       [comment] Surface [comment] —— [comment] {@link #attach(Surface)} [comment]「[comment]」[comment]「[comment]」
 *       [comment]，[comment]；</li>
 *   <li><b>[comment]Yes[comment]</b>：[comment]（Android 14 [comment]，[comment]「[comment]
 *       [comment]」[comment]），[comment]「[comment]」[comment]，[comment]（{@link #stopCast}）。</li>
 * </ul>
 */
public class CastService extends Service {

    private static final String TAG = "ExtCast";

    /** [comment] */
    public static final String ACTION_START = "com.wb.extrotator.action.CAST_START";
    /** [comment] */
    public static final String ACTION_STOP = "com.wb.extrotator.action.CAST_STOP";

    public static final String EXTRA_CODE = "code";
    public static final String EXTRA_DATA = "data";
    /** [comment]（[comment]）；[comment] */
    public static final String EXTRA_SRC_W = "src_w";
    public static final String EXTRA_SRC_H = "src_h";
    /**
     * [comment]。
     *
     * <p>⚠ [comment]<b>[comment]</b>，[comment]「[comment]」：[comment] 1199×999、[comment]
     * vd [comment]Yes 1296×1080、dpi=320 ⇒ [comment]<b>[comment]</b>。
     *
     * <p>[comment]Yes「<b>[comment] : [comment]</b>」，[comment]Yes
     * 「[comment] vd [comment]」（[comment] {@code CastActivity.RESIZE_SETTLE_MS}）。
     */
    public static final String EXTRA_DPI = "dpi";

    private static final String CH_ID = "extrot_cast";
    private static final int NOTI_ID = 4201;

    /** [comment]；[comment]"[comment]" */
    private static volatile CastService inst;

    private MediaProjection projection;
    private VirtualDisplay display;
    private Surface surface;

    private int srcW, srcH, srcDpi;
    private Listener listener;

    /**
     * [comment]"[comment] / [comment]"。
     * [comment] —— [comment]（[comment]），
     * [comment]，[comment]。
     */
    public interface Listener {
        /**
         * @param projecting true = [comment]、[comment]；false = [comment]
         * @param why        [comment]（[comment]），[comment] null
         */
        void onCastState(boolean projecting, String why);
    }

    public static CastService get() {
        return inst;
    }

    /** [comment]（[comment]Notice[comment]） */
    public static boolean ready() {
        CastService s = inst;
        return s != null && s.projection != null;
    }

    /**
     * [comment]（null = [comment]Yes「[comment]」）。
     *
     * <p>[comment]：[comment]Yes[comment]<b>[comment]</b>[comment]（[comment]，[comment]
     * [comment] {@code onStart}），[comment] {@code notifyState} [comment]。[comment]
     * [comment] —— No[comment]Failed[comment]「[comment]」，[comment]Yes[comment]、[comment]Yes[comment]
     * [comment]Failed。
     */
    public static String lastWhy() {
        return lastWhy;
    }

    private static volatile String lastWhy;

    /**
     * [comment]"[comment]"[comment]。
     *
     * @param code [comment] resultCode（[comment]Yes RESULT_OK）
     * @param data [comment] Intent
     */
    public static void start(Context ctx, int code, Intent data, int w, int h, int dpi) {
        Intent i = new Intent(ctx, CastService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_CODE, code)
                .putExtra(EXTRA_DATA, data)
                .putExtra(EXTRA_SRC_W, w)
                .putExtra(EXTRA_SRC_H, h)
                .putExtra(EXTRA_DPI, dpi);
        try {
            ctx.startForegroundService(i);
        } catch (Throwable t) {
            Log.w(TAG, "startForegroundService Failed: " + t);
        }
    }

    // ------------------------------------------------------------------ [comment]

    @Override
    public void onCreate() {
        super.onCreate();
        inst = this;
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAsForeground();

        if (intent == null) {
            // [comment]（START_NOT_STICKY [comment]），[comment]
            stopCast("Text，Text");
            return START_NOT_STICKY;
        }
        if (ACTION_STOP.equals(intent.getAction())) {
            stopCast("Text");
            return START_NOT_STICKY;
        }

        if (projection != null) {
            // [comment]：[comment]Update[comment]（[comment]）
            srcW = intent.getIntExtra(EXTRA_SRC_W, srcW);
            srcH = intent.getIntExtra(EXTRA_SRC_H, srcH);
            notifyState(true, null);
            return START_NOT_STICKY;
        }

        final int code = intent.getIntExtra(EXTRA_CODE, 0);
        final Intent data = intent.getParcelableExtra(EXTRA_DATA);
        srcW = intent.getIntExtra(EXTRA_SRC_W, 0);
        srcH = intent.getIntExtra(EXTRA_SRC_H, 0);
        srcDpi = intent.getIntExtra(EXTRA_DPI, 320);
        if (code == 0 || data == null || srcW <= 0 || srcH <= 0) {
            stopCast("Text（" + srcW + "×" + srcH + "）");
            return START_NOT_STICKY;
        }
        startProjection(code, data);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        inst = null;
        release(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ------------------------------------------------------------------ [comment]

    private void startProjection(int code, Intent data) {
        MediaProjection p = null;
        try {
            MediaProjectionManager mpm =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            p = mpm == null ? null : mpm.getMediaProjection(code, data);
        } catch (Throwable t) {
            Log.w(TAG, "getMediaProjection Failed: " + t);
        }
        if (p == null) {
            stopCast("Text（TextDeny）");
            return;
        }
        /*
         * ⚠ [comment] createVirtualDisplay **[comment]**[comment]：
         * Android 14 [comment]，[comment] IllegalStateException。
         * [comment]Yes"[comment]"[comment] —— [comment]，
         * [comment]。
         */
        try {
            p.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    Log.i(TAG, "Text");
                    release(false);
                    notifyState(false, "Text");
                    stopForeground(true);
                    stopSelf();
                }
            }, new Handler(Looper.getMainLooper()));
        } catch (Throwable t) {
            Log.w(TAG, "registerCallback Failed: " + t);
        }
        projection = p;
        Log.i(TAG, "Text，Text " + srcW + "×" + srcH + " dpi=" + srcDpi);
        notifyState(true, null);
    }

    /**
     * [comment] Surface [comment]（[comment] TextureView [comment]）。
     *
     * <p>“[comment]”[comment]“[comment]”[comment]Yes[comment]，[comment] Android 14 [comment]。
     *
     * @return false = [comment] / [comment]Failed，[comment]Notice
     */
    public boolean attach(Surface s) {
        MediaProjection p = projection;
        if (p == null || s == null) {
            return false;
        }
        try {
            if (display == null) {
                display = p.createVirtualDisplay("extrot-cast", srcW, srcH, srcDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, s, null,
                        new Handler(Looper.getMainLooper()));
                Log.i(TAG, "Text " + srcW + "×" + srcH + " → " + (display != null));
            } else {
                display.setSurface(s);
                Log.i(TAG, "Text Surface");
            }
            surface = s;
            return display != null;
        } catch (Throwable t) {
            Log.w(TAG, "attach Failed: " + t);
            notifyState(false, "TextFailed：" + t);
            return false;
        }
    }

    /**
     * [comment] / [comment]：[comment]。
     *
     * <p><b>[comment]</b>：[comment]Yes[comment]，[comment]
     * <b>[comment]</b>[comment]（[comment] ↔ [comment]）[comment] —— [comment] buffer，
     * [comment]，[comment]，[comment]Yes"[comment]"。
     *
     * <p>⚠ <b>[comment]</b>[comment]：Android 14 [comment] createVirtualDisplay [comment]
     * （[comment]），[comment] resize [comment]。
     *
     * @return true = [comment]、[comment]Success
     */
    public boolean refit(int w, int h) {
        VirtualDisplay d = display;
        if (d == null || w <= 0 || h <= 0) {
            return false;
        }
        if (w == srcW && h == srcH) {
            return false;
        }
        try {
            d.resize(w, h, srcDpi);
            Log.i(TAG, "Text " + srcW + "×" + srcH + " → " + w + "×" + h);
            srcW = w;
            srcH = h;
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed: " + t);
            return false;
        }
    }

    /** [comment]：[comment]，[comment]（[comment]） */
    public void detach() {
        if (display != null) {
            try {
                display.setSurface(null);
            } catch (Throwable ignored) {
            }
        }
        surface = null;
    }

    /** [comment]：[comment]、[comment]、[comment] */
    public void stopCast(String why) {
        release(true);
        notifyState(false, why);
        try {
            stopForeground(true);
        } catch (Throwable ignored) {
        }
        stopSelf();
        revokeGrant();
    }

    /**
     * [comment] appops [comment]。
     *
     * <p>[comment]：{@code CastActivity.grantProjection()} [comment]「[comment]
     * [comment]」[comment]，[comment] {@code PROJECT_MEDIA} [comment] allow。[comment]，
     * [comment]「[comment]」[comment]。
     *
     * <p>[comment]<b>[comment]</b>[comment]、<b>[comment]</b>[comment] —— appops Yes[comment]「[comment] + [comment]」[comment]，
     * [comment]（[comment] {@code appops query-op PROJECT_MEDIA allow} [comment]）。
     *
     * <p>⚠ [comment] {@code default}（[comment]Default[comment]），<b>[comment]Yes</b> {@code ignore} —— [comment]
     * Default[comment]，[comment]Yes[comment]。
     *
     * <p>⚠ [comment]Failed：Shizuku [comment] / [comment]None[comment]，[comment]。[comment]Yes「[comment]」，
     * [comment]。
     */
    private void revokeGrant() {
        final String pkg = getPackageName();
        new Thread(() -> {
            try {
                if (ShellRunner.isReady()) {
                    String out = ShellRunner.run("appops set " + pkg + " PROJECT_MEDIA default");
                    Log.i(TAG, "PROJECT_MEDIA Text → " + (out == null ? "" : out.trim()));
                }
            } catch (Throwable t) {
                Log.w(TAG, "Text PROJECT_MEDIA Failed: " + t);
            }
        }, "extrot-cast-revoke").start();
    }

    public void setListener(Listener l) {
        listener = l;
    }

    private void notifyState(boolean projecting, String why) {
        // [comment]：[comment]Yes[comment]，
        // [comment] notifyState [comment]，[comment]（[comment] lastWhy）。
        lastWhy = projecting ? null : why;
        Listener l = listener;
        if (l != null) {
            l.onCastState(projecting, why);
        }
    }

    /**
     * [comment]。
     *
     * @param stopProjection [comment]。
     *                       ⚠ [comment] {@code onStop()} [comment] false —— [comment]Yes[comment]
     *                       [comment]，[comment] {@code stop()} Yes[comment]（[comment]Yes[comment]
     *                       [comment]，[comment]）。
     */
    private void release(boolean stopProjection) {
        if (display != null) {
            try {
                display.release();
            } catch (Throwable ignored) {
            }
            display = null;
        }
        MediaProjection p = projection;
        projection = null;
        if (p != null && stopProjection) {
            try {
                p.stop();
            } catch (Throwable ignored) {
            }
        }
        // Surface [comment]（[comment]、[comment] release），[comment]
        surface = null;
    }

    // ------------------------------------------------------------------ [comment]

    private void createChannel() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        NotificationChannel ch = new NotificationChannel(
                CH_ID, "Text", NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false);
        ch.setSound(null, null);
        nm.createNotificationChannel(ch);
    }

    private void startAsForeground() {
        try {
            Intent open = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification n = new Notification.Builder(this, CH_ID)
                    .setSmallIcon(R.drawable.ic_stat_rot)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Text")
                    .setOngoing(true)
                    .setShowWhen(false)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(pi)
                    .build();
            startForeground(NOTI_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } catch (Throwable t) {
            // [comment]：[comment]Yes[comment] FOREGROUND_SERVICE_MEDIA_PROJECTION
            Log.w(TAG, "startForeground Failed: " + t);
            stopSelf();
        }
    }
}
