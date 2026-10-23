package com.wb.extrotator;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

/**
 * 「[comment]」（[comment]）—— [comment]：[comment]、[comment]、[comment]、[comment]/[comment]。
 *
 * <p><b>[comment]</b>，[comment]Yes[comment]"[comment]"。[comment]Yes[comment]
 * {@link MediaSessionManager}：[comment] MediaSession [comment]（[comment]、QQ [comment]、B [comment]、
 * [comment]…）[comment]，[comment]
 * {@link MediaController} —— [comment]（[comment]/[comment]/[comment]/[comment]）[comment]，
 * [comment]（{@code getTransportControls()}）。
 *
 * <p>⚠ <b>[comment]</b>：{@code getActiveSessions} [comment]Yes[comment]。[comment]
 * [comment]（[comment] {@link CoverNotifListener}），[comment]；
 * [comment]"[comment]"，[comment]Yes[comment]。
 *
 * <p>⚠ [comment]Yes<b>[comment]</b>[comment]（[comment] {@link Lyric} [comment]）。[comment]Yes[comment]，
 * [comment] —— [comment]。
 */
public class PlayerActivity extends Activity {

    private ImageView cover;
    private TextView titleView, artistView, lyricPrev, lyricNow, lyricNext, timeView, hintView;
    private SeekBar seek;
    private ImageButton btnPlay;

    private MediaController ctrl;
    /** [comment]（[comment]"[comment]"[comment]Yes[comment]，Notice[comment]） */
    private boolean denied;
    /** [comment] —— [comment] */
    private boolean seeking;
    /** [comment]（"[comment]|[comment]"）；[comment] */
    private String lyricKey;
    private Lyric lyric = Lyric.EMPTY;
    /** [comment]：[comment]，[comment] */
    private int lyricSeq;
    /** [comment] tick（[comment] 3 [comment]） */
    private int missTicks;

    private final Handler ui = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            sync();
            ui.postDelayed(this, 500L);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_ext_player);
        // [comment]（[comment]「[comment]」[comment]）
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        cover = findViewById(R.id.playerCover);
        titleView = findViewById(R.id.playerTitle);
        artistView = findViewById(R.id.playerArtist);
        lyricPrev = findViewById(R.id.playerLyricPrev);
        lyricNow = findViewById(R.id.playerLyricNow);
        lyricNext = findViewById(R.id.playerLyricNext);
        timeView = findViewById(R.id.playerTime);
        hintView = findViewById(R.id.playerHint);
        seek = findViewById(R.id.playerSeek);
        btnPlay = findViewById(R.id.playerPlay);

        findViewById(R.id.playerPrev).setOnClickListener(v -> {
            if (ctrl != null) {
                ctrl.getTransportControls().skipToPrevious();
            }
        });
        findViewById(R.id.playerNext).setOnClickListener(v -> {
            if (ctrl != null) {
                ctrl.getTransportControls().skipToNext();
            }
        });
        btnPlay.setOnClickListener(v -> {
            if (ctrl == null) {
                return;
            }
            PlaybackState ps = ctrl.getPlaybackState();
            boolean playing = ps != null && ps.getState() == PlaybackState.STATE_PLAYING;
            if (playing) {
                ctrl.getTransportControls().pause();
            } else {
                ctrl.getTransportControls().play();
            }
        });

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                seeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                seeking = false;
                if (ctrl != null) {
                    ctrl.getTransportControls().seekTo(sb.getProgress());
                }
            }
        });

        // [comment]（[comment]），[comment]
        findViewById(R.id.playerClose).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        missTicks = 0;
        connect();
        sync();
        ui.removeCallbacks(tick);
        ui.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ui.removeCallbacks(tick);
    }

    // ============================================================ [comment]

    /** [comment] */
    private void connect() {
        try {
            MediaSessionManager msm =
                    (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (msm == null) {
                return;
            }
            List<MediaController> list = msm.getActiveSessions(
                    new ComponentName(this, CoverNotifListener.class));
            MediaController best = null;
            if (list != null) {
                for (MediaController mc : list) {
                    // [comment]"[comment]"[comment]：[comment]
                    if (mc.getMetadata() != null) {
                        best = mc;
                        break;
                    }
                }
                if (best == null && !list.isEmpty()) {
                    best = list.get(0);
                }
            }
            ctrl = best;
            denied = false;
        } catch (SecurityException se) {
            // [comment]
            ctrl = null;
            denied = true;
        } catch (Throwable t) {
            ctrl = null;
        }
    }

    // ============================================================ [comment]

    private void sync() {
        if (ctrl == null) {
            // [comment] 3 [comment]（[comment]，[comment]）
            if (++missTicks % 6 == 0) {
                connect();
            }
            showIdle();
            return;
        }
        try {
            MediaMetadata md = ctrl.getMetadata();
            if (md == null) {
                showIdle();
                return;
            }
            hintView.setVisibility(TextView.GONE);
            String title = text(md, MediaMetadata.METADATA_KEY_TITLE);
            String artist = text(md, MediaMetadata.METADATA_KEY_ARTIST);
            if (artist.isEmpty()) {
                artist = text(md, MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
            }
            titleView.setText(title.isEmpty() ? "Text" : title);
            artistView.setText(artist);

            long dur = md.getLong(MediaMetadata.METADATA_KEY_DURATION);
            PlaybackState ps = ctrl.getPlaybackState();
            long pos = ps == null ? 0L : Math.max(0L, ps.getPosition());

            // [comment]（getBitmap [comment]，[comment] 0.5 [comment]）
            String key = title + "|" + artist;
            if (!key.equals(lyricKey)) {
                lyricKey = key;
                Bitmap art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (art == null) {
                    art = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
                }
                if (art == null) {
                    cover.setImageResource(R.drawable.ic_pseudo_player);
                } else {
                    cover.setImageBitmap(art);
                }
                requestLyric(title, artist);
            }

            if (dur > 0 && seek.getMax() != (int) dur) {
                seek.setMax((int) dur);
            }
            if (!seeking) {
                seek.setProgress((int) Math.min(pos, seek.getMax()));
            }
            timeView.setText(fmt(pos) + " / " + fmt(dur));

            boolean playing = ps != null && ps.getState() == PlaybackState.STATE_PLAYING;
            btnPlay.setImageResource(playing
                    ? R.drawable.ic_player_pause : R.drawable.ic_player_play);

            showLyric(pos);
        } catch (Throwable t) {
            showIdle();
        }
    }

    /** [comment]（[comment]）[comment]"[comment]" */
    private void showIdle() {
        titleView.setText(R.string.player_idle_title);
        artistView.setText("");
        cover.setImageResource(R.drawable.ic_pseudo_player);
        lyricKey = null;
        lyric = Lyric.EMPTY;
        lyricPrev.setText("");
        lyricNext.setText("");
        lyricNow.setText("");
        // [comment]：[comment] / [comment]，Yes[comment]
        hintView.setVisibility(TextView.VISIBLE);
        hintView.setText(denied ? R.string.player_need_access : R.string.player_idle_hint);
        seek.setProgress(0);
        timeView.setText("--:-- / --:--");
        btnPlay.setImageResource(R.drawable.ic_player_play);
    }

    // ============================================================ [comment]

    private void requestLyric(final String title, final String artist) {
        lyric = Lyric.EMPTY;
        final int seq = ++lyricSeq;
        if (title.isEmpty()) {
            return;
        }
        lyricNow.setText(R.string.player_lyric_loading);
        new Thread(() -> {
            final Lyric got = Lyric.fetchBlocking(title, artist);
            ui.post(() -> {
                // [comment] —— [comment]
                if (seq != lyricSeq) {
                    return;
                }
                lyric = got;
                if (got.isEmpty()) {
                    lyricNow.setText(R.string.player_lyric_none);
                }
            });
        }, "extrot-lyric").start();
    }

    private void showLyric(long pos) {
        if (lyric.isEmpty()) {
            return;
        }
        int i = lyric.indexAt(pos);
        if (i < 0) {
            lyricPrev.setText("");
            lyricNow.setText("");
            lyricNext.setText(lyric.lines.get(0).text);
            return;
        }
        lyricPrev.setText(i > 0 ? lyric.lines.get(i - 1).text : "");
        lyricNow.setText(lyric.lines.get(i).text);
        lyricNext.setText(i + 1 < lyric.lines.size() ? lyric.lines.get(i + 1).text : "");
    }

    // ============================================================ [comment]

    private static String text(MediaMetadata md, String key) {
        try {
            String s = md.getString(key);
            return s == null ? "" : s;
        } catch (Throwable t) {
            return "";
        }
    }

    private static String fmt(long ms) {
        if (ms <= 0) {
            return "0:00";
        }
        long s = ms / 1000L;
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }
}
