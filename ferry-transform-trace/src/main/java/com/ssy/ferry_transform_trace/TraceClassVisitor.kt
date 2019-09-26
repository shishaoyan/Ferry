package com.ssy.ferry_transform_trace

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class TraceClassVisitor(verson: Int, cw: ClassVisitor) : ClassVisitor(verson, cw) {
    internal lateinit var className: String
    private var startTimeIndex = 0
    private var mVerson = verson

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


                println("--------------   onMethodEnter:" + name)
//                mv.visitLdcInsn(name)
//                mv.visitLdcInsn(className)
//                mv.visitLdcInsn(desc)
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/ssy/ferry/trace/AppMethodBeat",
                    "i",
                    "(I)V",
                    false
                )
            }

            override fun onMethodExit(opcode: Int) {
//                mv.visitLdcInsn(name)
//                mv.visitLdcInsn(className)
//                mv.visitLdcInsn(desc)
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/ssy/ferry/trace/AppMethodBeat",
                    "o",
                    "(I)V",
                    false
                )
                println("--------------   onMethodExit")
            }
        }


        return mv
    }
}
