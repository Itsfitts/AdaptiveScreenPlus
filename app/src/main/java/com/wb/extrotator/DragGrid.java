package com.wb.extrotator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * [comment]<b>[comment]</b>[comment] GridView。
 *
 * <p><b>[comment]，[comment]</b>：{@code startDragAndDrop()} [comment]Yes[comment]
 * （[comment]、[comment]），[comment]Yes[comment]、[comment]、alpha [comment]，[comment] DragEvent ——
 * [comment]。[comment]，[comment]：
 * <b>[comment]</b> —— [comment]，[comment] FrameLayout [comment]，[comment] margin
 * （[comment] 1:1，[comment]）；<b>[comment]</b> —— [comment]，[comment]
 * 170ms [comment]；<b>[comment]</b> —— [comment] {@link OrderListener}，
 * [comment]。
 *
 * <p><b>[comment]</b>：
 * <pre>
 *   DragGrid g = findViewById(R.id.gvPick);
 *   g.setDragHost(findViewById(R.id.gridHost));   // ⚠ [comment]Yes[comment] FrameLayout
 *   g.setOrderListener(this);
 *   g.setDropLimit(n);                            // [comment]Allow[comment]「[comment]」[comment]
 * </pre>
 *
 * <p>⚠ <b>[comment]</b>：[comment]「[comment]」，[comment] {@link HiddenSlot}
 * [comment] alpha [comment] 0 —— [comment] {@code setVisibility(INVISIBLE)}，[comment] GridView [comment]；
 * [comment] {@code getView} [comment] {@code translationX/Y} [comment] 0（[comment]Yes[comment]，
 * [comment]）。
 */
public class DragGrid extends GridView {

    /** [comment]。[comment]Yes[comment] position。 */
    public interface OrderListener {
        /**
         * [comment]：[comment] from [comment] to [comment]。
         * [comment] —— [comment] + {@code notifyDataSetChanged()}。
         */
        void onOrderMove(int from, int to);

        /** [comment]，[comment] to。[comment]、[comment]。from == to [comment]。 */
        void onOrderChanged(int from, int to);

        /** [comment] / [comment]。[comment]Notice[comment]。 */
        void onDragStateChanged(boolean dragging);

        /**
         * [comment]，[comment]<b>[comment]</b>[comment]。
         *
         * <p>[comment]"[comment]"（[comment]Yes[comment] / [comment]）。
         * ⚠ [comment]"[comment] → [comment] → [comment]"[comment]；
         * [comment]（[comment]Yes[comment]）[comment] CANCEL [comment]，[comment]。
         */
        void onHoldReleased(int position);
    }

    /** [comment]，[comment]（[comment]） */
    public interface HiddenSlot {
        void setHiddenPosition(int position);
    }

    /** [comment]"[comment]"（[comment]）。[comment] 500ms [comment]，[comment]。 */
    private static final int HOLD_MS = 240;
    /** [comment] */
    private static final int EDGE_PX = 72;
    /** [comment] / [comment] */
    private static final int SCROLL_STEP = 10;
    private static final int SCROLL_EVERY_MS = 24;

    private OrderListener listener;
    private ViewGroup dragHost;

    private boolean dragEnabled = true;
    /** [comment]Allow[comment] [0, dropLimit) [comment] —— [comment]"[comment]"[comment] */
    private int dropLimit = Integer.MAX_VALUE;

    private final Handler ui = new Handler(Looper.getMainLooper());

    private int holdPos = -1;
    private boolean dragging;
    /** [comment] —— [comment]"[comment]" */
    private boolean movedSinceHold;
    private int dragFrom = -1;
    private int dragTo = -1;
    private View dragView;
    private Bitmap dragBmp;
    private float downX, downY, lastX, lastY;
    private float grabDX, grabDY;
    private int touchSlop;
    private int scrollDir;

    /** [comment]（[comment]） */
    private boolean animPending;
    private int animFrom = -1;
    private int animTo = -1;

    private final Runnable holdTrigger = new Runnable() {
        @Override
        public void run() {
            startDrag();
        }
    };

    private final Runnable scrollTick = new Runnable() {
        @Override
        public void run() {
            if (!dragging || scrollDir == 0 || !canScrollVertically(scrollDir)) {
                return;
            }
            scrollListBy(scrollDir * SCROLL_STEP);
            hover(lastX, lastY);
            ui.postDelayed(this, SCROLL_EVERY_MS);
        }
    };

    public DragGrid(Context c) {
        super(c);
        init();
    }

    public DragGrid(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    public DragGrid(Context c, AttributeSet a, int defStyle) {
        super(c, a, defStyle);
        init();
    }

    private void init() {
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    // ------------------------------------------------------------------ [comment]

    public void setOrderListener(OrderListener l) {
        this.listener = l;
    }

    /**
     * [comment]。
     *
     * <p>⚠ [comment]Yes<b>[comment] GridView [comment] FrameLayout</b>：[comment] leftMargin/topMargin [comment]，
     * [comment] {@code GridView.getLeft() + child.getLeft()} [comment]，[comment]。
     */
    public void setDragHost(ViewGroup host) {
        this.dragHost = host;
    }

    public void setDragEnabled(boolean v) {
        this.dragEnabled = v;
    }

    /** Allow[comment]（[comment]）。[comment]"[comment]"，[comment]。 */
    public void setDropLimit(int limit) {
        this.dropLimit = limit <= 0 ? 0 : limit;
    }

    // ------------------------------------------------------------------ [comment]

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!dragEnabled || dragHost == null || getAdapter() == null) {
            return super.onTouchEvent(e);
        }

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = e.getX();
                downY = lastY = e.getY();
                holdPos = pointToPosition((int) downX, (int) downY);
                movedSinceHold = false;
                if (holdPos == INVALID_POSITION || holdPos >= dropLimit) {
                    holdPos = -1;
                } else {
                    ui.postDelayed(holdTrigger, HOLD_MS);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                lastX = e.getX();
                lastY = e.getY();
                if (dragging) {
                    hover(lastX, lastY);
                    return true;
                }
                if (holdPos >= 0
                        && (Math.abs(lastX - downX) > touchSlop
                        || Math.abs(lastY - downY) > touchSlop)) {
                    // [comment]，[comment]Yes[comment]，[comment]Yes[comment]
                    cancelHold();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (dragging) {
                    endDrag(true);
                    return true;
                }
                cancelHold();
                break;

            case MotionEvent.ACTION_CANCEL:
                // [comment] / [comment]"[comment]"，[comment]
                if (dragging) {
                    endDrag(false);
                    return true;
                }
                cancelHold();
                break;

            default:
                break;
        }
        return super.onTouchEvent(e);
    }

    private void cancelHold() {
        holdPos = -1;
        ui.removeCallbacks(holdTrigger);
    }

    // ------------------------------------------------------------------ [comment]

    private void startDrag() {
        ui.removeCallbacks(holdTrigger);
        final int pos = holdPos;
        holdPos = -1;
        if (pos < 0 || pos >= dropLimit || !isAttachedToWindow()) {
            return;
        }
        final View child = getChildAt(pos - getFirstVisiblePosition());
        if (child == null || child.getWidth() <= 0 || child.getHeight() <= 0) {
            return;
        }

        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(child.getWidth(), child.getHeight(), Bitmap.Config.ARGB_8888);
            child.draw(new Canvas(bmp));
        } catch (Throwable ignored) {
        }
        if (bmp == null) {
            return;
        }
        dragBmp = bmp;

        ImageView iv = new ImageView(getContext());
        iv.setImageBitmap(bmp);
        iv.setAlpha(0.92f);
        iv.setElevation(dp(8));
        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(child.getWidth(), child.getHeight());
        lp.leftMargin = getLeft() + child.getLeft();
        lp.topMargin = getTop() + child.getTop();
        dragHost.addView(iv, lp);
        dragView = iv;

        // [comment]，[comment] —— [comment]"[comment]"[comment]
        grabDX = downX - child.getLeft();
        grabDY = downY - child.getTop();

        dragging = true;
        dragFrom = pos;
        dragTo = pos;
        hide(pos);

        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        ViewParentCompat.requestDisallow(getParent());
        if (listener != null) {
            listener.onDragStateChanged(true);
        }
    }

    // ------------------------------------------------------------------ [comment]

    private void hover(float x, float y) {
        // 1) [comment]
        if (dragView != null && dragView.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) dragView.getLayoutParams();
            lp.leftMargin = Math.round(getLeft() + x - grabDX);
            lp.topMargin = Math.round(getTop() + y - grabDY);
            dragView.setLayoutParams(lp);
        }

        // 2) [comment]
        int pos = pointToPosition((int) x, (int) y);
        if (pos != INVALID_POSITION) {
            if (pos >= dropLimit) {
                pos = dropLimit - 1;
            }
            if (pos >= 0 && pos != dragTo) {
                final int from = dragTo;
                dragTo = pos;
                movedSinceHold = true;
                if (listener != null) {
                    listener.onOrderMove(from, pos);
                }
                hide(pos);
                animateShift(from, pos);
            }
        }

        // 3) [comment]，[comment]
        int dir = 0;
        if (y < EDGE_PX + getPaddingTop()) {
            dir = -1;
        } else if (y > getHeight() - EDGE_PX - getPaddingBottom()) {
            dir = 1;
        }
        if (dir != scrollDir) {
            scrollDir = dir;
            ui.removeCallbacks(scrollTick);
            if (dir != 0) {
                ui.postDelayed(scrollTick, SCROLL_EVERY_MS);
            }
        }
    }

    /** [comment]，[comment] */
    private void hide(int pos) {
        if (getAdapter() instanceof HiddenSlot && getAdapter() instanceof BaseAdapter) {
            ((HiddenSlot) getAdapter()).setHiddenPosition(pos);
            ((BaseAdapter) getAdapter()).notifyDataSetChanged();
        }
    }

    /**
     * [comment]。
     *
     * <p>[comment]，[comment] ——
     * [comment]"[comment]"[comment]，[comment]。
     *
     * <p>[comment]：[comment]Yes[comment]，{@code position} [comment]，
     * [comment]Yes[comment]。[comment]"[comment]"[comment]。
     */
    private void animateShift(final int from, final int to) {
        animFrom = from;
        animTo = to;
        if (animPending) {
            return;
        }
        animPending = true;
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver vto = getViewTreeObserver();
                if (vto.isAlive()) {
                    vto.removeOnPreDrawListener(this);
                }
                animPending = false;
                applyShift(animFrom, animTo);
                return true;
            }
        });
    }

    private void applyShift(int from, int to) {
        final int cols = getNumColumns();
        if (cols <= 1 || getChildCount() == 0) {
            return;
        }
        final View first = getChildAt(0);
        final int cw = first.getWidth();
        final int ch = first.getHeight();
        if (cw <= 0 || ch <= 0) {
            return;
        }
        final int hs = getHorizontalSpacing();
        final int vs = getVerticalSpacing();
        final int fv = getFirstVisiblePosition();

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            final int p = fv + i;
            int prev = p;
            if (from < to) {
                if (p >= from && p < to) {
                    prev = p + 1;
                }
            } else if (from > to) {
                if (p > to && p <= from) {
                    prev = p - 1;
                }
            }
            if (prev == p) {
                continue;
            }
            final int dr = prev / cols - p / cols;
            final int dc = prev % cols - p % cols;
            if (dr == 0 && dc == 0) {
                continue;
            }
            child.setTranslationX(dc * (cw + hs));
            child.setTranslationY(dr * (ch + vs));
            child.animate().translationX(0f).translationY(0f).setDuration(170).start();
        }
    }

    // ------------------------------------------------------------------ [comment]

    private void endDrag(boolean released) {
        ui.removeCallbacks(scrollTick);
        ui.removeCallbacks(holdTrigger);
        scrollDir = 0;

        if (dragView != null && dragView.getParent() == dragHost) {
            dragHost.removeView(dragView);
        }
        dragView = null;
        if (dragBmp != null && !dragBmp.isRecycled()) {
            dragBmp.recycle();
        }
        dragBmp = null;

        hide(-1);

        final int from = dragFrom;
        final int to = dragTo;
        dragging = false;
        dragFrom = -1;
        dragTo = -1;

        if (listener != null) {
            listener.onDragStateChanged(false);
            if (from >= 0 && to >= 0 && from != to) {
                listener.onOrderChanged(from, to);
            } else if (released && from >= 0 && !movedSinceHold) {
                listener.onHoldReleased(from);
            }
        }
    }

    // ------------------------------------------------------------------ [comment]

    private int dp(int v) {
        float d;
        try {
            d = getResources().getDisplayMetrics().density;
        } catch (Throwable t) {
            d = 2f;
        }
        return Math.round(v * d);
    }

    /** [comment]：[comment]。[comment]Yes[comment] API [comment]。 */
    private static final class ViewParentCompat {
        static void requestDisallow(android.view.ViewParent p) {
            try {
                if (p != null) {
                    p.requestDisallowInterceptTouchEvent(true);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
