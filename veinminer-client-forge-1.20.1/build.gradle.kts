import net.minecraftforge.gradle.userdev.UserDevExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("net.minecraftforge.gradle") version "6.0.29"
}

group = property("group") as String
version = property("version") as String

val minecraftVersion: String by project
val forgeVersion: String by project
val mappingsVersion: String by project
val kffVersion: String by project
val modid: String by project
val author = property("author") as String
val projectName = property("projectName") as String
val description = property("description") as String
val licence = property("licence") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

extensions.configure<UserDevExtension>("minecraft") {
    mappings("official", mappingsVersion)
    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

    runs {
        create("client") {
            workingDirectory(project.file("run/client"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create(modid) {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net/")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
}

dependencies {
    "minecraft"("net.minecraftforge:forge:$forgeVersion")
    implementation("thedarkcolour:kotlinforforge:$kffVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

tasks.processResources {
    val tokens = mapOf(
        "modid" to modid,
        "version" to project.version.toString(),
        "name" to projectName,
        "description" to description,
        "author" to author,
        "license" to licence,
        "forgeVersion" to forgeVersion,
        "minecraftVersion" to minecraftVersion,
    )
    inputs.properties(tokens)
    filesMatching("META-INF/mods.toml") {
        expand(tokens)
    }
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Specification-Title" to modid,
                "Specification-Vendor" to author,
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version.toString(),
                "Implementation-Vendor" to author,
            )
        )
    }
}
