package com.wb.extrotator;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final int REQ_SHIZUKU = 4242;
    private static final int REQ_NOTI = 101;

    private static final int[] ROT_ALL = {0, 1, 2, 3};
    private static final String[] LAB_ALL = {"0°", "90°", "180°", "270°"};
    private static final int[] ROT_PORT = {1, 3};
    private static final String[] LAB_PORT = {"90°", "270°"};

    /**
     * 「[comment]」[comment]。
     *
     * <p>[comment]，[comment]Yes {@link Prefs#isLockPhoneRotation()} [comment]
     * {@link Prefs#getPhoneRotation()} [comment] —— [comment]「Save[comment]Settings」[comment]、
     * [comment]、[comment]。
     */
    private static final int[] DIR_VALS = {0, 1, 2};
    private static final String[] DIR_LABS = {"Text", "Text", "Text"};
    private static final int DIR_LAND = 0, DIR_PORT = 1, DIR_AUTO = 2;

    /** 「[comment]」[comment]：[comment] / [comment]（[comment]）/ [comment]（[comment]） */
    private static final int[] BOOT_VALS = {Prefs.BOOT_NONE, Prefs.BOOT_FOLDED, Prefs.BOOT_UNFOLDED};
    private static final String[] BOOT_LABS = {"TextSettings", "Text", "Text"};

    /** [comment]：[comment]，[comment]Deny */
    private static final int MIN_RES = 480;

    private Prefs prefs;
    private DisplayManager dm;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvShizuku, tvRoot, tvInfo, tvLog, tvResState, tvResHint;
    private Switch swEnable, swResAsk, swResAutoReset, swResAutoMatch;
    private Button btnResToggle;
    private RadioGroup rgTabs;
    private LinearLayout panelAuto, panelRes, panelState;
    private Spinner spPortrait, spLandscape, spScreenDir, spBootMode;
    private CheckBox cbFixToUser;
    private EditText etResW, etResH, etResDpi;
    private TextView tvFoldState;

    /** [comment] / [comment] */
    private Spinner spPeakHz, spMinHz;
    private TextView tvHzState;

    /** [comment] 1 · [comment] [comment]「[comment]」 */
    private Switch swQASync;
    /** [comment]「[comment]」（v4.23）。[comment] swQASync Yes[comment]，[comment]、[comment] */
    private Switch swQACoverRot;

    /** 「[comment] → [comment]」[comment]「[comment]None[comment]」[comment]（v4.20 Yes[comment]，v4.22 [comment]） */
    private Button btnQAAdbWifi;

    /** [comment] 4 · [comment] */
    private LinearLayout panelDual;
    private TextView tvDualState, tvSubState, tvDexInfo;

    /** [comment] · [comment]SaveSettings（[comment]、「[comment]Save」[comment]「[comment]」[comment]） */
    private TextView tvProfileHead;
    private LinearLayout profileTable;
    private Button btnSaveProfile, btnClearProfile;

    /** [comment]「[comment]」[comment]，[comment] */
    private long lockToggleAt = 0L;
    /** [comment]「[comment]」[comment] */
    private int lockSyncStrikes = 0;

    /** [comment]，[comment]"[comment]/[comment]"[comment] */
    private static final int COLOR_ON = 0xFF2E7D32;
    private static final int COLOR_OFF = 0xFF757575;

    private boolean binding = true;

    private interface IntSetter {
        void set(int v);
    }

    private final Runnable uiTick = new Runnable() {
        @Override
        public void run() {
            updateInfo();
            // [comment]Settings（[comment]），[comment]
            syncFromPrefs();
            // 「[comment]SaveSettings」[comment]，[comment]
            if (panelAuto != null && panelAuto.getVisibility() == View.VISIBLE) {
                refreshProfileUi();
            }
            handler.postDelayed(this, 1000L);
        }
    };

    private final Shizuku.OnRequestPermissionResultListener permListener =
            (requestCode, grantResult) -> runOnUiThread(() -> {
                refreshShizukuState();
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    toast("Shizuku TextSuccess");
                    if (prefs.shouldKeepRunning()) {
                        RotationService.start(this);
                    }
                } else {
                    toast("TextDeny");
                }
            });

    private final Shizuku.OnBinderReceivedListener binderReceivedListener =
            () -> runOnUiThread(this::refreshShizukuState);

    private final Shizuku.OnBinderDeadListener binderDeadListener =
            () -> runOnUiThread(this::refreshShizukuState);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new Prefs(this);
        dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);

        tvShizuku = findViewById(R.id.tvShizuku);
        tvRoot = findViewById(R.id.tvRoot);
        tvInfo = findViewById(R.id.tvInfo);
        tvLog = findViewById(R.id.tvLog);
        tvResState = findViewById(R.id.tvResState);
        tvResHint = findViewById(R.id.tvResHint);
        tvProfileHead = findViewById(R.id.tvProfileHead);
        profileTable = findViewById(R.id.profileTable);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnClearProfile = findViewById(R.id.btnClearProfile);
        swEnable = findViewById(R.id.swEnable);
        btnResToggle = findViewById(R.id.btnResToggle);
        swResAsk = findViewById(R.id.swResAsk);
        swResAutoReset = findViewById(R.id.swResAutoReset);
        swResAutoMatch = findViewById(R.id.swResAutoMatch);
        rgTabs = findViewById(R.id.rgTabs);
        panelAuto = findViewById(R.id.panelAuto);
        panelRes = findViewById(R.id.panelRes);
        panelState = findViewById(R.id.panelState);
        spPortrait = findViewById(R.id.spPortrait);
        spLandscape = findViewById(R.id.spLandscape);
        spScreenDir = findViewById(R.id.spScreenDir);
        spBootMode = findViewById(R.id.spBootMode);
        cbFixToUser = findViewById(R.id.cbFixToUser);
        etResW = findViewById(R.id.etResW);
        etResH = findViewById(R.id.etResH);
        etResDpi = findViewById(R.id.etResDpi);
        tvFoldState = findViewById(R.id.tvFoldState);
        spPeakHz = findViewById(R.id.spPeakHz);
        spMinHz = findViewById(R.id.spMinHz);
        tvHzState = findViewById(R.id.tvHzState);

        // ---- [comment]（[comment]）
        swQASync = findViewById(R.id.swQASync);
        swQACoverRot = findViewById(R.id.swQACoverRot);

        // None[comment]；v4.22 [comment]（[comment]）。
        // ⚠ [comment]Yes[comment]（Settings.Global.ADB_WIFI_ENABLED）。v4.21 [comment]
        //   Shizuku：[comment]，[comment]None[comment]（[comment] AdbWifi）。
        //   [comment]、[comment] sleep，[comment]，[comment]。
        btnQAAdbWifi = findViewById(R.id.btnQAAdbWifi);
        if (btnQAAdbWifi != null) {
            btnQAAdbWifi.setOnClickListener(v -> openAdbWifiOnce());
        }
        // [comment]（v4.31）：[comment] + [comment] + [comment] + [comment]，[comment]
        findViewById(R.id.btnQACast).setOnClickListener(v -> onOneKeyCast());
        // 「[comment]/[comment]」[comment] —— [comment]，
        // [comment]。
        findViewById(R.id.btnQAOneKey).setOnClickListener(v -> onOneKeyDual());

        panelDual = findViewById(R.id.panelDual);
        tvDualState = findViewById(R.id.tvDualState);
        tvSubState = findViewById(R.id.tvSubState);
        tvDexInfo = findViewById(R.id.tvDexInfo);

        try {
            Shizuku.addRequestPermissionResultListener(permListener);
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
            Shizuku.addBinderDeadListener(binderDeadListener);
        } catch (Throwable ignored) {
        }

        // ---- [comment]（[comment]：0=[comment]，1=[comment]，2=[comment]，3=[comment]）
        rgTabs.setOnCheckedChangeListener((group, checkedId) ->
                showTab(checkedId == R.id.tabAuto ? 1
                        : checkedId == R.id.tabRes ? 2
                        : checkedId == R.id.tabDual ? 3 : 0));

        // ---- [comment]「Beta [comment]」[comment] → BetaActivity（[comment]，[comment]）
        bindBetaEntry();

        // ---- [comment] → 「[comment]Update」[comment]
        bindVersionEntry();

        // ---- [comment]。
        // [comment] manifest [comment] android:label [comment]（[comment]），[comment]Yes
        // TaskDescription，[comment] —— [comment]"[comment]、[comment]"[comment] label。
        // [comment]，[comment]、[comment]「Settings → [comment]」[comment]。
        setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name_en)));

        // ---- [comment]「ⓘ」[comment]。
        // v2.3 [comment]/[comment]（[comment]），[comment] ⓘ，
        // [comment]。[comment] strings.xml [comment] info_* [comment]。
        bindInfo(R.id.iSwEnable, "Text", R.string.info_sync);
        bindInfo(R.id.iQASync, "Text", R.string.info_sync);
        bindInfo(R.id.iQAAdbWifi, "NoneText", R.string.info_adb_wifi);
        bindInfo(R.id.iQACoverRot, "Text", R.string.info_cover_autorot);
        bindInfo(R.id.iQACast, "Text", R.string.info_qa_cast);
        bindInfo(R.id.iSwLockPhone, "Text", R.string.info_lock_phone);
        bindInfo(R.id.iBootMode, "Text", R.string.info_boot_mode);
        bindInfo(R.id.iFixToUser, "Text", R.string.info_fix_to_user);
        bindInfo(R.id.iCalib, "Text", R.string.info_calib);
        bindInfo(R.id.iRepair, "Text", R.string.info_repair);
        bindInfo(R.id.iProfile, "SaveTextSettings", R.string.info_profile);
        bindInfo(R.id.iResFields, "Text DPI", R.string.info_res_fields);
        bindInfo(R.id.iRefreshRate, "Text", R.string.info_refresh_rate);
        bindInfo(R.id.iResAutoMatch, "Text", R.string.info_res_automatch);
        bindInfo(R.id.iResTiming, "Text", R.string.info_res_timing);
        bindInfo(R.id.iFold, "Text/Text", R.string.info_fold);
        bindInfo(R.id.iShizuku, "Shizuku", R.string.info_shizuku);
        bindInfo(R.id.iRoot, "Root", R.string.info_root);
        bindInfo(R.id.iRealtime, "Text", R.string.info_realtime);
        // [comment] ⓘ [comment]「[comment]」[comment]；
        // [comment]，[comment]（[comment]）
        bindInfo(R.id.tvAboutDual, "Text", R.string.info_onekey);
        bindInfo(R.id.iSubPanel, "Text", R.string.info_beta_sub);
        bindInfo(R.id.iPseudo, "Text", R.string.info_pseudo);
        bindInfo(R.id.iDex, "DeX Text", R.string.info_dex);

        // ---- [comment]（「[comment]」[comment]）
        swEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (binding) {
                return;
            }
            setSyncEnabled(isChecked);
        });

        // ---- [comment]「[comment]」[comment]，[comment]。
        // [comment] setSyncEnabled + syncQuickSyncSwitch，[comment]。
        swQASync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (binding) {
                return;
            }
            setSyncEnabled(isChecked);
        });

        // ---- [comment]（v4.23）。[comment]「[comment]」Yes[comment]：[comment]Yes[comment]
        //      [comment]，[comment] —— [comment]，[comment] + [comment]。
        //      [comment] shell，[comment]。
        //      ⚠ [comment] setChecked [comment]，[comment] binding [comment]（[comment]）。
        if (swQACoverRot != null) {
            swQACoverRot.setChecked(CoverAutoRot.on(this));
            swQACoverRot.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (binding) {
                    return;
                }
                new Thread(() -> {
                    final String err = CoverAutoRot.set(MainActivity.this, isChecked);
                    runOnUiThread(() -> {
                        if (isFinishing()) {
                            return;
                        }
                        if (err != null) {
                            toast(err);
                        } else {
                            toast(isChecked ? "Text：Text" : "Text：Text");
                        }
                        syncQuickCoverRotSwitch();
                    });
                }, "cover-autorot-sw").start();
            });
        }

        // ---- [comment]
        bindSpinner(spPortrait, ROT_PORT, LAB_PORT, prefs.getPortraitAngle(), prefs::setPortraitAngle);
        bindSpinner(spLandscape, ROT_ALL, LAB_ALL, prefs.getLandscapeAngle(), prefs::setLandscapeAngle);

        // ---- [comment]
        // [comment]Yes「Disable[comment]，[comment]」[comment]，[comment]。[comment]Yes[comment]，
        // [comment]「Save[comment]Settings」[comment]「[comment]」[comment]、[comment] settings [comment]。
        bindSpinner(spScreenDir, DIR_VALS, DIR_LABS, dirValue(), this::applyScreenDir);

        // ---- [comment]（[comment]「[comment]」[comment]）
        bindSpinner(spBootMode, BOOT_VALS, BOOT_LABS, prefs.getBootMode(), prefs::setBootMode);

        cbFixToUser.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (binding) {
                return;
            }
            prefs.setFixToUserRotation(isChecked);
            onSettingChanged();
        });

        // ---- [comment]
        etResW.setText(String.valueOf(prefs.getResWidth()));
        etResH.setText(String.valueOf(prefs.getResHeight()));
        etResDpi.setText(String.valueOf(prefs.getResDensity()));
        TextWatcher resWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                saveResFields();
            }
        };
        etResW.addTextChangedListener(resWatcher);
        etResH.addTextChangedListener(resWatcher);
        etResDpi.addTextChangedListener(resWatcher);

        btnResToggle.setOnClickListener(v -> {
            saveResFields();
            boolean wantOn = !prefs.isResolutionEnabled();
            if (wantOn && !resFieldsValid()) {
                toast("Text " + MIN_RES + "，Text");
                return;
            }
            setResTakeover(wantOn, true);
        });

        swResAsk.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (binding) {
                return;
            }
            prefs.setAskOnConnect(isChecked);
        });

        swResAutoReset.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (binding) {
                return;
            }
            prefs.setAutoResetSize(isChecked);
        });

        swResAutoMatch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (binding) {
                return;
            }
            prefs.setAutoMatchExt(isChecked);
            toast(isChecked ? "Text（Text）"
                    : "TextDisableText，Text");
        });

        // [comment]
        findViewById(R.id.btnResFromExt).setOnClickListener(v -> fillResFromExt());

        findViewById(R.id.btnApplyRes).setOnClickListener(v -> {
            saveResFields();
            if (!resFieldsValid()) {
                toast("Text " + MIN_RES);
                return;
            }
            if (!ShellRunner.isReady()) {
                toast("Text Shizuku Text");
                return;
            }
            setResTakeover(true, true);
        });

        findViewById(R.id.btnResetRes).setOnClickListener(v -> {
            if (!ShellRunner.isReady()) {
                toast("Text Shizuku Text");
                return;
            }
            RotationService.resetSizeNow(this);
            toast("Text…");
        });

        // ---- [comment]（[comment]「[comment] DPI」[comment]）
        setupRefreshRate();

        findViewById(R.id.btnOverlay).setOnClickListener(v -> requestOverlayPermission());

        findViewById(R.id.btnTestPrompt).setOnClickListener(v -> RotationService.testPrompt(this));

        // ---- [comment]（[comment] DeviceStateManager）
        findViewById(R.id.btnFoldToggle).setOnClickListener(v -> onFoldToggle());
        findViewById(R.id.btnStateAuto).setOnClickListener(v -> onDeviceStateReset());

        // ---- [comment]：[comment]
        // [comment]：[comment]、[comment]、[comment]Yes[comment]；
        // [comment]，[comment]"[comment]"[comment]。
        findViewById(R.id.btnDualOneKey).setOnClickListener(v -> onOneKeyDual());
        // [comment]（v4.11 [comment] Beta [comment]）。[comment]
        // AppRepo.Item.pseudoDefaultOn [comment]，[comment] —— [comment]
        // [comment]Default[comment]、[comment]Default[comment]。
        findViewById(R.id.btnSubOpen).setOnClickListener(v -> doOpenSub());
        // v4.17：[comment]，[comment]。
        // [comment] drawable/ic_pseudo_*.xml [comment] fillColor，[comment]。
        bindPseudoToggle(R.id.swPseudoPad, AppRepo.PSEUDO_TOUCHPAD, false, 0xFF2F7DFF);
        bindPseudoToggle(R.id.swPseudoCast, AppRepo.PSEUDO_CAST, true, 0xFF8E6BE8);
        bindPseudoToggle(R.id.swPseudoStatus, AppRepo.PSEUDO_STATUS, true, 0xFF21BFD6);
        bindPseudoToggle(R.id.swPseudoPlayer, AppRepo.PSEUDO_PLAYER, true, 0xFFEC407A);
        findViewById(R.id.btnDexStart).setOnClickListener(v -> startDex());
        // [comment]「[comment]」[comment]（[comment]："[comment]"）。
        // pinWidget() [comment] —— [comment]，[comment]，
        // [comment]。

        // ---- [comment]
        findViewById(R.id.btnGrant).setOnClickListener(v -> {
            refreshShizukuState();
            if (!ShellRunner.isBinderAlive()) {
                toast("Shizuku Text，Text Shizuku App Text");
                return;
            }
            if (ShellRunner.hasPermission()) {
                toast("Text");
                if (prefs.shouldKeepRunning()) {
                    RotationService.start(this);
                }
                return;
            }
            try {
                Shizuku.requestPermission(REQ_SHIZUKU);
            } catch (Throwable t) {
                toast("TextFailed：" + t);
            }
        });

        findViewById(R.id.btnShizuku).setOnClickListener(v -> openShizuku());

        // ---- Root [comment]（KernelSU / Magisk）：[comment] su -c id [comment]。
        //  [comment]/[comment] —— [comment] root [comment] exec [comment]。
        findViewById(R.id.btnRootGrant).setOnClickListener(v -> {
            if (RootUtil.state() == RootUtil.ST_OK) {
                toast("Text root Text");
                return;
            }
            if (!RootUtil.hasSu()) {
                toast("Text root（su），Text");
                return;
            }
            toast("Text root Text，Text root TextAllow");
            RootUtil.refresh(() -> runOnUiThread(this::refreshRootState));
        });

        // ---- [comment]
        ((Button) findViewById(R.id.btn0)).setOnClickListener(v -> manual(0));
        ((Button) findViewById(R.id.btn90)).setOnClickListener(v -> manual(1));
        ((Button) findViewById(R.id.btn180)).setOnClickListener(v -> manual(2));
        ((Button) findViewById(R.id.btn270)).setOnClickListener(v -> manual(3));
        ((Button) findViewById(R.id.btnFree)).setOnClickListener(v -> manual(-1));

        // ---- [comment]
        findViewById(R.id.btnRepair).setOnClickListener(v -> {
            if (!ShellRunner.isReady()) {
                toast("Text Shizuku Text");
                return;
            }
            RotationService.repair(this);
            toast("Text");
        });

        // ---- [comment]SaveSettings
        btnSaveProfile.setOnClickListener(v -> onSaveProfile());
        btnClearProfile.setOnClickListener(v -> onClearProfile());

        // ---- [comment]Settings
        swEnable.setChecked(prefs.isEnabled());
        swResAsk.setChecked(prefs.isAskOnConnect());
        swResAutoReset.setChecked(prefs.isAutoResetSize());
        swResAutoMatch.setChecked(prefs.isAutoMatchExt());
        cbFixToUser.setChecked(prefs.isFixToUserRotation());
        updateVisibility();
        syncQuickSyncSwitch();
        syncQuickCoverRotSwitch();

        // [comment]（0=[comment]，1=[comment]，2=[comment]，3=[comment]）
        int lastTab = prefs.getLastTab();
        rgTabs.check(lastTab == 1 ? R.id.tabAuto
                : lastTab == 2 ? R.id.tabRes
                : lastTab == 3 ? R.id.tabDual : R.id.tabState);

        binding = false;

        updateResToggleUi();
        refreshSubState();
        refreshShizukuState();
        // ⚠ Root [comment] exec [comment] su：[comment] root [comment]、exec [comment]，
        //   [comment] RootUtil.refresh [comment]，[comment] UI [comment]。
        RootUtil.refresh(() -> runOnUiThread(this::refreshRootState));

        // [comment]，[comment]「[comment]」[comment]（[comment] maybeShowIntro）
        maybeShowIntro();
    }

    // ------------------------------------------------------------- ⓘ / [comment]

    /**
     * [comment]「ⓘ」[comment] → [comment]。
     *
     * <p>[comment]：[comment]，[comment]。
     * [comment]（[comment]）。
     *
     * @param iconId    ⓘ [comment]（[comment] id [comment] i [comment]）[comment] id
     * @param title     [comment]，[comment]Yes[comment]
     * @param detailRes [comment]，[comment] strings.xml [comment] info_* [comment]
     */
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

    /**
     * [comment]「Beta [comment]」[comment]。
     *
     * <p>[comment]，[comment] —— [comment]Yes[comment]
     * （{@link BetaActivity}，[comment]，[comment]）。[comment] rgTabs，
     * [comment]，[comment]Yes[comment]。
     */
    private void bindBetaEntry() {
        View btn = findViewById(R.id.btnBeta);
        if (btn == null) {
            return;
        }
        btn.setOnClickListener(v -> startActivity(new Intent(this, BetaActivity.class)));
    }

    /** [comment]：[comment]，[comment]「[comment]Update」。 */
    private void bindVersionEntry() {
        TextView chip = findViewById(R.id.btnVersion);
        if (chip == null) {
            return;
        }
        String versionName = "—";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (pi.versionName != null && !pi.versionName.isEmpty()) {
                versionName = pi.versionName;
            }
        } catch (Throwable ignored) {
        }
        // [comment]Yes[comment]，[comment]Yes[comment] —— [comment] build.gradle [comment]，
        // [comment]"[comment] 2.2、[comment]Yes 2.3"[comment]。
        chip.setText("v" + versionName + " · " + getString(R.string.version_entry));
        chip.setOnClickListener(v ->
                startActivity(new Intent(this, ChangelogActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshShizukuState();
        refreshSubState();
        refreshDualState(true);
        refreshProfileUi();
        // [comment]：[comment]Yes[comment]、Shizuku [comment]，[comment]
        // （[comment]、[comment]）→ [comment]
        if (prefs.shouldKeepRunning() && ShellRunner.isReady() && !RotationService.isRunning()) {
            RotationService.start(this);
        }
        // [comment]「[comment]」[comment]，[comment]
        PanelWidgetProvider.updateAll(this);
        handler.post(uiTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(uiTick);
    }

    @Override
    protected void onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(permListener);
            Shizuku.removeBinderReceivedListener(binderReceivedListener);
            Shizuku.removeBinderDeadListener(binderDeadListener);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    // ------------------------------------------------------------------ [comment]

    private static int parseInt(String s, int def) {
        try {
            String t = s == null ? "" : s.trim();
            if (t.isEmpty()) {
                return def;
            }
            return Integer.parseInt(t);
        } catch (Throwable t) {
            return def;
        }
    }

    /** [comment]Settings。[comment]，[comment]。 */
    private void saveResFields() {
        if (binding) {
            return;
        }
        int w = parseInt(etResW.getText().toString(), prefs.getResWidth());
        int h = parseInt(etResH.getText().toString(), prefs.getResHeight());
        int dpi = parseInt(etResDpi.getText().toString(), 0);
        if (w < MIN_RES || h < MIN_RES) {
            return;
        }
        if (w > 10000 || h > 10000) {
            return;
        }
        if (dpi < 0 || dpi > 2000) {
            dpi = 0;
        }
        prefs.setResSize(w, h);
        prefs.setResDensity(dpi);
    }

    private boolean resFieldsValid() {
        int w = parseInt(etResW.getText().toString(), 0);
        int h = parseInt(etResH.getText().toString(), 0);
        return w >= MIN_RES && h >= MIN_RES && w <= 10000 && h <= 10000;
    }

    /**
     * [comment]/Disable[comment]。
     *
     * @param on        true = Enable，false = Disable
     * @param applyNow  Enable[comment]YesNo[comment]。
     *                  [comment] = true（[comment]，[comment]）；
     *                  [comment]Yes[comment]Settings[comment] false。
     */
    private void setResTakeover(boolean on, boolean applyNow) {
        prefs.setResolutionEnabled(on);
        ensureNotificationPermission();
        if (on) {
            RotationService.start(this);
            if (applyNow) {
                if (!ShellRunner.isReady()) {
                    toast("TextEnable，Text Shizuku Text，Text");
                } else {
                    RotationService.applySizeNow(this);
                    toast("TextEnable：Text → " + effSizeText()
                            + (prefs.isAutoMatchExt() ? "（Text）" : ""));
                }
            }
        } else {
            RotationService.resetSizeNow(this);
            if (!prefs.shouldKeepRunning()) {
                RotationService.stop(this);
            }
            toast("TextDisableText，Text");
        }
        updateResToggleUi();
        updateInfo();
    }

    /** [comment]（[comment]） */
    private String effSizeText() {
        int[] p = ScreenSizeUtil.portrait(prefs.getResWidth(), prefs.getResHeight());
        return p[0] + "×" + p[1];
    }

    /** [comment]Settings[comment]：[comment]=[comment]，[comment]=[comment]，[comment] */
    private void updateResToggleUi() {
        if (btnResToggle == null) {
            return;
        }
        boolean on = prefs.isResolutionEnabled();
        String text = on
                ? "Text：TextEnable（" + effSizeText() + "）\nTextDisable"
                : "Text：TextDisable\nTextEnable（" + effSizeText() + "）";
        if (!text.contentEquals(btnResToggle.getText())) {
            btnResToggle.setText(text);
        }
        btnResToggle.setBackgroundColor(on ? COLOR_ON : COLOR_OFF);
        btnResToggle.setTextColor(0xFFFFFFFF);
    }

    /** [comment]，[comment] */
    private void fillResFromExt() {
        List<Display> ext = DisplayUtil.externalDisplays(dm);
        if (ext.isEmpty()) {
            toast("Text，Text");
            return;
        }
        // [comment]
        int[] best = null;
        Display bestD = null;
        for (Display d : ext) {
            int[] s = DisplayUtil.suggestedInternalSize(d);
            if (s[0] <= 0 || s[1] <= 0) {
                continue;
            }
            if (best == null || (long) s[0] * s[1] > (long) best[0] * best[1]) {
                best = s;
                bestD = d;
            }
        }
        if (best == null) {
            toast("Text，Text");
            return;
        }
        int[] n = DisplayUtil.nativeSize(bestD);
        binding = true;
        etResW.setText(String.valueOf(best[0]));
        etResH.setText(String.valueOf(best[1]));
        binding = false;
        prefs.setResSize(best[0], best[1]);
        toast("Text " + best[0] + "×" + best[1]
                + (n[0] > n[1] ? "（Text " + n[0] + "×" + n[1] + "，Text）" : ""));
        updateResToggleUi();
    }

    /** 「[comment]」：[comment] */
    private void requestOverlayPermission() {
        boolean granted = false;
        try {
            granted = Settings.canDrawOverlays(this);
        } catch (Throwable ignored) {
        }
        if (granted) {
            toast("Text");
            return;
        }
        if (!ShellRunner.isReady()) {
            openOverlaySettings();
            return;
        }
        toast("Text Shizuku Text…");
        final String pkg = getPackageName();
        final int uid = android.os.Process.myUid();
        new Thread(() -> {
            ShellRunner.run("appops set --uid " + uid + " SYSTEM_ALERT_WINDOW allow");
            ShellRunner.run("appops set " + pkg + " SYSTEM_ALERT_WINDOW allow");
            runOnUiThread(() -> {
                boolean ok = false;
                try {
                    ok = Settings.canDrawOverlays(this);
                } catch (Throwable ignored) {
                }
                if (ok) {
                    toast("Text");
                } else {
                    openOverlaySettings();
                }
            });
        }, "extrot-overlay").start();
    }

    private void openOverlaySettings() {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            toast("Text「AllowText」");
        } catch (Throwable t) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(i);
                toast("Text「AllowText」");
            } catch (Throwable t2) {
                toast("TextSettingsText：" + t2);
            }
        }
    }

    // ------------------------------------------------------------------ [comment]

    /** 「[comment]」[comment]：[comment] / [comment] + [comment] / [comment] */
    private void setupRefreshRate() {
        if (spPeakHz == null || spMinHz == null) {
            return;
        }
        float[] modes = RefreshRate.supported(this);
        String[] labels = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            labels[i] = String.format(Locale.US, "%.0f Hz", modes[i]);
        }
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPeakHz.setAdapter(ad);
        spMinHz.setAdapter(ad);
        spPeakHz.setSelection(nearestIndex(modes, RefreshRate.readPeak(this)), false);
        spMinHz.setSelection(nearestIndex(modes, RefreshRate.readMin(this)), false);

        findViewById(R.id.btnApplyHz).setOnClickListener(v -> {
            float peak = modes[spPeakHz.getSelectedItemPosition()];
            float min = modes[spMinHz.getSelectedItemPosition()];
            String err = RefreshRate.apply(this, peak, min);
            toast(err != null ? err
                    : String.format(Locale.US, "Text：%.0f ~ %.0f Hz", min, peak));
            refreshRateState();
        });

        findViewById(R.id.btnResetHz).setOnClickListener(v -> {
            String err = RefreshRate.reset(this);
            toast(err != null ? err : "Text，Text");
            refreshRateState();
        });

        refreshRateState();
    }

    /** [comment]；[comment]（[comment]）[comment] */
    private static int nearestIndex(float[] modes, float hz) {
        for (int i = 0; i < modes.length; i++) {
            if (Math.abs(modes[i] - hz) < 0.5f) {
                return i;
            }
        }
        return 0;
    }

    /** [comment]：[comment] + [comment] */
    private void refreshRateState() {
        if (tvHzState == null) {
            return;
        }
        String now = "Text：" + hzText(RefreshRate.readMin(this))
                + " ~ " + hzText(RefreshRate.readPeak(this)) + " Hz";
        tvHzState.setText(now + (RefreshRate.innerActive(this)
                ? " · Text" : " · Text，Text"));
    }

    /** [comment]（-1）[comment]，[comment] -1 */
    private static String hzText(float v) {
        return v < 0 ? "—" : String.format(Locale.US, "%.0f", v);
    }

    private void bindSpinner(Spinner sp, int[] values, String[] labels, int current, IntSetter setter) {
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        int idx = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                idx = i;
            }
        }
        sp.setSelection(idx, false);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (binding) {
                    return;
                }
                setter.set(values[position]);
                onSettingChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateVisibility() {
        // 「[comment]」[comment]，[comment]"[comment]"[comment]，[comment]。
        // ⚠ [comment]"[comment]"[comment]"[comment]"。
        // [comment]Yes[comment]，[comment]"[comment]"[comment]Yes[comment] ——
        // [comment]，[comment]，[comment]。
        findViewById(R.id.rowFixToUser).setVisibility(
                prefs.isLockPhoneRotation() ? View.VISIBLE : View.GONE);
    }

    /** [comment]。[comment]：0=[comment]，1=[comment]，2=[comment]，3=[comment] */
    private void showTab(int idx) {
        if (panelAuto == null) {
            return;
        }
        panelState.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
        panelAuto.setVisibility(idx == 1 ? View.VISIBLE : View.GONE);
        panelRes.setVisibility(idx == 2 ? View.VISIBLE : View.GONE);
        if (panelDual != null) {
            panelDual.setVisibility(idx == 3 ? View.VISIBLE : View.GONE);
        }
        prefs.setLastTab(idx);
        // [comment]（idx 0），[comment]，
        // [comment]。
        if (idx == 0) {
            refreshFoldState();
        }
        if (idx == 3) {
            refreshDualState(true);
            refreshSubState();
        }
    }

    /** [comment]「[comment]」[comment]Yes[comment]Yes[comment] */
    private boolean sysAutoRotate() {
        try {
            return Settings.System.getInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 1) == 1;
        } catch (Throwable t) {
            return false;   // Text"Text"，Text
        }
    }

    /**
     * [comment]「[comment]」[comment]。
     *
     * <p>[comment]「[comment]」[comment]，[comment]"[comment]Enable"，
     * [comment]，[comment]。[comment]Yes[comment]：
     * [comment]，[comment]（[comment]Yes[comment]）。
     */
    private void syncPhoneLockSwitch() {
        if (spScreenDir == null) {
            return;
        }
        if (!prefs.isLockPhoneRotation() || !sysAutoRotate()
                || System.currentTimeMillis() - lockToggleAt < 2500L) {
            lockSyncStrikes = 0;
            return;
        }
        // [comment]，[comment]
        if (++lockSyncStrikes < 2) {
            return;
        }
        lockSyncStrikes = 0;
        prefs.setLockPhoneRotation(false);
        binding = true;
        spScreenDir.setSelection(indexOf(DIR_VALS, DIR_AUTO), false);
        binding = false;
        updateVisibility();
        toast("Text「Text」，TextDisable");
    }

    /** 「[comment]」[comment]Yes[comment]（[comment] (lockPhone, phoneRotation) [comment]） */
    private int dirValue() {
        if (!prefs.isLockPhoneRotation()) {
            return DIR_AUTO;
        }
        // 0° = [comment]，[comment]（90°/270°）[comment] —— [comment]，
        // [comment] 270° [comment]"[comment]"[comment]，[comment]。
        return prefs.getPhoneRotation() == 0 ? DIR_PORT : DIR_LAND;
    }

    /** [comment]「[comment]」[comment]，[comment]（[comment]「[comment]」[comment]Yes[comment]） */
    private void applyScreenDir(int v) {
        lockToggleAt = System.currentTimeMillis();
        lockSyncStrikes = 0;
        if (v == DIR_AUTO) {
            prefs.setLockPhoneRotation(false);
            RotationService.restorePhoneRotation(this);
            if (!prefs.shouldKeepRunning()) {
                RotationService.stop(this);
            }
            updateVisibility();
            toast("Text：Text");
            return;
        }
        prefs.setLockPhoneRotation(true);
        prefs.setPhoneRotation(v == DIR_PORT ? 0 : 1);
        if (!ShellRunner.isReady()) {
            toast("TextSettings，Text Shizuku Text，Text");
        } else {
            toast(v == DIR_PORT ? "TextEnable：DisableText"
                    : "TextEnable：DisableText");
        }
        RotationService.lockPhoneNow(this);
        updateVisibility();
    }

    private void onSettingChanged() {
        if (prefs.isEnabled()) {
            RotationService.start(this);
        }
        // [comment]Yes"[comment]"[comment]，Settings[comment]
        PanelWidgetProvider.updateAll(this);
    }

    /**
     * [comment] / Disable「[comment]」（[comment]）。
     *
     * <p>Disable[comment]「[comment]」[comment]：[comment]「[comment]」[comment]，
     * [comment] ACTION_STOP，[comment]、[comment] ——
     * [comment]Yes「[comment]Yes[comment]，[comment]」。
     *
     * <p>[comment]「[comment]」[comment]Yes[comment]：[comment]，[comment] —— [comment]Yes[comment]。
     */
    private void setSyncEnabled(boolean on) {
        prefs.setEnabled(on);
        if (on) {
            ensureNotificationPermission();
            RotationService.start(this);
        } else {
            if (!prefs.isLockPhoneRotation()) {
                RotationService.restorePhoneRotation(this);
            }
            if (!prefs.shouldKeepRunning()) {
                RotationService.stop(this);
            }
        }
        syncQuickSyncSwitch();
        syncQuickCoverRotSwitch();
    }

    private static int indexOf(int[] values, int v) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == v) {
                return i;
            }
        }
        return 0;
    }

    /**
     * [comment] / [comment] / [comment]「Settings」[comment]。
     *
     * <p>[comment]：[comment]Settings[comment] —— [comment]
     * （[comment]Yes[comment]Save[comment]）。[comment]，
     * [comment]"Settings[comment]，[comment]"[comment]。
     *
     * <p>[comment] {@code binding}，No[comment]，
     * [comment] Toast。
     */
    private void syncFromPrefs() {
        if (swEnable == null) {
            return;
        }
        boolean b = binding;
        binding = true;
        try {
            if (swEnable.isChecked() != prefs.isEnabled()) {
                swEnable.setChecked(prefs.isEnabled());
            }
            if (swResAutoMatch.isChecked() != prefs.isAutoMatchExt()) {
                swResAutoMatch.setChecked(prefs.isAutoMatchExt());
            }
            if (swResAsk.isChecked() != prefs.isAskOnConnect()) {
                swResAsk.setChecked(prefs.isAskOnConnect());
            }
            if (swResAutoReset.isChecked() != prefs.isAutoResetSize()) {
                swResAutoReset.setChecked(prefs.isAutoResetSize());
            }
            if (cbFixToUser.isChecked() != prefs.isFixToUserRotation()) {
                cbFixToUser.setChecked(prefs.isFixToUserRotation());
            }
            int p = indexOf(ROT_PORT, prefs.getPortraitAngle());
            if (spPortrait.getSelectedItemPosition() != p) {
                spPortrait.setSelection(p, false);
            }
            int l = indexOf(ROT_ALL, prefs.getLandscapeAngle());
            if (spLandscape.getSelectedItemPosition() != l) {
                spLandscape.setSelection(l, false);
            }
            int sd = indexOf(DIR_VALS, dirValue());
            if (spScreenDir.getSelectedItemPosition() != sd) {
                spScreenDir.setSelection(sd, false);
            }
            int bm = indexOf(BOOT_VALS, prefs.getBootMode());
            if (spBootMode.getSelectedItemPosition() != bm) {
                spBootMode.setSelection(bm, false);
            }
        } finally {
            binding = b;
        }
        syncQuickSyncSwitch();
        syncQuickCoverRotSwitch();
        updateVisibility();
    }

    /** [comment]「[comment]」[comment]Settings[comment]（[comment]，[comment]） */
    private void syncQuickSyncSwitch() {
        if (swQASync == null) {
            return;
        }
        boolean on = prefs.isEnabled();
        if (swQASync.isChecked() != on) {
            boolean b = binding;
            binding = true;
            swQASync.setChecked(on);
            binding = b;
        }
    }

    /** [comment]，「[comment]」[comment] */
    private void syncQuickCoverRotSwitch() {
        if (swQACoverRot == null) {
            return;
        }
        boolean on = CoverAutoRot.on(this);
        if (swQACoverRot.isChecked() != on) {
            boolean b = binding;
            binding = true;
            swQACoverRot.setChecked(on);
            binding = b;
        }
    }

    /**
     * 「[comment]None[comment]」[comment]（v4.22 [comment]）。
     *
     * <p><b>[comment]</b>：[comment]。[comment]Notice[comment]、[comment]
     * —— [comment]Yes[comment]，[comment]"[comment]"[comment]。
     *
     * <p>[comment]（[comment]、[comment]None[comment]、[comment]），
     * [comment]；[comment]，[comment]（[comment]）。
     */
    private void openAdbWifiOnce() {
        if (AdbWifi.isOn(this)) {
            toast("NoneTextYesText");
            return;
        }
        toast("TextNoneText…");
        new Thread(() -> {
            final String err = AdbWifi.set(MainActivity.this, true);
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                toast(err == null ? "NoneText" : ("TextSuccess：" + err));
            });
        }, "adb-wifi-on").start();
    }

    // ------------------------------------------------------------------ [comment]SaveSettings

    /**
     * [comment]。
     *
     * <p>[comment] {@link DisplayUtil#largestByPanel}：[comment]、[comment]"[comment]"、
     * [comment]"[comment]"[comment]Yes**[comment]**，[comment]。
     */
    private Display mainExternal() {
        return DisplayUtil.largestByPanel(DisplayUtil.externalDisplays(dm));
    }

    /**
     * [comment]，[comment] {@code 90°}。
     *
     * <p>⚠ [comment]「[comment] / [comment]」：[comment]（0° [comment] 180° [comment]「[comment]」，
     * 90° [comment] 270° [comment]「[comment]」），[comment]「[comment]Save / [comment]」[comment]、[comment]，[comment]。
     * [comment]，[comment]。
     */
    private static String orient(int rotation) {
        return DisplayUtil.degrees(rotation) + "°";
    }

    /**
     * 「[comment]」[comment]：{@code [comment]} / {@code [comment] 90°} / {@code [comment] 0°} …
     *
     * <p>⚠ [comment] {@link #orient} <b>[comment]</b>，[comment]：{@code orient()} [comment]Yes
     * <b>[comment]</b>（[comment]，[comment] 90° [comment]），[comment] 90°/270° Yes<b>[comment]</b>
     * [comment] —— [comment]。
     *
     * <p>{@code [comment]} = <b>[comment]</b>：「[comment]」[comment]Yes「<b>[comment]</b>[comment]」，
     * [comment] false [comment]Yes「[comment]」。
     *
     * <p>⚠ [comment]：[comment]「[comment] / [comment]」[comment]
     * （「[comment]」[comment]Yes 90° [comment]Yes 270° [comment]）。[comment]「[comment]」[comment]，[comment]「[comment] / [comment]」[comment]。
     * 180° / 270° [comment]，[comment]。
     */
    private static String phoneRotation(DisplayProfile.Item it) {
        if (!it.lockPhone) {
            return "Text";
        }
        int deg = DisplayUtil.degrees(it.phoneRot);
        return ((deg == 90 || deg == 270) ? "Text " : "Text ") + deg + "°";
    }


    /**
     * [comment]，[comment]Yes {@code {[comment], [comment]}}，[comment]。
     *
     * <p>[comment] 4 [comment] —— [comment]、[comment] 50dp [comment]，[comment]
     * （[comment] item_profile_row.xml [comment]）。
     *
     * <p>「[comment]」「[comment]」[comment]<b>[comment]</b>
     * （[comment] {@link DisplayProfile.Item#applyTo}），「[comment]Save」[comment]Yes[comment]Save[comment]Yes[comment]。
     *
     * <p>[comment] DPI [comment]，[comment]（[comment] {@link #profileHead}）。
     */
    private static String[][] profileItems(DisplayProfile.Item it) {
        return new String[][]{
                {"Text", it.sync ? "Text" : "Text"},
                {"Text", phoneRotation(it)},
                {"Text", orient(it.portrait)},
                {"Text", orient(it.landscape)},
                {"DPI", it.resDensity > 0 ? String.valueOf(it.resDensity) : "Text"},
        };
    }

    /**
     * [comment]：[comment]Yes[comment] · [comment]Yes[comment] · [comment] · [comment]。
     *
     * <p>[comment] —— [comment]，[comment]
     * "Settings[comment] 1080×1920"[comment]，[comment]「[comment]」[comment]。
     */
    private static String profileHead(Display d, DisplayProfile.Item cur, DisplayProfile.Item saved) {
        String res = cur.resEnabled ? (cur.resW + "×" + cur.resH) : "Text";
        boolean same = saved != null
                && saved.resEnabled == cur.resEnabled
                && saved.resW == cur.resW
                && saved.resH == cur.resH;
        return DisplayProfile.label(d) + " · SettingsText " + res + (same ? "（TextSave）" : "（TextSave）");
    }

    /**
     * [comment]。
     *
     * <p>**[comment]**：[comment]（v4.13 [comment]「[comment]」[comment]），
     * [comment]，[comment]"[comment]"[comment]"DPI"[comment]。
     * [comment]，[comment]。
     */
    private static String savedValue(String[][] items, String name) {
        if (items == null) {
            return "—";
        }
        for (String[] kv : items) {
            if (kv[0].equals(name)) {
                return kv[1];
            }
        }
        return "—";
    }

    /**
     * [comment]「Save[comment]Settings」[comment]。
     *
     * <p>[comment]Yes"**[comment]**、[comment] `[comment]Save` [comment] `[comment]` [comment]"。v2.6 [comment]、
     * [comment]，[comment]；[comment] 3 [comment]，[comment]"[comment]"[comment]
     * [comment] —— [comment]Yes。
     *
     * <p>[comment]，[comment]Notice + [comment]（[comment]）。
     */
    private void refreshProfileUi() {
        if (tvProfileHead == null || profileTable == null) {
            return;
        }
        Display d = mainExternal();
        if (d == null) {
            tvProfileHead.setText("Text —— Text，Text");
            profileTable.setVisibility(View.GONE);
            btnSaveProfile.setEnabled(false);
            btnClearProfile.setEnabled(false);
            return;
        }
        profileTable.setVisibility(View.VISIBLE);
        btnSaveProfile.setEnabled(true);

        DisplayProfile.Item saved = DisplayProfile.load(this, DisplayProfile.signature(d));
        btnClearProfile.setEnabled(saved != null);

        DisplayProfile.Item cur = DisplayProfile.Item.from(prefs);
        tvProfileHead.setText(profileHead(d, cur, saved));

        String[][] curItems = profileItems(cur);
        String[][] oldItems = saved == null ? null : profileItems(saved);

        // [comment]，[comment] ——
        // [comment]"[comment] View [comment]Settings"[comment]
        profileTable.removeAllViews();
        String[] head = {"SettingsText", "TextSave", "Text"};
        profileTable.addView(profileRow(head, head, true));
        for (int i = 0; i < curItems.length; i += 2) {
            String[] a = item(curItems[i], oldItems);
            String[] b = (i + 1 < curItems.length) ? item(curItems[i + 1], oldItems) : null;
            profileTable.addView(profileRow(a, b, false));
        }
    }

    /** [comment] {@code {[comment], [comment]}} [comment]，[comment]。 */
    private static String[] item(String[] curItem, String[][] oldItems) {
        return new String[]{curItem[0], savedValue(oldItems, curItem[0]), curItem[1]};
    }

    /**
     * [comment]，[comment]（[comment] + [comment]Save + [comment]）；{@code b} [comment] null [comment]。
     *
     * <p>[comment] {@code res/layout/item_profile_row.xml}，[comment] ——
     * [comment]、[comment]、[comment] XML [comment]，[comment]。
     */
    private View profileRow(String[] a, String[] b, boolean header) {
        View row = getLayoutInflater().inflate(R.layout.item_profile_row, profileTable, false);
        fillProfileCell(row, R.id.cellName, R.id.cellSaved, R.id.cellNow, a, header);
        if (b == null) {
            row.findViewById(R.id.cellName2).setVisibility(View.GONE);
            row.findViewById(R.id.cellSaved2).setVisibility(View.GONE);
            row.findViewById(R.id.cellNow2).setVisibility(View.GONE);
        } else {
            fillProfileCell(row, R.id.cellName2, R.id.cellSaved2, R.id.cellNow2, b, header);
        }
        return row;
    }

    /** [comment]；[comment]，[comment]。 */
    private void fillProfileCell(View row, int nameId, int savedId, int nowId,
                                 String[] v, boolean header) {
        TextView name = row.findViewById(nameId);
        TextView saved = row.findViewById(savedId);
        TextView now = row.findViewById(nowId);
        name.setText(v[0]);
        saved.setText(v[1]);
        now.setText(v[2]);
        if (header) {
            int hint = 0x99000000;
            name.setTextColor(hint);
            saved.setTextColor(hint);
            now.setTextColor(hint);
        }
    }

    private void onSaveProfile() {
        Display d = mainExternal();
        if (d == null) {
            toast("Text，TextSave");
            return;
        }
        String sig = DisplayProfile.signature(d);
        if (sig.isEmpty()) {
            toast("Text，Text（Text）");
            return;
        }
        DisplayProfile.saveFrom(this, sig, DisplayProfile.label(d), d.getDisplayId(), prefs);
        refreshProfileUi();
        toast("TextSettingsText，Text");
    }

    private void onClearProfile() {
        Display d = mainExternal();
        if (d == null) {
            toast("Text");
            return;
        }
        String sig = DisplayProfile.signature(d);
        if (!DisplayProfile.exists(this, sig)) {
            toast("TextSaveText");
            return;
        }
        DisplayProfile.clear(this, sig);
        refreshProfileUi();
        toast("Text");
    }

    private void manual(int rotation) {
        if (!ShellRunner.isReady()) {
            toast("Text Shizuku Text");
            return;
        }
        RotationService.manual(this, rotation);
        toast(rotation < 0 ? "Text" : ("Text " + DisplayUtil.degrees(rotation) + "°"));
    }

    private void refreshShizukuState() {
        if (tvShizuku == null) {
            return;
        }
        String text;
        if (!isShizukuInstalled()) {
            text = "Shizuku：Text。Text Shizuku（Text），Text ADB Text。";
        } else if (!ShellRunner.isBinderAlive()) {
            text = "Shizuku：Text。Text Shizuku Text。";
        } else if (!ShellRunner.hasPermission()) {
            text = "Shizuku：Text，Text → Text「Text / Text」。";
        } else {
            text = "Shizuku：Text ✓ Text shell（ADB）Text，Text。";
        }
        tvShizuku.setText(text);
    }

    private void refreshRootState() {
        if (tvRoot == null) {
            return;
        }
        String text;
        switch (RootUtil.state()) {
            case RootUtil.ST_OK:
                text = "Root：Text ✓ Text root Text。";
                break;
            case RootUtil.ST_DENIED:
                text = "Root：Text root Text，Text → Text「Root Text / Text」。";
                break;
            case RootUtil.ST_NONE:
                text = "Root：Text。Text root（KernelSU / Magisk），Text。";
                break;
            default:
                text = "Root：Text…";
                break;
        }
        tvRoot.setText(text);
    }

    private void updateInfo() {
        if (tvInfo == null) {
            return;
        }
        // [comment]「[comment]」[comment]，[comment]
        syncPhoneLockSwitch();

        Display def = DisplayUtil.defaultDisplay(dm);
        List<Display> ext = DisplayUtil.externalDisplays(dm);

        StringBuilder sb = new StringBuilder();
        // [comment]「[comment]」[comment]，[comment]，[comment]
        sb.append("Text：").append(DisplayUtil.isOn(def) ? "Text（Text）" : "Text（Text / Text）")
                .append("　Text：").append(ext.isEmpty() ? "Text" : ext.size() + " Text").append('\n');
        // [comment]Yes「[comment]」：[comment]Yes[comment]，
        // [comment] rotation [comment]，[comment]"[comment]"（→ [comment]）。
        Display phone = DisplayUtil.activePhoneDisplay(dm);
        sb.append("Text：");
        if (!DisplayUtil.isOn(phone)) {
            sb.append("Text（Text，Text）");
        } else {
            sb.append(DisplayUtil.isPhonePortrait(phone) ? "Text" : "Text")
                    .append("（Text ").append(DisplayUtil.kindOf(dm, phone))
                    .append(" rotation=").append(phone.getRotation()).append("）");
        }
        sb.append('\n');

        String ov = DisplayUtil.sizeOverrideInfo(def);
        if (ov == null) {
            sb.append("Text：" + DisplayUtil.sizeOf(def) + "（Text，NoneText）\n");
        } else {
            sb.append("Text：⚠ Text ").append(ov);
            if (prefs.isSizeApplied()) {
                sb.append(" ← TextSettings\n");
            } else {
                sb.append(" ← TextSettings，Text「Text」Text\n");
            }
        }

        if (ext.isEmpty()) {
            sb.append("Text：Text\n");
        } else {
            for (Display d : ext) {
                sb.append("Text：").append(DisplayUtil.describeFull(d)).append('\n');
                int[] s = DisplayUtil.suggestedInternalSize(d);
                if (s[0] > 0) {
                    sb.append("　　　　Text：").append(s[0]).append("×").append(s[1]).append('\n');
                }
            }
        }
        sb.append("Text：").append(prefs.isEnabled() ? "Text" : "Text");
        // [comment]。[comment]「[comment]（[comment]）」[comment]Yes 90° [comment]Yes 270°，
        // [comment]「[comment]」Yes[comment]。
        // [comment]"[comment]" —— [comment]。[comment]，[comment]。
        sb.append("　Text：");
        if (prefs.isLockPhoneRotation()) {
            int d = DisplayUtil.degrees(prefs.getPhoneRotation());
            sb.append("Text（").append(d == 0 || d == 180 ? "Text " : "Text ").append(d).append("°）");
        } else {
            sb.append("Text");
        }
        sb.append("　Text：").append(prefs.isResolutionEnabled() ? "Text" : "Text");
        sb.append('\n');
        sb.append("Text：").append(sysAutoRotate() ? "Text" : "Text");
        sb.append("　Shell：").append(ShellRunner.isReady()
                ? (ShellRunner.rootReady() ? "Text（root）" : "Text（Shizuku）")
                : "Not Ready");
        tvInfo.setText(sb.toString());

        if (tvResState != null) {
            StringBuilder rs = new StringBuilder();
            int cw = prefs.getResWidth();
            int ch = prefs.getResHeight();
            int[] eff = ScreenSizeUtil.portrait(cw, ch);
            rs.append("Text：").append(cw).append("×").append(ch);
            if (cw > ch) {
                rs.append("（Text）");
            }
            int dpi = prefs.getResDensity();
            if (dpi > 0) {
                rs.append("　DPI：").append(dpi);
            }
            rs.append('\n');
            rs.append("Text：").append(eff[0]).append("×").append(eff[1]);
            if (cw > ch) {
                rs.append("　← Text");
            }
            rs.append('\n');
            if (ov == null) {
                rs.append("Text：Text（Text）");
            } else if (prefs.isSizeApplied()) {
                rs.append("Text：Text ").append(ov);
            } else {
                rs.append("Text：Text ").append(ov);
            }
            if (prefs.isSizeApplied()) {
                rs.append(prefs.isResBoundToExt() ? "（Text）" : "（Text，Text）");
            }
            rs.append('\n');
            boolean overlay = false;
            try {
                overlay = Settings.canDrawOverlays(this);
            } catch (Throwable ignored) {
            }
            rs.append("Text：").append(overlay ? "Text（Text）"
                    : "Text（Text，Text「Text」TextEnable）");
            tvResState.setText(rs.toString());
        }

        if (tvResHint != null) {
            int cw = prefs.getResWidth();
            int ch = prefs.getResHeight();
            int[] eff = ScreenSizeUtil.portrait(cw, ch);
            StringBuilder h = new StringBuilder();
            h.append("Text：1920×1080 → 1080×1920。\n");
            h.append("Text：").append(eff[0]).append("×").append(eff[1]);
            if (!ext.isEmpty() && prefs.isAutoMatchExt()) {
                int[] s = DisplayUtil.suggestedInternalSize(ext.get(0));
                if (s[0] > 0) {
                    h.append("　|　Text：").append(s[0]).append("×").append(s[1]);
                }
            }
            tvResHint.setText(h.toString());
        }

        syncQuickSyncSwitch();
        syncQuickCoverRotSwitch();
        updateResToggleUi();

        // [comment]「[comment]」[comment]，[comment]
        if (panelDual != null && panelDual.getVisibility() == View.VISIBLE) {
            refreshDualState(false);
        }

        String log = RotationService.LAST_LOG;
        tvLog.setText(log == null || log.isEmpty() ? "（TextNoneText）" : log);
    }

    private boolean isShizukuInstalled() {
        try {
            getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void openShizuku() {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
            if (i == null) {
                i = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"));
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable t) {
            toast("Text Shizuku：" + t);
        }
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                try {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTI);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    // ------------------------------------------------------------- [comment]

    /** 「[comment]」[comment] */
    private static final String FILE_INTRO = "extrot_intro";
    private static final String KEY_INTRO_SHOWN = "intro_shown";

    /**
     * [comment]，[comment]「[comment]」[comment]。
     *
     * <p>[comment]Yes「[comment]，[comment]」——
     * <b>[comment]</b>[comment]，[comment]"[comment]"[comment]，[comment]。
     * [comment] {@code BetaActivity} [comment]Yes[comment]：[comment]Yes"[comment]Yes[comment]"[comment]
     * [comment]，[comment]；[comment]Yes[comment]，[comment]。
     * [comment]，[comment]「Update[comment]」[comment]。
     *
     * <p>⚠ [comment]Custom[comment] + [comment] ScrollView，[comment] {@code dialog_beta_intro.xml}：
     * [comment]，AlertDialog [comment] message [comment]，[comment]。
     */
    private void maybeShowIntro() {
        if (isFinishing()) {
            return;
        }
        SharedPreferences sp = getSharedPreferences(FILE_INTRO, MODE_PRIVATE);
        if (sp.getBoolean(KEY_INTRO_SHOWN, false)) {
            return;
        }
        sp.edit().putBoolean(KEY_INTRO_SHOWN, true).apply();
        View body = getLayoutInflater().inflate(R.layout.dialog_app_intro, null);
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setView(body)
                .setPositiveButton("Text", null)
                .show();
    }

    // ------------------------------------------------------------------ [comment] / [comment]

    /**
     * [comment] ↔ [comment]。
     *
     * <p>[comment] {@link DualScreen} [comment] —— [comment]Yes[comment]，
     * [comment]、[comment]。
     */
    private void onFoldToggle() {
        if (!requireShell()) {
            return;
        }
        new Thread(() -> {
            final int target = DualScreen.toggleFold(this);
            runOnUiThread(() -> {
                toast(target < 0 ? "TextFailed：Text Shizuku Text"
                        : "Text「" + DualScreen.stateName(target) + "」");
                refreshFoldState();
            });
        }, "extrot-fold").start();
    }

    /** [comment]（[comment]，[comment]） */
    private void onDeviceStateReset() {
        if (!requireShell()) {
            return;
        }
        new Thread(() -> {
            DualScreen.resetState();
            runOnUiThread(() -> {
                toast("Text（Text）");
                refreshFoldState();
            });
        }, "extrot-dsr").start();
    }

    /** [comment]（[comment]，[comment]） */
    private void refreshFoldState() {
        if (tvFoldState == null) {
            return;
        }
        if (!ShellRunner.isReady()) {
            tvFoldState.setText("Text：Text Shizuku Text");
            return;
        }
        new Thread(() -> {
            final int st = DualScreen.readDeviceState();
            final int phy = DualScreen.readPhysicalState();
            final boolean cover = DualScreen.isCoverMode(MainActivity.this);
            final int exts = DisplayUtil.externalDisplays(dm).size();
            runOnUiThread(() -> {
                if (tvFoldState == null) {
                    return;
                }
                // [comment]：[comment]，"[comment]"[comment]"[comment]"
                // [comment]，[comment] —— [comment]
                tvFoldState.setText("Text：" + DualScreen.stateName(st)
                        + " [id=" + st + "]\n"
                        + "Text：" + DualScreen.stateName(phy)
                        + " · Text" + (cover ? "Text" : "Text") + "\n"
                        + "Text：" + exts);
            });
        }, "extrot-foldq").start();
    }

    // ------------------------------------------------------------------ [comment] / [comment]

    /** [comment]（[comment]）[comment] display id，[comment] -1 */
    private int coverDisplayId() {
        Display c = DisplayUtil.coverDisplay(dm);
        return c == null ? -1 : c.getDisplayId();
    }

    /** [comment]（[comment]）[comment] display id，[comment] -1 */
    private int firstExtDisplayId() {
        List<Display> ext = DisplayUtil.externalDisplays(dm);
        return ext.isEmpty() ? -1 : ext.get(0).getDisplayId();
    }

    /**
     * [comment]：[comment] → [comment] → [comment]。
     *
     * <p>[comment]（[comment]"[comment] Display [comment]"[comment]）[comment]
     * {@link DualScreen#oneKeyDual} [comment]，[comment]Yes[comment]。
     */
    private void onOneKeyDual() {
        if (!requireShell()) {
            return;
        }
        toast("Text…");
        new Thread(() -> {
            final boolean ok = DualScreen.oneKeyDual(this, dm);
            runOnUiThread(() -> {
                toast(ok ? "Text，Text"
                        : "Text，Text；TextYesText");
                refreshFoldState();
                refreshDualState(false);
            });
        }, "extrot-onekey").start();
    }

    /**
     * [comment]：[comment] → [comment]「[comment]」[comment] → [comment]、[comment]。
     *
     * <p>[comment]。[comment]Yes「[comment]」→ [comment]「[comment]」→ [comment]「[comment]」，
     * [comment]。[comment] {@link DualScreen#oneKeyCast} [comment]，[comment]、[comment]。
     */
    private void onOneKeyCast() {
        if (!requireShell()) {
            return;
        }
        toast("Text…");
        new Thread(() -> {
            final boolean ok = DualScreen.oneKeyCast(this, dm);
            runOnUiThread(() -> {
                toast(ok ? "Text，Text"
                        : "Text，Text；TextYesText");
                refreshFoldState();
                refreshDualState(false);
            });
        }, "extrot-onekey-cast").start();
    }

    /**
     * [comment]（Android 8 [comment] requestPinAppWidget）。
     *
     * <p>[comment]「[comment]」[comment]，[comment]。[comment]，
     * [comment]；[comment]Notice[comment]。
     */
    private void pinWidget(Class<?> provider, String name) {
        try {
            AppWidgetManager awm = (AppWidgetManager) getSystemService(APPWIDGET_SERVICE);
            if (awm == null || !awm.isRequestPinAppWidgetSupported()) {
                toast("Text，Text → Text → Text");
                return;
            }
            if (awm.requestPinAppWidget(new ComponentName(this, provider), null, null)) {
                toast("Text「Text」，Text「" + name + "」Text");
            } else {
                toast("Text，Text");
            }
        } catch (Throwable t) {
            toast("TextFailed：" + t);
        }
    }

    // ---- [comment]（v4.11 [comment] Beta [comment]「[comment]」[comment]）

    /**
     * 「[comment]」—— [comment]，<b>[comment]</b>。
     *
     * <p>[comment]「[comment]」[comment]：[comment] {@code cmd device_state state 4}
     * [comment]（[comment]），[comment]，[comment]Yes[comment]Yes[comment]。
     */
    private void doOpenSub() {
        if (CoverDisplay.id(this) <= 0) {
            toast("Text");
            return;
        }
        if (!requireShell()) {
            return;
        }
        toast("Text…");
        new Thread(() -> {
            final boolean ok = DualScreen.openSecondaryHome(this, dm);
            runOnUiThread(() -> {
                toast(ok ? "Text" : "Text，Text Shizuku Text");
                refreshSubState();
            });
        }, "extrot-sub").start();
    }

    /** [comment]：[comment]、id Yes[comment] */
    private void refreshSubState() {
        if (tvSubState == null) {
            return;
        }
        int id = CoverDisplay.id(this);
        tvSubState.setText(id <= 0
                ? "Text。"
                : ("Text（Text）display=" + id + "。"));
    }

    /**
     * [comment]（v4.17 [comment]Yes[comment]）。
     *
     * <p>[comment]Yes {@link CheckBox}：[comment]Yes"[comment]"，[comment]Yes[comment]。
     * {@code color} Yes[comment]，[comment]、[comment]；
     * [comment] id [comment] {@code swPseudo*}，Yes[comment]，[comment]。
     *
     * <p>⚠ <b>[comment] setChecked [comment]，[comment]</b>：[comment] setChecked [comment]
     * 「[comment]」，[comment] prefs —— [comment]「Default[comment]」[comment]
     * [comment]，[comment]。
     *
     * <p>[comment] / [comment]：[comment] {@code load()} [comment]，
     * [comment] prefs + Notice[comment]，[comment]。
     */
    private void bindPseudoToggle(int viewId, String pkg, boolean defaultOn, int color) {
        CheckBox cb = findViewById(viewId);
        if (cb == null) {
            return;
        }
        boolean on = ExtPrefs.pseudoOn(this, pkg, defaultOn);
        cb.setChecked(on);
        applyPseudoLook(cb, color, on);
        cb.setOnCheckedChangeListener((b, isOn) -> {
            ExtPrefs.setPseudoOn(this, pkg, isOn);
            applyPseudoLook(cb, color, isOn);
            String name = cb.getText().toString();
            toast(isOn ? "Text「" + name + "」"
                    : "「" + name + "」Text");
        });
    }

    /**
     * [comment]：[comment] = [comment]，[comment]Yes[comment]；[comment] / [comment]。
     *
     * <p>[comment] 8dp，[comment] {@code drawable/pseudo_toggle_bg.xml} [comment] —— [comment]
     * [comment]，[comment]。
     */
    private void applyPseudoLook(CheckBox cb, int color, boolean on) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(getResources().getDisplayMetrics().density * 8f);
        bg.setColor(on ? color : 0x1F888888);
        cb.setBackground(bg);
        cb.setTextColor(on ? 0xFFFFFFFF : 0xFFBBBBBB);
    }

    // ---- DeX [comment]

    /**
     * DeX [comment]。[comment]（{@code com.tianweixing.atianwidget}）[comment]
     * {@code SamsungDexWidgetProvider}，2026-10-18 [comment]：
     *
     * <pre>{@code
     * if cmd display get-displays | grep -q 'type EXTERNAL'; then
     *   if [ "$(id -u)" = "0" ]; then pkill surfaceflinger;
     *   else settings put global navigationbar_current_color 0xFF; fi
     *   for i in 1 2 3 4 5 6 7 8 9 10; do
     *     if cmd display get-displays | grep -q FLAG_EXTERNAL_DEX_HOSTING; then
     *       cmd display power-off 0; cmd display power-off 1; break
     *     fi; sleep 1
     *   done
     * fi
     * }</pre>
     *
     * <p><b>[comment]Yes[comment] am start</b>：Flip5 [comment] DeX，[comment] root [comment]
     * {@code pkill surfaceflinger} [comment]，[comment] DeX [comment]
     * [comment] hosting（{@code cmd display get-displays} [comment]
     * {@code FLAG_EXTERNAL_DEX_HOSTING}），[comment] —— [comment] am start。
     * [comment]「settings [comment] + [comment] freeform + am start SecondaryLauncher」[comment]：
     * [comment] One UI 8 [comment]，am start [comment]，[comment] hosting [comment]
     * [comment]（[comment]：[comment] + [comment] dock，[comment]）—— [comment]Yes
     * 「[comment] dex」。
     *
     * <p>am start [comment]：[comment] root、[comment] SF [comment] hosting [comment]，[comment]
     * [comment]Yes[comment]，[comment]Success。[comment] {@link #tvDexInfo}。
     */
    private void startDex() {
        if (!requireShell()) {
            return;
        }
        if (tvDexInfo != null) {
            tvDexInfo.setText("Text DeX…");
        }
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            // ① [comment] —— [comment]：get-displays [comment] type EXTERNAL
            String ext = ShellRunner.run(
                    "cmd display get-displays | grep 'type EXTERNAL'", 15);
            if (ext == null || ext.startsWith("ERR") || ext.trim().isEmpty()) {
                sb.append("Text。Text。");
                finishDex(sb.toString(), false);
                return;
            }
            int dexId = parseExternalId(ext);
            sb.append("Text displayId = ").append(dexId).append('\n');
            // ② root [comment] SurfaceFlinger —— [comment]，DeX [comment] hosting。
            //    [comment]Yes[comment]：[comment] root [comment] Shizuku [comment]Yes[comment]。
            boolean hosted = false;
            if (ShellRunner.rootReady()) {
                sb.append(step("Text SurfaceFlinger",
                        ShellRunner.run("pkill surfaceflinger", 10), ""));
                // SF [comment]Yes[comment]，[comment]（[comment] applySelected [comment]）
                ShellRunner.run("cmd display power-on 0", 10);
                hosted = waitDexHosting(12);
            } else {
                sb.append("Text root，Text hosting（Text root）。\n");
                // [comment] root [comment] Shizuku [comment]：[comment] hosting [comment]
                hosted = waitDexHosting(6);
            }
            if (hosted) {
                // ③ hosting Success = [comment] DeX [comment]。[comment]，
                //    [comment]（[comment]：power-off 0 + power-off 1）。
                ShellRunner.run("cmd display power-off 0; cmd display power-off 1", 10);
                sb.append("DeX Text，Text。");
                finishDex(sb.toString(), true);
                return;
            }
            // ④ [comment]：[comment] am start [comment] SecondaryLauncher（LibreDeX [comment]，
            //    [comment]）。[comment] force-stop [comment] —— LibreDeX [comment]
            //    restartSecondaryLauncher [comment]Yes[comment]。[comment] hosting [comment]Yes[comment]。
            sb.append("Text hosting，Text DeX Text……\n");
            ShellRunner.run("am force-stop com.sec.android.app.launcher", 10);
            String amOut = ShellRunner.run(
                    "am start --display " + dexId
                            + " --activityType 2 --windowingMode 1"
                            + " -a android.intent.action.MAIN"
                            + " -c android.intent.category.SECONDARY_HOME"
                            + " -n com.sec.android.app.launcher"
                            + "/com.honeyspace.dexservice.SecondaryLauncher", 20);
            sb.append("am start Text：").append(
                    amOut == null ? "Text（Text）"
                            : amOut.startsWith("ERR") ? amOut
                            : tail(amOut.trim(), 150)).append('\n');
            // ⑤ [comment]（LibreDeX [comment] isSecondaryLauncherOnDisplay [comment]）
            boolean onDex = false;
            for (int i = 0; i < 10 && !onDex; i++) {
                if (i > 0) {
                    try {
                        Thread.sleep(700);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                String v = ShellRunner.run(
                        "dumpsys window windows | grep -A 1 'dexservice.SecondaryLauncher'"
                                + " | grep -oE 'mDisplayId=[0-9]+' | head -1", 15);
                onDex = v != null && !v.startsWith("ERR")
                        && v.contains("mDisplayId=" + dexId);
            }
            if (onDex) {
                sb.append("Text DeX（Text）—— ")
                        .append(ShellRunner.rootReady()
                                ? "Text hosting，Text。"
                                : "Text DeX，Text root（KernelSU / Magisk）Text hosting。");
            } else {
                sb.append("Text DeX Text。");
            }
            finishDex(sb.toString(), false);
        }, "extrot-dex").start();
    }

    /**
     * [comment] DeX hosting [comment]：{@code cmd display get-displays} [comment]
     * {@code FLAG_EXTERNAL_DEX_HOSTING} [comment] hosting DeX。
     *
     * @param maxSec [comment]（[comment]；SF [comment]，[comment]）
     */
    private boolean waitDexHosting(int maxSec) {
        for (int i = 0; i < maxSec; i++) {
            String v = ShellRunner.run(
                    "cmd display get-displays | grep -q FLAG_EXTERNAL_DEX_HOSTING"
                            + " && echo HOSTED || echo NO", 15);
            if (v != null && v.contains("HOSTED")) {
                return true;
            }
            if (i + 1 < maxSec) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        return false;
    }

    /**
     * [comment]（[comment]，[comment]）。
     *
     * @param expect [comment]；[comment]，[comment]。
     *               [comment]"Success[comment]"（wm [comment]）
     */
    private static String step(String what, String out, String expect) {
        if (out == null) {
            return what + "：Text（Text）\n";
        }
        if (out.startsWith("ERR")) {
            return what + "：" + out + '\n';
        }
        String s = out.trim();
        if (s.startsWith("TIMEOUT")) {
            return what + "：Text\n";
        }
        if (expect != null && !expect.isEmpty()) {
            if (expect.equals(s)) {
                return what + "：Text\n";
            }
            return what + "：Text（" + tail(s, 120) + "）\n";
        }
        if (s.isEmpty()) {
            return what + "：Text\n";
        }
        return what + "：" + tail(s, 200) + '\n';
    }

    /**
     * [comment] {@code cmd display get-displays | grep 'type EXTERNAL'} [comment]
     * displayId —— [comment]（get-displays [comment] DisplayInfo [comment]
     * {@code type XXX}）。[comment] -1。
     */
    private static int parseExternalId(String out) {
        if (out == null || out.startsWith("ERR")) {
            return -1;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("Display id (\\d+)").matcher(out);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** DeX [comment]：[comment] + [comment] toast */
    private void finishDex(String text, boolean up) {
        runOnUiThread(() -> {
            if (tvDexInfo != null) {
                tvDexInfo.setText(text);
            }
            toast(up ? "DeX Text，Text" : "Text，Text");
        });
    }

    /** [comment] —— shell [comment]，[comment] */
    private static String tail(String s, int n) {
        return s.length() <= n ? s : ("…" + s.substring(s.length() - n));
    }

    /** [comment] */
    private void refreshDualState(boolean withFold) {
        if (tvDualState == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        Display def = DisplayUtil.defaultDisplay(dm);
        sb.append("Text：ID 0 · ").append(DisplayUtil.sizeOf(def))
                .append(" · ").append(DisplayUtil.isOn(def) ? "Text" : "Text").append('\n');

        List<Display> secs = DisplayUtil.secondaryDisplays(dm);
        if (secs.isEmpty()) {
            sb.append("Text：Text\n");
            sb.append("　（Text「Text」Text；Text）\n");
        } else {
            for (Display d : secs) {
                sb.append(DisplayUtil.kindOf(dm, d))
                        .append("：ID ").append(d.getDisplayId())
                        .append(" · ").append(DisplayUtil.sizeOf(d));
                int[] n = DisplayUtil.nativeSize(d);
                if (n[0] > 0) {
                    sb.append(" · Text ").append(n[0]).append("×").append(n[1]);
                }
                sb.append(" · ").append(DisplayUtil.isOn(d) ? "Text ✓" : "Text");
                sb.append(d.getDisplayId() == coverDisplayId() ? "　← Text" : "　← Text");
                sb.append('\n');
            }
        }

        // [comment]：[comment]。[comment]「[comment]」[comment]。
        // [comment]Yes DisplayUtil.allDisplays（[comment] id [comment]），
        // [comment] dm.getDisplays() [comment] —— [comment]Yes[comment]。
        Display d0 = DisplayUtil.defaultDisplay(dm);
        sb.append("DefaultText：").append(d0 == null ? "?" : String.valueOf(d0.getName())).append('\n');
        try {
            for (Display d : DisplayUtil.allDisplays(dm)) {
                sb.append("id=").append(d.getDisplayId())
                        .append(" Text=").append(String.valueOf(d.getName()))
                        .append(" cutout=").append(d.getCutout() != null)
                        .append(" flags=0x").append(Integer.toHexString(d.getFlags()))
                        .append('\n');
            }
        } catch (Throwable ignored) {
        }

        tvDualState.setText(sb.toString());

        if (withFold) {
            refreshFoldState();
        }
    }

    private boolean requireShell() {
        if (!ShellRunner.isReady()) {
            toast("Text Shizuku Text Root Text");
            return false;
        }
        return true;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
