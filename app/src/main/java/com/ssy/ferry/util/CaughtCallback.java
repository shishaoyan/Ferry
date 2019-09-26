package com.ssy.ferry.util;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
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

