import tools.elmfer.Compile

plugins {
    `lifecycle-base`
}

val generatedSources by configurations.registering {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val compile by tasks.registering(Compile::class) {
    generatedDirectory = generatedSources.flatMap { it.elements }.map { it.first().asFile }
}

tasks.assemble {
    dependsOn(compile)
}

dependencies {
    generatedSources(project(":bindings", "nativesSourcesElements"))
}

val default by configurations.registering {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(default.name, compile)
}