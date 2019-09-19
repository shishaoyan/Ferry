package com.ssy.ferry_task_list_permission

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.tasks.CheckManifest
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.ddmlib.Log
import com.google.auto.service.AutoService
import com.ssy.ferry.VariantProcessor
import java.io.File
import java.util.regex.Pattern


@AutoService(VariantProcessor::class)
class ListPermissionVariantProcessor : VariantProcessor {
    override fun process(variant: BaseVariant) {


        var baseVariantData =
            javaClass.getDeclaredMethod("getVariantData").invoke(this) as BaseVariantData
        var tasks = baseVariantData.scope.globalScope.project.tasks
        tasks.create(
            "list-${variant.name.capitalize()}-permmisons-tasl",
            ListPermissionTask::class.java
        ) {
            it.variant = variant
            it.outputs.upToDateWhen { false }
        }


    }


}