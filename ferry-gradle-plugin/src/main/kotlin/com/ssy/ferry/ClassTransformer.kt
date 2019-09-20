package com.ssy.ferry

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter


interface ClassTransformer {

    fun transform(cw: ClassWriter): ClassVisitor



}