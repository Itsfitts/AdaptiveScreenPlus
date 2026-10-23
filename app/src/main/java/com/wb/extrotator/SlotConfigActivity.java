package com.wb.extrotator;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 「[comment]」[comment] —— [comment]Custom[comment]。
 *
 * <p>[comment] Activity [comment]Yes[comment]：RemoteViews [comment]"[comment]、
 * [comment] PendingIntent"，[comment]。[comment]Yes**[comment]**（[comment]
 * [comment]），[comment] —— [comment]、[comment]、
 * Cancel[comment]。
 *
 * <p>[comment] {@link SlotWidgetProvider#refreshAll} [comment]，
 * [comment]，[comment]。
 *
 * <p>⚠ [comment]Yes**[comment]**[comment] {@link SlotConfig} [comment]；[comment]（[comment]
 * "[comment]"[comment]）。
 */
public class SlotConfigActivity extends Activity {

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setTitle(R.string.widget_slot_cfg_title);
        build();
    }

    private void build() {
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * d);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView hint = new TextView(this);
        hint.setText(R.string.widget_slot_cfg_hint);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        hint.setAlpha(0.72f);
        hint.setPadding(0, 0, 0, pad / 2);
        root.addView(hint);

        for (int i = 0; i < SlotConfig.SLOTS; i++) {
            final int idx = i;
            TextView row = new TextView(this);
            row.setText(getString(R.string.widget_slot_cfg_row, i + 1, SlotConfig.label(this, i)));
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            row.setPadding(pad / 2, pad, pad / 2, pad);
            row.setBackgroundResource(android.R.drawable.list_selector_background);
            row.setOnClickListener(v -> pick(idx));
            root.addView(row);
        }
        setContentView(root);
    }

    private void pick(final int idx) {
        final String[] keys = SlotConfig.choices(this);
        final String[] names = new String[keys.length + 1];
        names[0] = getString(R.string.widget_slot_cfg_none);
        for (int i = 0; i < keys.length; i++) {
            names[i + 1] = QuickActions.label(this, keys[i]);
        }
        // [comment]
        int cur = 0;
        String now = SlotConfig.slot(this, idx);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(now)) {
                cur = i + 1;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.widget_slot_cfg_pick, idx + 1))
                .setSingleChoiceItems(names, cur, (dlg, which) -> {
                    SlotConfig.setSlot(this, idx, which == 0 ? "" : keys[which - 1]);
                    dlg.dismiss();
                    build();
                    SlotWidgetProvider.refreshAll(this,
                            SlotWidgetProvider.class, SlotCoverWidgetProvider.class);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
