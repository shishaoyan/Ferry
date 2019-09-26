package com.ssy.ferry_android_instrument;

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;

/**
 * 2019-09-26
 *
 * @author Mr.S
 */
public class CaughtCallback implements Callback {
    private final Handler mHandler;

    public CaughtCallback(Handler handler) {
        this.mHandler = handler;
    }

    public boolean handleMessage(Message msg) {
        try {
            this.mHandler.handleMessage(msg);
        } catch (RuntimeException var3) {
        }

        return true;
    }
}

