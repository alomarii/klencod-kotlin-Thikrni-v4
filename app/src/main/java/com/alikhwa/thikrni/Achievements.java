package com.alikhwa.thikrni;

/** الإنجازات والأوسمة. */
public final class Achievements {
    private Achievements() {}

    static final class Def {
        final String id, title, desc;
        final int type; // 0 إجمالي، 1 أطول سلسلة، 2 صلوات في يوم واحد
        final long need;

        Def(String id, String title, String desc, int type, long need) {
            this.id = id;
            this.title = title;
            this.desc = desc;
            this.type = type;
            this.need = need;
        }
    }

    static final Def[] ALL = {
            new Def("t100", "البداية", "١٠٠ صلاة على النبي", 0, 100),
            new Def("t1000", "ألف صلاة", "١٠٠٠ صلاة على النبي", 0, 1000),
            new Def("t10000", "عشرة آلاف", "١٠٬٠٠٠ صلاة على النبي", 0, 10000),
            new Def("t100000", "مئة ألف", "١٠٠٬٠٠٠ صلاة على النبي", 0, 100000),
            new Def("s1", "أول هدف", "أتممت هدفك اليومي", 1, 1),
            new Def("s3", "ثلاثة أيام", "٣ أيام متتالية", 1, 3),
            new Def("s7", "أسبوع كامل", "٧ أيام متتالية", 1, 7),
            new Def("s30", "شهر متواصل", "٣٠ يومًا متتاليًا", 1, 30),
            new Def("s100", "مئة يوم", "١٠٠ يوم متتالية", 1, 100),
            new Def("d500", "يوم مبارك", "٥٠٠ صلاة في يوم واحد", 2, 500)
    };

    /** عنوان ووصف آخر إنجاز مفتوح حديثًا (للنافذة المنبثقة). */
    static volatile String lastTitle, lastDesc;

    static boolean unlocked(Prefs p, String id) {
        return p.raw().getLong("ach_" + id, 0) > 0;
    }

    static int unlockedCount(Prefs p) {
        int n = 0;
        for (Def d : ALL) if (unlocked(p, d.id)) n++;
        return n;
    }

    /** يفحص الإنجازات الجديدة؛ يعيد عنوان آخر إنجاز فُتح أو null. */
    static String check(android.content.Context c, Prefs p, boolean dayDone) {
        long total = p.count();
        long today = Stats.today(p);
        int best = dayDone ? Stats.bestStreak(p) : -1;
        String title = null;
        for (Def d : ALL) {
            if (unlocked(p, d.id)) continue;
            boolean ok;
            switch (d.type) {
                case 0:
                    ok = total >= d.need;
                    break;
                case 1:
                    ok = best >= d.need;
                    break;
                default:
                    ok = today >= d.need;
                    break;
            }
            if (ok) {
                p.raw().edit().putLong("ach_" + d.id, System.currentTimeMillis()).apply();
                title = d.title;
                lastTitle = d.title;
                lastDesc = d.desc;
            }
        }
        return title;
    }

    /** يأخذ الإنجاز المعلّق (مرة واحدة) لعرضه. */
    static String[] take() {
        String t = lastTitle;
        if (t == null) return null;
        String[] r = {t, lastDesc};
        lastTitle = null;
        lastDesc = null;
        return r;
    }
}
