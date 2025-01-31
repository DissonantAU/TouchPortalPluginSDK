package com.christophecvb.touchportal.packager

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class TouchPortalPluginPackager implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def extension = project.extensions.create('tpPlugin', TouchPortalPluginPackagerExtension)

        final def currentJavaVer = JavaVersion.current()

        project.tasks.withType(JavaCompile).configureEach { task ->
            if (!JavaVersion.current().java8) {
                // If JDK newer than 8 - set 'release'
                println('Task ' + task.name + ': Build Target Version Provider passed to Java Release Option')
                options.release.set(extension.targetJvmVersion)
            }

            task.doFirst {
                println('Task ' + task.name + ': Adding -parameters to Compiler Args and setting encoding to UTF-8')
                options.compilerArgs.add('-parameters')
                options.encoding = "UTF-8"
            }

            task.doLast {
                // Make sure the JDK is Compatible with the Target Version
                JavaVersion javaTargetVer = JavaVersion.toVersion(extension.targetJvmVersion.get())
                if (!currentJavaVer.isCompatibleWith(javaTargetVer)) {
                    throw new GradleException("The JDK version ${JavaVersion.current()} is not compatible with JDK Version ${javaTargetVer}.")
                }

                if (!currentJavaVer.java8) {
                    // JDK newer than 8 - check 'release'
                    println('Note: Task ' + task.name + ' - JRE Release is set to ' + options.release.get())
                }
            }
        }


        project.tasks.withType(KotlinJvmCompile).configureEach { task ->
            // Make sure JDK is at least Version 8 / Version 8 Compatible
            if (!currentJavaVer.isCompatibleWith(JavaVersion.VERSION_1_8)) {
                throw new Exception("Current JRE/JDK (" + currentJavaVer.toString() + ") is not Compatible with Java 8")
            }

            final Provider<JvmTarget> targetJreVersionKotlin = project.providers.provider {
                JavaVersion javaTargetVer = JavaVersion.toVersion(extension.targetJvmVersion.get())
                javaTargetVer.isJava8() ? JvmTarget.JVM_1_8 : JvmTarget.valueOf("JVM_${javaTargetVer}")
            }

            final Provider<List<String>> targetJreVersionKotlinRelease = project.providers.provider {
                JavaVersion javaTargetVer = JavaVersion.toVersion(extension.targetJvmVersion.get())
                ["-Xjdk-release=" + javaTargetVer.majorVersion].toList()
            }


            println('Note: Task ' + task.name + ' - set javaParameters to true')
            compilerOptions.javaParameters.set(true)

            if (!currentJavaVer.java8) {
                println('Task ' + task.name + ': Prepared JVM Target/JDK Release')
                compilerOptions.jvmTarget.set(targetJreVersionKotlin)
                compilerOptions.freeCompilerArgs.set(targetJreVersionKotlinRelease)
            }

            task.doLast {
                // Make sure the JDK is Compatible with the Target Version
                JavaVersion javaTargetVer = JavaVersion.toVersion(extension.targetJvmVersion.get())
                if (!currentJavaVer.isCompatibleWith(javaTargetVer)) {
                    throw new GradleException("The current JDK version ${JavaVersion.current()} is not compatible with ${javaTargetVer}.")
                }

                if (!currentJavaVer.java8) {
                    println('Task ' + task.name + ': Kotlin JVM Target is set to ' + compilerOptions.jvmTarget.get())
                }
            }
        }


        project.tasks.withType(Jar).configureEach { task ->
            task.dependsOn project.configurations.runtimeClasspath

            task.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

            task.doFirst {
                manifest {
                    attributes 'Implementation-Title': "${extension.mainClassSimpleName.get()}",
                            'Implementation-Version': "${project.version}",
                            'Main-Class': "${project.group}.${extension.mainClassSimpleName.get()}",
                            'Build-Jdk-Spec': JavaVersion.current().majorVersion,
                            'Target-Jre-Spec': extension.targetJvmVersion.get()
                }

                from {
                    project.configurations.runtimeClasspath.findAll { it.name.endsWith('jar') }.collect { project.zipTree(it) }
                }
            }
        }

        def copyResources = project.tasks.register('copyResources', Copy) {
            group = 'Touch Portal Plugin'
            dependsOn project.processResources
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
    /**
     * Java Version to be targeted during Build
     *
     * This sets the JRE version the project to be compiled to.
     *
     * Can be supplied a Provider to allow for setting or
     * changing Version at build using meta-tasks
     *
     * Version 8 by default
     *
     */
    abstract Property<Integer> getTargetJvmVersion()

    TouchPortalPluginPackagerExtension() {
        mainClassSimpleName.convention('TouchPortalPlugin')
        targetJvmVersion.convention(8)
    }
}