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
    implementation("com.google.guava:guava:32.1.2-jre")
}
