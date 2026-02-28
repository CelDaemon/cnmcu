import net.fabricmc.loom.util.Constants

plugins {
    id("cnmcu-java-conventions")
}

val generateNativesSources by tasks.registering(GenerateSources::class)

val nativesSourcesElements by configurations.registering {
    isCanBeConsumed = true
    isCanBeResolved = false
}

dependencies {
    implementation(projects.common) {
        targetConfiguration = Constants.Configurations.NAMED_ELEMENTS
    }
}

artifacts {
    add(nativesSourcesElements.name, generateNativesSources)
}