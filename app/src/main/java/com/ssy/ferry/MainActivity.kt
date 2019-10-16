package com.ssy.ferry

import android.app.Activity
import android.os.Bundle
import com.ssy.ferry.mytrace.Ferry
import java.util.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ferry = Ferry()
        ferry.start()

    }
}
