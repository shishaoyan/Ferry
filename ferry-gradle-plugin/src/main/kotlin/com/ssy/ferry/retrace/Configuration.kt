package com.ssy.ferry.retrace

/**
 * 2019-10-09
 * @author Mr.S
 */
class Configuration {
    companion object {
        var mappingOut: String = ""
        var packageName: String = ""
        var methodMapFilePath: String = ""
        var traceClassOut: String = ""
        val DEFAULT_BLACK_TRACE = (
                "[package]\n"
                        + "-keeppackage android/\n"
                        + "-keeppackage com/ssy/ferry/\n")
    }
}