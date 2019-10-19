package com.ssy.ferry

import android.app.Activity
import android.os.Bundle
import com.ssy.ferry.core.MethodMonitor
import com.ssy.ferry.mytrace.Ferry
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        MethodMonitor.i(0)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ferry = Ferry()
        ferry.start()

        var count = 1

        btn.setOnClickListener {
            MethodMonitor.i(count)
            Thread.sleep((6000).toLong())
            MethodMonitor.o(count)
            count++
        }

        MethodMonitor.o(0)

    }
}
