package com.wb.extrotator;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * [comment] Activity [comment]。
 *
 * <p>[comment]，[comment]"[comment]"[comment]：
 *
 * <ol>
 *   <li><b>shell（Shizuku）</b> —— {@code am start --display <id> -n <pkg>/<cls>}。shell [comment]
 *       START_ANY_ACTIVITY，[comment]（[comment]），{@code --display} [comment] Android 14
 *       [comment]，[comment] displayId=1。</li>
 *   <li><b>[comment]</b> —— {@link #coverLaunch}。[comment] Good Lock（MultiStar）[comment]
 *       [comment]，[comment]、<b>[comment]</b>，[comment]。[comment] 1 [comment]
 *       （[comment] Shizuku / [comment]）[comment]Yes[comment]。</li>
 *   <li><b>[comment] startActivity + {@code setLaunchDisplayId}</b> —— [comment]，[comment]
 *       [comment]；[comment]「Permission Denial: ... not allowed on display」[comment]，[comment]。</li>
 * </ol>
 */
public final class SecondaryLauncher {

    private static final String TAG = "SecondaryLauncher";

    private SecondaryLauncher() {
    }

    public static String shellCommand(int displayId, ComponentName cn) {
        return "am start --display " + displayId + " -n " + cn.flattenToShortString();
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment]（MultiStar [comment]） */
    private static final String COVER_LAUNCHER_NAME = "AppsLauncherWidgetProvider";

    /** [comment]：[comment]「[comment]Yes[comment]」 */
    private static final String EXTRA_FROM_COVER_LAUNCHER =
            "com.sec.intent.extra.IS_LAUNCHED_FROM_MULTISTAR_COVER_LAUNCHER";

    /**
     * [comment] Intent [comment] —— <b>[comment] Shizuku，[comment] root</b>。
     *
     * <p>[comment] {@code IActivityTaskManager} [comment]
     * {@code startActivityForCoverLauncherAsUser(Intent, String launcherName, int userId)}，
     * Yes[comment] Good Lock（MultiStar）[comment]。[comment] Binder [comment]，
     * [comment]，[comment] display [comment] —— [comment]「[comment]、[comment] Shizuku」
     * [comment]。「[comment]」[comment]Yes[comment]。
     *
     * <p>⚠ [comment]<b>[comment]</b>[comment]：[comment]Yes[comment]（[comment] {@link #isCover}）。
     *
     * @return true = [comment]；false = [comment]，[comment]
     */
    public static boolean coverLaunch(Context ctx, Intent intent) {
        if (ctx == null || intent == null) {
            return false;
        }
        try {
            Intent i = new Intent(intent);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            i.putExtra(EXTRA_FROM_COVER_LAUNCHER, true);
            // [comment]「Allow[comment]」[comment] —— 「[comment]」[comment]
            // [comment]（SimpleLauncherStarter.launchPackageOnCover [comment]）。
            // [comment]，[comment]「[comment]」。
            String pkg = i.getComponent() != null
                    ? i.getComponent().getPackageName() : i.getPackage();
            if (pkg != null) {
                CoverPolicy.ensure(ctx, pkg);
            }
            Class<?> atm = Class.forName("android.app.ActivityTaskManager");
            Object svc = atm.getDeclaredMethod("getService").invoke(null);
            Method m = svc.getClass().getDeclaredMethod(
                    "startActivityForCoverLauncherAsUser", Intent.class, String.class, int.class);
            // ⚠ userId [comment] {@code ctx.getUserId()} —— [comment] API [comment]，[comment]。
            //   [comment]Yes UserHandle.getUserId(uid)：[comment] 10 [comment]（[comment]）。
            m.invoke(svc, i, COVER_LAUNCHER_NAME, android.os.Process.myUid() / 100000);
            Log.i(TAG, "coverLaunch -> " + intent.getComponent());
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "Text: " + t);
            return false;
        }
    }

    /** [comment]Yes[comment]Yes[comment]（[comment]） */
    public static boolean isCover(Context ctx, int displayId) {
        return displayId > 0 && displayId == CoverDisplay.id(ctx);
    }

    /** [comment] Activity [comment] */
    public static boolean launchOwn(Context ctx, int displayId, Class<?> cls) {
        return launchOwn(ctx, displayId, cls, null);
    }

    /**
     * [comment]，[comment]。
     *
     * <p>[comment]：[comment] shell [comment]Yes[comment]，[comment]
     * {@code am start … --es key value}，{@code Intent.putExtra} [comment]。[comment]
     * [comment] / [comment]，[comment]，[comment]。
     *
     * <p>⚠ [comment] shell [comment] —— [comment]。[comment]Yes
     * [comment]（[comment] {@code open_sort=1}），[comment]。
     */
    public static boolean launchOwn(Context ctx, int displayId, Class<?> cls, Bundle args) {
        ComponentName cn = new ComponentName(ctx, cls);
        if (ShellRunner.isReady() && displayId > 0) {
            StringBuilder sb = new StringBuilder(shellCommand(displayId, cn));
            appendArgs(sb, args);
            ShellRunner.run(sb.toString());
            Log.i(TAG, "launchOwn(shell) display=" + displayId + " -> " + cn.flattenToShortString());
            return true;
        }
        Intent i = new Intent(ctx, cls);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (args != null) {
            i.putExtras(args);
        }
        if (isCover(ctx, displayId) && coverLaunch(ctx, i)) {
            Log.i(TAG, "launchOwn(cover) -> " + cls.getName());
            return true;
        }
        try {
            ActivityOptions opts = ActivityOptions.makeBasic();
            opts.setLaunchDisplayId(Math.max(0, displayId));
            ctx.startActivity(i, opts.toBundle());
            Log.i(TAG, "launchOwn(direct) display=" + displayId + " -> " + cls.getName());
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "launchOwn failed", t);
            return false;
        }
    }

    /** [comment] Bundle [comment] am [comment] */
    private static void appendArgs(StringBuilder sb, Bundle args) {
        if (args == null) {
            return;
        }
        for (String k : args.keySet()) {
            Object v = args.get(k);
            if (v instanceof String) {
                sb.append(" --es ").append(k).append(' ').append(quote((String) v));
            } else if (v instanceof Integer) {
                sb.append(" --ei ").append(k).append(' ').append(v);
            } else if (v instanceof Boolean) {
                sb.append(" --ez ").append(k).append(' ').append(v);
            }
        }
    }

    /** [comment]；[comment] shell [comment] */
    private static String quote(String s) {
        if (s == null) {
            return "''";
        }
        return "'" + s.replace("'", "'\\''") + "'";
    }

    /**
     * [comment]（<b>[comment]</b> —— [comment] Shizuku [comment]）。
     *
     * @param launchIntent {@code PackageManager.getLaunchIntentForPackage(pkg)} [comment]
     * @return false [comment]（[comment]Yes launchIntent [comment]）
     */
    public static boolean launch(Context ctx, int displayId, Intent launchIntent) {
        if (launchIntent == null) {
            return false;
        }
        ComponentName cn = launchIntent.getComponent();
        if (cn == null) {
            return false;
        }
        if (ShellRunner.isReady() && displayId > 0) {
            ShellRunner.run(shellCommand(displayId, cn));
            Log.i(TAG, "launch(shell) display=" + displayId + " -> " + cn.flattenToShortString());
            return true;
        }
        if (isCover(ctx, displayId) && coverLaunch(ctx, launchIntent)) {
            Log.i(TAG, "launch(cover) -> " + cn.flattenToShortString());
            return true;
        }
        try {
            Intent i = new Intent(launchIntent);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ActivityOptions opts = ActivityOptions.makeBasic();
            opts.setLaunchDisplayId(Math.max(0, displayId));
            ctx.startActivity(i, opts.toBundle());
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "launch failed", t);
            return false;
        }
    }

    /** [comment]（[comment]） */
    public static boolean goHome(Context ctx, int displayId) {
        if (!ShellRunner.isReady()) {
            return false;
        }
        ShellRunner.run("am start --display " + displayId
                + " -a android.intent.action.MAIN -c android.intent.category.HOME");
        return true;
    }
}
