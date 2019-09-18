package com.ssy.ferry

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        LogUtil.log("main")
        ServiceLoader.load(Person::class.java, javaClass.classLoader).forEach { person ->
            person.eat()
        }

    }
}
