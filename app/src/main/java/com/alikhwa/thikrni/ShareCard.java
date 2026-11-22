package com.alikhwa.thikrni;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

/** بطاقة إنجاز بصورة بألوان وشكل الثيم الحالي للمشاركة. */
final class ShareCard {
    private ShareCard() {}

    static Bitmap render(Prefs p, Theme t) {
        final int W = 1080, H = 1350;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0, 0, 0, H, new int[]{t.bgTop, t.bgMid, t.bgBottom},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, W, H, paint);
        paint.setShader(new RadialGradient(W / 2f, 120, W, Theme.a(t.glowA, 0x66), Theme.a(t.glowA, 0),
                Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, W, H, paint);
        paint.setShader(new RadialGradient(W * 0.2f, H * 0.8f, W * 0.8f, Theme.a(t.glowB, 0x33), Theme.a(t.glowB, 0),
                Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, W, H, paint);
        paint.setShader(null);

        Random r = new Random(5);
        for (int i = 0; i < 90; i++) {
            paint.setColor(Theme.a(0xFFF1C6, 40 + r.nextInt(150)));
            cv.drawCircle(r.nextFloat() * W, r.nextFloat() * H, 1.5f + r.nextFloat() * 3f, paint);
        }

        // اللوحة بشكل الثيم
        RectF panel = new RectF(70, 150, W - 70, H - 150);
        Path path = new Path();
        Shapes.path(path, panel, t.st.shape, 70f, false);
        paint.setShader(new LinearGradient(0, panel.top, 0, panel.bottom, 0x33FFFFFF, 0x12FFFFFF, Shader.TileMode.CLAMP));
        cv.drawPath(path, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Theme.a(t.accent, 0xAA));
        cv.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);

        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTypeface(Typeface.DEFAULT_BOLD);

        tp.setColor(t.accent);
        tp.setTextSize(110);
        cv.drawText(L.t("ذكرني"), W / 2f, 330, tp);
        tp.setTypeface(Typeface.DEFAULT);
        tp.setColor(t.sub);
        tp.setTextSize(42);
        cv.drawText(L.t("اللهم صلِّ وسلِّم على نبينا محمد"), W / 2f, 400, tp);

        // الرقم الكبير
        tp.setTypeface(Typeface.DEFAULT_BOLD);
        tp.setColor(0xFFFFFFFF);
        tp.setTextSize(210);
        cv.drawText(HijriUtil.num(p.count()), W / 2f, 660, tp);
        tp.setColor(t.accentLight);
        tp.setTextSize(52);
        cv.drawText(L.t("صلاة على النبي صلى الله عليه وسلم"), W / 2f, 735, tp);

        // ثلاث بطاقات صغيرة
        String[] labs = {L.t("اليوم"), L.t("السلسلة"), L.t("الأوسمة")};
        String[] vals = {HijriUtil.num(Stats.today(p)), L.f("{0} يوم", HijriUtil.num(Stats.streak(p))),
                HijriUtil.num(Achievements.unlockedCount(p)) + "/" + HijriUtil.num(Achievements.ALL.length)};
        float bw = 270, gap = 30, x0 = (W - (3 * bw + 2 * gap)) / 2f;
        for (int i = 0; i < 3; i++) {
            RectF b = new RectF(x0 + i * (bw + gap), 800, x0 + i * (bw + gap) + bw, 960);
            Shapes.path(path, b, t.st.shape, 36f, false);
            paint.setColor(0x26FFFFFF);
            cv.drawPath(path, paint);
            tp.setColor(t.accent);
            tp.setTextSize(52);
            cv.drawText(vals[i], b.centerX(), b.top + 82, tp);
            tp.setTypeface(Typeface.DEFAULT);
            tp.setColor(t.sub);
            tp.setTextSize(34);
            cv.drawText(labs[i], b.centerX(), b.top + 130, tp);
            tp.setTypeface(Typeface.DEFAULT_BOLD);
        }

        // الحديث
        TextPaint hp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        hp.setColor(0xFFFFFFFF);
        hp.setTextSize(44);
        StaticLayout sl = StaticLayout.Builder.obtain("«من صلى عليَّ صلاةً صلى الله عليه بها عشرًا»",
                0, "«من صلى عليَّ صلاةً صلى الله عليه بها عشرًا»".length(), hp, W - 280)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(0f, 1.2f).build();
        cv.save();
        cv.translate(140, 1040);
        sl.draw(cv);
        cv.restore();

        tp.setTypeface(Typeface.DEFAULT);
        tp.setColor(t.sub);
        tp.setTextSize(36);
        cv.drawText(L.t("تطبيق ذكرني • ") + AppConfig.SHARE_URL, W / 2f, H - 190, tp);
        return bmp;
    }

    static void share(Activity a, Prefs p, Theme t) {
        try {
            Bitmap b = render(p, t);
            File dir = new File(a.getCacheDir(), "share");
            dir.mkdirs();
            File f = new File(dir, "card.png");
            try (FileOutputStream o = new FileOutputStream(f)) {
                b.compress(Bitmap.CompressFormat.PNG, 100, o);
            }
            b.recycle();
            Uri uri = CardProvider.uri(a);
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("image/png");
            s.putExtra(Intent.EXTRA_STREAM, uri);
            s.putExtra(Intent.EXTRA_TEXT, L.t("اللهم صلِّ وسلِّم على نبينا محمد\nشاركني الأجر بتطبيق «ذكرني»: ") + AppConfig.SHARE_URL);
            s.setClipData(android.content.ClipData.newRawUri("card", uri));
            s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.startActivity(Intent.createChooser(s, L.t("مشاركة بطاقة الإنجاز")));
        } catch (Throwable e) {
            Toast.makeText(a, L.t("تعذّر إنشاء البطاقة"), Toast.LENGTH_SHORT).show();
        }
    }
}
