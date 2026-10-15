package com.wb.extrotator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * On机自启。
 * 注意: Shizuku 本身在重启后需要重新启动服务，所以On机能起服务但可能拿不到 shell Permission，
 * 一旦用户在 Shizuku 里重新启动服务，本应用的点On即会重新生效。
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
            // v4.12「On机选项」: Off机重On之后自动进外Screen / Internal ScreenHome。
            // 用的还是「Fold Mode」那套命令，只是时机挪到On机后。
            final int mode = p.getBootMode();
            if (mode != Prefs.BOOT_NONE) {
                applyBootModeAsync(app, mode, goAsync());
            }
        } catch (Throwable ignored) {
            // Recents启动前台服务系统可能拒绝，忽略即可
        }
    }

    /**
     * 等 Shizuku 就绪，再把折叠状态改过去。
     *
     * <p>⚠ On机头一分钟系统还在忙，折叠状态机 (DeviceStateManager) 未必收得下命令。
     * Shizuku 走无线调试启动的话重启后要手动拉一次，等不到就安静跳过 ——
     * 界面照旧显示用户选的那档，不会自己改掉。
     *
     * <p>{@code goAsync()} 只撑住广播的存活时间，免得线程跑完前进程被回收。
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
                        Log.w(TAG, "On机选项: 等不到 Shizuku，跳过这一趟");
                        return;
                    }
                    int target = mode == Prefs.BOOT_FOLDED
                            ? DualScreen.CLOSED : DualScreen.OPENED;
                    DualScreen.setState(target);
                    DualScreen.keepScreenOn(ctx, mode == Prefs.BOOT_FOLDED);
                    Log.i(TAG, "On机选项已应用: " + DualScreen.stateName(target));
                } catch (Throwable t) {
                    Log.w(TAG, "On机选项Failed: " + t);
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
