package com.ssy.ferry.trace.retrace

import java.io.*

/**
 * 2019-09-26
 * @author Mr.S
 */
/**
com.android.installreferrer.BuildConfig -> com.android.installreferrer.BuildConfig:
boolean DEBUG -> DEBUG
java.lang.String APPLICATION_ID -> APPLICATION_ID
java.lang.String BUILD_TYPE -> BUILD_TYPE
java.lang.String FLAVOR -> FLAVOR
int VERSION_CODE -> VERSION_CODE
java.lang.String VERSION_NAME -> VERSION_NAME
void <init>() -> <init>
 */
class MappingReader {
    private val proguardMappingFile: File

    constructor(proguardMappingFile: File) {
        this.proguardMappingFile = proguardMappingFile
    }

    companion object {
        private val TAG = "MappingReader"
        private val SPLIP = ":"//这个是类和方法的分界
        private val SPACE = " "
        private val ARROW = "->" //新旧名称分割
        private val LEFT_PUNC = "("
        private val RIGHT_PUNC = ")"
        private val DOT = "."
    }

    fun read(mappingProcessor: MappingProcessor) {
        val reader = LineNumberReader(BufferedReader(FileReader(proguardMappingFile) as Reader?))
        try {
            var className: String? = null
            while (true) {
                val line = reader.readLine()
                if (!line.startsWith("#")) {
                    if (line.endsWith(SPLIP)) {//如果是：这里可以pick出 className
                        className = parceClassMapping(line, mappingProcessor)
                    } else if (className != null) {//如果不是 那就去解析里面的方法
                        parceClassMapping(className, line, mappingProcessor)
                    }
                }

            }
        } catch (e: IOException) {
            throw IOException("Can't read mapping file", e)
        }


    }

    private fun parceClassMapping(line: String?, mappingProcessor: MappingProcessor): String? {
        var leftInndex = line?.indexOf(ARROW) as Int
        val className = line?.substring(0, leftInndex).trim()
        var offset = 2
        var rightInndex = line?.indexOf(SPLIP, leftInndex + offset)
        if (rightInndex < 0) {
            return null
        }
        val newClassName = line?.substring(leftInndex + offset, rightInndex).trim()

        var ret = mappingProcessor.processClassMapping(className, newClassName)
        return if (ret) className else null


    }

    /** 解析 class 里面的 方法
     *                         ___ ___ -> ___
     *                         ___:___:___ ___(___) -> ___
     *                         ___:___:___ ___(___):___ -> ___
     *                         ___:___:___ ___(___):___:___ -> ___
     *
     */
    private fun parceClassMapping(
        className: String?,
        line: String?,
        mappingProcessor: MappingProcessor
    ): String? {
        val leftIndex1 = line.indexOf(SPLIP)

        return null
    }

}