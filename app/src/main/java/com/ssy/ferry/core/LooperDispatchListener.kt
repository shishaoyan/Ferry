package com.ssy.ferry.core

/**
 * 2019-10-16
 * @author Mr.S
 *
 * 这个就是呼吸监听 每一次的开始和结束 都是伴随着 Looper 的Printer 的 >>> 和 <<<
 */
abstract class LooperDispatchListener {

    var hasDispatchStart = false

    open fun dispatchStart() {
        hasDispatchStart = true
    }

    open  fun dispatchEnd() {
        hasDispatchStart = false
    }

    fun hasDispatchStart(): Boolean {
        return hasDispatchStart
    }

    open  fun isAlive(): Boolean {
        return false
    }

}