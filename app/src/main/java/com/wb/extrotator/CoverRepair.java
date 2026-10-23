package com.wb.extrotator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Process;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 「[comment]」—— [comment] / [comment]「[comment]」[comment]。
 *
 * <p><b>[comment]Yes[comment]</b>：[comment]Yes「[comment]」[comment] —— [comment]
 * [comment]，[comment]<b>[comment] / [comment]</b>[comment]：[comment]、[comment]。[comment]，
 * [comment]：[comment]、[comment]、[comment]。
 *
 * <p><b>[comment]</b>：[comment] {@code Settings.Secure} [comment]，[comment] Good Lock [comment] MultiStar [comment]
 * （[comment]YesSettings[comment]「[comment]」[comment]）。[comment] key，[comment] SystemUI [comment] ——
 * [comment]Yes[comment]「[comment]」[comment] {@code CoverPolicyInjector} [comment]，[comment]：
 *
 * <table>
 *   <tr><td>{@value #K_MASTER}</td><td>{@code 1}</td></tr>
 *   <tr><td>{@value #K_REPO}</td><td>29 [comment] {@code /} [comment]，[comment] <b>25</b> [comment]</td></tr>
 *   <tr><td>{@value #K_BACKUP}</td><td>{@code pkg,userId;pkg,userId;…}</td></tr>
 *   <tr><td>{@value #K_JSON}</td><td>JSON [comment]，[comment] {@code coverWidgetList}</td></tr>
 *   <tr><td>{@value #K_POLICY}</td><td><b>[comment]</b>：{@code Base64(AES-CBC(pkg))},480,2.0;…</td></tr>
 *   <tr><td>{@value #K_ENABLED}</td><td>{@code 1}</td></tr>
 *   <tr><td>{@value #K_LARGE}（System）</td><td>1</td></tr>
 * </table>
 *
 * <p>[comment]（[comment]，[comment]）：{@code AES/CBC/PKCS5Padding}，key [comment]
 * {@code goodlock_multistar} [comment] UTF-8 [comment] 16 [comment]（[comment]Yes {@code goodlock_multist}），
 * IV = {@link #POLICY_IV}，Base64 [comment] {@code NO_WRAP}。[comment] {@code com.android.systemui}
 * [comment] {@code com.sec.android.app.sublauncher_COMPONENT_ENABLED_CHANGED}，[comment]。
 *
 * <p>⚠ <b>[comment]Yes「[comment]」，[comment]</b>：[comment] key Yes MultiStar [comment]，[comment]、
 * [comment]、[comment]（[comment]），[comment]，[comment]。
 * [comment]<b>[comment]</b>[comment] key [comment] {@link #SNAP}，「[comment]Reset」[comment]Yes[comment]
 * [comment]（{@link #restore}）。
 *
 * <p>[comment] {@link ShellRunner}（Shizuku → shell [comment]）：shell [comment] WRITE_SECURE_SETTINGS，
 * {@code settings put} [comment]。
 */
public final class CoverRepair {

    private static final String TAG = "CoverRepair";

    /**
     * [comment] shell [comment]：{@code @@key@@}。
     *
     * <p>⚠⚠ [comment] {@code <<key>>}：{@code <<} [comment] sh [comment]Yes here-doc [comment]，
     * {@code echo <<foo>>; settings get …} [comment]「[comment]」，[comment]
     * {@code syntax error: unexpected ';'} —— [comment]，[comment] key [comment]
     * （[comment]：[comment]「[comment] 0 [comment] key」）。{@code @@} [comment] sh [comment]。
     */
    private static final String MARK = "@@";

    /** [comment] MultiStar [comment]，[comment] */
    static final String K_MASTER = "multistar_master_setting";
    static final String K_REPO = "multistar_setting_repository";
    static final String K_BACKUP = "multistar_cover_widget_backup_list";
    static final String K_JSON = "multistar_setting_json_repository";
    static final String K_POLICY = "multistar_cover_widget_policy_list";
    static final String K_ENABLED = "cover_screen_apps_enabled";
    /** [comment] {@code Settings.System} [comment]（[comment] Secure） */
    static final String K_LARGE = "large_cover_screen_apps";

    /** [comment] */
    private static final int REPO_SLOT = 25;
    /** [comment] 29 [comment] */
    private static final int REPO_LEN = 29;

    /** [comment] IV（[comment]Yes[comment]） */
    private static final byte[] POLICY_IV = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 16, 17, 18, 19, 20, 21, 22};

    /** [comment] */
    private static final String SNAP = "extrot_repair";
    private static final String SNAP_TIME = "snap_time";

    private static final String[] SECURE_KEYS = {
            K_MASTER, K_REPO, K_BACKUP, K_JSON, K_POLICY, K_ENABLED};

    private CoverRepair() {
    }

    /**
     * [comment] id。
     *
     * <p>⚠ [comment] {@code UserHandle.getUserId(uid)} —— [comment]Yes {@code @SystemApi}，
     * [comment]。[comment] uid [comment]：{@code uid = userId * 100000 + appId}
     * （[comment] 0 [comment] uid [comment] 10xxxx [comment]，[comment]Yes 10xxxxx）。
     */
    public static int userId() {
        try {
            return Process.myUid() / 100000;
        } catch (Throwable t) {
            return 0;
        }
    }

    // ==================================================================== [comment]

    /** [comment] key [comment] */
    public static final class Snapshot {
        public final Map<String, String> raw = new LinkedHashMap<>();

        public String get(String key) {
            return raw.get(key);
        }

        /** [comment] key [comment]（[comment] shell [comment]，[comment]） */
        public boolean valid() {
            return raw.size() >= SECURE_KEYS.length;
        }
    }

    /** [comment] shell [comment] key [comment]（[comment] {@code <<key>>} [comment]） */
    public static Snapshot read() {
        if (!ShellRunner.isReady()) {
            Log.w(TAG, "Shizuku Not Ready，Text");
            return null;
        }
        StringBuilder cmd = new StringBuilder();
        for (String k : SECURE_KEYS) {
            cmd.append("echo ").append(mark(k)).append("; settings get secure ").append(k).append("; ");
        }
        cmd.append("echo ").append(mark(K_LARGE)).append("; settings get system ").append(K_LARGE);
        String out = ShellRunner.run(cmd.toString(), 30);
        Snapshot s = new Snapshot();
        String cur = null;
        if (out != null) {
            for (String line : out.split("\n")) {
                String t = line.replace("\r", "");
                if (t.startsWith(MARK) && t.endsWith(MARK)) {
                    cur = t.substring(MARK.length(), t.length() - MARK.length());
                    continue;
                }
                if (cur != null && !s.raw.containsKey(cur)) {
                    // [comment] key [comment]（[comment]）；unset [comment] "null"，[comment]
                    s.raw.put(cur, t);
                    cur = null;
                }
            }
        }
        // [comment] = shell [comment]（[comment]Yes[comment]），[comment]
        Log.i(TAG, "Text " + s.raw.size() + "/" + (SECURE_KEYS.length + 1) + " Text key");
        for (Map.Entry<String, String> en : s.raw.entrySet()) {
            String v = en.getValue();
            Log.d(TAG, "  " + en.getKey() + " = "
                    + (v == null ? "(null)" : v.length() + " Text"));
        }
        return s;
    }

    /** [comment] key [comment]（[comment]，[comment]） */
    public static List<String> packages(Snapshot s) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (s == null) {
            return new ArrayList<>(set);
        }
        int uid = userId();
        addWithUser(set, s.get(K_BACKUP), uid);

        String repo = s.get(K_REPO);
        if (!TextUtils.isEmpty(repo) && !"null".equals(repo)) {
            String[] f = trimSlash(repo).split("/", -1);
            if (f.length > REPO_SLOT) {
                addWithUser(set, f[REPO_SLOT], uid);
            }
        }

        String js = s.get(K_JSON);
        if (!TextUtils.isEmpty(js) && !"null".equals(js)) {
            try {
                JSONObject st = new JSONObject(js).optJSONObject("settings");
                if (st != null) {
                    addWithUser(set, st.optString("coverWidgetListIncludedUserId", ""), uid);
                    addPlain(set, st.optString("coverWidgetList", ""));
                }
            } catch (Throwable t) {
                Log.d(TAG, "json TextFailed：" + t);
            }
        }
        return new ArrayList<>(set);
    }

    public static boolean isOn(Context c, Snapshot s, String pkg) {
        return packages(s).contains(pkg);
    }

    // ==================================================================== [comment]

    /**
     * [comment] / [comment]，[comment]。
     *
     * @return [comment]（SuccessYes {@code [comment] N [comment]（[comment] N [comment]）}，Failed[comment] {@code ERR:} [comment]）
     */
    public static String apply(Context c, Collection<String> on, Collection<String> off) {
        if (!ShellRunner.isReady()) {
            return "ERR: Shizuku Not Ready";
        }
        Snapshot s = read();
        if (s == null || !s.valid()) {
            return "ERR: Text（shell Text？），Text";
        }
        List<String> list = packages(s);
        if (!off.isEmpty()) {
            list.removeAll(off);
        }
        for (String p : on) {
            if (!list.contains(p)) {
                list.add(p);
            }
        }
        Collections.sort(list);
        return write(c, s, list);
    }

    /** [comment]：[comment]（[comment] ensureAllLauncherApps） */
    public static String applyAll(Context c) {
        List<String> pkgs = launcherPackages(c);
        if (pkgs.isEmpty()) {
            return "ERR: Text";
        }
        return apply(c, pkgs, Collections.<String>emptyList()) + "（Text " + pkgs.size() + " Text）";
    }

    /** [comment]Reset：[comment] */
    public static String restore(Context c) {
        if (!hasSnapshot(c)) {
            return "ERR: Text，Text";
        }
        if (!ShellRunner.isReady()) {
            return "ERR: Shizuku Not Ready";
        }
        SharedPreferences sp = snap(c);
        StringBuilder cmd = new StringBuilder();
        for (String k : SECURE_KEYS) {
            cmd.append(put(k, sp.getString(k, "null"), false));
        }
        cmd.append(put(K_LARGE, sp.getString(K_LARGE, "null"), true));
        String out = ShellRunner.run(cmd.toString(), 30);
        notifySystemUi(c);
        Snapshot back = read();
        int n = back == null ? -1 : packages(back).size();
        Log.i(TAG, "reset → Text " + n + " Text；shell: " + oneLine(out));
        return n < 0 ? "ERR: TextFailed" : "Text（Text " + n + " Text）";
    }

    public static boolean hasSnapshot(Context c) {
        return snap(c).contains(SNAP_TIME);
    }

    /** [comment] */
    private static String write(Context c, Snapshot s, List<String> list) {
        if (!hasSnapshot(c)) {
            saveSnapshot(c, s);
        }
        int uid = userId();
        String withUser = joinWithUser(list, uid);
        String plain = join(list, ";");
        StringBuilder cmd = new StringBuilder();
        cmd.append(put(K_MASTER, "1", false));
        cmd.append(put(K_REPO, legacyRepo(s.get(K_REPO), withUser), false));
        cmd.append(put(K_BACKUP, withUser, false));
        cmd.append(put(K_JSON, jsonRepo(s.get(K_JSON), withUser, plain), false));
        cmd.append(put(K_POLICY, policyList(list), false));
        cmd.append(put(K_ENABLED, "1", false));
        cmd.append(put(K_LARGE, "1", true));

        String out = ShellRunner.run(cmd.toString(), 60);
        notifySystemUi(c);

        // [comment] —— [comment]"[comment]YesSuccess[comment]"，[comment]
        Snapshot back = read();
        int n = back == null ? -1 : packages(back).size();
        Log.i(TAG, "Text " + list.size() + " Text → Text " + n + " Text；shell: " + oneLine(out));
        if (n < 0) {
            return "ERR: TextFailed（Shizuku Text？）";
        }
        if (n != list.size()) {
            return "Text " + list.size() + " Text，Text " + n + " Text（Text，Text MultiStar Text）";
        }
        return "Text " + n + " Text";
    }

    private static void saveSnapshot(Context c, Snapshot s) {
        SharedPreferences.Editor e = snap(c).edit();
        for (Map.Entry<String, String> en : s.raw.entrySet()) {
            e.putString(en.getKey(), en.getValue());
        }
        e.putString(SNAP_TIME, String.valueOf(System.currentTimeMillis()));
        e.apply();
        Log.i(TAG, "Text key Text");
    }

    private static SharedPreferences snap(Context c) {
        return c.getApplicationContext().getSharedPreferences(SNAP, Context.MODE_PRIVATE);
    }

    /**
     * [comment]：{@code settings put ...}；[comment]
     * （[comment] / [comment] / JSON，[comment]）。
     *
     * <p>[comment] {@code null}（[comment] key）[comment] {@code settings delete} ——
     * {@code settings get} [comment]「[comment]」[comment] {@code null}，[comment]<b>[comment]
     * null [comment] key</b>，MultiStar [comment]。
     */
    private static String put(String key, String value, boolean system) {
        String ns = system ? "system " : "secure ";
        if (value == null || "null".equals(value)) {
            return "settings delete " + ns + key + "; ";
        }
        return "settings put " + ns + key + " '" + value.replace("'", "'\\''") + "'; ";
    }

    /** [comment]，[comment] {@link #MARK} */
    private static String mark(String key) {
        return MARK + key + MARK;
    }

    /** [comment] SystemUI [comment] —— [comment]，[comment] */
    private static void notifySystemUi(Context c) {
        for (String p : new String[]{"com.samsung.android.multistar", c.getPackageName()}) {
            try {
                c.sendBroadcast(new Intent("com.sec.android.app.sublauncher_COMPONENT_ENABLED_CHANGED")
                        .setPackage("com.android.systemui")
                        .putExtra("pkgName", p));
            } catch (Throwable t) {
                Log.d(TAG, "Text SystemUI Failed：" + t);
            }
        }
    }

    public static List<String> launcherPackages(Context c) {
        List<String> out = new ArrayList<>();
        for (AppRepo.Item it : AppRepo.load(c)) {
            if (it != null && !it.isPseudo() && !TextUtils.isEmpty(it.pkg)) {
                out.add(it.pkg);
            }
        }
        return out;
    }

    // ==================================================================== [comment]

    private static String legacyRepo(String raw, String withUser) {
        String t = trimSlash(raw == null ? "" : raw);
        String[] f = (TextUtils.isEmpty(t) || "null".equals(t)) ? new String[0] : t.split("/", -1);
        String[] out = f;
        if (out.length < REPO_LEN) {
            out = new String[REPO_LEN];
            System.arraycopy(f, 0, out, 0, f.length);
        }
        for (int i = 0; i < out.length; i++) {
            if (TextUtils.isEmpty(out[i])) {
                out[i] = "0";
            }
        }
        out[REPO_SLOT] = withUser;
        StringBuilder b = new StringBuilder();
        for (String p : out) {
            b.append(p).append('/');
        }
        return b.toString();
    }

    private static String jsonRepo(String raw, String withUser, String plain) {
        try {
            JSONObject root = (TextUtils.isEmpty(raw) || "null".equals(raw))
                    ? new JSONObject() : new JSONObject(raw);
            JSONObject st = root.optJSONObject("settings");
            if (st == null) {
                st = new JSONObject();
            }
            st.put("coverWidgetListIncludedUserId", withUser);
            st.put("coverWidgetList", plain);
            st.put("coverWidgetFwBugFix", true);
            if (!root.has("version")) {
                root.put("version", 1);
            }
            root.put("settings", st);
            return root.toString();
        } catch (Throwable t) {
            Log.w(TAG, "Text json Text，Text", t);
            return "{\"version\":1,\"settings\":{\"coverWidgetListIncludedUserId\":\""
                    + withUser.replace("\"", "\\\"") + "\",\"coverWidgetList\":\""
                    + plain.replace("\"", "\\\"") + "\",\"coverWidgetFwBugFix\":true}}";
        }
    }

    /** [comment]。[comment]，[comment] */
    private static String policyList(List<String> list) {
        StringBuilder b = new StringBuilder();
        for (String p : list) {
            String e = encrypt(p);
            if (TextUtils.isEmpty(e)) {
                continue;
            }
            if (b.length() > 0) {
                b.append(';');
            }
            b.append(e).append(",480,2.0");
        }
        return b.toString();
    }

    static String encrypt(String pkg) {
        try {
            byte[] key = Arrays.copyOf("goodlock_multistar".getBytes("UTF-8"), 16);
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(POLICY_IV));
            byte[] out = c.doFinal(pkg.getBytes("UTF-8"));
            return Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Throwable t) {
            Log.w(TAG, "TextFailed：" + pkg, t);
            return "";
        }
    }

    private static String joinWithUser(List<String> list, int uid) {
        StringBuilder b = new StringBuilder();
        for (String p : list) {
            if (b.length() > 0) {
                b.append(';');
            }
            b.append(p).append(',').append(uid);
        }
        return b.toString();
    }

    static String join(List<String> list, String sep) {
        StringBuilder b = new StringBuilder();
        for (String p : list) {
            if (b.length() > 0) {
                b.append(sep);
            }
            b.append(p);
        }
        return b.toString();
    }

    private static String trimSlash(String s) {
        String t = s;
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    /** {@code pkg,uid;pkg,uid} [comment]，[comment] */
    private static void addWithUser(Collection<String> out, String v, int uid) {
        if (TextUtils.isEmpty(v) || "null".equals(v)) {
            return;
        }
        String suffix = "," + uid;
        for (String item : v.split(";")) {
            if (TextUtils.isEmpty(item)) {
                continue;
            }
            int i = item.indexOf(',');
            if (i < 0) {
                if (item.endsWith(suffix)) {
                    item = item.substring(0, item.length() - suffix.length());
                }
            } else {
                String u = item.substring(i + 1);
                if (TextUtils.isEmpty(u) || String.valueOf(uid).equals(u)) {
                    item = item.substring(0, i);
                } else {
                    continue;   // Text（Text）Text，Text
                }
            }
            valid(out, item);
        }
    }

    /** {@code pkg;pkg} [comment] */
    private static void addPlain(Collection<String> out, String v) {
        if (TextUtils.isEmpty(v) || "null".equals(v)) {
            return;
        }
        for (String item : v.split(";")) {
            valid(out, item);
        }
    }

    private static void valid(Collection<String> out, String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        String p = pkg.trim();
        if ("backup".equals(p) || p.indexOf('.') <= 0) {
            return;
        }
        out.add(p);
    }

    private static String oneLine(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim().replace("\n", " | ");
        return t.length() > 200 ? t.substring(0, 200) + "…" : t;
    }
}
