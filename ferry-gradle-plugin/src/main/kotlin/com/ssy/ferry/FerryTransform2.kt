package com.ssy.ferry

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.argusapm.gradle.internal.utils.eachFileRecurse
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ASM5
import java.io.File
import java.io.FileOutputStream
import java.util.*

abstract class FerryTransform2 : Transform() {


    override fun getName(): String {
        return "ferry"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun isIncremental(): Boolean {
        return false
    }

    final override fun transform(transformInvocation: TransformInvocation?) {


        println("********* --       transform     -- **********")
        val outputProvider = transformInvocation?.outputProvider

        transformInvocation?.inputs?.forEach { input ->
            input.directoryInputs.forEach { dirInput ->


                inject(dirInput.file.absolutePath)
                val dest = outputProvider?.getContentLocation(
                    dirInput.name,
                    dirInput.contentTypes,
                    dirInput.scopes,
                    Format.DIRECTORY
                )
                //  org.apache.commons.io.FileUtils.forceMkdir(dest)

                if (dest != null) {
                    println("==>" + dest.absolutePath)
                }
                FileUtils.copyDirectory(dirInput.file, dest)


            }


        }


    }

    private fun inject(path: String) {
        val dir = File(path)
        if (dir.isDirectory) {
            dir.eachFileRecurse { file ->
                val filePath = file.absolutePath
                if (filePath.endsWith(".class")
                    && !filePath.contains("R$")
                    && !filePath.contains("R.class")
                    && !filePath.contains("BuildConfig.class")
                    && !filePath.contains("TimeCache.class")
                ) {
                    val name = file.name

                    ServiceLoader.load(ClassTransformer::class.java, javaClass.classLoader)
                        .toList()
                        .forEach { classTransformer ->
                            val cr = ClassReader(file.readBytes())
                            val cw = ClassWriter(cr, ClassWriter.COMPUTE_FRAMES)

                            val classVisitor = classTransformer.transform(cw)
                            cr.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                            val fos = FileOutputStream(
                                file.parentFile.absolutePath + File.separator + name
                            )
                            println("（）（）（）（）（）（）（） fos" + file.parentFile.absolutePath + File.separator + name)
                            fos.write(cw.toByteArray())
                            fos.flush()
                        }



                }
            }
        }
    }

}