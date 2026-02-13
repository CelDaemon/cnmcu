import org.gradle.internal.file.Deleter

plugins {
    `lifecycle-base`
}

val a = layout.projectDirectory.dir("generated")

abstract class Cmake : DefaultTask() {
    @get:Inject
    abstract val exec: ExecOperations

    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val deleter: Deleter

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        outputDirectory.convention(layout.buildDirectory.dir("binaries"))
    }

    @get:InputDirectory
    abstract val sourceDirectory: DirectoryProperty

    init {
        sourceDirectory.convention(layout.projectDirectory.dir("src"))
    }

    @get:InputDirectory
    abstract val generatedDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        val outputDirectory = outputDirectory.get()
        val sourceDirectory = sourceDirectory.get()
        deleter.ensureEmptyDirectory(outputDirectory.asFile)

        exec.exec {
            executable = "cmake"
            args(
                "-S", sourceDirectory,
                "-B", temporaryDir,
                "-DGENERATED_SOURCES_DIR=${generatedDirectory}"
            )
        }

        exec.exec {
            executable = "cmake"
            args(
                "--build", temporaryDir,
                "--parallel", 4
            )
        }

        exec.exec {
            executable = "cmake"
            args(
                "--install", temporaryDir,
                "--prefix", outputDirectory
            )
        }
    }
}

val compile by tasks.registering(Cmake::class) {
    generatedDirectory = file("a")
}

tasks.assemble {
    dependsOn(compile)
}

val default by configurations.registering

artifacts {
    add(default.name, compile)
}