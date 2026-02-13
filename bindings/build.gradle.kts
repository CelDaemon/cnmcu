import tools.elmfer.GenerateSources

plugins {
    id("net.fabricmc.fabric-loom-remap")
}

val minecraftVersion: String by project
val loaderVersion: String by project

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

val generateNativesSources by tasks.registering(GenerateSources::class)

val nativesSourcesElements by configurations.registering {
    isCanBeConsumed = true
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
}

artifacts {
    add(nativesSourcesElements.name, generateNativesSources)
}