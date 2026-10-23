package com.wb.extrotator;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * 「[comment]」[comment]Settings。
 *
 * <p>[comment] SharedPreferences [comment]，[comment] {@link Prefs}（[comment]Settings）[comment] ——
 * [comment]Yes Beta [comment]，[comment]，[comment]Settings[comment] key Yes[comment]。
 */
public final class QuickPrefs {

    private static final String FILE = "extrot_quick";

    private static final String KEY_ASSIST = "assist_action";
    private static final String KEY_VOL_UP_ACTION = "volume_up_action";
    /** [comment] */
    private static final String KEY_VOL_DOWN_ACTION = "volume_down_action";
    private static final String KEY_VOL_ENABLED = "volume_key_enabled";

    /** 「[comment]」[comment] —— [comment] */
    public static final String SIDE_ASSIST = "assist";
    /** 「[comment]」[comment] —— [comment] */
    public static final String SIDE_VOL_UP = "vol_up";
    /** 「[comment]」[comment] —— [comment] */
    public static final String SIDE_VOL_DOWN = "vol_down";

    private QuickPrefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------ [comment]（[comment]）

    /** [comment]，Default「[comment]」 */
    public static String assistAction(Context c) {
        return sp(c).getString(KEY_ASSIST, QuickActions.KEY_COVER_HOME);
    }

    public static void setAssistAction(Context c, String key) {
        sp(c).edit().putString(KEY_ASSIST, key).apply();
    }

    // ------------------------------------------------------------ [comment] / [comment]

    /** [comment]，Default「[comment] 90°」 */
    public static String volumeUpAction(Context c) {
        return sp(c).getString(KEY_VOL_UP_ACTION, QuickActions.KEY_COVER_ROTATE);
    }

    public static void setVolumeUpAction(Context c, String key) {
        sp(c).edit().putString(KEY_VOL_UP_ACTION, key).apply();
    }

    /**
     * [comment]。
     *
     * <p>Default {@link QuickActions#KEY_NONE}（[comment]）—— [comment]"[comment]"[comment]
     * [comment]，[comment]。[comment]。
     */
    public static String volumeDownAction(Context c) {
        return sp(c).getString(KEY_VOL_DOWN_ACTION, QuickActions.KEY_NONE);
    }

    public static void setVolumeDownAction(Context c, String key) {
        sp(c).edit().putString(KEY_VOL_DOWN_ACTION, key).apply();
    }

    /**
     * [comment]。[comment]**[comment]**[comment]。
     *
     * <p>⚠️ [comment]"[comment]"[comment]「None」（{@link QuickActions#KEY_NONE}），
     * [comment] —— [comment]Yes[comment]。
     */
    public static boolean isVolumeKeyEnabled(Context c) {
        return sp(c).getBoolean(KEY_VOL_ENABLED, true);
    }

    public static void setVolumeKeyEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_VOL_ENABLED, v).apply();
    }

    // ------------------------------------------------------------ [comment]：[comment]

    /** v2.19.2 [comment] */
    private static final String KEY_SIDE_TARGET = "side_key_target";
    /** v2.19.2 [comment]（boolean）：true=[comment]，false=[comment]。[comment] */
    private static final String KEY_SIDE_TARGET_LEGACY = "side_key_assist";

    /**
     * 「[comment]」[comment]Yes[comment]（{@link #SIDE_ASSIST} / {@link #SIDE_VOL_UP} /
     * {@link #SIDE_VOL_DOWN}），Default[comment]。
     *
     * <p>⚠ [comment] key [comment]、[comment]：[comment] key [comment] boolean [comment] String [comment] SharedPreferences
     * [comment] ClassCastException。[comment]，[comment]。
     */
    public static String sideKey(Context c) {
        String v = sp(c).getString(KEY_SIDE_TARGET, null);
        if (v != null) {
            return v;
        }
        String migrated = SIDE_ASSIST;
        try {
            // try Yes[comment]：[comment]Yes[comment] boolean，getBoolean [comment]
            boolean wasAssist = sp(c).getBoolean(KEY_SIDE_TARGET_LEGACY, true);
            migrated = wasAssist ? SIDE_ASSIST : SIDE_VOL_UP;
        } catch (Throwable ignored) {
        }
        return migrated;
    }

    public static void setSideKey(Context c, String v) {
        sp(c).edit().putString(KEY_SIDE_TARGET,
                v == null ? SIDE_ASSIST : v).apply();
    }

    // ------------------------------------------------------------ None[comment]

    /**
     * [comment]None[comment]YesNo[comment]Enable。
     *
     * <p>[comment] {@code Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES} [comment]，
     * [comment]Yes[comment]、[comment] {@code pkg/cls} —— [comment] ComponentName [comment]：
     * [comment]Yes[comment]Yes[comment]，[comment]。
     */
    public static boolean isAccessibilityEnabled(Context c) {
        try {
            String flat = Settings.Secure.getString(c.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (TextUtils.isEmpty(flat)) {
                return false;
            }
            ComponentName me = new ComponentName(c, VolumeKeyService.class);
            String full = me.flattenToString();
            String shortName = me.flattenToShortString();
            for (String item : flat.split(":")) {
                if (item.equalsIgnoreCase(full) || item.equalsIgnoreCase(shortName)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
