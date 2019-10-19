package com.ssy.ferry.trace;

import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;

public class UIThreadMonitor implements Runnable, BeatLifecycle {
    private volatile long token = 0L;
    private boolean isBelongFrame = false;
    private long[] dispatchTimeMs = new long[4];
    private HashSet<LooperObserver> observers = new HashSet<>();
    private final static UIThreadMonitor sInstance = new UIThreadMonitor();
    private static final int METHOD_ID_MAX = 0xFFFFF;
    public static final int METHOD_ID_DISPATCH = METHOD_ID_MAX - 1;
    private static final String TAG = UIThreadMonitor.class.getName();
    private volatile boolean isAlive = false;

    private static final String ADD_CALLBACK = "addCallbackLocked";
    /**
     * Callback type: Input callback.  Runs first.
     *
     * @hide
     */
    public static final int CALLBACK_INPUT = 0;

    /**
     * Callback type: Animation callback.  Runs before traversals.
     *
     * @hide
     */
    public static final int CALLBACK_ANIMATION = 1;

    /**
     * Callback type: Commit callback.  Handles post-draw operations for the frame.
     * Runs after traversal completes.
     *
     * @hide
     */
    /**
     * never do queue end code
     */
    public static final int DO_QUEUE_END_ERROR = -100;
    public static final int CALLBACK_TRAVERSAL = 2;
    private static final int CALLBACK_LAST = CALLBACK_TRAVERSAL;
    private int[] queueStatus = new int[CALLBACK_LAST + 1];
    private boolean[] callbackExist = new boolean[CALLBACK_LAST + 1]; // ABA
    private long[] queueCost = new long[CALLBACK_LAST + 1];
    private static final int DO_QUEUE_DEFAULT = 0;
    private static final int DO_QUEUE_BEGIN = 1;
    private static final int DO_QUEUE_END = 2;

    private Object callbackQueueLock;
    private Object[] callbackQueues;
    private Method addTraversalQueue;
    private Method addInputQueue;
    private Method addAnimationQueue;
    private Choreographer choreographer;
    private long frameIntervalNanos = 16666666;

    public static UIThreadMonitor getMonitor() {
        return sInstance;
    }

    public void init() {
        choreographer = Choreographer.getInstance();
        callbackQueueLock = reflectObject(choreographer, "mLock");
        callbackQueues = reflectObject(choreographer, "mCallbackQueues");

        addInputQueue = reflectChoreographerMethod(callbackQueues[CALLBACK_INPUT], ADD_CALLBACK, long.class, Object.class, Object.class);
        addAnimationQueue = reflectChoreographerMethod(callbackQueues[CALLBACK_ANIMATION], ADD_CALLBACK, long.class, Object.class, Object.class);
        addTraversalQueue = reflectChoreographerMethod(callbackQueues[CALLBACK_TRAVERSAL], ADD_CALLBACK, long.class, Object.class, Object.class);
        frameIntervalNanos = reflectObject(choreographer, "mFrameIntervalNanos");

        LooperMonitor.register(new LooperMonitor.LooperDispatchListener() {
            @Override
            public boolean isValid() {
                return isAlive;
            }

            @Override
            public void dispatchStart() {
                super.dispatchStart();
                UIThreadMonitor.this.dispatchBegin();
            }

            @Override
            public void dispatchEnd() {
                super.dispatchEnd();
                UIThreadMonitor.this.dispatchEnd();
            }

        });
    }

    public void dispatchBegin() {
        token = dispatchTimeMs[0] = SystemClock.uptimeMillis();
        dispatchTimeMs[2] = SystemClock.currentThreadTimeMillis();
        AppMethodBeat.i(METHOD_ID_DISPATCH);
        synchronized (observers) {
            for (LooperObserver observer : observers) {
                if (!observer.isDispatchBegin()) {
                    observer.dispatchBegin(dispatchTimeMs[0], dispatchTimeMs[2], token);
                }
            }
        }

    }

    private void doFrameBegin(long token) {
        this.isBelongFrame = true;
    }

    public void dispatchEnd() {
        if (isBelongFrame) {
            doFrameEnd(token);
        }
        dispatchTimeMs[3] = SystemClock.currentThreadTimeMillis();
        dispatchTimeMs[1] = SystemClock.uptimeMillis();
        AppMethodBeat.o(METHOD_ID_DISPATCH);
        synchronized (observers) {
            for (LooperObserver observer : observers) {
                if (observer.isDispatchBegin()) {
                    observer.dispatchEnd(dispatchTimeMs[0], dispatchTimeMs[2], dispatchTimeMs[1], dispatchTimeMs[3], 1, true);
                }
            }
        }
    }

    @Override
    public void run() {
        doFrameBegin(token);
        final long start = System.nanoTime();
        try {
            doQueueBegin(CALLBACK_INPUT);

            addFrameCallback(CALLBACK_ANIMATION, new Runnable() {

                @Override
                public void run() {
                    doQueueEnd(CALLBACK_INPUT);
                    doQueueBegin(CALLBACK_ANIMATION);
                }
            }, true);

            addFrameCallback(CALLBACK_TRAVERSAL, new Runnable() {

                @Override
                public void run() {
                    doQueueEnd(CALLBACK_ANIMATION);
                    doQueueBegin(CALLBACK_TRAVERSAL);
                }
            }, true);

        } finally {

        }
    }

    private <T> T reflectObject(Object instance, String name) {
        try {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Method reflectChoreographerMethod(Object instance, String name, Class<?>... argTypes) {
        try {
            Method method = instance.getClass().getDeclaredMethod(name, argTypes);
            method.setAccessible(true);
            return method;
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public synchronized void onStart() {
        isAlive = true;
        addFrameCallback(CALLBACK_INPUT, this, true);
    }

    @Override
    public synchronized void onStop() {
        isAlive = false;
    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }

    private synchronized void addFrameCallback(int type, Runnable callback, boolean isAddHeader) {

        if (callbackExist[type]) {
            FerryLog.w(TAG, "[addFrameCallback] this type %s callback has exist!", type);
            return;
        }
        if (!isAlive && type == CALLBACK_INPUT) {
            Log.e(TAG, "[addFrameCallback] UIThreadMonitor is not alive!");
            return;
        }
        try {
            synchronized (callbackQueueLock) {
                Method method = null;
                switch (type) {
                    case CALLBACK_INPUT:
                        method = addInputQueue;
                        break;
                    case CALLBACK_ANIMATION:
                        method = addAnimationQueue;
                        break;
                    case CALLBACK_TRAVERSAL:
                        method = addTraversalQueue;
                        break;
                }
                if (null != method) {
                    method.invoke(callbackQueues[type], !isAddHeader ? SystemClock.uptimeMillis() : -1, callback, null);
                    callbackExist[type] = true;
                }
            }
        } catch (Exception e) {
        }
    }


    private void doQueueBegin(int type) {

        queueStatus[type] = DO_QUEUE_BEGIN;
        queueCost[type] = System.nanoTime();

        Log.e("haha", "doQueueBegin type  " + type+"   "+queueStatus[type]);
    }

    private void doQueueEnd(int type) {
        queueStatus[type] = DO_QUEUE_END;
        queueCost[type] = System.nanoTime() - queueCost[type];
        synchronized (callbackExist) {
            callbackExist[type] = false;
        }

        Log.e("haha", "doQueueEnd type  "+queueCost[type]);
    }

    private void doFrameEnd(long token) {
        doQueueEnd(CALLBACK_TRAVERSAL);


        for (int i : queueStatus) {
            if (i != DO_QUEUE_END) {
                queueCost[i] = DO_QUEUE_END_ERROR;
            }
        }


       // queueStatus = new int[CALLBACK_LAST + 1];
        long start = token;
        long end = SystemClock.uptimeMillis();
        synchronized (observers) {
            for (LooperObserver observer : observers) {
                if (observer.isDispatchBegin()) {
                    observer.doFrame("aaaaaaa", start, end, end - start, queueCost[CALLBACK_INPUT], queueCost[CALLBACK_ANIMATION], queueCost[CALLBACK_TRAVERSAL]);
                }
            }
        }

        addFrameCallback(CALLBACK_INPUT, this, true);
        this.isBelongFrame = false;
    }

    public long getQueueCost(int type, long token) {
        if (token != this.token) {
            return -1;
        }
        Log.e("haha", "queueStatus[type]: " + queueStatus[type]);
        return queueStatus[type] == DO_QUEUE_END ? queueCost[type] : 0;
    }

    public void addObserver(LooperObserver observer) {
        if (!isAlive) {
            onStart();
        }
        synchronized (observers) {
            observers.add(observer);
        }
    }

    public void removeObserver(LooperObserver observer) {
        synchronized (observers) {
            observers.remove(observer);
            if (observers.isEmpty()) {
                onStop();
            }
        }
    }
}
