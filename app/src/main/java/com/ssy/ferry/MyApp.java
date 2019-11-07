package com.ssy.ferry;

import android.app.Application;

import com.ssy.ferry.trace.Ferry;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Ferry ferry = new Ferry();
        Ferry.init(this);
        ferry.start();
    }
}
