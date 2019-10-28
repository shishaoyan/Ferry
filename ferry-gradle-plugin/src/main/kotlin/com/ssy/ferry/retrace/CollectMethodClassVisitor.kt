package com.ssy.ferry.retrace

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter
import java.util.concurrent.atomic.AtomicInteger

/**
 * 2019-10-10
 * @author Mr.S
 */
class CollectMethodClassVisitor(api: Int, cv: ClassVisitor?) : ClassVisitor(api, cv) {
    lateinit var methodCollector: MethodCollector
    lateinit var className: String
    lateinit var methodId: AtomicInteger
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
            println("""------> ${traceMethod.id}""")
        }
        var mv = cv.visitMethod(access, name, desc, signature, exceptions)
        mv = object : AdviceAdapter(Opcodes.ASM5, mv, access, name, desc) {

            override fun onMethodEnter() {


                println("--------------   onMethodEnter:" + name)
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
                println("--------------   onMethodExit")
            }
        }


        return mv
    }
}