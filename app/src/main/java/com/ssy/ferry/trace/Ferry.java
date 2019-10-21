package com.ssy.ferry.trace;

import com.ssy.ferry.core.MethodMonitor2;
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
        MethodMonitor2.getInstance().start();
        anrTracer.startTrace();


    }
}
