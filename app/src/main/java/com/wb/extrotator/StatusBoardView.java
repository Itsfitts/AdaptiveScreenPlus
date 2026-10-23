package com.wb.extrotator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 「[comment]」[comment] —— CPU [comment] / [comment] / [comment] / [comment] / [comment]，[comment]。
 * [comment] 748×654 [comment]（[comment] BASE_W / BASE_H），[comment]，
 * [comment] scripts/mock_status.py。[comment]，[comment]。
 */
public class StatusBoardView extends View {

    // ---- [comment]（[comment] scripts/mock_status.py） ----
    private static final float BASE_W = 748f, BASE_H = 654f;
    private static final float PAD = 14f;

    // ---- [comment] ----
    private static final int BG = 0xFF0B0F14;
    private static final int PANEL = 0xFF151A21;
    private static final int TRACK = 0xFF20262F;
    private static final int GRID = 0xFF1C222A;
    private static final int MAIN = 0xFF35D6A4;      // Text：Text
    private static final int BLUE = 0xFF4C9AFF;
    private static final int WARN = 0xFFF2C14E;
    private static final int HOT = 0xFFFF6B6B;
    private static final int TXT = 0xFFE8EDF5;
    private static final int DIM = 0xFF7E8899;
    private static final int FILL = 0x2A35D6A4;      // Text

    /** [comment]（[comment]，[comment]Update） */
    public interface Tap {
        void onInfo();

        void onClose();
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path line = new Path();
    private final RectF hitInfo = new RectF();
    private final RectF hitClose = new RectF();

    private final SimpleDateFormat fmtTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat fmtDate = new SimpleDateFormat("MTextdText EEE", Locale.CHINA);

    private StatusMonitor.Snap snap;
    private Tap tap;

    private float u = 1f, ox = 0f, oy = 0f;

    public StatusBoardView(Context c) {
        this(c, null);
    }

    public StatusBoardView(Context c, AttributeSet a) {
        super(c, a);
        setClickable(true);
    }

    /** [comment] */
    public void setSnap(StatusMonitor.Snap s) {
        this.snap = s;
        invalidate();
    }

    public void setTap(Tap t) {
        this.tap = t;
    }

    // ------------------------------------------------------------------ [comment]

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (hitClose.contains(e.getX(), e.getY())) {
                if (tap != null) {
                    tap.onClose();
                }
                return true;
            }
            if (hitInfo.contains(e.getX(), e.getY())) {
                if (tap != null) {
                    tap.onInfo();
                }
                return true;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ [comment]

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        // [comment]
        u = Math.min(w / BASE_W, h / BASE_H);
        ox = (w - BASE_W * u) / 2f;
        oy = (h - BASE_H * u) / 2f;

        c.drawColor(BG);

        topBar(c);
        gauge(c);
        tempBlock(c);
        memBlock(c);
        spark(c);
        cores(c);
        footer(c);
    }

    // ------------------------------------------------------------ [comment]

    private void topBar(Canvas c) {
        float y = PAD;
        long now = System.currentTimeMillis();
        text(c, fmtTime.format(new Date(now)), PAD, y + 37, 40, TXT, Paint.Align.LEFT, true);
        text(c, fmtDate.format(new Date(now)), PAD + 186, y + 39, 24, DIM, Paint.Align.LEFT, false);

        // [comment]：× / ⓘ / [comment]。⚠ [comment] ×(U+00D7) [comment] ✕(U+2715)：[comment]，[comment]
        // × [comment] ⓘ [comment]Yes[comment]，[comment]
        float cy = y + 25;
        text(c, "×", BASE_W - PAD - 18, cy, 30, DIM, Paint.Align.CENTER, false);

        float ix = BASE_W - PAD - 74;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(S(2));
        p.setColor(DIM);
        c.drawCircle(X(ix), Y(cy), S(14), p);
        text(c, "i", ix, cy + 1, 20, DIM, Paint.Align.CENTER, true);
        // [comment] 30×28dp [comment]（[comment]，[comment]），[comment]、[comment]
        hitInfo.set(X(BASE_W - PAD - 114), Y(cy - 30), X(BASE_W - PAD - 50), Y(cy + 30));
        hitClose.set(X(BASE_W - PAD - 50), Y(cy - 30), X(BASE_W - PAD), Y(cy + 30));

        boolean on = snap != null && snap.battPct >= 0;
        String batt = on ? snap.battPct + "%" : "—";
        text(c, batt, BASE_W - PAD - 126, y + 38, 30, TXT, Paint.Align.RIGHT, true);
        if (on && snap.charging) {
            // [comment]，[comment] —— [comment] 100% [comment]
            float bx = X(BASE_W - PAD - 126) - p.measureText(batt) - S(10) - S(17);
            line.reset();
            line.moveTo(bx + S(12), Y(y + 6));
            line.lineTo(bx, Y(y + 26));
            line.lineTo(bx + S(8), Y(y + 26));
            line.lineTo(bx + S(4), Y(y + 42));
            line.lineTo(bx + S(17), Y(y + 20));
            line.lineTo(bx + S(9), Y(y + 20));
            line.close();
            p.setStyle(Paint.Style.FILL);
            p.setColor(MAIN);
            c.drawPath(line, p);
        }

        p.setStyle(Paint.Style.FILL);
        p.setColor(GRID);
        c.drawRect(X(PAD), Y(y + 50), X(BASE_W - PAD), Y(y + 51), p);
    }

    // ------------------------------------------------------------ CPU [comment]

    private void gauge(Canvas c) {
        float cx = PAD + 124, cy = 194, rad = 100;
        float pct = snap == null ? -1 : snap.cpuPct;

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(S(17));
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setColor(TRACK);
        r.set(X(cx - rad), Y(cy - rad), X(cx + rad), Y(cy + rad));
        c.drawArc(r, 150f, 240f, false, p);

        if (pct > 0) {
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(levelColor(pct));
            c.drawArc(r, 150f, 240f * Math.min(100, pct) / 100f, false, p);
        }

        String num = pct < 0 ? "—" : String.valueOf(pct);
        int col = levelColor(pct);
        // [comment]「%」[comment]，[comment]，No[comment] 100% [comment]
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(S(76));
        float nw = p.measureText(num);
        String unit = pct < 0 ? "" : "%";
        p.setTextSize(S(34));
        float uw = p.measureText(unit);
        float gap = pct < 0 ? 0f : S(6);
        float left = X(cx) - (nw + gap + uw) / 2f;

        p.setStyle(Paint.Style.FILL);
        p.setColor(TXT);
        p.setTextSize(S(76));
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText(num, left, Y(cy + 14), p);

        if (!unit.isEmpty()) {
            p.setColor(col);
            p.setTextSize(S(34));
            c.drawText(unit, left + nw + gap, Y(cy - 14), p);
        }
        text(c, "CPU Text", cx, cy + 78, 22, DIM, Paint.Align.CENTER, false);
    }

    // ------------------------------------------------------------ [comment] / [comment]

    private void tempBlock(Canvas c) {
        float rx = PAD + 270;
        float rw = BASE_W - PAD - rx;
        float t = snap == null ? Float.NaN : snap.tempC;
        int col = tempColor(t);

        if (Float.isNaN(t)) {
            text(c, "—", rx, 138, 68, DIM, Paint.Align.LEFT, true);
        } else {
            text(c, String.format(Locale.US, "%.1f", t), rx, 138, 68, col, Paint.Align.LEFT, true);
            text(c, "℃", rx + 150, 134, 34, col, Paint.Align.LEFT, false);
        }
        text(c, "CPU Text", rx, 175, 22, DIM, Paint.Align.LEFT, false);

        // [comment] 20~80℃ [comment]，[comment]
        float pct = Float.isNaN(t) ? 0 : (t - 20f) / 60f * 100f;
        bar(c, rx, 186, rx + rw, 202, pct, col);
        text(c, "20", rx, 224, 18, DIM, Paint.Align.LEFT, false);
        text(c, "80", rx + rw, 224, 18, DIM, Paint.Align.RIGHT, false);
    }

    private void memBlock(Canvas c) {
        float rx = PAD + 270;
        float rw = BASE_W - PAD - rx;
        int pct = snap == null ? -1 : snap.memPct;

        text(c, "Text", rx, 262, 24, DIM, Paint.Align.LEFT, false);
        text(c, pct < 0 ? "—" : pct + "%", rx + rw, 262, 30, TXT, Paint.Align.RIGHT, true);
        bar(c, rx, 274, rx + rw, 290, pct < 0 ? 0 : pct, BLUE);

        String detail = "—";
        if (snap != null && snap.memTotalMb > 0) {
            detail = String.format(Locale.US, "%.1f / %.1f GB",
                    snap.memUsedMb / 1024f, snap.memTotalMb / 1024f);
        }
        text(c, detail, rx, 314, 20, DIM, Paint.Align.LEFT, false);
    }

    // ------------------------------------------------------------ [comment]

    private void spark(Canvas c) {
        float x0 = PAD, y0 = 348, x1 = BASE_W - PAD, y1 = 451;
        text(c, "CPU Text · Text 60 Text", PAD, 340, 20, DIM, Paint.Align.LEFT, false);

        p.setStyle(Paint.Style.FILL);
        p.setColor(PANEL);
        r.set(X(x0), Y(y0), X(x1), Y(y1));
        c.drawRoundRect(r, S(8), S(8), p);

        p.setColor(GRID);
        p.setStrokeWidth(S(1));
        for (int i = 1; i <= 3; i++) {
            float yy = y1 - (y1 - y0) * i / 4f;
            c.drawLine(X(x0 + 10), Y(yy), X(x1 - 10), Y(yy), p);
        }

        int[] v = snap == null ? null : snap.hist;
        if (v == null || v.length < 2) {
            return;
        }
        // [comment]Yes[comment]，[comment] -1 [comment]
        int first = 0;
        while (first < v.length && v[first] < 0) {
            first++;
        }
        int n = v.length - first;
        if (n < 2) {
            return;
        }
        float px0 = 0, px1 = 0;
        line.reset();
        for (int i = 0; i < n; i++) {
            float px = x0 + 10 + (x1 - x0 - 20) * i / (n - 1f);
            float py = y1 - 8 - (y1 - y0 - 16) * Math.min(100, Math.max(0, v[first + i])) / 100f;
            if (i == 0) {
                line.moveTo(X(px), Y(py));
                px0 = px;
            } else {
                line.lineTo(X(px), Y(py));
            }
            px1 = px;
        }

        Path area = new Path(line);
        area.lineTo(X(px1), Y(y1 - 6));
        area.lineTo(X(px0), Y(y1 - 6));
        area.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(FILL);
        c.drawPath(area, p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(S(3));
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(MAIN);
        c.drawPath(line, p);
        p.setStrokeCap(Paint.Cap.BUTT);
    }

    // ------------------------------------------------------------ [comment]

    private void cores(Canvas c) {
        text(c, "Text", PAD, 484, 20, DIM, Paint.Align.LEFT, false);
        if (snap == null || snap.curKhz.length == 0) {
            text(c, "Text", PAD, 522, 22, DIM, Paint.Align.LEFT, false);
            return;
        }
        int n = Math.min(3, snap.curKhz.length);
        for (int i = 0; i < n; i++) {
            float yy = 492 + i * 37;
            int cur = snap.curKhz[i];
            int max = snap.maxKhz[i] > 0 ? snap.maxKhz[i] : cur;
            int nc = i < snap.nrCores.length ? snap.nrCores[i] : 0;
            float pct = max > 0 ? cur * 100f / max : 0;

            text(c, nc > 0 ? nc + " Text" : "Text " + i, PAD, yy + 20, 22, DIM,
                    Paint.Align.LEFT, false);
            bar(c, PAD + 100, yy + 3, PAD + 430, yy + 19, pct, pct > 60 ? MAIN : BLUE);
            text(c, String.format(Locale.US, "%.1f / %.1f GHz", cur / 1e6, max / 1e6),
                    BASE_W - PAD, yy + 20, 22, TXT, Paint.Align.RIGHT, false);
        }
    }

    // ------------------------------------------------------------ [comment]

    private void footer(Canvas c) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(GRID);
        c.drawRect(X(PAD), Y(596), X(BASE_W - PAD), Y(597), p);

        text(c, "Text", PAD, 622, 20, DIM, Paint.Align.LEFT, false);
        float hz = snap == null ? Float.NaN : snap.refreshHz;
        text(c, Float.isNaN(hz) ? "—" : String.format(Locale.US, "%.0f Hz", hz),
                PAD + 128, 622, 24, TXT, Paint.Align.LEFT, true);
        if (snap != null && snap.panelW > 0) {
            text(c, snap.panelW + " × " + snap.panelH, BASE_W - PAD, 622, 20, DIM,
                    Paint.Align.RIGHT, false);
        }
    }

    // ------------------------------------------------------------------ [comment]

    private float X(float x) {
        return ox + x * u;
    }

    private float Y(float y) {
        return oy + y * u;
    }

    private float S(float v) {
        return v * u;
    }

    /** [comment]。x / baseY [comment]Yes[comment]，baseY Yes<b>[comment]</b> */
    private void text(Canvas c, String s, float x, float baseY, float size, int color,
                      Paint.Align align, boolean bold) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        p.setTextSize(S(size));
        p.setTextAlign(align);
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        c.drawText(s, X(x), Y(baseY), p);
    }

    /** [comment]（[comment]），pct [comment] 0~100 [comment] */
    private void bar(Canvas c, float x0, float y0, float x1, float y1, float pct, int color) {
        float rad = S((y1 - y0) / 2f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(TRACK);
        r.set(X(x0), Y(y0), X(x1), Y(y1));
        c.drawRoundRect(r, rad, rad, p);

        float f = Math.max(0f, Math.min(100f, pct)) / 100f;
        float w = (x1 - x0) * f;
        if (w > 2f) {
            p.setColor(color);
            r.set(X(x0), Y(y0), X(x0 + w), Y(y1));
            c.drawRoundRect(r, rad, rad, p);
        }
    }

    /** [comment]：[comment]，[comment]Yes[comment]Yes[comment] */
    private static int levelColor(float pct) {
        if (pct >= 80) {
            return HOT;
        }
        if (pct >= 50) {
            return WARN;
        }
        return MAIN;
    }

    /** [comment]：45℃ [comment]，45~60 [comment]，60 [comment] */
    private static int tempColor(float c) {
        if (Float.isNaN(c)) {
            return DIM;
        }
        if (c >= 60) {
            return HOT;
        }
        if (c >= 45) {
            return WARN;
        }
        return MAIN;
    }
}
