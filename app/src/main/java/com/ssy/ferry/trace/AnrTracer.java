package com.ssy.ferry.trace;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.ssy.ferry.core.Constants;
import com.ssy.ferry.core.FerryHandlerThread;
import com.ssy.ferry.core.MethodMonitor;
import com.ssy.ferry.core.UiThreadMonitor;
import com.ssy.ferry.item.MethodItem;
import com.ssy.ferry.util.DeviceUtil;
import com.ssy.ferry.util.FerryLog;
import com.ssy.ferry.util.TraceDataUtils;
import com.ssy.ferry.util.Utils;

import java.util.LinkedHashMap;
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
        anrTask = new AnrHandleTask(MethodMonitor.getInstance().maskIndex("AnrTracer#dispatchBegin"), token);
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
        if (null != anrTask) {
            anrTask.getBeginRecord().release();
            anrHandler.removeCallbacks(anrTask);
        }
    }

    @Override
    public void startTrace() {
        UiThreadMonitor.getInstance().addObserver(this);
        this.anrHandler = new Handler(FerryHandlerThread.getDefaultHandlerThread().getLooper());
    }

    @Override
    public void stopTrace() {
        UiThreadMonitor.getInstance().removeObserver(this);
    }

    class AnrHandleTask implements Runnable {
        private String TAG = "AnrHandleTask";
        MethodMonitor.IndexRecord beginRecord = null;

        public MethodMonitor.IndexRecord getBeginRecord() {
            return beginRecord;
        }

        AnrHandleTask(
                MethodMonitor.IndexRecord beginRecord, long token
        ) {
            this.beginRecord = beginRecord;
            this.token = token;
        }

        long token = 0;


        @Override
        public void run() {
            long curTime = SystemClock.uptimeMillis();
            long[] data = MethodMonitor.getInstance().copyData(beginRecord);


            for (int i = 0; i < data.length; i++) {
                Log.d(TAG, "methodID: " + getMethodId(data[i]));
                Log.d(TAG, "methodID: time " + geteTime(data[i]));
            }
            Log.d(TAG, "data --" + data.length);
            beginRecord.release();

            // memory
            long[] memoryInfo = dumpMemory();

            // Thread state
            Thread.State status = Looper.getMainLooper().getThread().getState();
            String dumpStack = Utils.getStack(Looper.getMainLooper().getThread().getStackTrace(), "|*\t\t", 12);

            //frame
            long inputCost = 0;
            long animationCost = 0;
            long traversalCost = 0;


            // trace
            LinkedList<MethodItem> stack = new LinkedList();
            if (data.length > 0) {
                TraceDataUtils.structuredDataToStack(data, stack, true, curTime);
//                TraceDataUtils.trimStack(stack, Constants.TARGET_EVIL_METHOD_STACK, new TraceDataUtils.IStructuredDataFilter() {
//                    @Override
//                    public boolean isFilter(long during, int filterCount) {
//                        return during < filterCount * Constants.TIME_UPDATE_CYCLE_MS;
//                    }
//
//                    @Override
//                    public int getFilterMaxCount() {
//                        return Constants.FILTER_STACK_MAX_COUNT;
//                    }
//
//                    @Override
//                    public void fallback(List<MethodItem> stack, int size) {
//                        FerryLog.w(TAG, "[fallback] size:%s targetSize:%s stack:%s", size, Constants.TARGET_EVIL_METHOD_STACK, stack);
//                        Iterator iterator = stack.listIterator(Math.min(size, Constants.TARGET_EVIL_METHOD_STACK));
//                        while (iterator.hasNext()) {
//                            iterator.next();
//                            iterator.remove();
//                        }
//                    }
//                });
            }

            StringBuilder reportBuilder = new StringBuilder();
            StringBuilder logcatBuilder = new StringBuilder();
            long stackCost = Math.max(Constants.DEFAULT_ANR, TraceDataUtils.stackToString(stack, reportBuilder, logcatBuilder));

            // stackKey
            String stackKey = TraceDataUtils.getTreeKey(stack, stackCost);
            FerryLog.w(TAG, "%s \npostTime:%s curTime:%s", printAnr(memoryInfo, status, logcatBuilder, stack.size(), stackKey, dumpStack, inputCost, animationCost, traversalCost), token, curTime); // for logcat

            // report

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


    private String printAnr(long[] memoryInfo, Thread.State state, StringBuilder stack, long stackSize, String stackKey, String dumpStack, long inputCost, long animationCost, long traversalCost) {
        StringBuilder print = new StringBuilder();
        print.append(" \n>>>>>>>>>>>>>>>>>>>>>>> maybe happens ANR(5s)! <<<<<<<<<<<<<<<<<<<<<<<\n");
        print.append("|* [Memory]").append("\n");
        print.append("|*\tDalvikHeap: ").append(memoryInfo[0]).append("kb\n");
        print.append("|*\tNativeHeap: ").append(memoryInfo[1]).append("kb\n");
        print.append("|*\tVmSize: ").append(memoryInfo[2]).append("kb\n");
        print.append("|* [doFrame]").append("\n");
        print.append("|*\tinputCost: ").append(inputCost).append("\n");
        print.append("|*\tanimationCost: ").append(animationCost).append("\n");
        print.append("|*\ttraversalCost: ").append(traversalCost).append("\n");
        print.append("|* [Thread]").append("\n");
        print.append("|*\tState: ").append(state).append("\n");
        print.append("|*\tStack: ").append(dumpStack);
        print.append("|* [Trace]").append("\n");
        print.append("|*\tStackSize: ").append(stackSize).append("\n");
        print.append("|*\tStackKey: ").append(stackKey).append("\n");
        print.append(stack.toString());

        print.append("=========================================================================");
        return print.toString();


    }

    private long[] dumpMemory() {
        long[] memory = new long[3];
        memory[0] = DeviceUtil.getDalvikHeap();
        memory[1] = DeviceUtil.getNativeHeap();
        memory[2] = DeviceUtil.getVmSize();
        return memory;
    }

}
