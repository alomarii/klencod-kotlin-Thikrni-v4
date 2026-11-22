package com.alikhwa.thikrni;

import android.annotation.TargetApi;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

/**
 * تأثير "الفقاعات اللزجة" (Goo / Venom): تمويه ثم عتبة على قناة الشفافية،
 * فتندمج الأشكال القريبة وتتمدد كالسائل. يعمل على أندرويد ١٢+ (RenderEffect)،
 * وفي الإصدارات الأقدم تُرسم الأشكال نفسها بدون الدمج.
 */
final class Goo {
    private Goo() {}

    static boolean supported() {
        return Build.VERSION.SDK_INT >= 31;
    }

    @TargetApi(31)
    static void apply(View v, float blurPx) {
        if (!supported()) return;
        // الشفافية الجديدة = ٢٢ × القديمة - ٢٦٧٧٫٥ (عتبة عند ٥٠٪)
        ColorMatrix cm = new ColorMatrix(new float[]{
                1, 0, 0, 0, 0,
                0, 1, 0, 0, 0,
                0, 0, 1, 0, 0,
                0, 0, 0, 22f, -2677.5f});
        RenderEffect blur = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.DECAL);
        v.setRenderEffect(RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(cm), blur));
    }

    /** فقاعة بفيزياء نابض: تتجه نحو هدفها بتخميد. */
    static final class Blob {
        float x, y, r, vx, vy, tx, ty, tr;

        Blob(float x, float y, float r) {
            this.x = tx = x;
            this.y = ty = y;
            this.r = tr = r;
        }

        void step(float dt, float k, float damp) {
            vx += (k * (tx - x) - damp * vx) * dt;
            vy += (k * (ty - y) - damp * vy) * dt;
            x += vx * dt;
            y += vy * dt;
            r += (tr - r) * Math.min(1f, dt * 12f);
        }

        boolean settled() {
            return Math.abs(tx - x) < 0.5f && Math.abs(ty - y) < 0.5f
                    && Math.abs(vx) < 6f && Math.abs(vy) < 6f;
        }
    }
}
