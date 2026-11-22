package com.alikhwa.thikrni;

/**
 * "شخصية" كل ثيم: شكل الأزرار والبطاقات، مؤثر الأزرار، حركة الضغط، ظهور العناصر،
 * الانتقال بين الصفحات، الخلفية المتحركة، شرارات اللمس، شكل الزر الرئيسي، وكشف تغيير الثيم.
 */
final class Style {
    // شكل الزوايا (الترتيب مهم: يُستخدم كفهرس في جداول الأنصاف)
    static final int ROUND = 0, SOFT = 1, CUT = 2, ORGANIC = 3, SHARP = 4, PETAL = 5, LEAF = 6, ARCH = 7;
    // مؤثر الأزرار
    static final int FX_GOO = 0, FX_SHEEN = 1, FX_NEON = 2, FX_RIPPLE = 3, FX_GLASS = 4;
    // حركة الضغط
    static final int PR_SPRING = 0, PR_SQUASH = 1, PR_TILT = 2, PR_SINK = 3, PR_SNAP = 4, PR_BEAT = 5, PR_WOBBLE = 6;
    // ظهور البطاقات
    static final int EN_SPRING = 0, EN_FADE = 1, EN_ZOOM = 2, EN_FLIP = 3, EN_SIDE = 4, EN_DROP = 5, EN_CASCADE = 6;
    // الانتقال بين الصفحات
    static final int TR_SLIDE = 0, TR_DEPTH = 1, TR_ZOOM = 2, TR_FLIP = 3, TR_SWIPE = 4, TR_ELASTIC = 5, TR_FADE = 6, TR_RISE = 7;
    // الخلفية المتحركة
    static final int AM_STARS = 0, AM_AURORA = 1, AM_DUST = 2, AM_FIREFLY = 3, AM_PETALS = 4, AM_BUBBLES = 5, AM_SCAN = 6, AM_EMBERS = 7;
    // شرارات اللمس
    static final int SP_STAR = 0, SP_DIAMOND = 1, SP_PETAL = 2, SP_GLOW = 3, SP_BUBBLE = 4, SP_SQUARE = 5, SP_SNOW = 6;
    // الزر الرئيسي (عدّاد الصلاة)
    static final int CT_GOO = 0, CT_STAR8 = 1, CT_DIAMOND = 2, CT_WAVE = 3, CT_NEON = 4, CT_SUN = 5, CT_ORBIT = 6;
    // كشف تغيير الثيم
    static final int RV_CIRCLE = 0, RV_DIAMOND = 1, RV_WIPE = 2;

    static final String[] TAGS = {"سائل", "زجاجي", "زخرفي", "عضوي", "جوهري", "مائي", "نيون", "شروق"};

    final int shape, fx, press, enter, trans, ambient, spark, counter, reveal;
    /** جاذبية الشرارات: موجب يسقط، سالب يصعد (بالـ dp/ث²). */
    final float sparkGravity;
    /** مضاعف المدد: أقل من ١ أسرع وأحدّ، أكبر من ١ أنعم وأبطأ. */
    final float tempo;

    private Style(int shape, int fx, int press, int enter, int trans, int ambient, int spark,
                  int counter, int reveal, float sparkGravity, float tempo) {
        this.shape = shape;
        this.fx = fx;
        this.press = press;
        this.enter = enter;
        this.trans = trans;
        this.ambient = ambient;
        this.spark = spark;
        this.counter = counter;
        this.reveal = reveal;
        this.sparkGravity = sparkGravity;
        this.tempo = tempo;
    }

    boolean goo() {
        return fx == FX_GOO;
    }

    private static final Style[] ALL = {
            // ٠ زمردي: سائل
            new Style(ROUND, FX_GOO, PR_SPRING, EN_SPRING, TR_SLIDE, AM_STARS, SP_STAR, CT_GOO, RV_CIRCLE, 320f, 1f),
            // ١ أزرق ليلي: زجاجي
            new Style(SOFT, FX_GLASS, PR_SINK, EN_FADE, TR_DEPTH, AM_AURORA, SP_SNOW, CT_ORBIT, RV_WIPE, 90f, 1.1f),
            // ٢ ذهبي: زخرفي
            new Style(CUT, FX_SHEEN, PR_TILT, EN_ZOOM, TR_ZOOM, AM_DUST, SP_DIAMOND, CT_STAR8, RV_DIAMOND, 240f, 1f),
            // ٣ بنفسجي: عضوي
            new Style(ORGANIC, FX_GOO, PR_SQUASH, EN_FLIP, TR_FLIP, AM_FIREFLY, SP_GLOW, CT_GOO, RV_CIRCLE, 140f, 1f),
            // ٤ ياقوتي: جوهري
            new Style(PETAL, FX_SHEEN, PR_BEAT, EN_SIDE, TR_SWIPE, AM_PETALS, SP_PETAL, CT_DIAMOND, RV_DIAMOND, 200f, 0.95f),
            // ٥ فيروزي: مائي
            new Style(LEAF, FX_RIPPLE, PR_WOBBLE, EN_DROP, TR_ELASTIC, AM_BUBBLES, SP_BUBBLE, CT_WAVE, RV_CIRCLE, -140f, 1.1f),
            // ٦ فحمي: نيون
            new Style(SHARP, FX_NEON, PR_SNAP, EN_FADE, TR_FADE, AM_SCAN, SP_SQUARE, CT_NEON, RV_WIPE, 380f, 0.7f),
            // ٧ غروب: شروق
            new Style(ARCH, FX_SHEEN, PR_SPRING, EN_CASCADE, TR_RISE, AM_EMBERS, SP_GLOW, CT_SUN, RV_CIRCLE, -200f, 1.05f)
    };

    static Style of(int id) {
        return ALL[Math.max(0, Math.min(ALL.length - 1, id))];
    }
}
