package com.wb.extrotator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * 「[comment]」[comment] —— [comment]。
 *
 * <p>[comment]，[comment]，[comment]；[comment]
 * 「[comment] + [comment]」，[comment]，[comment]<b>[comment]</b>[comment]。
 *
 * <p>[comment]（{@link CoverLauncherActivity}）[comment]：[comment]「[comment]、[comment]」，[comment]
 * 「[comment]、[comment]」，[comment]，[comment]Yes[comment]。
 *
 * <p>[comment]：[comment]，[comment]<b>[comment]</b>[comment] —— [comment]，
 * [comment]。
 */
public class CoverSearchActivity extends Activity {

    private static final String TAG = "CoverSearchPage";

    private EditText etInput;
    private TextView tvState;
    private GridView gvResults;

    private final List<AppRepo.Item> all = new ArrayList<>();
    private final List<AppRepo.Item> shown = new ArrayList<>();
    private ResultAdapter adapter;

    private int displayId = 0;
    private boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover_search);

        etInput = findViewById(R.id.etSearchInput);
        tvState = findViewById(R.id.tvSearchState);
        gvResults = findViewById(R.id.gvSearch);

        try {
            displayId = getWindowManager().getDefaultDisplay().getDisplayId();
        } catch (Throwable ignored) {
        }

        // [comment]Yes[comment] —— [comment]，[comment] dp [comment]，
        // [comment]"[comment]"[comment]。
        // [comment]（item_search_result.xml），[comment]。

        adapter = new ResultAdapter();
        gvResults.setAdapter(adapter);
        gvResults.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < shown.size()) {
                launchHere(shown.get(position));
            }
        });

        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        View close = findViewById(R.id.btnSearchClose);
        if (close != null) {
            close.setOnClickListener(v -> finish());
        }

        // [comment]**[comment]**[comment]：[comment] 339dp [comment]，[comment] 460px [comment]，
        // [comment] dp，[comment]，[comment]
        // （[comment] process/coverlaunch/13_search_on_cover.png）。
        // [comment]「[comment]」[comment]（[comment]、[comment]），[comment]。
        // adjustResize [comment]：[comment]。
        try {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } catch (Throwable ignored) {
        }

        load();
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]。{@link SecondaryLauncher} [comment]，shell Yes[comment]，
     * [comment]，[comment]。[comment] Shizuku [comment]（[comment]）。
     */
    private void launchHere(AppRepo.Item it) {
        Toast.makeText(this, "Text " + displayId + " Text「" + it.label + "」…",
                Toast.LENGTH_SHORT).show();

        final int display = displayId;
        new Thread(() -> {
            Intent li = null;
            try {
                li = getPackageManager().getLaunchIntentForPackage(it.pkg);
            } catch (Throwable ignored) {
            }
            if (li == null) {
                li = it.toIntent();
            }
            boolean ok = SecondaryLauncher.launch(this, display, li);
            Log.i(TAG, "Text " + it.pkg + " => " + ok);
            if (ok) {
                LauncherPrefs.bump(this, it.pkg);
            }
            runOnUiThread(this::finish);
        }, "extrot-search-launch").start();
    }

    // ------------------------------------------------------------------ [comment]

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        tvState.setText("Text…");
        new Thread(() -> {
            final List<AppRepo.Item> list = AppRepo.load(this);
            runOnUiThread(() -> {
                loading = false;
                all.clear();
                all.addAll(list);
                applyFilter();
            });
        }, "extrot-search-load").start();
    }

    private void applyFilter() {
        String q = etInput.getText() == null ? "" : etInput.getText().toString();
        shown.clear();
        shown.addAll(AppRepo.filter(all, q));
        adapter.notifyDataSetChanged();

        if (loading) {
            tvState.setText("Text…");
        } else if (all.isEmpty()) {
            tvState.setText("Text");
        } else if (q.trim().isEmpty()) {
            // [comment]，[comment]
            tvState.setText(all.size() + " Text · Text");
        } else if (shown.isEmpty()) {
            tvState.setText(getString(R.string.launcher_search_empty));
        } else {
            tvState.setText(shown.size() + " Text");
        }
    }

    // ------------------------------------------------------------------ [comment]

    private class ResultAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return shown.size();
        }

        @Override
        public Object getItem(int position) {
            return shown.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = getLayoutInflater().inflate(R.layout.item_search_result, parent, false);
            }
            AppRepo.Item it = shown.get(position);
            ImageView ic = v.findViewById(R.id.srIcon);
            TextView tv = v.findViewById(R.id.srLabel);
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
