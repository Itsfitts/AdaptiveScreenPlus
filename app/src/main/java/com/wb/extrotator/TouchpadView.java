package com.wb.extrotator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * [comment] —— <b>[comment]</b>，[comment]。
 *
 * <p>[comment]：[comment]，[comment]，[comment]。[comment]
 * [comment]，[comment]「[comment]」[comment] —— [comment] {@link Listener}，
 * [comment] Activity [comment]（[comment]，[comment] {@link CursorOverlay}）。
 *
 * <p><b>[comment]Yes[comment]</b>：[comment] dx/dy [comment] → [comment] dx/dy × [comment] [comment]，
 * [comment]。[comment]Yes「[comment]」—— [comment]Yes[comment]，[comment]Yes[comment]。
 *
 * <p><b>[comment]</b>：
 * <pre>
 *   [comment]        [comment]（[comment]，[comment]）
 *   [comment]        [comment]
 *   [comment]        [comment]（500ms [comment]，[comment] DOWN [comment]）
 *   [comment]      [comment] —— <b>[comment]</b>：[comment]，[comment]，[comment]
 *   [comment]      [comment]
 *   [comment]      [comment]
 * </pre>
 *
 * <p>⚠ <b>[comment]</b>：[comment]「[comment]Yes[comment]」[comment]<b>[comment]</b>[comment]，[comment]
 * {@code input swipe [comment]→[comment]} —— [comment]，[comment]
 * （[comment]「[comment]」）。[comment]：
 * <pre>
 *   [comment] → onHoldStart() → Activity [comment] DOWN
 *   [comment] → onMove()（[comment]）+ onDragMove()（[comment] MOVE，[comment]）
 *   [comment]     → onHoldEnd()   → Activity [comment] UP
 * </pre>
 * [comment]Yes MOVE [comment]（[comment] shell [comment]），[comment] ~130ms [comment]，[comment] ——
 * [comment]Yes<b>[comment]</b>[comment]，[comment]。
 *
 * <p>⚠ <b>[comment] dp [comment] slop</b>：[comment] View [comment]Yes 1496×1440（[comment] 748×720 [comment]
 * 2 [comment]），[comment]「5 [comment]」[comment] <b>1.2dp</b> —— [comment]，
 * [comment]Cancel，[comment]「[comment]」。[comment] {@link ViewConfiguration}
 * [comment]（[comment]），[comment]「Yes[comment]Yes[comment]」[comment]，[comment] Activity [comment]。
 */
public class TouchpadView extends View {

    /** [comment]。[comment]Yes[comment]，[comment] Activity [comment] */
    public interface Listener {
        /** [comment]（[comment] px），[comment]"[comment]"[comment] */
        void onMove(float dx, float dy);

        /** [comment] */
        void onTap();

        /**
         * [comment] —— [comment]<b>[comment]</b>，Activity [comment] DOWN [comment]。
         * [comment] {@link #onHoldEnd()} [comment]Yes"[comment]"[comment]。
         */
        void onHoldStart();

        /**
         * [comment]（[comment] px）。
         *
         * <p>[comment] {@link #onMove} [comment]：[comment]Yes[comment]（[comment]、[comment] shell）；
         * [comment]Yes[comment] DOWN [comment]，[comment]，
         * [comment]。[comment] —— [comment]。
         */
        void onDragMove(float dx, float dy);

        /** [comment]：[comment] / [comment]，Activity [comment] UP [comment] */
        void onHoldEnd();

        /** [comment]（[comment] = [comment]），[comment] px */
        void onScroll(float dy);

        /**
         * [comment]。
         *
         * <p>[comment]：[comment]，[comment]，
         * [comment]Yes[comment]。[comment]，[comment]。
         *
         * @param dir +1 = [comment]（[comment]），-1 = [comment]
         */
        void onPage(int dir);

        /** [comment] / [comment] —— [comment] */
        void onPress(boolean down);
    }

    /** [comment] touch slop（[comment] View [comment]）：[comment]"[comment]"[comment]Yes"[comment] / [comment]" */
    private final int slop;
    /** [comment]：[comment]（500ms），[comment] */
    private final long longPressMs;

    private Listener listener;

    /** [comment]（[comment]），[comment] Activity [comment] */
    private String infoText = "";
    /** [comment]Notice，[comment] Activity [comment] */
    private String centerHint = "";

    private boolean pressed;
    private boolean moved;
    private boolean longPressed;
    private boolean scrollActive;
    private float lastX, lastY;
    private float downX, downY;
    private float touchX, touchY;
    /** [comment]：[comment] = [comment]，[comment] = [comment]（[comment]） */
    private float scrollAcc, scrollAccX;

    private final Paint touchRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint touchDot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint infoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Runnable longPress = new Runnable() {
        @Override
        public void run() {
            if (!pressed) {
                return;
            }
            // [comment]Yes"[comment]"，[comment]Yes"[comment] MOVE"。
            // [comment] moved [comment]：[comment] slop，
            // moved [comment]，500ms [comment]No[comment]，[comment] moved=true [comment]
            // —— [comment]Yes"[comment]"，[comment]Yes[comment]"[comment]"（v3.5 [comment]）。
            if (Math.hypot(touchX - downX, touchY - downY) > slop) {
                return;   // Text：TextYesText
            }
            longPressed = true;
            // ⚠ [comment]**[comment]**[comment] Activity [comment] DOWN [comment]，[comment]。
            // [comment]Yes[comment]：[comment] → [comment]（[comment]）→ [comment]。
            if (listener != null) {
                listener.onHoldStart();
            }
            invalidate();
        }
    };

    public TouchpadView(Context c, AttributeSet a) {
        super(c, a);
        float d = getResources().getDisplayMetrics().density;
        slop = Math.max(1, ViewConfiguration.get(c).getScaledTouchSlop());
        longPressMs = ViewConfiguration.getLongPressTimeout();
        // [comment]，slop [comment]，[comment] —— [comment]。
        android.util.Log.i("ExtTouchpad", "Text density=" + d
                + " Text " + getResources().getDisplayMetrics().widthPixels
                + "×" + getResources().getDisplayMetrics().heightPixels
                + " slop=" + slop + "px Text=" + longPressMs + "ms");

        touchRing.setStyle(Paint.Style.STROKE);
        touchRing.setStrokeWidth(2f * d);
        touchRing.setColor(0x66FFFFFF);
        touchDot.setColor(0x44FFFFFF);

        infoPaint.setColor(0xFF7C8898);
        infoPaint.setTextSize(12f * d);

        hintPaint.setColor(0xFF5A6472);
        hintPaint.setTextSize(13f * d);

        edgePaint.setStyle(Paint.Style.STROKE);
        edgePaint.setStrokeWidth(1f * d);
        edgePaint.setColor(0x22FFFFFF);

        setFocusable(true);
        setClickable(true);
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    /** [comment]（[comment] / [comment]） */
    public void setInfoText(String s) {
        infoText = s == null ? "" : s;
        invalidate();
    }

    /** [comment]Notice */
    public void setCenterHint(String s) {
        centerHint = s == null ? "" : s;
        invalidate();
    }

    // ------------------------------------------------------------------ [comment]

    @Override
    protected void onDraw(Canvas cv) {
        super.onDraw(cv);

        // [comment]，[comment]Yes[comment]
        float inset = 6f;
        cv.drawRoundRect(inset, inset, getWidth() - inset, getHeight() - inset, 10f, 10f, edgePaint);

        if (pressed) {
            // [comment] —— [comment]，[comment]
            float u = getResources().getDisplayMetrics().density;
            cv.drawCircle(touchX, touchY, 34f * u, touchDot);
            cv.drawCircle(touchX, touchY, 34f * u, touchRing);
            if (longPressed) {
                cv.drawCircle(touchX, touchY, 13f * u, touchDot);
            }
        }

        if (!centerHint.isEmpty()) {
            float w = hintPaint.measureText(centerHint);
            cv.drawText(centerHint, (getWidth() - w) / 2f, getHeight() / 2f, hintPaint);
        }

        if (!infoText.isEmpty()) {
            float w = infoPaint.measureText(infoText);
            cv.drawText(infoText, getWidth() - w - 16f, getHeight() - 16f, infoPaint);
        }
    }

    // ------------------------------------------------------------------ [comment]

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = true;
                moved = false;
                longPressed = false;
                scrollActive = false;
                scrollAcc = 0f;
                scrollAccX = 0f;
                lastX = downX = touchX = e.getX();
                lastY = downY = touchY = e.getY();
                postDelayed(longPress, longPressMs);
                if (listener != null) {
                    listener.onPress(true);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (e.getPointerCount() >= 2) {
                    // [comment] = [comment]。[comment]Cancel，[comment]"[comment]"[comment]
                    removeCallbacks(longPress);
                    // [comment]，No[comment]"[comment]"
                    if (longPressed) {
                        longPressed = false;
                        if (listener != null) {
                            listener.onHoldEnd();
                        }
                    }
                    moved = true;
                    scrollActive = true;
                    scrollAcc = 0f;
                    scrollAccX = 0f;
                    lastX = e.getX(0);
                    lastY = e.getY(0);
                    touchX = e.getX(0);
                    touchY = e.getY(0);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_MOVE: {
                float x = e.getX(0), y = e.getY(0);
                touchX = x;
                touchY = y;
                if (scrollActive || e.getPointerCount() >= 2) {
                    scrollAcc += (y - lastY);
                    scrollAccX += (x - lastX);
                    lastY = y;
                    lastX = x;
                    invalidate();
                    return true;
                }
                // ⚠ [comment]**[comment]Update lastX/lastY**：[comment]，[comment]
                // [comment] onMove，[comment]。
                if (!moved && Math.hypot(x - lastX, y - lastY) < slop) {
                    return true;   // Text slop Text：Text，Text，Text
                }
                float dx = x - lastX, dy = y - lastY;
                moved = true;
                // ⚠ [comment]**[comment] removeCallbacks(longPress)**：[comment]
                // （[comment]Yes"[comment]"）。[comment] MOVE [comment]，[comment]
                // [comment] slop [comment]，[comment] 500ms [comment]。
                // [comment]Cancel —— [comment] slop，runnable [comment]。
                lastX = x;
                lastY = y;
                if (listener != null) {
                    listener.onMove(dx, dy);
                    if (longPressed) {
                        // [comment]：[comment]，[comment]
                        listener.onDragMove(dx, dy);
                    }
                }
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP:
                return true;

            case MotionEvent.ACTION_UP: {
                removeCallbacks(longPress);
                pressed = false;
                if (listener != null) {
                    listener.onPress(false);
                }
                if (scrollActive) {
                    scrollActive = false;
                    float accY = scrollAcc, accX = scrollAccX;
                    scrollAcc = 0f;
                    scrollAccX = 0f;
                    // [comment] ⇒ [comment]；No[comment]。
                    // [comment]，Yes[comment]。
                    if (Math.abs(accX) > Math.abs(accY)) {
                        if (Math.abs(accX) > slop * 2 && listener != null) {
                            // [comment] = [comment]（[comment]），[comment]
                            listener.onPage(accX < 0 ? +1 : -1);
                        }
                    } else if (Math.abs(accY) > slop && listener != null) {
                        listener.onScroll(accY);
                    }
                    invalidate();
                    return true;
                }
                // "[comment] / [comment]"[comment]。Yes[comment]Yes[comment]、[comment]，[comment]
                // DOWN + MOVE [comment]，[comment] swipe —— [comment]"[comment]"。
                if (longPressed) {
                    longPressed = false;
                    if (listener != null) {
                        listener.onHoldEnd();
                    }
                    invalidate();
                    return true;
                }
                if (!moved && listener != null) {
                    listener.onTap();
                }
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(longPress);
                pressed = false;
                if (longPressed) {
                    longPressed = false;
                    if (listener != null) {
                        listener.onHoldEnd();
                    }
                }
                scrollActive = false;
                if (listener != null) {
                    listener.onPress(false);
                }
                invalidate();
                return true;

            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(longPress);
        // ⚠ [comment]，[comment] UP [comment] ——
        // [comment]"[comment]"[comment]（[comment]）。
        if (longPressed) {
            longPressed = false;
            if (listener != null) {
                listener.onHoldEnd();
            }
        }
        super.onDetachedFromWindow();
    }
}
