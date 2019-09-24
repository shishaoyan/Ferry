package com.ssy.ferry.monitor.util

import java.io.*
import java.nio.charset.Charset

/**
 * 2019-09-24
 * @author Mr.S
 */
object IoUtil {

    fun closeSilent(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: Throwable) {
        }

    }

    @Throws(IOException::class)
    fun inputStreamToString(`is`: InputStream): String {
        val sb = StringBuilder()
        val br = BufferedReader(InputStreamReader(`is`, Charset.forName("UTF-8")))
        var line: String? = null
        line = br.readLine()
        while (line != null) {
            sb.append("\n")
            sb.append(line)
            line = br.readLine()
        }
        return sb.toString()
    }
}
