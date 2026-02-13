import tools.elmfer.Compile

plugins {
    `lifecycle-base`
}

val generatedNativesSources by configurations.registering {
    isCanBeResolved = true
}

val compile by tasks.registering(Compile::class) {
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