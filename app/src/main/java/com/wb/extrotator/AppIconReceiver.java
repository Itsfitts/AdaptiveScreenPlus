package com.wb.extrotator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 「[comment] / [comment] / Update[comment]」→ [comment] {@link AppIconLib} [comment]。
 *
 * <p><b>[comment]</b>：[comment]Yes[comment]，[comment]Yes[comment]；
 * [comment]，[comment]。[comment]"[comment]"[comment]，
 * [comment]。
 *
 * <p>⚠ [comment]：[comment]Yes**[comment]**[comment]，[comment]
 * [comment]；[comment] APK [comment]，[comment]。
 *
 * <p>⚠ [comment]「[comment]」[comment] extra（{@link Intent#getData()} [comment] scheme Yes package），
 * [comment]Delete[comment] —— [comment] {@link AppIconLib#sync} [comment]，
 * [comment]"[comment]"[comment]，[comment]。
 */
public class AppIconReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String act = intent == null ? null : intent.getAction();
        if (act == null) {
            return;
        }
        if (!Intent.ACTION_PACKAGE_ADDED.equals(act)
                && !Intent.ACTION_PACKAGE_REMOVED.equals(act)
                && !Intent.ACTION_PACKAGE_REPLACED.equals(act)) {
            return;
        }
        // [comment]"[comment]/[comment]"[comment]，[comment]、[comment] partition [comment]
        if (Intent.ACTION_PACKAGE_REMOVED.equals(act)
                && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
            return;   // UpdateText REMOVED，Text ADDED，Text
        }
        AppIconLib.syncAsync(context);
        // [comment]「[comment]」[comment]，[comment]
        // （[comment]）
        CoverNotifCenter.dropAppCache();
    }
}
