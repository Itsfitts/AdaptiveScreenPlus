package com.wb.extrotator;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * 「[comment]」[comment]（[comment] 2×2）[comment]，[comment]。
 *
 * <p><b>[comment]</b>：[comment]、CPU [comment]、[comment]、[comment]。
 *
 * <p><b>[comment]</b>：[comment]「[comment]」[comment]，[comment]
 * 「Unauthorized」—— [comment]Yes[comment]Notice（[comment]："[comment]Unauthorized，[comment]"）。
 * [comment]**[comment]**[comment]，[comment]。
 *
 * <p><b>[comment] {@link StatusMonitor}</b>：[comment]Yes[comment]「[comment]」[comment] —— [comment]
 * Activity、[comment]、[comment] 60 [comment]。[comment]Yes[comment]：[comment]，
 * [comment]，**[comment]**，[comment]。
 *
 * <p><b>[comment]Yes[comment] Bitmap</b>：RemoteViews [comment]，
 * [comment]、[comment]，[comment] —— [comment] ImageView。
 * （[comment] ProgressBar [comment]，[comment]。）
 */
public final class WidgetStatus {

    /** [comment]。[comment] -1 / NaN [comment]"[comment]"，[comment]。 */
    public static final class Data {
        /** [comment] 0~100；-1 = [comment] */
        public int battPct = -1;
        public boolean charging;
        /** CPU [comment]，[comment]；NaN = [comment] */
        public float tempC = Float.NaN;
        /** [comment] 0~100；-1 = [comment] */
        public int memPct = -1;
        public long memUsedMb = -1, memTotalMb = -1;
        /** [comment] 0~100；-1 = [comment] */
        public int storePct = -1;
        public long storeUsedGb = -1, storeTotalGb = -1;
    }

    private WidgetStatus() {
    }

    // ============================================================ [comment]

    public static Data sample(Context c) {
        Data d = new Data();
        try {
            sampleBattery(c, d);
        } catch (Throwable ignored) {
        }
        try {
            d.tempC = readTempC();
        } catch (Throwable ignored) {
        }
        try {
            sampleMem(c, d);
        } catch (Throwable ignored) {
        }
        try {
            sampleStore(d);
        } catch (Throwable ignored) {
        }
        return d;
    }

    private static void sampleBattery(Context c, Data d) {
        BatteryManager bm = (BatteryManager) c.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            int p = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if (p > 0) {
                d.battPct = Math.min(100, p);
            }
        }
        // [comment]（getIntProperty [comment]）
        Intent it = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (it != null) {
            int st = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            d.charging = st == BatteryManager.BATTERY_STATUS_CHARGING
                    || st == BatteryManager.BATTERY_STATUS_FULL;
            if (d.battPct < 0) {
                int lv = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int sc = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (lv >= 0 && sc > 0) {
                    d.battPct = lv * 100 / sc;
                }
            }
        }
    }

    private static void sampleMem(Context c, Data d) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            return;
        }
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long total = mi.totalMem;
        long avail = mi.availMem;
        if (total <= 0) {
            return;
        }
        d.memTotalMb = total / 1048576L;
        d.memUsedMb = Math.max(0, total - avail) / 1048576L;
        d.memPct = (int) Math.round((total - avail) * 100.0 / total);
    }

    /**
     * [comment]。
     *
     * <p>[comment]「[comment]」[comment]：{@code Environment.getDataDirectory()} [comment]
     * [comment]。⚠ [comment] {@code StatFs(path)} [comment] —— [comment] SDK 30 [comment] API。
     */
    private static void sampleStore(Data d) {
        StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
        long total = fs.getTotalBytes();
        long avail = fs.getAvailableBytes();
        if (total <= 0) {
            return;
        }
        d.storeTotalGb = total / 1073741824L;
        d.storeUsedGb = Math.max(0, total - avail) / 1073741824L;
        d.storePct = (int) Math.round((total - avail) * 100.0 / total);
    }

    /**
     * CPU [comment]（[comment]）。
     *
     * <p>[comment] {@link StatusMonitor} [comment] —— [comment]Yes[comment] thermal zone [comment]；
     * [comment]，Yes[comment]Yes private、[comment]。
     */
    private static float readTempC() {
        float best = Float.NaN;
        for (int i = 0; i < 40; i++) {
            File z = new File("/sys/class/thermal/thermal_zone" + i);
            String type = readLine(new File(z, "type"));
            if (type == null) {
                continue;
            }
            String t = type.toLowerCase();
            if (!(t.contains("cpu") || t.contains("soc") || t.contains("tsens")
                    || t.contains("ap") || t.contains("core"))) {
                continue;
            }
            long v = readLong(new File(z, "temp"));
            if (v <= 0) {
                continue;
            }
            float cc = v >= 1000 ? v / 1000f : v;   // TextYesText
            if (Float.isNaN(best) || cc > best) {
                best = cc;
            }
        }
        return best;
    }

    private static String readLine(File f) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(f));
            return r.readLine();
        } catch (Throwable t) {
            return null;
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static long readLong(File f) {
        String s = readLine(f);
        if (s == null) {
            return -1;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (Throwable t) {
            return -1;
        }
    }

    // ============================================================ [comment]

    /**
     * [comment]（[comment]，[comment]）。
     *
     * <p>[comment]Yes 2×2：[comment] · [comment] CPU [comment] · [comment] · [comment]。
     *
     * <p><b>[comment]</b>：2×2 [comment] 4×2 [comment]（75dp [comment] 156dp），
     * [comment] dp [comment] —— [comment]。
     * [comment]Yes「[comment] / [comment] / [comment]」[comment]，[comment]，
     * 「[comment]」[comment]。
     *
     * @param w [comment]（[comment]）
     * @param h [comment]（[comment]）；[comment]Yes 150×133dp，[comment] 1dp ≈ h/133 [comment]
     */
    public static Bitmap draw(int w, int h, Data d) {
        Bitmap bmp = Bitmap.createBitmap(Math.max(1, w), Math.max(1, h), Bitmap.Config.ARGB_8888);
        Canvas g = new Canvas(bmp);
        float k = h / 133f;                     // dp → Text

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.CENTER);

        float cw = w / 2f, ch = h / 2f;

        // [comment]：[comment]；[comment]（[comment]Yes"[comment]"[comment]）
        cell(g, p, k, 0, 0, cw, ch, "Text",
                d.battPct < 0 ? "—" : d.battPct + "%",
                d.battPct < 0 ? -1f : d.battPct / 100f,
                d.charging ? 0xFF26C6DA : 0xFF43A047);

        // CPU [comment]：20~70℃ [comment] 0~100%
        cell(g, p, k, 1, 0, cw, ch, "CPU",
                Float.isNaN(d.tempC) ? "—" : (Math.round(d.tempC) + "°"),
                Float.isNaN(d.tempC) ? -1f : (d.tempC - 20f) / 50f,
                tempColor(d.tempC));

        cell(g, p, k, 0, 1, cw, ch, "Text",
                d.memPct < 0 ? "—" : d.memPct + "%",
                d.memPct < 0 ? -1f : d.memPct / 100f, 0xFF7E57C2);

        cell(g, p, k, 1, 1, cw, ch, "Text",
                d.storePct < 0 ? "—" : d.storePct + "%",
                d.storePct < 0 ? -1f : d.storePct / 100f, 0xFF29B6F6);

        return bmp;
    }

    private static int tempColor(float c) {
        if (Float.isNaN(c)) {
            return 0xFF9E9E9E;
        }
        if (c >= 55f) {
            return 0xFFE53935;
        }
        if (c >= 45f) {
            return 0xFFFB8C00;
        }
        return 0xFF43A047;
    }

    /**
     * [comment]：[comment]、[comment]、[comment]，[comment]。
     *
     * @param frac [comment]；[comment] 0 [comment]"[comment]"（[comment]）
     */
    private static void cell(Canvas g, Paint p, float k, int col, int row,
                             float cw, float ch, String label, String value,
                             float frac, int color) {
        float left = col * cw, top = row * ch;
        float cx = left + cw / 2f;

        p.setStyle(Paint.Style.FILL);

        p.setColor(0xB3FFFFFF);
        p.setTextSize(10.5f * k);
        g.drawText(label, cx, top + ch * 0.30f, p);

        p.setColor(color);
        p.setTextSize(17f * k);
        g.drawText(value, cx, top + ch * 0.66f, p);

        float bw = cw * 0.72f;
        float bh = Math.max(1.5f, 3.4f * k);
        float bx = cx - bw / 2f;
        float by = top + ch * 0.80f;
        p.setColor(0x2EFFFFFF);
        g.drawRoundRect(new RectF(bx, by, bx + bw, by + bh), bh / 2f, bh / 2f, p);
        if (frac > 0f) {
            float fw = bw * Math.min(1f, Math.max(0f, frac));
            p.setColor(color);
            g.drawRoundRect(new RectF(bx, by, bx + fw, by + bh), bh / 2f, bh / 2f, p);
        }
    }
}
