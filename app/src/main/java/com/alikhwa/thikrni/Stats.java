package com.alikhwa.thikrni;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Calendar;
import java.util.GregorianCalendar;

/** إحصائيات الصلاة على النبي (يومي/أسبوعي/شهري) والهدف اليومي. */
public final class Stats {
    private Stats() {}

    public interface Listener {
        void onAdded(boolean goalReached);
    }

    public static volatile Listener listener;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static String key(Calendar c) {
        return "d_" + c.get(Calendar.YEAR)
                + String.format(java.util.Locale.US, "%02d%02d", c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static String todayKey() {
        return key(new GregorianCalendar());
    }

    public static long today(Prefs p) {
        return p.dayCount(todayKey());
    }

    /** آخر ٧ أيام (يشمل اليوم). */
    public static long week(Prefs p) {
        long s = 0;
        for (long v : last7(p)) s += v;
        return s;
    }

    /** الشهر الميلادي الحالي حتى اليوم. */
    public static long month(Prefs p) {
        Calendar c = new GregorianCalendar();
        int today = c.get(Calendar.DAY_OF_MONTH);
        long s = 0;
        for (int i = 0; i < today; i++) {
            s += p.dayCount(key(c));
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return s;
    }

    /** مصفوفة من ٧ قيم: الأقدم أولًا واليوم أخيرًا. */
    public static long[] last7(Prefs p) {
        long[] out = new long[7];
        Calendar c = new GregorianCalendar();
        for (int i = 6; i >= 0; i--) {
            out[i] = p.dayCount(key(c));
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return out;
    }

    /** ترتيب أسماء الأيام المقابلة لـ last7. */
    public static String[] last7Labels() {
        String[] out = new String[7];
        Calendar c = new GregorianCalendar();
        for (int i = 6; i >= 0; i--) {
            out[i] = i == 6 ? "اليوم" : HijriUtil.shortDay(c.get(Calendar.DAY_OF_WEEK));
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return out;
    }

    private static boolean met(Prefs p, Calendar c) {
        return p.dayCount(key(c)) >= (p.goalEnabled() ? p.goal() : 1);
    }

    /** عدد الأيام المتتالية التي أُتمّ فيها الهدف (اليوم يُحتسب إن أُتمّ، وإلا من الأمس). */
    public static int streak(Prefs p) {
        Calendar c = new GregorianCalendar();
        if (!met(p, c)) c.add(Calendar.DAY_OF_MONTH, -1);
        int n = 0;
        while (n < 400 && met(p, c)) {
            n++;
            c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return n;
    }

    /** أطول سلسلة خلال آخر ٤٠٠ يوم. */
    public static int bestStreak(Prefs p) {
        Calendar c = new GregorianCalendar();
        c.add(Calendar.DAY_OF_MONTH, -399);
        int best = 0, run = 0;
        for (int i = 0; i < 400; i++) {
            if (met(p, c)) {
                run++;
                best = Math.max(best, run);
            } else {
                run = 0;
            }
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return best;
    }

    /** هل السلسلة معرّضة للانقطاع اليوم؟ */
    public static boolean streakAtRisk(Prefs p) {
        return streak(p) > 0 && !met(p, new GregorianCalendar());
    }

    /** يضيف صلاة واحدة للعدّاد والإحصائيات. يعيد true إذا بلغ الهدف اليومي للتو. */
    public static boolean add(Context c) {
        Context app = c.getApplicationContext();
        Prefs p = new Prefs(app);
        String tk = todayKey();
        long before = p.dayCount(tk);
        p.incCount();
        p.addDay(tk, 1);
        long after = before + 1;

        // حذف الأيام القديمة (أكثر من ٤٠٠ يوم)
        Calendar old = new GregorianCalendar();
        old.add(Calendar.DAY_OF_MONTH, -400);
        p.removeKey(key(old));

        boolean reached = p.goalEnabled() && before < p.goal() && after >= p.goal()
                && !tk.equals(p.goalAnnouncedDay());
        if (reached) {
            p.setGoalAnnouncedDay(tk);
            // التطبيق مفتوح: تظهر الاحتفالية داخله، وإلا يصل إشعار خاص
            if (listener == null) Notifier.showGoal(app, p.goal());
        }
        boolean dayDone = reached || (!p.goalEnabled() && before == 0);
        String ach = Achievements.check(app, p, dayDone);
        if (ach != null && listener == null) {
            Notifier.showAchievement(app, ach, Achievements.lastDesc);
            Achievements.take();
        }
        SalawatWidget.updateAll(app);
        final Listener l = listener;
        final boolean r = reached;
        if (l != null) MAIN.post(() -> l.onAdded(r));
        return reached;
    }

    public static void resetAll(Context c) {
        Prefs p = new Prefs(c);
        p.setCount(0);
        p.clearDays();
        p.setGoalAnnouncedDay("");
        SalawatWidget.updateAll(c);
    }
}
