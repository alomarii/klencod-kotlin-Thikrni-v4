package com.alikhwa.thikrni;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_ALARM = "com.alikhwa.thikrni.ALARM";
    public static final String ACTION_COUNT = "com.alikhwa.thikrni.COUNT";
    public static final String ACTION_DHIKR = "com.alikhwa.thikrni.DHIKR";
    public static final String ACTION_OCCASION = "com.alikhwa.thikrni.OCCASION";
    public static final String ACTION_PRAYER = "com.alikhwa.thikrni.PRAYER";
    public static final String ACTION_STREAK = "com.alikhwa.thikrni.STREAK";

    @Override
    public void onReceive(Context context, Intent intent) {
        String a = intent.getAction();
        if (a == null) return;
        Prefs p = new Prefs(context);
        switch (a) {
            case ACTION_ALARM:
                if (p.enabled()) {
                    ReminderScheduler.schedule(context);
                    SalawatWidget.updateAll(context);
                    final PendingResult pr = goAsync();
                    SoundPlayer.play(context, "alarm", pr::finish);
                }
                break;
            case ACTION_DHIKR:
                if (p.enabled() && p.adhkar() && !Quiet.isQuiet(p)) {
                    Notifier.showDhikr(context, Adhkar.next());
                }
                break;
            case ACTION_OCCASION:
                ReminderScheduler.scheduleOccasion(context);
                SalawatWidget.updateAll(context);
                if (p.occasions() && !Quiet.isQuiet(p)) {
                    Occasions.Occ today = Occasions.onDay(p, 0);
                    if (today != null) {
                        Notifier.showOccasion(context, L.f("اليوم: {0}", L.t(today.name)), L.t(today.msg));
                    } else {
                        Occasions.Occ tomorrow = Occasions.onDay(p, 1);
                        if (tomorrow != null) {
                            Notifier.showOccasion(context, L.f("غدًا: {0}", L.t(tomorrow.name)), L.t(tomorrow.msg));
                        }
                    }
                }
                break;
            case ACTION_PRAYER:
                ReminderScheduler.schedulePrayers(context);
                if (p.prayerOn() && !Quiet.isQuiet(p)) {
                    Notifier.showPrayer(context);
                    final PendingResult pr2 = goAsync();
                    SoundPlayer.play(context, "alarm", pr2::finish);
                }
                break;
            case ACTION_STREAK:
                ReminderScheduler.scheduleStreak(context);
                if (p.streakWarn() && !Quiet.isQuiet(p) && Stats.streakAtRisk(p)) {
                    Notifier.showStreak(context, Stats.streak(p), Math.max(1, p.goal() - Stats.today(p)));
                }
                break;
            case ACTION_COUNT:
                Stats.add(context);
                break;
            default:
                break;
        }
    }
}
