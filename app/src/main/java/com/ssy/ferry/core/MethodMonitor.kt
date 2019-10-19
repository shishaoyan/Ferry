package com.ssy.ferry.core

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.ssy.ferry.listener.LooperDispatchListener

/**
 * 2019-10-16
 * @author Mr.S
 */
class MethodMonitor : MonitorLifecycle {

    override fun start() {
        synchronized(statusLock) {
            if (status < STATUS_STARTED) {
                status = STATUS_STARTED
                alive = true
            }

        }
    }

    override fun stop() {
        synchronized(statusLock) {
            if (status > STATUS_STOPED) {
                status = STATUS_STOPED
                alive = false
            }

        }

    }

    fun copyData(startRecord: MethodMonitor.Companion.IndexRecord): LongArray {


        return copyData(startRecord, IndexRecord(mIndex - 1))
    }

    fun copyData(
        startRecord: MethodMonitor.Companion.IndexRecord,
        endRecord: MethodMonitor.Companion.IndexRecord
    ): LongArray {

        var data = LongArray(0)
        if (endRecord.index == -1) {
            return data;
        }
        try {
            if (startRecord.isValid and endRecord.isValid) {
                var length: Int
                val start = startRecord.index
                val end = endRecord.index
                if (end > start) {
                    length = end - start + 1
                    data = LongArray(length)
                    System.arraycopy(sBuffer, start, data, 0, length)
                } else { //方法满了之后 又开始从新技术  所以 end 会小于 start  所以要分两部分来复制
                    length = 1 + end + (sBuffer.size - start)
                    data = LongArray(length)
                    System.arraycopy(sBuffer, start, data, 0, sBuffer.size - start)
                    System.arraycopy(sBuffer, 0, data, sBuffer.size - start, end + 1)
                }
                return data
            }
        } catch (e: OutOfMemoryError) {

        }
        return data
    }

    companion object {
        val METHOD_ID_MAX = 0xFFFFF
      @JvmField  open var METHOD_ID_DISPATCH = METHOD_ID_MAX - 1
        private val sMainThread = Looper.getMainLooper().thread
        private var sBuffer: LongArray = LongArray(Constants.BUFFER_SIZE)
        private val statusLock = Object()
        private var mIndex = 0
        val looperDispatchListener = object : LooperDispatchListener() {
            override fun isAlive(): Boolean {
                return alive
            }

            override fun dispatchStart() {
                super.dispatchStart()

                MethodMonitor.dispatchStart()
            }

            override fun dispatchEnd() {
                super.dispatchEnd()
                MethodMonitor.dispatchEnd()
            }
        }
        private val mInstance = MethodMonitor()
        private val TAG = MethodMonitor::class.java.name
        private val STATUS_DEFAULT = Integer.MAX_VALUE
        private val STATUS_STARTED = 2
        private val STATUS_STOPED = -1
        var alive = false
        var status = STATUS_DEFAULT
        private val sTimerUpdateThread =
            FerryHandlerThread.getNewHandlerThread("matrix_time_update_thread")
        private val sHandler = Handler(sTimerUpdateThread.looper)
        private val updateTimeLock = Object()
        var isUpdateTime = false

        var curDiffTime: Long = SystemClock.uptimeMillis()
      @JvmField  var diffTime: Long = curDiffTime


        open fun i(methodId: Int) {
            if (status <= STATUS_STOPED) {
                return
            }
            if (methodId >= METHOD_ID_MAX) {
                return
            }
            curDiffTime = SystemClock.uptimeMillis() - diffTime
            if (status == STATUS_DEFAULT) {
                synchronized(statusLock) {
                    realStart()
                }
            }
            if (Thread.currentThread() === sMainThread) {
                if (mIndex < Constants.BUFFER_SIZE) {
                    flatData(methodId, mIndex, true)
                } else {
                    mIndex = -1
                }
                ++mIndex
            }
        }


        open fun o(methodId: Int) {
            if (status <= STATUS_STOPED) {
                return
            }
            if (methodId >= METHOD_ID_MAX) {
                return
            }

            if (Thread.currentThread() === sMainThread) {
                if (mIndex < Constants.BUFFER_SIZE) {
                    flatData(methodId, mIndex, false)
                } else {
                    mIndex = -1
                }
                ++mIndex
            }
        }

        /**
         * 把方法信息变成 long 类型存储 方便上传信息
         *
         */
        private fun flatData(methodId: Int, mIndex: Int, isIn: Boolean) {
            if (methodId == METHOD_ID_DISPATCH) {
                curDiffTime = SystemClock.uptimeMillis() - diffTime
            }
            var trueId = 0L
            if (isIn) {
                trueId = trueId or (1L shl 63)
            }
            trueId = trueId or (methodId.toLong() shl 43)
            trueId = trueId or (curDiffTime and 0x7FFFFFFFFFFL)
            sBuffer?.set(mIndex, trueId)


        }

        private fun realStart() {
            sHandler.removeCallbacksAndMessages(null)
            sHandler.postDelayed(updateTimeRunnable, Constants.TIME_UPDATE_CYCLE_MS)
            LooperMonitor.register(looperDispatchListener)
        }


        fun dispatchStart() {
            isUpdateTime = true
            diffTime = SystemClock.uptimeMillis() - diffTime
            synchronized(updateTimeLock) {
                updateTimeLock.notifyAll()
            }
        }

        fun dispatchEnd() {
            isUpdateTime = false
        }


        open fun getInstance(): MethodMonitor {
            return mInstance
        }

        private val updateTimeRunnable = Runnable {
            try {
                while (true) {
                    while (isUpdateTime and (status > STATUS_STOPED)) {
                        curDiffTime = SystemClock.uptimeMillis() - diffTime
                        Log.e(TAG, "curDiffTime:  $curDiffTime")
                        try {
                            Thread.sleep(Constants.TIME_UPDATE_CYCLE_MS)
                        } catch (e: InterruptedException) {

                        }

                    }
                    synchronized(updateTimeLock) {
                        updateTimeLock.wait()
                    }

                }
            } catch (e: InterruptedException) {
            }
        }
        private var mIndexRecordHead: IndexRecord? = null

        /**
         *这个的含义 就是一个链表  有一个HEAD 代表起始 END 代表最后
         */
        class IndexRecord {
            var index: Int = 0
            var isValid = true
            var source: String? = null
            var next: IndexRecord? = null

            constructor() {
                this.isValid = false
            }

            constructor(index: Int) {
                this.index = index
            }

            /**
             * 该节点 到 head 节点都删除 并赋值新的 head 节点
             */
            fun release() {
                isValid = false
                var record: IndexRecord? = mIndexRecordHead
                var last: IndexRecord? = null
                while (null != record) {
                    if (record == this) {
                        if (null != last) {
                            last.next = record.next
                        } else {
                            mIndexRecordHead = record.next
                        }
                        record.next = null
                        break
                    }
                    last = record
                    record = record.next
                }
            }

            override fun toString(): String {
                return "IndexRecord(index=$index, isValid=$isValid, source=$source)"
            }


        }

    }

    /**
     * 创建 IndexRecord
     */
    fun maskIndex(source: String): IndexRecord {

        if (mIndexRecordHead == null) {
            mIndexRecordHead = IndexRecord(mIndex - 1)
            mIndexRecordHead?.source = source
            return mIndexRecordHead as IndexRecord
        } else {
            val indexRecord = IndexRecord(mIndex - 1)
            indexRecord.source = source
            var record: IndexRecord? = mIndexRecordHead
            var last: IndexRecord? = null
            while (record != null) {
                if (indexRecord.index <= record.index) {
                    if (null == last) {
                        val tmp = mIndexRecordHead
                        mIndexRecordHead = indexRecord
                        indexRecord.next = tmp
                    } else {
                        val tmp = last.next
                        last.next = indexRecord
                        indexRecord.next = tmp
                    }
                    return indexRecord
                }
                last = record
                record = record.next
            }

            last?.next = indexRecord

            return indexRecord
        }
    }

}