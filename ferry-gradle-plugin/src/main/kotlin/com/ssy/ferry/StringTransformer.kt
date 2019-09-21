package com.ssy.ferry

import com.google.auto.service.AutoService
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes


@AutoService(ClassTransformer::class)
class StringTransformer : ClassTransformer {
    override fun transform(cw: ClassVisitor): ClassVisitor {
        return TimeClassVisitor(Opcodes.ASM6,cw)
    }

}