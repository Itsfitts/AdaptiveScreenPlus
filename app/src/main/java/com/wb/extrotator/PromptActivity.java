package com.wb.extrotator;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * [comment] —— [comment]「[comment]」[comment] Activity，[comment] {@link RotationService}
 * [comment]。
 *
 * <p>[comment]，[comment]「[comment]」[comment]；[comment]
 * [comment] shell（{@code am start}）[comment]，[comment] —— [comment]。
 *
 * <p><b>[comment]</b>：[comment] 300 [comment] dp [comment]，[comment]，
 * [comment] —— [comment]「[comment]Confirm[comment]」[comment]。[comment]Yes：[comment]
 * <b>[comment]</b>（[comment] / [comment] / [comment]，[comment] DPI [comment]Yes[comment]），[comment] ScrollView
 * （[comment]），[comment]<b>[comment]</b> —— [comment]、[comment]。
 */
public class PromptActivity extends Activity {

    private static final String TAG = "PromptActivity";

    public static final String EXTRA_DESC = "desc";
    public static final String EXTRA_W = "w";
    public static final String EXTRA_H = "h";

    /** [comment] / [comment]：[comment] 1.2 [comment]，[comment] 0.62 [comment] */
    private static final float MAX_SCALE = 1.20f;
    private static final float MIN_SCALE = 0.62f;
    /** [comment]（[comment]，[comment]） */
    private static final float MAX_W_RATIO = 0.94f;
    private static final float MAX_H_RATIO = 0.85f;
    /** [comment]：[comment]（[comment] 880dp [comment]）[comment] */
    private static final float MAX_W_DP = 430f;

    private CheckBox cbNoAsk;

    /** [comment]，[comment]（[comment]） */
    private final List<TextView> scaledText = new ArrayList<>();
    private final List<Float> scaledTextBase = new ArrayList<>();
    private final List<View> scaledPad = new ArrayList<>();
    private final List<int[]> scaledPadBase = new ArrayList<>();
    private final List<View> scaledMargin = new ArrayList<>();
    private final List<int[]> scaledMarginBase = new ArrayList<>();

    private boolean hasScaled = false;
    /** [comment] */
    private float lastK = MAX_SCALE;
    private int targetW = 0;
    private int maxH = 0;
    /** [comment] */
    private int windowH = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prompt);

        // [comment]，[comment]"[comment]"[comment]
        setFinishOnTouchOutside(false);

        int w = getIntent().getIntExtra(EXTRA_W, 1080);
        int h = getIntent().getIntExtra(EXTRA_H, 1920);
        String desc = getIntent().getStringExtra(EXTRA_DESC);

        TextView tvExt = findViewById(R.id.tvPromptExt);
        tvExt.setText(desc == null || desc.isEmpty() ? "Text" : desc);

        TextView tvRes = findViewById(R.id.tvPromptRes);
        // [comment] w/h [comment]，[comment]，[comment]
        tvRes.setText(w + " × " + h + "（Text）");

        cbNoAsk = findViewById(R.id.cbPromptNoAsk);

        findViewById(R.id.btnPromptYes).setOnClickListener(v -> answer(true));
        findViewById(R.id.btnPromptNo).setOnClickListener(v -> answer(false));

        // [comment]，[comment]
        RotationService.dismissPromptNotification(this);

        final View root = findViewById(R.id.promptRoot);
        root.post(() -> fitToWindow(root));
    }

    /**
     * [comment]，[comment]「[comment]」。
     *
     * <p>[comment]Yes[comment]：[comment] —— [comment]
     * [comment]、[comment]、[comment]，[comment]。[comment]「[comment]」，
     * [comment]，[comment]（[comment]）[comment]。
     *
     * <p>[comment]，[comment]<b>[comment]</b>：[comment]、[comment]；
     * [comment] {@link #settle} [comment]<b>[comment]</b>[comment] —— [comment]
     * [comment]，No[comment]「[comment]」。[comment]（{@link #MAX_SCALE}），[comment]
     * {@link #MIN_SCALE}，[comment]（[comment]，
     * [comment]）。
     */
    private void fitToWindow(View root) {
        if (hasScaled) {
            return;
        }
        hasScaled = true;

        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density <= 0f ? 1f : dm.density;

        // [comment]：[comment] 94%，[comment] 430dp（[comment]）
        targetW = (int) Math.min(dm.widthPixels * MAX_W_RATIO, MAX_W_DP * density + 0.5f);
        maxH = (int) (dm.heightPixels * MAX_H_RATIO);

        collect(root);

        lastK = MAX_SCALE;
        applyScale(lastK);

        // [comment]：[comment]，[comment]
        int estimateInner = Math.max(1, targetW - Math.round(density * 26f));
        int natural = measureNatural(estimateInner);
        windowH = Math.max(1, Math.min(natural, maxH));
        getWindow().setLayout(targetW, windowH);

        root.postDelayed(() -> settle(root, 0), 40L);
    }

    /**
     * [comment]（[comment]）：[comment]<b>[comment]</b>[comment]「[comment] vs [comment]」[comment]，[comment]。
     *
     * <p>[comment]：[comment]（[comment]、[comment]、
     * [comment]，[comment]），[comment] ——
     * [comment]，[comment]，[comment]。
     *
     * @param round [comment]，[comment]「[comment]」[comment]
     */
    private void settle(View root, int round) {
        View body = findViewById(R.id.promptBody);
        View footer = findViewById(R.id.promptFooter);
        ScrollView scroll = findViewById(R.id.promptScroll);
        if (body == null || footer == null || scroll == null) {
            return;
        }
        int viewport = scroll.getHeight();
        int content = body.getHeight();
        if (viewport <= 0 || content <= 0) {
            if (round < 6) {
                root.postDelayed(() -> settle(root, round + 1), 40L);
            }
            return;
        }

        int deficit = content - viewport;   // >0 = Text，Text
        Log.d(TAG, "settle k=" + lastK + " win=" + windowH
                + " viewport=" + viewport + " content=" + content
                + " deficit=" + deficit + " maxH=" + maxH);

        if (deficit > 0) {
            int want = Math.min(maxH, windowH + deficit);
            if (want > windowH) {
                // [comment]：[comment]
                windowH = want;
                getWindow().setLayout(targetW, want);
                root.postDelayed(() -> settle(root, round + 1), 40L);
                return;
            }
            // [comment]Yes[comment] → [comment]
            if (lastK > MIN_SCALE + 0.01f) {
                lastK = Math.max(MIN_SCALE, lastK * 0.9f);
                applyScale(lastK);
                windowH = maxH;
                getWindow().setLayout(targetW, maxH);
                root.postDelayed(() -> settle(root, 0), 40L);
                return;
            }
            // [comment]：[comment]，[comment]，[comment]
            return;
        }

        // [comment]（[comment]）[comment]，[comment]
        int want = Math.max(1, Math.min(maxH, windowH + deficit));
        if (want < windowH) {
            windowH = want;
            getWindow().setLayout(targetW, want);
        }
    }

    /** [comment]"[comment]"（[comment]） */
    private int measureNatural(int innerW) {
        View root = findViewById(R.id.promptRoot);
        ScrollView scroll = findViewById(R.id.promptScroll);
        View body = findViewById(R.id.promptBody);
        View footer = findViewById(R.id.promptFooter);

        int specW = View.MeasureSpec.makeMeasureSpec(Math.max(1, innerW), View.MeasureSpec.EXACTLY);
        int specH = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        body.measure(specW, specH);
        footer.measure(specW, specH);

        int footerGap = 0;
        ViewGroup.LayoutParams flp = footer.getLayoutParams();
        if (flp instanceof ViewGroup.MarginLayoutParams) {
            footerGap = ((ViewGroup.MarginLayoutParams) flp).topMargin;
        }
        return root.getPaddingTop() + root.getPaddingBottom()
                + body.getMeasuredHeight()
                + scroll.getPaddingTop() + scroll.getPaddingBottom()
                + footerGap + footer.getMeasuredHeight();
    }

    // ---------------------------------------------------------------- [comment]

    /** [comment] / [comment] / [comment] */
    private void collect(View v) {
        if (v instanceof TextView) {
            TextView tv = (TextView) v;
            scaledText.add(tv);
            scaledTextBase.add(tv.getTextSize());
        }
        if (v.getPaddingLeft() != 0 || v.getPaddingTop() != 0
                || v.getPaddingRight() != 0 || v.getPaddingBottom() != 0) {
            scaledPad.add(v);
            scaledPadBase.add(new int[]{v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), v.getPaddingBottom()});
        }
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
            if (m.leftMargin != 0 || m.topMargin != 0
                    || m.rightMargin != 0 || m.bottomMargin != 0) {
                scaledMargin.add(v);
                scaledMarginBase.add(new int[]{m.leftMargin, m.topMargin,
                        m.rightMargin, m.bottomMargin});
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                collect(g.getChildAt(i));
            }
        }
    }

    private void applyScale(float k) {
        for (int i = 0; i < scaledText.size(); i++) {
            scaledText.get(i).setTextSize(TypedValue.COMPLEX_UNIT_PX, scaledTextBase.get(i) * k);
        }
        for (int i = 0; i < scaledPad.size(); i++) {
            int[] p = scaledPadBase.get(i);
            scaledPad.get(i).setPadding(Math.round(p[0] * k), Math.round(p[1] * k),
                    Math.round(p[2] * k), Math.round(p[3] * k));
        }
        for (int i = 0; i < scaledMargin.size(); i++) {
            int[] m = scaledMarginBase.get(i);
            View v = scaledMargin.get(i);
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mp = (ViewGroup.MarginLayoutParams) lp;
                mp.leftMargin = Math.round(m[0] * k);
                mp.topMargin = Math.round(m[1] * k);
                mp.rightMargin = Math.round(m[2] * k);
                mp.bottomMargin = Math.round(m[3] * k);
                v.setLayoutParams(mp);
            }
        }
    }

    // ---------------------------------------------------------------- [comment]

    private void answer(boolean yes) {
        boolean noAsk = cbNoAsk != null && cbNoAsk.isChecked();
        RotationService.answerPrompt(this, yes, noAsk);
        finish();
    }

    @Override
    public void onBackPressed() {
        // [comment] = [comment]，[comment]Disable「[comment]」[comment]
        RotationService.answerPrompt(this, false, false);
        finish();
    }
}
