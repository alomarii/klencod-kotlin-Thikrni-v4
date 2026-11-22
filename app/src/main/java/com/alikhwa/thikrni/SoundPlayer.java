package com.alikhwa.thikrni;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * تشغيل التذكير كتسجيل صوتي بشري للصلاة على النبي (بدون صوت آلي).
 * يُختار تسجيل من مكتبة المستخدم (عشوائيًا أو بالترتيب)، وإن تعذّر تشغيله تُجرَّب بقية التسجيلات.
 */
public final class SoundPlayer {
    private SoundPlayer() {}

    public interface ErrorCb {
        void on(String message);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // ------------------------------------------------------------ الملفات

    private static int rawId(Context c) {
        Context app = c.getApplicationContext();
        return app.getResources().getIdentifier("salawat", "raw", app.getPackageName());
    }

    public static boolean hasRaw(Context c) {
        return rawId(c) != 0;
    }

    public static boolean hasAudio(Context c) {
        return Tracks.hasAny(c) || hasRaw(c);
    }

    private static AudioAttributes attrs() {
        return new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
    }

    /** يفتح مشغّلًا جاهزًا لملف من المكتبة عبر FileDescriptor (أوثق من المسار). */
    private static MediaPlayer open(File f) throws Exception {
        MediaPlayer mp = new MediaPlayer();
        try (FileInputStream in = new FileInputStream(f)) {
            mp.setAudioAttributes(attrs());
            mp.setDataSource(in.getFD());
            mp.prepare();
            return mp;
        } catch (Exception e) {
            mp.release();
            throw e;
        }
    }

    private static MediaPlayer openRaw(Context app) throws Exception {
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setAudioAttributes(attrs());
            AssetFileDescriptor afd = app.getResources().openRawResourceFd(rawId(app));
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.prepare();
            return mp;
        } catch (Exception e) {
            mp.release();
            throw e;
        }
    }

    // ------------------------------------------------------------ معاينة

    private static MediaPlayer previewMp;

    public static synchronized void stopPreview() {
        if (previewMp != null) {
            try {
                previewMp.release();
            } catch (Exception ignored) {
            }
            previewMp = null;
        }
    }

    /** معاينة تسجيل من صفحة الصوتيات (بدون عدّ). */
    public static void preview(Context c, final File f, final Runnable onDone, final ErrorCb err) {
        stopPreview();
        new Thread(() -> {
            try {
                MediaPlayer mp = open(f);
                mp.setOnCompletionListener(m -> {
                    stopPreview();
                    if (onDone != null) MAIN.post(onDone);
                });
                mp.setOnErrorListener((m, w, e) -> {
                    stopPreview();
                    if (onDone != null) MAIN.post(onDone);
                    return true;
                });
                synchronized (SoundPlayer.class) {
                    previewMp = mp;
                }
                mp.start();
            } catch (Exception e) {
                if (err != null) MAIN.post(() -> err.on(L.t("تعذّر تشغيل هذا الملف، جرّب تسجيلًا بصيغة mp3 أو m4a")));
                if (onDone != null) MAIN.post(onDone);
            }
        }).start();
    }

    // -------------------------------------------------------------- تشغيل

    public static void play(Context c, String reason, final Runnable done) {
        play(c, reason, done, null);
    }

    /** يشغّل تذكيرًا. يستدعي done عند الانتهاء، و err برسالة إن تعذّر التشغيل. */
    public static void play(Context c, String reason, final Runnable done, final ErrorCb err) {
        final Context app = c.getApplicationContext();
        final Prefs p = new Prefs(app);
        final boolean isTest = "test".equals(reason);

        // أوقات الصمت: لا صوت ولا اهتزاز
        if (!isTest && Quiet.isQuiet(p)) {
            if (done != null) done.run();
            return;
        }

        final PowerManager.WakeLock wl = ((PowerManager) app.getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "thikrni:play");
        try {
            wl.acquire(65000);
        } catch (Exception ignored) {
        }
        final AtomicBoolean finished = new AtomicBoolean(false);
        final AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
        final AudioFocusRequest[] focus = new AudioFocusRequest[1];
        final Runnable finish = () -> {
            if (!finished.compareAndSet(false, true)) return;
            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    if (focus[0] != null) am.abandonAudioFocusRequest(focus[0]);
                } else {
                    am.abandonAudioFocus(null);
                }
            } catch (Exception ignored) {
            }
            try {
                if (wl.isHeld()) wl.release();
            } catch (Exception ignored) {
            }
            if (done != null) done.run();
        };
        MAIN.postDelayed(finish, 60000);

        if (p.vibrate()) vibrate(app);
        if (!p.sound()) {
            if (err != null && isTest) err.on("الصوت متوقف من صفحة الصوتيات");
            finish.run();
            return;
        }
        if (!hasAudio(app)) {
            if (err != null) err.on("لا يوجد تسجيل، أضف تسجيلًا من صفحة الصوتيات");
            finish.run();
            return;
        }

        final List<File> cands = Tracks.candidates(app);
        try {
            AudioAttributes a = attrs();
            if (Build.VERSION.SDK_INT >= 26) {
                focus[0] = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(a).build();
                am.requestAudioFocus(focus[0]);
            } else {
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            }
        } catch (Exception ignored) {
        }

        new Thread(() -> {
            MediaPlayer mp = null;
            for (File f : cands) {
                try {
                    mp = open(f);
                    break;
                } catch (Exception ignored) {
                }
            }
            if (mp == null && hasRaw(app)) {
                try {
                    mp = openRaw(app);
                } catch (Exception ignored) {
                }
            }
            if (mp == null) {
                if (err != null) MAIN.post(() -> err.on(L.t("تعذّر تشغيل التسجيلات، جرّب تسجيلًا بصيغة mp3 أو m4a")));
                finish.run();
                return;
            }
            try {
                mp.setOnCompletionListener(m -> {
                    m.release();
                    finish.run();
                });
                mp.setOnErrorListener((m, w, e) -> {
                    m.release();
                    finish.run();
                    return true;
                });
                mp.start();
                // العدّ التلقائي: كل تذكير يُسمَع يُضاف إلى العدّاد
                if (!isTest && p.autoCount()) Stats.add(app);
            } catch (Exception e) {
                mp.release();
                finish.run();
            }
        }).start();
    }

    private static void vibrate(Context app) {
        try {
            Vibrator v = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createWaveform(new long[]{0, 200, 100, 200}, -1));
            } else {
                v.vibrate(new long[]{0, 200, 100, 200}, -1);
            }
        } catch (Exception ignored) {
        }
    }
}
