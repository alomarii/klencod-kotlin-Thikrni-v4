package com.alikhwa.thikrni;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.animation.ValueAnimator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** الشاشة الرئيسية: أربع صفحات بشريط تنقل سفلي وانتقالات متحركة. */
public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 21;
    private static final int REQ_BK_EXPORT = 31;
    private static final int REQ_BK_IMPORT = 32;
    private static final int REQ_LOC = 41;
    private static final String STATE_TAB = "tab";
    private static final String STATE_SCROLL = "scroll";

    private Prefs p;
    private Ui ui;
    private final Handler h = new Handler(Looper.getMainLooper());

    private FrameLayout root, pager;
    private BottomNav nav;
    private final Page[] pages = new Page[4];
    private Page curPage;
    private int current = -1;
    private boolean celebrating = false;
    private FrameLayout session;
    private TextView sessionCount, sessionToday;
    private int sessionN = 0;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (curPage != null) curPage.tick();
            h.postDelayed(this, 1000);
        }
    };

    private final Stats.Listener statsListener = reached -> {
        HomePage home = (HomePage) pages[0];
        if (home != null) home.refreshCount(true);
        StatsPage st = (StatsPage) pages[1];
        if (st != null && current == 1) st.refreshValues(false);
        if (reached) celebrate();
        if (session != null) updateSession();
        String[] ach = Achievements.take();
        if (ach != null) showAchievement(ach[0], ach[1]);
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(L.wrap(base));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        p = new Prefs(this);
        Motion.init(this);
        Theme t = Theme.current(this);
        ui = new Ui(this, t);
        getWindow().setStatusBarColor(t.bgTop);
        getWindow().setNavigationBarColor(t.bgBottom);

        Tracks.migrateLegacy(this);

        root = new FrameLayout(this);
        root.setLayoutDirection(L.dir());
        root.addView(new StarsView(this, t), new FrameLayout.LayoutParams(-1, -1));

        pager = new FrameLayout(this);
        root.addView(pager, new FrameLayout.LayoutParams(-1, -1));

        int startTab = b != null ? b.getInt(STATE_TAB, 0) : 0;
        nav = new BottomNav(this, ui, startTab, i -> showTab(i, true, true));
        FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(-1, ui.dp(68),
                Gravity.BOTTOM);
        nlp.setMargins(ui.dp(14), 0, ui.dp(14), ui.dp(12));
        root.addView(nav, nlp);
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
        ReminderScheduler.syncAll(this);
        DeviceCounter.register(this);

        boolean cold = b == null;
        showTab(startTab, false, !cold);
        if (cold) {
            curPage.prepareEntrance();
            showSplash();
        } else {
            final int sc = b.getInt(STATE_SCROLL, 0);
            if (sc > 0) {
                final View sv = curPage.root;
                sv.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        sv.getViewTreeObserver().removeOnPreDrawListener(this);
                        sv.scrollTo(0, sc);
                        return true;
                    }
                });
            }
            ThemeReveal.playIfPending(this, root, t);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(STATE_TAB, Math.max(0, current));
        if (curPage != null) out.putInt(STATE_SCROLL, curPage.root.getScrollY());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Stats.listener = statsListener;
        if (p.effectiveTheme() != ui.t.id) {
            // تغيّر المظهر التلقائي (جمعة/ليل) أثناء غياب التطبيق
            recreate();
            return;
        }
        if (curPage != null) curPage.onShow();
        SalawatWidget.updateAll(this);
        h.post(tick);
    }

    @Override
    protected void onPause() {
        Stats.listener = null;
        h.removeCallbacks(tick);
        if (curPage != null) curPage.onHide();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (session != null) {
            closeSession();
            return;
        }
        if (current > 0) {
            nav.select(0, true);
            showTab(0, true, true);
        } else {
            super.onBackPressed();
        }
    }

    // ------------------------------------------------------------- التبويبات

    private Page page(int i) {
        if (pages[i] == null) {
            switch (i) {
                case 0:
                    pages[i] = new HomePage(this, ui);
                    break;
                case 1:
                    pages[i] = new StatsPage(this, ui);
                    break;
                case 2:
                    pages[i] = new AudioPage(this, ui);
                    break;
                default:
                    pages[i] = new SettingsPage(this, ui);
                    break;
            }
        }
        return pages[i];
    }

    private void showTab(int i, boolean animate, boolean entrance) {
        if (i == current && curPage != null) return;
        final Page prev = curPage;
        int prevIdx = current;
        final Page next = page(i);
        current = i;
        curPage = next;
        if (prev != null) prev.onHide();
        next.onShow();

        final View nv = next.root;
        if (nv.getParent() == null) pager.addView(nv, new FrameLayout.LayoutParams(-1, -1));
        nv.animate().cancel();
        nv.setVisibility(View.VISIBLE);
        next.root.scrollTo(0, 0);

        if (!animate || prev == null) {
            nv.setAlpha(1f);
            nv.setTranslationX(0f);
            nv.setScaleX(1f);
            nv.setScaleY(1f);
            if (entrance) next.playEntrance();
            return;
        }

        // الصفحة التالية في الترتيب تدخل من اليسار (واجهة عربية)؛ نوع الانتقال يتبع الثيم
        final View ov = prev.root;
        Motion.transition(ui.st, nv, ov, (i > prevIdx ? -1 : 1) * (L.rtl() ? 1 : -1), () -> {
            if (curPage == null || ov != curPage.root) {
                ov.setVisibility(View.GONE);
                Motion.reset(ov);
            }
        });
        next.playEntrance();
    }

    // ---------------------------------------------------------------- مساعدات

    void pickAudio(boolean anyFile) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        if (!anyFile) {
            i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "application/ogg",
                    "application/x-ogg", "video/mp4", "video/3gpp"});
        }
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_AUDIO);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res == RESULT_OK && data != null && data.getData() != null
                && (req == REQ_BK_EXPORT || req == REQ_BK_IMPORT)) {
            try {
                if (req == REQ_BK_EXPORT) {
                    Backup.export(this, data.getData());
                    android.widget.Toast.makeText(this, L.t("تم حفظ النسخة الاحتياطية"), android.widget.Toast.LENGTH_LONG).show();
                } else {
                    int n = Backup.restore(this, data.getData());
                    android.widget.Toast.makeText(this, L.f("تمت استعادة {0} عنصرًا", HijriUtil.num(n)), android.widget.Toast.LENGTH_LONG).show();
                    ReminderScheduler.syncAll(this);
                    SalawatWidget.updateAll(this);
                    recreate();
                }
            } catch (Exception e) {
                android.widget.Toast.makeText(this, L.t("تعذّرت العملية: الملف غير صالح أو لا يمكن الوصول إليه"), android.widget.Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (req == REQ_AUDIO && res == RESULT_OK && data != null) {
            List<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    uris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }
            ((AudioPage) page(2)).onPicked(uris);
        }
    }

    // ------------------------------------------------------ الموقع والمواقيت

    /** شرح سبب الإذن ثم طلبه، ثم تحديد الموقع وحساب المواقيت. */
    void requestLocationFlow() {
        if (LocationHelper.granted(this)) {
            fetchLocation();
            return;
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle(L.t("إذن الموقع"))
                .setMessage(L.t("نحتاج موقعك التقريبي مرة واحدة لحساب مواقيت الصلاة في مدينتك. يبقى الموقع على جهازك ولا يُرسل لأي جهة، ويمكنك بدلًا من ذلك إدخال الأوقات يدويًا."))
                .setPositiveButton(L.t("متابعة"), (d, w) -> requestPermissions(
                        new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC))
                .setNegativeButton(L.t("ليس الآن"), null)
                .show();
    }

    private void fetchLocation() {
        android.widget.Toast.makeText(this, L.t("جارٍ تحديد موقعك..."), android.widget.Toast.LENGTH_SHORT).show();
        LocationHelper.fetch(this, ok -> {
            if (ok) {
                p.setPrayerAuto(true);
                ReminderScheduler.schedulePrayers(this);
                android.widget.Toast.makeText(this, L.t("تم تحديد الموقع وحساب المواقيت"), android.widget.Toast.LENGTH_LONG).show();
                refreshPrayerViews();
            } else {
                android.widget.Toast.makeText(this, L.t("تعذّر تحديد الموقع، تأكد من تفعيل خدمة الموقع"), android.widget.Toast.LENGTH_LONG).show();
            }
        }, this::refreshPrayerViews);
    }

    /** يحدّث بطاقة المواقيت في الرئيسية وقسم الإعدادات. */
    void refreshPrayerViews() {
        if (pages[0] != null) ((HomePage) pages[0]).refreshPrayer();
        if (pages[3] != null) ((SettingsPage) pages[3]).refreshLocation();
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req != REQ_LOC) return;
        if (res.length > 0 && res[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else if (!shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
            new android.app.AlertDialog.Builder(this)
                    .setMessage(L.t("لم يُمنح إذن الموقع. يمكنك منحه من إعدادات التطبيق، أو إدخال أوقات الصلاة يدويًا."))
                    .setPositiveButton(L.t("فتح الإعدادات"), (d, w) -> startActivity(new Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:" + getPackageName()))))
                    .setNegativeButton(L.t("إلغاء"), null)
                    .show();
        } else {
            android.widget.Toast.makeText(this, L.t("لم يُمنح إذن الموقع، يمكنك إدخال الأوقات يدويًا"),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------- نسخ احتياطي

    void startBackupExport() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "thikrni-backup.json");
        startActivityForResult(i, REQ_BK_EXPORT);
    }

    void startBackupImport() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, REQ_BK_IMPORT);
    }

    // ---------------------------------------------------------- وضع الجلسة

    /** شاشة كاملة للذكر المتواصل: الزر الكبير وعدّاد الجلسة فقط. */
    void openSession() {
        if (session != null) return;
        final Theme t = ui.t;
        session = new FrameLayout(this);
        session.setClickable(true);
        session.addView(new StarsView(this, t), new FrameLayout.LayoutParams(-1, -1));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        sessionCount = ui.text("٠", 64, Color.WHITE, true);
        sessionToday = ui.text("", 15, t.sub, false);
        box.addView(ui.text("وضع الجلسة", 20, t.accent, true));
        box.addView(sessionCount);
        box.addView(sessionToday);
        GooCounterView big = new GooCounterView(this, t);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, ui.dp(330));
        blp.topMargin = ui.dp(12);
        box.addView(big, blp);
        final TextView phrase = ui.text(Salawat.next(p), 15, Color.WHITE, false);
        phrase.setPadding(ui.dp(24), ui.dp(8), ui.dp(24), ui.dp(8));
        phrase.setLineSpacing(0, 1.25f);
        box.addView(phrase);
        TextView done = ui.outlineButton("إنهاء الجلسة");
        done.setOnClickListener(v -> closeSession());
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ui.dp(200), ui.dp(46));
        dlp.topMargin = ui.dp(16);
        box.addView(done, dlp);
        session.addView(box, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        big.setOnClickListener(v -> {
            sessionN++;
            Stats.add(this);
            Haptics.count(this, t.st);
            phrase.setAlpha(0f);
            phrase.setText(Salawat.next(p));
            phrase.animate().alpha(1f).setDuration(Motion.ms(450, t.st)).start();
            updateSession();
        });
        sessionN = 0;
        updateSession();
        root.addView(session, new FrameLayout.LayoutParams(-1, -1));
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        session.setAlpha(0f);
        session.setScaleX(1.08f);
        session.setScaleY(1.08f);
        session.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(Motion.ms(450, t.st))
                .setInterpolator(new DecelerateInterpolator(2f)).start();
    }

    private void updateSession() {
        if (sessionCount == null) return;
        sessionCount.setText(HijriUtil.num(sessionN));
        sessionToday.setText(L.f("اليوم: {0} • الإجمالي: {1}", HijriUtil.num(Stats.today(p)), HijriUtil.num(p.count())));
    }

    private void closeSession() {
        final FrameLayout s = session;
        if (s == null) return;
        session = null;
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        s.animate().alpha(0f).scaleX(1.06f).scaleY(1.06f).setDuration(Motion.ms(320, ui.st))
                .withEndAction(() -> root.removeView(s)).start();
        HomePage home = (HomePage) pages[0];
        if (home != null) home.refreshCount(false);
    }

    // ------------------------------------------------------------ إنجاز جديد

    private void showAchievement(String title, String desc) {
        final Theme t = ui.t;
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setBackground(ui.shape(Ui.K_BANNER).gradient(false, Theme.a(t.bgMid, 0xF5), Theme.a(t.bgBottom, 0xF5))
                .stroke(t.accent, ui.dp(2)));
        card.setPadding(ui.dp(22), ui.dp(14), ui.dp(22), ui.dp(14));
        card.addView(ui.text("إنجاز جديد ★", 13, t.accentLight, true));
        card.addView(ui.text(L.t(title), 22, t.accent, true));
        card.addView(ui.text(desc == null ? "" : L.t(desc), 13, t.sub, false));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.topMargin = ui.dp(40);
        root.addView(card, lp);
        card.setTranslationY(-ui.dp(160));
        card.setAlpha(0f);
        card.animate().translationY(0f).alpha(1f).setDuration(Motion.ms(700, t.st))
                .setInterpolator(Motion.SPR).start();
        root.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
        h.postDelayed(() -> card.animate().translationY(-ui.dp(160)).alpha(0f).setDuration(400)
                .withEndAction(() -> root.removeView(card)).start(), 3400);
    }

    /** تطبيق ثيم جديد: التقاط الشاشة ثم إعادة البناء وكشف دائري/معيّني/ستارة من موضع الضغط. */
    void applyTheme(View origin) {
        int[] o = new int[2], r = new int[2];
        origin.getLocationInWindow(o);
        root.getLocationInWindow(r);
        float ox = o[0] + origin.getWidth() / 2f - r[0];
        float oy = o[1] + origin.getHeight() / 2f - r[1];
        ThemeReveal.capture(this, root, ox, oy, () -> {
            SalawatWidget.updateAll(this);
            recreate();
            overridePendingTransition(0, 0);
        });
    }

    // ------------------------------------------------------------ شاشة البداية

    private void showSplash() {
        final Theme t = ui.t;
        final FrameLayout sp = new FrameLayout(this);
        sp.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{t.bgTop, t.bgMid, t.bgBottom}));
        sp.setClickable(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        final ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo_alikhwa);
        box.addView(logo, new LinearLayout.LayoutParams(ui.dp(130), ui.dp(130)));
        final TextView title = ui.text("ذكرني", 44, t.accent, true);
        title.setPadding(0, ui.dp(12), 0, 0);
        final LinearGradient shader = new LinearGradient(0, 0, ui.dp(260), 0,
                new int[]{t.accentDark, t.accentLight, t.accent, t.accentDark},
                new float[]{0f, 0.35f, 0.6f, 1f}, Shader.TileMode.REPEAT);
        title.getPaint().setShader(shader);
        final Matrix m = new Matrix();
        ValueAnimator shimmer = ValueAnimator.ofFloat(0f, ui.dp(260));
        shimmer.setDuration(1400);
        shimmer.setRepeatCount(ValueAnimator.INFINITE);
        shimmer.setInterpolator(new LinearInterpolator());
        shimmer.addUpdateListener(a -> {
            m.setTranslate((float) a.getAnimatedValue(), 0);
            shader.setLocalMatrix(m);
            title.invalidate();
        });
        shimmer.start();
        final TextView verse = ui.text("اللهم صلِّ وسلِّم على نبينا محمد", 16, t.sub, false);
        verse.setPadding(0, ui.dp(6), 0, 0);
        box.addView(title);
        box.addView(verse);
        sp.addView(box, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        sp.addView(new RingsView(this, t), 0, new FrameLayout.LayoutParams(-1, -1));
        root.addView(sp, new FrameLayout.LayoutParams(-1, -1));

        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);
        logo.setAlpha(0f);
        logo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(750)
                .setInterpolator(new OvershootInterpolator(1.6f)).start();
        title.setAlpha(0f);
        title.setTranslationY(ui.dp(16));
        title.animate().alpha(1f).translationY(0).setStartDelay(300).setDuration(600).start();
        verse.setAlpha(0f);
        verse.animate().alpha(1f).setStartDelay(600).setDuration(600).start();

        h.postDelayed(() -> {
            sp.animate().alpha(0f).scaleX(1.12f).scaleY(1.12f).setDuration(Motion.ms(520, t.st))
                    .setInterpolator(new AccelerateInterpolator()).withEndAction(() -> {
                shimmer.cancel();
                root.removeView(sp);
            }).start();
            if (curPage != null) curPage.playEntrance();
        }, 1700);
    }

    // ----------------------------------------------------------------- احتفال

    private void celebrate() {
        if (celebrating) return;
        celebrating = true;
        final Theme t = ui.t;
        final FrameLayout ov = new FrameLayout(this);
        ConfettiView cf = new ConfettiView(this, t);
        ov.addView(cf, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackground(ui.shape(Ui.K_CARD).gradient(false, Theme.a(t.bgMid, 0xF2), Theme.a(t.bgBottom, 0xF2))
                .stroke(t.accent, ui.dp(2)));
        box.setPadding(ui.dp(28), ui.dp(24), ui.dp(28), ui.dp(24));
        box.addView(ui.text("ما شاء الله!", 32, t.accent, true));
        TextView sub = ui.text(L.f("أتممت هدفك اليومي: {0} صلاة على النبي", HijriUtil.num(p.goal())), 16, Color.WHITE, true);
        sub.setPadding(0, ui.dp(8), 0, ui.dp(4));
        box.addView(sub);
        box.addView(ui.text("تقبّل الله منك، اللهم صلِّ وسلِّم على نبينا محمد", 13, t.sub, false));
        ov.addView(box, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        root.addView(ov, new FrameLayout.LayoutParams(-1, -1));

        box.setScaleX(0.5f);
        box.setScaleY(0.5f);
        box.setAlpha(0f);
        box.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(520)
                .setInterpolator(new OvershootInterpolator(1.8f)).start();
        cf.start();
        root.performHapticFeedback(Build.VERSION.SDK_INT >= 30
                ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.LONG_PRESS);

        h.postDelayed(() -> ov.animate().alpha(0f).setDuration(500).withEndAction(() -> {
            root.removeView(ov);
            celebrating = false;
        }).start(), 3300);
    }
}
