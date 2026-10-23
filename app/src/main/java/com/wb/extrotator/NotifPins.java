package com.wb.extrotator;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * [comment]<b>[comment]</b>[comment]「[comment]」[comment]（v4.22 [comment]）。
 *
 * <p>[comment]「[comment]」[comment]Yes<b>[comment]</b> —— [comment]
 * [comment]Yes[comment]，[comment]（[comment] launcher [comment]）。[comment]
 * [comment]，[comment]Yes[comment]。
 *
 * <p>[comment]。[comment]，[comment]；
 * [comment]、[comment] —— [comment]Yes[comment]，[comment]。
 */
public final class NotifPins {

    private static final String SP = "cover_notif_panel";
    private static final String KEY_PINS = "notif_pins";

    /** [comment]。[comment]，[comment]，[comment] */
    public static final int MAX = 12;

    private NotifPins() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(SP, Context.MODE_PRIVATE);
    }

    /** [comment]：[comment]（[comment]） */
    public static List<String> list(Context c) {
        List<String> out = new ArrayList<>();
        String s = sp(c).getString(KEY_PINS, "");
        if (TextUtils.isEmpty(s)) {
            return out;
        }
        for (String p : s.split(",")) {
            String q = p.trim();
            if (!q.isEmpty() && !out.contains(q)) {
                out.add(q);
            }
        }
        return out;
    }

    private static void save(Context c, List<String> v) {
        sp(c).edit().putString(KEY_PINS, TextUtils.join(",", v)).apply();
    }

    public static boolean has(Context c, String pkg) {
        return pkg != null && !pkg.isEmpty() && list(c).contains(pkg);
    }

    /**
     * [comment]。[comment]Success（[comment]）；[comment] false，[comment]Notice[comment]。
     */
    public static boolean add(Context c, String pkg) {
        if (pkg == null || pkg.isEmpty()) {
            return false;
        }
        List<String> v = list(c);
        if (v.contains(pkg)) {
            return true;
        }
        if (v.size() >= MAX) {
            return false;
        }
        v.add(pkg);
        save(c, v);
        return true;
    }

    public static void remove(Context c, String pkg) {
        List<String> v = list(c);
        if (v.remove(pkg)) {
            save(c, v);
        }
    }

    /** [comment]：[comment]、[comment] */
    public static void toggle(Context c, String pkg) {
        if (has(c, pkg)) {
            remove(c, pkg);
        } else {
            add(c, pkg);
        }
    }
}
