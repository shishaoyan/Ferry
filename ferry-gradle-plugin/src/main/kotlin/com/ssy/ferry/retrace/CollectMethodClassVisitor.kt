package com.ssy.ferry.retrace

import com.ssy.ferry.TraceBuildConstants
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 2019-10-10
 * @author Mr.S
 */
class CollectMethodClassVisitor(api: Int, cv: ClassVisitor?) : ClassVisitor(api, cv) {
    lateinit var methodCollector: MethodCollector
    lateinit var className: String
    lateinit var methodId: AtomicInteger
    var hasWindowFocusMethod = false
    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        if (name != null) {
            className = name
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        desc: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        var traceMethod = TraceMethod()
        if (desc != null && name != null) {

            traceMethod.className = className
            traceMethod.desc = desc
            traceMethod.methodName = name
            traceMethod.accessFlag = access
            traceMethod.id = methodId.get()
            methodId.incrementAndGet()
            methodCollector.push("$traceMethod.id", traceMethod)
            // println("------> $className.$name" + traceMethod.toString())

        }
        var mv = cv.visitMethod(access, name, desc, signature, exceptions)
        mv = object : AdviceAdapter(Opcodes.ASM5, mv, access, name, desc) {

            override fun onMethodEnter() {


                mv.visitIntInsn(Opcodes.SIPUSH, traceMethod.id)
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC, "com/ssy/ferry/core/MethodMonitor", "i", "(I)V", false
                )
            }

            override fun onMethodExit(opcode: Int) {
                mv.visitIntInsn(Opcodes.SIPUSH, traceMethod.id)
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC, "com/ssy/ferry/core/MethodMonitor", "o", "(I)V", false
                )
                hasWindowFocusMethod = isWindowFocusChangeMethod(name, desc)
                if (hasWindowFocusMethod) {
                    println("---------hasWindowFocusMethod ------")
                    traceWindowFocusChangeMethod(mv, className)
                }else{
                    println("+++++++++hasWindowFocusMethod ------")
                }
            }
        }


        return mv
    }

    override fun visitEnd() {
        super.visitEnd()
        if (!hasWindowFocusMethod && isActivityOrSubClass(className)) {

            insertWindowFocusChangeMethod(cv, className)
        }
    }

    private fun insertWindowFocusChangeMethod(cv: ClassVisitor, classname: String) {
        val methodVisitor = cv.visitMethod(
            Opcodes.ACC_PUBLIC,
            TraceBuildConstants.FERRY_TRACE_ON_WINDOW_FOCUS_METHOD,
            TraceBuildConstants.FERRY_TRACE_ON_WINDOW_FOCUS_METHOD_ARGS,
            null,
            null
        )
        methodVisitor.visitCode()
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)
        methodVisitor.visitVarInsn(Opcodes.ILOAD, 1)
        methodVisitor.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            TraceBuildConstants.FERRY_TRACE_ACTIVITY_CLASS,
            TraceBuildConstants.FERRY_TRACE_ON_WINDOW_FOCUS_METHOD,
            TraceBuildConstants.FERRY_TRACE_ON_WINDOW_FOCUS_METHOD_ARGS,
            false
        )
        traceWindowFocusChangeMethod(methodVisitor, classname)
        methodVisitor.visitInsn(Opcodes.RETURN)
        methodVisitor.visitMaxs(2, 2)
        methodVisitor.visitEnd()

    }

    private fun traceWindowFocusChangeMethod(mv: MethodVisitor, classname: String) {
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitVarInsn(Opcodes.ILOAD, 1)
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            TraceBuildConstants.FERRY_TRACE_CLASS,
            "at",
            "(Landroid/app/Activity;Z)V",
            false
        )
    }

    private fun isActivityOrSubClass(
        className: String?
    ): Boolean {
        var className = className
        className = className!!.replace(".", "/")
        val isActivity =
            className == TraceBuildConstants.FERRY_TRACE_ACTIVITY_CLASS || className == TraceBuildConstants.FERRY_TRACE_V7_ACTIVITY_CLASS
        return isActivity
    }

    fun isWindowFocusChangeMethod(name: String?, desc: String?): Boolean {
        return null != name && null != desc && name == TraceBuildConstants.FERRY_TRACE_ON_WINDOW_FOCUS_METHOD && desc == TraceBuildConstants.FERRY_TRACE_ON_WINDOW_FOCUS_METHOD_ARGS
    }
}