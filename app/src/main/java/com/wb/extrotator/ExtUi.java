package com.wb.extrotator;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.function.Consumer;

/**
 * [comment]。
 *
 * <p>[comment]：[comment]（[comment] + Disable），
 * "[comment]"[comment]（[comment] ——
 * {@code dumpsys display} [comment]，[comment]Yes[comment]）。
 * [comment]，[comment]。
 */
public final class ExtUi {

    private ExtUi() {
    }

    /**
     * [comment]（[comment]）。
     *
     * @param cb [comment]；[comment] null
     */
    public static void resolve(final Activity a, final Consumer<ExtScreen.Dev> cb) {
        new Thread(() -> {
            final ExtScreen.Dev dev = ExtScreen.find(a);
            a.runOnUiThread(() -> {
                if (a.isFinishing() || a.isDestroyed()) {
                    return;
                }
                cb.accept(dev);
            });
        }, "extrot-ext-find").start();
    }

    /**
     * 「ⓘ」[comment] —— [comment] / Beta [comment]：[comment] strings.xml [comment] info_* [comment]。
     * [comment]，[comment]。
     */
    public static void info(final Activity a, int iconId, final int titleRes, final int detailRes) {
        View icon = a.findViewById(iconId);
        if (icon == null) {
            return;
        }
        icon.setOnClickListener(v -> {
            if (a.isFinishing() || a.isDestroyed()) {
                return;
            }
            new AlertDialog.Builder(a)
                    .setTitle(titleRes)
                    .setMessage(detailRes)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
    }

    /**
     * [comment]：[comment]「[comment] · [comment] 1080×2640」，[comment]。
     * [comment]Yes[comment] —— [comment]（wm [comment]）[comment]，[comment]。
     */
    public static void title(Activity a, TextView tv, int pageRes, ExtScreen.Dev dev) {
        if (tv == null) {
            return;
        }
        String page = a.getString(pageRes);
        // ⚠ [comment] dev.labelRes()，[comment]"[comment]"：[comment]，[comment]"[comment]"
        tv.setText(dev == null
                ? a.getString(R.string.ext_title_none, page)
                : a.getString(R.string.ext_title_dev, page,
                        a.getString(dev.labelRes()), dev.sizeText()));
    }

    public static void toast(Activity a, String s) {
        Toast.makeText(a, s, Toast.LENGTH_SHORT).show();
    }

    /** [comment]Notice[comment] */
    public static String noScreenHint(Activity a) {
        return a.getString(R.string.ext_no_screen);
    }
}
