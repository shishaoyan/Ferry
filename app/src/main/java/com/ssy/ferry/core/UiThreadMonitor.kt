package com.ssy.ferry.core

import android.util.Log

/**
 * 2019-10-16
 * @author Mr.S
 */
class UiThreadMonitor : Runnable ,MonitorLifecycle{
    override fun start() {
        alive = true
    }

    override fun stop() {
        alive = false
    }

    private val TAG = UiThreadMonitor::class.java.name
    var alive = false

    companion object {
        private val sInstance = UiThreadMonitor()
        open fun getInstance(): UiThreadMonitor {
            return sInstance
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

            }

            override fun dispatchEnd() {
                super.dispatchEnd()
            }
        })
    }

}