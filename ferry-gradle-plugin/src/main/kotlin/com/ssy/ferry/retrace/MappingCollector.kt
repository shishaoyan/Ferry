package com.ssy.ferry.trace.retrace

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

}