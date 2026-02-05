import tools.elmfer.CompileNativesTask
import tools.elmfer.GenNativeSourcesTask

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

val clientShade by configurations.registering

loom.splitEnvironmentSourceSets()

val client by sourceSets.existing

configurations.named("clientImplementation") {
	extendsFrom(clientShade.get())
}

loom {
	mods {
		register("cnmcu") {
			sourceSet(sourceSets.main.get())
			sourceSet(client.get())
		}
	}

	log4jConfigs.from("log4j-dev.xml")
}


fabricApi {
	configureDataGeneration {
		client = true
	}
}

repositories {
	mavenCentral()
}
dependencies {
	minecraft("com.mojang:minecraft:$minecraftVersion")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

	modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

	clientShade("io.github.spair:imgui-java-binding:$imguiVersion")
	clientShade("io.github.spair:imgui-java-lwjgl3:$imguiVersion") {
		exclude(group = "org.lwjgl") // Do not include transitive LWJGL3 dependency.
	}

	clientShade("io.github.spair:imgui-java-natives-windows:$imguiVersion")
	clientShade("io.github.spair:imgui-java-natives-linux:$imguiVersion")
	clientShade("io.github.spair:imgui-java-natives-macos:$imguiVersion")
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

val nativesProvider: Provider<Directory> = provider { project.findProperty("prebuilt_natives") as String? }
	.map { layout.projectDirectory.dir(it) }
	.orElse(compileNatives.flatMap { it.outputDir })

copyNatives {
	inputs.files(nativesProvider)

	from(nativesProvider)

	into(loom.runs.named("client").map { layout.projectDirectory.dir(it.runDir).dir("cnmcu/natives/${version}") })
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

tasks.jar {
	enabled = false
}

tasks.shadowJar {
	from(client.map { it.output })
	configurations = clientShade.map { listOf(it) }
	archiveClassifier = "dev"
	destinationDirectory = layout.buildDirectory.dir("devlibs")
}

tasks.named<Jar>("sourcesJar") {
	archiveClassifier = "dev-sources"
}

tasks.remapJar {
	inputFile = tasks.shadowJar.flatMap { it.archiveFile }
}
