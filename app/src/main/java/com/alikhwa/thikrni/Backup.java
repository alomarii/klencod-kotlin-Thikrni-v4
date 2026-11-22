package com.alikhwa.thikrni;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

/** نسخ احتياطي واستعادة للإعدادات والإحصائيات (ملف JSON). */
final class Backup {
    private Backup() {}

    private static boolean skip(String k) {
        return k.equals("installId") || k.startsWith("dev") || k.startsWith("tn_") || k.startsWith("td_")
                || k.equals("seqIdx") || k.equals("lastIdx");
    }

    static void export(Context c, Uri uri) throws Exception {
        JSONObject root = new JSONObject();
        root.put("app", "thikrni");
        root.put("version", 1);
        JSONObject data = new JSONObject();
        for (Map.Entry<String, ?> e : new Prefs(c).raw().getAll().entrySet()) {
            if (skip(e.getKey())) continue;
            Object v = e.getValue();
            String t;
            if (v instanceof Boolean) t = "b";
            else if (v instanceof Integer) t = "i";
            else if (v instanceof Long) t = "l";
            else if (v instanceof Float) t = "f";
            else if (v instanceof String) t = "s";
            else continue;
            data.put(e.getKey(), new JSONObject().put("t", t).put("v", v));
        }
        root.put("data", data);
        try (OutputStream o = c.getContentResolver().openOutputStream(uri)) {
            o.write(root.toString().getBytes("UTF-8"));
        }
    }

    /** يعيد عدد العناصر المستعادة. */
    static int restore(Context c, Uri uri) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        }
        JSONObject root = new JSONObject(bo.toString("UTF-8"));
        if (!"thikrni".equals(root.optString("app"))) throw new IllegalArgumentException("not a Thikrni backup");
        JSONObject data = root.getJSONObject("data");
        SharedPreferences.Editor ed = new Prefs(c).raw().edit();
        int count = 0;
        Iterator<String> it = data.keys();
        while (it.hasNext()) {
            String k = it.next();
            if (skip(k)) continue;
            JSONObject o = data.getJSONObject(k);
            switch (o.getString("t")) {
                case "b":
                    ed.putBoolean(k, o.getBoolean("v"));
                    break;
                case "i":
                    ed.putInt(k, o.getInt("v"));
                    break;
                case "l":
                    ed.putLong(k, o.getLong("v"));
                    break;
                case "f":
                    ed.putFloat(k, (float) o.getDouble("v"));
                    break;
                default:
                    ed.putString(k, o.getString("v"));
                    break;
            }
            count++;
        }
        ed.apply();
        return count;
    }
}
