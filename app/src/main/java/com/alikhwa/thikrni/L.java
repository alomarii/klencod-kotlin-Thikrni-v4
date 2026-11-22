package com.alikhwa.thikrni;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;

import java.util.HashMap;
import java.util.Locale;

/**
 * اللغات: العربية (الأصل)، الإنجليزية، الفرنسية، التركية، الأردية، الإندونيسية.
 * النصوص العربية مفاتيح لجدول الترجمة (Tr)؛ أي نص غير موجود يبقى كما هو.
 * القوالب تستخدم {0} {1} للقيم المتغيرة عبر L.f.
 */
public final class L {
    private L() {}

    static final String[] CODES = {"ar", "en", "fr", "tr", "ur", "id"};
    static final String[] NAMES = {"العربية", "English", "Français", "Türkçe", "اردو", "Indonesia"};

    private static int idx = 0;
    private static final HashMap<String, String[]> MAP = new HashMap<>();

    static {
        for (String[] r : Tr.ROWS) MAP.put(r[0], r);
    }

    /** يقرأ اللغة المحفوظة، أو لغة النظام إن كانت مدعومة، وإلا العربية. */
    public static void init(Context c) {
        String s = new Prefs(c).lang();
        if (s.isEmpty()) s = Locale.getDefault().getLanguage();
        idx = indexOf(s);
    }

    private static int indexOf(String code) {
        for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(code)) return i;
        if ("in".equals(code)) return 5; // رمز الإندونيسية القديم
        return 0;
    }

    static String code() {
        return CODES[idx];
    }

    static int index() {
        return idx;
    }

    static boolean rtl() {
        return idx == 0 || idx == 4;
    }

    static boolean arabic() {
        return idx == 0;
    }

    static int dir() {
        return rtl() ? L.dir() : View.LAYOUT_DIRECTION_LTR;
    }

    /** يطبّق اللغة على السياق (للحوارات ومنتقي الوقت وغيرها). */
    static Context wrap(Context base) {
        try {
            L.init(base);
            Locale loc = new Locale(code());
            Locale.setDefault(loc);
            Configuration cfg = new Configuration(base.getResources().getConfiguration());
            cfg.setLocale(loc);
            cfg.setLayoutDirection(loc);
            return base.createConfigurationContext(cfg);
        } catch (Exception e) {
            return base;
        }
    }

    /** ترجمة نص ثابت. */
    static String t(String ar) {
        if (ar == null) return null;
        String[] r = MAP.get(ar);
        if (r == null) return digits(ar);
        String v = r[idx];
        if ((v == null || v.isEmpty()) && idx != 0) v = r[1];
        if (v == null || v.isEmpty()) v = r[0];
        return v;
    }

    /** ترجمة قالب وتعويض {0} {1}... */
    static String f(String ar, Object... a) {
        String s = t(ar);
        for (int i = 0; i < a.length; i++) s = s.replace("{" + i + "}", String.valueOf(a[i]));
        return s;
    }

    /** تحويل الأرقام العربية الهندية إلى أرقام اللغة الحالية. */
    static String digits(String s) {
        if (idx == 0 || s == null) return s;
        StringBuilder sb = null;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            char out = ch;
            if (ch >= '٠' && ch <= '٩') out = idx == 4 ? (char) ('۰' + (ch - '٠')) : (char) ('0' + (ch - '٠'));
            else if (ch == '٬') out = idx == 4 ? '٬' : ',';
            if (out != ch && sb == null) {
                sb = new StringBuilder(s.length());
                sb.append(s, 0, i);
            }
            if (sb != null) sb.append(out);
        }
        return sb == null ? s : sb.toString();
    }
}
