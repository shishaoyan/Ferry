package com.ssy.ferry

import com.android.build.api.transform.*
import com.didiglobal.booster.gradle.*
import org.gradle.api.Project
import org.gradle.api.internal.AbstractTask
import java.io.File
import java.util.concurrent.ExecutorService

class FerryTransformInvocation(private val delegate: TransformInvocation)  : TransformInvocation, TransformContext {

    val TransformInvocation.project: Project
        get() = (this.context as AbstractTask).project

    override val name: String = delegate.context.variantName

    override val projectDir: File = delegate.project.projectDir

    override val buildDir: File = delegate.project.buildDir

    override val temporaryDir: File = delegate.context.temporaryDir

    override val reportsDir: File = File(buildDir, "reports").also { it.mkdirs() }

    override val bootClasspath = delegate.bootClasspath

    override val compileClasspath = delegate.compileClasspath

    override val runtimeClasspath = delegate.runtimeClasspath



    override val applicationId = delegate.applicationId

    override val originalApplicationId = delegate.originalApplicationId

    override val isDebuggable = delegate.variant.buildType.isDebuggable

    override fun hasProperty(name: String): Boolean {
        return project.hasProperty(name)
    }

    override fun getProperty(name: String): String? {
        return project.properties[name]?.toString()
    }

    override fun getInputs(): MutableCollection<TransformInput> = delegate.inputs

    override fun getSecondaryInputs(): MutableCollection<SecondaryInput> = delegate.secondaryInputs

    override fun getReferencedInputs(): MutableCollection<TransformInput> = delegate.referencedInputs

    override fun isIncremental() = delegate.isIncremental

    override fun getOutputProvider(): TransformOutputProvider = delegate.outputProvider

    override fun getContext(): Context = delegate.context


//    internal fun doFullTransform() {
//        this.inputs.parallelStream().forEach { input ->
//            input.directoryInputs.parallelStream().forEach {
//                project.logger.info("Transforming ${it.file}")
//                it.file.transform(outputProvider.getContentLocation(it.name, it.contentTypes, it.scopes, Format.DIRECTORY)) { bytecode ->
//                    bytecode.transform(this)
//                }
//            }
//            input.jarInputs.parallelStream().forEach {
//                project.logger.info("Transforming ${it.file}")
//                it.file.transform(outputProvider.getContentLocation(it.name, it.contentTypes, it.scopes, Format.JAR)) { bytecode ->
//                    bytecode.transform(this)
//                }
//            }
//        }
//    }


}