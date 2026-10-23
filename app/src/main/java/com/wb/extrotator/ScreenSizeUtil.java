package com.wb.extrotator;

/**
 * [comment]（[comment] display 0）[comment]。
 *
 * <p>[comment]（Galaxy Z Flip5 / Android 14，[comment] adb [comment]）：
 * <pre>
 *   wm size -d 0              → [comment]Yes【[comment]】display 0 [comment]
 *   wm size -d 0 1080x2160    → [comment] -d [comment]，[comment]，[comment]
 *   wm size 1080x1920         → [comment]Success（Default[comment]Yes display 0，[comment]）
 *   wm size reset             → [comment]，[comment]
 * </pre>
 * [comment] {@code -d}。⚠ {@code wm user-rotation} [comment]，[comment] {@code -d}
 * Yes[comment]（[comment]）。
 */
public final class ScreenSizeUtil {

    private ScreenSizeUtil() {
    }

    /**
     * [comment]「[comment]」[comment]，[comment] [comment] ≥ [comment]。
     *
     * <p>[comment]：[comment]Yes[comment]（[comment] 1920×1080），[comment]
     * [comment]Yes[comment]，[comment] 1080×1920 [comment] 90° [comment]，
     * No[comment]。[comment] 1920×1080 → 1080×1920。
     *
     * @return [comment] {[comment], [comment]}；[comment]
     */
    public static int[] portrait(int w, int h) {
        if (w <= 0 || h <= 0) {
            return new int[]{w, h};
        }
        return w <= h ? new int[]{w, h} : new int[]{h, w};
    }

    /** [comment]Yes[comment]Yes[comment]（[comment] > [comment]） */
    public static boolean isLandscape(int w, int h) {
        return w > 0 && h > 0 && w > h;
    }

    /**
     * [comment]；density &le; 0 [comment] DPI。
     * <p>[comment] {@link #portrait} —— [comment]Yes[comment]，
     * [comment]None[comment]。
     */
    public static String apply(int w, int h, int density) {
        int[] p = portrait(w, h);
        StringBuilder sb = new StringBuilder();
        sb.append(ShellRunner.run("wm size " + p[0] + "x" + p[1]));
        if (density > 0) {
            sb.append(" | dpi: ").append(ShellRunner.run("wm density " + density));
        }
        return sb.toString().trim();
    }

    /**
     * [comment]，<b>[comment]</b>「[comment]」。
     *
     * <p>{@link #apply} [comment]（[comment] ≥ [comment]），[comment]Yes[comment]「[comment]」[comment]
     * （[comment]，[comment]）。
     *
     * <p>[comment]「[comment]」[comment]：[comment]，[comment] {@code wm size}
     * [comment]Yes「[comment]」[comment] —— [comment]「[comment]」Yes[comment]，[comment]Yes[comment]，[comment]。
     * [comment]，[comment]（[comment] CastActivity.applyCastResize）。
     */
    public static String write(int w, int h, int density) {
        if (w <= 0 || h <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ShellRunner.run("wm size " + w + "x" + h));
        if (density > 0) {
            sb.append(" | dpi: ").append(ShellRunner.run("wm density " + density));
        }
        return sb.toString().trim();
    }

    /** [comment]，[comment] */
    public static String reset(boolean alsoDensity) {
        StringBuilder sb = new StringBuilder();
        sb.append(ShellRunner.run("wm size reset"));
        if (alsoDensity) {
            sb.append(" | dpi: ").append(ShellRunner.run("wm density reset"));
        }
        return sb.toString().trim();
    }

    /**
     * [comment]（[comment]Settings[comment]）。
     *
     * <p>「[comment]」Yes<b>[comment]</b>[comment]、[comment]；[comment]
     * {@code wm size reset} [comment]：[comment]，reset [comment]
     * —— [comment]「[comment]Settings」。[comment]，[comment]。
     *
     * <p>⚠ [comment] {@code 1080x1920} [comment] x [comment]，[comment] {@link #restore}
     * [comment]。
     *
     * @return [comment] {@code 1080x1920} [comment]；[comment] {@code null}
     */
    public static String forced() {
        String s = ShellRunner.run("settings get global display_size_forced");
        if (s == null) {
            return null;
        }
        // [comment] "null"（[comment]），[comment]"[comment]"
        String t = s.trim();
        if (t.isEmpty() || "null".equalsIgnoreCase(t)) {
            return null;
        }
        /*
         * ⚠ v3.20 [comment]：Settings[comment]Yes**[comment]**（"1080,1920"），
         * [comment] `wm size` [comment] x（"1080x1920"）。[comment] restore() [comment]
         * [comment]Failed（[comment]），[comment]Failed。
         * [comment] x，[comment]"[comment]"[comment]"[comment]"Yes[comment]。
         */
        return t.replace(',', 'x');
    }

    /**
     * [comment]。
     *
     * @param prev {@link #forced()} [comment]；{@code null} = [comment]
     */
    public static String restore(String prev) {
        if (prev == null || prev.isEmpty()) {
            return ShellRunner.run("wm size reset");
        }
        // [comment]：[comment]YesSettings[comment]，[comment] wm size [comment] x（[comment] forced）
        return ShellRunner.run("wm size " + prev.replace(',', 'x'));
    }

    /**
     * [comment]「[comment]<b>[comment]</b>[comment]」[comment] —— [comment]。
     *
     * <p>[comment]Yes[comment]，[comment]、[comment]；
     * [comment]<b>[comment]</b>[comment]，[comment] —— [comment]Yes「[comment]」[comment]
     * （[comment]，[comment]）。
     *
     * <p>⚠ <b>[comment]Yes「[comment]」[comment]，[comment]Yes「[comment]」</b>：[comment] 90° [comment]
     * {@code wm size} [comment]Yes[comment]，[comment] —— [comment]
     * （[comment] {@code CastActivity.applyCastResize}），[comment]。
     *
     * <p>⚠ <b>[comment] 1:1</b>：[comment]「[comment]」[comment]<b>[comment]</b>[comment]，
     * [comment]，[comment]
     * （[comment]：[comment] 1199×1000，[comment]Yes 1080×1080）。
     *
     * @param boxW [comment]（[comment]）
     * @param boxH [comment]（[comment]）
     * @param base [comment]（[comment]，[comment]）
     * @return {[comment], [comment]}（[comment]）；[comment] null
     */
    public static int[] fitToBox(int boxW, int boxH, int base) {
        if (boxW <= 0 || boxH <= 0 || base <= 0) {
            return null;
        }
        double r = (double) boxW / (double) boxH;   // Text
        int vw, vh;
        if (r >= 1.0) {
            // [comment]Yes[comment]（[comment]）：[comment]，[comment]
            vh = base;
            vw = (int) Math.round(base * r);
        } else {
            vw = base;
            vh = (int) Math.round(base / r);
        }
        // [comment] 4 [comment]：[comment]，[comment]
        vw = Math.max(4, (vw + 2) / 4 * 4);
        vh = Math.max(4, (vh + 2) / 4 * 4);
        return new int[]{vw, vh};
    }
}
