package com.alikhwa.thikrni;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** صفحة داخل التطبيق (تبويب). */
abstract class Page {
    final MainActivity act;
    final Ui ui;
    final Prefs p;
    final ScrollView root;
    final LinearLayout content;

    Page(MainActivity a, Ui ui) {
        this.act = a;
        this.ui = ui;
        this.p = new Prefs(a);
        root = new ScrollView(a);
        root.setVerticalScrollBarEnabled(false);
        root.setOverScrollMode(View.OVER_SCROLL_NEVER);
        content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, ui.dp(8), 0, ui.dp(112));
        root.addView(content, new ViewGroup.LayoutParams(-1, -2));
        root.setOnScrollChangeListener((v, sx, sy, ox, oy) -> Motion.scrollFx(root, content, ui.st));
    }

    abstract void build();

    /** يُستدعى كل مرة يظهر فيها التبويب. */
    void onShow() {
    }

    /** يُستدعى عند مغادرة التبويب. */
    void onHide() {
    }

    /** يُستدعى كل ثانية والتبويب ظاهر. */
    void tick() {
    }

    /** يخفي عناصر الصفحة تمهيدًا لحركة الظهور (بحسب حركة الثيم). */
    void prepareEntrance() {
        for (int i = 0; i < content.getChildCount(); i++) {
            Motion.enterInit(content.getChildAt(i), i, ui.st, ui.dens());
        }
    }

    /** ظهور متتابع للبطاقات بحركة تناسب الثيم. */
    void playEntrance() {
        for (int i = 0; i < content.getChildCount(); i++) {
            Motion.enterPlay(content.getChildAt(i), i, ui.st, ui.dens());
        }
    }
}
