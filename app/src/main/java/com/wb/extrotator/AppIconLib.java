package com.wb.extrotator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 「[comment]」—— [comment]**[comment]**[comment]，
 * [comment]。
 *
 * <p><b>[comment]，[comment]Yes[comment]</b>：[comment] APK [comment]，
 * [comment]，[comment] 8 [comment]Yes[comment]；[comment]Yes[comment]**[comment]**[comment]，
 * [comment]。[comment] PNG [comment]，[comment]。
 *
 * <p><b>[comment]</b>：
 * <ol>
 *   <li>{@link AppIconReceiver} [comment]「[comment] / [comment] / Update[comment]」[comment] —— [comment]Yes[comment]；</li>
 *   <li>{@link #ensure} [comment]：[comment]Yes[comment]、[comment]、[comment] {@link #RECHECK_MS}
 *       [comment]。[comment]Yes[comment] —— [comment]，[comment]。</li>
 * </ol>
 * [comment]Yes**[comment]**[comment]：[comment]，[comment]。
 *
 * <p>⚠ [comment]：[comment]Yes[comment]，[comment]
 * 「None[comment]」，[comment]"[comment]Yes[comment]"[comment]。
 *
 * <p>⚠ [comment]**[comment]**[comment] PNG。[comment]「[comment]」[comment]，
 * [comment]Delete —— [comment]Yes[comment]，[comment]。
 */
public final class AppIconLib {

    private static final String TAG = "AppIconLib";

    /** [comment]（px）。[comment] —— [comment]Yes[comment]。 */
    static final int PX = 72;

    private static final String DIR = "app_icons";
    private static final String SP = "app_icon_lib";
    private static final String K_INDEX = "index";
    private static final String K_VER = "ver";
    private static final String K_AT = "checked_at";

    /** [comment]。[comment] +1，[comment]（[comment] PNG [comment]）。 */
    private static final int LIB_VER = 1;

    /** [comment]。[comment]，[comment]Yes[comment]"[comment] 500 [comment]"[comment]。 */
    private static final int MAX = 200;

    /** [comment]。[comment]，[comment]。 */
    private static final long RECHECK_MS = 6 * 3600_000L;

    private static final String[] EMPTY = new String[0];

    /** [comment]，[comment] split —— [comment] */
    private static String[] cache;

    private AppIconLib() {
    }

    // ── [comment] ────────────────────────────────────────────────────────────

    /** [comment]。**[comment]，[comment]。** */
    public static void ensure(Context ctx) {
        try {
            SharedPreferences sp = sp(ctx);
            boolean fresh = sp.getInt(K_VER, 0) == LIB_VER
                    && list(ctx).length > 0
                    && System.currentTimeMillis() - sp.getLong(K_AT, 0L) < RECHECK_MS;
            if (!fresh) {
                sync(ctx);
            }
        } catch (Throwable t) {
            Log.w(TAG, "ensure Failed", t);
        }
    }

    /** [comment]"[comment]/[comment]/Update[comment]"[comment] */
    public static void syncAsync(final Context ctx) {
        final Context app = ctx.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                sync(app);
            }
        }, "app-icon-sync").start();
    }

    /**
     * [comment]：[comment]，[comment]。
     * <p>**[comment]**，[comment]（[comment]）。
     */
    public static synchronized void sync(Context ctx) {
        long t0 = System.currentTimeMillis();
        try {
            PackageManager pm = ctx.getPackageManager();
            List<ResolveInfo> ris = pm.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);

            // [comment] + [comment]（[comment] LAUNCHER Activity）
            TreeMap<String, ResolveInfo> want = new TreeMap<>();
            if (ris != null) {
                for (ResolveInfo ri : ris) {
                    if (ri == null || ri.activityInfo == null) {
                        continue;
                    }
                    String p = ri.activityInfo.packageName;
                    if (p == null || p.isEmpty() || p.equals(ctx.getPackageName())) {
                        continue;   // Text —— Text
                    }
                    if (!want.containsKey(p)) {
                        want.put(p, ri);
                    }
                }
            }

            String[] old = list(ctx);
            HashSet<String> oldSet = new HashSet<>(Arrays.asList(old));
            ArrayList<String> kept = new ArrayList<>();
            for (Map.Entry<String, ResolveInfo> e : want.entrySet()) {
                if (kept.size() >= MAX) {
                    break;
                }
                File f = png(ctx, e.getKey());
                if (!oldSet.contains(e.getKey()) || !f.isFile()) {
                    if (!write(f, e.getValue().loadIcon(pm))) {
                        continue;   // Text，Text，Text
                    }
                }
                kept.add(e.getKey());
            }

            // [comment]：[comment]。[comment]"[comment]、[comment]"[comment]，[comment]
            HashSet<String> keepSet = new HashSet<>(kept);
            for (String p : old) {
                if (!keepSet.contains(p)) {
                    png(ctx, p).delete();
                }
            }

            cache = kept.toArray(new String[0]);
            sp(ctx).edit()
                    .putString(K_INDEX, join(kept))
                    .putInt(K_VER, LIB_VER)
                    .putLong(K_AT, System.currentTimeMillis())
                    .apply();
            Log.i(TAG, "Text：" + kept.size() + " Text（Text " + want.size()
                    + " Text），" + (System.currentTimeMillis() - t0) + "ms");
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
    }

    /** [comment]（[comment]）。**[comment]/SharedPreferences，[comment]。** */
    public static String[] list(Context ctx) {
        if (cache != null) {
            return cache;
        }
        try {
            String s = sp(ctx).getString(K_INDEX, "");
            if (s == null || s.isEmpty()) {
                return EMPTY;
            }
            cache = s.split("\n", -1);
            return cache;
        } catch (Throwable t) {
            return EMPTY;
        }
    }

    /**
     * [comment]（[comment]，{@link #PX} [comment]）。
     * <p>[comment] —— [comment]（[comment]）。
     */
    public static Bitmap icon(Context ctx, String pkg) {
        try {
            File f = png(ctx, pkg);
            if (!f.isFile()) {
                if (!write(f, ctx.getPackageManager().getApplicationIcon(pkg))) {
                    return null;
                }
            }
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Throwable t) {
            return null;
        }
    }

    // ── [comment] ────────────────────────────────────────────────────────────

    /** [comment] drawable [comment] {@link #PX} [comment] PNG [comment] */
    private static boolean write(File out, Drawable d) {
        if (d == null) {
            return false;
        }
        Bitmap bm = null;
        try {
            bm = Bitmap.createBitmap(PX, PX, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            d.setBounds(0, 0, PX, PX);
            d.draw(c);
            FileOutputStream fos = new FileOutputStream(out);
            bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            if (bm != null) {
                bm.recycle();
            }
        }
    }

    private static File dir(Context ctx) {
        File d = new File(ctx.getFilesDir(), DIR);
        if (!d.isDirectory()) {
            d.mkdirs();
        }
        return d;
    }

    /** [comment] [a-z0-9._]，[comment]Yes[comment]（[comment]） */
    private static File png(Context ctx, String pkg) {
        return new File(dir(ctx), pkg + ".png");
    }

    private static String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private static SharedPreferences sp(Context ctx) {
        return ctx.getSharedPreferences(SP, Context.MODE_PRIVATE);
    }
}
