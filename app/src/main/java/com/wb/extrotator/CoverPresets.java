package com.wb.extrotator;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * [comment]「[comment]」[comment]。
 *
 * <p>[comment] = [comment] + [comment] + DPI。[comment] SharedPreferences [comment]，
 * [comment] {@link Prefs}（[comment]Yes[comment]Settings）[comment] —— [comment]Yes Beta [comment]，
 * [comment]。
 *
 * <p>[comment] JSON [comment]Yes[comment] key：[comment]Yes[comment]，
 * [comment] name_0/size_0/... [comment] key [comment]。
 */
public final class CoverPresets {

    private static final String FILE = "extrot_cover_presets";
    private static final String KEY_LIST = "list";
    /** [comment]：[comment] */
    public static final int MAX = 12;

    private CoverPresets() {
    }

    /** [comment] */
    public static final class Preset {
        public String name;
        public int w;
        public int h;
        public int dpi;

        public Preset(String name, int w, int h, int dpi) {
            this.name = name;
            this.w = w;
            this.h = h;
            this.dpi = dpi;
        }

        /** [comment]，[comment] "748×720 · 340dpi" */
        public String summary() {
            return w + "×" + h + (dpi > 0 ? " · " + dpi + "dpi" : " · Text DPI");
        }
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /**
     * [comment]。
     *
     * <p>[comment]（[comment]）[comment]，[comment]Default[comment] ——
     * [comment] 748×720 [comment] Z Flip5 [comment]，[comment]。
     *
     * @param physW/physH/physDpi [comment]；[comment] 0 [comment]，[comment] Z Flip5 [comment]
     */
    public static List<Preset> load(Context c, int physW, int physH, int physDpi) {
        List<Preset> out = new ArrayList<>();
        String raw = sp(c).getString(KEY_LIST, null);
        if (raw == null) {
            return defaults(physW, physH, physDpi);
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Preset(o.optString("name", "Text " + (i + 1)),
                        o.optInt("w"), o.optInt("h"), o.optInt("dpi")));
            }
        } catch (Throwable t) {
            return defaults(physW, physH, physDpi);
        }
        return out.isEmpty() ? defaults(physW, physH, physDpi) : out;
    }

    public static void save(Context c, List<Preset> list) {
        JSONArray arr = new JSONArray();
        for (Preset p : list) {
            JSONObject o = new JSONObject();
            try {
                o.put("name", p.name);
                o.put("w", p.w);
                o.put("h", p.h);
                o.put("dpi", p.dpi);
            } catch (Throwable ignored) {
            }
            arr.put(o);
        }
        sp(c).edit().putString(KEY_LIST, arr.toString()).apply();
    }

    public static void add(Context c, List<Preset> list, Preset p) {
        if (list.size() >= MAX) {
            return;
        }
        list.add(p);
        save(c, list);
    }

    public static void removeAt(Context c, List<Preset> list, int index) {
        if (index < 0 || index >= list.size()) {
            return;
        }
        list.remove(index);
        save(c, list);
    }

    /** [comment]Default[comment] */
    public static List<Preset> resetToDefault(Context c, int physW, int physH, int physDpi) {
        List<Preset> d = defaults(physW, physH, physDpi);
        save(c, d);
        return d;
    }

    /**
     * Default[comment]：[comment] + [comment]。
     * DPI [comment]，[comment]「[comment]」[comment]，[comment]Yes[comment] ——
     * [comment]Yes[comment]（[comment]、[comment]）。
     */
    private static List<Preset> defaults(int physW, int physH, int physDpi) {
        if (physW <= 0 || physH <= 0) {
            physW = 748;
            physH = 720;
        }
        if (physDpi <= 0) {
            physDpi = 340;
        }
        List<Preset> out = new ArrayList<>();
        out.add(new Preset("Text", physW, physH, physDpi));
        int[] scales = {90, 80, 70};
        for (int s : scales) {
            int w = Math.round(physW * s / 100f);
            int h = Math.round(physH * s / 100f);
            int dpi = Math.round(physDpi * s / 100f);
            out.add(new Preset("Text " + s + "%", w, h, dpi));
        }
        return out;
    }
}
