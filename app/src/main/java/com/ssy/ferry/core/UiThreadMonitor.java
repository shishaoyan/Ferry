package com.ssy.ferry.core;

import android.os.SystemClock;

import com.ssy.ferry.listener.LooperObserver;

import java.util.HashSet;

/**
 * 2019-10-21
 *
 * @author Mr.S
 */
public class UiThreadMonitor extends MonitorLifecycle implements Runnable {


    boolean alive = false;
    private static long[] dispatchTimeMs = new long[4];
    private static long token = 0;
    private static boolean isBelongFrame = false;
    private static HashSet<LooperObserver> observers = new HashSet<LooperObserver>();
    private String TAG = UiThreadMonitor.class.getName();
    private static UiThreadMonitor sInstance = new UiThreadMonitor();

    public static UiThreadMonitor getInstance() {
        return sInstance;
    }

   public static void dispatchStart() {
        MethodMonitor2.i(MethodMonitor2.METHOD_ID_DISPATCH);
        dispatchTimeMs[0] = SystemClock.uptimeMillis();
        token = dispatchTimeMs[0];
        dispatchTimeMs[2] = SystemClock.currentThreadTimeMillis();
        synchronized (observers) {

            for (LooperObserver observer : observers) {
                if (!observer.isDispatchBegin) {
                    observer.dispatchBegin(dispatchTimeMs[0], dispatchTimeMs[2], token);
                }
            }
        }
    }

   public static void dispatchEnd() {
        MethodMonitor2.o(MethodMonitor2.METHOD_ID_DISPATCH);
        dispatchTimeMs[1] = SystemClock.uptimeMillis();
        dispatchTimeMs[3] = SystemClock.currentThreadTimeMillis();
        synchronized (observers) {
            for (LooperObserver observer : observers) {
                if (observer.isDispatchBegin) {
                    observer.dispatchEnd(dispatchTimeMs[0], dispatchTimeMs[2], dispatchTimeMs[1], dispatchTimeMs[3], token, isBelongFrame);
                }
            }
        }
    }

   public void init() {

        LooperMonitor.register(new LooperMonitor.LooperDispatchListener() {


            @Override
            boolean isValid() {
                return alive;
            }

            public void dispatchStart() {
                super.dispatchStart();
                UiThreadMonitor.dispatchStart();
            }


            public void dispatchEnd() {
                super.dispatchEnd();
                UiThreadMonitor.dispatchEnd();
            }
        });
    }


    public void addObserver(LooperObserver observer) {
        if (!alive) {
            start();
        }
        synchronized (observers) {
            observers.add(observer);
        }
    }

    void removeObserver(LooperObserver observer) {
        synchronized (observers) {
            observers.remove(observer);
            if (observers.isEmpty()) {
                stop();
            }
        }
    }

    @Override
    public void start() {
        alive = true;
    }

    @Override
    public void stop() {
        alive = false;
    }

    @Override
    public void run() {

    }
}
