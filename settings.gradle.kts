pluginManagement {
	val loomVersion: String by settings
	val shadowVersion: String by settings
	plugins {
		id("net.fabricmc.fabric-loom-remap") version loomVersion
		id("com.gradleup.shadow") version shadowVersion
	}
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		mavenCentral()
		gradlePluginPortal()
	}
}