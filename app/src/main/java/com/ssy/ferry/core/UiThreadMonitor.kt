package com.ssy.ferry.core

import android.os.SystemClock
import android.util.Log
import com.ssy.ferry.listener.LooperDispatchListener
import com.ssy.ferry.listener.LooperObserver
import java.util.HashSet

/**
 * 2019-10-16
 * @author Mr.S
 */
class UiThreadMonitor : Runnable, MonitorLifecycle {


    override fun start() {
        alive = true
    }

    override fun stop() {
        alive = false
    }


    var alive = false

    companion object {
        /**
         * 0 starttime  1 endtime
         * 2 startThreadTime  3 endThreadTime
         */
        private val dispatchTimeMs = LongArray(4)
        private var token: Long = 0
        private var isBelongFrame = false
        private val observers = HashSet<LooperObserver>()
        private val TAG = UiThreadMonitor::class.java.name
        private val sInstance = UiThreadMonitor()
        open fun getInstance(): UiThreadMonitor {
            return sInstance
        }

        fun dispatchStart() {
            MethodMonitor.i(MethodMonitor.METHOD_ID_DISPATCH)
            dispatchTimeMs[0] = SystemClock.uptimeMillis()
            token = dispatchTimeMs[0]
            dispatchTimeMs[2] = SystemClock.currentThreadTimeMillis()
            synchronized(observers) {
                for (observer in observers) {
                    if (!observer.isDispatchBegin) {
                        observer.dispatchBegin(dispatchTimeMs[0], dispatchTimeMs[2], token)
                    }
                }
            }
        }

        fun dispatchEnd() {
            MethodMonitor.o(MethodMonitor.METHOD_ID_DISPATCH)
            dispatchTimeMs[1] = SystemClock.uptimeMillis()
            dispatchTimeMs[3] = SystemClock.currentThreadTimeMillis()
            synchronized(observers) {
                for (observer in observers) {
                    if (observer.isDispatchBegin) {
                        observer.dispatchEnd(dispatchTimeMs[0], dispatchTimeMs[2],dispatchTimeMs[1], dispatchTimeMs[3], token,isBelongFrame)
                    }
                }
            }
        }
    }


    override fun run() {

    }


    fun init() {

        LooperMonitor.register(object : LooperDispatchListener() {
            override fun isAlive(): Boolean {
                return alive
            }

            override fun dispatchStart() {
                super.dispatchStart()
                UiThreadMonitor.dispatchStart()
            }

            override fun dispatchEnd() {
                super.dispatchEnd()
                UiThreadMonitor.dispatchEnd()
            }
        })
    }


    fun addObserver(observer: LooperObserver) {
        if (!alive) {
            start()
        }
        synchronized(observers) {
            observers.add(observer)
        }
    }

    fun removeObserver(observer: LooperObserver) {
        synchronized(observers) {
            observers.remove(observer)
            if (observers.isEmpty()) {
                stop()
            }
        }
    }

}