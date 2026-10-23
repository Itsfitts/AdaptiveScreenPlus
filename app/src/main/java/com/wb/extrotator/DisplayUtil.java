package com.wb.extrotator;

import android.content.ContentResolver;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.provider.Settings;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

/** [comment] */
public final class DisplayUtil {

    private DisplayUtil() {
    }

    /**
     * [comment]Yes[comment]Yes「[comment]」（[comment] / [comment]），[comment]。
     *
     * [comment]（android.view.Display [comment] getType()，[comment]）：
     *  1. [comment]/[comment] DisplayCutout —— [comment]，[comment]Yes null
     *  2. [comment]Default[comment] —— [comment]（Z Flip [comment]）[comment]（"[comment]"）
     */
    public static boolean looksInternal(Display d, String defaultName) {
        if (d == null) {
            return true;
        }
        try {
            DisplayCutout cutout = d.getCutout();
            if (cutout != null) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            String name = String.valueOf(d.getName());
            if (defaultName != null && defaultName.equals(name)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * [comment]。
     *
     * <p>[comment]：Default[comment]（[comment]）、[comment]（[comment] / [comment]）、[comment] STATE_ON [comment]
     * （[comment] / AOD [comment]）、[comment]Yes[comment]、[comment]
     * （[comment] {@link #looksInternal}）。
     *
     * <p>[comment]：Galaxy Z Flip [comment]（displayId=1）Yes[comment] FLAG_PRESENTATION [comment]
     * [comment]，[comment]Yes STATE_ON，[comment] —— [comment]，[comment]
     * [comment]，[comment]Yes[comment]。
     */
    public static List<Display> externalDisplays(DisplayManager dm) {
        List<Display> out = new ArrayList<>();
        if (dm == null) {
            return out;
        }
        Display[] all;
        try {
            all = dm.getDisplays();
        } catch (Throwable t) {
            return out;
        }

        String defaultName = null;
        try {
            Display def = dm.getDisplay(Display.DEFAULT_DISPLAY);
            if (def != null) {
                defaultName = String.valueOf(def.getName());
            }
        } catch (Throwable ignored) {
        }

        for (Display d : all) {
            if (d == null) {
                continue;
            }
            try {
                if (d.getDisplayId() == Display.DEFAULT_DISPLAY) {
                    continue;
                }
                if ((d.getFlags() & Display.FLAG_PRIVATE) != 0) {
                    continue;
                }
                // [comment]"[comment]"[comment]：[comment]Yes STATE_ON
                if (d.getState() != Display.STATE_ON) {
                    continue;
                }
                String name = String.valueOf(d.getName());
                if (name.contains("Virtual") || name.contains("Overlay") || name.contains("Emulator")) {
                    continue;
                }
                if (looksInternal(d, defaultName)) {
                    continue;
                }
                out.add(d);
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    public static Display defaultDisplay(DisplayManager dm) {
        if (dm == null) {
            return null;
        }
        try {
            return dm.getDisplay(Display.DEFAULT_DISPLAY);
        } catch (Throwable t) {
            return null;
        }
    }

    /** [comment] id [comment]（[comment] 8 [comment]） */
    private static final int MAX_PROBE_ID = 12;

    /**
     * [comment] —— <b>[comment] {@code dm.getDisplays()} [comment]</b>。
     *
     * <p>[comment]（Galaxy Z Flip5 / Android 14）：[comment]（displayId=1）[comment] state [comment]Yes ON、
     * {@code dumpsys display} [comment]，[comment] {@code dm.getDisplays()} [comment]，
     * [comment]；[comment] {@code dm.getDisplay(1)} Yes[comment]。
     *
     * <p>[comment]：[comment] {@code getDisplays()} [comment]（[comment] null），[comment]
     * 「[comment]」，「[comment]」[comment]「[comment]」。
     *
     * <p>[comment] id [comment] 0 [comment] {@link #MAX_PROBE_ID} [comment] {@code getDisplay()} [comment]；[comment]
     * null，[comment]。
     */
    public static List<Display> allDisplays(DisplayManager dm) {
        List<Display> out = new ArrayList<>();
        if (dm == null) {
            return out;
        }
        for (int id = 0; id <= MAX_PROBE_ID; id++) {
            Display d = null;
            try {
                d = dm.getDisplay(id);
            } catch (Throwable ignored) {
            }
            if (d != null) {
                out.add(d);
            }
        }
        return out;
    }

    /**
     * [comment]「[comment]」—— [comment]<b>[comment]YesDefault[comment]</b>[comment]（Galaxy Z Flip [comment]）。
     *
     * <p>[comment] {@link #externalDisplays} [comment]：[comment]，[comment]
     * {@link #looksInternal} [comment]（[comment] [comment] [comment]Default[comment]）[comment]。[comment] Z Flip5 [comment]
     * cutout（[comment] DisplayInfo [comment] cutout [comment]），[comment]Yes「[comment]」。
     *
     * <p>[comment]<b>[comment] state</b>：[comment]Yes STATE_OFF / [comment] {@code getDisplays()} [comment]
     * [comment]，[comment]，[comment] —— [comment] {@link #allDisplays}。
     *
     * @return [comment] null（[comment]Yes null）
     */
    public static Display coverDisplay(DisplayManager dm) {
        if (dm == null) {
            return null;
        }
        String defaultName = null;
        Display def = defaultDisplay(dm);
        if (def != null) {
            defaultName = String.valueOf(def.getName());
        }
        for (Display d : allDisplays(dm)) {
            if (d == null) {
                continue;
            }
            try {
                if (d.getDisplayId() == Display.DEFAULT_DISPLAY) {
                    continue;
                }
                if ((d.getFlags() & Display.FLAG_PRIVATE) != 0) {
                    continue;
                }
                if (looksInternal(d, defaultName)) {
                    return d;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /**
     * [comment]Default[comment]、**[comment]**[comment]：[comment] + [comment]。
     * [comment] {@link #externalDisplays} [comment]：[comment] STATE_ON，[comment]。
     * [comment] {@link #allDisplays}，No[comment]。
     */
    public static List<Display> secondaryDisplays(DisplayManager dm) {
        List<Display> out = new ArrayList<>();
        for (Display d : allDisplays(dm)) {
            if (d == null) {
                continue;
            }
            try {
                if (d.getDisplayId() == Display.DEFAULT_DISPLAY) {
                    continue;
                }
                if ((d.getFlags() & Display.FLAG_PRIVATE) != 0) {
                    continue;
                }
                String name = String.valueOf(d.getName());
                if (name.contains("Virtual") || name.contains("Overlay")
                        || name.contains("Emulator") || name.contains("Text")) {
                    continue;
                }
                out.add(d);
            } catch (Throwable ignored) {
            }
        }
        return out;
    }

    /** [comment]Yes[comment]Yes[comment]（[comment]YesDefault[comment]） */
    public static boolean isCover(DisplayManager dm, int displayId) {
        Display c = coverDisplay(dm);
        return c != null && c.getDisplayId() == displayId;
    }

    /** [comment]YesNo[comment] */
    public static boolean isOn(Display d) {
        try {
            return d != null && d.getState() == Display.STATE_ON;
        } catch (Throwable t) {
            return false;
        }
    }

    /** [comment]，[comment] */
    public static String kindOf(DisplayManager dm, Display d) {
        if (d == null) {
            return "Text";
        }
        if (d.getDisplayId() == Display.DEFAULT_DISPLAY) {
            return "Text";
        }
        if (isCover(dm, d.getDisplayId())) {
            return "Text（Text）";
        }
        return "Text";
    }

    /**
     * [comment]Yes[comment]Yes「[comment]」[comment]。
     *
     * <p><b>[comment] rotation Yes[comment]Yes 0/180</b> —— [comment]「[comment]Yes[comment]」，[comment]
     * [comment]（Z Flip5 [comment] 748×720）[comment]Yes<b>[comment]</b>[comment]：[comment] 0°/180° [comment]Yes[comment]、
     * 90°/270° [comment]Yes[comment]，[comment]，[comment]Yes[comment]。
     *
     * <p>[comment]「[comment]」，[comment] 90/270 [comment] —— [comment]Yes[comment]
     * {@link #nativeSize}（Display.Mode [comment]）[comment]，[comment] wm size [comment]。
     */
    public static boolean isPhonePortrait(Display d) {
        if (d == null) {
            return false;
        }
        return isPortraitAt(d, d.getRotation());
    }

    /**
     * [comment] rotation [comment]Yes[comment]Yes[comment]（{@link #isPhonePortrait} [comment]）。
     *
     * <p>[comment]Yes[comment]<b>[comment]</b>[comment] rotation [comment] ——
     * [comment] {@code user_rotation}，[comment]"[comment] rotation
     * [comment]/[comment]"[comment]。
     */
    public static boolean isPortraitAt(Display d, int rotation) {
        if (d == null) {
            return false;
        }
        boolean quarterTurn = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
        int[] n = nativeSize(d);
        boolean naturalLandscape = n[0] > 0 && n[1] > 0 && n[0] > n[1];
        boolean landscape = naturalLandscape ? !quarterTurn : quarterTurn;
        return !landscape;
    }

    /**
     * 「[comment]」[comment]Default[comment]。
     *
     * <p>[comment]（{@code accelerometer_rotation == 0}），Default[comment]<b>[comment]
     * {@code user_rotation} [comment]</b>，[comment] —— [comment]Yes[comment]，[comment]
     * {@code getRotation()} [comment]：[comment]（Z Flip5 / Android 14）[comment]（[comment]
     * {@code device_state state 4}）[comment]，[comment] rotation [comment] 0，[comment] {@code user_rotation}
     * [comment]Yes 1（[comment]）→ [comment]「[comment]」，[comment] 270°。
     *
     * <p>[comment]，[comment] -1 [comment]「[comment]」。
     *
     * @return 0..3（Surface.ROTATION_*）；[comment] -1
     */
    public static int policyRotation(ContentResolver cr) {
        if (cr == null) {
            return -1;
        }
        try {
            if (Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 1) != 0) {
                return -1;
            }
            return Settings.System.getInt(cr, Settings.System.USER_ROTATION, 0);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * 「[comment]」—— [comment]Yes[comment]Yes[comment]。
     *
     * <p><b>[comment] {@code dm.getDisplay(DEFAULT_DISPLAY)}</b>：[comment]Default[comment]（[comment]）Yes
     * {@code STATE_OFF}，[comment] {@code getRotation()} [comment]（[comment] Z Flip5 [comment] 0），
     * [comment] —— [comment]「[comment]Yes[comment]」，[comment]，
     * [comment]Yes「[comment]Yes[comment]，[comment] 270°」[comment]。
     *
     * <p>[comment]：Default[comment]Default[comment]；Default[comment]（[comment]Yes[comment]）。
     * [comment]（[comment] / [comment]）[comment]Default[comment]，[comment] {@link #isOn} [comment]
     * [comment]。
     */
    public static Display activePhoneDisplay(DisplayManager dm) {
        Display def = defaultDisplay(dm);
        if (isOn(def)) {
            return def;
        }
        String defaultName = def == null ? null : String.valueOf(def.getName());
        for (Display d : allDisplays(dm)) {
            if (d == null) {
                continue;
            }
            try {
                if ((d.getFlags() & Display.FLAG_PRIVATE) != 0) {
                    continue;
                }
                if (!looksInternal(d, defaultName)) {
                    continue;
                }
                if (isOn(d)) {
                    return d;
                }
            } catch (Throwable ignored) {
            }
        }
        return def;
    }

    /**
     * [comment]：[comment]"[comment]"[comment]，[comment]。
     *
     * <p>[comment]"[comment]"[comment]（[comment] rotation [comment] / [comment]
     * [comment]），[comment] rotation、Mode [comment]、[comment]、[comment]。
     */
    public static String orientationDebug(Display d) {
        if (d == null) {
            return "（NoneText）";
        }
        int[] n = nativeSize(d);
        return "rot=" + d.getRotation()
                + " mode=" + n[0] + "x" + n[1]
                + " real=" + sizeOf(d)
                + " state=" + stateName(d)
                + " Text=" + (isPhonePortrait(d) ? "Text" : "Text");
    }

    /** [comment] */
    public static String stateName(Display d) {
        try {
            switch (d.getState()) {
                case Display.STATE_ON:
                    return "ON";
                case Display.STATE_OFF:
                    return "OFF";
                case Display.STATE_DOZE:
                    return "DOZE";
                case Display.STATE_DOZE_SUSPEND:
                    return "DOZE_SUSPEND";
                case Display.STATE_UNKNOWN:
                    return "UNKNOWN";
                default:
                    return String.valueOf(d.getState());
            }
        } catch (Throwable t) {
            return "?";
        }
    }

    public static int degrees(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }

    public static String sizeOf(Display d) {
        try {
            Point p = new Point();
            d.getRealSize(p);
            return p.x + "×" + p.y;
        } catch (Throwable t) {
            return "?×?";
        }
    }

    /**
     * [comment]「[comment]」，[comment]Yes[comment]。
     *
     * <p>[comment] {@link #sizeOf} [comment]：{@code getRealSize} [comment]Yes<b>[comment]</b>[comment]，[comment] wm size
     * [comment]；{@code Display.Mode} [comment] physicalWidth/Height [comment]Yes[comment]。
     * [comment]「[comment]」[comment]，No[comment]Yes[comment]，[comment]。
     *
     * @return {[comment], [comment]}；[comment] {0, 0}
     */
    public static int[] nativeSize(Display d) {
        if (d == null) {
            return new int[]{0, 0};
        }
        try {
            Display.Mode m = d.getMode();
            if (m != null && m.getPhysicalWidth() > 0 && m.getPhysicalHeight() > 0) {
                return new int[]{m.getPhysicalWidth(), m.getPhysicalHeight()};
            }
        } catch (Throwable ignored) {
        }
        try {
            Point p = new Point();
            d.getRealSize(p);
            return new int[]{p.x, p.y};
        } catch (Throwable ignored) {
        }
        return new int[]{0, 0};
    }

    /**
     * [comment]。
     *
     * <p>[comment]，[comment] —— [comment]Yes 1920×1080（[comment]）[comment]
     * [comment] 1080×1920，[comment]、[comment] 90° [comment]。
     */
    public static int[] suggestedInternalSize(Display ext) {
        int[] n = nativeSize(ext);
        if (n[0] <= 0 || n[1] <= 0) {
            return new int[]{0, 0};
        }
        return ScreenSizeUtil.portrait(n[0], n[1]);
    }

    /**
     * [comment]「[comment]」—— [comment]；[comment] null。
     *
     * <p>[comment]、[comment]，[comment]，[comment]：
     * 「[comment]SaveSettings」[comment]、[comment]、[comment]（{@code syncResFromExt}）
     * [comment]。[comment] —— [comment]「[comment]」，
     * [comment]「[comment]Yes[comment]」。
     */
    public static Display largestByPanel(List<Display> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        Display best = null;
        long bestPx = -1;
        for (Display d : list) {
            int[] n = nativeSize(d);
            long px = (long) n[0] * n[1];
            if (px > bestPx) {
                bestPx = px;
                best = d;
            }
        }
        return best;
    }

    public static String describe(Display d) {
        return "ID " + d.getDisplayId()
                + " · " + sizeOf(d)
                + " · Text " + degrees(d.getRotation()) + "°";
    }

    /** [comment]，[comment] "ID 5 · [comment] 1920×1080 · [comment] 1080×1920 · [comment] 90°" */
    public static String describeFull(Display d) {
        int[] n = nativeSize(d);
        return "ID " + d.getDisplayId()
                + " · Text " + n[0] + "×" + n[1]
                + " · Text " + sizeOf(d)
                + " · Text " + degrees(d.getRotation()) + "°";
    }

    /**
     * [comment]YesNo[comment]「[comment]」[comment]（[comment] wm size）。
     *
     * <p>[comment]：{@code Display.getMode()} [comment]Yes[comment]，{@code Display.getRealSize()}
     * [comment]Yes[comment]，[comment] ⇒ [comment]。
     *
     * <p>[comment]：[comment]，[comment]「[comment]」[comment]，
     * [comment]Yes[comment]、[comment]。
     *
     * @return [comment] 1080×2640 → 1080×1920，[comment] null
     */
    public static String sizeOverrideInfo(Display d) {
        if (d == null) {
            return null;
        }
        try {
            Point real = new Point();
            d.getRealSize(real);
            Display.Mode mode = d.getMode();
            if (mode == null) {
                return null;
            }
            int pw = mode.getPhysicalWidth();
            int ph = mode.getPhysicalHeight();
            int rMin = Math.min(real.x, real.y);
            int rMax = Math.max(real.x, real.y);
            int pMin = Math.min(pw, ph);
            int pMax = Math.max(pw, ph);
            if (rMin == pMin && rMax == pMax) {
                return null;
            }
            return pMin + "×" + pMax + " → " + rMin + "×" + rMax;
        } catch (Throwable t) {
            return null;
        }
    }
}
