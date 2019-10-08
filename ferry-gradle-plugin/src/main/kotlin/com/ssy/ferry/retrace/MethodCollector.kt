package com.ssy.ferry.retrace

import com.ssy.ferry.trace.retrace.MappingCollector
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashSet

/**
 * 2019-09-27
 * @author Mr.S
 */
class MethodCollector {
    private val TAG = "MethodCollector"

    private val executor: ExecutorService
    private val mappingCollector: MappingCollector
    private val collectedClassExtendMap = ConcurrentHashMap<String, String>()

    private val collectedIgnoreMethodMap = ConcurrentHashMap<String, TraceMethod>()
    //收集方法名 对应的方法信息
    private val collectedMethodMap: ConcurrentHashMap<String, TraceMethod>
    //这个就是我们的重点了 methodId
    private val methodId: AtomicInteger
    private val ignoreCount = AtomicInteger()
    private val incrementCount = AtomicInteger()

    constructor(
        executor: ExecutorService,
        mappingCollector: MappingCollector,
        methodId: AtomicInteger,
        collectedMethodMap: ConcurrentHashMap<String, TraceMethod>

    ) {
        this.executor = executor
        this.mappingCollector = mappingCollector
        this.collectedMethodMap = collectedMethodMap
        this.methodId = methodId
    }

    fun getCollectedClassExtendMap(): ConcurrentHashMap<String, String> {
        return collectedClassExtendMap
    }

    fun getCollectedMethodMap(): ConcurrentHashMap<String, TraceMethod> {
        return collectedMethodMap
    }

    fun collect(srcFolderList: HashSet<File>, dependcyJarList: HashSet<File>) {

        val futures = LinkedList<Future<*>>()

    }

    class CollectMethodNode : MethodVisitor {
        private val className: String
        private val isConstructor: Boolean

        constructor(
            className: String,
            isConstructor: Boolean,
            access: Int,
            name: String?,
            desc: String?,
            signature: String?,
            exceptions: Array<out String>?

        ) : super(Opcodes.ASM5,access, name, desc, signature, exceptions) {
            this.className = className
            this.isConstructor = isConstructor
        }

        override fun visitEnd() {
            super.visitEnd()


        }
    }
}