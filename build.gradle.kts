val minecraftVersion: String by project
val archivesBaseName: String by project
val loaderVersion: String by project
val fabricVersion: String by project
val imguiVersion: String by project

plugins {
	id("cnmcu-java-conventions")
	id("com.gradleup.shadow")
}

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

	attributes {
		attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class, Category.DOCUMENTATION))
	}
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
		register(project.name) {
			sourceSet(sourceSets.main.get())
			sourceSet(client.get())
			sourceSet(SourceSet.MAIN_SOURCE_SET_NAME, projects.common)
			sourceSet(SourceSet.MAIN_SOURCE_SET_NAME, projects.bindings)
			configuration(shade.get())
			configuration(clientShade.get())
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
	clientShade("io.github.spair:imgui-java-binding:$imguiVersion")
	clientShade("io.github.spair:imgui-java-lwjgl3:$imguiVersion") {
		exclude(group = "org.lwjgl") // Do not include transitive LWJGL3 dependency.
	}

	clientShade("io.github.spair:imgui-java-natives-windows:$imguiVersion")
	clientShade("io.github.spair:imgui-java-natives-linux:$imguiVersion")
	clientShade("io.github.spair:imgui-java-natives-macos:$imguiVersion")

	sequenceOf(projects.bindings, projects.common).forEach {
		shade(it) {
			targetConfiguration = "namedElements"
		}
		sources(it)
	}

	natives(projects.natives)
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

tasks.sourcesJar {
	from(
		sources.flatMap { it.elements }
			.map { it.map { file -> zipTree(file) } }
	)
}

tasks.remapJar {
	inputFile = tasks.shadowJar.flatMap { it.archiveFile }
}
