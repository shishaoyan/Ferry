package com.ssy.ferry.core;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;


import com.ssy.ferry.util.FerryLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by caichongyang on 2017/5/26.
 **/
public class ActivityThreadHacker {
    private static final String TAG = "Matrix.ActivityThreadHacker";
    private static long sApplicationCreateBeginTime = 0L;
    private static long sApplicationCreateEndTime = 0L;
    private static long sLastLaunchActivityTime = 0L;
    public static MethodMonitor.IndexRecord sLastLaunchActivityMethodIndex = new MethodMonitor.IndexRecord();
    public static MethodMonitor.IndexRecord sApplicationCreateBeginMethodIndex = new MethodMonitor.IndexRecord();
    public static int sApplicationCreateScene = -100;

    public static void hackSysHandlerCallback() {
        try {
            sApplicationCreateBeginTime = SystemClock.uptimeMillis();
            sApplicationCreateBeginMethodIndex = MethodMonitor.getInstance().maskIndex("ApplicationCreateBeginMethodIndex");
            Class<?> forName = Class.forName("android.app.ActivityThread");
            Field field = forName.getDeclaredField("sCurrentActivityThread");
            field.setAccessible(true);
            Object activityThreadValue = field.get(forName);
            Field mH = forName.getDeclaredField("mH");
            mH.setAccessible(true);
            Object handler = mH.get(activityThreadValue);
            Class<?> handlerClass = handler.getClass().getSuperclass();
            Field callbackField = handlerClass.getDeclaredField("mCallback");
            callbackField.setAccessible(true);
            Handler.Callback originalCallback = (Handler.Callback) callbackField.get(handler);
            HackCallback callback = new HackCallback(originalCallback);
            callbackField.set(handler, callback);
            FerryLog.i(TAG, "hook system handler completed. start:%s SDK_INT:%s", sApplicationCreateBeginTime, Build.VERSION.SDK_INT);
        } catch (Exception e) {
            FerryLog.e(TAG, "hook system handler err! %s", e.getCause().toString());
        }
    }

    public static long getApplicationCost() {
        return ActivityThreadHacker.sApplicationCreateEndTime - ActivityThreadHacker.sApplicationCreateBeginTime;
    }

    public static long getEggBrokenTime() {
        return ActivityThreadHacker.sApplicationCreateBeginTime;
    }

    public static long getLastLaunchActivityTime() {
        return ActivityThreadHacker.sLastLaunchActivityTime;
    }


    private final static class HackCallback implements Handler.Callback {
        private static final int LAUNCH_ACTIVITY = 100;
        private static final int CREATE_SERVICE = 114;
        private static final int RECEIVER = 113;
        public static final int EXECUTE_TRANSACTION = 159; // for Android 9.0
        private static boolean isCreated = false;
        private static int hasPrint = 10;

        private final Handler.Callback mOriginalCallback;

        HackCallback(Handler.Callback callback) {
            this.mOriginalCallback = callback;
        }

        @Override
        public boolean handleMessage(Message msg) {

            if (!MethodMonitor.isRealTrace()) {
                return null != mOriginalCallback && mOriginalCallback.handleMessage(msg);
            }

            boolean isLaunchActivity = isLaunchActivity(msg);
            if (hasPrint > 0) {
                FerryLog.i(TAG, "[handleMessage] msg.what:%s begin:%s isLaunchActivity:%s", msg.what, SystemClock.uptimeMillis(), isLaunchActivity);
                hasPrint--;
            }
            if (isLaunchActivity) {
                ActivityThreadHacker.sLastLaunchActivityTime = SystemClock.uptimeMillis();
                ActivityThreadHacker.sLastLaunchActivityMethodIndex = MethodMonitor.getInstance().maskIndex("LastLaunchActivityMethodIndex");
            }

            if (!isCreated) {
                if (isLaunchActivity || msg.what == CREATE_SERVICE || msg.what == RECEIVER) { // todo for provider
                    ActivityThreadHacker.sApplicationCreateEndTime = SystemClock.uptimeMillis();
                    ActivityThreadHacker.sApplicationCreateScene = msg.what;
                    isCreated = true;
                }
            }

            return null != mOriginalCallback && mOriginalCallback.handleMessage(msg);
        }

        private Method method = null;

        private boolean isLaunchActivity(Message msg) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                if (msg.what == EXECUTE_TRANSACTION && msg.obj != null) {
                    try {
                        if (null == method) {
                            Class clazz = Class.forName("android.app.servertransaction.ClientTransaction");
                            method = clazz.getDeclaredMethod("getCallbacks");
                            method.setAccessible(true);
                        }
                        List list = (List) method.invoke(msg.obj);
                        if (!list.isEmpty()) {
                            return list.get(0).getClass().getName().endsWith(".LaunchActivityItem");
                        }
                    } catch (Exception e) {
                        FerryLog.e(TAG, "[isLaunchActivity] %s", e);
                    }
                }
                return msg.what == LAUNCH_ACTIVITY;
            } else {
                return msg.what == LAUNCH_ACTIVITY;
            }
        }
    }


}
