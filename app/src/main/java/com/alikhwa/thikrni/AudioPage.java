package com.alikhwa.thikrni;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/** الصوتيات: عدة تسجيلات، عشوائي أو بالترتيب، معاينة، إضافة وحذف. */
final class AudioPage extends Page {
    private LinearLayout listBox;
    private TextView tvStatus;
    private File playing;

    AudioPage(MainActivity a, Ui ui) {
        super(a, ui);
        build();
    }

    @Override
    void build() {
        Theme t = ui.t;
        content.addView(ui.pageTitle("الصوتيات", null));

        LinearLayout modeCard = ui.card();
        modeCard.addView(ui.sectionTitle("طريقة التشغيل"));
        modeCard.addView(ui.switchRow("تشغيل الصوت", p.sound(), b -> p.setSound(b)));
        modeCard.addView(ui.divider());
        TextView lbl = ui.text("اختيار التسجيل عند كل تذكير:", 15, Color.WHITE, true);
        lbl.setGravity(Gravity.START);
        lbl.setPadding(0, ui.dp(4), 0, ui.dp(8));
        modeCard.addView(lbl);
        final Ui.Chips[] mode = new Ui.Chips[1];
        mode[0] = ui.chips(new String[]{"عشوائي", "بالترتيب"}, 2, p.sequential() ? 1 : 0, i -> {
            p.setSequential(i == 1);
            mode[0].select(i);
        });
        modeCard.addView(mode[0]);
        modeCard.addView(ui.hint("العشوائي لا يكرر التسجيل نفسه مرتين متتاليتين، وبالترتيب يمرّ على التسجيلات واحدًا بعد الآخر."));
        content.addView(modeCard);

        LinearLayout listCard = ui.card();
        listCard.addView(ui.sectionTitle("تسجيلاتي"));
        tvStatus = ui.text("", 13, t.sub, false);
        tvStatus.setGravity(Gravity.START);
        listCard.addView(tvStatus);
        listBox = new LinearLayout(act);
        listBox.setOrientation(LinearLayout.VERTICAL);
        listCard.addView(listBox, new LinearLayout.LayoutParams(-1, -2));

        VenomButton add = ui.venom("إضافة تسجيل صوتي", false);
        add.setOnClickListener(v -> act.pickAudio(false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, ui.dp(52) + 2 * ui.venomPad());
        lp.topMargin = ui.dp(6);
        listCard.addView(add, lp);

        TextView any = ui.text("لا تجد ملفك؟ استعرض كل الملفات", 12, t.sub, false);
        any.setPadding(ui.dp(8), ui.dp(2), ui.dp(8), ui.dp(8));
        any.setOnClickListener(v -> act.pickAudio(true));
        listCard.addView(any);

        VenomButton test = ui.venom("جرّب التذكير الآن", true);
        test.setOnClickListener(v -> {
            stopPlaying();
            SoundPlayer.play(act, "test", null, msg -> Toast.makeText(act, msg, Toast.LENGTH_LONG).show());
        });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(-1, ui.dp(48) + 2 * ui.venomPad());
        listCard.addView(test, lp2);
        listCard.addView(ui.hint("اختر تسجيلات بصوت قارئ فصيح للصلاة على النبي (mp3 / m4a / ogg / wav). تُنسخ إلى التطبيق وتعمل بدون إنترنت. الحد الأقصى ١٢ تسجيلًا، ٣٠ ميغابايت لكل تسجيل."));
        content.addView(listCard);
        refreshList();
    }

    private void stopPlaying() {
        SoundPlayer.stopPreview();
        playing = null;
    }

    void refreshList() {
        listBox.removeAllViews();
        List<Tracks.Track> tracks = Tracks.list(act);
        boolean raw = SoundPlayer.hasRaw(act);
        if (tracks.isEmpty()) {
            tvStatus.setText(raw ? "لا توجد تسجيلات مضافة؛ يُستخدم التسجيل المرفق مع التطبيق."
                    : "لم تُضف تسجيلات بعد. أضف تسجيلًا لتسمعه عند كل تذكير.");
        } else {
            tvStatus.setText(L.f("عدد التسجيلات: {0}", HijriUtil.digits(tracks.size())));
        }
        for (int i = 0; i < tracks.size(); i++) {
            final Tracks.Track tr = tracks.get(i);
            LinearLayout row = new LinearLayout(act);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, ui.dp(8), 0, ui.dp(8));
            String dur = tr.durationMs > 0 ? "  (" + fmtDur(tr.durationMs) + ")" : "";
            TextView name = ui.text(HijriUtil.digits(i + 1) + ". " + L.t(tr.name) + dur, 15, Color.WHITE, false);
            name.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));

            final TextView play = ui.pillButton(tr.file.equals(playing) ? "إيقاف" : "تشغيل", ui.t.accent);
            play.setOnClickListener(v -> {
                if (tr.file.equals(playing)) {
                    stopPlaying();
                    refreshList();
                } else {
                    playing = tr.file;
                    SoundPlayer.preview(act, tr.file, () -> {
                        playing = null;
                        refreshList();
                    }, msg -> Toast.makeText(act, msg, Toast.LENGTH_LONG).show());
                    refreshList();
                }
            });
            LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(-2, -2);
            pl.setMargins(ui.dp(6), 0, ui.dp(6), 0);
            row.addView(play, pl);

            TextView del = ui.pillButton("حذف", 0xFFFF8A80);
            del.setOnClickListener(v -> {
                if (tr.file.equals(playing)) stopPlaying();
                Tracks.delete(act, tr);
                refreshList();
            });
            row.addView(del);
            listBox.addView(row);
            if (i < tracks.size() - 1) listBox.addView(ui.divider());
        }
    }

    /** نتيجة اختيار ملفات صوتية من الهاتف. */
    void onPicked(final List<android.net.Uri> uris) {
        if (uris.isEmpty()) return;
        Toast.makeText(act, L.t("جارٍ إضافة التسجيل..."), Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            int ok = 0, status = Tracks.OK;
            for (android.net.Uri u : uris) {
                int r = Tracks.add(act, u);
                if (r == Tracks.OK) ok++;
                else status = r;
            }
            final int added = ok, last = status;
            act.runOnUiThread(() -> {
                String msg;
                if (added > 0 && last == Tracks.OK) msg = L.f("تمت إضافة {0} تسجيل", HijriUtil.digits(added));
                else if (last == Tracks.TOO_MANY) msg = L.t("وصلت للحد الأقصى من التسجيلات (١٢)");
                else if (last == Tracks.TOO_BIG) msg = L.t("الملف كبير جدًا (الحد ٣٠ ميغابايت)");
                else if (last == Tracks.BAD_FORMAT) msg = L.t("الملف ليس تسجيلًا صوتيًا مدعومًا، جرّب mp3 أو m4a");
                else msg = L.t(added > 0 ? "أُضيف بعض التسجيلات فقط" : "تعذّر قراءة الملف");
                Toast.makeText(act, msg, Toast.LENGTH_SHORT).show();
                refreshList();
            });
        }).start();
    }

    private static String fmtDur(long ms) {
        long sec = ms / 1000;
        return HijriUtil.digits(sec / 60) + ":" + HijriUtil.digits(sec % 60 / 10) + HijriUtil.digits(sec % 10);
    }

    @Override
    void onShow() {
        refreshList();
    }

    @Override
    void onHide() {
        stopPlaying();
    }
}
