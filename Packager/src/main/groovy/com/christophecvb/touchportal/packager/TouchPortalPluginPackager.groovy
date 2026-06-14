package com.christophecvb.touchportal.packager

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

class TouchPortalPluginPackager implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def extension = project.extensions.create('tpPlugin', TouchPortalPluginPackagerExtension)

        /** Base Directory used by Packager, prepack folder and Final Packaged Plugin (.tpp) goes here */
        final Provider<Directory> pluginBuildDir = project.getLayout().getBuildDirectory().dir("plugin")
        /** Plugin Package Staging Directory - Everything in here is zipped into Final Packaged Plugin (.tpp), so only one folder containing the plugin data (pluginMainDir) should be in here */
        final Provider<Directory> pluginStagingDir = project.getLayout().getBuildDirectory().dir("plugin/staging")
        /** Directory that holds jar, entry.tp, and any other resources for Plugin - will be zipped into final packaged plugin (.tpp) and unzipped to the Touch Portal Plugin folder during install */
        final Provider<Directory> pluginMainDir = project.getLayout().getBuildDirectory().dir(project.providers.provider { "plugin/staging/${extension.mainClassSimpleName.get()}" })

        project.tasks.withType(JavaCompile).configureEach { task ->
            task.doFirst {
                logger.info('Task ' + task.name + ': Adding -parameters to Compiler Args and setting encoding to UTF-8')
                task.options.compilerArgs.add('-parameters')
                task.options.encoding = "UTF-8"
            }
        }

        project.tasks.withType(KotlinJvmCompile).configureEach { task ->
            logger.info("Note: Task $task.name - set javaParameters to true")
            task.compilerOptions.javaParameters.set(true)
        }

        project.tasks.withType(Javadoc).configureEach { task ->
            task.doFirst {
                if (JavaVersion.current().isJava9Compatible()) {
                    (task.options as StandardJavadocDocletOptions).addStringOption("-release", extension.targetJvmVersion.get() as String)
                }
            }
        }

        project.tasks.withType(Jar).configureEach { task ->
            task.dependsOn project.configurations.runtimeClasspath

            task.duplicatesStrategy = DuplicatesStrategy.EXCLUDE

            task.doFirst {
                logger.info("Adding JAR Manifest")
                manifest {
                    attributes 'Implementation-Title': "${extension.mainClassSimpleName.get()}",
                            'Implementation-Version': "${project.version}",
                            'Main-Class': "${project.group}.${extension.mainClassSimpleName.get()}"
                }

                logger.info("Adding JARs from Runtime Classpath to create Fat JAR")

                from {
                    project.configurations.runtimeClasspath.findAll {
                        it.name.endsWith('jar')
                    }.collect {
                        logger.debug("Adding ${it} to JAR")
                        project.zipTree(it)
                    }
                }
            }

        }

        def copyResources = project.tasks.register('copyResources', Copy) { task ->
            task.group = 'Touch Portal Plugin'

            task.from(project.processResources)
            task.into(pluginMainDir)

            if (!logger.infoEnabled)
                task.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
            else {
                task.setDuplicatesStrategy(DuplicatesStrategy.WARN)

                task.doFirst {
                    logger.info("Destination: ${pluginMainDir.get()}")

                    if (logger.debugEnabled) {
                        task.getInputs().files.asFileTree.each {
                            logger.debug("Copying File: ${it}")
                        }
                    }
                }
            }

            task.doLast {
                logger.lifecycle('Resources Copied into plugin directory')

                if (logger.debugEnabled) {
                    task.getOutputs().files.asFileTree.each {
                        logger.debug("Copied file: ${it}")
                    }
                }
            }

        }

        def copyJar = project.tasks.register('copyJar', Copy) { task ->
            task.group = 'Touch Portal Plugin'

            task.from(project.jar)
            task.into(pluginMainDir)

            task.rename { "${extension.mainClassSimpleName.get()}.jar" }

            if (!logger.infoEnabled)
                task.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
            else {
                task.setDuplicatesStrategy(DuplicatesStrategy.WARN)

                task.doFirst {
                    logger.info("Destination: ${pluginMainDir.get()}")

                    if (logger.debugEnabled) {
                        task.getInputs().files.asFileTree.each {
                            logger.debug("Copying File: ${it}")
                        }
                    }
                }
            }

            task.doLast {
                logger.lifecycle("JAR Copied into plugin directory")

                if (logger.debugEnabled) {
                    task.getOutputs().files.asFileTree.each {
                        logger.debug("Copied file: ${it}")
                    }
                }
            }

        }

        def copyGeneratedJavaResources = project.tasks.register('copyGeneratedJavaResources', Copy) { task ->
            task.group = 'Touch Portal Plugin'
            task.dependsOn(project.jar)

            var source = project.getLayout().getBuildDirectory().dir("generated/sources/annotationProcessor/java/main/resources")

            task.from(source)
            task.into(pluginMainDir)

            if (logger.infoEnabled) {
                task.doFirst {
                    logger.info("Source:      ${source.get()}")
                    logger.info("Destination: ${pluginMainDir.get()}")

                    if (logger.debugEnabled) {
                        source.get().asFileTree.files.each {
                            logger.info("copyGeneratedJavaResources: ${it}")
                        }
                    }
                }
            }

            task.doLast {
                logger.lifecycle('Generated Java Resources Copied into plugin directory')

                if (logger.debugEnabled) {
                    task.getOutputs().files.asFileTree.each {
                        logger.debug("Copied file: ${it}")
                    }
                }
            }

        }

        def copyGeneratedKotlinResources = project.tasks.register('copyGeneratedKotlinResources', Copy) { task ->
            task.group = 'Touch Portal Plugin'
            task.dependsOn(project.jar)

            var source = project.getLayout().getBuildDirectory().dir("generated/source/kapt/main/resources")

            task.from(source)
            task.into(pluginMainDir)

            if (logger.infoEnabled) {
                task.doFirst {
                    logger.info("Source:      ${source.get()}")
                    logger.info("Destination: ${pluginMainDir.get()}")

                    if (logger.debugEnabled) {
                        source.get().asFileTree.files.each {
                            logger.debug("Copying: ${it}")
                        }
                    }
                }
            }

            task.doLast {
                logger.lifecycle('Generated Kotlin Resources Copied into plugin directory')

                if (logger.debugEnabled) {
                    task.getOutputs().files.asFileTree.each {
                        logger.debug("Copied file: ${it}")
                    }
                }
            }

        }

        def packagePlugin = project.tasks.register('packagePlugin', Zip) { task ->
            task.group = 'Touch Portal Plugin'
            task.description = 'Package the Project into a TPP'
            task.dependsOn copyResources, copyGeneratedJavaResources, copyGeneratedKotlinResources, copyJar

            var destinationFileName = project.provider { extension.mainClassSimpleName.get() + ".tpp" }

            task.from(pluginStagingDir)
            task.includeEmptyDirs = false
            task.reproducibleFileOrder = true

            task.destinationDirectory = pluginBuildDir
            task.archiveFileName = destinationFileName

            if (logger.infoEnabled) {
                task.doFirst {
                    logger.info("Source:      ${pluginStagingDir.get()}")
                    logger.info("Destination: ${pluginBuildDir.get()}/${destinationFileName.get()}")

                    if (logger.debugEnabled) {
                        pluginMainDir.get().asFileTree.files.each {
                            logger.debug("Packaging: ${it}")
                        }
                    }
                }
            }

            task.doLast {
                logger.lifecycle('Plugin Packaged')

                if (logger.debugEnabled) {
                    task.getOutputs().files.asFileTree.each {
                        logger.debug("Output file: ${it}")
                    }
                }
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