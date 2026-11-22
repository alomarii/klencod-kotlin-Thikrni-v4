package com.alikhwa.thikrni;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

/**
 * زر الصلاة على النبي. شكله وحركته يتبدّلان مع الثيم:
 * سائل Venom (زمردي/بنفسجي)، نجمة ثمانية دوّارة (ذهبي)، معيّن نابض (ياقوتي)،
 * قرص بماء يتماوج (فيروزي)، حلقة نيون (فحمي)، شمس بأشعة (غروب)، قرص زجاجي بأقمار تدور (أزرق ليلي).
 */
public class GooCounterView extends FrameLayout {
    private static final int N = 7;

    private final Theme th;
    private final Style st;
    private final int ct;
    private final float d;
    private final Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint misc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rr = new RectF();
    private final Path tmp = new Path();
    private final Path shapePath = new Path();

    private final Goo.Blob[] drops = new Goo.Blob[N];
    private final float[] ang = new float[N], rho = new float[N], spd = new float[N],
            ph = new float[N], rad = new float[N];
    private final Goo.Blob touch = new Goo.Blob(0, 0, 0);
    private boolean touching;
    private float touchX, touchY;

    // شرارات تتطاير عند كل ضغطة + "+١" صاعدة
    private static final int NS = 18;
    private final float[] sx = new float[NS], sy = new float[NS], svx = new float[NS], svy = new float[NS],
            slife = new float[NS], ssize = new float[NS], srot = new float[NS];
    private final Path star = new Path();
    private final Path diamond = new Path();
    private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float tpT = -1f, tpX, tpY;

    private float cx, cy, r0, press = 1f, pulse = 0f, burst = 1f, time = 0f;
    private float spin = 0f, boost = 0f, touchPow = 0f, idleT = 0f;
    private ValueAnimator loop, burstAnim, pressAnim;
    private long last;

    public GooCounterView(Context c, Theme theme) {
        super(c);
        th = theme;
        st = theme.st;
        ct = st.counter;
        d = c.getResources().getDisplayMetrics().density;
        setClipChildren(false);

        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(2f * d);
        ring.setStrokeCap(Paint.Cap.ROUND);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setColor(ct == Style.CT_NEON ? th.accent : th.onAccent);
        if (ct == Style.CT_NEON) text.setShadowLayer(12f * d, 0f, 0f, th.accent);
        misc.setStrokeCap(Paint.Cap.ROUND);

        float[] rhos = {1.00f, 1.08f, 0.96f, 1.18f, 1.02f, 1.28f, 1.12f};
        float[] spds = {0.55f, -0.40f, 0.70f, -0.62f, 0.33f, 0.48f, -0.52f};
        float[] rads = {0.24f, 0.19f, 0.27f, 0.15f, 0.22f, 0.13f, 0.17f};
        for (int i = 0; i < N; i++) {
            ang[i] = (float) (i * 2 * Math.PI / N);
            rho[i] = rhos[i];
            spd[i] = spds[i];
            rad[i] = rads[i];
            ph[i] = i * 1.3f;
            drops[i] = new Goo.Blob(0, 0, 0);
        }

        View back = new View(c) {
            @Override
            protected void onDraw(Canvas cv) {
                drawBack(cv);
            }
        };
        View gooLayer = new View(c) {
            @Override
            protected void onDraw(Canvas cv) {
                drawGoo(cv);
            }
        };
        View front = new View(c) {
            @Override
            protected void onDraw(Canvas cv) {
                drawFront(cv);
            }
        };
        addView(back, new LayoutParams(-1, -1));
        addView(gooLayer, new LayoutParams(-1, -1));
        addView(front, new LayoutParams(-1, -1));
        if (ct == Style.CT_GOO) Goo.apply(gooLayer, 8f * d);
        setClickable(true);

        fx.setTextAlign(Paint.Align.CENTER);
        fx.setTypeface(Typeface.DEFAULT_BOLD);
        star.moveTo(0f, -1f);
        star.lineTo(0.22f, -0.22f);
        star.lineTo(1f, 0f);
        star.lineTo(0.22f, 0.22f);
        star.lineTo(0f, 1f);
        star.lineTo(-0.22f, 0.22f);
        star.lineTo(-1f, 0f);
        star.lineTo(-0.22f, -0.22f);
        star.close();
        diamond.moveTo(0f, -1.3f);
        diamond.lineTo(0.85f, 0f);
        diamond.lineTo(0f, 1.3f);
        diamond.lineTo(-0.85f, 0f);
        diamond.close();

        loop = ValueAnimator.ofFloat(0f, 1f);
        loop.setDuration(1000);
        loop.setRepeatCount(ValueAnimator.INFINITE);
        loop.setInterpolator(new LinearInterpolator());
        loop.addUpdateListener(a -> frame());
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        cx = w / 2f;
        cy = h / 2f;
        r0 = Math.min(w, h) / 2f * 0.56f;
        disc.setShader(new RadialGradient(cx - r0 * 0.3f, cy - r0 * 0.35f, r0 * 1.3f,
                new int[]{th.accentLight, th.accent, th.accentDark},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        if (ct == Style.CT_DIAMOND) disc.setPathEffect(new CornerPathEffect(r0 * 0.18f));
        glow.setShader(new RadialGradient(cx, cy, r0 * 1.7f,
                Theme.a(th.accent, 0x66), Theme.a(th.accent, 0x00), Shader.TileMode.CLAMP));
        for (int i = 0; i < N; i++) {
            Goo.Blob b = drops[i];
            b.x = b.tx = cx + (float) Math.cos(ang[i]) * rho[i] * r0;
            b.y = b.ty = cy + (float) Math.sin(ang[i]) * rho[i] * r0;
            b.r = b.tr = rad[i] * r0;
        }
        touch.x = touch.tx = cx;
        touch.y = touch.ty = cy;
        touch.r = touch.tr = 0f;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        last = System.nanoTime();
        if (!loop.isStarted()) loop.start();
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

    // ------------------------------------------------------------ الفيزياء

    private void frame() {
        long now = System.nanoTime();
        float dt = Math.min(0.033f, (now - last) / 1e9f);
        last = now;
        time += dt;
        pulse = (pulse + dt / 2.8f) % 1f;
        boost = Math.max(0f, boost - dt * 1.3f);
        spin += dt * (14f + 260f * boost * boost + (touching ? 40f : 0f));
        touchPow += ((touching ? 1f : 0f) - touchPow) * Math.min(1f, dt * 10f);
        if (r0 <= 0) return;
        if (Motion.reduced) {
            if (!touching && burst >= 1f && tpT < 0f && boost <= 0f) {
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

        if (ct == Style.CT_GOO) {
            for (int i = 0; i < N; i++) {
                Goo.Blob b = drops[i];
                float a = ang[i] + time * spd[i];
                float rr0 = r0 * (rho[i] + 0.10f * (float) Math.sin(time * 1.7f + ph[i]));
                b.tx = cx + (float) Math.cos(a) * rr0;
                b.ty = cy + (float) Math.sin(a) * rr0;
                b.tr = r0 * rad[i] * (1f + 0.15f * (float) Math.sin(time * 2.3f + ph[i]));
                b.step(dt, 90f, 8f);
            }
            if (touching) {
                float dx = touchX - cx, dy = touchY - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float reach = r0 * (0.75f + 0.55f * Math.min(1f, dist / r0));
                if (dist > 1f) {
                    touch.tx = cx + dx / dist * reach;
                    touch.ty = cy + dy / dist * reach;
                } else {
                    touch.tx = cx;
                    touch.ty = cy - reach;
                }
                touch.tr = r0 * 0.34f;
            } else {
                touch.tx = cx;
                touch.ty = cy;
                touch.tr = 0f;
            }
            touch.step(dt, 220f, 16f);
        }

        for (int i = 0; i < NS; i++) {
            if (slife[i] <= 0f) continue;
            slife[i] -= dt * 1.1f;
            svy[i] += st.sparkGravity * d * dt;
            sx[i] += svx[i] * dt;
            sy[i] += svy[i] * dt;
            srot[i] += 160f * dt;
        }
        if (tpT >= 0f) {
            tpT += dt / 0.95f;
            if (tpT >= 1f) tpT = -1f;
        }

        for (int i = 0; i < getChildCount(); i++) getChildAt(i).invalidate();
    }

    /** شرارات تنطلق من حافة الزر (بشكل يناسب الثيم) وكلمة "+١" تصعد من موضع الإصبع. */
    private void sparkle() {
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < NS; i++) {
            float a = (float) (r.nextFloat() * 2 * Math.PI);
            float sp = (110f + r.nextFloat() * 200f) * d;
            sx[i] = cx + (float) Math.cos(a) * r0 * 0.95f;
            sy[i] = cy + (float) Math.sin(a) * r0 * 0.95f;
            svx[i] = (float) Math.cos(a) * sp;
            svy[i] = (float) Math.sin(a) * sp - 90f * d;
            slife[i] = 0.7f + r.nextFloat() * 0.5f;
            ssize[i] = (3f + r.nextFloat() * 5f) * d;
            srot[i] = r.nextFloat() * 90f;
        }
        tpT = 0f;
        tpX = touchX;
        tpY = Math.min(touchY, cy - r0 * 0.2f);
    }

    private void splash() {
        if (ct != Style.CT_GOO) return;
        for (int i = 0; i < N; i++) {
            Goo.Blob b = drops[i];
            float dx = b.x - cx, dy = b.y - cy;
            float len = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
            float sp = r0 * 7f;
            b.vx += dx / len * sp;
            b.vy += dy / len * sp;
        }
    }

    // --------------------------------------------------------------- الرسم

    private void diamondPath(Path p, float R, float k) {
        p.reset();
        float hw = R * 1.02f * k, hh = R * 1.2f * k;
        p.moveTo(cx, cy - hh);
        p.lineTo(cx + hw, cy);
        p.lineTo(cx, cy + hh);
        p.lineTo(cx - hw, cy);
        p.close();
    }

    private void drawBack(Canvas cv) {
        float R = r0 * press;
        switch (ct) {
            case Style.CT_SUN: {
                // أشعة الشمس
                misc.setStyle(Paint.Style.STROKE);
                for (int k = 0; k < 16; k++) {
                    float a = (float) Math.toRadians(k * 22.5f + spin * 0.25f);
                    float len = R * (0.26f + 0.10f * (float) Math.sin(time * 2f + k) + 0.28f * boost);
                    float r1 = R * 1.1f, r2 = r1 + len * (k % 2 == 0 ? 1f : 0.6f);
                    misc.setStrokeWidth((k % 2 == 0 ? 3.4f : 2.2f) * d);
                    misc.setColor(Theme.a(th.accentLight, k % 2 == 0 ? 0xDD : 0x99));
                    cv.drawLine(cx + (float) Math.cos(a) * r1, cy + (float) Math.sin(a) * r1,
                            cx + (float) Math.cos(a) * r2, cy + (float) Math.sin(a) * r2, misc);
                }
                break;
            }
            case Style.CT_NEON: {
                float k = 0.85f + 0.15f * (float) Math.sin(time * 2.4f);
                float[] wd = {10f, 6.5f, 3.4f, 1.8f};
                int[] al = {0x14, 0x26, 0x66, 0xFF};
                misc.setStyle(Paint.Style.STROKE);
                for (int i = 0; i < wd.length; i++) {
                    misc.setStrokeWidth(wd[i] * d);
                    misc.setColor(Theme.a(th.accent, (int) (al[i] * Math.min(1f, k + 0.4f * touchPow))));
                    cv.drawCircle(cx, cy, R, misc);
                }
                // قوسان متقابلان يدوران
                rr.set(cx - R * 1.16f, cy - R * 1.16f, cx + R * 1.16f, cy + R * 1.16f);
                misc.setStrokeWidth(3f * d);
                misc.setColor(th.accentLight);
                cv.drawArc(rr, spin, 64f, false, misc);
                cv.drawArc(rr, spin + 180f, 64f, false, misc);
                break;
            }
            case Style.CT_STAR8: {
                // مربعان متداخلان كأصداء دوّارة
                misc.setStyle(Paint.Style.STROKE);
                misc.setStrokeWidth(1.6f * d);
                for (int k = 0; k < 2; k++) {
                    float f = (pulse + k * 0.5f) % 1f;
                    misc.setColor(Theme.a(th.accent, (int) ((1f - f) * 120)));
                    float s = R * (0.95f + 0.42f * f);
                    for (int m = 0; m < 2; m++) {
                        cv.save();
                        cv.rotate(spin * 0.12f + m * 45f, cx, cy);
                        rr.set(cx - s, cy - s, cx + s, cy + s);
                        cv.drawRect(rr, misc);
                        cv.restore();
                    }
                }
                break;
            }
            case Style.CT_DIAMOND: {
                // أصداء معيّنية تتسع
                misc.setStyle(Paint.Style.STROKE);
                for (int k = 0; k < 3; k++) {
                    float f = (pulse + k / 3f) % 1f;
                    misc.setStrokeWidth(2f * d);
                    misc.setColor(Theme.a(th.accent, (int) ((1f - f) * 120)));
                    diamondPath(tmp, R, 1.12f + 0.5f * f);
                    cv.drawPath(tmp, misc);
                }
                break;
            }
            default: {
                for (int k = 0; k < 3; k++) {
                    float f = (pulse + k / 3f) % 1f;
                    ring.setColor(Theme.a(th.accent, (int) ((1f - f) * 110)));
                    ring.setStrokeWidth(2f * d);
                    cv.drawCircle(cx, cy, R * (1.1f + 0.55f * f), ring);
                }
                break;
            }
        }
        if (burst < 1f) {
            ring.setStrokeWidth(4f * d * (1f - burst) + 1f);
            ring.setColor(((int) ((1f - burst) * 230) << 24) | 0xFFFFFF);
            if (ct == Style.CT_DIAMOND) {
                diamondPath(tmp, R, 1f + 0.9f * burst);
                cv.drawPath(tmp, ring);
            } else if (ct == Style.CT_STAR8) {
                float s = R * (0.8f + 0.75f * burst);
                for (int m = 0; m < 2; m++) {
                    cv.save();
                    cv.rotate(spin * 0.12f + m * 45f, cx, cy);
                    rr.set(cx - s, cy - s, cx + s, cy + s);
                    cv.drawRect(rr, ring);
                    cv.restore();
                }
            } else {
                cv.drawCircle(cx, cy, R * (1f + 0.9f * burst), ring);
            }
        }
        cv.drawCircle(cx, cy, r0 * 1.7f, glow);
    }

    /** الطبقة الرئيسية: القرص بشكل الثيم (مع دمج سائل في الثيمات السائلة فقط). */
    private void drawGoo(Canvas cv) {
        cv.save();
        float k = press;
        if (ct == Style.CT_DIAMOND) k *= 1f + 0.025f * (float) Math.sin(time * 2.6f) + 0.05f * boost;
        cv.scale(k, k, cx, cy);
        switch (ct) {
            case Style.CT_STAR8: {
                float s = r0 * 0.80f;
                rr.set(cx - s, cy - s, cx + s, cy + s);
                for (int m = 0; m < 2; m++) {
                    cv.save();
                    cv.rotate(spin * 0.12f + m * 45f, cx, cy);
                    cv.drawRoundRect(rr, s * 0.12f, s * 0.12f, disc);
                    cv.restore();
                }
                break;
            }
            case Style.CT_DIAMOND:
                diamondPath(shapePath, r0, 1f);
                cv.drawPath(shapePath, disc);
                break;
            case Style.CT_NEON:
                misc.setStyle(Paint.Style.FILL);
                misc.setColor(0x33000000);
                cv.drawCircle(cx, cy, r0, misc);
                misc.setColor(0x14FFFFFF);
                cv.drawCircle(cx, cy, r0, misc);
                break;
            case Style.CT_GOO:
                cv.drawCircle(cx, cy, r0, disc);
                for (Goo.Blob b : drops) if (b.r > 0.5f) cv.drawCircle(b.x, b.y, b.r, disc);
                if (touch.r > 0.5f) cv.drawCircle(touch.x, touch.y, touch.r, disc);
                break;
            default:
                cv.drawCircle(cx, cy, r0, disc);
                break;
        }
        cv.restore();
    }

    private void drawFront(Canvas cv) {
        float R = r0 * press;
        ring.setStrokeWidth(2f * d);

        // تأثير خاص بكل ثيم فوق القرص
        switch (ct) {
            case Style.CT_WAVE:
                drawWater(cv, R);
                ring.setColor(0x66FFFFFF);
                cv.drawCircle(cx, cy, R * 0.93f, ring);
                break;
            case Style.CT_ORBIT:
                drawOrbit(cv, R);
                break;
            case Style.CT_STAR8: {
                ring.setColor(0x66FFF6D0);
                for (int m = 0; m < 2; m++) {
                    cv.save();
                    cv.rotate(-spin * 0.2f + m * 45f, cx, cy);
                    float s = R * 0.62f;
                    rr.set(cx - s, cy - s, cx + s, cy + s);
                    cv.drawRect(rr, ring);
                    cv.restore();
                }
                cv.drawCircle(cx, cy, R * 0.93f * 0.86f, ring);
                break;
            }
            case Style.CT_DIAMOND:
                ring.setColor(0x66FFF6D0);
                diamondPath(tmp, R, 0.86f);
                cv.drawPath(tmp, ring);
                break;
            case Style.CT_NEON:
                break;
            default:
                ring.setColor(0x66FFF6D0);
                cv.drawCircle(cx, cy, R * 0.93f, ring);
                break;
        }

        // وهج تحت الإصبع في الثيمات غير السائلة
        if (ct != Style.CT_GOO && touchPow > 0.02f) {
            misc.setStyle(Paint.Style.FILL);
            float[] rad2 = {0.55f, 0.40f, 0.25f};
            int[] a2 = {0x10, 0x1A, 0x26};
            for (int i = 0; i < 3; i++) {
                misc.setColor(((int) (a2[i] * touchPow) << 24) | 0xFFFFFF);
                cv.drawCircle(touchX, touchY, R * rad2[i], misc);
            }
        }

        text.setTextSize(R * 0.34f);
        cv.drawText(L.t("صلِّ على"), cx, cy - R * 0.05f, text);
        text.setTextSize(R * 0.40f);
        cv.drawText(L.t("النبي"), cx, cy + R * 0.38f, text);
        text.setTextSize(R * 0.16f);
        cv.drawText(L.t("اضغط للعدّ"), cx, cy - R * 0.45f, text);

        // الشرارات
        for (int i = 0; i < NS; i++) {
            if (slife[i] <= 0f) continue;
            drawSpark(cv, i);
        }
        // "+١" الصاعدة
        if (tpT >= 0f) {
            float e = 1f - (1f - tpT) * (1f - tpT);
            fx.setStyle(Paint.Style.FILL);
            fx.setColor(Theme.a(th.accentLight, (int) (255 * (1f - tpT * tpT))));
            fx.setTextSize(26f * d * (0.8f + 0.4f * (1f - tpT)));
            cv.drawText(L.digits("+١"), tpX, tpY - 80f * d * e, fx);
        }
    }

    private void drawSpark(Canvas cv, int i) {
        int a = (int) (255 * Math.min(1f, slife[i] * 1.8f));
        int col = Theme.a(i % 3 == 0 ? 0xFFFFFF : th.accentLight, a);
        fx.setColor(col);
        fx.setStyle(Paint.Style.FILL);
        cv.save();
        cv.translate(sx[i], sy[i]);
        cv.rotate(srot[i]);
        float s = ssize[i];
        cv.scale(s, s);
        switch (st.spark) {
            case Style.SP_DIAMOND:
                cv.drawPath(diamond, fx);
                break;
            case Style.SP_PETAL:
                rr.set(-1.3f, -0.6f, 1.3f, 0.6f);
                cv.drawOval(rr, fx);
                break;
            case Style.SP_GLOW:
                fx.setColor(Theme.a(th.accentLight, a / 4));
                cv.drawCircle(0f, 0f, 2.2f, fx);
                fx.setColor(col);
                cv.drawCircle(0f, 0f, 0.8f, fx);
                break;
            case Style.SP_BUBBLE:
                fx.setStyle(Paint.Style.STROKE);
                fx.setStrokeWidth(0.24f);
                cv.drawCircle(0f, 0f, 1.1f, fx);
                fx.setStyle(Paint.Style.FILL);
                fx.setColor(Theme.a(0xFFFFFF, a));
                cv.drawCircle(-0.4f, -0.4f, 0.22f, fx);
                break;
            case Style.SP_SQUARE:
                rr.set(-0.8f, -0.8f, 0.8f, 0.8f);
                cv.drawRect(rr, fx);
                break;
            case Style.SP_SNOW:
                fx.setStyle(Paint.Style.STROKE);
                fx.setStrokeWidth(0.22f);
                fx.setStrokeCap(Paint.Cap.ROUND);
                for (int m = 0; m < 3; m++) {
                    cv.save();
                    cv.rotate(m * 60f);
                    cv.drawLine(-1.1f, 0f, 1.1f, 0f, fx);
                    cv.restore();
                }
                break;
            case Style.SP_STAR:
            default:
                cv.drawPath(star, fx);
                break;
        }
        cv.restore();
        fx.setStyle(Paint.Style.FILL);
    }

    /** ماء يتماوج داخل القرص ويرتفع/يهتز عند الضغط. */
    private void drawWater(Canvas cv, float R) {
        tmp.reset();
        tmp.addCircle(cx, cy, R * 0.97f, Path.Direction.CW);
        cv.save();
        cv.clipPath(tmp);
        float level = cy + R * 0.16f - boost * R * 0.12f;
        float amp = R * (0.05f + 0.11f * boost + 0.04f * touchPow);
        misc.setStyle(Paint.Style.FILL);
        for (int layer = 0; layer < 2; layer++) {
            shapePath.reset();
            shapePath.moveTo(cx - R, cy + R);
            int steps = 16;
            for (int i = 0; i <= steps; i++) {
                float f = i / (float) steps;
                float x = cx - R + 2f * R * f;
                float y = level - layer * R * 0.06f
                        + amp * (float) Math.sin(f * 6.2832f * 1.3f + time * (2.0f + 0.9f * layer) + layer * 2.1f);
                shapePath.lineTo(x, y);
            }
            shapePath.lineTo(cx + R, cy + R);
            shapePath.close();
            misc.setColor(layer == 0 ? 0x66FFFFFF : 0x40FFFFFF);
            cv.drawPath(shapePath, misc);
        }
        cv.restore();
    }

    /** ثلاثة أقمار تدور حول القرص الزجاجي بمدارات مائلة وذيول متلاشية. */
    private void drawOrbit(Canvas cv, float R) {
        // لمعة زجاجية
        ring.setColor(0x88FFFFFF);
        ring.setStrokeWidth(3f * d);
        rr.set(cx - R * 0.86f, cy - R * 0.86f, cx + R * 0.86f, cy + R * 0.86f);
        cv.drawArc(rr, 200f, 70f, false, ring);
        ring.setStrokeWidth(2f * d);
        ring.setColor(0x55FFFFFF);
        cv.drawCircle(cx, cy, R * 0.97f, ring);

        float[] tilt = {-28f, 24f, 78f};
        float[] sp = {1.0f, -1.35f, 0.75f};
        for (int k = 0; k < 3; k++) {
            cv.save();
            cv.rotate(tilt[k], cx, cy);
            float a = R * 1.30f, b = R * 0.42f;
            rr.set(cx - a, cy - b, cx + a, cy + b);
            ring.setStrokeWidth(1f * d);
            ring.setColor(Theme.a(th.accentLight, 0x33));
            cv.drawOval(rr, ring);
            float base = time * sp[k] * (1f + 2.2f * boost) + k * 2.1f;
            misc.setStyle(Paint.Style.FILL);
            for (int j = 9; j >= 0; j--) {
                float t2 = base - j * 0.10f * Math.signum(sp[k]);
                float px = cx + a * (float) Math.cos(t2);
                float py = cy + b * (float) Math.sin(t2);
                misc.setColor(Theme.a(j == 0 ? 0xFFFFFF : th.accentLight, j == 0 ? 0xFF : (int) (150 * (1f - j / 10f))));
                cv.drawCircle(px, py, (j == 0 ? 4.2f : 3.2f * (1f - j / 12f)) * d, misc);
            }
            cv.restore();
        }
    }

    // ----------------------------------------------------------------- لمس

    private void animatePress(float to, boolean overshoot) {
        if (pressAnim != null) pressAnim.cancel();
        pressAnim = ValueAnimator.ofFloat(press, to);
        pressAnim.setDuration(overshoot ? 420 : (ct == Style.CT_NEON ? 60 : 120));
        if (overshoot) pressAnim.setInterpolator(new OvershootInterpolator(3f));
        pressAnim.addUpdateListener(a -> press = (float) a.getAnimatedValue());
        pressAnim.start();
    }

    private void burst() {
        if (burstAnim != null) burstAnim.cancel();
        burstAnim = ValueAnimator.ofFloat(0f, 1f);
        burstAnim.setDuration(750);
        burstAnim.addUpdateListener(a -> burst = (float) a.getAnimatedValue());
        burstAnim.start();
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
                animatePress(ct == Style.CT_NEON ? 0.97f : 0.94f, false);
                break;
            case MotionEvent.ACTION_MOVE:
                touchX = e.getX();
                touchY = e.getY();
                break;
            case MotionEvent.ACTION_UP:
                touching = false;
                touchX = e.getX();
                touchY = e.getY();
                idleT = 0f;
                splash();
                sparkle();
                boost = 1f;
                animatePress(1f, true);
                burst();
                break;
            case MotionEvent.ACTION_CANCEL:
                touching = false;
                animatePress(1f, true);
                break;
            default:
                break;
        }
        return super.onTouchEvent(e);
    }
}
