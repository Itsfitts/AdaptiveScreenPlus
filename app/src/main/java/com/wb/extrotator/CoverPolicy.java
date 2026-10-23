package com.wb.extrotator;

import android.content.Context;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * [comment]「Allow[comment]」[comment] —— [comment]，<b>[comment]、[comment] Shizuku</b>。
 *
 * <p>[comment]：[comment] {@code com.sec.android.app.launcher}（[comment]
 * [comment]）[comment]「[comment]」—— [comment]Notice[comment]Yes
 * {@code Launching from CoverLauncher is blocked, ask to open phone}，[comment]。
 * [comment]，[comment] API [comment]（[comment]、[comment]YesNone[comment]）[comment]；
 * [comment]Yes「[comment]None[comment]」[comment]。
 *
 * <p>[comment]：[comment] {@code IActivityTaskManager} [comment]
 * {@code setCoverLauncherPackageEnabled(String packageName, int userId)}，
 * [comment] Binder [comment] —— [comment]。「[comment]」[comment]，
 * [comment]（MAIN + LAUNCHER）[comment]，[comment]Yes[comment]；[comment]，
 * [comment]，[comment]。
 *
 * <p>⚠ [comment]Yes[comment] MultiStar [comment] Secure Settings[comment]，[comment]
 * {@code WRITE_SECURE_SETTINGS}（[comment]）。[comment]，Secure [comment]Failed[comment]
 * [comment]、[comment] —— [comment]Yes[comment]。
 */
public final class CoverPolicy {

    private static final String TAG = "CoverPolicy";

    /** [comment]，[comment]Yes[comment] */
    public static final String LAUNCHER_PKG = "com.sec.android.app.launcher";

    /** [comment]「[comment]」[comment]，[comment]（[comment] WRITE_SETTINGS，[comment]） */
    private static final String KEY_SYSTEM_LARGE = "large_cover_screen_apps";

    /** [comment]Success[comment]，[comment] Binder */
    private static final Set<String> DONE = Collections.synchronizedSet(new HashSet<String>());

    private CoverPolicy() {
    }

    /** [comment] {@link #LAUNCHER_PKG} [comment] —— [comment]、[comment] */
    public static void ensureLauncher(Context ctx) {
        ensure(ctx, LAUNCHER_PKG);
    }

    /**
     * [comment]：[comment]。
     *
     * <p>⚠ [comment]<b>Success</b>[comment]。Failed（[comment] / [comment]）[comment]，
     * [comment]，[comment]。
     */
    public static void ensure(Context ctx, String pkg) {
        if (pkg == null || pkg.isEmpty() || DONE.contains(pkg)) {
            return;
        }
        if (enable(ctx, pkg)) {
            DONE.add(pkg);
        }
    }

    /**
     * [comment]。[comment] true = [comment]。
     *
     * <p>[comment]「[comment]」[comment] —— [comment]，[comment]，
     * [comment]Yes[comment]。
     */
    public static boolean enable(Context ctx, String pkg) {
        if (pkg == null || pkg.isEmpty()) {
            return false;
        }
        boolean ok = callFramework(pkg, true);
        if (ctx != null) {
            markLargeCover(ctx);
        }
        return ok;
    }

    /** [comment]（[comment]，[comment]，[comment]「[comment]」[comment]） */
    public static boolean disable(Context ctx, String pkg) {
        if (pkg == null || pkg.isEmpty()) {
            return false;
        }
        return callFramework(pkg, false);
    }

    /** [comment]；[comment] */
    private static boolean callFramework(String pkg, boolean on) {
        String name = on ? "setCoverLauncherPackageEnabled" : "setCoverLauncherPackageDisabled";
        try {
            Class<?> atm = Class.forName("android.app.ActivityTaskManager");
            Object svc = atm.getDeclaredMethod("getService").invoke(null);
            Method m = svc.getClass().getDeclaredMethod(name, String.class, int.class);
            // ⚠ userId [comment] SecondaryLauncher.coverLaunch [comment]：[comment] 10 [comment]，
            //    [comment]。[comment] Context.getUserId()，[comment] API [comment]。
            Object ret = m.invoke(svc, pkg, Process.myUid() / 100000);
            boolean ok = !(ret instanceof Integer) || ((Integer) ret).intValue() == 0;
            Log.i(TAG, name + "(" + pkg + ") => " + ret + (ok ? "  Success" : "  Text"));
            return ok;
        } catch (Throwable t) {
            Log.w(TAG, name + "(" + pkg + ") Text：" + t, t);
            return false;
        }
    }

    /**
     * 「[comment]」[comment]（{@code Settings.System}）。
     *
     * <p>[comment] System [comment] {@code WRITE_SETTINGS}，[comment] —— [comment]、[comment]。
     * [comment]Yes[comment]，[comment]。
     */
    private static void markLargeCover(Context ctx) {
        try {
            Settings.System.putInt(ctx.getContentResolver(), KEY_SYSTEM_LARGE, 1);
            Log.i(TAG, KEY_SYSTEM_LARGE + " = 1");
        } catch (Throwable t) {
            Log.d(TAG, KEY_SYSTEM_LARGE + " Text（Text）：" + t);
        }
    }
}
