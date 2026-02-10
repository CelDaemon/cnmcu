val jnigenVersion: String by project

plugins {
	java
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(gradleApi())
	implementation("com.badlogicgames.jnigen:jnigen-core:$jnigenVersion")
}