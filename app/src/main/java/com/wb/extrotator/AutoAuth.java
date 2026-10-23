package com.wb.extrotator;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * [comment]<b>[comment]</b>。
 *
 * <p>⚠ <b>Default[comment]</b>：[comment]<b>[comment]</b>[comment]「Allow / Confirm」[comment]。[comment]
 * {@link AdbWifi#unfoldRoute} [comment] {@link #arm()}、[comment] {@link #disarm()}。
 *
 * <p>[comment]：[comment] {@code android:id/sec_alwaysUse}（[comment]、[comment]），
 * [comment] {@code android:id/button1}（[comment] id，[comment]）；[comment]
 * [comment]<b>[comment]</b>「Allow / Allow / Confirm / OK」[comment] —— [comment]，None[comment]Settings[comment]
 * 「None[comment]」[comment]，[comment] contains [comment]Settings[comment]。
 *
 * <p>⚠ [comment] {@code getRootInActiveWindow()}；{@code event.getSource()} [comment]Yes「[comment]
 * [comment]」，[comment]。
 */
public final class AutoAuth {

    private static final String TAG = "AutoAuth";

    /**
     * [comment]：[comment]，[comment]None[comment]，
     * [comment]Yes[comment]。
     */
    private static final long DELAY_MS = 500L;

    /** [comment]，[comment]Settings[comment]"[comment]"[comment] CPU [comment] */
    private static final long THROTTLE_MS = 400L;

    /** [comment]，[comment] */
    private static final int MAX_ANCESTOR = 6;

    /** [comment] {@code android:id/button1} [comment]（[comment]） */
    private static final String[] POSITIVE_TEXTS = {"Allow", "Allow", "Confirm", "OK"};

    private static final Handler H = new Handler(Looper.getMainLooper());
    private static final Runnable CLICK = AutoAuth::click;

    private static volatile AccessibilityService sSvc;
    private static volatile long sLastAt;

    /**
     * [comment]。⚠ [comment]：[comment]Yes[comment]「Allow / Confirm」[comment]。
     */
    private static volatile boolean sArmed;

    private AutoAuth() {
    }

    /** None[comment] / [comment] {@link VolumeKeyService} [comment]。 */
    static void attach(AccessibilityService svc) {
        sSvc = svc;
        Log.i(TAG, "Text（Text）");
    }

    static void detach() {
        sSvc = null;
        H.removeCallbacks(CLICK);
    }

    /** None[comment] —— {@link AdbWifi} [comment]"[comment]"[comment]。 */
    public static boolean isReady() {
        return sSvc != null;
    }

    /** [comment]：[comment]。[comment] {@link AdbWifi#unfoldRoute} [comment]。 */
    public static void arm() {
        sArmed = true;
        sLastAt = 0L;
        Log.i(TAG, "TextEnable（TextNoneText）");
    }

    /**
     * [comment]：[comment]，[comment]Cancel[comment]。
     * {@link AdbWifi#unfoldRoute} [comment]None[comment]。
     */
    public static void disarm() {
        sArmed = false;
        H.removeCallbacks(CLICK);
        Log.i(TAG, "TextDisable");
    }

    /**
     * [comment] {@link VolumeKeyService#onAccessibilityEvent} [comment]。
     *
     * <p>⚠ [comment] {@code typeWindowContentChanged}：[comment]Yes[comment]，
     * [comment] {@code typeWindowStateChanged} [comment]"[comment]、[comment]"。
     */
    static void handle(AccessibilityEvent ev, AccessibilityService svc) {
        if (ev == null || svc == null) {
            return;
        }
        sSvc = svc;
        if (!sArmed) {
            return;   // TextNoneText => Text、Text，Text
        }
        int type = ev.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }
        CharSequence p = ev.getPackageName();
        if (p != null && isExcluded(p.toString())) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - sLastAt < THROTTLE_MS) {
            return;
        }
        sLastAt = now;
        H.postDelayed(CLICK, DELAY_MS);
    }

    private static boolean isExcluded(String pkg) {
        // [comment]（[comment]）；Shizuku [comment]（[comment]）
        return "com.wb.extrotator".equals(pkg) || "moe.shizuku.privileged.api".equals(pkg);
    }

    private static void click() {
        AccessibilityService svc = sSvc;
        if (svc == null || !sArmed) {
            return;   // Text 500ms Text disarm Text
        }
        try {
            AccessibilityNodeInfo root = svc.getRootInActiveWindow();
            if (root == null) {
                return;
            }

            // ① [comment]「[comment]Allow」——[comment]Yes[comment]，[comment]
            AccessibilityNodeInfo cb = findById(root, "android:id/sec_alwaysUse");
            if (cb == null) {
                cb = findById(root, "android:id/alwaysUse");
            }
            if (cb != null && cb.isCheckable() && !cb.isChecked()) {
                if (cb.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Log.i(TAG, "Text「TextAllow」: " + cb.getViewIdResourceName());
                }
            }

            // ② [comment]：[comment] id，[comment]
            AccessibilityNodeInfo ok = findById(root, "android:id/button1");
            String how = "button1";
            if (ok == null) {
                ok = findByText(root, POSITIVE_TEXTS);
                how = "Text";
            }
            if (ok == null) {
                return;   // Text"Allow"Text ⇒ TextYesText，Text
            }

            String what = how + "=" + text(ok);
            if (performClick(ok)) {
                Log.i(TAG, "Text: " + what);
            } else {
                Log.w(TAG, "Text: " + what);
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
    }

    /** [comment] view id [comment] */
    private static AccessibilityNodeInfo findById(AccessibilityNodeInfo node, String id) {
        if (node == null) {
            return null;
        }
        try {
            if (id.equals(node.getViewIdResourceName())) {
                return node;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo hit = findById(node.getChild(i), id);
                if (hit != null) {
                    return hit;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** [comment] / None[comment]**[comment]**[comment]（[comment]Yes contains） */
    private static AccessibilityNodeInfo findByText(AccessibilityNodeInfo node, String[] wants) {
        if (node == null) {
            return null;
        }
        try {
            if (matches(node.getText(), wants) || matches(node.getContentDescription(), wants)) {
                return node;
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo hit = findByText(node.getChild(i), wants);
                if (hit != null) {
                    return hit;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean matches(CharSequence cs, String[] wants) {
        if (cs == null) {
            return false;
        }
        String s = cs.toString().trim();
        for (String w : wants) {
            if (w.equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * [comment]。[comment]（[comment] clickable [comment]），
     * [comment]、[comment] {@link #MAX_ANCESTOR} [comment]。
     */
    private static boolean performClick(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo cur = node;
        for (int i = 0; i <= MAX_ANCESTOR && cur != null; i++) {
            try {
                if (cur.isClickable() && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true;
                }
                cur = cur.getParent();
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    private static String text(AccessibilityNodeInfo n) {
        try {
            CharSequence t = n.getText();
            if (t == null) {
                t = n.getContentDescription();
            }
            String s = t == null ? "?" : t.toString();
            String id = n.getViewIdResourceName();
            return id == null ? s : s + " (" + id + ")";
        } catch (Throwable ignored) {
            return "?";
        }
    }
}
