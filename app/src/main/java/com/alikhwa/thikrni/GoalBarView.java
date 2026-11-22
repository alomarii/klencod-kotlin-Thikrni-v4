package com.alikhwa.thikrni;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/** شريط تقدّم الهدف اليومي: تعبئة متحركة مع لمعان يمرّ فوقها. */
public class GoalBarView extends View {
    private final Theme th;
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF rect = new RectF();
    private float progress = 0f;
    private float shinePos = 0f;
    private ValueAnimator anim, shineAnim;

    public GoalBarView(Context c, Theme theme) {
        super(c);
        th = theme;
        track.setColor(0x26FFFFFF);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(c.getResources().getDisplayMetrics().density);
        edge.setColor(Theme.a(th.accent, 0x55));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        shineAnim = ValueAnimator.ofFloat(0f, 1f);
        shineAnim.setDuration(2200);
        shineAnim.setRepeatCount(ValueAnimator.INFINITE);
        shineAnim.setInterpolator(new LinearInterpolator());
        shineAnim.addUpdateListener(a -> {
            shinePos = (float) a.getAnimatedValue();
            invalidate();
        });
        Motion.loop(shineAnim);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (shineAnim != null) shineAnim.cancel();
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        fill.setShader(new LinearGradient(w, 0, 0, 0,
                new int[]{th.accentLight, th.accent, th.accentDark}, null, Shader.TileMode.CLAMP));
    }

    public void setProgress(float v, boolean animate) {
        final float target = Math.max(0f, Math.min(1f, v));
        if (anim != null) anim.cancel();
        if (!animate) {
            progress = target;
            invalidate();
            return;
        }
        anim = ValueAnimator.ofFloat(progress, target);
        anim.setDuration(900);
        anim.setInterpolator(new DecelerateInterpolator(2f));
        anim.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!L.rtl()) {
            canvas.save();
            canvas.scale(-1f, 1f, getWidth() / 2f, getHeight() / 2f);
            draw0(canvas);
            canvas.restore();
        } else {
            draw0(canvas);
        }
    }

    private void draw0(Canvas cv) {
        float w = getWidth(), h = getHeight();
        rect.set(0, 0, w, h);
        Shapes.path(clip, rect, th.st.shape, h * 0.5f, true);
        cv.drawPath(clip, track);

        float fw = w * progress;
        if (fw > 1f) {
            cv.save();
            cv.clipPath(clip);
            // التعبئة من اليمين إلى اليسار (واجهة عربية)
            cv.drawRect(w - fw, 0, w, h, fill);
            // لمعان متحرك
            float bw = w * 0.28f;
            float bx = w - shinePos * (w + bw);
            shine.setShader(new LinearGradient(bx, 0, bx + bw, 0,
                    new int[]{0x00FFFFFF, 0x66FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            cv.save();
            cv.clipRect(w - fw, 0, w, h);
            cv.drawRect(bx, 0, bx + bw, h, shine);
            cv.restore();
            cv.restore();
        }
        // وهج عند الإتمام
        float pulse = 0.5f + 0.5f * (float) Math.sin(shinePos * 2 * Math.PI);
        if (progress >= 0.999f) {
            edge.setColor(Theme.a(th.accentLight, (int) (90 + 160 * pulse)));
            edge.setStrokeWidth(getResources().getDisplayMetrics().density * (1.5f + 1.5f * pulse));
        } else {
            edge.setColor(Theme.a(th.accent, 0x55));
            edge.setStrokeWidth(getResources().getDisplayMetrics().density);
        }
        cv.drawPath(clip, edge);
        // شرارة عند رأس الشريط
        if (fw > 1f && progress < 0.999f) {
            float hx = w - fw;
            headPaint.setShader(new android.graphics.RadialGradient(hx, h / 2f, h * (1.1f + 0.5f * pulse),
                    Theme.a(th.accentLight, 0xCC), Theme.a(th.accentLight, 0x00), Shader.TileMode.CLAMP));
            cv.drawCircle(hx, h / 2f, h * (1.1f + 0.5f * pulse), headPaint);
        }
    }
}
