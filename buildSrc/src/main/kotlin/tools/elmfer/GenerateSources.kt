package tools.elmfer

import com.badlogic.gdx.jnigen.NativeCodeGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.file.Deleter
import org.gradle.kotlin.dsl.getByType
import javax.inject.Inject

abstract class GenerateSources : DefaultTask() {
    @get:CompileClasspath
    abstract val classPath: ConfigurableFileCollection

    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val deleter: Deleter

    init {
        group = "build"
        description = "Creates JNI bridge .h and .cpp files."

        val main = project.extensions.getByType(JavaPluginExtension::class)
            .sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME)

        classPath.convention(main.map { layout.files(it.compileClasspath, it.output.classesDirs) })

        sourceDirectory.convention(layout.dir(main.map { it.java.srcDirs.first() }))

        outputDirectory.convention(layout.buildDirectory.dir("generated/sources/natives"))
    }
    @TaskAction
    fun execute() {
        deleter.ensureEmptyDirectory(outputDirectory.get().asFile)

        val generator = NativeCodeGenerator()
        generator.generate(sourceDirectory.get().asFile.absolutePath, classPath.asPath, outputDirectory.get().asFile.absolutePath)
    }
}