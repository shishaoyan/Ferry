package com.ssy.ferry

import android.app.Activity
import android.os.Bundle
import com.ssy.ferry.trace.Ferry
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val ferry = Ferry()
        ferry.start()

        btn.setOnClickListener {
            Thread.sleep((6000).toLong())
        }


    }
}
