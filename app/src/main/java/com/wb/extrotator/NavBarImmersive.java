package com.wb.extrotator;

import android.content.Context;

/**
 * 「[comment]」：[comment]（[comment]）。
 *
 * <p><b>[comment]</b> —— [comment] {@code Settings.Global.policy_control} [comment]
 * {@code immersive.navigation=*}。[comment]Yes[comment]，[comment]Yes[comment]：
 * Good Lock [comment] NavStar [comment]Yes[comment]（[comment] process/navbar/EVIDENCE.md §7）。
 *
 * <p><b>[comment]Yes[comment]「[comment]」</b>：[comment]「[comment] / [comment] / [comment]」[comment]，
 * <b>[comment]"None[comment]"[comment]</b>；[comment] framework [comment]，None root [comment]
 * （RRO [comment] android [comment]，`cmd overlay fabricate` [comment] 2022-01 [comment] root）。
 * [comment]Yes[comment]。
 *
 * <p><b>[comment]Enable[comment]</b>：[comment]Yes[comment]，[comment]（NavStar、[comment]）。
 * [comment]Yes[comment]，[comment] —— [comment]。
 *
 * <p>⚠ <b>NavStar [comment]</b>：[comment]、[comment]。[comment]
 * [comment]（[comment] {@link #apply}），[comment]"[comment]、[comment]"。
 */
public final class NavBarImmersive {

    /** [comment]。`immersive.navigation=` [comment]，`*` = [comment]（[comment]） */
    public static final String STRATEGY = "immersive.navigation=*";

    private NavBarImmersive() {
    }

    /**
     * [comment]。
     *
     * @return [comment]Settings[comment]<b>[comment]</b>；[comment]（Shizuku [comment] / [comment]）[comment] <b>null</b>
     *         —— [comment]：[comment]Yes"[comment]"，[comment]Yes"[comment]"，[comment]。
     */
    public static String read() {
        String s = ShellRunner.run("settings get global policy_control", 10);
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.isEmpty() || s.startsWith("ERR") || s.contains("TIMEOUT")) {
            return null;
        }
        if ("null".equals(s)) {
            return "";
        }
        return s;
    }

    /**
     * [comment]"[comment]"。
     *
     * <p>[comment]Yes <b>[comment]</b> {@code *} [comment]：NavStar [comment]「[comment]」[comment]Yes
     * {@code *,-com.sec.android.app.launcher}（[comment]），[comment]<u>[comment]</u> ——
     * [comment]Yes[comment]。
     */
    public static boolean isOn(String policy) {
        if (policy == null || policy.isEmpty()) {
            return false;
        }
        // [comment]：[comment]Settings[comment] ':' [comment]，[comment]Yes key=value，[comment] flags
        for (String item : policy.split(":")) {
            String v = item.trim();
            if (!v.startsWith("immersive.navigation=")) {
                continue;
            }
            String pkgs = v.substring("immersive.navigation=".length()).trim();
            String[] parts = pkgs.split(",");
            return parts.length == 1 && "*".equals(parts[0].trim());
        }
        return false;
    }

    /**
     * [comment]。
     *
     * @param on true = [comment]，false = [comment]
     * @return null [comment]Success；No[comment]Yes[comment]
     */
    public static String apply(Context c, boolean on) {
        if (!ShellRunner.isReady()) {
            return "Shizuku Not Ready，Text";
        }
        final String before = read();
        if (before == null) {
            return "Text，Text Shizuku Text";
        }

        final String target;
        if (on) {
            // [comment] —— [comment]"[comment]"
            if (ExtPrefs.navImmBackup(c) == null) {
                ExtPrefs.setNavImmBackup(c, before);
            }
            target = STRATEGY;
        } else {
            String saved = clean(ExtPrefs.navImmBackup(c));
            if (saved.isEmpty() || isOn(saved)) {
                // [comment]（[comment] / [comment]），[comment]Yes[comment] —— [comment]
                target = "";
            } else {
                target = saved;
            }
        }

        ShellRunner.run("settings put global policy_control "
                + (target.isEmpty() ? "null" : "'" + target + "'"), 10);

        // [comment]：NavStar [comment]
        final String after = read();
        final boolean ok = on ? isOn(after) : !isOn(after);
        if (!ok) {
            return "Text（TextYes "
                    + (after == null || after.isEmpty() ? "Text" : after) + "）"
                    + "。Text Good Lock Text，Text NavStar Text。";
        }

        ExtPrefs.setNavImmOn(c, on);
        return null;
    }

    /** [comment] shell [comment]。[comment]，[comment] */
    private static String clean(String s) {
        return s == null ? "" : s.replace("'", "").trim();
    }
}
