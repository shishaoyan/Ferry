package com.ssy.ferry.trace;

import android.os.SystemClock;
import android.util.Log;

import com.ssy.ferry.core.Constants;
import com.ssy.ferry.core.FerryHandlerThread;
import com.ssy.ferry.core.MethodMonitor;
import com.ssy.ferry.core.UiThreadMonitor;
import com.ssy.ferry.item.MethodItem;
import com.ssy.ferry.util.FerryLog;
import com.ssy.ferry.util.TraceDataUtils;
import com.ssy.ferry.util.Utils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
                String scene = "default";
                FerryHandlerThread.getDefaultHandlerThread();
                FerryHandlerThread.getDefaultHandler().post(new AnalyseTask(true, scene, data, queueCosts, cpuEndMs - cpuBeginMs, endMs - beginMs, endMs));

            }
        } finally {
            indexRecord.release();
            String usage = Utils.calculateCpuUsage(cpuEndMs - cpuBeginMs, endMs - beginMs);
            FerryLog.v(TAG, "[dispatchEnd] token:%s cost:%sms cpu:%sms usage:%s innerCost:%s",
                    token, endMs - beginMs, cpuEndMs - cpuBeginMs, usage, System.currentTimeMillis() - start);
        }


    }

    @Override
    public void startTrace() {
        UiThreadMonitor.getInstance().addObserver(this);
    }

    @Override
    public void stopTrace() {
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

        AnalyseTask(boolean isForeground, String scene, long[] data, long[] queueCost, long cpuCost, long cost, long endMs) {
            this.isForeground = isForeground;
            this.scene = scene;
            this.cost = cost;
            this.cpuCost = cpuCost;
            this.data = data;
            this.queueCost = queueCost;
            this.endMs = endMs;
        }

        @Override
        public void run() {
            analyse();
        }

        void analyse() {
            int[] processStat = Utils.getProcessPriority(android.os.Process.myPid());
            String usage = Utils.calculateCpuUsage(cpuCost, cost);
            LinkedList<MethodItem> stack = new LinkedList<>();
            if (data.length > 0) {
                TraceDataUtils.structuredDataToStack(data, stack, true, endMs);
                TraceDataUtils.trimStack(stack, Constants.TARGET_EVIL_METHOD_STACK, new TraceDataUtils.IStructuredDataFilter() {
                    @Override
                    public boolean isFilter(long during, int filterCount) {
                        return during < filterCount * Constants.TIME_UPDATE_CYCLE_MS;
                    }

                    @Override
                    public int getFilterMaxCount() {
                        return Constants.FILTER_STACK_MAX_COUNT;
                    }

                    @Override
                    public void fallback(List<MethodItem> stack, int size) {
                        FerryLog.w(TAG, "[fallback] size:%s targetSize:%s stack:%s", size, Constants.TARGET_EVIL_METHOD_STACK, stack);
                        Iterator iterator = stack.listIterator(Math.min(size, Constants.TARGET_EVIL_METHOD_STACK));
                        while (iterator.hasNext()) {
                            iterator.next();
                            iterator.remove();
                        }
                    }
                });
            }
            StringBuilder reportBuilder = new StringBuilder();
            StringBuilder logcatBuilder = new StringBuilder();
            long stackCost = Math.max(cost, TraceDataUtils.stackToString(stack, reportBuilder, logcatBuilder));
            String stackKey = TraceDataUtils.getTreeKey(stack, stackCost);

            FerryLog.w(TAG, "%s", printEvil(scene, processStat, isForeground, logcatBuilder, stack.size(), stackKey, usage, queueCost[0], queueCost[1], queueCost[2], cost)); // for logcat

            if (stackCost >= Constants.DEFAULT_ANR_INVALID || processStat[0] > 10) {
                FerryLog.w(TAG, "The checked anr task was not executed on time. "
                        + "The possible reason is that the current process has a low priority. just pass this report");
                return;
            }


        }
    }

    private String printEvil(String scene, int[] processStat, boolean isForeground, StringBuilder stack, long stackSize, String stackKey, String usage, long inputCost,
                             long animationCost, long traversalCost, long allCost) {
        StringBuilder print = new StringBuilder();
        print.append(String.format("-\n>>>>>>>>>>>>>>>>>>>>> maybe happens Jankiness!(%sms) <<<<<<<<<<<<<<<<<<<<<\n", allCost));
        print.append("|* scene: ").append(scene).append("\n");
        print.append("|* [ProcessStat]").append("\n");
        print.append("|*\t\tPriority: ").append(processStat[0]).append("\n");
        print.append("|*\t\tNice: ").append(processStat[1]).append("\n");
        print.append("|*\t\tForeground: ").append(isForeground).append("\n");
        print.append("|* [CPU]").append("\n");
        print.append("|*\t\tusage: ").append(usage).append("\n");
        print.append("|* [doFrame]").append("\n");
        print.append("|*\t\tinputCost: ").append(inputCost).append("\n");
        print.append("|*\t\tanimationCost: ").append(animationCost).append("\n");
        print.append("|*\t\ttraversalCost: ").append(traversalCost).append("\n");
        print.append("|* [Trace]").append("\n");
        print.append("|*\t\tStackSize: ").append(stackSize).append("\n");
        print.append("|*\t\tStackKey: ").append(stackKey).append("\n");
        print.append(stack.toString());

        print.append("=========================================================================");
        return print.toString();
    }

}
