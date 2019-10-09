package com.ssy.ferry.retrace

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.google.auto.service.AutoService
import com.google.common.base.Joiner
import com.ssy.ferry.ClassTransformer
import com.ssy.ferry.trace.retrace.MappingCollector
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger


@AutoService(ClassTransformer::class)
class TraceTransformer : ClassTransformer {
    override fun transform(cw: ClassVisitor): ClassVisitor {
        doTransform()
        return TraceClassVisitor(Opcodes.ASM6, cw)
    }

    private fun doTransform() {
        val futures = LinkedList<Future<*>>()
        val executor = Executors.newFixedThreadPool(16)

        val mappingCollector = MappingCollector()
        var methodId = AtomicInteger(0)
        val collectedMethodMap = ConcurrentHashMap<String, TraceMethod>()

        futures.add(
            executor.submit(
                ParseMappingTask(
                    mappingCollector,
                    collectedMethodMap,
                    methodId
                )
            )
        )
    }

    class ParseMappingTask(
        mappingCollector: MappingCollector,
        collectedMethodMap: ConcurrentHashMap<String, TraceMethod>,
        methodId: AtomicInteger
    ) : Runnable {

        val mappingCollector = mappingCollector
        val collectedMethodMap = collectedMethodMap
        val methodId = methodId




        override fun run() {
            val startTime = System.currentTimeMillis()
            val mappingFile = File("")
        }

    }

}


