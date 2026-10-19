package com.wb.extrotator;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * 把下载好的更新 APK 暴露成一个只读的 {@code content://} 地址，给系统安装器读。
 *
 * <p><b>为什么需要它</b>：没有 root / Shizuku 时，更新只能走系统安装器
 * （{@code ACTION_VIEW} 装 APK）。APK 躺在应用私有目录（{@code externalFilesDir}），
 * 而 {@code file://} 从 Android 7 起不许跨进程递 —— 所以要这么一层出口，
 * 和 {@link PhotoProvider} 是同一个理由。
 *
 * <p><b>为什么不用 androidx 的 FileProvider</b>：本工程不带 androidx（见 build.gradle），
 * 而且这里比 PhotoProvider 还简单 —— 只读一个固定文件，连白名单都不用做
 * （会来读的只有系统包安装器，调用时还带一次性 URI 授权）。
 *
 * <p>文件位置：{@code externalFilesDir/update.apk}，由 {@link AppUpdater#download}
 * 下载落地，两边必须一致。
 */
public class ApkProvider extends ContentProvider {

    /** 清单里注册的 authority，两处必须一致 */
    public static final String AUTHORITY = "com.wb.extrotator.apk";

    /** 路径段，只是个名字，不参与寻址（就一个文件） */
    private static final String NAME = "update.apk";

    /** 递给系统安装器的地址 */
    public static Uri uri() {
        return Uri.parse("content://" + AUTHORITY + "/" + NAME);
    }

    /** 下载落地处的那个文件 */
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
        throw new UnsupportedOperationException("只读");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("只读");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("只读");
    }
}
