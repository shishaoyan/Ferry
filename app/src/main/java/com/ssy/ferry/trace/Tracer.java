package com.ssy.ferry.trace;



public abstract class Tracer extends LooperObserver implements ITracer {

    private volatile boolean isAlive = false;
    private static final String TAG = "Matrix.Tracer";

   
    protected void onAlive() {
        FerryLog.i(TAG, "[onAlive] %s", this.getClass().getName());

    }

   
    protected void onDead() {
        FerryLog.i(TAG, "[onDead] %s", this.getClass().getName());
    }

    @Override
    final synchronized public void onStartTrace() {
        if (!isAlive) {
            this.isAlive = true;
            onAlive();
        }
    }

    @Override
    final synchronized public void onCloseTrace() {
        if (isAlive) {
            this.isAlive = false;
            onDead();
        }
    }

    @Override
    public boolean isAlive() {
        return isAlive;
    }

    private volatile boolean isForeground;


    @Override
    public void onForeground(boolean isForeground) {
        this.isForeground = isForeground;
    }

    public boolean isForeground() {
        return isForeground;
    }
}
