package com.ssy.ferry.core

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.util.HashSet

/**
 * 2019-10-17
 * @author Mr.S
 */
class FerryHandlerThread {


    companion object {
        private val TAG = "Ferry.HandlerThread"
        val Ferry_THREAD_NAME = "default_Ferry_thread"
        /**
         * unite defaultHandlerThread for lightweight work,
         * if you have heavy work checking, you can create a new thread
         */
        private var defaultHandlerThread: HandlerThread? = null
        private var defaultHandler: Handler? = null
        private val defaultMainHandler = Handler(Looper.getMainLooper())
        private val handlerThreads = HashSet<HandlerThread>()

        fun getDefaultMainHandler(): Handler {
            return defaultMainHandler
        }

        fun getDefaultHandlerThread(): HandlerThread? {

            synchronized(FerryHandlerThread::class.java) {
                if (null == defaultHandlerThread) {
                    defaultHandlerThread = HandlerThread(Ferry_THREAD_NAME)
                    defaultHandlerThread!!.start()
                    defaultHandler = Handler(defaultHandlerThread!!.looper)
                }
                return defaultHandlerThread
            }
        }

        fun getDefaultHandler(): Handler? {
            return defaultHandler
        }

        fun getNewHandlerThread(name: String): HandlerThread {
            val i = handlerThreads.iterator()
            while (i.hasNext()) {
                val element = i.next()
                if (!element.isAlive) {
                    i.remove()
                }
            }
            val handlerThread = HandlerThread(name)
            handlerThread.start()
            handlerThreads.add(handlerThread)
            return handlerThread
        }
    }

}