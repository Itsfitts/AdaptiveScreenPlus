package com.wb.extrotator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * 「[comment]Update」[comment]，[comment]「vX.Y · Update[comment]」[comment]。
 * [comment]，[comment]。
 */
public class ChangelogActivity extends Activity {

    /**
     * Update[comment]。[comment]Yes {[comment], [comment], [comment] 1 [comment], [comment] 2 [comment], ...}，[comment]。
     */
    // ⚠ [comment]：[comment]Add / [comment]，[comment]；[comment]、[comment]，
    //   UI [comment]、[comment]，[comment]。
    // ⚠ [comment]：[comment]No[comment]，[comment]，[comment]，
    //   [comment]、[comment]。
    private static final String[][] LOG = {
            {"v4.26", "2026-10-19",
                    "TextNoticeText"},
            {"v4.25", "2026-10-19",
                    "UpdateTextAdd「TextUpdate」，Text",
                    "TextAdd root Text（Text KernelSU / APatch）；DeX Text root Text SurfaceFlinger Text"},
            {"v4.24", "2026-10-18",
                    "Text：Text，TextAddText",
                    "Beta Text：AddText"},
            {"v4.23", "2026-09-26",
                    "Text：Text，TextAddText；Text",
                    "AddText「CustomText」「Text」、Text「Text」，Text"},
            {"v4.22", "2026-09-26",
                    "Text：Text，Text、Text，Text",
                    "Add「Text」（Beta Text · Text、Text、Text）；Text：Text"},
            {"v4.21", "2026-09-26",
                    "Text、Text Shizuku，「Text」Add「TextSettingsText」",
                    "Add「Text」Text（Text）；Text「Text」Text"},
            {"v4.20", "2026-09-26",
                    "Text：「Text」Text，TextAdd「Text」"},
            {"v4.19", "2026-09-23",
                    "Text",
                    "Text",
                    "Text：Text"},
            {"v4.18", "2026-09-23",
                    "「Text」Text"},
            {"v4.17", "2026-09-22",
                    "Text、Text",
                    "Beta TextSettingsText「Text」，TextSettingsText"},
            {"v4.16", "2026-09-22",
                    "TextAdd「Text」，TextAdd「Text」",
                    "Text；Text：Text、Text，Text"},
            {"v4.15", "2026-09-22",
                    "Add「Text」（Text）Text「Text」（Text）",
                    "Text：Text ⓘ Text",
                    "Text：Beta Text「TextNoneText」Text"},
            {"v4.14", "2026-09-22",
                    "Text（Text）"},
            {"v4.13", "2026-09-22",
                    "「TextNoneText」Text（Text）",
                    "Text ⓘ Text；Text「Text」Text，Text"},
            {"v4.12", "2026-09-21",
                    "Add「TextNoneText」Text（Text）",
                    "Beta Add「Text」；Text"},
            {"v4.11", "2026-09-21",
                    "「Text」Text",
                    "TextAddText"},
            {"v4.10", "2026-09-21",
                    "「Text」Text，「Text」TextDelete",
                    "「Text」TextAdd「Text」"},
            {"v4.9", "2026-09-20",
                    "「Text」Text「Text」，Add「Text 0° Text」",
                    "「Text」Text「Text」Text，Add「Text DeX Text」"},
            {"v4.8", "2026-09-20",
                    "Text：Text「NoneText」"},
            {"v4.7", "2026-09-20",
                    "Text：Text、Text",
                    "Text，Text"},
            {"v4.5", "2026-09-20",
                    "Text",
                    "Text：Text"},
            {"v4.4", "2026-09-20",
                    "Add「Text」"},
            {"v4.3", "2026-09-20",
                    "Text：「Text」Text",
                    "Text：Text"},
            {"v4.2", "2026-09-19",
                    "「Text」Text「Text」Text；「Text」Text「Text」",
                    "「Text」Text"},
            {"v4.1", "2026-09-19",
                    "Add「Text」Text，Text「Text」Text「Text」"},
            {"v4.0", "2026-09-19",
                    "Add「Text」",
                    "「Text」Text「Text」；Text「Text」"},
            {"v3.2", "2026-09-18",
                    "「Text」Text「Text」Text，Text"},
            {"v3.1", "2026-09-18",
                    "Text：Text；NoneText"},
            {"v3.0", "2026-09-18",
                    "Add「Beta Text」，TextCustomText「Text」",
                    "AddText DPI Text、Text、TextCustomText"},
            {"v2.17", "2026-09-15",
                    "Text：「SaveTextSettings」Text「Text」Text「Text」Text（v2.14 Text）"},
            {"v2.16", "2026-09-15",
                    "Text：「Text/Text」Text"},
            {"v2.9–v2.15", "2026-09-15",
                    "Text：Text、Text、Text、Text「Text/Text」Text",
                    "Text：Text 2×2；Text「Text / Text」Text"},
            {"v2.8", "2026-09-15",
                    "Text：Text/Text「Text」；Text「Text」Text",
                    "Text：Text（v2.9 Text 2×2 Text）"},
            {"v2.7", "2026-09-15",
                    "Text：Text「Text / Text / Text」Text",
                    "Text（SaveTextSettings · Text · Text/Text · Text · Text）"},
            {"v2.6", "2026-09-15",
                    "「TextSaveSettings」Text，TextSave / Text",
                    "Text：Text；Text / Text"},
            {"v2.5", "2026-09-15",
                    "「Text」Text；Add「TextSaveSettings」",
                    "Text：Text"},
            {"v2.4", "2026-09-15",
                    "「Text」Text",
                    "Text，Text"},
            {"v2.3", "2026-09-14",
                    "Text：Text「ⓘ」",
                    "AddText"},
            {"v2.2", "2026-09-14",
                    "Text：EnableText 270°",
                    "Text「Text」"},
            {"v2.1", "2026-09-14",
                    "Text"},
            {"v2.0", "2026-09-13",
                    "Text：Text 270°、Text、Text",
                    "TextAddText"},
            {"v1.9", "2026-09-13",
                    "Text「Text」",
                    "AddText"},
            {"v1.8", "2026-09-13",
                    "TextYesText：Text",
                    "Add「Text」Text"},
            {"v1.7", "2026-09-13",
                    "Text「Text」，Text",
                    "AddText / Text「Text」Text"},
            {"v1.6", "2026-09-13",
                    "Text：Text 1/4 Text（Text）",
                    "Text"},
            {"v1.5", "2026-09-13",
                    "Text：Text 2.6~3.3 Text 0.44 Text",
                    "Text，Text"},
            {"v1.4", "2026-09-13",
                    "Text",
                    "Text"},
            {"v1.3", "2026-09-13",
                    "Text，Text SecondScreen",
                    "Text，Text"},
            {"v1.2", "2026-09-13",
                    "Text：Text",
                    "AddText"},
            {"v1.1", "2026-09-13",
                    "Text：Shizuku TextAuthorized，Text"},
            {"v1.0", "2026-09-13",
                    "Text：Text 90°"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changelog);

        findViewById(R.id.btnChangeBack).setOnClickListener(v -> finish());

        String versionName = "—";
        long versionCode = 0L;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pi.versionName == null ? "—" : pi.versionName;
            versionCode = pi.getLongVersionCode();
        } catch (Throwable ignored) {
        }

        ((TextView) findViewById(R.id.tvChangeVersion)).setText("v" + versionName);
        ((TextView) findViewById(R.id.tvChangeBuild)).setText(
                getString(R.string.changelog_current)
                        + " v" + versionName + "（versionCode " + versionCode + "）");

        renderLog(findViewById(R.id.changeContainer), versionName);

        wireUpdateRow(versionName);
    }

    // ---------------- [comment]Update（v4.25） ----------------

    private TextView tvUpdateState;

    /** [comment]「[comment]Update」[comment]：[comment]，[comment] / [comment]。 */
    private void wireUpdateRow(String currentVersion) {
        Button btn = findViewById(R.id.btnCheckUpdate);
        tvUpdateState = findViewById(R.id.tvUpdateState);
        btn.setOnClickListener(v -> checkUpdate(currentVersion));
    }

    /** [comment] Release：[comment]；[comment]Yes[comment] / Failed[comment]Update[comment]。 */
    private void checkUpdate(String currentVersion) {
        tvUpdateState.setText(R.string.update_checking);
        new Thread(() -> {
            try {
                AppUpdater.Info info = AppUpdater.check(currentVersion);
                runOnUiThread(() -> {
                    if (info == null) {
                        tvUpdateState.setText(R.string.update_latest);
                    } else {
                        showUpdateDialog(info);
                    }
                });
            } catch (Throwable t) {
                Log.w("Changelog", "check update failed", t);
                runOnUiThread(() -> {
                    tvUpdateState.setText(R.string.update_failed);
                    Toast.makeText(this, String.valueOf(t.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "check-update").start();
    }

    /** [comment]：[comment]Update[comment]，[comment]。 */
    private void showUpdateDialog(AppUpdater.Info info) {
        String msg = (info.notes == null || info.notes.trim().isEmpty())
                ? "—" : info.notes.trim();
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.update_new_title) + " " + info.version)
                .setMessage(msg)
                .setNegativeButton(R.string.update_cancel, null)
                .setPositiveButton(R.string.update_btn_now, (d, w) -> downloadUpdate(info))
                .show();
    }

    /** [comment] + [comment]：root / Shizuku [comment]，[comment]。 */
    private void downloadUpdate(AppUpdater.Info info) {
        tvUpdateState.setText(R.string.update_downloading);
        new Thread(() -> {
            try {
                File apk = AppUpdater.download(this, info, pct -> runOnUiThread(() ->
                        tvUpdateState.setText(pct < 0
                                ? getString(R.string.update_downloading)
                                : getString(R.string.update_downloading) + " " + pct + "%")));
                runOnUiThread(() -> {
                    tvUpdateState.setText(R.string.update_installing);
                    if (AppUpdater.silentInstall(apk.getAbsolutePath())) {
                        tvUpdateState.setText(R.string.update_done);
                    } else {
                        launchSystemInstaller();
                    }
                });
            } catch (Throwable t) {
                Log.w("Changelog", "download update failed", t);
                runOnUiThread(() -> {
                    tvUpdateState.setText(R.string.update_failed);
                    Toast.makeText(this, String.valueOf(t.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "download-update").start();
    }

    /** [comment] APK [comment] ApkProvider [comment]；[comment]「[comment]」[comment]Settings[comment]。 */
    private void launchSystemInstaller() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(ApkProvider.uri(), "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Throwable t) {
            try {
                startActivity(new Intent(
                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName())));
            } catch (Throwable ignored) {
                tvUpdateState.setText(R.string.update_failed);
            }
        }
    }

    /** [comment] {@link #LOG} [comment]。 */
    private void renderLog(LinearLayout box, String currentVersion) {
        if (box == null) {
            return;
        }
        for (int i = 0; i < LOG.length; i++) {
            String[] entry = LOG[i];
            boolean isCurrent = ("v" + currentVersion).equalsIgnoreCase(entry[0]);

            // ---- [comment] + [comment]（[comment]）----
            LinearLayout head = new LinearLayout(this);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams headLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            headLp.topMargin = dp(i == 0 ? 14 : 22);
            head.setLayoutParams(headLp);

            TextView ver = new TextView(this);
            ver.setText(entry[0]);
            ver.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            ver.setTypeface(Typeface.DEFAULT_BOLD);
            ver.setTextColor(0xFF1565C0);
            head.addView(ver);

            TextView date = new TextView(this);
            date.setText(entry[1]);
            date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            date.setTextColor(0x99000000);
            LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            dateLp.leftMargin = dp(8);
            date.setLayoutParams(dateLp);
            head.addView(date);

            if (isCurrent) {
                TextView now = new TextView(this);
                now.setText("Text");
                now.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                now.setTextColor(0xFF1565C0);
                now.setBackgroundResource(R.drawable.version_chip);
                now.setPadding(dp(8), dp(2), dp(8), dp(2));
                LinearLayout.LayoutParams nowLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                nowLp.leftMargin = dp(8);
                now.setLayoutParams(nowLp);
                head.addView(now);
            }
            box.addView(head);

            // ---- [comment] ----
            for (int k = 2; k < entry.length; k++) {
                TextView bullet = new TextView(this);
                bullet.setText("·  " + entry[k]);
                bullet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                bullet.setTextColor(0xDE000000);
                bullet.setLineSpacing(dp(3), 1f);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(5);
                lp.leftMargin = dp(2);
                bullet.setLayoutParams(lp);
                box.addView(bullet);
            }
        }
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }
}
