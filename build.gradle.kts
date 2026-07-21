plugins {
    id("java-library")
    alias(libs.plugins.run.paper)
    alias(libs.plugins.minotaur)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.netty.buffer)
    compileOnly(libs.netty.transport)

    testImplementation(libs.paper.api)
    testImplementation(libs.netty.transport)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    compileJava {
        options.release = 21
    }

    test {
        useJUnitPlatform()
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveClassifier.set("")
    }
}

modrinth {
    token.set(providers.environmentVariable("MODRINTH_TOKEN"))
    projectId.set("jeirecipefix")
    versionNumber.set(project.version.toString())
    versionType.set(resolveVersionType(project.version.toString()))
    uploadFile.set(tasks.jar.flatMap { it.archiveFile })
    gameVersions.addAll(
        "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7",
        "1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2", "26.2"
    )
    loaders.addAll("paper", "purpur", "folia")
    changelog.set(provider { extractChangelogSection(project.version.toString()) })
    syncBodyFrom.set(provider { file("README.md").readText() })
}

fun resolveVersionType(version: String): String = when {
    version.contains("alpha") -> "alpha"
    version.contains("beta") -> "beta"
    else -> "release"
}

fun extractChangelogSection(version: String): String {
    val file = file("CHANGELOG.md")
    if (!file.exists()) return ""
    val out = StringBuilder()
    var capture = false
    for (line in file.readLines()) {
        if (line.startsWith("## ")) {
            if (capture) break
            capture = line.contains(version)
            continue
        }
        if (capture) out.appendLine(line)
    }
    return out.toString().trim()
}
