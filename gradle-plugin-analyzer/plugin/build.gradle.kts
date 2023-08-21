plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "1.8.20"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api(projects.analyzer)

    implementation("org.jsoup:jsoup:1.15.3")
    implementation("com.google.guava:guava:32.1.2-jre")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.5.1")
}
