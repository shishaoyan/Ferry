package com.ssy.ferry

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class TimeClassVisitor(cw: ClassVisitor) : ClassVisitor(Opcodes.ASM6, cw) {
    internal lateinit var className: String


    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        className = name
    }

    override fun visitMethod(
        access: Int,
        name: String,
        desc: String,
        signature: String,
        exceptions: Array<String>
    ): MethodVisitor {
        var mv = cv.visitMethod(access, name, desc, signature, exceptions)
        return mv
        mv = object : AdviceAdapter(Opcodes.ASM5, mv, access, name, desc) {

            override fun onMethodEnter() {
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
            }
        }


        return mv
    }
}
