package com.alikhwa.thikrni;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** التذكير الأساسي صوتي فقط. إشعارات هادئة اختيارية للهدف والأذكار والمناسبات. */
public final class Notifier {
    private Notifier() {}

    public static final int ID_REMINDER = 1001;
    public static final int ID_SERVICE = 1002;
    public static final int ID_DHIKR = 1003;
    public static final int ID_OCCASION = 1004;
    public static final int ID_GOAL = 1005;
    public static final int ID_PRAYER = 1006;
    public static final int ID_STREAK = 1007;
    public static final int ID_ACH = 1008;

    public static final String CH_SERVICE = "thikrni_service";
    public static final String CH_GOAL = "thikrni_goal";
    public static final String CH_DHIKR = "thikrni_dhikr";
    public static final String CH_OCCASION = "thikrni_occasion";

    public static void ensureServiceChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            NotificationChannel ch = new NotificationChannel(CH_SERVICE, L.t("التشغيل في الخلفية"),
                    NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            ch.setSound(null, null);
            nm.createNotificationChannel(ch);
        }
    }

    private static void ensureChannels(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            NotificationChannel goal = new NotificationChannel(CH_GOAL, L.t("إتمام الهدف اليومي"),
                    NotificationManager.IMPORTANCE_DEFAULT);
            goal.enableVibration(true);
            goal.setVibrationPattern(new long[]{0, 250, 120, 250, 120, 400});
            nm.createNotificationChannel(goal);

            NotificationChannel dhikr = new NotificationChannel(CH_DHIKR, L.t("أذكار قصيرة"),
                    NotificationManager.IMPORTANCE_LOW);
            dhikr.setShowBadge(false);
            nm.createNotificationChannel(dhikr);

            NotificationChannel occ = new NotificationChannel(CH_OCCASION, L.t("المناسبات الهجرية"),
                    NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(occ);
        }
    }

    private static Notification.Builder builder(Context c, String channel) {
        return Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, channel) : new Notification.Builder(c);
    }

    private static void post(Context c, int id, String channel, String title, String text, boolean quiet) {
        try {
            Context app = c.getApplicationContext();
            ensureChannels(app);
            PendingIntent open = PendingIntent.getActivity(app, 0, new Intent(app, MainActivity.class),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Builder b = builder(app, channel)
                    .setSmallIcon(R.drawable.ic_stat_crescent)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(new Notification.BigTextStyle().bigText(text))
                    .setContentIntent(open)
                    .setAutoCancel(true);
            if (Build.VERSION.SDK_INT >= 26 && quiet) b.setTimeoutAfter(10 * 60_000L);
            if (Build.VERSION.SDK_INT < 26 && quiet) b.setPriority(Notification.PRIORITY_LOW);
            NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(id, b.build());
        } catch (Exception ignored) {
        }
    }

    public static void showGoal(Context c, int goal) {
        post(c, ID_GOAL, CH_GOAL, L.t("ما شاء الله! أتممت هدفك اليومي"),
                L.f("صلّيت على النبي صلى الله عليه وسلم {0} مرة اليوم. تقبّل الله منك.", HijriUtil.num(goal)), false);
    }

    public static void showDhikr(Context c, String dhikr) {
        post(c, ID_DHIKR, CH_DHIKR, L.t("ذكر"), dhikr, true);
    }

    public static void showOccasion(Context c, String title, String msg) {
        post(c, ID_OCCASION, CH_OCCASION, title, msg, false);
    }

    public static void showPrayer(Context c) {
        post(c, ID_PRAYER, CH_OCCASION, L.t("تقبّل الله صلاتك"),
                L.t("اللهم صلِّ وسلِّم على نبينا محمد، هذه لحظة طيبة للصلاة عليه صلى الله عليه وسلم."), true);
    }

    public static void showStreak(Context c, int days, long left) {
        post(c, ID_STREAK, CH_GOAL, L.f("حافظ على سلسلتك {0} يومًا", HijriUtil.num(days)),
                L.f("بقي لك {0} صلاة لتُتمّ هدف اليوم قبل أن تنقطع السلسلة.", HijriUtil.num(left)), false);
    }

    public static void showAchievement(Context c, String title, String desc) {
        post(c, ID_ACH, CH_OCCASION, L.f("إنجاز جديد: {0}", L.t(title)), desc == null ? "" : L.t(desc), false);
    }

    /** تشغيل تذكير صوتي. */
    public static void show(Context c, String reason) {
        SoundPlayer.play(c, reason, null);
    }
}
