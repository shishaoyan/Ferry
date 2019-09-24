package com.ssy.ferry.monitor.servier

import android.util.Log
import com.koushikdutta.async.http.WebSocket
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.koushikdutta.async.http.server.HttpServerRequestCallback
import com.ssy.ferry.monitor.util.L
import java.lang.Exception

/**
 * 2019-09-24
 * @author Mr.S
 */
class FerryMonitorServer : Messager {

    private var mPort: Int
    private val mServer: AsyncHttpServer
    private val mWebSockets: MutableList<WebSocket>
    private var mMonitorServerCallback: MonitorServerCallback? = null
    private val mLockForWebSockets = Any()

    constructor(mPort: Int) {
        this.mPort = mPort
        this.mServer = AsyncHttpServer()
        this.mWebSockets = ArrayList()
        mServer.websocket(
            "/refresh"
        ) { webSocket, request ->
            synchronized(mLockForWebSockets) {
                webSocket?.let { mWebSockets.add(it) }

                if (webSocket != null) {
                    mMonitorServerCallback?.onClientAdded(mWebSockets, webSocket)
                }
                L.d("connection build. current count:" + mWebSockets.size)

            }
            webSocket?.setClosedCallback { ex: Exception? ->
                synchronized(mLockForWebSockets) {
                    mWebSockets.remove(webSocket)
                    mMonitorServerCallback?.onClientRemoved(mWebSockets, webSocket)
                    L.d("connection released. current count:" + mWebSockets.size)
                }
            }
            webSocket?.setStringCallback { s ->
                mMonitorServerCallback?.onWebSocketRequest(webSocket, s)
            }
        }
        mServer.get(
            "/.*[(.html)|(.css)|(.js)|(.png)|(.jpg)|(.jpeg)|(.ico)]"
        ) { request, response ->
            mMonitorServerCallback?.onHttpRequest(request, response)
        }
    }

    override fun sendMessage(message: String) {
        val wss = mWebSockets.toList()
        if (wss != null) {
            for (webSocket in wss) {
                if (webSocket.isOpen) {
                    webSocket.send(message)
                }
            }
        }
    }

    /**
     * server 的消息回调
     */
    interface MonitorServerCallback {
        fun onClientAdded(webSockets: List<WebSocket>, added: WebSocket)

        fun onClientRemoved(webSockets: List<WebSocket>, removed: WebSocket)

        fun onHttpRequest(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse)

        fun onWebSocketRequest(webSocket: WebSocket, messageFromClient: String)
    }

    fun setMonitorServerCallback(monitorServerCallback: MonitorServerCallback) {
        mMonitorServerCallback = monitorServerCallback
    }

    fun start() {
        mServer.listen(mPort)
    }

    fun stop() {
        mServer.stop()
    }

}