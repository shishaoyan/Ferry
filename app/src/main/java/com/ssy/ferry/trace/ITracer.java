package com.ssy.ferry.trace;

public interface ITracer {

    boolean isAlive();

    void onStartTrace();

    void onCloseTrace();

    void onForeground(boolean isForeground);
}
