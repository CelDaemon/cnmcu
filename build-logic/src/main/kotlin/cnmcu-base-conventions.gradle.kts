val modVersion: String by project
val minecraftVersion: String by project
val mavenGroup: String by project

group = mavenGroup
version = "$modVersion+$minecraftVersion"