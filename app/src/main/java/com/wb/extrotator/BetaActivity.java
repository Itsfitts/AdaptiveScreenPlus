package com.wb.extrotator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Beta [comment]。
 *
 * <p>[comment]「Beta [comment]」[comment]，Yes[comment]：[comment]，
 * [comment]「[comment] / [comment] / [comment]」[comment]。
 *
 * <p>[comment]：[comment]Yes[comment]，[comment]，
 * [comment]；[comment]，
 * [comment]。
 *
 * <p>[comment] {@link CoverDisplay}（[comment]Settings）、{@link QuickActions} [comment]
 * {@link QuickPrefs}（[comment]）、{@link CustomCmds}（Custom[comment]）。
 *
 * <p><b>[comment]</b>：[comment] binder / sleep [comment] {@link #pool}（[comment]）[comment]，
 * [comment]。[comment]Yes[comment]，Yes[comment]（[comment] size [comment]），
 * [comment]。
 */
public class BetaActivity extends Activity {

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    // ---- [comment]Settings[comment]
    private EditText etW, etH, etDpi;
    private TextView tvCoverState;
    private LinearLayout listPresets;
    /** 「[comment]」[comment]（[comment]） */
    private TextView tvLauncherState;
    // ---- [comment]
    private TextView tvAssistState, tvVolumeState;
    private LinearLayout listCustomCmds;
    /** 「[comment]」[comment]Settings（{@link QuickPrefs#SIDE_ASSIST} [comment]） */
    private String keyTarget = QuickPrefs.SIDE_ASSIST;
    /** 「[comment]」[comment]（v4.2 [comment]「[comment]」[comment]，v4.17 [comment]「[comment]」[comment]） */
    private TextView tvEdgeState;
    /** 「[comment]」[comment]：[comment] */
    private TextView tvRepairBrief;
    /** [comment]，[comment] {@link #setNavImmSwitch} */
    private boolean navImmSyncing;
    /** 「[comment]」[comment]：[comment]，[comment]Yes[comment] */
    private boolean coverRotSyncing;

    /** [comment]（[comment]） */
    private List<CoverPresets.Preset> presets;
    /** [comment]，[comment]Default[comment]、[comment]Yes「[comment]Default」[comment] */
    private int physW, physH, physDpi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beta);

        View back = findViewById(R.id.btnBetaBack);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }

        // [comment] id [comment]，[comment] —— [comment]，[comment]。
        RadioGroup tabs = findViewById(R.id.rgBetaTabs);
        if (tabs != null) {
            tabs.setOnCheckedChangeListener((group, checkedId) -> {
                showPanel(R.id.panelBetaExt, checkedId == R.id.tabBetaExt);
                showPanel(R.id.panelBetaCmd, checkedId == R.id.tabBetaCmd);
                showPanel(R.id.panelBetaEdge, checkedId == R.id.tabBetaEdge);
                showPanel(R.id.panelBetaApp, checkedId == R.id.tabBetaApp);
            });
            tabs.check(R.id.tabBetaExt);
        }

        bindInfo();

        bindCoverPanel();
        bindQuickPanel();
        bindEdgePanel();
        bindRepairPanel();
        bindInnerGesture();
        bindNavImmPanel();

        refreshCoverState();
        refreshQuickState();
        refreshLauncherState();
        refreshEdgeState();
        maybeShowIntro();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // [comment]Settings[comment]，None[comment]Enable[comment]
        refreshQuickState();
        // [comment]
        refreshLauncherState();
        // [comment]「[comment]」[comment]（[comment]、MultiStar [comment]）[comment]
        refreshRepairBrief();
        // [comment] NavStar [comment]，[comment]
        refreshNavImmState();
        // [comment]（[comment] / [comment]）[comment]，[comment]
        refreshCoverAutoRotSwitch();
    }

    @Override
    protected void onDestroy() {
        pool.shutdownNow();
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ================================================================== [comment]

    private void showPanel(int viewId, boolean visible) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    /**
     * [comment]「ⓘ」[comment]（[comment] MainActivity.bindInfo）。
     * [comment] strings.xml [comment] info_* [comment]，[comment]：[comment] / [comment] / [comment]。
     */
    private void bindInfo() {
        bindInfo(R.id.iBetaTop, "Beta Text", R.string.info_beta_intro);
        bindInfo(R.id.iBetaExt, "Text", R.string.info_beta_ext);
        bindInfo(R.id.iBetaCoverRes, "Text DPI", R.string.info_beta_cover_res);
        bindInfo(R.id.iBetaPreset, "Text", R.string.info_beta_preset);
        bindInfo(R.id.iBetaRotate, "Text", R.string.info_beta_rotate);
        bindInfo(R.id.iBetaCoverAutoRot, "Text", R.string.info_cover_autorot);
        bindInfo(R.id.iBetaLauncher, "Text", R.string.info_launcher);
        bindInfo(R.id.iBetaCmd, "Text", R.string.info_beta_cmd);
        bindInfo(R.id.iBetaKey, "Text", R.string.info_beta_key);
        bindInfo(R.id.iBetaCustom, "CustomText", R.string.info_beta_custom);
        bindInfo(R.id.iBetaExternal, "Text", R.string.info_beta_external);
        bindInfo(R.id.iBetaEdge, "Text", R.string.info_edge_cutout);
        bindInfo(R.id.iBetaCoverRecents, "Text", R.string.info_cover_recents);
        bindInfo(R.id.iBetaSidebar, "Text", R.string.info_cover_sidebar);
        bindInfo(R.id.iBetaInner, "Text", R.string.info_inner_gesture);
        bindInfo(R.id.iBetaGameGesture, "Text", R.string.info_game_gesture);
        bindInfo(R.id.iBetaNavImm, "Text", R.string.info_nav_immersive);
        bindInfo(R.id.iBetaApp, "Text", R.string.info_repair_app);
    }

    private void bindInfo(int iconId, String title, int detailRes) {
        View icon = findViewById(iconId);
        if (icon == null) {
            return;
        }
        icon.setOnClickListener(v -> {
            if (isFinishing()) {
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(detailRes)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
    }

    /** [comment]「Shizuku [comment]」：[comment] true [comment]Notice[comment]，[comment] return */
    private boolean needShizuku() {
        if (CoverDisplay.id(this) <= 0) {
            toast("Text");
            return true;
        }
        if (!ShellRunner.isReady()) {
            toast("Shizuku Not Ready，Text");
            return true;
        }
        return false;
    }

    // ================================================================== [comment]

    /**
     * 「[comment]」[comment]。
     *
     * <p>⚠ [comment] setChecked [comment]，[comment] {@link #bindPseudoSwitch}。
     *
     * <p><b>[comment]</b> —— [comment]Yes[comment]，[comment]<b>None[comment]</b>[comment]
     * （{@code TYPE_ACCESSIBILITY_OVERLAY} YesNone[comment]），[comment]
     * {@link VolumeKeyService} [comment]；<b>[comment]</b> —— [comment]。
     */
    // ================================================================== [comment]

    /**
     * 「[comment]」[comment]：[comment]「[comment]」[comment]。
     *
     * <p>[comment]Yes<b>[comment]</b>[comment]，[comment]Yes[comment] —— [comment] Shizuku，
     * [comment]，[comment]。
     */
    private void bindRepairPanel() {
        tvRepairBrief = findViewById(R.id.tvRepairBrief);

        View open = findViewById(R.id.btnOpenRepair);
        if (open != null) {
            open.setOnClickListener(v -> startActivity(new Intent(this, RepairActivity.class)));
        }

    }

    private void refreshRepairBrief() {
        final TextView tv = tvRepairBrief;
        if (tv == null) {
            return;
        }
        tv.setText("Text…");
        pool.execute(() -> {
            CoverRepair.Snapshot s = CoverRepair.read();
            final String text;
            if (s == null || !s.valid()) {
                text = "Text —— Shizuku Text？";
            } else {
                text = "Text " + CoverRepair.packages(s).size() + " Text"
                        + (CoverRepair.hasSnapshot(this) ? " · Text" : "");
            }
            ui.post(() -> tv.setText(text));
        });
    }

    private void bindEdgePanel() {
        tvEdgeState = findViewById(R.id.tvEdgeState);
        View apply = findViewById(R.id.btnCutoutApply);
        if (apply != null) {
            apply.setOnClickListener(v -> doApplyCutout());
        }

        Switch recents = findViewById(R.id.swCoverRecents);
        if (recents != null) {
            recents.setChecked(ExtPrefs.coverRecentsGesture(this));
            recents.setOnCheckedChangeListener((b, on) -> {
                ExtPrefs.setCoverRecentsGesture(this, on);
                if (on && !QuickPrefs.isAccessibilityEnabled(this)) {
                    toast("TextNoneText，Text「Text」Text");
                }
                VolumeKeyService.syncCoverRecents();
                toast(on ? "Text → Text" : "Text");
                refreshEdgeState();
            });
        }
        // [comment]。⚠ [comment] setChecked [comment] listener —— [comment]"[comment]"。
        RadioGroup band = findViewById(R.id.rgRecentsBand);
        if (band != null) {
            int cur = ExtPrefs.coverRecentsBand(this);
            band.check(cur == 0 ? R.id.rbBandSmall : cur == 2 ? R.id.rbBandBig : R.id.rbBandMid);
            band.setOnCheckedChangeListener((g, id) -> {
                int pick = id == R.id.rbBandSmall ? 0 : id == R.id.rbBandBig ? 2 : 1;
                if (pick == ExtPrefs.coverRecentsBand(this)) {
                    return;
                }
                ExtPrefs.setCoverRecentsBand(this, pick);
                VolumeKeyService.refreshCoverRecents();
                toast("Text「" + (pick == 0 ? "Text" : pick == 2 ? "Text" : "Text")
                        + "」—— Text");
            });
        }

        // [comment]。[comment] setChecked [comment] listener。
        RadioGroup zone = findViewById(R.id.rgRecentsZone);
        if (zone != null) {
            int z = ExtPrefs.coverRecentsZone(this);
            zone.check(z == 0 ? R.id.rbZoneQuarter : z == 2 ? R.id.rbZoneHalf
                    : z == 3 ? R.id.rbZoneFull : R.id.rbZoneThird);
            zone.setOnCheckedChangeListener((g, id) -> {
                int pick = id == R.id.rbZoneQuarter ? 0
                        : id == R.id.rbZoneHalf ? 2
                        : id == R.id.rbZoneFull ? 3 : 1;
                if (pick == ExtPrefs.coverRecentsZone(this)) {
                    return;
                }
                ExtPrefs.setCoverRecentsZone(this, pick);
                VolumeKeyService.refreshCoverRecents();
                toast("Text「" + zoneWidthName(pick) + "」—— Text");
            });
        }

        // 「[comment] 0° [comment]」。0° [comment]，
        // [comment]；[comment]，[comment]。
        // ⚠ [comment] setChecked [comment] listener —— [comment]"[comment]"。
        CheckBox corner = findViewById(R.id.cbRecentsCorner);
        if (corner != null) {
            corner.setChecked(ExtPrefs.coverRecentsCornerLeft(this));
            corner.setOnCheckedChangeListener((b, on) -> {
                ExtPrefs.setCoverRecentsCornerLeft(this, on);
                VolumeKeyService.refreshCoverRecents();
                toast(on ? "Text 0° Text" : "Text");
            });
        }


        // [comment]「[comment]」Settings[comment]。[comment]"[comment]"，
        // [comment] —— [comment]。
        View navMode = findViewById(R.id.btnNavMode);
        if (navMode != null) {
            navMode.setOnClickListener(v -> doOpenNavMode());
        }

        Switch sidebar = findViewById(R.id.swCoverSidebar);
        if (sidebar != null) {
            sidebar.setChecked(ExtPrefs.coverSidebar(this));
            sidebar.setOnCheckedChangeListener((b, on) -> {
                ExtPrefs.setCoverSidebar(this, on);
                if (on && !QuickPrefs.isAccessibilityEnabled(this)) {
                    toast("TextNoneText，Text「Text」Text");
                }
                VolumeKeyService.syncSidebar();
                toast(on ? "Text：Text（Text）" : "Text：Text");
                refreshEdgeState();
            });
        }
        bindSidebarPanel();

        View refresh = findViewById(R.id.btnEdgeRefresh);
        if (refresh != null) {
            refresh.setOnClickListener(v -> refreshEdgeState());
        }
    }

    // ---------------------------------------------------------------- [comment]

    /**
     * 「[comment]」[comment]。
     *
     * <p>⚠ [comment] setChecked [comment] —— [comment]"[comment]"（[comment]）。
     *
     * <p>[comment]<b>[comment]</b>[comment]；[comment]，
     * [comment] {@link #refreshNavImmState()} [comment]。
     */
    private void bindNavImmPanel() {
        Switch sw = findViewById(R.id.swNavImm);
        if (sw != null) {
            sw.setChecked(ExtPrefs.navImmOn(this));
            sw.setOnCheckedChangeListener((b, on) -> {
                // [comment]Yes[comment]，[comment]Yes[comment]
                if (navImmSyncing) {
                    return;
                }
                doNavImm(on);
            });
        }
    }

    /**
     * 「[comment]」[comment]。
     *
     * <p>[comment]「[comment]」[comment]，[comment]<b>[comment]Settings</b>：[comment]None[comment]，
     * [comment]、[comment]。
     *
     * <p>[comment]（None[comment]）[comment] —— [comment]、[comment]。
     */
    private void bindInnerGesture() {
        Switch sw = findViewById(R.id.swInnerGesture);
        if (sw == null) {
            return;
        }
        sw.setChecked(ExtPrefs.innerGesture(this));
        sw.setOnCheckedChangeListener((b, on) -> {
            ExtPrefs.setInnerGesture(this, on);
            VolumeKeyService.syncInnerGesture();
            if (on && !InnerGesture.installed()) {
                toast("Text，TextNoneText");
            } else {
                toast(on ? "Text" : "Text");
            }
        });

        View animNative = findViewById(R.id.btnAnimNative);
        View animGlow = findViewById(R.id.btnAnimGlow);
        if (animNative != null && animGlow != null) {
            animNative.setOnClickListener(v -> pickGestureAnim(GestureAnim.STYLE_NATIVE));
            animGlow.setOnClickListener(v -> pickGestureAnim(GestureAnim.STYLE_GLOW));
            applyGestureAnim();
        }

        View gameApps = findViewById(R.id.btnGameGestureApps);
        if (gameApps != null) {
            gameApps.setOnClickListener(v -> openGameAppsDialog());
        }
        refreshGameAppsBtn();
    }

    /** 「[comment]」[comment]：[comment]"[comment]"，[comment]"[comment] N [comment]" */
    private void refreshGameAppsBtn() {
        TextView t = findViewById(R.id.btnGameGestureApps);
        if (t == null) {
            return;
        }
        int n = ExtPrefs.gameGestureApps(this).size();
        t.setText(n == 0 ? "Text" : "Text " + n + " Text");
    }

    /**
     * 「[comment]」[comment]：[comment]，[comment]
     * （[comment]、[comment]），[comment] {@link InnerGesture}。
     *
     * <p>[comment]（IO），[comment] {@link AppRepo#load} [comment]，[comment]。
     */
    private void openGameAppsDialog() {
        pool.execute(() -> {
            final List<AppRepo.Item> apps = new ArrayList<>();
            for (AppRepo.Item it : AppRepo.load(this)) {
                if (!getPackageName().equals(it.pkg) && !it.isPseudo()) {
                    apps.add(it);
                }
            }
            ui.post(() -> {
                if (isFinishing()) {
                    return;
                }
                if (apps.isEmpty()) {
                    toast("Text");
                    return;
                }
                showGameAppsDialog(apps);
            });
        });
    }

    private void showGameAppsDialog(final List<AppRepo.Item> apps) {
        final Set<String> picked = ExtPrefs.gameGestureApps(this);
        final Set<String> before = new HashSet<>(picked);
        final ListView lv = new ListView(this);
        lv.setDivider(null);
        final BaseAdapter ad = new BaseAdapter() {
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
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView;
                if (row == null) {
                    row = LayoutInflater.from(BetaActivity.this)
                            .inflate(R.layout.item_app_check, parent, false);
                }
                AppRepo.Item it = apps.get(position);
                ImageView icon = row.findViewById(R.id.icon);
                if (it.icon != null) {
                    icon.setImageDrawable(it.icon);
                }
                TextView label = row.findViewById(R.id.label);
                label.setText(it.label);
                CheckBox cb = row.findViewById(R.id.cb);
                cb.setChecked(picked.contains(it.pkg));
                return row;
            }
        };
        lv.setAdapter(ad);
        lv.setOnItemClickListener((parent, view, position, id) -> {
            String pkg = apps.get(position).pkg;
            if (picked.contains(pkg)) {
                picked.remove(pkg);
            } else {
                picked.add(pkg);
            }
            ad.notifyDataSetChanged();
        });
        new AlertDialog.Builder(this)
                .setTitle("Text：Text")
                .setView(lv)
                .setPositiveButton("Text", (d, w) -> {
                    if (!before.equals(picked)) {
                        for (AppRepo.Item it : apps) {
                            boolean on = picked.contains(it.pkg);
                            if (on != before.contains(it.pkg)) {
                                ExtPrefs.setGameGestureApp(this, it.pkg, on);
                            }
                        }
                        // [comment] ⇒ [comment]
                        InnerGesture.reevalMode(this);
                    }
                    refreshGameAppsBtn();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** [comment]。[comment]Yes[comment]，[comment] */
    private void pickGestureAnim(int style) {
        ExtPrefs.setGestureAnimStyle(this, style);
        applyGestureAnim();
    }

    /** [comment]：[comment]（[comment]） */
    private void applyGestureAnim() {
        int now = ExtPrefs.gestureAnimStyle(this);
        paintSegment(R.id.btnAnimNative, now == GestureAnim.STYLE_NATIVE);
        paintSegment(R.id.btnAnimGlow, now == GestureAnim.STYLE_GLOW);
    }

    private void paintSegment(int id, boolean on) {
        TextView t = findViewById(id);
        if (t == null) {
            return;
        }
        t.setBackgroundResource(on ? R.drawable.beta_btn_primary : R.drawable.beta_btn_ghost);
        t.setTextColor(on ? 0xFFFFFFFF : 0xE6FFFFFF);
    }

    /**
     * [comment]「[comment]」。
     *
     * <p>[comment]Yes[comment]<b>[comment]Settings</b>[comment]：[comment]Yes {@code Settings.Global.policy_control}，
     * [comment] {@link NavBarImmersive#apply}。[comment] —— [comment] NavStar [comment]
     * [comment]，[comment]"[comment]、[comment]"。
     */
    private void doNavImm(final boolean on) {
        if (!ShellRunner.isReady()) {
            toast("Shizuku Not Ready，Text");
            setNavImmSwitch(!on);           // Text，Text
            return;
        }
        final Context app = getApplicationContext();
        pool.execute(() -> {
            final String err = NavBarImmersive.apply(app, on);
            ui.post(() -> {
                toast(err != null ? err
                        : (on ? "Text" : "TextSettings"));
                refreshNavImmState();
            });
        });
    }

    /**
     * [comment]，[comment]（[comment]）。
     *
     * <p>⚠ [comment] —— [comment]Settings，[comment] NavStar [comment]
     * [comment]；[comment]，[comment]。
     *
     * <p>[comment]（Shizuku [comment]）[comment]：[comment]，
     * [comment]Yes[comment]。
     */
    private void refreshNavImmState() {
        if (!ShellRunner.isReady()) {
            return;
        }
        pool.execute(() -> {
            final String policy = NavBarImmersive.read();
            if (policy == null) {
                return;
            }
            final boolean on = NavBarImmersive.isOn(policy);
            ui.post(() -> {
                if (isFinishing()) {
                    return;
                }
                ExtPrefs.setNavImmOn(this, on);
                setNavImmSwitch(on);
            });
        });
    }

    /** [comment]，<b>[comment]</b>（[comment]） */
    private void setNavImmSwitch(boolean on) {
        Switch sw = findViewById(R.id.swNavImm);
        if (sw == null || sw.isChecked() == on) {
            return;
        }
        navImmSyncing = true;
        sw.setChecked(on);
        navImmSyncing = false;
    }

    /**
     * [comment]（[comment]）。
     *
     * <p>⚠ [comment] {@code zoneName} —— [comment] {@code zoneName(int)}
     * （[comment]Yes"[comment]/[comment]/[comment]"[comment]），[comment]。
     */
    private static String zoneWidthName(int pick) {
        switch (pick) {
            case 0:
                return "Text 1/4";
            case 2:
                return "Text 1/2";
            case 3:
                return "Text";
            default:
                return "Text 1/3";
        }
    }

    /**
     * [comment]「[comment]」。
     *
     * <p><b>[comment]，[comment]Yes[comment]</b>：[comment] {@link CutoutPolicy#POL_OFF}
     * （[comment]），[comment]Settings[comment]。
     *
     * <p>[comment] binder，[comment]，[comment] {@link #pool} [comment]，
     * [comment]Update[comment]。[comment]（[comment] / Shizuku [comment]）[comment]，[comment]，[comment]Success。
     */
    private void doApplyCutout() {
        if (!CutoutPolicy.supported()) {
            toast("Text Android Text，Text");
            refreshEdgeState();
            return;
        }
        if (!ShellRunner.isReady()) {
            toast("Shizuku Not Ready，Text");
            refreshEdgeState();
            return;
        }
        // [comment] prefs [comment]"[comment]"[comment]，[comment]（[comment]）
        ExtPrefs.setCutoutGlobal(this, true);
        final Context app = getApplicationContext();
        if (tvEdgeState != null) {
            tvEdgeState.setText("Text…（Text，Text）");
        }
        pool.execute(() -> {
            final String out = CutoutPolicy.applyAll(app, CutoutPolicy.POL_OFF);
            ui.post(() -> {
                toast(out.startsWith("ERR") ? out : ("Text · " + out));
                refreshEdgeState();
            });
        });
    }

    /**
     * [comment]「[comment]」Settings[comment]。
     *
     * <p>[comment]：[comment]"[comment]"，[comment]Yes[comment]。
     * [comment]「[comment]」[comment]「[comment]」[comment]（[comment]Yes[comment]），
     * [comment]「[comment]」[comment]。
     * [comment]，[comment]Settings[comment]。
     */
    private void doOpenNavMode() {
        if (!ShellRunner.isReady()) {
            toast("Shizuku Not Ready，Text");
            return;
        }
        toast("Text「Text」Settings…");
        pool.execute(() -> {
            // ⚠ $ [comment]，No[comment] shell [comment]，[comment]
            final String out = ShellRunner.run(
                    "am start -n 'com.android.settings/.Settings$NavigationBarSettingsActivity'"
                            + " || am start -a android.settings.DISPLAY_SETTINGS"
                            + " || am start -a android.settings.SETTINGS", 15);
            ui.post(() -> toast(out == null || out.contains("Error")
                    ? "Text，Text：Settings → Text → Text"
                    : "Text，Text「Text」Text"));
        });
    }

    /** 「[comment]」[comment]：[comment] + [comment] */
    /**
     * 「[comment]」[comment]Settings。
     *
     * <p>[comment]、[comment]（[comment]/[comment] × [comment]/[comment]/[comment]）、[comment]、[comment]。
     *
     * <p>⚠ [comment]"[comment]"[comment] ⇒ [comment]
     * {@link VolumeKeyService#refreshSidebar()}；[comment] prefs Yes[comment]
     * （[comment]Yes[comment]，[comment]）。
     */
    private void bindSidebarPanel() {
        RadioGroup anim = findViewById(R.id.rgSidebarAnim);
        if (anim != null) {
            int cur = ExtPrefs.sidebarStyle(this);
            anim.check(cur == 1 ? R.id.rbAnimGear
                    : (cur == 3 ? R.id.rbAnimWin : R.id.rbAnimRing));
            anim.setOnCheckedChangeListener((g, id) -> {
                int pick = id == R.id.rbAnimGear ? 1 : (id == R.id.rbAnimWin ? 3 : 0);
                if (pick == ExtPrefs.sidebarStyle(this)) {
                    return;
                }
                ExtPrefs.setSidebarStyle(this, pick);
                VolumeKeyService.refreshSidebar();
                toast("Text：" + animName(pick));
            });
        }

        RadioGroup side = findViewById(R.id.rgSidebarSide);
        if (side != null) {
            int cur = ExtPrefs.sidebarSide(this);
            side.check(cur == 0 ? R.id.rbSideLeft : R.id.rbSideRight);
            side.setOnCheckedChangeListener((g, id) -> {
                int pick = id == R.id.rbSideLeft ? 0 : 1;
                if (pick == ExtPrefs.sidebarSide(this)) {
                    return;
                }
                ExtPrefs.setSidebarSide(this, pick);
                VolumeKeyService.refreshSidebar();
                toast("Text" + (pick == 0 ? "Text" : "Text") + "Text");
            });
        }

        RadioGroup zone = findViewById(R.id.rgSidebarZone);
        if (zone != null) {
            int cur = ExtPrefs.sidebarZone(this);
            zone.check(cur == 0 ? R.id.rbZoneTop
                    : (cur == 2 ? R.id.rbZoneBottom : R.id.rbZoneMid));
            zone.setOnCheckedChangeListener((g, id) -> {
                int pick = id == R.id.rbZoneTop ? 0 : (id == R.id.rbZoneBottom ? 2 : 1);
                if (pick == ExtPrefs.sidebarZone(this)) {
                    return;
                }
                ExtPrefs.setSidebarZone(this, pick);
                VolumeKeyService.refreshSidebar();
                toast("Text" + zoneName(pick) + "Text");
            });
        }

        applySbItemsOrder();

        View grant = findViewById(R.id.btnNotifGrant);
        if (grant != null) {
            grant.setOnClickListener(v -> doGrantNotif());
        }
        refreshNotifState();
    }

    private static String zoneName(int z) {
        return z == 0 ? "Text" : (z == 2 ? "Text" : "Text");
    }

    /**
     * 「[comment]」[comment]：0=[comment] 1=[comment] 2=[comment] 3=[comment]。
     *
     * <p>[comment]Yes {@link ExtPrefs#SIDEBAR_ITEM_IDS} [comment]（[comment]）——
     * [comment]、[comment]，[comment]。
     */
    /** 「[comment]」[comment]（[comment]Notice[comment]） */
    private static String animName(int kind) {
        return kind == 1 ? "Text" : (kind == 3 ? "Text" : "Text");
    }

    /**
     * [comment]「[comment]」[comment]：[comment] i [comment]
     * {@link ExtPrefs#sidebarOrder(Context)} [comment] i [comment]。
     *
     * <p>⚠ [comment]Yes<b>[comment]</b>[comment]（cbSbItem0 ~ 3 [comment]Yes[comment] 1 ~ 4 [comment]），
     * [comment]Yes"[comment]" —— [comment] View。ViewGroup [comment] parent [comment]、
     * [comment] addView [comment]，[comment]。
     *
     * <p>[comment] {@code setChecked}，[comment]。
     */
    private void applySbItemsOrder() {
        final int[] cbIds = {R.id.cbSbItem0, R.id.cbSbItem1, R.id.cbSbItem2, R.id.cbSbItem3};
        final int[] upIds = {R.id.upSbItem0, R.id.upSbItem1, R.id.upSbItem2, R.id.upSbItem3};
        final int[] dnIds = {R.id.dnSbItem0, R.id.dnSbItem1, R.id.dnSbItem2, R.id.dnSbItem3};
        final int[] order = ExtPrefs.sidebarOrder(this);
        final boolean[] on = ExtPrefs.sidebarItemsOn(this);
        for (int i = 0; i < order.length && i < cbIds.length; i++) {
            final int act = order[i];
            final int row = i;      // lambda Text final，Text i Text
            final int at = ExtPrefs.itemIndex(act);
            CheckBox cb = findViewById(cbIds[i]);
            if (cb != null && at >= 0) {
                cb.setText(ExtPrefs.ringActionName(this, act));
                cb.setOnCheckedChangeListener(null);
                cb.setChecked(on[at]);
                cb.setOnCheckedChangeListener((b, isOn) -> setSbItem(act, isOn));
            }
            View up = findViewById(upIds[i]);
            if (up != null) {
                up.setOnClickListener(v -> moveSbItemAt(row, -1));
                dimSortArrow(up, i > 0);
            }
            View dn = findViewById(dnIds[i]);
            if (dn != null) {
                dn.setOnClickListener(v -> moveSbItemAt(row, 1));
                dimSortArrow(dn, i < order.length - 1);
            }
        }
    }

    /**
     * [comment] / Cancel[comment]。
     *
     * <p>⚠ [comment]Yes<b>[comment]</b>，[comment]Yes[comment] —— [comment]Yes[comment]
     * {@link ExtPrefs#SIDEBAR_ITEM_IDS} [comment]，[comment]None[comment]。
     */
    private void setSbItem(int act, boolean isOn) {
        int at = ExtPrefs.itemIndex(act);
        if (at < 0) {
            return;
        }
        boolean[] cur = ExtPrefs.sidebarItemsOn(this);
        cur[at] = isOn;
        ExtPrefs.setSidebarItemsOn(this, cur);
        VolumeKeyService.refreshSidebar();
        if (ExtPrefs.sidebarSlots(this).length == 0) {
            toast("Text，Text");
        }
    }

    /** [comment] at [comment]（dir = -1）/ [comment]（dir = +1）[comment] */
    private void moveSbItemAt(int at, int dir) {
        int[] order = ExtPrefs.sidebarOrder(this);
        int to = at + dir;
        if (at < 0 || at >= order.length || to < 0 || to >= order.length) {
            return;
        }
        int t = order[at];
        order[at] = order[to];
        order[to] = t;
        ExtPrefs.setSidebarOrder(this, order);
        applySbItemsOrder();
        VolumeKeyService.refreshSidebar();
    }

    /** [comment]、[comment]（[comment]"[comment]"、[comment]"[comment]"） */
    private static void dimSortArrow(View v, boolean usable) {
        v.setEnabled(usable);
        v.setAlpha(usable ? 1f : 0.22f);
    }

    /** [comment]（[comment]、[comment]） */
    private void refreshNotifState() {
        TextView tv = findViewById(R.id.tvNotifState);
        if (tv != null) {
            tv.setText("Text：" + CoverNotifListener.statusText(this));
        }
    }

    /**
     * [comment]「[comment]」。
     *
     * <p>[comment]Yes[comment]None[comment]**[comment]**[comment]，[comment]Settings[comment]「[comment] → [comment]」。
     * [comment] Shizuku [comment] {@code cmd notification allow_listener} [comment]。
     */
    private void doGrantNotif() {
        if (!ShellRunner.isReady()) {
            toast("Text Shizuku Text（TextSettings → Text → Text）");
            return;
        }
        toast("Text…");
        final Context ctx = this;
        pool.execute(() -> {
            String out = ShellRunner.run(CoverNotifListener.grantCmd(ctx), 12);
            android.util.Log.i("BetaActivity", "Text => " + out);
            try {
                Thread.sleep(700);
            } catch (InterruptedException ignored) {
            }
            CoverNotifListener.refreshAll();
            final boolean ok = CoverNotifListener.granted(ctx);
            runOnUiThread(() -> {
                refreshNotifState();
                toast(ok ? "Text" : "Text，TextSettingsText");
            });
        });
    }

    private void refreshEdgeState() {
        final TextView tv = tvEdgeState;
        if (tv == null) {
            return;
        }
        if (!CutoutPolicy.supported()) {
            tv.setText("Text（sdk=" + Build.VERSION.SDK_INT
                    + "，release=" + Build.VERSION.RELEASE + "），Text。");
            return;
        }
        tv.setText("Text…");
        final String probe = pickProbePackage();
        pool.execute(() -> {
            final int v = CutoutPolicy.read(this, probe);
            ui.post(() -> {
                if (isFinishing() || tvEdgeState == null) {
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Text　").append(probe).append(" Text=");
                if (v < 0) {
                    sb.append("Text（Shizuku Text？）");
                } else {
                    sb.append(v);
                    if (v == CutoutPolicy.POL_OFF) {
                        sb.append("（Text ⇒ Text）");
                    } else if (v == CutoutPolicy.POL_DEFAULT) {
                        sb.append("（DefaultText）");
                    }
                }
                sb.append(ExtPrefs.cutoutGlobal(this)
                        ? "\nText　Text" : "\nText　Text");
                sb.append("\nText，Text。");
                tvEdgeState.setText(sb.toString());
            });
        });
    }

    /**
     * [comment]。
     *
     * <p>[comment]<b>[comment]</b>：[comment]Settings[comment]，
     * [comment]。[comment]Settings —— [comment]。
     */
    private String pickProbePackage() {
        for (String p : new String[]{"com.tencent.mobileqq", "tv.danmaku.bili",
                "com.autonavi.minimap", "com.ss.android.ugc.aweme"}) {
            try {
                getPackageManager().getPackageInfo(p, 0);
                return p;
            } catch (Throwable ignored) {
            }
        }
        return "com.android.settings";
    }

    // ================================================================== [comment]

    /** 「Beta [comment]」[comment]，[comment] */
    private static final String FILE_INTRO = "extrot_beta";
    private static final String KEY_INTRO_OFF = "intro_off";
    /** [comment]"[comment]"[comment]Yes[comment] versionCode */
    private static final String KEY_INTRO_VER = "intro_off_ver";

    /**
     * [comment]。
     *
     * <p>[comment]「Default[comment]」[comment]：[comment]，[comment] ⓘ
     * （[comment]Yes[comment]）。
     *
     * <p>⚠ [comment]「[comment]」[comment]。[comment]Yes「[comment]，[comment] Beta [comment]
     * [comment]」—— [comment]Yes[comment]，[comment]「[comment]」[comment]，[comment]。
     * [comment]，[comment]：[comment]。
     */
    private void maybeShowIntro() {
        SharedPreferences sp = getSharedPreferences(FILE_INTRO, MODE_PRIVATE);
        boolean off = sp.getBoolean(KEY_INTRO_OFF, false)
                && sp.getInt(KEY_INTRO_VER, -1) == myVersionCode();
        if (off || isFinishing()) {
            return;
        }
        // [comment]Custom[comment]Yes setMessage + setMultiChoiceItems：[comment]
        // [comment]，[comment]（[comment]）。
        View body = getLayoutInflater().inflate(R.layout.dialog_beta_intro, null);
        final android.widget.CheckBox cb = body.findViewById(R.id.cbBetaIntroOff);
        new AlertDialog.Builder(this)
                .setTitle("Beta Text")
                .setView(body)
                .setPositiveButton("Text", (d, w) -> {
                    if (cb != null && cb.isChecked()) {
                        getSharedPreferences(FILE_INTRO, MODE_PRIVATE).edit()
                                .putBoolean(KEY_INTRO_OFF, true)
                                .putInt(KEY_INTRO_VER, myVersionCode())
                                .apply();
                    }
                })
                .show();
    }

    /** [comment] versionCode，[comment]"[comment]Yes[comment]Yes[comment]" */
    private int myVersionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Throwable t) {
            return -1;
        }
    }

    // ================================================================== [comment]Settings

    private void bindCoverPanel() {
        etW = findViewById(R.id.etCoverW);
        etH = findViewById(R.id.etCoverH);
        etDpi = findViewById(R.id.etCoverDpi);
        tvCoverState = findViewById(R.id.tvCoverState);
        listPresets = findViewById(R.id.listCoverPresets);

        View apply = findViewById(R.id.btnCoverApply);
        if (apply != null) {
            apply.setOnClickListener(v -> applyCoverSize());
        }
        View reset = findViewById(R.id.btnCoverReset);
        if (reset != null) {
            reset.setOnClickListener(v -> confirmResetCoverSize());
        }

        View addPreset = findViewById(R.id.btnPresetAdd);
        if (addPreset != null) {
            addPreset.setOnClickListener(v -> showPresetEditor(-1));
        }
        View resetPreset = findViewById(R.id.btnPresetReset);
        if (resetPreset != null) {
            resetPreset.setOnClickListener(v -> {
                presets = CoverPresets.resetToDefault(this, physW, physH, physDpi);
                renderPresets();
                toast("TextDefaultText");
            });
        }

        View rotate = findViewById(R.id.btnCoverRotate);
        if (rotate != null) {
            rotate.setOnClickListener(v -> doCoverRotate());
        }
        View rotateReset = findViewById(R.id.btnCoverRotateReset);
        if (rotateReset != null) {
            rotateReset.setOnClickListener(v -> doCoverRotateReset());
        }

        // ---- [comment]（v4.23）。[comment]「[comment] 90°」Yes[comment]：[comment]
        //      [comment]，[comment]（[comment]）。[comment] shell，
        //      [comment]；[comment]，[comment]。
        //      ⚠ [comment] setChecked [comment] —— [comment]"[comment]"（[comment]）。
        Switch coverAutoRot = findViewById(R.id.swCoverAutoRot);
        if (coverAutoRot != null) {
            coverAutoRot.setChecked(CoverAutoRot.on(this));
            coverAutoRot.setOnCheckedChangeListener((b, on) -> {
                if (coverRotSyncing) {
                    return;
                }
                pool.execute(() -> {
                    String err = CoverAutoRot.set(this, on);
                    ui.post(() -> {
                        if (err != null) {
                            toast(err);
                            // [comment] → [comment]，[comment]
                            coverRotSyncing = true;
                            coverAutoRot.setChecked(!on);
                            coverRotSyncing = false;
                        } else {
                            toast(on ? "Text：Text" : "Text：Text");
                        }
                        refreshCoverState();
                    });
                });
            });
        }

        // ---- [comment]
        tvLauncherState = findViewById(R.id.tvLauncherState);

        View openLauncher = findViewById(R.id.btnLauncherOpen);
        if (openLauncher != null) {
            openLauncher.setOnClickListener(v -> openLauncherConfig());
        }

        View pinHelp = findViewById(R.id.btnLauncherPin);
        if (pinHelp != null) {
            pinHelp.setOnClickListener(v -> showPinHelp());
        }
    }

    /**
     * 「[comment]」。
     *
     * <p>[comment]「[comment]」[comment]（{@link SecondaryLauncher#launchOwn}），
     * [comment]Yes[comment] {@code startActivity}：[comment]Yes[comment] GridView + [comment]，
     * [comment]「[comment]」。
     *
     * <p>[comment] Shizuku —— [comment] {@code launchOwn} [comment]；
     * [comment]（[comment]），[comment]，[comment]Yes[comment]。
     */
    private void openLauncherConfig() {
        int display = CoverDisplay.id(this);
        if (display > 0) {
            pool.execute(() -> {
                boolean ok = SecondaryLauncher.launchOwn(this, display, CoverLauncherActivity.class);
                if (!ok) {
                    ui.post(() -> startActivity(new Intent(this, CoverLauncherActivity.class)));
                }
            });
        } else {
            startActivity(new Intent(this, CoverLauncherActivity.class));
        }
    }

    /**
     * 「[comment]」。
     *
     * <p>[comment]Settings[comment]，adb [comment] —— [comment]Yes[comment]，
     * [comment]。[comment]，[comment]。
     */
    private void showPinHelp() {
        if (isFinishing()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Text")
                .setMessage(R.string.info_launcher_pin)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /** 「[comment]」[comment]（[comment]、[comment]） */
    private void refreshCoverAutoRotSwitch() {
        Switch sw = findViewById(R.id.swCoverAutoRot);
        if (sw == null) {
            return;
        }
        boolean on = CoverAutoRot.on(this);
        if (sw.isChecked() != on) {
            coverRotSyncing = true;
            sw.setChecked(on);
            coverRotSyncing = false;
        }
    }

    /** [comment]「[comment]」[comment] */
    private void refreshLauncherState() {
        if (tvLauncherState == null) {
            return;
        }
        int n = LauncherPrefs.apps(this).size();
        if (n == 0) {
            tvLauncherState.setText("Text。Text「Text」，Text。");
        } else {
            tvLauncherState.setText("Text " + n + " Text。Text，Text。");
        }
    }

    /** [comment]（[comment]、[comment]） */
    private void refreshCoverState() {
        pool.execute(() -> {
            CoverDisplay.State st = CoverDisplay.read(this);
            ui.post(() -> renderCoverState(st));
        });
    }

    private void renderCoverState(CoverDisplay.State st) {
        if (isFinishing() || tvCoverState == null) {
            return;
        }
        if (st.displayId <= 0) {
            tvCoverState.setText("Text。");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Text   ").append(nz(st.physSize, "?")).append("   ").append(st.physDpi).append("dpi\n");
        sb.append("Text   ");
        if (st.overridden()) {
            sb.append(st.hasSizeOverride() ? st.overrideSize : "（Text）");
            sb.append("   ");
            sb.append(st.hasDpiOverride() ? st.overrideDpi + "dpi" : "（Text DPI）");
        } else {
            sb.append("None");
        }
        sb.append('\n');
        sb.append("Text       ").append(CoverDisplay.degreeText(st.rotation));
        if (st.rotation != CoverDisplay.ROT_FREE) {
            sb.append("（Text）");
        }
        tvCoverState.setText(sb.toString());

        // [comment]，[comment]Default[comment]
        if (st.physSize != null && st.physSize.contains("x")) {
            String[] p = st.physSize.split("x");
            try {
                physW = Integer.parseInt(p[0].trim());
                physH = Integer.parseInt(p[1].trim());
                physDpi = st.physDpi;
            } catch (Throwable ignored) {
            }
        }

        // [comment] —— [comment]
        if (etW != null && TextUtils.isEmpty(etW.getText())) {
            String w = st.hasSizeOverride() ? st.overrideSize : st.physSize;
            if (w != null && w.contains("x")) {
                String[] p = w.split("x");
                etW.setText(p[0].trim());
                if (etH != null) {
                    etH.setText(p[1].trim());
                }
            }
        }
        if (etDpi != null && TextUtils.isEmpty(etDpi.getText())) {
            int d = st.hasDpiOverride() ? st.overrideDpi : st.physDpi;
            etDpi.setText(String.valueOf(d));
        }

        if (presets == null) {
            presets = CoverPresets.load(this, physW, physH, physDpi);
            renderPresets();
        }
    }

    private static String nz(String s, String fallback) {
        return s == null ? fallback : s;
    }

    private void applyCoverSize() {
        if (needShizuku()) {
            return;
        }
        int w = parse(etW);
        int h = parse(etH);
        int dpi = parse(etDpi);
        if (w <= 0 || h <= 0) {
            toast("Text「Text」Text「Text」Text");
            return;
        }
        pool.execute(() -> {
            String err = CoverDisplay.applySize(this, w, h, dpi);
            ui.post(() -> {
                toast(err == null ? ("Text " + w + "×" + h + (dpi > 0 ? " / " + dpi + "dpi" : "")) : err);
                refreshCoverState();
            });
        });
    }

    private void confirmResetCoverSize() {
        if (needShizuku()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("TextDefault")
                .setMessage("Text DPI Text？")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Text", (d, w) -> pool.execute(() -> {
                    String err = CoverDisplay.resetSize(this, true);
                    ui.post(() -> {
                        toast(err == null ? "TextDefault" : err);
                        if (etW != null) {
                            etW.setText("");
                        }
                        if (etH != null) {
                            etH.setText("");
                        }
                        if (etDpi != null) {
                            etDpi.setText("");
                        }
                        refreshCoverState();
                    });
                }))
                .show();
    }

    private int parse(EditText e) {
        if (e == null) {
            return 0;
        }
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (Throwable t) {
            return 0;
        }
    }

    // ---------------------------------------------------------------- [comment]

    private void renderPresets() {
        if (listPresets == null) {
            return;
        }
        listPresets.removeAllViews();
        if (presets == null) {
            return;
        }
        for (int i = 0; i < presets.size(); i++) {
            listPresets.addView(buildPresetRow(presets.get(i), i));
        }
        if (presets.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Text，Text「＋ Add」Text。");
            tv.setTextColor(0x8CFFFFFF);
            tv.setTextSize(13);
            tv.setPadding(0, dp(6), 0, 0);
            listPresets.addView(tv);
        }
    }

    private View buildPresetRow(final CoverPresets.Preset p, final int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.beta_row);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        row.setLayoutParams(lp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextColor(0xFFF0F0F0);
        name.setTextSize(14);
        col.addView(name);

        TextView sum = new TextView(this);
        sum.setText(p.summary());
        sum.setTextColor(0x8CFFFFFF);
        sum.setTextSize(12);
        col.addView(sum);

        row.addView(col);

        TextView hint = new TextView(this);
        hint.setText("Text");
        hint.setTextColor(0xFF7FC77F);
        hint.setTextSize(12);
        row.addView(hint);

        row.setOnClickListener(v -> applyPreset(p));
        row.setOnLongClickListener(v -> {
            confirmDeletePreset(index);
            return true;
        });
        return row;
    }

    private void applyPreset(CoverPresets.Preset p) {
        if (needShizuku()) {
            return;
        }
        if (etW != null) {
            etW.setText(String.valueOf(p.w));
        }
        if (etH != null) {
            etH.setText(String.valueOf(p.h));
        }
        if (etDpi != null) {
            etDpi.setText(String.valueOf(p.dpi));
        }
        pool.execute(() -> {
            String err = CoverDisplay.applySize(this, p.w, p.h, p.dpi);
            ui.post(() -> {
                toast(err == null ? ("Text「" + p.name + "」") : err);
                refreshCoverState();
            });
        });
    }

    private void confirmDeletePreset(final int index) {
        if (presets == null || index < 0 || index >= presets.size()) {
            return;
        }
        final CoverPresets.Preset p = presets.get(index);
        new AlertDialog.Builder(this)
                .setTitle("DeleteText")
                .setMessage("Text「" + p.name + "」（" + p.summary() + "）？")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Delete", (d, w) -> {
                    CoverPresets.removeAt(this, presets, index);
                    renderPresets();
                })
                .show();
    }

    /**
     * Add / Edit[comment]。
     *
     * <p>[comment]：[comment]，[comment] xml [comment]
     * （[comment] res/layout [comment]，[comment]）。
     *
     * @param editIndex [comment] -1 [comment]Add
     */
    private void showPresetEditor(final int editIndex) {
        // [comment] presets Yes null（[comment]Yes[comment]），[comment]，
        // [comment]「＋ Add」[comment]
        if (presets == null) {
            presets = CoverPresets.load(this, physW, physH, physDpi);
            renderPresets();
        }
        if (editIndex < 0 && presets.size() >= CoverPresets.MAX) {
            toast("Text " + CoverPresets.MAX + " Text");
            return;
        }
        final CoverPresets.Preset editing =
                (editIndex >= 0 && presets != null && editIndex < presets.size())
                        ? presets.get(editIndex) : null;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(8), pad, 0);

        final EditText name = new EditText(this);
        name.setHint("Text，Text Text");
        if (editing != null) {
            name.setText(editing.name);
        }
        box.addView(name);

        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        sizeRow.setPadding(0, dp(10), 0, 0);

        final EditText w = new EditText(this);
        w.setHint("Text");
        w.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        w.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sizeRow.addView(w);

        final EditText h = new EditText(this);
        h.setHint("Text");
        h.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        h.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sizeRow.addView(h);

        final EditText dpi = new EditText(this);
        dpi.setHint("DPI，Text");
        dpi.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        dpi.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sizeRow.addView(dpi);
        box.addView(sizeRow);

        if (editing != null) {
            w.setText(String.valueOf(editing.w));
            h.setText(String.valueOf(editing.h));
            dpi.setText(editing.dpi > 0 ? String.valueOf(editing.dpi) : "");
        } else {
            // Add[comment]：[comment]
            w.setText(String.valueOf(physW > 0 ? physW : 748));
            h.setText(String.valueOf(physH > 0 ? physH : 720));
            dpi.setText(String.valueOf(physDpi > 0 ? physDpi : 340));
        }

        new AlertDialog.Builder(this)
                .setTitle(editIndex >= 0 ? "EditText" : "AddText")
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Save", (d, which) -> {
                    String nm = name.getText().toString().trim();
                    int ww = parseIntText(w);
                    int hh = parseIntText(h);
                    int dd = parseIntText(dpi);
                    if (TextUtils.isEmpty(nm)) {
                        toast("Text");
                        return;
                    }
                    if (ww <= 0 || hh <= 0) {
                        toast("Text");
                        return;
                    }
                    if (editing != null) {
                        editing.name = nm;
                        editing.w = ww;
                        editing.h = hh;
                        editing.dpi = dd;
                        CoverPresets.save(this, presets);
                        renderPresets();
                    } else {
                        CoverPresets.add(this, presets, new CoverPresets.Preset(nm, ww, hh, dd));
                        renderPresets();
                    }
                })
                .show();
    }

    private int parseIntText(EditText e) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (Throwable t) {
            return 0;
        }
    }

    // ---------------------------------------------------------------- [comment]

    private void doCoverRotate() {
        if (needShizuku()) {
            return;
        }
        pool.execute(() -> {
            int deg = CoverDisplay.rotate(this);
            ui.post(() -> {
                toast(deg < 0 ? "TextFailed" : ("Text " + CoverDisplay.degreeText(deg)));
                refreshCoverState();
            });
        });
    }

    private void doCoverRotateReset() {
        if (needShizuku()) {
            return;
        }
        pool.execute(() -> {
            boolean ok = CoverDisplay.resetRotation(this);
            ui.post(() -> {
                toast(ok ? "Text" : "TextFailed");
                refreshCoverState();
            });
        });
    }

    // ================================================================== [comment]

    private void bindQuickPanel() {
        tvAssistState = findViewById(R.id.tvAssistState);
        tvVolumeState = findViewById(R.id.tvVolumeState);

        // 「[comment]」：[comment]，[comment]Settings[comment]
        keyTarget = QuickPrefs.sideKey(this);
        View keyAssist = findViewById(R.id.btnKeyAssist);
        if (keyAssist != null) {
            keyAssist.setOnClickListener(v -> selectKey(QuickPrefs.SIDE_ASSIST));
        }
        View keyVolume = findViewById(R.id.btnKeyVolume);
        if (keyVolume != null) {
            keyVolume.setOnClickListener(v -> selectKey(QuickPrefs.SIDE_VOL_UP));
        }
        View keyVolumeDown = findViewById(R.id.btnKeyVolumeDown);
        if (keyVolumeDown != null) {
            keyVolumeDown.setOnClickListener(v -> selectKey(QuickPrefs.SIDE_VOL_DOWN));
        }
        tvExternalAction = findViewById(R.id.tvExternalAction);

        View assistPick = findViewById(R.id.btnAssistPick);
        if (assistPick != null) {
            assistPick.setOnClickListener(v -> pickAction(QuickPrefs.SIDE_ASSIST));
        }
        View assistSet = findViewById(R.id.btnAssistSet);
        if (assistSet != null) {
            assistSet.setOnClickListener(v -> confirmSetAssistant());
        }
        View assistTest = findViewById(R.id.btnAssistTest);
        if (assistTest != null) {
            assistTest.setOnClickListener(v -> runAction(QuickPrefs.assistAction(this)));
        }

        View volPick = findViewById(R.id.btnVolumePick);
        if (volPick != null) {
            volPick.setOnClickListener(v -> pickAction(keyTarget));
        }
        View volA11y = findViewById(R.id.btnVolumeA11y);
        if (volA11y != null) {
            volA11y.setOnClickListener(v -> openAccessibilitySettings());
        }
        View volTest = findViewById(R.id.btnVolumeTest);
        if (volTest != null) {
            volTest.setOnClickListener(v -> runAction(currentVolumeAction()));
        }

        View copy = findViewById(R.id.btnCopyAction);
        if (copy != null) {
            copy.setOnClickListener(v -> copyActionToClipboard());
        }

        // Custom[comment]
        listCustomCmds = findViewById(R.id.listCustomCmds);
        View allowRestricted = findViewById(R.id.btnAllowRestricted);
        if (allowRestricted != null) {
            allowRestricted.setOnClickListener(v -> confirmAllowRestricted(false));
        }
        View grantSecure = findViewById(R.id.btnGrantSecure);
        if (grantSecure != null) {
            grantSecure.setOnClickListener(v -> doGrantSecure());
        }
        View addCmd = findViewById(R.id.btnCustomAdd);
        if (addCmd != null) {
            addCmd.setOnClickListener(v -> showCmdEditor(null));
        }
        applyKeySelection();
        renderCustomCmds();
    }

    // ------------------------------------------------------------ [comment]

    /** [comment]「[comment]」[comment]；[comment]Settings */
    private void selectKey(String target) {
        if (keyTarget.equals(target)) {
            return;
        }
        keyTarget = target;
        QuickPrefs.setSideKey(this, target);
        applyKeySelection();
        refreshQuickState();
    }

    /** [comment]（「[comment]」[comment]）；[comment]Yes[comment] */
    private String currentVolumeAction() {
        return QuickPrefs.SIDE_VOL_DOWN.equals(keyTarget)
                ? QuickPrefs.volumeDownAction(this)
                : QuickPrefs.volumeUpAction(this);
    }

    /**
     * [comment]Settings，[comment] chip [comment]。
     *
     * <p>[comment]Settings[comment]（id [comment]），[comment]Yes[comment] ——
     * [comment] refreshQuickState [comment]。
     *
     * <p>[comment]<b>[comment]</b>[comment]：[comment]（[comment]），
     * [comment] chip [comment]。
     */
    private void applyKeySelection() {
        boolean isAssist = QuickPrefs.SIDE_ASSIST.equals(keyTarget);
        showPanel(R.id.keyAssistBlock, isAssist);
        showPanel(R.id.keyVolumeBlock, !isAssist);
        chipSelected(findViewById(R.id.btnKeyAssist), isAssist);
        chipSelected(findViewById(R.id.btnKeyVolume), QuickPrefs.SIDE_VOL_UP.equals(keyTarget));
        chipSelected(findViewById(R.id.btnKeyVolumeDown), QuickPrefs.SIDE_VOL_DOWN.equals(keyTarget));

        boolean down = QuickPrefs.SIDE_VOL_DOWN.equals(keyTarget);
        View pick = findViewById(R.id.btnVolumePick);
        if (pick instanceof TextView) {
            ((TextView) pick).setText(down ? "Text" : "Text");
        }
        View test = findViewById(R.id.btnVolumeTest);
        if (test instanceof TextView) {
            ((TextView) test).setText(down ? "Text" : "Text");
        }
    }

    /** [comment] chip [comment]，[comment] */
    private void chipSelected(View v, boolean on) {
        if (v == null) {
            return;
        }
        v.setBackgroundResource(on ? R.drawable.beta_btn_primary : R.drawable.beta_tab_bg);
        if (v instanceof TextView) {
            ((TextView) v).setTextColor(on ? 0xFFFFFFFF : 0xB3FFFFFF);
        }
    }

    /** [comment] */
    private void refreshQuickState() {
        boolean a11y = QuickPrefs.isAccessibilityEnabled(this);
        if (tvVolumeState != null) {
            tvVolumeState.setText(
                    "NoneText：" + (a11y ? "TextEnable" : "TextEnable（TextEnableText）")
                            + "\nText：" + QuickActions.label(this, QuickPrefs.volumeUpAction(this))
                            + "\nText：" + QuickActions.label(this, QuickPrefs.volumeDownAction(this))
                            + (QuickPrefs.isVolumeKeyEnabled(this)
                                    ? "" : "\n（Text，Text）"));
        }
        if (tvAssistState != null) {
            tvAssistState.setText("Text：" + QuickActions.label(this, QuickPrefs.assistAction(this))
                    + "\nText：Text…");
        }
        if (tvExternalAction != null) {
            tvExternalAction.setText(QuickActions.ACTION_RUN
                    + "\n  " + QuickActions.EXTRA_ACTION + " = toggle_fold | cover_home |"
                    + " cover_rotate | cover_rotate_reset | custom:<id>");
        }

        // [comment]Yes[comment]，[comment] shell [comment]
        if (ShellRunner.isReady()) {
            pool.execute(() -> {
                final String cur = ShellRunner.run("settings get secure assistant").trim();
                ui.post(() -> {
                    if (tvAssistState != null && !isFinishing()) {
                        tvAssistState.setText("Text：" + QuickActions.label(this, QuickPrefs.assistAction(this))
                                + "\nText：" + (TextUtils.isEmpty(cur) || "null".equals(cur) ? "（TextSettings）" : cur));
                    }
                });
            });
        }
    }

    private TextView tvExternalAction;

    /**
     * [comment]。
     *
     * @param target {@link QuickPrefs#SIDE_ASSIST} / {@link QuickPrefs#SIDE_VOL_UP}
     *               / {@link QuickPrefs#SIDE_VOL_DOWN}
     */
    private void pickAction(final String target) {
        final boolean forAssist = QuickPrefs.SIDE_ASSIST.equals(target);
        final boolean forDown = QuickPrefs.SIDE_VOL_DOWN.equals(target);

        // [comment] = [comment] + Custom[comment]，[comment] options [comment]、[comment] ALL
        final String[] base = QuickActions.options(this);
        // [comment]「None」= [comment]，[comment]。
        // [comment]：[comment]"[comment]"[comment]，[comment]「None」[comment]，
        // [comment]Settings[comment] —— [comment]。
        final String[] keys;
        if (forAssist) {
            keys = base;
        } else {
            keys = new String[base.length + 1];
            keys[0] = QuickActions.KEY_NONE;
            System.arraycopy(base, 0, keys, 1, base.length);
        }

        final String[] labels = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            labels[i] = QuickActions.label(this, keys[i]);
        }

        final String current = forAssist ? QuickPrefs.assistAction(this)
                : (forDown ? QuickPrefs.volumeDownAction(this) : QuickPrefs.volumeUpAction(this));
        int checked = 0;
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(current)) {
                checked = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(forAssist ? "Text"
                        : (forDown ? "Text" : "Text"))
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    String key = keys[which];
                    if (forAssist) {
                        QuickPrefs.setAssistAction(this, key);
                    } else if (forDown) {
                        QuickPrefs.setVolumeDownAction(this, key);
                    } else {
                        QuickPrefs.setVolumeUpAction(this, key);
                    }
                    d.dismiss();
                    refreshQuickState();
                })
                .show();
    }

    /**
     * [comment]「[comment]」。
     *
     * <p>[comment]Yes「[comment]」[comment]：[comment]Settings[comment]
     * 「[comment] / [comment] / None」，[comment]。
     *
     * <p>⚠️ [comment]，[comment]（[comment]Yes
     * 「[comment]」），[comment]。
     */
    private void confirmSetAssistant() {
        if (!ShellRunner.isReady()) {
            toast("Shizuku Not Ready，Text");
            return;
        }
        final String target = getPackageName() + "/" + QuickActionActivity.class.getName();
        pool.execute(() -> {
            final String cur = ShellRunner.run("settings get secure assistant").trim();
            ui.post(() -> new AlertDialog.Builder(this)
                    .setTitle("Text")
                    .setMessage("Text：\n" + (TextUtils.isEmpty(cur) ? "（TextSettings）" : cur)
                            + "\n\nText：\n" + target
                            + "\n\nText，Text。"
                            + "SettingsText「Settings → Text → Text」Text「Text」，"
                            + "Text。")
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton("Text", (d, w) -> pool.execute(() -> {
                        ShellRunner.run("settings put secure assistant " + target);
                        final String after = ShellRunner.run("settings get secure assistant").trim();
                        ui.post(() -> {
                            toast(target.equals(after) ? "Text" : "SettingsText，Text：" + after);
                            refreshQuickState();
                        });
                    }))
                    .show());
        });
    }

    /**
     * 「EnableNone[comment]」[comment]。
     *
     * <p>⚠️ Android 13 [comment]，targetSdk ≥ 33 [comment]，[comment]「[comment]Settings」[comment]None[comment]
     * （Notice「[comment]Deny[comment]」）。One UI 8 [comment]Settings[comment]「Allow[comment]Settings」[comment]
     * （[comment]），UI [comment]None[comment] —— [comment]None[comment]Settings[comment]
     * {@code appops get … ACCESS_RESTRICTED_SETTINGS}，[comment]Allow[comment]Allow[comment]，[comment]。
     */
    private void openAccessibilitySettings() {
        if (!ShellRunner.isReady()) {
            toast("Shizuku Not Ready，Text");
            return;
        }
        toast("TextSettings…");
        pool.execute(() -> {
            final String st = ShellRunner.run(
                    "appops get " + getPackageName() + " ACCESS_RESTRICTED_SETTINGS").trim();
            final boolean allowed = st.contains(": allow");
            ui.post(() -> {
                if (allowed) {
                    jumpA11y();
                } else {
                    confirmAllowRestricted(true);
                }
            });
        });
    }

    /** [comment]None[comment]Settings */
    private void jumpA11y() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            // [comment] label，[comment]，[comment]
            toast("Text「" + getString(R.string.a11y_service_label) + "」TextEnable");
        } catch (Throwable t) {
            toast("TextNoneTextSettings，Text");
        }
    }

    /**
     * [comment]「Allow[comment]Settings」。
     *
     * @param jumpAfter [comment]Success[comment]YesNo[comment]None[comment]Settings
     *                  （[comment]「EnableNone[comment]」[comment]Yes true，[comment]Yes false）
     */
    private void confirmAllowRestricted(final boolean jumpAfter) {
        if (!ShellRunner.isReady()) {
            toast("Shizuku Not Ready，Text");
            return;
        }
        final String cmd = "appops set " + getPackageName() + " ACCESS_RESTRICTED_SETTINGS allow";
        new AlertDialog.Builder(this)
                .setTitle("AllowTextSettings")
                .setMessage("TextAllow「TextSettings」，TextDenyTextNoneText。"
                        + "\n\nText（Text shell Text）：\n" + cmd
                        + "\n\n" + (jumpAfter
                        ? "TextSuccessTextNoneTextSettings。"
                        : "TextSuccessTextNoneTextSettingsTextEnableText。"))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Text", (d, w) -> pool.execute(() -> {
                    ShellRunner.run(cmd);
                    final String after = ShellRunner.run(
                            "appops get " + getPackageName() + " ACCESS_RESTRICTED_SETTINGS").trim();
                    final boolean ok = after.contains(": allow");
                    ui.post(() -> {
                        if (ok) {
                            toast("TextAllowTextSettings");
                            if (jumpAfter) {
                                jumpA11y();
                            }
                        } else {
                            toast("Text，Text：" + after);
                        }
                    });
                }))
                .show();
    }

    /**
     * [comment]「[comment]Settings」[comment]（{@code WRITE_SECURE_SETTINGS}）。
     *
     * <p>[comment]Yes[comment] development [comment]，[comment]，[comment] shell [comment] {@code pm grant} [comment]。
     * [comment]，[comment]、[comment]。
     *
     * <p>[comment]Yes[comment]Settings[comment]（None[comment]），[comment]。
     */
    private void doGrantSecure() {
        final String perm = android.Manifest.permission.WRITE_SECURE_SETTINGS;
        if (checkSelfPermission(perm) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            toast("Text");
            return;
        }
        if (!ShellRunner.isReady()) {
            toast("Text Shizuku Text");
            return;
        }
        final String cmd = "pm grant " + getPackageName() + " " + perm;
        toast("Text…");
        pool.execute(() -> {
            String out = ShellRunner.run(cmd, 12);
            android.util.Log.i("BetaActivity", "TextSettingsText => " + out);
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
            final boolean ok = checkSelfPermission(perm)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
            ui.post(() -> toast(ok ? "TextSettingsText"
                    : ("Text" + (out.isEmpty() ? "" : "：" + out))));
        });
    }

    private void runAction(final String key) {
        // ★ None[comment] Shizuku —— [comment]Yes"Shizuku [comment]"，
        //   [comment]（[comment] QuickActions.run [comment]）。
        if (!ShellRunner.isReady() && !QuickActions.KEY_ADB_WIFI.equals(key)) {
            toast("Shizuku Not Ready，Text");
            return;
        }
        toast("Text…");
        pool.execute(() -> {
            String err = QuickActions.run(this, key);
            ui.post(() -> {
                toast(err == null
                        ? (QuickActions.label(this, key) + " Text")
                        : (QuickActions.label(this, key) + "：" + err));
                refreshCoverState();
            });
        });
    }

    // ------------------------------------------------------------ Custom[comment]

    /** [comment]Custom[comment] */
    private void renderCustomCmds() {
        if (listCustomCmds == null) {
            return;
        }
        listCustomCmds.removeAllViews();
        List<CustomCmds.Cmd> all = CustomCmds.list(this);
        if (all.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("TextCustomText，Text「＋ AddText」Text。");
            tv.setTextColor(0x8CFFFFFF);
            tv.setTextSize(13);
            tv.setPadding(0, dp(6), 0, 0);
            listCustomCmds.addView(tv);
            return;
        }
        for (CustomCmds.Cmd c : all) {
            listCustomCmds.addView(buildCmdRow(c));
        }
    }

    private View buildCmdRow(final CustomCmds.Cmd c) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.beta_row);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        row.setLayoutParams(lp);

        // [comment]：[comment] + [comment] key（[comment]）
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(this);
        name.setText(c.name);
        name.setTextColor(0xFFF0F0F0);
        name.setTextSize(14);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(name);

        TextView key = new TextView(this);
        key.setText(CustomCmds.KEY_PREFIX + c.id);
        key.setTextColor(0x59FFFFFF);
        key.setTextSize(11);
        head.addView(key);
        row.addView(head);

        TextView cmd = new TextView(this);
        cmd.setText(c.cmd);
        cmd.setTextColor(0x8CFFFFFF);
        cmd.setTextSize(12);
        cmd.setTypeface(Typeface.MONOSPACE);
        row.addView(cmd);

        // [comment]，[comment]
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(8);
        btns.setLayoutParams(blp);

        TextView run = cmdBtn("Text", v -> runAction(CustomCmds.KEY_PREFIX + c.id));
        TextView edit = cmdBtn("Edit", v -> showCmdEditor(c));
        TextView del = cmdBtn("Delete", v -> confirmDeleteCmd(c));
        ((LinearLayout.LayoutParams) run.getLayoutParams()).rightMargin = dp(8);
        ((LinearLayout.LayoutParams) edit.getLayoutParams()).rightMargin = dp(8);
        btns.addView(run);
        btns.addView(edit);
        btns.addView(del);
        row.addView(btns);
        return row;
    }

    /** [comment]（[comment]） */
    private TextView cmdBtn(String text, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(13);
        b.setTextColor(0xE6FFFFFF);
        b.setBackgroundResource(R.drawable.beta_btn_ghost);
        b.setPadding(0, dp(9), 0, dp(9));
        b.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        b.setOnClickListener(l);
        return b;
    }

    /** Add（editing = null）[comment]Edit[comment]Custom[comment] */
    private void showCmdEditor(final CustomCmds.Cmd editing) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(8), pad, 0);

        final EditText name = new EditText(this);
        name.setHint("Text，Text Text");
        if (editing != null) {
            name.setText(editing.name);
        }
        box.addView(name);

        final EditText cmd = new EditText(this);
        cmd.setHint("shell Text，Text svc bluetooth disable");
        cmd.setTypeface(Typeface.MONOSPACE);
        cmd.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (editing != null) {
            cmd.setText(editing.cmd);
        }
        box.addView(cmd);

        new AlertDialog.Builder(this)
                .setTitle(editing == null ? "AddText" : "EditText")
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Save", (d, w) -> {
                    String c1 = cmd.getText().toString().trim();
                    if (c1.isEmpty()) {
                        toast("Text");
                        return;
                    }
                    CustomCmds.Cmd item = (editing == null) ? new CustomCmds.Cmd() : editing;
                    item.name = name.getText().toString().trim();
                    item.cmd = c1;
                    CustomCmds.put(this, item);
                    renderCustomCmds();
                    refreshQuickState();
                })
                .show();
    }

    private void confirmDeleteCmd(final CustomCmds.Cmd c) {
        new AlertDialog.Builder(this)
                .setTitle("DeleteText")
                .setMessage("Delete「" + c.name + "」？\n\nText，Text「CustomText（TextDelete）」。")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Delete", (d, w) -> {
                    CustomCmds.remove(this, c.id);
                    renderCustomCmds();
                    refreshQuickState();
                })
                .show();
    }

    private void copyActionToClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("action", QuickActions.ACTION_RUN));
                toast("Text：" + QuickActions.ACTION_RUN);
            }
        } catch (Throwable t) {
            toast("TextFailed");
        }
    }
}
