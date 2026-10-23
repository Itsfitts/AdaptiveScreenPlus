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
 * [comment]Update：[comment] GitHub Release → [comment] APK → [comment]。
 *
 * <p><b>[comment]Yes GitHub Release</b>：[comment]Yes public [comment]（[comment] README），Release [comment]
 * [comment] APK [comment]Update[comment]，[comment]，[comment]。
 * [comment] Release [comment] tag（{@code v4.25}）[comment] versionName [comment] —— tag [comment]Yes[comment]
 * [comment]，[comment] {@code versionName} [comment]，[comment]。
 *
 * <p><b>[comment]</b>（[comment]「[comment]」[comment]）：
 * <ul>
 *   <li>root / Shizuku [comment] → {@code pm install -r} [comment]，[comment]；</li>
 *   <li>[comment] → [comment] APK [comment] {@link ApkProvider} [comment]，[comment]
 *       （Android 8 [comment]，[comment]，[comment]）。</li>
 * </ul>
 *
 * <p><b>[comment]</b>：GitHub API [comment] 60 [comment]/[comment]（[comment] IP），[comment]；
 * [comment] {@code User-Agent} Yes[comment]，[comment] GitHub [comment] 403。
 */
public final class AppUpdater {

    private static final String TAG = "AppUpdater";

    /** latest release [comment]。tag / [comment] / APK [comment]。 */
    private static final String API =
            "https://api.github.com/repos/PyMakesMeProud/AdaptiveScreenPlus/releases/latest";

    /** [comment] APK [comment]，ApkProvider [comment] */
    public static final String APK_NAME = "update.apk";

    /** [comment]；{@code pct} [comment] -1 [comment] Content-Length，[comment] */
    public interface ProgressCB {
        void onProgress(int pct);
    }

    /** [comment]。 */
    public static final class Info {
        /** [comment]（[comment] v [comment]），[comment] "4.25" */
        public final String version;
        /** Release [comment]（Update[comment]），[comment] */
        public final String notes;
        /** APK [comment]（Release [comment] .apk） */
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
     * [comment]No new version。<b>[comment]</b>，[comment]。
     *
     * @param current [comment] versionName，[comment] "4.24"
     * @return [comment] {@link Info}；[comment]Yes[comment] {@code null}
     * @throws Exception [comment]Failed（[comment] message [comment]）
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
                throw new Exception("Release Text");
            }

            // Release [comment] .apk —— [comment]，[comment] scripts [comment]
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
                throw new Exception("Release Text APK");
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
     * [comment] APK。<b>[comment]</b>，[comment]。
     *
     * @param ctx      [comment]（{@code externalFilesDir}，None[comment]）
     * @param info     {@link #check} [comment]
     * @param progress [comment]（[comment]，[comment] UI [comment]）；[comment]
     * @return [comment]，[comment] {@link #silentInstall} [comment]
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
                throw new Exception("TextFailed");
            }
            return out;
        } finally {
            c.disconnect();
        }
    }

    /**
     * [comment]（{@code pm install -r} [comment]，[comment]）。
     *
     * <p>root / Shizuku [comment]；[comment] false，
     * [comment]（[comment] {@link ApkProvider}）。
     * [comment] {@code Success} [comment] —— {@code pm install} Failed[comment]Yes
     * [comment] 0 [comment] FAILED_...，[comment]。
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
     * [comment]。
     *
     * @return remote [comment] local [comment] true；[comment] 0 [comment]
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
        // GitHub [comment] UA [comment] 403，[comment]Yes[comment]
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
