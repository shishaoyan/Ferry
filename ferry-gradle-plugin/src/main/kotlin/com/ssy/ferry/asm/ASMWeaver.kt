package com.ssy.ferry.asm

import com.argusapm.gradle.internal.concurrent.ITask
import com.argusapm.gradle.internal.concurrent.ThreadPool
import com.argusapm.gradle.internal.utils.ZipFileUtils
import com.ssy.ferry.TimeClassVisitor
import com.ssy.ferry.utils.TypeUtil
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.*
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


class ASMWeaver {
    private val taskManager = ThreadPool()
    fun weaveClass(inputFile: File, outputFile: File) {

        if (TypeUtil.isMatchCondition(inputFile.absolutePath)) {
            println("---------  " + inputFile.absolutePath)

            taskManager.addTask(object : ITask {
                override fun call(): Any? {

                    FileUtils.touch(outputFile)
                    val inputStream = FileInputStream(inputFile)
                    val bytes = weaveSingleClassToByteArray(inputStream)
                    val fos = FileOutputStream(outputFile)
                    fos.write(bytes)
                    fos.close()
                    inputStream.close()
                    return null
                }
            })
        }
    }

    private fun weaveSingleClassToByteArray(inputStream: InputStream): ByteArray {
        val classReader = ClassReader(inputStream)
        val classWriter = ExtendClassWriter(ClassWriter.COMPUTE_MAXS)
        var classWriterWrapper: ClassVisitor = classWriter
        classWriterWrapper = TimeClassVisitor(Opcodes.ASM5, classWriterWrapper)

        classReader.accept(classWriterWrapper, ClassReader.EXPAND_FRAMES)
        return classWriter.toByteArray()
    }


    fun weaveJar(inputJar: File, outputJar: File) {
        taskManager.addTask(object : ITask {
            override fun call(): Any? {
                FileUtils.copyFile(inputJar, outputJar)

                weaveJarTask(inputJar, outputJar)

                return null
            }
        })
    }

    private fun weaveJarTask(input: File, output: File) {
        var zipOutputStream: ZipOutputStream? = null
        var zipFile: ZipFile? = null
        try {
            zipOutputStream =
                ZipOutputStream(BufferedOutputStream(Files.newOutputStream(output.toPath())))
            zipFile = ZipFile(input)
            val enumeration = zipFile.entries()
            while (enumeration.hasMoreElements()) {
                val zipEntry = enumeration.nextElement()
                val zipEntryName = zipEntry.name
                    val data = weaveSingleClassToByteArray(
                        BufferedInputStream(
                            zipFile.getInputStream(zipEntry)
                        )
                    )
                    val byteArrayInputStream = ByteArrayInputStream(data)
                    val newZipEntry = ZipEntry(zipEntryName)
                    ZipFileUtils.addZipEntry(zipOutputStream, newZipEntry, byteArrayInputStream)

            }
        } catch (e: Exception) {
        } finally {
            try {
                if (zipOutputStream != null) {
                    zipOutputStream.finish()
                    zipOutputStream.flush()
                    zipOutputStream.close()
                }
                zipFile?.close()
            } catch (e: Exception) {
                println("close stream err!")
            }
        }
    }

    fun start() {
        taskManager.startWork()
    }
}