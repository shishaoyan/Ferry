package com.ssy.ferry

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.argusapm.gradle.internal.utils.eachFileRecurse
import com.argusapm.gradle.internal.utils.getUniqueJarName
import com.ssy.ferry.asm.ASMWeaver
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.FileOutputStream
import java.util.*

abstract class FerryTransform : Transform() {
    private lateinit var asmWeaver: ASMWeaver
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
        asmWeaver = ASMWeaver()

        if (transformInvocation != null) {
            if (!transformInvocation.isIncremental) {
                transformInvocation?.outputProvider.deleteAll()
            }
        }

        transformInvocation?.inputs?.forEach { input ->
            input.directoryInputs.forEach { dirInput ->
                val dest = transformInvocation.outputProvider.getContentLocation(
                    dirInput.name,
                    dirInput.contentTypes, dirInput.scopes,
                    Format.DIRECTORY
                )
                FileUtils.forceMkdir(dest)
                if (transformInvocation.isIncremental) {
                    val srcDirPath = dirInput.file.absolutePath
                    val destDirPath = dest.absolutePath
                    dirInput.changedFiles.forEach { (file, status) ->
                        val destFilePath = file.absolutePath.replace(srcDirPath, destDirPath)
                        val destFile = File(destFilePath)
                        when (status) {
                            Status.REMOVED -> {
                                FileUtils.deleteQuietly(destFile)
                            }
                            Status.CHANGED -> {
                                FileUtils.deleteQuietly(destFile)
                                asmWeaver.weaveClass(file, destFile)
                            }
                            Status.ADDED -> {
                                asmWeaver.weaveClass(file, destFile)
                            }
                            else -> {
                            }
                        }
                    }
                } else {
                    dirInput.file.eachFileRecurse { file ->


                        asmWeaver.weaveClass(
                            file,
                            File(
                                file.absolutePath.replace(
                                    dirInput.file.absolutePath,
                                    dest.absolutePath
                                )
                            )
                        )
                    }
                }

            }

            input.jarInputs.forEach { jarInput ->
                val dest = transformInvocation.outputProvider.getContentLocation(
                    jarInput.file.getUniqueJarName(),
                    jarInput.contentTypes,
                    jarInput.scopes,
                    Format.JAR
                )
                if (transformInvocation.isIncremental) {
                    val status = jarInput.status
                    when (status) {
                        Status.REMOVED -> {
                            FileUtils.deleteQuietly(dest)
                        }
                        Status.CHANGED -> {
                            FileUtils.deleteQuietly(dest)
                            asmWeaver.weaveJar(jarInput.file, dest)
                        }
                        Status.ADDED -> {
                            asmWeaver.weaveJar(jarInput.file, dest)
                        }
                        else -> {
                        }
                    }
                } else {
                    asmWeaver.weaveJar(jarInput.file, dest)
                }
            }


        asmWeaver.start()


    }


}

}