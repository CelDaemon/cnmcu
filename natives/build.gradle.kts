import org.gradle.internal.file.Deleter

plugins {
    `lifecycle-base`
}

val generatedNativesSources by configurations.registering {
    isCanBeResolved = true
}

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
                "-DGENERATED_SOURCES_DIR=${generatedDirectory.get().asFile.absolutePath}"
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
    inputs.files(generatedNativesSources.map { it.incoming.files })

    generatedDirectory = generatedNativesSources.map { it.singleFile }.get()
}

tasks.assemble {
    dependsOn(compile)
}

dependencies {
    generatedNativesSources(project(":bindings", "nativesSourcesElements"))
}

val default by configurations.registering

artifacts {
    add(default.name, compile)
}