package com.alikhwa.thikrni;

import android.app.Application;

/** يهيّئ اللغة قبل أي مستقبِل أو خدمة أو ودجت. */
public class ThikrniApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        L.init(this);
    }
}
