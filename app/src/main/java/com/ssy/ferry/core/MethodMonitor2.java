package com.ssy.ferry.core;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;


/**
 * 2019-10-21
 *
 * @author Mr.S
 */
public class MethodMonitor2 extends MonitorLifecycle {
    private static IndexRecord mIndexRecordHead = null;
    private static int METHOD_ID_MAX = 0xFFFFF;
    //  private val sMainThread = Looper.getMainLooper().thread as Thread
    private static Object statusLock = new Object();
    private static long[] sBuffer = new long[Constants.BUFFER_SIZE];
    private static int mIndex = 0;
    private static LooperMonitor.LooperDispatchListener looperDispatchListener = new LooperMonitor.LooperDispatchListener() {
        @Override
        public void dispatchStart() {
            super.dispatchStart();
            MethodMonitor2.dispatchStart();
        }

        @Override
        public void dispatchEnd() {
            super.dispatchEnd();
            MethodMonitor2.dispatchEnd();
        }

        public boolean isAlive() {
            return alive;
        }
    };


    public static final int METHOD_ID_DISPATCH = METHOD_ID_MAX - 1;

    private static MethodMonitor2 mInstance = new MethodMonitor2();
    private static String TAG = MethodMonitor2.class.getName();

    private static final int STATUS_DEFAULT = Integer.MAX_VALUE;
    private static final int STATUS_STARTED = 2;
    private static final int STATUS_STOPED = -1;
    private static boolean alive = false;
    private static int status = STATUS_DEFAULT;
    //   private static HandlerThread sTimerUpdateThread = FerryHandlerThread.getNewHandlerThread("matrix_time_update_thread");
    // private static Handler sHandler = new Handler(sTimerUpdateThread.getLooper());
    private static Object updateTimeLock = new Object();
    static boolean isUpdateTime = false;

    static long curDiffTime = SystemClock.uptimeMillis();
    public static long diffTime = curDiffTime;

    public static void i(int methodId) {
//        if (status <= STATUS_STOPED) {
//            return;
//        }
//        if (methodId >= METHOD_ID_MAX) {
//            return;
//        }
//        curDiffTime = SystemClock.uptimeMillis() - diffTime;
//        if (status == STATUS_DEFAULT) {
//            synchronized (statusLock) {
//                realStart();
//            }
//        }
//        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
//            if (mIndex < Constants.BUFFER_SIZE) {
//                flatData(methodId, mIndex, true);
//            } else {
//                mIndex = -1;
//            }
//            ++mIndex;
//        }
    }

    public static void o(int methodId) {
//        if (status <= STATUS_STOPED) {
//            return;
//        }
//        if (methodId >= METHOD_ID_MAX) {
//            return;
//        }
//
//        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
//            if (mIndex < Constants.BUFFER_SIZE) {
//                flatData(methodId, mIndex, false);
//            } else {
//                mIndex = -1;
//            }
//            ++mIndex;
//        }
    }

    /**
     * 把方法信息变成 long 类型存储 方便上传信息
     */
    private static void flatData(int methodId, int mIndex, boolean isIn) {
        if (methodId == METHOD_ID_DISPATCH) {
            curDiffTime = SystemClock.uptimeMillis() - diffTime;
        }
        long trueId = 0L;
        if (isIn) {
            trueId |= 1L << 63;
        }
        trueId |= (long) methodId << 43;
        trueId |= curDiffTime & 0x7FFFFFFFFFFL;
        sBuffer[mIndex] = trueId;

    }

    private static void realStart() {
//        sHandler.removeCallbacksAndMessages(null);
//        sHandler.postDelayed(updateTimeRunnable, Constants.TIME_UPDATE_CYCLE_MS);
//        LooperMonitor.register(looperDispatchListener);
    }


    static void dispatchStart() {
        isUpdateTime = true;
        diffTime = SystemClock.uptimeMillis() - diffTime;
        synchronized (updateTimeLock) {
            updateTimeLock.notifyAll();
        }
    }

    static void dispatchEnd() {
        isUpdateTime = false;
    }


    public static MethodMonitor2 getInstance() {
        return mInstance;
    }

    static Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    while (isUpdateTime && (status > STATUS_STOPED)) {
                        curDiffTime = SystemClock.uptimeMillis() - diffTime;
                        Log.e(TAG, "curDiffTime:  $curDiffTime");
                        try {
                            Thread.sleep(Constants.TIME_UPDATE_CYCLE_MS);
                        } catch (InterruptedException e) {

                        }

                    }
                    synchronized (updateTimeLock) {
                        updateTimeLock.wait();
                    }

                }
            } catch (InterruptedException e) {
            }
        }
    };


    @Override
    public void start() {
        synchronized (statusLock) {
            if (status < STATUS_STARTED) {
                status = STATUS_STARTED;
                alive = true;
            }

        }
    }

    @Override
    public void stop() {
        synchronized (statusLock) {
            if (status > STATUS_STOPED) {
                status = STATUS_STOPED;
                alive = false;
            }

        }
    }

    public long[] copyData(IndexRecord startRecord) {


        return copyData(startRecord, new IndexRecord(mIndex - 1));
    }

    long[] copyData(
            IndexRecord startRecord,
            IndexRecord endRecord
    ) {

        long[] data = new long[0];
        if (endRecord.index == -1) {
            return data;
        }
        try {
            if (startRecord.isValid && endRecord.isValid) {
                int length;
                int start = startRecord.index;
                int end = endRecord.index;
                if (end > start) {
                    length = end - start + 1;
                    data = new long[length];
                    System.arraycopy(sBuffer, start, data, 0, length);
                } else { //方法满了之后 又开始从新技术  所以 end 会小于 start  所以要分两部分来复制
                    length = 1 + end + (sBuffer.length - start);
                    data = new long[length];
                    System.arraycopy(sBuffer, start, data, 0, sBuffer.length - start);
                    System.arraycopy(sBuffer, 0, data, sBuffer.length - start, end + 1);
                }
                return data;
            }
        } catch (OutOfMemoryError e) {

        }
        return data;
    }


    /**
     * 创建 IndexRecord
     */
  public  IndexRecord maskIndex(String source) {

        if (mIndexRecordHead == null) {
            mIndexRecordHead = new IndexRecord(mIndex - 1);
            mIndexRecordHead.source = source;
            return mIndexRecordHead;
        } else {
            IndexRecord indexRecord = new IndexRecord(mIndex - 1);
            indexRecord.source = source;
            IndexRecord record = mIndexRecordHead;
            IndexRecord last = null;
            while (record != null) {
                if (indexRecord.index <= record.index) {
                    if (null == last) {
                        IndexRecord tmp = mIndexRecordHead;
                        mIndexRecordHead = indexRecord;
                        indexRecord.next = tmp;
                    } else {
                        IndexRecord tmp = last.next;
                        last.next = indexRecord;
                        indexRecord.next = tmp;
                    }
                    return indexRecord;
                }
                last = record;
                record = record.next;
            }

            last.next = indexRecord;

            return indexRecord;
        }
    }

    /**
     * 这个的含义 就是一个链表  有一个HEAD 代表起始 END 代表最后
     */
    public class IndexRecord {
        int index = 0;
        boolean isValid = true;
        String source = null;
        IndexRecord next = null;

        IndexRecord() {
            this.isValid = false;
        }

        IndexRecord(int index) {
            this.index = index;
        }

        /**
         * 该节点 到 head 节点都删除 并赋值新的 head 节点
         */
        public void release() {
            isValid = false;
            IndexRecord record = mIndexRecordHead;
            IndexRecord last = null;
            while (null != record) {
                if (record == this) {
                    if (null != last) {
                        last.next = record.next;
                    } else {
                        mIndexRecordHead = record.next;
                    }
                    record.next = null;
                    break;
                }
                last = record;
                record = record.next;
            }
        }

        @Override
        public String toString() {
            return "IndexRecord(index=$index, isValid=$isValid, source=$source)";

        }
    }
}
