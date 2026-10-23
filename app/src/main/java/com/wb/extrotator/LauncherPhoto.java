package com.wb.extrotator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 「[comment]」[comment]：[comment]、[comment]、[comment]（[comment]）[comment]。
 *
 * <p><b>[comment]，[comment]Yes[comment]</b>：
 * <ul>
 *   <li>{@code launcher_bg} —— [comment] {@link #MAX_EDGE} [comment]（JPEG），
 *       [comment]。[comment]Yes[comment] Activity，[comment]。</li>
 *   <li>{@code launcher_bg_widget} —— [comment]、<b>[comment]</b>[comment] PNG，
 *       [comment] ImageView。[comment]：[comment] RemoteViews [comment]，
 *       [comment]，[comment]；[comment]Yes 20dp、[comment] shape [comment]，
 *       [comment]，[comment]。</li>
 * </ul>
 *
 * <p><b>[comment]Yes"[comment]"[comment]Yes"[comment]"</b>：[comment]，[comment]（{@link LauncherPrefs#bgDrawable}）
 * [comment]，[comment]Yes[comment]"[comment]"。[comment]「[comment]」[comment]「[comment]」
 * Yes[comment]，[comment] —— [comment]，[comment]Yes[comment]。
 *
 * <p>⚠ {@link #WIDGET_W} / {@link #WIDGET_H} Yes<b>[comment]</b>（748×720，[comment] RULES [comment]），
 * [comment]Yes dp。[comment]，No[comment]。
 *
 * <p>⚠ {@link #save} [comment]，[comment]。
 */
public final class LauncherPhoto {

    private static final String TAG = "LauncherPhoto";

    /** [comment]（JPEG） */
    private static final String NAME_FULL = "launcher_bg";
    /** [comment]（PNG，[comment]） */
    private static final String NAME_WIDGET = "launcher_bg_widget";

    /**
     * [comment]。[comment] 748px [comment]，1520 [comment]，
     * [comment] 1200 [comment]。
     */
    private static final int MAX_EDGE = 1520;

    /** [comment] = [comment]（748×720） */
    private static final int WIDGET_W = 748;
    private static final int WIDGET_H = 720;

    /**
     * [comment]（[comment]）。
     * [comment] {@code launcher_widget_bg_*.xml} [comment] {@code corners radius="20dp"} [comment]：
     * 20dp × (340/160) ≈ 43px（[comment] 340dpi）。
     */
    private static final float WIDGET_RADIUS = 43f;

    /** [comment] —— [comment]、[comment]，[comment] */
    private static Bitmap sCache;
    private static long sCacheStamp = -1L;

    private LauncherPhoto() {
    }

    // ------------------------------------------------------------------ [comment]

    /** [comment]（[comment]） */
    static File fileFull(Context c) {
        return new File(c.getFilesDir(), NAME_FULL);
    }

    /** [comment]（[comment] PNG） */
    static File fileWidget(Context c) {
        return new File(c.getFilesDir(), NAME_WIDGET);
    }

    /**
     * [comment] —— <b>[comment]</b>，[comment]。
     * [comment]Failed、[comment]，[comment]。
     */
    public static boolean has(Context c) {
        File f = fileWidget(c);
        return f.exists() && f.length() > 0L;
    }

    /** [comment]，[comment]。[comment]，[comment]。 */
    public static void clear(Context c) {
        //noinspection ResultOfMethodCallIgnored
        fileFull(c).delete();
        //noinspection ResultOfMethodCallIgnored
        fileWidget(c).delete();
        synchronized (LauncherPhoto.class) {
            if (sCache != null && !sCache.isRecycled()) {
                sCache.recycle();
            }
            sCache = null;
            sCacheStamp = -1L;
        }
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]。⚠ [comment]。
     *
     * <p>[comment] {@code .tmp}、[comment]。[comment]Failed[comment] ——
     * [comment]。
     *
     * @return [comment]
     */
    public static boolean save(Context c, Uri src) {
        File tmpFull = new File(c.getFilesDir(), NAME_FULL + ".tmp");
        File tmpWidget = new File(c.getFilesDir(), NAME_WIDGET + ".tmp");
        Bitmap full = null;
        Bitmap round = null;
        try {
            full = decode(c, src, MAX_EDGE);
            if (full == null) {
                Log.w(TAG, "Text：" + src);
                return false;
            }

            OutputStream o1 = new FileOutputStream(tmpFull);
            try {
                full.compress(Bitmap.CompressFormat.JPEG, 90, o1);
            } finally {
                o1.close();
            }

            round = rounded(full);
            OutputStream o2 = new FileOutputStream(tmpWidget);
            try {
                round.compress(Bitmap.CompressFormat.PNG, 100, o2);
            } finally {
                o2.close();
            }

            if (!tmpFull.renameTo(fileFull(c)) || !tmpWidget.renameTo(fileWidget(c))) {
                Log.w(TAG, "TextFailed，Text");
                return false;
            }
            Log.i(TAG, "Text：full=" + fileFull(c).length()
                    + "B / widget=" + fileWidget(c).length() + "B");
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed", t);
            return false;
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmpFull.delete();
            //noinspection ResultOfMethodCallIgnored
            tmpWidget.delete();
            if (round != null) {
                round.recycle();
            }
            if (full != null) {
                full.recycle();
            }
        }
    }

    /**
     * [comment] {@code maxEdge} [comment]。
     *
     * <p>[comment] {@link ImageDecoder} [comment]Yes {@code BitmapFactory}：<b>[comment] EXIF [comment]</b>
     * （[comment]，BitmapFactory [comment]Yes[comment]），[comment]，
     * [comment] inSampleSize。
     *
     * <p>[comment]：Default[comment]，[comment]、[comment]。
     */
    private static Bitmap decode(Context c, Uri src, int maxEdge) throws IOException {
        ImageDecoder.Source source = ImageDecoder.createSource(c.getContentResolver(), src);
        return ImageDecoder.decodeBitmap(source, (decoder, info, s) -> {
            int w = info.getSize().getWidth();
            int h = info.getSize().getHeight();
            int longEdge = Math.max(w, h);
            float k = longEdge > maxEdge ? maxEdge / (float) longEdge : 1f;
            decoder.setTargetSize(Math.max(1, Math.round(w * k)),
                    Math.max(1, Math.round(h * k)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
    }

    /** [comment] + [comment] —— [comment]Yes[comment] */
    private static Bitmap rounded(Bitmap src) {
        Bitmap cut = crop(src, WIDGET_W, WIDGET_H);
        Bitmap out = Bitmap.createBitmap(WIDGET_W, WIDGET_H, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p.setShader(new BitmapShader(cut, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float r = Math.min(WIDGET_RADIUS, Math.min(WIDGET_W, WIDGET_H) / 2f);
        cv.drawRoundRect(new RectF(0f, 0f, WIDGET_W, WIDGET_H), r, r, p);
        cut.recycle();
        return out;
    }

    /** [comment] + [comment]，[comment] ImageView [comment] centerCrop [comment] */
    private static Bitmap crop(Bitmap src, int w, int h) {
        float k = Math.max(w / (float) src.getWidth(), h / (float) src.getHeight());
        int sw = Math.min(src.getWidth(), Math.max(1, Math.round(w / k)));
        int sh = Math.min(src.getHeight(), Math.max(1, Math.round(h / k)));
        int x = Math.max(0, (src.getWidth() - sw) / 2);
        int y = Math.max(0, (src.getHeight() - sh) / 2);
        Bitmap cut = Bitmap.createBitmap(src, x, y, sw, sh);
        if (sw == w && sh == h) {
            return cut;
        }
        Bitmap out = Bitmap.createScaledBitmap(cut, w, h, true);
        if (out != cut) {
            cut.recycle();
        }
        return out;
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]：[comment]、[comment] —— [comment]。
     *
     * <p>[comment] {@code null}，[comment]，[comment]。
     *
     * <p>⚠ [comment]，[comment]。
     *
     * @param alphaPct [comment]Yes「[comment]」[comment]（0/10/30/50）
     */
    public static Drawable pageBackdrop(Context c, int alphaPct) {
        File f = fileFull(c);
        if (!f.exists()) {
            return null;
        }
        Bitmap bmp;
        synchronized (LauncherPhoto.class) {
            long stamp = f.lastModified();
            if (sCache == null || sCache.isRecycled() || sCacheStamp != stamp) {
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (b == null) {
                    return null;
                }
                if (sCache != null && !sCache.isRecycled()) {
                    sCache.recycle();
                }
                sCache = b;
                sCacheStamp = stamp;
            }
            bmp = sCache;
        }
        int a = Math.max(0, Math.min(100, alphaPct)) * 255 / 100;
        // [comment]（#101014），[comment] alpha —— [comment]
        ColorDrawable veil = new ColorDrawable((a << 24) | 0x00101014);
        return new LayerDrawable(new Drawable[]{new Cover(bmp), veil});
    }

    /**
     * [comment] centerCrop [comment]。
     *
     * <p>[comment] BitmapDrawable [comment] {@code setGravity(FILL)}：[comment]。
     * [comment]"[comment]"[comment]，[comment]。
     */
    static final class Cover extends Drawable {

        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Bitmap bmp;

        Cover(Bitmap bmp) {
            this.bmp = bmp;
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            if (b.isEmpty() || bmp.isRecycled() || bmp.getWidth() <= 0 || bmp.getHeight() <= 0) {
                return;
            }
            float k = Math.max(b.width() / (float) bmp.getWidth(),
                    b.height() / (float) bmp.getHeight());
            float w = bmp.getWidth() * k;
            float h = bmp.getHeight() * k;
            float l = b.left + (b.width() - w) / 2f;
            float t = b.top + (b.height() - h) / 2f;
            canvas.drawBitmap(bmp, null, new RectF(l, t, l + w, t + h), paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            paint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
