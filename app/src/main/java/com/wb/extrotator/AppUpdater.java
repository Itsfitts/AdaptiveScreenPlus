package com.wb.extrotator;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 在线更新：查 GitHub Release → 下载 APK → 装。
 *
 * <p><b>为什么是 GitHub Release</b>：仓库本来就是 public 的（见 README），Release 里挂着
 * 每个版本的 APK 和更新说明，一个接口全有，不用自己搭服务器。
 * 版本号用 Release 的 tag（{@code v4.25}）跟当前 versionName 对齐 —— tag 就是发版时
 * 手打的那个号，跟 {@code versionName} 同源，不会对不上。
 *
 * <p><b>安装分两条路</b>（满足「一键」的取舍）：
 * <ul>
 *   <li>root / Shizuku 任一就绪 → {@code pm install -r} 静默装，用户零操作；</li>
 *   <li>都没有 → 把 APK 经 {@link ApkProvider} 递给系统安装器，要点几下确认
 *       （Android 8 起系统强制，绕不过，除非有特权）。</li>
 * </ul>
 *
 * <p><b>注意</b>：GitHub API 未认证限流 60 次/小时（按 IP），单人手动点绰绰有余；
 * 带 {@code User-Agent} 是硬要求，不带会被 GitHub 直接 403。
 */
public final class AppUpdater {

    private static final String TAG = "AppUpdater";

    /** latest release 元信息。tag / 说明 / APK 地址一次全拿到。 */
    private static final String API =
            "https://api.github.com/repos/PyMakesMeProud/AdaptiveScreenPlus/releases/latest";

    /** 下载下来的 APK 统一叫这个名，ApkProvider 也按这个名字找文件 */
    public static final String APK_NAME = "update.apk";

    /** 下载进度回调；{@code pct} 为 -1 表示服务端没给 Content-Length，进度未知 */
    public interface ProgressCB {
        void onProgress(int pct);
    }

    /** 一次检查的结果。 */
    public static final class Info {
        /** 远端版本号（已去掉 v 前缀），如 "4.25" */
        public final String version;
        /** Release 正文（更新说明），可能为空 */
        public final String notes;
        /** APK 下载地址（Release 附件里第一个 .apk） */
        public final String apkUrl;

        Info(String version, String notes, String apkUrl) {
            this.version = version;
            this.notes = notes;
            this.apkUrl = apkUrl;
        }
    }

    private AppUpdater() {
    }

    /**
     * 检查有没有新版本。<b>阻塞</b>，放后台线程调。
     *
     * @param current 当前 versionName，如 "4.24"
     * @return 有新版本返回 {@link Info}；已是最新返回 {@code null}
     * @throws Exception 网络或解析失败（调用方负责把 message 显示出来）
     */
    public static Info check(String current) throws Exception {
        HttpURLConnection c = open(API);
        try {
            int code = c.getResponseCode();
            if (code != 200) {
                throw new Exception("HTTP " + code);
            }
            JSONObject rel = new JSONObject(readAll(c.getInputStream(), 1 << 20));

            String remote = rel.optString("tag_name", "").trim();
            if (remote.startsWith("v") || remote.startsWith("V")) {
                remote = remote.substring(1);
            }
            if (remote.isEmpty()) {
                throw new Exception("Release 没有版本号");
            }

            // Release 附件里挑第一个 .apk —— 发版时只挂一个成品，见 scripts 的归档流程
            String apkUrl = null;
            JSONArray assets = rel.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject a = assets.optJSONObject(i);
                    if (a == null) {
                        continue;
                    }
                    String name = a.optString("name", "");
                    if (name.toLowerCase().endsWith(".apk")) {
                        apkUrl = a.optString("browser_download_url", null);
                        if (apkUrl != null) {
                            break;
                        }
                    }
                }
            }
            if (apkUrl == null) {
                throw new Exception("Release 里没有 APK");
            }

            if (!isNewer(remote, current)) {
                return null;
            }
            return new Info(remote, rel.optString("body", ""), apkUrl);
        } finally {
            c.disconnect();
        }
    }

    /**
     * 下载 APK。<b>阻塞</b>，放后台线程调。
     *
     * @param ctx      用来定位下载目录（{@code externalFilesDir}，无需存储权限）
     * @param info     {@link #check} 的返回值
     * @param progress 进度回调（跑在下载线程里，调用方自己切 UI 线程）；别在回调里做重活
     * @return 下载完的文件，直接传给 {@link #silentInstall} 或系统安装器
     */
    public static File download(Context ctx, Info info, ProgressCB progress) throws Exception {
        File out = new File(ctx.getExternalFilesDir(null), APK_NAME);
        File tmp = new File(out.getAbsolutePath() + ".part");
        if (tmp.exists()) {
            tmp.delete();
        }

        HttpURLConnection c = open(info.apkUrl);
        try {
            int code = c.getResponseCode();
            if (code != 200) {
                throw new Exception("HTTP " + code);
            }
            int total = c.getContentLength();
            int lastPct = -1;
            try (InputStream is = c.getInputStream();
                 FileOutputStream fos = new FileOutputStream(tmp)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                int done = 0;
                while ((n = is.read(buf)) > 0) {
                    fos.write(buf, 0, n);
                    done += n;
                    if (total > 0) {
                        int pct = done * 100 / total;
                        if (pct != lastPct) {
                            lastPct = pct;
                            progress.onProgress(pct);
                        }
                    }
                }
                fos.flush();
            }
            if (total <= 0) {
                progress.onProgress(-1);
            }
            if (!tmp.renameTo(out)) {
                throw new Exception("写文件失败");
            }
            return out;
        } finally {
            c.disconnect();
        }
    }

    /**
     * 静默安装（{@code pm install -r} 覆盖安装，保留数据）。
     *
     * <p>root / Shizuku 任一就绪才装得成；两者都没有返回 false，
     * 调用方应改走系统安装器（见 {@link ApkProvider}）。
     * 输出里出现 {@code Success} 才算成 —— {@code pm install} 失败时也是
     * 退出码 0 但打一堆 FAILED_...，不能只看退出码。
     */
    public static boolean silentInstall(String apkPath) {
        if (!ShellRunner.isReady()) {
            return false;
        }
        String out = ShellRunner.run("pm install -r \"" + apkPath + "\"", 180);
        Log.d(TAG, "pm install => " + out);
        return out != null && out.contains("Success");
    }

    /**
     * 按点分数字逐段比版本号。
     *
     * @return remote 比 local 新返回 true；任一段解析不了按 0 算
     */
    public static boolean isNewer(String remote, String local) {
        String[] r = remote.split("\\.");
        String[] l = local.split("\\.");
        int n = Math.max(r.length, l.length);
        for (int i = 0; i < n; i++) {
            int rv = i < r.length ? parseInt(r[i]) : 0;
            int lv = i < l.length ? parseInt(l[i]) : 0;
            if (rv != lv) {
                return rv > lv;
            }
        }
        return false;
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Throwable t) {
            return 0;
        }
    }

    private static HttpURLConnection open(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestMethod("GET");
        // GitHub 对没有 UA 的请求直接 403，这是硬要求
        c.setRequestProperty("User-Agent", "AdaptiveScreenPlus-Updater");
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        return c;
    }

    private static String readAll(InputStream is, int maxBytes) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(1 << 16);
        byte[] buf = new byte[8192];
        int n;
        int total = 0;
        while ((n = is.read(buf)) > 0) {
            bos.write(buf, 0, n);
            total += n;
            if (total >= maxBytes) {
                break;
            }
        }
        return bos.toString("UTF-8");
    }
}
