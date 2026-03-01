import net.fabricmc.loom.task.RemapSourcesJarTask
import net.fabricmc.loom.task.RemapTaskConfiguration

plugins {
    id("net.fabricmc.fabric-loom-remap")
}
val libs = versionCatalogs.named("libs")

dependencies {
    minecraft(libs.findLibrary("minecraft").get())
    mappings(loom.officialMojangMappings())
    modImplementation(libs.findLibrary("fabric-loader").get())
    modImplementation(libs.findLibrary("fabric-api").get())
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}


tasks.named<RemapSourcesJarTask>(RemapTaskConfiguration.REMAP_SOURCES_JAR_TASK_NAME) {
    archiveClassifier = "dev-sources"
}