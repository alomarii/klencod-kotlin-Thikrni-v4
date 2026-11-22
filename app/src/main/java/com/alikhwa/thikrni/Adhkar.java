package com.alikhwa.thikrni;

import java.util.Random;

/** أذكار قصيرة تُعرض بين تذكيرات الصلاة على النبي. */
public final class Adhkar {
    private Adhkar() {}

    private static final String[] LIST = {
            "سبحان الله وبحمده، سبحان الله العظيم",
            "أستغفر الله العظيم وأتوب إليه",
            "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير",
            "لا حول ولا قوة إلا بالله",
            "سبحان الله، والحمد لله، ولا إله إلا الله، والله أكبر",
            "حسبي الله لا إله إلا هو، عليه توكلت وهو رب العرش العظيم",
            "اللهم أعنّي على ذكرك وشكرك وحسن عبادتك",
            "سبحان الله وبحمده، عدد خلقه ورضا نفسه وزنة عرشه ومداد كلماته",
            "رضيت بالله ربًّا، وبالإسلام دينًا، وبمحمد صلى الله عليه وسلم نبيًّا"
    };

    private static final Random RND = new Random();
    private static int last = -1;

    public static String next() {
        int i;
        do {
            i = RND.nextInt(LIST.length);
        } while (i == last && LIST.length > 1);
        last = i;
        return LIST[i];
    }
}
