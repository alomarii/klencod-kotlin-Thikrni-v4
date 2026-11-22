package com.alikhwa.thikrni;

import java.util.Calendar;
import java.util.GregorianCalendar;

/** أوقات الصمت: ساعات لا يعمل فيها التذكير. */
public final class Quiet {
    private Quiet() {}

    public static boolean isQuiet(Prefs p) {
        if (!p.quietEnabled()) return false;
        int s = p.quietStart(), e = p.quietEnd();
        if (s == e) return false;
        Calendar c = new GregorianCalendar();
        int now = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        return s < e ? (now >= s && now < e) : (now >= s || now < e);
    }
}
