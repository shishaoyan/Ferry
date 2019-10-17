package com.ssy.ferry.listener

/**
 * 2019-10-17
 * @author Mr.S
 */
open abstract class LooperObserver {

     var isDispatchBegin: Boolean = false

    open  fun dispatchBegin(beginMs: Long, cpuBeginMs: Long, token: Long) {
        isDispatchBegin = true
    }

    open   fun doFrame(
        focusedActivityName: String,
        start: Long,
        end: Long,
        frameCostMs: Long,
        inputCostNs: Long,
        animationCostNs: Long,
        traversalCostNs: Long
    ) {

    }

    open  fun dispatchEnd(
        beginMs: Long,
        cpuBeginMs: Long,
        endMs: Long,
        cpuEndMs: Long,
        token: Long,
        isBelongFrame: Boolean
    ) {
        isDispatchBegin = false
    }


}