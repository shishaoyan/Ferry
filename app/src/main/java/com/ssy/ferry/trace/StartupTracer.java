package com.ssy.ferry.trace;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.SystemClock;

import com.ssy.ferry.core.ActivityThreadHacker;
import com.ssy.ferry.core.Constants;
import com.ssy.ferry.core.FerryHandlerThread;
import com.ssy.ferry.core.MethodMonitor;
import com.ssy.ferry.item.MethodItem;
import com.ssy.ferry.util.FerryLog;
import com.ssy.ferry.util.TraceDataUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.ssy.ferry.core.Constants.DEFAULT_STARTUP_THRESHOLD_MS_COLD;
import static com.ssy.ferry.core.Constants.DEFAULT_STARTUP_THRESHOLD_MS_WARM;

/**
 * 2019-11-05
 * <p>
 * firstMethod.i       LAUNCH_ACTIVITY   onWindowFocusChange   LAUNCH_ACTIVITY    onWindowFocusChange
 * ^                         ^                   ^                     ^                  ^
 * |                         |                   |                     |                  |
 * |---------app---------|---|---firstActivity---|---------...---------|---careActivity---|
 * |<--applicationCost-->|
 * |<----firstScreenCost---->|
 * |<---------------------------allCost(cold)------------------------->|
 * .                         |<--allCost(warm)-->|
 *
 * @author Mr.S
 */
public class StartupTracer extends Tracer implements MethodMonitor.IMethodMonitorListener, Application.ActivityLifecycleCallbacks {
    private static final String TAG = "Ferry.StartupTracer";
    private long coldCost;
    private long firstScreenCost = 0;
    private int activeActivityCount;
    private boolean hasShowSpashActiviry;
    private Set<String> splashActivitys = new HashSet<>();
    private long coldStartupThresholdMs = DEFAULT_STARTUP_THRESHOLD_MS_COLD;
    private long warmStartupThresholdMs = DEFAULT_STARTUP_THRESHOLD_MS_WARM;

    @Override
    public void startTrace() {
        MethodMonitor.getInstance().addListener(this);
        Ferry.with().getApplication().registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void stopTrace() {
        MethodMonitor.getInstance().removeListener(this);
    }

    @Override
    public void dispatchBegin(long beginMs, long cpuBeginMs, long token) {
        super.dispatchBegin(beginMs, cpuBeginMs, token);
    }

    @Override
    public void dispatchEnd(long beginMs, long cpuBeginMs, long endMs, long cpuEndMs, long token, boolean isBelongFrame) {
        super.dispatchEnd(beginMs, cpuBeginMs, endMs, cpuEndMs, token, isBelongFrame);
    }


    private boolean isColdStartup() {
        return coldCost == 0;
    }


    @Override
    public void onActivityFocused(String activityName) {
        long allCost = 0;
        boolean isWarmStartup = false;
        if (isColdStartup()) {
            if (firstScreenCost == 0) {
                firstScreenCost = SystemClock.uptimeMillis() - ActivityThreadHacker.getEggBrokenTime();
            }
            if (hasShowSpashActiviry) {
                allCost = coldCost = SystemClock.uptimeMillis() - ActivityThreadHacker.getEggBrokenTime();
            } else {
                if (splashActivitys.contains(activityName)) {
                    hasShowSpashActiviry = true;
                } else if (splashActivitys.isEmpty()) {
                    FerryLog.i(TAG, "default care activity[%s]", activityName);
                    allCost = coldCost = firstScreenCost;
                } else {
                    FerryLog.w(TAG, "pass this activity[%s] in duration of startup!", activityName);

                }
            }
        } else if (isWarmStartup == isWarmStartUp()) {
            allCost = SystemClock.uptimeMillis() - ActivityThreadHacker.getsLastLauncherAcitvityTime();

        }
        if (allCost > 0) {
            analyse(ActivityThreadHacker.getApplicationCost(), firstScreenCost, allCost, isWarmStartup);
        }

    }

    private void analyse(long applicationCost, long firstScreenCost, long allCost, boolean isWarmStartup) {
        FerryLog.i(TAG, "[report] applicationCost:%s firstScreenCost:%s allCost:%s isWarmStartUp:%s", applicationCost, firstScreenCost, allCost, isWarmStartup);
        long[] data = new long[0];
        if (!isWarmStartup && allCost >= coldStartupThresholdMs) { // for cold startup
            data = MethodMonitor.getInstance().copyData(ActivityThreadHacker.sApplicationCreateBeginMethodIndex);
            ActivityThreadHacker.sApplicationCreateBeginMethodIndex.release();

        } else if (isWarmStartup && allCost >= warmStartupThresholdMs) {
            data = MethodMonitor.getInstance().copyData(ActivityThreadHacker.sLastLauncherActivityMethodIndex);
            ActivityThreadHacker.sLastLauncherActivityMethodIndex.release();
        }

        FerryHandlerThread.getDefaultHandler().post(new AnalyseTask(data, applicationCost, firstScreenCost, allCost, isWarmStartup, ActivityThreadHacker.sApplicationCreateScene));
    }

    /**
     * 温启动 当activity数量为 0 或者 1 而且距离上次的时间小于5s
     *
     * @return
     */
    private boolean isWarmStartUp() {
        return activeActivityCount <= 1 && (SystemClock.uptimeMillis() - ActivityThreadHacker.getsLastLauncherAcitvityTime() > Constants.LIMIT_WARM_THRESHOLD_MS ? false : true);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        activeActivityCount++;
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    private class AnalyseTask implements Runnable {
        long[] data;
        long applicationCost;
        long firstScreenCost;
        long allCost;
        boolean isWarmStartUp;
        int scene;

        public AnalyseTask(long[] data, long applicationCost, long firstScreenCost, long allCost, boolean isWarmStartup, int sApplicationCreateScene) {
            this.data = data;
            this.scene = scene;
            this.applicationCost = applicationCost;
            this.firstScreenCost = firstScreenCost;
            this.allCost = allCost;
            this.isWarmStartUp = isWarmStartUp;
        }

        @Override
        public void run() {
            LinkedList<MethodItem> stack = new LinkedList();
            if (data.length > 0) {
                TraceDataUtils.structuredDataToStack(data, stack, false, -1);
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
            long stackCost = Math.max(allCost, TraceDataUtils.stackToString(stack, reportBuilder, logcatBuilder));
            String stackKey = TraceDataUtils.getTreeKey(stack, stackCost);

            // for logcat
            if ((allCost > coldStartupThresholdMs && !isWarmStartUp)
                    || (allCost > warmStartupThresholdMs && isWarmStartUp)) {
                FerryLog.w(TAG, "stackKey:%s \n%s", stackKey, logcatBuilder.toString());
            }
        }
    }
}
