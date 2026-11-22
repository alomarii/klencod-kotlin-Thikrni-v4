package com.alikhwa.thikrni;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import java.util.ArrayList;
import java.util.List;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** الإعدادات: التذكير، أوقات الصمت، الجمعة، الهدف، الأذكار والمناسبات، المظهر، المشاركة. */
final class SettingsPage extends Page {
    private static final int[] CHIP_MINS = {5, 10, 15, 30, 60, 120, 180, 0};
    private static final String[] CHIP_LABELS = {"٥ دقائق", "١٠ دقائق", "١٥ دقيقة", "٣٠ دقيقة",
            "ساعة", "ساعتان", "٣ ساعات", "مخصص"};
    private static final int[] FRI_MINS = {5, 10, 15, 30};
    private static final String[] FRI_LABELS = {"٥ د", "١٠ د", "١٥ د", "٣٠ د"};
    private static final int[] GOAL_VALUES = {50, 100, 300, 500, 1000, 0};
    private static final String[] GOAL_LABELS = {"٥٠", "١٠٠", "٣٠٠", "٥٠٠", "١٠٠٠", "مخصص"};

    private Ui.Chips intervalChips, goalChips, fridayChips;
    private TextView btnFrom, btnTo, tvQuiet;
    private LinearLayout quietRow, goalBox, fridayBox;

    SettingsPage(MainActivity a, Ui ui) {
        super(a, ui);
        build();
    }

    @Override
    void build() {
        content.addView(ui.pageTitle("الإعدادات", null));
        section("🔔", "التذكير", "الفاصل، أوقات الصمت، ليلة الجمعة", true, () -> {
            buildReminderCard();
            buildQuietCard();
            buildFridayCard();
        });
        section("🕌", "مواقيت الصلاة", "الموقع، طريقة الحساب، التذكير بعد الصلاة", false, this::buildPrayerCard);
        section("🎯", "الأهداف والتحفيز", "الهدف اليومي، السلسلة، صيغة الصلاة", false, () -> {
            buildGoalCard();
            buildExtrasCard();
            buildFormulaCard();
        });
        section("🎨", "المظهر واللغة", "الثيمات، اللغة، الحركة والاهتزاز", false, () -> {
            buildThemeCard();
            buildLanguageCard();
            buildMotionCard();
        });
        section("🧰", "الأدوات والبيانات", "الويدجت، الخلفية، النسخ الاحتياطي", false, () -> {
            buildAppCard();
            buildBackupCard();
        });
        section("ℹ️", "عن التطبيق", "المشاركة والمطوّر", false, this::buildFooter);
    }

    // ------------------------------------------------------- أقسام قابلة للطي

    private LinearLayout target;
    private final List<LinearLayout> bodies = new ArrayList<>();
    private final List<TextView> chevrons = new ArrayList<>();

    private void put(View v) {
        (target != null ? target : content).addView(v);
    }

    /** قسم بترويسة (أيقونة + عنوان + وصف + سهم) يُفتح ويُغلق بحركة؛ يُفتح قسم واحد في كل مرة. */
    private void section(String icon, String title, String sub, boolean open, Runnable fill) {
        final LinearLayout head = new LinearLayout(act);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setLayoutDirection(L.dir());
        head.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
        head.setBackground(ui.shape(Ui.K_BANNER)
                .gradient(true, Theme.a(ui.t.accent, 0x40), Theme.a(ui.t.accent, 0x14))
                .stroke(Theme.a(ui.t.accent, 0x77), ui.dp(1)));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, -2);
        hlp.setMargins(ui.dp(16), ui.dp(10), ui.dp(16), ui.dp(4));
        TextView ic = ui.text(icon, 20, Color.WHITE, false);
        ic.setBackground(ui.shape(Ui.K_SMALL).solid(Theme.a(ui.t.accent, 0x44)));
        ic.setGravity(Gravity.CENTER);
        head.addView(ic, new LinearLayout.LayoutParams(ui.dp(42), ui.dp(42)));
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(ui.dp(12), 0, ui.dp(8), 0);
        TextView tt = ui.text(title, 17, Color.WHITE, true);
        tt.setGravity(Gravity.START);
        TextView ss = ui.text(sub, 12, ui.t.sub, false);
        ss.setGravity(Gravity.START);
        col.addView(tt);
        col.addView(ss);
        head.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView chev = ui.text("⌄", 24, ui.t.accent, true);
        head.addView(chev);
        ui.pressEffect(head);

        final LinearLayout body = new LinearLayout(act);
        body.setOrientation(LinearLayout.VERTICAL);
        target = body;
        fill.run();
        target = null;
        body.setVisibility(open ? View.VISIBLE : View.GONE);
        chev.setRotation(open ? 180f : 0f);
        bodies.add(body);
        chevrons.add(chev);
        head.setOnClickListener(v -> {
            ui.tick(v);
            boolean willOpen = body.getVisibility() != View.VISIBLE;
            for (int i = 0; i < bodies.size(); i++) {
                LinearLayout o = bodies.get(i);
                if (o != body && o.getVisibility() == View.VISIBLE) setOpen(o, chevrons.get(i), false);
            }
            setOpen(body, chev, willOpen);
        });
        content.addView(head, hlp);
        content.addView(body, new LinearLayout.LayoutParams(-1, -2));
    }

    private void setOpen(final LinearLayout body, TextView chev, boolean open) {
        chev.animate().rotation(open ? 180f : 0f).setDuration(Motion.ms(320, ui.st))
                .setInterpolator(Motion.SPR_SOFT).start();
        final ViewGroup.LayoutParams lp = body.getLayoutParams();
        if (open) {
            body.measure(View.MeasureSpec.makeMeasureSpec(act.getResources().getDisplayMetrics().widthPixels,
                    View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            final int target = body.getMeasuredHeight();
            lp.height = 0;
            body.setVisibility(View.VISIBLE);
            body.setAlpha(0f);
            ValueAnimator va = ValueAnimator.ofInt(0, target);
            va.setDuration(Motion.ms(380, ui.st));
            va.setInterpolator(new DecelerateInterpolator(2f));
            va.addUpdateListener(a -> {
                lp.height = (int) a.getAnimatedValue();
                body.setAlpha(a.getAnimatedFraction());
                body.setLayoutParams(lp);
            });
            va.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    body.setAlpha(1f);
                    body.setLayoutParams(lp);
                }
            });
            va.start();
        } else {
            final int from = body.getHeight();
            ValueAnimator va = ValueAnimator.ofInt(from, 0);
            va.setDuration(Motion.ms(300, ui.st));
            va.setInterpolator(new DecelerateInterpolator(1.6f));
            va.addUpdateListener(a -> {
                lp.height = (int) a.getAnimatedValue();
                body.setAlpha(1f - a.getAnimatedFraction());
                body.setLayoutParams(lp);
            });
            va.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    body.setVisibility(View.GONE);
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    body.setAlpha(1f);
                    body.setLayoutParams(lp);
                }
            });
            va.start();
        }
    }

    // ---------------------------------------------------------------- التذكير

    private void buildReminderCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("التذكير"));
        c.addView(ui.switchRow("تفعيل التذكير", p.enabled(), b -> {
            p.setEnabled(b);
            ReminderScheduler.syncAll(act);
        }));
        c.addView(ui.divider());

        TextView every = ui.text("ذكّرني كل:", 15, Color.WHITE, true);
        every.setGravity(Gravity.START);
        every.setPadding(0, ui.dp(4), 0, ui.dp(6));
        c.addView(every);
        intervalChips = ui.chips(CHIP_LABELS, 4, -1, this::onIntervalChip);
        c.addView(intervalChips);
        refreshIntervalChips();

        c.addView(ui.divider());
        c.addView(ui.switchRow("عند توصيل الشاحن", p.onCharge(), b -> {
            p.setOnCharge(b);
            ReminderScheduler.syncAll(act);
        }));
        c.addView(ui.switchRow("عند فتح قفل الهاتف", p.onUnlock(), b -> {
            p.setOnUnlock(b);
            ReminderScheduler.syncAll(act);
        }));
        c.addView(ui.switchRow("الاهتزاز", p.vibrate(), b -> p.setVibrate(b)));
        c.addView(ui.switchRow("عدّ تلقائي لكل تذكير يُسمَع", p.autoCount(), b -> p.setAutoCount(b)));
        c.addView(ui.hint("عند التفعيل يُضاف كل تذكير يُشغَّل إلى عدّاد صلواتك وإحصائياتك تلقائيًا."));
        put(c);
    }

    private void refreshIntervalChips() {
        int cur = p.intervalMin();
        int sel = 7;
        for (int i = 0; i < 7; i++) if (CHIP_MINS[i] == cur) sel = i;
        intervalChips.select(sel);
        intervalChips.setLabel(7, sel == 7 ? L.f("كل {0} د", HijriUtil.digits(cur)) : L.t("مخصص"));
    }

    private void onIntervalChip(int idx) {
        if (CHIP_MINS[idx] > 0) {
            p.setIntervalMin(CHIP_MINS[idx]);
            ReminderScheduler.syncAll(act);
            refreshIntervalChips();
            return;
        }
        askNumber("ذكّرني كل كم دقيقة؟", "عدد الدقائق", 1, 1440, v -> {
            p.setIntervalMin(v);
            ReminderScheduler.syncAll(act);
            refreshIntervalChips();
        });
    }

    private interface OnNumber {
        void got(int v);
    }

    private void askNumber(String title, String hint, final int min, final int max, final OnNumber cb) {
        final EditText et = new EditText(act);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setHint(L.t(hint));
        et.setGravity(Gravity.CENTER);
        new AlertDialog.Builder(act)
                .setTitle(L.t(title))
                .setView(et)
                .setPositiveButton(L.t("حفظ"), (d, w) -> {
                    try {
                        int v = Integer.parseInt(et.getText().toString().trim());
                        cb.got(Math.max(min, Math.min(max, v)));
                    } catch (Exception ignored) {
                    }
                })
                .setNegativeButton(L.t("إلغاء"), null)
                .show();
    }

    // ------------------------------------------------------------ أوقات الصمت

    private void buildQuietCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("أوقات الصمت"));
        c.addView(ui.switchRow("لا تزعجني في هذه الساعات", p.quietEnabled(), b -> {
            p.setQuietEnabled(b);
            quietRow.animate().alpha(b ? 1f : 0.4f).setDuration(250).start();
            refreshQuiet();
        }));

        quietRow = new LinearLayout(act);
        quietRow.setOrientation(LinearLayout.HORIZONTAL);
        quietRow.setPadding(0, ui.dp(8), 0, 0);
        btnFrom = ui.outlineButton("");
        btnTo = ui.outlineButton("");
        btnFrom.setOnClickListener(v -> pickTime(true));
        btnTo.setOnClickListener(v -> pickTime(false));
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, ui.dp(46), 1f);
        l1.setMargins(0, 0, ui.dp(5), 0);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(0, ui.dp(46), 1f);
        l2.setMargins(ui.dp(5), 0, 0, 0);
        quietRow.addView(btnFrom, l1);
        quietRow.addView(btnTo, l2);
        quietRow.setAlpha(p.quietEnabled() ? 1f : 0.4f);
        c.addView(quietRow, new LinearLayout.LayoutParams(-1, -2));

        tvQuiet = ui.hint("");
        c.addView(tvQuiet);
        put(c);
        refreshQuiet();
    }

    private void pickTime(final boolean from) {
        int cur = from ? p.quietStart() : p.quietEnd();
        new TimePickerDialog(act, (view, h, m) -> {
            if (from) p.setQuietStart(h * 60 + m);
            else p.setQuietEnd(h * 60 + m);
            refreshQuiet();
        }, cur / 60, cur % 60, false).show();
    }

    private void refreshQuiet() {
        btnFrom.setText(L.f("من: {0}", HijriUtil.clock(p.quietStart())));
        btnTo.setText(L.f("إلى: {0}", HijriUtil.clock(p.quietEnd())));
        if (!p.quietEnabled()) {
            tvQuiet.setText("فعّل الخيار لإيقاف الصوت والتنبيهات في ساعات النوم أو العمل مثلًا.");
        } else if (Quiet.isQuiet(p)) {
            tvQuiet.setText(L.f("نحن الآن في وقت الصمت، لن يُشغَّل أي تذكير حتى الساعة {0}", HijriUtil.clock(p.quietEnd())));
        } else {
            tvQuiet.setText("لن يُشغَّل أي صوت أو تنبيه بين هذين الوقتين. العدّ اليدوي يعمل دائمًا.");
        }
    }

    // ------------------------------------------------------------------ الجمعة

    private void buildFridayCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("ليلة الجمعة ويومها"));
        c.addView(ui.switchRow("تكثيف التذكير", p.fridayBoost(), b -> {
            p.setFridayBoost(b);
            fridayBox.animate().alpha(b ? 1f : 0.4f).setDuration(250).start();
            ReminderScheduler.syncAll(act);
        }));
        fridayBox = new LinearLayout(act);
        fridayBox.setOrientation(LinearLayout.VERTICAL);
        TextView lbl = ui.text("ذكّرني كل:", 14, Color.WHITE, true);
        lbl.setGravity(Gravity.START);
        lbl.setPadding(0, ui.dp(6), 0, ui.dp(4));
        fridayBox.addView(lbl);
        int sel = -1;
        for (int i = 0; i < FRI_MINS.length; i++) if (FRI_MINS[i] == p.fridayInterval()) sel = i;
        fridayChips = ui.chips(FRI_LABELS, 4, sel, i -> {
            p.setFridayInterval(FRI_MINS[i]);
            fridayChips.select(i);
            ReminderScheduler.syncAll(act);
        });
        fridayBox.addView(fridayChips);
        fridayBox.setAlpha(p.fridayBoost() ? 1f : 0.4f);
        c.addView(fridayBox, new LinearLayout.LayoutParams(-1, -2));
        c.addView(ui.hint("يسرّع التذكير من مساء الخميس (٦ م) حتى مساء الجمعة (٦ م) تقريبًا، تذكيرًا بفضل الصلاة على النبي في هذا الوقت."));
        put(c);
    }

    // ------------------------------------------------------------ الهدف اليومي

    private void buildGoalCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("الهدف اليومي"));
        c.addView(ui.switchRow("تفعيل الهدف", p.goalEnabled(), b -> {
            p.setGoalEnabled(b);
            goalBox.animate().alpha(b ? 1f : 0.4f).setDuration(250).start();
            SalawatWidget.updateAll(act);
        }));
        goalBox = new LinearLayout(act);
        goalBox.setOrientation(LinearLayout.VERTICAL);
        goalChips = ui.chips(GOAL_LABELS, 3, -1, this::onGoalChip);
        goalBox.addView(goalChips);
        goalBox.setAlpha(p.goalEnabled() ? 1f : 0.4f);
        c.addView(goalBox, new LinearLayout.LayoutParams(-1, -2));
        c.addView(ui.hint("يظهر شريط تقدّم متحرك في الرئيسية، وعند إتمام الهدف تُعرض احتفالية وتصلك إشعار خاص."));
        put(c);
        refreshGoalChips();
    }

    private void refreshGoalChips() {
        int cur = p.goal();
        int sel = 5;
        for (int i = 0; i < 5; i++) if (GOAL_VALUES[i] == cur) sel = i;
        goalChips.select(sel);
        goalChips.setLabel(5, sel == 5 ? HijriUtil.num(cur) : "مخصص");
    }

    private void onGoalChip(int idx) {
        if (GOAL_VALUES[idx] > 0) {
            p.setGoal(GOAL_VALUES[idx]);
            refreshGoalChips();
            SalawatWidget.updateAll(act);
            return;
        }
        askNumber("كم صلاة تريد كل يوم؟", "عدد الصلوات", 1, 100000, v -> {
            p.setGoal(v);
            refreshGoalChips();
            SalawatWidget.updateAll(act);
        });
    }

    // ------------------------------------------------------- الأذكار والمناسبات

    private void buildExtrasCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("أذكار ومناسبات"));
        c.addView(ui.switchRow("أذكار قصيرة بين التذكيرات", p.adhkar(), b -> {
            p.setAdhkar(b);
            ReminderScheduler.syncAll(act);
        }));
        c.addView(ui.hint("إشعار هادئ بدون صوت بذكر قصير (استغفار، تسبيح...) في منتصف المدة بين تذكيرين."));
        c.addView(ui.divider());
        c.addView(ui.switchRow("تنبيه حماية السلسلة (٨ مساءً)", p.streakWarn(), b -> {
            p.setStreakWarn(b);
            ReminderScheduler.scheduleStreak(act);
        }));
        c.addView(ui.hint("إن لم تُتمّ هدف اليوم وكانت لديك سلسلة متتالية يصلك تنبيه لطيف قبل أن تنقطع."));
        c.addView(ui.divider());
        c.addView(ui.switchRow("تنبيه المناسبات الهجرية", p.occasions(), b -> {
            p.setOccasions(b);
            ReminderScheduler.scheduleOccasion(act);
        }));
        c.addView(ui.hint("المولد النبوي ورمضان والعيدان وغيرها، تُحسب تلقائيًا من التاريخ الهجري وتصلك صباحًا. يمكنك تعديل الهجري ±٣ أيام من الرئيسية."));
        put(c);
    }

    // --------------------------------------------------------------- صيغة الصلاة

    private void buildFormulaCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("صيغة الصلاة على النبي"));
        final TextView cur = ui.hint(formulaLabel());
        TextView pick = ui.outlineButton("اختيار الصيغة المفضلة");
        pick.setOnClickListener(v -> {
            String[] items = new String[Salawat.LIST.length + 1];
            items[0] = L.t("تبديل تلقائي بين الصيغ");
            System.arraycopy(Salawat.LIST, 0, items, 1, Salawat.LIST.length);
            new AlertDialog.Builder(act)
                    .setTitle(L.t("اختر الصيغة"))
                    .setSingleChoiceItems(items, p.formula() + 1, (dlg, which) -> {
                        p.setFormula(which - 1);
                        cur.setText(formulaLabel());
                        dlg.dismiss();
                    })
                    .setNegativeButton(L.t("إلغاء"), null)
                    .show();
        });
        c.addView(pick, new LinearLayout.LayoutParams(-1, ui.dp(46)));
        c.addView(cur);
        put(c);
    }

    private String formulaLabel() {
        int f = p.formula();
        return f >= 0 && f < Salawat.LIST.length ? L.f("الصيغة المختارة: {0}", Salawat.LIST[f])
                : L.t("تتبدّل الصيغة تلقائيًا مع كل ضغطة.");
    }

    // ------------------------------------------------------- مواقيت الصلاة

    private LinearLayout prayerBox;
    private TextView tvLocName;
    private VenomButton locBtn;

    private String locLabel() {
        if (!p.hasLocation()) return L.t("لم يُحدَّد الموقع بعد");
        String n = p.locName();
        return L.f("الموقع: {0}", n.isEmpty() ? "—" : n);
    }

    private void buildPrayerCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("الموقع ومواقيت الصلاة"));
        tvLocName = ui.hint(locLabel());
        c.addView(tvLocName);
        locBtn = ui.venom(p.hasLocation() ? "تحديث موقعي" : "تحديد موقعي (طلب إذن الموقع)", false);
        locBtn.setOnClickListener(v -> act.requestLocationFlow());
        c.addView(locBtn, new LinearLayout.LayoutParams(-1, ui.dp(50) + 2 * ui.venomPad()));
        c.addView(ui.hint("يُستخدم موقعك التقريبي مرة واحدة لحساب المواقيت فلكيًا دون إنترنت، ويبقى على جهازك ولا يُرسل لأي جهة."));
        c.addView(ui.divider());
        c.addView(ui.switchRow("حساب المواقيت تلقائيًا من موقعي", p.prayerAuto(), b -> {
            p.setPrayerAuto(b);
            prayerChanged();
        }));
        prayerBox = new LinearLayout(act);
        prayerBox.setOrientation(LinearLayout.VERTICAL);
        c.addView(prayerBox, new LinearLayout.LayoutParams(-1, -2));
        fillPrayerBox();
        put(c);

        LinearLayout r = ui.card();
        r.addView(ui.sectionTitle("التذكير بعد الصلاة"));
        r.addView(ui.switchRow("تذكير بعد كل صلاة", p.prayerOn(), b -> {
            p.setPrayerOn(b);
            ReminderScheduler.schedulePrayers(act);
        }));
        r.addView(ui.hint("يذكّرك التطبيق بالصلاة على النبي بعد كل صلاة بالمدة التي تحددها (يلتزم بأوقات الصمت)."));
        r.addView(ui.hint("المدة بعد الصلاة:"));
        final int[] mins = {0, 5, 10, 15, 30};
        String[] labels = {"مباشرة", "٥ د", "١٠ د", "١٥ د", "٣٠ د"};
        int sel = 2;
        for (int i = 0; i < mins.length; i++) if (mins[i] == p.prayerAfter()) sel = i;
        final Ui.Chips[] holder = new Ui.Chips[1];
        holder[0] = ui.chips(labels, 5, sel, i -> {
            p.setPrayerAfter(mins[i]);
            holder[0].select(i);
            ReminderScheduler.schedulePrayers(act);
        });
        r.addView(holder[0]);
        put(r);
    }

    private void prayerChanged() {
        ReminderScheduler.schedulePrayers(act);
        fillPrayerBox();
        act.refreshPrayerViews();
    }

    /** يُستدعى بعد تحديد الموقع أو معرفة اسم المدينة. */
    void refreshLocation() {
        if (tvLocName == null) return;
        tvLocName.setText(locLabel());
        locBtn.setText(p.hasLocation() ? "تحديث موقعي" : "تحديد موقعي (طلب إذن الموقع)");
        fillPrayerBox();
    }

    private void fillPrayerBox() {
        prayerBox.removeAllViews();
        if (p.prayerAuto()) {
            if (!p.hasLocation()) {
                prayerBox.addView(ui.hint("حدّد موقعك أولًا ليُحسب الوقت تلقائيًا، أو عطّل الحساب التلقائي لإدخال الأوقات يدويًا."));
                return;
            }
            TextView m = ui.text("طريقة الحساب", 14, Color.WHITE, true);
            m.setGravity(Gravity.START);
            m.setPadding(0, ui.dp(8), 0, ui.dp(6));
            prayerBox.addView(m);
            final Ui.Chips[] mc = new Ui.Chips[1];
            mc[0] = ui.chips(PrayerTimes.METHODS, 1, p.calcMethod(), i -> {
                p.setCalcMethod(i);
                mc[0].select(i);
                prayerChanged();
            });
            prayerBox.addView(mc[0]);
            TextView a = ui.text("وقت العصر", 14, Color.WHITE, true);
            a.setGravity(Gravity.START);
            a.setPadding(0, ui.dp(10), 0, ui.dp(6));
            prayerBox.addView(a);
            final Ui.Chips[] ac = new Ui.Chips[1];
            ac[0] = ui.chips(new String[]{"الجمهور (ظل المثل)", "الحنفي (ظل المثلين)"}, 2, p.asrHanafi() ? 1 : 0, i -> {
                p.setAsrHanafi(i == 1);
                ac[0].select(i);
                prayerChanged();
            });
            prayerBox.addView(ac[0]);
            int[] t = PrayerTimes.minutes(p, PrayerTimes.today());
            prayerBox.addView(ui.divider());
            prayerBox.addView(timeRow("الفجر", t[0]));
            int sr = PrayerTimes.sunrise(p, PrayerTimes.today());
            if (sr >= 0) prayerBox.addView(timeRow("الشروق", sr));
            prayerBox.addView(timeRow("الظهر", t[1]));
            prayerBox.addView(timeRow("العصر", t[2]));
            prayerBox.addView(timeRow("المغرب", t[3]));
            prayerBox.addView(timeRow("العشاء", t[4]));
        } else {
            prayerBox.addView(ui.hint("أدخل أوقات الصلوات الخمس في مدينتك:"));
            for (int i = 0; i < 5; i++) {
                final int idx = i;
                final TextView btn = ui.outlineButton(L.t(PrayerTimes.NAMES[i]) + "   " + HijriUtil.clock(p.prayerTime(i)));
                btn.setOnClickListener(v -> {
                    int m = p.prayerTime(idx);
                    new TimePickerDialog(act, (view, h, mi) -> {
                        p.setPrayerTime(idx, h * 60 + mi);
                        btn.setText(L.t(PrayerTimes.NAMES[idx]) + "   " + HijriUtil.clock(h * 60 + mi));
                        ReminderScheduler.schedulePrayers(act);
                        act.refreshPrayerViews();
                    }, m / 60, m % 60, false).show();
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, ui.dp(44));
                lp.topMargin = ui.dp(8);
                prayerBox.addView(btn, lp);
            }
        }
    }

    private View timeRow(String name, int minutes) {
        LinearLayout r = new LinearLayout(act);
        r.setLayoutDirection(L.dir());
        r.setPadding(0, ui.dp(6), 0, ui.dp(6));
        TextView n = ui.text(name, 15, ui.t.sub, false);
        n.setGravity(Gravity.START);
        TextView v = ui.text(HijriUtil.clock(minutes), 16, Color.WHITE, true);
        v.setGravity(Gravity.END);
        r.addView(n, new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(v, new LinearLayout.LayoutParams(0, -2, 1f));
        return r;
    }

    // ----------------------------------------------------------------- اللغة

    private void buildLanguageCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("اللغة"));
        String[] labels = new String[L.NAMES.length + 1];
        System.arraycopy(L.NAMES, 0, labels, 0, L.NAMES.length);
        labels[L.NAMES.length] = "لغة الهاتف";
        int sel = p.lang().isEmpty() ? L.NAMES.length : L.index();
        final Ui.Chips[] ch = new Ui.Chips[1];
        ch[0] = ui.chips(labels, 3, sel, i -> {
            p.setLang(i < L.CODES.length ? L.CODES[i] : "");
            L.init(act);
            SalawatWidget.updateAll(act);
            act.recreate();
        });
        c.addView(ch[0]);
        c.addView(ui.hint("يتبدّل اتجاه الواجهة تلقائيًا؛ من اليمين لليسار للعربية والأردية، ومن اليسار لليمين لغيرهما. تبقى الأذكار وصيغ الصلاة على النبي بالعربية."));
        put(c);
    }

    // ----------------------------------------------------------- حركة واهتزاز

    private void buildMotionCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("الحركة والاهتزاز"));
        c.addView(ui.switchRow("تخفيف الحركة", p.reduceMotion(), b -> {
            p.setReduceMotion(b);
            Motion.init(act);
            act.recreate();
        }));
        c.addView(ui.hint("يوقف الخلفية المتحركة والتأثيرات الثقيلة ويبسّط الانتقالات؛ يوفّر البطارية ويناسب الهواتف القديمة. يُفعَّل تلقائيًا إن أوقفتَ الحركة من إعدادات الهاتف."));
        c.addView(ui.divider());
        c.addView(ui.switchRow("اهتزاز عند الضغط على الزر", p.haptics(), b -> p.setHaptics(b)));
        c.addView(ui.hint("لكل مظهر نبضة اهتزاز خاصة به."));
        put(c);
    }

    // ------------------------------------------------------------ نسخ احتياطي

    private void buildBackupCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("نسخ احتياطي"));
        TextView ex = ui.outlineButton("حفظ نسخة احتياطية");
        ex.setOnClickListener(v -> act.startBackupExport());
        c.addView(ex, new LinearLayout.LayoutParams(-1, ui.dp(46)));
        TextView im = ui.outlineButton("استعادة من نسخة");
        im.setOnClickListener(v -> act.startBackupImport());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, ui.dp(46));
        lp.topMargin = ui.dp(8);
        c.addView(im, lp);
        c.addView(ui.hint("تحفظ العدّاد والإحصائيات والإنجازات والإعدادات في ملف تنقله إلى هاتف جديد. لا تتضمن التسجيلات الصوتية."));
        put(c);
    }

    // ------------------------------------------------------------------ المظهر

    private void buildThemeCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("المظهر"));
        c.addView(ui.hint("لكل مظهر شكل أزرار وحركة وخلفية وانتقالات خاصة به."));
        c.addView(ui.switchRow("مظهر تلقائي", p.autoTheme(), b -> {
            p.setAutoTheme(b);
            act.recreate();
        }));
        c.addView(ui.hint("يتحول إلى الذهبي في ليلة الجمعة ويومها، وإلى الأزرق الليلي من ٩ مساءً حتى ٥ فجرًا، وإلا يبقى المظهر الذي اخترته."));
        final int perRow = 4;
        LinearLayout row = null;
        for (int i = 0; i < Theme.NAMES.length; i++) {
            if (i % perRow == 0) {
                row = new LinearLayout(act);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutDirection(L.dir());
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
                rp.topMargin = ui.dp(8);
                c.addView(row, rp);
            }
            final int idx = i;
            Theme th = Theme.of(i);
            LinearLayout item = new LinearLayout(act);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            View sw = new View(act);
            boolean sel = p.theme() == i;
            // معاينة بشكل أزرار هذا الثيم نفسه
            sw.setBackground(ui.shapeFor(th.st, Ui.K_SMALL).gradient(true, th.bgTop, th.bgMid, th.accent)
                    .stroke(sel ? Color.WHITE : Theme.a(th.accent, 0x88), ui.dp(sel ? 3 : 1)));
            if (sel) {
                sw.setScaleX(1.1f);
                sw.setScaleY(1.1f);
            }
            item.addView(sw, new LinearLayout.LayoutParams(ui.dp(50), ui.dp(50)));
            TextView nm = ui.text(Theme.NAMES[i], 12, sel ? ui.t.accent : ui.t.sub, sel);
            nm.setPadding(0, ui.dp(6), 0, 0);
            nm.setSingleLine(true);
            item.addView(nm);
            TextView tag = ui.text(Style.TAGS[i], 10, ui.t.sub, false);
            tag.setAlpha(0.8f);
            item.addView(tag);
            ui.pressEffect(item);
            item.setOnClickListener(v -> {
                if (p.theme() != idx) {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
                    p.setTheme(idx);
                    act.applyTheme(v);
                }
            });
            row.addView(item, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        // إكمال الصف الأخير بفراغات
        int rem = Theme.NAMES.length % perRow;
        if (rem != 0 && row != null) {
            for (int k = rem; k < perRow; k++) row.addView(new View(act), new LinearLayout.LayoutParams(0, 1, 1f));
        }
        put(c);
    }

    // ------------------------------------------------------------------ التطبيق

    private void buildAppCard() {
        LinearLayout c = ui.card();
        c.addView(ui.sectionTitle("التطبيق"));

        VenomButton widget = ui.venom("إضافة الويدجت للشاشة الرئيسية", true);
        widget.setOnClickListener(v -> WidgetPin.request(act));
        c.addView(widget, new LinearLayout.LayoutParams(-1, ui.dp(48) + 2 * ui.venomPad()));
        c.addView(ui.hint("ويدجت يعرض التاريخ الهجري والميلادي وعدّاد اليوم، وبه زر للصلاة على النبي. تتبع ألوانه المظهر المختار."));
        VenomButton mini = ui.venom("إضافة ويدجت الزر الصغير", true);
        mini.setOnClickListener(v -> WidgetPin.requestMini(act));
        c.addView(mini, new LinearLayout.LayoutParams(-1, ui.dp(48) + 2 * ui.venomPad()));
        c.addView(ui.hint("ويدجت مصغّر بحجم أيقونة: عدّاد اليوم وزر الصلاة فقط."));
        c.addView(ui.divider());

        TextView bat = ui.outlineButton("السماح بالعمل في الخلفية (مهم)");
        bat.setOnClickListener(v -> {
            try {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + act.getPackageName()));
                act.startActivity(i);
            } catch (Exception e) {
                try {
                    act.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                } catch (Exception ignored) {
                }
            }
        });
        c.addView(bat, new LinearLayout.LayoutParams(-1, ui.dp(46)));

        VenomButton share = ui.venom("مشاركة التطبيق", false);
        share.setOnClickListener(v -> {
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("text/plain");
            s.putExtra(Intent.EXTRA_SUBJECT, L.t("تطبيق ذكرني"));
            s.putExtra(Intent.EXTRA_TEXT, L.t("اللهم صلِّ وسلِّم على نبينا محمد\nجرّب تطبيق «ذكرني» لتذكيرك بالصلاة على النبي صلى الله عليه وسلم، ومن دلّ على خير فله مثل أجر فاعله:\n") + AppConfig.SHARE_URL);
            act.startActivity(Intent.createChooser(s, L.t("مشاركة التطبيق")));
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, ui.dp(50) + 2 * ui.venomPad());
        lp.topMargin = ui.dp(4);
        c.addView(share, lp);
        c.addView(ui.hint("زر المشاركة يرسل رابط التطبيق مع دعاء، وهو ثواب جارٍ للجميع بإذن الله."));
        put(c);
    }

    private void buildFooter() {
        LinearLayout c = ui.card();
        TextView hadith = ui.text("«من صلى عليَّ صلاةً صلى الله عليه بها عشرًا»", 15, Color.WHITE, false);
        hadith.setLineSpacing(0, 1.25f);
        c.addView(hadith);
        c.addView(ui.divider());
        c.addView(ui.text("برمجة وتطوير", 13, ui.t.sub, false));
        c.addView(ui.text("محمد العمري", 24, ui.t.accent, true));
        TextView phone = ui.text("730215825", 18, Color.WHITE, true);
        phone.setPadding(ui.dp(10), ui.dp(6), ui.dp(10), ui.dp(6));
        phone.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        phone.setOnClickListener(v -> {
            try {
                act.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:730215825")));
            } catch (Exception ignored) {
            }
        });
        c.addView(phone);
        c.addView(ui.text(AppConfig.VERSION_LABEL, 11, ui.t.sub, false));
        put(c);
    }

    @Override
    void onShow() {
        refreshQuiet();
    }
}
