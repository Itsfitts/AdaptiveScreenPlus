package com.wb.extrotator;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * [comment]「[comment]」[comment] —— [comment]，
 * [comment]。
 *
 * <p>[comment] MultiStar [comment]「[comment]」，[comment]Settings[comment]：
 * <pre>
 *   settings get secure multistar_cover_widget_backup_list
 *   → com.qq,0;com.douyin,0;...
 * </pre>
 * [comment]Yes {@code [comment],userId} [comment]。
 *
 * <p>⚠ [comment]「[comment] shell [comment]Settings」（[comment]Settings[comment]），[comment] {@link ShellRunner}
 * （Shizuku）。[comment]<b>[comment]</b>，[comment]。
 */
public final class CoverListImport {

    private static final String TAG = "CoverListImport";

    /** [comment] */
    private static final String NS = "secure";
    private static final String KEY = "multistar_cover_widget_backup_list";

    private CoverListImport() {
    }

    /**
     * [comment]（[comment]、[comment]）。
     *
     * <p>[comment]：[comment] / [comment] / [comment]。
     * [comment]Failed[comment]"[comment]Yes[comment]"[comment] —— [comment]（Notice[comment]）。
     */
    public static List<String> readRaw() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        try {
            String raw = ShellRunner.run("settings get " + NS + " " + KEY);
            if (raw == null) {
                return new ArrayList<>();
            }
            String v = raw.trim();
            if (TextUtils.isEmpty(v) || "null".equals(v)) {
                return new ArrayList<>();
            }
            for (String piece : v.split(";")) {
                String p = piece == null ? "" : piece.trim();
                if (p.isEmpty()) {
                    continue;
                }
                // [comment] "[comment],userId"，[comment]
                int comma = p.indexOf(',');
                String pkg = (comma > 0 ? p.substring(0, comma) : p).trim();
                if (!pkg.isEmpty() && pkg.indexOf('/') < 0 && pkg.indexOf(' ') < 0) {
                    out.add(pkg);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
        return new ArrayList<>(out);
    }

    /**
     * [comment] ∩ [comment]、[comment]。
     *
     * <p>[comment]：[comment]Yes[comment]，[comment]。
     * [comment]。
     *
     * <p>[comment]（[comment]Yes[comment]），[comment]Yes[comment]。
     */
    public static List<String> readInstalled(Context ctx) {
        List<String> raw = readRaw();
        if (raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<AppRepo.Item> installed = AppRepo.load(ctx);
        if (installed == null || installed.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> have = new LinkedHashSet<>();
        for (AppRepo.Item it : installed) {
            if (it != null && it.pkg != null) {
                have.add(it.pkg);
            }
        }
        List<String> out = new ArrayList<>();
        for (String p : raw) {
            if (have.contains(p) && !out.contains(p)) {
                out.add(p);
            }
        }
        Log.i(TAG, "Text " + raw.size() + " Text -> Text " + out.size() + " Text");
        return out;
    }
}
