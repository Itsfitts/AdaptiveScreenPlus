package com.wb.extrotator;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

/**
 * [comment]。
 *
 * <p>[comment]「[comment] Activity」[comment]Yes「[comment] + [comment]」：[comment] PendingIntent Yes[comment]，
 * [comment]，[comment]「[comment] Activity [comment]」[comment]；[comment] Shizuku [comment] binder，
 * Activity [comment]；[comment] Toast [comment]（[comment]Notice[comment]）。
 *
 * <p>[comment]Yes {@code Theme.Translucent.NoTitleBar}，[comment]，[comment] Toast，
 * [comment] finish。
 */
public class WidgetActionActivity extends Activity {

    public static final String EXTRA_ACTION = "widget_action";
    /**
     * [comment] key（{@link QuickActions} [comment]），[comment]。
     *
     * <p>「Custom[comment]」[comment]Yes[comment]：[comment]，[comment]Yes
     * [comment] key，[comment] {@link QuickActions#run} —— [comment]。
     */
    public static final String EXTRA_KEY = "widget_action_key";
    /** [comment]：[comment] + [comment] */
    public static final String ACT_DUAL = "dual";
    /** [comment] ↔ [comment] */
    public static final String ACT_FOLD = "fold";
    /** [comment]「[comment]」（[comment]） */
    public static final String ACT_SYNC = "sync";
    /** [comment]「[comment]」（[comment]） */
    public static final String ACT_LOCK = "lock";
    /**
     * [comment]（[comment]，[comment]）。
     *
     * <p>⚠ [comment]Yes[comment]**[comment] Shizuku** [comment]：[comment]Yes[comment]，
     * [comment] shell [comment]None[comment]。[comment]「[comment]」[comment] ——
     * No[comment] Shizuku [comment]，[comment] Shizuku。
     */
    public static final String ACT_CAST = "cast";

    /** [comment]（[comment]/[comment]"[comment]"） */
    private static final long DEDUP_MS = 1500L;
    private static String sLastAction = null;
    private static long sLastActionAt = 0L;

    /** @return true = [comment]；false = Yes[comment]，[comment] */
    private static synchronized boolean claim(String action) {
        long now = SystemClock.elapsedRealtime();
        if (action != null && action.equals(sLastAction) && now - sLastActionAt < DEDUP_MS) {
            return false;
        }
        sLastAction = action;
        sLastActionAt = now;
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(true);

        // ⚠ [comment]：[comment]/[comment]「[comment]」[comment]，[comment]
        // [comment] Activity [comment]，[comment]Yes onCreate [comment] Intent [comment]。[comment]Yes
        // 「[comment]、[comment]」—— [comment]→[comment]，[comment]，[comment]。
        // [comment]：① [comment]（savedInstanceState != null）[comment]；② [comment] 1.5 [comment]。
        if (savedInstanceState != null) {
            finish();
            return;
        }

        String action = null;
        try {
            action = getIntent().getStringExtra(EXTRA_ACTION);
        } catch (Throwable ignored) {
        }
        // 「Custom[comment]」[comment]：[comment]Yes[comment] key [comment]（[comment] CustomCoverWidgetProvider）。
        String key = null;
        try {
            key = getIntent().getStringExtra(EXTRA_KEY);
        } catch (Throwable ignored) {
        }
        // [comment]"[comment]"：[comment] key [comment] key，[comment] action
        if (!claim(key != null ? key : action)) {
            finish();
            return;
        }
        if (key != null && !key.isEmpty()) {
            // ⚠ [comment]**[comment]**[comment] Shizuku：{@link QuickActions#run} [comment]
            //   shell（needsShell），[comment]「[comment]」「[comment]」[comment] shell [comment]
            //   [comment]。
            runKey(key);
            return;
        }

        if (!ACT_CAST.equals(action) && !ShellRunner.isReady()) {
            toast("Text「Text」Text Shizuku Text");
            finish();
            return;
        }
        if (ACT_DUAL.equals(action)) {
            runDual();
        } else if (ACT_FOLD.equals(action)) {
            runFold();
        } else if (ACT_SYNC.equals(action)) {
            runToggleSync();
        } else if (ACT_LOCK.equals(action)) {
            runToggleLock();
        } else if (ACT_CAST.equals(action)) {
            runCast();
        } else {
            finish();
        }
    }

    /** [comment]：[comment] + [comment] + [comment]（[comment] 2 [comment]，[comment]） */
    private void runDual() {
        toast("Text…");
        final DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        new Thread(() -> {
            final boolean ok = DualScreen.oneKeyDual(getApplicationContext(), dm);
            runOnUiThread(() -> {
                toast(ok ? "Text，Text"
                        : "Text，Text（TextYesText）");
                finish();
            });
        }, "extrot-widget-dual").start();
    }

    /** [comment] ↔ [comment] */
    private void runFold() {
        new Thread(() -> {
            final int target = DualScreen.toggleFold(this);
            runOnUiThread(() -> {
                // [comment]Yes**[comment]**，[comment]Yes[comment]
                toast(target < 0
                        ? "TextFailed：Text Shizuku Text"
                        : "Text「" + DualScreen.shortName(target) + "」");
                finish();
            });
        }, "extrot-widget-fold").start();
    }

    /**
     * [comment]「[comment]」。
     *
     * <p>[comment]（[comment] MainActivity.setSyncEnabled）：
     * [comment]「[comment]」[comment]，[comment]、[comment]。
     */
    private void runToggleSync() {
        Prefs p = new Prefs(this);
        boolean on = !p.isEnabled();
        p.setEnabled(on);
        if (on) {
            RotationService.start(this);
        } else {
            if (!p.isLockPhoneRotation()) {
                RotationService.restorePhoneRotation(this);
            }
            if (!p.shouldKeepRunning()) {
                RotationService.stop(this);
            }
        }
        PanelWidgetProvider.updateAll(this);
        toast("Text：" + (on ? "Text" : "Text"));
        finish();
    }

    /**
     * [comment]「[comment]」—— [comment]Yes[comment]「[comment]」[comment]，
     * [comment]（v2.13 [comment]）。
     */
    private void runToggleLock() {
        Prefs p = new Prefs(this);
        boolean on = !p.isLockPhoneRotation();
        p.setLockPhoneRotation(on);
        if (on) {
            // [comment]（ACTION_START [comment]），[comment]
            RotationService.start(this);
            RotationService.lockPhoneNow(this);
        } else {
            RotationService.restorePhoneRotation(this);
            if (!p.shouldKeepRunning()) {
                RotationService.stop(this);
            }
        }
        PanelWidgetProvider.updateAll(this);
        toast("Text：" + (on ? "Text（Text）" : "Text"));
        finish();
    }

    /**
     * [comment]。[comment] {@link QuickActions} [comment] —— [comment]「[comment]」、
     * [comment]、MacroDroid [comment]Yes[comment]，[comment]。
     */
    private void runCast() {
        final Context app = getApplicationContext();
        new Thread(() -> {
            final String err = QuickActions.run(app, QuickActions.KEY_CAST);
            runOnUiThread(() -> {
                if (err != null) {
                    toast(err);
                }
                finish();
            });
        }, "extrot-widget-cast").start();
    }

    /**
     * [comment]「Custom[comment]」[comment]。
     *
     * <p>[comment]：[comment]，[comment] {@link QuickActions#run}。
     * [comment]Yes[comment]（[comment]「[comment]」、[comment]、[comment]、[comment]Yes[comment]），
     * [comment]、[comment]，[comment]。
     */
    private void runKey(final String key) {
        final android.content.Context app = getApplicationContext();
        new Thread(() -> {
            final String err = QuickActions.run(app, key);
            runOnUiThread(() -> {
                if (err != null) {
                    toast(err);
                }
                finish();
            });
        }, "extrot-widget-key").start();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
