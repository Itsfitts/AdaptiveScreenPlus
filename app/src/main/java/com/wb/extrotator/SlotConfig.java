package com.wb.extrotator;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * 「[comment]」[comment]：[comment]Yes[comment]Yes[comment]，[comment]。
 *
 * <p><b>[comment]</b>：[comment] —— [comment]（{@link SlotWidgetProvider}）、
 * [comment]（{@link SlotConfigActivity}）、[comment]。[comment]，[comment]
 * Default[comment]（[comment]"[comment]"、[comment]"[comment]"）。
 *
 * <p>⚠ [comment]<b>[comment]</b>[comment] {@code SlotWidgetProvider} [comment]：[comment]
 * [comment]，[comment]，[comment]"[comment]，[comment]"。
 *
 * <p>⚠ [comment]：[comment]Yes[comment]，[comment]
 * 「None[comment]」，[comment]"[comment]"[comment]。
 */
public final class SlotConfig {

    /** [comment]：[comment]Yes[comment]、[comment]Yes「[comment]」 */
    public static final int MODE_SPIN = 0;
    /** [comment]：[comment]、[comment]Yes「Settings[comment]」 */
    public static final int MODE_BTN = 1;

    /** [comment] */
    public static final int SLOTS = 3;

    private static final String SP = "slot_custom";
    private static final String K_MODE = "mode";

    /**
     * [comment]，[comment]。
     *
     * <p>[comment]「[comment]」：[comment]Yes[comment]，[comment]。[comment] / [comment]、
     * [comment] —— [comment]"[comment]"[comment]。
     */
    private static final String[] BASE = {
            QuickActions.KEY_ADB_WIFI,
            QuickActions.KEY_TOGGLE_FOLD,
            QuickActions.KEY_SYNC,
            QuickActions.KEY_DUAL,
            QuickActions.KEY_COVER_DESK,
            QuickActions.KEY_LOCK,
            QuickActions.KEY_CAST,
            QuickActions.KEY_OPEN_APP,
            QuickActions.KEY_COVER_ROTATE,
            QuickActions.KEY_COVER_ROTATE_RESET,
            QuickActions.KEY_COVER_AUTOROT,
            QuickActions.KEY_COVER_HOME,
    };

    private SlotConfig() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(SP, Context.MODE_PRIVATE);
    }

    public static int mode(Context c) {
        try {
            return sp(c).getInt(K_MODE, MODE_SPIN);
        } catch (Throwable t) {
            return MODE_SPIN;
        }
    }

    public static void setMode(Context c, int m) {
        try {
            sp(c).edit().putInt(K_MODE, m).apply();
        } catch (Throwable ignored) {
        }
    }

    public static int toggleMode(Context c) {
        int m = mode(c) == MODE_SPIN ? MODE_BTN : MODE_SPIN;
        setMode(c, m);
        return m;
    }

    /** [comment] i [comment] key；[comment]Yes[comment]（[comment]Yes null —— [comment]） */
    public static String slot(Context c, int i) {
        try {
            String s = sp(c).getString("b" + i, "");
            return s == null ? "" : s;
        } catch (Throwable t) {
            return "";
        }
    }

    public static void setSlot(Context c, int i, String key) {
        try {
            sp(c).edit().putString("b" + i, key == null ? "" : key).apply();
        } catch (Throwable ignored) {
        }
    }

    /** [comment]（[comment]Yes[comment]，[comment]） */
    public static String label(Context c, int i) {
        String k = slot(c, i);
        if (k.isEmpty()) {
            return c.getString(R.string.widget_slot_idle);
        }
        return QuickActions.label(c, k);
    }

    /**
     * [comment]。
     *
     * <p>[comment]：[comment]YesCustom[comment]、[comment]。
     * [comment]"[comment]"，[comment] —— [comment]None[comment]，[comment]。
     */
    public static boolean slotAlive(Context c, int i) {
        String k = slot(c, i);
        return !k.isEmpty() && QuickActions.isValidKey(c, k);
    }

    /** [comment] = [comment] + [comment]Custom[comment] */
    public static String[] choices(Context c) {
        List<String> out = new ArrayList<>();
        for (String k : BASE) {
            out.add(k);
        }
        try {
            for (CustomCmds.Cmd cmd : CustomCmds.list(c)) {
                out.add(CustomCmds.KEY_PREFIX + cmd.id);
            }
        } catch (Throwable ignored) {
        }
        return out.toArray(new String[0]);
    }
}
