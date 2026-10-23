package com.wb.extrotator;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 「[comment]」[comment]。
 *
 * <p>[comment]Yes[comment]（{@link SecondaryHomeActivity}）—— [comment] / [comment]
 * [comment]，[comment]。
 *
 * <p>⚠ Android 11（API 30）[comment]「[comment]」[comment]，[comment]。[comment]Yes
 * [comment] {@code QUERY_ALL_PACKAGES}，[comment]Yes[comment] {@code <queries>} [comment] MAIN/LAUNCHER
 * intent —— [comment]Yes[comment]，[comment]。
 *
 * <p><b>[comment]</b>：{@link Item#pseudoItems} [comment]「[comment]、[comment]Yes[comment]」
 * （[comment] / [comment] / [comment] / [comment]）。
 * [comment]<b>[comment] {@link #load} [comment]</b> ——
 * [comment]，[comment]（[comment] / [comment] / [comment]）[comment]，
 * [comment]（[comment]Yes[comment]）；[comment]Yes[comment]（{@link #PSEUDO_PREFIX} [comment]），
 * [comment] Activity，[comment]；[comment]
 * {@code getLaunchIntentForPackage} [comment]（[comment]），[comment] null [comment]
 * {@link Item#toIntent()}，[comment]Yes[comment]。[comment]，[comment] Beta [comment] →「[comment]」，
 * [comment] {@link ExtPrefs#pseudoOn}。
 */
public final class AppRepo {

    /** [comment]"[comment]"[comment]。[comment]。 */
    public static final String PSEUDO_PREFIX = "com.wb.extrotator.pseudo.";

    public static final String PSEUDO_TOUCHPAD = PSEUDO_PREFIX + "touchpad";
    /** [comment]—— [comment]、[comment] */
    public static final String PSEUDO_CAST = PSEUDO_PREFIX + "cast";
    /** [comment] —— [comment]（CPU / [comment] / [comment] / [comment]） */
    public static final String PSEUDO_STATUS = PSEUDO_PREFIX + "status";
    /** [comment] —— [comment]：[comment] / [comment] / [comment] / [comment] */
    public static final String PSEUDO_PLAYER = PSEUDO_PREFIX + "player";

    /** [comment]（[comment]） */
    public static final class Item {
        public final String label;
        public final String pkg;
        public final ComponentName component;
        public final Drawable icon;
        /**
         * [comment]"[comment]"；[comment] null。
         *
         * <p>[comment]Yes[comment] {@link #isPseudo()} [comment] —— [comment]Yes
         * {@link #component}，[comment]，[comment]。
         */
        public final Class<?> ownActivity;

        /**
         * [comment]<b>Default</b>[comment]（[comment] false，[comment]）。
         *
         * <p>[comment]Yes"[comment]Settings[comment]"[comment]，[comment] {@link ExtPrefs#pseudoOn}。
         * [comment] false（[comment]"[comment]"）；[comment] true
         * —— [comment]Yes[comment]「[comment]」[comment]，[comment]。
         */
        public final boolean pseudoDefaultOn;

        Item(String label, String pkg, ComponentName component, Drawable icon) {
            this(label, pkg, component, icon, null, false);
        }

        Item(String label, String pkg, ComponentName component, Drawable icon,
             Class<?> ownActivity, boolean pseudoDefaultOn) {
            this.label = label;
            this.pkg = pkg;
            this.component = component;
            this.icon = icon;
            this.ownActivity = ownActivity;
            this.pseudoDefaultOn = pseudoDefaultOn;
        }

        /** Yes[comment]Yes[comment] */
        public boolean isPseudo() {
            return ownActivity != null;
        }

        /** [comment] Intent（[comment] NEW_TASK，[comment] startActivity） */
        public Intent toIntent() {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setComponent(component);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return i;
        }
    }

    /**
     * [comment]，[comment]（[comment]）。
     *
     * <p>[comment]Yes[comment]，[comment]Yes[comment] PackageManager [comment] —— [comment]「[comment]
     * [comment]」[comment]（[comment] ic_pseudo_*.xml [comment]）；[comment] strings.xml，[comment]。
     *
     * <p>⚠ <b>[comment]</b>：[comment]，[comment]。[comment]
     * —— [comment]Yes[comment]，[comment]，[comment]「[comment]」[comment]
     * （[comment] / [comment] / [comment]，[comment]）。
     */
    public static List<Item> pseudoItems(Context c) {
        List<Item> out = new ArrayList<>();
        addVisible(out, c, pseudo(c, PSEUDO_TOUCHPAD, R.string.ext_pad_title,
                R.drawable.ic_pseudo_touchpad, TouchpadActivity.class, false));
        addVisible(out, c, pseudo(c, PSEUDO_CAST, R.string.ext_cast_title,
                R.drawable.ic_pseudo_cast, CastActivity.class, true));
        addVisible(out, c, pseudo(c, PSEUDO_STATUS, R.string.ext_status_title,
                R.drawable.ic_pseudo_status, StatusActivity.class, true));
        addVisible(out, c, pseudo(c, PSEUDO_PLAYER, R.string.ext_player_title,
                R.drawable.ic_pseudo_player, PlayerActivity.class, true));
        return out;
    }

    /** [comment]（[comment] {@link Item#pseudoDefaultOn} [comment]） */
    private static void addVisible(List<Item> out, Context c, Item it) {
        if (ExtPrefs.pseudoOn(c, it.pkg, it.pseudoDefaultOn)) {
            out.add(it);
        }
    }

    private static Item pseudo(Context c, String pkg, int labelRes, int iconRes,
                               Class<?> cls, boolean defaultOn) {
        Drawable icon = null;
        try {
            icon = c.getDrawable(iconRes);
        } catch (Throwable ignored) {
        }
        return new Item(c.getString(labelRes), pkg,
                new ComponentName(c.getPackageName(), cls.getName()), icon, cls, defaultOn);
    }

    private AppRepo() {
    }

    /**
     * [comment]「[comment]」[comment]，[comment]。
     *
     * <p>[comment]（IO），**[comment]**。
     * [comment] —— [comment]Yes Settings [comment] launcher [comment]。
     */
    public static List<Item> load(Context ctx) {
        List<Item> out = new ArrayList<>();
        PackageManager pm;
        try {
            pm = ctx.getPackageManager();
        } catch (Throwable t) {
            return out;
        }
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> ris;
        try {
            ris = pm.queryIntentActivities(main, 0);
        } catch (Throwable t) {
            return out;
        }
        if (ris == null) {
            return out;
        }

        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : ris) {
            if (ri == null || ri.activityInfo == null) {
                continue;
            }
            String pkg = ri.activityInfo.packageName;
            if (pkg == null || !seen.add(pkg)) {
                continue;
            }
            String label;
            try {
                label = String.valueOf(ri.loadLabel(pm));
            } catch (Throwable t) {
                label = pkg;
            }
            Drawable icon = null;
            try {
                icon = ri.loadIcon(pm);
            } catch (Throwable ignored) {
            }
            out.add(new Item(label, pkg,
                    new ComponentName(pkg, ri.activityInfo.name), icon));
        }

        final Collator col = Collator.getInstance(Locale.CHINA);
        try {
            Collections.sort(out, (a, b) -> col.compare(a.label, b.label));
        } catch (Throwable ignored) {
        }
        return out;
    }

    /** [comment]（[comment]，[comment]） */
    public static List<Item> filter(List<Item> src, String q) {
        if (q == null || q.trim().isEmpty()) {
            return new ArrayList<>(src);
        }
        String k = q.trim().toLowerCase(Locale.ROOT);
        List<Item> out = new ArrayList<>();
        for (Item it : src) {
            if (it.label.toLowerCase(Locale.ROOT).contains(k)
                    || it.pkg.toLowerCase(Locale.ROOT).contains(k)) {
                out.add(it);
            }
        }
        return out;
    }
}
