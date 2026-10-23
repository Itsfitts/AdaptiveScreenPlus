package com.wb.extrotator;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Random;

/**
 * 「[comment]」[comment] —— [comment]，[comment]「[comment] + [comment]」，4×2。
 *
 * <p><b>[comment]</b>（[comment] {@link SlotConfig}）：
 * <ul>
 *   <li><b>[comment]</b>：[comment]Yes[comment]，[comment]；[comment]、[comment]。</li>
 *   <li><b>[comment]</b>：[comment]，[comment]；[comment]「Settings[comment]」。
 *       [comment]。</li>
 * </ul>
 *
 * <p><b>[comment]Yes[comment]</b>（[comment]「[comment]」[comment]）：[comment]
 * {@link AppIconLib} [comment] {@link SlotPool#APPS} [comment] + [comment]「[comment]」[comment]，
 * [comment] {@link SlotPool} [comment]。[comment]「[comment]」Yes[comment] —— [comment]
 * [comment] 7 [comment]Yes[comment]。
 *
 * <p><b>「[comment]」Yes[comment]</b>：RemoteViews [comment] API —— [comment]
 * [comment] {@code setScrollY} [comment]，[comment]Yes {@code ViewFlipper}（[comment]
 * {@code @RemoteView} [comment]，[comment] {@code setDisplayedChild} [comment]）。[comment]Yes[comment]
 * [comment] {@link #SLOTS_PER_REEL} [comment] child [comment] ViewFlipper，[comment] k [comment] child [comment] k [comment]，
 * [comment] {@link #TICK} [comment]，[comment] {@code anim/slot_roll_in|out.xml} [comment]。
 *
 * <p>[comment] {@link #STOP} [comment]（[comment]，[comment]）。[comment]Yes"[comment]"[comment]
 * —— [comment] i [comment]Yes {@code STOP[i]/TICK}，[comment] {@code fin[i]} [comment]
 * [comment]，[comment]Yes[comment] {@code fin[i]}，[comment]"[comment]"。
 *
 * <p>⚠⚠ <b>[comment]（[comment] slotResX），[comment]。</b>
 * [comment] {@code ViewAnimator.showOnly()} [comment]"[comment] child"Yes<b>None[comment]</b>
 * {@code startAnimation(mInAnimation)} [comment] —— [comment]。
 * [comment]"[comment]"＝[comment]：
 * [comment]"[comment]"[comment]Yes[comment]，[comment]"[comment]"[comment]
 * （[comment]，[comment]）。
 *
 * <p>⚠⚠ <b>[comment]"[comment]"[comment]</b>（[comment] 0 [comment]）：[comment] 3 [comment] × 8 [comment]，
 * [comment] 72px [comment] 10KB，[comment]Yes 250KB [comment] —— [comment]Yes[comment]，
 * [comment]；[comment] int，[comment]，[comment]。
 * [comment]：**[comment] setImageViewBitmap [comment]**。
 *
 * <p>⚠ <b>[comment]、[comment]</b>（[comment] PNG）；[comment]。
 * [comment]，[comment]Yes"[comment]"。
 *
 * <p>⚠ <b>[comment] SharedPreferences</b>（[comment] Shizuku、[comment]）：
 * [comment]，[comment]「None[comment]」。
 *
 * <p>⚠ {@code setDisplayedChild} [comment] API 31+。[comment]"[comment]、[comment]"
 * （[comment]），[comment]Yes[comment]，[comment]。
 */
public class SlotWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "SlotWidget";

    /** [comment]：[comment] = [comment] */
    static final String ACT_SPIN = "com.wb.extrotator.SLOT_SPIN";
    /** [comment]：[comment]「[comment] / [comment]」[comment] */
    static final String ACT_MODE = "com.wb.extrotator.SLOT_MODE";

    /** [comment] */
    static final int REELS = 3;
    /** [comment] child —— [comment] widget_slot.xml [comment] ViewFlipper [comment] child [comment] */
    static final int SLOTS_PER_REEL = 8;

    /**
     * [comment] child id：{@code ICON[[comment]][[comment]]}。
     *
     * ⚠ [comment] slotA0..slotC7 [comment]，[comment] ——
     * RemoteViews [comment]Yes**[comment] id [comment] View** [comment]，[comment]Yes"[comment]"。
     * [comment] id [comment]（R.id [comment]），[comment]"[comment]"Yes[comment]。
     */
    private static final int[][] ICON = {
            {R.id.slotA0, R.id.slotA1, R.id.slotA2, R.id.slotA3,
                    R.id.slotA4, R.id.slotA5, R.id.slotA6, R.id.slotA7},
            {R.id.slotB0, R.id.slotB1, R.id.slotB2, R.id.slotB3,
                    R.id.slotB4, R.id.slotB5, R.id.slotB6, R.id.slotB7},
            {R.id.slotC0, R.id.slotC1, R.id.slotC2, R.id.slotC3,
                    R.id.slotC4, R.id.slotC5, R.id.slotC6, R.id.slotC7},
    };

    /**
     * [comment]。
     *
     * ⚠ [comment]（anim/slot_roll_in|out.xml [comment] 100ms）[comment]**[comment]**：[comment]
     * [comment]，[comment]。[comment]，[comment] IPC [comment]
     * [comment] —— [comment]Yes"[comment]"[comment]。
     */
    private static final int TICK = 130;
    /** [comment]（[comment]）：[comment] 10、[comment] 14、[comment] 18 ⇒ [comment] 1.3 / 1.8 / 2.3 [comment]，[comment] 520ms */
    private static final int[] STOP = {10, 14, 18};

    /** 「[comment]」—— [comment]、[comment]、[comment] */
    private static final boolean[] HELD_ALL = {true, true, true};

    /** [comment]（[comment]）[comment] */
    private static final int PT_DOUBLE = 2;
    /** [comment]（[comment]）[comment] */
    private static final int PT_TRIPLE = 7;

    /** [comment] */
    private static final long HOLD_MS = 1800L;

    /** [comment] */
    private static final String SP = "slot_stats";

    /** [comment]（[comment]Yes"[comment]"，Yes[comment]） */
    private static final String[] NO_POOL = new String[0];

    private static final int[] LED = {
            R.drawable.widget_slot_led_0, R.drawable.widget_slot_led_1,
            R.drawable.widget_slot_led_2, R.drawable.widget_slot_led_3,
            R.drawable.widget_slot_led_4, R.drawable.widget_slot_led_5,
            R.drawable.widget_slot_led_6, R.drawable.widget_slot_led_7,
            R.drawable.widget_slot_led_8, R.drawable.widget_slot_led_9,
    };

    /** [comment] API [comment] API 31（S）[comment] */
    private static final boolean CAN_ROLL = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;

    private static final Random RND = new Random();

    /** [comment]。[comment]、[comment] */
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** [comment]：[comment]，No[comment] */
    private static Handler loop;
    /** [comment]，[comment] */
    private static BroadcastReceiver.PendingResult pending;
    /** [comment]：[comment] +1；[comment]/[comment] */
    private static int gen;
    /** [comment]，[comment]（[comment]Yes[comment]） */
    private static Bitmap winArt;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String act = intent.getAction();
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            super.onReceive(ctx, intent);
            return;
        }
        if (ACT_SPIN.equals(act)) {
            // Custom[comment]Yes「Settings[comment]」。[comment]（[comment]Yes[comment]），
            // [comment]Yes[comment] PendingIntent，[comment]。
            if (SlotConfig.mode(ctx) == SlotConfig.MODE_SPIN) {
                spin(ctx, getClass(), id, goAsync());
            } else {
                redraw(ctx, getClass(), id);
            }
            return;
        }
        if (ACT_MODE.equals(act)) {
            SlotConfig.toggleMode(ctx);
            redraw(ctx, getClass(), id);
            return;
        }
        super.onReceive(ctx, intent);
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        final Context app = ctx.getApplicationContext();
        final Class<?> cls = getClass();
        for (int id : ids) {
            try {
                mgr.updateAppWidget(id, make(app, cls, id, idle(app), HELD_ALL, null, false, true));
            } catch (Throwable ignored) {
            }
        }
        // [comment]（[comment]）[comment]Yes[comment]，[comment]，
        // [comment] —— No[comment]Yes[comment]。
        new Thread(new Runnable() {
            @Override
            public void run() {
                AppIconLib.ensure(app);
                refreshAll(app, SlotWidgetProvider.class, SlotCoverWidgetProvider.class);
            }
        }, "slot-warmup").start();
    }

    // ── [comment] ────────────────────────────────────────────────────────────

    /**
     * [comment]：**[comment]**（[comment]）。
     * [comment] 0/1/2 —— [comment]，[comment]。
     */
    private static int[] idle(Context ctx) {
        try {
            String s = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
                    .getString("last_fin", null);
            if (s != null) {
                String[] a = s.split(",");
                if (a.length == 3) {
                    return new int[]{Integer.parseInt(a[0]), Integer.parseInt(a[1]),
                            Integer.parseInt(a[2])};
                }
            }
        } catch (Throwable ignored) {
        }
        return new int[]{0, 1, 2};
    }

    /**
     * [comment]。**[comment]**（[comment]），
     * [comment]、[comment]，[comment]。
     */
    private static void spin(final Context ctx, final Class<?> cls, final int id,
                             BroadcastReceiver.PendingResult pr) {
        cancelPrev();
        pending = pr;
        final int my = gen;
        final Context app = ctx.getApplicationContext();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] pool = null;
                Bitmap[] tiles = null;
                try {
                    AppIconLib.ensure(app);                 // Text/Text，Text
                    pool = SlotPool.pick(app);              // Text，Text
                    if (pool.length >= 2) {
                        tiles = SlotPool.tiles(app, pool);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "TextFailed", t);
                }
                final String[] fp = pool;
                final Bitmap[] ft = tiles;
                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        if (my != gen) {
                            return;                         // Text
                        }
                        if (fp == null || fp.length < 2) {
                            redraw(app, cls, id);           // TextYesText（Text）
                            done();
                            return;
                        }
                        SlotPool.store(app, fp);            // Text：Text
                        roll(app, cls, id, fp, ft);
                    }
                });
            }
        }, "slot-prepare").start();
    }

    /** [comment]。[comment] int，[comment] 0 [comment]（[comment] ⚠⚠）。 */
    private static void roll(final Context ctx, final Class<?> cls, final int id,
                             final String[] pool, final Bitmap[] tiles) {
        if (loop != null) {
            loop.removeCallbacksAndMessages(null);
        }
        loop = new Handler(Looper.getMainLooper());
        final AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        final int my = gen;
        final int p = pool.length;

        // [comment]：[comment]
        final int[] fin = new int[REELS];
        for (int i = 0; i < REELS; i++) {
            fin[i] = RND.nextInt(p);
        }

        // [comment]**[comment]**[comment]：[comment]"[comment]"[comment]，
        // [comment]，[comment]"[comment]"。
        final int end = STOP[2] + 1;
        for (int f = 0; f <= end; f++) {
            final int frame = f;
            loop.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (my != gen) {
                        return;
                    }
                    if (frame >= end) {
                        settle(ctx, mgr, cls, id, pool, tiles, fin);
                        return;
                    }
                    int[] cur = new int[REELS];
                    boolean[] held = new boolean[REELS];
                    for (int i = 0; i < REELS; i++) {
                        if (frame > STOP[i]) {
                            // [comment]：[comment]，[comment]
                            cur[i] = fin[i];
                            held[i] = true;
                        } else if (frame == STOP[i]) {
                            // [comment]，[comment] fin[i]
                            cur[i] = fin[i];
                        } else {
                            // [comment]：[comment]，[comment]Yes[comment]
                            int back = STOP[i] - frame;
                            cur[i] = ((fin[i] - back) % p + p) % p;
                        }
                    }
                    // [comment]Yes[comment]，[comment]；chrome [comment] 0 [comment]
                    try {
                        mgr.updateAppWidget(id, make(ctx, cls, id, cur, held, null,
                                frame < 2, frame == 0, pool, tiles));
                    } catch (Throwable t) {
                        Log.w(TAG, "Text " + frame + " TextFailed", t);
                    }
                }
            }, (long) f * TICK);
        }
    }

    /** [comment]：[comment]、[comment]、[comment]、[comment]、[comment]，1.8 [comment] */
    private static void settle(final Context ctx, final AppWidgetManager mgr,
                               final Class<?> cls, final int id,
                               final String[] pool, final Bitmap[] tiles, final int[] fin) {
        final int best = sameCount(fin);
        final boolean[] hl = highlight(fin, best);

        SharedPreferences sp = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE);
        int gain = best == 3 ? PT_TRIPLE : best == 2 ? PT_DOUBLE : 0;
        sp.edit()
                .putInt("spins", sp.getInt("spins", 0) + 1)
                .putInt("dbl", sp.getInt("dbl", 0) + (best == 2 ? 1 : 0))
                .putInt("tri", sp.getInt("tri", 0) + (best == 3 ? 1 : 0))
                .putInt("score", sp.getInt("score", 0) + gain)
                // [comment]（[comment]、[comment]、[comment]）[comment]，[comment]
                .putString("last_fin", fin[0] + "," + fin[1] + "," + fin[2])
                .apply();

        // [comment]，[comment]"[comment]"[comment]：[comment] + [comment] + [comment]
        try {
            mgr.updateAppWidget(id, make(ctx, cls, id, fin, HELD_ALL, hl, false, true, pool, tiles));
        } catch (Throwable ignored) {
        }
        buzz(ctx, best);

        if (loop == null) {
            done();
            return;
        }
        loop.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    mgr.updateAppWidget(id,
                            make(ctx, cls, id, fin, HELD_ALL, null, false, true, pool, tiles));
                } catch (Throwable ignored) {
                }
                done();
            }
        }, HOLD_MS);
    }

    /** [comment]：[comment]、[comment]、[comment] +1 */
    private static void cancelPrev() {
        gen++;
        if (loop != null) {
            loop.removeCallbacksAndMessages(null);
        }
        if (pending != null) {
            try {
                pending.finish();
            } catch (Throwable ignored) {
            }
            pending = null;
        }
    }

    private static void done() {
        if (pending != null) {
            try {
                pending.finish();
            } catch (Throwable ignored) {
            }
            pending = null;
        }
    }

    /** [comment]（[comment]、[comment]Error[comment]） */
    private static void redraw(Context ctx, Class<?> cls, int id) {
        try {
            AppWidgetManager.getInstance(ctx)
                    .updateAppWidget(id, make(ctx, cls, id, idle(ctx), HELD_ALL, null, false, true));
        } catch (Throwable ignored) {
        }
    }

    // ── [comment] ──────────────────────────────────────────────────────────────

    static RemoteViews make(Context ctx, Class<?> cls, int id, int[] idx,
                            boolean[] held, boolean[] hl, boolean down, boolean chrome) {
        return make(ctx, cls, id, idx, held, hl, down, chrome, null, null);
    }

    /**
     * [comment]。
     *
     * @param idx    [comment]（[comment] = [comment]；[comment] = [comment]）
     * @param held   [comment]**[comment]**。null [comment]**[comment] false [comment]"[comment]"**
     *               （[comment] ⚠⚠）。[comment] slotResX [comment]，[comment]**[comment]
     *               setDisplayedChild** —— [comment] ⚠
     * @param hl     [comment]；null = [comment]（[comment]）
     * @param down   [comment]（[comment]）
     * @param chrome [comment]"[comment]"[comment]（[comment]、[comment]、[comment]、[comment]）。
     *               [comment] false：[comment]，[comment]
     * @param pool   [comment]；null = [comment]（[comment]、[comment]）
     * @param tiles  [comment]，[comment] pool [comment]；null = [comment]
     */
    static RemoteViews make(Context ctx, Class<?> cls, int id, int[] idx,
                            boolean[] held, boolean[] hl, boolean down, boolean chrome,
                            String[] pool, Bitmap[] tiles) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_slot);

        final boolean custom = SlotConfig.mode(ctx) == SlotConfig.MODE_BTN;
        // [comment] ViewFlipper [comment] child，[comment]，[comment]
        final boolean roll = !custom && CAN_ROLL;
        final boolean flat = !custom && !CAN_ROLL;
        // [comment] —— [comment]。
        // ⚠⚠ [comment]**[comment] held == null**：roll() [comment]Yes[comment]**[comment] false [comment]**（[comment]
        // "[comment]"），[comment]Yes null。[comment] == null [comment] rolling [comment] false ⇒ [comment]
        // ⇒ [comment]Yes[comment]，[comment] child [comment]（v466 [comment]"[comment]"[comment]Yes[comment]；
        // v465 Yes[comment] setTextViewText，[comment]，[comment]）。
        boolean anySpinning = true;
        if (held != null) {
            anySpinning = false;
            for (int i = 0; i < held.length; i++) {
                if (!held[i]) {
                    anySpinning = true;
                    break;
                }
            }
        }
        final boolean rolling = !custom && anySpinning;

        final String[] use = custom ? NO_POOL : (pool != null ? pool : SlotPool.pool(ctx));
        final int n = Math.min(use.length, SLOTS_PER_REEL);

        int[] rolls = {R.id.slotRollA, R.id.slotRollB, R.id.slotRollC};
        int[] btns = {R.id.slotBtnA, R.id.slotBtnB, R.id.slotBtnC};
        int[] ress = {R.id.slotResA, R.id.slotResB, R.id.slotResC};
        int[] glows = {R.id.slotGlowA, R.id.slotGlowB, R.id.slotGlowC};

        // [comment]。**[comment] setDisplayedChild [comment]** —— [comment]Yes[comment]
        // [comment]，"[comment]、[comment]"[comment]Yes[comment]（[comment]）。
        // [comment] + [comment] = [comment] 3×8 [comment]（[comment] 250KB），[comment]Yes[comment]，[comment] 0 [comment]；
        // [comment]，[comment]。[comment]（[comment]）[comment]。
        if (chrome && rolling && roll && n > 0) {
            Bitmap[] t = tiles != null ? tiles : SlotPool.tiles(ctx, use);
            for (int i = 0; i < REELS; i++) {
                for (int k = 0; k < n; k++) {
                    Bitmap b = k < t.length ? t[k] : null;
                    if (b != null) {
                        v.setImageViewBitmap(ICON[i][k], b);
                    }
                }
            }
        }

        for (int i = 0; i < REELS; i++) {
            final boolean h = held != null && held[i];
            final boolean spinning = roll && !h;            // Text
            final boolean landed = (roll && h) || flat;     // Text，Text
            final int k = n <= 0 ? 0 : Math.max(0, Math.min(idx[i], n - 1));

            // [comment]：[comment]Yes[comment]；[comment]Yes
            // "[comment] inflate"，[comment] XML Default[comment]（[comment] 0 [comment]）。
            v.setViewVisibility(rolls[i], spinning ? View.VISIBLE : View.GONE);
            v.setViewVisibility(btns[i], custom ? View.VISIBLE : View.GONE);
            v.setViewVisibility(ress[i], landed && n > 0 ? View.VISIBLE : View.GONE);

            if (spinning) {
                v.setDisplayedChild(rolls[i], idx[i]);
            }
            if (landed && n > 0) {
                // [comment]Yes[comment]，[comment]Yes[comment]
                Bitmap b = tileAt(ctx, use, tiles, k);
                if (b != null) {
                    v.setImageViewBitmap(ress[i], b);
                }
            }
            if (custom) {
                v.setTextViewText(btns[i], SlotConfig.label(ctx, i));
            }
            if (chrome) {
                // [comment]；Custom[comment]
                v.setViewVisibility(glows[i],
                        (!custom && hl != null && hl[i]) ? View.VISIBLE : View.GONE);
            }
        }

        // [comment]：[comment]，[comment] 0 [comment]（[comment] 0，[comment]）
        int score = score(ctx);
        v.setImageViewResource(R.id.slotLedH,
                score >= 100 ? LED[score / 100 % 10] : R.drawable.widget_slot_led_blank);
        v.setImageViewResource(R.id.slotLedT,
                score >= 10 ? LED[score / 10 % 10] : R.drawable.widget_slot_led_blank);
        v.setImageViewResource(R.id.slotLedO, LED[score % 10]);

        v.setTextViewText(R.id.slotFoot, footNow(ctx));

        v.setImageViewResource(R.id.slotLever,
                down ? R.drawable.widget_slot_lever_on : R.drawable.widget_slot_lever);

        if (!chrome) {
            // [comment]：[comment]，[comment] PendingIntent Yes Parcelable，
            // [comment]，[comment] int [comment]。
            return v;
        }

        v.setOnClickPendingIntent(R.id.slotLever, leverIntent(ctx, cls, id, custom));
        v.setOnClickPendingIntent(R.id.slotScoreBox, modeIntent(ctx, cls, id));

        if (custom) {
            for (int i = 0; i < REELS; i++) {
                if (!SlotConfig.slotAlive(ctx, i)) {
                    continue;   // Text / Text：Text，Text
                }
                Intent fi = new Intent(ctx, QuickActionReceiver.class)
                        .setAction(QuickActions.ACTION_RUN)
                        .putExtra(QuickActions.EXTRA_ACTION, SlotConfig.slot(ctx, i));
                v.setOnClickPendingIntent(btns[i], PendingIntent.getBroadcast(ctx,
                        id * 16 + 10 + i, fi,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }
        }

        // [comment]：child 1 Yes[comment]、child 0 Yes[comment]。[comment] anim/slot_win_in。
        // ⚠ [comment]：[comment] child 1，[comment]。[comment]
        // [comment]，[comment]（v463 [comment]Yes[comment]）。
        if (hl != null) {
            v.setImageViewBitmap(R.id.slotWinArt, art(ctx));
            v.setDisplayedChild(R.id.slotWinFlip, 1);
        } else {
            v.setDisplayedChild(R.id.slotWinFlip, 0);
        }
        return v;
    }

    /** [comment] k [comment]：[comment]，[comment] */
    private static Bitmap tileAt(Context ctx, String[] pool, Bitmap[] tiles, int k) {
        if (tiles != null && k < tiles.length && tiles[k] != null) {
            return tiles[k];
        }
        if (k >= pool.length) {
            return null;
        }
        return SlotPool.tile(ctx, pool[k]);
    }

    /** [comment]：[comment]，Custom[comment] */
    private static PendingIntent leverIntent(Context ctx, Class<?> cls, int id, boolean custom) {
        int rc = id * 16 + 1;
        if (custom) {
            return PendingIntent.getActivity(ctx, rc,
                    new Intent(ctx, SlotConfigActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }
        Intent i = new Intent(ctx, cls)
                .setAction(ACT_SPIN)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        return PendingIntent.getBroadcast(ctx, rc, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** [comment]：[comment] */
    private static PendingIntent modeIntent(Context ctx, Class<?> cls, int id) {
        Intent i = new Intent(ctx, cls)
                .setAction(ACT_MODE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        return PendingIntent.getBroadcast(ctx, id * 16 + 2, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** [comment]：[comment]Notice[comment]；Custom[comment]「[comment]」 */
    private static String footNow(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE);
        if (SlotConfig.mode(ctx) == SlotConfig.MODE_BTN) {
            String m = sp.getString("last_msg", null);
            if (m != null && !m.isEmpty()) {
                return m;
            }
            return ctx.getString(R.string.widget_slot_btn_hint);
        }
        int n = sp.getInt("spins", 0);
        if (n == 0) {
            return ctx.getString(R.string.widget_slot_ready);
        }
        return ctx.getString(R.string.widget_slot_stats,
                n, sp.getInt("dbl", 0), sp.getInt("tri", 0));
    }

    /** [comment] —— [comment] +2、[comment] +7 */
    static int score(Context ctx) {
        try {
            return ctx.getSharedPreferences(SP, Context.MODE_PRIVATE).getInt("score", 0);
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * [comment]「[comment]！」。
     *
     * <p>[comment] Bitmap：RemoteViews [comment]（[comment] Paint，
     * [comment] setTextViewText [comment]）。[comment] —— [comment]、[comment]、
     * [comment] —— 「[comment]」[comment]。
     *
     * <p>⭐ <b>[comment] 1/3，[comment]（400×120）[comment]</b>：[comment]Yes[comment]
     * {@code fitEnd} [comment]，[comment]，[comment]Yes[comment]Yes
     * [comment] 1/3 —— [comment]，[comment]（[comment]）。
     * [comment] 1/3 [comment]，[comment]。
     *
     * <p>[comment]Yes[comment]，[comment]。⚠ [comment]：[comment]Yes[comment]，
     * [comment] 400×120 [comment]（[comment] 190KB）[comment]"[comment]"[comment]。
     */
    private static Bitmap art(Context ctx) {
        if (winArt != null) {
            return winArt;
        }
        int w = 400;
        int h = 120;
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextSize(25f);
        p.setTextAlign(Paint.Align.CENTER);
        String s = ctx.getString(R.string.widget_slot_win);
        float cx = w / 2f;
        float cy = h / 2f - (p.descent() + p.ascent()) / 2f;

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4.3f);
        p.setColor(0xFF7C0F13);
        c.drawText(s, cx, cy, p);

        p.setStrokeWidth(1.7f);
        p.setColor(0xFFFFF4C8);
        c.drawText(s, cx, cy, p);

        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, cy - 14, 0, cy + 10,
                0xFFFFF8DC, 0xFFFFAE00, Shader.TileMode.CLAMP));
        c.drawText(s, cx, cy, p);
        p.setShader(null);

        winArt = bm;
        return bm;
    }

    // ── [comment] ────────────────────────────────────────────────────────────

    /** 3 = [comment]，2 = [comment]，1 = [comment] */
    private static int sameCount(int[] s) {
        if (s[0] == s[1] && s[1] == s[2]) {
            return 3;
        }
        if (s[0] == s[1] || s[1] == s[2] || s[0] == s[2]) {
            return 2;
        }
        return 1;
    }

    /** [comment] —— [comment]Yes[comment]/[comment]；[comment] null（[comment]） */
    private static boolean[] highlight(int[] s, int best) {
        if (best < 2) {
            return null;
        }
        boolean[] h = new boolean[3];
        if (best == 3) {
            h[0] = h[1] = h[2] = true;
        } else if (s[0] == s[1]) {
            h[0] = h[1] = true;
        } else if (s[1] == s[2]) {
            h[1] = h[2] = true;
        } else {
            h[0] = h[2] = true;
        }
        return h;
    }

    /** [comment]：[comment]，[comment] */
    private static void buzz(Context ctx, int best) {
        if (best < 2) {
            return;
        }
        try {
            android.os.Vibrator v =
                    (android.os.Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) {
                return;
            }
            if (best == 3) {
                v.vibrate(android.os.VibrationEffect.createWaveform(
                        new long[]{0, 60, 90, 60, 90, 170}, -1));
            } else {
                v.vibrate(android.os.VibrationEffect.createOneShot(
                        45, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Custom[comment]，[comment]，6 [comment]。
     *
     * <p>[comment]：[comment] / [comment]，[comment]None[comment]，
     * [comment]"[comment]"。[comment] —— [comment] Toast
     * （[comment] Toast [comment] Android 11 [comment]，[comment]）。
     *
     * <p>⚠ [comment] Handler [comment]：[comment] Runnable [comment]，
     * [comment]（[comment]）。
     */
    static void noteResult(Context ctx, String msg) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        try {
            final Context app = ctx.getApplicationContext();
            app.getSharedPreferences(SP, Context.MODE_PRIVATE).edit()
                    .putString("last_msg", msg)
                    .apply();
            refreshAll(app, SlotWidgetProvider.class, SlotCoverWidgetProvider.class);
            POST.removeCallbacksAndMessages(null);
            POST.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        app.getSharedPreferences(SP, Context.MODE_PRIVATE).edit()
                                .remove("last_msg").apply();
                        refreshAll(app, SlotWidgetProvider.class, SlotCoverWidgetProvider.class);
                    } catch (Throwable ignored) {
                    }
                }
            }, 6000L);
        } catch (Throwable ignored) {
        }
    }

    /** [comment]（[comment]） */
    private static final Handler POST = new Handler(Looper.getMainLooper());

    /** [comment]（[comment] / [comment] / [comment]Update[comment]）—— [comment] */
    static void refreshAll(Context ctx, Class<?>... classes) {
        for (Class<?> cls : classes) {
            try {
                AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
                int[] ids = mgr.getAppWidgetIds(new android.content.ComponentName(ctx, cls));
                if (ids == null) {
                    continue;
                }
                for (int id : ids) {
                    mgr.updateAppWidget(id, make(ctx, cls, id, idle(ctx), HELD_ALL, null, false, true));
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
