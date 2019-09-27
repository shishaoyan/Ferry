package com.ssy.ferry.trace.retrace

/**
 * 2019-09-26
 * @author Mr.S
 */
interface MappingProcessor {
    fun processClassMapping(className: String, newClassName: String): Boolean
    fun processMethodMapping(
        className: String, methodReturnTpye: String, methodName: String, methodArgement: String,
        newClassName: String, newMethodName: String
    )

    fun proguardClassName(originalClassName: String, defaultClassName: String): String?
    fun originalClassName(proguardClassName: String, defaultClassName: String): String?
}