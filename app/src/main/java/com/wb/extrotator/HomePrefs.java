package com.wb.extrotator;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 「[comment]」（{@link SecondaryHomeActivity}）[comment]Settings。
 *
 * <p>[comment] {@link LauncherPrefs} [comment]：[comment]，[comment] ——
 * {@code extrot_launcher / launcher_apps} Yes[comment]（[comment] + [comment]）[comment]，
 * {@code extrot_home / home_apps} [comment]Yes「[comment]」。[comment]<b>[comment]</b>，[comment]Yes
 * [comment]「[comment]」—— [comment]<b>[comment]</b>，[comment]。
 *
 * <p>⚠ [comment]Yes {@link #setApps}。[comment] {@link LauncherPrefs#setApps} [comment]
 * [comment]，[comment]，[comment]。
 *
 * <p>{@link LauncherPrefs#SORT_HOME}「[comment]」[comment]，[comment]。
 *
 * <p>[comment]Yes[comment]：{@link #apps} [comment] ⇒ {@link #sort} [comment] ⇒
 * [comment]，[comment]Yes {@link AppRepo#load} [comment]，[comment]「[comment] = [comment]」。
 *
 * <p>⚠ [comment]Yes「[comment]」[comment]Yes「[comment]」—— [comment]，[comment]，
 * [comment]。
 */
public final class HomePrefs {

    private static final String FILE = "extrot_home";
    static final String KEY_APPS = "home_apps";

    /** [comment]，[comment] {@link LauncherPrefs} [comment] */
    private static final String SEP = ";";

    private HomePrefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** [comment]Custom[comment]（[comment]、[comment]）；[comment] = [comment] */
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
    }

    public static boolean isConfigured(Context c) {
        return !apps(c).isEmpty();
    }

    /**
     * [comment]Custom[comment]（[comment]）—— [comment]、[comment]；
     * [comment]，[comment]（[comment]）。
     */
    public static void sort(Context c, List<AppRepo.Item> items) {
        if (items == null || items.size() < 2) {
            return;
        }
        List<String> order = apps(c);
        if (order.isEmpty()) {
            return;
        }
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            idx.put(order.get(i), i);
        }
        try {
            Collections.sort(items, (a, b) -> Integer.compare(at(idx, a.pkg), at(idx, b.pkg)));
        } catch (Throwable ignored) {
        }
    }

    private static int at(Map<String, Integer> m, String pkg) {
        Integer i = m.get(pkg);
        return i == null ? Integer.MAX_VALUE : i;
    }
}
