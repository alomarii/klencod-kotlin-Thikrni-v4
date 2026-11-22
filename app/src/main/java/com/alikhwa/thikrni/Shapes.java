package com.alikhwa.thikrni;

import android.graphics.Path;
import android.graphics.RectF;

/** يبني مسارات المستطيلات بحسب شكل الثيم (كبسولة، ناعم، مقصوص، عضوي، حاد، بتلة، ورقة، قوس). */
final class Shapes {
    private Shapes() {}

    // أنصاف الأقطار بالـ dp لكل شكل بالترتيب: ROUND, SOFT, CUT, ORGANIC, SHARP, PETAL, LEAF, ARCH
    static final float[] R_CARD = {28, 24, 20, 32, 14, 30, 30, 30};
    static final float[] R_BANNER = {24, 20, 16, 28, 12, 26, 26, 26};
    static final float[] R_NAV = {28, 22, 18, 30, 14, 30, 30, 30};
    static final float[] R_BTN = {16, 16, 12, 22, 14, 22, 22, 22};
    static final float[] R_CHIP = {14, 13, 10, 16, 12, 16, 16, 16};
    static final float[] R_SMALL = {17, 11, 9, 17, 8, 14, 14, 14};

    /**
     * @param r    نصف القطر الأساسي بالبكسل
     * @param pill إن كان الشكل ROUND فتُرسم كبسولة كاملة
     */
    static void path(Path p, RectF rc, int shape, float r, boolean pill) {
        p.reset();
        float m = Math.min(rc.width(), rc.height()) / 2f;
        if (m <= 0f) return;
        float big = (pill && shape == Style.ROUND) ? m : Math.min(r, m);
        float small = Math.min(Math.max(r * 0.28f, 2f), m);
        switch (shape) {
            case Style.SOFT:
                p.addRoundRect(rc, big, big, Path.Direction.CW);
                break;
            case Style.SHARP: {
                float k = Math.min(Math.max(r * 0.35f, 2f), m);
                p.addRoundRect(rc, k, k, Path.Direction.CW);
                break;
            }
            case Style.CUT: {
                float c = Math.min(r, m);
                p.moveTo(rc.left + c, rc.top);
                p.lineTo(rc.right - c, rc.top);
                p.lineTo(rc.right, rc.top + c);
                p.lineTo(rc.right, rc.bottom - c);
                p.lineTo(rc.right - c, rc.bottom);
                p.lineTo(rc.left + c, rc.bottom);
                p.lineTo(rc.left, rc.bottom - c);
                p.lineTo(rc.left, rc.top + c);
                p.close();
                break;
            }
            case Style.ORGANIC:
                corners(p, rc, big, big * 0.4f, big * 0.85f, big * 0.55f);
                break;
            case Style.PETAL:
                corners(p, rc, big, small, big, small);
                break;
            case Style.LEAF:
                corners(p, rc, small, big, small, big);
                break;
            case Style.ARCH:
                corners(p, rc, big, big, small, small);
                break;
            case Style.ROUND:
            default:
                p.addRoundRect(rc, big, big, Path.Direction.CW);
                break;
        }
    }

    private static void corners(Path p, RectF rc, float tl, float tr, float br, float bl) {
        p.addRoundRect(rc, new float[]{tl, tl, tr, tr, br, br, bl, bl}, Path.Direction.CW);
    }
}
