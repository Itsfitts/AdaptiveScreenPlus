package com.wb.extrotator;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 「[comment]」[comment] —— [comment] {@link NotificationListenerService}：[comment]「[comment]」[comment]
 * [comment]，[comment] {@link #MAX} [comment]，[comment]。
 *
 * <p><b>[comment]</b>：[comment]Yes「[comment]」，[comment]None[comment]Yes[comment]，
 * [comment] —— [comment]「[comment]」[comment]Yes[comment] Shizuku [comment] {@link #GRANT_CMD} [comment]。
 *
 * <p>[comment]<b>[comment]</b>：[comment]，[comment]，[comment]「[comment] prefs [comment]
 * [comment]」[comment]；[comment]。
 */
public class CoverNotifListener extends NotificationListenerService {

    private static final String TAG = "CoverNotif";

    /** [comment]。[comment]，[comment] */
    public static final int MAX = 12;

    /** [comment] shell [comment]（[comment]）。[comment] = [comment]/[comment] */
    public static String grantCmd(Context c) {
        return "cmd notification allow_listener "
                + c.getPackageName() + "/" + CoverNotifListener.class.getName();
    }

    private static final List<Entry> SNAP = new ArrayList<>();
    /** [comment]，[comment] */
    private static volatile boolean connected;

    /** [comment]（[comment]） */
    public static final class Entry {
        public final String key;
        public final String pkg;
        public final String title;
        public final String text;
        public final long when;
        public final PendingIntent contentIntent;
        /** [comment]（[comment]、[comment]） */
        public final String label;
        /** [comment]；[comment]Yes null，[comment] */
        public final Drawable icon;

        Entry(String key, String pkg, String title, String text, long when,
              PendingIntent contentIntent, String label, Drawable icon) {
            this.key = key;
            this.pkg = pkg;
            this.title = title;
            this.text = text;
            this.when = when;
            this.contentIntent = contentIntent;
            this.label = label;
            this.icon = icon;
        }
    }

    // ------------------------------------------------------------------ [comment]

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        connected = true;
        Log.i(TAG, "Text");
        refreshAll();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        connected = false;
        synchronized (SNAP) {
            SNAP.clear();
        }
        Log.w(TAG, "Text");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // ⚠ [comment]Yes**[comment]**[comment]：[comment] FATAL（None[comment]），
        //    [comment] —— [comment]，[comment]。
        try {
            if (sbn == null || !isShowable(sbn)) {
                return;
            }
            Entry e = toEntry(sbn);
            if (e == null) {
                return;
            }
            synchronized (SNAP) {
                removeKey(e.key);
                SNAP.add(0, e);
                while (SNAP.size() > MAX) {
                    SNAP.remove(SNAP.size() - 1);
                }
            }
            // [comment]
            CoverSidebar.onNotificationsChanged();
        } catch (Throwable t) {
            Log.w(TAG, "Text（Text，Text）", t);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        try {
            if (sbn == null) {
                return;
            }
            synchronized (SNAP) {
                removeKey(keyOf(sbn));
            }
            CoverSidebar.onNotificationsChanged();
        } catch (Throwable t) {
            Log.w(TAG, "Text（Text，Text）", t);
        }
    }

    /** [comment]（[comment]、[comment]） */
    public static void refreshAll() {
        try {
            CoverNotifListener self = Holder.self;
            if (self == null) {
                return;
            }
            StatusBarNotification[] all = self.getActiveNotifications();
            List<Entry> fresh = new ArrayList<>();
            if (all != null) {
                for (StatusBarNotification sbn : all) {
                    if (!isShowable(sbn)) {
                        continue;
                    }
                    Entry e = toEntry(sbn);
                    if (e != null) {
                        fresh.add(e);
                    }
                }
            }
            // [comment]
            Collections.sort(fresh, (a, b) -> Long.compare(b.when, a.when));
            synchronized (SNAP) {
                SNAP.clear();
                SNAP.addAll(fresh.subList(0, Math.min(fresh.size(), MAX)));
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
    }

    /** [comment] */
    public static List<Entry> snapshot() {
        synchronized (SNAP) {
            return new ArrayList<>(SNAP);
        }
    }

    /** [comment]（[comment] × ） */
    public static void dismiss(String key) {
        try {
            CoverNotifListener self = Holder.self;
            if (self != null && key != null) {
                self.cancelNotification(key);
            }
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
        synchronized (SNAP) {
            removeKey(key);
        }
    }

    /**
     * [comment]。
     *
     * <p>⚠ [comment] {@code NotificationManager.getEnabledListenerPackages(Context)} ——
     * [comment]Yes <b>AndroidX</b> [comment] {@code NotificationManagerCompat} [comment]，[comment]，
     * [comment]。[comment]Settings[comment]：{@code enabled_notification_listeners}，
     * [comment]Yes「[comment]/[comment]」[comment]。
     */
    public static boolean granted(Context c) {
        try {
            Context app = c.getApplicationContext();
            String flat = android.provider.Settings.Secure.getString(
                    app.getContentResolver(), "enabled_notification_listeners");
            if (TextUtils.isEmpty(flat)) {
                return false;
            }
            ComponentName me = new ComponentName(app, CoverNotifListener.class);
            return flat.contains(me.flattenToString())
                    || flat.contains(me.flattenToShortString());
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
        return false;
    }

    public static boolean alive() {
        return connected;
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment]（[comment]，[comment] static [comment] lint [comment]） */
    private static final class Holder {
        static CoverNotifListener self;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Holder.self = this;
    }

    @Override
    public void onDestroy() {
        if (Holder.self == this) {
            Holder.self = null;
        }
        connected = false;
        super.onDestroy();
    }

    /**
     * [comment] key [comment]。
     *
     * <p>⚠⚠ [comment]：{@code indexOf} [comment] <b>-1</b>，[comment]
     * {@code List.remove(int)} [comment] -1 Yes<b>[comment] {@code IndexOutOfBoundsException}</b>、
     * [comment]Yes「[comment]」。[comment]「[comment]」[comment]（[comment]、[comment]
     * {@link #isShowable} [comment]，[comment]），[comment]
     * {@code SNAP.remove(indexOf(...))} [comment] FATAL —— [comment]：
     * {@code Index -1 out of bounds for length 0}。
     */
    private static void removeKey(String key) {
        int i = indexOf(key);
        if (i >= 0) {
            SNAP.remove(i);
        }
    }

    private static int indexOf(String key) {
        for (int i = 0; i < SNAP.size(); i++) {
            if (SNAP.get(i).key.equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private static String keyOf(StatusBarNotification sbn) {
        try {
            return sbn.getKey();
        } catch (Throwable t) {
            return "";
        }
    }

    /** [comment]：[comment]、[comment]、[comment] */
    private static boolean isShowable(StatusBarNotification sbn) {
        try {
            Notification n = sbn.getNotification();
            if (n == null) {
                return false;
            }
            if ((n.flags & Notification.FLAG_ONGOING_EVENT) != 0) {
                return false;
            }
            if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) {
                return false;
            }
            String p = sbn.getPackageName();
            return !TextUtils.isEmpty(p) && !p.equals("com.wb.extrotator")
                    && !p.equals("android");
        } catch (Throwable t) {
            return false;
        }
    }

    private static Entry toEntry(StatusBarNotification sbn) {
        try {
            Notification n = sbn.getNotification();
            Bundle x = n.extras;
            CharSequence t = x == null ? null : x.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence c = x == null ? null : x.getCharSequence(Notification.EXTRA_TEXT);
            if (TextUtils.isEmpty(t) && TextUtils.isEmpty(c)) {
                return null;
            }
            String title = TextUtils.isEmpty(t) ? sbn.getPackageName() : t.toString();
            String text = TextUtils.isEmpty(c) ? "" : c.toString();
            String pkg = sbn.getPackageName();
            return new Entry(sbn.getKey(), pkg, title, text,
                    sbn.getPostTime(), n.contentIntent, appLabel(pkg), appIcon(pkg));
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment]：[comment]，[comment] */
    private static final Map<String, String> LABELS = new HashMap<>();
    private static final Map<String, Drawable> ICONS = new HashMap<>();

    private static Context appCtx() {
        CoverNotifListener self = Holder.self;
        return self == null ? null : self.getApplicationContext();
    }

    /**
     * [comment]。[comment]（[comment]、[comment]）[comment]，
     * [comment] —— [comment] null。
     */
    private static String appLabel(String pkg) {
        if (pkg == null) {
            return null;
        }
        synchronized (LABELS) {
            String hit = LABELS.get(pkg);
            if (hit != null) {
                return hit;
            }
        }
        String out = null;
        try {
            Context c = appCtx();
            PackageManager pm = c == null ? null : c.getPackageManager();
            if (pm != null) {
                out = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
            }
        } catch (Throwable t) {
            out = null;
        }
        if (TextUtils.isEmpty(out)) {
            int i = pkg.lastIndexOf('.');
            out = i >= 0 && i + 1 < pkg.length() ? pkg.substring(i + 1) : pkg;
        }
        synchronized (LABELS) {
            LABELS.put(pkg, out);
        }
        return out;
    }

    /** [comment]。[comment] null（[comment]）—— [comment] null [comment] */
    private static Drawable appIcon(String pkg) {
        if (pkg == null) {
            return null;
        }
        synchronized (ICONS) {
            if (ICONS.containsKey(pkg)) {
                return ICONS.get(pkg);
            }
        }
        Drawable out = null;
        try {
            Context c = appCtx();
            PackageManager pm = c == null ? null : c.getPackageManager();
            if (pm != null) {
                out = pm.getApplicationIcon(pkg);
            }
        } catch (Throwable t) {
            out = null;
        }
        synchronized (ICONS) {
            ICONS.put(pkg, out);
        }
        return out;
    }

    /** [comment] */
    public static String statusText(Context c) {
        if (!granted(c)) {
            return "Text —— Text";
        }
        return alive() ? "TextReady" : "Authorized，Text（Text）";
    }
}
