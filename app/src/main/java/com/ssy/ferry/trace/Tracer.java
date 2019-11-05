package com.ssy.ferry.trace;

import com.ssy.ferry.listener.LooperObserver;

/**
 * 2019-10-21
 *
 * @author Mr.S
 */
public abstract class Tracer extends LooperObserver {

    public abstract void startTrace();
    public abstract void stopTrace();


}
