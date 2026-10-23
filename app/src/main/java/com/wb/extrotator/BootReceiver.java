package com.wb.extrotator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * [comment]。
 * [comment]：Shizuku [comment]，[comment] shell [comment]，
 * [comment] Shizuku [comment]，[comment]。
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "ExtRotBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                return;
            }
            final Context app = context.getApplicationContext();
            Prefs p = new Prefs(app);
            if (p.shouldKeepRunning()) {
                RotationService.start(app);
            }
            // v4.12「[comment]」：[comment] / [comment]。
            // [comment]Yes「[comment]」[comment]，[comment]Yes[comment]。
            final int mode = p.getBootMode();
            if (mode != Prefs.BOOT_NONE) {
                applyBootModeAsync(app, mode, goAsync());
            }
        } catch (Throwable ignored) {
            // [comment]Deny，[comment]
        }
    }

    /**
     * [comment] Shizuku [comment]，[comment]。
     *
     * <p>⚠ [comment]，[comment]（DeviceStateManager）[comment]。
     * Shizuku [comment]None[comment]，[comment] ——
     * [comment]，[comment]。
     *
     * <p>{@code goAsync()} [comment]，[comment]。
     */
    private static void applyBootModeAsync(final Context ctx, final int mode,
                                           final PendingResult pr) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 23; i++) {          // 23 × 2s ≈ 45s
                        if (ShellRunner.isReady()) {
                            break;
                        }
                        Thread.sleep(2000L);
                    }
                    if (!ShellRunner.isReady()) {
                        Log.w(TAG, "Text：Text Shizuku，Text");
                        return;
                    }
                    int target = mode == Prefs.BOOT_FOLDED
                            ? DualScreen.CLOSED : DualScreen.OPENED;
                    DualScreen.setState(target);
                    DualScreen.keepScreenOn(ctx, mode == Prefs.BOOT_FOLDED);
                    Log.i(TAG, "Text：" + DualScreen.stateName(target));
                } catch (Throwable t) {
                    Log.w(TAG, "TextFailed: " + t);
                } finally {
                    try {
                        pr.finish();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }, "extrot-boot-mode").start();
    }
}
