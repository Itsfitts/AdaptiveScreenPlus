package com.wb.extrotator;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * [comment]：[comment]（[comment] / [comment] / [comment] / [comment]）[comment]<b>[comment]</b>，
 * [comment]。
 *
 * <p><b>[comment]</b>：[comment]Yes[comment]，[comment] ——
 * [comment]，[comment]，[comment]。[comment]Add[comment]。
 *
 * <p>⚠ {@link CoverWheel} [comment] —— [comment]，
 * [comment]，[comment]。
 */
final class CoverGlyphs {

    private CoverGlyphs() {
    }

    /** [comment] */
    static String label(int act) {
        switch (act) {
            case ExtPrefs.RING_BACK:
                return "Text";
            case ExtPrefs.RING_RECENTS:
                return "Text";
            case ExtPrefs.RING_NOTIFY:
                return "Text";
            case ExtPrefs.RING_HOME:
                return "Text";
            default:
                return "Text";
        }
    }

    /**
     * [comment]。
     *
     * @param glyph [comment]（STROKE + [comment]）
     * @param dot   [comment]：[comment]，[comment] FILL
     * @param path  [comment]，[comment] reset
     * @param rect  [comment]，[comment]
     * @param dir   [comment]：+1 = [comment]，-1 = [comment]（[comment]）
     * @param s     [comment]（px）
     */
    static void draw(Canvas cv, Paint glyph, Paint dot, Path path, RectF rect,
                     int act, float x, float y, float s, float dir) {
        switch (act) {
            case ExtPrefs.RING_BACK:
                // [comment]"[comment]"[comment]，[comment]
                path.reset();
                path.moveTo(x - dir * s * 0.5f, y - s * 0.55f);
                path.lineTo(x + dir * s * 0.42f, y);
                path.lineTo(x - dir * s * 0.5f, y + s * 0.55f);
                cv.drawPath(path, glyph);
                break;
            case ExtPrefs.RING_RECENTS: {
                float w = s * 0.6f;
                float h = s * 1.0f;
                for (int k = 0; k < 3; k++) {
                    float ox = x + (k - 1) * s * 0.56f;
                    rect.set(ox - w / 2f, y - h / 2f, ox + w / 2f, y + h / 2f);
                    cv.drawRoundRect(rect, s * 0.22f, s * 0.22f, glyph);
                }
                break;
            }
            case ExtPrefs.RING_NOTIFY: {
                rect.set(x - s * 0.5f, y - s * 0.62f, x + s * 0.5f, y + s * 0.2f);
                path.reset();
                path.addArc(rect, 180f, 180f);
                path.moveTo(x - s * 0.5f, rect.centerY());
                path.lineTo(x - s * 0.5f, y + s * 0.38f);
                path.lineTo(x + s * 0.5f, y + s * 0.38f);
                path.lineTo(x + s * 0.5f, rect.centerY());
                cv.drawPath(path, glyph);
                dot.setStyle(Paint.Style.FILL);
                dot.setShader(null);
                dot.setColor(glyph.getColor());
                cv.drawCircle(x, y + s * 0.66f, s * 0.14f, dot);
                break;
            }
            case ExtPrefs.RING_HOME:
                path.reset();
                path.moveTo(x - s * 0.55f, y + s * 0.05f);
                path.lineTo(x, y - s * 0.6f);
                path.lineTo(x + s * 0.55f, y + s * 0.05f);
                cv.drawPath(path, glyph);
                rect.set(x - s * 0.34f, y + s * 0.05f, x + s * 0.34f, y + s * 0.6f);
                cv.drawRoundRect(rect, s * 0.14f, s * 0.14f, glyph);
                break;
            default:
                // [comment]：[comment]
                cv.drawCircle(x, y, s * 0.5f, glyph);
                break;
        }
    }
}
