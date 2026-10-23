package com.wb.extrotator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 「[comment]」[comment] —— [comment] Beta [comment]「[comment]」[comment]。
 *
 * <p>[comment]，[comment]：[comment] = [comment]「[comment]」[comment]
 * （[comment]，[comment]、[comment]），[comment] = [comment]。
 * [comment] {@link CoverRepair}。
 *
 * <p>[comment]Yes[comment] ListView [comment]：[comment] Beta [comment] ScrollView [comment]、
 * [comment]，[comment]。
 *
 * <p>⚠ [comment]：[comment]Yes「[comment] → [comment] → [comment]」，
 * [comment]，[comment] {@link #pool}，[comment] {@link #busy} [comment]。
 */
public class RepairActivity extends Activity {

    private static final String TAG = "RepairPage";

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    private ListView list;
    private TextView tvState;
    private TextView btnAll;
    private TextView btnReset;

    private final List<AppRepo.Item> apps = new ArrayList<>();
    /** [comment] */
    private final Set<String> on = new HashSet<>();

    private RowAdapter adapter;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repair);

        View back = findViewById(R.id.btnRepairBack);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        list = findViewById(R.id.lvRepair);
        tvState = findViewById(R.id.tvRepairState);
        btnAll = findViewById(R.id.btnRepairAll);
        btnReset = findViewById(R.id.btnRepairReset);

        adapter = new RowAdapter();
        list.setAdapter(adapter);

        bindInfo(R.id.iRepairList, R.string.info_repair_list);

        if (btnAll != null) {
            btnAll.setOnClickListener(v -> oneKey(true));
        }
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> confirmReset());
        }

        if (!ShellRunner.isReady()) {
            toast("Shizuku Not Ready，Text");
        }
        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // [comment]（MultiStar / [comment]）[comment]，[comment]Yes[comment]
        reload();
    }

    @Override
    protected void onDestroy() {
        pool.shutdownNow();
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ==================================================================== [comment]

    private void reload() {
        setBusy(true);
        tvState.setText("Text…");
        pool.execute(() -> {
            final List<AppRepo.Item> loaded = new ArrayList<>();
            for (AppRepo.Item it : AppRepo.load(getApplicationContext())) {
                if (it != null && !it.isPseudo() && !TextUtils.isEmpty(it.pkg)) {
                    loaded.add(it);
                }
            }
            CoverRepair.Snapshot snap = CoverRepair.read();
            final Set<String> now = new HashSet<>(CoverRepair.packages(snap));
            final boolean readable = snap != null && snap.valid();
            ui.post(() -> {
                apps.clear();
                apps.addAll(loaded);
                on.clear();
                on.addAll(now);
                adapter.notifyDataSetChanged();
                setBusy(false);
                if (!readable) {
                    tvState.setText("Text（Shizuku Text？）—— Text，Text");
                } else {
                    updateState();
                }
            });
        });
    }

    private void updateState() {
        int hit = 0;
        for (AppRepo.Item it : apps) {
            if (on.contains(it.pkg)) {
                hit++;
            }
        }
        String snap = CoverRepair.hasSnapshot(this)
                ? "Text，TextReset" : "Text（Text）";
        tvState.setText("Text " + on.size() + " Text · Text " + apps.size()
                + " Text（Text " + hit + " TextEnable）\n" + snap);
    }

    // ==================================================================== [comment]

    private void toggle(AppRepo.Item it, boolean want) {
        if (busy) {
            return;
        }
        setBusy(true);
        final String pkg = it.pkg;
        pool.execute(() -> {
            String msg = CoverRepair.apply(getApplicationContext(),
                    want ? Collections.singletonList(pkg) : Collections.<String>emptyList(),
                    want ? Collections.<String>emptyList() : Collections.singletonList(pkg));
            Log.i(TAG, (want ? "Enable " : "Disable ") + pkg + " → " + msg);
            ui.post(() -> {
                setBusy(false);
                if (msg.startsWith("ERR")) {
                    toast(msg);
                    reload();          // TextFailedText，Text
                } else {
                    if (want) {
                        on.add(pkg);
                    } else {
                        on.remove(pkg);
                    }
                    updateState();
                }
            });
        });
    }

    /** [comment] / [comment]Reset */
    private void oneKey(final boolean fix) {
        if (busy) {
            return;
        }
        if (!fix && !CoverRepair.hasSnapshot(this)) {
            toast("Text，Text");
            return;
        }
        setBusy(true);
        tvState.setText(fix ? "Text…" : "Text…");
        pool.execute(() -> {
            String msg = fix ? CoverRepair.applyAll(getApplicationContext())
                    : CoverRepair.restore(getApplicationContext());
            Log.i(TAG, (fix ? "Text → " : "TextReset → ") + msg);
            ui.post(() -> {
                setBusy(false);
                toast(msg);
                reload();
            });
        });
    }

    private void confirmReset() {
        if (busy) {
            return;
        }
        if (!CoverRepair.hasSnapshot(this)) {
            toast("Text，Text");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("TextReset")
                .setMessage("Text（Text）。\n\n"
                        + "⚠ Text MultiStar Text。")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Text", (d, w) -> oneKey(false))
                .show();
    }

    // ==================================================================== [comment]

    private void setBusy(boolean b) {
        busy = b;
        if (btnAll != null) {
            btnAll.setEnabled(!b);
            btnAll.setAlpha(b ? 0.5f : 1f);
        }
        if (btnReset != null) {
            btnReset.setEnabled(!b);
            btnReset.setAlpha(b ? 0.5f : 1f);
        }
    }

    /** [comment] = [comment] + [comment]。[comment]，[comment]（[comment]） */
    private final class RowAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return apps.size();
        }

        @Override
        public Object getItem(int position) {
            return apps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View reuse, ViewGroup parent) {
            LinearLayout row;
            TextView tv;
            Switch sw;
            if (reuse instanceof LinearLayout && reuse.getTag() != null) {
                row = (LinearLayout) reuse;
                tv = (TextView) row.getChildAt(0);
                sw = (Switch) row.getChildAt(1);
            } else {
                row = new LinearLayout(RepairActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                int pad = dp(9);
                row.setPadding(0, pad, 0, pad);
                row.setTag(Boolean.TRUE);

                tv = new TextView(RepairActivity.this);
                tv.setTextColor(0xFFF0F0F0);
                tv.setTextSize(14f);
                tv.setSingleLine(true);
                tv.setEllipsize(TextUtils.TruncateAt.END);
                tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                sw = new Switch(RepairActivity.this);
                tint(sw);

                row.addView(tv);
                row.addView(sw);
            }
            final AppRepo.Item it = apps.get(position);
            tv.setText(it.label);
            // [comment] —— [comment]"[comment]"，[comment]
            sw.setOnCheckedChangeListener(null);
            sw.setChecked(on.contains(it.pkg));
            sw.setOnCheckedChangeListener((b, checked) -> toggle(it, checked));
            return row;
        }
    }

    /** [comment] Beta [comment]（[comment]Default[comment]/[comment]） */
    private void tint(Switch sw) {
        try {
            ColorStateList thumb = getResources().getColorStateList(R.color.beta_switch_thumb);
            ColorStateList track = getResources().getColorStateList(R.color.beta_switch_track);
            sw.setThumbTintList(thumb);
            sw.setTrackTintList(track);
        } catch (Throwable ignored) {
        }
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    private void bindInfo(int iconId, int detailRes) {
        View icon = findViewById(iconId);
        if (icon == null) {
            return;
        }
        icon.setOnClickListener(v -> {
            if (isFinishing()) {
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Text")
                    .setMessage(detailRes)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
