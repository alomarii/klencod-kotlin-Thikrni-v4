package com.alikhwa.thikrni;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * الزر المتغيّر بحسب الثيم:
 * سائل (Venom) في الزمردي والبنفسجي، لمعان يعبر الزر في الذهبي والياقوتي والغروب،
 * موجة وتموّج ماء في الفيروزي، زجاج مع تموّج في الأزرق الليلي، وإطار نيون متوهج في الفحمي.
 * شكل الزر نفسه (كبسولة، مقصوص، بتلة، ورقة، قوس...) يتبع الثيم أيضًا.
 */
public class VenomButton extends FrameLayout {
    private static final int N = 4;

    private final boolean dark;
    private final Theme th;
    private final Style st;
    private final boolean goo;
    private final float d;
    private final int pad;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path shape = new Path();
    private final Path tmp = new Path();
    private final PathMeasure pm = new PathMeasure();
    private final TextView label;

    private final Goo.Blob[] drops = new Goo.Blob[N];
    private final float[] ph = new float[N], spd = new float[N];
    private final Goo.Blob tail = new Goo.Blob(0, 0, 0);
    private boolean touching;
    private float touchX, touchY, time, sx = 1f, sy = 1f, touchK = 0f;
    private float rx, ry, rt = -1f, idleT = 0f;
    private float pw, ph2, cx, cy, pathLen; // قياسات الزر
    private ValueAnimator loop, scaleAnim;
    private long last;

    public VenomButton(Context c, Theme theme, String text, boolean dark) {
        super(c);
        th = theme;
        st = theme.st;
        goo = st.goo();
        this.dark = dark;
        d = c.getResources().getDisplayMetrics().density;
        pad = Math.round(10 * d);
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(true);

        for (int i = 0; i < N; i++) {
            drops[i] = new Goo.Blob(0, 0, 0);
            ph[i] = i * 1.7f;
            spd[i] = 0.7f + i * 0.23f;
        }

        View baseLayer = new View(c) {
            @Override
            protected void onDraw(Canvas cv) {
                drawBase(cv);
            }
        };
        View fxLayer = new View(c) {
            @Override
            protected void onDraw(Canvas cv) {
                drawFx(cv);
            }
        };
        label = new TextView(c);
        label.setText(L.t(text));
        label.setTextSize(17);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        boolean accentText = dark || st.fx == Style.FX_NEON;
        label.setTextColor(accentText ? th.accent : th.onAccent);
        if (st.fx == Style.FX_NEON) label.setShadowLayer(10f * d, 0f, 0f, th.accent);
        label.setGravity(Gravity.CENTER);
        label.setLayoutDirection(L.dir());

        addView(baseLayer, new LayoutParams(-1, -1));
        addView(fxLayer, new LayoutParams(-1, -1));
        addView(label, new LayoutParams(-2, -2, Gravity.CENTER));
        if (goo) Goo.apply(baseLayer, 7f * d);
        else Motion.press(this, st);

        fx.setStrokeCap(Paint.Cap.ROUND);

        loop = ValueAnimator.ofFloat(0f, 1f);
        loop.setDuration(1000);
        loop.setRepeatCount(ValueAnimator.INFINITE);
        loop.setInterpolator(new LinearInterpolator());
        loop.addUpdateListener(a -> frame());
    }

    public void setText(String s) {
        label.setText(L.t(s));
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        pw = w - 2f * pad;
        ph2 = h - 2f * pad;
        cx = w / 2f;
        cy = h / 2f;
        rect.set(pad, pad, w - pad, h - pad);
        Shapes.path(shape, rect, st.shape, Shapes.R_BTN[st.shape] * d, true);
        pm.setPath(shape, true);
        pathLen = pm.getLength();
        int[] cols;
        if (st.fx == Style.FX_NEON) {
            cols = new int[]{0xFF1E1E1E, 0xFF090909};
        } else if (dark) {
            cols = new int[]{0xFF222A33, 0xFF06090C};
        } else if (st.fx == Style.FX_GLASS) {
            cols = new int[]{Theme.a(th.accentLight, 0xEE), Theme.a(th.accent, 0xE6), Theme.a(th.accentDark, 0xDD)};
        } else {
            cols = new int[]{th.accentLight, th.accent, th.accentDark};
        }
        fill.setShader(new LinearGradient(0, pad, 0, h - pad, cols, null, Shader.TileMode.CLAMP));
        shinePaint.setShader(new LinearGradient(0, pad, 0, cy,
                0x44FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        for (int i = 0; i < N; i++) {
            drops[i].x = drops[i].tx = cx;
            drops[i].y = drops[i].ty = cy;
            drops[i].r = drops[i].tr = 0.3f * ph2;
        }
        tail.x = tail.tx = cx;
        tail.y = tail.ty = cy;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        last = System.nanoTime();
        if (!loop.isStarted()) loop.start(); // يتوقف تلقائيًا بعد ثوانٍ في وضع تخفيف الحركة
        idleT = 0f;
    }

    @Override
    protected void onDetachedFromWindow() {
        loop.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    public void onVisibilityAggregated(boolean visible) {
        super.onVisibilityAggregated(visible);
        if (visible) {
            last = System.nanoTime();
            if (isAttachedToWindow() && !loop.isStarted()) loop.start();
        } else {
            loop.cancel();
        }
    }

    // ------------------------------------------------------------- الفيزياء

    private void frame() {
        long now = System.nanoTime();
        float dt = Math.min(0.033f, (now - last) / 1e9f);
        last = now;
        time += dt;
        if (ph2 <= 0) return;
        touchK += ((touching ? 1f : 0f) - touchK) * Math.min(1f, dt * 8f);
        if (Motion.reduced) {
            // تخفيف الحركة: تتوقف الحلقة بعد اللمس بثوانٍ قليلة
            if (!touching && rt < 0f) {
                idleT += dt;
                if (idleT > 1.4f) {
                    loop.cancel();
                    for (int i = 0; i < getChildCount(); i++) getChildAt(i).invalidate();
                    return;
                }
            } else {
                idleT = 0f;
            }
        }
        if (rt >= 0f) {
            rt += dt / 0.75f;
            if (rt >= 1f) rt = -1f;
        }
        if (goo) {
            float span = Math.max(0f, pw / 2f - ph2 / 2f);
            for (int i = 0; i < N; i++) {
                Goo.Blob b = drops[i];
                float side = (i % 2 == 0) ? -1f : 1f;
                float wob = 4f * d * (float) Math.sin(time * 1.9f + ph[i]);
                if (touching) {
                    // القطرات تتجمع نحو الإصبع فتنتفخ الحواف عنده
                    b.tx = Math.max(pad, Math.min(getWidth() - pad, touchX)) + (i - 1.5f) * 6f * d;
                    b.tr = 0.36f * ph2;
                } else {
                    b.tx = cx + span * (float) Math.sin(time * spd[i] * 0.8f + ph[i]);
                    b.tr = 0.30f * ph2 + wob * 0.5f;
                }
                b.ty = cy + side * (ph2 / 2f - 8f * d + wob);
                b.step(dt, 110f, 9f);
            }
            if (touching) {
                tail.tx = Math.max(pad, Math.min(getWidth() - pad, touchX));
                tail.ty = Math.max(pad * 0.6f, Math.min(getHeight() - pad * 0.6f, touchY));
                tail.tr = 0.46f * ph2;
            } else {
                tail.tx = cx;
                tail.ty = cy;
                tail.tr = 0f;
            }
            tail.step(dt, 240f, 17f);
        }
        for (int i = 0; i < getChildCount(); i++) getChildAt(i).invalidate();
    }

    // ---------------------------------------------------------------- الرسم

    private void drawBase(Canvas cv) {
        cv.save();
        cv.scale(sx, sy, cx, cy);
        cv.drawPath(shape, fill);
        if (goo) {
            for (Goo.Blob b : drops) if (b.r > 0.5f) cv.drawCircle(b.x, b.y, b.r, fill);
            if (tail.r > 0.5f) cv.drawCircle(tail.x, tail.y, tail.r, fill);
        }
        cv.restore();
    }

    private void drawFx(Canvas cv) {
        if (shape.isEmpty()) return;
        cv.save();
        cv.scale(sx, sy, cx, cy);
        cv.save();
        cv.clipPath(shape);
        if (st.fx != Style.FX_NEON) cv.drawRect(rect.left, rect.top, rect.right, cy, shinePaint);
        switch (st.fx) {
            case Style.FX_SHEEN:
                drawSheen(cv, 3.4f, 1f);
                break;
            case Style.FX_GLASS:
                drawSheen(cv, 5.2f, 0.7f);
                drawRipple(cv);
                break;
            case Style.FX_RIPPLE:
                drawWave(cv);
                drawRipple(cv);
                break;
            case Style.FX_NEON:
                drawRipple(cv);
                break;
            default:
                break;
        }
        if (st.fx == Style.FX_SHEEN) drawRipple(cv);
        cv.restore();
        // الإطار
        if (st.fx == Style.FX_GLASS) {
            fx.setStyle(Paint.Style.STROKE);
            fx.setStrokeWidth(1.2f * d);
            fx.setColor(0x77FFFFFF);
            cv.drawPath(shape, fx);
        } else if (st.fx == Style.FX_NEON) {
            drawNeon(cv);
        }
        cv.restore();
    }

    /** شريط لمعان مائل يعبر الزر بين فترة وأخرى. */
    private void drawSheen(Canvas cv, float period, float strength) {
        float p = (time % period) / 1.25f;
        if (p >= 1f) return;
        float e = p * p * (3f - 2f * p);
        float x = rect.left - pw * 0.3f + pw * 1.6f * e;
        float bw = pw * 0.22f, skew = ph2 * 0.5f;
        fx.setStyle(Paint.Style.FILL);
        for (int k = 0; k < 2; k++) {
            float w2 = bw * (k == 0 ? 1.8f : 1f);
            int a = (int) ((k == 0 ? 0x1A : 0x38) * strength);
            fx.setColor((a << 24) | 0xFFFFFF);
            tmp.reset();
            tmp.moveTo(x - w2 / 2f + skew, rect.top);
            tmp.lineTo(x + w2 / 2f + skew, rect.top);
            tmp.lineTo(x + w2 / 2f - skew, rect.bottom);
            tmp.lineTo(x - w2 / 2f - skew, rect.bottom);
            tmp.close();
            cv.drawPath(tmp, fx);
        }
    }

    /** موجتان تتحركان داخل الزر وتهتزان عند اللمس. */
    private void drawWave(Canvas cv) {
        fx.setStyle(Paint.Style.FILL);
        for (int k = 0; k < 2; k++) {
            tmp.reset();
            float base = cy + ph2 * (0.16f - 0.10f * k);
            float amp = ph2 * (0.07f + 0.02f * k) * (1f + 1.5f * touchK);
            tmp.moveTo(rect.left, rect.bottom);
            int steps = 18;
            for (int i = 0; i <= steps; i++) {
                float f = i / (float) steps;
                float x = rect.left + pw * f;
                float y = base + amp * (float) Math.sin(f * 6.2832f * 1.4f + time * (1.7f + 0.8f * k) + k * 2f);
                tmp.lineTo(x, y);
            }
            tmp.lineTo(rect.right, rect.bottom);
            tmp.close();
            fx.setColor(k == 0 ? 0x2CFFFFFF : 0x1CFFFFFF);
            cv.drawPath(tmp, fx);
        }
    }

    /** تموّج ينتشر من موضع اللمس. */
    private void drawRipple(Canvas cv) {
        if (rt < 0f) return;
        float e = 1f - (1f - rt) * (1f - rt);
        float maxR = (float) Math.hypot(pw, ph2);
        fx.setStyle(Paint.Style.FILL);
        fx.setColor(((int) (90 * (1f - rt)) << 24) | 0xFFFFFF);
        cv.drawCircle(rx, ry, maxR * e, fx);
        fx.setStyle(Paint.Style.STROKE);
        fx.setStrokeWidth(2f * d);
        fx.setColor(((int) (160 * (1f - rt)) << 24) | 0xFFFFFF);
        cv.drawCircle(rx, ry, maxR * e, fx);
    }

    /** إطار نيون متوهج مع شرارة تدور حوله. */
    private void drawNeon(Canvas cv) {
        float pulse = 0.85f + 0.15f * (float) Math.sin(time * 2.2f);
        float k = Math.min(1f, pulse + 0.6f * touchK);
        float[] wd = {7f, 4.5f, 2.4f, 1.3f};
        int[] al = {0x14, 0x26, 0x66, 0xFF};
        fx.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < wd.length; i++) {
            fx.setStrokeWidth(wd[i] * d);
            fx.setColor(Theme.a(th.accent, (int) (al[i] * k)));
            cv.drawPath(shape, fx);
        }
        if (pathLen > 1f) {
            float seg = pathLen * 0.14f;
            float s0 = ((time * (0.3f + 0.5f * touchK)) % 1f) * pathLen;
            float s1 = s0 + seg;
            tmp.reset();
            if (s1 <= pathLen) {
                pm.getSegment(s0, s1, tmp, true);
            } else {
                pm.getSegment(s0, pathLen, tmp, true);
                pm.getSegment(0f, s1 - pathLen, tmp, true);
            }
            fx.setStrokeWidth(2.8f * d);
            fx.setColor(th.accentLight);
            cv.drawPath(tmp, fx);
        }
    }

    // ----------------------------------------------------------------- لمس

    private void animateScale(final float tx, final float ty, long dur, Interpolator ip) {
        if (scaleAnim != null) scaleAnim.cancel();
        final float sx0 = sx, sy0 = sy;
        scaleAnim = ValueAnimator.ofFloat(0f, 1f);
        scaleAnim.setDuration(dur);
        scaleAnim.setInterpolator(ip);
        scaleAnim.addUpdateListener(a -> {
            float k = (float) a.getAnimatedValue();
            sx = sx0 + (tx - sx0) * k;
            sy = sy0 + (ty - sy0) * k;
            label.setScaleX(sx);
            label.setScaleY(sy);
        });
        scaleAnim.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touching = true;
                touchX = e.getX();
                touchY = e.getY();
                idleT = 0f;
                if (!loop.isStarted()) {
                    last = System.nanoTime();
                    loop.start();
                }
                if (goo) {
                    tail.x = touchX;
                    tail.y = touchY;
                    if (st.press == Style.PR_SQUASH) {
                        animateScale(1.05f, 0.92f, 110, new DecelerateInterpolator());
                    } else {
                        animateScale(0.96f, 0.96f, 110, new LinearInterpolator());
                    }
                } else {
                    rx = touchX;
                    ry = touchY;
                    rt = 0f;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                touchX = e.getX();
                touchY = e.getY();
                break;
            case MotionEvent.ACTION_UP:
                touching = false;
                if (goo) {
                    for (int i = 0; i < N; i++) {
                        drops[i].vy += ((i % 2 == 0) ? -1f : 1f) * ph2 * 9f;
                        drops[i].vx += (drops[i].x - cx) * 3f;
                    }
                    if (st.press == Style.PR_SQUASH) {
                        animateScale(1f, 1f, 560, Motion.SPR_WILD);
                    } else {
                        animateScale(1f, 1f, 380, new OvershootInterpolator(3f));
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                touching = false;
                if (goo) animateScale(1f, 1f, 380, new OvershootInterpolator(3f));
                break;
            default:
                break;
        }
        return super.onTouchEvent(e);
    }
}
