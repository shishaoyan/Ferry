package com.ssy.ferry.core;

import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import com.ssy.ferry.util.FerryLog;

import java.util.HashSet;
import java.util.Set;

public class MethodMonitor extends MonitorLifecycle {

    private static final String TAG = "Ferry.MethodMonitor";
    private static MethodMonitor sInstance = new MethodMonitor();
    private static final int STATUS_DEFAULT = Integer.MAX_VALUE;
    private static final int STATUS_STARTED = 2;
    private static final int STATUS_READY = 1;
    private static final int STATUS_STOPPED = -1;
    private static final int STATUS_EXPIRED_START = -2;
    private static final int STATUS_OUT_RELEASE = -3;

    private static volatile int status = STATUS_DEFAULT;
    private static Object statusLock = new Object();

    private static long[] sBuffer = new long[Constants.BUFFER_SIZE];
    private static int sIndex = 0;
    private static int sLastIndex = -1;
    private static boolean assertIn = false;
    private volatile static long sCurrentDiffTime = SystemClock.uptimeMillis();
    private volatile static long sDiffTime = sCurrentDiffTime;
    private static Thread sMainThread = Looper.getMainLooper().getThread();
    private static HandlerThread sTimerUpdateThread = FerryHandlerThread.getNewHandlerThread("matrix_time_update_thread");
    private static Handler sHandler = new Handler(sTimerUpdateThread.getLooper());
    private static final int METHOD_ID_MAX = 0xFFFFF;
    public static final int METHOD_ID_DISPATCH = METHOD_ID_MAX - 1;
    private static Set<String> sFocusActivitySet = new HashSet<>();
    private static String sFocusedActivity = "default";
    private static HashSet<IMethodMonitorListener> listeners = new HashSet<>();
    private static Object updateTimeLock = new Object();
    private static boolean isPauseUpdateTime = false;
    private static Runnable checkStartExpiredRunnable = null;

    static {
        sHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                realRelease();
            }
        }, Constants.DEFAULT_RELEASE_BUFFER_DELAY);
    }

    /**
     * update time runnable
     */
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

    public static MethodMonitor getInstance() {
        return sInstance;
    }


    public boolean isAlive() {
        return status >= STATUS_STARTED;
    }


    public static boolean isRealTrace() {
        return status >= STATUS_READY;
    }

    private static void realRelease() {
        synchronized (statusLock) {
            if (status == STATUS_DEFAULT) {
                FerryLog.i(TAG, "[realRelease] timestamp:%s", System.currentTimeMillis());
                sHandler.removeCallbacksAndMessages(null);
                sTimerUpdateThread.quit();
                sBuffer = null;
                status = STATUS_OUT_RELEASE;
            }
        }
    }

    private static void realExecute() {
        FerryLog.i(TAG, "[realExecute] timestamp:%s", System.currentTimeMillis());

        sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;

        sHandler.removeCallbacksAndMessages(null);
        sHandler.postDelayed(sUpdateDiffTimeRunnable, Constants.TIME_UPDATE_CYCLE_MS);
        sHandler.postDelayed(checkStartExpiredRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (statusLock) {
                    if (status == STATUS_DEFAULT || status == STATUS_READY) {
                        status = STATUS_EXPIRED_START;
                    }
                }
            }
        }, Constants.DEFAULT_RELEASE_BUFFER_DELAY);

      ActivityThreadHacker.hackSysHandlerCallback();
        LooperMonitor.register(new LooperMonitor.LooperDispatchListener() {
            @Override
            public boolean isValid() {
                return status >= STATUS_READY;
            }

            @Override
            public void dispatchStart() {
                super.dispatchStart();
                MethodMonitor.dispatchBegin();
            }

            @Override
            public void dispatchEnd() {
                super.dispatchEnd();
                MethodMonitor.dispatchEnd();
            }
        });
    }

    private static void dispatchBegin() {
        sCurrentDiffTime = SystemClock.uptimeMillis() - sDiffTime;
        isPauseUpdateTime = false;

        synchronized (updateTimeLock) {
            updateTimeLock.notifyAll();
        }
    }

    private static void dispatchEnd() {
        isPauseUpdateTime = true;
    }


    /**
     * hook method when it's called in.
     *
     * @param methodId
     */
    public static void i(int methodId) {

        if (status <= STATUS_STOPPED) {
            return;
        }
        if (methodId >= METHOD_ID_MAX) {
            return;
        }

        if (status == STATUS_DEFAULT) {
            synchronized (statusLock) {
                if (status == STATUS_DEFAULT) {
                    realExecute();
                    status = STATUS_READY;
                }
            }
        }

        if (Thread.currentThread() == sMainThread) {
            if (assertIn) {
                android.util.Log.e(TAG, "ERROR!!! MethodMonitor.i Recursive calls!!!");
                return;
            }
            assertIn = true;
            if (sIndex < Constants.BUFFER_SIZE) {
                mergeData(methodId, sIndex, true);
            } else {
                sIndex = -1;
            }
            ++sIndex;
            assertIn = false;
        }
    }

    /**
     * hook method when it's called out.
     *
     * @param methodId
     */
    public static void o(int methodId) {

        if (status <= STATUS_STOPPED) {
            return;
        }
        if (methodId >= METHOD_ID_MAX) {
            return;
        }
        if (Thread.currentThread() == sMainThread) {
            if (sIndex < Constants.BUFFER_SIZE) {
                mergeData(methodId, sIndex, false);
            } else {
                sIndex = -1;
            }
            ++sIndex;
        }
    }

    /**
     * when the special method calls,it's will be called.
     *
     * @param activity now at which activity
     * @param isFocus  this window if has focus
     */
    public static void at(Activity activity, boolean isFocus) {
        String activityName = activity.getClass().getName();
        if (isFocus) {
            sFocusedActivity = activityName;
            if (sFocusActivitySet.add(activityName)) {
                synchronized (listeners) {
                    for (IMethodMonitorListener listener : listeners) {
                        listener.onActivityFocused(activityName);
                    }
                }
                FerryLog.i(TAG, "[at] Activity[%s] has %s focus!", activityName, isFocus ? "attach" : "detach");
            }
        } else {
            if (sFocusActivitySet.remove(activityName)) {
                FerryLog.i(TAG, "[at] Activity[%s] has %s focus!", activityName, isFocus ? "attach" : "detach");
            }
        }


    }


    public static String getFocusedActivity() {
        return sFocusedActivity;
    }

    /**
     * merge trace info as a long data
     *
     * @param methodId
     * @param index
     * @param isIn
     */
    private static void mergeData(int methodId, int index, boolean isIn) {
        long trueId = 0L;
        if (isIn) {
            trueId |= 1L << 63;
        }
        trueId |= (long) methodId << 43;
        trueId |= sCurrentDiffTime & 0x7FFFFFFFFFFL;
        sBuffer[index] = trueId;
        checkPileup(index);
        sLastIndex = index;
    }

    public void addListener(IMethodMonitorListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    public void clearListener( ) {
        synchronized (listeners) {
            listeners.clear();
        }
    }

    public void removeListener(IMethodMonitorListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private static IndexRecord sIndexRecordHead = null;

    public IndexRecord maskIndex(String source) {
        if (sIndexRecordHead == null) {
            sIndexRecordHead = new IndexRecord(sIndex - 1);
            sIndexRecordHead.source = source;
            return sIndexRecordHead;
        } else {
            IndexRecord indexRecord = new IndexRecord(sIndex - 1);
            indexRecord.source = source;
            IndexRecord record = sIndexRecordHead;
            IndexRecord last = null;
            while (record != null) {
                if (indexRecord.index <= record.index) {
                    if (null == last) {
                        IndexRecord tmp = sIndexRecordHead;
                        sIndexRecordHead = indexRecord;
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

    @Override
    public void start() {
        synchronized (statusLock) {
            if (status < STATUS_STARTED && status >= STATUS_EXPIRED_START) {
                sHandler.removeCallbacks(checkStartExpiredRunnable);
                if (sBuffer == null) {
                    throw new RuntimeException(TAG + " sBuffer == null");
                }
                //  FerryLog.i(TAG, "[onStart] preStatus:%s", status, Utils.getStack());
                this.status = STATUS_STARTED;
            } else {
                FerryLog.w(TAG, "[onStart] current status:%s", status);
            }
        }
    }

    @Override
    public void stop() {
        synchronized (statusLock) {
            if (status == STATUS_STARTED) {
                //  FerryLog.i(TAG, "[onStop] %s", Utils.getStack());
                this.status = STATUS_STOPPED;
            } else {
                FerryLog.w(TAG, "[onStop] current status:%s", status);
            }
        }
    }


    public static final class IndexRecord {
        public IndexRecord(int index) {
            this.index = index;
        }

        public IndexRecord() {
            this.isValid = false;
        }

        public int index;
        private IndexRecord next;
        public boolean isValid = true;
        public String source;

        public void release() {
            isValid = false;
            IndexRecord record = sIndexRecordHead;
            IndexRecord last = null;
            while (null != record) {
                if (record == this) {
                    if (null != last) {
                        last.next = record.next;
                    } else {
                        sIndexRecordHead = record.next;
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
            return "index:" + index + ",\tisValid:" + isValid + " source:" + source;
        }
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
                } else if (end < start) {
                    length = 1 + end + (sBuffer.length - start);
                    data = new long[length];
                    System.arraycopy(sBuffer, start, data, 0, sBuffer.length - start);
                    System.arraycopy(sBuffer, 0, data, sBuffer.length - start, end + 1);
                }
                return data;
            }
            return data;
        } catch (OutOfMemoryError e) {
            FerryLog.e(TAG, e.toString());
            return data;
        } finally {
            FerryLog.i(TAG, "[copyData] [%s:%s] cost:%sms", Math.max(0, startRecord.index), endRecord.index, System.currentTimeMillis() - current);
        }
    }

    public static long getDiffTime() {
        return sDiffTime;
    }

    public void printIndexRecord() {
        StringBuilder ss = new StringBuilder(" \n");
        IndexRecord record = sIndexRecordHead;
        while (null != record) {
            ss.append(record).append("\n");
            record = record.next;
        }
        FerryLog.i(TAG, "[printIndexRecord] %s", ss.toString());
    }

    public interface IMethodMonitorListener {
        void onActivityFocused(String activityName);
    }
}
