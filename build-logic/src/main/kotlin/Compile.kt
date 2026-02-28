import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.file.Deleter
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class Compile : DefaultTask() {

    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    @get:InputDirectory
    abstract val generatedDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val configuration: Property<String>

    @get:Inject
    abstract val exec: ExecOperations

    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val deleter: Deleter

    init {
        sourceDirectory.convention(layout.projectDirectory.dir("src"))
        outputDirectory.convention(layout.buildDirectory.dir("bin"))
        configuration.convention("Release")

        group = "build"
        description = "Compiles native source files using CMake."
    }

    @TaskAction
    fun execute() {
        val outputDirectory = outputDirectory.get()
        val sourceDirectory = sourceDirectory.get()
        val generatedDirectory = generatedDirectory.get()
        deleter.ensureEmptyDirectory(outputDirectory.asFile)

        exec.exec {
            executable = "cmake"
            args(
                "-S", sourceDirectory,
                "-B", temporaryDir,
                "-DGENERATED_SOURCES_DIR=${generatedDirectory.asFile.absolutePath}",
                "-DCMAKE_BUILD_TYPE=${configuration.get()}"
            )
        }

        exec.exec {
            executable = "cmake"
            args(
                "--build", temporaryDir,
                "--parallel", 4,
                "--config", configuration.get()
            )
        }

        exec.exec {
            executable = "cmake"
            args(
                "--install", temporaryDir,
                "--prefix", outputDirectory,
                "--config", configuration.get()
            )
        }
    }
}