package com.ssy.ferry.trace.retrace

import jdk.internal.org.objectweb.asm.Type

/**
 * 2019-09-26
 * @author Mr.S
 */
class MappingCollector : MappingProcessor {
    val mRawObfuscatedPackageMap = HashMap<String, String>(DEFAULT_CAPACITY) //原始 混淆后 包
    val mRawObfuscatedClassMap = HashMap<String, String>(DEFAULT_CAPACITY) //原始 混淆后
    val mObfuscatedRawClassMap = HashMap<String, String>(DEFAULT_CAPACITY)//混淆后 原始

    private val mObfuscatedClassMethodMap = HashMap<String, Map<String, Set<MethodInfo>>>()
    private val mOriginalClassMethodMap = HashMap<String, Map<String, Set<MethodInfo>>>()

    companion object {
        private val TAG = "MappingCollector"
        private val DEFAULT_CAPACITY = 2000
    }

    /**
     * 把【原始和混淆后】、【混淆后和原始】的class 以及 包名 都一一对应
     */
    override fun processClassMapping(className: String, newClassName: String): Boolean {
        mRawObfuscatedClassMap.put(className, newClassName)
        mObfuscatedRawClassMap.put(newClassName, className)
        mRawObfuscatedPackageMap.put(
            className.substring(0, className.lastIndexOf(".")),
            className.substring(0, newClassName.lastIndexOf("."))
        )
        return true
    }

    /**
     * 对方法进行关系对应
     */
    override fun processMethodMapping(
        className: String,
        methodReturnTpye: String,
        methodName: String,
        methodArgement: String,
        newClassName: String,
        newMethodName: String
    ) {
        var obfuscatedMethodMap =
            mObfuscatedClassMethodMap.get(newClassName) as HashMap<String, Set<MethodInfo>>
        if (obfuscatedMethodMap == null) {
            obfuscatedMethodMap = HashMap<String, Set<MethodInfo>>()
            mObfuscatedClassMethodMap.put(newClassName, obfuscatedMethodMap)

        }
        var mObfuscatedMethodSet = obfuscatedMethodMap?.get(newMethodName) as HashSet<MethodInfo>
        if (mObfuscatedMethodSet == null) {
            mObfuscatedMethodSet = LinkedHashSet<MethodInfo>()
            obfuscatedMethodMap.put(newMethodName, mObfuscatedMethodSet)
        }
        mObfuscatedMethodSet.add(
            MethodInfo(
                newClassName,
                methodReturnTpye,
                newMethodName,
                methodArgement
            )
        )

        var originMethodMap =
            mOriginalClassMethodMap.get(className) as HashMap<String, Set<MethodInfo>>
        if (originMethodMap == null) {
            originMethodMap = HashMap<String, Set<MethodInfo>>()
            mOriginalClassMethodMap.put(className, originMethodMap)

        }
        var mOriginMethodSet = originMethodMap?.get(methodName) as HashSet<MethodInfo>
        if (mOriginMethodSet == null) {
            mOriginMethodSet = LinkedHashSet<MethodInfo>()
            originMethodMap.put(methodName, mOriginMethodSet)
        }
        mOriginMethodSet.add(
            MethodInfo(
                className,
                methodReturnTpye,
                methodName,
                methodArgement
            )
        )


    }

    override fun originalClassName(proguardClassName: String, defaultClassName: String): String? {
        return if (mObfuscatedRawClassMap.containsKey(proguardClassName)) {
            mObfuscatedRawClassMap[proguardClassName]
        } else {
            defaultClassName
        }
    }

    override fun proguardClassName(originalClassName: String, defaultClassName: String): String? {
        return if (mRawObfuscatedClassMap.containsKey(originalClassName)) {
            mRawObfuscatedClassMap[originalClassName]
        } else {
            defaultClassName
        }
    }

    /**
     * 获取 原始方法信息
     *
     */
    fun originalMethodInfo(
        obfuscatedClass: String,
        obfuscatedMethodName: String,
        obfuscatedMethodDesc: String
    ): MethodInfo {
        //解析原始方法的方法描述符
        val descInfo = parseMethodDesc(obfuscatedMethodDesc, false)
        val methodMap = mObfuscatedClassMethodMap.get(obfuscatedClass)
        if (methodMap != null) {
            val methodSet = methodMap.get(obfuscatedMethodName)
            methodSet?.let {
                val methodInfoIterator = methodSet.iterator()
                //可能会有重写 或者 重构的方法
                while (methodInfoIterator.hasNext()) {
                    val methodInfo = methodInfoIterator.next()
                    //如果参数 以及返回值 并没有被混淆 那么就直接替换desc
                    if (methodInfo.matches(descInfo.returnType, descInfo.arguments)) {
                        val newMethodInfo = MethodInfo(methodInfo)
                        newMethodInfo.desc = descInfo.desc.toString()
                        return newMethodInfo
                    }
                }
            }

        }
        val defaultMethodInfo = MethodInfo.deFault()
        defaultMethodInfo.desc = descInfo.desc.toString()
        defaultMethodInfo.originalName = obfuscatedMethodName
        return defaultMethodInfo

    }

    /**
     * 获取 混淆方法信息
     */
    fun obfuscatedMethodInfo(
        originalClassName: String,
        originalMethodName: String,
        originalMethodDesc: String
    ): MethodInfo {
        //通过mappimg 文件 把原始方法描述符 解析成混淆后的方法描述符
        val descInfo = parseMethodDesc(originalMethodDesc, true)

        // Class name -> obfuscated method names.
        val methodMap = mOriginalClassMethodMap.get(originalClassName)
        if (methodMap != null) {
            val methodSet = methodMap.get(originalMethodName)
            if (null != methodSet) {
                // Find all matching methods.
                val methodInfoIterator = methodSet.iterator()
                while (methodInfoIterator.hasNext()) {
                    val methodInfo = methodInfoIterator.next()
                    val newMethodInfo = MethodInfo(methodInfo)
                    obfuscatedMethodInfo(newMethodInfo)
                    if (newMethodInfo.matches(descInfo.returnType, descInfo.arguments)) {
                        newMethodInfo.desc = descInfo.desc.toString()
                        return newMethodInfo
                    }
                }
            }
        }
        val defaultMethodInfo = MethodInfo.deFault()
        defaultMethodInfo.desc = descInfo.desc.toString()
        defaultMethodInfo.originalName = originalMethodName
        return defaultMethodInfo
    }

    private fun obfuscatedMethodInfo(methodInfo: MethodInfo) {
        val methodArguments = methodInfo.originalArguments
        val args = methodArguments.split(",")
        val stringBuffer = StringBuffer()
        for (str in args) {
            val key = str.replace("[", "").replace("]", "")
            if (mRawObfuscatedClassMap.containsKey(key)) {
                stringBuffer.append(str.replace(key, mRawObfuscatedClassMap.get(key).toString()))
            } else {
                stringBuffer.append(str)
            }
            stringBuffer.append(',')
        }
        if (stringBuffer.length > 0) {
            stringBuffer.deleteCharAt(stringBuffer.length - 1)
        }
        var methodReturnType = methodInfo.originalType
        val key = methodReturnType.replace("[", "").replace("]", "")
        if (mRawObfuscatedClassMap.containsKey(key)) {
            methodReturnType = methodReturnType.replace(key, mRawObfuscatedClassMap[key].toString())
        }
        methodInfo.originalArguments = stringBuffer.toString()
        methodInfo.originalType = methodReturnType
    }


    /**
     * 解析方法描述符
     * 如果mapping里有该混淆方法那么就替换 如果没有那么就默认原来的 这可以作为有无mapping文件的处理
     */
    private fun parseMethodDesc(desc: String, isRawToObfuscated: Boolean): DescInfo {
        val descInfo = DescInfo()
        var argsObj = Type.getArgumentTypes(desc)
        val argumentsBuffer = StringBuffer()
        val descBuffer = StringBuffer()
        descBuffer.append("(")
        for (type in argsObj) {
            val key = type.className.replace("[", "").replace("]", "")
            if (isRawToObfuscated) {
                if (mRawObfuscatedClassMap.containsKey(key)) {
                    argumentsBuffer.append(
                        type.className.replace(
                            key,
                            mRawObfuscatedClassMap.get(key).toString()
                        )
                    )
                    descBuffer.append(
                        type.toString().replace(
                            key,
                            mRawObfuscatedClassMap.get(key).toString()
                        )
                    )
                } else {
                    argumentsBuffer.append(type.className)
                    descBuffer.append(type.toString())
                }
            } else {
                if (mObfuscatedRawClassMap.containsKey(key)) {
                    argumentsBuffer.append(
                        type.className.replace(
                            key,
                            mObfuscatedRawClassMap[key].toString()
                        )
                    )
                    descBuffer.append(type.toString().replace(key, mObfuscatedRawClassMap[key]!!))
                } else {
                    argumentsBuffer.append(type.className)
                    descBuffer.append(type.toString())
                }
            }
            argumentsBuffer.append(",")
        }
        descBuffer.append(")")

        var returnObj: Type
        try {
            returnObj = Type.getReturnType(desc)

        } catch (e: ArrayIndexOutOfBoundsException) {
            returnObj = Type.getReturnType(desc + ";")
        }

        if (isRawToObfuscated) {
            val key = returnObj.getClassName().replace("[", "").replace("]", "")

            if (mRawObfuscatedClassMap.containsKey(key)) {
                descInfo.setReturnType(
                    returnObj.getClassName().replace(
                        key,
                        mRawObfuscatedClassMap.get(key).toString()
                    )
                )
                descBuffer.append(
                    returnObj.toString().replace(
                        key,
                        mRawObfuscatedClassMap.get(key).toString()
                    )
                )
            } else {
                descInfo.setReturnType(returnObj.getClassName())
                descBuffer.append(returnObj.toString())
            }
        }

        //删除最后的,
        if (argumentsBuffer.length > 0) {
            argumentsBuffer.deleteCharAt(argumentsBuffer.length - 1)
        }
        descInfo.setArguments(argumentsBuffer.toString())
        descInfo.setDesc(descBuffer.toString())
        return descInfo


    }

    /**
     * about method desc info
     */
    private class DescInfo {
        internal var desc: String? = null
        internal var arguments: String? = null
        internal var returnType: String? = null

        fun setArguments(arguments: String) {
            this.arguments = arguments
        }

        fun setReturnType(returnType: String) {
            this.returnType = returnType
        }

        fun setDesc(desc: String) {
            this.desc = desc
        }
    }
}