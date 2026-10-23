package com.wb.extrotator;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/**
 * [comment]Settings。
 *
 * <p>[comment]Yes[comment]（[comment] v4.18 Delete、[comment] v4.18 [comment]），[comment]<b>[comment]</b>[comment]
 * （[comment]Yes[comment]，[comment] {@link ExtScreen}），[comment]Settings[comment]。
 *
 * <p>[comment] {@link LauncherPrefs} / {@link QuickPrefs} [comment]：[comment] SharedPreferences [comment]，
 * [comment]。
 *
 * <p>[comment]：[comment]（[comment]，[comment] {@link #SENS_ALL}）、[comment]（[comment] /
 * [comment]，[comment] {@link #KB_*}）、[comment]（[comment]，[comment]
 * {@link #pseudoOn}）。[comment]「[comment]」「[comment]」—— [comment]
 * [comment]。
 */
public final class ExtPrefs {

    private static final String FILE = "extrot_ext";

    private static final String KEY_SENS = "ext_sens";
    private static final String KEY_KB = "ext_kb_mode";
    /** [comment]，[comment] —— [comment] {@link #pseudoOn} */
    private static final String KEY_PSEUDO = "pseudo_on_";
    /** [comment] */
    private static final String KEY_CAST_RESIZE = "cast_resize";
    private static final String KEY_CAST_ZOOM = "cast_zoom";
    /** [comment] + [comment]（v4.1 [comment]；v4.2 [comment]） */
    private static final String KEY_EDGE_CUTOUT = "edge_cutout_global";
    /** 「[comment]」：[comment]，[comment] policy_control [comment] */
    private static final String KEY_NAV_IMM = "nav_immersive";
    private static final String KEY_NAV_IMM_BACKUP = "nav_immersive_backup";
    /** 「[comment]」：[comment] / [comment] / [comment] */
    private static final String KEY_INNER_GESTURE = "inner_gesture";
    /** [comment]：0 = [comment]，1 = [comment]（[comment] {@link GestureAnim}） */
    private static final String KEY_GESTURE_ANIM_STYLE = "gesture_anim_style";
    /** 「[comment]」：[comment]，[comment]（[comment]、[comment]） */
    private static final String KEY_GAME_APPS = "inner_gesture_game_apps";
    private static final String KEY_COVER_RECENTS = "cover_recents_gesture";
    private static final String KEY_COVER_SIDEBAR = "cover_sidebar";
    /** 「[comment]」[comment]：0=[comment] 11dp / 1=[comment] 16dp / 2=[comment] 24dp */
    private static final String KEY_COVER_RECENTS_BAND = "cover_recents_band";
    /** [comment]：0=[comment] 1/4 / 1=[comment] 1/3 / 2=[comment] 1/2 / 3=[comment] */
    private static final String KEY_COVER_RECENTS_ZONE = "cover_recents_zone";
    /** 「[comment] 0° [comment]」[comment] */
    private static final String KEY_COVER_RECENTS_CORNER = "cover_recents_corner_left";
    /** [comment]：0 = [comment]，1 = [comment] */
    private static final String KEY_SIDEBAR_SIDE = "cover_sidebar_side";
    /** [comment]：0 = [comment]，1 = [comment]，2 = [comment] */
    private static final String KEY_SIDEBAR_ZONE = "cover_sidebar_zone";
    /**
     * 「[comment]」[comment]<b>[comment]</b>：[comment] "1110"，[comment] {@link #SIDEBAR_ITEM_IDS}
     * （[comment]Yes [comment] / [comment] / [comment] / [comment]）。
     *
     * <p>[comment]"[comment]"，[comment]"[comment]" —— [comment] {@link #KEY_SIDEBAR_ORDER} [comment]。
     */
    private static final String KEY_SIDEBAR_ITEMS = "cover_sidebar_items";
    /**
     * 「[comment]」[comment]<b>[comment]</b>：[comment] "3,2,1,4"。
     *
     * <p>[comment] {@link #KEY_SIDEBAR_ITEMS} <b>[comment]</b>[comment]Yes[comment]：[comment]，
     * [comment]。[comment]Yes[comment]"Cancel[comment]"[comment] ——
     * Cancel[comment]，[comment]。
     *
     * <p>[comment]（[comment]、[comment]、[comment]）[comment] {@link #SIDEBAR_ITEM_IDS}
     * [comment]。
     */
    private static final String KEY_SIDEBAR_ORDER = "cover_sidebar_order";
    /**
     * [comment]（「[comment]」[comment]）：0 = [comment]（[comment]），1 = [comment]，
     * 2 = [comment]（[comment]，[comment]），3 = [comment]。
     *
     * <p>⚠ [comment]Yes<b>[comment]</b>[comment]：2 [comment]Yes[comment]，[comment]，[comment]。
     */
    private static final String KEY_SIDEBAR_STYLE = "cover_sidebar_style";


    /**
     * [comment]。
     *
     * <p>[comment]：[comment] × [comment] = [comment]。
     * 1.0 Yes"[comment] = [comment]"，[comment]Yes[comment] 1:1；
     * [comment]，[comment]。
     */
    public static final float[] SENS_ALL = {0.6f, 1.0f, 1.6f};
    private static final int SENS_DEFAULT = 1;

    /** [comment]：[comment]（[comment] ASCII [comment]，[comment]） */
    public static final int KB_AUTO = 0;
    /** [comment]：[comment] `input text` [comment] */
    public static final int KB_TEXT = 1;
    /** [comment]：[comment]"[comment] + [comment]" */
    public static final int KB_CLIP = 2;

    private ExtPrefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** [comment]（0..2） */
    public static int sensIndex(Context c) {
        int v = sp(c).getInt(KEY_SENS, SENS_DEFAULT);
        return (v < 0 || v >= SENS_ALL.length) ? SENS_DEFAULT : v;
    }

    public static void setSensIndex(Context c, int i) {
        sp(c).edit().putInt(KEY_SENS, Math.max(0, Math.min(SENS_ALL.length - 1, i))).apply();
    }

    /** [comment] */
    public static float sens(Context c) {
        return SENS_ALL[sensIndex(c)];
    }

    /** [comment]，[comment] {@link #KB_AUTO} [comment] */
    public static int kbMode(Context c) {
        int v = sp(c).getInt(KEY_KB, KB_AUTO);
        return (v < KB_AUTO || v > KB_CLIP) ? KB_AUTO : v;
    }

    public static void setKbMode(Context c, int v) {
        sp(c).edit().putInt(KEY_KB, Math.max(KB_AUTO, Math.min(KB_CLIP, v))).apply();
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * 「[comment]」[comment]。
     *
     * <p>[comment]<b>[comment]</b>：[comment] {@code wm size}，[comment]"[comment]Settings"[comment]，
     * [comment]，[comment]。
     */
    public static boolean castResize(Context c) {
        return sp(c).getBoolean(KEY_CAST_RESIZE, false);
    }

    public static void setCastResize(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_CAST_RESIZE, on).apply();
    }

    /** 「Allow[comment]」[comment]（[comment] —— [comment]Yes[comment]，[comment]） */
    public static boolean castZoom(Context c) {
        return sp(c).getBoolean(KEY_CAST_ZOOM, false);
    }

    public static void setCastZoom(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_CAST_ZOOM, on).apply();
    }

    // ------------------------------------------------------------------ [comment] / [comment]（v4.2 [comment]）

    /**
     * 「[comment]」[comment]（[comment] —— [comment]）。
     *
     * <p>[comment]Yes"[comment]"，[comment]Yes"[comment]Yes[comment]Yes[comment]"：[comment]
     * （[comment] {@link CutoutPolicy}），[comment]Yes[comment]。
     */
    public static boolean cutoutGlobal(Context c) {
        return sp(c).getBoolean(KEY_EDGE_CUTOUT, false);
    }

    public static void setCutoutGlobal(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_EDGE_CUTOUT, on).apply();
    }

    /** 「[comment]」[comment]（[comment] —— [comment]） */
    public static boolean coverRecentsGesture(Context c) {
        return sp(c).getBoolean(KEY_COVER_RECENTS, false);
    }

    public static void setCoverRecentsGesture(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_COVER_RECENTS, on).apply();
    }

    /**
     * [comment]「[comment]」[comment]（0=[comment] 11dp / 1=[comment] 16dp / 2=[comment] 24dp，[comment] 1）。
     *
     * <p>⚠ [comment]Yes[comment]：[comment]，Android [comment]。
     * [comment] —— <b>[comment]，[comment]</b>。
     * [comment]，[comment]（[comment] {@link #setCoverRecentsBand} [comment]）。
     */
    public static int coverRecentsBand(Context c) {
        return sp(c).getInt(KEY_COVER_RECENTS_BAND, 1);
    }

    public static void setCoverRecentsBand(Context c, int idx) {
        sp(c).edit().putInt(KEY_COVER_RECENTS_BAND, idx).apply();
    }

    /**
     * [comment]（0=[comment] 1/4 / 1=[comment] 1/3 / 2=[comment] 1/2 / 3=[comment]，[comment] 1）。
     *
     * <p>[comment]Yes[comment]。[comment]Yes[comment]：
     * [comment]、[comment]（[comment]）。
     */
    public static int coverRecentsZone(Context c) {
        return sp(c).getInt(KEY_COVER_RECENTS_ZONE, 1);
    }

    public static void setCoverRecentsZone(Context c, int idx) {
        sp(c).edit().putInt(KEY_COVER_RECENTS_ZONE, idx).apply();
    }

    /**
     * 「[comment] 0° [comment]」[comment]（[comment]）。
     *
     * <p>[comment]：[comment] 0° [comment]，[comment]，[comment]
     * [comment]；[comment]，[comment] —— [comment]，[comment]
     * [comment]。
     *
     * <p>[comment]<b>[comment]Yes 0°</b> [comment]（[comment] {@link CoverRecentsGesture} [comment]
     * {@code wantCornerLeft}）；[comment]。
     */
    public static boolean coverRecentsCornerLeft(Context c) {
        return sp(c).getBoolean(KEY_COVER_RECENTS_CORNER, false);
    }

    public static void setCoverRecentsCornerLeft(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_COVER_RECENTS_CORNER, on).apply();
    }

    /**
     * 「[comment]」[comment]（[comment]）。
     *
     * <p>⚠ [comment]Yes<b>[comment]</b>，[comment]Yes[comment] —— [comment]，
     * [comment]（[comment] {@link NavBarImmersive#read()}）。
     */
    public static boolean navImmOn(Context c) {
        return sp(c).getBoolean(KEY_NAV_IMM, false);
    }

    public static void setNavImmOn(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_NAV_IMM, on).apply();
    }

    /**
     * [comment] {@code policy_control} [comment]Yes[comment]，<b>null = [comment]</b>。
     *
     * <p>[comment]Yes[comment]：[comment]Yes[comment]，NavStar [comment]，
     * [comment]。
     */
    public static String navImmBackup(Context c) {
        return sp(c).getString(KEY_NAV_IMM_BACKUP, null);
    }

    public static void setNavImmBackup(Context c, String v) {
        sp(c).edit().putString(KEY_NAV_IMM_BACKUP, v).apply();
    }

    /**
     * 「[comment]」[comment]。
     *
     * <p>[comment]：[comment]YesNone[comment]
     * （[comment] {@link InnerGesture#sync}），[comment]。
     */
    public static boolean innerGesture(Context c) {
        return sp(c).getBoolean(KEY_INNER_GESTURE, false);
    }

    public static void setInnerGesture(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_INNER_GESTURE, on).apply();
    }

    /**
     * [comment]：{@link GestureAnim#STYLE_NATIVE}（0）[comment]
     * {@link GestureAnim#STYLE_GLOW}（1）。
     *
     * <p>[comment]"[comment]"，[comment]None[comment] —— [comment]：
     * {@link GestureAnim} [comment]，[comment]。
     */
    public static int gestureAnimStyle(Context c) {
        return sp(c).getInt(KEY_GESTURE_ANIM_STYLE, GestureAnim.STYLE_NATIVE);
    }

    public static void setGestureAnimStyle(Context c, int v) {
        sp(c).edit().putInt(KEY_GESTURE_ANIM_STYLE, v).apply();
    }

    // ------------------------------------------------------------------ [comment]（[comment]）

    /**
     * 「[comment]」[comment]（[comment] = [comment]）。
     *
     * <p>[comment]，[comment]：[comment]，
     * 3 [comment] / [comment]（[comment] {@link InnerGesture}）。[comment]。
     *
     * <p>[comment]Yes[comment] —— [comment]，[comment] SharedPreferences [comment]。
     */
    public static Set<String> gameGestureApps(Context c) {
        Set<String> s = sp(c).getStringSet(KEY_GAME_APPS, new HashSet<String>());
        return s == null ? new HashSet<String>() : new HashSet<String>(s);
    }

    /** [comment]「[comment]」[comment] */
    public static boolean gameGestureOn(Context c, String pkg) {
        return pkg != null && gameGestureApps(c).contains(pkg);
    }

    /** [comment] / Cancel[comment]（[comment]，[comment]） */
    public static void setGameGestureApp(Context c, String pkg, boolean on) {
        if (pkg == null) {
            return;
        }
        Set<String> s = gameGestureApps(c);
        if (on) {
            s.add(pkg);
        } else {
            s.remove(pkg);
        }
        sp(c).edit().putStringSet(KEY_GAME_APPS, s).apply();
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment]：0 = [comment]，1 = [comment]（[comment] —— [comment]） */
    public static int sidebarSide(Context c) {
        return sp(c).getInt(KEY_SIDEBAR_SIDE, 1);
    }

    public static void setSidebarSide(Context c, int v) {
        sp(c).edit().putInt(KEY_SIDEBAR_SIDE, v).apply();
    }

    /**
     * [comment]：0 = [comment]，1 = [comment]，2 = [comment]（[comment]）。
     *
     * <p>[comment]。[comment]Yes"[comment]" ——
     * [comment]Yes side=1 + zone=0，[comment]（Z Flip [comment]）[comment]。
     */
    public static int sidebarZone(Context c) {
        return sp(c).getInt(KEY_SIDEBAR_ZONE, 0);
    }

    public static void setSidebarZone(Context c, int v) {
        sp(c).edit().putInt(KEY_SIDEBAR_ZONE, v).apply();
    }

    /** [comment]：[comment]（[comment] / [comment] / [comment]） */
    private static final boolean[] SIDEBAR_ITEMS_ON_DEF = {true, true, true, false};

    /**
     * [comment]<b>[comment]</b>（[comment] {@link #SIDEBAR_ITEM_IDS}）。
     *
     * <p>[comment]"[comment]"，[comment]"[comment]" —— [comment] {@link #sidebarOrder(Context)} [comment]。
     * [comment]：[comment]Yes[comment] "1110"，[comment]。
     */
    public static boolean[] sidebarItemsOn(Context c) {
        boolean[] def = SIDEBAR_ITEMS_ON_DEF.clone();
        String s = sp(c).getString(KEY_SIDEBAR_ITEMS, "");
        if (s == null || s.length() != def.length) {
            return def;
        }
        boolean[] out = new boolean[def.length];
        for (int i = 0; i < def.length; i++) {
            char ch = s.charAt(i);
            if (ch != '0' && ch != '1') {
                return def;
            }
            out[i] = ch == '1';
        }
        return out;
    }

    public static void setSidebarItemsOn(Context c, boolean[] on) {
        if (on == null || on.length != SIDEBAR_ITEM_IDS.length) {
            return;
        }
        StringBuilder b = new StringBuilder();
        for (boolean x : on) {
            b.append(x ? '1' : '0');
        }
        sp(c).edit().putString(KEY_SIDEBAR_ITEMS, b.toString()).apply();
    }

    /**
     * 「[comment]」[comment]<b>[comment]</b>：[comment]。
     *
     * <p>[comment]Yes {@link #SIDEBAR_ITEM_IDS} [comment]；[comment]Yes[comment]。
     *
     * <p>⚠ [comment]Yes[comment]<b>[comment]</b> —— [comment]、[comment]、[comment]、[comment]，
     * [comment]（[comment]，[comment]）。
     */
    public static int[] sidebarOrder(Context c) {
        int[] def = SIDEBAR_ITEM_IDS.clone();
        String s = sp(c).getString(KEY_SIDEBAR_ORDER, "");
        if (s == null || s.isEmpty()) {
            return def;
        }
        String[] parts = s.split(",");
        if (parts.length != def.length) {
            return def;
        }
        int[] out = new int[def.length];
        boolean[] seen = new boolean[RING_HOME + 1];
        for (int i = 0; i < parts.length; i++) {
            int v;
            try {
                v = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                return def;
            }
            if (v <= RING_NONE || v > RING_HOME || seen[v]) {
                return def;
            }
            seen[v] = true;
            out[i] = v;
        }
        for (int v : SIDEBAR_ITEM_IDS) {
            if (!seen[v]) {
                return def;
            }
        }
        return out;
    }

    public static void setSidebarOrder(Context c, int[] order) {
        if (order == null || order.length != SIDEBAR_ITEM_IDS.length) {
            return;
        }
        StringBuilder b = new StringBuilder();
        for (int v : order) {
            if (b.length() > 0) {
                b.append(',');
            }
            b.append(v);
        }
        sp(c).edit().putString(KEY_SIDEBAR_ORDER, b.toString()).apply();
    }

    /** [comment] {@link #SIDEBAR_ITEM_IDS} [comment]（[comment]Yes[comment]） */
    public static int itemIndex(int id) {
        for (int i = 0; i < SIDEBAR_ITEM_IDS.length; i++) {
            if (SIDEBAR_ITEM_IDS[i] == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 「[comment]」[comment]（<b>[comment]，[comment]</b>）。
     *
     * <p>[comment]「Z Flip [comment]」[comment]Yes[comment]，
     * [comment]，[comment]/[comment]。
     */
    public static boolean coverSidebar(Context c) {
        return sp(c).getBoolean(KEY_COVER_SIDEBAR, false);
    }

    public static void setCoverSidebar(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_COVER_SIDEBAR, on).apply();
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]（「[comment]」[comment]）：0 = [comment]（[comment]），1 = [comment]，
     * 2 = [comment]（[comment]），3 = [comment]。
     *
     * <p>[comment] {@link CoverSidebar#animKind()} [comment] 0。
     */
    public static int sidebarStyle(Context c) {
        return sp(c).getInt(KEY_SIDEBAR_STYLE, 0);
    }

    public static void setSidebarStyle(Context c, int v) {
        sp(c).edit().putInt(KEY_SIDEBAR_STYLE, v).apply();
    }

    /** [comment]。[comment] prefs [comment]Yes[comment] */
    public static final int RING_NONE = 0;
    public static final int RING_BACK = 1;
    public static final int RING_RECENTS = 2;
    public static final int RING_NOTIFY = 3;
    public static final int RING_HOME = 4;

    /**
     * 「[comment]」[comment]<b>[comment]</b>[comment]（[comment] / [comment] / [comment] / [comment]），
     * [comment]Yes[comment] "1110" [comment]。
     *
     * <p>⚠ [comment]，[comment]<b>[comment] {@link #sidebarOrder(Context)}</b>，
     * [comment]Yes[comment]。[comment]Yes：[comment]、[comment]、[comment]，[comment]。
     *
     * <p>⚠ [comment] RING_* [comment]<b>[comment]</b> —— [comment]，
     * [comment] javac [comment]「[comment]」。
     */
    public static final int[] SIDEBAR_ITEM_IDS = {
            RING_NOTIFY, RING_RECENTS, RING_BACK, RING_HOME};

    /**
     * [comment]（[comment]Yes[comment] → [comment]）[comment]。
     *
     * <p>[comment]Yes「[comment]」[comment]（0 ~ 4）：[comment]；<b>[comment]
     * {@link #sidebarOrder(Context)} [comment]</b>（[comment]Yes[comment]，[comment]）。
     * [comment] —— [comment]。
     *
     * <p>[comment]：[comment]「[comment] / [comment] / [comment]」，[comment]「[comment] / [comment] / [comment]」，
     * [comment]。
     */
    public static int[] sidebarSlots(Context c) {
        boolean[] on = sidebarItemsOn(c);
        int[] order = sidebarOrder(c);
        int n = 0;
        for (int id : order) {
            int at = itemIndex(id);
            if (at >= 0 && on[at]) {
                n++;
            }
        }
        int[] out = new int[n];
        int k = 0;
        for (int id : order) {
            int at = itemIndex(id);
            if (at >= 0 && on[at]) {
                out[k++] = id;
            }
        }
        return out;
    }

    /** 「[comment]」[comment]（[comment]） */
    public static int sidebarSlotCount(Context c) {
        return sidebarSlots(c).length;
    }

    /** [comment]（Settings[comment]） */
    public static String ringActionName(Context c, int id) {
        switch (id) {
            case RING_BACK:
                return "Text";
            case RING_RECENTS:
                return "Text";
            case RING_NOTIFY:
                return "Text";
            case RING_HOME:
                return "Text";
            default:
                return "Text";
        }
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]。
     *
     * <p>[comment]：{@code pseudo_on_com.wb.extrotator.pseudo.remote} —— [comment]，
     * [comment]（{@link AppRepo#pseudoItems} [comment]），[comment]。
     *
     * <p>[comment]（{@link AppRepo.Item#pseudoDefaultOn}）：<b>[comment]Default[comment]</b>
     * （[comment]「[comment]」），[comment]Default[comment]（[comment]Yes[comment]）。
     * [comment] Beta [comment] →「[comment]」。
     *
     * @param defaultOn [comment]Settings[comment]Yes[comment]
     */
    public static boolean pseudoOn(Context c, String pkg, boolean defaultOn) {
        return sp(c).getBoolean(KEY_PSEUDO + pkg, defaultOn);
    }

    public static void setPseudoOn(Context c, String pkg, boolean on) {
        sp(c).edit().putBoolean(KEY_PSEUDO + pkg, on).apply();
    }

}
