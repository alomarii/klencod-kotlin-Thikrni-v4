package com.alikhwa.thikrni;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;

import java.util.Random;

/**
 * خلفية متحركة تتغير مع الثيم: نجوم وشهب (زمردي)، شفق قطبي (أزرق ليلي)، غبار ذهبي وأشعة (ذهبي)،
 * يراعات (بنفسجي)، بتلات متساقطة (ياقوتي)، فقاعات (فيروزي)، مسح ضوئي هادئ (فحمي)، جمرات وشمس (غروب).
 * وتتحرك الطبقات بتأثير عمق خفيف مع ميل الهاتف.
 */
public class StarsView extends View implements SensorEventListener {
    private static final int N = 80;
    private final float[] x = new float[N], y = new float[N], r = new float[N],
            ph = new float[N], sp = new float[N], drift = new float[N];
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private final Matrix scanM = new Matrix();
    private LinearGradient scanShader;
    private final long t0 = System.nanoTime();
    private final Theme th;
    private final Style st;
    private final float d;
    private final Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random shotRnd = new Random();
    private float nextShot = 2.5f, shotStart = -10f, shx, shy, shdx, shdy;
    private static final float SHOT_DUR = 1.0f;

    private float tiltX, tiltY, tgtX, tgtY;
    private SensorManager sm;

    public StarsView(Context c, Theme theme) {
        super(c);
        th = theme;
        st = theme.st;
        d = c.getResources().getDisplayMetrics().density;
        Random rnd = new Random(11);
        for (int i = 0; i < N; i++) {
            x[i] = rnd.nextFloat();
            y[i] = rnd.nextFloat();
            r[i] = 0.8f + rnd.nextFloat() * 2.2f;
            ph[i] = rnd.nextFloat() * 6.28f;
            sp[i] = 0.8f + rnd.nextFloat() * 2.2f;
            drift[i] = 0.004f + rnd.nextFloat() * 0.012f;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        bgPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{th.bgTop, th.bgMid, th.bgBottom},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        rayPaint.setShader(new LinearGradient(0, 0, 0, h * 0.8f,
                Theme.a(th.glowB, 0x30), Theme.a(th.glowB, 0x00), Shader.TileMode.CLAMP));
        float band = 130f * d;
        scanShader = new LinearGradient(0, 0, 0, band,
                new int[]{0x00FFFFFF, 0x14FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP);
        scanPaint.setShader(scanShader);
        sunPaint.setShader(new RadialGradient(w / 2f, h, w * 1.05f,
                new int[]{Theme.a(th.glowA, 0x88), Theme.a(th.glowA, 0x30), Theme.a(th.glowA, 0x00)},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
    }

    // ------------------------------------------------------ حسّاس الميل

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (Motion.reduced) return;
        sm = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor s = sm != null ? sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        if (s != null) sm.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (sm != null) sm.unregisterListener(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        tgtX = Math.max(-1f, Math.min(1f, -e.values[0] / 4f));
        tgtY = Math.max(-1f, Math.min(1f, (e.values[1] - 6.5f) / 4f));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // --------------------------------------------------------------- الرسم

    @Override
    protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        if (Motion.reduced) {
            // وضع تخفيف الحركة: خلفية ساكنة بلا إعادة رسم مستمرة
            cv.drawRect(0, 0, w, h, bgPaint);
            drawGlows(cv, 0f, w, h);
            drawStars(cv, 0f, w, h, N, 1f);
            return;
        }
        float t = (System.nanoTime() - t0) / 1e9f;
        tiltX += (tgtX - tiltX) * 0.08f;
        tiltY += (tgtY - tiltY) * 0.08f;
        cv.drawRect(0, 0, w, h, bgPaint);
        drawGlows(cv, t, w, h);
        switch (st.ambient) {
            case Style.AM_AURORA:
                drawAurora(cv, t, w, h);
                drawStars(cv, t, w, h, 46, 1f);
                drawShootingStar(cv, t, w, h);
                break;
            case Style.AM_DUST:
                drawRays(cv, t, w, h);
                drawDust(cv, t, w, h);
                break;
            case Style.AM_FIREFLY:
                drawFireflies(cv, t, w, h);
                break;
            case Style.AM_PETALS:
                drawPetals(cv, t, w, h);
                break;
            case Style.AM_BUBBLES:
                drawBubbles(cv, t, w, h);
                break;
            case Style.AM_SCAN:
                drawStars(cv, t, w, h, 30, 0.45f);
                drawScan(cv, t, w, h);
                break;
            case Style.AM_EMBERS:
                drawSun(cv, t, w, h);
                drawEmbers(cv, t, w, h);
                break;
            case Style.AM_STARS:
            default:
                drawStars(cv, t, w, h, N, 1f);
                drawShootingStar(cv, t, w, h);
                break;
        }
        if (HijriUtil.isFridayWindow()) drawFriday(cv, t, w, h);
        postInvalidateOnAnimation();
    }

    /** هالة ذهبية هادئة وأشعة نازلة في ليلة الجمعة ويومها (فوق أي ثيم). */
    private void drawFriday(Canvas cv, float t, int w, int h) {
        glowPaint.setShader(new RadialGradient(w / 2f + tiltX * 10f * d, -h * 0.05f, w * 1.05f,
                Theme.a(0xFFE6C15A, (int) (0x38 + 0x18 * Math.sin(t * 0.7f))), 0x00E6C15A, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, glowPaint);
        dot.setStyle(Paint.Style.STROKE);
        dot.setStrokeWidth(1.4f * d);
        for (int i = 0; i < 7; i++) {
            float a = (float) Math.toRadians(62 + i * 9.5f + 3f * Math.sin(t * 0.4f + i));
            float len = h * (0.35f + 0.1f * (float) Math.sin(t * 0.5f + i * 1.3f));
            dot.setColor(Theme.a(0xFFE6C15A, 0x16));
            cv.drawLine(w / 2f, -10f * d, w / 2f + (float) Math.cos(a) * len, (float) Math.sin(a) * len, dot);
        }
        dot.setStyle(Paint.Style.FILL);
    }

    private void drawGlows(Canvas cv, float t, int w, int h) {
        // توهج علوي
        float g1x = w * (0.5f + 0.18f * (float) Math.sin(t * 0.35f)) + tiltX * 14f * d;
        glowPaint.setShader(new RadialGradient(g1x, h * 0.10f + tiltY * 8f * d, w * 0.95f,
                Theme.a(th.glowA, 0x44), Theme.a(th.glowA, 0x00), Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, glowPaint);

        // توهج ثانٍ متحرك
        float g2x = w * (0.5f + 0.40f * (float) Math.sin(t * 0.22f + 1.3f)) + tiltX * 22f * d;
        float g2y = h * (0.62f + 0.12f * (float) Math.cos(t * 0.27f)) + tiltY * 14f * d;
        glowPaint.setShader(new RadialGradient(g2x, g2y, w * 0.75f,
                Theme.a(th.glowB, 0x22), Theme.a(th.glowB, 0x00), Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, glowPaint);

        // شريط شفقي بطيء
        float bx = w * (0.5f + 0.5f * (float) Math.sin(t * 0.13f + 0.6f));
        glowPaint.setShader(new RadialGradient(bx, h * 0.34f, w * 0.55f,
                Theme.a(th.accent, 0x14), Theme.a(th.accent, 0x00), Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, glowPaint);
    }

    private void drawStars(Canvas cv, float t, int w, int h, int n, float dim) {
        for (int i = 0; i < n; i++) {
            float yy = ((y[i] - t * drift[i]) % 1f + 1f) % 1f;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * sp[i] + ph[i]);
            int a = (int) ((50 + 205 * tw) * dim);
            starPaint.setColor((a << 24) | 0xFFF1C6);
            float px = x[i] * w + tiltX * r[i] * 6f * d, py = yy * h + tiltY * r[i] * 6f * d;
            float rad = r[i] * (0.7f + 0.5f * tw) * d * 0.6f;
            cv.drawCircle(px, py, rad, starPaint);
            if (r[i] > 2.4f) {
                starPaint.setStrokeWidth(1.2f);
                float L = rad * 3.2f * tw;
                cv.drawLine(px - L, py, px + L, py, starPaint);
                cv.drawLine(px, py - L, px, py + L, starPaint);
            }
        }
    }

    /** شهاب يعبر الشاشة كل بضع ثوانٍ. */
    private void drawShootingStar(Canvas cv, float t, int w, int h) {
        if (t >= nextShot && shotStart < 0) {
            shotStart = t;
            shx = w * (0.35f + 0.6f * shotRnd.nextFloat());
            shy = h * (0.04f + 0.25f * shotRnd.nextFloat());
            float ang = (float) Math.toRadians(145 + shotRnd.nextInt(25));
            float speed = w * 0.95f;
            shdx = (float) Math.cos(ang) * speed;
            shdy = (float) Math.sin(ang) * speed;
        }
        if (shotStart < 0) return;
        float k = (t - shotStart) / SHOT_DUR;
        if (k >= 1f) {
            shotStart = -10f;
            nextShot = t + 5f + shotRnd.nextFloat() * 8f;
            return;
        }
        float hx = shx + shdx * k * SHOT_DUR, hy = shy + shdy * k * SHOT_DUR;
        float fade = (float) Math.sin(Math.PI * k);
        int segs = 14;
        float len = 0.16f;
        trailPaint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < segs; i++) {
            float f0 = i / (float) segs, f1 = (i + 1) / (float) segs;
            float ax = hx - shdx * len * f0, ay = hy - shdy * len * f0;
            float bx = hx - shdx * len * f1, by = hy - shdy * len * f1;
            int a = (int) (230 * fade * (1f - f0));
            trailPaint.setColor((a << 24) | 0xFFFFFF);
            trailPaint.setStrokeWidth(Math.max(1f, (2.6f - 2.2f * f0) * d));
            cv.drawLine(ax, ay, bx, by, trailPaint);
        }
        trailPaint.setColor(((int) (255 * fade) << 24) | 0xFFFFFF);
        cv.drawCircle(hx, hy, 2.2f * d, trailPaint);
    }

    // ------------------------------------------------------------- شفق قطبي

    private void drawAurora(Canvas cv, float t, int w, int h) {
        int[] cols = {th.glowA, th.accent, th.glowB};
        int steps = 20;
        dot.setStyle(Paint.Style.FILL);
        for (int b = 0; b < 3; b++) {
            float baseY = h * (0.14f + 0.12f * b) + tiltY * 10f * d;
            float amp = h * (0.035f + 0.01f * b);
            float spd = 0.30f + 0.11f * b;
            for (int layer = 0; layer < 4; layer++) {
                float thick = h * 0.022f * (layer + 1);
                path.reset();
                for (int i = 0; i <= steps; i++) {
                    float f = i / (float) steps;
                    float px = w * f + tiltX * 12f * d;
                    float py = baseY + amp * (float) Math.sin(f * 7f + t * spd + b * 1.7f)
                            + amp * 0.5f * (float) Math.sin(f * 14.7f - t * spd * 1.3f);
                    if (i == 0) path.moveTo(px, py);
                    else path.lineTo(px, py);
                }
                for (int i = steps; i >= 0; i--) {
                    float f = i / (float) steps;
                    float px = w * f + tiltX * 12f * d;
                    float py = baseY + thick + amp * (float) Math.sin(f * 7f + t * spd + b * 1.7f + 0.4f)
                            + amp * 0.5f * (float) Math.sin(f * 14.7f - t * spd * 1.3f);
                    path.lineTo(px, py);
                }
                path.close();
                dot.setColor(Theme.a(cols[b], 0x1E - layer * 4));
                cv.drawPath(path, dot);
            }
        }
    }

    // ----------------------------------------------------------- غبار ذهبي

    private void drawRays(Canvas cv, float t, int w, int h) {
        for (int i = 0; i < 3; i++) {
            float cx = w * (0.2f + 0.3f * i) + (float) Math.sin(t * 0.25f + i * 2f) * w * 0.08f + tiltX * 12f * d;
            float a = w * 0.03f, b = w * 0.13f, skew = w * 0.18f, hh = h * 0.8f;
            path.reset();
            path.moveTo(cx - a, 0);
            path.lineTo(cx + a, 0);
            path.lineTo(cx + b + skew, hh);
            path.lineTo(cx - b + skew, hh);
            path.close();
            rayPaint.setAlpha((int) (130 + 100 * Math.sin(t * 0.6f + i * 1.9f)));
            cv.drawPath(path, rayPaint);
        }
    }

    private void drawDust(Canvas cv, float t, int w, int h) {
        dot.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 46; i++) {
            float yy = ((y[i] - t * drift[i] * 1.4f) % 1f + 1f) % 1f;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * sp[i] + ph[i]);
            float px = x[i] * w + (float) Math.sin(t * sp[i] * 0.4f + ph[i]) * 14f * d + tiltX * r[i] * 7f * d;
            float py = yy * h + tiltY * r[i] * 7f * d;
            float rad = r[i] * (0.6f + 0.5f * tw) * d * 0.55f;
            dot.setColor(Theme.a(th.accentLight, (int) (28 + 20 * tw)));
            cv.drawCircle(px, py, rad * 3.2f, dot);
            dot.setColor(Theme.a(0xFFE9A8, (int) (90 + 160 * tw)));
            cv.drawCircle(px, py, rad, dot);
            if (r[i] > 2.5f) {
                dot.setStrokeWidth(1.1f);
                float L = rad * 3f * tw;
                cv.drawLine(px - L, py, px + L, py, dot);
                cv.drawLine(px, py - L, px, py + L, dot);
            }
        }
    }

    // ---------------------------------------------------------------- يراعات

    private void drawFireflies(Canvas cv, float t, int w, int h) {
        dot.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 28; i++) {
            float px = x[i] * w + (float) Math.sin(t * 0.35f * sp[i] + ph[i]) * w * 0.10f + tiltX * r[i] * 9f * d;
            float py = y[i] * h + (float) Math.cos(t * 0.28f * sp[i] + ph[i] * 1.3f) * h * 0.05f + tiltY * r[i] * 9f * d;
            float pulse = 0.5f + 0.5f * (float) Math.sin(t * sp[i] * 1.4f + ph[i]);
            float rad = (1.2f + r[i] * 0.6f) * d;
            int col = i % 2 == 0 ? th.glowB : th.accentLight;
            dot.setColor(Theme.a(col, (int) (14 + 22 * pulse)));
            cv.drawCircle(px, py, rad * 6f * (0.7f + 0.5f * pulse), dot);
            dot.setColor(Theme.a(col, (int) (40 + 60 * pulse)));
            cv.drawCircle(px, py, rad * 3f * (0.7f + 0.5f * pulse), dot);
            dot.setColor(Theme.a(0xFFFFFF, (int) (70 + 185 * pulse)));
            cv.drawCircle(px, py, rad * 0.8f, dot);
        }
    }

    // ---------------------------------------------------------------- بتلات

    private void drawPetals(Canvas cv, float t, int w, int h) {
        dot.setStyle(Paint.Style.FILL);
        int[] cols = {th.accent, th.accentLight, th.glowA};
        for (int i = 0; i < 24; i++) {
            float fall = 0.025f + drift[i] * 3f;
            float yy = ((y[i] + t * fall) % 1f + 1f) % 1f;
            float px = x[i] * w + (float) Math.sin(t * 0.7f * sp[i] * 0.6f + ph[i]) * 34f * d + tiltX * r[i] * 8f * d;
            float py = yy * h + tiltY * r[i] * 8f * d;
            float s = (3.4f + r[i] * 2.2f) * d;
            float flutter = Math.max(0.18f, Math.abs((float) Math.cos(t * sp[i] * 0.9f + ph[i])));
            int a = (int) (70 + 100 * (1f - Math.abs(yy - 0.5f) * 1.2f));
            dot.setColor(Theme.a(cols[i % 3], Math.max(30, Math.min(190, a))));
            cv.save();
            cv.translate(px, py);
            cv.rotate(t * 38f * sp[i] * 0.5f + ph[i] * 57f);
            cv.scale(1f, flutter);
            oval.set(-s, -s * 0.55f, s, s * 0.55f);
            cv.drawOval(oval, dot);
            cv.restore();
        }
    }

    // --------------------------------------------------------------- فقاعات

    private void drawBubbles(Canvas cv, float t, int w, int h) {
        for (int i = 0; i < 30; i++) {
            float yy = ((y[i] - t * drift[i] * 2.4f) % 1f + 1f) % 1f;
            float px = x[i] * w + (float) Math.sin(t * sp[i] * 0.8f + ph[i]) * 12f * d + tiltX * r[i] * 8f * d;
            float py = yy * h + tiltY * r[i] * 8f * d;
            float rad = (4f + r[i] * 4.5f) * d;
            float fadeEdge = Math.min(1f, Math.min(yy, 1f - yy) * 6f);
            dot.setStyle(Paint.Style.FILL);
            dot.setColor(Theme.a(th.accentLight, (int) (16 * fadeEdge)));
            cv.drawCircle(px, py, rad, dot);
            dot.setStyle(Paint.Style.STROKE);
            dot.setStrokeWidth(1.2f * d);
            dot.setColor(Theme.a(th.accentLight, (int) (95 * fadeEdge)));
            cv.drawCircle(px, py, rad, dot);
            dot.setStyle(Paint.Style.FILL);
            dot.setColor(Theme.a(0xFFFFFF, (int) (150 * fadeEdge)));
            cv.drawCircle(px - rad * 0.35f, py - rad * 0.35f, rad * 0.16f, dot);
        }
        dot.setStyle(Paint.Style.FILL);
    }

    // ------------------------------------------------------------ مسح هادئ

    private void drawScan(Canvas cv, float t, int w, int h) {
        float band = 130f * d;
        float yy = ((t * 0.06f) % 1.25f) * (h + band) - band;
        scanM.setTranslate(0f, yy);
        scanShader.setLocalMatrix(scanM);
        cv.drawRect(0, yy, w, yy + band, scanPaint);
        dot.setStyle(Paint.Style.STROKE);
        dot.setStrokeWidth(1f * d);
        dot.setColor(Theme.a(th.accent, 0x22));
        cv.drawLine(0, yy + band * 0.5f, w, yy + band * 0.5f, dot);
        dot.setStyle(Paint.Style.FILL);
    }

    // ------------------------------------------------------- شمس وجمرات

    private void drawSun(Canvas cv, float t, int w, int h) {
        sunPaint.setAlpha((int) (200 + 55 * Math.sin(t * 0.8f)));
        cv.save();
        cv.translate(tiltX * 16f * d, tiltY * 8f * d);
        cv.drawRect(-30f * d, 0, w + 30f * d, h, sunPaint);
        cv.restore();
    }

    private void drawEmbers(Canvas cv, float t, int w, int h) {
        dot.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 50; i++) {
            float rise = 0.03f + drift[i] * 3.2f;
            float yy = 1f - (((1f - y[i]) + t * rise) % 1f);
            float px = x[i] * w + (float) Math.sin(t * sp[i] * 0.8f + ph[i]) * 18f * d + tiltX * r[i] * 8f * d;
            float py = yy * h + tiltY * r[i] * 8f * d;
            float flick = 0.6f + 0.4f * (float) Math.sin(t * sp[i] * 3f + ph[i]);
            float life = Math.max(0f, Math.min(1f, yy * 1.1f)); // ساطعة قرب الأسفل وتخفت وهي تصعد
            float rad = r[i] * 0.55f * d * (1.1f - 0.5f * (1f - life));
            int col = i % 2 == 0 ? th.glowA : th.accentLight;
            dot.setColor(Theme.a(col, (int) (30 * flick * life)));
            cv.drawCircle(px, py, rad * 3.6f, dot);
            dot.setColor(Theme.a(col, (int) (230 * flick * life)));
            cv.drawCircle(px, py, rad, dot);
        }
    }
}
