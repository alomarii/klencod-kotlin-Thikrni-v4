package com.alikhwa.thikrni;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** الرئيسية: التاريخ، زر الصلاة على النبي، الهدف اليومي، عدّاد الأجهزة. */
final class HomePage extends Page {
    private TextView tvDay, tvHijri, tvGreg, tvTime, tvAdj, tvCount, tvPhrase, tvGoal, tvDevices, tvBannerTitle, tvBannerMsg;
    private LinearLayout banner, goalBox, devCard, widgetCard;
    private TextView pvHijri, pvCount, pvLabel, tvStreak;
    private FlameView flame;
    private GooCounterView counter;
    private GoalBarView goalBar;
    private long shownCount = -1;
    private long shownDevices = 0;

    HomePage(MainActivity a, Ui ui) {
        super(a, ui);
        build();
    }

    @Override
    void build() {
        buildHeader();
        buildBanner();
        buildDateCard();
        buildCounterCard();
        buildPrayerCard();
        buildWidgetCard();
        buildDevicesCard();
    }

    private void buildHeader() {
        Theme t = ui.t;
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(0, ui.dp(10), 0, ui.dp(4));

        ImageView logo = new ImageView(act);
        logo.setImageResource(R.drawable.logo_alikhwa);
        box.addView(logo, new LinearLayout.LayoutParams(ui.dp(104), ui.dp(104)));
        ObjectAnimator fl = ObjectAnimator.ofFloat(logo, "translationY", 0f, -ui.dp(8));
        fl.setDuration(2300);
        fl.setRepeatMode(ValueAnimator.REVERSE);
        fl.setRepeatCount(ValueAnimator.INFINITE);
        fl.setInterpolator(new AccelerateDecelerateInterpolator());
        Motion.loop(fl);
        ObjectAnimator sc = ObjectAnimator.ofPropertyValuesHolder(logo,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.06f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.06f));
        sc.setDuration(2300);
        sc.setRepeatMode(ValueAnimator.REVERSE);
        sc.setRepeatCount(ValueAnimator.INFINITE);
        Motion.loop(sc);

        final TextView title = ui.text("ذكرني", 40, t.accent, true);
        title.setPadding(0, ui.dp(8), 0, 0);
        final LinearGradient shader = new LinearGradient(0, 0, ui.dp(260), 0,
                new int[]{t.accentDark, t.accentLight, t.accent, t.accentDark},
                new float[]{0f, 0.35f, 0.6f, 1f}, Shader.TileMode.REPEAT);
        title.getPaint().setShader(shader);
        final Matrix m = new Matrix();
        ValueAnimator shimmer = ValueAnimator.ofFloat(0f, ui.dp(260));
        shimmer.setDuration(2600);
        shimmer.setRepeatCount(ValueAnimator.INFINITE);
        shimmer.setInterpolator(new LinearInterpolator());
        shimmer.addUpdateListener(a -> {
            m.setTranslate((float) a.getAnimatedValue(), 0);
            shader.setLocalMatrix(m);
            title.invalidate();
        });
        Motion.loop(shimmer);
        box.addView(title);
        box.addView(ui.text("اللهم صلِّ وسلِّم على نبينا محمد", 15, t.sub, false));
        content.addView(box, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildBanner() {
        Theme t = ui.t;
        banner = new LinearLayout(act);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setGravity(Gravity.CENTER);
        banner.setBackground(ui.shape(Ui.K_BANNER).gradient(true, Theme.a(t.accent, 0x55), Theme.a(t.accent, 0x22))
                .stroke(Theme.a(t.accent, 0xAA), ui.dp(1)));
        banner.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(8));
        banner.setLayoutParams(lp);
        tvBannerTitle = ui.text("", 17, t.accent, true);
        tvBannerMsg = ui.text("", 13, Color.WHITE, false);
        tvBannerMsg.setLineSpacing(0, 1.2f);
        tvBannerMsg.setPadding(0, ui.dp(4), 0, 0);
        banner.addView(tvBannerTitle);
        banner.addView(tvBannerMsg);
        ObjectAnimator breathe = ObjectAnimator.ofPropertyValuesHolder(tvBannerTitle,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.06f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.06f),
                PropertyValuesHolder.ofFloat("alpha", 1f, 0.75f));
        breathe.setDuration(1500);
        breathe.setRepeatMode(ValueAnimator.REVERSE);
        breathe.setRepeatCount(ValueAnimator.INFINITE);
        Motion.loop(breathe);
        banner.setVisibility(View.GONE);
        content.addView(banner);
    }

    private void buildDateCard() {
        Theme t = ui.t;
        LinearLayout c = ui.card();
        tvDay = ui.text("", 16, t.sub, false);
        tvHijri = ui.text("", 28, t.accent, true);
        tvGreg = ui.text("", 19, Color.WHITE, true);
        tvTime = ui.text("", 15, t.sub, false);
        c.addView(tvDay);
        c.addView(tvHijri);
        c.addView(ui.divider());
        c.addView(tvGreg);
        c.addView(tvTime);

        LinearLayout adj = new LinearLayout(act);
        adj.setGravity(Gravity.CENTER);
        adj.setPadding(0, ui.dp(10), 0, 0);
        TextView lbl = ui.text("تعديل الهجري:", 13, t.sub, false);
        TextView minus = ui.smallBtn("−");
        TextView plus = ui.smallBtn("+");
        tvAdj = ui.text("", 14, Color.WHITE, true);
        tvAdj.setMinWidth(ui.dp(60));
        minus.setOnClickListener(v -> {
            p.setHijriAdjust(p.hijriAdjust() - 1);
            refreshDates();
            refreshBanner();
            SalawatWidget.updateAll(act);
        });
        plus.setOnClickListener(v -> {
            p.setHijriAdjust(p.hijriAdjust() + 1);
            refreshDates();
            refreshBanner();
            SalawatWidget.updateAll(act);
        });
        adj.addView(lbl);
        adj.addView(ui.space(ui.dp(8)));
        adj.addView(plus);
        adj.addView(tvAdj);
        adj.addView(minus);
        c.addView(adj);
        content.addView(c);
        refreshDates();
    }

    private void buildCounterCard() {
        Theme t = ui.t;
        LinearLayout c = ui.card();
        counter = new GooCounterView(act, t);
        c.addView(counter, new LinearLayout.LayoutParams(-1, ui.dp(250)));
        tvCount = ui.text("", 20, t.accent, true);
        c.addView(tvCount);

        goalBox = new LinearLayout(act);
        goalBox.setOrientation(LinearLayout.VERTICAL);
        goalBox.setPadding(ui.dp(4), ui.dp(12), ui.dp(4), 0);
        tvGoal = ui.text("", 13, t.sub, false);
        goalBar = new GoalBarView(act, t);
        goalBox.addView(tvGoal);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-1, ui.dp(12));
        glp.topMargin = ui.dp(7);
        goalBox.addView(goalBar, glp);
        c.addView(goalBox, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout streakRow = new LinearLayout(act);
        streakRow.setGravity(Gravity.CENTER);
        streakRow.setLayoutDirection(L.dir());
        streakRow.setPadding(0, ui.dp(10), 0, 0);
        flame = new FlameView(act, t);
        streakRow.addView(flame, new LinearLayout.LayoutParams(ui.dp(26), ui.dp(32)));
        tvStreak = ui.text("", 14, t.accent, true);
        tvStreak.setPadding(ui.dp(8), 0, 0, 0);
        streakRow.addView(tvStreak);
        c.addView(streakRow);

        tvPhrase = ui.text("اللهم صلِّ وسلِّم وبارك على سيدنا محمد", 15, Color.WHITE, false);
        tvPhrase.setPadding(ui.dp(6), ui.dp(14), ui.dp(6), ui.dp(2));
        tvPhrase.setLineSpacing(0, 1.25f);
        c.addView(tvPhrase);

        counter.setOnClickListener(v -> {
            Stats.add(act);
            Haptics.count(act, ui.st);
            tvPhrase.setAlpha(0f);
            tvPhrase.setText(Salawat.next(p));
            tvPhrase.animate().alpha(1f).setDuration(450).start();
        });
        TextView session = ui.outlineButton("وضع الجلسة (ذكر متواصل)");
        session.setOnClickListener(v -> act.openSession());
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, ui.dp(44));
        slp.topMargin = ui.dp(12);
        c.addView(session, slp);
        content.addView(c);
        refreshCount(false);
    }

    // ------------------------------------------------------------ مواقيت الصلاة

    private TextView tvNext, tvCountdown, tvLoc, tvSunrise;
    private final TextView[] pName = new TextView[5], pTime = new TextView[5];
    private final LinearLayout[] pBox = new LinearLayout[5];
    private VenomButton locBtn;
    private int shownNext = -2;

    private void buildPrayerCard() {
        Theme t = ui.t;
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("مواقيت الصلاة"));
        tvLoc = ui.hint("");
        c.addView(tvLoc);
        tvNext = ui.text("", 20, t.accent, true);
        tvCountdown = ui.text("", 15, Color.WHITE, false);
        c.addView(tvNext);
        c.addView(tvCountdown);
        LinearLayout row = new LinearLayout(act);
        row.setLayoutDirection(L.dir());
        row.setPadding(0, ui.dp(12), 0, ui.dp(4));
        for (int i = 0; i < 5; i++) {
            LinearLayout b = new LinearLayout(act);
            b.setOrientation(LinearLayout.VERTICAL);
            b.setGravity(Gravity.CENTER);
            b.setPadding(ui.dp(2), ui.dp(10), ui.dp(2), ui.dp(10));
            pName[i] = ui.text(PrayerTimes.NAMES[i], 12, t.sub, true);
            pTime[i] = ui.text("", 12, Color.WHITE, true);
            b.addView(pName[i]);
            b.addView(pTime[i]);
            pBox[i] = b;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.setMargins(ui.dp(2), 0, ui.dp(2), 0);
            row.addView(b, lp);
        }
        c.addView(row, new LinearLayout.LayoutParams(-1, -2));
        tvSunrise = ui.hint("");
        c.addView(tvSunrise);
        locBtn = ui.venom("تحديد موقعي لحساب المواقيت", true);
        locBtn.setOnClickListener(v -> act.requestLocationFlow());
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, ui.dp(48) + 2 * ui.venomPad());
        c.addView(locBtn, blp);
        content.addView(c);
        refreshPrayer();
    }

    /** يحدّث الأوقات والصلاة القادمة والعدّ التنازلي (يُستدعى كل ثانية). */
    void refreshPrayer() {
        if (tvNext == null) return;
        Theme t = ui.t;
        java.util.Calendar now = PrayerTimes.today();
        int[] ts = PrayerTimes.minutes(p, now);
        int nowSec = now.get(java.util.Calendar.HOUR_OF_DAY) * 3600 + now.get(java.util.Calendar.MINUTE) * 60
                + now.get(java.util.Calendar.SECOND);
        int next = -1;
        for (int i = 0; i < 5; i++) {
            if (ts[i] * 60 > nowSec) {
                next = i;
                break;
            }
        }
        long diff;
        if (next < 0) {
            java.util.Calendar tm = PrayerTimes.today();
            tm.add(java.util.Calendar.DAY_OF_MONTH, 1);
            next = 0;
            diff = (24 * 3600 - nowSec) + PrayerTimes.minutes(p, tm)[0] * 60L;
        } else {
            diff = ts[next] * 60L - nowSec;
        }
        boolean needLoc = p.prayerAuto() && !p.hasLocation();
        locBtn.setVisibility(needLoc ? View.VISIBLE : View.GONE);
        tvLoc.setText(p.prayerAuto() && p.hasLocation()
                ? L.f("الموقع: {0}", p.locName().isEmpty() ? "—" : p.locName())
                : (needLoc ? L.t("حدّد موقعك لتُحسب المواقيت تلقائيًا، أو أدخلها يدويًا من الإعدادات.")
                : L.t("أوقات يدوية من الإعدادات")));
        tvNext.setText(L.f("الصلاة القادمة: {0}", L.t(PrayerTimes.NAMES[next])));
        long h = diff / 3600, m = (diff % 3600) / 60, s = diff % 60;
        tvCountdown.setText(L.f("بعد {0}", HijriUtil.digits(h) + ":" + HijriUtil.digits(m / 10) + HijriUtil.digits(m % 10)
                + ":" + HijriUtil.digits(s / 10) + HijriUtil.digits(s % 10)));
        for (int i = 0; i < 5; i++) pTime[i].setText(HijriUtil.clock(ts[i]));
        if (shownNext != next) {
            shownNext = next;
            for (int i = 0; i < 5; i++) {
                if (i == next) {
                    pBox[i].setBackground(ui.shape(Ui.K_CHIP).gradient(false, t.accentLight, t.accentDark));
                    pName[i].setTextColor(t.onAccent);
                    pTime[i].setTextColor(t.onAccent);
                } else {
                    pBox[i].setBackground(ui.shape(Ui.K_CHIP).solid(0x12FFFFFF));
                    pName[i].setTextColor(t.sub);
                    pTime[i].setTextColor(Color.WHITE);
                }
            }
        }
        int sr = PrayerTimes.sunrise(p, now);
        tvSunrise.setText(sr >= 0 ? L.f("الشروق: {0}", HijriUtil.clock(sr)) : "");
        tvSunrise.setVisibility(sr >= 0 ? View.VISIBLE : View.GONE);
    }

    private void buildWidgetCard() {
        Theme t = ui.t;
        widgetCard = ui.card();
        widgetCard.addView(ui.sectionTitle("ويدجت الشاشة الرئيسية"));
        widgetCard.addView(ui.hint("ضع التاريخ الهجري وعدّاد صلواتك على شاشة هاتفك، واضغط الزر داخله لتصلّي على النبي بلمسة."));

        LinearLayout pv = new LinearLayout(act);
        pv.setOrientation(LinearLayout.VERTICAL);
        pv.setGravity(Gravity.CENTER);
        pv.setPadding(ui.dp(18), ui.dp(14), ui.dp(18), ui.dp(14));
        pv.setBackground(ui.shape(Ui.K_BANNER).gradient(false, t.bgTop, t.bgBottom)
                .stroke(Theme.a(t.accent, 0x88), ui.dp(1)));
        pvHijri = ui.text("", 15, t.accent, true);
        pvCount = ui.text("٠", 30, Color.WHITE, true);
        pvLabel = ui.text("", 11, t.sub, false);
        TextView pvBtn = ui.text("صلِّ على النبي", 12, t.onAccent, true);
        pvBtn.setBackground(ui.shape(Ui.K_CHIP).gradient(true, t.accentDark, t.accentLight, t.accentDark));
        pvBtn.setPadding(ui.dp(18), ui.dp(6), ui.dp(18), ui.dp(6));
        pv.addView(pvHijri);
        pv.addView(pvCount);
        pv.addView(pvLabel);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-2, -2);
        blp.topMargin = ui.dp(6);
        pv.addView(pvBtn, blp);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ui.dp(210), -2);
        plp.topMargin = ui.dp(12);
        plp.gravity = Gravity.CENTER_HORIZONTAL;
        widgetCard.addView(pv, plp);
        ObjectAnimator fl = ObjectAnimator.ofFloat(pv, "translationY", 0f, -ui.dp(5));
        fl.setDuration(1800);
        fl.setRepeatMode(ValueAnimator.REVERSE);
        fl.setRepeatCount(ValueAnimator.INFINITE);
        fl.setInterpolator(new AccelerateDecelerateInterpolator());
        Motion.loop(fl);

        VenomButton add = ui.venom("إضافة الويدجت للشاشة", false);
        add.setOnClickListener(v -> WidgetPin.request(act));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, ui.dp(50) + 2 * ui.venomPad());
        alp.topMargin = ui.dp(8);
        widgetCard.addView(add, alp);
        content.addView(widgetCard);
    }

    private void refreshWidgetCard() {
        if (widgetCard == null) return;
        widgetCard.setVisibility(WidgetPin.hasWidget(act) ? View.GONE : View.VISIBLE);
        pvHijri.setText(HijriUtil.hijri(p.hijriAdjust()));
        pvCount.setText(HijriUtil.num(Stats.today(p)));
        pvLabel.setText(p.goalEnabled() ? L.f("من هدف {0} صلاة اليوم", HijriUtil.num(p.goal())) : L.t("صلواتك اليوم"));
    }

    private void buildDevicesCard() {
        Theme t = ui.t;
        devCard = new LinearLayout(act);
        devCard.setGravity(Gravity.CENTER);
        devCard.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
        devCard.setBackground(ui.shape(Ui.K_BANNER).solid(0x18FFFFFF)
                .stroke(Theme.a(t.accent, 0x44), ui.dp(1)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(8));
        devCard.setLayoutParams(lp);

        View dot = new View(act);
        GradientDrawable dg = new GradientDrawable();
        dg.setShape(GradientDrawable.OVAL);
        dg.setColor(0xFF3DDC84);
        dot.setBackground(dg);
        devCard.addView(dot, new LinearLayout.LayoutParams(ui.dp(9), ui.dp(9)));
        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(dot,
                PropertyValuesHolder.ofFloat("alpha", 1f, 0.25f),
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.5f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.5f));
        pulse.setDuration(1100);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        Motion.loop(pulse);

        tvDevices = ui.text("", 14, Color.WHITE, true);
        tvDevices.setPadding(ui.dp(10), 0, 0, 0);
        devCard.addView(tvDevices);
        devCard.setVisibility(DeviceCounter.configured() ? View.VISIBLE : View.GONE);
        content.addView(devCard);
    }

    // ------------------------------------------------------------------ تحديث

    private void refreshDates() {
        if (tvHijri == null) return;
        tvDay.setText(L.f("يوم {0}", HijriUtil.dayName()));
        tvHijri.setText(HijriUtil.hijri(p.hijriAdjust()));
        tvGreg.setText(HijriUtil.gregorian());
        tvTime.setText(HijriUtil.time());
        int a = p.hijriAdjust();
        tvAdj.setText(a == 0 ? L.digits("٠") : (a > 0 ? "+" : "−") + HijriUtil.digits(Math.abs(a)));
    }

    private void refreshBanner() {
        Occasions.Occ o = Occasions.onDay(p, 0);
        if (o != null) {
            tvBannerTitle.setText(L.f("اليوم: {0}", L.t(o.name)));
            tvBannerMsg.setText(L.t(o.msg));
            showBanner();
        } else if (HijriUtil.isFridayWindow()) {
            tvBannerTitle.setText(HijriUtil.fridayLabel());
            tvBannerMsg.setText("«أكثروا من الصلاة عليَّ يوم الجمعة وليلة الجمعة»");
            showBanner();
        } else {
            banner.setVisibility(View.GONE);
        }
    }

    private void showBanner() {
        if (banner.getVisibility() != View.VISIBLE) {
            banner.setVisibility(View.VISIBLE);
        }
    }

    void refreshCount(boolean pulse) {
        if (tvCount == null) return;
        long total = p.count();
        if (pulse && shownCount >= 0 && total != shownCount) {
            ui.countUp(tvCount, shownCount, total, L.t("«cnt»") + " ", "");
            tvCount.setScaleX(1.3f);
            tvCount.setScaleY(1.3f);
            tvCount.animate().scaleX(1f).scaleY(1f).setDuration(320).start();
        } else {
            tvCount.setText(L.f("عدد صلواتك: {0}", HijriUtil.num(total)));
        }
        shownCount = total;
        if (tvStreak != null) {
            int sk = Stats.streak(p);
            tvStreak.setText(sk > 0 ? L.f("سلسلة {0} يوم متتالية", HijriUtil.num(sk)) : L.t("ابدأ سلسلتك اليوم"));
            flame.setLit(sk > 0);
        }

        if (p.goalEnabled()) {
            long today = Stats.today(p);
            int g = p.goal();
            goalBox.setVisibility(View.VISIBLE);
            tvGoal.setText(L.f("هدف اليوم: {0} من {1}", HijriUtil.num(today), HijriUtil.num(g))
                    + (today >= g ? "  ✓" : ""));
            goalBar.setProgress((float) today / g, true);
        } else {
            goalBox.setVisibility(View.GONE);
        }
    }

    private void refreshDevices() {
        if (!DeviceCounter.configured()) return;
        long cached = p.devTotal();
        if (cached >= 0) {
            ui.countUp(tvDevices, shownDevices, cached, L.t("«run»") + " ", " " + L.t("«dev»"));
            shownDevices = cached;
        } else {
            tvDevices.setText("جارٍ حساب عدد الأجهزة...");
        }
        DeviceCounter.fetch(act, false, (total, active) -> {
            if (total >= 0 && total != shownDevices) {
                ui.countUp(tvDevices, shownDevices, total, L.t("«run»") + " ", " " + L.t("«dev»"));
                shownDevices = total;
            } else if (total < 0 && shownDevices == 0) {
                tvDevices.setText("عدّاد الأجهزة غير متاح الآن");
            }
        });
    }

    @Override
    void onShow() {
        refreshDates();
        refreshBanner();
        goalBar.setProgress(0f, false);
        refreshCount(false);
        refreshWidgetCard();
        refreshDevices();
    }

    @Override
    void tick() {
        refreshDates();
        refreshPrayer();
    }
}
