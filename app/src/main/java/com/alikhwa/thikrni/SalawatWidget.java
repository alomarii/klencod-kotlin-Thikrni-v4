package com.alikhwa.thikrni;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** ويدجت الشاشة الرئيسية: التاريخ الهجري والميلادي وعدّاد اليوم مع زر "صلّيت". */
public class SalawatWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        updateAll(context);
    }

    public static void updateAll(Context c) {
        SalawatMiniWidget.update(c);
        try {
            Context app = c.getApplicationContext();
            AppWidgetManager m = AppWidgetManager.getInstance(app);
            int[] ids = m.getAppWidgetIds(new ComponentName(app, SalawatWidget.class));
            if (ids == null || ids.length == 0) return;
            Prefs p = new Prefs(app);
            RemoteViews rv = new RemoteViews(app.getPackageName(), R.layout.widget_salawat);
            // ألوان الويدجت تتبع الثيم المختار
            Theme th = Theme.of(p.effectiveTheme());
            int bg = app.getResources().getIdentifier("widget_bg_" + th.id, "drawable", app.getPackageName());
            int btn = app.getResources().getIdentifier("widget_btn_" + th.id, "drawable", app.getPackageName());
            if (bg != 0) rv.setInt(R.id.w_root, "setBackgroundResource", bg);
            if (btn != 0) rv.setInt(R.id.w_btn, "setBackgroundResource", btn);
            rv.setInt(R.id.w_root, "setLayoutDirection", L.dir());
            rv.setTextViewText(R.id.w_btn, L.t("صلِّ على النبي"));
            rv.setTextColor(R.id.w_hijri, th.accent);
            rv.setTextColor(R.id.w_greg, th.sub);
            rv.setTextColor(R.id.w_label, th.sub);
            rv.setTextColor(R.id.w_btn, th.onAccent);
            rv.setTextViewText(R.id.w_hijri, HijriUtil.hijri(p.hijriAdjust()));
            rv.setTextViewText(R.id.w_greg, HijriUtil.dayName() + " • " + HijriUtil.gregorian());
            rv.setTextViewText(R.id.w_count, HijriUtil.num(Stats.today(p)));
            rv.setTextViewText(R.id.w_label, p.goalEnabled()
                    ? L.f("من هدف {0} صلاة اليوم", HijriUtil.num(p.goal())) : L.t("صلواتك اليوم"));

            PendingIntent open = PendingIntent.getActivity(app, 200, new Intent(app, MainActivity.class),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.w_root, open);

            Intent ci = new Intent(app, ReminderReceiver.class);
            ci.setAction(ReminderReceiver.ACTION_COUNT);
            PendingIntent count = PendingIntent.getBroadcast(app, 201, ci,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.w_btn, count);

            m.updateAppWidget(ids, rv);
        } catch (Exception ignored) {
        }
    }
}
