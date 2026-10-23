package com.wb.extrotator;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 「[comment]」[comment]Settings。
 *
 * <p>[comment] SharedPreferences [comment]（[comment] {@link QuickPrefs} [comment]）：[comment]
 * [comment]，[comment]Settings[comment] key Yes[comment]。
 *
 * <p>[comment]：[comment]（[comment]）、[comment]（[comment] / [comment] / [comment] /
 * [comment] / [comment] / [comment]）、[comment]、[comment]（[comment]「[comment]」[comment]，[comment]）。
 * [comment] —— [comment] {@link AppRepo} [comment]，[comment] / [comment] / [comment]。
 *
 * <p><b>[comment]，[comment]</b>：[comment] {@code launcher_apps} Yes<b>[comment]</b>[comment]
 * （[comment]，[comment] {@link #SORT_MANUAL}），{@link HomePrefs#KEY_APPS} Yes<b>[comment]</b>
 * [comment]。[comment]，[comment]「[comment]」[comment]。
 * ⚠ [comment] {@link #setApps} [comment] —— [comment]，[comment]。
 *
 * <p>{@link #SORT_HOME} Yes「[comment]」，[comment]，[comment]。
 *
 * <p>[comment]<b>[comment]</b>：[comment]、[comment]、[comment]Yes[comment]；
 * [comment]，[comment]Yes「[comment]」。
 */
public final class LauncherPrefs {

    private static final String FILE = "extrot_launcher";

    private static final String KEY_APPS = "launcher_apps";
    private static final String KEY_SHOW_LABEL = "launcher_show_label";
    private static final String KEY_SORT = "launcher_sort";
    private static final String KEY_COLUMNS = "launcher_columns";
    private static final String KEY_ICON = "launcher_icon_size";
    private static final String KEY_GAP = "launcher_row_gap";
    private static final String KEY_SHAPE = "launcher_icon_shape";
    private static final String KEY_ALPHA = "launcher_bg_alpha";
    private static final String KEY_USAGE = "launcher_usage";
    private static final String KEY_FAV = "launcher_fav";
    private static final String KEY_FAV_ONLY = "launcher_fav_only";

    /** [comment]，[comment] */
    private static final String SEP = ";";

    // ------------------------------------------------------------ [comment]

    /** [comment]：[comment]（[comment]） */
    public static final String SORT_MANUAL = "manual";
    /** [comment]：[comment] */
    public static final String SORT_NAME = "name";
    /** [comment]：[comment]，[comment] */
    public static final String SORT_USAGE = "usage";
    /** [comment]：[comment] {@link HomePrefs} [comment]（[comment]） */
    public static final String SORT_HOME = "home";

    /**
     * [comment] —— <b>[comment]</b>。
     *
     * <p>[comment]、[comment] ⇅ [comment]、[comment]"[comment]：xxx"[comment]，
     * [comment]"[comment]、[comment]"[comment]。
     */
    public static final String[] SORT_ALL = {SORT_MANUAL, SORT_NAME, SORT_USAGE, SORT_HOME};

    // ------------------------------------------------------------ [comment]

    public static final int SIZE_SMALL = 0;
    public static final int SIZE_MID = 1;
    public static final int SIZE_LARGE = 2;

    public static final int GAP_TIGHT = 0;
    public static final int GAP_MID = 1;
    public static final int GAP_LOOSE = 2;

    /**
     * [comment]。[comment]（[comment] {@link CoverLauncherProvider#layoutFor}）——
     * {@code GridView.setNumColumns} [comment] {@code @RemotableViewMethod} [comment]，[comment]<b>[comment]</b>
     * [comment]，[comment]。
     *
     * <p>[comment]：[comment]，8 [comment] 45dp，[comment]。
     */
    public static final int[] COLUMN_CHOICES = {4, 5, 6, 7};

    public static final int COLUMNS_MIN = 4;
    public static final int COLUMNS_MAX = 7;

    /** [comment]：[comment] */
    public static final int SHAPE_APP = 0;
    /** [comment] */
    public static final int SHAPE_ROUND = 1;
    /** [comment]（Default[comment]，[comment] {@link #iconShape}） */
    public static final int SHAPE_CIRCLE = 2;

    public static final int[] SHAPE_ALL = {SHAPE_APP, SHAPE_ROUND, SHAPE_CIRCLE};

    /**
     * [comment] = [comment] × [comment]。
     *
     * <p>24% Yes[comment]。[comment]：
     * [comment]（{@link CoverLauncherService#toBitmap}）[comment]。
     */
    public static final float SHAPE_RADIUS_RATIO = 0.24f;

    /**
     * [comment]（%），[comment]。
     *
     * <p>[comment]Yes「[comment] / [comment] / [comment]」：[comment]「[comment]」[comment]Yes[comment]，[comment]，
     * [comment]。
     *
     * <p>[comment] shape drawable（[comment] {@link #bgDrawable}）。[comment] drawable
     * [comment]，[comment]Yes[comment]。
     */
    public static final int[] ALPHA_CHOICES = {0, 10, 30, 50};

    /** Default 30% —— [comment]，[comment] */
    public static final int ALPHA_DEFAULT = 30;

    private LauncherPrefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------ [comment]

    /** [comment]，[comment] —— [comment]Yes"[comment]"[comment] */
    public static List<String> apps(Context c) {
        String raw = sp(c).getString(KEY_APPS, "");
        List<String> out = new ArrayList<>();
        if (TextUtils.isEmpty(raw)) {
            return out;
        }
        for (String s : raw.split(SEP)) {
            String t = s == null ? "" : s.trim();
            if (!t.isEmpty() && !out.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    public static void setApps(Context c, List<String> pkgs) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (pkgs != null) {
            for (String p : pkgs) {
                if (p != null && !p.trim().isEmpty()) {
                    set.add(p.trim());
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String p : set) {
            if (sb.length() > 0) {
                sb.append(SEP);
            }
            sb.append(p);
        }
        sp(c).edit().putString(KEY_APPS, sb.toString()).apply();

        /*
         * ⚠ [comment]<b>[comment]</b>[comment] {@link HomePrefs}（v3.2 [comment]）。
         *
         * [comment]Yes[comment]，[comment]
         * 「[comment]」[comment]。[comment]"[comment]，
         * [comment]"，[comment]。
         */
    }

    public static boolean isSelected(Context c, String pkg) {
        return pkg != null && apps(c).contains(pkg);
    }

    /**
     * [comment] / Cancel，[comment]YesNo[comment]。
     *
     * <p>⚠ [comment]Yes <b>[comment]</b>，[comment]Yes[comment] —— [comment]Yes[comment]：
     * [comment]"[comment]"，[comment]，[comment]，
     * [comment]Yes[comment]（[comment]）。
     */
    public static boolean toggle(Context c, String pkg) {
        if (pkg == null || pkg.trim().isEmpty()) {
            return false;
        }
        List<String> list = apps(c);
        boolean now;
        if (list.contains(pkg)) {
            list.remove(pkg);
            now = false;
        } else {
            list.add(0, pkg);
            now = true;
        }
        setApps(c, list);
        return now;
    }

    /** [comment]（[comment] = [comment]，[comment]） */
    public static boolean isConfigured(Context c) {
        return !apps(c).isEmpty();
    }

    // ------------------------------------------------------------ [comment]

    public static String sort(Context c) {
        String v = sp(c).getString(KEY_SORT, SORT_MANUAL);
        for (String m : SORT_ALL) {
            if (m.equals(v)) {
                return m;
            }
        }
        return SORT_MANUAL;
    }

    public static void setSort(Context c, String mode) {
        sp(c).edit().putString(KEY_SORT, mode == null ? SORT_MANUAL : mode).apply();
    }

    /** [comment]。[comment]、[comment]、Toast [comment]。 */
    public static String sortLabel(Context c, String mode) {
        if (SORT_NAME.equals(mode)) {
            return c.getString(R.string.launcher_sort_name);
        }
        if (SORT_USAGE.equals(mode)) {
            return c.getString(R.string.launcher_sort_usage);
        }
        if (SORT_HOME.equals(mode)) {
            return c.getString(R.string.launcher_sort_home);
        }
        return c.getString(R.string.launcher_sort_manual);
    }

    public static String sortLabel(Context c) {
        return sortLabel(c, sort(c));
    }

    /**
     * [comment]（[comment]）。
     *
     * <ul>
     *   <li>{@link #SORT_MANUAL} —— [comment] {@link #apps} [comment]（[comment]）；</li>
     *   <li>{@link #SORT_NAME} —— [comment]（[comment] {@link AppRepo#load}，[comment] Collator）；</li>
     *   <li>{@link #SORT_USAGE} —— [comment]，[comment] 0；[comment] {@code Collections.sort}
     *       [comment]；</li>
     *   <li>{@link #SORT_HOME} —— [comment] {@link HomePrefs#apps} [comment]；[comment]
     *       [comment] ⇒ [comment] ⇒ [comment]（= [comment]）。</li>
     * </ul>
     */
    public static void sortItems(Context c, List<AppRepo.Item> items) {
        if (items == null || items.size() < 2) {
            return;
        }
        final String mode = sort(c);
        try {
            if (SORT_NAME.equals(mode)) {
                final Collator col = Collator.getInstance(Locale.CHINA);
                Collections.sort(items, (a, b) -> col.compare(a.label, b.label));
            } else if (SORT_USAGE.equals(mode)) {
                final Map<String, Integer> use = usage(c);
                Collections.sort(items, (a, b) -> Integer.compare(count(use, b.pkg), count(use, a.pkg)));
            } else if (SORT_HOME.equals(mode)) {
                final Map<String, Integer> idx = index(HomePrefs.apps(c));
                Collections.sort(items, (a, b) -> Integer.compare(at(idx, a.pkg), at(idx, b.pkg)));
            } else {
                final Map<String, Integer> idx = index(apps(c));
                Collections.sort(items, (a, b) -> Integer.compare(at(idx, a.pkg), at(idx, b.pkg)));
            }
        } catch (Throwable ignored) {
            // [comment]Failed[comment]：[comment]
        }
    }

    private static Map<String, Integer> index(List<String> order) {
        Map<String, Integer> m = new HashMap<>();
        if (order != null) {
            for (int i = 0; i < order.size(); i++) {
                m.put(order.get(i), i);
            }
        }
        return m;
    }

    /** [comment] —— [comment]（[comment]Yes[comment]） */
    private static int at(Map<String, Integer> m, String pkg) {
        Integer i = m.get(pkg);
        return i == null ? Integer.MAX_VALUE : i;
    }

    private static int count(Map<String, Integer> m, String pkg) {
        Integer n = m.get(pkg);
        return n == null ? 0 : n;
    }

    // ------------------------------------------------------------ [comment]

    /** [comment]，「[comment]」[comment] */
    public static Map<String, Integer> usage(Context c) {
        Map<String, Integer> m = new HashMap<>();
        String raw = sp(c).getString(KEY_USAGE, "");
        if (TextUtils.isEmpty(raw)) {
            return m;
        }
        for (String part : raw.split(SEP)) {
            int i = part.indexOf('=');
            if (i <= 0) {
                continue;
            }
            try {
                m.put(part.substring(0, i), Integer.parseInt(part.substring(i + 1).trim()));
            } catch (Throwable ignored) {
            }
        }
        return m;
    }

    /**
     * [comment]。[comment] {@link LauncherTapReceiver} [comment]Success[comment]。
     * [comment]（[comment]Yes[comment]），[comment] apply() [comment]。
     */
    public static void bump(Context c, String pkg) {
        if (pkg == null || pkg.trim().isEmpty()) {
            return;
        }
        Map<String, Integer> m = usage(c);
        m.put(pkg, count(m, pkg) + 1);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (sb.length() > 0) {
                sb.append(SEP);
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        sp(c).edit().putString(KEY_USAGE, sb.toString()).apply();
    }

    // ------------------------------------------------------------ [comment]

    /**
     * [comment]，[comment]（[comment]）。
     *
     * <p>[comment] {@link #apps} Yes<b>[comment]</b>：[comment]Yes[comment]，[comment]
     * "[comment]、[comment]"。[comment] {@link #toggleFav} —— [comment]
     * （[comment]，[comment]"[comment]"[comment]）。
     */
    public static List<String> favs(Context c) {
        String raw = sp(c).getString(KEY_FAV, "");
        List<String> out = new ArrayList<>();
        if (TextUtils.isEmpty(raw)) {
            return out;
        }
        for (String s : raw.split(SEP)) {
            String t = s == null ? "" : s.trim();
            if (!t.isEmpty() && !out.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    public static void setFavs(Context c, List<String> pkgs) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (pkgs != null) {
            for (String p : pkgs) {
                if (p != null && !p.trim().isEmpty()) {
                    set.add(p.trim());
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String p : set) {
            if (sb.length() > 0) {
                sb.append(SEP);
            }
            sb.append(p);
        }
        sp(c).edit().putString(KEY_FAV, sb.toString()).apply();
    }

    public static boolean isFav(Context c, String pkg) {
        return pkg != null && favs(c).contains(pkg);
    }

    /**
     * [comment] / Cancel[comment]，[comment]。
     *
     * <p>[comment]<b>[comment]</b>（[comment]，[comment] {@link #toggle} [comment]）；
     * Cancel[comment]<b>[comment]</b>[comment] —— [comment]。
     */
    public static boolean toggleFav(Context c, String pkg) {
        if (pkg == null || pkg.trim().isEmpty()) {
            return false;
        }
        List<String> fav = favs(c);
        boolean now;
        if (fav.contains(pkg)) {
            fav.remove(pkg);
            now = false;
        } else {
            fav.add(pkg);
            now = true;
            if (!isSelected(c, pkg)) {
                List<String> sel = apps(c);
                sel.add(0, pkg);
                setApps(c, sel);
            }
        }
        setFavs(c, fav);
        return now;
    }

    /**
     * 「[comment]」。[comment]<b>[comment]</b> —— [comment]，[comment]Yes[comment]。
     * Default[comment]（= [comment]）。
     */
    public static boolean favOnly(Context c) {
        return sp(c).getBoolean(KEY_FAV_ONLY, false);
    }

    public static void setFavOnly(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_FAV_ONLY, v).apply();
    }

    /** [comment]：[comment] */
    public static void forget(Context c, String pkg) {
        if (pkg == null) {
            return;
        }
        List<String> sel = apps(c);
        if (sel.remove(pkg)) {
            setApps(c, sel);
        }
        List<String> fav = favs(c);
        if (fav.remove(pkg)) {
            setFavs(c, fav);
        }
    }

    // ------------------------------------------------------------ [comment]

    /** [comment]。[comment]、[comment]。 */
    public static boolean showLabel(Context c) {
        return sp(c).getBoolean(KEY_SHOW_LABEL, true);
    }

    public static void setShowLabel(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_SHOW_LABEL, v).apply();
    }

    /** [comment] */
    public static int columns(Context c) {
        int n = sp(c).getInt(KEY_COLUMNS, 4);
        return Math.max(COLUMNS_MIN, Math.min(COLUMNS_MAX, n));
    }

    public static void setColumns(Context c, int n) {
        sp(c).edit().putInt(KEY_COLUMNS, Math.max(COLUMNS_MIN, Math.min(COLUMNS_MAX, n))).apply();
    }

    public static int iconSize(Context c) {
        int v = sp(c).getInt(KEY_ICON, SIZE_MID);
        return (v < 0 || v > 2) ? SIZE_MID : v;
    }

    public static void setIconSize(Context c, int v) {
        sp(c).edit().putInt(KEY_ICON, Math.max(0, Math.min(2, v))).apply();
    }

    /** [comment]（[comment]，[comment] LayoutParams） */
    public static float iconScale(Context c) {
        switch (iconSize(c)) {
            case SIZE_SMALL:
                return 0.78f;
            case SIZE_LARGE:
                return 1.22f;
            default:
                return 1f;
        }
    }

    /**
     * [comment]。Default<b>[comment]</b>。
     *
     * <p>[comment]Default[comment]：[comment]（[comment]，
     * [comment]，[comment]），[comment]。[comment]Yes[comment]"[comment]"[comment] ——
     * [comment]；[comment]，[comment]Yes[comment]。
     * [comment]「[comment]」。
     */
    public static int iconShape(Context c) {
        int v = sp(c).getInt(KEY_SHAPE, SHAPE_CIRCLE);
        return (v < 0 || v > SHAPE_CIRCLE) ? SHAPE_CIRCLE : v;
    }

    public static void setIconShape(Context c, int v) {
        sp(c).edit().putInt(KEY_SHAPE,
                Math.max(SHAPE_APP, Math.min(SHAPE_CIRCLE, v))).apply();
    }

    public static int rowGap(Context c) {
        int v = sp(c).getInt(KEY_GAP, GAP_MID);
        return (v < 0 || v > 2) ? GAP_MID : v;
    }

    public static void setRowGap(Context c, int v) {
        sp(c).edit().putInt(KEY_GAP, Math.max(0, Math.min(2, v))).apply();
    }

    /**
     * [comment] dp。
     *
     * <p>GridView [comment] {@code verticalSpacing} [comment] RemoteViews [comment]（[comment]
     * {@code @RemotableViewMethod}），[comment]Yes[comment]
     * —— [comment] {@code CoverLauncherService.AppFactory.getViewAt}。
     *
     * <p>⚠ [comment]Yes <b>dp</b>，[comment] AppFactory [comment]<b>[comment]</b>[comment] density [comment] px（[comment]Yes[comment]），
     * [comment] ≈ [comment] × 3.0。
     */
    public static int rowPadDp(Context c) {
        switch (rowGap(c)) {
            case GAP_TIGHT:
                return 1;
            case GAP_LOOSE:
                return 9;
            default:
                return 5;
        }
    }

    /** [comment]（%）。[comment]（[comment] pref）[comment]Default[comment]。 */
    public static int bgAlpha(Context c) {
        int v = sp(c).getInt(KEY_ALPHA, ALPHA_DEFAULT);
        for (int a : ALPHA_CHOICES) {
            if (a == v) {
                return a;
            }
        }
        return ALPHA_DEFAULT;
    }

    public static void setBgAlpha(Context c, int v) {
        int pick = ALPHA_DEFAULT;
        for (int a : ALPHA_CHOICES) {
            if (a == v) {
                pick = a;
            }
        }
        sp(c).edit().putInt(KEY_ALPHA, pick).apply();
    }

    /**
     * [comment] drawable。
     *
     * <p>[comment]Yes[comment] alpha [comment]：RemoteViews [comment]，[comment]
     * （setColorFilter）[comment] setter [comment]；[comment] {@code setImageViewResource}
     * Yes[comment]，[comment]。[comment] {@link CoverLauncherProvider#build}。
     */
    public static int bgDrawable(Context c) {
        switch (bgAlpha(c)) {
            case 0:
                return R.drawable.launcher_widget_bg_0;
            case 10:
                return R.drawable.launcher_widget_bg_10;
            case 50:
                return R.drawable.launcher_widget_bg_50;
            default:
                return R.drawable.launcher_widget_bg_30;
        }
    }
}
