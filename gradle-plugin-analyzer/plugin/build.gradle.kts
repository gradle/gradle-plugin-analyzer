plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api(projects.analyzer)

    implementation("org.jsoup:jsoup:1.15.3")
}
