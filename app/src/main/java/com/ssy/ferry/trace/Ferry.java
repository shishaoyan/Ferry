package com.ssy.ferry.trace;

import com.ssy.ferry.core.MethodMonitor;
import com.ssy.ferry.core.UiThreadMonitor;

/**
 * 2019-10-21
 *
 * @author Mr.S
 */
public class Ferry {

   public  void start() {

        AnrTracer anrTracer = new AnrTracer();

        UiThreadMonitor.getInstance().init();
        UiThreadMonitor.getInstance().start();
        MethodMonitor.getInstance().start();
        anrTracer.startTrace();


    }
}
