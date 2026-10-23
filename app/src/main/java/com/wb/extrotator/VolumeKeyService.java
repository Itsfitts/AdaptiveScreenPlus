package com.wb.extrotator;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * 「[comment]」[comment]。
 *
 * <p>[comment]（[comment]Settings[comment]），[comment]
 * None[comment]。[comment]Yes[comment] {@code flagRequestFilterKeyEvents} —— [comment]，
 * [comment] KEYCODE_VOLUME_UP / KEYCODE_VOLUME_DOWN [comment] {@link #onKeyEvent}。
 *
 * <p><b>[comment]</b>：[comment]Yes[comment]，[comment]<b>[comment]</b>，[comment]
 * [comment] ——
 * <ul>
 *   <li>[comment] → [comment]、[comment] 700ms [comment]；</li>
 *   <li>700ms [comment]（[comment]）→ [comment]<b>[comment]</b>（{@link #stepVolume}），[comment]
 *       [comment]：[comment]、[comment]；</li>
 *   <li>[comment] 700ms → [comment]，[comment]<b>[comment]</b>。</li>
 * </ul>
 *
 * <p>⚠ [comment]：[comment]Yes「[comment]」，[comment]、[comment] 700ms [comment]
 * Yes[comment]Yes[comment]，[comment]（[comment]「[comment]」）。[comment]Yes「[comment]
 * [comment]」[comment] —— [comment]。
 *
 * <p><b>[comment]</b>：[comment]、[comment]「[comment]」[comment]，[comment]。
 * [comment] {@link QuickActions#KEY_NONE} [comment]Yes「[comment]」，[comment]<b>[comment]</b>，
 * [comment]（[comment]）。
 */
public class VolumeKeyService extends AccessibilityService {

    private static final String TAG = "VolumeKeyA11y";

    /** [comment]「[comment]」[comment] */
    private static final long LONG_PRESS_MS = 700L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    /** [comment]YesNo[comment] —— [comment]，[comment] */
    private boolean firedUp;
    private boolean firedDown;

    /** [comment]，[comment]"[comment]" */
    private static volatile VolumeKeyService sInstance;

    private final Runnable onLongPressUp = () -> {
        firedUp = true;
        run(KeyEvent.KEYCODE_VOLUME_UP, QuickPrefs.volumeUpAction(this));
    };

    private final Runnable onLongPressDown = () -> {
        firedDown = true;
        run(KeyEvent.KEYCODE_VOLUME_DOWN, QuickPrefs.volumeDownAction(this));
    };

    /** [comment]（[comment] binder / sleep，[comment]） */
    private void run(int keyCode, String actionKey) {
        Log.i(TAG, (keyCode == KeyEvent.KEYCODE_VOLUME_UP ? "Text" : "Text")
                + " → " + actionKey);
        if (QuickActions.KEY_NONE.equals(actionKey)) {
            // [comment]：[comment]「None」[comment]（onKeyEvent [comment]）
            return;
        }
        vibrate();
        final ContextRef ref = new ContextRef(getApplicationContext());
        new Thread(() -> QuickActions.run(ref.ctx, actionKey), "volume-key-action").start();
    }

    public static boolean isRunning() {
        return sInstance != null;
    }

    /**
     * [comment]「[comment]」[comment]Settings[comment]。[comment]。
     *
     * <p><b>[comment]</b>：[comment]Yes {@code TYPE_ACCESSIBILITY_OVERLAY}，
     * [comment]None[comment]，[comment] Activity [comment]。
     *
     * <p>[comment] —— [comment]，[comment] {@link #onServiceConnected()} [comment]
     * [comment]。
     */
    public static void syncCoverRecents() {
        VolumeKeyService s = sInstance;
        if (s != null) {
            CoverRecentsGesture.sync(s);
        } else {
            Log.d(TAG, "NoneText，「Text」Text");
        }
    }

    /**
     * [comment] ⇒ [comment]。
     *
     * <p>[comment] {@link #syncCoverRecents()} [comment]：sync [comment]「[comment]」[comment]，
     * [comment]。
     *
     * <p>[comment] sync [comment]，[comment] —— [comment]Yes {@code TYPE_ACCESSIBILITY_OVERLAY}，
     * Activity [comment]。
     */
    public static void refreshCoverRecents() {
        VolumeKeyService s = sInstance;
        if (s != null) {
            CoverRecentsGesture.refresh(s);
        } else {
            Log.d(TAG, "NoneText，Text");
        }
    }

    /** [comment]：[comment] / [comment] / [comment] ⇒ [comment] */
    public static void syncSidebar() {
        VolumeKeyService s = sInstance;
        if (s != null) {
            CoverSidebar.sync(s);
        } else {
            Log.d(TAG, "NoneText，Text");
        }
    }

    /** [comment] / [comment] ⇒ [comment]（[comment]） */
    public static void refreshSidebar() {
        VolumeKeyService s = sInstance;
        if (s != null) {
            CoverSidebar.refresh(s);
        } else {
            Log.d(TAG, "NoneText，Text");
        }
    }

    /**
     * [comment]：[comment] ⇒ [comment]Settings[comment]。
     *
     * <p>[comment]，[comment]（{@code TYPE_ACCESSIBILITY_OVERLAY}）。
     */
    public static void syncInnerGesture() {
        VolumeKeyService s = sInstance;
        if (s != null) {
            InnerGesture.sync(s);
        } else {
            Log.d(TAG, "NoneText，Text");
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;
        /*
         * [comment]，[comment]「[comment]」——
         * [comment] dispatchGesture（[comment] A11yInject）。
         * [comment] v3.1 [comment]（XML [comment] canPerformGestures），
         * [comment]Yes[comment]，[comment]「Settings → None[comment]」[comment]。
         */
        A11yInject.attach(this);
        /*
         * [comment]「[comment]」—— None[comment]，
         * [comment]"[comment]"[comment]（[comment] AdbWifi / AutoAuth）。
         */
        AutoAuth.attach(this);
        /*
         * [comment]「[comment]」—— [comment]。
         * [comment]（[comment]None[comment]、[comment]）[comment]，
         * [comment]。
         */
        CoverRecentsGesture.sync(this);
        CoverSidebar.sync(this);
        /*
         * [comment]「[comment]」：[comment]，[comment]。
         * [comment]（[comment]None[comment]、[comment]）[comment]，
         * [comment]。
         */
        CoverAutoRot.sync(this);
        /*
         * [comment]「[comment]」—— [comment]
         * （[comment]、[comment] / [comment]）。
         */
        InnerGesture.sync(this);
        // [comment]：[comment]，[comment]
        InnerGesture.initForeground(this);
        /*
         * [comment]「[comment]」[comment]。
         *
         * ⚠ [comment] flagRequestFilterKeyEvents、[comment] canRequestFilterKeyEvents
         * [comment]，[comment]**[comment]**[comment] —— [comment]Yes「[comment]、[comment]None[comment]、[comment]
         * [comment]」，[comment] dumpsys accessibility [comment] capabilities [comment]。[comment]
         * [comment]，[comment] dump。
         */
        boolean canFilter = false;
        boolean canGesture = false;
        int caps = 0;
        try {
            android.accessibilityservice.AccessibilityServiceInfo info = getServiceInfo();
            if (info != null) {
                caps = info.getCapabilities();
                canFilter = (caps & android.accessibilityservice.AccessibilityServiceInfo
                        .CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS) != 0;
                canGesture = (caps & android.accessibilityservice.AccessibilityServiceInfo
                        .CAPABILITY_CAN_PERFORM_GESTURES) != 0;
            }
        } catch (Throwable ignored) {
        }
        Log.i(TAG, "NoneText；Text=" + canFilter
                + " Text=" + canGesture
                + " capabilities=" + caps
                + (canFilter ? "" : " ⚠ Text canRequestFilterKeyEvents，Text")
                + (canGesture ? "" : " ⚠ Text canPerformGestures，Text input Text"));
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        final int code = event.getKeyCode();
        if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false;
        }
        // Settings[comment]，[comment]
        if (!QuickPrefs.isVolumeKeyEnabled(this)) {
            return false;
        }

        final boolean up = code == KeyEvent.KEYCODE_VOLUME_UP;
        final String actionKey = up ? QuickPrefs.volumeUpAction(this)
                : QuickPrefs.volumeDownAction(this);
        // [comment]「None」⇒ [comment]，[comment]（[comment]）
        if (QuickActions.KEY_NONE.equals(actionKey)) {
            return false;
        }

        final Runnable task = up ? onLongPressUp : onLongPressDown;
        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                if (event.getRepeatCount() == 0) {
                    // [comment]「[comment]」：
                    // [comment] = None[comment]，[comment]；
                    // [comment] = [comment]（flagRequestFilterKeyEvents [comment]）。
                    Log.d(TAG, (up ? "Text" : "Text") + "Text，Text " + LONG_PRESS_MS + "ms Text");
                    setFired(up, false);
                    handler.removeCallbacks(task);
                    handler.postDelayed(task, LONG_PRESS_MS);
                }
                /*
                 * [comment] —— [comment]。
                 * [comment]：[comment] = [comment]，
                 * [comment]"[comment]"；[comment]。
                 */
                return true;

            case KeyEvent.ACTION_UP:
                handler.removeCallbacks(task);
                boolean wasFired = isFired(up);
                setFired(up, false);
                if (!wasFired) {
                    // [comment] ⇒ [comment]Yes[comment]，[comment]（[comment]）
                    stepVolume(up);
                }
                return true;

            default:
                // [comment]，[comment]
                return true;
        }
    }

    /**
     * [comment] —— [comment]Yes[comment]。
     *
     * <p>[comment] {@code adjustSuggestedStreamVolume} [comment]Yes[comment] {@code STREAM_MUSIC}：[comment]
     * {@code USE_DEFAULT_STREAM_TYPE} Yes[comment]「[comment]」（[comment] / [comment] /
     * [comment]），[comment]。
     *
     * <p>[comment] {@code FLAG_SHOW_UI} Yes[comment] —— [comment]，[comment]
     * [comment]，[comment]。
     */
    private void stepVolume(boolean up) {
        try {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (am == null) {
                return;
            }
            am.adjustSuggestedStreamVolume(
                    up ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
                    AudioManager.USE_DEFAULT_STREAM_TYPE,
                    AudioManager.FLAG_SHOW_UI);
            Log.d(TAG, "Text" + (up ? "Text" : "Text") + " → Text");
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
        }
    }

    private boolean isFired(boolean up) {
        return up ? firedUp : firedDown;
    }

    private void setFired(boolean up, boolean v) {
        if (up) {
            firedUp = v;
        } else {
            firedDown = v;
        }
    }

    /** ([comment]) */
    private void vibrate() {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(40);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // [comment]，[comment] canRetrieveWindowContent [comment] true [comment]
        // filterKeyEvents —— [comment]。
        // v4.21 [comment]「[comment]」：None[comment]，[comment] AutoAuth。
        AutoAuth.handle(event, this);
        // [comment]：[comment]「[comment]」（[comment]）[comment] / [comment]
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence p = event.getPackageName();
            if (p != null) {
                InnerGesture.onForegroundApp(this, p.toString());
            }
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        sInstance = null;
        // [comment]（A11yInject [comment]）
        A11yInject.detach(this);
        AutoAuth.detach();
        // [comment] —— [comment]，[comment]
        CoverRecentsGesture.remove();
        CoverSidebar.remove();
        InnerGesture.remove();
        // [comment]，[comment]（[comment] sync [comment]）
        CoverAutoRot.stop();
        handler.removeCallbacks(onLongPressUp);
        handler.removeCallbacks(onLongPressDown);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        sInstance = null;
        A11yInject.detach(this);
        AutoAuth.detach();
        CoverRecentsGesture.remove();
        CoverSidebar.remove();
        InnerGesture.remove();
        CoverAutoRot.stop();
        handler.removeCallbacks(onLongPressUp);
        handler.removeCallbacks(onLongPressDown);
        super.onDestroy();
    }

    /** [comment]Yes[comment] lambda [comment] Activity [comment] */
    private static final class ContextRef {
        final Context ctx;

        ContextRef(Context c) {
            this.ctx = c;
        }
    }
}
