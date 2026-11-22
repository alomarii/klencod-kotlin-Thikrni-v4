package com.alikhwa.thikrni;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

/** إضافة الويدجت إلى الشاشة الرئيسية بضغطة زر (أندرويد ٨+)، أو شرح الطريقة اليدوية. */
final class WidgetPin {
    private WidgetPin() {}

    static boolean hasWidget(Context c) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, SalawatWidget.class));
            return ids != null && ids.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    static void request(Activity a) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(a);
            if (Build.VERSION.SDK_INT >= 26 && m.isRequestPinAppWidgetSupported()) {
                boolean sent = m.requestPinAppWidget(new ComponentName(a, SalawatWidget.class), null, null);
                if (sent) {
                    Toast.makeText(a, L.t("أكّد الإضافة من النافذة التي ظهرت"), Toast.LENGTH_LONG).show();
                    return;
                }
            }
        } catch (Exception ignored) {
        }
        showManual(a);
    }

    static void requestMini(Activity a) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(a);
            if (Build.VERSION.SDK_INT >= 26 && m.isRequestPinAppWidgetSupported()) {
                if (m.requestPinAppWidget(new ComponentName(a, SalawatMiniWidget.class), null, null)) {
                    Toast.makeText(a, L.t("أكّد الإضافة من النافذة التي ظهرت"), Toast.LENGTH_LONG).show();
                    return;
                }
            }
        } catch (Exception ignored) {
        }
        showManual(a);
    }

    private static void showManual(Activity a) {
        new AlertDialog.Builder(a)
                .setTitle(L.t("إضافة الويدجت"))
                .setMessage(L.t("لم يسمح الهاتف بالإضافة المباشرة، فاتبع الخطوات:\n\n١) اضغط مطوّلًا على مكان فارغ في الشاشة الرئيسية.\n٢) اختر «الأدوات» أو «Widgets».\n٣) ابحث عن «ذكرني» واسحب الويدجت إلى الشاشة."))
                .setPositiveButton(L.t("حسنًا"), null)
                .show();
    }
}
