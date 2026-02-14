import tools.elmfer.Compile

plugins {
    `lifecycle-base`
}

val generatedSources by configurations.registering {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val compile by tasks.registering(Compile::class) {
    inputs.files(generatedSources.map { it.incoming.files })

    generatedDirectory = generatedSources.map { it.singleFile }.get()
}

tasks.assemble {
    dependsOn(compile)
}

dependencies {
    generatedSources(project(":bindings", "nativesSourcesElements"))
}

val default by configurations.registering

artifacts {
    add(default.name, compile)
}