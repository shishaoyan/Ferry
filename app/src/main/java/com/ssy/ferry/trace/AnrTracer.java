package com.ssy.ferry.trace;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.ssy.ferry.core.Constants;
import com.ssy.ferry.core.FerryHandlerThread;
import com.ssy.ferry.core.MethodMonitor2;
import com.ssy.ferry.core.UiThreadMonitor;

import java.util.LinkedList;

/**
 * 2019-10-21
 *
 * @author Mr.S
 */
public class AnrTracer extends Tracer {


    private static final String TAG = AnrTracer.class.getName();

    private AnrHandleTask anrTask = null;
    private Handler anrHandler;


    public void dispatchBegin(long beginMs, long cpuBeginMs, long token) {
        if (null != anrTask) {
            anrHandler.removeCallbacks(anrTask);
        }
        isDispatchBegin = true;
        anrTask =
                new AnrHandleTask(MethodMonitor2.getInstance().maskIndex("AnrTracer#dispatchBegin"), token);
        anrHandler.postDelayed(anrTask, Constants.DEFAULT_ANR);
        Log.d(TAG, "dispatchBegin");
    }


    public void dispatchEnd(
            long beginMs,
            long cpuBeginMs,
            long endMs,
            long cpuEndMs,
            long token,
            boolean isBelongFrame
    ) {
        super.dispatchEnd(beginMs, cpuBeginMs, endMs, cpuEndMs, token, isBelongFrame);
        isDispatchBegin = false;
        Log.d(TAG, "dispatchEnd");
    }

    void startTrace() {
        UiThreadMonitor.getInstance().addObserver(this);
        this.anrHandler = new Handler(FerryHandlerThread.getDefaultHandlerThread().getLooper());
    }


    class AnrHandleTask implements Runnable {
        private String TAG = "AnrHandleTask";
        MethodMonitor2.IndexRecord beginRecord = null;

        AnrHandleTask(
                MethodMonitor2.IndexRecord beginRecord, long token
        ) {
            this.beginRecord = beginRecord;
            this.token = token;
        }

        long token = 0;


        @Override
        public void run() {
            long curTime = SystemClock.uptimeMillis();
            long[] data = MethodMonitor2.getInstance().copyData(beginRecord);
            Log.d(TAG, "data --" + data.length);
            beginRecord.release();
            LinkedList<MethodItem> stack = new LinkedList<MethodItem>();
            if (data.length > 0) {
                TraceDataUtils.structuredDataToStack(data, stack, true, curTime);


            }

        }
    }


}
