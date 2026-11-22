package com.alikhwa.thikrni;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** لهب متحرك بألوان الثيم يرمز للسلسلة المتتالية؛ يخبو إن كانت السلسلة صفرًا. */
public class FlameView extends View {
    private final Theme th;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private ValueAnimator loop;
    private float time;
    private long last;
    private boolean lit = true;

    public FlameView(Context c, Theme theme) {
        super(c);
        th = theme;
        loop = ValueAnimator.ofFloat(0f, 1f);
        loop.setDuration(1000);
        loop.setRepeatCount(ValueAnimator.INFINITE);
        loop.setInterpolator(new LinearInterpolator());
        loop.addUpdateListener(a -> {
            long now = System.nanoTime();
            time += Math.min(0.05f, (now - last) / 1e9f);
            last = now;
            invalidate();
        });
    }

    public void setLit(boolean on) {
        lit = on;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        last = System.nanoTime();
        Motion.loop(loop);
    }

    @Override
    protected void onDetachedFromWindow() {
        loop.cancel();
        super.onDetachedFromWindow();
    }

    private void flame(Canvas cv, float cx, float base, float w, float h, float sway, int color) {
        path.reset();
        path.moveTo(cx, base);
        path.cubicTo(cx - w, base - h * 0.25f, cx - w * 0.6f + sway, base - h * 0.72f, cx + sway * 1.6f, base - h);
        path.cubicTo(cx + w * 0.6f + sway, base - h * 0.72f, cx + w, base - h * 0.25f, cx, base);
        path.close();
        p.setColor(color);
        cv.drawPath(path, p);
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, base = h * 0.96f;
        float f1 = 1f + 0.06f * (float) Math.sin(time * 7f);
        float f2 = 1f + 0.08f * (float) Math.sin(time * 9f + 1f);
        float sway = w * 0.07f * (float) Math.sin(time * 3.2f);
        p.setStyle(Paint.Style.FILL);
        if (!lit) {
            flame(cv, cx, base, w * 0.40f, h * 0.8f, 0f, 0x33FFFFFF);
            return;
        }
        // وهج
        p.setColor(Theme.a(th.accent, 0x30));
        cv.drawCircle(cx, h * 0.62f, w * 0.55f * f1, p);
        flame(cv, cx, base, w * 0.46f * f1, h * 0.96f * f1, sway, th.accentDark);
        flame(cv, cx, base, w * 0.34f * f2, h * 0.76f * f2, sway * 0.8f, th.accent);
        flame(cv, cx, base, w * 0.20f * f1, h * 0.50f * f1, sway * 0.5f, th.accentLight);
    }
}
