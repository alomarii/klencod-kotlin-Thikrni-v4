package com.alikhwa.thikrni;

import android.icu.util.IslamicCalendar;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/** التاريخ الهجري (أم القرى) والميلادي بالعربية. */
public final class HijriUtil {
    private HijriUtil() {}

    public static final String[] HIJRI_MONTHS = {
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"};

    private static final String[] GREG_MONTHS = {
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};

    private static final String[] DAYS = {
            "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"};

    private static final String[] SHORT_DAYS = {
            "أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت"};

    private static Calendar now() {
        return new GregorianCalendar();
    }

    public static String digits(long n) {
        String s = String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') sb.append((char) ('٠' + (ch - '0')));
            else sb.append(ch);
        }
        return L.digits(sb.toString());
    }

    /** رقم بأرقام اللغة الحالية مع فاصل الآلاف. */
    public static String num(long n) {
        String s = String.valueOf(Math.abs(n));
        StringBuilder sb = new StringBuilder();
        int cnt = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            sb.append(s.charAt(i));
            if (++cnt % 3 == 0 && i > 0) sb.append('٬');
        }
        sb.reverse();
        return (n < 0 ? "-" : "") + L.digits(digits2(sb.toString()));
    }

    private static String digits2(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') sb.append((char) ('٠' + (ch - '0')));
            else sb.append(ch);
        }
        return sb.toString();
    }

    public static String dayName() {
        return L.t(DAYS[now().get(Calendar.DAY_OF_WEEK) - 1]);
    }

    public static String shortDay(int dayOfWeek) {
        return L.t(SHORT_DAYS[Math.max(1, Math.min(7, dayOfWeek)) - 1]);
    }

    /** تقويم أم القرى لليوم الحالي مع إزاحة بالأيام. */
    public static IslamicCalendar newIslamic(int adjustDays) {
        IslamicCalendar ic = new IslamicCalendar();
        ic.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        ic.setTime(new Date());
        if (adjustDays != 0) ic.add(android.icu.util.Calendar.DATE, adjustDays);
        return ic;
    }

    /** اسم الشهر الهجري بلغة التطبيق. */
    public static String monthName(int m) {
        return L.t(HIJRI_MONTHS[Math.max(0, Math.min(11, m))]);
    }

    public static String hijri(int adjustDays) {
        IslamicCalendar ic = newIslamic(adjustDays);
        int d = ic.get(android.icu.util.Calendar.DAY_OF_MONTH);
        int m = ic.get(android.icu.util.Calendar.MONTH);
        int y = ic.get(android.icu.util.Calendar.YEAR);
        return digits(d) + " " + monthName(m) + " " + digits(y) + " " + L.t("«ah»");
    }

    public static boolean isFriday() {
        return now().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
    }

    /** من مساء الخميس (٦ م) إلى مساء الجمعة (٦ م) تقريبًا: ليلة الجمعة ويومها. */
    public static boolean isFridayWindow() {
        Calendar c = now();
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int h = c.get(Calendar.HOUR_OF_DAY);
        return (dow == Calendar.THURSDAY && h >= 18) || (dow == Calendar.FRIDAY && h < 18);
    }

    public static String fridayLabel() {
        return L.t(now().get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY ? "ليلة الجمعة المباركة" : "يوم الجمعة المبارك");
    }

    public static String gregorian() {
        Calendar c = now();
        return digits(c.get(Calendar.DAY_OF_MONTH)) + " " + L.t(GREG_MONTHS[c.get(Calendar.MONTH)])
                + " " + digits(c.get(Calendar.YEAR)) + " " + L.t("«ce»");
    }

    public static String time() {
        Calendar c = now();
        int h = c.get(Calendar.HOUR);
        if (h == 0) h = 12;
        int m = c.get(Calendar.MINUTE);
        int s = c.get(Calendar.SECOND);
        boolean pm = c.get(Calendar.AM_PM) == Calendar.PM;
        return digits(h) + ":" + digits(m / 10) + digits(m % 10) + ":" + digits(s / 10) + digits(s % 10)
                + " " + L.t(pm ? "«pm»" : "«am»");
    }

    /** وقت من دقائق اليوم، مثل ١٠:٣٠ م. */
    public static String clock(int minutesOfDay) {
        int h24 = (minutesOfDay / 60) % 24;
        int m = minutesOfDay % 60;
        boolean pm = h24 >= 12;
        int h = h24 % 12;
        if (h == 0) h = 12;
        return digits(h) + ":" + digits(m / 10) + digits(m % 10) + " " + L.t(pm ? "«pm»" : "«am»");
    }

    public static String daysPhrase(int days) {
        if (days <= 0) return L.t("اليوم");
        if (days == 1) return L.t("غدًا");
        if (days == 2) return L.t("بعد يومين");
        if (days <= 10) return L.f("بعد {0} أيام", digits(days));
        return L.f("بعد {0} يومًا", digits(days));
    }
}
