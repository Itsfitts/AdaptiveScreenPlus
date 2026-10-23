package com.wb.extrotator;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Display;

import java.lang.reflect.Method;

/**
 * [comment] —— [comment]，[comment]。
 *
 * <p><b>[comment] displayId [comment] key</b>：displayId Yes[comment]，[comment]
 * [comment]（[comment]、[comment]），[comment] key [comment]
 * 「[comment]，[comment]」。[comment]Yes「[comment]」：[comment]
 * {@link Display#getUniqueId()}（Android 13+）—— [comment] EDID [comment]，[comment]
 * [comment]；[comment]「[comment] + [comment]」。displayId [comment]，
 * [comment]，[comment]。
 *
 * <p><b>[comment]Yes「[comment]Settings」</b>，[comment]Yes[comment] —— [comment]：
 * [comment]（[comment] / [comment]），[comment]、[comment]、[comment]
 * [comment] DPI，[comment]，[comment]。[comment] · [comment] · [comment]
 * [comment]；[comment] · [comment] · [comment] · DPI。
 *
 * <p>「[comment]」[comment]「[comment]」[comment]<b>[comment]</b>（[comment]），[comment] {@link Item#applyTo}：
 * [comment]，[comment]，[comment]Yes「[comment]
 * [comment]」「[comment]」。[comment]Yes「[comment]Save[comment]Yes[comment]」，
 * [comment]Yes「[comment]」。
 *
 * <p>[comment]<b>[comment]</b>「[comment]」[comment]「[comment]」—— [comment]Yes[comment]
 * （[comment]），[comment]「[comment]」[comment]，[comment]。
 */
public final class DisplayProfile {

    /** [comment] prefs [comment]，[comment]Settings[comment]，[comment] */
    private static final String FILE = "extrot_profiles";

    private DisplayProfile() {
    }

    // [comment]Default[comment]，[comment] {@link Prefs} [comment]Default[comment] ——
    // No[comment]（[comment] 5 [comment]）[comment]。
    private static final boolean DEF_SYNC = false;
    private static final int DEF_PORTRAIT = 1;
    private static final int DEF_LANDSCAPE = 0;
    private static final boolean DEF_FIX_TO_USER = true;
    private static final boolean DEF_LOCK_PHONE = true;
    private static final int DEF_PHONE_ROT = 1;
    private static final boolean DEF_RES_AUTO = true;
    private static final boolean DEF_RES_ENABLED = false;
    private static final int DEF_RES_W = 1080;
    private static final int DEF_RES_H = 1920;
    private static final int DEF_RES_DPI = 0;

    /** [comment]Settings */
    public static final class Item {
        /** [comment]（[comment]，[comment] {@link #applyTo}） */
        public final boolean sync;
        public final int portrait;
        public final int landscape;
        /** [comment]（wm fixed-to-user-rotation） */
        public final boolean fixToUser;
        /** [comment]、[comment]（[comment]，[comment] {@link #applyTo}） */
        public final boolean lockPhone;
        /** [comment]（Surface.ROTATION_*，[comment]） */
        public final int phoneRot;
        /** [comment] */
        public final boolean resAuto;
        /** [comment] */
        public final boolean resEnabled;
        public final int resW;
        public final int resH;
        /** 0 = [comment] DPI */
        public final int resDensity;
        /** [comment] displayId / [comment]，[comment] */
        public final int displayId;
        public final String label;

        Item(boolean sync, int portrait, int landscape,
             boolean fixToUser, boolean lockPhone, int phoneRot,
             boolean resAuto, boolean resEnabled, int resW, int resH, int resDensity,
             int displayId, String label) {
            this.sync = sync;
            this.portrait = portrait;
            this.landscape = landscape;
            this.fixToUser = fixToUser;
            this.lockPhone = lockPhone;
            this.phoneRot = phoneRot;
            this.resAuto = resAuto;
            this.resEnabled = resEnabled;
            this.resW = resW;
            this.resH = resH;
            this.resDensity = resDensity;
            this.displayId = displayId;
            this.label = label == null ? "" : label;
        }

        /**
         * [comment]**[comment]**[comment]Settings[comment] Item（[comment]）。
         *
         * <p>[comment]Yes[comment]「[comment]」[comment]「[comment]」[comment] ——
         * [comment]，[comment]、[comment]Save。
         */
        public static Item from(Prefs p) {
            return new Item(p.isEnabled(),
                    p.getPortraitAngle(), p.getLandscapeAngle(),
                    p.isFixToUserRotation(), p.isLockPhoneRotation(), p.getPhoneRotation(),
                    p.isAutoMatchExt(), p.isResolutionEnabled(),
                    p.getResWidth(), p.getResHeight(), p.getResDensity(),
                    -1, "");
        }

        /**
         * [comment]Settings。
         *
         * @return YesNo[comment] —— [comment]Notice[comment]"[comment]"
         */
        public boolean applyTo(Prefs p) {
            boolean changed = false;
            // ⚠ v2.14 [comment]「[comment]」[comment]「[comment]」，v2.17 [comment]
            // [comment]。[comment]Yes[comment]，[comment]Yes"[comment]"；[comment]，
            // [comment]（v2.9–v2.15 [comment] bug）。
            // [comment]，[comment]Yes[comment]「[comment]Save」[comment]。
            if (p.getPortraitAngle() != portrait) {
                p.setPortraitAngle(portrait);
                changed = true;
            }
            if (p.getLandscapeAngle() != landscape) {
                p.setLandscapeAngle(landscape);
                changed = true;
            }
            if (p.isFixToUserRotation() != fixToUser) {
                p.setFixToUserRotation(fixToUser);
                changed = true;
            }
            if (p.isAutoMatchExt() != resAuto) {
                p.setAutoMatchExt(resAuto);
                changed = true;
            }
            if (p.isResolutionEnabled() != resEnabled) {
                p.setResolutionEnabled(resEnabled);
                changed = true;
            }
            if (p.getResWidth() != resW || p.getResHeight() != resH) {
                p.setResSize(resW, resH);
                changed = true;
            }
            if (p.getResDensity() != resDensity) {
                p.setResDensity(resDensity);
                changed = true;
            }
            return changed;
        }
    }

    /**
     * [comment]。
     *
     * <p>[comment] {@code Display.getUniqueId()} —— [comment] EDID [comment]，[comment]
     * [comment]。⚠ [comment]<b>[comment] SDK [comment]</b>（android.jar [comment]，[comment]
     * 「[comment]」），[comment]；[comment]，[comment]（[comment]、[comment]）[comment]。
     *
     * <p>[comment]Yes「[comment] + [comment]」：[comment]，[comment]Yes[comment]<b>[comment]</b>[comment]
     * [comment] —— [comment]Settings，[comment]。
     *
     * <p>⚠ [comment] —— [comment]「None[comment]」[comment]，[comment]
     * 「[comment] key [comment]」，[comment]Settings。
     */
    public static String signature(Display d) {
        if (d == null) {
            return "";
        }
        try {
            Method m = Display.class.getMethod("getUniqueId");
            Object uid = m.invoke(d);
            if (uid instanceof String && !((String) uid).isEmpty()) {
                return "u:" + uid;
            }
        } catch (Throwable ignored) {
        }
        int[] n = DisplayUtil.nativeSize(d);
        if (n[0] <= 0 || n[1] <= 0) {
            return "";
        }
        return "n:" + d.getName() + "#" + n[0] + "x" + n[1];
    }

    /** [comment]，[comment] "display 5 · [comment] 1920×1080" */
    public static String label(Display d) {
        int[] n = DisplayUtil.nativeSize(d);
        return "display " + d.getDisplayId() + " · Text " + n[0] + "×" + n[1];
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static String prefix(String sig) {
        return "p:" + sig + ":";
    }

    public static boolean exists(Context c, String sig) {
        return sig != null && !sig.isEmpty()
                && sp(c).contains(prefix(sig) + "land");
    }

    /** [comment]；[comment] null */
    public static Item load(Context c, String sig) {
        if (sig == null || sig.isEmpty()) {
            return null;
        }
        SharedPreferences p = sp(c);
        String k = prefix(sig);
        // [comment] "land"：[comment]（[comment]）；
        // [comment] "sync" [comment]，[comment]"[comment]"。
        if (!p.contains(k + "land")) {
            return null;
        }
        return new Item(
                p.getBoolean(k + "sync", DEF_SYNC),
                p.getInt(k + "port", DEF_PORTRAIT),
                p.getInt(k + "land", DEF_LANDSCAPE),
                p.getBoolean(k + "fixtouser", DEF_FIX_TO_USER),
                p.getBoolean(k + "lockphone", DEF_LOCK_PHONE),
                p.getInt(k + "phonerot", DEF_PHONE_ROT),
                p.getBoolean(k + "resauto", DEF_RES_AUTO),
                p.getBoolean(k + "resenabled", DEF_RES_ENABLED),
                p.getInt(k + "resw", DEF_RES_W),
                p.getInt(k + "resh", DEF_RES_H),
                p.getInt(k + "resdpi", DEF_RES_DPI),
                p.getInt(k + "id", -1),
                p.getString(k + "label", ""));
    }

    /**
     * [comment]**[comment]**[comment]Settings[comment]。
     *
     * <p>[comment] {@link Prefs}，[comment] —— [comment]，[comment]，
     * [comment]Settings[comment]。
     */
    public static void saveFrom(Context c, String sig, String label, int displayId, Prefs p) {
        if (sig == null || sig.isEmpty()) {
            return;
        }
        String k = prefix(sig);
        sp(c).edit()
                .putBoolean(k + "sync", p.isEnabled())
                .putInt(k + "port", p.getPortraitAngle())
                .putInt(k + "land", p.getLandscapeAngle())
                .putBoolean(k + "fixtouser", p.isFixToUserRotation())
                .putBoolean(k + "lockphone", p.isLockPhoneRotation())
                .putInt(k + "phonerot", p.getPhoneRotation())
                .putBoolean(k + "resauto", p.isAutoMatchExt())
                .putBoolean(k + "resenabled", p.isResolutionEnabled())
                .putInt(k + "resw", p.getResWidth())
                .putInt(k + "resh", p.getResHeight())
                .putInt(k + "resdpi", p.getResDensity())
                .putInt(k + "id", displayId)
                .putString(k + "label", label == null ? "" : label)
                .apply();
    }

    public static void clear(Context c, String sig) {
        if (sig == null || sig.isEmpty()) {
            return;
        }
        String k = prefix(sig);
        sp(c).edit()
                .remove(k + "sync")
                // mode / fixed [comment]，[comment]Yes[comment] —— [comment]
                .remove(k + "mode")
                .remove(k + "port")
                .remove(k + "land")
                .remove(k + "fixed")
                .remove(k + "fixtouser")
                .remove(k + "lockphone")
                .remove(k + "phonerot")
                .remove(k + "resauto")
                .remove(k + "resenabled")
                .remove(k + "resw")
                .remove(k + "resh")
                .remove(k + "resdpi")
                .remove(k + "id")
                .remove(k + "label")
                .apply();
    }
}
