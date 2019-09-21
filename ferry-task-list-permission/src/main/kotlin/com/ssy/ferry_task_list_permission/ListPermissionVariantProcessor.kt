package com.ssy.ferry_task_list_permission

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.google.auto.service.AutoService
import com.ssy.ferry.ListPermissionTask
import com.ssy.ferry.VariantProcessor
import org.gradle.api.Task


@AutoService(VariantProcessor::class)
class ListPermissionVariantProcessor : VariantProcessor {
    override fun process(variant: BaseVariant) {


        var baseVariantData = (variant as ApplicationVariantImpl).variantData
        var tasks = baseVariantData.scope.globalScope.project.tasks
        tasks.create(
            "${variant.name.capitalize()}-list-permmisons-task",
            ListPermissionTask::class.java
        ) {
            it.variant = variant
            it.outputs.upToDateWhen { false }
        }.also {
            var assemble =  tasks.findByName("clean") as Task
            assemble.dependsOn(it)
        }


    }


}