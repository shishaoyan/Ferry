package com.ssy.ferry.mytrace

/**
 * 2019-10-16
 * @author Mr.S
 */
class AnrTracer : Tracer {

    var live = false //是否存活

    override fun start() {
        live = true

    }

    override fun end() {
        live = false
    }

    override fun isAlive(): Boolean {
        return live
    }



}