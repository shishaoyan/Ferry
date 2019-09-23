package com.ssy.ferry_transform_trace

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class TimeClassVisitor(verson:Int,cw: ClassVisitor) : ClassVisitor(verson, cw) {
    internal lateinit var className: String
    private var startTimeIndex = 0

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        className = name

    }

    override fun visitMethod(
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {


        var mv = cv.visitMethod(access, name, desc, signature, exceptions)

        mv = object : AdviceAdapter(Opcodes.ASM5, mv, access, name, desc) {

            override fun onMethodEnter() {


                println("--------------   onMethodEnter:"+name)
                mv.visitLdcInsn(name)
                mv.visitLdcInsn(className)
                mv.visitLdcInsn(desc)
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/ssy/ferry/TimeCache",
                    "i",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    false
                )
            }

            override fun onMethodExit(opcode: Int) {
                mv.visitLdcInsn(name)
                mv.visitLdcInsn(className)
                mv.visitLdcInsn(desc)
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/ssy/ferry/TimeCache",
                    "o",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                    false
                )
                println("--------------   onMethodExit")
            }
        }


        return mv
    }
}
