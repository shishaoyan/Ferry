package com.ssy.ferry.trace;

import android.app.Application;

import com.ssy.ferry.core.MethodMonitor;
import com.ssy.ferry.core.UiThreadMonitor;
import com.ssy.ferry.util.FerryLog;

/**
 * 2019-10-21
 *
 * @author Mr.S
 */
public class Ferry {
    private static Application application;
    private static final String TAG = "Ferry.Ferry";
    private static volatile Ferry sInstance;

    public void start() {

        AnrTracer anrTracer = new AnrTracer();
        EvilMethodTracer evilMethodTracer = new EvilMethodTracer();
        StartupTracer startupTracer = new StartupTracer();

        UiThreadMonitor.getInstance().init();
        UiThreadMonitor.getInstance().start();
        MethodMonitor.getInstance().start();

        anrTracer.startTrace();
        evilMethodTracer.startTrace();
        startupTracer.startTrace();
    }

    public static Ferry init(Application app) {
        application = app;
        synchronized (Ferry.class) {
            if (sInstance == null) {
                sInstance = new Ferry();
            } else {
                FerryLog.e(TAG, "Ferry instance is already set. this invoking will be ignored");
            }
        }
        return sInstance;
    }

    public static Ferry with() {
        if (sInstance == null) {
            throw new RuntimeException("you must init Ferry sdk first");
        }
        return sInstance;
    }

    public Application getApplication() {
        return application;
    }
}
