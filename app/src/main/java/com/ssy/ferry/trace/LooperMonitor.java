package com.ssy.ferry.trace;

import android.os.Build;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.Log;
import android.util.Printer;

import java.lang.reflect.Field;
import java.util.HashSet;

public class LooperMonitor implements MessageQueue.IdleHandler {
    private static final String TAG = LooperMonitor.class.getName();
    private static Printer printer;
    private static final HashSet<LooperDispatchListener> listeners = new HashSet<>();

    @Override
    public boolean queueIdle() {
        resetPrinter();
        return true;
    }
    private static final LooperMonitor monitor = new LooperMonitor();
    public LooperMonitor(){
        resetPrinter();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Looper.getMainLooper().getQueue().addIdleHandler(this);
        } else {
            MessageQueue queue = reflectObject(Looper.getMainLooper(), "mQueue");
            queue.addIdleHandler(this);
        }
    }

    public abstract static class LooperDispatchListener {

        boolean isHasDispatchStart = false;

        boolean isValid() {
            return false;
        }

        void dispatchStart() {
            this.isHasDispatchStart = true;
        }

        void dispatchEnd() {
            this.isHasDispatchStart = false;
        }
    }

    public static void register(LooperDispatchListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    public static void unregister(LooperDispatchListener listener) {
        if (null == listener) {
            return;
        }
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    private static void resetPrinter() {
        Log.e("haha","resetPrinter");
        final Printer originPrinter = reflectObject(Looper.getMainLooper(), "mLogging");
        Looper.getMainLooper().setMessageLogging(printer = new Printer() {
            boolean isHasChecked = false;
            boolean isValid = false;

            @Override
            public void println(String x) {
                if (null != originPrinter) {
                    originPrinter.println(x);
                }

                if (!isHasChecked) {
                    isValid = x.charAt(0) == '>' || x.charAt(0) == '<';
                    isHasChecked = true;
                    if (!isValid) {
                    }
                }

                if (isValid) {
                    dispatch(x.charAt(0) == '>');
                }

            }
        });

    }
    private static void dispatch(boolean isBegin) {

        for (LooperDispatchListener listener : listeners) {
            if (listener.isValid()) {
                if (isBegin) {
                    if (!listener.isHasDispatchStart) {
                        listener.dispatchStart();
                    }
                } else {
                    if (listener.isHasDispatchStart) {
                        listener.dispatchEnd();
                    }
                }
            } else if (!isBegin && listener.isHasDispatchStart) {
                listener.dispatchEnd();
            }
        }

    }
    private static <T> T reflectObject(Object instance, String name) {
        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
