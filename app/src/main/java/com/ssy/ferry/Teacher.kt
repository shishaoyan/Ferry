package com.ssy.ferry

import com.google.auto.service.AutoService

@AutoService(Person::class)
class Teacher : Person {
    override fun eat() {
       // LogUtil.log(" teacher eat")
    }

}