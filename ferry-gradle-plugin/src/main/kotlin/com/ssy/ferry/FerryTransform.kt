package com.ssy.ferry

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.argusapm.gradle.internal.utils.eachFileRecurse
import com.ssy.ferry.retrace.*
import com.ssy.ferry.trace.retrace.MappingCollector
import com.ssy.ferry.trace.retrace.MappingReader
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

abstract class FerryTransform : Transform() {
    private val TAG = "MatrixTraceTransform"

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
        parceMapping()
        transformInvocation?.inputs?.forEach { input ->
            input.directoryInputs.forEach { dirInput ->


                inject(dirInput.file.absolutePath)
                val dest = outputProvider?.getContentLocation(
                    dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY
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
                if (filePath.endsWith(".class") && !filePath.contains("R$") && !filePath.contains("R.class") && !filePath.contains(
                        "BuildConfig.class"
                    ) && !filePath.contains("TimeCache.class")
                ) {
                    val name = file.name


                    ServiceLoader.load(ClassTransformer::class.java, javaClass.classLoader).toList()
                        .forEach { classTransformer ->
                            val cr = ClassReader(file.readBytes())
                            val cw = ClassWriter(cr, ClassWriter.COMPUTE_FRAMES)

                            val classVisitor = classTransformer.transform(cw)
                            cr.accept(classVisitor, ClassReader.EXPAND_FRAMES)
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

    private fun parceMapping() {

        /**
         * step 1 解析 mapping文件
         */
        var startTime = System.currentTimeMillis()
        val futures = LinkedList<Future<*>>()
        val executor = Executors.newFixedThreadPool(16)

        val mappingCollector = MappingCollector()
        var methodId = AtomicInteger(0)
        val collectedMethodMap = ConcurrentHashMap<String, TraceMethod>()
        val dirInputOutMap = ConcurrentHashMap<File, File>()


        futures.add(
            executor.submit(
                ParseMappingTask(
                    mappingCollector, collectedMethodMap, methodId
                )
            )
        )

        for (future in futures) {
            future.get()
        }
        futures.clear()
        Log.i(
            TAG, "[parceMapping] Step(1)[Parse]... cost:%sms", System.currentTimeMillis() - startTime
        )

    }

    class ParseMappingTask(
        mappingCollector: MappingCollector,
        collectedMethodMap: ConcurrentHashMap<String, TraceMethod>,
        methodId: AtomicInteger
    ) : Runnable {

        val mappingCollector = mappingCollector
        val collectedMethodMap = collectedMethodMap
        val methodId = methodId


        override fun run() {
            val startTime = System.currentTimeMillis()
            val mappingFile = File(Configuration.mappingOut + "mapping.txt")
            if (mappingFile.exists() and mappingFile.isFile) {
                val mappingReader = MappingReader(mappingFile)
                mappingReader.read(mappingCollector)
                retraceMethodMap(mappingCollector, collectedMethodMap)


            }
        }

        private fun retraceMethodMap(
            processor: MappingCollector, methodMap: ConcurrentHashMap<String, TraceMethod>
        ) {
            if (null == processor || null == methodMap) {
                return
            }
            val retraceMethodMap = HashMap<String, TraceMethod>(methodMap.size)
            for (entry in methodMap.entries) {
                val traceMethod = entry.value
                traceMethod.proguard(processor)
                retraceMethodMap.put(traceMethod.methodName, traceMethod)
            }
            methodMap.clear()
            methodMap.putAll(retraceMethodMap)
            retraceMethodMap.clear()
        }

    }

}