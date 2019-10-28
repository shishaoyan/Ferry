package com.ssy.ferry.trace;

import android.os.SystemClock;

import com.ssy.ferry.core.Constants;
import com.ssy.ferry.core.MethodMonitor;
import com.ssy.ferry.core.UiThreadMonitor;
import com.ssy.ferry.util.FerryLog;
import com.ssy.ferry.util.Utils;

public class EvilMethodTracer extends Tracer {
    private final static String TAG = "EvilMethodTracer";
    private int evilMethodTime = Constants.DEFAULT_EVIL_METHOD_THRESHOLD_MS;
    private MethodMonitor.IndexRecord indexRecord;
    private long[] queueTypeCosts = new long[3];

    @Override
    public void doFrame(String focusedActivityName, long start, long end, long frameCostMs, long inputCostNs, long animationCostNs, long traversalCostNs) {
        queueTypeCosts[0] = inputCostNs;
        queueTypeCosts[1] = animationCostNs;
        queueTypeCosts[2] = traversalCostNs;
    }

    @Override
    public void dispatchBegin(long beginMs, long cpuBeginMs, long token) {
        super.dispatchBegin(beginMs, cpuBeginMs, token);
        indexRecord = MethodMonitor.getInstance().maskIndex("EvilMethodTracer.dispatchBegin");
    }

    @Override
    public void dispatchEnd(long beginMs, long cpuBeginMs, long endMs, long cpuEndMs, long token, boolean isBelongFrame) {
        super.dispatchEnd(beginMs, cpuBeginMs, endMs, cpuEndMs, token, isBelongFrame);
        long start = SystemClock.currentThreadTimeMillis();
        try {
            long dispatchTime = endMs - beginMs;
            if (dispatchTime > evilMethodTime) {
                long[] data = MethodMonitor.getInstance().copyData(indexRecord);
                long[] queueCosts = new long[3];
                System.arraycopy(queueTypeCosts, 0, queueCosts, 0, 3);
            }
        } finally {
            indexRecord.release();
            String usage = Utils.calculateCpuUsage(cpuEndMs - cpuBeginMs, endMs - beginMs);
            FerryLog.v(TAG, "[dispatchEnd] token:%s cost:%sms cpu:%sms usage:%s innerCost:%s",
                    token, endMs - beginMs, cpuEndMs - cpuBeginMs, usage, System.currentTimeMillis() - start);
        }


    }

    void startTrace() {
        UiThreadMonitor.getInstance().addObserver(this);
    }

    void stopTrace() {
        UiThreadMonitor.getInstance().removeObserver(this);
    }


    private class AnalyseTask implements Runnable {

        long[] queueCost;
        long[] data;
        long cpuCost;
        long cost;
        long endMs;
        String scene;
        boolean isForeground;

        public AnalyseTask(long[] queueCost, long[] data, long cpuCost, long cost, long endMs, boolean isForeground) {
            this.queueCost = queueCost;
            this.data = data;
            this.cpuCost = cpuCost;
            this.cost = cost;
            this.endMs = endMs;
            this.isForeground = isForeground;
        }

        @Override
        public void run() {

        }
    }


}
