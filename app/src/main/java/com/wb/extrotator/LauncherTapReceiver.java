package com.wb.extrotator;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * [comment]「[comment]」[comment]。
 *
 * <p>[comment]、[comment] {@code startActivity}，[comment] PendingIntent
 * —— [comment]Yes[comment] PendingIntent [comment]：[comment] → [comment]<b>[comment]</b>[comment]；
 * [comment] 🔍 → [comment]；⇅ → [comment]<b>[comment]</b>；
 * ⚙ → [comment]；★（[comment]）→ [comment]「[comment]」，[comment]。
 * 「[comment]」[comment] {@link LauncherPrefs#SORT_ALL} [comment]。
 *
 * <p>⚠ [comment]：{@link ShellRunner#run} Yes<b>[comment]</b>[comment]（[comment] {@code waitFor}），
 * [comment] {@code onReceive} [comment] 10 [comment] ANR [comment] —— [comment] {@code goAsync()} [comment]，
 * [comment] finish；[comment] {@link SecondaryLauncher} [comment] —— [comment] Shizuku [comment]
 * [comment]，[comment]<b>[comment]</b>[comment]"[comment] Shizuku"[comment]（[comment]）。
 */
public class LauncherTapReceiver extends BroadcastReceiver {

    private static final String TAG = "CoverLauncherTap";

    /** [comment]：[comment]（[comment]） */
    public static final String ACTION_OPEN_SEARCH = "com.wb.extrotator.action.LAUNCHER_SEARCH";
    /** [comment]：[comment]（[comment]） */
    public static final String ACTION_SORT = "com.wb.extrotator.action.LAUNCHER_SORT";
    /** [comment]Settings[comment]：[comment]（[comment]） */
    public static final String ACTION_OPEN_CONFIG = "com.wb.extrotator.action.LAUNCHER_CONFIG";
    /** [comment]：[comment]「[comment]」 */
    public static final String ACTION_FAV_FILTER = "com.wb.extrotator.action.LAUNCHER_FAV";

    public static final String EXTRA_PKG = "pkg";
    public static final String EXTRA_CLS = "cls";
    public static final String EXTRA_LABEL = "label";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        final Context ctx = context.getApplicationContext();
        final String action = intent.getAction();

        if (ACTION_OPEN_SEARCH.equals(action)) {
            runAsync(ctx, "extrot-launcher-search",
                    () -> openOwn(ctx, CoverSearchActivity.class, null));
            return;
        }
        if (ACTION_SORT.equals(action)) {
            Bundle args = new Bundle();
            args.putString(CoverLauncherActivity.EXTRA_OPEN_SORT, "1");
            runAsync(ctx, "extrot-launcher-sort",
                    () -> openOwn(ctx, CoverLauncherActivity.class, args));
            return;
        }
        if (ACTION_OPEN_CONFIG.equals(action)) {
            runAsync(ctx, "extrot-launcher-cfg",
                    () -> openOwn(ctx, CoverLauncherActivity.class, null));
            return;
        }

        /*
         * [comment]：[comment]，[comment] Shizuku，[comment]**[comment]**[comment] ——
         * [comment] goAsync()。[comment]：
         * [comment]「[comment]」，[comment]。
         */
        if (ACTION_FAV_FILTER.equals(action)) {
            boolean on = !LauncherPrefs.favOnly(ctx);
            LauncherPrefs.setFavOnly(ctx, on);
            Log.i(TAG, "Text -> " + (on ? "Text" : "Text"));
            CoverLauncherProvider.refreshAll(ctx);
            return;
        }

        if (!CoverLauncherProvider.ACTION_TAP.equals(action)) {
            return;
        }

        final String pkg = intent.getStringExtra(EXTRA_PKG);
        final String cls = intent.getStringExtra(EXTRA_CLS);
        if (pkg == null || pkg.isEmpty()) {
            Log.w(TAG, "Text，Text");
            return;
        }
        runAsync(ctx, "extrot-launcher-tap", () -> launch(ctx, pkg, cls));
    }

    /**
     * [comment]。
     *
     * <p>[comment] onReceive [comment] 10 [comment] ANR [comment]，[comment] {@link ShellRunner#run} Yes[comment]
     * （[comment] waitFor [comment]），[comment] onReceive [comment]。
     * [comment] {@code goAsync()} [comment]"[comment]"[comment]，[comment] finish。
     */
    private void runAsync(Context ctx, String name, Runnable job) {
        final PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                job.run();
            } catch (Throwable t) {
                Log.w(TAG, name + " failed", t);
            } finally {
                try {
                    pr.finish();
                } catch (Throwable ignored) {
                }
            }
        }, name).start();
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]，[comment] ——
     * [comment]，[comment]。
     * [comment] {@link SecondaryLauncher#launchOwn}（[comment]）；
     * [comment]Default[comment]，[comment]，[comment]Yes[comment]。
     */
    private static void openOwn(Context ctx, Class<?> cls, Bundle args) {
        int display = CoverDisplay.id(ctx);
        if (display > 0 && SecondaryLauncher.launchOwn(ctx, display, cls, args)) {
            Log.i(TAG, cls.getSimpleName() + " Text " + display + " Text");
            return;
        }
        try {
            Intent i = new Intent(ctx, cls).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (args != null) {
                i.putExtras(args);
            }
            ctx.startActivity(i);
            Log.i(TAG, cls.getSimpleName() + " TextDefaultText");
        } catch (Throwable t) {
            Log.w(TAG, "openOwn failed", t);
        }
    }

    // ------------------------------------------------------------------ [comment]

    private static void launch(Context ctx, String pkg, String cls) {
        int display = CoverDisplay.id(ctx);
        Intent li = null;
        try {
            if (cls != null && !cls.isEmpty()) {
                li = new Intent(Intent.ACTION_MAIN);
                li.addCategory(Intent.CATEGORY_LAUNCHER);
                li.setComponent(new ComponentName(pkg, cls));
            }
        } catch (Throwable ignored) {
        }
        if (li == null) {
            try {
                li = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
            } catch (Throwable ignored) {
            }
        }
        if (li == null) {
            Log.w(TAG, "Text: " + pkg);
            return;
        }

        boolean ok = SecondaryLauncher.launch(ctx, display, li);
        Log.i(TAG, "Text " + pkg + " Text " + display + " Text => " + ok);
        if (ok) {
            /*
             * [comment]，[comment]"[comment]"[comment]。
             * [comment]**[comment]**[comment] —— No[comment]"[comment]"[comment]
             * [comment]，[comment]。
             * [comment]（[comment]、[comment]、[comment]）[comment]Update。
             */
            LauncherPrefs.bump(ctx, pkg);
        }
    }
}
