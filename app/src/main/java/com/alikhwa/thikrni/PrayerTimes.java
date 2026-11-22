package com.alikhwa.thikrni;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * حساب مواقيت الصلاة فلكيًا دون إنترنت (خوارزمية PrayTimes المعروفة).
 * الفجر والعشاء بزاويتين حسب الطريقة، والعصر بظل مثل/مثلين، والشروق والمغرب عند ٠٫٨٣٣°.
 */
final class PrayerTimes {
    private PrayerTimes() {}

    static final String[] NAMES = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};

    static final String[] METHODS = {
            "رابطة العالم الإسلامي", "أم القرى (مكة المكرمة)", "الهيئة المصرية العامة للمساحة",
            "جامعة العلوم الإسلامية بكراتشي", "أمريكا الشمالية (ISNA)"};

    // زاوية الفجر، وزاوية العشاء (أو سالب = دقائق بعد المغرب)
    private static final double[][] M = {{18, 17}, {18.5, -90}, {19.5, 17.5}, {18, 18}, {15, 15}};

    // -------------------------------------------------------- رياضيات
    private static double sin(double d) { return Math.sin(Math.toRadians(d)); }
    private static double cos(double d) { return Math.cos(Math.toRadians(d)); }
    private static double tan(double d) { return Math.tan(Math.toRadians(d)); }
    private static double asin(double x) { return Math.toDegrees(Math.asin(x)); }
    private static double acos(double x) { return Math.toDegrees(Math.acos(x)); }
    private static double atan2(double y, double x) { return Math.toDegrees(Math.atan2(y, x)); }
    private static double fix(double a, double b) {
        a = a - b * Math.floor(a / b);
        return a < 0 ? a + b : a;
    }

    private static double julian(int y, int m, int d) {
        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        double A = Math.floor(y / 100.0);
        double B = 2 - A + Math.floor(A / 4.0);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + B - 1524.5;
    }

    /** {الميل، معادلة الزمن}. */
    private static double[] sunPosition(double jd) {
        double D = jd - 2451545.0;
        double g = fix(357.529 + 0.98560028 * D, 360);
        double q = fix(280.459 + 0.98564736 * D, 360);
        double L = fix(q + 1.915 * sin(g) + 0.020 * sin(2 * g), 360);
        double e = 23.439 - 0.00000036 * D;
        double RA = fix(atan2(cos(e) * sin(L), cos(L)) / 15.0, 24);
        double eqt = q / 15.0 - RA;
        double decl = asin(sin(e) * sin(L));
        return new double[]{decl, eqt};
    }

    private static double midDay(double jd, double t) {
        return fix(12 - sunPosition(jd + t)[1], 24);
    }

    private static double sunAngleTime(double jd, double lat, double angle, double t, boolean ccw) {
        double decl = sunPosition(jd + t)[0];
        double noon = midDay(jd, t);
        double x = (-sin(angle) - sin(decl) * sin(lat)) / (cos(decl) * cos(lat));
        if (x > 1 || x < -1) return Double.NaN;
        double v = acos(x) / 15.0;
        return noon + (ccw ? -v : v);
    }

    private static double asrTime(double jd, double lat, double factor, double t) {
        double decl = sunPosition(jd + t)[0];
        double angle = -Math.toDegrees(Math.atan(1.0 / (factor + tan(Math.abs(lat - decl)))));
        return sunAngleTime(jd, lat, angle, t, false);
    }

    /** الأوقات بالساعات: فجر، شروق، ظهر، عصر، مغرب، عشاء (قد تكون NaN في العروض العليا). */
    static double[] compute(Calendar day, double lat, double lng, int method, boolean hanafi) {
        method = Math.max(0, Math.min(M.length - 1, method));
        double jd = julian(day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH))
                - lng / (15.0 * 24.0);
        double tz = TimeZone.getDefault().getOffset(day.getTimeInMillis()) / 3600000.0;
        double[] portions = {5 / 24.0, 6 / 24.0, 12 / 24.0, 13 / 24.0, 18 / 24.0, 18 / 24.0, 19 / 24.0};
        double[] r = new double[6];
        for (int pass = 0; pass < 2; pass++) {
            double fajr = sunAngleTime(jd, lat, M[method][0], portions[0], true);
            double sunrise = sunAngleTime(jd, lat, 0.833, portions[1], true);
            double dhuhr = midDay(jd, portions[2]);
            double asr = asrTime(jd, lat, hanafi ? 2 : 1, portions[3]);
            double sunset = sunAngleTime(jd, lat, 0.833, portions[4], false);
            double isha = M[method][1] < 0 ? Double.NaN : sunAngleTime(jd, lat, M[method][1], portions[6], false);
            double adj = tz - lng / 15.0;
            r[0] = fajr + adj;
            r[1] = sunrise + adj;
            r[2] = dhuhr + adj + 1 / 60.0;
            r[3] = asr + adj;
            r[4] = sunset + adj;
            r[5] = M[method][1] < 0 ? r[4] + (-M[method][1]) / 60.0 : isha + adj;
            if (pass == 0) {
                portions[0] = r[0] / 24.0;
                portions[1] = r[1] / 24.0;
                portions[2] = r[2] / 24.0;
                portions[3] = r[3] / 24.0;
                portions[4] = r[4] / 24.0;
                portions[5] = r[4] / 24.0;
                portions[6] = (M[method][1] < 0 ? r[4] : r[5]) / 24.0;
                if (Double.isNaN(portions[0])) portions[0] = 5 / 24.0;
                if (Double.isNaN(portions[6])) portions[6] = 19 / 24.0;
            }
        }
        return r;
    }

    private static int toMin(double h, int fallback) {
        if (Double.isNaN(h) || Double.isInfinite(h)) return fallback;
        int m = (int) Math.round(fix(h, 24) * 60);
        return m % (24 * 60);
    }

    /** الصلوات الخمس بدقائق اليوم لليوم المعطى: تلقائيًا من الموقع أو يدويًا. */
    static int[] minutes(Prefs p, Calendar day) {
        int[] out = new int[5];
        if (p.prayerAuto() && p.hasLocation()) {
            double[] r = compute(day, p.lat(), p.lon(), p.calcMethod(), p.asrHanafi());
            out[0] = toMin(r[0], p.prayerTime(0));
            out[1] = toMin(r[2], p.prayerTime(1));
            out[2] = toMin(r[3], p.prayerTime(2));
            out[3] = toMin(r[4], p.prayerTime(3));
            out[4] = toMin(r[5], p.prayerTime(4));
        } else {
            for (int i = 0; i < 5; i++) out[i] = p.prayerTime(i);
        }
        return out;
    }

    /** وقت الشروق بدقائق اليوم، أو ‎-١‎ إن لم يكن متاحًا (الوضع اليدوي). */
    static int sunrise(Prefs p, Calendar day) {
        if (!(p.prayerAuto() && p.hasLocation())) return -1;
        double[] r = compute(day, p.lat(), p.lon(), p.calcMethod(), p.asrHanafi());
        return toMin(r[1], -1);
    }

    static Calendar today() {
        return new GregorianCalendar();
    }
}
