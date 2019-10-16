package com.ssy.ferry.mytrace

/**
 * 2019-10-16
 * @author Mr.S
 */
interface Tracer {
    /**
     * 开始
     */
    fun start()

    /**
     * 结束
     */
    fun end()

    /**
     * 是否存活
     */
    fun isAlive(): Boolean




}
