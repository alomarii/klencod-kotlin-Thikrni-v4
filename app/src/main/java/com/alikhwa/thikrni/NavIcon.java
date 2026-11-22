package com.alikhwa.thikrni;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/** أيقونات شريط التنقل مرسومة بالكود (٠ رئيسية، ١ إحصائيات، ٢ صوتيات، ٣ إعدادات). */
public class NavIcon extends View {
    private final int type;
    private final int off, on;
    private final float d;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float sel = 0f;
    private ValueAnimator va;

    public NavIcon(Context c, int type, int off, int on) {
        super(c);
        this.type = type;
        this.off = off;
        this.on = on;
        d = c.getResources().getDisplayMetrics().density;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(2f * d);
    }

    public void setSelectedAnimated(boolean selected, boolean animate) {
        float to = selected ? 1f : 0f;
        if (va != null) va.cancel();
        if (!animate) {
            sel = to;
            invalidate();
            return;
        }
        va = ValueAnimator.ofFloat(sel, to);
        va.setDuration(360);
        va.setInterpolator(new OvershootInterpolator(2.2f));
        va.addUpdateListener(a -> {
            sel = (float) a.getAnimatedValue();
            invalidate();
        });
        va.start();
    }

    private static int blend(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t);
        int g = (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t);
        int bl = (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
        return Color.rgb(r, g, bl);
    }

    @Override
    protected void onDraw(Canvas cv) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        p.setColor(blend(off, on, sel));
        cv.save();
        float sc = 1f + 0.16f * sel;
        cv.scale(sc, sc, cx, cy);
        cv.translate(0, -2f * d * sel);
        switch (type) {
            case 0: // بيت
                path.reset();
                path.moveTo(cx - 10 * d, cy - 0.5f * d);
                path.lineTo(cx, cy - 9.5f * d);
                path.lineTo(cx + 10 * d, cy - 0.5f * d);
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(cx - 7 * d, cy - 2.5f * d);
                path.lineTo(cx - 7 * d, cy + 8.5f * d);
                path.lineTo(cx + 7 * d, cy + 8.5f * d);
                path.lineTo(cx + 7 * d, cy - 2.5f * d);
                cv.drawPath(path, p);
                cv.drawLine(cx, cy + 8.5f * d, cx, cy + 3 * d, p);
                break;
            case 1: // أعمدة
                cv.drawLine(cx - 7 * d, cy + 8 * d, cx - 7 * d, cy, p);
                cv.drawLine(cx, cy + 8 * d, cx, cy - 8 * d, p);
                cv.drawLine(cx + 7 * d, cy + 8 * d, cx + 7 * d, cy - 3.5f * d, p);
                break;
            case 2: // نوتة موسيقية
                cv.drawCircle(cx - 3.5f * d, cy + 6 * d, 3 * d, p);
                cv.drawLine(cx - 0.5f * d, cy + 6 * d, cx - 0.5f * d, cy - 8 * d, p);
                path.reset();
                path.moveTo(cx - 0.5f * d, cy - 8 * d);
                path.quadTo(cx + 6 * d, cy - 6 * d, cx + 6 * d, cy - 1 * d);
                cv.drawPath(path, p);
                break;
            default: // ترس
                cv.drawCircle(cx, cy, 3 * d, p);
                cv.drawCircle(cx, cy, 7 * d, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    float c1 = (float) Math.cos(a), s1 = (float) Math.sin(a);
                    cv.drawLine(cx + c1 * 7 * d, cy + s1 * 7 * d, cx + c1 * 10 * d, cy + s1 * 10 * d, p);
                }
                break;
        }
        cv.restore();
    }
}
