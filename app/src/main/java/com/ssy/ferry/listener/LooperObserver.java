package com.ssy.ferry.listener;

/**
 * 2019-10-21
 *
 * @author Mr.S
 */
public abstract class LooperObserver {

    public boolean isDispatchBegin = false;

  public  void dispatchBegin(long beginMs, long cpuBeginMs, long token) {
        isDispatchBegin = true;
    }

    void doFrame(
            String focusedActivityName,
            long start,
            long end,
            long frameCostMs,
            long inputCostNs,
            long animationCostNs,
            long traversalCostNs
    ) {

    }

    public  void dispatchEnd(
            long beginMs,
            long cpuBeginMs,
            long endMs,
            long cpuEndMs,
            long token,
            boolean isBelongFrame
    ) {
        isDispatchBegin = false;
    }

}
