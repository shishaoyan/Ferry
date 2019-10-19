package com.ssy.ferry.mytrace

import com.ssy.ferry.core.MethodMonitor
import com.ssy.ferry.core.UiThreadMonitor

/**
 * 2019-10-16
 * @author Mr.S
 */
class Ferry {

    fun start() {

        val anrTracer = AnrTracer()

        UiThreadMonitor.getInstance().init()
        UiThreadMonitor.getInstance().start()
        MethodMonitor.getInstance().start()
        anrTracer.startTrace()




    }
}