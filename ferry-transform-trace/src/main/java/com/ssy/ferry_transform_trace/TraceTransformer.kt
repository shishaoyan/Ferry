package com.ssy.ferry_transform_trace

import com.google.auto.service.AutoService
import com.ssy.ferry.ClassTransformer
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes


@AutoService(ClassTransformer::class)
class TraceTransformer : ClassTransformer {
    override fun transform(cw: ClassVisitor): ClassVisitor {
        return TraceClassVisitor(Opcodes.ASM6,cw)
    }

}