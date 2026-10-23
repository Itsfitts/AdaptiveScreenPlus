package com.wb.extrotator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Settings[comment]。
 * [comment] Android Surface.ROTATION_* [comment]：0=0°, 1=90°, 2=180°, 3=270°。
 */
public final class Prefs {

    public static final String NAME = "extrot";

    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return sp.getBoolean("enabled", false);
    }

    public void setEnabled(boolean v) {
        sp.edit().putBoolean("enabled", v).apply();
    }

    /** [comment]（Default 90°） */
    public int getPortraitAngle() {
        return sp.getInt("portrait_angle", 1);
    }

    public void setPortraitAngle(int v) {
        sp.edit().putInt("portrait_angle", v).apply();
    }

    /** [comment]（Default 0°） */
    public int getLandscapeAngle() {
        return sp.getInt("landscape_angle", 0);
    }

    public void setLandscapeAngle(int v) {
        sp.edit().putInt("landscape_angle", v).apply();
    }

    /**
     * YesNo[comment] wm fixed-to-user-rotation，[comment]。
     *
     * <p>⚠ Default「[comment]」（v4.26 [comment]）：[comment]<b>[comment]</b>[comment]，
     * [comment]Yes「[comment]」。[comment]。
     */
    public boolean isFixToUserRotation() {
        return sp.getBoolean("fix_to_user", false);
    }

    public void setFixToUserRotation(boolean v) {
        sp.edit().putBoolean("fix_to_user", v).apply();
    }

    // ---------------------------------------------------------------- [comment]

    /**
     * [comment]，[comment]「[comment]」[comment]。
     * [comment]：[comment]，[comment]，
     * [comment]。
     */
    public boolean isLockPhoneRotation() {
        return sp.getBoolean("lock_phone_rotation", true);
    }

    public void setLockPhoneRotation(boolean v) {
        sp.edit().putBoolean("lock_phone_rotation", v).apply();
    }

    /** [comment]（1 = 90° = [comment]，2 = 180°，3 = 270°，0 = 0°）。Default 1（[comment]）。 */
    public int getPhoneRotation() {
        return sp.getInt("phone_rotation", 1);
    }

    public void setPhoneRotation(int v) {
        sp.edit().putInt("phone_rotation", v).apply();
    }

    // ---------------------------------------------------------------- [comment]（[comment] SecondScreen）

    /**
     * [comment]：[comment]，YesNo[comment]。
     * [comment]「[comment]」[comment] —— [comment]，[comment]。
     */
    public boolean isResolutionEnabled() {
        return sp.getBoolean("res_enabled", false);
    }

    public void setResolutionEnabled(boolean v) {
        sp.edit().putBoolean("res_enabled", v).apply();
    }

    public int getResWidth() {
        return sp.getInt("res_w", 1080);
    }

    public int getResHeight() {
        return sp.getInt("res_h", 1920);
    }

    public void setResSize(int w, int h) {
        sp.edit().putInt("res_w", w).putInt("res_h", h).apply();
    }

    /** [comment] DPI；0 = [comment]（Default） */
    public int getResDensity() {
        return sp.getInt("res_density", 0);
    }

    public void setResDensity(int v) {
        sp.edit().putInt("res_density", v).apply();
    }

    /** [comment]（Default[comment]） */
    public boolean isAskOnConnect() {
        return sp.getBoolean("res_ask", true);
    }

    public void setAskOnConnect(boolean v) {
        sp.edit().putBoolean("res_ask", v).apply();
    }

    /** [comment]（Default[comment]） */
    public boolean isAutoResetSize() {
        return sp.getBoolean("res_auto_reset", true);
    }

    public void setAutoResetSize(boolean v) {
        sp.edit().putBoolean("res_auto_reset", v).apply();
    }

    /**
     * [comment]，[comment]（Default[comment]）。
     * <p>[comment]Yes[comment]（1920×1080），[comment] 1080×1920 [comment]，
     * [comment]、[comment]。
     */
    public boolean isAutoMatchExt() {
        return sp.getBoolean("res_auto_match", true);
    }

    public void setAutoMatchExt(boolean v) {
        sp.edit().putBoolean("res_auto_match", v).apply();
    }

    /** [comment]YesNo[comment]（[comment]） */
    public boolean isSizeApplied() {
        return sp.getBoolean("res_applied", false);
    }

    public void setSizeApplied(boolean v) {
        sp.edit().putBoolean("res_applied", v).apply();
    }

    /**
     * [comment]Yes[comment]Yes「[comment]」[comment]。
     * <p>true  → [comment] / [comment]，[comment]（[comment]Yes[comment]）
     * <p>false → [comment]「[comment]」[comment]，[comment]None[comment]，[comment]
     */
    public boolean isResBoundToExt() {
        return sp.getBoolean("res_bound", false);
    }

    public void setResBoundToExt(boolean v) {
        sp.edit().putBoolean("res_bound", v).apply();
    }

    /** DPI [comment]YesNo[comment]（[comment] reset，[comment]Settings） */
    public boolean isDensityApplied() {
        return sp.getBoolean("res_density_applied", false);
    }

    public void setDensityApplied(boolean v) {
        sp.edit().putBoolean("res_density_applied", v).apply();
    }

    /**
     * [comment]YesNo[comment]。
     *
     * <p>[comment]，[comment]：
     * [comment]、[comment]、[comment]<b>[comment]</b>。
     * <p>[comment]Yes[comment] —— [comment]，[comment]"[comment]、
     * [comment]"[comment]（[comment]）。
     */
    public boolean shouldKeepRunning() {
        return isEnabled() || isResolutionEnabled() || isLockPhoneRotation();
    }

    // ---------------------------------------------------------------- [comment]

    /** [comment]，[comment]（Default） */
    public static final int BOOT_NONE = 0;
    /** [comment]，[comment] */
    public static final int BOOT_FOLDED = 1;
    /** [comment]，[comment] */
    public static final int BOOT_UNFOLDED = 2;

    /**
     * [comment]。
     *
     * <p>[comment]「[comment]」[comment]Yes[comment] {@code cmd device_state state N}，
     * [comment]Yes[comment]"[comment]"[comment]"[comment]"。[comment] Shizuku [comment] ——
     * [comment]None[comment] Shizuku [comment]，[comment]。
     */
    public int getBootMode() {
        return sp.getInt("boot_mode", BOOT_NONE);
    }

    public void setBootMode(int v) {
        sp.edit().putInt("boot_mode", v).apply();
    }

    /** [comment]（0=[comment]，1=[comment]，2=[comment]） */
    public int getLastTab() {
        return sp.getInt("last_tab", 0);
    }

    public void setLastTab(int v) {
        sp.edit().putInt("last_tab", v).apply();
    }
}
