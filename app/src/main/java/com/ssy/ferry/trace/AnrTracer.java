package com.ssy.ferry.trace;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class AnrTracer extends Tracer {

    private static final String TAG = AnrTracer.class.getName();
    AnrHandleTask anrTask;
    private Handler anrHandler;

    @Override
    protected void onAlive() {
        super.onAlive();

        this.anrHandler = new Handler(FerryHandlerThread.getDefaultHandlerThread().getLooper());
        UIThreadMonitor.getMonitor().addObserver(this);
    }

    @Override
    public void dispatchBegin(long beginMs, long cpuBeginMs, long token) {
        super.dispatchBegin(beginMs, cpuBeginMs, token);
        Log.e("haha", "--------------dispatchBegin");
        if (null != anrTask) {
            anrHandler.removeCallbacks(anrTask);
        }
        anrTask = new AnrHandleTask(token);

        anrHandler.postDelayed(anrTask, Constants.DEFAULT_ANR);
    }


    @Override
    public void dispatchEnd(long beginMs, long cpuBeginMs, long endMs, long cpuEndMs, long token, boolean isBelongFrame) {
        super.dispatchEnd(beginMs, cpuBeginMs, endMs, cpuEndMs, token, isBelongFrame);
        Log.e("haha", "--------------dispatchEnd");
        if (null != anrTask) {
            anrHandler.removeCallbacks(anrTask);
        }
    }

    class AnrHandleTask implements Runnable {
        long token;
        AppMethodBeat.IndexRecord beginRecord;

        AnrHandleTask(long token) {
            this.token = token;
        }

        public AppMethodBeat.IndexRecord getBeginRecord() {
            return beginRecord;
        }

        AnrHandleTask(AppMethodBeat.IndexRecord record, long token) {
            this.beginRecord = record;
            this.token = token;
        }

        @Override
        public void run() {

            long curTime = SystemClock.uptimeMillis();
            int[] processStat = Utils.getProcessPriority(android.os.Process.myPid());
            long[] data = AppMethodBeat.getInstance().copyData(beginRecord);
            beginRecord.release();
            //   String scene = AppMethodBeat.getVisibleScene();
            LinkedList<MethodItem> stack = new LinkedList<>();
            if (data.length > 0) {
                TraceDataUtils.structuredDataToStack(data, stack, true, curTime);
                TraceDataUtils.trimStack(stack, Constants.TARGET_EVIL_METHOD_STACK, new TraceDataUtils.IStructuredDataFilter() {
                    @Override
                    public boolean isFilter(long during, int fiterCount) {

                        return during < fiterCount * Constants.TIME_UPDATE_CYCLE_MS;
                    }

                    @Override
                    public int getFilterMaxCount() {
                        return Constants.FILTER_STACK_MAX_COUNT;
                    }

                    @Override
                    public void fallback(List<MethodItem> stack, int size) {
                        FerryLog.w(TAG, "[fallback] size:%s targetSize:%s stack:%s", size, Constants.TARGET_EVIL_METHOD_STACK, stack);

                        //直接把超过数量的全部干掉了
                        Iterator iterator = stack.listIterator(Math.min(size,Constants.TARGET_EVIL_METHOD_STACK));
                        while (iterator.hasNext()){
                            iterator.next();
                            iterator.remove();
                        }
                    }
                });
            }

            StringBuilder logcatBuilder = new StringBuilder();
            // memory
            long[] memoryInfo = dumpMemory();
            // Thread state
            Thread.State status = Looper.getMainLooper().getThread().getState();
            String dumpStack = Utils.getStack(Looper.getMainLooper().getThread().getStackTrace(), "|*\t\t", 12);


            UIThreadMonitor monitor = UIThreadMonitor.getMonitor();
            long inputCost = monitor.getQueueCost(UIThreadMonitor.CALLBACK_INPUT, token);
            long animationCost = monitor.getQueueCost(UIThreadMonitor.CALLBACK_ANIMATION, token);
            long traversalCost = monitor.getQueueCost(UIThreadMonitor.CALLBACK_TRAVERSAL, token);

            String printStr = printAnr(memoryInfo, status, logcatBuilder, 0, "", dumpStack, inputCost, animationCost, traversalCost); // for logcat
            Log.e("haha", printStr);
        }
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
//        print.append("|* [Trace]").append("\n");
//        print.append("|*\tStackSize: ").append(stackSize).append("\n");
//        print.append("|*\tStackKey: ").append(stackKey).append("\n");
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
