package com.ssy.ferry

import com.google.auto.service.AutoService

@AutoService(Person::class)
class Student : Person {
    override fun eat() {
        LogUtil.log(" studnet eat ")
    }

}