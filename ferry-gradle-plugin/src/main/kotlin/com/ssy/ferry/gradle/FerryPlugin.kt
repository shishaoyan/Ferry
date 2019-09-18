package com.ssy.ferry.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.ssy.ferry.VariantProcessor
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.*

class FerryPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        when {

            project.plugins.hasPlugin("com.android.application") -> project.getAndroid<AppExtension>().let { android ->
               // android.registerTransform(FerryAppTransform())
                project.afterEvaluate {
                    ServiceLoader.load(VariantProcessor::class.java, javaClass.classLoader).toList()
                        .let { processors ->
                            android.applicationVariants.forEach { variant ->
                                processors.forEach { processor ->
                                    processor.process(variant)
                                }
                            }
                        }
                }
            }
            project.plugins.hasPlugin("com.android.library") -> project.getAndroid<LibraryExtension>().let { android ->
              //  android.registerTransform(FerryLibTransform())
                project.afterEvaluate {
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