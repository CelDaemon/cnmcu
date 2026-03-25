plugins {
	id("cnmcu-java-conventions")
	alias(libs.plugins.shadow)
}

group = "com.elmfer.cnmcu"
version = "0.0.10-alpha+${libs.minecraft.get().version}"

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
	clientShade(libs.imgui)
	clientShade(libs.imgui.lwjgl3) {
		exclude(group = "org.lwjgl")
	}
	libs.imgui.natives.let { sequenceOf(it.windows, it.linux, it.macos) }.forEach {
		clientShade(it)
	}

	sequenceOf(projects.bindings, projects.common).forEach {
		shade(it)
		sources(it)
	}

	natives(projects.natives)
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_${project.name}" }
	}
}

val nativesProvider: Provider<FileCollection> = provider { project.findProperty("prebuilt_natives") as String? }
	.map<FileCollection> { files(it) }
	.orElse(natives.map { it.incoming.files })

tasks.processResources {
	inputs.property("version", version)
	inputs.property("minecraft_version", libs.minecraft.map { it.version })
	inputs.property("loader_version", libs.fabric.loader.map { it.version })

	filesMatching("fabric.mod.json") {
        expand(
            "version" to version,
			"minecraft_version" to libs.minecraft.get().version as String,
			"loader_version" to libs.fabric.loader.get().version as String
        )
    }

    from(nativesProvider) {
		into("com/elmfer/cnmcu/natives")
    }
}

tasks.shadowJar {
	from(client.map { it.output })
	configurations = clientShade.zip(shade, ::Pair).map { listOf(it.first, it.second) }
	archiveClassifier = null
}

tasks.sourcesJar {
	from(
		sources.flatMap { it.elements }
			.map { it.map { file -> zipTree(file) } }
	)
}
