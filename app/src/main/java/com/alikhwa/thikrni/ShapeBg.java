package com.alikhwa.thikrni;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** خلفية بشكل الثيم (مسار) مع تعبئة متدرجة أو ثابتة وحدّ اختياري. */
final class ShapeBg extends Drawable {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rc = new RectF();
    private final int shape;
    private final float r;
    private final boolean pill;
    private int[] colors;
    private boolean horizontal;
    private int solid = 0;
    private float strokeW = 0f;
    private int alpha = 255;

    ShapeBg(int shape, float r, boolean pill) {
        this.shape = shape;
        this.r = r;
        this.pill = pill;
        line.setStyle(Paint.Style.STROKE);
    }

    ShapeBg solid(int color) {
        solid = color;
        colors = null;
        rebuild();
        invalidateSelf();
        return this;
    }

    ShapeBg gradient(boolean horizontal, int... cols) {
        this.horizontal = horizontal;
        this.colors = cols;
        rebuild();
        invalidateSelf();
        return this;
    }

    ShapeBg stroke(int color, float w) {
        line.setColor(color);
        line.setStrokeWidth(w);
        strokeW = w;
        rebuild();
        invalidateSelf();
        return this;
    }

    @Override
    protected void onBoundsChange(Rect b) {
        super.onBoundsChange(b);
        rebuild();
    }

    private void rebuild() {
        Rect b = getBounds();
        float half = strokeW / 2f;
        rc.set(b.left + half, b.top + half, b.right - half, b.bottom - half);
        if (rc.width() <= 0f || rc.height() <= 0f) {
            path.reset();
            return;
        }
        Shapes.path(path, rc, shape, r, pill);
        if (colors != null) {
            fill.setShader(horizontal
                    ? new LinearGradient(rc.left, 0, rc.right, 0, colors, null, Shader.TileMode.CLAMP)
                    : new LinearGradient(0, rc.top, 0, rc.bottom, colors, null, Shader.TileMode.CLAMP));
        } else {
            fill.setShader(null);
        }
    }

    @Override
    public void draw(Canvas cv) {
        if (path.isEmpty()) return;
        if (colors != null) {
            fill.setAlpha(alpha);
            cv.drawPath(path, fill);
        } else if (Color.alpha(solid) > 0) {
            fill.setColor(solid);
            fill.setAlpha(Color.alpha(solid) * alpha / 255);
            cv.drawPath(path, fill);
        }
        if (strokeW > 0f) cv.drawPath(path, line);
    }

    @Override
    public void setAlpha(int a) {
        alpha = a;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        fill.setColorFilter(cf);
        line.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
