package com.ssy.ferry.trace.retrace

/**
 * 2019-09-26
 * @author Mr.S
 */
class MappingCollector : MappingProcessor {
    val mRawObfuscatedPackageMap = HashMap<String, String>(DEFAULT_CAPACITY) //原始 混淆后 包
    val mRawObfuscatedClassMap = HashMap<String, String>(DEFAULT_CAPACITY) //原始 混淆后
    val mObfuscatedRawClassMap = HashMap<String, String>(DEFAULT_CAPACITY)//混淆后 原始


    companion object {
        private val TAG = "MappingCollector"
        private val DEFAULT_CAPACITY = 2000
    }


    override fun processClassMapping(className: String, newClassName: String): Boolean {


        return false;
    }

    override fun processMethodMapping(
        className: String,
        methodReturnTpye: String,
        methodName: String,
        methodArgement: String,
        newClassName: String,
        newMethodName: String
    ) {
    }

}