package com.wb.extrotator;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * [comment]「[comment]」[comment]，[comment] {@code content://} [comment]。
 *
 * <p><b>[comment]</b>：[comment]Yes RemoteViews，[comment]。
 * [comment] —— [comment]，[comment]。[comment]：
 * 748×720 [comment] ARGB [comment] 2MB，[comment] RemoteViews [comment] Binder [comment]Yes 1MB，[comment]。
 * [comment]Yes[comment]，[comment]Yes {@code content://}（{@code file://} [comment] Android 7 [comment]
 * [comment]），[comment] —— [comment]。
 * [comment] ImageView [comment] {@code setImageViewUri}，[comment] {@link CoverLauncherProvider#build}。
 *
 * <p><b>[comment] androidx [comment] FileProvider</b>：[comment] androidx（[comment] build.gradle）。
 * [comment]"[comment]"，[comment]。
 *
 * <p><b>⚠ [comment]（v4.14 [comment]）</b>
 * <ol>
 *   <li>[comment] {@code exported} <b>[comment] true</b>。[comment] false，[comment]"Update[comment]
 *       system_server [comment] RemoteViews [comment] URI [comment]" —— [comment]<b>[comment]</b>，
 *       [comment]（systemui，uid 10049）[comment] {@code not exported from UID ...} [comment]。</li>
 *   <li>{@link #openFile} <b>[comment]</b>。[comment]Yes[comment]
 *       {@code RemoteViews$BaseReflectionAction.apply} [comment] {@code setImageURI}，
 *       [comment] {@code ActionException} [comment]，
 *       [comment]<b>[comment]</b>[comment]「None[comment]」—— [comment]Yes[comment]，Yes[comment]。</li>
 * </ol>
 * [comment]Yes[comment]：{@code exported=true} [comment] exported [comment]，
 * [comment]（{@link #callerTrusted}）；
 * [comment]，{@link #openFile} [comment]<b>[comment]</b>
 * （[comment] {@link #blank}）。
 */
public class PhotoProvider extends ContentProvider {

    /** [comment] authority，[comment] */
    public static final String AUTHORITY = "com.wb.extrotator.photo";

    /** [comment]，[comment]Yes[comment]，[comment]（[comment]） */
    private static final String NAME = "bg";

    /** [comment] 1×1 [comment]（[comment]） */
    private static final String BLANK = "photo_blank.png";

    /** [comment] {@code setImageViewUri} [comment] */
    public static Uri uri() {
        return Uri.parse("content://" + AUTHORITY + "/" + NAME);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "image/png";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = callerTrusted() ? LauncherPhoto.fileWidget(getContext()) : null;
        if (f == null || !f.exists() || f.length() <= 0L) {
            /*
             * [comment]：[comment] / [comment]、[comment] /
             * [comment]。**[comment]，[comment]**（[comment]）。
             */
            f = blank();
        }
        if (f == null) {
            // [comment]（[comment]），[comment]
            throw new FileNotFoundException("Text");
        }
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    /**
     * [comment]。[comment]Yes[comment]：[comment] systemui、[comment]（[comment] launcher）、
     * [comment]。
     *
     * <p>[comment]<b>[comment]</b>，[comment]Yes[comment] —— [comment]Yes"[comment]"，
     * [comment]Yes"[comment]"（v4.14 [comment]Yes[comment]）。
     */
    private boolean callerTrusted() {
        final int uid = Binder.getCallingUid();
        if (uid == android.os.Process.myUid()) {
            return true;
        }
        final String[] pkgs = getContext().getPackageManager().getPackagesForUid(uid);
        if (pkgs == null) {
            return false;
        }
        for (String p : pkgs) {
            if ("com.android.systemui".equals(p) || "com.sec.android.app.launcher".equals(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * [comment] 1×1 [comment] PNG，[comment]，[comment]。
     * [comment]：[comment]。
     */
    private File blank() {
        File f = new File(getContext().getFilesDir(), BLANK);
        if (f.exists() && f.length() > 0L) {
            return f;
        }
        try {
            Bitmap b = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            FileOutputStream out = new FileOutputStream(f);
            try {
                b.compress(Bitmap.CompressFormat.PNG, 100, out);
            } finally {
                out.close();
                b.recycle();
            }
        } catch (Throwable t) {
            Log.w("PhotoProvider", "Text", t);
        }
        return f.exists() && f.length() > 0L ? f : null;
    }

    // [comment]，[comment] provider [comment]

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
