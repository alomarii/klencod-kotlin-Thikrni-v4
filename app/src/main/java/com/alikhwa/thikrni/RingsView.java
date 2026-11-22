package com.alikhwa.thikrni;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** حلقات ضوئية تتّسع من المركز (لشاشة البداية). */
public class RingsView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Theme th;
    private final float d;
    private float t = 0f;
    private ValueAnimator va;

    public RingsView(Context c, Theme theme) {
        super(c);
        th = theme;
        d = c.getResources().getDisplayMetrics().density;
        p.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(2400);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.setInterpolator(new LinearInterpolator());
        va.addUpdateListener(a -> {
            t = (float) a.getAnimatedValue();
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
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float max = Math.min(getWidth(), getHeight()) / 2f;
        for (int k = 0; k < 3; k++) {
            float f = (t + k / 3f) % 1f;
            p.setStrokeWidth((3f - 2f * f) * d);
            p.setColor(Theme.a(th.accent, (int) ((1f - f) * 150)));
            cv.drawCircle(cx, cy, max * (0.25f + 0.75f * f), p);
        }
    }
}
