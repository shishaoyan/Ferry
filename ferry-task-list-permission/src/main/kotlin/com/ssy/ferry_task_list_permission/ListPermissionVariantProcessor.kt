package com.ssy.ferry_task_list_permission

import com.android.build.gradle.api.BaseVariant
import com.android.ddmlib.Log
import com.google.auto.service.AutoService
import com.ssy.ferry.VariantProcessor


@AutoService(VariantProcessor::class)
class ListPermissionVariantProcessor : VariantProcessor {
    override fun process(variant: BaseVariant) {
        Log.e("haha", "ListPermissionVariantProcessor --->")
        println("---------++++++++++++++++++++++++--------------------")
    }

}