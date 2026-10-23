package com.wb.extrotator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 「[comment]」[comment]：[comment] {@code .lrc} [comment]，
 * [comment] LRCLIB [comment]。
 *
 * <p><b>[comment]</b>：[comment]。[comment] App [comment]，
 * Android [comment] 11 [comment]，[comment]（[comment]「[comment]」[comment]）。
 * [comment]、[comment]、[comment]、[comment] —— [comment]。
 * [comment] LRCLIB [comment]：[comment]、[comment]、[comment] key、[comment] {@code syncedLyrics}
 * （[comment]Yes[comment]），[comment]。
 *
 * <p>⚠ [comment]Yes[comment]：[comment]、[comment]、[comment]（"[comment] (Live)"）[comment]。
 * [comment] {@link #fetchBlocking} [comment] {@link #EMPTY}，[comment]，
 * <b>[comment]、[comment]</b> —— [comment]Yes[comment]。
 */
public final class Lyric {

    /** [comment] */
    public static final class Line {
        /** [comment] */
        public final long at;
        public final String text;

        Line(long at, String text) {
            this.at = at;
            this.text = text;
        }
    }

    /** [comment] */
    public final List<Line> lines;

    private Lyric(List<Line> lines) {
        this.lines = lines;
    }

    /** [comment] */
    public static final Lyric EMPTY = new Lyric(new ArrayList<Line>());

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /**
     * [comment] {@code ms} [comment]；[comment] -1。
     *
     * <p>[comment]：[comment] LRC [comment]，[comment]Yes 0.5 [comment]。
     */
    public int indexAt(long ms) {
        int hit = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).at <= ms) {
                hit = i;
            } else {
                break;
            }
        }
        return hit;
    }

    // ============================================================ [comment]

    /** {@code [mm:ss.xx]} [comment] {@code [mm:ss.xxx]}，[comment]（[comment]） */
    private static final Pattern TAG = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]");

    /**
     * [comment] LRC [comment]。[comment]（LRC [comment] {@code [ar:]} {@code [ti:]} [comment]）。
     */
    public static Lyric parse(String lrc) {
        List<Line> out = new ArrayList<>();
        if (lrc == null) {
            return EMPTY;
        }
        for (String raw : lrc.split("\n")) {
            String s = raw.trim();
            if (s.isEmpty()) {
                continue;
            }
            Matcher m = TAG.matcher(s);
            List<Long> times = new ArrayList<>();
            int end = 0;
            while (m.find()) {
                long mm = Long.parseLong(m.group(1));
                long ss = Long.parseLong(m.group(2));
                long frac = 0;
                if (m.group(3) != null) {
                    String f = m.group(3);
                    // [comment]Yes[comment]、[comment]Yes[comment] —— [comment]
                    frac = f.length() >= 3 ? Long.parseLong(f.substring(0, 3))
                            : Long.parseLong(f) * 10;
                }
                times.add(mm * 60000L + ss * 1000L + frac);
                end = m.end();
            }
            if (times.isEmpty()) {
                continue;
            }
            String text = s.substring(end).trim();
            if (text.isEmpty()) {
                continue;
            }
            for (long t : times) {
                out.add(new Line(t, text));
            }
        }
        Collections.sort(out, new Comparator<Line>() {
            @Override
            public int compare(Line a, Line b) {
                return Long.compare(a.at, b.at);
            }
        });
        return out.isEmpty() ? EMPTY : new Lyric(out);
    }

    // ============================================================ [comment]

    /**
     * [comment] LRCLIB [comment] + [comment]。<b>[comment]</b>，[comment]。
     *
     * @return [comment]Yes[comment]；[comment] {@link #EMPTY}（[comment] null）
     */
    public static Lyric fetchBlocking(String title, String artist) {
        if (title == null || title.trim().isEmpty()) {
            return EMPTY;
        }
        String t = title.trim();
        String a = artist == null ? "" : artist.trim();
        // [comment]"[comment]"[comment]（LRCLIB [comment] get [comment]Yes[comment]）
        String lrc = get("https://lrclib.net/api/get?track_name=" + enc(t)
                + (a.isEmpty() ? "" : "&artist_name=" + enc(a)));
        Lyric parsed = parse(lrc);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        // [comment]，[comment]
        String json = get("https://lrclib.net/api/search?track_name=" + enc(t)
                + (a.isEmpty() ? "" : "&artist_name=" + enc(a)));
        return parse(firstSynced(json));
    }

    /** [comment] {@code /api/get} [comment] syncedLyrics */
    private static String firstSynced(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            if (json.trim().startsWith("[")) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) {
                        continue;
                    }
                    String s = o.optString("syncedLyrics", "");
                    if (!s.isEmpty()) {
                        return s;
                    }
                }
                return null;
            }
            return new JSONObject(json).optString("syncedLyrics", null);
        } catch (Throwable t) {
            return null;
        }
    }

    /** [comment] GET，[comment]；[comment] 200 [comment] null（[comment]YesError） */
    private static String get(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            c.setRequestProperty("User-Agent", "AdaptiveScreen/1.0 (Android)");
            c.setRequestProperty("Accept", "application/json");
            if (c.getResponseCode() != 200) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            r.close();
            return sb.toString();
        } catch (Throwable t) {
            return null;
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Throwable t) {
            return "";
        }
    }
}
