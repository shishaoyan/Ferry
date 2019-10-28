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
            d();
        }


    }

    fun a() {
        Thread.sleep((300).toLong())
    }

    fun b() {
        Thread.sleep((2000).toLong())
    }

    fun c() {
        Thread.sleep((3000).toLong())
    }

    fun d() {
        a();
        b();
        c();
    }
}
