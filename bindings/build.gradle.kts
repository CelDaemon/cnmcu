plugins {
    id("cnmcu-java-conventions")
}

val generateNativesSources by tasks.registering(GenerateSources::class)

val nativesSourcesElements by configurations.registering {
    isCanBeConsumed = true
    isCanBeResolved = false
}

dependencies {
    implementation(projects.common)
}

artifacts {
    add(nativesSourcesElements.name, generateNativesSources)
}