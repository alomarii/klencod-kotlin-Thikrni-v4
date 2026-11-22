package com.alikhwa.thikrni;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** شريط تنقل سفلي عائم بمؤشر منزلق وأيقونات متحركة. */
public class BottomNav extends FrameLayout {
    public interface OnSelect {
        void onSelect(int index);
    }

    private static final String[] LABELS = {"الرئيسية", "إحصائياتي", "الصوتيات", "الإعدادات"};

    private final Ui ui;
    private final NavGoo goo;
    private final LinearLayout row;
    private final LinearLayout[] items = new LinearLayout[4];
    private final NavIcon[] icons = new NavIcon[4];
    private final TextView[] labels = new TextView[4];
    private int sel;
    private final OnSelect cb;

    public BottomNav(Context c, Ui ui, int initial, OnSelect cb) {
        super(c);
        this.ui = ui;
        this.cb = cb;
        this.sel = initial;
        Theme t = ui.t;

        setBackground(ui.shape(Ui.K_NAV).solid(t.navBg)
                .stroke(Theme.a(t.accent, t.st.fx == Style.FX_NEON ? 0xCC : 0x66), ui.dp(t.st.fx == Style.FX_NEON ? 2 : 1)));
        setElevation(ui.dp(10));

        goo = new NavGoo(c, t);
        addView(goo, new LayoutParams(-1, -1));
        if (t.st.goo()) Goo.apply(goo, ui.dp(7));

        row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(L.dir());
        row.setPadding(ui.dp(6), 0, ui.dp(6), 0);
        addView(row, new LayoutParams(-1, -1));

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(c);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            NavIcon ic = new NavIcon(c, i, t.sub, t.onAccent);
            ic.setSelectedAnimated(i == sel, false);
            item.addView(ic, new LinearLayout.LayoutParams(ui.dp(30), ui.dp(26)));
            TextView lb = ui.text(LABELS[i], 11, i == sel ? t.onAccent : t.sub, true);
            lb.setPadding(0, ui.dp(2), 0, 0);
            item.addView(lb);
            item.setOnClickListener(v -> {
                if (idx != sel) {
                    ui.tick(v);
                    select(idx, true);
                    this.cb.onSelect(idx);
                }
            });
            row.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
            items[i] = item;
            icons[i] = ic;
            labels[i] = lb;
        }

        addOnLayoutChangeListener((v, l, tp, r, b, ol, ot, or, ob) -> placePill(false));
    }

    private void placePill(boolean animate) {
        View item = items[sel];
        if (item.getWidth() == 0) return;
        goo.setPillSize(item.getWidth() - ui.dp(10), ui.dp(54));
        float x = row.getLeft() + item.getLeft() + item.getWidth() / 2f;
        if (animate) goo.moveTo(x);
        else goo.snapTo(x);
    }

    public void select(int i, boolean animate) {
        if (i == sel && !animate) {
            placePill(false);
            return;
        }
        sel = i;
        for (int k = 0; k < 4; k++) {
            icons[k].setSelectedAnimated(k == i, animate);
            labels[k].setTextColor(k == i ? ui.t.onAccent : ui.t.sub);
        }
        if (animate) {
            // ارتداد الأيقونة المختارة بنابض يناسب الثيم
            items[i].animate().cancel();
            items[i].setScaleX(0.78f);
            items[i].setScaleY(ui.st.press == Style.PR_SQUASH ? 1.2f : 0.78f);
            items[i].animate().scaleX(1f).scaleY(1f).setDuration(Motion.ms(560, ui.st))
                    .setInterpolator(Motion.SPR).start();
        }
        placePill(animate);
    }
}
