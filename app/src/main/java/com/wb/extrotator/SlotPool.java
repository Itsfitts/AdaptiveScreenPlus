package com.wb.extrotator;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 * [comment]「[comment]」。
 *
 * <p>[comment] = [comment]。[comment] {@link #SIZE} [comment]：[comment] {@link AppIconLib} [comment]
 * {@link #APPS} [comment]，[comment]「[comment]」（{@link #SEVEN}，[comment]
 * {@code drawable/widget_slot_seven.xml}）[comment] —— [comment]。
 *
 * <p><b>[comment]</b>：[comment] PNG Yes[comment]，[comment]
 * {@link Bitmap.Config#RGB_565}（[comment] ARGB [comment]）[comment] alpha，[comment]；
 * [comment]，[comment]Yes[comment]。
 * [comment]（[comment] 42dp、[comment]），[comment]，[comment]。
 *
 * <p>⚠ [comment]**[comment]**：[comment]，[comment]、[comment]、[comment]
 * [comment]。[comment]Yes[comment]（[comment] = [comment]「[comment]」）。
 */
final class SlotPool {

    /** [comment] */
    static final int APPS = 7;
    /** [comment]：7 [comment] + [comment]「[comment]」 */
    static final int SIZE = APPS + 1;
    /** [comment]（px），[comment] AppIconLib.PX / [comment] 42dp [comment] */
    static final int TILE_PX = 72;
    /** [comment]（= drawable/widget_slot_cell [comment] solid）。[comment] drawable [comment]。 */
    private static final int CELL_BG = 0xFF2C2E33;

    private static final String SP = "slot_pool";
    private static final String K_POOL = "pool";

    private static final Random RND = new Random();

    /** [comment]。[comment] = [comment] / {@link #SEVEN}。[comment]，[comment]。 */
    private static final LruCache<String, Bitmap> TILES =
            new LruCache<String, Bitmap>(4 * 1024 * 1024) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    /** 「[comment]」[comment]（[comment]） */
    static final String SEVEN = "@seven";

    private static final String[] EMPTY = new String[0];

    private SlotPool() {
    }

    /** [comment]；[comment] */
    static String[] pool(Context ctx) {
        String[] p = decode(prefs(ctx).getString(K_POOL, ""));
        if (p.length >= 2) {
            return p;
        }
        p = pick(ctx);
        store(ctx, p);
        return p;
    }

    /**
     * [comment]。**[comment]**（[comment]，[comment]），[comment]。
     * [comment] —— [comment]，[comment]。
     */
    static String[] pick(Context ctx) {
        String[] all = AppIconLib.list(ctx);
        if (all.length == 0) {
            return EMPTY;
        }
        ArrayList<String> c = new ArrayList<>(Arrays.asList(all));
        Collections.shuffle(c, RND);
        int n = Math.min(APPS, c.size());
        String[] p = new String[n + 1];
        for (int i = 0; i < n; i++) {
            p[i] = c.get(i);
        }
        p[n] = null;                       // 「Text」
        // [comment]「[comment]」[comment] —— [comment]，"[comment]"[comment]
        int k = RND.nextInt(p.length);
        String t = p[k];
        p[k] = p[p.length - 1];
        p[p.length - 1] = t;
        return p;
    }

    static void store(Context ctx, String[] pool) {
        try {
            prefs(ctx).edit().putString(K_POOL, encode(pool)).apply();
        } catch (Throwable ignored) {
        }
    }

    /** [comment]；[comment]Yes null（[comment]，[comment]） */
    static Bitmap[] tiles(Context ctx, String[] pool) {
        Bitmap[] out = new Bitmap[pool.length];
        for (int i = 0; i < pool.length; i++) {
            out[i] = tile(ctx, pool[i]);
        }
        return out;
    }

    /** [comment]。[comment] —— [comment]。 */
    static Bitmap tile(Context ctx, String item) {
        String key = item == null ? SEVEN : item;
        Bitmap hit = TILES.get(key);
        if (hit != null) {
            return hit;
        }
        Bitmap out = null;
        try {
            out = Bitmap.createBitmap(TILE_PX, TILE_PX, Bitmap.Config.RGB_565);
            Canvas c = new Canvas(out);
            c.drawColor(CELL_BG);
            if (item == null) {
                Drawable d = ctx.getResources().getDrawable(R.drawable.widget_slot_seven, null);
                if (d != null) {
                    d.setBounds(0, 0, TILE_PX, TILE_PX);
                    d.draw(c);
                }
            } else {
                Bitmap src = AppIconLib.icon(ctx, item);
                if (src == null) {
                    return null;
                }
                // [comment]Yes TILE_PX [comment]，[comment]Yes 1:1；[comment]Yes[comment]，[comment]
                float s = TILE_PX / (float) src.getWidth();
                float dy = (TILE_PX - src.getHeight() * s) / 2f;
                c.save();
                c.scale(s, s);
                c.drawBitmap(src, 0, dy / s, new Paint(Paint.FILTER_BITMAP_FLAG));
                c.restore();
                src.recycle();
            }
        } catch (Throwable t) {
            return null;
        }
        TILES.put(key, out);
        return out;
    }

    // ── [comment] ────────────────────────────────────────────────────────
    // [comment] \n [comment]，「[comment]」[comment] —— [comment]。

    private static String encode(String[] p) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < p.length; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            if (p[i] != null) {
                sb.append(p[i]);
            }
        }
        return sb.toString();
    }

    private static String[] decode(String s) {
        if (s == null || s.isEmpty()) {
            return EMPTY;
        }
        String[] raw = s.split("\n", -1);
        String[] out = new String[raw.length];
        for (int i = 0; i < raw.length; i++) {
            out[i] = raw[i].isEmpty() ? null : raw[i];
        }
        return out;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(SP, Context.MODE_PRIVATE);
    }
}
