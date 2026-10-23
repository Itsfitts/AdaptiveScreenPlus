package com.wb.extrotator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * [comment]「[comment]」—— [comment] App（MacroDroid / Tasker / [comment]）[comment]：
 * <pre>
 *   action : com.wb.extrotator.action.RUN_ACTION
 *   extra  : action_key = toggle_fold | cover_home | cover_rotate | cover_rotate_reset
 * </pre>
 *
 * <p>⚠ [comment]Yes exported [comment]（No[comment]），[comment]Custom action，[comment] action_key [comment]
 * {@link QuickActions#isValidKey} [comment] —— [comment]「[comment]」[comment]。
 *
 * <p>⚠ {@code goAsync()} [comment]：[comment] shell [comment] sleep，[comment] onReceive [comment]，
 * [comment] onReceive [comment]。
 */
public class QuickActionReceiver extends BroadcastReceiver {

    private static final String TAG = "QuickActionRx";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !QuickActions.ACTION_RUN.equals(intent.getAction())) {
            return;
        }
        String key = intent.getStringExtra(QuickActions.EXTRA_ACTION);
        if (!QuickActions.isValidKey(context, key)) {
            Log.w(TAG, "Text: " + key);
            return;
        }
        final PendingResult pr = goAsync();
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                String msg = QuickActions.run(app, key);
                // [comment]「[comment]」：[comment]，[comment]
                // [comment]"[comment]"。[comment]。
                SlotWidgetProvider.noteResult(app, msg == null
                        ? app.getString(R.string.widget_slot_ran, QuickActions.label(app, key))
                        : msg);
            } catch (Throwable t) {
                Log.e(TAG, "TextFailed: " + key, t);
            } finally {
                pr.finish();
            }
        }, "quick-action-rx").start();
    }
}
