package com.ssy.ferry.core;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by zhangshaowen on 17/7/5.
 */

public class FerryHandlerThread {
    private static final String TAG = "Ferry.HandlerThread";

    public static final String Ferry_THREAD_NAME = "default_Ferry_thread";

    /**
     * unite defaultHandlerThread for lightweight work,
     * if you have heavy work checking, you can create a new thread
     */
    private static volatile HandlerThread defaultHandlerThread;
    private static volatile Handler defaultHandler;
    private static volatile Handler defaultMainHandler = new Handler(Looper.getMainLooper());
    private static HashSet<HandlerThread> handlerThreads = new HashSet<>();

    public static Handler getDefaultMainHandler() {
        return defaultMainHandler;
    }

    public static HandlerThread getDefaultHandlerThread() {

        synchronized (FerryHandlerThread.class) {
            if (null == defaultHandlerThread) {
                defaultHandlerThread = new HandlerThread(Ferry_THREAD_NAME);
                defaultHandlerThread.start();
                defaultHandler = new Handler(defaultHandlerThread.getLooper());
            }
            return defaultHandlerThread;
        }
    }

    public static Handler getDefaultHandler() {
        return defaultHandler;
    }

    public static HandlerThread getNewHandlerThread(String name) {
        for (Iterator<HandlerThread> i = handlerThreads.iterator(); i.hasNext();) {
            HandlerThread element = i.next();
            if (!element.isAlive()) {
                i.remove();
            }
        }
        HandlerThread handlerThread = new HandlerThread(name);
        handlerThread.start();
        handlerThreads.add(handlerThread);
        return handlerThread;
    }
}