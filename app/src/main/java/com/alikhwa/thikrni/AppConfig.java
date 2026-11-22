package com.alikhwa.thikrni;

/** إعدادات ثابتة يمكن للمطوّر تعديلها. */
public final class AppConfig {
    private AppConfig() {}

    /**
     * رابط قاعدة Firebase Realtime Database لعدّاد الأجهزة،
     * مثال: "https://thikrni-default-rtdb.firebaseio.com"
     * اتركه فارغًا لتعطيل العدّاد (راجع README لخطوات الإعداد).
     */
    public static final String DEVICES_DB_URL = "";

    /** رابط مشاركة التطبيق (غيّره إلى رابط تطبيقك الفعلي). */
    public static final String SHARE_URL = "https://play.google.com/store/apps/details?id=com.alikhwa.thikrni";

    public static final String VERSION_LABEL = "الإصدار ٢٫٠";
}
