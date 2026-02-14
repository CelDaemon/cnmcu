plugins {
    id("net.fabricmc.fabric-loom-remap")
}

val minecraftVersion: String by project
val loaderVersion: String by project

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
}