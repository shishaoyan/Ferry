package com.ssy.ferry

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.google.common.base.Joiner
import com.ssy.ferry.VariantProcessor
import com.ssy.ferry.retrace.Configuration
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import java.util.*

class FerryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        when {

            project.plugins.hasPlugin("com.android.application") -> project.getAndroid<AppExtension>().let { android ->


                project.afterEvaluate {
                    println("*********************************************")
                    println("********* --                    -- **********")
                    println("********* --       Ferry        -- **********")

                    android.applicationVariants.forEach { variant ->
                        if (variant.name == "debug") {
                            var baseVariantData =
                                (variant as ApplicationVariantImpl).variantData
                            Configuration.mappingOut = Joiner.on(File.separatorChar)
                                .join(
                                    baseVariantData.scope.globalScope.buildDir.toString(),
                                    "outputs",
                                    "mapping",
                                    baseVariantData.scope.variantConfiguration.dirName
                                )
                            Configuration.packageName = variant.applicationId
                            Configuration.methodMapFilePath =
                                Configuration.mappingOut + "/methodMapping.txt"
                            Configuration.traceClassOut = Joiner.on(File.separatorChar).join(
                                baseVariantData.scope.globalScope.buildDir.toString(),
                                "outputs",
                                "traceClassOut",
                                baseVariantData.scope.variantConfiguration.dirName
                            )
                            android.registerTransform(FerryAppTransform())
                        }
                    }
                    ServiceLoader.load(VariantProcessor::class.java, javaClass.classLoader).toList()
                        .let { processors ->
                            android.applicationVariants.forEach { variant ->


                                if (variant.name == "debug") {
                                    processors.forEach { processor ->


                                        processor.process(variant)
                                    }
                                }
                            }
                        }
                    println("********* --                    -- **********")
                    println("*********************************************")
                }
            }
            project.plugins.hasPlugin("com.android.library") -> project.getAndroid<LibraryExtension>().let { android ->
                android.registerTransform(FerryLibTransform())
                project.afterEvaluate {
                    println("FerryPlugin library:  hasPlugin")
                    ServiceLoader.load(VariantProcessor::class.java, javaClass.classLoader).toList()
                        .let { processors ->
                            android.libraryVariants.forEach { variant ->
                                processors.forEach { processor ->
                                    processor.process(variant)
                                }
                            }
                        }
                }
            }
        }
    }
}