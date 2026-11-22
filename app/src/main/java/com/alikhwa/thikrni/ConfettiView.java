package com.alikhwa.thikrni;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Random;

/** قصاصات ملونة تتساقط عند إتمام الهدف اليومي. */
public class ConfettiView extends View {
    private static final int N = 90;
    private final float[] x = new float[N], y = new float[N], vx = new float[N], vy = new float[N],
            sz = new float[N], rot = new float[N], vr = new float[N];
    private final int[] col = new int[N];
    private final boolean[] round = new boolean[N];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Path path = new android.graphics.Path();
    private ValueAnimator anim;
    private long last;
    private final float d;
    private boolean ready = false;
    private final Theme th;

    public ConfettiView(Context c, Theme theme) {
        super(c);
        th = theme;
        d = c.getResources().getDisplayMetrics().density;
    }

    private void init() {
        Random r = new Random();
        int[] palette = {th.accent, th.accentLight, 0xFFFFFFFF, th.glowA | 0xFF000000, th.accentDark, th.glowB | 0xFF000000};
        for (int i = 0; i < N; i++) {
            x[i] = r.nextFloat() * getWidth();
            y[i] = -r.nextFloat() * getHeight() * 0.6f;
            vx[i] = (r.nextFloat() - 0.5f) * 90 * d;
            vy[i] = (160 + r.nextFloat() * 260) * d;
            sz[i] = (4 + r.nextFloat() * 6) * d;
            rot[i] = r.nextFloat() * 360f;
            vr[i] = (r.nextFloat() - 0.5f) * 540f;
            col[i] = palette[r.nextInt(palette.length)];
            round[i] = r.nextBoolean();
        }
        ready = true;
    }

    public void start() {
        last = System.nanoTime();
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(3600);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            long now = System.nanoTime();
            float dt = Math.min(0.05f, (now - last) / 1e9f);
            last = now;
            if (!ready && getWidth() > 0) init();
            if (ready) {
                for (int i = 0; i < N; i++) {
                    x[i] += vx[i] * dt;
                    y[i] += vy[i] * dt;
                    rot[i] += vr[i] * dt;
                }
            }
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas cv) {
        if (!ready) return;
        for (int i = 0; i < N; i++) {
            if (y[i] < -20 * d || y[i] > getHeight() + 20 * d) continue;
            paint.setColor(col[i]);
            cv.save();
            cv.translate(x[i], y[i]);
            cv.rotate(rot[i]);
            float s2 = sz[i] / 2f;
            switch (th.st.spark) {
                case Style.SP_DIAMOND:
                    path.reset();
                    path.moveTo(0, -s2 * 1.3f);
                    path.lineTo(s2 * 0.85f, 0);
                    path.lineTo(0, s2 * 1.3f);
                    path.lineTo(-s2 * 0.85f, 0);
                    path.close();
                    cv.drawPath(path, paint);
                    break;
                case Style.SP_PETAL:
                    cv.drawOval(-s2 * 1.3f, -s2 * 0.6f, s2 * 1.3f, s2 * 0.6f, paint);
                    break;
                case Style.SP_SQUARE:
                    cv.drawRect(-s2 * 0.8f, -s2 * 0.8f, s2 * 0.8f, s2 * 0.8f, paint);
                    break;
                case Style.SP_BUBBLE:
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(1.6f * d);
                    cv.drawCircle(0, 0, s2, paint);
                    paint.setStyle(Paint.Style.FILL);
                    break;
                default:
                    if (round[i]) cv.drawCircle(0, 0, s2, paint);
                    else cv.drawRect(-s2, -s2 / 2f, s2, s2 / 2f, paint);
                    break;
            }
            cv.restore();
        }
    }
}
