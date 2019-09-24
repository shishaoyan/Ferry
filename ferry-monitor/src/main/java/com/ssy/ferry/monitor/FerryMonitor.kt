package com.ssy.ferry.monitor

import android.content.Context
import com.ssy.ferry.monitor.servier.FerryMonitorServer

/**
 * 2019-09-24
 * @author Mr.S
 */
class FerryMonitor {


    companion object {
        lateinit var mFerryMonitorServer: FerryMonitorServer
        var sIsWorking = false
        private val DEFAULT_PORT = 5390
        fun work(context: Context, port: Int) {
            if (sIsWorking) {
                return
            }
            mFerryMonitorServer = FerryMonitorServer(DEFAULT_PORT)

        }
    }


}