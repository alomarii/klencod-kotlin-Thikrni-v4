package com.alikhwa.thikrni;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.Locale;

/** يجلب الموقع التقريبي لمرة واحدة لحساب مواقيت الصلاة؛ يبقى على الجهاز ولا يُرسل لأي خادم. */
final class LocationHelper {
    private LocationHelper() {}

    interface Done {
        void on(boolean ok);
    }

    static boolean granted(Context c) {
        return c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** يجلب الموقع (آخر معروف أو تحديث واحد) ثم يحفظه ويستدعي done، وبعده يحاول معرفة اسم المدينة. */
    @SuppressWarnings("MissingPermission")
    static void fetch(final Activity a, final Done done, final Runnable onNamed) {
        if (!granted(a)) {
            done.on(false);
            return;
        }
        final LocationManager lm = (LocationManager) a.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            done.on(false);
            return;
        }
        Location best = null;
        try {
            for (String prov : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(prov);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
        } catch (Exception ignored) {
        }
        if (best != null) {
            save(a, best, done, onNamed);
            return;
        }
        String prov = null;
        try {
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) prov = LocationManager.NETWORK_PROVIDER;
            else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) prov = LocationManager.GPS_PROVIDER;
        } catch (Exception ignored) {
        }
        if (prov == null) {
            done.on(false);
            return;
        }
        final boolean[] finished = {false};
        final Handler h = new Handler(Looper.getMainLooper());
        final LocationListener ll = new LocationListener() {
            @Override
            public void onLocationChanged(Location l) {
                if (finished[0]) return;
                finished[0] = true;
                try {
                    lm.removeUpdates(this);
                } catch (Exception ignored) {
                }
                save(a, l, done, onNamed);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        try {
            lm.requestLocationUpdates(prov, 0L, 0f, ll, Looper.getMainLooper());
        } catch (Exception e) {
            done.on(false);
            return;
        }
        h.postDelayed(() -> {
            if (finished[0]) return;
            finished[0] = true;
            try {
                lm.removeUpdates(ll);
            } catch (Exception ignored) {
            }
            done.on(false);
        }, 25_000L);
    }

    private static void save(final Activity a, final Location l, Done done, final Runnable onNamed) {
        final Prefs p = new Prefs(a);
        p.setLocation(l.getLatitude(), l.getLongitude(),
                String.format(Locale.US, "%.2f°, %.2f°", l.getLatitude(), l.getLongitude()));
        done.on(true);
        new Thread(() -> {
            try {
                if (!Geocoder.isPresent()) return;
                List<Address> list = new Geocoder(a, new Locale(L.code())).getFromLocation(l.getLatitude(), l.getLongitude(), 1);
                if (list == null || list.isEmpty()) return;
                Address ad = list.get(0);
                String city = ad.getLocality() != null ? ad.getLocality()
                        : ad.getSubAdminArea() != null ? ad.getSubAdminArea() : ad.getAdminArea();
                if (city == null) return;
                String name = ad.getCountryName() != null ? city + "، " + ad.getCountryName() : city;
                p.setLocationName(name);
                if (onNamed != null) a.runOnUiThread(onNamed);
            } catch (Exception ignored) {
            }
        }).start();
    }
}
