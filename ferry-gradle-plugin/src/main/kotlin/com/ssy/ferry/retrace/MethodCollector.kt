package com.ssy.ferry.retrace

import com.ssy.ferry.trace.retrace.MappingCollector
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

/**
 * 2019-09-27
 * @author Mr.S
 */
class MethodCollector {
    private val TAG = "MethodCollector"

    private var mappingCollector: MappingCollector? = null
    private val collectedClassExtendMap = ConcurrentHashMap<String, String>()

    private val collectedIgnoreMethodMap = ConcurrentHashMap<String, TraceMethod>()
    //收集方法名 对应的方法信息
    private val collectedMethodMap: ConcurrentHashMap<String, TraceMethod>
    //这个就是我们的重点了 methodId
    private var methodId: AtomicInteger
    private val ignoreCount = AtomicInteger()
    private val incrementCount = AtomicInteger()

    constructor(
        mappingCollector: MappingCollector,
        methodId: AtomicInteger,
        collectedMethodMap: ConcurrentHashMap<String, TraceMethod>

    ) {
        if (mappingCollector != null) {
            this.mappingCollector = mappingCollector
        }
        this.collectedMethodMap = collectedMethodMap
        this.methodId = methodId
    }


    fun getCollectedMethodMap(): ConcurrentHashMap<String, TraceMethod> {
        return collectedMethodMap
    }

    fun push(name: String?, traceMethod: TraceMethod) {
        if (name != null && !collectedMethodMap.contains(name)) {
            collectedMethodMap.put(name, traceMethod)
        }

    }


}