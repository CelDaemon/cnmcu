import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    implementation(libs.fabric.loom)
    implementation(libs.jnigen)

    constraints {
        implementation("ch.qos.logback:logback-classic") {
            version {
                require("[1.5.13,)")
            }
            because("version 1.4.2 is vulnerable to CVE-2024-12798")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin.compilerOptions.jvmTarget = JvmTarget.JVM_21