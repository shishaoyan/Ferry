package com.ssy.ferry.retrace

import com.ssy.ferry.trace.retrace.MappingCollector
import com.ssy.ferry.trace.retrace.MethodInfo
import org.objectweb.asm.Opcodes

/**
 * 2019-09-27
 * @author Mr.S
 */
class TraceMethod {

    private val TAG = "Matrix.TraceMethod"

    var id: Int = 0
    var accessFlag: Int = 0
    lateinit var className: String
    lateinit var methodName: String
    lateinit var desc: String

    companion object {
        fun create(
            id: Int,
            accessFlag: Int,
            className: String,
            methodName: String,
            desc: String
        ): TraceMethod {
            val traceMethod = TraceMethod()
            traceMethod.id = id
            traceMethod.accessFlag = accessFlag
            traceMethod.className = className.replace("/", ".")
            traceMethod.methodName = methodName
            traceMethod.desc = desc.replace("/", ".")
            return traceMethod
        }

    }

    fun getTraceMethodName(): String {

        if (desc == null || isNativeMethod()) {
            return this.className + "." + this.methodName
        }
        return this.className + "." + this.methodName + "." + this.desc
    }

    /**
     * proguard -> original
     * 将混淆后的解析成原来的
     */
    fun revert(processor: MappingCollector) {
        if (null == processor) {
            return
        }

        val methodInfo = processor.originalMethodInfo(className, methodName, desc)
        this.methodName = methodInfo.originalName
        this.desc = methodInfo.desc
        this.className = methodInfo.originalClassName
    }

    private fun isNativeMethod(): Boolean {
        return accessFlag and Opcodes.ACC_NATIVE != 0
    }

    /**
     * original -> proguard
     *
     * @param processor
     */
    fun proguard(processor: MappingCollector?) {
        if (null == processor) {
            return
        }
        val methodInfo = processor.obfuscatedMethodInfo(className, methodName, desc)
        this.methodName = methodInfo.originalName
        this.desc = methodInfo.desc
        this.className = processor.proguardClassName(className, className).toString()
    }
}