package com.ssy.ferry.core

/**
 * 2019-10-17
 * @author Mr.S
 */
class Constants {
    companion object {


        val BUFFER_SIZE  = 100 * 10000 // 7.6M
        val TIME_UPDATE_CYCLE_MS:Long = 5
        val FILTER_STACK_MAX_COUNT = 60
        val FILTER_STACK_KEY_ALL_PERCENT = .3f
        val FILTER_STACK_KEY_PATENT_PERCENT = .8f
        val DEFAULT_EVIL_METHOD_THRESHOLD_MS = 1000
        val DEFAULT_FPS_TIME_SLICE_ALIVE_MS = 10 * 1000
        val TIME_MILLIS_TO_NANO = 1000000
        @kotlin.jvm.JvmField val DEFAULT_ANR:Long = 5 * 1000

        val DEFAULT_DROPPED_NORMAL = 3
        val DEFAULT_DROPPED_MIDDLE = 9
        val DEFAULT_DROPPED_HIGH = 24
        val DEFAULT_DROPPED_FROZEN = 42

        val DEFAULT_STARTUP_THRESHOLD_MS_WARM = 4 * 1000
        val DEFAULT_STARTUP_THRESHOLD_MS_COLD = 10 * 1000

        val DEFAULT_RELEASE_BUFFER_DELAY = 15 * 1000
        val TARGET_EVIL_METHOD_STACK = 30
        val MAX_LIMIT_ANALYSE_STACK_KEY_NUM = 10

        val LIMIT_WARM_THRESHOLD_MS = 5 * 1000

        enum class Type {
            NORMAL, ANR, STARTUP
        }
    }
}