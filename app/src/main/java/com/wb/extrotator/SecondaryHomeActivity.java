package com.wb.extrotator;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * [comment] —— [comment]（[comment]）[comment]。
 *
 * <p>[comment]：{@code cmd device_state state 4}（CONCURRENT）[comment]，[comment]
 * [comment] SubHomeActivity，[comment]Yes[comment]、[comment]「[comment]」；[comment] Activity [comment]Yes
 * [comment]。
 *
 * <p>[comment]：[comment] {@code getDefaultDisplay()} [comment] id，[comment]
 * {@link SecondaryLauncher} [comment]<b>[comment]</b>[comment] —— [comment]，[comment]。
 *
 * <p>[comment]Yes IO，[comment]。
 */
public class SecondaryHomeActivity extends Activity {

    private GridView gvApps;
    private TextView tvClock, tvDate, tvBattery, tvEmpty;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final List<AppRepo.Item> items = new ArrayList<>();
    private AppAdapter adapter;

    /** [comment] Activity [comment] */
    private int displayId = 0;
    private boolean loading = false;
    private int tick = 0;

    private final SimpleDateFormat fmtTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat fmtDate = new SimpleDateFormat("MTextdText EEEE", Locale.CHINA);

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            // [comment]，10 [comment]
            if (tick % 10 == 0) {
                updateBattery();
            }
            tick++;
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary_home);

        gvApps = findViewById(R.id.gvApps);
        tvClock = findViewById(R.id.tvClock);
        tvDate = findViewById(R.id.tvDate);
        tvBattery = findViewById(R.id.tvBattery);
        tvEmpty = findViewById(R.id.tvEmpty);

        try {
            displayId = getWindowManager().getDefaultDisplay().getDisplayId();
        } catch (Throwable ignored) {
        }

        // [comment]：[comment] 748px/340dpi ≈ 352dp → 4 [comment]；[comment]
        int wDp = 360;
        try {
            wDp = getResources().getConfiguration().screenWidthDp;
        } catch (Throwable ignored) {
        }
        int cols = Math.max(3, Math.min(6, wDp / 80));
        gvApps.setNumColumns(cols);

        adapter = new AppAdapter();
        gvApps.setAdapter(adapter);
        gvApps.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= items.size()) {
                return;
            }
            launch(items.get(position));
        });

        findViewById(R.id.btnSHomeRefresh).setOnClickListener(v -> load());
        findViewById(R.id.btnSHomeHome).setOnClickListener(v -> {
            if (SecondaryLauncher.goHome(this, displayId)) {
                toast("Text");
            } else {
                toast("Text Shizuku Text");
            }
        });
        findViewById(R.id.btnSHomeExit).setOnClickListener(v -> finish());

        updateClock();
        updateBattery();
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(clockTick);
        handler.post(clockTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockTick);
    }

    // ------------------------------------------------------------------ [comment]

    private void launch(AppRepo.Item it) {
        if (it.isPseudo()) {
            // [comment]Yes[comment]，[comment]/[comment] ——
            // [comment]，[comment] onStart
            // （[comment] 19:24:29.917、[comment] 19:24:29.984，[comment] 70ms）。[comment] ExtControlMode。
            ExtControlMode.arm(20000L);
        }
        Intent li = null;
        try {
            li = getPackageManager().getLaunchIntentForPackage(it.pkg);
        } catch (Throwable ignored) {
        }
        if (li == null) {
            li = it.toIntent();
        }
        if (SecondaryLauncher.launch(this, displayId, li)) {
            toast("Text " + displayId + " Text「" + it.label + "」");
        } else {
            toast("TextFailed：Text Shizuku Text");
        }
    }

    // ------------------------------------------------------------------ [comment]

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("Text…");
        new Thread(() -> {
            final List<AppRepo.Item> list = AppRepo.load(this);
            // [comment]：[comment]，[comment]。
            // [comment] HomePrefs.sort [comment]，[comment]Yes[comment]。
            HomePrefs.sort(this, list);
            // [comment]：[comment]Yes[comment]，AppRepo.load [comment]，
            // [comment] —— [comment]，[comment]"[comment]"，
            // [comment]，[comment]。
            list.addAll(0, AppRepo.pseudoItems(this));
            runOnUiThread(() -> {
                loading = false;
                items.clear();
                items.addAll(list);
                adapter.notifyDataSetChanged();
                if (items.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Text。\nTextYes release Text，Text <queries> Text MAIN/LAUNCHER。");
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            });
        }, "extrot-cover-apps").start();
    }

    // ------------------------------------------------------------------ [comment] / [comment]

    private void updateClock() {
        Date now = new Date();
        tvClock.setText(fmtTime.format(now));
        tvDate.setText(fmtDate.format(now));
    }

    private void updateBattery() {
        try {
            Intent b = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (b == null) {
                return;
            }
            int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level < 0 || scale <= 0) {
                tvBattery.setText("—");
                return;
            }
            int pct = (int) (level * 100f / scale);
            int status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
            tvBattery.setText(pct + "%" + (charging ? " ⚡" : ""));
        } catch (Throwable t) {
            tvBattery.setText("—");
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ [comment]

    private class AppAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = getLayoutInflater().inflate(R.layout.item_app_grid, parent, false);
            }
            AppRepo.Item it = items.get(position);
            ImageView ic = v.findViewById(R.id.ivGridIcon);
            TextView tv = v.findViewById(R.id.tvGridName);
            try {
                ic.setImageDrawable(it.icon);
            } catch (Throwable ignored) {
                ic.setImageDrawable(null);
            }
            tv.setText(it.label);
            return v;
        }
    }
}
