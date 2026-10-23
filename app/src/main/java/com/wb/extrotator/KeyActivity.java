package com.wb.extrotator;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * [comment] —— [comment]，[comment]。
 *
 * <p>[comment]「[comment] + [comment]」[comment]Yes[comment]，Yes[comment]：{@code input text} [comment]
 * [comment] ASCII [comment]，[comment]None[comment]。[comment]：
 * <ul>
 *   <li>[comment]（{@code input text}）—— [comment] / [comment]；</li>
 *   <li>[comment] + [comment]（[comment] {@code KEYCODE_PASTE}）—— [comment]、emoji、[comment]。</li>
 * </ul>
 *
 * <p>⚠ [comment]：[comment]。[comment]「[comment]」，
 * [comment] —— [comment]。
 */
public class KeyActivity extends Activity {

    private TextView tvTitle;
    private TextView tvState;
    private EditText etText;
    private Button btnMode;

    private ExtScreen.Dev dev;
    private String lastAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ext_key);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tvTitle = findViewById(R.id.tvKeyTitle);
        tvState = findViewById(R.id.tvKeyState);
        etText = findViewById(R.id.etKeyText);
        btnMode = findViewById(R.id.btnKeyMode);

        ExtUi.info(this, R.id.iKeyTop, R.string.ext_key_title, R.string.info_ext_key);

        findViewById(R.id.btnKeyClose).setOnClickListener(v -> finish());

        findViewById(R.id.btnKeySend).setOnClickListener(v -> doSend());
        findViewById(R.id.btnKeyEnter).setOnClickListener(v -> sendKey("KEYCODE_ENTER", "Text"));
        findViewById(R.id.btnKeyDel).setOnClickListener(v -> sendKey("KEYCODE_DEL", "Text"));
        findViewById(R.id.btnKeySpace).setOnClickListener(v -> sendKey("KEYCODE_SPACE", "Text"));
        findViewById(R.id.btnKeyTab).setOnClickListener(v -> sendKey("KEYCODE_TAB", "Tab"));
        findViewById(R.id.btnKeyEsc).setOnClickListener(v -> sendKey("KEYCODE_ESCAPE", "Esc"));
        findViewById(R.id.btnKeyClear).setOnClickListener(v -> {
            etText.setText("");
            lastAction = "Text";
            refreshState();
        });
        btnMode.setOnClickListener(v -> {
            int now = ExtPrefs.kbMode(this);
            ExtPrefs.setKbMode(this, (now + 1) % 3);
            syncModeButton();
            refreshState();
        });

        syncModeButton();
        resolve();
    }

    private void resolve() {
        tvState.setText(R.string.ext_finding);
        ExtUi.resolve(this, d -> {
            dev = d;
            ExtUi.title(this, tvTitle, R.string.ext_key_title, dev);
            refreshState();
        });
    }

    // ------------------------------------------------------------------ [comment]

    /**
     * [comment]。
     *
     * <p>[comment]（[comment]Yes[comment]）：[comment] / [comment] / [comment]。
     * [comment]Yes"[comment] ASCII [comment]" —— [comment]，
     * [comment]（[comment]，[comment]"[comment]"[comment]Notice）。
     */
    private void doSend() {
        String text = etText.getText() == null ? "" : etText.getText().toString();
        if (text.isEmpty()) {
            ExtUi.toast(this, "Text");
            return;
        }
        if (dev == null) {
            ExtUi.toast(this, getString(R.string.ext_no_screen));
            return;
        }
        int mode = ExtPrefs.kbMode(this);
        if (mode == ExtPrefs.KB_AUTO) {
            mode = isAscii(text) ? ExtPrefs.KB_TEXT : ExtPrefs.KB_CLIP;
        }
        if (mode == ExtPrefs.KB_CLIP) {
            putClipboard(text);
            ExtScreen.paste(dev.id, text);
            lastAction = "Text " + text.length() + " Text";
        } else {
            ExtScreen.typeText(dev.id, text);
            lastAction = "Text " + text.length() + " Text";
        }
        etText.setText("");
        refreshState();
    }

    private void putClipboard(String text) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Z flipText", text));
            }
        } catch (Throwable ignored) {
        }
    }

    /** [comment]Yes ASCII [comment]"[comment]"；[comment] */
    private static boolean isAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    private void sendKey(String keyCode, String label) {
        if (dev == null) {
            ExtUi.toast(this, getString(R.string.ext_no_screen));
            return;
        }
        ExtScreen.key(dev.id, keyCode);
        lastAction = "Text " + label;
        refreshState();
    }

    private void syncModeButton() {
        switch (ExtPrefs.kbMode(this)) {
            case ExtPrefs.KB_TEXT:
                btnMode.setText(R.string.ext_key_mode_text);
                break;
            case ExtPrefs.KB_CLIP:
                btnMode.setText(R.string.ext_key_mode_clip);
                break;
            default:
                btnMode.setText(R.string.ext_key_mode_auto);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // [comment]，[comment] / [comment]Settings（[comment] ExtControlMode）
        ExtControlMode.on();
    }

    @Override
    protected void onStop() {
        ExtControlMode.off();
        super.onStop();
    }

    private void refreshState() {
        if (dev == null) {
            tvState.setText(R.string.ext_no_screen);
            return;
        }
        String base = getString(R.string.ext_key_state_fmt, dev.sizeText());
        tvState.setText(lastAction == null ? base : lastAction + "　·　" + base);
    }
}
