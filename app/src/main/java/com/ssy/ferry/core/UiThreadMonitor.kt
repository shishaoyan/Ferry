package com.ssy.ferry.core

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

        private val observers = HashSet<LooperObserver>()
        private val TAG = UiThreadMonitor::class.java.name
        private val sInstance = UiThreadMonitor()
        open fun getInstance(): UiThreadMonitor {
            return sInstance
        }

        fun dispatchStart() {
            synchronized(observers) {
                for (observer in observers) {
                    if (!observer.isDispatchBegin()) {

                    }
                }
            }
        }

        fun dispatchEnd() {

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