package com.christophecvb.touchportal.packager

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class TouchPortalPluginPackager implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def extension = project.extensions.create('tpPlugin', TouchPortalPluginPackagerExtension)

        project.tasks.withType(JavaCompile).configureEach { task ->
            task.doFirst {
                println('Task ' + task.name + ': Adding -parameters to Compiler Args and setting encoding to UTF-8')
                options.compilerArgs.add('-parameters')
                options.encoding = "UTF-8"
            }
        }

        project.tasks.withType(KotlinJvmCompile).configureEach { task ->
            println('Note: Task ' + task.name + ' - set javaParameters to true')
            compilerOptions.javaParameters.set(true)
        }

        project.tasks.withType(Jar).configureEach { task ->
            task.dependsOn project.configurations.runtimeClasspath

            task.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

            task.doFirst {
                manifest {
                    attributes 'Implementation-Title': "${extension.mainClassSimpleName.get()}",
                            'Implementation-Version': "${project.version}",
                            'Main-Class': "${project.group}.${extension.mainClassSimpleName.get()}"
                }

                from {
                    project.configurations.runtimeClasspath.findAll { it.name.endsWith('jar') }.collect { project.zipTree(it) }
                }
            }
        }

        def copyResources = project.tasks.register('copyResources', Copy) {
            dependsOn project.processResources

            group = 'Touch Portal Plugin'
            from(project.file("${project.buildDir}/resources/main/"))
            into("${project.buildDir}/plugin/${extension.mainClassSimpleName.get()}/")
            setDuplicatesStrategy(DuplicatesStrategy.WARN)

            doLast {
                println 'Resources Copied into plugin directory'
            }
        }

        def copyJar = project.tasks.register('copyJar', Copy) {
            group = 'Touch Portal Plugin'
            dependsOn project.jar
            from(project.file("${project.buildDir}/libs/"))
            into("${project.buildDir}/plugin/${extension.mainClassSimpleName.get()}/")
            rename {
                "${extension.mainClassSimpleName.get()}.jar"
            }
            setDuplicatesStrategy(DuplicatesStrategy.WARN)

            doLast {
                println 'Jar Copied into plugin directory'
            }
        }

        def copyGeneratedJavaResources = project.tasks.register('copyGeneratedJavaResources', Copy) {
            group = 'Touch Portal Plugin'
            dependsOn copyJar
            from(project.file("${project.buildDir}/generated/sources/annotationProcessor/java/main/resources/"))
            into("${project.buildDir}/plugin/${extension.mainClassSimpleName.get()}/")

            doLast {
                println 'Generated Java Resources Copied into plugin directory'
            }
        }

        def copyGeneratedKotlinResources = project.tasks.register('copyGeneratedKotlinResources', Copy) {
            group = 'Touch Portal Plugin'
            dependsOn copyJar
            from(project.file("${project.buildDir}/generated/source/kapt/main/resources/"))
            into("${project.buildDir}/plugin/${extension.mainClassSimpleName.get()}/")

            doLast {
                println 'Generated Kotlin Resources Copied into plugin directory'
            }
        }

        def packagePlugin = project.tasks.register('packagePlugin', Zip) {
            group = 'Touch Portal Plugin'
            description = 'Package the Project into a TPP'
            dependsOn copyResources, copyGeneratedJavaResources, copyGeneratedKotlinResources

            archiveFileName = "${extension.mainClassSimpleName.get()}.tpp"
            destinationDirectory = project.file("${project.buildDir}/plugin")
            from "${project.buildDir}/plugin/"
            exclude "*.tpp"
            includeEmptyDirs = false

            doLast {
                println 'Plugin Packaged'
            }
        }
    }
}

abstract class TouchPortalPluginPackagerExtension {
    abstract Property<String> getMainClassSimpleName()

    TouchPortalPluginPackagerExtension() {
        mainClassSimpleName.convention('TouchPortalPlugin')
    }
}