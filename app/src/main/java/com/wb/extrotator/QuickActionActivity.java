package com.wb.extrotator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * [comment]「[comment]」—— [comment] Activity：[comment]，[comment] intent [comment]、
 * [comment] toast、[comment]。
 *
 * <p>[comment] Activity [comment]Yes[comment]：[comment]「[comment] → [comment]」[comment]<b>[comment]</b>
 * （[comment] {@code settings get secure assistant}），[comment]Yes Activity；[comment]
 * {@link QuickActionReceiver}。
 *
 * <p>[comment]：<b>[comment]</b> —— [comment]「Settings → [comment] → [comment]」[comment]「[comment]
 * [comment]」[comment]（[comment] {@code ACTION_ASSIST}）；<b>[comment] / MacroDroid / Tasker</b> ——
 * [comment]Custom action {@code com.wb.extrotator.action.RUN_ACTION}、extra [comment] {@code action_key}；
 * <b>[comment]</b> —— [comment] {@code res/xml/quick_actions.xml}，[comment] / [comment]
 * 「[comment]」[comment]。
 *
 * <p>⚠ [comment]：[comment] onCreate [comment]。[comment]/[comment]，[comment] Activity [comment]，
 * [comment]（[comment]，[comment] RULES §9）。
 */
public class QuickActionActivity extends Activity {

    private static final String TAG = "QuickAction";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ① [comment]：[comment]Yes[comment]，[comment]Yes[comment]
        if (savedInstanceState != null) {
            Log.i(TAG, "onCreate Text，Text（Text）");
            finish();
            return;
        }

        final String key = resolveKey(getIntent());
        if (key == null) {
            Log.w(TAG, "Text，Text");
            finish();
            return;
        }

        // ② [comment]：[comment] sleep [comment] binder [comment]
        new Thread(() -> {
            final String err = QuickActions.run(getApplicationContext(), key);
            runOnUiThread(() -> {
                String text = (err == null)
                        ? QuickActions.label(this, key) + " Text"
                        : QuickActions.label(this, key) + "：" + err;
                try {
                    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {
                }
                finish();
            });
        }, "quick-action").start();
    }

    /**
     * [comment] intent [comment]。
     * [comment] {@code action_key} [comment]：
     *  - [comment]「[comment]」[comment]（ACTION_ASSIST / VOICE_ASSIST）→ [comment]Settings[comment]Default[comment]
     *  - [comment] → null，[comment]，[comment]
     */
    private String resolveKey(Intent it) {
        if (it == null) {
            return null;
        }
        String k = it.getStringExtra(QuickActions.EXTRA_ACTION);
        if (QuickActions.isValidKey(this, k)) {
            return k;
        }
        String action = it.getAction();
        if (Intent.ACTION_ASSIST.equals(action)
                || "android.intent.action.VOICE_ASSIST".equals(action)) {
            return QuickPrefs.assistAction(this);
        }
        return null;
    }
}
