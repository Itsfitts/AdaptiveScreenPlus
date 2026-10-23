package com.wb.extrotator;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * [comment]（[comment]）：[comment]，[comment]。
 * [comment]Settings，[comment] {@link ExtControlMode} [comment]。
 */
public class StatusActivity extends Activity {

    private StatusBoardView board;
    private StatusMonitor monitor;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_ext_status);
        // [comment]，[comment]
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        board = findViewById(R.id.statusBoard);
        board.setTap(new StatusBoardView.Tap() {
            @Override
            public void onInfo() {
                showInfo();
            }

            @Override
            public void onClose() {
                finish();
            }
        });

        monitor = new StatusMonitor(this, snap -> board.setSnap(snap));
    }

    @Override
    protected void onResume() {
        super.onResume();
        monitor.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // [comment]。[comment]Yes[comment]，[comment]Yes[comment]
        monitor.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        monitor.release();
    }

    private void showInfo() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.ext_status_title)
                .setMessage(R.string.info_ext_status)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
