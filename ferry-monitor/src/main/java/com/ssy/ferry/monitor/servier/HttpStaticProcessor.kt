package com.ssy.ferry.monitor.servier

import android.content.Context
import android.content.res.AssetManager
import android.text.TextUtils
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.ssy.ferry.monitor.util.IoUtil
import java.io.IOException
import java.io.InputStream

/**
 * 2019-09-24
 * 静态资源模块
 * @author Mr.S
 */
class HttpStaticProcessor {
    val mAsserts: AssetManager

    constructor(context: Context) {
        this.mAsserts = context.resources.assets
    }

    fun process(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse) {
        var path = request.path
        if (path.startsWith("/")) {
            path = path.substring(1)
        }
        if (path.isEmpty()) {
            path = "index.html"
        }
        val fileName = "android-godeye-dashboard/$path"
        val staticResource = StaticResource(parseMimeType(fileName), loadContent(fileName, mAsserts))
    }

    @Throws(IOException::class)
    private fun loadContent(fileName: String, assetManager: AssetManager): String {
        var input: InputStream? = null
        try {
            input = assetManager.open(fileName)
            return IoUtil.inputStreamToString(input)
        } finally {
            IoUtil.closeSilent(input)
        }
    }

    private fun parseMimeType(fileName: String): String? {
        return if (TextUtils.isEmpty(fileName)) {
            null
        } else if (fileName.endsWith(".html")) {
            "text/html;charset=utf-8"
        } else if (fileName.endsWith(".js")) {
            "application/javascript;charset=utf-8"
        } else if (fileName.endsWith(".css")) {
            "text/css;charset=utf-8"
        } else {
            "application/octet-stream"
        }
    }

    class StaticResource(var contentType: String?, var payload: String)
}