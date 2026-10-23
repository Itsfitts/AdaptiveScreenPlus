package com.wb.extrotator;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * [comment]Update APK [comment] {@code content://} [comment]，[comment]。
 *
 * <p><b>[comment]</b>：[comment] root / Shizuku [comment]，Update[comment]
 * （{@code ACTION_VIEW} [comment] APK）。APK [comment]（{@code externalFilesDir}），
 * [comment] {@code file://} [comment] Android 7 [comment] —— [comment]，
 * [comment] {@link PhotoProvider} Yes[comment]。
 *
 * <p><b>[comment] androidx [comment] FileProvider</b>：[comment] androidx（[comment] build.gradle），
 * [comment] PhotoProvider [comment] —— [comment]，[comment]
 * （[comment]，[comment] URI [comment]）。
 *
 * <p>[comment]：{@code externalFilesDir/update.apk}，[comment] {@link AppUpdater#download}
 * [comment]，[comment]。
 */
public class ApkProvider extends ContentProvider {

    /** [comment] authority，[comment] */
    public static final String AUTHORITY = "com.wb.extrotator.apk";

    /** [comment]，[comment]Yes[comment]，[comment]（[comment]） */
    private static final String NAME = "update.apk";

    /** [comment] */
    public static Uri uri() {
        return Uri.parse("content://" + AUTHORITY + "/" + NAME);
    }

    /** [comment] */
    private File apkFile() {
        return new File(getContext().getExternalFilesDir(null), AppUpdater.APK_NAME);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = apkFile();
        if (!f.exists()) {
            throw new FileNotFoundException(f.getAbsolutePath());
        }
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Text");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Text");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Text");
    }
}
