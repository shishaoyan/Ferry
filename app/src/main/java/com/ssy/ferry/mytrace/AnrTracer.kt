package com.ssy.ferry.mytrace

import android.os.Handler
import android.os.SystemClock
import android.util.Log
import com.ssy.ferry.core.Constants
import com.ssy.ferry.core.FerryHandlerThread
import com.ssy.ferry.core.MethodMonitor
import com.ssy.ferry.core.UiThreadMonitor
import java.util.*

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
        if (null != anrTask) {
            anrHandler.removeCallbacks(anrTask)
        }
        isDispatchBegin = true
        anrTask =
            AnrHandleTask(MethodMonitor.getInstance().maskIndex("AnrTracer#dispatchBegin"), token)
        anrHandler.postDelayed(anrTask, Constants.DEFAULT_ANR)
        Log.d(TAG, "dispatchBegin")
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
        Log.d(TAG, "dispatchEnd")
    }

    fun startTrace() {
        UiThreadMonitor.getInstance().addObserver(this)
        this.anrHandler = Handler(FerryHandlerThread.getDefaultHandlerThread()?.getLooper())
    }


    class AnrHandleTask : Runnable {
        private val TAG = "AnrHandleTask"
        var beginRecord: MethodMonitor.Companion.IndexRecord? = null

        constructor(
            beginRecord: MethodMonitor.Companion.IndexRecord, token: Long
        ) {
            this.beginRecord = beginRecord
            this.token = token
        }

        var token: Long = 0

        override fun run() {
            beginRecord?.let {
                val curTime = SystemClock.uptimeMillis()
                var data = MethodMonitor.getInstance().copyData(it)
                Log.d(TAG, "data --" + data.size)
                it.release()
                val stack = LinkedList<MethodItem>()
                if (data.size > 0) {
                    TraceDataUtils.structuredDataToStack(data, stack, true, curTime)
                    TraceDataUtils.trimStack(stack,
                        Constants.TARGET_EVIL_METHOD_STACK,
                        object : TraceDataUtils.IStructuredDataFilter {

                            override fun getFilterMaxCount(): Int {
                                return Constants.FILTER_STACK_MAX_COUNT
                            }


                            override fun isFilter(during: Long, filterCount: Int): Boolean {
                                return during < filterCount * Constants.TIME_UPDATE_CYCLE_MS
                            }

                            override fun fallback(stack: MutableList<MethodItem>, size: Int) {
                                Log.d(
                                    TAG,
                                    "[fallback] size:%s targetSize:%s stack:%s" + size + Constants.TARGET_EVIL_METHOD_STACK + stack
                                )
                                val iterator = stack.listIterator(
                                    Math.min(
                                        size, Constants.TARGET_EVIL_METHOD_STACK
                                    )
                                )
                                while (iterator.hasNext()) {
                                    iterator.next()
                                    iterator.remove()
                                }
                            }
                        })


                    val reportBuilder = StringBuilder()
                    val logcatBuilder = StringBuilder()
                    val stackCost = Math.max(
                        Constants.DEFAULT_ANR,
                        TraceDataUtils.stackToString(stack, reportBuilder, logcatBuilder)
                    )
                    Log.d(TAG,"++++++++ :   $stackCost")
                    Log.d(TAG,"--${logcatBuilder.toString()}")
                }
            }

        }

    }


}