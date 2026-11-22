package com.alikhwa.thikrni;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * عدّاد الأجهزة: كل تثبيت يسجّل معرّفًا عشوائيًا مجهولًا في Firebase Realtime Database (عبر REST بدون مكتبات).
 * لا تُرسل أي بيانات شخصية. يتعطّل تلقائيًا إن كان AppConfig.DEVICES_DB_URL فارغًا.
 */
public final class DeviceCounter {
    private DeviceCounter() {}

    public interface Callback {
        /** القيمة -1 تعني غير معروفة. */
        void onResult(long total, long activeWeek);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static boolean configured() {
        return !AppConfig.DEVICES_DB_URL.trim().isEmpty();
    }

    private static String base() {
        String u = AppConfig.DEVICES_DB_URL.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    private static String http(String method, String url, String body) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestMethod(method);
            if (body != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                try (OutputStream o = c.getOutputStream()) {
                    o.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = c.getResponseCode();
            if (code >= 400) throw new IOException("HTTP " + code);
            StringBuilder sb = new StringBuilder();
            try (InputStream in = c.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }

    /** يسجّل هذا الجهاز (أو يحدّث آخر ظهور) مرة كل ٦ ساعات كحد أقصى. */
    public static void register(Context c) {
        if (!configured()) return;
        final Context app = c.getApplicationContext();
        final Prefs p = new Prefs(app);
        final long now = System.currentTimeMillis();
        if (now - p.devRegisteredAt() < 6 * 3600_000L) return;
        new Thread(() -> {
            try {
                http("PUT", base() + "/devices/" + p.installId() + ".json", "{\"seen\":{\".sv\":\"timestamp\"}}");
                p.setDevRegisteredAt(now);
            } catch (Exception ignored) {
            }
        }).start();
    }

    private static long countKeys(String json) throws Exception {
        if (json == null || json.trim().equals("null") || json.trim().isEmpty()) return 0;
        return new JSONObject(json).length();
    }

    /** يجلب العدد الحالي (يستخدم المخزَّن إن كان حديثًا ما لم يُطلب التحديث). */
    public static void fetch(Context c, boolean force, final Callback cb) {
        final Context app = c.getApplicationContext();
        final Prefs p = new Prefs(app);
        if (!configured()) {
            cb.onResult(-1, -1);
            return;
        }
        if (!force && System.currentTimeMillis() - p.devFetchedAt() < 60_000L && p.devTotal() >= 0) {
            cb.onResult(p.devTotal(), p.devActive());
            return;
        }
        new Thread(() -> {
            long total = -1, active = -1;
            try {
                total = countKeys(http("GET", base() + "/devices.json?shallow=true", null));
            } catch (Exception ignored) {
            }
            if (total >= 0) {
                try {
                    long since = System.currentTimeMillis() - 7L * 86400_000L;
                    active = countKeys(http("GET",
                            base() + "/devices.json?orderBy=%22seen%22&startAt=" + since, null));
                } catch (Exception ignored) {
                }
                p.setDevCache(total, active);
            } else {
                total = p.devTotal();
                active = p.devActive();
            }
            final long t = total, a = active;
            MAIN.post(() -> cb.onResult(t, a));
        }).start();
    }
}
