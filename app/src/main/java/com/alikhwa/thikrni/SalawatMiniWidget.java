package com.alikhwa.thikrni;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** ويدجت صغير (١×١): عدّاد اليوم وزر صلاة واحد. */
public class SalawatMiniWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        update(context);
    }

    public static void update(Context c) {
        try {
            Context app = c.getApplicationContext();
            AppWidgetManager m = AppWidgetManager.getInstance(app);
            int[] ids = m.getAppWidgetIds(new ComponentName(app, SalawatMiniWidget.class));
            if (ids == null || ids.length == 0) return;
            Prefs p = new Prefs(app);
            Theme th = Theme.of(p.effectiveTheme());
            RemoteViews rv = new RemoteViews(app.getPackageName(), R.layout.widget_mini);
            int bg = app.getResources().getIdentifier("widget_bg_" + th.id, "drawable", app.getPackageName());
            int btn = app.getResources().getIdentifier("widget_btn_" + th.id, "drawable", app.getPackageName());
            if (bg != 0) rv.setInt(R.id.m_root, "setBackgroundResource", bg);
            if (btn != 0) rv.setInt(R.id.m_btn, "setBackgroundResource", btn);
            rv.setInt(R.id.m_root, "setLayoutDirection", L.dir());
            rv.setTextViewText(R.id.m_btn, L.t("صلِّ"));
            rv.setTextColor(R.id.m_count, 0xFFFFFFFF);
            rv.setTextColor(R.id.m_btn, th.onAccent);
            rv.setTextViewText(R.id.m_count, HijriUtil.num(Stats.today(p)));

            PendingIntent open = PendingIntent.getActivity(app, 210, new Intent(app, MainActivity.class),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.m_root, open);
            Intent ci = new Intent(app, ReminderReceiver.class);
            ci.setAction(ReminderReceiver.ACTION_COUNT);
            PendingIntent count = PendingIntent.getBroadcast(app, 211, ci,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.m_btn, count);
            m.updateAppWidget(ids, rv);
        } catch (Exception ignored) {
        }
    }
}
