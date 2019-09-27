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
        private val SPACE = " "//类型和名称分割
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
                var line = reader.readLine()
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
        className: String,
        line: String?,
        mappingProcessor: MappingProcessor
    ) {
        line?.let {
            var className2 = className
            //左边：类型中最多两个：  名称中也是最多两个：
            val leftIndex1 = it.indexOf(SPLIP)
            val leftIndex2 = if (leftIndex1 < 0) -1 else it.indexOf(SPLIP, leftIndex1 + 1)
            val spaceIndex = it.indexOf(SPACE, leftIndex2 + 2)
            val argIndex1 = it.indexOf(LEFT_PUNC, spaceIndex + 1)
            val argIndex2 = if (argIndex1 < 0) -1 else it.indexOf(RIGHT_PUNC, argIndex1 + 1)
            val leftIndex3 = if (argIndex2 < 0) -1 else it.indexOf(SPLIP, argIndex2 + 1)
            val leftIndex4 = if (leftIndex3 < 0) -1 else it.indexOf(SPLIP, leftIndex3 + 1)

            val rightIndex = line.indexOf(
                ARROW, (if (leftIndex4 >= 0)
                    leftIndex4
                else if (leftIndex3 >= 0)
                    leftIndex3
                else if (argIndex2 >= 0) argIndex2 else spaceIndex) + 1
            )
            //如果没有空格 或者 没有 -> 那就返回
            if (spaceIndex < 0 || rightIndex < 0) {
                return
            }

            //开始筛选我们所需的
            val type = it.substring(leftIndex2 + 1, spaceIndex).trim()
            var name =
                it.substring(spaceIndex + 1, if (argIndex1 > 0) argIndex1 else rightIndex).trim()
            val newName = it.substring(rightIndex + 2).trim()

            val newClassName =
                className?.let { it1 -> mappingProcessor.proguardClassName(it1, it1) }

            val dotIndex = name.lastIndexOf(DOT)
            if (dotIndex >= 0) {
                className2 = name.substring(0, dotIndex)
                name = name.substring(dotIndex + 1)
            }

            if (type.isNotEmpty() && name.isNotEmpty() && newName.isNotEmpty() && argIndex2 >= 0) {
                val arguments = it.substring(argIndex1 + 1, argIndex2).trim()
                if (newClassName != null) {
                    mappingProcessor.processMethodMapping(
                        className,
                        type,
                        name,
                        arguments,
                        newClassName,
                        newName
                    )
                }
            }


        }
    }

}