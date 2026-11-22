package com.alikhwa.thikrni;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** رسم بياني بسيط لآخر ٧ أيام (الأقدم على اليمين واليوم على اليسار). */
public class BarChartView extends View {
    private final Theme th;
    private final float d;
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private long[] vals = new long[7];
    private String[] labels = new String[7];
    private float anim = 1f;
    private ValueAnimator va;

    public BarChartView(Context c, Theme theme) {
        super(c);
        th = theme;
        d = c.getResources().getDisplayMetrics().density;
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setTypeface(Typeface.DEFAULT_BOLD);
        base.setColor(0x22FFFFFF);
        for (int i = 0; i < 7; i++) labels[i] = "";
    }

    public void setData(long[] v, String[] l, boolean animate) {
        vals = v;
        labels = l;
        if (va != null) va.cancel();
        if (!animate) {
            anim = 1f;
            invalidate();
            return;
        }
        anim = 0f;
        va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(1200);
        va.setInterpolator(new android.view.animation.LinearInterpolator());
        va.addUpdateListener(a -> {
            anim = (float) a.getAnimatedValue();
            invalidate();
        });
        va.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (va != null) va.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float topPad = 20 * d, bottomPad = 24 * d;
        float chartH = h - topPad - bottomPad;
        long max = 1;
        for (long v : vals) max = Math.max(max, v);
        float slot = w / 7f;
        float bw = slot * 0.52f;

        cv.drawRect(0, h - bottomPad, w, h - bottomPad + d, base);

        for (int i = 0; i < 7; i++) {
            float cx = L.rtl() ? w - (i + 0.5f) * slot : (i + 0.5f) * slot;
            boolean today = i == 6;
            float tt = Math.max(0f, Math.min(1f, (anim - i * 0.07f) / 0.58f));
            float u = tt - 1f;
            float local = tt <= 0f ? 0f : 1f + 2.70158f * u * u * u + 1.70158f * u * u; // easeOutBack
            float bh = chartH * (float) vals[i] / max * local;
            if (vals[i] > 0 && bh < 4 * d) bh = 4 * d;
            float top = h - bottomPad - bh;
            rect.set(cx - bw / 2, top, cx + bw / 2, h - bottomPad);
            if (bh > 0) {
                bar.setShader(new LinearGradient(0, top, 0, h - bottomPad,
                        today ? th.accentLight : Theme.a(th.accent, 0xB0),
                        today ? th.accentDark : Theme.a(th.accentDark, 0x80), Shader.TileMode.CLAMP));
                cv.drawRoundRect(rect, 8 * d, 8 * d, bar);
            }
            if (vals[i] > 0 && local > 0.85f) {
                txt.setTextSize(11 * d);
                txt.setColor(today ? th.accent : 0xFFFFFFFF);
                cv.drawText(HijriUtil.num(vals[i]), cx, Math.max(14 * d, top - 5 * d), txt);
            }
            txt.setTextSize(10.5f * d);
            txt.setColor(today ? th.accent : th.sub);
            cv.drawText(labels[i], cx, h - 7 * d, txt);
        }
    }
}
