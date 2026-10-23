package com.wb.extrotator;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * 「[comment]」[comment] —— [comment]。
 *
 * <p><b>[comment]</b>：[comment]（[comment] {@code widget_launcher_cover.xml}
 * [comment] {@code widget_panel_samsung.xml} [comment]）—— [comment] xml [comment] {@code widgetCategory} [comment]
 * {@code keyguard}、[comment] <b>352×339dp / targetCell 4×4</b>；[comment] meta-data
 * {@code com.samsung.android.appwidget.provider}，[comment]、[comment] {@code sub_screen}
 * [comment] xml。[comment]Yes[comment]「[comment]」[comment] APK [comment]，[comment]Yes[comment]。
 *
 * <p><b>[comment]</b>：[comment] 54dp [comment]（[comment]<b>[comment]</b>，[comment] ——
 * [comment] / [comment] / [comment] / Settings），[comment]Yes[comment]。[comment]Yes[comment]：[comment]，
 * [comment]「[comment]」。
 *
 * <p><b>[comment] GridView + RemoteViewsService</b>：[comment] setAdapter
 * （[comment]，[comment] RemoteViews [comment]），[comment] {@link CoverLauncherService}
 * [comment] position [comment]。[comment]：{@code setPendingIntentTemplate} [comment]Yes[comment]，
 * [comment] {@code setOnClickFillInIntent} [comment]，[comment] {@link LauncherTapReceiver}
 * [comment]。
 *
 * <p><b>[comment]Settings[comment] RemoteViews [comment]</b>：RemoteViews [comment] API，[comment]，
 * [comment]Yes[comment] —— [comment]（{@link #layoutFor}）；[comment] / [comment] / [comment]
 * [comment] {@link CoverLauncherService} [comment]、[comment]；[comment] shape [comment]
 * ImageView（[comment] {@link #build}）。
 */
public class CoverLauncherProvider extends AppWidgetProvider {

    private static final String TAG = "CoverLauncher";

    /** [comment] */
    public static final String ACTION_TAP = "com.wb.extrotator.action.LAUNCHER_TAP";
    /** [comment]，[comment] */
    public static final String ACTION_REFRESH = "com.wb.extrotator.action.LAUNCHER_REFRESH";

    private static final int REQ_TEMPLATE = 0x3101;
    private static final int REQ_CONFIG = 0x3102;
    private static final int REQ_SEARCH = 0x3103;
    private static final int REQ_SORT = 0x3104;
    private static final int REQ_FAV = 0x3105;

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        RemoteViews v = build(ctx);
        for (int id : ids) {
            mgr.updateAppWidget(id, v);
        }
        notifyData(mgr, ids);
        Log.i(TAG, "onUpdate Text " + ids.length + " Text");
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (intent != null && ACTION_REFRESH.equals(intent.getAction())) {
            refreshAll(ctx);
        }
    }

    /**
     * [comment]「[comment]」[comment] —— [comment]，[comment] GridView [comment] numColumns [comment]
     * （[comment]Yes[comment] numColumns，[comment] {@code widget_launcher.xml} [comment]）。
     *
     * <p>⚠ [comment] {@link LauncherPrefs#COLUMN_CHOICES} [comment]，
     * [comment] {@code scripts/gen_launcher_column_layouts.py} [comment]。
     */
    static int layoutFor(Context ctx) {
        switch (LauncherPrefs.columns(ctx)) {
            case 7:
                return R.layout.widget_launcher_c7;
            case 6:
                return R.layout.widget_launcher_c6;
            case 5:
                return R.layout.widget_launcher_c5;
            default:
                return R.layout.widget_launcher;
        }
    }

    /**
     * [comment] —— [comment]。
     *
     * <p>[comment] {@link LauncherPrefs#sortLabel(Context, String)}：
     * [comment]、[comment]、[comment] Toast [comment] {@link LauncherPrefs#SORT_ALL}，
     * [comment]"[comment]"[comment]。
     */
    public static String sortLabel(Context ctx) {
        return LauncherPrefs.sortLabel(ctx);
    }

    /** [comment]（[comment]，[comment]） */
    static RemoteViews build(Context ctx) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), layoutFor(ctx));

        /*
         * [comment]：[comment]，[comment]Yes[comment] —— RemoteViews [comment]
         * （setColorFilter [comment] setter，[comment]）。[comment] shape
         * （launcher_widget_bg_0/10/30/50），[comment]Settings[comment] ImageView。
         * （[comment]Yes ImageView [comment]Yes[comment] background，[comment]。）
         */
        v.setImageViewResource(R.id.launcherBg, LauncherPrefs.bgDrawable(ctx));

        /*
         * [comment]：[comment]，[comment]（[comment]）。
         *
         * [comment]Yes**[comment]**[comment]Yes[comment]：748×720 [comment] ARGB [comment] 2MB，[comment] RemoteViews [comment] Binder
         * [comment]Yes 1MB，setImageViewBitmap [comment]。[comment] setImageViewUri，
         * [comment] —— [comment]Yes PhotoProvider。
         * [comment]，[comment] ImageView [comment]，[comment]。
         */
        if (LauncherPhoto.has(ctx)) {
            v.setImageViewUri(R.id.launcherPhoto, PhotoProvider.uri());
        }

        // [comment]：[comment] RemoteViewsService
        v.setRemoteAdapter(R.id.launcherGrid, new Intent(ctx, CoverLauncherService.class));
        // [comment]
        v.setEmptyView(R.id.launcherGrid, R.id.launcherEmpty);

        // [comment]：[comment]
        v.setTextViewText(R.id.launcherBarTitle, LauncherPrefs.favOnly(ctx)
                ? ctx.getString(R.string.launcher_fav_bar)
                : ctx.getString(R.string.launcher_sort_now, sortLabel(ctx)));

        // [comment]：[comment]"[comment]"[comment]，[comment]
        v.setImageViewResource(R.id.launcherSort,
                LauncherPrefs.SORT_MANUAL.equals(LauncherPrefs.sort(ctx))
                        ? R.drawable.ic_launcher_sort : R.drawable.ic_launcher_sort_on);

        // [comment]：[comment]「[comment]」[comment]
        v.setImageViewResource(R.id.launcherFav,
                LauncherPrefs.favOnly(ctx)
                        ? R.drawable.ic_launcher_star_on : R.drawable.ic_launcher_star);

        /*
         * [comment]Yes[comment]，[comment]Yes getActivity —— [comment]
         * **[comment]**[comment]（[comment] startActivity [comment]Default[comment]），[comment]
         * LauncherTapReceiver [comment] shell [comment] am start --display [comment]。
         */
        v.setOnClickPendingIntent(R.id.launcherSearch,
                broadcast(ctx, REQ_SEARCH, LauncherTapReceiver.ACTION_OPEN_SEARCH));
        v.setOnClickPendingIntent(R.id.launcherSort,
                broadcast(ctx, REQ_SORT, LauncherTapReceiver.ACTION_SORT));
        v.setOnClickPendingIntent(R.id.launcherConfig,
                broadcast(ctx, REQ_CONFIG, LauncherTapReceiver.ACTION_OPEN_CONFIG));
        // [comment]：[comment]「[comment]」。[comment] Activity —— [comment]
        v.setOnClickPendingIntent(R.id.launcherFav,
                broadcast(ctx, REQ_FAV, LauncherTapReceiver.ACTION_FAV_FILTER));

        // [comment]"[comment]"。[comment]Yes MUTABLE —— [comment]
        // fillInIntent（[comment]）[comment]，IMMUTABLE [comment]，[comment]。
        Intent tap = new Intent(ctx, LauncherTapReceiver.class).setAction(ACTION_TAP);
        v.setPendingIntentTemplate(R.id.launcherGrid, PendingIntent.getBroadcast(
                ctx, REQ_TEMPLATE, tap,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE));

        return v;
    }

    /** [comment] IMMUTABLE：[comment] action [comment]，[comment]。 */
    private static PendingIntent broadcast(Context ctx, int req, String action) {
        Intent i = new Intent(ctx, LauncherTapReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(ctx, req, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** [comment] / [comment] / [comment]：[comment] + [comment] */
    public static void refreshAll(Context ctx) {
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, CoverLauncherProvider.class));
            if (ids == null || ids.length == 0) {
                Log.i(TAG, "Text，Text");
                return;
            }
            RemoteViews v = build(ctx);
            for (int id : ids) {
                mgr.updateAppWidget(id, v);
            }
            notifyData(mgr, ids);
            Log.i(TAG, "refreshAll Text " + ids.length + " Text");
        } catch (Throwable t) {
            Log.w(TAG, "refreshAll failed", t);
        }
    }

    private static void notifyData(AppWidgetManager mgr, int[] ids) {
        try {
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.launcherGrid);
        } catch (Throwable t) {
            Log.w(TAG, "notifyAppWidgetViewDataChanged failed", t);
        }
    }
}
