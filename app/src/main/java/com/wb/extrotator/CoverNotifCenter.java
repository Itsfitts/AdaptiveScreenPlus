package com.wb.extrotator;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * [comment]「[comment]」—— [comment]「[comment]」[comment]（v4.22 [comment]）。
 *
 * <p><b>[comment]</b>（[comment]、[comment]）：
 * <pre>
 *   [comment] · N [comment]                                   [comment]
 *   [WiFi][[comment]][[comment]][[comment]][[comment]][[comment]][[comment]]…  ＋  ×
 *   ┌──┬───────────────────────────────────────┐
 *   │[comment]│ [[comment]][[comment]][QQ] …                     │  ← [comment]
 *   │[comment]│ [comment]                            12:30  │
 *   │[comment]│ [comment]…                                  │
 *   │＋│ …                                      │
 *   └──┴───────────────────────────────────────┘
 * </pre>
 *
 * <p><b>[comment]</b>：
 * <ol>
 *   <li>[comment]（[comment]Yes[comment] 5dp [comment]"[comment]"），[comment]；</li>
 *   <li>[comment]<b>[comment]</b>（{@link NotifPins}），[comment]；
 *       [comment]「[comment]」[comment]Yes"[comment]"—— [comment]Yes[comment]，
 *       [comment]，[comment]；</li>
 *   <li>[comment]<b>[comment]</b>（{@link QuickSettings}），[comment]<b>[comment]Yes[comment]</b> ——
 *       [comment]「＋」[comment]Edit[comment]，[comment]、[comment]、[comment]；[comment]。</li>
 * </ol>
 *
 * <p>「[comment]」[comment]Yes[comment]，[comment]Yes[comment] —— [comment]Yes
 * {@code TYPE_ACCESSIBILITY_OVERLAY}，[comment] Dialog/AlertDialog [comment]<b>Default[comment]</b>
 * （[comment]，[comment]Yes[comment]）[comment]，[comment]。[comment]。
 *
 * <p>[comment]Yes[comment] {@code TYPE_ACCESSIBILITY_OVERLAY}，<b>[comment]</b>，[comment]
 * {@code FLAG_NOT_FOCUSABLE} —— [comment]。⚠ [comment] {@code gravity = TOP|START}
 * [comment] {@code FLAG_LAYOUT_NO_LIMITS}，No[comment] x/y [comment]"[comment]"、
 * [comment] inset [comment]（[comment]Yes v4.21b [comment]）。
 *
 * <p>[comment] {@link CoverNotifListener}（[comment]），[comment]、[comment]；
 * [comment] {@code contentIntent}，[comment]。
 */
public final class CoverNotifCenter {

    private static final String TAG = "CoverNotifCenter";

    /** [comment]。[comment]，[comment]，[comment] */
    private static final int C_BG = 0xFF141418;
    private static final int C_TITLE = 0xF2FFFFFF;
    private static final int C_TEXT = 0x99FFFFFF;
    private static final int C_DIM = 0x6BFFFFFF;
    private static final int C_ACCENT = 0xFF5B9DFF;
    private static final int C_CHIP = 0x14FFFFFF;
    private static final int C_CHIP_ON = 0x335B9DFF;
    private static final int C_TILE = 0x14FFFFFF;
    private static final int C_TILE_ON = 0x3D5B9DFF;
    /** [comment]Success[comment] */
    private static final int C_ERR = 0xFFC2453A;

    private static final long ANIM_MS = 150L;
    /** [comment]（[comment]，[comment]） */
    private static final int MAX_ROWS = 30;
    /** [comment] / [comment]（dp）。[comment]"[comment]" */
    private static final int PIN_W = 36;
    private static final int CELL = 36;
    private static final int CELL_ICON = 30;
    /** [comment] */
    private static final int TILE = 34;
    private static final int TILE_ICON = 16;
    /** [comment]「×」[comment]（v4.23 [comment]） */
    private static final int CLOSE = 42;

    private static Context displayCtx;
    private static WindowManager wm;
    private static int display;
    private static int wpx;
    private static int hpx;
    private static float density = 2.125f;

    private static FrameLayout root;
    private static LinearLayout card;
    private static LinearLayout qsRow;
    private static LinearLayout body;
    private static TextView countView;

    private static Handler ui;

    /** [comment]（null = [comment]） */
    private static String filterPkg;
    /** [comment]Yes「[comment]」[comment]Yes「[comment]」 */
    private static boolean picking;
    /** [comment]Yes「[comment]」[comment]（[comment] picking [comment]） */
    private static boolean editingQs;
    /** [comment] */
    private static LinearLayout qsEditBox;
    /** [comment]（[comment]Yes[comment]，[comment]） */
    private static LinearLayout pickBox;

    /** [comment]：[comment]Yes IO，[comment] {@code AppRepo.load} [comment]，[comment] */
    private static volatile List<AppRepo.Item> appCache;

    /** [comment]：[comment] → [comment]（[comment]Yes null，[comment]"[comment]"，[comment]） */
    private static final Map<String, Bitmap> PIN_ICON_CACHE = new HashMap<>();

    private CoverNotifCenter() {
    }

    static void env(Context ctx, WindowManager w, int disp, int wPx, int hPx, float den) {
        displayCtx = ctx;
        wm = w;
        display = disp;
        wpx = wPx;
        hpx = hPx;
        density = den;
        ui = new Handler(Looper.getMainLooper());
    }

    public static boolean showing() {
        return root != null;
    }

    /** [comment]（[comment]，[comment]Yes[comment]） */
    static void show() {
        if (displayCtx == null || wm == null) {
            Log.w(TAG, "Text，Text");
            return;
        }
        if (root != null) {
            refreshViews();
            return;
        }
        try {
            FrameLayout r = new FrameLayout(displayCtx);
            card = buildCard(displayCtx);
            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            r.addView(card, clp);
            card.setAlpha(0f);
            card.setTranslationY(-dp(12));

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;
            // ⚠ [comment]「[comment] inset」：TYPE_ACCESSIBILITY_OVERLAY [comment] fitInsetsTypes
            //   Default[comment] systemBars，[comment] —— [comment]
            //   [comment]Yes「[comment]、[comment]」（[comment]Yes[comment]）。
            //   FLAG_LAYOUT_IN_SCREEN [comment]，[comment] setter；[comment]Yes API 30 [comment]。
            if (Build.VERSION.SDK_INT >= 30) {
                lp.setFitInsetsTypes(0);
            }
            wm.addView(r, lp);
            root = r;
            buildContent(displayCtx);
            card.animate().alpha(1f).translationY(0f).setDuration(ANIM_MS).start();
            Log.i(TAG, "Text：Text " + wpx + "x" + hpx + "，Text " + density);
        } catch (Throwable t) {
            Log.e(TAG, "TextFailed", t);
            hide();
        }
    }

    public static void hide() {
        final FrameLayout r = root;
        root = null;
        final LinearLayout c = card;
        card = null;
        qsRow = null;
        body = null;
        countView = null;
        pickBox = null;
        filterPkg = null;
        picking = false;
        editingQs = false;
        qsEditBox = null;
        if (r == null) {
            return;
        }
        if (c != null) {
            c.animate().alpha(0f).setDuration(ANIM_MS).start();
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                if (wm != null) {
                    wm.removeViewImmediate(r);
                }
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed", t);
            }
        }, ANIM_MS);
        Log.i(TAG, "Text");
    }

    /** [comment] */
    static void release() {
        FrameLayout r = root;
        root = null;
        card = null;
        qsRow = null;
        body = null;
        countView = null;
        pickBox = null;
        filterPkg = null;
        picking = false;
        editingQs = false;
        qsEditBox = null;
        if (r != null && wm != null) {
            try {
                wm.removeViewImmediate(r);
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed", t);
            }
        }
        displayCtx = null;
        wm = null;
        ui = null;
        synchronized (PIN_ICON_CACHE) {
            PIN_ICON_CACHE.clear();
        }
    }

    /** [comment] / [comment]：[comment] */
    static void dropAppCache() {
        appCache = null;
    }

    /** [comment]（[comment]） */
    static void onNotificationsChanged() {
        if (root == null) {
            return;
        }
        refreshViews();
    }

    private static void refreshViews() {
        if (root == null || card == null || picking || editingQs) {
            return;
        }
        card.post(() -> {
            try {
                if (root != null && !picking && !editingQs) {
                    buildContent(card.getContext());
                }
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed", t);
            }
        });
    }

    // ------------------------------------------------------------------ [comment]

    private static LinearLayout buildCard(Context c) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(C_BG);
        box.setClickable(true);            // Text
        box.setPadding(dp(8), dp(5), dp(8), dp(6));

        // ① [comment]：[comment] · N [comment] ......................... [comment]
        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView t = text(c, "Text", 13f, C_TITLE, true);
        head.addView(t);

        countView = text(c, "", 10f, C_DIM, false);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cp.leftMargin = dp(5);
        head.addView(countView, cp);

        // 「[comment]」[comment]，[comment]（[comment]、[comment] 30dp [comment]）——
        // [comment]，[comment] 10.5sp [comment]，[comment]（v4.23 [comment]）
        TextView clear = text(c, "Text", 11.5f, C_TEXT, false);
        clear.setGravity(Gravity.CENTER);
        clear.setPadding(dp(11), dp(6), dp(11), dp(6));
        clear.setBackground(chipBg(C_CHIP));
        clear.setMinHeight(dp(30));
        clear.setOnClickListener(v -> dismissAll());
        head.addView(clear);

        box.addView(head);

        // ② [comment] + 「＋」+ 「×」
        //
        // ⚠ [comment]Yes[comment] HorizontalScrollView [comment]，[comment]「＋」[comment]「×」**[comment]** ——
        //   [comment]，[comment]。
        //   [comment]：[comment]，「＋」「×」[comment]。
        LinearLayout qsBar = new LinearLayout(c);
        qsBar.setOrientation(LinearLayout.HORIZONTAL);
        qsBar.setGravity(Gravity.CENTER_VERTICAL);

        HorizontalScrollView qs = new HorizontalScrollView(c);
        qs.setHorizontalScrollBarEnabled(false);
        qsRow = new LinearLayout(c);
        qsRow.setOrientation(LinearLayout.HORIZONTAL);
        qsRow.setGravity(Gravity.CENTER_VERTICAL);
        qsRow.setPadding(0, dp(4), 0, dp(4));
        qs.addView(qsRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        qsBar.addView(qs, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // 「＋」＝[comment]（[comment] / [comment] / [comment]）
        TextView qsAdd = text(c, "＋", 17f, C_DIM, false);
        qsAdd.setGravity(Gravity.CENTER);
        qsAdd.setBackground(chipBg(C_CHIP));
        qsAdd.setContentDescription("Text");
        qsAdd.setOnClickListener(v -> {
            editingQs = true;
            picking = false;
            buildContent(c);
        });
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(TILE), dp(TILE));
        ap.leftMargin = dp(6);
        qsBar.addView(qsAdd, ap);

        // 「×」：v4.23 [comment]，[comment] 42dp [comment] —— [comment]，
        // [comment]Yes[comment]Yes[comment]；[comment] 10dp，[comment]
        TextView close = text(c, "×", 26f, C_TITLE, false);
        close.setGravity(Gravity.CENTER);
        close.setBackground(chipBg(C_CHIP));
        close.setContentDescription("Disable");
        close.setOnClickListener(v -> hide());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(CLOSE), dp(CLOSE));
        clp.leftMargin = dp(10);
        qsBar.addView(close, clp);

        box.addView(qsBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // ③ [comment]（[comment] + [comment]，[comment]"[comment]"）
        body = new LinearLayout(c);
        body.setOrientation(LinearLayout.HORIZONTAL);
        box.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return box;
    }

    private static void buildContent(Context c) {
        List<CoverNotifListener.Entry> all = new ArrayList<>();
        try {
            CoverNotifListener.refreshAll();
            all = CoverNotifListener.snapshot();
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
        if (countView != null) {
            countView.setText(all.isEmpty() ? "" : ("· " + all.size() + " Text"));
        }
        fillQs(c);
        if (picking) {
            buildPicker(c);
        } else if (editingQs) {
            buildQsEditor(c);
        } else {
            buildBody(c, all);
        }
    }

    // ------------------------------------------------------------------ [comment]

    private static void fillQs(Context c) {
        if (qsRow == null) {
            return;
        }
        qsRow.removeAllViews();
        List<Integer> ids = QuickSettings.list(c);
        for (int id : ids) {
            qsRow.addView(qsTile(c, id));
        }
        if (ids.isEmpty()) {
            TextView t = text(c, "TextYesText，Text「＋」Text", 10f, C_DIM, false);
            t.setPadding(dp(4), 0, 0, 0);
            qsRow.addView(t);
        }
    }

    /**
     * [comment]：[comment] 34dp [comment] + 16dp [comment]。
     * [comment]＝[comment]（[comment]），[comment]＝[comment]。
     */
    private static View qsTile(Context c, final int id) {
        boolean on = QuickSettings.isOn(c, id);
        FrameLayout box = new FrameLayout(c);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(on ? C_TILE_ON : C_TILE);
        if (on) {
            bg.setStroke(dp(1), 0x665B9DFF);
        }
        box.setBackground(bg);

        ImageView iv = new ImageView(c);
        iv.setImageResource(QuickSettings.icon(id));
        iv.setColorFilter(on ? 0xFFFFFFFF : 0x8AFFFFFF);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(dp(TILE_ICON), dp(TILE_ICON));
        ip.gravity = Gravity.CENTER;
        box.addView(iv, ip);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(TILE), dp(TILE));
        lp.rightMargin = dp(6);
        box.setLayoutParams(lp);
        box.setContentDescription(QuickSettings.name(id));
        box.setOnClickListener(v -> tapQs(c, id, box));
        // [comment]（[comment]「＋」）——
        // [comment]"[comment]Edit[comment]"[comment]，[comment]
        box.setOnLongClickListener(v -> {
            QuickSettings.remove(c, id);
            hint("Text「" + QuickSettings.name(id) + "」，Text「＋」Text");
            fillQs(c);
            return true;
        });
        return box;
    }

    /** [comment]（[comment]） */
    private static void hint(String s) {
        if (countView != null) {
            countView.setText(s);
        }
    }

    private static void tapQs(final Context c, final int id, final View tile) {
        if (tile.getAlpha() < 1f) {
            return;                     // Text
        }
        tile.setAlpha(0.4f);
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            final String err = QuickSettings.toggle(app, id);
            post(() -> {
                if (root == null) {
                    return;
                }
                tile.setAlpha(1f);
                if (err != null) {
                    flashErr(c, tile, QuickSettings.name(id) + "：" + err);
                } else {
                    fillQs(c);          // Text，Text
                }
            });
        }, "qs-toggle").start();
    }

    /** [comment]Success：[comment]，[comment]，[comment] */
    private static void flashErr(Context c, View tile, String msg) {
        Log.w(TAG, "Text：" + msg);
        hint(msg);
        Drawable d = tile.getBackground();
        if (d instanceof GradientDrawable) {
            try {
                ((GradientDrawable) d).setColor(C_ERR);
            } catch (Throwable ignored) {
            }
        }
        tile.postDelayed(() -> {
            if (root != null) {
                buildContent(c);
            }
        }, 900);
    }

    // ------------------------------------------------------------------ [comment]

    private static void buildBody(Context c, List<CoverNotifListener.Entry> all) {
        body.removeAllViews();

        // [comment]：[comment]
        ScrollView pinScroll = new ScrollView(c);
        pinScroll.setVerticalScrollBarEnabled(false);
        LinearLayout pinCol = new LinearLayout(c);
        pinCol.setOrientation(LinearLayout.VERTICAL);
        // ⚠ [comment]（[comment]Yes[comment]）：[comment]「[comment]」[comment]、[comment]
        //   [comment]（[comment]Yes card [comment] 8dp padding）。[comment] 6dp。
        pinCol.setGravity(Gravity.START);
        pinScroll.addView(pinCol, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                dp(PIN_W), ViewGroup.LayoutParams.MATCH_PARENT);
        plp.rightMargin = dp(6);
        body.addView(pinScroll, plp);
        fillPins(c, pinCol);

        // [comment]：[comment] + [comment]
        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.VERTICAL);

        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chipBox = new LinearLayout(c);
        chipBox.setOrientation(LinearLayout.HORIZONTAL);
        chipBox.setGravity(Gravity.CENTER_VERTICAL);
        chipBox.setPadding(0, dp(4), 0, dp(4));
        hs.addView(chipBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        right.addView(hs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout listBox = new LinearLayout(c);
        listBox.setOrientation(LinearLayout.VERTICAL);
        sv.addView(listBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        right.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        body.addView(right, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        fillChips(c, chipBox, all);
        fillList(c, listBox, all);
    }

    // ------------------------------------------------------------------ [comment]：[comment]

    private static void fillPins(Context c, LinearLayout pinCol) {
        pinCol.removeAllViews();
        List<String> pins = NotifPins.list(c);
        List<ImageView> waiting = new ArrayList<>();
        List<String> waitingPkg = new ArrayList<>();

        for (String pkg : pins) {
            ImageView iv = new ImageView(c);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Bitmap bm;
            synchronized (PIN_ICON_CACHE) {
                bm = PIN_ICON_CACHE.get(pkg);
            }
            if (bm != null) {
                iv.setImageBitmap(bm);
            } else {
                waiting.add(iv);
                waitingPkg.add(pkg);
            }
            FrameLayout cell = cellBox(c);
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(
                    dp(CELL_ICON), dp(CELL_ICON));
            ip.gravity = Gravity.CENTER;
            cell.addView(iv, ip);
            cell.setOnClickListener(v -> {
                hide();
                launchPkg(c, pkg);
            });
            cell.setOnLongClickListener(v -> {
                NotifPins.remove(c, pkg);
                fillPins(c, pinCol);
                return true;
            });
            pinCol.addView(cell);
        }

        // [comment]「＋」：[comment]
        FrameLayout add = cellBox(c);
        TextView plus = text(c, "＋", 17f, C_DIM, false);
        plus.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.gravity = Gravity.CENTER;
        add.addView(plus, pp);
        add.setOnClickListener(v -> {
            picking = true;
            editingQs = false;
            buildContent(c);
        });
        pinCol.addView(add);

        if (!waiting.isEmpty()) {
            final Context app = c.getApplicationContext();
            new Thread(() -> {
                for (int i = 0; i < waiting.size(); i++) {
                    String pkg = waitingPkg.get(i);
                    Bitmap bm = AppIconLib.icon(app, pkg);
                    synchronized (PIN_ICON_CACHE) {
                        PIN_ICON_CACHE.put(pkg, bm);
                    }
                    if (bm == null) {
                        continue;
                    }
                    final Bitmap f = bm;
                    final ImageView v = waiting.get(i);
                    post(() -> v.setImageBitmap(f));
                }
            }, "notif-pin-icon").start();
        }
    }

    // ------------------------------------------------------------------ [comment]

    private static void buildPicker(Context c) {
        body.removeAllViews();
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);

        col.addView(pickTop(c, "Text，Text（Text）"));

        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        pickBox = new LinearLayout(c);
        pickBox.setOrientation(LinearLayout.VERTICAL);
        sv.addView(pickBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        body.addView(col, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        fillPicker(c);
    }

    private static void fillPicker(final Context c) {
        if (pickBox == null) {
            return;
        }
        List<AppRepo.Item> cache = appCache;
        if (cache != null) {
            renderPicker(c, cache);
            return;
        }
        pickBox.removeAllViews();
        TextView t = text(c, "Text…", 11f, C_DIM, false);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(20), 0, 0);
        pickBox.addView(t);

        final Context app = c.getApplicationContext();
        new Thread(() -> {
            List<AppRepo.Item> items;
            try {
                items = AppRepo.load(app);
            } catch (Throwable e) {
                items = new ArrayList<>();
            }
            appCache = items;
            final List<AppRepo.Item> f = items;
            post(() -> {
                if (root != null && picking) {
                    renderPicker(c, f);
                }
            });
        }, "notif-pick-load").start();
    }

    private static void renderPicker(Context c, List<AppRepo.Item> items) {
        if (pickBox == null) {
            return;
        }
        pickBox.removeAllViews();
        for (AppRepo.Item it : items) {
            pickBox.addView(pickRow(c, it));
        }
        if (items.isEmpty()) {
            TextView t = text(c, "Text", 11f, C_DIM, false);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(20), 0, 0);
            pickBox.addView(t);
        }
    }

    /**
     * [comment]。[comment]<b>[comment]</b>[comment]，[comment] ——
     * 「✓」[comment]，[comment]（ⓘ [comment]）。
     */
    private static View pickRow(final Context c, final AppRepo.Item it) {
        boolean on = NotifPins.has(c, it.pkg);
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(on ? C_CHIP_ON : 0x00FFFFFF);
        row.setBackground(bg);
        row.setPadding(dp(7), dp(5), dp(7), dp(5));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = dp(3);
        row.setLayoutParams(rlp);

        ImageView iv = new ImageView(c);
        if (it.icon != null) {
            iv.setImageDrawable(it.icon);
        }
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(26), dp(26));
        ip.rightMargin = dp(8);
        row.addView(iv, ip);

        TextView name = text(c, it.label, 11.5f, on ? 0xFFFFFFFF : C_TEXT, false);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(name, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.setOnClickListener(v -> {
            if (NotifPins.has(c, it.pkg)) {
                NotifPins.remove(c, it.pkg);
                recolorRow(row, name, false);
            } else if (NotifPins.add(c, it.pkg)) {
                recolorRow(row, name, true);
            } else if (countView != null) {
                countView.setText("Text " + NotifPins.MAX + " Text");
            }
        });
        return row;
    }

    /**
     * Edit[comment]：[comment]Notice + 「[comment]」。[comment]。
     */
    private static View pickTop(Context c, String tipText) {
        LinearLayout top = new LinearLayout(c);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView tip = text(c, tipText, 9.5f, C_DIM, false);
        top.addView(tip, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView done = text(c, "Text", 11.5f, C_ACCENT, false);
        done.setPadding(dp(10), dp(5), dp(4), dp(5));
        done.setOnClickListener(v -> {
            picking = false;
            editingQs = false;
            buildContent(c);
        });
        top.addView(done);
        return top;
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * 「[comment]」[comment]（v4.23）。[comment]「[comment]」[comment]：[comment]，
     * [comment] —— [comment]（[comment]）。
     *
     * <p>[comment]：「[comment]」[comment]，[comment]、↑↓ [comment]；「[comment]」[comment]，
     * [comment]。[comment]，[comment] —— [comment]。
     */
    private static void buildQsEditor(Context c) {
        body.removeAllViews();
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(pickTop(c, "Text，Text「Text」Text，↑↓ Text"));

        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        qsEditBox = new LinearLayout(c);
        qsEditBox.setOrientation(LinearLayout.VERTICAL);
        sv.addView(qsEditBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        body.addView(col, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        fillQsEditor(c);
    }

    private static void fillQsEditor(Context c) {
        if (qsEditBox == null) {
            return;
        }
        qsEditBox.removeAllViews();
        List<Integer> on = QuickSettings.list(c);
        qsEditBox.addView(qsSection(c, "Text（" + on.size() + "）"));
        for (int i = 0; i < on.size(); i++) {
            qsEditBox.addView(qsPickRow(c, on.get(i), true, i == 0, i == on.size() - 1));
        }
        List<Integer> rest = new ArrayList<>();
        for (int id : QuickSettings.ALL_IDS) {
            if (!on.contains(id)) {
                rest.add(id);
            }
        }
        qsEditBox.addView(qsSection(c, rest.isEmpty() ? "Text" : "Text"));
        for (int id : rest) {
            qsEditBox.addView(qsPickRow(c, id, false, true, true));
        }
    }

    /** Edit[comment]。[comment] + ↑↓；[comment]，[comment]「＋」 */
    private static View qsPickRow(Context c, final int id, final boolean on,
                                  boolean first, boolean last) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(on ? C_CHIP_ON : 0x00FFFFFF);
        row.setBackground(bg);
        row.setPadding(dp(7), dp(4), dp(3), dp(4));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = dp(3);
        row.setLayoutParams(rlp);

        ImageView iv = new ImageView(c);
        iv.setImageResource(QuickSettings.icon(id));
        iv.setColorFilter(on ? 0xFFFFFFFF : 0x8AFFFFFF);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(17), dp(17));
        ip.rightMargin = dp(8);
        row.addView(iv, ip);

        TextView name = text(c, QuickSettings.name(id), 11.5f, on ? 0xFFFFFFFF : C_TEXT, false);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(name, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (on) {
            row.addView(qsArrow(c, "↑", !first, v -> {
                QuickSettings.move(c, id, -1);
                fillQsEditor(c);
                fillQs(c);
            }));
            row.addView(qsArrow(c, "↓", !last, v -> {
                QuickSettings.move(c, id, 1);
                fillQsEditor(c);
                fillQs(c);
            }));
        } else {
            TextView plus = text(c, "＋", 14f, C_DIM, false);
            plus.setGravity(Gravity.CENTER);
            plus.setMinWidth(dp(32));
            row.addView(plus);
        }

        row.setOnClickListener(v -> {
            if (on) {
                QuickSettings.remove(c, id);
            } else {
                QuickSettings.add(c, id);
            }
            fillQsEditor(c);
            fillQs(c);
        });
        return row;
    }

    /** [comment]；[comment]、[comment] */
    private static View qsArrow(Context c, String s, boolean usable, View.OnClickListener click) {
        TextView t = text(c, s, 14f, usable ? C_TITLE : 0x30FFFFFF, false);
        t.setGravity(Gravity.CENTER);
        t.setMinWidth(dp(32));
        t.setMinHeight(dp(30));
        t.setBackground(usable ? chipBg(C_CHIP) : null);
        if (usable) {
            t.setOnClickListener(click);
        }
        return t;
    }

    private static View qsSection(Context c, String s) {
        TextView t = text(c, s, 9.5f, C_DIM, false);
        t.setPadding(dp(3), dp(8), 0, dp(4));
        return t;
    }

    private static void recolorRow(View row, TextView name, boolean on) {
        Drawable d = row.getBackground();
        if (d instanceof GradientDrawable) {
            try {
                ((GradientDrawable) d).setColor(on ? C_CHIP_ON : 0x00FFFFFF);
            } catch (Throwable ignored) {
            }
        }
        name.setTextColor(on ? 0xFFFFFFFF : C_TEXT);
    }

    // ------------------------------------------------------------------ [comment]

    private static void fillChips(Context c, LinearLayout chipBox,
                                  List<CoverNotifListener.Entry> all) {
        chipBox.removeAllViews();
        Set<String> pkgs = new LinkedHashSet<>();
        for (CoverNotifListener.Entry e : all) {
            pkgs.add(e.pkg);
        }
        if (pkgs.size() > 1) {
            chipBox.addView(chip(c, "Text", null, null));
        }
        for (String pkg : pkgs) {
            CoverNotifListener.Entry one = firstOf(all, pkg);
            chipBox.addView(chip(c, labelOf(one, pkg), one == null ? null : one.icon, pkg));
        }
    }

    private static View chip(Context c, String name, Drawable icon, String pkg) {
        boolean on = pkg == null ? filterPkg == null : pkg.equals(filterPkg);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(13));
        bg.setColor(on ? C_CHIP_ON : C_CHIP);
        if (on) {
            bg.setStroke(dp(1), 0x805B9DFF);
        }
        box.setBackground(bg);
        box.setPadding(dp(7), dp(3), dp(9), dp(3));

        if (icon != null) {
            ImageView iv = new ImageView(c);
            iv.setImageDrawable(icon);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(12), dp(12));
            ip.rightMargin = dp(4);
            box.addView(iv, ip);
        }
        TextView t = text(c, name, 10f, on ? 0xFFFFFFFF : C_TEXT, false);
        box.addView(t);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(5);
        box.setLayoutParams(lp);
        box.setOnClickListener(v -> {
            filterPkg = pkg;
            buildContent(c);
        });
        return box;
    }

    // ------------------------------------------------------------------ [comment]

    private static void fillList(Context c, LinearLayout listBox,
                                 List<CoverNotifListener.Entry> all) {
        listBox.removeAllViews();
        List<CoverNotifListener.Entry> show = new ArrayList<>();
        for (CoverNotifListener.Entry e : all) {
            if (filterPkg == null || filterPkg.equals(e.pkg)) {
                show.add(e);
            }
        }
        if (show.isEmpty()) {
            LinearLayout box = new LinearLayout(c);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER);
            box.setPadding(0, dp(24), 0, dp(24));
            TextView t = text(c, "", 11.5f, C_DIM, false);
            if (!CoverNotifListener.granted(c)) {
                t.setText("Text\nText「Beta Text → Text」Text");
            } else if (all.isEmpty()) {
                t.setText("Text");
            } else {
                t.setText("Text");
            }
            t.setGravity(Gravity.CENTER);
            box.addView(t);
            listBox.addView(box);
            return;
        }
        int n = Math.min(MAX_ROWS, show.size());
        for (int i = 0; i < n; i++) {
            listBox.addView(notifRow(c, show.get(i)));
        }
        if (show.size() > n) {
            TextView more = text(c, "Text " + (show.size() - n) + " Text", 10f, C_DIM, false);
            more.setGravity(Gravity.CENTER);
            more.setPadding(0, dp(6), 0, dp(4));
            listBox.addView(more);
        }
    }

    /**
     * [comment]。[comment]Yes[comment]（[comment]+[comment] / [comment] / [comment]），[comment]<b>[comment]</b> ——
     * [comment]，[comment]，[comment]。
     */
    private static View notifRow(Context c, final CoverNotifListener.Entry e) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(0x0DFFFFFF);
        row.setBackground(bg);
        row.setPadding(dp(7), dp(6), dp(2), dp(6));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.bottomMargin = dp(5);
        row.setLayoutParams(rlp);
        row.setClickable(true);

        View icon = appIcon(c, e);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(22), dp(22));
        ip.rightMargin = dp(7);
        row.addView(icon, ip);

        LinearLayout textCol = new LinearLayout(c);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout meta = new LinearLayout(c);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text(c, e.title, 11.5f, C_TITLE, false);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        meta.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView when = text(c, hhmm(e.when), 9.5f, C_DIM, false);
        when.setPadding(dp(4), 0, 0, 0);
        meta.addView(when);
        textCol.addView(meta);

        if (!TextUtils.isEmpty(e.text)) {
            TextView t = text(c, e.text, 10f, C_TEXT, false);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            textCol.addView(t);
        }
        row.addView(textCol);

        // [comment] ×。[comment] —— [comment]Yes[comment]Yes[comment]。
        TextView x = text(c, "×", 18f, C_DIM, false);
        x.setGravity(Gravity.CENTER);
        x.setMinWidth(dp(34));
        x.setMinHeight(dp(34));
        x.setOnClickListener(v -> {
            CoverNotifListener.dismiss(e.key);
            buildContent(c);
        });
        row.addView(x);

        row.setOnClickListener(v -> open(e));
        return row;
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment]：[comment] contentIntent，[comment] */
    private static void open(final CoverNotifListener.Entry e) {
        boolean went = false;
        try {
            if (e.contentIntent != null) {
                e.contentIntent.send();
                went = true;
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
        hide();
        if (!went) {
            launchPkg(displayCtx, e.pkg);
        }
    }

    private static void dismissAll() {
        List<CoverNotifListener.Entry> all;
        try {
            all = CoverNotifListener.snapshot();
        } catch (Throwable t) {
            all = new ArrayList<>();
        }
        for (CoverNotifListener.Entry e : all) {
            if (filterPkg == null || filterPkg.equals(e.pkg)) {
                CoverNotifListener.dismiss(e.key);
            }
        }
        if (card == null) {
            return;
        }
        buildContent(card.getContext());
    }

    /**
     * [comment]。
     *
     * <p>[comment] {@link SecondaryLauncher}（shell → [comment] → [comment]，[comment]），
     * [comment]Yes[comment] {@code am start -a MAIN -c LAUNCHER -p <[comment]>} ——
     * [comment]"[comment]、[comment]Yes MAIN/LAUNCHER"[comment]Failed。
     */
    private static void launchPkg(final Context c, final String pkg) {
        if (c == null || pkg == null) {
            return;
        }
        final Context app = c.getApplicationContext();
        final int d = display;
        new Thread(() -> {
            try {
                Intent li = app.getPackageManager().getLaunchIntentForPackage(pkg);
                if (li == null) {
                    Log.w(TAG, pkg + " Text");
                    return;
                }
                boolean ok = SecondaryLauncher.launch(app, d, li);
                Log.i(TAG, "Text " + pkg + " => " + ok);
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed " + pkg, t);
            }
        }, "notif-launch").start();
    }

    // ------------------------------------------------------------------ [comment]

    private static CoverNotifListener.Entry firstOf(List<CoverNotifListener.Entry> all, String pkg) {
        for (CoverNotifListener.Entry e : all) {
            if (e.pkg != null && e.pkg.equals(pkg)) {
                return e;
            }
        }
        return null;
    }

    /** [comment]：[comment]，[comment] */
    private static String labelOf(CoverNotifListener.Entry e, String pkg) {
        if (e != null && !TextUtils.isEmpty(e.label)) {
            return e.label;
        }
        if (pkg == null) {
            return "Text";
        }
        int i = pkg.lastIndexOf('.');
        return i >= 0 && i + 1 < pkg.length() ? pkg.substring(i + 1) : pkg;
    }

    /** [comment]；[comment] */
    private static View appIcon(Context c, CoverNotifListener.Entry e) {
        if (e != null && e.icon != null) {
            ImageView iv = new ImageView(c);
            iv.setImageDrawable(e.icon);
            return iv;
        }
        TextView tv = new TextView(c);
        String s = labelOf(e, e == null ? null : e.pkg);
        tv.setText(s.isEmpty() ? "?" : s.substring(0, 1));
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, 10f * density);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(hueOf(e == null ? "" : e.pkg));
        tv.setBackground(d);
        return tv;
    }

    /** [comment]：[comment] + [comment] */
    private static GradientDrawable chipBg(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(10));
        d.setColor(color);
        return d;
    }

    /** [comment]：[comment] + [comment]，[comment]/[comment] */
    private static FrameLayout cellBox(Context c) {
        FrameLayout box = new FrameLayout(c);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(10));
        bg.setColor(0x14FFFFFF);
        box.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(CELL), dp(CELL));
        lp.bottomMargin = dp(6);
        box.setLayoutParams(lp);
        return box;
    }

    /** [comment]（[comment] sp、[comment]、[comment]） */
    private static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) {
            t.setTypeface(t.getTypeface(), Typeface.BOLD);
        }
        return t;
    }

    /** [comment]，[comment]Yes[comment] */
    private static int hueOf(String pkg) {
        int h = pkg == null ? 0 : (pkg.hashCode() & 0x7FFFFFFF);
        return android.graphics.Color.HSVToColor(new float[]{(h % 360), 0.42f, 0.62f});
    }

    private static String hhmm(long when) {
        try {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(when));
        } catch (Throwable t) {
            return "";
        }
    }

    private static void post(Runnable r) {
        Handler h = ui;
        if (h == null) {
            h = new Handler(Looper.getMainLooper());
        }
        h.post(r);
    }

    private static int dp(float v) {
        return Math.round(v * density);
    }
}
