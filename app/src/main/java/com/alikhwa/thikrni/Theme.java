package com.alikhwa.thikrni;

import android.content.Context;

/** ألوان المظهر (ثيمات). */
public final class Theme {
    public static final String[] NAMES = {"زمردي", "أزرق ليلي", "ذهبي", "بنفسجي", "ياقوتي", "فيروزي", "فحمي", "غروب"};

    public final int id, bgTop, bgMid, bgBottom, glowA, glowB;
    public final int accent, accentDark, accentLight, sub, onAccent, navBg;
    /** شخصية الثيم: أشكال وحركة. */
    public final Style st;

    private Theme(int id, int bgTop, int bgMid, int bgBottom, int glowA, int glowB,
                  int accent, int accentDark, int accentLight, int sub, int onAccent, int navBg) {
        this.id = id;
        this.bgTop = bgTop;
        this.bgMid = bgMid;
        this.bgBottom = bgBottom;
        this.glowA = glowA;
        this.glowB = glowB;
        this.accent = accent;
        this.accentDark = accentDark;
        this.accentLight = accentLight;
        this.sub = sub;
        this.onAccent = onAccent;
        this.navBg = navBg;
        this.st = Style.of(id);
    }

    private static final Theme[] ALL = {
            new Theme(0, 0xFF0B4A36, 0xFF072A24, 0xFF050E1F, 0x20C997, 0xE6C15A,
                    0xFFE6C15A, 0xFFB8912B, 0xFFFFE9A0, 0xFFB7D3C8, 0xFF06281D, 0xF0072A24),
            new Theme(1, 0xFF123C7A, 0xFF0A2250, 0xFF040B1A, 0x4C8DFF, 0x9CC9FF,
                    0xFF9CC9FF, 0xFF5B8FD1, 0xFFE3F0FF, 0xFFB5C7E3, 0xFF07142E, 0xF00A1A3F),
            new Theme(2, 0xFF4A3510, 0xFF2A1D08, 0xFF120C03, 0xE0A030, 0xFFD27A,
                    0xFFF2C94C, 0xFFB8861B, 0xFFFFF0B3, 0xFFE0CFA6, 0xFF2A1A00, 0xF02A1D08),
            // بنفسجي ملكي
            new Theme(3, 0xFF3B1E6E, 0xFF22104A, 0xFF0C0620, 0x9B6BFF, 0xD58BFF,
                    0xFFD6B4FF, 0xFF9A6BD6, 0xFFF1E3FF, 0xFFCDBFE6, 0xFF1E0B3F, 0xF0160A33),
            // ياقوتي
            new Theme(4, 0xFF6B1230, 0xFF3E0A1C, 0xFF16040B, 0xFF4D6D, 0xFFB3C1,
                    0xFFFF9FB2, 0xFFD6405E, 0xFFFFE0E6, 0xFFE8BFC8, 0xFF3A0716, 0xF02A0612),
            // فيروزي
            new Theme(5, 0xFF0A5C66, 0xFF063B45, 0xFF021419, 0x2DD4BF, 0x7FE7FF,
                    0xFF5EEAD4, 0xFF14A394, 0xFFCCFBF1, 0xFFA7D8D2, 0xFF03312C, 0xF0052A31),
            // فحمي (أسود AMOLED)
            new Theme(6, 0xFF1A1A1A, 0xFF0B0B0B, 0xFF000000, 0x9E9E9E, 0xE0E0E0,
                    0xFFE8E8E8, 0xFF9E9E9E, 0xFFFFFFFF, 0xFFB0B0B0, 0xFF111111, 0xF00B0B0B),
            // غروب
            new Theme(7, 0xFF7A2E12, 0xFF4A1A0C, 0xFF1A0803, 0xFF8A3D, 0xFFC46B,
                    0xFFFFB067, 0xFFE0731F, 0xFFFFE2C2, 0xFFEAC9B0, 0xFF3A1600, 0xF0301208)
    };

    public static Theme of(int id) {
        return ALL[Math.max(0, Math.min(ALL.length - 1, id))];
    }

    public static Theme current(Context c) {
        return of(new Prefs(c).effectiveTheme());
    }

    /** يضع قيمة شفافية على لون. */
    public static int a(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }
}
