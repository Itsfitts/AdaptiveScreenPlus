package com.wb.extrotator;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 「Custom[comment]」[comment] —— [comment]。
 *
 * <p>[comment]，[comment]：
 * <ul>
 *   <li>[comment] {@link #EXTRA_SLOT}（[comment]Yes[comment]）→ [comment]，[comment]；</li>
 *   <li>[comment]（[comment]"[comment]Settings"[comment]）→ [comment]，[comment]。</li>
 * </ul>
 *
 * <p>[comment] Activity [comment]Yes[comment]：RemoteViews [comment]"[comment]、
 * [comment] PendingIntent"，[comment]。[comment]Yes[comment]（[comment]），
 * [comment] —— [comment]、[comment]、Cancel[comment]。
 *
 * <p>[comment] {@link CustomCoverWidgetProvider#refreshAll} [comment]，[comment]
 * [comment]，[comment]。
 *
 * <p>⚠ [comment]Yes<b>[comment]</b>[comment] {@link CustomWidgetCfg} [comment]；[comment]（[comment]
 * "[comment]"[comment]）。
 */
public class CustomCfgActivity extends Activity {

    /** [comment]Yes[comment]（0 [comment]）；[comment]Yes"[comment]" */
    public static final String EXTRA_SLOT = "slot";

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setTitle(R.string.widget_custom_cfg_title);
        int direct = -1;
        try {
            direct = getIntent().getIntExtra(EXTRA_SLOT, -1);
        } catch (Throwable ignored) {
        }
        if (direct >= 0 && direct < CustomWidgetCfg.SLOTS) {
            pick(direct, true);
        } else {
            build();
        }
    }

    /** [comment]（[comment]） */
    private void build() {
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * d);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView hint = new TextView(this);
        hint.setText(R.string.widget_custom_cfg_hint);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        hint.setAlpha(0.72f);
        hint.setPadding(0, 0, 0, pad / 2);
        root.addView(hint);

        for (int i = 0; i < CustomWidgetCfg.SLOTS; i++) {
            final int idx = i;
            TextView row = new TextView(this);
            row.setText(getString(R.string.widget_custom_cfg_row, i + 1, CustomWidgetCfg.label(this, i)));
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            row.setPadding(pad / 2, pad, pad / 2, pad);
            row.setBackgroundResource(android.R.drawable.list_selector_background);
            row.setOnClickListener(v -> pick(idx, false));
            root.addView(row);
        }
        setContentView(root);
    }

    /**
     * [comment] idx [comment]。
     *
     * @param finishAfter [comment]（[comment]）；false = [comment]
     */
    private void pick(final int idx, final boolean finishAfter) {
        final String[] keys = SlotConfig.choices(this);
        final String[] names = new String[keys.length + 1];
        names[0] = getString(R.string.widget_slot_cfg_none);
        for (int i = 0; i < keys.length; i++) {
            names[i + 1] = QuickActions.label(this, keys[i]);
        }
        // [comment]
        int cur = 0;
        String now = CustomWidgetCfg.slot(this, idx);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(now)) {
                cur = i + 1;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.widget_custom_cfg_pick, idx + 1))
                .setSingleChoiceItems(names, cur, (dlg, which) -> {
                    CustomWidgetCfg.setSlot(this, idx, which == 0 ? "" : keys[which - 1]);
                    dlg.dismiss();
                    CustomCoverWidgetProvider.refreshAll(this);
                    if (finishAfter) {
                        finish();
                    } else {
                        build();
                    }
                })
                .setNegativeButton(android.R.string.cancel,
                        (d, w) -> {
                            if (finishAfter) {
                                finish();
                            }
                        })
                .setOnCancelListener(d -> {
                    if (finishAfter) {
                        finish();
                    }
                })
                .show();
    }
}
