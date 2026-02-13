import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val jnigenVersion: String by project

plugins {
	`kotlin-dsl`
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(gradleApi())
	implementation("com.badlogicgames.jnigen:jnigen-core:$jnigenVersion")
}

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

kotlin.compilerOptions.jvmTarget = JvmTarget.JVM_21