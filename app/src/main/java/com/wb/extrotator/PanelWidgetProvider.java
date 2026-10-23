package com.wb.extrotator;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * 「[comment]」[comment]，[comment]：
 * <ul>
 *   <li>[comment] 2×1 [comment]（{@code widget_panel.xml}）：[comment]/[comment] · [comment]；</li>
 *   <li>[comment] 2×2 [comment]（{@code widget_panel_2x2.xml}）：[comment]/[comment] · [comment]
 *       · [comment] · [comment]。</li>
 * </ul>
 *
 * <p>「[comment]」[comment]Yes[comment] {@link Prefs} [comment]（[comment] SharedPreferences，
 * [comment] Shizuku、[comment]），[comment]Settings[comment] {@link #updateAll} [comment]。
 *
 * <p>[comment] {@link WidgetActionActivity}（[comment] Activity，[comment] Toast、[comment] Shizuku）。
 */
public class PanelWidgetProvider extends AppWidgetProvider {

    private static final int REQ_FOLD = 0x2001;
    private static final int REQ_DUAL = 0x2002;
    private static final int REQ_SYNC = 0x2003;
    private static final int REQ_CAST = 0x2004;

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        // getClass() [comment]，[comment] 2×2 [comment]
        RemoteViews v = build(ctx, layoutFor(getClass()));
        for (int id : ids) {
            mgr.updateAppWidget(id, v);
        }
    }

    /** [comment]Yes[comment]，[comment] */
    static int layoutFor(Class<?> cls) {
        return PanelCoverWidgetProvider.class.isAssignableFrom(cls)
                ? R.layout.widget_panel_2x2
                : R.layout.widget_panel;
    }

    static RemoteViews build(Context ctx, int layoutId) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), layoutId);
        v.setOnClickPendingIntent(R.id.btnWidgetFold,
                click(ctx, WidgetActionActivity.ACT_FOLD, REQ_FOLD));
        v.setOnClickPendingIntent(R.id.btnWidgetDual,
                click(ctx, WidgetActionActivity.ACT_DUAL, REQ_DUAL));

        if (layoutId == R.layout.widget_panel_2x2) {
            // [comment]：[comment]Yes[comment]（[comment]），
            // [comment]Yes[comment]，[comment]
            Prefs p = new Prefs(ctx);
            v.setOnClickPendingIntent(R.id.btnWidgetSync,
                    click(ctx, WidgetActionActivity.ACT_SYNC, REQ_SYNC));
            v.setOnClickPendingIntent(R.id.btnWidgetCast,
                    click(ctx, WidgetActionActivity.ACT_CAST, REQ_CAST));
            bindToggle(v, R.id.btnWidgetSync, R.drawable.widget_btn_sync,
                    ctx.getString(R.string.widget_btn_sync), p.isEnabled());
        }
        return v;
    }

    /**
     * [comment]「[comment] + [comment]/[comment]」[comment]，[comment]。
     * @param onBg Enable[comment]；Disable[comment]（widget_btn_off）
     */
    private static void bindToggle(RemoteViews v, int id, int onBg, String name, boolean on) {
        v.setTextViewText(id, name + "\n" + (on ? "Text" : "Text"));
        v.setInt(id, "setBackgroundResource", on ? onBg : R.drawable.widget_btn_off);
    }

    private static PendingIntent click(Context ctx, String action, int reqCode) {
        Intent i = new Intent(ctx, WidgetActionActivity.class)
                .setAction("com.wb.extrotator.WIDGET_" + action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(WidgetActionActivity.EXTRA_ACTION, action);
        return PendingIntent.getActivity(ctx, reqCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** [comment]（[comment] / [comment] / [comment]）—— [comment] */
    public static void updateAll(Context ctx) {
        refresh(ctx, PanelWidgetProvider.class);
        refresh(ctx, PanelCoverWidgetProvider.class);
    }

    private static void refresh(Context ctx, Class<?> cls) {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, cls));
            if (ids == null || ids.length == 0) {
                return;
            }
            RemoteViews v = build(ctx, layoutFor(cls));
            for (int id : ids) {
                mgr.updateAppWidget(id, v);
            }
        } catch (Throwable ignored) {
        }
    }
}
