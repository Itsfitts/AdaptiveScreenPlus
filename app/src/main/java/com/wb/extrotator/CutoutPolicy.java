package com.wb.extrotator;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 「[comment]」。
 *
 * <p><b>[comment]</b>：[comment]，[comment]「[comment]」。
 * [comment] {@code layoutInDisplayCutoutMode}，[comment]「[comment]」，
 * [comment]，[comment]（[comment]，[comment]Yes[comment]「[comment]」）。
 * [comment]：
 * {@code IActivityTaskManager.setCutoutPolicy(pkg, value)}，[comment] {@code service call} [comment]。
 *
 * <p><b>[comment]（[comment] {@code com.android.settings} [comment] A/B [comment]：[comment] 0/1/2/3 [comment] dump [comment]
 * [comment] {@code layoutInDisplayCutoutMode}，[comment] 0→always、1→always、2→never、3→always）</b>：
 * <ul>
 *   <li>{@link #POL_OFF} = <b>1</b> —— [comment] ⇒ <b>[comment]</b>；</li>
 *   <li>{@link #POL_DEFAULT} = <b>0</b> —— [comment]「Default」[comment]，[comment]
 *       （API 35 [comment]，[comment]「[comment]」）；</li>
 *   <li>2 = [comment]（[comment]）；3 = [comment]Yes[comment]（[comment] {@code always}）。</li>
 * </ul>
 * [comment]Yes[comment]（get = set + 1），[comment]Yes[comment]。
 *
 * <p>⚠ <b>[comment]「[comment]」</b>：[comment]「[comment] 1」、[comment]，[comment]
 * {@link #POL_DEFAULT}（0）。[comment]「[comment]」[comment]Yes 1，[comment] 0 [comment] ——
 * [comment]「[comment]」[comment]。
 *
 * <p>⚠ [comment]「[comment]」<b>[comment]</b>（[comment]），
 * [comment]Yes[comment]，[comment]。
 *
 * <p>[comment] {@link ShellRunner}（Shizuku → shell [comment]），[comment] Shizuku [comment]。
 */
public final class CutoutPolicy {

    private static final String TAG = "CutoutPolicy";

    /** [comment]：[comment] */
    public static final int POL_OFF = 1;
    /** [comment]"Default"[comment] */
    public static final int POL_DEFAULT = 0;

    /**
     * [comment]：[comment]Settings（[comment]Settings[comment]）[comment]。
     * <p>[comment]，[comment] —— [comment]，
     * [comment]。
     */
    private static final String SKIP_SETTINGS = "com.android.settings";

    private static final Pattern LAST_WORD =
            Pattern.compile("([0-9a-fA-F]{8})\\s*'");

    private CutoutPolicy() {
    }

    /**
     * [comment] {@code setCutoutPolicy} [comment] —— [comment] SDK [comment]
     * （34→107、35→115、36→126），[comment] release [comment]（14/15/16）。
     *
     * @return [comment]；[comment] SDK [comment] -1
     */
    public static int serviceCode() {
        int sdk = Build.VERSION.SDK_INT;
        if (sdk >= 36) {
            return 126;
        }
        if (sdk == 35) {
            return 115;
        }
        if (sdk == 34) {
            return 107;
        }
        String rel = Build.VERSION.RELEASE == null ? "" : Build.VERSION.RELEASE;
        if (rel.startsWith("16")) {
            return 126;
        }
        if (rel.startsWith("15")) {
            return 115;
        }
        if (rel.startsWith("14")) {
            return 107;
        }
        return -1;
    }

    public static boolean supported() {
        return serviceCode() > 0;
    }

    /**
     * [comment]。
     *
     * @return [comment]；[comment] / [comment] -1
     */
    public static int read(Context c, String pkg) {
        int code = serviceCode();
        if (code < 0) {
            return -1;
        }
        String out = ShellRunner.run(
                "service call activity_task " + (code + 1) + " i32 0 s16 " + pkg, 12);
        return parseValue(out);
    }

    /**
     * [comment] {@code Result: Parcel(  00000000 00000001   '........')} [comment] 8 [comment]。
     * <p>[comment]"[comment]"Yes[comment]Yes 0（[comment]），[comment]。
     */
    static int parseValue(String out) {
        if (out == null) {
            return -1;
        }
        int v = -1;
        try {
            Matcher m = LAST_WORD.matcher(out);
            while (m.find()) {
                v = (int) Long.parseLong(m.group(1), 16);
            }
        } catch (Throwable ignored) {
        }
        return v;
    }

    /**
     * [comment]<b>[comment]</b>[comment]（[comment] {@code com.android.settings} [comment]）。
     *
     * <p><b>[comment]</b>：[comment] {@code service call}，[comment] 40~60ms，[comment] ——
     * [comment]。
     *
     * @param policy {@link #POL_OFF} [comment] {@link #POL_DEFAULT}
     * @return [comment]（[comment] {@code OK=… FAIL=… SKIP=…}），[comment] ERR [comment]
     */
    public static String applyAll(Context c, int policy) {
        int code = serviceCode();
        if (code < 0) {
            return "ERR: Text Android Text（sdk="
                    + Build.VERSION.SDK_INT + " release=" + Build.VERSION.RELEASE + "）";
        }
        if (!ShellRunner.isReady()) {
            return "ERR: Shizuku Not Ready";
        }
        final String self = c.getPackageName();
        String cmd =
                "TMP=/data/local/tmp/extrot_cutout_$$.txt; "
                        + "pm list packages > \"$TMP\" 2>&1 || { echo ERR:pmlist; exit 3; }; "
                        + "OK=0; FAIL=0; SKIP=0; FIRSTFAIL=''; "
                        + "while IFS= read -r LINE; do "
                        + "PKG=${LINE#package:}; PKG=$(echo \"$PKG\" | tr -d '\\r'); "
                        + "if [ -z \"$PKG\" ] || [ \"$PKG\" = \"" + SKIP_SETTINGS + "\" ] "
                        + "|| [ \"$PKG\" = \"" + self + "\" ]; then SKIP=$((SKIP+1)); continue; fi; "
                        + "OUT=$(service call activity_task " + code
                        + " i32 0 s16 \"$PKG\" i32 " + policy + " 2>&1); "
                        + "if [ $? = 0 ]; then OK=$((OK+1)); "
                        + "else FAIL=$((FAIL+1)); "
                        + "[ -z \"$FIRSTFAIL\" ] && FIRSTFAIL=\"$PKG:$OUT\"; fi; "
                        + "done < \"$TMP\"; rm -f \"$TMP\"; "
                        + "echo \"OK=$OK FAIL=$FAIL SKIP=$SKIP $FIRSTFAIL\"";
        String out = ShellRunner.run(cmd, 300);
        Log.i(TAG, "applyAll policy=" + policy + " => " + out);
        return out;
    }
}
