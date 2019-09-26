package com.ssy.ferry_android_instrument_toast;

import android.os.Build.VERSION;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.ssy.ferry_android_instrument.CaughtCallback;
import com.ssy.ferry_android_instrument.CaughtRunnable;
import com.ssy.ferry_android_instrument.Reflection;

/**
 * 2019-09-26
 *
 * @author Mr.S
 */

public class ShadowToast {
    public ShadowToast() {
    }

    public static void show(Toast toast) {
        if (VERSION.SDK_INT == 25) {
            workaround(toast).show();
            Log.e("haha","show show show");
        } else {
            toast.show();
        }

    }

    private static Toast workaround(Toast toast) {
        Object tn = Reflection.getFieldValue(toast, "mTN");
        if (null == tn) {
            Log.w("ferry", "Field mTN of " + toast + " is null");
            return toast;
        } else {
            Object handler = Reflection.getFieldValue(tn, "mHandler");
            if (handler instanceof Handler && Reflection.setFieldValue(handler, "mCallback", new CaughtCallback((Handler) handler))) {
                return toast;
            } else {
                Object show = Reflection.getFieldValue(tn, "mShow");
                if (show instanceof Runnable && Reflection.setFieldValue(tn, "mShow", new CaughtRunnable((Runnable) show))) {
                    return toast;
                } else {
                    Log.w("ferry", "Neither field mHandler nor mShow of " + tn + " is accessible");
                    return toast;
                }
            }
        }
    }
}

