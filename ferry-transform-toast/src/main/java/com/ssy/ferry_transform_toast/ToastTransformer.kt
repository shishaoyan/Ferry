package com.ssy.ferry_transform_toast

import com.google.auto.service.AutoService
import com.ssy.ferry.ClassTransformer
import com.ssy.ferry_transform_trace.ToastClassVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes


@AutoService(ClassTransformer::class)
class ToastTransformer : ClassTransformer {
    override fun transform(cw: ClassVisitor): ClassVisitor {
        return ToastClassVisitor(Opcodes.ASM6,cw)
    }

}