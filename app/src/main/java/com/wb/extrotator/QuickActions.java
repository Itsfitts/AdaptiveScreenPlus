package com.wb.extrotator;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 「[comment]」[comment]。
 *
 * <p><b>[comment]</b>：[comment]<b>[comment]</b> —— [comment]（[comment]
 * {@code ACTION_ASSIST}）、[comment] / [comment] App（[comment]Custom action [comment]）、
 * [comment] Beta [comment]「[comment]」[comment]。[comment] shell [comment]（[comment]
 * [comment]，[comment]），[comment]。
 *
 * <p>[comment]Yes[comment]：{@link CustomCmds} [comment]
 * （key [comment] {@code custom:<id>}），[comment] {@code label / isValidKey} [comment] Context [comment]。
 *
 * <p>⚠ [comment] run [comment] binder / sleep，<b>[comment]</b>。
 */
public final class QuickActions {

    private static final String TAG = "QuickActions";

    /** [comment] extra [comment]（intent [comment] broadcast [comment]） */
    public static final String EXTRA_ACTION = "action_key";
    /** [comment] action（[comment]、MacroDroid、[comment]） */
    public static final String ACTION_RUN = "com.wb.extrotator.action.RUN_ACTION";

    /** [comment] ↔ [comment] [comment] */
    public static final String KEY_TOGGLE_FOLD = "toggle_fold";
    /** [comment] */
    public static final String KEY_COVER_HOME = "cover_home";
    /** [comment] 90° */
    public static final String KEY_COVER_ROTATE = "cover_rotate";
    /** [comment]（[comment]） */
    public static final String KEY_COVER_ROTATE_RESET = "cover_rotate_reset";
    /**
     * [comment]None[comment]（[comment]，[comment]）。
     */
    public static final String KEY_ADB_WIFI = "adb_wifi";

    /** [comment]（[comment] + [comment]）—— [comment]「[comment]」[comment] */
    public static final String KEY_DUAL = "dual";
    /** [comment]「[comment]」（[comment]） */
    public static final String KEY_SYNC = "sync";
    /** [comment]「[comment]」—— [comment] */
    public static final String KEY_LOCK = "lock";
    /** [comment]：[comment]，[comment] */
    public static final String KEY_CAST = "cast";
    /** [comment] */
    public static final String KEY_OPEN_APP = "open_app";
    /**
     * [comment]「[comment]」—— [comment]（[comment] {@link CoverAutoRot}）。
     *
     * <p>[comment] {@link #KEY_COVER_ROTATE} [comment]Yes[comment]：[comment]Yes<b>[comment]</b>，[comment]Yes[comment]<b>[comment]</b>
     * —— [comment]，[comment]。
     */
    public static final String KEY_COVER_AUTOROT = "cover_autorot";

    /** [comment]（[comment]）—— [comment]，[comment] */
    public static final String KEY_COVER_DESK = "cover_desk";

    /**
     * 「None」—— [comment]。
     *
     * <p>[comment]<b>[comment]</b>[comment]：[comment] {@link VolumeKeyService} [comment]，
     * [comment] —— [comment]Yes[comment]「[comment]」。[comment]，[comment]
     * [comment]None[comment]（[comment]）。
     *
     * <p>[comment]<b>[comment] {@link #ALL}</b>：{@code ALL} Yes「[comment]」，「None」[comment]Yes[comment]
     * [comment]，[comment]Yes[comment]（[comment] BetaActivity.pickAction）。
     */
    public static final String KEY_NONE = "none";

    /** [comment] key，[comment] */
    public static final String[] ALL = {
            KEY_TOGGLE_FOLD, KEY_COVER_HOME, KEY_COVER_ROTATE, KEY_COVER_ROTATE_RESET,
            KEY_COVER_AUTOROT, KEY_COVER_DESK, KEY_ADB_WIFI, KEY_DUAL, KEY_SYNC, KEY_LOCK,
            KEY_CAST, KEY_OPEN_APP,
    };

    /**
     * [comment] = [comment] + [comment]Custom[comment]（{@link CustomCmds}）。
     *
     * <p>[comment]Yes「[comment]」[comment]，[comment]。
     * ⚠️ [comment] {@link #KEY_NONE} —— [comment]Yes"[comment]"，[comment]。
     */
    public static String[] options(Context ctx) {
        List<String> out = new ArrayList<>();
        for (String k : ALL) {
            out.add(k);
        }
        for (CustomCmds.Cmd c : CustomCmds.list(ctx)) {
            out.add(CustomCmds.KEY_PREFIX + c.id);
        }
        return out.toArray(new String[0]);
    }

    /** Custom[comment] key Yes[comment]Yes[comment] */
    public static boolean customAlive(Context ctx, String key) {
        return CustomCmds.find(ctx, key.substring(CustomCmds.KEY_PREFIX.length())) != null;
    }

    private QuickActions() {
    }

    /** [comment]（[comment]、[comment]Notice[comment]）。Custom[comment]「Custom：[comment]」 */
    public static String label(Context ctx, String key) {
        if (key == null) {
            return "Text";
        }
        if (KEY_NONE.equals(key)) {
            return "None（TextDefault）";
        }
        if (key.startsWith(CustomCmds.KEY_PREFIX)) {
            CustomCmds.Cmd c = CustomCmds.find(ctx, key.substring(CustomCmds.KEY_PREFIX.length()));
            // [comment]、[comment] —— [comment]，[comment]
            return c == null ? "CustomText（TextDelete）" : ("Custom：" + c.name);
        }
        switch (key) {
            case KEY_TOGGLE_FOLD:
                return "Text/Text";
            case KEY_COVER_HOME:
                return "Text";
            case KEY_COVER_ROTATE:
                return "Text 90°";
            case KEY_COVER_ROTATE_RESET:
                return "TextCancelText";
            case KEY_COVER_AUTOROT:
                return "Text";
            case KEY_ADB_WIFI:
                return "TextNoneText";
            case KEY_DUAL:
                return "Text";
            case KEY_COVER_DESK:
                return "Text";
            case KEY_SYNC:
                return "Text";
            case KEY_LOCK:
                return "Text";
            case KEY_CAST:
                return "Text";
            case KEY_OPEN_APP:
                return "Text";
            default:
                return "Text(" + key + ")";
        }
    }

    public static boolean isValidKey(Context ctx, String key) {
        if (key == null) {
            return false;
        }
        if (KEY_NONE.equals(key)) {
            return true;
        }
        if (key.startsWith(CustomCmds.KEY_PREFIX)) {
            return customAlive(ctx, key);
        }
        for (String k : ALL) {
            if (k.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * [comment]。<b>[comment]</b>（0.2 ~ 2.5 [comment]，[comment]）。
     *
     * @return null = Success；No[comment]Yes[comment]
     */
    public static String run(Context ctx, String key) {
        if (key == null) {
            return "Text";
        }
        // 「None」= [comment]。[comment]、[comment]NoticeSuccess，[comment]。
        // （[comment]：[comment]「None」[comment]「[comment]」。）
        if (KEY_NONE.equals(key)) {
            return null;
        }
        // ★ None[comment]**[comment]**[comment] Shizuku。
        //   [comment]Yes"Shizuku [comment]None[comment]"（Shizuku [comment]
        //   None[comment]），[comment] Shizuku [comment]。
        //   [comment] Shizuku：[comment] app [comment]（[comment] AdbWifi / DualScreen.openLocal）。
        if (needsShell(key) && !ShellRunner.isReady()) {
            return "Shizuku Not Ready";
        }
        Log.i(TAG, "Text: " + key);
        if (key.startsWith(CustomCmds.KEY_PREFIX)) {
            return runCustom(ctx, key);
        }
        switch (key) {
            case KEY_TOGGLE_FOLD:
                return DualScreen.toggleFold(ctx) < 0 ? "TextFailed" : null;
            case KEY_COVER_HOME:
                return coverHome(ctx);
            case KEY_COVER_ROTATE:
                return CoverDisplay.rotate(ctx) < 0 ? "TextFailed（Text）" : null;
            case KEY_COVER_ROTATE_RESET:
                return CoverDisplay.resetRotation(ctx) ? null : "TextFailed";
            case KEY_COVER_AUTOROT:
                return toggleCoverAutoRot(ctx);
            case KEY_COVER_DESK:
                return coverDesk(ctx);
            case KEY_ADB_WIFI:
                return AdbWifi.set(ctx, true);
            case KEY_DUAL:
                return DualScreen.oneKeyDual(ctx,
                        (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE))
                        ? null : "Text（Text）";
            case KEY_SYNC:
                return toggleSync(ctx);
            case KEY_LOCK:
                return toggleLock(ctx);
            case KEY_CAST:
                return openCast(ctx);
            case KEY_OPEN_APP:
                return openApp(ctx);
            default:
                return "Text: " + key;
        }
    }

    /**
     * [comment]Yes[comment]Yes[comment] shell [comment]。
     *
     * <p>[comment]Yes[comment] run() [comment]Yes[comment]「[comment] Shizuku [comment]」—— [comment] KEY_ADB_WIFI [comment]
     * （[comment]Yes「Shizuku [comment]None[comment]」，[comment] run [comment]），
     * [comment] shell [comment]，[comment]。
     */
    private static boolean needsShell(String key) {
        switch (key) {
            case KEY_ADB_WIFI:
            case KEY_OPEN_APP:
            case KEY_SYNC:
            case KEY_LOCK:
                return false;
            default:
                return true;
        }
    }

    /**
     * [comment]「[comment]」。
     *
     * <p>[comment]Yes[comment]，[comment]"[comment]Yes[comment]Yes[comment]" —— [comment]、[comment]Yes[comment]
     * [comment]，[comment]（[comment]
     * {@link #toggleSync} / {@link #toggleLock} [comment]）。
     */
    public static String toggleCoverAutoRot(Context ctx) {
        boolean want = !CoverAutoRot.on(ctx);
        String err = CoverAutoRot.set(ctx, want);
        if (err != null) {
            return err;
        }
        return want ? "Text：Text" : "Text：Text";
    }

    /**
     * [comment]「[comment]」—— [comment]、[comment]Yes[comment]
     * （[comment] MainActivity.setSyncEnabled）：[comment]「[comment]」[comment]，
     * [comment]、[comment]。
     *
     * <p>[comment]Yes null：[comment]Yes[comment]，[comment]，
     * [comment]Yes[comment]。
     */
    public static String toggleSync(Context ctx) {
        Prefs p = new Prefs(ctx);
        boolean on = !p.isEnabled();
        p.setEnabled(on);
        if (on) {
            RotationService.start(ctx);
        } else {
            if (!p.isLockPhoneRotation()) {
                RotationService.restorePhoneRotation(ctx);
            }
            if (!p.shouldKeepRunning()) {
                RotationService.stop(ctx);
            }
        }
        PanelWidgetProvider.updateAll(ctx);
        return on ? "Text：Text" : "Text：Text";
    }

    /**
     * [comment]「[comment]」—— [comment]Yes[comment]「[comment]」[comment]，
     * [comment]。
     */
    public static String toggleLock(Context ctx) {
        Prefs p = new Prefs(ctx);
        boolean on = !p.isLockPhoneRotation();
        p.setLockPhoneRotation(on);
        if (on) {
            // [comment]，[comment]
            RotationService.start(ctx);
            RotationService.lockPhoneNow(ctx);
        } else {
            RotationService.restorePhoneRotation(ctx);
            if (!p.shouldKeepRunning()) {
                RotationService.stop(ctx);
            }
        }
        PanelWidgetProvider.updateAll(ctx);
        return on ? "Text：Text（Text）" : "Text：Text";
    }

    /**
     * [comment]：[comment] → [comment] → [comment] → [comment]。
     *
     * <p>⚠ [comment]<b>[comment]Yes</b>"[comment]"[comment]。[comment]Yes[comment]：「[comment]，
     * [comment]，[comment]」—— [comment] startActivity [comment]
     * [comment]，[comment]。[comment] {@link DualScreen#oneKeyCast}，
     * [comment]「[comment]」[comment]"[comment]"[comment]。
     *
     * <p>⚠ [comment]、[comment]：[comment]，[comment]
     * [comment]，[comment]（v4.16 [comment]）。
     *
     * <p><b>[comment]</b>（[comment] 2 [comment]），[comment]。
     */
    private static String openCast(Context ctx) {
        try {
            DisplayManager dm = (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
            return DualScreen.oneKeyCast(ctx, dm) ? null : "Text（Text）";
        } catch (Throwable t) {
            return "Text";
        }
    }

    /** [comment] */
    private static String openApp(Context ctx) {
        try {
            ctx.startActivity(new Intent(ctx, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return null;
        } catch (Throwable t) {
            return "Text";
        }
    }

    /**
     * [comment]，[comment]，[comment]。
     *
     * <p>[comment]：① {@link CoverDisplay#avoidCutout} —— [comment] {@code TENT} [comment]
     * 180°，[comment] 66px（[comment] RULES §27）；
     * <b>[comment] {@code CLOSED}</b>：[comment] CLOSED [comment]，[comment] ——
     * [comment] TENT [comment]。② [comment]（[comment]；TENT [comment]，
     * [comment]）。③ [comment]<b>[comment]</b>（[comment]YesDefault[comment]）[comment] HOME，[comment] App [comment]
     * [comment] —— [comment] {@code -d <[comment] id>}：{@code input} [comment] -d Yes[comment]Default[comment]（[comment]）[comment]，
     * [comment]Yes[comment]，[comment]。
     *
     * <p>[comment]Yes[comment]，[comment] —— [comment]，
     * [comment]。
     */
    private static String coverHome(Context ctx) {
        int did = CoverDisplay.id(ctx);
        if (did <= 0) {
            return "Text";
        }
        CoverDisplay.avoidCutout(ctx);
        // [comment] toggleFold [comment]，[comment]，[comment]（[comment]，TENT [comment]）
        DualScreen.keepScreenOn(ctx, false);
        ShellRunner.run("input -d " + did + " keyevent KEYCODE_HOME");
        Log.i(TAG, "Text（Text）(display " + did + ")");
        return null;
    }

    /**
     * [comment]「[comment]」（{@link SecondaryHomeActivity}）—— [comment]「[comment]」[comment]、
     * 「[comment] / [comment]」[comment]Yes[comment]：[comment] + [comment] + [comment]，[comment]。
     *
     * <p>⛔ <b>[comment] {@link CoverLauncherActivity}</b>（v476 [comment]）：[comment]Yes「[comment]」
     * [comment]（[comment] / [comment] / [comment] / [comment] / [comment]），[comment]Yes[comment]。
     * [comment]、[comment]"[comment] / [comment]"，[comment]「[comment]」[comment]Yes[comment]。
     *
     * <p>[comment] {@link #KEY_DUAL}（[comment]）[comment]：[comment]<b>[comment]</b>，
     * [comment]。[comment]：「[comment]（[comment]）」。
     */
    private static String coverDesk(Context ctx) {
        int did = CoverDisplay.id(ctx);
        if (did <= 0) {
            return "Text";
        }
        // [comment]「[comment]」[comment]：[comment]、[comment]
        DualScreen.keepScreenOn(ctx, false);
        CoverDisplay.avoidCutout(ctx);
        if (SecondaryLauncher.launchOwn(ctx, did, SecondaryHomeActivity.class)) {
            return null;
        }
        return "Text";
    }

    /**
     * [comment]Custom[comment]。
     *
     * <p>Success[comment]No**[comment]** —— {@link ShellRunner#run} [comment]、[comment]，
     * [comment]，[comment]；[comment]Success。
     *
     * <p>[comment]Failed：[comment]，[comment]。
     */
    private static String runCustom(Context ctx, String key) {
        String id = key.substring(CustomCmds.KEY_PREFIX.length());
        CustomCmds.Cmd c = CustomCmds.find(ctx, id);
        if (c == null) {
            return "TextCustomText";
        }
        String out = ShellRunner.run(c.cmd);
        String brief = out == null ? "" : out.replace('\n', ' ').trim();
        if (brief.length() > 60) {
            brief = brief.substring(0, 60) + "…";
        }
        return brief.isEmpty() ? null : ("Text：" + brief);
    }
}
