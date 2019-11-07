package com.ssy.ferry

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.ssy.ferry.trace.Ferry
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        btn_next.setOnClickListener {
            val intent = Intent(this, SecondActivity::class.java)
            startActivity(intent)
        }
        btn.setOnClickListener {
            d()
        }


    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
    }
    fun a() {
        Thread.sleep((300).toLong())
    }

    fun b() {
        Thread.sleep((2000).toLong())
    }

    fun c() {
        Thread.sleep((4300).toLong())
    }

    fun d() {
        a();
        b();
        c();
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d("haha", "MainActivity onPause")

    }

    override fun onStop() {
        super.onStop()
        Log.d("haha", "MainActivity onStop")
    }
}
