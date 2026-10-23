package com.wb.extrotator;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.RemoteViews;

/**
 * 「[comment]」[comment]（[comment] 2×2）—— [comment] / CPU [comment] / [comment] / [comment]，[comment]Yes[comment]。
 *
 * <p><b>[comment] ImageView</b>：[comment]"[comment]"（[comment] + [comment]）
 * [comment] RemoteViews [comment]，[comment] {@link WidgetStatus#draw} [comment]。
 * [comment] ImageView [comment] {@code fitXY}，[comment] dp [comment]，[comment]。
 *
 * <p><b>[comment]</b>：{@code updatePeriodMillis} [comment] 30 [comment]
 * （[comment]Yes 30 [comment]），[comment]、[comment]，[comment] {@link AlarmManager}
 * [comment]。⚠ [comment] Doze [comment]，[comment]——[comment]Yes[comment]。
 *
 * <p>⚠ [comment]<b>[comment]、[comment]</b>，[comment]<b>[comment]</b>[comment]
 * {@code setOnClickPendingIntent}。
 */
public class StatusCoverWidgetProvider extends AppWidgetProvider {

    /** [comment]"[comment]" */
    public static final String ACTION_TICK = "com.wb.extrotator.widget.STATUS_TICK";

    /** [comment]；[comment]，[comment] */
    private static final long PERIOD_MS = 60_000L;

    private static final int REQ_TICK = 0x3300;

    /** [comment]（dp）：[comment] 2×2 [comment] */
    private static final int BASE_W_DP = 150;
    private static final int BASE_H_DP = 133;

    /**
     * [comment]。
     *
     * <p>⚠ [comment]Yes[comment] binder [comment] 1MB [comment]：2×2（150×133dp）[comment]（340dpi ≈ 2.125）
     * Yes 319×283 [comment] ARGB ≈ 361KB，[comment]；[comment]，2.5 [comment] 375×333 ≈ 499KB。
     * （[comment]Yes 4×2，312×133dp [comment] 663×283 ≈ 750KB，[comment]，[comment] 1.5 [comment]。）
     */
    private static final float MAX_SCALE = 2.5f;

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            mgr.updateAppWidget(id, build(ctx, mgr, id));
        }
        schedule(ctx);
    }

    @Override
    public void onEnabled(Context ctx) {
        super.onEnabled(ctx);
        schedule(ctx);
    }

    @Override
    public void onDisabled(Context ctx) {
        super.onDisabled(ctx);
        cancel(ctx);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (ACTION_TICK.equals(intent.getAction())) {
            refreshAll(ctx);
        }
    }

    /** [comment]、[comment]。[comment]，[comment]，[comment]。 */
    static RemoteViews build(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_status_2x2);
        int wDp = BASE_W_DP;
        int hDp = BASE_H_DP;
        try {
            Bundle o = mgr.getAppWidgetOptions(id);
            if (o != null) {
                int mw = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
                int mh = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
                if (mw > 0) {
                    wDp = mw;
                }
                if (mh > 0) {
                    hDp = mh;
                }
            }
        } catch (Throwable ignored) {
        }
        float scale = Math.min(ctx.getResources().getDisplayMetrics().density, MAX_SCALE);
        int wPx = Math.max(64, Math.round(wDp * scale));
        int hPx = Math.max(32, Math.round(hDp * scale));
        v.setImageViewBitmap(R.id.statusImage, WidgetStatus.draw(wPx, hPx, WidgetStatus.sample(ctx)));
        return v;
    }

    /** [comment]（[comment]、[comment]） */
    public static void refreshAll(Context ctx) {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, StatusCoverWidgetProvider.class));
            if (ids == null || ids.length == 0) {
                return;
            }
            for (int id : ids) {
                mgr.updateAppWidget(id, build(ctx, mgr, id));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void schedule(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) {
                return;
            }
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + PERIOD_MS, PERIOD_MS, tick(ctx));
        } catch (Throwable ignored) {
        }
    }

    private static void cancel(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.cancel(tick(ctx));
            }
        } catch (Throwable ignored) {
        }
    }

    private static PendingIntent tick(Context ctx) {
        Intent i = new Intent(ctx, StatusCoverWidgetProvider.class).setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(ctx, REQ_TICK, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
