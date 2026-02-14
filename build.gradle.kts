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

val clientShade by configurations.registering {
	isCanBeConsumed = false
	isCanBeResolved = true
}
val shade by configurations.registering {
	isCanBeConsumed = false
	isCanBeResolved = true
}
val natives by configurations.registering {
	isCanBeConsumed = false
	isCanBeResolved = true
}

val sources by configurations.registering {
	isCanBeConsumed = false
	isCanBeResolved = true
}

loom.splitEnvironmentSourceSets()

val client by sourceSets.existing

configurations.named("clientImplementation") {
	extendsFrom(clientShade.get())
}

configurations.implementation {
	extendsFrom(shade.get())
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

	shade(project(":bindings", "namedElements"))
	shade(project(":common", "namedElements"))

	natives(project(":natives"))

	sources(project(":bindings", "sourcesElements"))
	sources(project(":common", "sourcesElements"))
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

val nativesProvider: Provider<FileCollection> = provider { project.findProperty("prebuilt_natives") as String? }
	.map<FileCollection> { files(it) }
	.orElse(natives.map { it.incoming.files })

tasks.processResources {
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
		into("com/elmfer/cnmcu/natives")
    }
}

tasks.jar {
	enabled = false
}

tasks.shadowJar {
	from(client.map { it.output })
	configurations = clientShade.zip(shade, ::Pair).map { listOf(it.first, it.second) }
	archiveClassifier = "dev"
	destinationDirectory = layout.buildDirectory.dir("devlibs")
}

tasks.named<Jar>("sourcesJar") {
	archiveClassifier = "dev-sources"
	from(sources.map { it.incoming.files })
}

tasks.remapJar {
	inputFile = tasks.shadowJar.flatMap { it.archiveFile }
}
