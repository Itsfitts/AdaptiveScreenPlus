package com.wb.extrotator;

/**
 * 「[comment]」[comment] —— [comment] {@link RotationService} [comment]Settings。
 *
 * <p>[comment]（[comment] / [comment] / [comment]）Yes[comment]<b>[comment]</b>[comment]，[comment]Yes
 * [comment]；[comment] / [comment] —— [comment]「[comment]」[comment]
 * [comment] cur [comment] 1080x1920 [comment] 1920x1080，[comment]Yes「[comment] 90°」。[comment]Yes
 * 「[comment]」：[comment]，[comment]，[comment]。
 *
 * <p>[comment]：{@link #on()} / {@link #off()} [comment] onStart / onStop [comment]，
 * [comment] evaluate [comment]；{@link #arm(long)} [comment]
 * [comment] —— [comment]<b>[comment]</b>[comment] onStart（[comment] 70ms）。
 *
 * <p>[comment]：[comment] {@code RotationService} [comment]（[comment]）[comment]。[comment]
 * [comment]「[comment] / [comment]」Yes[comment] action，[comment]。
 */
public final class ExtControlMode {

    /** [comment]（[comment]，[comment]Yes[comment]） */
    private static volatile int pages;

    /** [comment] —— [comment]"[comment] → [comment]"[comment]，[comment] */
    private static volatile long armedUntil;

    /**
     * [comment]。
     *
     * <p>[comment]：[comment] + [comment] + [comment] display [comment] + [comment] 150ms [comment]
     * + [comment] 400ms [comment] —— [comment] 1 [comment]，[comment] 3 [comment]Yes[comment]，
     * [comment]「[comment]」（[comment]Yes[comment] action，[comment]）。
     */
    private static final long EXIT_GRACE_MS = 3000L;

    private ExtControlMode() {
    }

    /** [comment] */
    public static void on() {
        pages++;
    }

    /** [comment] */
    public static void off() {
        if (pages > 0) {
            pages--;
        }
        // ⚠ [comment]（[comment] arm [comment]）：[comment]
        // （[comment]），[comment] display [comment] evaluate [comment]。
        // [comment] pages [comment]、[comment]，[comment] / [comment] —— [comment]Yes
        // 「[comment]、[comment]」。[comment]，[comment]
        // [comment]，[comment]。
        arm(EXIT_GRACE_MS);
    }

    /**
     * [comment]（[comment]）。[comment]"[comment]"。
     *
     * <p>[comment]、[comment] —— [comment]。
     */
    public static void arm(long ms) {
        long t = System.currentTimeMillis() + Math.max(1000L, ms);
        if (t > armedUntil) {
            armedUntil = t;
        }
    }

    /** [comment]（[comment]） */
    public static boolean active() {
        return pages > 0 || System.currentTimeMillis() < armedUntil;
    }
}
