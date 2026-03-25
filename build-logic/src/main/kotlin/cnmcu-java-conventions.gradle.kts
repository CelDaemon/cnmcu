plugins {
    id("net.fabricmc.fabric-loom")
}
val libs = versionCatalogs.named("libs")

dependencies {
    minecraft(libs.findLibrary("minecraft").get())
    implementation(libs.findLibrary("fabric-loader").get())
    implementation(libs.findLibrary("fabric-api").get())
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}