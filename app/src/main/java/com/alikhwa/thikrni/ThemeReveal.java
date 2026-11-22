package com.alikhwa.thikrni;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

/**
 * انتقال تغيير الثيم: تُلتقط صورة للشاشة القديمة ثم تُعاد بناء الواجهة بالثيم الجديد،
 * وتنفتح الصورة القديمة كفتحة تتسع (دائرة / معيّن / ستارة) من مكان الضغط وبحافة متوهجة بلون الثيم الجديد.
 */
final class ThemeReveal {
    private ThemeReveal() {}

    private static Bitmap snap;
    private static float ox, oy;
    private static int oldTop, oldBottom;

    interface Ready {
        void go();
    }

    /** يلتقط الشاشة الحالية ثم يستدعي ready (حتى لو فشل الالتقاط). */
    static void capture(final Activity a, final View root, float originX, float originY, final Ready ready) {
        ox = originX;
        oy = originY;
        oldTop = a.getWindow().getStatusBarColor();
        oldBottom = a.getWindow().getNavigationBarColor();
        final int w = root.getWidth(), h = root.getHeight();
        if (w <= 0 || h <= 0) {
            snap = null;
            ready.go();
            return;
        }
        final Bitmap bmp;
        try {
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        } catch (Throwable t) {
            snap = null;
            ready.go();
            return;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            int[] loc = new int[2];
            root.getLocationInWindow(loc);
            Rect src = new Rect(loc[0], loc[1], loc[0] + w, loc[1] + h);
            try {
                PixelCopy.request(a.getWindow(), src, bmp, result -> {
                    snap = result == PixelCopy.SUCCESS ? bmp : null;
                    ready.go();
                }, new Handler(Looper.getMainLooper()));
                return;
            } catch (Throwable ignored) {
                // نكمل بالطريقة البديلة
            }
        }
        try {
            root.draw(new Canvas(bmp));
            snap = bmp;
        } catch (Throwable t) {
            snap = null;
        }
        ready.go();
    }

    /** يُستدعى من النشاط الجديد: إن وُجدت صورة معلّقة يشغّل الكشف فوق الواجهة. */
    static void playIfPending(final Activity a, final ViewGroup root, final Theme newTheme) {
        final Bitmap old = snap;
        snap = null;
        if (old == null) return;
        final Reveal rv = new Reveal(a, old, ox, oy, newTheme);
        root.addView(rv, new ViewGroup.LayoutParams(-1, -1));

        // تدرّج لون شريط الحالة والتنقل من القديم إلى الجديد
        final ArgbEvaluator ev = new ArgbEvaluator();
        final int newTop = newTheme.bgTop, newBottom = newTheme.bgBottom;
        a.getWindow().setStatusBarColor(oldTop);
        a.getWindow().setNavigationBarColor(oldBottom);

        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(Motion.ms(900, newTheme.st));
        va.setInterpolator(new DecelerateInterpolator(1.5f));
        va.addUpdateListener(an -> {
            float p = (float) an.getAnimatedValue();
            rv.progress = p;
            rv.invalidate();
            a.getWindow().setStatusBarColor((Integer) ev.evaluate(p, oldTop, newTop));
            a.getWindow().setNavigationBarColor((Integer) ev.evaluate(p, oldBottom, newBottom));
        });
        va.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                root.removeView(rv);
                old.recycle();
                a.getWindow().setStatusBarColor(newTop);
                a.getWindow().setNavigationBarColor(newBottom);
            }
        });
        va.start();
    }

    private static final class Reveal extends View {
        private final Bitmap old;
        private final float ox, oy;
        private final int type;
        private final int edge, edge2;
        private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path hole = new Path();
        private final float d;
        float progress = 0f;

        Reveal(Context c, Bitmap old, float ox, float oy, Theme t) {
            super(c);
            this.old = old;
            this.ox = ox;
            this.oy = oy;
            this.type = t.st.reveal;
            this.edge = t.accentLight;
            this.edge2 = t.accent;
            this.d = c.getResources().getDisplayMetrics().density;
            edgePaint.setStyle(Paint.Style.STROKE);
            edgePaint.setStrokeCap(Paint.Cap.ROUND);
            setClickable(true); // يمنع اللمس أثناء الانتقال
        }

        private void buildHole(float p, float w, float h, float inset) {
            hole.reset();
            float maxR = (float) Math.hypot(Math.max(ox, w - ox), Math.max(oy, h - oy));
            switch (type) {
                case Style.RV_DIAMOND: {
                    float rad = maxR * 1.45f * p + inset;
                    hole.moveTo(ox, oy - rad);
                    hole.lineTo(ox + rad, oy);
                    hole.lineTo(ox, oy + rad);
                    hole.lineTo(ox - rad, oy);
                    hole.close();
                    break;
                }
                case Style.RV_WIPE: {
                    // ستارة تنفتح من المركز الأفقي نحو الجانبين بحافة مائلة
                    float half = (w * 0.6f + h * 0.3f) * p + inset;
                    float skew = h * 0.12f;
                    hole.moveTo(ox - half + skew, 0);
                    hole.lineTo(ox + half + skew, 0);
                    hole.lineTo(ox + half - skew, h);
                    hole.lineTo(ox - half - skew, h);
                    hole.close();
                    break;
                }
                case Style.RV_CIRCLE:
                default:
                    hole.addCircle(ox, oy, maxR * p + inset, Path.Direction.CW);
                    break;
            }
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            if (progress >= 1f) return;
            buildHole(progress, w, h, 0f);
            cv.save();
            if (Build.VERSION.SDK_INT >= 26) cv.clipOutPath(hole);
            else cv.clipPath(hole, Region.Op.DIFFERENCE);
            cv.drawBitmap(old, 0f, 0f, null);
            cv.restore();

            // حافة متوهجة بلون الثيم الجديد تتلاشى مع التقدم
            float fade = 1f - progress;
            if (fade <= 0.02f) return;
            buildHole(progress, w, h, 0f);
            float[] wd = {18f, 10f, 4f};
            int[] al = {0x22, 0x44, 0xFF};
            for (int i = 0; i < wd.length; i++) {
                edgePaint.setStrokeWidth(wd[i] * d);
                int base = i == 2 ? edge : edge2;
                edgePaint.setColor(((int) (al[i] * fade) << 24) | (base & 0xFFFFFF));
                cv.drawPath(hole, edgePaint);
            }
        }
    }
}
