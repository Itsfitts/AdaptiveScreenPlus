package com.wb.extrotator;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 「[comment]」[comment] —— [comment]（[comment]）。
 *
 * <p><b>[comment]</b>：[comment]Yes「[comment]」，[comment]、[comment]，[comment] RemoteViews [comment]；
 * [comment]Yes「[comment]Settings[comment]」，[comment] Activity，[comment] —— [comment]Yes[comment]、[comment]、[comment]、[comment]、[comment]。
 *
 * <p>[comment]：[comment] {@code getDefaultDisplay().getDisplayId()} [comment]，
 * [comment] {@link SecondaryLauncher} [comment]<b>[comment]</b>[comment]，[comment]Yes[comment]。
 *
 * <p><b>[comment]</b>：[comment] = [comment] / [comment]。[comment]Yes[comment]
 * （[comment]）；[comment] —— [comment]<b>[comment]</b> = [comment]
 * （[comment]，[comment] {@link DragGrid}），[comment]<b>[comment]</b> = [comment]「[comment] / [comment]」
 * （{@link DragGrid.OrderListener#onHoldReleased}）。
 *
 * <p><b>[comment]</b>：[comment] = [comment]（[comment]）+ [comment]（[comment]），[comment]，
 * [comment]；[comment]「[comment]」[comment]（{@link DragGrid#setDropLimit}）。
 *
 * <p><b>[comment]</b>：[comment]，[comment]（{@link #applyLook}），
 * [comment]。
 *
 * <p><b>[comment]</b>：[comment]，[comment] {@link LauncherPhoto}。
 * [comment]，[comment]「[comment]」Yes[comment]（[comment]、[comment]）。
 */
public class CoverLauncherActivity extends Activity implements DragGrid.OrderListener {

    private static final String TAG = "CoverLauncherPage";

    /**
     * [comment] ⇅ [comment] extra：[comment]。
     * [comment]"[comment]"[comment]"[comment]"Yes[comment]，[comment]Yes[comment]。
     */
    public static final String EXTRA_OPEN_SORT = "open_sort";

    /** chip [comment]：[comment] */
    private interface OnPick {
        void on(int value);
    }

    /** chip [comment] */
    private interface IntGetter {
        int get();
    }

    private DragGrid gvApps;
    private EditText etSearch;
    private TextView tvCount;
    private TextView tvHint;
    private ImageView btnFav;
    private ImageView btnLook;
    private ImageView btnPhoto;
    private LinearLayout lookStrip;
    private View btnDone;

    private final List<AppRepo.Item> all = new ArrayList<>();
    private final List<AppRepo.Item> shown = new ArrayList<>();

    /** [comment]。rebuild [comment] —— [comment]，[comment] pref [comment] */
    private final Set<String> favs = new HashSet<>();
    private PickAdapter adapter;

    private boolean loading = false;

    /** [comment]Yes[comment]Yes[comment]（[comment]Yes[comment]，Yes[comment] —— [comment]） */
    private boolean lookOpen = false;

    /** [comment]，[comment] */
    private boolean lookBuilt = false;

    /** "[comment]"[comment] —— [comment]、[comment] */
    private int selectedCount = 0;

    /** [comment]Yes[comment]Yes[comment]（[comment] ⇅ [comment]） */
    private boolean wantSortDialog = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover_launcher);

        gvApps = findViewById(R.id.gvPick);
        etSearch = findViewById(R.id.etPickSearch);
        tvCount = findViewById(R.id.tvPickCount);
        tvHint = findViewById(R.id.tvPickHint);
        btnFav = findViewById(R.id.btnPickFav);
        btnLook = findViewById(R.id.btnPickLook);
        btnPhoto = findViewById(R.id.btnPickPhoto);
        lookStrip = findViewById(R.id.lookStrip);
        btnDone = findViewById(R.id.btnPickDone);

        wantSortDialog = "1".equals(extra(EXTRA_OPEN_SORT));

        adapter = new PickAdapter();
        gvApps.setAdapter(adapter);
        gvApps.setDragHost(findViewById(R.id.gridHost));
        gvApps.setOrderListener(this);

        // [comment] = [comment] / [comment]（v4.14 [comment]"[comment]"[comment]，[comment]）
        gvApps.setOnItemClickListener((parent, view, position, id) -> onTap(position));
        // ⚠ [comment] setOnItemLongClickListener —— [comment] DragGrid [comment]，
        //   [comment]。

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                rebuild();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnFav.setOnClickListener(v -> toggleFavOnly());

        /*
         * [comment]「ⓘ」：[comment]。
         *
         * [comment]（Edit / [comment] / [comment]）[comment]Yes「[comment]Yes[comment]」[comment]，[comment]
         * [comment] —— [comment]，[comment]。[comment] strings.xml [comment] info_launcher_page。
         */
        View info = findViewById(R.id.iPickTitle);
        if (info != null) {
            info.setOnClickListener(v -> {
                if (isFinishing()) {
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle(R.string.launcher_title)
                        .setMessage(R.string.info_launcher_page)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            });
        }

        View look = findViewById(R.id.btnPickLook);
        if (look != null) {
            look.setOnClickListener(v -> toggleLook());
        }
        View sort = findViewById(R.id.btnPickSort);
        if (sort != null) {
            sort.setOnClickListener(v -> showSort());
        }
        View close = findViewById(R.id.btnPickClose);
        if (close != null) {
            close.setOnClickListener(v -> finish());
        }
        View photo = findViewById(R.id.btnPickPhoto);
        if (photo != null) {
            photo.setOnClickListener(v -> onPhotoTap());
        }

        View importBtn = findViewById(R.id.btnPickImport);
        if (importBtn != null) {
            importBtn.setOnClickListener(v -> showIoDialog());
        }
        View clearBtn = findViewById(R.id.btnPickClear);
        if (clearBtn != null) {
            clearBtn.setOnClickListener(v -> confirmClear());
        }
        btnDone.setOnClickListener(v -> {
            CoverLauncherProvider.refreshAll(this);
            int n = LauncherPrefs.apps(this).size();
            toast(n == 0 ? "Text" : "TextSave " + n + " Text");
            finish();
        });

        renderFavIcon();
        renderLookIcon();
        renderPhotoIcon();
        applyLook();
        updateCount();
        updateHint();
        load();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if ("1".equals(extra(EXTRA_OPEN_SORT))) {
            showSort();
        }
        rebuild();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // [comment]，[comment]
        if (!loading && !all.isEmpty()) {
            rebuild();
        }
    }

    private String extra(String key) {
        try {
            Intent i = getIntent();
            return i == null ? null : i.getStringExtra(key);
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------ [comment]

    private void onTap(int position) {
        if (position < 0 || position >= shown.size()) {
            return;
        }
        AppRepo.Item it = shown.get(position);
        boolean on = LauncherPrefs.toggle(this, it.pkg);
        Log.i(TAG, (on ? "Text " : "Cancel ") + it.pkg);
        rebuild();
        /*
         * [comment]。
         *
         * [comment] 339dp [comment]，[comment] 460px —— [comment] adjustPan
         * （[comment]、[comment]），[comment]。
         * [comment]"[comment] → [comment]"[comment]，[comment]；
         * [comment]，[comment]。
         */
        if (!query().trim().isEmpty()) {
            hideIme();
        }
    }

    private void hideIme() {
        try {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && etSearch != null) {
                imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            }
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]「[comment]」。[comment]<b>[comment]</b>
     * （{@link LauncherPrefs#favOnly}），[comment]Yes[comment]。
     *
     * <p>[comment]：[comment]。[comment]，
     * [comment] —— [comment]
     * （[comment] {@link #mergeOrder}）。
     */
    private void toggleFavOnly() {
        boolean on = !LauncherPrefs.favOnly(this);
        LauncherPrefs.setFavOnly(this, on);
        Log.i(TAG, on ? "Text" : "Text");
        renderFavIcon();
        rebuild();
        CoverLauncherProvider.refreshAll(this);
        toast(getString(on ? R.string.launcher_fav_only_on : R.string.launcher_fav_only_off));
    }

    /** [comment]：[comment]（[comment] / [comment]） */
    private void renderFavIcon() {
        if (btnFav == null) {
            return;
        }
        boolean on = LauncherPrefs.favOnly(this);
        btnFav.setImageResource(on
                ? R.drawable.ic_launcher_star_on : R.drawable.ic_launcher_star);
        btnFav.setAlpha(on ? 1f : 0.72f);
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]。[comment]、[comment] {@link LauncherPrefs#SORT_ALL} ——
     * [comment] ⇅ [comment]Yes[comment]（{@link #EXTRA_OPEN_SORT}），[comment]"[comment]"[comment]。
     */
    private void showSort() {
        final String[] modes = LauncherPrefs.SORT_ALL;
        final String[] names = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            names[i] = LauncherPrefs.sortLabel(this, modes[i]);
        }

        int cur = 0;
        String now = LauncherPrefs.sort(this);
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(now)) {
                cur = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.launcher_sort)
                .setSingleChoiceItems(names, cur, (d, which) -> {
                    String mode = modes[which];
                    LauncherPrefs.setSort(this, mode);
                    d.dismiss();
                    rebuild();
                    CoverLauncherProvider.refreshAll(this);
                    Log.i(TAG, "Text " + mode);
                    if (LauncherPrefs.SORT_HOME.equals(mode) && !HomePrefs.isConfigured(this)) {
                        toast(getString(R.string.home_order_none));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment] / [comment]。
     *
     * <p>[comment]<b>[comment]</b>：[comment]Yes"[comment]"，
     * [comment]（[comment] scrim），[comment] —— [comment]。
     * [comment]，[comment]，[comment]。
     */
    private void toggleLook() {
        lookOpen = !lookOpen;
        if (lookOpen && !lookBuilt) {
            buildLookRows();
            lookBuilt = true;
        }
        if (lookStrip != null) {
            lookStrip.setVisibility(lookOpen ? View.VISIBLE : View.GONE);
        }
        renderLookIcon();
        // [comment]，[comment]，[comment]
        if (gvApps != null) {
            gvApps.requestLayout();
        }
    }

    private void renderLookIcon() {
        if (btnLook == null) {
            return;
        }
        btnLook.setImageResource(lookOpen
                ? R.drawable.ic_launcher_tune_on : R.drawable.ic_launcher_tune);
        btnLook.setAlpha(lookOpen ? 1f : 0.72f);
    }

    /**
     * [comment] —— [comment] / [comment] ｜ [comment] / [comment] ｜ [comment] / [comment]。
     *
     * <p>[comment]Yes「[comment] + [comment]」：[comment]（{@link #applyLook}），
     * [comment]，[comment]。[comment]「[comment]」[comment] ——
     * [comment]Yes[comment]，[comment]（[comment] {@link LauncherPrefs#bgDrawable}）。
     */
    private void buildLookRows() {
        if (lookStrip == null) {
            return;
        }
        LinearLayout hostCols = lookStrip.findViewById(R.id.lookCols);
        LinearLayout hostIcon = lookStrip.findViewById(R.id.lookIcon);
        LinearLayout hostGap = lookStrip.findViewById(R.id.lookGap);
        LinearLayout hostLabel = lookStrip.findViewById(R.id.lookLabel);
        LinearLayout hostShape = lookStrip.findViewById(R.id.lookShape);
        LinearLayout hostAlpha = lookStrip.findViewById(R.id.lookAlpha);

        int[] colValues = LauncherPrefs.COLUMN_CHOICES;
        String[] colLabels = new String[colValues.length];
        for (int i = 0; i < colValues.length; i++) {
            colLabels[i] = getString(R.string.launcher_look_cols_fmt, colValues[i]);
        }
        ChipRow rowCols = new ChipRow(hostCols, colLabels, colValues,
                () -> LauncherPrefs.columns(this), v -> {
            LauncherPrefs.setColumns(this, v);
            applyLook();
            CoverLauncherProvider.refreshAll(this);
        });

        int[] sizeValues = {LauncherPrefs.SIZE_SMALL, LauncherPrefs.SIZE_MID, LauncherPrefs.SIZE_LARGE};
        String[] sizeLabels = {
                getString(R.string.launcher_size_small),
                getString(R.string.launcher_size_mid),
                getString(R.string.launcher_size_large)};
        ChipRow rowIcon = new ChipRow(hostIcon, sizeLabels, sizeValues,
                () -> LauncherPrefs.iconSize(this), v -> {
            LauncherPrefs.setIconSize(this, v);
            applyLook();
            CoverLauncherProvider.refreshAll(this);
        });

        int[] gapValues = {LauncherPrefs.GAP_TIGHT, LauncherPrefs.GAP_MID, LauncherPrefs.GAP_LOOSE};
        String[] gapLabels = {
                getString(R.string.launcher_gap_tight),
                getString(R.string.launcher_gap_mid),
                getString(R.string.launcher_gap_loose)};
        ChipRow rowGap = new ChipRow(hostGap, gapLabels, gapValues,
                () -> LauncherPrefs.rowGap(this), v -> {
            LauncherPrefs.setRowGap(this, v);
            applyLook();
            CoverLauncherProvider.refreshAll(this);
        });

        int[] labelValues = {1, 0};
        String[] labelLabels = {
                getString(R.string.launcher_label_show),
                getString(R.string.launcher_label_hide)};
        ChipRow rowLabel = new ChipRow(hostLabel, labelLabels, labelValues,
                () -> LauncherPrefs.showLabel(this) ? 1 : 0, v -> {
            LauncherPrefs.setShowLabel(this, v == 1);
            applyLook();
            CoverLauncherProvider.refreshAll(this);
        });

        /*
         * [comment]。[comment]，[comment] ——
         * [comment]（[comment]"[comment]"）[comment] LauncherPrefs.SHAPE_ALL [comment]。
         */
        int[] shapeValues = LauncherPrefs.SHAPE_ALL;
        String[] shapeLabels = {
                getString(R.string.launcher_shape_app),
                getString(R.string.launcher_shape_round),
                getString(R.string.launcher_shape_circle)};
        ChipRow rowShape = new ChipRow(hostShape, shapeLabels, shapeValues,
                () -> LauncherPrefs.iconShape(this), v -> {
            LauncherPrefs.setIconShape(this, v);
            applyLook();
            CoverLauncherProvider.refreshAll(this);
        });

        /*
         * [comment]。[comment] —— "[comment]/[comment]/[comment]"[comment]
         * [comment]（[comment]"[comment]"[comment]Yes[comment]？）。
         *
         * ⚠ [comment] applyLook()：[comment]，[comment]
         * （[comment]，[comment]）。
         */
        int[] alphaValues = LauncherPrefs.ALPHA_CHOICES;
        String[] alphaLabels = new String[alphaValues.length];
        for (int i = 0; i < alphaValues.length; i++) {
            alphaLabels[i] = getString(R.string.launcher_alpha_fmt, alphaValues[i]);
        }
        ChipRow rowAlpha = new ChipRow(hostAlpha, alphaLabels, alphaValues,
                () -> LauncherPrefs.bgAlpha(this), v -> {
            LauncherPrefs.setBgAlpha(this, v);
            applyLook();
            CoverLauncherProvider.refreshAll(this);
        });

        rowCols.render();
        rowIcon.render();
        rowGap.render();
        rowLabel.render();
        rowShape.render();
        rowAlpha.render();
    }

    /**
     * [comment]。[comment] = [comment]（launcher_chip_on），[comment] = [comment]。
     * [comment] —— [comment]，[comment]。
     */
    private class ChipRow {

        private final LinearLayout host;
        private final String[] labels;
        private final int[] values;
        private final IntGetter getter;
        private final OnPick cb;

        ChipRow(LinearLayout host, String[] labels, int[] values, IntGetter getter, OnPick cb) {
            this.host = host;
            this.labels = labels;
            this.values = values;
            this.getter = getter;
            this.cb = cb;
        }

        void render() {
            if (host == null) {
                return;
            }
            host.removeAllViews();
            final int cur = getter.get();
            for (int i = 0; i < labels.length && i < values.length; i++) {
                final int val = values[i];
                TextView t = new TextView(CoverLauncherActivity.this);
                t.setText(labels[i]);
                t.setTextSize(11);
                t.setGravity(Gravity.CENTER);
                t.setTextColor(val == cur ? 0xFF34C759 : 0xFFE8EDF5);
                t.setBackgroundResource(val == cur
                        ? R.drawable.launcher_chip_on : R.drawable.launcher_chip_off);
                // [comment]，[comment] 26dp
                LinearLayout.LayoutParams lp =
                        new LinearLayout.LayoutParams(0, dp(26), 1f);
                if (i > 0) {
                    lp.leftMargin = dp(4);
                }
                t.setLayoutParams(lp);
                t.setOnClickListener(v -> {
                    cb.on(val);
                    render();
                });
                host.addView(t);
            }
        }
    }

    /**
     * [comment]Settings[comment]。
     *
     * <p>[comment] {@code setNumColumns}（[comment]Yes[comment] GridView，[comment] RemoteViews [comment]）；
     * [comment] {@link PickAdapter#getView} [comment]Settings[comment]，
     * [comment] {@code notifyDataSetChanged()} [comment]。
     *
     * <p>[comment]（[comment] + [comment]）[comment] —— [comment] {@link #applyBackdrop}。
     */
    private void applyLook() {
        if (gvApps != null) {
            gvApps.setNumColumns(LauncherPrefs.columns(this));
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        applyBackdrop();
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment]，[comment] */
    private static final int REQ_PHOTO = 0x2207;

    /**
     * [comment]「[comment] + [comment]」。
     *
     * <p>[comment]（#0E1116），[comment]。[comment]，
     * [comment] —— 「[comment]」[comment]Yes[comment]（[comment]），
     * [comment]，[comment]。
     *
     * <p>⚠ [comment]：1520px [comment] JPEG [comment] MB，[comment]。
     * [comment] {@link LauncherPhoto} [comment]，[comment]。
     */
    private void applyBackdrop() {
        final View root = findViewById(R.id.pickRoot);
        if (root == null) {
            return;
        }
        if (!LauncherPhoto.has(this)) {
            root.setBackgroundColor(0xFF0E1116);
            return;
        }
        final int alpha = LauncherPrefs.bgAlpha(this);
        final Context app = getApplicationContext();
        new Thread(() -> {
            final android.graphics.drawable.Drawable bg =
                    LauncherPhoto.pageBackdrop(app, alpha);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                View v = findViewById(R.id.pickRoot);
                if (v == null) {
                    return;
                }
                if (bg == null) {
                    v.setBackgroundColor(0xFF0E1116);
                } else {
                    v.setBackground(bg);
                }
            });
        }, "pick-backdrop").start();
    }

    /** [comment] = [comment]；[comment]「[comment] / [comment]」 */
    private void onPhotoTap() {
        if (LauncherPhoto.has(this)) {
            showPhotoMenu();
        } else {
            pickPhoto();
        }
    }

    /**
     * [comment]。
     *
     * <p>[comment] {@code ACTION_OPEN_DOCUMENT} [comment]Yes GET_CONTENT：[comment]Yes[comment]
     * [comment]，<b>[comment]</b>，[comment]。
     *
     * <p>⚠ [comment]，[comment]。
     * [comment] —— [comment]，[comment]
     * （[comment] onActivityResult），[comment]Yes[comment]。
     */
    private void pickPhoto() {
        Intent it = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        it.addCategory(Intent.CATEGORY_OPENABLE);
        it.setType("image/*");
        try {
            ActivityOptions op = ActivityOptions.makeBasic();
            op.setLaunchDisplayId(getWindowManager().getDefaultDisplay().getDisplayId());
            startActivityForResult(it, REQ_PHOTO, op.toBundle());
        } catch (Throwable t) {
            Log.w(TAG, "Text，TextDefaultText", t);
            startActivityForResult(it, REQ_PHOTO);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_PHOTO) {
            return;
        }
        if (res != RESULT_OK || data == null || data.getData() == null) {
            Log.i(TAG, "TextCancel");
            return;
        }
        final Uri src = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(
                    src, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) {
            // [comment]，[comment] —— [comment]
        }
        toast(getString(R.string.launcher_photo_saving));
        final Context app = getApplicationContext();
        new Thread(() -> {
            final boolean ok = LauncherPhoto.save(app, src);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                toast(getString(ok ? R.string.launcher_photo_done : R.string.launcher_photo_fail));
                if (ok) {
                    renderPhotoIcon();
                    applyLook();
                    CoverLauncherProvider.refreshAll(CoverLauncherActivity.this);
                }
            });
        }, "pick-save").start();
    }

    /** [comment]：[comment] / [comment] */
    private void showPhotoMenu() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_launcher_menu);
        box.setPadding(dp(4), dp(4), dp(4), dp(4));

        final int menuW = dp(168);
        final PopupWindow pw = new PopupWindow(box, menuW,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pw.setOutsideTouchable(true);
        pw.setFocusable(true);

        box.addView(menuRow(getString(R.string.launcher_photo_change), 0xFFECEFF4, v -> {
            pw.dismiss();
            pickPhoto();
        }));
        box.addView(menuRow(getString(R.string.launcher_photo_remove), 0xFFFF7A6E, v -> {
            pw.dismiss();
            LauncherPhoto.clear(this);
            renderPhotoIcon();
            applyLook();
            CoverLauncherProvider.refreshAll(this);
            toast(getString(R.string.launcher_photo_removed));
        }));

        if (btnPhoto == null) {
            pw.showAtLocation(findViewById(R.id.gvPick), Gravity.CENTER, 0, 0);
            return;
        }
        // [comment]，[comment]、[comment]（xoff [comment] = [comment]）
        pw.showAsDropDown(btnPhoto, dp(34) - menuW, dp(4));
    }

    /** [comment] —— [comment]/[comment]/[comment]Yes[comment] */
    private void renderPhotoIcon() {
        if (btnPhoto == null) {
            return;
        }
        boolean on = LauncherPhoto.has(this);
        btnPhoto.setImageResource(on
                ? R.drawable.ic_launcher_photo_on : R.drawable.ic_launcher_photo);
        btnPhoto.setAlpha(on ? 1f : 0.72f);
    }

    private int dp(int v) {
        float d;
        try {
            d = getResources().getDisplayMetrics().density;
        } catch (Throwable t) {
            d = 2f;
        }
        return Math.round(v * d);
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]、[comment] —— [comment]「[comment] / [comment]」[comment]。
     *
     * <p>[comment]：[comment]。[comment]＝[comment]
     * Yes[comment]，[comment]"[comment]"[comment]，
     * [comment]Yes[comment]、[comment]Yes[comment]，[comment]。
     */
    @Override
    public void onHoldReleased(int position) {
        if (isFinishing() || position < 0 || position >= shown.size()) {
            return;
        }
        showItemMenu(shown.get(position),
                gvApps.getChildAt(position - gvApps.getFirstVisiblePosition()));
    }

    /**
     * [comment]（[comment] + {@code PopupWindow}）。
     *
     * <p>[comment] {@link AlertDialog}：[comment]、[comment]、[comment]Cancel[comment]，[comment]，
     * [comment]Yes[comment]，[comment]。[comment]Yes[comment] —— [comment]、[comment]，
     * [comment]，[comment]。<b>[comment]，[comment] box [comment] append [comment]。</b>
     *
     * <p>⚠ [comment] Dialog [comment] {@code PopupWindow}：Dialog [comment]，[comment]
     * [comment]，[comment]（[comment]）[comment] —— [comment]。{@code PopupWindow.showAsDropDown}
     * [comment]Yes「[comment] View [comment]」：[comment]，[comment]、[comment]，
     * [comment]，[comment]。
     *
     * @param cell [comment]；[comment]（[comment]）[comment]
     */
    private void showItemMenu(final AppRepo.Item it, final View cell) {
        final boolean faved = favs.contains(it.pkg);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_launcher_menu);
        box.setPadding(dp(4), dp(4), dp(4), dp(4));

        TextView head = new TextView(this);
        head.setText(it.label);
        head.setTextSize(12f);
        head.setTextColor(0xFF8A94A6);
        head.setSingleLine(true);
        head.setEllipsize(TextUtils.TruncateAt.END);
        head.setPadding(dp(12), dp(6), dp(12), dp(6));
        box.addView(head);

        View div = new View(this);
        div.setBackgroundColor(0x1AFFFFFF);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        dlp.setMargins(dp(8), 0, dp(8), dp(2));
        box.addView(div, dlp);

        final int menuW = dp(186);
        final PopupWindow pw = new PopupWindow(box, menuW,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pw.setOutsideTouchable(true);
        pw.setFocusable(true);

        box.addView(menuRow(getString(faved ? R.string.launcher_unfav : R.string.launcher_fav),
                0xFFECEFF4, v -> {
                    pw.dismiss();
                    toggleFav(it);
                }));
        box.addView(menuRow(getString(R.string.launcher_uninstall), 0xFFFF7A6E, v -> {
            pw.dismiss();
            confirmUninstall(it);
        }));

        View host = findViewById(R.id.gridHost);
        if (cell == null || host == null) {   // Text，Text
            pw.showAtLocation(findViewById(R.id.gvPick), Gravity.CENTER, 0, 0);
            return;
        }

        // [comment]：[comment]，[comment]
        box.measure(View.MeasureSpec.makeMeasureSpec(menuW, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int menuH = box.getMeasuredHeight();

        // [comment]：Default[comment]；[comment]；[comment]，[comment]
        // （[comment]Yes「[comment]」，[comment]，[comment]）
        int[] cl = new int[2];
        int[] hl = new int[2];
        cell.getLocationInWindow(cl);
        host.getLocationInWindow(hl);
        int roomBelow = (hl[1] + host.getHeight()) - (cl[1] + cell.getHeight());
        int roomAbove = cl[1] - hl[1];
        boolean below;
        if (roomBelow >= menuH + dp(4)) {
            below = true;
        } else if (roomAbove >= menuH + dp(4)) {
            below = false;
        } else {
            below = roomBelow >= roomAbove;
        }

        // [comment]：[comment]，[comment]（[comment] 2dp）
        int cellLeftInHost = gvApps.getLeft() + cell.getLeft();
        int left = cellLeftInHost + (cell.getWidth() - menuW) / 2;
        int lo = dp(2);
        int hi = Math.max(lo, host.getWidth() - menuW - dp(2));
        if (left < lo) {
            left = lo;
        } else if (left > hi) {
            left = hi;
        }

        pw.showAsDropDown(cell, left - cellLeftInHost, below ? 0 : -dp(6),
                below ? Gravity.NO_GRAVITY : Gravity.TOP);
    }

    /** [comment]。[comment]（ripple [comment] drawable [comment]） */
    private View menuRow(String text, int color, View.OnClickListener click) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14f);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setBackgroundResource(R.drawable.launcher_menu_item);
        tv.setClickable(true);
        tv.setOnClickListener(click);
        return tv;
    }

    /** [comment] / Cancel[comment]。[comment]（[comment]）。 */
    private void toggleFav(AppRepo.Item it) {
        boolean now = LauncherPrefs.toggleFav(this, it.pkg);
        Log.i(TAG, (now ? "Text " : "CancelText ") + it.pkg);
        rebuild();
        CoverLauncherProvider.refreshAll(this);
        toast(getString(now ? R.string.launcher_fav_added : R.string.launcher_fav_removed, it.label));
    }

    /** [comment]Yes[comment]，[comment] */
    private void confirmUninstall(final AppRepo.Item it) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.launcher_uninstall_confirm, it.label))
                .setMessage(R.string.launcher_uninstall_msg)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.launcher_uninstall, (d, w) -> doUninstall(it))
                .show();
    }

    /**
     * [comment]。[comment] Shizuku [comment] shell（{@code pm uninstall --user 0}）——
     * [comment]，[comment]Settings[comment]。
     * Failed[comment]，[comment]Success。
     */
    private void doUninstall(final AppRepo.Item it) {
        if (!ShellRunner.isReady()) {
            toast(getString(R.string.launcher_uninstall_no_shizuku));
            return;
        }
        // [comment]Yes[comment]；[comment] shell [comment]
        if (!it.pkg.matches("[A-Za-z0-9._]+")) {
            toast(getString(R.string.launcher_uninstall_bad_pkg));
            return;
        }
        toast(getString(R.string.launcher_uninstalling, it.label));
        new Thread(() -> {
            String out = ShellRunner.run("pm uninstall --user 0 " + it.pkg, 40);
            final boolean ok = out != null && out.contains("Success");
            final String why = firstLine(out);
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                if (!ok) {
                    Log.w(TAG, "TextFailed " + it.pkg + " => " + out);
                    toast(getString(R.string.launcher_uninstall_fail, why));
                    return;
                }
                // [comment]，[comment]
                LauncherPrefs.forget(this, it.pkg);
                all.remove(it);
                Log.i(TAG, "Text " + it.pkg);
                rebuild();
                CoverLauncherProvider.refreshAll(this);
                toast(getString(R.string.launcher_uninstall_done, it.label));
            });
        }, "extrot-launcher-uninstall").start();
    }

    /** [comment] —— Failed[comment] */
    private static String firstLine(String s) {
        if (s == null) {
            return "";
        }
        int i = s.indexOf('\n');
        return (i < 0 ? s : s.substring(0, i)).trim();
    }

    // ------------------------------------------------------------------ [comment]

    @Override
    public void onOrderMove(int from, int to) {
        if (from < 0 || to < 0 || from >= shown.size() || to >= shown.size()) {
            return;
        }
        // [comment]：[comment]，[comment]"[comment]"
        AppRepo.Item it = shown.remove(from);
        shown.add(to, it);
        adapter.notifyDataSetChanged();
    }

    /**
     * [comment]：[comment]"[comment]"[comment]。
     *
     * <p>[comment] {@code LauncherPrefs} [comment] move ——
     * [comment]Yes[comment]"[comment]"[comment]。[comment]Yes[comment]，
     * [comment]。[comment]，[comment]"[comment]"（[comment]Yes[comment]）。
     */
    @Override
    public void onOrderChanged(int from, int to) {
        List<String> seg = new ArrayList<>();
        for (int i = 0; i < selectedCount && i < shown.size(); i++) {
            seg.add(shown.get(i).pkg);
        }
        List<String> order = mergeOrder(seg);
        String was = LauncherPrefs.sort(this);
        LauncherPrefs.setSort(this, LauncherPrefs.SORT_MANUAL);
        LauncherPrefs.setApps(this, order);
        CoverLauncherProvider.refreshAll(this);
        Log.i(TAG, "Text " + from + " -> " + to + "，TextUpdateText " + order.size() + " Text"
                + (LauncherPrefs.SORT_MANUAL.equals(was) ? "" : "（Text " + was + " -> Text）"));
        // [comment]Yes"[comment]"[comment] —— [comment]Notice[comment]
        if (!LauncherPrefs.SORT_MANUAL.equals(was)) {
            toast(getString(R.string.launcher_manual_on));
        }
        updateHint();
    }

    @Override
    public void onDragStateChanged(boolean dragging) {
        if (dragging) {
            tvHint.setText(R.string.launcher_hint_drag);
        } else {
            updateHint();
        }
    }

    /**
     * [comment]"[comment]"[comment]。
     *
     * <p>[comment]「[comment]」[comment]Yes[comment]，[comment]（[comment]）。
     * [comment] —— [comment]，
     * No[comment]。[comment]Yes<b>[comment]</b>：
     * [comment]，[comment]，[comment]。
     * [comment] = [comment]，[comment]。
     */
    private List<String> mergeOrder(List<String> seg) {
        if (!LauncherPrefs.favOnly(this)) {
            return seg;
        }
        List<String> full = LauncherPrefs.apps(this);
        int k = 0;
        for (int i = 0; i < full.size() && k < seg.size(); i++) {
            if (favs.contains(full.get(i))) {
                full.set(i, seg.get(k++));
            }
        }
        return full;
    }

    // ------------------------------------------------------------------ [comment]

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        tvHint.setText("Text…");
        new Thread(() -> {
            final List<AppRepo.Item> list = AppRepo.load(this);
            runOnUiThread(() -> {
                loading = false;
                all.clear();
                all.addAll(list);
                rebuild();
                if (wantSortDialog) {
                    wantSortDialog = false;
                    showSort();
                }
            });
        }, "extrot-launcher-load").start();
    }

    private String query() {
        return etSearch == null || etSearch.getText() == null
                ? "" : etSearch.getText().toString();
    }

    /**
     * [comment]：[comment]（[comment]）[comment]，[comment]（[comment]）[comment]。
     *
     * <p>[comment]、[comment]、[comment] —— [comment]，
     * [comment]"[comment]"。
     */
    private void rebuild() {
        String q = query();
        final List<AppRepo.Item> filtered = AppRepo.filter(all, q);
        final Set<String> picked = new HashSet<>(LauncherPrefs.apps(this));

        // [comment]：[comment]、[comment]
        favs.clear();
        favs.addAll(LauncherPrefs.favs(this));

        final boolean favOnly = LauncherPrefs.favOnly(this);
        List<AppRepo.Item> sel = new ArrayList<>();
        List<AppRepo.Item> un = new ArrayList<>();
        for (AppRepo.Item it : filtered) {
            if (favOnly) {
                // [comment]：[comment]（[comment]，[comment]Yes[comment]）
                if (favs.contains(it.pkg)) {
                    sel.add(it);
                }
                continue;
            }
            if (picked.contains(it.pkg)) {
                sel.add(it);
            } else {
                un.add(it);
            }
        }
        // [comment]；[comment] AppRepo [comment]
        LauncherPrefs.sortItems(this, sel);

        shown.clear();
        shown.addAll(sel);
        shown.addAll(un);
        selectedCount = sel.size();

        if (gvApps != null) {
            gvApps.setDropLimit(selectedCount);
            // [comment]，[comment] —— [comment]
            gvApps.setDragEnabled(q.trim().isEmpty() && selectedCount > 1);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updateCount();
        updateHint();
    }

    private void updateCount() {
        if (tvCount == null) {
            return;
        }
        if (LauncherPrefs.favOnly(this)) {
            // [comment]"[comment] N [comment]"[comment]（[comment] N [comment]）—— [comment]
            tvCount.setText(getString(R.string.launcher_fav_count, favs.size()));
            return;
        }
        int n = LauncherPrefs.apps(this).size();
        tvCount.setText(n == 0
                ? getString(R.string.launcher_pick_none)
                : getString(R.string.launcher_apps_fmt, n));
    }

    private void updateHint() {
        if (tvHint == null) {
            return;
        }
        String q = query().trim();
        if (loading) {
            tvHint.setText("Text…");
        } else if (all.isEmpty()) {
            tvHint.setText("Text");
        } else if (!q.isEmpty()) {
            tvHint.setText(shown.isEmpty()
                    ? getString(R.string.launcher_search_empty)
                    : getString(R.string.launcher_hint_search));
        } else {
            tvHint.setText(LauncherPrefs.favOnly(this)
                    ? R.string.launcher_hint_fav : R.string.launcher_hint_tap);
        }
    }

    // ------------------------------------------------------------------ [comment] / [comment]

    /**
     * [comment]「[comment]」：[comment]Yes「[comment]」[comment]，[comment]。
     *
     * <p><b>[comment]</b> —— [comment] multistar [comment]（{@link CoverListImport}），
     * [comment]。<b>[comment]</b> —— [comment]：[comment]<b>[comment]</b>[comment]
     * [comment]（{@link HomePrefs}）。[comment]Yes[comment]，[comment]。
     *
     * <p>「[comment]」[comment] —— [comment]Yes[comment]
     * （{@link LauncherPrefs#SORT_HOME}）：[comment]，[comment]
     * [comment]，[comment]。
     */
    private void showIoDialog() {
        final String[] titles = {
                getString(R.string.launcher_io_sys),
                getString(R.string.launcher_io_export)};
        final String[] subs = {
                getString(R.string.launcher_io_sys_sub),
                getString(R.string.launcher_io_export_sub)};

        BaseAdapter ia = new BaseAdapter() {
            @Override
            public int getCount() {
                return titles.length;
            }

            @Override
            public Object getItem(int position) {
                return titles[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.item_io_choice, parent, false);
                }
                ((TextView) v.findViewById(R.id.ioTitle)).setText(titles[position]);
                ((TextView) v.findViewById(R.id.ioSub)).setText(subs[position]);
                return v;
            }
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.launcher_io_title)
                .setAdapter(ia, (d, which) -> {
                    if (which == 0) {
                        doImportSystem();
                    } else {
                        doExportHome();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * [comment]。
     *
     * <p>[comment]Yes"[comment]Settings"，[comment] Shizuku，[comment]Yes[comment] ⇒ [comment]，
     * [comment]（[comment]）。
     */
    private void doImportSystem() {
        if (!ShellRunner.isReady()) {
            toast(getString(R.string.launcher_import_fail));
            return;
        }
        toast("Text…");
        new Thread(() -> {
            final List<String> found = CoverListImport.readInstalled(this);
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                if (found.isEmpty()) {
                    toast(getString(R.string.launcher_import_none));
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle(R.string.launcher_import)
                        .setMessage(getString(R.string.launcher_import_confirm, found.size()))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (d, w) -> {
                            LauncherPrefs.setApps(this, found);
                            rebuild();
                            Log.i(TAG, "Text " + found.size() + " Text");
                            toast(getString(R.string.launcher_import_done, found.size()));
                        })
                        .show();
            });
        }, "extrot-launcher-import").start();
    }

    /**
     * [comment]：[comment]<b>[comment]</b>[comment]（{@link SecondaryHomeActivity}）。
     *
     * <p>[comment]Yes[comment]<b>[comment]</b>[comment] —— [comment]：[comment]、
     * [comment] / [comment]，[comment]。
     *
     * <p>[comment]Yes「[comment]」：[comment]「[comment]」[comment]，[comment]Yes[comment]
     * （[comment]，[comment]Yes[comment]）。
     */
    private void doExportHome() {
        List<String> order;
        if (query().trim().isEmpty()) {
            // [comment]：[comment]"[comment]"[comment]，[comment]Yes[comment]
            order = new ArrayList<>();
            for (int i = 0; i < selectedCount && i < shown.size(); i++) {
                order.add(shown.get(i).pkg);
            }
        } else {
            order = LauncherPrefs.apps(this);
        }
        if (order.isEmpty()) {
            toast(getString(R.string.launcher_io_export_empty));
            return;
        }
        HomePrefs.setApps(this, order);
        toast(getString(R.string.launcher_io_export_done, order.size()));
        Log.i(TAG, "Text " + order.size() + " Text");
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.launcher_clear)
                .setMessage(R.string.launcher_clear_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    LauncherPrefs.setApps(this, new ArrayList<>());
                    rebuild();
                })
                .show();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ [comment]

    private class PickAdapter extends BaseAdapter implements DragGrid.HiddenSlot {

        /** [comment]（[comment]）。-1 = [comment]。 */
        private int hiddenPos = -1;

        @Override
        public void setHiddenPosition(int position) {
            hiddenPos = position;
        }

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
                v = getLayoutInflater().inflate(R.layout.item_app_pick, parent, false);
            }
            AppRepo.Item it = shown.get(position);
            boolean on = LauncherPrefs.isSelected(CoverLauncherActivity.this, it.pkg);

            ImageView ic = v.findViewById(R.id.pickIcon);
            TextView tv = v.findViewById(R.id.pickLabel);
            ImageView ck = v.findViewById(R.id.pickCheck);
            ImageView star = v.findViewById(R.id.pickFav);

            tv.setText(it.label);

            // 「[comment]」：[comment] 40dp [comment]Yes[comment]，[comment]
            final int side = dp(iconDp());

            /*
             * 「[comment]」：[comment]
             * （CoverLauncherService.maskPath），[comment]Yes[comment]，
             * [comment]Yes[comment] —— [comment]Yes[comment]，[comment]。
             * 「[comment]」[comment] drawable：[comment]"[comment]"[comment]。
             */
            final int shape = LauncherPrefs.iconShape(CoverLauncherActivity.this);
            try {
                if (shape == LauncherPrefs.SHAPE_APP) {
                    ic.setImageDrawable(it.icon);
                } else {
                    ic.setImageBitmap(CoverLauncherService.toBitmap(it.icon, side, shape));
                }
            } catch (Throwable ignored) {
                ic.setImageDrawable(null);
            }

            ViewGroup.LayoutParams lp = ic.getLayoutParams();
            if (lp != null && (lp.width != side || lp.height != side)) {
                lp.width = side;
                lp.height = side;
                ic.setLayoutParams(lp);
            }

            // 「[comment]」：GridView [comment] verticalSpacing [comment]，[comment]
            final int pad = dp(LauncherPrefs.rowPadDp(CoverLauncherActivity.this));
            final boolean showLabel = LauncherPrefs.showLabel(CoverLauncherActivity.this);
            v.setPadding(v.getPaddingLeft(), pad, v.getPaddingRight(),
                    showLabel ? pad : pad * 2);

            // [comment]：[comment]，[comment] + [comment]
            ic.setAlpha(on ? 1f : 0.42f);
            tv.setAlpha(on ? 1f : 0.55f);
            tv.setVisibility(showLabel ? View.VISIBLE : View.GONE);
            ck.setVisibility(on ? View.VISIBLE : View.GONE);
            // [comment]：[comment]
            star.setVisibility(favs.contains(it.pkg) ? View.VISIBLE : View.GONE);

            /*
             * [comment]：
             *   · [comment]（[comment] INVISIBLE，[comment]）；
             *   · [comment] 0 —— [comment]Yes[comment]，[comment]
             *     translation [comment]，[comment]。
             */
            v.setTranslationX(0f);
            v.setTranslationY(0f);
            v.setAlpha(position == hiddenPos ? 0f : 1f);
            return v;
        }
    }

    /** 「[comment]」[comment]（dp）—— 4 [comment] 40dp，[comment] dp [comment] */
    private int iconDp() {
        switch (LauncherPrefs.iconSize(this)) {
            case LauncherPrefs.SIZE_SMALL:
                return 34;
            case LauncherPrefs.SIZE_LARGE:
                return 48;
            default:
                return 40;
        }
    }
}
