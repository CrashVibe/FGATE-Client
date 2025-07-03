import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.0-RC2"
    id("com.gradleup.shadow") version "8.3.6"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "org.crashvibe"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.0.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.java-websocket:Java-WebSocket:1.6.0")
    implementation("com.github.technicallycoded:FoliaLib:0.4.4")
    implementation("org.bstats:bstats-bukkit:3.1.0")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.4")
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))

            freeCompilerArgs.addAll("-Xjsr305=strict", "-java-parameters")

        }
    }
    shadowJar {
        archiveFileName = "${rootProject.name}-Modern-${project.version}.${archiveExtension.get()}"
        exclude("META-INF/**")
        relocate("org.java_websocket", "${project.group}.libs.websocket")
        relocate("com.github.technicallycoded.folialib", "${project.group}.libs.folialib")
        relocate("org.bstats", "${project.group}.libs.bstats")
        relocate("com.tcoded.folialib", "${project.group}.libs.bstats")
    }

    build {
        dependsOn(shadowJar)
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}
