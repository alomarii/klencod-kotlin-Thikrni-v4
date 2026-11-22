package com.alikhwa.thikrni;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** أدوات بناء الواجهة بالألوان الحالية للثيم. */
public final class Ui {
    public final Context ctx;
    public final Theme t;
    public final Style st;
    private final float d;

    public static final int K_CARD = 0, K_BTN = 1, K_CHIP = 2, K_NAV = 3, K_BANNER = 4, K_SMALL = 5;

    public Ui(Context c, Theme theme) {
        ctx = c;
        t = theme;
        st = theme.st;
        d = c.getResources().getDisplayMetrics().density;
    }

    public float dens() {
        return d;
    }

    public int dp(float v) {
        return Math.round(v * d);
    }


    /** خلفية بشكل الثيم الحالي حسب نوع العنصر. */
    public ShapeBg shape(int kind) {
        return shapeFor(st, kind);
    }

    public ShapeBg shapeFor(Style s, int kind) {
        float[] tbl;
        switch (kind) {
            case K_CARD:
                tbl = Shapes.R_CARD;
                break;
            case K_BANNER:
                tbl = Shapes.R_BANNER;
                break;
            case K_NAV:
                tbl = Shapes.R_NAV;
                break;
            case K_SMALL:
                tbl = Shapes.R_SMALL;
                break;
            case K_CHIP:
                tbl = Shapes.R_CHIP;
                break;
            default:
                tbl = Shapes.R_BTN;
                break;
        }
        boolean pill = s.shape == Style.ROUND && (kind == K_BTN || kind == K_CHIP || kind == K_SMALL);
        return new ShapeBg(s.shape, dp(tbl[s.shape]), pill);
    }

    /** نبضة اهتزاز تناسب الثيم (حادّة للنيون، ناعمة لغيره). */
    public void tick(View v) {
        v.performHapticFeedback(st.press == Style.PR_SNAP
                ? android.view.HapticFeedbackConstants.KEYBOARD_TAP
                : android.view.HapticFeedbackConstants.CLOCK_TICK);
    }

    public TextView text(String s, float sp, int color, boolean bold) {
        TextView tv = new LText(ctx);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tv.setLayoutDirection(L.dir());
        return tv;
    }

    public TextView sectionTitle(String s) {
        TextView tv = text(s, 19, t.accent, true);
        tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tv.setPadding(0, 0, 0, dp(6));
        return tv;
    }

    public TextView pageTitle(String title, String sub) {
        TextView tv = text(title, 30, t.accent, true);
        tv.setPadding(0, dp(18), 0, dp(sub == null ? 10 : 0));
        return tv;
    }

    public TextView hint(String s) {
        TextView tv = text(s, 12, t.sub, false);
        tv.setGravity(Gravity.START);
        tv.setLineSpacing(0, 1.2f);
        tv.setPadding(dp(2), dp(8), dp(2), 0);
        return tv;
    }

    public LinearLayout card() {
        LinearLayout l = new LinearLayout(ctx);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setBackground(shape(K_CARD).gradient(false, 0x30FFFFFF, 0x14FFFFFF)
                .stroke(Theme.a(t.accent, 0x55), dp(1)));
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(16), dp(8), dp(16), dp(8));
        l.setLayoutParams(lp);
        return l;
    }

    public View divider() {
        View v = new View(ctx);
        v.setBackgroundColor(Theme.a(t.accent, 0x33));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.setMargins(0, dp(10), 0, dp(10));
        v.setLayoutParams(lp);
        return v;
    }

    public View space(int w) {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(w, 1));
        return v;
    }

    public TextView goldButton(String s) {
        TextView tv;
        if (st.fx == Style.FX_NEON) {
            tv = text(s, 17, t.accent, true);
            tv.setBackground(shape(K_BTN).gradient(false, 0xFF1B1B1B, 0xFF0A0A0A)
                    .stroke(t.accent, dp(2)));
        } else if (st.fx == Style.FX_GLASS) {
            tv = text(s, 17, t.onAccent, true);
            tv.setBackground(shape(K_BTN).gradient(false, Theme.a(t.accentLight, 0xEE), Theme.a(t.accentDark, 0xDD))
                    .stroke(0x66FFFFFF, dp(1)));
        } else {
            tv = text(s, 17, t.onAccent, true);
            tv.setBackground(shape(K_BTN).gradient(true, t.accentDark, t.accentLight, t.accentDark));
        }
        tv.setGravity(Gravity.CENTER);
        pressEffect(tv);
        return tv;
    }

    /** زر Venom السائل (يتبدّل شكله وحركته حسب الثيم). الارتفاع الكلي = ارتفاع الكبسولة + ٢ × venomPad(). */
    public VenomButton venom(String s, boolean dark) {
        return new VenomButton(ctx, t, s, dark);
    }

    public int venomPad() {
        return dp(10);
    }

    public TextView outlineButton(String s) {
        TextView tv = text(s, 14, t.accent, true);
        boolean neon = st.fx == Style.FX_NEON;
        tv.setBackground(shape(K_BTN).solid(neon ? 0x22000000 : 0x11FFFFFF)
                .stroke(Theme.a(t.accent, neon ? 0xFF : 0x88), dp(neon ? 2 : 1)));
        tv.setGravity(Gravity.CENTER);
        pressEffect(tv);
        return tv;
    }

    public TextView smallBtn(String s) {
        TextView tv = text(s, 20, t.accent, true);
        tv.setBackground(shape(K_SMALL).solid(0x22FFFFFF).stroke(Theme.a(t.accent, 0x66), dp(1)));
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(34), dp(34)));
        pressEffect(tv);
        return tv;
    }

    /** زر صغير بحدود (للتشغيل/الحذف داخل القوائم). */
    public TextView pillButton(String s, int color) {
        TextView tv = text(s, 12, color, true);
        tv.setBackground(shape(K_CHIP).solid(0x14FFFFFF).stroke(Theme.a(color, 0x88), dp(1)));
        tv.setPadding(dp(12), dp(7), dp(12), dp(7));
        pressEffect(tv);
        return tv;
    }

    /** حركة الضغط بحسب الثيم. */
    public void pressEffect(final View v) {
        Motion.press(v, st);
    }

    /** TextView يترجم النصوص الثابتة تلقائيًا بحسب لغة التطبيق. */
    static final class LText extends TextView {
        LText(Context c) {
            super(c);
        }

        @Override
        public void setText(CharSequence t, BufferType type) {
            if (t instanceof String) t = L.t((String) t);
            super.setText(t, type);
        }
    }

    public interface OnToggle {
        void on(boolean b);
    }

    public LinearLayout switchRow(String label, boolean value, OnToggle cb) {
        LinearLayout r = new LinearLayout(ctx);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(8), 0, dp(8));
        TextView l = text(label, 16, Color.WHITE, false);
        l.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));
        Switch s = new Switch(ctx);
        s.setChecked(value);
        s.setThumbTintList(new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{t.accent, 0xFFB0BEC5}));
        s.setTrackTintList(new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{Theme.a(t.accentDark, 0x99), 0x55FFFFFF}));
        s.setOnCheckedChangeListener((bv, checked) -> {
            bv.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            cb.on(checked);
        });
        r.addView(s);
        return r;
    }

    // ----------------------------------------------------------------- الشرائح

    public interface OnPick {
        void pick(int index);
    }

    public static final class Chips extends LinearLayout {
        private final List<TextView> items = new ArrayList<>();
        private final Ui ui;

        Chips(Ui ui, String[] labels, int perRow, OnPick cb) {
            super(ui.ctx);
            this.ui = ui;
            setOrientation(VERTICAL);
            for (int start = 0; start < labels.length; start += perRow) {
                LinearLayout row = new LinearLayout(ui.ctx);
                row.setOrientation(HORIZONTAL);
                for (int i = start; i < start + perRow; i++) {
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
                    lp.setMargins(ui.dp(3), ui.dp(3), ui.dp(3), ui.dp(3));
                    if (i < labels.length) {
                        final int idx = i;
                        TextView c = ui.text(labels[i], 13, Color.WHITE, true);
                        c.setPadding(ui.dp(4), ui.dp(10), ui.dp(4), ui.dp(10));
                        c.setOnClickListener(v -> {
                            ui.tick(v);
                            cb.pick(idx);
                        });
                        ui.pressEffect(c);
                        row.addView(c, lp);
                        items.add(c);
                    } else {
                        row.addView(new View(ui.ctx), lp);
                    }
                }
                addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
        }

        public void select(int idx) {
            for (int i = 0; i < items.size(); i++) ui.styleChip(items.get(i), i == idx);
        }

        public void setLabel(int i, String s) {
            items.get(i).setText(s);
        }
    }

    public Chips chips(String[] labels, int perRow, int selected, OnPick cb) {
        Chips c = new Chips(this, labels, perRow, cb);
        c.select(selected);
        return c;
    }

    public void styleChip(TextView c, boolean sel) {
        ShapeBg g = shape(K_CHIP);
        if (sel) {
            g.gradient(false, t.accentLight, t.accentDark);
            if (st.fx == Style.FX_NEON) g.stroke(t.accent, dp(2));
        } else {
            g.gradient(false, 0x22FFFFFF, 0x11FFFFFF).stroke(Theme.a(t.accent, 0x44), dp(1));
        }
        c.setBackground(g);
        c.setTextColor(sel ? t.onAccent : Color.WHITE);
    }

    // ------------------------------------------------------------------ حركة

    /** عدّ تصاعدي متحرك لرقم داخل TextView. */
    public void countUp(final TextView tv, long from, final long to, final String prefix, final String suffix) {
        Object old = tv.getTag();
        if (old instanceof ValueAnimator) ((ValueAnimator) old).cancel();
        if (from == to) {
            tv.setText(prefix + HijriUtil.num(to) + suffix);
            return;
        }
        ValueAnimator va = ValueAnimator.ofFloat(from, to);
        va.setDuration((long) Math.min(1100, 450 + Math.abs(to - from) * 4L));
        va.setInterpolator(new DecelerateInterpolator(1.6f));
        va.addUpdateListener(a -> tv.setText(prefix + HijriUtil.num(Math.round((float) a.getAnimatedValue())) + suffix));
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tv.setText(prefix + HijriUtil.num(to) + suffix);
            }
        });
        tv.setTag(va);
        va.start();
    }
}
