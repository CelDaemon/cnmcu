import tools.elmfer.*

val modVersion: String by project
val minecraftVersion: String by project
val mavenGroup: String by project
val archivesBaseName: String by project
val loaderVersion: String by project
val fabricVersion: String by project
val imguiVersion: String by project

plugins {
	id("net.fabricmc.fabric-loom-remap")
	id("com.gradleup.shadow")
}

version = "$modVersion+$minecraftVersion"
group = mavenGroup

loom.log4jConfigs.from("log4j-dev.xml")

repositories {
	mavenCentral()
}

val bundle by configurations.registering

configurations.implementation {
	extendsFrom(bundle.get())
}

dependencies {
	minecraft("com.mojang:minecraft:$minecraftVersion")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

	modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")


	bundle("io.github.spair:imgui-java-binding:$imguiVersion")
	bundle("io.github.spair:imgui-java-lwjgl3:$imguiVersion") {
        exclude(group = "org.lwjgl") // Do not include transitive LWJGL3 dependency.
    }

	bundle("io.github.spair:imgui-java-natives-windows:$imguiVersion")
	bundle("io.github.spair:imgui-java-natives-linux:$imguiVersion")
	bundle("io.github.spair:imgui-java-natives-macos:$imguiVersion")
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_$archivesBaseName" }
	}
}

val genNativeSources by tasks.registering(GenNativeSourcesTask::class) {
	sourceDir = sourceSets.main.map { it.java.sourceDirectories.first() }
}

val copyNatives by tasks.registering(Copy::class)

val compileNatives by tasks.registering(CompileNativesTask::class) {
	sourceDir = file("src/main/cpp")
	bridgeDir = genNativeSources.flatMap { it.bridgeDir }
	outputDir = layout.buildDirectory.dir("natives")

	finalizedBy(copyNatives)
}

var prebuiltProvider: Provider<Directory> = project.provider {
	layout.projectDirectory.dir(project.property("prebuilt_natives") as String)
}
var compiledProvider: Provider<Directory> = compileNatives.flatMap { it.outputDir }
var nativesProvider = if(project.hasProperty("prebuilt_natives")) prebuiltProvider else compiledProvider

copyNatives {
	inputs.files(nativesProvider)

	from(nativesProvider)

	into(layout.projectDirectory.dir(loom.runs.getByName("client").runDir)
			.dir("cnmcu/natives/${version}"))
}

tasks.processResources {
	inputs.files(nativesProvider)
	inputs.property("version", version)
	inputs.property("minecraft_version", minecraftVersion)
	inputs.property("loader_version", loaderVersion)

	filesMatching("fabric.mod.json") {
        expand(
            "version" to version,
            "minecraft_version" to minecraftVersion,
            "loader_version" to loaderVersion
        )
    }

    from(nativesProvider) {
        into("com/elmfer/cnmcu/config")
    }
}

tasks.shadowJar {
	configurations = bundle.map { listOf(it) }
}

tasks.remapJar {
	from(tasks.getByName<Jar>("shadowJar").archiveFile)
}
