package com.ssy.ferry

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.dependency.ArtifactCollectionWithExtraArtifact
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.tasks.CheckManifest
import com.android.build.gradle.internal.variant.BaseVariantData
import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier
import java.io.File
import java.util.regex.Pattern

internal open class ListPermissionTask : DefaultTask() {
    //ProcessApplicationManifest
    lateinit var variant: BaseVariant


    @TaskAction
    fun run() {
        println("*********************************************")
        println("********* -- ListPermissionTask -- **********")
        println("***** -- projectDir/permissions.json -- *****")
        println("*********************************************")

        val map = HashMap<String, List<String>>()
        val checkManifest = variant.checkManifestProvider.get() as CheckManifest
        if (checkManifest.manifest != null) {
            //获取 app module 的权限
            map.put("app module", matchPerssion(checkManifest.manifest.readText()))
        }


        val baseVariantData = (variant as ApplicationVariantImpl).variantData
        //获取 app aar 的权限
//
        val manifests = baseVariantData.scope.getArtifactCollection(
            AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH,
            AndroidArtifacts.ArtifactScope.ALL, AndroidArtifacts.ArtifactType.MANIFEST
        )

        val artifacts = manifests.artifacts

        for (artifact in artifacts) {

            if (artifact.file != null && !map.containsKey(getArtifactName(artifact)) && matchPerssion(
                    artifact.file.readText()
                ).isNotEmpty()
            ) {
                map.put(getArtifactName(artifact), matchPerssion(artifact.file.readText()))
            }
        }

        writePermmisonToFile(map)


    }


    fun getArtifactName(artifact: ResolvedArtifactResult): String {
        val id = artifact.id.componentIdentifier
        return if (id is ProjectComponentIdentifier) {
            id.projectPath

        } else if (id is ModuleComponentIdentifier) {
            id.group + ":" + id.module + ":" + id.version

        } else if (id is OpaqueComponentArtifactIdentifier) {
            // this is the case for local jars.
            // FIXME: use a non internal class.
            id.getDisplayName()
        } else if (id is ArtifactCollectionWithExtraArtifact.ExtraComponentIdentifier) {
            id.getDisplayName()
        } else {
            throw RuntimeException("Unsupported type of ComponentIdentifier")
        }
    }

    fun matchPerssion(text: String): List<String> {

        var list = ArrayList<String>()
        var pattern = Pattern.compile("<uses-permission.+.>")
        var matcher = pattern.matcher(text)
        while (matcher.find()) {
            list.add(matcher.group())
        }
        return list

    }

    fun writePermmisonToFile(map: HashMap<String, List<String>>) {
        var jsonFile = File("${project.parent?.projectDir}/permissions.json")
        if (jsonFile.exists()) {
            jsonFile.delete()
        }
        jsonFile.createNewFile()
        val json = JsonOutput.toJson(map)
        jsonFile.writeText(JsonOutput.prettyPrint(json), Charsets.UTF_8)
        println(JsonOutput.prettyPrint(json))

    }
}