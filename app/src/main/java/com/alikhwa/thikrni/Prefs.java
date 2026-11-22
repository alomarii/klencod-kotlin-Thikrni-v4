package com.alikhwa.thikrni;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** تخزين إعدادات التطبيق. */
public class Prefs {
    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("thikrni", Context.MODE_PRIVATE);
    }

    public boolean enabled() { return sp.getBoolean("enabled", true); }
    public void setEnabled(boolean v) { sp.edit().putBoolean("enabled", v).apply(); }

    public int intervalMin() { return Math.max(1, sp.getInt("interval", 30)); }
    public void setIntervalMin(int v) { sp.edit().putInt("interval", Math.max(1, v)).apply(); }

    public boolean onCharge() { return sp.getBoolean("onCharge", true); }
    public void setOnCharge(boolean v) { sp.edit().putBoolean("onCharge", v).apply(); }

    public boolean onUnlock() { return sp.getBoolean("onUnlock", true); }
    public void setOnUnlock(boolean v) { sp.edit().putBoolean("onUnlock", v).apply(); }

    public boolean sound() { return sp.getBoolean("sound", true); }
    public void setSound(boolean v) { sp.edit().putBoolean("sound", v).apply(); }

    public boolean vibrate() { return sp.getBoolean("vibrate", true); }
    public void setVibrate(boolean v) { sp.edit().putBoolean("vibrate", v).apply(); }

    public long count() { return sp.getLong("count", 0); }
    public void setCount(long v) { sp.edit().putLong("count", Math.max(0, v)).apply(); }
    public long incCount() {
        long v = count() + 1;
        setCount(v);
        return v;
    }

    public int hijriAdjust() { return sp.getInt("hijriAdj", 0); }
    public void setHijriAdjust(int v) { sp.edit().putInt("hijriAdj", Math.max(-3, Math.min(3, v))).apply(); }

    // ---------------------------------------------------------- أوقات الصمت
    public boolean quietEnabled() { return sp.getBoolean("quietOn", false); }
    public void setQuietEnabled(boolean v) { sp.edit().putBoolean("quietOn", v).apply(); }
    public int quietStart() { return sp.getInt("quietStart", 22 * 60); }
    public void setQuietStart(int m) { sp.edit().putInt("quietStart", m).apply(); }
    public int quietEnd() { return sp.getInt("quietEnd", 5 * 60); }
    public void setQuietEnd(int m) { sp.edit().putInt("quietEnd", m).apply(); }

    // ------------------------------------------------------------ التسجيلات
    public boolean sequential() { return sp.getBoolean("seq", false); }
    public void setSequential(boolean v) { sp.edit().putBoolean("seq", v).apply(); }
    public int seqIndex() { return sp.getInt("seqIdx", 0); }
    public void setSeqIndex(int v) { sp.edit().putInt("seqIdx", v).apply(); }
    public int lastIndex() { return sp.getInt("lastIdx", -1); }
    public void setLastIndex(int v) { sp.edit().putInt("lastIdx", v).apply(); }
    public String trackName(String file) { return sp.getString("tn_" + file, null); }
    public void setTrackName(String file, String name) { sp.edit().putString("tn_" + file, name).apply(); }
    public void removeTrackName(String file) { sp.edit().remove("tn_" + file).remove("td_" + file).apply(); }
    public long trackDur(String file) { return sp.getLong("td_" + file, -1); }
    public void setTrackDur(String file, long ms) { sp.edit().putLong("td_" + file, ms).apply(); }

    // ---------------------------------------------------- العدّ والهدف اليومي
    public boolean autoCount() { return sp.getBoolean("autoCount", true); }
    public void setAutoCount(boolean v) { sp.edit().putBoolean("autoCount", v).apply(); }

    public boolean goalEnabled() { return sp.getBoolean("goalOn", true); }
    public void setGoalEnabled(boolean v) { sp.edit().putBoolean("goalOn", v).apply(); }
    public int goal() { return Math.max(1, sp.getInt("goal", 100)); }
    public void setGoal(int v) { sp.edit().putInt("goal", Math.max(1, Math.min(100000, v))).apply(); }
    public String goalAnnouncedDay() { return sp.getString("goalDay", ""); }
    public void setGoalAnnouncedDay(String s) { sp.edit().putString("goalDay", s).apply(); }

    public long dayCount(String key) { return sp.getLong(key, 0); }
    public void addDay(String key, long n) { sp.edit().putLong(key, dayCount(key) + n).apply(); }
    public void removeKey(String key) { sp.edit().remove(key).apply(); }
    public void clearDays() {
        List<String> keys = new ArrayList<>();
        for (String k : sp.getAll().keySet()) if (k.startsWith("d_")) keys.add(k);
        SharedPreferences.Editor e = sp.edit();
        for (String k : keys) e.remove(k);
        e.apply();
    }

    // ------------------------------------------------------ الجمعة / الأذكار
    public boolean fridayBoost() { return sp.getBoolean("friday", true); }
    public void setFridayBoost(boolean v) { sp.edit().putBoolean("friday", v).apply(); }
    public int fridayInterval() { return Math.max(1, sp.getInt("fridayInt", 15)); }
    public void setFridayInterval(int v) { sp.edit().putInt("fridayInt", Math.max(1, v)).apply(); }

    public boolean adhkar() { return sp.getBoolean("adhkar", false); }
    public void setAdhkar(boolean v) { sp.edit().putBoolean("adhkar", v).apply(); }
    public boolean occasions() { return sp.getBoolean("occasions", true); }
    public void setOccasions(boolean v) { sp.edit().putBoolean("occasions", v).apply(); }

    // ---------------------------------------------------------------- المظهر
    public int theme() { return sp.getInt("theme", 0); }
    public void setTheme(int v) { sp.edit().putInt("theme", v).apply(); }


    public SharedPreferences raw() { return sp; }

    // ------------------------------------------- الحركة والاهتزاز والمظهر التلقائي
    /** رمز اللغة المختارة، أو فارغ لاتباع لغة النظام. */
    public String lang() { return sp.getString("lang", ""); }
    public void setLang(String v) { sp.edit().putString("lang", v).apply(); }

    public boolean reduceMotion() { return sp.getBoolean("reduce", false); }
    public void setReduceMotion(boolean v) { sp.edit().putBoolean("reduce", v).apply(); }
    public boolean haptics() { return sp.getBoolean("haptics", true); }
    public void setHaptics(boolean v) { sp.edit().putBoolean("haptics", v).apply(); }
    public boolean autoTheme() { return sp.getBoolean("autoTheme", false); }
    public void setAutoTheme(boolean v) { sp.edit().putBoolean("autoTheme", v).apply(); }

    /** الثيم الفعلي: عند التفعيل التلقائي يكون الذهبي في الجمعة والأزرق الليلي ليلًا. */
    public int effectiveTheme() {
        if (!autoTheme()) return theme();
        if (HijriUtil.isFridayWindow()) return 2;
        int h = new java.util.GregorianCalendar().get(java.util.Calendar.HOUR_OF_DAY);
        if (h >= 21 || h < 5) return 1;
        return theme();
    }

    // ----------------------------------------------------- صيغ / سلسلة / صلوات
    /** ‎-١‎ = تبديل تلقائي بين الصيغ، وإلا فهرس الصيغة المفضلة. */
    public int formula() { return sp.getInt("formula", -1); }
    public void setFormula(int v) { sp.edit().putInt("formula", v).apply(); }
    public boolean streakWarn() { return sp.getBoolean("streakWarn", true); }
    public void setStreakWarn(boolean v) { sp.edit().putBoolean("streakWarn", v).apply(); }

    // ---------------------------------------------------- الموقع ومواقيت الصلاة
    public boolean hasLocation() { return sp.contains("locLat"); }
    public double lat() { return Double.parseDouble(sp.getString("locLat", "0")); }
    public double lon() { return Double.parseDouble(sp.getString("locLon", "0")); }
    public String locName() { return sp.getString("locName", ""); }
    public void setLocation(double lat, double lon, String name) {
        sp.edit().putString("locLat", String.valueOf(lat)).putString("locLon", String.valueOf(lon))
                .putString("locName", name).apply();
    }
    public void setLocationName(String n) { sp.edit().putString("locName", n).apply(); }
    public int calcMethod() { return sp.getInt("calcMethod", 0); }
    public void setCalcMethod(int v) { sp.edit().putInt("calcMethod", v).apply(); }
    public boolean asrHanafi() { return sp.getBoolean("asrHanafi", false); }
    public void setAsrHanafi(boolean v) { sp.edit().putBoolean("asrHanafi", v).apply(); }
    /** حساب الأوقات تلقائيًا من الموقع (وإلا الأوقات اليدوية). */
    public boolean prayerAuto() { return sp.getBoolean("prayerAuto", true); }
    public void setPrayerAuto(boolean v) { sp.edit().putBoolean("prayerAuto", v).apply(); }

    public boolean prayerOn() { return sp.getBoolean("prayerOn", false); }
    public void setPrayerOn(boolean v) { sp.edit().putBoolean("prayerOn", v).apply(); }
    public int prayerTime(int i) {
        int[] def = {5 * 60, 12 * 60, 15 * 60 + 30, 18 * 60, 19 * 60 + 30};
        return sp.getInt("pt" + i, def[i]);
    }
    public void setPrayerTime(int i, int m) { sp.edit().putInt("pt" + i, m).apply(); }
    public int prayerAfter() { return Math.max(0, sp.getInt("pAfter", 10)); }
    public void setPrayerAfter(int v) { sp.edit().putInt("pAfter", Math.max(0, v)).apply(); }

    // ------------------------------------------------------- عدّاد الأجهزة
    /** معرّف عشوائي مجهول لهذا التثبيت (لا يرتبط بأي بيانات شخصية). */
    public String installId() {
        String id = sp.getString("installId", null);
        if (id == null) {
            id = UUID.randomUUID().toString().replace("-", "");
            sp.edit().putString("installId", id).apply();
        }
        return id;
    }
    public long devTotal() { return sp.getLong("devTotal", -1); }
    public long devActive() { return sp.getLong("devActive", -1); }
    public void setDevCache(long total, long active) {
        sp.edit().putLong("devTotal", total).putLong("devActive", active)
                .putLong("devFetchedAt", System.currentTimeMillis()).apply();
    }
    public long devFetchedAt() { return sp.getLong("devFetchedAt", 0); }
    public long devRegisteredAt() { return sp.getLong("devRegAt", 0); }
    public void setDevRegisteredAt(long t) { sp.edit().putLong("devRegAt", t).apply(); }
}
