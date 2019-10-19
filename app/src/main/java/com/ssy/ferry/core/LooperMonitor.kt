package com.ssy.ferry.core

import android.os.Build
import android.os.Looper
import android.os.MessageQueue
import android.util.Printer
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import com.ssy.ferry.listener.LooperDispatchListener
import kotlin.collections.HashSet


/**
 * 2019-10-16
 * @author Mr.S
 *
 * 这个是我们tracer 的发动机，当 idle 的时候就开始运行
 *
 */
class LooperMonitor : MessageQueue.IdleHandler {
    private val TAG = LooperMonitor::class.java.name
    var printer: Printer? = null


    companion object {
        val looperDispatchListeners = HashSet<LooperDispatchListener>()
        open val looperMonitor = LooperMonitor()

        open fun register(looperDispatchListener: LooperDispatchListener) {
            synchronized(looperDispatchListeners) {
                looperDispatchListeners.add(looperDispatchListener)
            }
        }

    }

    override fun queueIdle(): Boolean {
        repalcePrinter()
        return true
    }

    constructor() {
        repalcePrinter()
        if (SDK_INT >= Build.VERSION_CODES.M) {
            Looper.getMainLooper().queue.addIdleHandler(this)
        } else {
            val queue = reflectObject<MessageQueue>(Looper.getMainLooper(), "mQueue")
            queue?.addIdleHandler(this)
        }
    }

    private fun repalcePrinter() {
        val originPrinter = reflectObject<Printer>(Looper.getMainLooper(), "mLogging")

        Looper.getMainLooper().setMessageLogging( object : Printer {
             var isHasChecked = false
             var isValid = false

            override fun println(x: String) {
                originPrinter?.println(x)

                if (!isHasChecked) {
                    isValid = x[0] == '>' || x[0] == '<'
                    isHasChecked = true
                    if (!isValid) {
                    }
                }

                if (isValid) {
                    dispatch(x[0] == '>')
                }
            }
        })
    }

    private fun dispatch(isStart: Boolean) {
        for (item in looperDispatchListeners) {
            if (item.isAlive()) {
                if (isStart) {
                    item.dispatchStart()
                } else {
                    item.dispatchEnd()
                }

            } else {
                if (item.hasDispatchStart) {
                    item.dispatchEnd()
                }
            }
        }

    }


    private fun <T> reflectObject(instance: Any, name: String): T? {
        try {
            val field = instance.javaClass.getDeclaredField(name)
            field.isAccessible = true
            return field.get(instance) as T
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}