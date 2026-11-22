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
import android.view.animation.LinearInterpolator;

/**
 * مؤشر شريط التنقل. في الثيمات السائلة: كبسولة تنفصل عنها كبسولة تتأخر خلفها ثم تلتحم بها.
 * وفي غيرها: شكل الثيم نفسه (مقصوص، بتلة، قوس...) يتمدد مع السرعة ويضغط عند الوصول، مع توهج.
 */
public class NavGoo extends View {
    private final Theme th;
    private final Style st;
    private final float d;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final Goo.Blob lead = new Goo.Blob(0, 0, 0);
    private final Goo.Blob trail = new Goo.Blob(0, 0, 0);
    private float pw = 0, ph = 0;
    private ValueAnimator loop;
    private long last;

    public NavGoo(Context c, Theme theme) {
        super(c);
        th = theme;
        st = theme.st;
        d = c.getResources().getDisplayMetrics().density;
        glow.setStyle(Paint.Style.STROKE);
        loop = ValueAnimator.ofFloat(0f, 1f);
        loop.setDuration(1000);
        loop.setRepeatCount(ValueAnimator.INFINITE);
        loop.setInterpolator(new LinearInterpolator());
        loop.addUpdateListener(a -> frame());
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        fill.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{th.accentLight, th.accent, th.accentDark}, null, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDetachedFromWindow() {
        loop.cancel();
        super.onDetachedFromWindow();
    }

    void setPillSize(float w, float h) {
        pw = w;
        ph = h;
        invalidate();
    }

    void snapTo(float x) {
        loop.cancel();
        lead.x = lead.tx = trail.x = trail.tx = x;
        lead.vx = trail.vx = 0;
        invalidate();
    }

    void moveTo(float x) {
        lead.tx = x;
        trail.tx = x;
        last = System.nanoTime();
        if (!loop.isStarted()) loop.start();
    }

    private void frame() {
        long now = System.nanoTime();
        float dt = Math.min(0.033f, (now - last) / 1e9f);
        last = now;
        lead.step(dt, 380f, 26f);
        trail.step(dt, 120f, 13f);
        invalidate();
        if (lead.settled() && trail.settled()) {
            lead.x = trail.x = lead.tx;
            lead.vx = trail.vx = 0;
            loop.cancel();
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas cv) {
        if (pw <= 0 || ph <= 0) return;
        float cy = getHeight() / 2f;
        if (st.goo()) {
            drawPill(cv, lead.x, cy, 1f, 1f);
            // بدون Goo (أندرويد أقدم من ١٢) تُرسم الكبسولة الأمامية فقط
            if (Goo.supported()) drawPill(cv, trail.x, cy, 1f, 1f);
            return;
        }
        // مؤشر بشكل الثيم: يتمدد أفقيًا مع السرعة ويضغط عموديًا
        float speed = Math.min(1f, Math.abs(lead.vx) / (2600f * d / 3f));
        float stretch = 1f + 0.32f * speed;
        float squash = 1f - 0.10f * speed;
        float lag = Math.abs(trail.x - lead.x);
        drawStyled(cv, lead.x, cy, stretch, squash, lag);
    }

    private void drawPill(Canvas cv, float x, float cy, float sw, float sh) {
        float w = pw * sw, h = ph * sh;
        rect.set(x - w / 2f, cy - h / 2f, x + w / 2f, cy + h / 2f);
        cv.drawRoundRect(rect, h / 2f, h / 2f, fill);
    }

    private void drawStyled(Canvas cv, float x, float cy, float sw, float sh, float lag) {
        float w = pw * sw, h = ph * sh;
        rect.set(x - w / 2f, cy - h / 2f, x + w / 2f, cy + h / 2f);
        Shapes.path(path, rect, st.shape, Shapes.R_BTN[st.shape] * d, true);
        if (st.fx == Style.FX_NEON) {
            // نيون: تعبئة داكنة وإطار متوهج
            fill.setAlpha(255);
            cv.drawPath(path, fill);
            glow.setStrokeCap(Paint.Cap.ROUND);
            float[] wd = {8f, 5f, 2.6f};
            int[] al = {0x18, 0x30, 0x90};
            for (int i = 0; i < wd.length; i++) {
                glow.setStrokeWidth(wd[i] * d);
                glow.setColor(Theme.a(th.accentLight, al[i]));
                cv.drawPath(path, glow);
            }
        } else {
            // هالة ناعمة خلف المؤشر
            glow.setStrokeWidth(7f * d);
            glow.setColor(Theme.a(th.accent, 0x22 + (int) Math.min(0x40, lag * 0.5f)));
            cv.drawPath(path, glow);
            cv.drawPath(path, fill);
        }
    }
}
