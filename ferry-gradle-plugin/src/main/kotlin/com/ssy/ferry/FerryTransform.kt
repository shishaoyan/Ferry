package com.ssy.ferry

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileOutputStream
import java.util.*

abstract class FerryTransform : Transform() {

    private val transformers =
        ServiceLoader.load(ClassTransformer::class.java, javaClass.classLoader).toList()


    override fun getName(): String {
        return "ferry"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun isIncremental(): Boolean {
        return true
    }

    final override fun transform(transformInvocation: TransformInvocation?) {


        println("********* --       transform     -- **********")
        val outputProvider = transformInvocation?.outputProvider

        transformInvocation?.inputs?.parallelStream()?.forEach { input ->
            input.directoryInputs.parallelStream().forEach { directoryInput ->

                val dir = File(directoryInput.file.absolutePath)
                if (dir.isDirectory) {
                    val fileTree = dir.walk()
                    fileTree.forEach { file ->
                        val filePath = file.absolutePath
                        if (filePath.endsWith(".class")
                            && !filePath.contains("R$")
                            && !filePath.contains("R.class")
                            && !filePath.contains("BuildConfig.class")
                        ) {

                            val cr = ClassReader(file.readBytes())
                            val cw = ClassWriter(cr, ClassWriter.COMPUTE_FRAMES)
                            ServiceLoader.load(ClassTransformer::class.java, javaClass.classLoader)
                                .toList()
                                .forEach { classTransformer ->
                                    val classVisitor = classTransformer.transform(cw)
                                    cr.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                                    println("********* --    forEach   classTransformer        -- **********     "+file.name)
                                   return@forEach
                                }


                            val fos = FileOutputStream(
                                file.parentFile.absolutePath + File.separator + name
                            )
                            fos.write(cw.toByteArray())
                            fos.flush()
                        }


                    }

                }

            }


        }


    }

}