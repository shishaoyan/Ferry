package com.ssy.ferry_android_instrument;

/**
 * 2019-09-26
 *
 * @author Mr.S
 */
public class CaughtRunnable implements Runnable {
    private final Runnable mRunnable;

    public CaughtRunnable(Runnable runnable) {
        this.mRunnable = runnable;
    }

    public void run() {
        try {
            this.mRunnable.run();
        } catch (RuntimeException var2) {
        }

    }
}
