package com.wb.extrotator;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * 「Custom[comment]」[comment]（[comment] 2×2）—— [comment]，[comment]，[comment]。
 *
 * <p><b>[comment] {@code android:configure}</b>：[comment]Yes「[comment]Yes[comment]，
 * [comment]Settings」。[comment]"[comment]"（{@code android:configure}）
 * [comment] —— [comment]Yes[comment]，Cancel[comment]。[comment]，[comment]
 * 「[comment]」。
 *
 * <p><b>[comment]</b>：
 * <ul>
 *   <li>[comment] → [comment] {@link WidgetActionActivity}（[comment] {@code EXTRA_KEY}）[comment]；</li>
 *   <li>[comment]Yes[comment] → [comment] {@link CustomCfgActivity} [comment]。</li>
 * </ul>
 *
 * <p>⚠ [comment] {@code android:id} [comment] {@link #build} [comment]；[comment]Yes[comment] RemoteViews
 * [comment]、[comment]「None[comment]」。
 *
 * <p>⚠ [comment]（[comment] {@code keyguard} + [comment] meta-data）。[comment]Yes
 * 「[comment]」，[comment]，[comment] receiver。
 */
public class CustomCoverWidgetProvider extends AppWidgetProvider {

    /** [comment] view id，[comment] */
    private static final int[] BTN = {
            R.id.btnCustom0, R.id.btnCustom1, R.id.btnCustom2, R.id.btnCustom3,
    };

    /** PendingIntent [comment] requestCode [comment]（[comment]） */
    private static final int REQ_RUN = 0x3100;
    private static final int REQ_CFG = 0x3200;

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        RemoteViews v = build(ctx);
        for (int id : ids) {
            mgr.updateAppWidget(id, v);
        }
    }

    /** [comment]Settings[comment]（[comment]Yes[comment] SharedPreferences，[comment] Shizuku、[comment]） */
    static RemoteViews build(Context ctx) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_custom_2x2);
        for (int i = 0; i < BTN.length; i++) {
            if (CustomWidgetCfg.slotAlive(ctx, i)) {
                v.setTextViewText(BTN[i], CustomWidgetCfg.label(ctx, i));
                v.setOnClickPendingIntent(BTN[i],
                        runClick(ctx, CustomWidgetCfg.slot(ctx, i), i));
            } else {
                v.setTextViewText(BTN[i], ctx.getString(R.string.widget_custom_empty));
                v.setOnClickPendingIntent(BTN[i], cfgClick(ctx, i));
            }
        }
        return v;
    }

    /** [comment]：[comment] key [comment] Activity [comment] */
    private static PendingIntent runClick(Context ctx, String key, int slot) {
        Intent i = new Intent(ctx, WidgetActionActivity.class)
                .setAction("com.wb.extrotator.WIDGET_CUSTOM_" + slot)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(WidgetActionActivity.EXTRA_KEY, key);
        return PendingIntent.getActivity(ctx, REQ_RUN + slot, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** [comment]：[comment] */
    private static PendingIntent cfgClick(Context ctx, int slot) {
        Intent i = new Intent(ctx, CustomCfgActivity.class)
                .setAction("com.wb.extrotator.WIDGET_CUSTOM_CFG_" + slot)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(CustomCfgActivity.EXTRA_SLOT, slot);
        return PendingIntent.getActivity(ctx, REQ_CFG + slot, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** [comment]（[comment]、[comment]） */
    public static void refreshAll(Context ctx) {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, CustomCoverWidgetProvider.class));
            if (ids == null || ids.length == 0) {
                return;
            }
            RemoteViews v = build(ctx);
            for (int id : ids) {
                mgr.updateAppWidget(id, v);
            }
        } catch (Throwable ignored) {
        }
    }
}
