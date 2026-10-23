package com.wb.extrotator;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.Icon;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Surface;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * [comment]，[comment]：
 *
 * <p><b>1) [comment]</b>：[comment]，[comment]。
 * [comment] —— {@code wm fixed-to-user-rotation -d <ID> enabled}（[comment]，
 * [comment]）、{@code wm user-rotation -d <ID> lock <0|1|2|3>}。
 *
 * <p><b>2) [comment]</b>（[comment] SecondScreen）：{@code wm size <W>x<H>} / {@code wm size reset}
 * （⚠ {@code size} [comment] {@code -d}，[comment] {@link ScreenSizeUtil}）。[comment]、[comment]；
 * [comment]，[comment]（[comment] 1920×1080 → [comment] 1080×1920），
 * [comment]。
 *
 * <p>[comment]（[comment]Yes[comment]）：
 * <ul>
 *   <li>[comment]：[comment]，[comment]。</li>
 *   <li>shell [comment]，[comment] —— [comment] DisplayListener [comment]「[comment]」。</li>
 *   <li>[comment] {@link #STABLE_MS} [comment]；[comment] {@link #SETTLE_MS} [comment]，
 *       [comment]（[comment]，[comment]Yes[comment]）。</li>
 *   <li>[comment]（display 0）：[comment]Yes[comment]，[comment]。</li>
 *   <li><b>「[comment]」[comment]「[comment]」</b>：[comment]、[comment] / [comment] rotation [comment]，[comment]
 *       [comment] —— [comment] rotation [comment]，[comment]Yes「[comment] 1/4 [comment]」[comment]。
 *       [comment]：[comment]（[comment] + [comment] + [comment]）[comment]
 *       free → lock [comment]，[comment]YesNo[comment]「[comment]」。</li>
 *   <li>「[comment]」[comment]Yes[comment]（[comment]，[comment]）。
 *       [comment]：[comment]，[comment]，[comment]。</li>
 *   <li>[comment]「[comment]」[comment]，「[comment]」[comment] —— No[comment]、
 *       [comment]，[comment]。</li>
 * </ul>
 */
public class RotationService extends Service {

    private static final String TAG = "RotationService";

    public static final String ACTION_START = "com.wb.extrotator.START";
    public static final String ACTION_STOP = "com.wb.extrotator.STOP";
    public static final String ACTION_MANUAL = "com.wb.extrotator.MANUAL";
    /** [comment]「[comment] free [comment] lock」[comment]，[comment] */
    public static final String ACTION_REPAIR = "com.wb.extrotator.REPAIR";
    /** [comment] */
    public static final String ACTION_APPLY_SIZE = "com.wb.extrotator.APPLY_SIZE";
    /** [comment] */
    public static final String ACTION_RESET_SIZE = "com.wb.extrotator.RESET_SIZE";
    /** [comment]「[comment]」/「[comment]」（[comment]） */
    public static final String ACTION_PROMPT_YES = "com.wb.extrotator.PROMPT_YES";
    public static final String ACTION_PROMPT_NO = "com.wb.extrotator.PROMPT_NO";
    /** [comment]「[comment]」：[comment]，[comment] */
    public static final String ACTION_TEST_PROMPT = "com.wb.extrotator.TEST_PROMPT";
    /**
     * [comment]「[comment]」[comment]。
     * <p>[comment]：[comment]「[comment]」[comment]，
     * [comment] {@link #ACTION_STOP}，[comment]、[comment]
     * [comment] —— [comment]Yes"[comment]Yes[comment]，[comment]"。
     */
    public static final String ACTION_UNLOCK_PHONE = "com.wb.extrotator.UNLOCK_PHONE";
    /**
     * [comment]「[comment]」[comment]。
     *
     * <p>[comment] {@link #ACTION_UNLOCK_PHONE} [comment]。[comment]Yes[comment]「[comment]」[comment]
     * [comment]：[comment]「[comment]」[comment]，[comment]
     * [comment]（[comment]：[comment]，[comment]Yes[comment]）。
     */
    public static final String ACTION_LOCK_PHONE = "com.wb.extrotator.LOCK_PHONE";

    private static final String CH_ID = "extrot";
    private static final String CH_PROMPT = "extrot_prompt";
    private static final int NOTI_ID = 1001;
    private static final int NOTI_PROMPT = 1002;

    /** [comment] */
    public static volatile String LAST_LOG = "";
    /** [comment]YesNo[comment]（[comment]） */
    public static volatile boolean RUNNING = false;

    /** [comment]。[comment] DisplayListener [comment]，[comment] */
    private static final long POLL_MS = 1500L;
    /**
     * [comment]。
     * [comment]（getRotation() [comment]Yes[comment]），
     * [comment]Yes[comment]"[comment]"，[comment]。
     */
    private static final long STABLE_MS = 400L;
    /** [comment]（[comment]，[comment] MIN_SETTLE_MS） */
    private static final long SETTLE_MS = 2500L;
    /** [comment]：[comment]，[comment]Allow[comment] */
    private static final long MIN_SETTLE_MS = 800L;
    private static final long REARM_MS = 15000L;  // TextAllowText
    private static final int MAX_RETRY = 2;       // Text
    /** [comment]：[comment]，[comment] */
    private static final long EVAL_DEBOUNCE_MS = 150L;

    /**
     * [comment]「[comment]」（[comment] / [comment]）[comment]，[comment]。
     * [comment]Yes[comment]：[comment]
     * [comment]，[comment]。
     */
    private static final long TOPO_SETTLE_MS = 900L;
    /** [comment]，[comment] */
    private static final long REASSERT_MIN_GAP_MS = 2500L;
    /**
     * [comment]，[comment]"[comment]"[comment]，[comment]。
     * [comment] {@link #SETTLE_MS} [comment]：[comment]，[comment]。
     */
    private static final long TOPO_WAIT_MS = 1300L;
    /** [comment]Failed[comment]（[comment]，[comment]） */
    private static final int MAX_GEOM_REPAIR = 3;
    /** [comment]「[comment]」[comment]，[comment]Allow[comment] */
    private static final long GEOM_BACKOFF_MS = 60000L;
    /** [comment]，[comment]"[comment]" */
    private static final long LOCK_DIVERGE_GRACE_MS = 3000L;
    /** [comment]「[comment]YesNo[comment]」[comment] */
    private static final long LOCK_CHECK_MS = 2500L;

    /** Default[comment]，Allow[comment] */
    private static final long POLICY_GRACE_MS = 4000L;

    /**
     * [comment]Yes static —— [comment]Settings[comment] startService，
     * [comment]Reset，[comment]"[comment]Settings[comment]"。
     */
    private static boolean sExtPresent = false;
    private static boolean sPrompted = false;

    private DisplayManager dm;
    private Handler handler;
    private Prefs prefs;
    private ExecutorService exec;

    /** [comment] */
    private int lastOrientKey = -1;
    private long orientSince = 0L;

    /**
     * [comment]「[comment]」[comment]。
     * <p>[comment]：[comment] / [comment]，[comment]，
     * [comment] rotation [comment] —— [comment]。
     */
    private boolean lastPhonePortrait = false;
    private boolean lastPhonePortraitValid = false;
    /** Default[comment]Yes[comment]Yes[comment]（[comment] OFF→ON [comment]） */
    private boolean lastDefOn = false;
    /** Default[comment] */
    private long defOnAt = 0L;
    private int lastLoggedKey = -1;
    /** "[comment] vs [comment]"[comment]（-1 = [comment]），[comment] */
    private int lastMismatchKey = -1;

    /** [comment]「[comment]」[comment]，[comment] */
    private long lastDefaultChangeAt = 0L;

    /** [comment] evaluate [comment]：[comment]，[comment]"[comment]"[comment] */
    private boolean firstEvalDone = false;

    /** [comment]「[comment]」，[comment] / [comment] */
    private String lastTopology = "";
    /** [comment]（[comment]） */
    private boolean pendingRepair = false;
    /** [comment] */
    private long topoChangedAt = 0L;
    /** [comment]（[comment]），[comment]YesNo[comment] */
    private long lastLockIssuedAt = 0L;
    /** [comment] */
    private long lastReassertAt = 0L;
    /** [comment]Failed[comment] */
    private int geomRepairCount = 0;
    /** [comment]"[comment]"[comment]，[comment] */
    private boolean geomCheckUnreliable = false;

    /** [comment]「[comment]」[comment] */
    private long lockWriteAt = 0L;
    /** [comment]「[comment]」 */
    private int lockDivergeStrikes = 0;
    private long lastLockCheckAt = 0L;

    /** [comment] force [comment]（[comment] scheduleEval(delay, true) [comment]） */
    private boolean pendingForce = false;

    /** [comment] */
    private final SparseArray<ExtState> states = new SparseArray<>();
    /** fixed-to-user-rotation [comment]（[comment]SettingsYes[comment]，[comment]） */
    private final Set<Integer> fixToUserDone = new HashSet<>();

    private String lastNotiText = "";

    private static class ExtState {
        int target = -1;
        long issuedAt = 0L;
        int retries = 0;
    }

    /**
     * 「[comment]」[comment]。
     * <p>[comment] {@link #pollRunnable} [comment]：[comment]Yes[comment]，[comment]、[comment]
     * [comment]Yes"[comment]/[comment]"。[comment] Runnable [comment] Handler [comment]，
     * [comment] removeCallbacks [comment]，[comment]。
     */
    private final Runnable evalRunnable = new Runnable() {
        @Override
        public void run() {
            boolean force = pendingForce;
            pendingForce = false;
            try {
                evaluate(force);
            } catch (Throwable t) {
                Log.w(TAG, "evaluate failed", t);
            }
        }
    };

    /** [comment]：[comment]，[comment] */
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            evalRunnable.run();
            handler.postDelayed(this, POLL_MS);
        }
    };

    /**
     * [comment]"[comment]"：[comment]，[comment]。
     *
     * @param delayMs 0 = [comment]
     */
    private void scheduleEval(long delayMs) {
        scheduleEval(delayMs, false);
    }

    /**
     * @param force true [comment] / [comment]，[comment]
     */
    private void scheduleEval(long delayMs, boolean force) {
        if (handler == null) {
            return;
        }
        if (force) {
            pendingForce = true;   // Text remove Text，force Text
        }
        handler.removeCallbacks(evalRunnable);
        if (delayMs <= 0L) {
            handler.post(evalRunnable);
        } else {
            handler.postDelayed(evalRunnable, delayMs);
        }
    }

    /**
     * [comment]。
     * <p><b>[comment]（display 0）</b>：[comment]＝[comment]，[comment] ——
     * [comment]Yes[comment]。<br>
     * [comment]Yes[comment]，[comment]，[comment]
     * （[comment]"[comment]"[comment]，[comment]）。
     */
    private final DisplayManager.DisplayListener displayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
            evaluate(false);
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            states.remove(displayId);
            fixToUserDone.remove(displayId);
            evaluate(false);
        }

        @Override
        public void onDisplayChanged(int displayId) {
            if (displayId != Display.DEFAULT_DISPLAY) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastDefaultChangeAt < EVAL_DEBOUNCE_MS) {
                return;   // Text，Text
            }
            lastDefaultChangeAt = now;
            Log.d(TAG, "default display changed → re-evaluate");
            scheduleEval(0L);
        }
    };

    // ---------------------------------------------------------------- [comment]

    public static void start(Context c) {
        try {
            Intent i = new Intent(c, RotationService.class).setAction(ACTION_START);
            c.startForegroundService(i);
        } catch (Throwable ignored) {
        }
    }

    public static boolean isRunning() {
        return RUNNING;
    }

    public static void stop(Context c) {
        try {
            Intent i = new Intent(c, RotationService.class).setAction(ACTION_STOP);
            c.startService(i);
        } catch (Throwable ignored) {
        }
    }

    public static void manual(Context c, int rotation) {
        try {
            Intent i = new Intent(c, RotationService.class)
                    .setAction(ACTION_MANUAL)
                    .putExtra("rotation", rotation);
            c.startService(i);
        } catch (Throwable ignored) {
        }
    }

    public static void repair(Context c) {
        try {
            Intent i = new Intent(c, RotationService.class).setAction(ACTION_REPAIR);
            c.startService(i);
        } catch (Throwable ignored) {
        }
    }

    /** [comment] */
    public static void applySizeNow(Context c) {
        try {
            c.startService(new Intent(c, RotationService.class).setAction(ACTION_APPLY_SIZE));
        } catch (Throwable ignored) {
        }
    }

    /** [comment] */
    public static void resetSizeNow(Context c) {
        try {
            c.startService(new Intent(c, RotationService.class).setAction(ACTION_RESET_SIZE));
        } catch (Throwable ignored) {
        }
    }

    /** [comment]：[comment]「[comment]」[comment] */
    public static void testPrompt(Context c) {
        try {
            c.startService(new Intent(c, RotationService.class).setAction(ACTION_TEST_PROMPT));
        } catch (Throwable ignored) {
        }
    }

    /**
     * [comment]「[comment]」[comment]（[comment]，[comment]）。
     * [comment]、[comment]「[comment]」[comment]，
     * [comment] {@link #ACTION_UNLOCK_PHONE} [comment] {@link #ACTION_LOCK_PHONE}。
     */
    public static void restorePhoneRotation(Context c) {
        try {
            c.startService(new Intent(c, RotationService.class).setAction(ACTION_UNLOCK_PHONE));
        } catch (Throwable ignored) {
        }
    }

    /**
     * [comment] {@link Prefs#getPhoneRotation()} [comment]。
     * <p>「[comment]」[comment]；[comment]「[comment]」YesNoEnableNone[comment]。
     */
    public static void lockPhoneNow(Context c) {
        try {
            c.startForegroundService(
                    new Intent(c, RotationService.class).setAction(ACTION_LOCK_PHONE));
        } catch (Throwable ignored) {
        }
    }

    /** [comment] / [comment]「[comment]」「[comment]」[comment] */
    public static void answerPrompt(Context c, boolean yes, boolean noAsk) {
        try {
            Intent i = new Intent(c, RotationService.class)
                    .setAction(yes ? ACTION_PROMPT_YES : ACTION_PROMPT_NO)
                    .putExtra("noAsk", noAsk);
            c.startService(i);
        } catch (Throwable ignored) {
        }
    }

    /** [comment]（[comment]） */
    public static void dismissPromptNotification(Context c) {
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.cancel(NOTI_PROMPT);
            }
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- [comment]

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new Prefs(this);
        handler = new Handler(Looper.getMainLooper());
        dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        exec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "extrot-shell");
            t.setDaemon(true);
            return t;
        });

        createChannels();
        startForeground(NOTI_ID, buildNotification("Text…"));
        RUNNING = true;

        if (dm != null) {
            dm.registerDisplayListener(displayListener, handler);
        }
        handler.post(pollRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();

        if (ACTION_STOP.equals(action)) {
            prefs.setEnabled(false);
            // [comment]，[comment]，[comment]
            exec.execute(() -> {
                try {
                    if (prefs.isLockPhoneRotation()) {
                        ShellRunner.run("settings put system accelerometer_rotation 1");
                    }
                } catch (Throwable ignored) {
                }
                handler.post(this::stopSelf);
            });
            updateNotification("TextDisableText");
            return START_NOT_STICKY;
        }

        if (ACTION_UNLOCK_PHONE.equals(action)) {
            // [comment]。[comment] action [comment]Yes"[comment]"，
            // [comment]，[comment]，[comment]。
            final boolean keep = prefs.shouldKeepRunning();
            exec.execute(() -> {
                try {
                    ShellRunner.run("settings put system accelerometer_rotation 1");
                    Log.d(TAG, "phone auto-rotate restored (keep=" + keep + ")");
                } catch (Throwable ignored) {
                }
                if (!keep) {
                    handler.post(this::stopSelf);
                }
            });
            return START_NOT_STICKY;
        }

        if (ACTION_LOCK_PHONE.equals(action)) {
            // [comment]：[comment]"[comment]"。
            // [comment]Yes[comment]，shouldKeepRunning() [comment] true，[comment]。
            applyPhoneRotationLock();
            return START_STICKY;
        }

        if (ACTION_MANUAL.equals(action) && intent != null) {
            applyManual(intent.getIntExtra("rotation", -1));
            return START_STICKY;
        }

        if (ACTION_REPAIR.equals(action)) {
            repairAll();
            return START_STICKY;
        }

        if (ACTION_APPLY_SIZE.equals(action)) {
            // [comment]「[comment]」/ [comment]：[comment]Yes[comment]，
            // [comment]"[comment]None[comment]"，[comment]
            applySizeAsync("Text", false);
            return START_STICKY;
        }

        if (ACTION_RESET_SIZE.equals(action)) {
            resetSizeAsync("Text");
            return START_STICKY;
        }

        if (ACTION_TEST_PROMPT.equals(action)) {
            askAboutSize(DisplayUtil.externalDisplays(dm));
            return START_STICKY;
        }

        if (ACTION_PROMPT_YES.equals(action)) {
            prefs.setResolutionEnabled(true);
            if (intent != null && intent.getBooleanExtra("noAsk", false)) {
                prefs.setAskOnConnect(false);
            }
            sPrompted = true;
            dismissPromptNotification(this);
            // [comment]，[comment]"[comment]"
            boolean bound = !DisplayUtil.externalDisplays(dm).isEmpty();
            applySizeAsync("Text", bound);
            return START_STICKY;
        }

        if (ACTION_PROMPT_NO.equals(action)) {
            if (intent != null && intent.getBooleanExtra("noAsk", false)) {
                prefs.setAskOnConnect(false);
            }
            sPrompted = true;
            dismissPromptNotification(this);
            updateNotification("Text");
            return START_STICKY;
        }

        // ACTION_START（[comment]、[comment]、[comment]Settings[comment]）
        //
        // [comment]Yes isLockPhoneRotation() [comment]Yes isEnabled()：[comment]Yes[comment]，
        // 「[comment]」[comment]（[comment]）[comment]。
        //
        // ⚠ [comment]，[comment]Yes「[comment]」：
        //   · [comment]（[comment] / [comment] / [comment]）[comment]，[comment]Settings；
        //   · [comment]（intent == null）[comment] —— [comment]Yes[comment]
        //     （[comment]：[comment] → [comment] user_rotation 1 → [comment] 1920×1080，
        //     [comment]Yes「[comment] 90°」）。
        if (ExtControlMode.active()) {
            Log.i(TAG, "Text → Text");
            return START_STICKY;
        }
        if (prefs.isLockPhoneRotation() && intent != null) {
            applyPhoneRotationLock();
        }
        lastOrientKey = -1;      // Text
        orientSince = 0L;
        evaluate(true);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        RUNNING = false;
        if (handler != null) {
            handler.removeCallbacks(pollRunnable);
            handler.removeCallbacks(evalRunnable);
        }
        if (dm != null) {
            try {
                dm.unregisterDisplayListener(displayListener);
            } catch (Throwable ignored) {
            }
        }
        if (exec != null) {
            exec.shutdown();   // AllowText
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ---------------------------------------------------------------- [comment]

    /**
     * [comment] + [comment] settings [comment]，
     * [comment]「[comment]」[comment]。
     * [comment]「[comment]」[comment]。
     */
    private void applyPhoneRotationLock() {
        if (!prefs.isLockPhoneRotation()) {
            return;
        }
        final int rot = prefs.getPhoneRotation();
        lockWriteAt = System.currentTimeMillis();
        exec.execute(() -> {
            try {
                ShellRunner.run("settings put system accelerometer_rotation 0");
                ShellRunner.run("settings put system user_rotation " + rot);
                Log.d(TAG, "phone rotation locked to " + rot);
            } catch (Throwable t) {
                Log.e(TAG, "lock phone rotation failed", t);
            }
        });
    }

    /**
     * [comment]「[comment]」[comment]。
     *
     * <p>[comment]「[comment]」[comment]，{@code accelerometer_rotation} [comment]
     * [comment] 1，[comment] —— [comment]、[comment]，
     * [comment]"[comment]"[comment]，[comment]。
     *
     * <p>[comment]Yes<b>[comment]</b>：[comment]，[comment]
     * [comment]，[comment]Yes[comment]（[comment]）。
     */
    /**
     * [comment]「[comment]」：[comment]，[comment]。
     *
     * <p>[comment] {@code firstEvalDone} / {@code lastDefOn} [comment]Update：[comment]Yes「[comment]
     * [comment]」[comment]，[comment]「[comment]」[comment]；
     * [comment]，[comment]「[comment]」[comment]。
     */
    private void trackTopologyOnly() {
        try {
            List<Display> ext = DisplayUtil.externalDisplays(dm);
            Display def = DisplayUtil.defaultDisplay(dm);
            String t = topologyKey(def, ext);
            if (t != null && !t.equals(lastTopology)) {
                Log.i(TAG, "Text → TextUpdateText: " + t);
                lastTopology = t;
            }
        } catch (Throwable t) {
            // [comment]FailedNone[comment]：[comment] evaluate [comment]"[comment]"[comment]，
            // [comment]Yes[comment]，[comment]
        }
    }

    private void checkPhoneLockDivergence(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastLockCheckAt < LOCK_CHECK_MS) {
            return;
        }
        lastLockCheckAt = now;

        if (!prefs.isLockPhoneRotation()) {
            lockDivergeStrikes = 0;
            return;
        }
        // [comment]，[comment]
        if (now - lockWriteAt < LOCK_DIVERGE_GRACE_MS) {
            lockDivergeStrikes = 0;
            return;
        }

        int accel;
        try {
            accel = Settings.System.getInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 1);
        } catch (Throwable t) {
            return;
        }
        if (accel != 1) {
            lockDivergeStrikes = 0;
            return;
        }
        // [comment]"[comment]"[comment]，[comment]
        lockDivergeStrikes++;
        if (lockDivergeStrikes < 2) {
            return;
        }
        lockDivergeStrikes = 0;
        Log.d(TAG, "system auto-rotate re-enabled → turn off phone rotation lock");
        prefs.setLockPhoneRotation(false);
        updateNotification("Text，TextDisable「Text」");
    }

    // ---------------------------------------------------------------- [comment]

    /**
     * [comment]，[comment]<b>[comment]</b>Settings[comment]Settings。
     *
     * <p>[comment] {@link #syncResFromExt} <b>[comment]</b>[comment]：[comment]「[comment]」
     * [comment]，[comment]，[comment]。
     *
     * <p>[comment]「[comment]」[comment]（[comment] {@link #handleExtConnection}），
     * [comment] —— [comment]（[comment]、[comment]）[comment]，
     * [comment]，[comment]Yes「[comment]」。
     *
     * <p>⚠ [comment]<b>[comment]</b>「[comment]」[comment]「[comment]」：[comment]Yes[comment]，[comment]；
     * [comment]。
     *
     * @return YesNo[comment]。[comment]，[comment]。
     */
    private boolean applyProfileIfAny(List<Display> ext) {
        if (ext == null || ext.isEmpty()) {
            return false;
        }
        Display best = DisplayUtil.largestByPanel(ext);
        if (best == null) {
            return false;
        }
        DisplayProfile.Item it = DisplayProfile.load(this, DisplayProfile.signature(best));
        if (it == null) {
            return false;
        }
        boolean changed = it.applyTo(prefs);
        // ⚠ [comment]「[comment]」[comment]，[comment]Settings。
        // [comment]（[comment] / [comment]）；[comment]Yes[comment]。
        Log.d(TAG, "display profile hit " + DisplayProfile.label(best)
                + " port=" + it.portrait + " land=" + it.landscape
                + " fixToUser=" + it.fixToUser
                + " resAuto=" + it.resAuto
                + " res=" + (it.resEnabled ? (it.resW + "x" + it.resH) : "Text")
                + " dpi=" + it.resDensity
                + " changed=" + changed);
        updateNotification("TextSaveTextSettings");
        return true;
    }

    /**
     * [comment]「[comment] / [comment]」[comment]。
     * [comment]，[comment]。
     */
    private void handleExtConnection(List<Display> ext, boolean isFirstEval) {
        boolean has = !ext.isEmpty();

        if (isFirstEval) {
            // [comment]"[comment]"[comment]，No[comment]
            sExtPresent = has;
            if (has) {
                if (prefs.isResolutionEnabled() || prefs.isAskOnConnect()) {
                    syncResFromExt(ext);
                }
                if (prefs.isResolutionEnabled()) {
                    applySizeAsync("Text");
                } else if (prefs.isAskOnConnect() && !sPrompted) {
                    sPrompted = true;
                    askAboutSize(ext);
                }
            } else if (prefs.isSizeApplied() && prefs.isResBoundToExt() && prefs.isAutoResetSize()) {
                // [comment]Yes"[comment]"[comment]，[comment] → [comment]，[comment]
                resetSizeAsync("Text");
            }
            return;
        }

        if (has && !sExtPresent) {
            sExtPresent = true;
            sPrompted = false;
            Log.d(TAG, "external display connected");
            // [comment]Yes[comment]Yes[comment] —— [comment]Settings[comment]
            boolean profiled = applyProfileIfAny(ext);
            // [comment]，[comment]Yes[comment]Yes[comment]
            // （[comment] "[comment]" [comment]，[comment]）
            syncResFromExt(ext);
            if (profiled) {
                // [comment] = [comment]，[comment]。
                // [comment]；[comment]。
                sPrompted = true;
                if (prefs.isResolutionEnabled()) {
                    applySizeAsync("TextSaveTextSettings");
                }
            } else if (prefs.isAskOnConnect()) {
                sPrompted = true;
                askAboutSize(ext);
            } else if (prefs.isResolutionEnabled()) {
                applySizeAsync("Text");
            }
            return;
        }

        if (!has && sExtPresent) {
            sExtPresent = false;
            sPrompted = false;
            Log.d(TAG, "external display disconnected");
            if (prefs.isAutoResetSize() && prefs.isSizeApplied() && prefs.isResBoundToExt()) {
                resetSizeAsync("Text");
            }
        }
    }

    /**
     * [comment]，[comment]，[comment]Settings。
     *
     * <p>[comment]Yes[comment]（1920×1080 [comment]），[comment]Yes[comment]，
     * [comment] 1080×1920。[comment]。
     *
     * <p>[comment]（{@link Prefs#isAutoMatchExt()} = false）[comment]，
     * [comment]。
     */
    private void syncResFromExt(List<Display> ext) {
        if (!prefs.isAutoMatchExt() || ext == null || ext.isEmpty()) {
            return;
        }
        int[] best = null;
        for (Display d : ext) {
            int[] s = DisplayUtil.suggestedInternalSize(d);
            if (s[0] <= 0 || s[1] <= 0) {
                continue;
            }
            if (best == null || (long) s[0] * s[1] > (long) best[0] * best[1]) {
                best = s;
            }
        }
        if (best == null) {
            return;
        }
        if (best[0] == prefs.getResWidth() && best[1] == prefs.getResHeight()) {
            return;
        }
        Log.d(TAG, "auto match ext size => " + best[0] + "x" + best[1]);
        prefs.setResSize(best[0], best[1]);
    }

    /** [comment]：[comment]Yes[comment]（[comment] ≥ [comment]） */
    private int[] effSize() {
        return ScreenSizeUtil.portrait(prefs.getResWidth(), prefs.getResHeight());
    }

    // ---------------------------------------------------------------- [comment]

    private void applySizeAsync(String why) {
        applySizeAsync(why, true);
    }

    /**
     * @param boundToExt YesNo「[comment]」[comment]。
     *                   [comment] {@link Prefs#isResBoundToExt()} —— [comment]「[comment]」[comment] false，
     *                   [comment]"[comment]"[comment]。
     */
    private void applySizeAsync(String why, boolean boundToExt) {
        if (!ShellRunner.isReady()) {
            updateNotification("Text Shizuku Text（Text App Text）");
            return;
        }
        // [comment] —— 1920×1080 [comment] 1080×1920
        final int[] p = effSize();
        final int w = p[0];
        final int h = p[1];
        final int dpi = prefs.getResDensity();
        exec.execute(() -> {
            try {
                String out = ScreenSizeUtil.apply(w, h, dpi);
                prefs.setSizeApplied(true);
                prefs.setResBoundToExt(boundToExt);
                if (dpi > 0) {
                    prefs.setDensityApplied(true);
                }
                Log.d(TAG, "apply size " + w + "x" + h + " dpi=" + dpi + " bound=" + boundToExt + " => " + out);
                handler.post(() -> updateNotification("Text → " + w + "×" + h + "（" + why + "）"));
            } catch (Throwable t) {
                Log.e(TAG, "apply size failed", t);
            }
        });
    }

    private void resetSizeAsync(String why) {
        if (!ShellRunner.isReady()) {
            updateNotification("Text Shizuku Text（Text App Text）");
            return;
        }
        final boolean alsoDpi = prefs.isDensityApplied() || prefs.getResDensity() > 0;
        exec.execute(() -> {
            try {
                String out = ScreenSizeUtil.reset(alsoDpi);
                prefs.setSizeApplied(false);
                prefs.setResBoundToExt(false);
                prefs.setDensityApplied(false);
                Log.d(TAG, "reset size => " + out);
                handler.post(() -> updateNotification("Text（" + why + "）"));
            } catch (Throwable t) {
                Log.e(TAG, "reset size failed", t);
            }
        });
    }

    /**
     * [comment]YesNo[comment]。
     * [comment]：[comment]（[comment]）+ [comment]（[comment]）。
     */
    private void askAboutSize(List<Display> ext) {
        // [comment]Yes[comment]（[comment]），[comment]
        // [comment]、[comment]Yes[comment]
        final int[] p = effSize();
        final int w = p[0];
        final int h = p[1];
        final String desc = extDesc(ext, w, h);

        showPromptNotification(desc, w, h);
        tryShowDialog(desc, w, h);
    }

    /** [comment]：[comment] */
    private String extDesc(List<Display> ext, int targetW, int targetH) {
        if (ext == null || ext.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Text ");
        for (int i = 0; i < ext.size(); i++) {
            if (i > 0) {
                sb.append("、");
            }
            Display d = ext.get(i);
            int[] n = DisplayUtil.nativeSize(d);
            sb.append("ID ").append(d.getDisplayId())
                    .append(" Text ").append(n[0]).append("×").append(n[1])
                    .append("（Text 90° Text ").append(n[1]).append("×").append(n[0]).append("）");
        }
        // [comment]"[comment]"，No[comment]
        int cw = prefs.getResWidth();
        int ch = prefs.getResHeight();
        if (cw > ch) {
            sb.append(" · Text ").append(cw).append("×").append(ch)
                    .append(" → ").append(targetW).append("×").append(targetH);
        }
        return sb.toString();
    }

    /**
     * [comment]。
     * <p>[comment] —— [comment]，[comment]。
     */
    private void tryShowDialog(String desc, int w, int h) {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm == null || !pm.isInteractive()) {
                Log.d(TAG, "skip dialog: screen off");
                return;
            }
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && km.isKeyguardLocked()) {
                Log.d(TAG, "skip dialog: keyguard locked");
                return;
            }
        } catch (Throwable ignored) {
        }

        final Intent i = new Intent(this, PromptActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                .putExtra(PromptActivity.EXTRA_DESC, desc)
                .putExtra(PromptActivity.EXTRA_W, w)
                .putExtra(PromptActivity.EXTRA_H, h);

        boolean overlay = false;
        try {
            overlay = Settings.canDrawOverlays(this);
        } catch (Throwable ignored) {
        }

        // A. [comment]「[comment]」[comment]，[comment] Activity Yes[comment]Allow[comment]
        if (overlay) {
            try {
                startActivity(i);
                Log.d(TAG, "prompt dialog started directly (overlay granted)");
                return;
            } catch (Throwable t) {
                Log.w(TAG, "startActivity blocked", t);
            }
        }

        // B. [comment]，[comment] shell [comment] —— shell [comment]
        final String cmd = "am start -n " + getPackageName() + "/.PromptActivity"
                + " --activity-single-top"
                + " --es " + PromptActivity.EXTRA_DESC + " '" + safe(desc) + "'"
                + " --ei " + PromptActivity.EXTRA_W + " " + w
                + " --ei " + PromptActivity.EXTRA_H + " " + h;
        final boolean hasOverlay = overlay;
        exec.execute(() -> {
            String out = ShellRunner.run(cmd);
            Log.d(TAG, "shell am start (overlay=" + hasOverlay + ") => " + out);
        });
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("'", "").replace("\n", " ");
    }

    // ---------------------------------------------------------------- [comment]

    /**
     * [comment]：[comment]「[comment]」，[comment]「[comment]」。
     *
     * <p>v4.13 [comment]「[comment]」[comment] —— [comment]Yes[comment]；
     * [comment]（[comment] {@code isEnabled()} [comment] return）。
     */
    private int targetFor(boolean phonePortrait) {
        return phonePortrait ? prefs.getPortraitAngle() : prefs.getLandscapeAngle();
    }

    /** [comment] */
    private String sizeTail() {
        if (!prefs.isSizeApplied()) {
            return "";
        }
        int[] p = effSize();
        return " · Text " + p[0] + "×" + p[1];
    }

    /**
     * @param force true [comment]，[comment]（[comment] / [comment]）
     */
    private void evaluate(boolean force) {
        // [comment]，[comment] —— [comment] / [comment]，
        // [comment]。[comment]，[comment]。
        //
        // ⚠ [comment]Yes return：**[comment]**。[comment] return [comment] lastTopology [comment]
        // 「[comment]」[comment]，[comment]「[comment]」→ pendingRepair
        // → [comment] free→lock [comment]，[comment]（[comment]Yes「[comment]、[comment]」）。
        if (ExtControlMode.active()) {
            trackTopologyOnly();
            return;
        }
        List<Display> ext = DisplayUtil.externalDisplays(dm);
        Display def = DisplayUtil.defaultDisplay(dm);

        boolean first = !firstEvalDone;
        firstEvalDone = true;
        handleExtConnection(ext, first);

        // ---- [comment]「[comment]」[comment]
        checkPhoneLockDivergence(force);

        // ---- [comment]（[comment] / [comment] / [comment]）。
        // [comment]，[comment] rotation [comment] ——
        // [comment]Yes"[comment]、[comment]"[comment]，[comment]。
        String topology = topologyKey(def, ext);
        if (!topology.equals(lastTopology)) {
            boolean known = !lastTopology.isEmpty();
            lastTopology = topology;
            geomRepairCount = 0;
            geomCheckUnreliable = false;
            // [comment] —— [comment]，[comment] OFF→ON
            // [comment]。[comment]"[comment]"[comment]。
            boolean defOn = def == null || def.getState() == Display.STATE_ON;
            if (defOn && !lastDefOn) {
                defOnAt = System.currentTimeMillis();   // Text：Text rotation TextYesText
            }
            lastDefOn = defOn;
            if (known && !first && defOn) {
                Log.d(TAG, "topology changed → schedule full repair");
                pendingRepair = true;
                topoChangedAt = System.currentTimeMillis();
                scheduleEval(TOPO_SETTLE_MS, true);
                // [comment]；[comment]Yes[comment]
                if (prefs.isSizeApplied() && prefs.isResBoundToExt()
                        && DisplayUtil.sizeOverrideInfo(def) == null) {
                    applySizeAsync("Text", true);
                }
            }
        }

        // ---- [comment]。
        // [comment]：[comment]，[comment]「[comment]」[comment]
        // （STABLE_MS [comment]），[comment] —— [comment]。
        //
        // [comment]Yes「[comment]」：[comment]Yes[comment]，[comment] getRotation() [comment]
        // [comment] 0，[comment]「[comment]」，[comment]（[comment] 270°）。
        Display phone = DisplayUtil.activePhoneDisplay(dm);
        boolean phoneOn = DisplayUtil.isOn(phone);

        // [comment]：[comment]。
        //
        // ⚠ 「[comment]」[comment]Yes[comment] —— [comment]Yes[comment]「[comment]」[comment]，
        // [comment]。[comment]，[comment]
        // [comment]，[comment]Yes「[comment]，[comment]Yes[comment]」。
        //
        // [comment]，[comment]（[comment]，[comment] QQ），[comment]，
        // [comment]，No[comment]、[comment]，[comment]Yes[comment]。
        boolean phoneKnown = phoneOn;
        boolean phonePortrait = lastPhonePortrait;

        long now = System.currentTimeMillis();

        if (phoneKnown) {
            boolean p = DisplayUtil.isPhonePortrait(phone);

            // ---- [comment]「Default[comment]」[comment]，[comment]。
            //
            // [comment]Yes[comment]：[comment]（[comment]）[comment]，[comment] rotation [comment]
            // [comment] 0，[comment] user_rotation [comment]Yes 1 —— [comment]，[comment] 270°。
            //
            // ⚠ [comment]None[comment]：user_rotation [comment]Yes[comment]「[comment]」[comment]，
            // None[comment]「[comment]」，[comment]。
            // [comment] POLICY_GRACE_MS，[comment]。
            if (phone != null && phone.getDisplayId() == Display.DEFAULT_DISPLAY
                    && now - defOnAt < POLICY_GRACE_MS) {
                int policy = DisplayUtil.policyRotation(getContentResolver());
                if (policy >= 0) {
                    boolean byPolicy = DisplayUtil.isPortraitAt(phone, policy);
                    if (byPolicy != p) {
                        // [comment]（[comment]），[comment]"[comment]"[comment]，
                        // No[comment] 1.5 [comment]
                        int mk = byPolicy ? 1 : 0;
                        if (mk != lastMismatchKey) {
                            lastMismatchKey = mk;
                            Log.w(TAG, "Text：Text，Text"
                                    + "（user_rotation=" + policy + " → " + (byPolicy ? "Text" : "Text")
                                    + "；Text rotation=" + phone.getRotation()
                                    + " → " + (p ? "Text" : "Text") + "）");
                        }
                        p = byPolicy;
                    } else {
                        lastMismatchKey = -1;   // Text，Text
                    }
                }
            }

            lastPhonePortrait = p;
            lastPhonePortraitValid = true;
            phonePortrait = p;

            int orientKey = p ? 1 : 0;
            if (orientKey != lastOrientKey) {
                lastOrientKey = orientKey;
                orientSince = now;
                Log.d(TAG, "phone orientation -> " + (p ? "portrait" : "landscape")
                        + " (by " + DisplayUtil.kindOf(dm, phone) + ") "
                        + DisplayUtil.orientationDebug(phone));
                if (!force) {
                    // [comment]，[comment]
                    scheduleEval(STABLE_MS + 30L);
                }
            }
        }

        if (!prefs.isEnabled()) {
            // [comment]，[comment] / [comment] —— [comment]
            String busy;
            if (prefs.isResolutionEnabled()) {
                busy = ext.isEmpty() ? "Text…（Text）"
                                     : "Text" + sizeTail();
            } else if (prefs.isLockPhoneRotation()) {
                busy = "Text（Text）";
            } else {
                busy = "TextDisableText";
            }
            updateNotification(busy);
            return;
        }

        if (!ShellRunner.isReady()) {
            updateNotification("Text Shizuku Text（Text App Text）");
            return;
        }

        if (ext.isEmpty()) {
            states.clear();
            pendingRepair = false;
            updateNotification("Text" + sizeTail());
            return;
        }

        // [comment]（[comment] / [comment]）→ [comment]。
        // [comment]，[comment]"[comment]"[comment] 270°。
        if (!lastPhonePortraitValid) {
            updateNotification("Text，Text");
            return;
        }

        if (!force && now - orientSince < STABLE_MS) {
            updateNotification("Text，Text…");
            return;
        }

        int target = targetFor(phonePortrait);

        // ---- [comment]。
        // [comment]（[comment]"[comment]"[comment]），[comment] free → lock；
        // [comment]，[comment]。
        if (pendingRepair) {
            // [comment]：①[comment] ~0.7s [comment]；②[comment]
            // [comment] TOPO_WAIT_MS。[comment] 5s [comment]，[comment]。
            boolean settled = now - topoChangedAt >= 700L
                    && (lastLockIssuedAt == 0L || now - lastLockIssuedAt >= TOPO_WAIT_MS);
            if (!settled && now - topoChangedAt < 5000L) {
                scheduleEval(400L, true);
                updateNotification("Text，Text…");
                return;
            }
            pendingRepair = false;
            lastReassertAt = now;
            geomRepairCount = 0;
            Log.d(TAG, "topology re-assert → full repair");
            repairAllInternal(target);
            updateNotification("Text，Text → " + DisplayUtil.degrees(target) + "°");
            return;
        }

        // ---- [comment]：[comment] ≠ [comment]。
        // [comment]（getRealSize [comment]）[comment]
        // [comment]，[comment] → [comment]。
        //
        // ⚠ [comment]「[comment]」[comment]：[comment]Yes[comment]，
        // [comment]Yes<b>[comment]</b>。None[comment]「[comment]」、
        // [comment] free → lock [comment]，[comment] free [comment]，[comment]。
        boolean angleAtTarget = true;
        for (Display d : ext) {
            if (d.getRotation() != target) {
                angleAtTarget = false;
                break;
            }
        }
        if (angleAtTarget && !geomCheckUnreliable && !geometryOk(ext, target)) {
            if (now - lastReassertAt > GEOM_BACKOFF_MS) {
                geomRepairCount = 0;   // Text，AllowText
            }
            if (geomRepairCount < MAX_GEOM_REPAIR
                    && now - lastReassertAt >= REASSERT_MIN_GAP_MS) {
                geomRepairCount++;
                lastReassertAt = now;
                Log.d(TAG, "geometry mismatch → full repair #" + geomRepairCount);
                repairAllInternal(target);
                updateNotification("Text，Text → " + DisplayUtil.degrees(target) + "°");
                return;
            }
            if (geomRepairCount >= MAX_GEOM_REPAIR) {
                // [comment]Yes"[comment]"，[comment]Yes[comment]，[comment]Yes[comment]
                // [comment]。[comment]，[comment]；
                // [comment]。
                geomCheckUnreliable = true;
                Log.w(TAG, "geometry check seems unreliable for this display → disabled");
            }
        } else {
            geomRepairCount = 0;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Text").append(phonePortrait ? "Text" : "Text")
                .append(" → Text ").append(DisplayUtil.degrees(target)).append("° | ");

        boolean issued = false;
        for (Display d : ext) {
            int id = d.getDisplayId();
            ExtState st = states.get(id);
            if (st == null) {
                st = new ExtState();
                states.put(id, st);
            }

            int cur = d.getRotation();

            // ---- [comment]：[comment]，[comment]。
            // [comment]"[comment]"（[comment]Yes[comment]），[comment]
            // [comment] —— No[comment]None[comment] 2.5 [comment]。
            // [comment]Yes"[comment]"，[comment]Yes"[comment]"，[comment]。
            if (!force && now - st.issuedAt < SETTLE_MS) {
                boolean lastCommitted = st.target >= 0
                        && cur == st.target
                        && now - st.issuedAt >= MIN_SETTLE_MS;
                if (!lastCommitted) {
                    sb.append("Text").append(id).append(" Text ");
                    continue;
                }
            }

            if (cur == target) {
                st.target = target;
                st.retries = 0;
                sb.append("Text").append(id).append(" Text ");
                continue;
            }

            // [comment] → [comment]，[comment]
            if (st.target == target) {
                if (now - st.issuedAt > REARM_MS) {
                    st.retries = 0;   // Text，AllowText
                }
                if (st.retries >= MAX_RETRY) {
                    sb.append("Text").append(id).append(" Text(").append(DisplayUtil.degrees(cur)).append("°) ");
                    continue;
                }
                st.retries++;
            } else {
                st.retries = 0;
            }

            if (prefs.isFixToUserRotation() && !fixToUserDone.contains(id)) {
                final int fixId = id;
                exec.execute(() -> ShellRunner.run("wm fixed-to-user-rotation -d " + fixId + " enabled"));
                fixToUserDone.add(id);
            }

            final int lockId = id;
            final int lockTarget = target;
            exec.execute(() -> ShellRunner.run("wm user-rotation -d " + lockId + " lock " + lockTarget));

            st.target = target;
            st.issuedAt = now;
            lastLockIssuedAt = now;
            issued = true;

            sb.append("Text").append(id).append(" ")
                    .append(DisplayUtil.degrees(cur)).append("°→")
                    .append(DisplayUtil.degrees(target)).append("° ");
        }

        String text = sb.toString().trim() + sizeTail();
        updateNotification(text);
        // [comment]"[comment]"[comment]，[comment]
        // "[comment]"[comment]"[comment]"。[comment]None[comment]。
        int logKey = (phonePortrait ? 2 : 0) | (target == 0 ? 1 : 0);
        if (issued || logKey != lastLoggedKey) {
            lastLoggedKey = logKey;
            Log.d(TAG, "Text locked=" + prefs.isLockPhoneRotation()
                    + " phoneOn=" + phoneOn + " → " + text);
        }
    }

    /**
     * [comment]/[comment]（[comment] App [comment]）
     * rotation &lt; 0 [comment]。
     */
    private void applyManual(int rotation) {
        if (!ShellRunner.isReady()) {
            updateNotification("Text Shizuku Text");
            return;
        }
        List<Display> ext = DisplayUtil.externalDisplays(dm);
        if (ext.isEmpty()) {
            updateNotification("Text");
            return;
        }
        final int[] ids = new int[ext.size()];
        for (int i = 0; i < ext.size(); i++) {
            ids[i] = ext.get(i).getDisplayId();
        }
        final int rot = rotation;
        exec.execute(() -> {
            for (int id : ids) {
                if (rot < 0) {
                    ShellRunner.run("wm user-rotation -d " + id + " free");
                    ShellRunner.run("wm fixed-to-user-rotation -d " + id + " default");
                } else {
                    ShellRunner.run("wm fixed-to-user-rotation -d " + id + " enabled");
                    ShellRunner.run("wm user-rotation -d " + id + " lock " + rot);
                }
            }
        });

        long now = System.currentTimeMillis();
        for (int id : ids) {
            ExtState st = states.get(id);
            if (st == null) {
                st = new ExtState();
                states.put(id, st);
            }
            st.issuedAt = now;
            st.retries = 0;
            st.target = rot;
            if (rot < 0) {
                fixToUserDone.remove(id);
            }
        }
        if (rot >= 0) {
            lastOrientKey = -1;  // Text
        }
        updateNotification(rot < 0
                ? "Text"
                : "Text " + DisplayUtil.degrees(rot) + "°");
    }

    /**
     * [comment]：[comment]「[comment]、[comment]」[comment]。
     * [comment]（[comment] 1/4 [comment]）[comment]，[comment]。
     */
    private void repairAll() {
        if (!ShellRunner.isReady()) {
            updateNotification("Text Shizuku Text");
            return;
        }
        List<Display> ext = DisplayUtil.externalDisplays(dm);
        if (ext.isEmpty()) {
            updateNotification("Text");
            return;
        }
        final int target = targetFor(phonePortraitNow());
        lastReassertAt = System.currentTimeMillis();
        repairAllInternal(target);
        updateNotification("Text → " + DisplayUtil.degrees(target) + "°");
    }

    /**
     * [comment]Yes[comment]Yes[comment] —— [comment]「[comment]」。
     * <p>[comment]（[comment] {@link #lastPhonePortraitValid}），
     * [comment] rotation [comment]。
     */
    private boolean phonePortraitNow() {
        // [comment] evaluate() [comment]：[comment]，[comment]
        Display phone = DisplayUtil.activePhoneDisplay(dm);
        if (DisplayUtil.isOn(phone)) {
            lastPhonePortrait = DisplayUtil.isPhonePortrait(phone);
            lastPhonePortraitValid = true;
        }
        return lastPhonePortrait;
    }

    /**
     * [comment]：[comment]「[comment] free [comment] lock」。
     *
     * <p>[comment] free [comment]：[comment] lock [comment]，DisplayManager [comment]，
     * [comment]，[comment]。[comment]。
     */
    private void repairAllInternal(int target) {
        List<Display> ext = DisplayUtil.externalDisplays(dm);
        if (ext.isEmpty()) {
            return;
        }
        final int[] ids = new int[ext.size()];
        for (int i = 0; i < ext.size(); i++) {
            ids[i] = ext.get(i).getDisplayId();
        }

        long now = System.currentTimeMillis();
        lastLockIssuedAt = now;

        exec.execute(() -> {
            for (int id : ids) {
                // [comment]：[comment]，[comment]；[comment]
                ShellRunner.run("wm fixed-to-user-rotation -d " + id + " default");
                ShellRunner.run("wm user-rotation -d " + id + " free");
                try {
                    Thread.sleep(400L);
                } catch (InterruptedException ignored) {
                }
                ShellRunner.run("wm fixed-to-user-rotation -d " + id + " enabled");
                ShellRunner.run("wm user-rotation -d " + id + " lock " + target);
            }
        });

        for (int id : ids) {
            ExtState st = states.get(id);
            if (st == null) {
                st = new ExtState();
                states.put(id, st);
            }
            st.issuedAt = now;
            st.retries = 0;
            st.target = target;
            fixToUserDone.add(id);
        }
    }

    // ---------------------------------------------------------------- [comment] / [comment]

    /**
     * [comment]。[comment]「[comment]」：
     * <ul>
     *   <li>[comment] / [comment] —— [comment] / [comment]</li>
     *   <li>[comment] —— [comment]</li>
     *   <li>[comment] —— [comment] / [comment]</li>
     * </ul>
     * [comment]<b>[comment]</b>[comment]：[comment]Yes[comment]，[comment]。
     */
    private String topologyKey(Display def, List<Display> ext) {
        StringBuilder sb = new StringBuilder();
        if (def == null) {
            sb.append("no-def");
        } else {
            sb.append(def.getState() == Display.STATE_ON ? "on" : "off");
            int[] n = DisplayUtil.nativeSize(def);
            sb.append('/').append(n[0]).append('x').append(n[1]);
        }
        for (Display d : ext) {
            sb.append("|e").append(d.getDisplayId());
            int[] n = DisplayUtil.nativeSize(d);
            sb.append(':').append(n[0]).append('x').append(n[1]);
        }
        return sb.toString();
    }

    /**
     * [comment]YesNo[comment]。
     *
     * <p>[comment]：[comment] (pw, ph)。[comment]Yes 0°/180° [comment]Yes (pw, ph)，90°/270° [comment]Yes
     * (ph, pw)（{@code getRealSize} [comment]）。[comment] DisplayManager [comment]Yes
     * [comment] —— [comment]、[comment] 1/4。
     *
     * <p>[comment]「[comment]」：[comment]（[comment]）。
     */
    private boolean geometryOk(List<Display> ext, int target) {
        boolean rotated = target == Surface.ROTATION_90 || target == Surface.ROTATION_270;
        for (Display d : ext) {
            try {
                int[] n = DisplayUtil.nativeSize(d);
                if (n[0] <= 0 || n[1] <= 0) {
                    continue;
                }
                int ew = rotated ? n[1] : n[0];
                int eh = rotated ? n[0] : n[1];
                Point real = new Point();
                d.getRealSize(real);
                if (real.x != ew || real.y != eh) {
                    Log.d(TAG, "geometry mismatch id=" + d.getDisplayId()
                            + " expected " + ew + "x" + eh
                            + " actual " + real.x + "x" + real.y
                            + " (target " + DisplayUtil.degrees(target) + "°)");
                    return false;
                }
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- [comment]

    private void createChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        NotificationChannel ch = new NotificationChannel(CH_ID, "Text", NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false);
        ch.setSound(null, null);
        nm.createNotificationChannel(ch);

        // [comment]"[comment]"，[comment]
        NotificationChannel pc = new NotificationChannel(CH_PROMPT, "Text", NotificationManager.IMPORTANCE_HIGH);
        pc.setShowBadge(true);
        nm.createNotificationChannel(pc);
    }

    private PendingIntent servicePi(int requestCode, String action, int rotation) {
        Intent i = new Intent(this, RotationService.class).setAction(action);
        if (ACTION_MANUAL.equals(action)) {
            i.putExtra("rotation", rotation);
        }
        return PendingIntent.getService(this, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification buildNotification(String text) {
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentPi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Icon icon = Icon.createWithResource(this, R.drawable.ic_stat_rot);

        Notification.Builder b = new Notification.Builder(this, CH_ID)
                .setSmallIcon(R.drawable.ic_stat_rot)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setOngoing(true)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentPi)
                .addAction(new Notification.Action.Builder(icon, "Text 90°", servicePi(201, ACTION_MANUAL, 1)).build())
                .addAction(new Notification.Action.Builder(icon, "Text 0°", servicePi(202, ACTION_MANUAL, 0)).build())
                .addAction(new Notification.Action.Builder(icon, "Text", servicePi(203, ACTION_REPAIR, 0)).build())
                .addAction(new Notification.Action.Builder(icon, "Text", servicePi(204, ACTION_STOP, 0)).build());

        return b.build();
    }

    private void updateNotification(String text) {
        if (text == null || text.equals(lastNotiText)) {
            return;
        }
        lastNotiText = text;
        LAST_LOG = System.currentTimeMillis() % 100000 + "  " + text;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        try {
            nm.notify(NOTI_ID, buildNotification(text));
        } catch (Throwable ignored) {
        }
    }

    /**
     * [comment]：[comment]（[comment] / [comment]），
     * [comment]，[comment]。
     */
    private void showPromptNotification(String desc, int w, int h) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }

        Intent open = new Intent(this, PromptActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(PromptActivity.EXTRA_DESC, desc)
                .putExtra(PromptActivity.EXTRA_W, w)
                .putExtra(PromptActivity.EXTRA_H, h);
        PendingIntent contentPi = PendingIntent.getActivity(this, 10, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Icon icon = Icon.createWithResource(this, R.drawable.ic_stat_rot);
        String body = "YesNoText " + w + "×" + h + "？\n" + desc;

        Notification n = new Notification.Builder(this, CH_PROMPT)
                .setSmallIcon(R.drawable.ic_stat_rot)
                .setContentTitle("Text")
                .setContentText("YesNoText " + w + "×" + h + "？")
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(contentPi)
                .addAction(new Notification.Action.Builder(icon, "Text", servicePi(211, ACTION_PROMPT_YES, 0)).build())
                .addAction(new Notification.Action.Builder(icon, "Text", servicePi(212, ACTION_PROMPT_NO, 0)).build())
                .build();

        try {
            nm.notify(NOTI_PROMPT, n);
        } catch (Throwable ignored) {
        }
    }
}
