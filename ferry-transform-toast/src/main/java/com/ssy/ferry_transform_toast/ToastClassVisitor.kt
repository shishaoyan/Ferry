package com.ssy.ferry_transform_trace

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class ToastClassVisitor(verson: Int, cw: ClassVisitor) : ClassVisitor(verson, cw) {
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

    }

    override fun visitMethod(
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {

        var mv = cv.visitMethod(access, name, desc, signature, exceptions)
        mv = ToastMethodVisitor(mVerson, mv)
        return mv
    }
}
