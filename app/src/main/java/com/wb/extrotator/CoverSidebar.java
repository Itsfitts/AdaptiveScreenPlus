package com.wb.extrotator;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * 「[comment]」。
 *
 * <p>[comment]<b>[comment]</b>[comment]（[comment]：[comment]/[comment] × [comment]/[comment]/[comment]，[comment]），
 * [comment] —— [comment]：[comment] / [comment] / [comment] / [comment]，
 * [comment]，[comment]（[comment]），[comment]。
 *
 * <p><b>[comment]</b>：[comment] {@link #TRIGGER_DP}dp [comment]，[comment]，[comment]、
 * [comment] —— [comment]<b>[comment]Yes[comment]</b>（[comment]「[comment] + [comment]」，
 * [comment]「[comment]」）。[comment]，[comment]（[comment]）。
 * [comment] 190ms [comment]。
 *
 * <p><b>[comment]</b>：[comment]「[comment]」[comment] —— 0 Yes {@link CoverWheel} [comment]，
 * 1 Yes {@link CoverGear} [comment]，3 Yes {@link CoverRoulette} [comment]。[comment]<b>[comment]</b>、
 * [comment]，[comment]「[comment]」[comment]Yes {@link CoverNotifCenter}。[comment]，
 * [comment] 2 [comment]，[comment]。
 *
 * <p><b>[comment]</b>：[comment]Yes[comment]，[comment]；[comment] 5x64dp [comment]
 * [comment]（[comment]、[comment]，[comment]）。[comment]<b>[comment]</b>，
 * [comment]。
 *
 * <p>⚠ <b>[comment]（[comment]）</b>：[comment]，[comment]
 * {@link #EDGE_W_DP}dp（≈30px），[comment]。[comment]Yes[comment]
 * （[comment]），[comment]Yes[comment] —— [comment]。
 *
 * <p><b>[comment]</b>：
 * <ul>
 *   <li>[comment]Yes {@code TYPE_ACCESSIBILITY_OVERLAY} ⇒ [comment]None[comment]
 *       （Activity [comment]），[comment] {@link VolumeKeyService}；</li>
 *   <li>dp [comment]<b>[comment]</b>[comment]（340dpi），[comment]Yes {@code Resources.getSystem()}；</li>
 *   <li>[comment]Yes<b>[comment]</b>：[comment]，[comment]、[comment]。</li>
 * </ul>
 */
public final class CoverSidebar {

    private static final String TAG = "CoverSidebar";

    /** [comment]（dp）。[comment]，[comment] —— [comment] */
    private static final int EDGE_W_DP = 14;
    /**
     * [comment]（dp）[comment]。
     *
     * <p>[comment]Yes「[comment] + [comment]」：[comment] 16dp [comment]、[comment]
     * （[comment] 190px）[comment]，[comment]Yes[comment]。[comment]。
     * [comment]<b>[comment]Yes[comment]</b>：[comment]「[comment] + [comment]」，[comment]「[comment]」。
     */
    private static final int TRIGGER_DP = 10;
    /**
     * [comment]（[comment] 1/3）。
     *
     * <p>[comment]Yes<b>[comment]</b>：[comment]，Android [comment]。[comment]
     * [comment]Yes[comment] —— [comment] 14dp [comment]，[comment]Yes[comment]，
     * [comment]。
     *
     * <p>[comment]：[comment]、[comment]、[comment]（[comment] install [comment] bandY）。
     */
    private static final float BAND_RATIO = 0.45f;
    /** [comment] */
    private static final float PANEL_RATIO = 0.68f;
    /** [comment]（ms） */
    private static final long ANIM_MS = 190L;
    /** [comment]（ms） */
    private static final long BUZZ_MS = 14L;
    /** [comment] */
    private static final int MAX_NOTIF_ROWS = 6;
    /**
     * [comment]（[comment]"[comment]"）：[comment] / [comment]（dp）。
     *
     * <p>v4.11 [comment] 40dp [comment] 64dp。[comment]Yes"[comment]，[comment]<b>[comment]</b>[comment]" ——
     * [comment]，[comment]Yes[comment]"[comment]"，
     * [comment]，[comment]。[comment]（5dp）。
     */
    private static final int HANDLE_W_DP = 5;
    private static final int HANDLE_H_DP = 64;
    /** [comment]（View alpha）：[comment] / [comment] */
    private static final float HANDLE_REST = 0.5f;
    private static final float HANDLE_TOUCH = 0.95f;
    /** [comment]（ms） */
    private static final long HANDLE_ANIM_MS = 220L;

    private static final Handler UI = new Handler(Looper.getMainLooper());

    private static WindowManager wm;
    /** [comment] */
    private static View band;
    /** [comment]（[comment]"[comment]"） */
    private static View handle;
    /** [comment]（[comment]）；[comment]，[comment] null */
    private static FrameLayout panelRoot;
    private static View panelCard;
    private static View scrim;
    private static LinearLayout itemsBox;
    private static TextView titleView;

    private static Context appCtx;
    private static int shownDisplay = -1;
    private static float density = 2.125f;
    private static int panelW;
    /** 0 = [comment]，1 = [comment] */
    private static int side = 1;
    /** [comment]（[comment]） */
    private static int bandLeft;
    private static int bandTop;
    /** [comment] */
    private static float downX;
    private static float downY;
    private static boolean dragging;
    private static boolean open;
    /** [comment] 0..1（0 = [comment]） */
    private static float progress;

    private CoverSidebar() {
    }

    // ------------------------------------------------------------------ [comment]

    public static boolean installed() {
        return band != null;
    }

    /** [comment]Settings[comment]：[comment] ⇒ [comment]；No[comment] */
    public static void sync(Context ctx) {
        boolean want = ExtPrefs.coverSidebar(ctx);
        int display = CoverDisplay.id(ctx);
        Log.d(TAG, "sync want=" + want + " Text=" + display + " Text=" + installed());
        if (!want || display <= 0) {
            remove();
            return;
        }
        if (band != null && shownDisplay == display) {
            return;
        }
        remove();
        install(ctx, display);
    }

    /** [comment] / [comment] ⇒ [comment]（[comment]） */
    public static void refresh(Context ctx) {
        remove();
        sync(ctx);
    }

    public static void remove() {
        View b = band;
        FrameLayout r = panelRoot;
        WindowManager w = wm;
        band = null;
        handle = null;
        panelRoot = null;
        panelCard = null;
        scrim = null;
        itemsBox = null;
        titleView = null;
        shownDisplay = -1;
        open = false;
        dragging = false;
        progress = 0;
        try {
            if (r != null && w != null) {
                w.removeViewImmediate(r);
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
        try {
            if (b != null && w != null) {
                w.removeViewImmediate(b);
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
        if (b != null || r != null) {
            Log.i(TAG, "Text");
        }
        // [comment]Yes[comment]，[comment]
        CoverWheel.release();
        CoverGear.release();
        CoverRoulette.release();
        CoverNotifCenter.release();
        wm = null;
        appCtx = null;
    }

    private static void install(Context ctx, int display) {
        if (!(ctx instanceof AccessibilityService)) {
            Log.w(TAG, "TextYesNoneText，Text");
            return;
        }
        try {
            DisplayManager dm = (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
            Display d = dm == null ? null : dm.getDisplay(display);
            if (d == null) {
                Log.w(TAG, "Text display " + display);
                return;
            }
            Context displayCtx = ctx.createDisplayContext(d);
            wm = (WindowManager) displayCtx.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return;
            }
            density = displayCtx.getResources().getDisplayMetrics().density;
            int h = displayCtx.getResources().getDisplayMetrics().heightPixels;
            int wpx = displayCtx.getResources().getDisplayMetrics().widthPixels;
            appCtx = ctx.getApplicationContext();
            side = ExtPrefs.sidebarSide(ctx);
            panelW = Math.round(wpx * PANEL_RATIO);

            int zone = ExtPrefs.sidebarZone(ctx);
            if (zone < 0 || zone > 2) {
                zone = 0;
            }
            // [comment]（v4.11 [comment] BAND_RATIO，[comment] 1/3）。
            // [comment] ⇒ [comment] 2：[comment]、[comment]、[comment]。
            int bandH = Math.max(dp(140), Math.round(h * BAND_RATIO));
            int bandY = Math.round(zone * (h - bandH) / 2f);

            // [comment]，[comment]（[comment]）
            FrameLayout box = new FrameLayout(displayCtx);
            handle = makeHandle(displayCtx);
            FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(
                    dp(HANDLE_W_DP), dp(HANDLE_H_DP));
            hlp.gravity = Gravity.CENTER;
            box.addView(handle, hlp);
            box.setOnTouchListener((v, e) -> onBandTouch(v, e));
            band = box;

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    dp(EDGE_W_DP),
                    bandH,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | (side == 1 ? Gravity.END : Gravity.START);
            lp.y = bandY;

            // [comment]（[comment] 66px，RULES §64.1）
            bandTop = bandY;
            bandLeft = side == 1 ? wpx - dp(EDGE_W_DP) : 0;
            CoverWheel.env(displayCtx, wm, wpx, density, side);
            CoverGear.env(displayCtx, wm, wpx, h, density, side);
            CoverRoulette.env(displayCtx, wm, wpx, h, density, side);
            CoverNotifCenter.env(displayCtx, wm, display, wpx, h, density);

            wm.addView(band, lp);
            shownDisplay = display;
            Log.i(TAG, "Text display " + display + "：" + (side == 1 ? "Text" : "Text")
                    + "Text" + (zone == 0 ? "Text" : zone == 1 ? "Text" : "Text") + "Text，Text "
                    + dp(EDGE_W_DP) + "px Text " + bandH + "px；Text " + panelW
                    + "px；Text " + dp(HANDLE_W_DP) + "x" + dp(HANDLE_H_DP) + "px");
        } catch (Throwable t) {
            Log.e(TAG, "TextFailed", t);
            remove();
        }
    }

    // ------------------------------------------------------------------ [comment]：[comment]

    private static boolean onBandTouch(View v, MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                dragging = false;
                UI.removeCallbacksAndMessages(null);
                handleTo(HANDLE_TOUCH, 1.06f, 90);
                // [comment]（[comment]Yes[comment]，[comment]）
                animPrepare(winX(e.getX()), winY(e.getY()));
                return true;

            case MotionEvent.ACTION_MOVE: {
                if (dragging) {
                    // [comment]：[comment]，[comment]
                    animMove(winX(e.getX()), winY(e.getY()));
                    return true;
                }
                float in = inward(e.getX());
                if (in < dp(TRIGGER_DP)) {
                    return true;
                }
                // [comment]（[comment]）[comment]，[comment]
                if (in < Math.abs(e.getY() - downY)) {
                    return true;
                }
                dragging = true;
                if (animKind() == 2) {
                    buildPanel();
                    settle(true);
                } else {
                    // v4.9 [comment]：[comment]**[comment]**（[comment]"[comment]"[comment]）
                    animShow(winX(e.getX()), winY(e.getY()));
                }
                // [comment]，[comment]
                handleTo(HANDLE_TOUCH * 0.08f, 1.5f, 140);
                Log.i(TAG, "Text：Text " + (int) in + "px");
                return true;
            }

            case MotionEvent.ACTION_UP: {
                UI.removeCallbacksAndMessages(null);
                if (dragging) {
                    // [comment]（[comment] MOVE）：[comment]，[comment]
                    dragging = false;
                    handleTo(HANDLE_REST, 1f, HANDLE_ANIM_MS);
                    // [comment]：[comment]（[comment]Yes RING_NONE）
                    runRingAction(animPickAndHide());
                    return true;
                }
                handleTo(HANDLE_REST, 1f, HANDLE_ANIM_MS);
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
                UI.removeCallbacksAndMessages(null);
                // [comment]（[comment]Yes[comment]）：[comment]，[comment]
                dragging = false;
                animHide();
                handleTo(HANDLE_REST, 1f, HANDLE_ANIM_MS);
                return true;

            default:
                return true;
        }
    }

    /**
     * [comment]Yes[comment]：0 = [comment]（[comment]），1 = [comment]，2 = [comment]，
     * 3 = [comment]。
     *
     * <p>[comment] 0 —— [comment]，[comment]。
     */
    private static int animKind() {
        Context c = appCtx;
        int v = c == null ? 0 : ExtPrefs.sidebarStyle(c);
        return (v >= 0 && v <= 3) ? v : 0;
    }

    /** [comment]：[comment]（[comment]） */
    private static void animPrepare(float x, float y) {
        switch (animKind()) {
            case 1:
                CoverGear.prepare(x, y);
                break;
            case 3:
                CoverRoulette.prepare(x, y);
                break;
            case 2:
                break;
            default:
                CoverWheel.prepare(x, y);
                break;
        }
    }

    /** [comment]：[comment]Update[comment] */
    private static void animMove(float x, float y) {
        switch (animKind()) {
            case 1:
                CoverGear.move(x, y);
                break;
            case 3:
                CoverRoulette.move(x, y);
                break;
            case 2:
                break;
            default:
                CoverWheel.move(x, y);
                break;
        }
    }

    /** [comment]：[comment] */
    private static void animShow(float x, float y) {
        switch (animKind()) {
            case 1:
                CoverGear.show(x, y);
                break;
            case 3:
                CoverRoulette.show(x, y);
                break;
            case 2:
                break;
            default:
                CoverWheel.show(x, y);
                break;
        }
    }

    /**
     * [comment]：[comment]<b>[comment]</b>[comment]，[comment]。
     *
     * <p>⚠ [comment]"[comment]"[comment]，[comment] {@link #animKind()} —— [comment]Yes[comment]
     * [comment]，[comment]。
     */
    private static int animPickAndHide() {
        if (CoverGear.showing()) {
            int act = CoverGear.pick();
            CoverGear.hide();
            return act;
        }
        if (CoverRoulette.showing()) {
            int act = CoverRoulette.pick();
            CoverRoulette.hide();
            return act;
        }
        if (CoverWheel.showing()) {
            int act = CoverWheel.pick();
            CoverWheel.hide();
            return act;
        }
        return ExtPrefs.RING_NONE;
    }

    /** [comment]（[comment]） */
    private static void animHide() {
        CoverWheel.hide();
        CoverGear.hide();
        CoverRoulette.hide();
    }

    /** [comment] -> [comment]（[comment]，RULES §64.1） */
    private static float winX(float localX) {
        return bandLeft + localX;
    }

    private static float winY(float localY) {
        return bandTop + localY;
    }

    /** [comment]（[comment]） */
    private static void runRingAction(int act) {
        switch (act) {
            case ExtPrefs.RING_BACK:
                CoverRecentsGesture.goBack();
                break;
            case ExtPrefs.RING_RECENTS:
                CoverRecentsGesture.openRecentsNow(shownDisplay);
                break;
            case ExtPrefs.RING_NOTIFY:
                CoverNotifCenter.show();
                break;
            case ExtPrefs.RING_HOME:
                CoverRecentsGesture.goHome(shownDisplay);
                break;
            default:
                Log.i(TAG, "Text：Text");
                break;
        }
    }

    /** [comment]"[comment]"[comment]（[comment] = [comment]） */
    private static float inward(float x) {
        // [comment]，[comment]Yes"[comment]"；[comment]
        return side == 1 ? (downX - x) : (x - downX);
    }

    private static void settle(boolean toOpen) {
        if (panelCard == null) {
            return;
        }
        float off = side == 1 ? panelW : -panelW;
        float target = toOpen ? 0f : off;
        panelCard.animate()
                .translationX(target)
                .alpha(toOpen ? 1f : 0.65f)
                .setDuration(ANIM_MS)
                .withEndAction(() -> {
                    if (toOpen) {
                        open = true;
                        progress = 1f;
                    } else {
                        teardownPanel();
                    }
                })
                .start();
        if (scrim != null) {
            scrim.animate().alpha(toOpen ? 1f : 0f).setDuration(ANIM_MS).start();
        }
    }

    /** [comment]（[comment]、[comment]） */
    public static void close() {
        if (panelRoot == null || !open) {
            return;
        }
        open = false;
        settle(false);
    }

    private static void teardownPanel() {
        FrameLayout r = panelRoot;
        panelRoot = null;
        panelCard = null;
        scrim = null;
        itemsBox = null;
        titleView = null;
        progress = 0;
        // [comment]：[comment]（[comment]）
        handleTo(HANDLE_REST, 1f, HANDLE_ANIM_MS);
        if (r != null && wm != null) {
            try {
                wm.removeViewImmediate(r);
            } catch (Throwable t) {
                Log.w(TAG, "TextFailed", t);
            }
        }
    }

    // ------------------------------------------------------------------ [comment]

    private static void buildPanel() {
        if (panelRoot != null || wm == null || appCtx == null) {
            return;
        }
        try {
            Context c = band.getContext();
            FrameLayout root = new FrameLayout(c);
            scrim = new View(c);
            scrim.setBackgroundColor(0x33000000);
            scrim.setOnClickListener(v -> close());

            panelCard = buildCard(c);
            FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                    panelW, ViewGroup.LayoutParams.MATCH_PARENT);
            plp.gravity = side == 1 ? Gravity.END : Gravity.START;
            plp.topMargin = dp(6);
            plp.bottomMargin = dp(6);

            root.addView(scrim, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            root.addView(panelCard, plp);

            float off = side == 1 ? panelW : -panelW;
            panelCard.setTranslationX(off);
            panelCard.setAlpha(0.65f);
            scrim.setAlpha(0f);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    // [comment]（[comment]），[comment]**[comment]** NOT_TOUCH_MODAL
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            wm.addView(root, lp);
            panelRoot = root;
            fillItems(c);
            Log.i(TAG, "Text（" + (side == 1 ? "Text" : "Text") + "）");
        } catch (Throwable t) {
            Log.e(TAG, "TextFailed", t);
            teardownPanel();
        }
    }

    /** [comment]：[comment] + [comment] + [comment] + [comment]Notice */
    private static View buildCard(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true);          // Text，Text
        GradientDrawable bg = new GradientDrawable();
        // [comment]：72% [comment] —— [comment]Yes"[comment]"
        bg.setColor(0xB81B1B1F);
        bg.setCornerRadius(dp(24));
        bg.setStroke(dp(1), 0x1FFFFFFF);
        card.setBackground(bg);
        int pad = dp(10);
        card.setPadding(pad, pad + dp(2), pad, pad);

        titleView = new TextView(c);
        titleView.setText("Text");
        titleView.setTextColor(0x99FFFFFF);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        titleView.setPadding(dp(8), dp(2), dp(8), dp(6));
        card.addView(titleView);

        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        itemsBox = new LinearLayout(c);
        itemsBox.setOrientation(LinearLayout.VERTICAL);
        sv.addView(itemsBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView hint = new TextView(c);
        hint.setText("Text");
        hint.setTextColor(0x59FFFFFF);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        hint.setPadding(dp(8), dp(6), dp(8), dp(2));
        card.addView(hint);
        return card;
    }

    /** [comment]Settings[comment]（[comment]、[comment]） */
    private static void fillItems(Context c) {
        if (itemsBox == null) {
            return;
        }
        itemsBox.removeAllViews();
        try {
            // [comment]Yes「[comment]」[comment]（[comment]，[comment]）
            for (int act : ExtPrefs.sidebarSlots(c)) {
                switch (act) {
                    case ExtPrefs.RING_NOTIFY:
                        if (!CoverNotifListener.granted(c)) {
                            itemsBox.addView(actionRow(c, "Text", "Text，Text",
                                    () -> grantNotif(c)));
                        } else {
                            CoverNotifListener.refreshAll();
                            List<CoverNotifListener.Entry> list = CoverNotifListener.snapshot();
                            if (list.isEmpty()) {
                                itemsBox.addView(actionRow(c, "Text", null, null));
                            } else {
                                int n = Math.min(MAX_NOTIF_ROWS, list.size());
                                for (int i = 0; i < n; i++) {
                                    itemsBox.addView(notifRow(c, list.get(i)));
                                }
                                if (list.size() > n) {
                                    itemsBox.addView(actionRow(c,
                                            "Text " + (list.size() - n) + " Text", null, null));
                                }
                            }
                        }
                        break;
                    case ExtPrefs.RING_RECENTS:
                        itemsBox.addView(actionRow(c, "Text", null, () -> {
                            close();
                            CoverRecentsGesture.openRecentsNow(shownDisplay);
                        }));
                        break;
                    case ExtPrefs.RING_BACK:
                        itemsBox.addView(actionRow(c, "Text", null, () -> {
                            close();
                            CoverRecentsGesture.goBack();
                        }));
                        break;
                    case ExtPrefs.RING_HOME:
                        itemsBox.addView(actionRow(c, "Text", null, () -> {
                            close();
                            CoverRecentsGesture.goHome(shownDisplay);
                        }));
                        break;
                    default:
                        break;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
    }

    /** [comment]（[comment]、[comment]） */
    public static void onNotificationsChanged() {
        // [comment]
        CoverNotifCenter.onNotificationsChanged();
        if (panelRoot == null) {
            return;
        }
        UI.post(() -> {
            if (itemsBox != null && appCtx != null) {
                fillItems(itemsBox.getContext());
            }
        });
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment]：[comment] + [comment]，[comment]、[comment] × [comment] */
    private static View notifRow(Context c, CoverNotifListener.Entry e) {
        LinearLayout row = baseRow(c);
        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView t = new TextView(c);
        t.setText(e.title);
        t.setTextColor(0xF0FFFFFF);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        body.addView(t);

        if (!TextUtils.isEmpty(e.text)) {
            TextView s = new TextView(c);
            s.setText(e.text);
            s.setTextColor(0x8CFFFFFF);
            s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            s.setMaxLines(2);
            s.setEllipsize(TextUtils.TruncateAt.END);
            body.addView(s);
        }
        row.addView(body);

        // × [comment]：[comment]，[comment] —— [comment] 40dp [comment]
        TextView x = new TextView(c);
        x.setText("×");
        x.setTextColor(0x99FFFFFF);
        x.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
        x.setGravity(Gravity.CENTER);
        x.setMinWidth(dp(40));
        x.setMinHeight(dp(40));
        x.setPadding(dp(6), 0, dp(6), 0);
        x.setOnClickListener(v -> {
            CoverNotifListener.dismiss(e.key);
            if (itemsBox != null) {
                fillItems(itemsBox.getContext());
            }
        });
        row.addView(x);

        row.setOnClickListener(v -> {
            boolean went = false;
            try {
                if (e.contentIntent != null) {
                    e.contentIntent.send();
                    went = true;
                }
            } catch (Throwable err) {
                Log.w(TAG, "TextFailed", err);
            }
            if (!went) {
                // [comment] contentIntent [comment]：[comment]
                final String pkg = e.pkg;
                new Thread(() -> ShellRunner.run(
                        "monkey -p " + pkg + " -c android.intent.category.LAUNCHER 1", 10),
                        "sidebar-open").start();
            }
            close();
        });
        return row;
    }

    /** [comment]：[comment] + [comment]（runnable [comment] null [comment]Notice[comment]，[comment]） */
    private static View actionRow(Context c, String label, String sub, Runnable action) {
        LinearLayout row = baseRow(c);
        View dot = new View(c);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(action == null ? 0x40FFFFFF : 0xCC7AA2FF);
        dot.setBackground(d);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(7), dp(7));
        dlp.rightMargin = dp(9);
        dlp.gravity = Gravity.CENTER_VERTICAL;
        row.addView(dot, dlp);

        LinearLayout body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(c);
        t.setText(label);
        t.setTextColor(0xF0FFFFFF);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        body.addView(t);
        if (!TextUtils.isEmpty(sub)) {
            TextView s = new TextView(c);
            s.setText(sub);
            s.setTextColor(0x8CFFFFFF);
            s.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
            body.addView(s);
        }
        row.addView(body, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (action != null) {
            row.setOnClickListener(v -> action.run());
        }
        return row;
    }

    private static LinearLayout baseRow(Context c) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(7), dp(8), dp(7));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(2);
        row.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x14FFFFFF);
        bg.setCornerRadius(dp(14));
        row.setBackground(bg);
        // [comment]（API 23+ [comment] setForeground）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            row.setForeground(new RippleDrawable(
                    ColorStateList.valueOf(0x33FFFFFF), null, null));
        }
        return row;
    }

    // ------------------------------------------------------------------ [comment]

    private static void grantNotif(Context c) {
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            String cmd = CoverNotifListener.grantCmd(app);
            String out = ShellRunner.run(cmd, 12);
            Log.i(TAG, "Text => " + out);
            try {
                Thread.sleep(600);
            } catch (InterruptedException ignored) {
            }
            CoverNotifListener.refreshAll();
            UI.post(() -> {
                if (itemsBox != null) {
                    fillItems(app);
                }
            });
        }, "sidebar-grant").start();
    }

    /**
     * [comment]。
     *
     * <p>⚠ v4.9 [comment]<b>[comment]</b>（[comment]："[comment]"，
     * [comment]）。[comment]，[comment]。
     */
    private static void buzz() {
        Context c = appCtx;
        if (c == null) {
            return;
        }
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager)
                        c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                v = vm == null ? null : vm.getDefaultVibrator();
            } else {
                v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (v == null || !v.hasVibrator()) {
                return;
            }
            int amp = v.hasAmplitudeControl() ? 120 : VibrationEffect.DEFAULT_AMPLITUDE;
            v.vibrate(VibrationEffect.createOneShot(BUZZ_MS, amp));
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
    }

    // ------------------------------------------------------------------ [comment]

    // ------------------------------------------------------------------ [comment]

    /** [comment]（[comment]）。[comment]，[comment] alpha [comment] */
    private static View makeHandle(Context c) {
        View v = new View(c);
        GradientDrawable d = new GradientDrawable();
        d.setColor(0xFFFFFFFF);
        d.setCornerRadius(dp(HANDLE_W_DP) / 2f);
        v.setBackground(d);
        v.setAlpha(HANDLE_REST);
        return v;
    }

    /** [comment]（[comment] / [comment]） */
    private static void handleTo(float alpha, float scaleY, long ms) {
        View h = handle;
        if (h == null) {
            return;
        }
        h.animate().cancel();
        h.animate().alpha(alpha).scaleY(scaleY).setDuration(ms).start();
    }

    /** [comment]（[comment]，[comment]） */
    private static void handleNow(float alpha, float scaleY) {
        View h = handle;
        if (h == null) {
            return;
        }
        h.animate().cancel();
        h.setAlpha(alpha);
        h.setScaleY(scaleY);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int dp(float v) {
        return Math.round(v * density);
    }

    /** [comment]"[comment]"[comment]（[comment]，[comment]） */
    public static int themeColor() {
        return Color.argb(0xB8, 0x1B, 0x1B, 0x1F);
    }
}
