package com.wb.extrotator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 「Custom[comment]」[comment]。
 *
 * <p><b>[comment]</b>：[comment] —— [comment]（{@link CustomCoverWidgetProvider}）、
 * [comment]（{@link CustomCfgActivity}）、[comment]。[comment] {@link SlotConfig} Yes[comment]：
 * [comment]，[comment]Default[comment]（[comment]"[comment]"、[comment]"[comment]"）。
 *
 * <p>⚠ [comment]<b>[comment]</b>[comment] provider [comment]：[comment]，
 * [comment]，[comment]"[comment]，[comment]"。
 *
 * <p>⚠ [comment]：[comment]Yes[comment]，[comment]
 * 「None[comment]」，[comment]"[comment]"[comment]。
 *
 * <p>[comment]<b>[comment]</b>（[comment] {@link SlotConfig#choices}），
 * [comment]、[comment]。
 */
public final class CustomWidgetCfg {

    /** [comment]：2×2 [comment] */
    public static final int SLOTS = 4;

    private static final String SP = "custom_widget";

    private CustomWidgetCfg() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(SP, Context.MODE_PRIVATE);
    }

    /** [comment] i [comment] key；[comment]Yes[comment]（[comment]Yes null —— [comment]） */
    public static String slot(Context c, int i) {
        try {
            String s = sp(c).getString("s" + i, "");
            return s == null ? "" : s;
        } catch (Throwable t) {
            return "";
        }
    }

    public static void setSlot(Context c, int i, String key) {
        try {
            sp(c).edit().putString("s" + i, key == null ? "" : key).apply();
        } catch (Throwable ignored) {
        }
    }

    /**
     * [comment]。
     *
     * <p>[comment]：[comment]YesCustom[comment]、[comment]。
     * [comment]"[comment]Settings"、[comment]Settings[comment] —— [comment]None[comment]，[comment]。
     */
    public static boolean slotAlive(Context c, int i) {
        String k = slot(c, i);
        return !k.isEmpty() && QuickActions.isValidKey(c, k);
    }

    /** [comment]：[comment]，[comment]（[comment]）[comment] */
    public static String label(Context c, int i) {
        if (!slotAlive(c, i)) {
            return c.getString(R.string.widget_custom_empty);
        }
        return QuickActions.label(c, slot(c, i));
    }
}
