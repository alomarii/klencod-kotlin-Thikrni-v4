package com.alikhwa.thikrni;

import android.animation.ObjectAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ScrollView;

/** محرّك الحركة: ضغط، ظهور، انتقال بين الصفحات، وتأثير التمرير؛ كلٌّ بحسب شخصية الثيم. */
final class Motion {
    private Motion() {}

    /** نابض مخمّد: يصعد من ٠ إلى ١ مع ارتداد طبيعي. */
    static final class Spring implements Interpolator {
        private final float damp, freq;

        Spring(float damp, float freq) {
            this.damp = damp;
            this.freq = freq;
        }

        @Override
        public float getInterpolation(float t) {
            if (t >= 1f) return 1f;
            return (float) (1.0 - Math.exp(-damp * t) * Math.cos(freq * t));
        }
    }

    static final Interpolator SPR = new Spring(5.5f, 9.5f);
    static final Interpolator SPR_SOFT = new Spring(7f, 8f);
    static final Interpolator SPR_WILD = new Spring(4.2f, 11.5f);

    /** وضع تخفيف الحركة (من الإعدادات أو من إعداد النظام). */
    static boolean reduced = false;

    static void init(android.content.Context c) {
        boolean sys = false;
        try {
            sys = android.provider.Settings.Global.getFloat(c.getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Exception ignored) {
        }
        reduced = sys || new Prefs(c).reduceMotion();
    }

    /** يشغّل حركة متكررة إلا في وضع تخفيف الحركة. */
    static void loop(android.animation.Animator a) {
        if (!reduced) a.start();
    }

    private static float dens(View v) {
        return v.getResources().getDisplayMetrics().density;
    }

    static long ms(float base, Style st) {
        return (long) (base * st.tempo * (reduced ? 0.35f : 1f));
    }

    /** يعيد كل تحويلات العرض إلى الوضع الطبيعي. */
    static void reset(View v) {
        v.setAlpha(1f);
        v.setTranslationX(0f);
        v.setTranslationY(0f);
        v.setScaleX(1f);
        v.setScaleY(1f);
        v.setRotation(0f);
        v.setRotationX(0f);
        v.setRotationY(0f);
    }

    // ------------------------------------------------------------------ ضغط

    static void press(final View v, final Style st) {
        final float d = dens(v);
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    down(view, st, e, d);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!reduced && st.press == Style.PR_TILT) tilt(view, e);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    up(view, st, d);
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    private static void tilt(View v, MotionEvent e) {
        float nx = (e.getX() / Math.max(1f, v.getWidth()) - 0.5f) * 2f;
        float ny = (e.getY() / Math.max(1f, v.getHeight()) - 0.5f) * 2f;
        nx = Math.max(-1f, Math.min(1f, nx));
        ny = Math.max(-1f, Math.min(1f, ny));
        v.setRotationY(nx * 9f);
        v.setRotationX(-ny * 9f);
    }

    private static void down(View v, Style st, MotionEvent e, float d) {
        v.animate().cancel();
        switch (reduced ? Style.PR_SNAP : st.press) {
            case Style.PR_SQUASH:
                v.animate().scaleX(1.05f).scaleY(0.90f).setDuration(110)
                        .setInterpolator(new DecelerateInterpolator()).start();
                break;
            case Style.PR_TILT:
                v.setCameraDistance(d * 1600f);
                tilt(v, e);
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(110).start();
                break;
            case Style.PR_SINK:
                v.animate().translationY(2f * d).scaleX(0.975f).scaleY(0.975f).alpha(0.88f)
                        .setDuration(100).start();
                break;
            case Style.PR_SNAP:
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(50)
                        .setInterpolator(new LinearInterpolator()).start();
                break;
            case Style.PR_BEAT:
                v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(90).start();
                break;
            case Style.PR_SPRING:
            case Style.PR_WOBBLE:
            default:
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90)
                        .setInterpolator(new DecelerateInterpolator()).start();
                break;
        }
    }

    private static void up(final View v, Style st, float d) {
        v.animate().cancel();
        switch (reduced ? Style.PR_SNAP : st.press) {
            case Style.PR_SQUASH:
                v.animate().scaleX(1f).scaleY(1f).setDuration(560).setInterpolator(SPR_WILD).start();
                break;
            case Style.PR_TILT:
                v.animate().rotationX(0f).rotationY(0f).scaleX(1f).scaleY(1f).setDuration(520)
                        .setInterpolator(SPR).start();
                break;
            case Style.PR_SINK:
                v.animate().translationY(0f).scaleX(1f).scaleY(1f).alpha(1f).setDuration(220)
                        .setInterpolator(new DecelerateInterpolator()).start();
                break;
            case Style.PR_SNAP:
                v.animate().scaleX(1f).scaleY(1f).setDuration(90)
                        .setInterpolator(new LinearInterpolator()).start();
                break;
            case Style.PR_BEAT:
                v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(110)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(420)
                                .setInterpolator(SPR).start()).start();
                break;
            case Style.PR_WOBBLE:
                v.setRotation(4f);
                v.animate().rotation(0f).scaleX(1f).scaleY(1f).setDuration(620)
                        .setInterpolator(SPR_WILD).start();
                break;
            case Style.PR_SPRING:
            default:
                v.animate().scaleX(1f).scaleY(1f).setDuration(420).setInterpolator(SPR_SOFT).start();
                break;
        }
    }

    // ----------------------------------------------------------------- ظهور

    /** الحالة الابتدائية لعنصر قبل ظهوره. */
    static void enterInit(View v, int i, Style st, float d) {
        v.animate().cancel();
        reset(v);
        v.setAlpha(0f);
        switch (reduced ? Style.EN_FADE : st.enter) {
            case Style.EN_FADE:
                v.setTranslationY(18f * d);
                break;
            case Style.EN_ZOOM:
                v.setScaleX(0.82f);
                v.setScaleY(0.82f);
                break;
            case Style.EN_FLIP:
                v.setCameraDistance(d * 1600f);
                v.setRotationX(-65f);
                v.setTranslationY(24f * d);
                break;
            case Style.EN_SIDE:
                v.setTranslationX(((i & 1) == 0 ? -1f : 1f) * 70f * d);
                break;
            case Style.EN_DROP:
                v.setTranslationY(-100f * d);
                break;
            case Style.EN_CASCADE:
                v.setTranslationY(70f * d);
                v.setScaleX(0.9f);
                v.setScaleY(0.9f);
                break;
            case Style.EN_SPRING:
            default:
                v.setTranslationY(40f * d);
                v.setScaleX(0.94f);
                v.setScaleY(0.94f);
                break;
        }
    }

    static void enterPlay(View v, int i, Style st, float d) {
        enterInit(v, i, st, d);
        switch (reduced ? Style.EN_FADE : st.enter) {
            case Style.EN_FADE:
                v.animate().alpha(1f).translationY(0f).setStartDelay(ms(55 * i, st)).setDuration(ms(380, st))
                        .setInterpolator(new DecelerateInterpolator(2f)).start();
                break;
            case Style.EN_ZOOM:
                v.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(ms(70 * i, st)).setDuration(ms(520, st))
                        .setInterpolator(new OvershootInterpolator(1.3f)).start();
                break;
            case Style.EN_FLIP:
                v.animate().alpha(1f).rotationX(0f).translationY(0f).setStartDelay(ms(85 * i, st))
                        .setDuration(ms(700, st)).setInterpolator(new OvershootInterpolator(0.8f)).start();
                break;
            case Style.EN_SIDE:
                v.animate().alpha(1f).translationX(0f).setStartDelay(ms(70 * i, st)).setDuration(ms(560, st))
                        .setInterpolator(new DecelerateInterpolator(2.2f)).start();
                break;
            case Style.EN_DROP: {
                ObjectAnimator a = ObjectAnimator.ofFloat(v, View.ALPHA, 0f, 1f);
                a.setStartDelay(ms(80 * i, st));
                a.setDuration(220);
                a.start();
                v.animate().translationY(0f).setStartDelay(ms(80 * i, st)).setDuration(ms(760, st))
                        .setInterpolator(new BounceInterpolator()).start();
                break;
            }
            case Style.EN_CASCADE:
                v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setStartDelay(ms(90 * i, st))
                        .setDuration(ms(760, st)).setInterpolator(new Spring(6.5f, 8.5f)).start();
                break;
            case Style.EN_SPRING:
            default:
                v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setStartDelay(ms(75 * i, st))
                        .setDuration(ms(620, st)).setInterpolator(new OvershootInterpolator(0.9f)).start();
                break;
        }
    }

    // -------------------------------------------------------------- انتقال

    /**
     * انتقال بين صفحتين. dir = ‎-١‎ إن كانت الجديدة تدخل من اليسار، و ‎+١‎ من اليمين.
     * outDone يُستدعى عند اكتمال خروج القديمة.
     */
    static void transition(Style st, final View in, final View out, int dir, Runnable outDone) {
        final float d = dens(in);
        final float w = Math.max(in.getWidth(), d * 360f);
        in.animate().cancel();
        out.animate().cancel();
        reset(in);
        in.setVisibility(View.VISIBLE);
        in.bringToFront();
        switch (reduced ? Style.TR_FADE : st.trans) {
            case Style.TR_DEPTH:
                in.setAlpha(0f);
                in.setTranslationX(dir * w);
                in.animate().alpha(1f).translationX(0f).setDuration(ms(480, st))
                        .setInterpolator(new DecelerateInterpolator(2.4f)).start();
                out.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).translationX(-dir * w * 0.25f)
                        .setDuration(ms(340, st)).setInterpolator(new AccelerateInterpolator())
                        .withEndAction(outDone).start();
                break;
            case Style.TR_ZOOM:
                in.setAlpha(0f);
                in.setScaleX(1.15f);
                in.setScaleY(1.15f);
                in.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(ms(520, st))
                        .setInterpolator(new DecelerateInterpolator(2f)).start();
                out.animate().alpha(0f).scaleX(0.86f).scaleY(0.86f).setDuration(ms(320, st))
                        .setInterpolator(new AccelerateInterpolator()).withEndAction(outDone).start();
                break;
            case Style.TR_FLIP:
                in.setCameraDistance(d * 1400f);
                out.setCameraDistance(d * 1400f);
                in.setAlpha(0f);
                in.setRotationY(dir * 70f);
                in.setScaleX(0.92f);
                in.setScaleY(0.92f);
                in.animate().alpha(1f).rotationY(0f).scaleX(1f).scaleY(1f).setDuration(ms(560, st))
                        .setInterpolator(new DecelerateInterpolator(2f)).start();
                out.animate().alpha(0f).rotationY(-dir * 70f).scaleX(0.92f).scaleY(0.92f)
                        .setDuration(ms(300, st)).setInterpolator(new AccelerateInterpolator())
                        .withEndAction(outDone).start();
                break;
            case Style.TR_SWIPE:
                in.setAlpha(0f);
                in.setTranslationX(dir * w * 0.55f);
                in.setRotation(-dir * 7f);
                in.animate().alpha(1f).translationX(0f).rotation(0f).setDuration(ms(520, st))
                        .setInterpolator(new DecelerateInterpolator(2.2f)).start();
                out.animate().alpha(0f).translationX(-dir * w * 0.35f).rotation(dir * 5f)
                        .setDuration(ms(320, st)).setInterpolator(new AccelerateInterpolator())
                        .withEndAction(outDone).start();
                break;
            case Style.TR_ELASTIC: {
                in.setAlpha(0f);
                in.setTranslationX(dir * w * 0.45f);
                in.setScaleX(0.9f);
                in.setScaleY(0.9f);
                ObjectAnimator fade = ObjectAnimator.ofFloat(in, View.ALPHA, 0f, 1f);
                fade.setDuration(220);
                fade.start();
                in.animate().translationX(0f).scaleX(1f).scaleY(1f).setDuration(ms(700, st))
                        .setInterpolator(SPR_SOFT).start();
                out.animate().alpha(0f).translationX(-dir * w * 0.3f).setDuration(ms(220, st))
                        .setInterpolator(new AccelerateInterpolator()).withEndAction(outDone).start();
                break;
            }
            case Style.TR_FADE:
                in.setAlpha(0f);
                in.setScaleX(0.985f);
                in.setScaleY(0.985f);
                in.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(ms(260, st))
                        .setInterpolator(new DecelerateInterpolator()).start();
                out.animate().alpha(0f).setDuration(ms(150, st)).withEndAction(outDone).start();
                break;
            case Style.TR_RISE:
                in.setAlpha(0f);
                in.setTranslationY(90f * d);
                in.animate().alpha(1f).translationY(0f).setDuration(ms(460, st))
                        .setInterpolator(new DecelerateInterpolator(2.4f)).start();
                out.animate().alpha(0f).translationY(-36f * d).setDuration(ms(260, st))
                        .setInterpolator(new AccelerateInterpolator()).withEndAction(outDone).start();
                break;
            case Style.TR_SLIDE:
            default: {
                float shift = 64f * d;
                in.setAlpha(0f);
                in.setTranslationX(dir * shift);
                in.setScaleX(0.93f);
                in.setScaleY(0.93f);
                in.animate().alpha(1f).translationX(0f).scaleX(1f).scaleY(1f).setDuration(420)
                        .setInterpolator(new DecelerateInterpolator(2.2f)).start();
                out.animate().alpha(0f).translationX(-dir * shift * 0.7f).scaleX(0.93f).scaleY(0.93f)
                        .setDuration(260).setInterpolator(new AccelerateInterpolator())
                        .withEndAction(outDone).start();
                break;
            }
        }
    }

    // ---------------------------------------------------------------- تمرير

    /**
     * تأثير عمق أثناء التمرير: البطاقات التي تقترب من الحافة السفلية (تحت الشريط) أو تغادر من أعلى
     * تصغر وتخفت قليلًا، ومع الأنماط ثلاثية الأبعاد تميل قليلًا.
     */
    static void scrollFx(ScrollView sv, ViewGroup content, Style st) {
        final int vh = sv.getHeight();
        if (vh <= 0 || reduced) return;
        final float d = dens(sv);
        final int sy = sv.getScrollY();
        final float zoneTop = 90f * d;
        final float zoneBottom = 170f * d;
        final boolean tilty = st.enter == Style.EN_FLIP || st.press == Style.PR_TILT;
        for (int i = 0; i < content.getChildCount(); i++) {
            View c = content.getChildAt(i);
            if (c.getVisibility() == View.GONE || c.getHeight() == 0) continue;
            float top = c.getTop() - sy;
            float bottom = c.getBottom() - sy;
            float e = 0f;
            if (bottom < zoneTop) e = Math.max(e, Math.min(1f, 1f - bottom / zoneTop));
            if (top > vh - zoneBottom) e = Math.max(e, Math.min(1f, (top - (vh - zoneBottom)) / zoneBottom));
            boolean above = bottom < zoneTop;
            c.setScaleX(1f - 0.07f * e);
            c.setScaleY(1f - 0.07f * e);
            c.setAlpha(1f - 0.55f * e);
            if (tilty) {
                c.setCameraDistance(d * 1600f);
                c.setRotationX((above ? 1f : -1f) * 14f * e);
            }
        }
    }
}
