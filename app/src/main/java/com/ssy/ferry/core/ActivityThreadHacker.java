package com.ssy.ferry.core;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.ssy.ferry.util.FerryLog;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 2019-11-05
 *
 * @author Mr.S
 */
public class ActivityThreadHacker {


    private static final String TAG = "Ferry.ActivityThreadHacker";
    private static long sApplicationCreateBeginTime = 0L;
    private static long sApplicationCreateEndTime = 0L;
    private static long sLastLauncherAcitvityTime = 0L;
    public static MethodMonitor.IndexRecord sLastLauncherActivityMethodIndex = new MethodMonitor.IndexRecord();
    public static MethodMonitor.IndexRecord sApplicationCreateBeginMethodIndex = new MethodMonitor.IndexRecord();
    public static int sApplicationCreateScene = -100;


    public static void hackSysHandlerCallback() {
        sApplicationCreateBeginTime = SystemClock.uptimeMillis();
        sApplicationCreateBeginMethodIndex = MethodMonitor.getInstance().maskIndex("ApplicationCreateBeginMethodIndex");

        try {
            Class<?> forName = Class.forName("android.app.ActivityThread");
            Field field = null;
            field = forName.getDeclaredField("sCurrentActivityThread");
            field.setAccessible(true);
            Object activityThreadValue = field.get(forName);
            Field mH = forName.getDeclaredField("mH");
            mH.setAccessible(true);
            Object handler = mH.get(forName);
            Class<?> handerClass = handler.getClass().getSuperclass();
            Field callbackField = handerClass.getDeclaredField("mCallback");
            callbackField.setAccessible(true);
            Handler.Callback originalCallback = (Handler.Callback) callbackField.get(handler);
            HackCallback hackCallback = new HackCallback(originalCallback);
            callbackField.set(handler, hackCallback);
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

    public static long getsLastLauncherAcitvityTime() {
        return ActivityThreadHacker.sLastLauncherAcitvityTime;
    }

    private final static class HackCallback implements Handler.Callback {
        private static final int LAUNCHER_ACTIVITY = 100;
        private static final int CREATE_SERVICE = 114;
        private static final int RECEIVER = 113;
        private static final int EXECUTE_TRANSACTION = 159;//for android 9.0
        private static boolean isCreateed = false;
        private static int hasPrint = 10;
        private final Handler.Callback mOriginalCallback;

        public HackCallback(Handler.Callback mOriginalCallback) {
            this.mOriginalCallback = mOriginalCallback;
        }

        @Override
        public boolean handleMessage(Message msg) {

            if (!MethodMonitor.isRealTrace()) {
                return null == mOriginalCallback ? false : mOriginalCallback.handleMessage(msg);
            }
            boolean isLaunchActivity = isLancherActivity(msg);
            if (hasPrint > 0) {
                FerryLog.i(TAG, "[handleMessage] msg.what:%s begin:%s isLaunchActivity:%s", msg.what, SystemClock.uptimeMillis(), isLaunchActivity);
                hasPrint--;
            }
            if (isLaunchActivity) {
                ActivityThreadHacker.sLastLauncherAcitvityTime = SystemClock.uptimeMillis();
                ActivityThreadHacker.sLastLauncherActivityMethodIndex = MethodMonitor.getInstance().maskIndex("LastLaunchActivityMethodIndex");
            }
            if (!isCreateed) {
                if (isLaunchActivity || msg.what == CREATE_SERVICE || msg.what == RECEIVER) {
                    ActivityThreadHacker.sApplicationCreateEndTime = SystemClock.uptimeMillis();
                    ActivityThreadHacker.sApplicationCreateScene = msg.what;
                    isCreateed = true;
                }
            }


            return null == mOriginalCallback ? false : mOriginalCallback.handleMessage(msg);
        }

        private Method method = null;

        private boolean isLancherActivity(Message msg) {
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
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                return msg.what == LAUNCHER_ACTIVITY;
            } else {
                return msg.what == LAUNCHER_ACTIVITY;
            }

        }
    }
}
