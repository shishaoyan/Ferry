package com.ssy.ferry.core

import android.os.SystemClock

/**
 * 2019-10-16
 * @author Mr.S
 */
class MethodMonitor : MonitorLifecycle {

    private val STATUS_DEFAULT = Integer.MAX_VALUE
    var alive = false
    var status = STATUS_DEFAULT

    private val updateTimeRunnable = Runnable {
        try {
            while (true) {

            }
        } catch (e: InterruptedException) {
        }
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

    override fun start() {
        alive = true
    }

    override fun stop() {
        alive = false
    }

    companion object {
        open fun i(index: Int) {

        }

        open fun o(index: Int) {

        }
    }

}