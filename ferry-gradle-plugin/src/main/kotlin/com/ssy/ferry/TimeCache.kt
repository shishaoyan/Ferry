package com.ssy.ferry

import java.util.HashMap

/**
 * 2019-09-20
 * @author Mr.S
 */
object TimeCache {
    var sStartTimes: MutableMap<String, Long> = HashMap()
    var sEndTimes: MutableMap<String, Long> = HashMap()

    fun setStartTime(methodName: String, time: Long) {
        sStartTimes[methodName] = time
    }

    fun setEndTime(methodName: String, time: Long) {
        sEndTimes[methodName] = time
    }

    fun getCostTime(methodName: String): String {
        val start = sStartTimes[methodName]!!
        val end = sEndTimes[methodName]!!
        return "method: " + methodName + " main " + java.lang.Long.valueOf(end - start) + " ns"
    }


}