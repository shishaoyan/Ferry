package com.ssy.ferry

import com.google.auto.service.AutoService
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter


@AutoService(ClassTransformer::class)
class StringTransformer : ClassTransformer {
    override fun transform(cw: ClassWriter): ClassVisitor {
        return TimeClassVisitor(cw)
    }

}