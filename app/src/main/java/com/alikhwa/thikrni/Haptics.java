package com.alikhwa.thikrni;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** نبضات اهتزاز لكل ثيم عند الضغط على زر الصلاة. */
final class Haptics {
    private Haptics() {}

    @SuppressWarnings("deprecation")
    static void count(Context c, Style st) {
        try {
            if (!new Prefs(c).haptics()) return;
            Vibrator vb = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (vb == null || !vb.hasVibrator()) return;
            long[] t;
            int[] a;
            switch (st.counter) {
                case Style.CT_NEON:
                    t = new long[]{0, 10};
                    a = new int[]{0, 255};
                    break;
                case Style.CT_STAR8:
                    t = new long[]{0, 12, 40, 12};
                    a = new int[]{0, 200, 0, 255};
                    break;
                case Style.CT_DIAMOND: // نبض قلب
                    t = new long[]{0, 18, 70, 32};
                    a = new int[]{0, 160, 0, 255};
                    break;
                case Style.CT_WAVE:
                    t = new long[]{0, 8, 30, 8, 30, 8};
                    a = new int[]{0, 90, 0, 120, 0, 90};
                    break;
                case Style.CT_SUN:
                    t = new long[]{0, 10, 25, 14, 25, 20};
                    a = new int[]{0, 80, 0, 140, 0, 220};
                    break;
                case Style.CT_ORBIT:
                    t = new long[]{0, 14};
                    a = new int[]{0, 110};
                    break;
                default:
                    t = new long[]{0, 22};
                    a = new int[]{0, 130};
                    break;
            }
            if (Build.VERSION.SDK_INT >= 26) vb.vibrate(VibrationEffect.createWaveform(t, a, -1));
            else vb.vibrate(t, -1);
        } catch (Exception ignored) {
        }
    }
}
