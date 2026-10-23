package com.wb.extrotator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * [comment]（[comment]）—— [comment]，[comment]<b>[comment]</b>[comment]。
 *
 * <p>[comment]「[comment]」[comment]：[comment]，[comment]，[comment]
 * [comment]（[comment]，[comment]Yes[comment]）。
 *
 * <p>[comment]Yes[comment]（[comment] {@link CursorOverlay}），[comment]「[comment]」[comment]，
 * [comment]。[comment] —— [comment]，
 * [comment]，[comment] root None[comment]。
 *
 * <p>⚠ <b>[comment]Yes[comment]</b>（[comment] {@link #blocked()}），[comment]。
 *
 * <p>[comment] {@link TouchInjector}：[comment] Shizuku Binder [comment]，[comment]、[comment]；
 * [comment]，[comment]「[comment]」，[comment]。[comment]None[comment]
 * （[comment]，[comment]，[comment]），「[comment]」[comment]，
 * [comment]。[comment]（[comment] Shizuku）[comment] {@link TouchInjector} [comment] {@code input}，
 * [comment]。
 */
public class TouchpadActivity extends Activity implements TouchpadView.Listener {

    private static final String TAG = "ExtTouchpad";

    /**
     * 「[comment]」[comment]（[comment]「[comment]」[comment]，[comment]Yes[comment]）。
     *
     * <p>[comment]：[comment]Yes「[comment]（500ms）[comment] DOWN [comment]」，[comment]
     * DOWN [comment] —— [comment] 0.6 [comment]Yes[comment]，[comment] 0.1 [comment]，[comment]
     * [comment]。[comment] {@link TouchInjector} [comment]，[comment]。
     */
    private static final int HOLD_MS = 700;

    private TouchpadView pad;
    private TextView tvTitle;
    private TextView tvHint;
    private Button btnSens;

    /** [comment]（[comment] displayId [comment]；[comment] {@link CursorOverlay} [comment]） */
    private ExtScreen.Dev dev;

    private boolean started;
    /** [comment] —— [comment] */
    private int downCx, downCy;
    /** [comment]Yes[comment]Yes"[comment] / [comment]"[comment]（[comment] DOWN [comment]，[comment] UP） */
    private boolean heldDown;
    /** [comment] MOVE */
    private int lastMoveX = -1, lastMoveY = -1;
    /** [comment] —— [comment]（[comment] {@link TouchInjector}） */
    private TouchInjector inj;
    /** [comment]（[comment]） */
    private volatile boolean padDirect;
    /** [comment] Handler（[comment]） */
    private final android.os.Handler ui =
            new android.os.Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ext_touchpad);
        // [comment]，[comment]
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        /*
         * ⚠⚠ [comment]**[comment]**[comment] FLAG_NOT_FOCUSABLE。
         *
         * [comment]Yes[comment]，[comment]「[comment]」；
         * [comment]，[comment] ANR、[comment] ——
         * [comment]Yes**[comment]、[comment]**（events [comment]Yes
         * {@code am_anr: Input dispatching timed out (Application does not have a focused window)}）。
         *
         * [comment]Yes[comment]「[comment]」：
         * [comment] {@link #onTargetScreen()} [comment]
         * —— [comment]。[comment]。
         */

        pad = findViewById(R.id.padView);
        tvTitle = findViewById(R.id.tvPadTitle);
        tvHint = findViewById(R.id.tvPadHint);
        btnSens = findViewById(R.id.btnPadSens);

        pad.setListener(this);
        pad.setCenterHint(getString(R.string.ext_pad_center_hint));

        ExtUi.info(this, R.id.iPadTop, R.string.ext_pad_title, R.string.info_ext_pad);

        findViewById(R.id.btnPadClose).setOnClickListener(v -> finish());
        btnSens.setOnClickListener(v -> {
            int next = (ExtPrefs.sensIndex(this) + 1) % ExtPrefs.SENS_ALL.length;
            ExtPrefs.setSensIndex(this, next);
            syncSensButton();
            flash(getString(R.string.ext_flash_sens, ExtPrefs.SENS_ALL[next]));
        });

        // [comment]：[comment]"[comment]"[comment]，[comment]
        findViewById(R.id.btnPadClick).setOnClickListener(v -> tapAtCursor());
        findViewById(R.id.btnPadHold).setOnClickListener(v -> holdAtCursor());
        findViewById(R.id.btnPadBack).setOnClickListener(v -> sendKey("KEYCODE_BACK"));
        findViewById(R.id.btnPadHome).setOnClickListener(v -> sendKey("KEYCODE_HOME"));
        // [comment] —— [comment]
        findViewById(R.id.btnPadPrevPage).setOnClickListener(v -> pageFlip(-1));
        findViewById(R.id.btnPadNextPage).setOnClickListener(v -> pageFlip(+1));

        // None[comment]，[comment] input [comment]（[comment] A11yInject）——
        // [comment]Yes"[comment]"[comment]，[comment]None[comment]Settings[comment]。
        tvHint.setOnClickListener(v -> {
            if (TouchInject.ready() || A11yInject.ready()) {
                return;
            }
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                ExtUi.toast(this, getString(R.string.ext_pad_a11y_toast));
            } catch (Throwable e) {
                Log.w(TAG, "TextNoneTextSettingsText: " + e);
            }
        });

        syncSensButton();
        resolve();
    }

    private void syncSensButton() {
        btnSens.setText(getString(R.string.ext_sens_btn, ExtPrefs.sens(this)));
    }

    // ------------------------------------------------------------------ [comment]

    @Override
    protected void onStart() {
        super.onStart();
        started = true;
        // [comment]，[comment] / [comment]Settings（[comment] ExtControlMode）——
        // No[comment]，[comment] 90°。
        ExtControlMode.on();
        // [comment]（[comment] / wm [comment]）[comment]
        CursorOverlay.setBoundsListener(this::syncCursorText);
        // [comment]Yes[comment]Settings[comment]None[comment] —— [comment]，
        // [comment]Notice[comment]"[comment]"[comment]
        if (tvHint != null) {
            syncCursorText();
            tvHint.setText(defaultHint());
        }
        if (dev != null) {
            startCursor();
            ensureInjector();
        }
    }

    @Override
    protected void onStop() {
        started = false;
        // [comment]（[comment]「Disable」）[comment] UP [comment]，
        // No[comment]，[comment]
        releaseHold();
        // [comment]（[comment]，[comment]"[comment]"）
        stopInjector();
        // [comment]：[comment]
        CursorOverlay.hide();
        CursorOverlay.setBoundsListener(null);
        ExtControlMode.off();
        super.onStop();
    }

    private void resolve() {
        tvHint.setText(R.string.ext_finding);
        ExtUi.resolve(this, d -> {
            dev = d;
            tvTitle.setText(getString(R.string.ext_title_inner, getString(R.string.ext_pad_title)));
            if (dev == null) {
                tvHint.setText(R.string.ext_pad_no_target);
                pad.setInfoText("");
                return;
            }
            if (started) {
                startCursor();
                ensureInjector();
            }
        });
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]。
     *
     * <p>[comment] onCreate [comment]：[comment] {@link #resolve()} [comment]
     * Yes[comment]（[comment]Yes[comment]，[comment]Yes[comment]），[comment] {@link TouchInjector} [comment]
     * [comment]Yes[comment] —— [comment]。
     */
    private void ensureInjector() {
        if (inj != null || dev == null) {
            return;
        }
        // [comment]Yes"[comment]"，[comment] HOLD_MS [comment]
        inj = new TouchInjector("ExtPadInject", dev.id, HOLD_MS);
        inj.setStatus((direct, detail) -> {
            padDirect = direct;
            ui.post(this::syncCursorText);
        });
        inj.start();
    }

    private void stopInjector() {
        if (inj != null) {
            inj.stop();
            inj = null;
        }
    }

    /** [comment]（[comment] / [comment]）—— [comment]，[comment] ExtScreen [comment] */
    private void swipe(int x0, int y0, int x1, int y1, int ms) {
        ensureInjector();
        if (inj != null) {
            inj.swipe(x0, y0, x1, y1, ms);
        } else {
            ExtScreen.swipe(dev.id, x0, y0, x1, y1, ms);
        }
    }

    private void startCursor() {
        if (!Settings.canDrawOverlays(this)) {
            needOverlayPermission();
            return;
        }
        if (!CursorOverlay.show(getApplicationContext(), ExtScreen.INNER_ID)) {
            tvHint.setText(R.string.ext_pad_cursor_fail);
            Log.w(TAG, "Text");
            return;
        }
        syncCursorText();
        tvHint.setText(defaultHint());
    }

    /**
     * [comment]。
     *
     * <p>[comment]：[comment]Yes[comment]，[comment]
     * [comment]Yes[comment]。[comment] —— Presentation [comment]（[comment]），
     * [comment] shell [comment]（[comment] {@link CursorOverlay} [comment]）。
     */
    private void needOverlayPermission() {
        tvHint.setText(R.string.ext_pad_need_overlay);
        pad.setInfoText("");
        new AlertDialog.Builder(this)
                .setTitle(R.string.ext_pad_title)
                .setMessage(R.string.info_ext_pad_overlay)
                .setPositiveButton(R.string.ext_grant, (d, w) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName())));
                    } catch (Throwable t) {
                        Log.w(TAG, "TextSettingsText: " + t);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Notice[comment]Default[comment] —— None[comment]"[comment]Yes[comment]、[comment]"。
     *
     * <p>[comment]：v3.7 [comment]None[comment] dispatchGesture [comment]，
     * [comment]DefaultYes[comment]（[comment] enabled_accessibility_services [comment]）。
     * [comment]Notice，[comment]"[comment]Yes[comment]"，[comment]。
     */
    private CharSequence defaultHint() {
        // [comment]：Shizuku [comment]（[comment]）[comment]None[comment]。[comment]Yes"[comment]"。
        if (TouchInject.ready() || A11yInject.ready()) {
            tvHint.setTextColor(0xFF6C7889);
            return getString(R.string.ext_pad_hint);
        }
        tvHint.setTextColor(0xFFE8B44A);
        return getString(R.string.ext_pad_need_a11y);
    }

    /** [comment]：[comment]、[comment]、[comment] */
    private void syncCursorText() {
        int[] b = CursorOverlay.bounds();
        if (b == null) {
            return;
        }
        pad.setInfoText(getString(R.string.ext_pad_info_fmt,
                CursorOverlay.x(), CursorOverlay.y(), b[0], b[1], pathText()));
    }

    /**
     * [comment]Yes[comment]。
     *
     * <p>[comment]，[comment]：<b>[comment]</b>（Shizuku Binder，[comment]）→
     * <b>[comment]</b>（None[comment] dispatchGesture，[comment]）→ <b>[comment]</b>（input [comment]，[comment] 47ms）。
     * [comment]"[comment]"，[comment]Yes[comment]Yes[comment]。
     */
    private String pathText() {
        if (padDirect || TouchInject.ready()) {
            return getString(R.string.ext_path_direct);
        }
        return getString(A11yInject.ready() ? R.string.ext_path_gesture : R.string.ext_path_input);
    }

    // ------------------------------------------------------------------ [comment]

    @Override
    public void onMove(float dx, float dy) {
        if (!CursorOverlay.isOn()) {
            return;
        }
        float s = ExtPrefs.sens(this);
        CursorOverlay.move(
                CursorOverlay.x() + Math.round(dx * s),
                CursorOverlay.y() + Math.round(dy * s));
        syncCursorText();
    }

    @Override
    public void onPress(boolean down) {
        CursorOverlay.setHot(down);
        if (down) {
            downCx = CursorOverlay.x();
            downCy = CursorOverlay.y();
        }
    }

    @Override
    public void onTap() {
        tapAtCursor();
    }

    /**
     * [comment] —— [comment] DOWN [comment]，[comment]"[comment]"[comment]。
     *
     * <p>v3.6 [comment]Yes[comment]（[comment] `input swipe`），[comment]
     * [comment]None[comment]，[comment]Yes"[comment]"。
     */
    @Override
    public void onHoldStart() {
        if (blocked()) {
            return;
        }
        downCx = CursorOverlay.x();
        downCy = CursorOverlay.y();
        heldDown = true;
        lastMoveX = lastMoveY = -1;
        /*
         * [comment] DOWN —— [comment]Yes「[comment]」[comment]。
         *
         * [comment]None[comment]（willContinue [comment]）：[comment]，[comment]
         * [comment]，[comment]「[comment]」[comment] —— [comment]，[comment]Yes
         * [comment]。[comment]Yes[comment]，[comment]。
         */
        ensureInjector();
        if (inj != null) {
            inj.press(downCx, downCy);
        } else {
            ExtScreen.motionDown(dev.id, downCx, downCy);
        }
        flash(getString(R.string.ext_flash_hold, downCx, downCy));
    }

    /** [comment] —— [comment]，[comment] */
    @Override
    public void onDragMove(float dx, float dy) {
        if (!heldDown || blocked()) {
            return;
        }
        int x = CursorOverlay.x(), y = CursorOverlay.y();
        if (x == lastMoveX && y == lastMoveY) {
            return;   // Text
        }
        lastMoveX = x;
        lastMoveY = y;
        if (inj != null) {
            /*
             * [comment]，[comment] —— **[comment]**。
             * [comment]（MOVE_GAP_MS）Yes[comment]，[comment]；
             * [comment] 1ms，[comment]。[comment]，
             * [comment]Yes[comment]。
             */
            inj.move(x, y);
        } else {
            ExtScreen.motionMove(dev.id, x, y);
        }
    }

    /** [comment] —— [comment] / [comment] */
    @Override
    public void onHoldEnd() {
        if (!heldDown) {
            return;
        }
        heldDown = false;
        if (dev == null) {
            return;
        }
        final int x = CursorOverlay.x(), y = CursorOverlay.y();
        /*
         * [comment] blocked() [comment] —— [comment] DOWN [comment]，UP [comment]，
         * [comment]。
         *
         * [comment]，[comment]，[comment] —— [comment]
         * [comment]（[comment] HOLD_MS），[comment]，[comment] UP。
         */
        if (inj != null) {
            inj.release(x, y);
        } else {
            ExtScreen.motionUp(dev.id, x, y);
        }
        if (x != downCx || y != downCy) {
            flash(getString(R.string.ext_flash_drag, downCx, downCy, x, y));
        }
    }

    @Override
    public void onScroll(float dy) {
        if (blocked()) {
            return;
        }
        int[] b = CursorOverlay.bounds();
        if (b == null) {
            return;
        }
        int x = CursorOverlay.x(), y = CursorOverlay.y();
        int ty = CursorOverlay.clamp(y + Math.round(dy * ExtPrefs.sens(this)), 0, b[1] - 1);
        swipe(x, y, x, ty, 260);
        flash(getString(R.string.ext_flash_scroll, ty - y));
    }
    /**
     * [comment] → [comment]。
     *
     * <p>[comment]：[comment]，"[comment]"[comment]，
     * [comment]Yes[comment]。[comment]，[comment]。
     */
    @Override
    public void onPage(int dir) {
        pageFlip(dir);
    }


    // ------------------------------------------------------------------ [comment]

    /**
     * [comment] UP [comment]（[comment]）。
     *
     * <p>⚠ [comment] {@code abort} [comment]Yes {@code release}：abort [comment]，
     * release [comment]"[comment]"[comment]（[comment]）——
     * [comment]、[comment]Disable，[comment]。
     */
    private void releaseHold() {
        if (!heldDown) {
            return;
        }
        heldDown = false;
        if (inj != null) {
            inj.abort();
        } else if (dev != null) {
            ExtScreen.motionUp(dev.id, CursorOverlay.x(), CursorOverlay.y());
        }
    }

    private void tapAtCursor() {
        if (blocked()) {
            return;
        }
        int x = CursorOverlay.x(), y = CursorOverlay.y();
        ensureInjector();
        if (inj != null) {
            inj.tap(x, y);
        } else {
            ExtScreen.tap(dev.id, x, y);
        }
        flash(getString(R.string.ext_flash_tap, x, y));
    }

    private void holdAtCursor() {
        if (blocked()) {
            return;
        }
        int x = CursorOverlay.x(), y = CursorOverlay.y();
        ensureInjector();
        if (inj != null) {
            inj.hold(x, y, HOLD_MS);
        } else {
            ExtScreen.hold(dev.id, x, y, HOLD_MS);
        }
        flash(getString(R.string.ext_flash_hold, x, y));
    }

    private void sendKey(String keyCode) {
        if (blocked()) {
            return;
        }
        ExtScreen.key(dev.id, keyCode);
    }

    /**
     * [comment]Yes[comment]Yes"[comment]"。
     *
     * <p>[comment]：[comment]，`input -d <[comment]> tap x y` [comment]
     * [comment] —— [comment]"[comment]"[comment]，[comment]Yes[comment]，
     * [comment]。
     *
     * @return true = [comment]
     */
    /**
     * [comment] —— [comment]。
     *
     * <p>[comment]：dir &gt; 0 = [comment]（[comment]），[comment]。
     * [comment] 18% / 82%（[comment]：[comment]）。
     */
    private void pageFlip(int dir) {
        if (blocked()) {
            return;
        }
        int[] b = CursorOverlay.bounds();
        if (b == null || b[0] <= 0 || b[1] <= 0) {
            flash(getString(R.string.ext_pad_cursor_fail));
            return;
        }
        int y = b[1] / 2;
        int from = (int) (b[0] * (dir > 0 ? 0.82f : 0.18f));
        int to = (int) (b[0] * (dir > 0 ? 0.18f : 0.82f));
        swipe(from, y, to, y, 220);
        flash(getString(dir > 0 ? R.string.ext_flash_page_next : R.string.ext_flash_page_prev));
    }


    private boolean blocked() {
        if (dev == null) {
            return true;
        }
        int mine;
        try {
            mine = getWindowManager().getDefaultDisplay().getDisplayId();
        } catch (Throwable t) {
            return false;
        }
        if (dev.id == mine) {
            String s = getString(R.string.ext_self_target);
            ExtUi.toast(this, s);
            flash(s);
            return true;
        }
        return false;
    }

    /** [comment]Notice[comment] —— [comment]"[comment]"[comment]，[comment]Yes[comment] */
    private void flash(String s) {
        if (tvHint == null) {
            return;
        }
        tvHint.setText(s);
        tvHint.removeCallbacks(restoreHint);
        tvHint.postDelayed(restoreHint, 1200);
    }

    private final Runnable restoreHint = () -> {
        if (tvHint != null && dev != null && !isFinishing()) {
            tvHint.setText(defaultHint());
        }
    };
}
