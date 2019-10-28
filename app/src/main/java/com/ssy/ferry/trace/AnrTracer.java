package com.ssy.ferry.trace;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.ssy.ferry.core.Constants;
import com.ssy.ferry.core.FerryHandlerThread;
import com.ssy.ferry.core.MethodMonitor2;
import com.ssy.ferry.core.UiThreadMonitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

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

            for (int i = 0; i < data.length; i++) {
                Log.d(TAG, "methodID: " + getMethodId(data[i]));
                Log.d(TAG, "methodID: time " + geteTime(data[i]));
            }
            Log.d(TAG, "data --" + data.length);
            beginRecord.release();
            LinkedHashMap<Integer, Long> longLinkedHashMap = dealData(data);

            Iterator<Map.Entry<Integer, Long>> iterator= longLinkedHashMap.entrySet().iterator();

            while(iterator.hasNext()){
                Map.Entry entry = iterator.next();
                Log.d(TAG,entry.getKey()+":"+entry.getValue());
            }

        }
    }

    private static int getMethodId(long trueId) {
        return (int) ((trueId >> 43) & 0xFFFFFL);
    }

    private static long geteTime(long trueId) {
        return trueId & 0x7FFFFFFFFFFL;
    }


    private static LinkedHashMap<Integer, Long> dealData(long[] data) {
        LinkedHashMap<Integer, Long> linkedHashMap = new LinkedHashMap<>();

        for (int i = 0; i < data.length; i++) {
            int methodId = getMethodId(data[i]);
            long time = geteTime(data[i]);
            if (linkedHashMap.containsKey(methodId)) {
                linkedHashMap.put(methodId, time - linkedHashMap.get(methodId));
            } else {
                linkedHashMap.put(methodId, time);
            }
        }

        return linkedHashMap;
    }

}
