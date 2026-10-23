package com.wb.extrotator;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

/**
 * [comment]「[comment]」[comment]。
 *
 * <p>RemoteViews [comment] setAdapter，[comment] RemoteViewsService [comment] position
 * [comment]，[comment]Yes[comment] {@link AppFactory}。
 *
 * <p>⚠ [comment] {@code onDataSetChanged()} [comment]**[comment]**[comment] ——
 * [comment]Allow[comment]，[comment] {@code getViewAt()} [comment]，[comment]。
 *
 * <p><b>[comment]</b>（[comment]，[comment]）：
 * [comment] —— itemIcon [comment]Yes wrap_content，[comment]，
 * [comment] {@link LauncherPrefs#iconScale} [comment]；[comment] —— [comment]Settings[comment]，
 * [comment] {@link #toBitmap}；[comment] —— GridView [comment] verticalSpacing [comment]（[comment] @RemotableViewMethod），
 * [comment] {@code setViewPadding} [comment]；[comment] —— [comment]。
 * [comment] —— [comment]Yes[comment]，[comment] {@link CoverLauncherProvider} [comment]。
 */
public class CoverLauncherService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new AppFactory(getApplicationContext());
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * Drawable → [comment] Bitmap，[comment]「[comment]」[comment]。[comment] null，
     * [comment]Default[comment]。
     *
     * <p>[comment]：[comment]（[comment] / [comment] / [comment]），
     * [comment]，[comment]「[comment]」。
     *
     * <p>[comment]：[comment] —— [comment]，[comment]；[comment] —— [comment] = [comment] ×
     * {@link LauncherPrefs#SHAPE_RADIUS_RATIO}；[comment] —— [comment]。
     * ⚠ [comment]：[comment]，[comment]「[comment]」[comment]Yes[comment]，[comment]「[comment]」[comment]
     * [comment]（[comment] ⓘ [comment]）。
     *
     * <p>⚠ [comment] Canvas Yes[comment] Bitmap [comment]（[comment]），clipPath [comment]；
     * [comment] View [comment]。
     *
     * @param shape [comment]；[comment]「[comment]」[comment]
     */
    public static Bitmap toBitmap(Drawable d, int px, int shape) {
        if (d == null || px <= 0) {
            return null;
        }
        try {
            Bitmap bm = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            Path clip = maskPath(px, shape);
            if (clip != null) {
                c.clipPath(clip);
            }
            d.setBounds(0, 0, px, px);
            d.draw(c);
            return bm;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * [comment] → [comment]；"[comment]"[comment] null（[comment]）。
     *
     * <p>[comment] —— [comment]，No[comment]"[comment]"
     * [comment]"[comment]"[comment]Yes[comment]。
     */
    public static Path maskPath(int px, int shape) {
        if (px <= 0) {
            return null;
        }
        try {
            if (shape == LauncherPrefs.SHAPE_CIRCLE) {
                Path p = new Path();
                p.addCircle(px / 2f, px / 2f, px / 2f, Path.Direction.CW);
                return p;
            }
            if (shape == LauncherPrefs.SHAPE_ROUND) {
                float r = px * LauncherPrefs.SHAPE_RADIUS_RATIO;
                Path p = new Path();
                p.addRoundRect(new RectF(0, 0, px, px), r, r, Path.Direction.CW);
                return p;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    // ------------------------------------------------------------------ [comment]

    private static class AppFactory implements RemoteViewsService.RemoteViewsFactory {

        private static final String TAG = "CoverLauncherFactory";

        /**
         * [comment] RemoteViews [comment]（"[comment]"[comment]）。
         * RemoteViews [comment] Binder [comment]，[comment]
         * TransactionTooLarge；108px [comment](340dpi)[comment] 51dp。
         * [comment]/[comment] {@link LauncherPrefs#iconScale} [comment]。
         */
        private static final int ICON_PX = 108;

        /**
         * [comment]（sp），[comment] —— [comment] 8/9/10。
         *
         * <p>⚠ [comment]<b>[comment] {@code widget_launcher_item.xml} [comment] itemLabel.minHeight</b>：
         * [comment]Yes[comment]「[comment]」[comment]（2.82 × [comment] sp），[comment]
         * ⇒ [comment]、[comment]。
         */
        private static final float[] LABEL_SP = {8f, 9f, 10f};

        private final Context ctx;
        private final List<AppRepo.Item> items = new ArrayList<>();
        private final List<Bitmap> icons = new ArrayList<>();

        /**
         * [comment]"[comment] + [comment] + [comment] + [comment] + [comment]"，
         * [comment]。
         * ⚠ [comment] key —— [comment]，[comment]Settings[comment]"[comment]"[comment]，
         * [comment]（v2.18.9 [comment]；v4.14 [comment]）。
         */
        private String lastKey = null;

        AppFactory(Context c) {
            this.ctx = c;
        }

        @Override
        public void onCreate() {
            // [comment]，[comment] onDataSetChanged
        }

        @Override
        public void onDataSetChanged() {
            List<String> picked = LauncherPrefs.apps(ctx);
            final boolean favOnly = LauncherPrefs.favOnly(ctx);
            final List<String> favs = LauncherPrefs.favs(ctx);
            String key = picked.toString()
                    + "|" + LauncherPrefs.sort(ctx)
                    + "|" + LauncherPrefs.iconSize(ctx)
                    + "|" + LauncherPrefs.iconShape(ctx)
                    + "|" + favOnly
                    + "|" + favs;

            if (key.equals(lastKey) && !items.isEmpty()) {
                return;
            }
            lastKey = key;

            items.clear();
            icons.clear();
            if (picked.isEmpty()) {
                return;
            }

            // 1) [comment] Item [comment]
            List<AppRepo.Item> all = AppRepo.load(ctx);
            List<AppRepo.Item> pickedItems = new ArrayList<>();
            for (String pkg : picked) {
                // 「[comment]」[comment]，[comment]
                if (favOnly && !favs.contains(pkg)) {
                    continue;
                }
                for (AppRepo.Item it : all) {
                    if (pkg.equals(it.pkg)) {
                        pickedItems.add(it);
                        break;
                    }
                }
            }

            // 2) [comment] —— [comment]，
            //    No[comment]
            LauncherPrefs.sortItems(ctx, pickedItems);
            items.addAll(pickedItems);

            final int px = iconPx();
            final int shape = LauncherPrefs.iconShape(ctx);
            for (AppRepo.Item it : items) {
                icons.add(toBitmap(it.icon, px, shape));
            }
            Log.i(TAG, "Text " + items.size() + " Text（Text " + px + "px，Text "
                    + shape + "）");
        }

        @Override
        public void onDestroy() {
            items.clear();
            icons.clear();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= items.size()) {
                return null;
            }
            AppRepo.Item it = items.get(position);
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_launcher_item);

            Bitmap bm = position < icons.size() ? icons.get(position) : null;
            if (bm != null) {
                rv.setImageViewBitmap(R.id.itemIcon, bm);
            } else {
                rv.setImageViewResource(R.id.itemIcon, R.mipmap.ic_launcher);
            }

            boolean showLabel = LauncherPrefs.showLabel(ctx);
            rv.setViewVisibility(R.id.itemLabel, showLabel ? View.VISIBLE : View.GONE);
            if (showLabel) {
                rv.setTextViewText(R.id.itemLabel, it.label);
                int size = LauncherPrefs.iconSize(ctx);
                float sp = LABEL_SP[Math.max(0, Math.min(LABEL_SP.length - 1, size))];
                rv.setTextViewTextSize(R.id.itemLabel, TypedValue.COMPLEX_UNIT_SP, sp);
            }

            // "[comment]"：[comment] GridView [comment]，[comment]
            int pad = dp(LauncherPrefs.rowPadDp(ctx));
            rv.setViewPadding(R.id.itemRoot, 0, pad, 0, showLabel ? pad : pad * 2);

            /*
             * [comment]Yes[comment]：[comment] PendingIntent [comment] Provider [comment]，
             * [comment]/[comment]，[comment]。
             */
            Intent fill = new Intent();
            fill.putExtra(LauncherTapReceiver.EXTRA_PKG, it.pkg);
            fill.putExtra(LauncherTapReceiver.EXTRA_CLS,
                    it.component == null ? "" : it.component.getClassName());
            fill.putExtra(LauncherTapReceiver.EXTRA_LABEL, it.label);
            rv.setOnClickFillInIntent(R.id.itemRoot, fill);

            return rv;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        // -------------------------------------------------------------- [comment]

        private int iconPx() {
            int px = Math.round(ICON_PX * LauncherPrefs.iconScale(ctx));
            return Math.max(48, Math.min(200, px));
        }

        /**
         * dp → px。[comment]Yes[comment] density（Default[comment]），[comment] 340dpi [comment] ——
         * [comment]Yes[comment]，[comment]，[comment] metrics。
         */
        private int dp(int v) {
            float d;
            try {
                d = ctx.getResources().getDisplayMetrics().density;
            } catch (Throwable t) {
                d = 2f;
            }
            return Math.round(v * d);
        }
    }
}
