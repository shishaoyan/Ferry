package com.ssy.ferry.trace.retrace

/**
 * 2019-09-26
 * @author Mr.S
 */
class MethodInfo {
    val originalClassName: String
    var originalType: String
    var originalArguments: String
    var originalName: String
    lateinit var desc: String

    constructor(
        originalClassName: String,
        originalType: String,
        originalArguments: String,
        originalName: String
    ) {
        this.originalClassName = originalClassName
        this.originalType = originalType
        this.originalArguments = originalArguments
        this.originalName = originalName
    }

    constructor(methodInfo: MethodInfo) {
        this.originalType = methodInfo.originalType
        this.originalArguments = methodInfo.originalArguments
        this.originalClassName = methodInfo.originalClassName
        this.originalName = methodInfo.originalName
        this.desc = methodInfo.desc
    }
}