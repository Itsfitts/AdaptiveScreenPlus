package com.wb.extrotator;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 「Custom[comment]」—— [comment] shell [comment]。
 *
 * <p>[comment]：Beta [comment]（[comment]）、{@link QuickActions} [comment]
 * （[comment]）、[comment]。[comment] {@link QuickPrefs} [comment] SharedPreferences [comment]，
 * [comment] Beta [comment]。
 *
 * <p>[comment]Yes JSON [comment]（[comment] Android [comment] org.json），[comment]：id / name / cmd。
 * [comment] key [comment] {@code custom:<id>}，[comment]，[comment] {@link #KEY_PREFIX}。
 */
public final class CustomCmds {

    /** Custom[comment] key [comment] */
    public static final String KEY_PREFIX = "custom:";

    private static final String FILE = "extrot_quick";
    private static final String KEY_LIST = "custom_cmds";
    /** [comment]，[comment] */
    private static final int MAX_NAME = 24;

    private CustomCmds() {
    }

    /** [comment]Custom[comment] */
    public static final class Cmd {
        public String id = "";
        public String name = "";
        public String cmd = "";
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** [comment]，[comment] */
    public static List<Cmd> list(Context c) {
        List<Cmd> out = new ArrayList<>();
        String raw = sp(c).getString(KEY_LIST, "");
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                Cmd x = new Cmd();
                x.id = o.optString("id", "");
                x.name = o.optString("name", "");
                x.cmd = o.optString("cmd", "");
                if (!x.id.isEmpty() && !x.cmd.isEmpty()) {
                    out.add(x);
                }
            }
        } catch (Throwable t) {
            // [comment] —— [comment]
        }
        return out;
    }

    /** [comment] id [comment]；[comment] null */
    public static Cmd find(Context c, String id) {
        if (id == null) {
            return null;
        }
        for (Cmd x : list(c)) {
            if (id.equals(x.id)) {
                return x;
            }
        }
        return null;
    }

    /** Add[comment]Update（id [comment] = Add，[comment]） */
    public static void put(Context c, Cmd item) {
        if (item == null || item.cmd == null || item.cmd.trim().isEmpty()) {
            return;
        }
        List<Cmd> all = list(c);
        if (item.id == null || item.id.isEmpty()) {
            item.id = Long.toString(System.currentTimeMillis(), 36);
        }
        item.cmd = item.cmd.trim();
        item.name = clip(item.name);
        if (item.name.isEmpty()) {
            // [comment]，[comment]
            item.name = item.cmd.length() > MAX_NAME ? item.cmd.substring(0, MAX_NAME) : item.cmd;
        }
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(item.id)) {
                all.set(i, item);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(item);
        }
        save(c, all);
    }

    /** [comment] */
    public static void remove(Context c, String id) {
        List<Cmd> all = list(c);
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(id)) {
                all.remove(i);
                break;
            }
        }
        save(c, all);
    }

    private static void save(Context c, List<Cmd> all) {
        JSONArray arr = new JSONArray();
        try {
            for (Cmd x : all) {
                JSONObject o = new JSONObject();
                o.put("id", x.id);
                o.put("name", x.name);
                o.put("cmd", x.cmd);
                arr.put(o);
            }
        } catch (Throwable ignored) {
        }
        sp(c).edit().putString(KEY_LIST, arr.toString()).apply();
    }

    private static String clip(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ').trim();
        return t.length() > MAX_NAME ? t.substring(0, MAX_NAME) : t;
    }
}
