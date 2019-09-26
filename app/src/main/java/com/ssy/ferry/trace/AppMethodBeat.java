package com.ssy.ferry.trace;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import java.util.HashSet;

public class AppMethodBeat implements BeatLifecycle {

    private static final String TAG = "SSY.AppMethodBeat";
    private static AppMethodBeat sInstance = new AppMethodBeat();
    private static long sApplicationCreateBeginTime = 0L;//app启动的时间戳
    private static long sApplicationCteateEndTime = 0L;//app结束的时间戳
    private static long sLastLaunchActivityTime = 0L;//上一次 启动 LauncherActivity 的时间戳
    private static long[] sBuffer = new long[Constants.BUFFER_SIZE];
    private static int sLastIndex = -1;
    private static HandlerThread sTimerUpdateThread = FerryHandlerThread.getNewHandlerThread("matrix_time_update_thread");
    private static Handler sHandler = new Handler(sTimerUpdateThread.getLooper());
    private static Runnable checkStartExpiredRunnable = null;

    @Override
    public void onStart() {
        synchronized (statusLock) {
            if (status < STATUS_STARTED && status >= STATUS_OUT_RELEASE) {
                sHandler.removeCallbacksAndMessages(null);
                if (sBuffer == null) {
                    throw new RuntimeException(TAG + " sBuffer == null");
                }
                FerryLog.i(TAG, "[onsSart] preStatus:%s", status, Utils.getStack());
                status = STATUS_STARTED;
            } else {
                FerryLog.w(TAG, "[onStart] current status:%s", status);
            }
        }

    }

    @Override
    public void onStop() {
        synchronized (statusLock) {
            if (status == STATUS_STARTED) {
                FerryLog.i(TAG, "[onStop] %s", Utils.getStack());
                status = STATUS_STOPPED;
            } else {
                FerryLog.w(TAG, "[onStop] currnt status:%s", status);
            }
        }
    }

    @Override
    public boolean isAlive() {
        return status >= STATUS_STARTED;
    }

    public boolean isRealTrace() {
        return status >= STATUS_READY;
    }

    static {
        sHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                realRealease();
            }
        }, Constants.DEFAULT_RELEASE_BUFFER_DELAY);//15秒
    }

    private static void realRealease() {
        synchronized (statusLock) {
            if (status == STATUS_DEFAULT) {
                FerryLog.i(TAG, "[realRealease] timeestamp%s", System.currentTimeMillis());
                sHandler.removeCallbacksAndMessages(null);
                LooperMonitor.register(looperMonitorListener);
                sTimerUpdateThread.quit();
                sBuffer = null;
                status = STATUS_OUT_RELEASE;
            }
        }
    }

    private static IndexRecord sIndexRecordHead = null;

    public static final class IndexRecord {

        public IndexRecord(int index) {
            this.index = index;
        }

        public IndexRecord() {
            this.isValid = false;
        }

        private int index;
        private IndexRecord next;
        public boolean isValid = true;
        public String source;

        //这个方法是 链表需要释放掉 record 以及他之前的节点
        //
        // index     this
        // 0-   0-   0-  0-  0-  0-  0
        public void release() {
            isValid = false;
            IndexRecord record = sIndexRecordHead;
            IndexRecord last = null;
            while (null != record) {
                if (record == this) {
                    if (null != last) {
                        last.next = record.next;
                    } else {
                        sIndexRecordHead = record.next;//null
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
            return "IndexRecord{" +
                    "index=" + index +
                    ", isValid=" + isValid +
                    ", source='" + source + '\'' +
                    '}';
        }
    }

    private static final int METHOD_ID_MAX = 0xFFFFF;
    public static final int METHOD_ID_DISPATCH = METHOD_ID_MAX - 1;
    private volatile static long sCurrentDiffTime = SystemClock.uptimeMillis();
    private volatile static long sDiffTime = sCurrentDiffTime;

    private static void mergeData(int methodId, int index, boolean isIn) {
        if (methodId == AppMethodBeat.METHOD_ID_DISPATCH) {
            sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;
        }
        long turnId = 0L;
        if (isIn) {
            turnId |= 1L << 63;
        }
        turnId |= (long) methodId << 43;
        turnId |= sCurrentDiffTime & 0x7FFFFFFFFFFL;// 后 43位
        sBuffer[index] = turnId;
        checkPileup(index);
        sLastIndex = index;
    }
    public long[] copyData(IndexRecord startRecord) {
        return copyData(startRecord, new IndexRecord(sIndex - 1));
    }
    public long[] copyData(IndexRecord startRecord, IndexRecord endRecord) {
        long current = System.currentTimeMillis();
        long[] data = new long[0];
        try {


            if (startRecord.isValid && endRecord.isValid) {
                int length;
                int start = Math.max(0, startRecord.index);
                int end = Math.max(0, endRecord.index);
                if (end > start) {
                    length = end - start + 1;
                    data = new long[length];
                    System.arraycopy(sBuffer, start, data, 0, length);
                } else {
                    length = 1 + end + (sBuffer.length - start);//这是饶了一圈再到end
                    data = new long[length];
                    System.arraycopy(sBuffer, start, data, 0, sBuffer.length - start);
                    System.arraycopy(sBuffer, 0, data, sBuffer.length - start, end + 1);

                }
                return data;
            }
            return data;
        } catch (OutOfMemoryError error) {
            FerryLog.e(TAG, error.toString());
            return data;
        } finally {
            FerryLog.i(TAG, "[copyData] [%s:%s] cost:%sms", Math.max(0, startRecord.index), endRecord.index, System.currentTimeMillis() - current);

        }
    }

    //没有明白这个方法的意思  如果是head 或者 结尾节点赋值为不可用
    private static void checkPileup(int index) {
        IndexRecord indexRecord = sIndexRecordHead;
        while (indexRecord != null) {
            if (indexRecord.index == index || (indexRecord.index == -1 && sLastIndex == Constants.BUFFER_SIZE - 1)) {
                indexRecord.isValid = false;
                FerryLog.w(TAG, "[checkPileup] %s", indexRecord.toString());
                sIndexRecordHead = indexRecord = indexRecord.next;
            } else {
                break;
            }
        }
    }

    public void printIndexRecord() {
        StringBuffer ss = new StringBuffer("\n");
        IndexRecord record = sIndexRecordHead;
        while (null != record) {
            ss.append(record).append("\n");
            record = record.next;
        }
        FerryLog.i(TAG, "[printIndexRecord] %s", ss.toString());

    }

    private static final int STATUS_DEFAULT = Integer.MAX_VALUE;
    private static final int STATUS_STARTED = 2;
    private static final int STATUS_READY = 1;
    private static final int STATUS_STOPPED = -1;
    private static final int STATUS_EXPIRED_START = -2;
    private static final int STATUS_OUT_RELEASE = -3;
    private static volatile int status = STATUS_DEFAULT;
    private static Thread sMainThread = Looper.getMainLooper().getThread();
    private static Object statusLock = new Object();
    private static boolean assertIn = false;
    private static int sIndex = 0;
    public static long getDiffTime() {
        return sDiffTime;
    }
    public static void i(int menthodId) {
        //如果这个方法已经停止 那么就不用再用 i 方法了
        if (status <= STATUS_STOPPED) {
            return;
        }
        //方法ID 不能超过最大值
        if (menthodId >= METHOD_ID_MAX) {
            return;
        }
        //如果是默认状态 那么就真正执行 状态变成 ready
        if (status == STATUS_DEFAULT) {
            synchronized (statusLock) {
                if (status == STATUS_DEFAULT) {
                    realExecute();
                    status = STATUS_READY;
                }
            }
        }
        //判断线程是主线程
        if (Thread.currentThread().getId() == sMainThread.getId()) {
            //assertIn 防止多次进入
            if (assertIn) {
                android.util.Log.e(TAG, "ERROR!!! AppMethodBeat.i Recursive calls!!!");
                return;
            }
            assertIn = true;
            if (sIndex < Constants.BUFFER_SIZE) {
                mergeData(menthodId, sIndex, true);
            } else {
                sIndex = -1;
            }
            ++sIndex;
            assertIn = false;
        }
    }

    public static void o(int methodId) {
        if (status <= STATUS_STOPPED) {
            return;
        }
        if (methodId >= METHOD_ID_MAX) {
            return;
        }
        if (Thread.currentThread().getId() == sMainThread.getId()) {
            if (sIndex < Constants.BUFFER_SIZE) {
                mergeData(methodId, sIndex, false);
            } else {
                sIndex = -1;
            }
            ++sIndex;
        }
    }


    private static void realExecute() {
        FerryLog.i(TAG, "[realExecute] timestamp:%s", System.currentTimeMillis());
        sCurrentDiffTime = SystemClock.uptimeMillis();

        sHandler.removeCallbacksAndMessages(null);
        //更新时间 dispatchBegin  dispatchEnd
        //就是 我们 i o 方法是切面编程注入的，当有方法执行的时候 调用i  方法结束的时候 调用o
        // 当 i 方法的时候 唤醒更新时间线程，当 o 的时候 wait
        sHandler.postDelayed(sUpdateDiffTimeRunnable, Constants.TIME_UPDATE_CYCLE_MS);
        //15 秒 失效
        sHandler.postDelayed(checkStartExpiredRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (statusLock) {
                    FerryLog.i(TAG, "[startExpired] timestamp:%s status:%s", System.currentTimeMillis(), status);
                    if (status == STATUS_DEFAULT || status == STATUS_READY) {
                        status = STATUS_EXPIRED_START;
                    }
                }
            }
        }, Constants.DEFAULT_RELEASE_BUFFER_DELAY);
        //TODO 这个类的方法需要实现
        //   ActivityThreadHacker.hackSysHandlerCallback();

        // 定期调用
        LooperMonitor.register(looperMonitorListener);
    }

    public static AppMethodBeat getInstance() {
        return sInstance;
    }

    private static LooperMonitor.LooperDispatchListener looperMonitorListener = new LooperMonitor.LooperDispatchListener() {
        @Override
        public boolean isValid() {
            return status >= STATUS_READY;
        }

        @Override
        public void dispatchStart() {
            super.dispatchStart();
            AppMethodBeat.dispatchBegin();
        }

        @Override
        public void dispatchEnd() {
            super.dispatchEnd();
            AppMethodBeat.dispatchEnd();
        }
    };

    //这两个 dispatch 主要改变 isPauseUpdateTime 的值
    // isPauseUpdateTime ==false  说明 dispatchBegin 了 ，然后 sUpdateDiffTimeRunnable 每五秒更新时间
    //这个的功能是为了 避免每次方法获取时间的开销 开一个线程每5毫秒更新一下时间
    //当然这个也是通过 UImonitor 用时更新
    private static void dispatchBegin() {
        sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;
        isPauseUpdateTime = false;
        synchronized (updateTimeLock) {
            updateTimeLock.notify();
        }
    }

    private static void dispatchEnd() {
        isPauseUpdateTime = true;
    }

    private static HashSet<IAppMethodBeatListener> listeners = new HashSet<>();

    public void addListener(IAppMethodBeatListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(IAppMethodBeatListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private static boolean isPauseUpdateTime = false;
    private static Object updateTimeLock = new Object();
    private static Runnable sUpdateDiffTimeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    while (!isPauseUpdateTime && status > STATUS_STOPPED) {
                        sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;
                        SystemClock.sleep(Constants.TIME_UPDATE_CYCLE_MS);
                    }
                    synchronized (updateTimeLock) {
                        updateTimeLock.wait();
                    }
                }
            } catch (InterruptedException e) {
                FerryLog.e(TAG, "" + e.toString());
            }
        }
    };
}
