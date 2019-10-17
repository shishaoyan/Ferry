package com.ssy.ferry.mytrace

import android.os.Handler
import android.util.Log
import com.ssy.ferry.core.Constants
import com.ssy.ferry.core.FerryHandlerThread
import com.ssy.ferry.core.MethodMonitor
import com.ssy.ferry.core.UiThreadMonitor
import com.ssy.ferry.listener.LooperDispatchListener
import com.ssy.ferry.listener.LooperObserver

/**
 * 2019-10-16
 * @author Mr.S
 */
class AnrTracer : Tracer() {
    private val TAG = AnrTracer::class.java.name


    @Volatile
    private var anrTask: AnrHandleTask? = null
    private lateinit var anrHandler: Handler


    override fun dispatchBegin(beginMs: Long, cpuBeginMs: Long, token: Long) {
        isDispatchBegin = true
        anrTask =
            AnrHandleTask(MethodMonitor.getInstance().maskIndex("AnrTracer#dispatchBegin"), token)
        anrHandler.postDelayed(anrTask, Constants.DEFAULT_ANR)
    }

    override fun dispatchEnd(
        beginMs: Long,
        cpuBeginMs: Long,
        endMs: Long,
        cpuEndMs: Long,
        token: Long,
        isBelongFrame: Boolean
    ) {
        super.dispatchEnd(beginMs, cpuBeginMs, endMs, cpuEndMs, token, isBelongFrame)
        isDispatchBegin = false
    }

    fun startTrace() {
        UiThreadMonitor.getInstance().addObserver(this)
        this.anrHandler = Handler(FerryHandlerThread.getDefaultHandlerThread()?.getLooper())
    }


    class AnrHandleTask : Runnable {
        constructor(
            beginRecord: MethodMonitor.Companion.IndexRecord, token: Long
        ) {
            this.beginRecord = beginRecord
            this.token = token
        }

        var token: Long = 0
        var beginRecord: MethodMonitor.Companion.IndexRecord? = null
        override fun run() {
            beginRecord?.let {
                var data = MethodMonitor.getInstance().copyData(it)
                it.release()
            }

        }

    }


}