package com.alikhwa.thikrni;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/** خدمة خلفية تستمع لفتح القفل وتركيب الشاحن. */
public class ReminderService extends Service {
    private BroadcastReceiver receiver;
    private long last = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Notifier.ensureServiceChannel(this);
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, Notifier.CH_SERVICE)
                : new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_stat_crescent)
                .setContentTitle("ذكرني")
                .setContentText(L.t("التذكير الصوتي يعمل"))
                .setContentIntent(open)
                .setOngoing(true);
        if (Build.VERSION.SDK_INT < 26) b.setPriority(Notification.PRIORITY_MIN);
        Notification n = b.build();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(Notifier.ID_SERVICE, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(Notifier.ID_SERVICE, n);
            }
        } catch (Exception e) {
            stopSelf();
            return;
        }

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String a = intent.getAction();
                if (Intent.ACTION_DATE_CHANGED.equals(a) || Intent.ACTION_TIMEZONE_CHANGED.equals(a)) {
                    SalawatWidget.updateAll(ctx);
                    ReminderScheduler.scheduleOccasion(ctx);
                    return;
                }
                Prefs p = new Prefs(ctx);
                if (!p.enabled()) return;
                long now = System.currentTimeMillis();
                if (now - last < 15000) return;
                if (Intent.ACTION_USER_PRESENT.equals(a) && p.onUnlock()) {
                    last = now;
                    Notifier.show(ctx, "unlock");
                } else if (Intent.ACTION_POWER_CONNECTED.equals(a) && p.onCharge()) {
                    last = now;
                    Notifier.show(ctx, "charge");
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_USER_PRESENT);
        f.addAction(Intent.ACTION_POWER_CONNECTED);
        f.addAction(Intent.ACTION_DATE_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, Context.RECEIVER_EXPORTED);
        else registerReceiver(receiver, f);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception ignored) {
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
