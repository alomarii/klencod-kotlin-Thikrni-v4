package com.alikhwa.thikrni;

import android.icu.util.IslamicCalendar;

import java.util.ArrayList;
import java.util.List;

/** المناسبات الهجرية، تُحسب تلقائيًا من التاريخ الهجري. */
public final class Occasions {
    private Occasions() {}

    public static final class Occ {
        public final int month, day; // الشهر من ٠ (محرم) إلى ١١
        public final String name, msg;

        Occ(int month, int day, String name, String msg) {
            this.month = month;
            this.day = day;
            this.name = name;
            this.msg = msg;
        }
    }

    public static final class Up {
        public final Occ occ;
        public final int inDays;

        Up(Occ occ, int inDays) {
            this.occ = occ;
            this.inDays = inDays;
        }

        public String hijriText() {
            return HijriUtil.digits(occ.day) + " " + HijriUtil.monthName(occ.month);
        }
    }

    private static final Occ[] ALL = {
            new Occ(0, 1, "رأس السنة الهجرية", "عامٌ هجريٌّ جديد، نسأل الله أن يجعله عام خير وبركة. أكثِر من الصلاة على النبي صلى الله عليه وسلم"),
            new Occ(0, 10, "يوم عاشوراء", "يوم عاشوراء، ولا تنسَ الصلاة على النبي صلى الله عليه وسلم"),
            new Occ(2, 12, "المولد النبوي الشريف", "ذكرى مولد خير البرية صلى الله عليه وسلم، فأكثِر اليوم من الصلاة والسلام عليه"),
            new Occ(8, 1, "بداية شهر رمضان", "رمضان مبارك، شهر الصيام والقرآن. أكثِر من الصلاة على النبي صلى الله عليه وسلم"),
            new Occ(8, 20, "دخول العشر الأواخر من رمضان", "تبدأ هذه الليلة العشر الأواخر من رمضان، فاجتهد في الدعاء والصلاة على النبي صلى الله عليه وسلم"),
            new Occ(9, 1, "عيد الفطر المبارك", "تقبّل الله طاعاتكم وكل عام وأنتم بخير. اللهم صلِّ وسلِّم على نبينا محمد"),
            new Occ(11, 1, "بداية العشر من ذي الحجة", "أيام عشر ذي الحجة من أفضل أيام العام، فأكثِر من الذكر والصلاة على النبي صلى الله عليه وسلم"),
            new Occ(11, 9, "يوم عرفة", "يوم عرفة يوم الدعاء، فأكثِر من الصلاة على النبي صلى الله عليه وسلم"),
            new Occ(11, 10, "عيد الأضحى المبارك", "تقبّل الله منا ومنكم وكل عام وأنتم بخير. اللهم صلِّ وسلِّم على نبينا محمد")
    };

    private static Occ match(int month, int day) {
        for (Occ o : ALL) if (o.month == month && o.day == day) return o;
        return null;
    }

    /** مناسبة اليوم (offset=0) أو الغد (offset=1)، أو null. */
    public static Occ onDay(Prefs p, int offset) {
        IslamicCalendar ic = HijriUtil.newIslamic(p.hijriAdjust());
        if (offset != 0) ic.add(android.icu.util.Calendar.DATE, offset);
        return match(ic.get(android.icu.util.Calendar.MONTH), ic.get(android.icu.util.Calendar.DAY_OF_MONTH));
    }

    /** أقرب المناسبات القادمة (تشمل اليوم). */
    public static List<Up> upcoming(Prefs p, int n) {
        List<Up> out = new ArrayList<>();
        IslamicCalendar ic = HijriUtil.newIslamic(p.hijriAdjust());
        for (int i = 0; i < 400 && out.size() < n; i++) {
            Occ o = match(ic.get(android.icu.util.Calendar.MONTH), ic.get(android.icu.util.Calendar.DAY_OF_MONTH));
            if (o != null) out.add(new Up(o, i));
            ic.add(android.icu.util.Calendar.DATE, 1);
        }
        return out;
    }
}
