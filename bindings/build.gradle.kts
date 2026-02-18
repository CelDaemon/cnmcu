import net.fabricmc.loom.util.Constants
import tools.elmfer.GenerateSources

plugins {
    id("cnmcu-java-conventions")
}

val generateNativesSources by tasks.registering(GenerateSources::class)

val nativesSourcesElements by configurations.registering {
    isCanBeConsumed = true
    isCanBeResolved = false
}

dependencies {
    implementation(project(":common", Constants.Configurations.NAMED_ELEMENTS))
}

artifacts {
    add(nativesSourcesElements.name, generateNativesSources)
}