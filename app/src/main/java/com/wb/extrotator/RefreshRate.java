package com.wb.extrotator;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 「[comment] → [comment]」[comment]：[comment] peak_refresh_rate / min_refresh_rate [comment]，[comment]。
 * ⚠ [comment] 60Hz [comment]（[comment]Yes[comment]，[comment] 60Hz）。
 */
public final class RefreshRate {

    private static final String TAG = "RefreshRate";

    private static final String KEY_PEAK = "peak_refresh_rate";
    private static final String KEY_MIN = "min_refresh_rate";

    /** [comment] */
    private static final float[] FALLBACK = {120f, 96f, 60f, 48f, 30f, 24f, 10f};

    private RefreshRate() {
    }

    /** [comment]，[comment]、[comment] */
    public static float[] supported(Context c) {
        List<Float> out = new ArrayList<>();
        try {
            DisplayManager dm = (DisplayManager) c.getSystemService(Context.DISPLAY_SERVICE);
            Display d = DisplayUtil.defaultDisplay(dm);
            if (d != null) {
                for (Display.Mode m : d.getSupportedModes()) {
                    float hz = m.getRefreshRate();
                    if (!has(out, hz)) {
                        out.add(hz);
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed：" + t);
        }
        if (out.size() < 2) {
            return FALLBACK.clone();
        }
        Collections.sort(out, Collections.reverseOrder());
        float[] a = new float[out.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = out.get(i);
        }
        return a;
    }

    /** [comment]；[comment]、[comment] -1。 */
    public static float readPeak(Context c) {
        return read(c, KEY_PEAK);
    }

    /** [comment]；[comment]、[comment] -1。 */
    public static float readMin(Context c) {
        return read(c, KEY_MIN);
    }

    /** [comment]。[comment] null = Success，No[comment]Yes[comment] */
    public static String apply(Context c, float a, float b) {
        float lo = Math.min(a, b), hi = Math.max(a, b);
        ContentResolver cr = c.getContentResolver();
        try {
            Settings.System.putFloat(cr, KEY_PEAK, hi);
            Settings.System.putFloat(cr, KEY_MIN, lo);
            return null;
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed：" + t);
            return "Text（" + t.getClass().getSimpleName() + "）";
        }
    }

    /** [comment]：[comment]，[comment] */
    public static String reset(Context c) {
        ContentResolver cr = c.getContentResolver();
        try {
            Settings.System.putString(cr, KEY_PEAK, null);
            Settings.System.putString(cr, KEY_MIN, null);
            return null;
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed：" + t);
            return "TextFailed（" + t.getClass().getSimpleName() + "）";
        }
    }

    /** [comment] */
    public static boolean innerActive(Context c) {
        try {
            DisplayManager dm = (DisplayManager) c.getSystemService(Context.DISPLAY_SERVICE);
            return DisplayUtil.isOn(DisplayUtil.defaultDisplay(dm));
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean has(List<Float> l, float v) {
        for (float f : l) {
            if (Math.abs(f - v) < 0.5f) {
                return true;
            }
        }
        return false;
    }

    private static float read(Context c, String key) {
        try {
            String s = Settings.System.getString(c.getContentResolver(), key);
            return s == null || s.isEmpty() ? -1f : Float.parseFloat(s);
        } catch (Throwable t) {
            return -1f;
        }
    }
}
